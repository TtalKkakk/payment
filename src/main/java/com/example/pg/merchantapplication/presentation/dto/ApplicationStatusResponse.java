package com.example.pg.merchantapplication.presentation.dto;

import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;

import java.time.LocalDateTime;

public record ApplicationStatusResponse(
        String name,
        String businessNumber,
        MerchantApplicationStatus status,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
    public static ApplicationStatusResponse from(MerchantApplication merchantApplication) {
        return new ApplicationStatusResponse(
                merchantApplication.getName(),
                merchantApplication.getBusinessNumber(),
                merchantApplication.getStatus(),
                merchantApplication.getRejectReason(),
                merchantApplication.getCreatedAt(),
                merchantApplication.getProcessedAt()
        );
    }
}
