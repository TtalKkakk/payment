package com.example.pg.cardcompany.application;

import com.example.pg.cardcompany.domain.port.CardCompanyBillingKeyPort;
import com.example.pg.cardcompany.domain.result.BillingKeyTokenResult;
import com.example.pg.cardcompany.domain.result.RegistrationSessionResult;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
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
@DisplayName("CardCompanyBillingKeyService")
class CardCompanyBillingKeyServiceTest {

    private static final String CARD_COMPANY_CODE = "CARD_A";
    private static final String RETURN_URL = "https://pg.example.com/card/callback/register?token=xxx";
    private static final String AUTH_CODE = "auth-123";
    private static final String BILLING_KEY_TOKEN = "bk_abc";

    @Mock
    private CardCompanyPortRegistry portRegistry;
    @Mock
    private CardCompanyBillingKeyPort port;

    @InjectMocks
    private CardCompanyBillingKeyService sut;

    @Nested
    @DisplayName("createRegistrationSession")
    class CreateRegistrationSession {

        @Test
        @DisplayName("포트에서 반환한 결과 그대로 반환")
        void success() {
            RegistrationSessionResult expected = new RegistrationSessionResult("token-1", "/register/form");
            when(portRegistry.getPortOrThrow(CARD_COMPANY_CODE)).thenReturn(port);
            when(port.createRegistrationSession(RETURN_URL)).thenReturn(expected);

            RegistrationSessionResult result = sut.createRegistrationSession(CARD_COMPANY_CODE, RETURN_URL);

            assertThat(result).isEqualTo(expected);
            assertThat(result.token()).isEqualTo("token-1");
            assertThat(result.registrationUrl()).isEqualTo("/register/form");
            verify(port).createRegistrationSession(RETURN_URL);
        }

        @Test
        @DisplayName("카드사가 레지스트리에 없으면 CARD_COMPANY_NOT_FOUND")
        void portNotFound() {
            when(portRegistry.getPortOrThrow("INVALID"))
                    .thenThrow(new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, "INVALID"));

            assertThatThrownBy(() -> sut.createRegistrationSession("INVALID", RETURN_URL))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_COMPANY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("issueBillingKey")
    class IssueBillingKey {

        @Test
        @DisplayName("포트에서 반환한 billingKeyToken 그대로 반환")
        void success() {
            BillingKeyTokenResult expected = new BillingKeyTokenResult(BILLING_KEY_TOKEN);
            when(portRegistry.getPortOrThrow(CARD_COMPANY_CODE)).thenReturn(port);
            when(port.issueBillingKey(AUTH_CODE)).thenReturn(expected);

            BillingKeyTokenResult result = sut.issueBillingKey(CARD_COMPANY_CODE, AUTH_CODE);

            assertThat(result.billingKeyToken()).isEqualTo(BILLING_KEY_TOKEN);
            verify(port).issueBillingKey(AUTH_CODE);
        }

        @Test
        @DisplayName("카드사가 레지스트리에 없으면 CARD_COMPANY_NOT_FOUND")
        void portNotFound() {
            when(portRegistry.getPortOrThrow("INVALID"))
                    .thenThrow(new BusinessException(ErrorCode.CARD_COMPANY_NOT_FOUND, "INVALID"));

            assertThatThrownBy(() -> sut.issueBillingKey("INVALID", AUTH_CODE))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.CARD_COMPANY_NOT_FOUND);
        }
    }
}
