package com.example.pg.cardcompany.domain.aggregate;

import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardCompany 애그리거트")
class CardCompanyTest {

    private static final String ID = "id-1";
    private static final String CODE = "CARD_A";
    private static final String NAME = "테스트카드사";
    private static final String BASE_URL = "https://card-a.example.com/";

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("code, name, baseUrl로 생성 시 ACTIVE, displayOrder 0, id UUID 형식")
        void success() {
            CardCompany company = CardCompany.create(CODE, NAME, BASE_URL);

            assertThat(company.getId()).isNotBlank();
            assertThat(company.getId()).hasSize(36);
            assertThat(company.getCode()).isEqualTo(CODE);
            assertThat(company.getName()).isEqualTo(NAME);
            assertThat(company.getBaseUrl()).isEqualTo(BASE_URL);
            assertThat(company.getStatus()).isEqualTo(CardCompanyStatus.ACTIVE);
            assertThat(company.getDisplayOrder()).isZero();
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActive {

        @Test
        @DisplayName("ACTIVE일 때만 true")
        void onlyWhenActive() {
            CardCompany active = new CardCompany(ID, CODE, NAME, BASE_URL, CardCompanyStatus.ACTIVE, 0);
            CardCompany inactive = new CardCompany(ID, CODE, NAME, BASE_URL, CardCompanyStatus.INACTIVE, 0);

            assertThat(active.isActive()).isTrue();
            assertThat(inactive.isActive()).isFalse();
        }
    }
}
