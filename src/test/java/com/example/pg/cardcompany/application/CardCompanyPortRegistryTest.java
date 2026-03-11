package com.example.pg.cardcompany.application;

import com.example.pg.cardcompany.domain.aggregate.CardCompany;
import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import com.example.pg.cardcompany.domain.port.CardCompanyBillingKeyPort;
import com.example.pg.cardcompany.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardCompanyPortRegistry")
class CardCompanyPortRegistryTest {

    private static final String CODE_A = "CARD_A";
    private static final String CODE_B = "CARD_B";
    private static final String BASE_URL_A = "https://card-a.example.com/";

    @Mock
    private CardCompanyRepository cardCompanyRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private CardCompanyPortRegistry sut;

    @Nested
    @DisplayName("refresh / getPort / getPortOrThrow")
    class RefreshAndGetPort {

        @Test
        @DisplayName("ACTIVE이고 baseUrl 있으면 해당 코드로 포트 등록됨")
        void registersActiveWithBaseUrl() {
            CardCompany company = new CardCompany("id-1", CODE_A, "카드사A", BASE_URL_A, CardCompanyStatus.ACTIVE, 0);
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of(company));

            sut = new CardCompanyPortRegistry(cardCompanyRepository, restTemplate, objectMapper);
            sut.refresh();

            Optional<CardCompanyBillingKeyPort> port = sut.getPort(CODE_A);
            assertThat(port).isPresent();
            CardCompanyBillingKeyPort actual = sut.getPortOrThrow(CODE_A);
            assertThat(actual).isNotNull();
            verify(cardCompanyRepository).findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
        }

        @Test
        @DisplayName("baseUrl이 null이거나 blank면 해당 카드사는 등록 안 함")
        void skipsWhenBaseUrlBlank() {
            CardCompany noUrl = new CardCompany("id-1", CODE_A, "카드사A", null, CardCompanyStatus.ACTIVE, 0);
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of(noUrl));

            sut = new CardCompanyPortRegistry(cardCompanyRepository, restTemplate, objectMapper);
            sut.refresh();

            assertThat(sut.getPort(CODE_A)).isEmpty();
            verify(cardCompanyRepository).findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
        }

        @Test
        @DisplayName("getPortOrThrow 시 등록된 코드 없으면 CARD_COMPANY_NOT_FOUND")
        void getPortOrThrowNotFound() {
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of());

            sut = new CardCompanyPortRegistry(cardCompanyRepository, restTemplate, objectMapper);
            sut.refresh();

            assertThatThrownBy(() -> sut.getPortOrThrow("UNKNOWN"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_COMPANY_NOT_FOUND);
        }

        @Test
        @DisplayName("refresh 시 기존 맵 clear 후 재등록")
        void refreshClearsAndReregisters() {
            CardCompany company = new CardCompany("id-1", CODE_A, "A", BASE_URL_A, CardCompanyStatus.ACTIVE, 0);
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of(company))
                    .thenReturn(List.of()); // second refresh: no companies

            sut = new CardCompanyPortRegistry(cardCompanyRepository, restTemplate, objectMapper);
            sut.refresh();
            assertThat(sut.getPort(CODE_A)).isPresent();

            sut.refresh();
            assertThat(sut.getPort(CODE_A)).isEmpty();
        }
    }
}
