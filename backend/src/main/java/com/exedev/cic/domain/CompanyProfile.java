package com.exedev.cic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "company_profiles")
public class CompanyProfile {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String industry;

    @Column(nullable = false, length = 150)
    private String headquarters;

    @Column(name = "employee_band", nullable = false, length = 40)
    private String employeeBand;

    @Column(name = "overall_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal overallRating = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "culture_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal cultureScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "compensation_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal compensationScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "active_job_postings", nullable = false)
    private int activeJobPostings = 0;

    @Column(name = "seo_organic_ctr", nullable = false, precision = 5, scale = 2)
    private BigDecimal seoOrganicCtr = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "seo_monthly_impressions", nullable = false)
    private long seoMonthlyImpressions = 0L;

    @Column(name = "verified_employer", nullable = false)
    private boolean verifiedEmployer = false;

    @Version
    @Column(nullable = false)
    private long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyProfile() {
        // JPA
    }

    public CompanyProfile(
            UUID id,
            String slug,
            String displayName,
            String industry,
            String headquarters,
            String employeeBand,
            int activeJobPostings,
            BigDecimal seoOrganicCtr,
            long seoMonthlyImpressions,
            boolean verifiedEmployer
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.slug = Objects.requireNonNull(slug, "slug must not be null").toLowerCase();
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.industry = Objects.requireNonNull(industry, "industry must not be null");
        this.headquarters = Objects.requireNonNull(headquarters, "headquarters must not be null");
        this.employeeBand = Objects.requireNonNull(employeeBand, "employeeBand must not be null");
        this.activeJobPostings = Math.max(0, activeJobPostings);
        this.seoOrganicCtr = seoOrganicCtr.setScale(2, RoundingMode.HALF_UP);
        this.seoMonthlyImpressions = Math.max(0L, seoMonthlyImpressions);
        this.verifiedEmployer = verifiedEmployer;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Domain invariant: recomputes weighted sliding averages in O(1) without loading
     * historical review collections into memory (prevents OOM and N+1 queries).
     */
    public void recordNewReviewScore(BigDecimal rating, BigDecimal culture, BigDecimal compensation) {
        BigDecimal currentCount = BigDecimal.valueOf(this.reviewCount);
        BigDecimal nextCount = BigDecimal.valueOf(this.reviewCount + 1L);

        this.overallRating = this.overallRating.multiply(currentCount)
                .add(rating)
                .divide(nextCount, 2, RoundingMode.HALF_UP);

        this.cultureScore = this.cultureScore.multiply(currentCount)
                .add(culture)
                .divide(nextCount, 2, RoundingMode.HALF_UP);

        this.compensationScore = this.compensationScore.multiply(currentCount)
                .add(compensation)
                .divide(nextCount, 2, RoundingMode.HALF_UP);

        this.reviewCount += 1;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getDisplayName() { return displayName; }
    public String getIndustry() { return industry; }
    public String getHeadquarters() { return headquarters; }
    public String getEmployeeBand() { return employeeBand; }
    public BigDecimal getOverallRating() { return overallRating; }
    public BigDecimal getCultureScore() { return cultureScore; }
    public BigDecimal getCompensationScore() { return compensationScore; }
    public int getReviewCount() { return reviewCount; }
    public int getActiveJobPostings() { return activeJobPostings; }
    public BigDecimal getSeoOrganicCtr() { return seoOrganicCtr; }
    public long getSeoMonthlyImpressions() { return seoMonthlyImpressions; }
    public boolean isVerifiedEmployer() { return verifiedEmployer; }
    public Instant getUpdatedAt() { return updatedAt; }
}
