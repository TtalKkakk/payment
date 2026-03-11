package com.example.pg.cardcompany.query.application;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import com.example.pg.cardcompany.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
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
@DisplayName("CardCompanyQueryService")
class CardCompanyQueryServiceTest {

    private static final String CODE = "CARD_A";
    private static final CardCompany CARD_A = CardCompany.create(CODE, "카드사A", "https://a.example.com/");

    @Mock
    private CardCompanyRepository cardCompanyRepository;

    @InjectMocks
    private CardCompanyQueryService sut;

    @Nested
    @DisplayName("findByCode")
    class FindByCode {

        @Test
        @DisplayName("있으면 해당 엔티티 반환")
        void success() {
            when(cardCompanyRepository.findByCode(CODE)).thenReturn(Optional.of(CARD_A));

            CardCompany result = sut.findByCode(CODE);

            assertThat(result).isEqualTo(CARD_A);
            assertThat(result.getCode()).isEqualTo(CODE);
            verify(cardCompanyRepository).findByCode(CODE);
        }

        @Test
        @DisplayName("없으면 CARD_COMPANY_NOT_FOUND")
        void notFound() {
            when(cardCompanyRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.findByCode("INVALID"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_COMPANY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findByCodeOptional")
    class FindByCodeOptional {

        @Test
        @DisplayName("있으면 Optional.of 반환")
        void present() {
            when(cardCompanyRepository.findByCode(CODE)).thenReturn(Optional.of(CARD_A));

            assertThat(sut.findByCodeOptional(CODE)).contains(CARD_A);
        }

        @Test
        @DisplayName("없으면 Optional.empty 반환")
        void empty() {
            when(cardCompanyRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            assertThat(sut.findByCodeOptional("INVALID")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("ACTIVE 목록 반환")
        void success() {
            List<CardCompany> activeList = List.of(CARD_A);
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(activeList);

            List<CardCompany> result = sut.findAllActive();

            assertThat(result).isEqualTo(activeList);
            verify(cardCompanyRepository).findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
        }
    }
}
