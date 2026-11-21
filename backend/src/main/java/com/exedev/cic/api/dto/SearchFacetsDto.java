package com.exedev.cic.api.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SearchFacetsDto(
        long totalCompanies,
        long verifiedCompanies,
        BigDecimal averageRating,
        Map<String, Long> countByIndustry
) {
}
