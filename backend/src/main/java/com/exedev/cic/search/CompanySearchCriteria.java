package com.exedev.cic.search;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable, self-validating search criteria for querying company profiles.
 * Normalizes blank inputs, validates numeric bounds, and enforces a sort-field allowlist.
 */
public record CompanySearchCriteria(
        String industry,
        BigDecimal minRating,
        Boolean verifiedOnly,
        String headquarters,
        Integer minActiveJobs,
        String query
) {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal FIVE = new BigDecimal("5.00");

    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "seoMonthlyImpressions",
            "seoOrganicCtr",
            "overallRating",
            "cultureScore",
            "compensationScore",
            "reviewCount",
            "activeJobPostings",
            "displayName",
            "updatedAt"
    );

    public CompanySearchCriteria {
        industry = normalize(industry);
        headquarters = normalize(headquarters);
        query = normalize(query);

        if (minRating != null && (minRating.compareTo(ZERO) < 0 || minRating.compareTo(FIVE) > 0)) {
            throw new IllegalArgumentException("minRating must be between 0.00 and 5.00");
        }
        if (minActiveJobs != null && minActiveJobs < 0) {
            throw new IllegalArgumentException("minActiveJobs must be >= 0");
        }
    }

    public List<String> queryTokens() {
        if (query == null) {
            return List.of();
        }
        return List.of(query.toLowerCase(Locale.ROOT).split("\\s+"));
    }

    /**
     * Escapes SQL LIKE wildcards so user-supplied '%' or '_' are treated literally.
     */
    public static String escapeLikePattern(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Clamps page size and rejects unknown sort properties before they reach Hibernate.
     */
    public static Pageable sanitizePageable(Pageable pageable, int maxPageSize) {
        int safeSize = Math.min(Math.max(1, pageable.getPageSize()), maxPageSize);
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                    throw new IllegalArgumentException(
                            "Unsupported sort field '" + order.getProperty() + "'. Allowed: " + ALLOWED_SORT_FIELDS
                    );
                }
            }
        }
        return PageRequest.of(Math.max(0, pageable.getPageNumber()), safeSize, sort);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
