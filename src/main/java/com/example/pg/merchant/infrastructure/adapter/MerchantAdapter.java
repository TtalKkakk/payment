package com.example.pg.merchant.infrastructure.adapter;

import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.payment.command.application.port.MerchantPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * merchant 쪽 어댑터. 영수증 발급 시 가맹점명만 제공 (merchant 도메인만 의존).
 */
@Component
@RequiredArgsConstructor
public class MerchantAdapter implements MerchantPort {

    private final MerchantRepository merchantRepository;

    @Override
    public String getMerchantName(String merchantId) {
        return merchantRepository.findById(merchantId)
                .map(m -> m.getName())
                .orElse("");
    }
}
