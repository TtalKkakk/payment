package com.example.pg.card.command.application;

import com.example.pg.card.command.application.port.BillingKeyPort;
import com.example.pg.card.domain.repository.AuthCodeRepository;
import com.example.pg.card.domain.repository.CardRegistrationTokenRepository;
import com.example.pg.merchant.domain.event.MerchantDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardDomainEventListenerTest {

    private static final String MERCHANT_ID = "merchant-1";
    private static final String NAME = "테스트가맹점";
    private static final String APPLICATION_ID = "app-1";

    @Mock
    private CardRegistrationTokenRepository tokenRepository;
    @Mock
    private AuthCodeRepository authCodeRepository;
    @Mock
    private BillingKeyPort billingKeyPort;

    @InjectMocks
    private CardDomainEventListener listener;

    @Test
    @DisplayName("onMerchantDeleted 시 토큰·AuthCode 삭제 및 빌링키 차단 요청 호출")
    void onMerchantDeleted_callsDeleteAndRevoke() {
        MerchantDeletedEvent event = MerchantDeletedEvent.from(MERCHANT_ID, NAME, APPLICATION_ID);

        listener.onMerchantDeleted(event);

        verify(tokenRepository).deleteByMerchantId(MERCHANT_ID);
        verify(authCodeRepository).deleteByMerchantId(MERCHANT_ID);
        verify(billingKeyPort).revokeBillingKeysByMerchantId(MERCHANT_ID);
    }
}
