package com.exedev.cic.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyProfileTest {

    private static CompanyProfile sampleCompany() {
        return new CompanyProfile(
                UUID.randomUUID(),
                "acme-cloud",
                "Acme Cloud",
                "Cloud Infrastructure",
                "Austin, TX",
                "1,000-5,000",
                85,
                new BigDecimal("6.85"),
                1_500_000L,
                true
        );
    }

    @Test
    void recomputesWeightedAveragesInConstantTime() {
        CompanyProfile company = sampleCompany();

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

    @Test
    void rejectsOutOfRangeScores() {
        CompanyProfile company = sampleCompany();

        assertThatThrownBy(() -> company.recordNewReviewScore(
                new BigDecimal("5.01"),
                new BigDecimal("4.00"),
                new BigDecimal("4.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1.00 and 5.00");

        assertThatThrownBy(() -> company.recordNewReviewScore(
                new BigDecimal("4.00"),
                new BigDecimal("0.50"),
                new BigDecimal("4.00")
        ))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
