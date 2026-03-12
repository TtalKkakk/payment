package com.example.pg.merchant.query.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.command.application.port.MerchantPort;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.merchant.query.application.dto.PagedMerchantsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * CQRS Query: 가맹점 조회·인증 (상태 변경 없음)
 */
@Service
@RequiredArgsConstructor
public class MerchantQueryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int PAGE_BLOCK_SIZE = 10;

    private final MerchantRepository merchantRepository;
    private final MerchantPort merchantPort;

    /**
     * API Key + Secret으로 가맹점 인증.
     * 성공 시 merchantId 반환, 실패 시 empty.
     */
    @Transactional(readOnly = true)
    public Optional<String> authenticate(String apiKey, String apiSecret) {
        if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            return Optional.empty();
        }
        return merchantRepository.findByApiKey(apiKey)
                .filter(merchant -> merchant.matchesSecret(apiSecret))
                .filter(Merchant::isActive)
                .map(Merchant::getId);
    }

    /**
     * merchantId로 가맹점 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Merchant findById(String merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, merchantId));
    }

    /**
     * 페이지네이션된 가맹점 목록 (관리자용).
     * id: Merchant ID 부분 검색 (null/blank면 전체), id 기준 내림차순.
     */
    @Transactional(readOnly = true)
    public PagedMerchantsResult findPaged(String id, int pageNumber) {
        if (pageNumber < 0) {
            pageNumber = 0;
        }
        Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
        Page<Merchant> page = (id != null && !id.isBlank())
                ? merchantRepository.findByIdContaining(id.trim(), pageable)
                : merchantRepository.findAll(pageable);
        return PagedMerchantsResult.of(page, PAGE_BLOCK_SIZE);
    }

    @Transactional(readOnly = true)
    public Merchant findKeyAndSecretResponse(String businessNumber, String password){
        String applicationId = merchantPort
                .findApprovedApplicationIdByBusinessNumberAndPassword(businessNumber, password);

        return merchantRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND, applicationId));
    }
}
