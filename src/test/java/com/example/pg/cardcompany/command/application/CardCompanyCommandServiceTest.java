package com.example.pg.cardcompany.command.application;

import com.example.pg.cardcompany.application.CardCompanyPortRegistry;
import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import com.example.pg.cardcompany.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardCompanyCommandService")
class CardCompanyCommandServiceTest {

    private static final String CODE = "CARD_A";
    private static final String NAME = "테스트카드사";
    private static final String BASE_URL = "https://card-a.example.com/";

    @Mock
    private CardCompanyRepository cardCompanyRepository;
    @Mock
    private CardCompanyPortRegistry portRegistry;

    @InjectMocks
    private CardCompanyCommandService sut;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("동일 code 없으면 생성 후 save, refresh 후 반환")
        void success() {
            when(cardCompanyRepository.findByCode(CODE)).thenReturn(Optional.empty());

            CardCompany result = sut.register(CODE, NAME, BASE_URL);

            assertThat(result.getCode()).isEqualTo(CODE);
            assertThat(result.getName()).isEqualTo(NAME);
            assertThat(result.getBaseUrl()).isEqualTo(BASE_URL);
            assertThat(result.getStatus()).isEqualTo(CardCompanyStatus.ACTIVE);

            ArgumentCaptor<CardCompany> captor = ArgumentCaptor.forClass(CardCompany.class);
            verify(cardCompanyRepository).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo(CODE);
            verify(portRegistry).refresh();
        }

        @Test
        @DisplayName("이미 동일 code가 있으면 CARD_COMPANY_DUPLICATE_CODE")
        void duplicateCode() {
            CardCompany existing = CardCompany.create(CODE, NAME, BASE_URL);
            when(cardCompanyRepository.findByCode(CODE)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> sut.register(CODE, "다른이름", "https://other.com"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_COMPANY_DUPLICATE_CODE);

            verify(cardCompanyRepository, never()).save(any());
            verify(portRegistry, never()).refresh();
        }
    }
}
