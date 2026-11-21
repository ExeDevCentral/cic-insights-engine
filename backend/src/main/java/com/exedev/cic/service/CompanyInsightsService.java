package com.exedev.cic.service;

import com.exedev.cic.api.dto.CompanyInsightDto;
import com.exedev.cic.api.dto.CompanySuggestionDto;
import com.exedev.cic.api.dto.SearchFacetsDto;
import com.exedev.cic.api.dto.SubmitReviewRequest;
import com.exedev.cic.config.CacheConfig;
import com.exedev.cic.config.CicProperties;
import com.exedev.cic.domain.CompanyNotFoundException;
import com.exedev.cic.domain.CompanyProfile;
import com.exedev.cic.domain.EmployerReview;
import com.exedev.cic.repository.CompanyProfileRepository;
import com.exedev.cic.repository.EmployerReviewRepository;
import com.exedev.cic.search.CompanySearchCriteria;
import com.exedev.cic.search.CompanySpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CompanyInsightsService {

    private final CompanyProfileRepository companyRepository;
    private final EmployerReviewRepository reviewRepository;
    private final CicProperties properties;

    public CompanyInsightsService(
            CompanyProfileRepository companyRepository,
            EmployerReviewRepository reviewRepository,
            CicProperties properties
    ) {
        this.companyRepository = companyRepository;
        this.reviewRepository = reviewRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Page<CompanyInsightDto> searchCompanies(CompanySearchCriteria criteria, Pageable pageable) {
        Pageable safePageable = CompanySearchCriteria.sanitizePageable(pageable, properties.maxPageSize());
        return companyRepository.findAll(CompanySpecifications.fromCriteria(criteria), safePageable)
                .map(this::toInsightDto);
    }

    @Transactional(readOnly = true)
    public List<CompanySuggestionDto> suggestCompanies(String rawQuery, Integer requestedLimit) {
        if (rawQuery == null || rawQuery.trim().length() < 2) {
            return List.of();
        }
        int limit = requestedLimit == null
                ? properties.defaultSuggestLimit()
                : Math.min(Math.max(1, requestedLimit), 20);
        String sanitized = CompanySearchCriteria.escapeLikePattern(rawQuery.trim().toLowerCase(Locale.ROOT));
        return companyRepository.findSuggestions(sanitized, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.COMPANY_FACETS_CACHE, key = "'global'")
    public SearchFacetsDto getSearchFacets() {
        long total = companyRepository.count();
        long verified = companyRepository.countByVerifiedEmployerTrue();
        BigDecimal avg = companyRepository.findAverageOverallRating();
        BigDecimal scaledAvg = (avg == null ? BigDecimal.ZERO : avg).setScale(2, RoundingMode.HALF_UP);

        Map<String, Long> byIndustry = new LinkedHashMap<>();
        for (var row : companyRepository.countGroupedByIndustry()) {
            byIndustry.put(row.getIndustry(), row.getCompanyCount());
        }

        return new SearchFacetsDto(total, verified, scaledAvg, byIndustry);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.COMPANY_BY_SLUG_CACHE, key = "#slug.toLowerCase()")
    public CompanyInsightDto getBySlug(String slug) {
        String normalized = slug.toLowerCase(Locale.ROOT);
        CompanyProfile company = companyRepository.findBySlug(normalized)
                .orElseThrow(() -> new CompanyNotFoundException(normalized));
        return toInsightDto(company);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.COMPANY_BY_SLUG_CACHE, key = "#slug.toLowerCase()"),
            @CacheEvict(value = CacheConfig.COMPANY_FACETS_CACHE, allEntries = true)
    })
    public CompanyInsightDto submitReview(String slug, SubmitReviewRequest request) {
        String normalized = slug.toLowerCase(Locale.ROOT);
        CompanyProfile company = companyRepository.findBySlug(normalized)
                .orElseThrow(() -> new CompanyNotFoundException(normalized));

        EmployerReview review = new EmployerReview(
                UUID.randomUUID(),
                company.getId(),
                request.jobTitle().trim(),
                request.employmentStatus(),
                request.rating(),
                request.cultureRating(),
                request.compensationRating(),
                request.headline().trim(),
                request.pros().trim(),
                request.cons().trim()
        );
        reviewRepository.save(review);

        company.recordNewReviewScore(
                request.rating(),
                request.cultureRating(),
                request.compensationRating()
        );
        CompanyProfile saved = companyRepository.save(company);
        return toInsightDto(saved);
    }

    private CompanyInsightDto toInsightDto(CompanyProfile c) {
        Map<String, Object> jsonLd = new LinkedHashMap<>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "EmployerAggregateRating");
        jsonLd.put("itemReviewed", Map.of(
                "@type", "Organization",
                "name", c.getDisplayName(),
                "address", c.getHeadquarters(),
                "sameAs", properties.canonicalBaseUrl() + c.getSlug()
        ));
        jsonLd.put("ratingValue", c.getOverallRating());
        jsonLd.put("bestRating", 5);
        jsonLd.put("worstRating", 1);
        jsonLd.put("ratingCount", Math.max(1, c.getReviewCount()));

        return new CompanyInsightDto(
                c.getId(),
                c.getSlug(),
                c.getDisplayName(),
                c.getIndustry(),
                c.getHeadquarters(),
                c.getEmployeeBand(),
                c.getOverallRating(),
                c.getCultureScore(),
                c.getCompensationScore(),
                c.getReviewCount(),
                c.getActiveJobPostings(),
                c.getSeoOrganicCtr(),
                c.getSeoMonthlyImpressions(),
                c.isVerifiedEmployer(),
                c.getUpdatedAt(),
                jsonLd
        );
    }
}
