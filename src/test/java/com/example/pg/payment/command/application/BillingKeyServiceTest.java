package com.example.pg.payment.command.application;

import com.example.pg.payment.domain.repository.CardCompanyPortRegistry;
import com.example.pg.payment.presentation.CardCompanyConnect;
import com.example.pg.payment.presentation.dto.BillingKeyTokenResponse;
import com.example.pg.payment.presentation.dto.RegistrationSessionResponse;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingKeyService")
class BillingKeyServiceTest {

    private static final String CARD_COMPANY_CODE = "SHINHAN";
    private static final String RETURN_URL = "https://merchant.com/callback";
    private static final String AUTH_CODE = "auth-123";

    @Mock
    private CardCompanyPortRegistry portRegistry;
    @Mock
    private CardCompanyConnect cardCompanyConnect;

    @InjectMocks
    private BillingKeyService billingKeyService;

    @Nested
    @DisplayName("createRegistrationSession")
    class CreateRegistrationSession {

        @Test
        @DisplayName("카드사 포트로 등록 세션 생성 위임")
        void success() {
            when(portRegistry.getPortOrThrow(CARD_COMPANY_CODE)).thenReturn(cardCompanyConnect);
            RegistrationSessionResponse dto = new RegistrationSessionResponse("token-1", "/register");
            when(cardCompanyConnect.createRegistrationSession(RETURN_URL)).thenReturn(dto);

            RegistrationSessionResponse result = billingKeyService.createRegistrationSession(CARD_COMPANY_CODE, RETURN_URL);

            assertThat(result.token()).isEqualTo("token-1");
            assertThat(result.registrationUrl()).isEqualTo("/register");
            verify(cardCompanyConnect).createRegistrationSession(RETURN_URL);
        }

        @Test
        @DisplayName("카드사 없으면 CARD_COMPANY_NOT_FOUND")
        void cardCompanyNotFound() {
            when(portRegistry.getPortOrThrow(CARD_COMPANY_CODE))
                    .thenThrow(new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, CARD_COMPANY_CODE));

            assertThatThrownBy(() -> billingKeyService.createRegistrationSession(CARD_COMPANY_CODE, RETURN_URL))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_COMPANY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("issueBillingKey")
    class IssueBillingKey {

        @Test
        @DisplayName("카드사 포트로 빌링키 발급 위임")
        void success() {
            when(portRegistry.getPortOrThrow(CARD_COMPANY_CODE)).thenReturn(cardCompanyConnect);
            BillingKeyTokenResponse dto = new BillingKeyTokenResponse("bk-token-1", "VISA", "****1234", "12/30");
            when(cardCompanyConnect.issueBillingKey(AUTH_CODE)).thenReturn(dto);

            BillingKeyTokenResponse result = billingKeyService.issueBillingKey(CARD_COMPANY_CODE, AUTH_CODE);

            assertThat(result.billingKeyToken()).isEqualTo("bk-token-1");
            verify(cardCompanyConnect).issueBillingKey(AUTH_CODE);
        }
    }
}
