package com.example.pg.merchantapplication.application.dto;

import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;

public enum SearchType {
    ALL,
    BY_STATUS,
    BY_BUSINESS_NUMBER,
    BY_BOTH;


    public static SearchType from(String status, String businessNumber) {
        boolean hasStatus = hasValidStatus(status);
        boolean hasBusinessNumber = businessNumber != null && !businessNumber.isBlank();
        if (hasStatus && hasBusinessNumber) {
            return BY_BOTH;
        }
        if (hasStatus) {
            return BY_STATUS;
        }
        if (hasBusinessNumber) {
            return BY_BUSINESS_NUMBER;
        }
        return ALL;
    }

    private static boolean hasValidStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        try {
            MerchantApplicationStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
