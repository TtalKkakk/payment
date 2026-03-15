package com.example.pg.merchantapplication.command.application.adapter;

import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.domain.vo.MerchantName;
import com.example.pg.merchantapplication.command.application.MerchantApplicationService;
import com.example.pg.merchantapplication.presentation.port.MerchantApplicationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MerchantApplicationPortAdapter implements MerchantApplicationPort {

    private final MerchantPort merchantPort;
    private final MerchantApplicationService merchantApplicationService;

    public MerchantApplicationPortAdapter(
            MerchantPort merchantPort,
            @Lazy MerchantApplicationService merchantApplicationService
    ) {
        this.merchantPort = merchantPort;
        this.merchantApplicationService = merchantApplicationService;
    }

    @Override
    public Merchant createFromApprovedApplication(String applicationId, MerchantName name) {
        log.debug("[MerchantApplication] Port createFromApprovedApplication applicationId={} name={}", applicationId, name != null ? name.value() : null);
        return merchantPort.createFromApprovedApplication(applicationId, name);
    }

    @Override
    public void markSubscriptionEnded(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            return;
        }
        log.debug("[MerchantApplication] Port markSubscriptionEnded applicationId={}", applicationId);
        merchantApplicationService.markSubscriptionEnded(applicationId);
    }
}
