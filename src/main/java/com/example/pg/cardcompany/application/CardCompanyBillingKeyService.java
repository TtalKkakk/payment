package com.example.pg.cardcompany.application;

import com.example.pg.cardcompany.domain.BillingKeyRecord;
import com.example.pg.cardcompany.domain.BillingKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 카드사 빌링키 발급·차단 서비스.
 * 같은 프로젝트일 때 PG가 이 서비스를 직접 호출하고,
 * 분리 시 이 서비스는 카드사 REST API(Controller) 뒤에서 호출된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardCompanyBillingKeyService {

    private final BillingKeyRepository billingKeyRepository;

    /**
     * cardToken 기준으로 빌링키 발급. (동일 cardToken이면 기존 빌링키 반환)
     */
    @Transactional
    public String issueBillingKey(String cardToken, String merchantId) {
        return billingKeyRepository.findByCardToken(cardToken)
                .map(BillingKeyRecord::getBillingKeyValue)
                .orElseGet(() -> {
                    String billingKey = "bk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                    billingKeyRepository.save(new BillingKeyRecord(cardToken, billingKey, merchantId));
                    log.info("[CardCompany] BillingKey issued. cardToken={}, merchantId={}", cardToken, merchantId);
                    return billingKey;
                });
    }

    /**
     * 해당 가맹점에 발급된 모든 빌링키 차단.
     */
    @Transactional
    public void revokeByMerchantId(String merchantId) {
        billingKeyRepository.deleteByMerchantId(merchantId);
        log.info("[CardCompany] BillingKeys revoked. merchantId={}", merchantId);
    }
}
