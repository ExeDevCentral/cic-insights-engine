package com.exedev.cic.api.dto;

import java.math.BigDecimal;

public record CompanySuggestionDto(
        String slug,
        String displayName,
        String industry,
        BigDecimal overallRating,
        boolean verifiedEmployer
) {
}
