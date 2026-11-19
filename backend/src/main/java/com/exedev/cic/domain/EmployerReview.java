package com.exedev.cic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employer_reviews")
public class EmployerReview {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(name = "job_title", nullable = false, length = 140)
    private String jobTitle;

    @Column(name = "employment_status", nullable = false, length = 40)
    private String employmentStatus;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "culture_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal cultureRating;

    @Column(name = "compensation_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal compensationRating;

    @Column(nullable = false, length = 240)
    private String headline;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pros;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String cons;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmployerReview() {
        // JPA
    }

    public EmployerReview(
            UUID id,
            UUID companyId,
            String jobTitle,
            String employmentStatus,
            BigDecimal rating,
            BigDecimal cultureRating,
            BigDecimal compensationRating,
            String headline,
            String pros,
            String cons
    ) {
        this.id = id;
        this.companyId = companyId;
        this.jobTitle = jobTitle;
        this.employmentStatus = employmentStatus;
        this.rating = rating;
        this.cultureRating = cultureRating;
        this.compensationRating = compensationRating;
        this.headline = headline;
        this.pros = pros;
        this.cons = cons;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getJobTitle() { return jobTitle; }
    public String getEmploymentStatus() { return employmentStatus; }
    public BigDecimal getRating() { return rating; }
    public BigDecimal getCultureRating() { return cultureRating; }
    public BigDecimal getCompensationRating() { return compensationRating; }
    public String getHeadline() { return headline; }
    public String getPros() { return pros; }
    public String getCons() { return cons; }
    public Instant getCreatedAt() { return createdAt; }
}
