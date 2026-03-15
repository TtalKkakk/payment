package com.example.pg.merchant.query.application;

import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.enumerate.MerchantStatus;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.merchant.query.application.dto.PagedMerchantsResultDto;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantQueryServiceTest {

    private static final String MERCHANT_ID = "merchant-1";
    private static final String API_KEY = "pk_merchant_abc";
    private static final String API_SECRET = "sk_merchant_xyz";
    private static final String NAME = "테스트가맹점";
    private static final String APPLICATION_ID = "app-1";
    private static final String BUSINESS_NUMBER = "123-45-67890";
    private static final String PASSWORD = "password123";

    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private MerchantPort merchantPort;

    @InjectMocks
    private MerchantQueryService merchantQueryService;

    private static Merchant activeMerchant() {
        return new Merchant(MERCHANT_ID, API_KEY, API_SECRET, NAME, APPLICATION_ID, MerchantStatus.ACTIVE);
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("apiKey·apiSecret 일치하고 ACTIVE면 merchantId 반환")
        void success() {
            Merchant merchant = activeMerchant();
            when(merchantRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(merchant));

            Optional<String> result = merchantQueryService.authenticate(API_KEY, API_SECRET);

            assertThat(result).contains(MERCHANT_ID);
        }

        @Test
        @DisplayName("apiKey 없으면 empty")
        void emptyWhenApiKeyNull() {
            assertThat(merchantQueryService.authenticate(null, API_SECRET)).isEmpty();
            assertThat(merchantQueryService.authenticate("", API_SECRET)).isEmpty();
            assertThat(merchantQueryService.authenticate("  ", API_SECRET)).isEmpty();
        }

        @Test
        @DisplayName("apiSecret 없으면 empty")
        void emptyWhenApiSecretNull() {
            assertThat(merchantQueryService.authenticate(API_KEY, null)).isEmpty();
            assertThat(merchantQueryService.authenticate(API_KEY, "")).isEmpty();
        }

        @Test
        @DisplayName("apiKey에 해당하는 가맹점 없으면 empty")
        void emptyWhenNotFound() {
            when(merchantRepository.findByApiKey("unknown")).thenReturn(Optional.empty());

            assertThat(merchantQueryService.authenticate("unknown", API_SECRET)).isEmpty();
        }

        @Test
        @DisplayName("secret 불일치면 empty")
        void emptyWhenSecretMismatch() {
            Merchant merchant = activeMerchant();
            when(merchantRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(merchant));

            assertThat(merchantQueryService.authenticate(API_KEY, "wrong_secret")).isEmpty();
        }

        @Test
        @DisplayName("SUSPENDED면 empty (isActive false)")
        void emptyWhenSuspended() {
            Merchant merchant = new Merchant(MERCHANT_ID, API_KEY, API_SECRET, NAME, APPLICATION_ID, MerchantStatus.SUSPENDED);
            when(merchantRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(merchant));

            assertThat(merchantQueryService.authenticate(API_KEY, API_SECRET)).isEmpty();
        }

        @Test
        @DisplayName("WITHDRAWN이면 empty")
        void emptyWhenWithdrawn() {
            Merchant merchant = new Merchant(MERCHANT_ID, API_KEY, API_SECRET, NAME, APPLICATION_ID, MerchantStatus.WITHDRAWN);
            when(merchantRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(merchant));

            assertThat(merchantQueryService.authenticate(API_KEY, API_SECRET)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("존재하면 Merchant 반환")
        void success() {
            Merchant merchant = activeMerchant();
            when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

            Merchant result = merchantQueryService.findById(MERCHANT_ID);

            assertThat(result).isSameAs(merchant);
        }

        @Test
        @DisplayName("없으면 MERCHANT_NOT_FOUND")
        void notFound() {
            when(merchantRepository.findById("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantQueryService.findById("invalid"))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findPaged")
    class FindPaged {

        @Test
        @DisplayName("id가 null/blank면 findAll 호출")
        void findAllWhenIdBlank() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            Page<Merchant> page = new PageImpl<>(List.of(activeMerchant()), pageable, 1);
            when(merchantRepository.findAll(any(Pageable.class))).thenReturn(page);

            PagedMerchantsResultDto result = merchantQueryService.findPaged(null, 0);

            assertThat(result.content()).hasSize(1);
            verify(merchantRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("id가 있으면 findByIdContaining 호출")
        void findByIdContainingWhenIdPresent() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            Page<Merchant> page = new PageImpl<>(List.of(activeMerchant()), pageable, 1);
            when(merchantRepository.findByIdContaining(eq("abc"), any(Pageable.class))).thenReturn(page);

            PagedMerchantsResultDto result = merchantQueryService.findPaged("abc", 0);

            assertThat(result.content()).hasSize(1);
            verify(merchantRepository).findByIdContaining(eq("abc"), any(Pageable.class));
        }

        @Test
        @DisplayName("pageNumber가 음수면 0으로 보정")
        void negativePageCorrectedToZero() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
            Page<Merchant> page = new PageImpl<>(List.of(), pageable, 0);
            when(merchantRepository.findAll(any(Pageable.class))).thenReturn(page);

            merchantQueryService.findPaged(null, -1);

            verify(merchantRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("findKeyAndSecretResponse")
    class FindKeyAndSecretResponse {

        @Test
        @DisplayName("포트로 applicationId 조회 후 해당 Merchant 반환")
        void success() {
            when(merchantPort.findApprovedApplicationIdByBusinessNumberAndPassword(BUSINESS_NUMBER, PASSWORD))
                    .thenReturn(APPLICATION_ID);
            Merchant merchant = activeMerchant();
            when(merchantRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.of(merchant));

            Merchant result = merchantQueryService.findKeyAndSecretResponse(BUSINESS_NUMBER, PASSWORD);

            assertThat(result).isSameAs(merchant);
            verify(merchantPort).findApprovedApplicationIdByBusinessNumberAndPassword(BUSINESS_NUMBER, PASSWORD);
            verify(merchantRepository).findByApplicationId(APPLICATION_ID);
        }

        @Test
        @DisplayName("applicationId에 해당하는 Merchant 없으면 MERCHANT_NOT_FOUND")
        void notFound() {
            when(merchantPort.findApprovedApplicationIdByBusinessNumberAndPassword(BUSINESS_NUMBER, PASSWORD))
                    .thenReturn(APPLICATION_ID);
            when(merchantRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> merchantQueryService.findKeyAndSecretResponse(BUSINESS_NUMBER, PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.MERCHANT_NOT_FOUND);
        }
    }
}
