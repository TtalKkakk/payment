package com.example.pg.payment.query.application;

import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.enumerate.CardCompanyStatus;
import com.example.pg.payment.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingKeyQueryService")
class BillingKeyQueryServiceTest {

    private static final String CODE = "SHINHAN";

    @Mock
    private CardCompanyRepository cardCompanyRepository;

    @InjectMocks
    private BillingKeyQueryService billingKeyQueryService;

    @Nested
    @DisplayName("findByCode")
    class FindByCode {

        @Test
        @DisplayName("있으면 카드사 반환")
        void success() {
            CardCompany company = CardCompany.create(CODE, "신한카드", "https://shinhan.com/");
            when(cardCompanyRepository.findByCode(CODE)).thenReturn(Optional.of(company));

            CardCompany result = billingKeyQueryService.findByCode(CODE);

            assertThat(result.getCode()).isEqualTo(CODE);
            assertThat(result.getName()).isEqualTo("신한카드");
        }

        @Test
        @DisplayName("없으면 CARD_COMPANY_NOT_FOUND")
        void notFound() {
            when(cardCompanyRepository.findByCode(CODE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> billingKeyQueryService.findByCode(CODE))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_COMPANY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findByCodeOptional")
    class FindByCodeOptional {

        @Test
        @DisplayName("있으면 Optional에 담아 반환")
        void success() {
            CardCompany company = CardCompany.create(CODE, "신한", "https://a.com/");
            when(cardCompanyRepository.findByCode(CODE)).thenReturn(Optional.of(company));

            Optional<CardCompany> result = billingKeyQueryService.findByCodeOptional(CODE);

            assertThat(result).hasValueSatisfying(c -> assertThat(c.getCode()).isEqualTo(CODE));
        }

        @Test
        @DisplayName("없으면 empty")
        void empty() {
            when(cardCompanyRepository.findByCode(CODE)).thenReturn(Optional.empty());
            assertThat(billingKeyQueryService.findByCodeOptional(CODE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("ACTIVE 카드사 목록 조회")
        void success() {
            List<CardCompany> companies = List.of(
                    CardCompany.create("A", "카드사A", "https://a.com/"),
                    CardCompany.create("B", "카드사B", "https://b.com/")
            );
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(companies);

            List<CardCompany> result = billingKeyQueryService.findAllActive();

            assertThat(result).hasSize(2);
            verify(cardCompanyRepository).findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
        }
    }
}
