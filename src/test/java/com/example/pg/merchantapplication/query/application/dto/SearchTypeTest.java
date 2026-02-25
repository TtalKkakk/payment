package com.example.pg.merchantapplication.query.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SearchType")
class SearchTypeTest {

    @Test
    @DisplayName("status·businessNumber 둘 다 없으면 ALL")
    void all() {
        assertThat(SearchType.from(null, null)).isEqualTo(SearchType.ALL);
        assertThat(SearchType.from("", null)).isEqualTo(SearchType.ALL);
        assertThat(SearchType.from("  ", "")).isEqualTo(SearchType.ALL);
    }

    @Test
    @DisplayName("status만 유효하면 BY_STATUS")
    void byStatus() {
        assertThat(SearchType.from("PENDING", null)).isEqualTo(SearchType.BY_STATUS);
        assertThat(SearchType.from("APPROVED", "")).isEqualTo(SearchType.BY_STATUS);
        assertThat(SearchType.from("REJECTED", "  ")).isEqualTo(SearchType.BY_STATUS);
    }

    @Test
    @DisplayName("businessNumber만 있으면 BY_BUSINESS_NUMBER")
    void byBusinessNumber() {
        assertThat(SearchType.from(null, "123")).isEqualTo(SearchType.BY_BUSINESS_NUMBER);
        assertThat(SearchType.from("", "123-45")).isEqualTo(SearchType.BY_BUSINESS_NUMBER);
        assertThat(SearchType.from("INVALID", "123")).isEqualTo(SearchType.BY_BUSINESS_NUMBER);
    }

    @Test
    @DisplayName("status·businessNumber 둘 다 있으면 BY_BOTH")
    void byBoth() {
        assertThat(SearchType.from("PENDING", "123")).isEqualTo(SearchType.BY_BOTH);
        assertThat(SearchType.from("APPROVED", "123-45-67890")).isEqualTo(SearchType.BY_BOTH);
    }
}
