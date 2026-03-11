package com.example.pg.cardcompany.query.application.dto;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardCompanyListItem")
class CardCompanyListItemTest {

    @Test
    @DisplayName("from(CardCompany) 시 code, name 매핑")
    void from() {
        CardCompany company = new CardCompany(
                "id-1",
                "CARD_A",
                "테스트카드사",
                "https://example.com/",
                CardCompanyStatus.ACTIVE,
                0
        );

        CardCompanyListItem item = CardCompanyListItem.from(company);

        assertThat(item.code()).isEqualTo("CARD_A");
        assertThat(item.name()).isEqualTo("테스트카드사");
    }
}
