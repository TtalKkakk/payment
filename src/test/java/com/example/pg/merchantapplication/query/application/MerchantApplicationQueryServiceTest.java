package com.example.pg.merchantapplication.query.application;

import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.domain.vo.ApplicationName;
import com.example.pg.merchantapplication.domain.vo.BusinessNumber;
import com.example.pg.merchantapplication.domain.vo.ContactEmail;
import com.example.pg.merchantapplication.domain.vo.ContactPhone;
import com.example.pg.merchantapplication.domain.vo.PasswordHash;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
import com.example.pg.merchantapplication.query.application.dto.PagedApplicationsResult;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantApplicationQueryServiceTest {

    private static final String NAME = "테스트가맹점";
    private static final String BUSINESS_NUMBER = "123-45-67890";
    private static final String PHONE = "010-1234-5678";
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD_HASH = "$2a$10$encodedHash";
    private static final String RAW_PASSWORD = "password123";

    @Mock
    private MerchantApplicationRepository merchantApplicationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MerchantApplicationQueryService merchantApplicationQueryService;

    private static MerchantApplication createApproved() {
        MerchantApplication app = MerchantApplication.create(
                ApplicationName.of(NAME),
                BusinessNumber.of(BUSINESS_NUMBER),
                ContactPhone.of(PHONE),
                ContactEmail.of(EMAIL),
                PasswordHash.of(PASSWORD_HASH)
        );
        app.approve();
        return app;
    }

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
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("존재하면 MerchantApplication 반환")
        void success() {
            MerchantApplication app = createPending();
            String id = app.getId();
            when(merchantApplicationRepository.findById(id)).thenReturn(Optional.of(app));

            MerchantApplication result = merchantApplicationQueryService.findById(id);

            assertThat(result).isSameAs(app);
        }

        @Test
        @DisplayName("없으면 MERCHANT_APPLICATION_NOT_FOUND")
        void notFound() {
            when(merchantApplicationRepository.findById("invalid-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantApplicationQueryService.findById("invalid-id"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_APPLICATION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getByBusinessNumberAndPassword")
    class GetByBusinessNumberAndPassword {

        @Test
        @DisplayName("비밀번호 일치·APPROVED면 MerchantApplication 반환")
        void success() {
            MerchantApplication app = createApproved();
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(app));
            when(passwordEncoder.matches(RAW_PASSWORD, app.getPasswordHash())).thenReturn(true);

            MerchantApplication result = merchantApplicationQueryService.getByBusinessNumberAndPassword(BUSINESS_NUMBER, RAW_PASSWORD);

            assertThat(result).isSameAs(app);
        }

        @Test
        @DisplayName("사업자번호에 해당 신청 없으면 APPLICATION_PASSWORD_MISMATCH")
        void notFound() {
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantApplicationQueryService.getByBusinessNumberAndPassword(BUSINESS_NUMBER, RAW_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("비밀번호 불일치 시 APPLICATION_PASSWORD_MISMATCH")
        void passwordMismatch() {
            MerchantApplication app = createApproved();
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(app));
            when(passwordEncoder.matches(RAW_PASSWORD, app.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> merchantApplicationQueryService.getByBusinessNumberAndPassword(BUSINESS_NUMBER, RAW_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("APPROVED가 아니면 APPLICATION_NOT_APPROVED")
        void notApproved() {
            MerchantApplication app = createPending();
            when(merchantApplicationRepository.findByBusinessNumber(BUSINESS_NUMBER)).thenReturn(Optional.of(app));
            when(passwordEncoder.matches(RAW_PASSWORD, app.getPasswordHash())).thenReturn(true);

            assertThatThrownBy(() -> merchantApplicationQueryService.getByBusinessNumberAndPassword(BUSINESS_NUMBER, RAW_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.APPLICATION_NOT_APPROVED);
        }
    }

    @Nested
    @DisplayName("findPaged")
    class FindPaged {

        private static final List<MerchantApplicationStatus> EXCLUDED =
                List.of(MerchantApplicationStatus.CANCELLED, MerchantApplicationStatus.SUBSCRIPTION_ENDED);

        private Page<MerchantApplication> emptyPage() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            return new PageImpl<>(List.of(), pageable, 0);
        }

        @Test
        @DisplayName("status·businessNumber 둘 다 없으면 findAllByStatusNotIn 호출 (ALL)")
        void all() {
            Page<MerchantApplication> page = emptyPage();
            when(merchantApplicationRepository.findAllByStatusNotIn(eq(EXCLUDED), any(Pageable.class))).thenReturn(page);

            PagedApplicationsResult result = merchantApplicationQueryService.findPaged(null, null, 0);

            assertThat(result.content()).isEmpty();
            verify(merchantApplicationRepository).findAllByStatusNotIn(eq(EXCLUDED), any(Pageable.class));
        }

        @Test
        @DisplayName("status만 유효하면 findByStatus 호출 (BY_STATUS)")
        void byStatus() {
            Page<MerchantApplication> page = emptyPage();
            when(merchantApplicationRepository.findByStatus(eq(MerchantApplicationStatus.PENDING), any(Pageable.class))).thenReturn(page);

            PagedApplicationsResult result = merchantApplicationQueryService.findPaged("PENDING", null, 0);

            assertThat(result.content()).isEmpty();
            verify(merchantApplicationRepository).findByStatus(eq(MerchantApplicationStatus.PENDING), any(Pageable.class));
        }

        @Test
        @DisplayName("businessNumber만 있으면 findByBusinessNumberContainingAndStatusNotIn 호출 (BY_BUSINESS_NUMBER)")
        void byBusinessNumber() {
            Page<MerchantApplication> page = emptyPage();
            when(merchantApplicationRepository.findByBusinessNumberContainingAndStatusNotIn(eq("123"), eq(EXCLUDED), any(Pageable.class))).thenReturn(page);

            PagedApplicationsResult result = merchantApplicationQueryService.findPaged(null, "123", 0);

            assertThat(result.content()).isEmpty();
            verify(merchantApplicationRepository).findByBusinessNumberContainingAndStatusNotIn(eq("123"), eq(EXCLUDED), any(Pageable.class));
        }

        @Test
        @DisplayName("status·businessNumber 둘 다 있으면 findByBusinessNumberContainingAndStatus 호출 (BY_BOTH)")
        void byBoth() {
            Page<MerchantApplication> page = emptyPage();
            when(merchantApplicationRepository.findByBusinessNumberContainingAndStatus(eq("123"), eq(MerchantApplicationStatus.APPROVED), any(Pageable.class))).thenReturn(page);

            PagedApplicationsResult result = merchantApplicationQueryService.findPaged("APPROVED", "123", 0);

            assertThat(result.content()).isEmpty();
            verify(merchantApplicationRepository).findByBusinessNumberContainingAndStatus(eq("123"), eq(MerchantApplicationStatus.APPROVED), any(Pageable.class));
        }

        @Test
        @DisplayName("pageNumber 음수면 0으로 보정")
        void negativePageCorrectedToZero() {
            Page<MerchantApplication> page = emptyPage();
            when(merchantApplicationRepository.findAllByStatusNotIn(eq(EXCLUDED), any(Pageable.class))).thenReturn(page);

            merchantApplicationQueryService.findPaged(null, null, -1);

            verify(merchantApplicationRepository).findAllByStatusNotIn(eq(EXCLUDED), any(Pageable.class));
        }

        @Test
        @DisplayName("잘못된 status 문자열이면 ALL로 처리 (findAllByStatusNotIn)")
        void invalidStatusTreatedAsAll() {
            Page<MerchantApplication> page = emptyPage();
            when(merchantApplicationRepository.findAllByStatusNotIn(eq(EXCLUDED), any(Pageable.class))).thenReturn(page);

            merchantApplicationQueryService.findPaged("INVALID_STATUS", null, 0);

            verify(merchantApplicationRepository).findAllByStatusNotIn(eq(EXCLUDED), any(Pageable.class));
        }
    }
}
