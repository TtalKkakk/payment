package com.example.pg.payment.presentation.impl;

import com.example.pg.common.util.HttpOutbound;
import com.example.pg.payment.presentation.FranchiseConnect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 웹훅 발송 HTTP 어댑터.
 * 공통 HttpOutboundPort를 사용해 POST로 JSON 본문을 보내고, 서명이 있으면 X-PG-Signature 헤더에 설정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpWebhookSenderConnectImpl implements FranchiseConnect {

    private static final String SIGNATURE_HEADER = "X-PG-Signature";

    private final HttpOutbound httpOutbound;

    @Override
    public void send(String url, String bodyJson, String signatureValue) {
        Map<String, String> extraHeaders = null;
        if (signatureValue != null && !signatureValue.isBlank()) {
            extraHeaders = Map.of(SIGNATURE_HEADER, signatureValue);
        }
        try {
            httpOutbound.post(url, bodyJson, extraHeaders);
            log.info("[Payment] webhook delivery done url={}", url);
        } catch (Exception e) {
            log.error("[Payment] webhook delivery failed url={}", url, e);
        }
    }
}
