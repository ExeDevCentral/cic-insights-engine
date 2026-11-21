package com.exedev.cic.search;

import com.exedev.cic.domain.CompanyProfile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Composable JPA Specifications for dynamic multi-criteria company search.
 * Replaces static JPQL 'OR :param IS NULL' queries so Hibernate only emits active predicates.
 */
public final class CompanySpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private CompanySpecifications() {
    }

    public static Specification<CompanyProfile> fromCriteria(CompanySearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.industry() != null) {
                predicates.add(cb.equal(
                        cb.lower(root.get("industry")),
                        criteria.industry().toLowerCase(Locale.ROOT)
                ));
            }

            if (criteria.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("overallRating"), criteria.minRating()));
            }

            if (Boolean.TRUE.equals(criteria.verifiedOnly())) {
                predicates.add(cb.isTrue(root.get("verifiedEmployer")));
            }

            if (criteria.headquarters() != null) {
                String hqPattern = "%" + CompanySearchCriteria.escapeLikePattern(
                        criteria.headquarters().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.get("headquarters")), hqPattern, LIKE_ESCAPE));
            }

            if (criteria.minActiveJobs() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("activeJobPostings"), criteria.minActiveJobs()));
            }

            // Every whitespace-delimited token must match at least one indexed/searchable field
            for (String token : criteria.queryTokens()) {
                String pattern = "%" + CompanySearchCriteria.escapeLikePattern(token) + "%";
                Predicate matchesName = cb.like(cb.lower(root.get("displayName")), pattern, LIKE_ESCAPE);
                Predicate matchesIndustry = cb.like(cb.lower(root.get("industry")), pattern, LIKE_ESCAPE);
                Predicate matchesHq = cb.like(cb.lower(root.get("headquarters")), pattern, LIKE_ESCAPE);
                Predicate matchesSlug = cb.like(cb.lower(root.get("slug")), pattern, LIKE_ESCAPE);
                predicates.add(cb.or(matchesName, matchesIndustry, matchesHq, matchesSlug));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
