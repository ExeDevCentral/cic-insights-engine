package com.exedev.cic.service;

import com.exedev.cic.api.dto.CompanyInsightDto;
import com.exedev.cic.api.dto.CompanySuggestionDto;
import com.exedev.cic.api.dto.SearchFacetsDto;
import com.exedev.cic.api.dto.SubmitReviewRequest;
import com.exedev.cic.config.CicProperties;
import com.exedev.cic.domain.CompanyNotFoundException;
import com.exedev.cic.domain.CompanyProfile;
import com.exedev.cic.domain.EmployerReview;
import com.exedev.cic.repository.CompanyProfileRepository;
import com.exedev.cic.repository.EmployerReviewRepository;
import com.exedev.cic.search.CompanySearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyInsightsServiceTest {

    @Mock
    private CompanyProfileRepository companyRepository;

    @Mock
    private EmployerReviewRepository reviewRepository;

    private CompanyInsightsService service;

    @BeforeEach
    void setUp() {
        CicProperties props = new CicProperties("https://insights.exedev.com/companies/", 50, 8);
        service = new CompanyInsightsService(companyRepository, reviewRepository, props);
    }

    @Test
    void skipsDatabaseCallWhenSuggestionPrefixIsTooShort() {
        List<CompanySuggestionDto> result = service.suggestCompanies("a", 5);
        assertThat(result).isEmpty();
        verifyNoInteractions(companyRepository);
    }

    @Test
    void escapesWildcardsAndAppliesLimitOnSuggestions() {
        CompanySuggestionDto suggestion = new CompanySuggestionDto(
                "acme-cloud", "Acme Cloud", "Cloud", new BigDecimal("4.60"), true
        );
        when(companyRepository.findSuggestions(eq("ac\\%"), eq(PageRequest.of(0, 5))))
                .thenReturn(List.of(suggestion));

        List<CompanySuggestionDto> result = service.suggestCompanies(" Ac% ", 5);

        assertThat(result).containsExactly(suggestion);
    }

    @Test
    void aggregatesGlobalSearchFacets() {
        CompanyProfileRepository.IndustryFacetProjection fintech = new CompanyProfileRepository.IndustryFacetProjection() {
            @Override public String getIndustry() { return "Fintech"; }
            @Override public long getCompanyCount() { return 4L; }
        };
        when(companyRepository.count()).thenReturn(10L);
        when(companyRepository.countByVerifiedEmployerTrue()).thenReturn(7L);
        when(companyRepository.findAverageOverallRating()).thenReturn(new BigDecimal("4.346"));
        when(companyRepository.countGroupedByIndustry()).thenReturn(List.of(fintech));

        SearchFacetsDto facets = service.getSearchFacets();

        assertThat(facets.totalCompanies()).isEqualTo(10L);
        assertThat(facets.verifiedCompanies()).isEqualTo(7L);
        assertThat(facets.averageRating()).isEqualByComparingTo("4.35");
        assertThat(facets.countByIndustry()).containsEntry("Fintech", 4L);
    }

    @Test
    void submitsReviewAndBuildsCanonicalJsonLd() {
        CompanyProfile profile = new CompanyProfile(
                UUID.randomUUID(),
                "acme-cloud",
                "Acme Cloud",
                "Cloud",
                "Austin, TX",
                "1,000+",
                40,
                new BigDecimal("5.40"),
                250_000L,
                true
        );
        when(companyRepository.findBySlug("acme-cloud")).thenReturn(Optional.of(profile));
        when(companyRepository.save(any(CompanyProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitReviewRequest req = new SubmitReviewRequest(
                "Staff Engineer",
                "CURRENT_EMPLOYEE",
                new BigDecimal("4.80"),
                new BigDecimal("4.60"),
                new BigDecimal("4.70"),
                "Strong engineering culture",
                "Clear ownership and modern Java stack",
                "On-call rotation can get busy during releases"
        );

        CompanyInsightDto result = service.submitReview("ACME-CLOUD", req);

        verify(reviewRepository).save(any(EmployerReview.class));
        assertThat(result.reviewCount()).isEqualTo(1);
        assertThat(result.overallRating()).isEqualByComparingTo("4.80");
        assertThat(result.jsonLdStructuredData().get("@type")).isEqualTo("EmployerAggregateRating");
    }

    @Test
    void throwsDomainExceptionWhenSlugDoesNotExist() {
        when(companyRepository.findBySlug("missing-co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("missing-co"))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("missing-co");
    }

    @Test
    @SuppressWarnings("unchecked")
    void delegatesSearchWithSpecificationAndClampedPageable() {
        CompanySearchCriteria criteria = new CompanySearchCriteria("Cloud", new BigDecimal("4.00"), true, null, null, "acme");
        when(companyRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<CompanyInsightDto> page = service.searchCompanies(criteria, PageRequest.of(0, 200));

        assertThat(page.getContent()).isEmpty();
        verify(companyRepository).findAll(any(Specification.class), eq(PageRequest.of(0, 50)));
    }
}
