package com.exedev.cic.search;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanySearchCriteriaTest {

    @Test
    void normalizesBlankStringsAndTokenizesQuery() {
        CompanySearchCriteria criteria = new CompanySearchCriteria(
                "   ",
                new BigDecimal("4.20"),
                true,
                " Austin ",
                10,
                "  Cloud   Search  "
        );

        assertThat(criteria.industry()).isNull();
        assertThat(criteria.headquarters()).isEqualTo("Austin");
        assertThat(criteria.queryTokens()).containsExactly("cloud", "search");
    }

    @Test
    void escapesLikeWildcardsLiterally() {
        assertThat(CompanySearchCriteria.escapeLikePattern("100%_real\\co"))
                .isEqualTo("100\\%\\_real\\\\co");
    }

    @Test
    void clampsPageSizeAndValidatesSortAllowlist() {
        Pageable oversized = PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "overallRating"));
        Pageable sanitized = CompanySearchCriteria.sanitizePageable(oversized, 50);

        assertThat(sanitized.getPageSize()).isEqualTo(50);
        assertThat(sanitized.getSort()).isEqualTo(oversized.getSort());

        Pageable invalidSort = PageRequest.of(0, 20, Sort.by("version"));
        assertThatThrownBy(() -> CompanySearchCriteria.sanitizePageable(invalidSort, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort field");
    }

    @Test
    void rejectsInvalidNumericBounds() {
        assertThatThrownBy(() -> new CompanySearchCriteria(
                null, new BigDecimal("5.50"), null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CompanySearchCriteria(
                null, null, null, null, -1, null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
