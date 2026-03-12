package com.example.pg.merchant.command.application.adapter;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.command.application.port.MerchantPort;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.enumerate.MerchantStatus;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MerchantPortAdapter implements MerchantPort {

    private final MerchantRepository merchantRepository;
    private final MerchantApplicationRepository merchantApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String findApprovedApplicationIdByBusinessNumberAndPassword(String businessNumber, String rawPassword) {
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
    public void createFromApprovedApplication(String applicationId, String name) {
        Merchant merchant = merchantRepository.findByApplicationId(applicationId)
                .filter(m -> m.getStatus() == MerchantStatus.WITHDRAWN)
                .map(m -> {
                    m.reactivate();
                    return m;
                })
                .orElseGet(() -> Merchant.create(name, applicationId));
        merchantRepository.save(merchant);
    }
}
