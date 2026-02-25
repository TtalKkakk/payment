package com.example.pg.merchantapplication.command.application.adapter;

import com.example.pg.merchant.command.application.port.MerchantPort;
import com.example.pg.merchantapplication.command.application.MerchantApplicationService;
import com.example.pg.merchantapplication.command.application.port.MerchantApplicationPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

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
    public void createFromApprovedApplication(String applicationId, String name) {
        merchantPort.createFromApprovedApplication(applicationId, name);
    }

    @Override
    public void markSubscriptionEnded(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            return;
        }
        merchantApplicationService.markSubscriptionEnded(applicationId);
    }
}
