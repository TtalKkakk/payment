package com.example.pg.merchantapplication.command.application.adapter;

import com.example.pg.merchantapplication.command.application.port.MerchantApplicationPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * #4 연동 전까지 사용. approve/탈퇴 연동이 없을 때 MerchantService가 의존하는 포트를 채움.
 */
@Primary
@Component
public class NoOpMerchantApplicationPortAdapter implements MerchantApplicationPort {

    @Override
    public void createFromApprovedApplication(String applicationId, String name) {
        // no-op until #4
    }

    @Override
    public void markSubscriptionEnded(String applicationId) {
        // no-op until #4
    }
}
