package com.example.pg.card_company.presentation.dto;

/**
 * billingKey 교환 요청.
 * 가맹점 서버가 교환 code를 받아 PG로 서버-서버 호출할 때 사용한다.
 */
public record BillingKeyExchangeRequest(
        String code
){
}

