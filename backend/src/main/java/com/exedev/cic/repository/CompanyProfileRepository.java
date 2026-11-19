package com.exedev.cic.repository;

import com.exedev.cic.domain.CompanyProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {

    Optional<CompanyProfile> findBySlug(String slug);

    @Query("""
        SELECT c FROM CompanyProfile c
        WHERE (:industry IS NULL OR LOWER(c.industry) = LOWER(:industry))
          AND (:minRating IS NULL OR c.overallRating >= :minRating)
          AND (:query IS NULL OR LOWER(c.displayName) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<CompanyProfile> searchCompanies(
            @Param("industry") String industry,
            @Param("minRating") BigDecimal minRating,
            @Param("query") String query,
            Pageable pageable
    );
}
