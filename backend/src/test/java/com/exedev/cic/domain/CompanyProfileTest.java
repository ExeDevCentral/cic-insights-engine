package com.exedev.cic.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyProfileTest {

    @Test
    void shouldRecomputeWeightedAverageRatingsInConstantTime() {
        CompanyProfile company = new CompanyProfile(
                UUID.randomUUID(),
                "indeed-global",
                "Indeed",
                "HR Tech & Search",
                "Austin, TX",
                "10,000+",
                420,
                new BigDecimal("6.85"),
                14_500_000L,
                true
        );

        company.recordNewReviewScore(
                new BigDecimal("5.00"),
                new BigDecimal("4.80"),
                new BigDecimal("4.60")
        );

        assertThat(company.getReviewCount()).isEqualTo(1);
        assertThat(company.getOverallRating()).isEqualByComparingTo("5.00");

        company.recordNewReviewScore(
                new BigDecimal("4.00"),
                new BigDecimal("4.20"),
                new BigDecimal("4.40")
        );

        assertThat(company.getReviewCount()).isEqualTo(2);
        assertThat(company.getOverallRating()).isEqualByComparingTo("4.50");
        assertThat(company.getCultureScore()).isEqualByComparingTo("4.50");
        assertThat(company.getCompensationScore()).isEqualByComparingTo("4.50");
    }
}
