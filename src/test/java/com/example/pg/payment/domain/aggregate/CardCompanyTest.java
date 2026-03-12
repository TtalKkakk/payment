package com.example.pg.payment.domain.aggregate;

import com.example.pg.payment.domain.enumerate.CardCompanyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardCompany 애그리거트")
class CardCompanyTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("code, name, baseUrl으로 생성 시 ACTIVE, displayOrder 0")
        void success() {
            CardCompany company = CardCompany.create("SHINHAN", "신한카드", "https://shinhan.com/");

            assertThat(company.getId()).isNotBlank();
            assertThat(company.getCode()).isEqualTo("SHINHAN");
            assertThat(company.getName()).isEqualTo("신한카드");
            assertThat(company.getBaseUrl()).isEqualTo("https://shinhan.com/");
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
            CardCompany active = CardCompany.create("A", "카드사A", "https://a.com/");
            assertThat(active.isActive()).isTrue();

            CardCompany inactive = new CardCompany(
                    "id2", "C", "카드사C", "https://c.com/", CardCompanyStatus.INACTIVE, 0
            );
            assertThat(inactive.isActive()).isFalse();
        }
    }
}
