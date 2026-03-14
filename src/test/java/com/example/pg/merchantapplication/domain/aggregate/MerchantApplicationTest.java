package com.example.pg.merchantapplication.domain.aggregate;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.domain.vo.ApplicationName;
import com.example.pg.merchantapplication.domain.vo.BusinessNumber;
import com.example.pg.merchantapplication.domain.vo.ContactEmail;
import com.example.pg.merchantapplication.domain.vo.ContactPhone;
import com.example.pg.merchantapplication.domain.vo.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantApplicationTest {

    private static final String NAME = "테스트가맹점";
    private static final String BUSINESS_NUMBER = "123-45-67890";
    private static final String PHONE = "010-1234-5678";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuv"; // 60자 이하

    private static MerchantApplication createPending() {
        return MerchantApplication.create(
                ApplicationName.of(NAME),
                BusinessNumber.of(BUSINESS_NUMBER),
                ContactPhone.of(PHONE),
                ContactEmail.of(EMAIL),
                PasswordHash.of(PASSWORD_HASH)
        );
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("VO로 생성 시 PENDING, id·version은 null (persist 시 @PrePersist에서 부여)")
        void success() {
            MerchantApplication app = createPending();

            assertThat(app.getId()).isNull();
            assertThat(app.getVersion()).isNull();
            assertThat(app.getName()).isEqualTo(NAME);
            assertThat(app.getBusinessNumber()).isEqualTo(BUSINESS_NUMBER);
            assertThat(app.getContactPhone()).isEqualTo(PHONE);
            assertThat(app.getContactEmail()).isEqualTo(EMAIL);
            assertThat(app.getPasswordHash()).isEqualTo(PASSWORD_HASH);
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.PENDING);
            assertThat(app.getRejectReason()).isNull();
            assertThat(app.getProcessedAt()).isNull();
            assertThat(app.getCreatedAt()).isNotNull();
            assertThat(app.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("PENDING이면 APPROVED로 변경, processedAt·updatedAt 설정")
        void success() {
            MerchantApplication app = createPending();

            app.approve();

            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.APPROVED);
            assertThat(app.getProcessedAt()).isNotNull();
            assertThat(app.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 처리된 상태면 APPLICATION_ALREADY_PROCESSED")
        void alreadyProcessed() {
            MerchantApplication app = createPending();
            app.approve();

            assertThatThrownBy(app::approve)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("REJECTED 상태에서 approve 시 예외")
        void rejectThenApprove() {
            MerchantApplication app = createPending();
            app.reject("사유");

            assertThatThrownBy(app::approve)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("사유와 함께 거절 시 REJECTED, rejectReason·processedAt 설정")
        void successWithReason() {
            MerchantApplication app = createPending();
            String reason = "서류 미비";

            app.reject(reason);

            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.REJECTED);
            assertThat(app.getRejectReason()).isEqualTo(reason);
            assertThat(app.getProcessedAt()).isNotNull();
            assertThat(app.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("사유 null 허용")
        void successWithNullReason() {
            MerchantApplication app = createPending();

            app.reject(null);

            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.REJECTED);
            assertThat(app.getRejectReason()).isNull();
        }

        @Test
        @DisplayName("거절 사유 500자 초과 시 REJECT_REASON_TOO_LONG")
        void reasonTooLong() {
            MerchantApplication app = createPending();
            String longReason = "a".repeat(MerchantApplication.REJECT_REASON_MAX_LENGTH + 1);

            assertThatThrownBy(() -> app.reject(longReason))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.REJECT_REASON_TOO_LONG);
        }

        @Test
        @DisplayName("이미 처리된 상태면 APPLICATION_ALREADY_PROCESSED")
        void alreadyProcessed() {
            MerchantApplication app = createPending();
            app.approve();

            assertThatThrownBy(() -> app.reject("사유"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("isPending")
    class IsPending {

        @Test
        @DisplayName("PENDING일 때 true")
        void whenPending() {
            MerchantApplication app = createPending();
            assertThat(app.isPending()).isTrue();
        }

        @Test
        @DisplayName("APPROVED일 때 false")
        void whenApproved() {
            MerchantApplication app = createPending();
            app.approve();
            assertThat(app.isPending()).isFalse();
        }

        @Test
        @DisplayName("REJECTED일 때 false")
        void whenRejected() {
            MerchantApplication app = createPending();
            app.reject("사유");
            assertThat(app.isPending()).isFalse();
        }

        @Test
        @DisplayName("CANCELLED일 때 false")
        void whenCancelled() {
            MerchantApplication app = createPending();
            app.cancel();
            assertThat(app.isPending()).isFalse();
        }

        @Test
        @DisplayName("SUBSCRIPTION_ENDED일 때 false")
        void whenSubscriptionEnded() {
            MerchantApplication app = createPending();
            app.approve();
            app.subscriptionEnded();
            assertThat(app.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("PENDING이면 CANCELLED로 변경")
        void success() {
            MerchantApplication app = createPending();
            app.cancel();
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.CANCELLED);
        }

        @Test
        @DisplayName("PENDING이 아니면 APPLICATION_ALREADY_PROCESSED")
        void notPending() {
            MerchantApplication app = createPending();
            app.approve();
            assertThatThrownBy(app::cancel)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("subscriptionEnded")
    class SubscriptionEnded {

        @Test
        @DisplayName("APPROVED이면 SUBSCRIPTION_ENDED로 변경")
        void success() {
            MerchantApplication app = createPending();
            app.approve();
            app.subscriptionEnded();
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.SUBSCRIPTION_ENDED);
        }

        @Test
        @DisplayName("APPROVED가 아니면 APPLICATION_ALREADY_PROCESSED")
        void notApproved() {
            MerchantApplication app = createPending();
            assertThatThrownBy(app::subscriptionEnded)
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("reapply")
    class Reapply {

        @Test
        @DisplayName("CANCELLED이면 PENDING으로 갱신")
        void fromCancelled() {
            MerchantApplication app = createPending();
            app.cancel();
            String id = app.getId();
            String businessNumber = app.getBusinessNumber();

            app.reapply(
                    ApplicationName.of("새이름"),
                    ContactPhone.of("010-9999-8888"),
                    ContactEmail.of("new@example.com"),
                    PasswordHash.of("newHash")
            );

            assertThat(app.getId()).isEqualTo(id);
            assertThat(app.getBusinessNumber()).isEqualTo(businessNumber);
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.PENDING);
            assertThat(app.getName()).isEqualTo("새이름");
            assertThat(app.getContactPhone()).isEqualTo("010-9999-8888");
            assertThat(app.getContactEmail()).isEqualTo("new@example.com");
            assertThat(app.getPasswordHash()).isEqualTo("newHash");
            assertThat(app.getRejectReason()).isNull();
            assertThat(app.getProcessedAt()).isNull();
        }

        @Test
        @DisplayName("SUBSCRIPTION_ENDED이면 PENDING으로 갱신")
        void fromSubscriptionEnded() {
            MerchantApplication app = createPending();
            app.approve();
            app.subscriptionEnded();
            app.reapply(
                    ApplicationName.of("재신청"),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.PENDING);
        }

        @Test
        @DisplayName("REJECTED이면 PENDING으로 갱신")
        void fromRejected() {
            MerchantApplication app = createPending();
            app.reject("사유");
            app.reapply(
                    ApplicationName.of("재신청"),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.PENDING);
            assertThat(app.getRejectReason()).isNull();
        }

        @Test
        @DisplayName("PENDING/APPROVED이면 APPLICATION_ALREADY_PROCESSED")
        void notReapplyable() {
            MerchantApplication app = createPending();
            assertThatThrownBy(() -> app.reapply(
                    ApplicationName.of("x"),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);

            app.approve();
            assertThatThrownBy(() -> app.reapply(
                    ApplicationName.of("x"),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
    }
}
