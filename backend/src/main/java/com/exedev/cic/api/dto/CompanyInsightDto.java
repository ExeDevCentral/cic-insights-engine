package com.exedev.cic.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CompanyInsightDto(
        UUID id,
        String slug,
        String displayName,
        String industry,
        String headquarters,
        String employeeBand,
        BigDecimal overallRating,
        BigDecimal cultureScore,
        BigDecimal compensationScore,
        int reviewCount,
        int activeJobPostings,
        BigDecimal seoOrganicCtr,
        long seoMonthlyImpressions,
        boolean verifiedEmployer,
        Instant updatedAt,
        Map<String, Object> jsonLdStructuredData
) {}
