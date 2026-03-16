package com.example.pg.payment.command.application;

import com.example.pg.payment.domain.repository.CardCompanyPortRegistry;
import com.example.pg.payment.presentation.CardCompanyConnect;
import com.example.pg.payment.presentation.impl.CardCompanyApiTemplate;
import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.enumerate.CardCompanyStatus;
import com.example.pg.payment.infrastructure.persistence.CardCompanyRepository;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardCompanyPortRegistry")
class CardCompanyConnectRegistryTest {

    private static final String CODE = "SHINHAN";
    private static final String BASE_URL = "https://cardcompany.com/";

    @Mock
    private CardCompanyRepository cardCompanyRepository;
    @Mock
    private CardCompanyApiTemplate apiTemplate;

    private CardCompanyPortRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CardCompanyPortRegistry(cardCompanyRepository, apiTemplate);
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("ACTIVE 카드사 중 baseUrl 있는 것만 포트 등록")
        void success() {
            CardCompany company = new CardCompany(
                    "id-1", CODE, "신한", BASE_URL, CardCompanyStatus.ACTIVE, 0
            );
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of(company));

            registry.refresh();

            Optional<CardCompanyConnect> port = registry.getPort(CODE);
            assertThat(port).isPresent();
            verify(cardCompanyRepository).findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE);
        }

        @Test
        @DisplayName("baseUrl이 null이거나 blank인 카드사는 제외")
        void skipsNoBaseUrl() {
            CardCompany noUrl = new CardCompany("id-1", "A", "카드사A", null, CardCompanyStatus.ACTIVE, 0);
            CardCompany blankUrl = new CardCompany("id-2", "B", "카드사B", "  ", CardCompanyStatus.ACTIVE, 0);
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of(noUrl, blankUrl));

            registry.refresh();

            assertThat(registry.getPort("A")).isEmpty();
            assertThat(registry.getPort("B")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPort / getPortOrThrow")
    class GetPort {

        @Test
        @DisplayName("등록된 코드면 getPort Optional에 담아 반환")
        void getPortSuccess() {
            CardCompany company = new CardCompany("id", CODE, "신한", BASE_URL, CardCompanyStatus.ACTIVE, 0);
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of(company));
            registry.refresh();

            assertThat(registry.getPort(CODE)).isPresent();
        }

        @Test
        @DisplayName("없는 코드면 getPort empty")
        void getPortEmpty() {
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of());
            registry.refresh();

            assertThat(registry.getPort("UNKNOWN")).isEmpty();
        }

        @Test
        @DisplayName("getPortOrThrow 없으면 CARD_COMPANY_NOT_FOUND")
        void getPortOrThrowNotFound() {
            when(cardCompanyRepository.findByStatusOrderByDisplayOrderAsc(CardCompanyStatus.ACTIVE))
                    .thenReturn(List.of());
            registry.refresh();

            assertThatThrownBy(() -> registry.getPortOrThrow("UNKNOWN"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_COMPANY_NOT_FOUND);
        }
    }
}
