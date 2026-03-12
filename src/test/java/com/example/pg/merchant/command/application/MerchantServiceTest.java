package com.example.pg.merchant.command.application;

import com.example.pg.merchant.command.application.dto.RegenerateSecretResult;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.enumerate.MerchantStatus;
import com.example.pg.merchant.domain.event.MerchantDeletedEvent;
import com.example.pg.merchant.domain.event.MerchantSecretRegeneratedEvent;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.merchantapplication.command.application.port.MerchantApplicationPort;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    private static final String MERCHANT_ID = "merchant-1";
    private static final String NAME = "테스트가맹점";
    private static final String APPLICATION_ID = "app-1";
    private static final String API_KEY = "pk_merchant_abc";
    private static final String API_SECRET = "sk_merchant_xyz";

    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private MerchantApplicationPort merchantApplicationPort;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private MerchantService merchantService;

    private static Merchant activeMerchant() {
        Merchant m = new Merchant(MERCHANT_ID, API_KEY, API_SECRET, NAME, APPLICATION_ID, MerchantStatus.ACTIVE);
        return m;
    }

    private static Merchant suspendedMerchant() {
        return new Merchant(MERCHANT_ID, API_KEY, API_SECRET, NAME, APPLICATION_ID, MerchantStatus.SUSPENDED);
    }

    private static Merchant withdrawnMerchant() {
        return new Merchant(MERCHANT_ID, API_KEY, API_SECRET, NAME, null, MerchantStatus.WITHDRAWN);
    }

    @Nested
    @DisplayName("regenerateSecret")
    class RegenerateSecret {

        @Test
        @DisplayName("성공 시 secret 갱신 후 이벤트 발행, apiKey·apiSecret 반환")
        void success() {
            Merchant merchant = activeMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            RegenerateSecretResult result = merchantService.regenerateSecret(MERCHANT_ID);

            assertThat(result.apiKey()).isEqualTo(API_KEY);
            assertThat(result.apiSecret()).isNotEqualTo(API_SECRET);
            assertThat(result.apiSecret()).startsWith("sk_merchant_");
            ArgumentCaptor<MerchantSecretRegeneratedEvent> captor = ArgumentCaptor.forClass(MerchantSecretRegeneratedEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().merchantId()).isEqualTo(MERCHANT_ID);
        }

        @Test
        @DisplayName("가맹점 없으면 MERCHANT_NOT_FOUND")
        void notFound() {
            when(merchantRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantService.regenerateSecret("invalid"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_NOT_FOUND);
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("deleteMerchant")
    class DeleteMerchant {

        @Test
        @DisplayName("성공 시 withdraw, save, markSubscriptionEnded, 이벤트 발행")
        void success() {
            Merchant merchant = activeMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            merchantService.deleteMerchant(MERCHANT_ID);

            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.WITHDRAWN);
            verify(merchantRepository).save(merchant);
            verify(merchantApplicationPort).markSubscriptionEnded(APPLICATION_ID);
            ArgumentCaptor<MerchantDeletedEvent> captor = ArgumentCaptor.forClass(MerchantDeletedEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().merchantId()).isEqualTo(MERCHANT_ID);
            assertThat(captor.getValue().name()).isEqualTo(NAME);
            assertThat(captor.getValue().applicationId()).isEqualTo(APPLICATION_ID);
        }

        @Test
        @DisplayName("applicationId가 null이어도 markSubscriptionEnded 호출 (포트에서 무시)")
        void whenApplicationIdNull() {
            Merchant merchant = new Merchant(MERCHANT_ID, API_KEY, API_SECRET, NAME, null, MerchantStatus.ACTIVE);
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            merchantService.deleteMerchant(MERCHANT_ID);

            verify(merchantApplicationPort).markSubscriptionEnded(null);
        }

        @Test
        @DisplayName("가맹점 없으면 MERCHANT_NOT_FOUND")
        void notFound() {
            when(merchantRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantService.deleteMerchant("invalid"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_NOT_FOUND);
            verify(merchantApplicationPort, never()).markSubscriptionEnded(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("suspendMerchant")
    class SuspendMerchant {

        @Test
        @DisplayName("ACTIVE면 SUSPENDED로 변경 후 save")
        void success() {
            Merchant merchant = activeMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            merchantService.suspendMerchant(MERCHANT_ID);

            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);
            verify(merchantRepository).save(merchant);
        }

        @Test
        @DisplayName("가맹점 없으면 MERCHANT_NOT_FOUND")
        void notFound() {
            when(merchantRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantService.suspendMerchant("invalid"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_NOT_FOUND);
        }

        @Test
        @DisplayName("WITHDRAWN이면 MERCHANT_CANNOT_SUSPEND")
        void whenWithdrawn() {
            Merchant merchant = withdrawnMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            assertThatThrownBy(() -> merchantService.suspendMerchant(MERCHANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_SUSPEND);
            verify(merchantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("activateMerchant")
    class ActivateMerchant {

        @Test
        @DisplayName("SUSPENDED면 ACTIVE로 변경 후 save")
        void success() {
            Merchant merchant = suspendedMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            merchantService.activateMerchant(MERCHANT_ID);

            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
            verify(merchantRepository).save(merchant);
        }

        @Test
        @DisplayName("가맹점 없으면 MERCHANT_NOT_FOUND")
        void notFound() {
            when(merchantRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantService.activateMerchant("invalid"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_NOT_FOUND);
        }

        @Test
        @DisplayName("ACTIVE면 MERCHANT_CANNOT_ACTIVATE")
        void whenActive() {
            Merchant merchant = activeMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            assertThatThrownBy(() -> merchantService.activateMerchant(MERCHANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_ACTIVATE);
            verify(merchantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("withdrawMerchantFromSuspended")
    class WithdrawMerchantFromSuspended {

        @Test
        @DisplayName("SUSPENDED면 WITHDRAWN, save, markSubscriptionEnded, 이벤트 발행")
        void success() {
            Merchant merchant = suspendedMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            merchantService.withdrawMerchantFromSuspended(MERCHANT_ID);

            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.WITHDRAWN);
            verify(merchantRepository).save(merchant);
            verify(merchantApplicationPort).markSubscriptionEnded(APPLICATION_ID);
            verify(applicationEventPublisher).publishEvent(any(MerchantDeletedEvent.class));
        }

        @Test
        @DisplayName("가맹점 없으면 MERCHANT_NOT_FOUND")
        void notFound() {
            when(merchantRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantService.withdrawMerchantFromSuspended("invalid"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_NOT_FOUND);
            verify(merchantApplicationPort, never()).markSubscriptionEnded(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("ACTIVE면 MERCHANT_CANNOT_WITHDRAW")
        void whenActive() {
            Merchant merchant = activeMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            assertThatThrownBy(() -> merchantService.withdrawMerchantFromSuspended(MERCHANT_ID))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_WITHDRAW);
            verify(merchantRepository, never()).save(any());
            verify(merchantApplicationPort, never()).markSubscriptionEnded(any());
        }
    }
}
