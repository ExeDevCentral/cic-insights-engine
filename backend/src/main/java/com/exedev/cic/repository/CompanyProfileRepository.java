package com.exedev.cic.repository;

import com.exedev.cic.api.dto.CompanySuggestionDto;
import com.exedev.cic.domain.CompanyProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyProfileRepository
        extends JpaRepository<CompanyProfile, UUID>, JpaSpecificationExecutor<CompanyProfile> {

    Optional<CompanyProfile> findBySlug(String slug);

    long countByVerifiedEmployerTrue();

    @Query("SELECT COALESCE(AVG(c.overallRating), 0) FROM CompanyProfile c")
    BigDecimal findAverageOverallRating();

    @Query("""
            SELECT c.industry AS industry, COUNT(c) AS companyCount
            FROM CompanyProfile c
            GROUP BY c.industry
            ORDER BY COUNT(c) DESC, c.industry ASC
            """)
    List<IndustryFacetProjection> countGroupedByIndustry();

    @Query("""
            SELECT new com.exedev.cic.api.dto.CompanySuggestionDto(
                c.slug, c.displayName, c.industry, c.overallRating, c.verifiedEmployer
            )
            FROM CompanyProfile c
            WHERE LOWER(c.displayName) LIKE LOWER(CONCAT(:prefix, '%'))
               OR LOWER(c.slug) LIKE LOWER(CONCAT(:prefix, '%'))
               OR LOWER(c.displayName) LIKE LOWER(CONCAT('%', :prefix, '%'))
            ORDER BY CASE WHEN LOWER(c.displayName) LIKE LOWER(CONCAT(:prefix, '%')) THEN 0 ELSE 1 END,
                     c.seoMonthlyImpressions DESC
            """)
    List<CompanySuggestionDto> findSuggestions(@Param("prefix") String prefix, Pageable pageable);

    interface IndustryFacetProjection {
        String getIndustry();
        long getCompanyCount();
    }
}
