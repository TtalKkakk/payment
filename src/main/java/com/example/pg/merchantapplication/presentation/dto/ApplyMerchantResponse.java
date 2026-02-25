package com.example.pg.merchantapplication.presentation.dto;

import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;

public record ApplyMerchantResponse(
        String applicationId,
        String status
) {
    public static ApplyMerchantResponse from(MerchantApplication merchantApplication){
        return new ApplyMerchantResponse(
                merchantApplication.getId(),
                merchantApplication.getStatus().name()
        );
    }
}
