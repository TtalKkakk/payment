package com.example.pg.merchantapplication.command.application.adapter;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.vo.MerchantName;
import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
import com.example.pg.merchantapplication.presentation.port.MerchantApplicationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantApplicationAdapter implements MerchantApplicationPort {

    private final MerchantPort merchantPort;
    private final MerchantApplicationRepository merchantApplicationRepository;

    @Override
    public Merchant createFromApprovedApplication(String applicationId, MerchantName name) {
        log.debug("[MerchantApplication] Port createFromApprovedApplication applicationId={} name={}", applicationId, name != null ? name.value() : null);
        return merchantPort.createFromApprovedApplication(applicationId, name);
    }

    @Override
    public void markSubscriptionEnded(String applicationId) {
        log.debug("[MerchantApplication] Port markSubscriptionEnded applicationId={}", applicationId);
        MerchantApplication application = merchantApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_APPLICATION_NOT_FOUND, applicationId));
        application.subscriptionEnded();
        merchantApplicationRepository.save(application);
    }
}
