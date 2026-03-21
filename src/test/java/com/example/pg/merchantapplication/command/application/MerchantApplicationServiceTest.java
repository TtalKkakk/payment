package com.example.pg.merchantapplication.command.application;

import com.example.pg.merchantapplication.command.application.port.MerchantApplicationPort;
import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationApprovedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationRejectedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationSubmittedEvent;
import com.example.pg.merchantapplication.domain.vo.ApplicationName;
import com.example.pg.merchantapplication.domain.vo.BusinessNumber;
import com.example.pg.merchantapplication.domain.vo.ContactEmail;
import com.example.pg.merchantapplication.domain.vo.ContactPhone;
import com.example.pg.merchantapplication.domain.vo.PasswordHash;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantApplicationServiceTest {

    private static final String NAME = "테스트가맹점";
    private static final String BUSINESS_NUMBER = "123-45-67890";
    private static final String PHONE = "010-1234-5678";
    private static final String EMAIL = "test@example.com";
    private static final String RAW_PASSWORD = "password123";
    private static final String PASSWORD_HASH = "$2a$10$encodedHash";

    @Mock
    private MerchantApplicationRepository merchantApplicationRepository;
    @Mock
    private MerchantApplicationPort merchantApplicationPort;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MerchantApplicationService merchantApplicationService;

    @Nested
    @DisplayName("apply")
    class Apply {

        @Test
        @DisplayName("신규 신청 시 create 후 save, 이벤트 발행")
        void newApplication() {
            when(merchantApplicationRepository.existsByBusinessNumberAndStatusIn(eq(BUSINESS_NUMBER), any()))
                    .thenReturn(false);
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);

            MerchantApplication result = merchantApplicationService.apply(NAME, BUSINESS_NUMBER, PHONE, EMAIL, RAW_PASSWORD);

            ArgumentCaptor<MerchantApplication> captor = ArgumentCaptor.forClass(MerchantApplication.class);
            verify(merchantApplicationRepository).save(captor.capture());
            MerchantApplication saved = captor.getValue();

            assertThat(saved.getId()).isNotBlank();
            assertThat(saved.getName()).isEqualTo(NAME);
            assertThat(saved.getBusinessNumber()).isEqualTo(BUSINESS_NUMBER);
            assertThat(saved.getStatus()).isEqualTo(MerchantApplicationStatus.PENDING);
            assertThat(result).isSameAs(saved);
            verify(applicationEventPublisher).publishEvent(any(MerchantApplicationSubmittedEvent.class));
            verify(merchantApplicationPort, never()).createFromApprovedApplication(any(), any());
        }

        @Test
        @DisplayName("동일 사업자번호로 PENDING/APPROVED가 있으면 DUPLICATE_BUSINESS_NUMBER")
        void duplicateBusinessNumber() {
            when(merchantApplicationRepository.existsByBusinessNumberAndStatusIn(eq(BUSINESS_NUMBER), any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> merchantApplicationService.apply(NAME, BUSINESS_NUMBER, PHONE, EMAIL, RAW_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.DUPLICATE_BUSINESS_NUMBER);

            verify(merchantApplicationRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("CANCELLED인 동일 사업자번호가 있으면 reapply 후 save, 이벤트 발행")
        void reapplyFromCancelled() {
            MerchantApplication existing = MerchantApplication.create(
                    ApplicationName.of("구이름"),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of("010-1111-2222"),
                    ContactEmail.of("old@example.com"),
                    PasswordHash.of("oldHash")
            );
            existing.cancel();
            when(merchantApplicationRepository.existsByBusinessNumberAndStatusIn(eq(BUSINESS_NUMBER), any()))
                    .thenReturn(false);
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);

            MerchantApplication result = merchantApplicationService.apply(NAME, BUSINESS_NUMBER, PHONE, EMAIL, RAW_PASSWORD);

            verify(merchantApplicationRepository).save(existing);
            assertThat(existing.getStatus()).isEqualTo(MerchantApplicationStatus.PENDING);
            assertThat(existing.getName()).isEqualTo(NAME);
            assertThat(existing.getContactPhone()).isEqualTo(PHONE);
            assertThat(existing.getContactEmail()).isEqualTo(EMAIL);
            assertThat(result).isSameAs(existing);
            verify(applicationEventPublisher).publishEvent(any(MerchantApplicationSubmittedEvent.class));
        }

        @Test
        @DisplayName("SUBSCRIPTION_ENDED인 동일 사업자번호가 있으면 reapply 후 save")
        void reapplyFromSubscriptionEnded() {
            MerchantApplication existing = MerchantApplication.create(
                    ApplicationName.of("구이름"),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            existing.approve();
            existing.subscriptionEnded();
            when(merchantApplicationRepository.existsByBusinessNumberAndStatusIn(eq(BUSINESS_NUMBER), any()))
                    .thenReturn(false);
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);

            merchantApplicationService.apply(NAME, BUSINESS_NUMBER, PHONE, EMAIL, RAW_PASSWORD);

            verify(merchantApplicationRepository).save(existing);
            assertThat(existing.getStatus()).isEqualTo(MerchantApplicationStatus.PENDING);
            assertThat(existing.getName()).isEqualTo(NAME);
        }

        @Test
        @DisplayName("REJECTED인 동일 사업자번호가 있으면 reapply 후 save")
        void reapplyFromRejected() {
            MerchantApplication existing = MerchantApplication.create(
                    ApplicationName.of("구이름"),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            existing.reject("사유");
            when(merchantApplicationRepository.existsByBusinessNumberAndStatusIn(eq(BUSINESS_NUMBER), any()))
                    .thenReturn(false);
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);

            merchantApplicationService.apply(NAME, BUSINESS_NUMBER, PHONE, EMAIL, RAW_PASSWORD);

            verify(merchantApplicationRepository).save(existing);
            assertThat(existing.getStatus()).isEqualTo(MerchantApplicationStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("PENDING 신청 승인 시 save, Merchant 생성 포트 호출, 이벤트 발행")
        void success() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            String applicationId = app.getId();
            when(merchantApplicationRepository.findById(applicationId)).thenReturn(Optional.of(app));

            merchantApplicationService.approve(applicationId);

            verify(merchantApplicationRepository).save(app);
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.APPROVED);
            verify(merchantApplicationPort).createFromApprovedApplication(applicationId, NAME);
            verify(applicationEventPublisher).publishEvent(any(MerchantApplicationApprovedEvent.class));
        }

        @Test
        @DisplayName("신청 없으면 MERCHANT_APPLICATION_NOT_FOUND")
        void notFound() {
            when(merchantApplicationRepository.findById("invalid-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantApplicationService.approve("invalid-id"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_APPLICATION_NOT_FOUND);
            verify(merchantApplicationRepository, never()).save(any());
            verify(merchantApplicationPort, never()).createFromApprovedApplication(any(), any());
        }

        @Test
        @DisplayName("이미 처리된 신청이면 APPLICATION_ALREADY_PROCESSED")
        void alreadyProcessed() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            app.approve();
            when(merchantApplicationRepository.findById(app.getId())).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> merchantApplicationService.approve(app.getId()))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
            verify(merchantApplicationPort, never()).createFromApprovedApplication(any(), any());
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("PENDING 신청 거절 시 save, 이벤트 발행")
        void success() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            when(merchantApplicationRepository.findById(app.getId())).thenReturn(Optional.of(app));
            String reason = "서류 미비";

            merchantApplicationService.reject(app.getId(), reason);

            verify(merchantApplicationRepository).save(app);
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.REJECTED);
            assertThat(app.getRejectReason()).isEqualTo(reason);
            verify(applicationEventPublisher).publishEvent(any(MerchantApplicationRejectedEvent.class));
        }

        @Test
        @DisplayName("신청 없으면 MERCHANT_APPLICATION_NOT_FOUND")
        void notFound() {
            when(merchantApplicationRepository.findById("invalid-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantApplicationService.reject("invalid-id", "사유"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_APPLICATION_NOT_FOUND);
            verify(merchantApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 처리된 신청이면 APPLICATION_ALREADY_PROCESSED")
        void alreadyProcessed() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            app.approve();
            when(merchantApplicationRepository.findById(app.getId())).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> merchantApplicationService.reject(app.getId(), "사유"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("deleteByBusinessNumberAndPassword")
    class DeleteByBusinessNumberAndPassword {

        @Test
        @DisplayName("비밀번호 일치·PENDING이면 cancel 후 save")
        void success() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(app));
            when(passwordEncoder.matches(RAW_PASSWORD, app.getPasswordHash())).thenReturn(true);

            merchantApplicationService.deleteByBusinessNumberAndPassword(BUSINESS_NUMBER, RAW_PASSWORD);

            verify(merchantApplicationRepository).save(app);
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.CANCELLED);
        }

        @Test
        @DisplayName("신청 없으면 APPLICATION_PASSWORD_MISMATCH")
        void notFound() {
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantApplicationService.deleteByBusinessNumberAndPassword(BUSINESS_NUMBER, RAW_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_PASSWORD_MISMATCH);
            verify(merchantApplicationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("이미 승인/거절된 신청이면 APPLICATION_ALREADY_PROCESSED")
        void alreadyProcessed() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            app.approve();
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(app));
            // status != PENDING 검사에서 먼저 예외 발생 → passwordEncoder.matches()는 호출되지 않음

            assertThatThrownBy(() -> merchantApplicationService.deleteByBusinessNumberAndPassword(BUSINESS_NUMBER, RAW_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
            verify(merchantApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("비밀번호 불일치 시 APPLICATION_PASSWORD_MISMATCH")
        void passwordMismatch() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(app));
            when(passwordEncoder.matches(RAW_PASSWORD, app.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> merchantApplicationService.deleteByBusinessNumberAndPassword(BUSINESS_NUMBER, RAW_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_PASSWORD_MISMATCH);
            verify(merchantApplicationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markSubscriptionEnded")
    class MarkSubscriptionEnded {

        @Test
        @DisplayName("APPROVED 신청이면 subscriptionEnded 후 save")
        void success() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            app.approve();
            String applicationId = app.getId();
            when(merchantApplicationRepository.findById(applicationId)).thenReturn(Optional.of(app));

            merchantApplicationService.markSubscriptionEnded(applicationId);

            verify(merchantApplicationRepository).save(app);
            assertThat(app.getStatus()).isEqualTo(MerchantApplicationStatus.SUBSCRIPTION_ENDED);
        }

        @Test
        @DisplayName("신청 없으면 MERCHANT_APPLICATION_NOT_FOUND")
        void notFound() {
            when(merchantApplicationRepository.findById("invalid-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantApplicationService.markSubscriptionEnded("invalid-id"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_APPLICATION_NOT_FOUND);
            verify(merchantApplicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("APPROVED가 아니면 APPLICATION_ALREADY_PROCESSED")
        void notApproved() {
            MerchantApplication app = MerchantApplication.create(
                    ApplicationName.of(NAME),
                    BusinessNumber.of(BUSINESS_NUMBER),
                    ContactPhone.of(PHONE),
                    ContactEmail.of(EMAIL),
                    PasswordHash.of(PASSWORD_HASH)
            );
            when(merchantApplicationRepository.findById(app.getId())).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> merchantApplicationService.markSubscriptionEnded(app.getId()))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_ALREADY_PROCESSED);
            verify(merchantApplicationRepository, never()).save(any());
        }
    }
}
