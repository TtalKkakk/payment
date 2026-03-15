package com.example.pg.merchant.domain.aggregate;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.domain.enumerate.MerchantStatus;
import com.example.pg.merchant.domain.vo.MerchantName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Merchant 애그리거트")
class MerchantTest {

    private static final String ID = "merchant-1";
    private static final String API_KEY = "pk_merchant_abc123";
    private static final String API_SECRET = "sk_merchant_xyz789";
    private static final String NAME = "테스트가맹점";
    private static final String APPLICATION_ID = "app-1";

    private static Merchant activeMerchant() {
        return new Merchant(ID, API_KEY, API_SECRET, NAME, APPLICATION_ID, MerchantStatus.ACTIVE);
    }

    private static Merchant suspendedMerchant() {
        return new Merchant(ID, API_KEY, API_SECRET, NAME, APPLICATION_ID, MerchantStatus.SUSPENDED);
    }

    private static Merchant withdrawnMerchant() {
        return new Merchant(ID, API_KEY, API_SECRET, NAME, APPLICATION_ID, MerchantStatus.WITHDRAWN);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("이름과 applicationId로 생성 시 ACTIVE, apiKey/apiSecret 자동 생성")
        void success() {
            Merchant merchant = Merchant.create(MerchantName.of(NAME), APPLICATION_ID);

            assertThat(merchant.getId()).isNotBlank();
            assertThat(merchant.getName()).isEqualTo(NAME);
            assertThat(merchant.getApplicationId()).isEqualTo(APPLICATION_ID);
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
            assertThat(merchant.getApiKey()).startsWith("pk_merchant_");
            assertThat(merchant.getApiSecret()).startsWith("sk_merchant_");
        }
    }

    @Nested
    @DisplayName("suspend")
    class Suspend {

        @Test
        @DisplayName("ACTIVE면 SUSPENDED로 전이")
        void success() {
            Merchant merchant = activeMerchant();
            merchant.suspend();
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);
        }

        @Test
        @DisplayName("이미 SUSPENDED여도 SUSPENDED 유지")
        void whenSuspended() {
            Merchant merchant = suspendedMerchant();
            merchant.suspend();
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);
        }

        @Test
        @DisplayName("WITHDRAWN이면 MERCHANT_CANNOT_SUSPEND")
        void whenWithdrawn() {
            Merchant merchant = withdrawnMerchant();
            assertThatThrownBy(merchant::suspend)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_SUSPEND);
        }
    }

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("SUSPENDED면 ACTIVE로 전이")
        void success() {
            Merchant merchant = suspendedMerchant();
            merchant.activate();
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE면 MERCHANT_CANNOT_ACTIVATE")
        void whenActive() {
            Merchant merchant = activeMerchant();
            assertThatThrownBy(merchant::activate)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_ACTIVATE);
        }

        @Test
        @DisplayName("WITHDRAWN이면 MERCHANT_CANNOT_ACTIVATE")
        void whenWithdrawn() {
            Merchant merchant = withdrawnMerchant();
            assertThatThrownBy(merchant::activate)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_ACTIVATE);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("어떤 상태에서든 WITHDRAWN으로 전이")
        void success() {
            Merchant merchant = activeMerchant();
            merchant.withdraw();
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.WITHDRAWN);
        }
    }

    @Nested
    @DisplayName("withdrawFromSuspended")
    class WithdrawFromSuspended {

        @Test
        @DisplayName("SUSPENDED면 WITHDRAWN으로 전이")
        void success() {
            Merchant merchant = suspendedMerchant();
            merchant.withdrawFromSuspended();
            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("ACTIVE면 MERCHANT_CANNOT_WITHDRAW")
        void whenActive() {
            Merchant merchant = activeMerchant();
            assertThatThrownBy(merchant::withdrawFromSuspended)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_WITHDRAW);
        }

        @Test
        @DisplayName("WITHDRAWN이면 MERCHANT_CANNOT_WITHDRAW")
        void whenWithdrawn() {
            Merchant merchant = withdrawnMerchant();
            assertThatThrownBy(merchant::withdrawFromSuspended)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_WITHDRAW);
        }
    }

    @Nested
    @DisplayName("regenerateSecret")
    class RegenerateSecret {

        @Test
        @DisplayName("apiSecret만 변경되고 apiKey는 유지")
        void success() {
            Merchant merchant = activeMerchant();
            String beforeKey = merchant.getApiKey();
            String beforeSecret = merchant.getApiSecret();

            merchant.regenerateSecret();

            assertThat(merchant.getApiKey()).isEqualTo(beforeKey);
            assertThat(merchant.getApiSecret()).isNotEqualTo(beforeSecret);
            assertThat(merchant.getApiSecret()).startsWith("sk_merchant_");
        }
    }

    @Nested
    @DisplayName("reactivate")
    class Reactivate {

        @Test
        @DisplayName("WITHDRAWN이면 ACTIVE로 전이하고 name 동기화, apiKey·apiSecret 재발급")
        void success() {
            Merchant merchant = withdrawnMerchant();
            String oldKey = merchant.getApiKey();
            String oldSecret = merchant.getApiSecret();
            String newName = "재승인가맹점";

            merchant.reactivate(MerchantName.of(newName));

            assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
            assertThat(merchant.getName()).isEqualTo(newName);
            assertThat(merchant.getApiKey()).isNotEqualTo(oldKey);
            assertThat(merchant.getApiSecret()).isNotEqualTo(oldSecret);
            assertThat(merchant.getApiKey()).startsWith("pk_merchant_");
            assertThat(merchant.getApiSecret()).startsWith("sk_merchant_");
        }

        @Test
        @DisplayName("ACTIVE면 MERCHANT_CANNOT_REACTIVATE")
        void whenActive() {
            Merchant merchant = activeMerchant();
            assertThatThrownBy(() -> merchant.reactivate(MerchantName.of(NAME)))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_REACTIVATE);
        }

        @Test
        @DisplayName("SUSPENDED면 MERCHANT_CANNOT_REACTIVATE")
        void whenSuspended() {
            Merchant merchant = suspendedMerchant();
            assertThatThrownBy(() -> merchant.reactivate(MerchantName.of(NAME)))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_CANNOT_REACTIVATE);
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActive {

        @Test
        @DisplayName("ACTIVE일 때만 true")
        void onlyWhenActive() {
            assertThat(activeMerchant().isActive()).isTrue();
            assertThat(suspendedMerchant().isActive()).isFalse();
            assertThat(withdrawnMerchant().isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("matchesSecret")
    class MatchesSecret {

        @Test
        @DisplayName("동일 secret이면 true")
        void match() {
            assertThat(activeMerchant().matchesSecret(API_SECRET)).isTrue();
        }

        @Test
        @DisplayName("다른 secret이면 false")
        void noMatch() {
            assertThat(activeMerchant().matchesSecret("other")).isFalse();
        }
    }
}
