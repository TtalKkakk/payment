package com.example.pg.common.presentation;

/**
 * 웹훅 발송 아웃바운드 포트.
 * URL로 JSON 본문을 POST하며, 선택적으로 서명 헤더를 붙인다.
 */
public interface FranchiseConnect {

    /**
     * POST로 본문을 전송한다.
     *
     * @param url             전송 대상 URL
     * @param bodyJson        요청 본문 (JSON 문자열)
     * @param signatureValue  X-PG-Signature 헤더 값 (null이면 헤더 미설정)
     */
    void send(String url, String bodyJson, String signatureValue);
}
