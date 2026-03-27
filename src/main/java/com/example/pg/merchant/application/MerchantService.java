package com.example.pg.merchant.application;

import com.example.pg.merchant.application.dto.RegenerateSecretResultDto;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.event.MerchantActivatedEvent;
import com.example.pg.merchant.domain.event.MerchantDeletedEvent;
import com.example.pg.merchant.domain.event.MerchantSecretRegeneratedEvent;
import com.example.pg.merchant.domain.event.MerchantSuspendedEvent;
import com.example.pg.merchant.domain.vo.ApiKey;
import com.example.pg.merchant.domain.vo.ApiSecret;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.merchant.application.dto.PagedMerchantsResultDto;
import com.example.pg.merchantapplication.presentation.port.MerchantApplicationPort;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int PAGE_BLOCK_SIZE = 10;

    private final MerchantPort merchantPort;
    private final MerchantRepository merchantRepository;
    private final MerchantApplicationPort merchantApplicationPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * @param merchantId
     * @return RegenerateSecretResultDto
     * X-API-KEY 및 X-API-SECRET 재생성
     */
    @Transactional
    public RegenerateSecretResultDto regenerateSecret(String merchantId) {
        log.debug("[Merchant] regenerateSecret start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));

        merchant.regenerateSecret();

        applicationEventPublisher.publishEvent(MerchantSecretRegeneratedEvent.from(merchant.getId()));

        log.debug("[Merchant] regenerateSecret committed merchantId={}", merchantId);
        return new RegenerateSecretResultDto(merchant.getApiKey(), merchant.getApiSecret());
    }

    /**
     * 가맹점 탈퇴(soft delete). 상태를 WITHDRAWN으로 변경하고, 같은 트랜잭션에서 신청서를 SUBSCRIPTION_ENDED로 전이한 뒤 이벤트를 발행한다.
     */
    @Transactional
    public void deleteMerchant(String merchantId) {
        log.debug("[Merchant] deleteMerchant start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));

        merchant.withdraw();
        merchantRepository.save(merchant);

        merchantApplicationPort.markSubscriptionEnded(merchant.getApplicationId());

        applicationEventPublisher.publishEvent(MerchantDeletedEvent.from(merchantId, merchant.getName(), merchant.getApplicationId()));
        log.debug("[Merchant] deleteMerchant committed merchantId={}", merchantId);
    }

    /**
     * 가맹점 API 정지. ACTIVE → SUSPENDED.
     */
    @Transactional
    public void suspendMerchant(String merchantId) {
        log.debug("[Merchant] suspendMerchant start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.suspend();
        merchantRepository.save(merchant);
        applicationEventPublisher.publishEvent(MerchantSuspendedEvent.from(merchantId, merchant.getName()));
        log.debug("[Merchant] suspendMerchant committed merchantId={}", merchantId);
    }

    /**
     * 가맹점 정지 해제. SUSPENDED → ACTIVE.
     */
    @Transactional
    public void activateMerchant(String merchantId) {
        log.debug("[Merchant] activateMerchant start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.activate();
        merchantRepository.save(merchant);
        applicationEventPublisher.publishEvent(MerchantActivatedEvent.from(merchantId, merchant.getName()));
        log.debug("[Merchant] activateMerchant committed merchantId={}", merchantId);
    }

    /**
     * 관리자용: 정지(SUSPENDED) 상태인 가맹점만 탈퇴(WITHDRAWN) 처리.
     * 같은 트랜잭션에서 신청서를 SUBSCRIPTION_ENDED로 전이한 뒤 MerchantDeletedEvent 발행.
     */
    @Transactional
    public void withdrawMerchantFromSuspended(String merchantId) {
        log.debug("[Merchant] withdrawMerchantFromSuspended start merchantId={}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
        merchant.withdrawFromSuspended();
        merchantRepository.save(merchant);

        merchantApplicationPort.markSubscriptionEnded(merchant.getApplicationId());

        applicationEventPublisher.publishEvent(MerchantDeletedEvent.from(merchantId, merchant.getName(), merchant.getApplicationId()));
        log.debug("[Merchant] withdrawMerchantFromSuspended committed merchantId={}", merchantId);
    }

    /**
     * API Key + Secret으로 가맹점 인증.
     * 성공 시 merchantId 반환, 실패 시 empty.
     */
    @Transactional(readOnly = true)
    public Optional<String> authenticate(String apiKey, String apiSecret) {
        log.debug("[Merchant] Query authenticate");
        return ApiKey.tryParse(apiKey)
                .flatMap(key -> ApiSecret.tryParse(apiSecret)
                        .flatMap(secret -> merchantRepository.findByApiKey(key)
                                .filter(merchant -> merchant.matchesSecret(secret.value()))
                                .filter(Merchant::isActive)
                                .map(Merchant::getId)));
    }

    /**
     * apiKey로 활성 가맹점 조회.
     * 토큰/필터 계층에서 apiSecret을 이용한 서명 검증 등에 사용된다.
     */
    @Transactional(readOnly = true)
    public Optional<Merchant> findActiveByApiKey(String apiKey) {
        log.debug("[Merchant] Query findActiveByApiKey");
        return ApiKey.tryParse(apiKey)
                .flatMap(merchantRepository::findByApiKey)
                .filter(Merchant::isActive);
    }

    /**
     * merchantId로 가맹점 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Merchant findById(String merchantId) {
        log.debug("[Merchant] Query findById merchantId={}", merchantId);
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
    }

    /**
     * 페이지네이션된 가맹점 목록 (관리자용).
     * id: Merchant ID 부분 검색 (null/blank면 전체), id 기준 내림차순.
     */
    @Transactional(readOnly = true)
    public PagedMerchantsResultDto findPaged(String id, int pageNumber) {
        log.debug("[Merchant] Query findPaged id={} page={}", id, pageNumber);
        Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        Page<Merchant> page = (id != null && !id.isBlank())
                ? merchantRepository.findByIdContaining(id.trim(), pageable)
                : merchantRepository.findAll(pageable);
        return PagedMerchantsResultDto.of(page, PAGE_BLOCK_SIZE);
    }

    @Transactional(readOnly = true)
    public Merchant findKeyAndSecretResponse(String businessNumber, String password){
        log.debug("[Merchant] Query findKeyAndSecretResponse businessNumber={}", businessNumber);
        String applicationId = merchantPort
                .findApprovedApplicationIdByBusinessNumberAndPassword(businessNumber, password);

        return merchantRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, applicationId));
    }
}
