package com.example.pg.merchant.command.application.adapter;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.enumerate.MerchantStatus;
import com.example.pg.merchant.domain.vo.MerchantName;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantPortAdapter implements MerchantPort {

    private final MerchantRepository merchantRepository;
    private final MerchantApplicationRepository merchantApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String findApprovedApplicationIdByBusinessNumberAndPassword(String businessNumber, String rawPassword) {
        log.debug("[Merchant] Port findApprovedApplicationId businessNumber={}", businessNumber);
        var merchantApplication = merchantApplicationRepository.findByBusinessNumber(businessNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_PASSWORD_MISMATCH));
        if (!passwordEncoder.matches(rawPassword, merchantApplication.getPasswordHash())) {
            throw new BusinessException(ErrorCode.APPLICATION_PASSWORD_MISMATCH);
        }
        if (merchantApplication.getStatus() != MerchantApplicationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_APPROVED);
        }
        return merchantApplication.getId();
    }

    @Override
    public Merchant createFromApprovedApplication(String applicationId, MerchantName name) {
        log.debug("[Merchant] Port createFromApprovedApplication applicationId={} name={}", applicationId, name != null ? name.value() : null);
        Merchant merchant = merchantRepository.findByApplicationId(applicationId)
                .filter(m -> m.getStatus() == MerchantStatus.WITHDRAWN)
                .map(m -> {
                    m.reactivate(name);
                    return m;
                })
                .orElseGet(() -> Merchant.create(name, applicationId));
        return merchantRepository.save(merchant);
    }

    @Override
    public String getMerchantName(String merchantId) {
        log.debug("[Merchant] Adapter getMerchantName merchantId={}", merchantId);
        return merchantRepository.findById(merchantId)
                .map(Merchant::getName)
                .orElse("");
    }
}
