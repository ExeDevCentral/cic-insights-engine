package com.exedev.cic.service;

import com.exedev.cic.api.dto.CompanyInsightDto;
import com.exedev.cic.api.dto.SubmitReviewRequest;
import com.exedev.cic.domain.CompanyProfile;
import com.exedev.cic.domain.EmployerReview;
import com.exedev.cic.repository.CompanyProfileRepository;
import com.exedev.cic.repository.EmployerReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CompanyInsightsService {

    private final CompanyProfileRepository companyRepository;
    private final EmployerReviewRepository reviewRepository;

    public CompanyInsightsService(
            CompanyProfileRepository companyRepository,
            EmployerReviewRepository reviewRepository
    ) {
        this.companyRepository = companyRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public Page<CompanyInsightDto> searchCompanies(
            String industry,
            BigDecimal minRating,
            String query,
            Pageable pageable
    ) {
        return companyRepository.searchCompanies(industry, minRating, query, pageable)
                .map(this::toInsightDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "companyBySlug", key = "#slug.toLowerCase()")
    public CompanyInsightDto getBySlug(String slug) {
        CompanyProfile company = companyRepository.findBySlug(slug.toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("Company not found for slug: " + slug));
        return toInsightDto(company);
    }

    @Transactional
    @CacheEvict(value = "companyBySlug", key = "#slug.toLowerCase()")
    public CompanyInsightDto submitReview(String slug, SubmitReviewRequest request) {
        CompanyProfile company = companyRepository.findBySlug(slug.toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("Company not found for slug: " + slug));

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

    /**
     * Builds Schema.org EmployerAggregateRating JSON-LD payload for SEO marketing crawlers.
     */
    private CompanyInsightDto toInsightDto(CompanyProfile c) {
        Map<String, Object> jsonLd = new LinkedHashMap<>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "EmployerAggregateRating");
        jsonLd.put("itemReviewed", Map.of(
                "@type", "Organization",
                "name", c.getDisplayName(),
                "address", c.getHeadquarters(),
                "sameAs", "https://indeed.com/cmp/" + c.getSlug()
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
