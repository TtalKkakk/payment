package com.example.pg.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 에러 코드·메시지·HTTP 상태를 enum으로 관리.
 * 클라이언트는 code(E001 등)로 분기할 수 있다.
 * - 메시지는 한글 정중체, 끝에 마침표.
 * - placeholder(%s)가 있으면 반드시 BusinessException 생성 시 해당 인자 전달.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ----- Merchant 도메인 -----
    // 404 Not Found
    MERCHANT_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "가맹점을 찾을 수 없습니다. (id=%s)"),
    // 409 Conflict
    MERCHANT_CANNOT_SUSPEND(HttpStatus.CONFLICT, "E024", "탈퇴한 가맹점은 정지할 수 없습니다."),
    MERCHANT_CANNOT_ACTIVATE(HttpStatus.CONFLICT, "E025", "정지 상태가 아닌 가맹점은 정지 해제할 수 없습니다."),
    MERCHANT_CANNOT_REACTIVATE(HttpStatus.CONFLICT, "E026", "탈퇴 상태가 아닌 가맹점은 재활성화할 수 없습니다. (status=%s)"),
    MERCHANT_CANNOT_WITHDRAW(HttpStatus.CONFLICT, "E027", "정지된 가맹점만 탈퇴 처리할 수 있습니다. (현재 상태: %s)"),

    // ----- MerchantApplication 도메인 -----
    // 401 Unauthorized
    APPLICATION_PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "E004", "사업자번호 또는 비밀번호가 올바르지 않습니다."),
    // 403 Forbidden
    APPLICATION_NOT_APPROVED(HttpStatus.FORBIDDEN, "E005", "API 키는 승인된 신청에만 조회할 수 있습니다."),
    // 404 Not Found
    MERCHANT_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "E003", "가맹점 신청을 찾을 수 없습니다. (applicationId=%s)"),
    // 400 Bad Request
    REJECT_REASON_TOO_LONG(HttpStatus.BAD_REQUEST, "E018", "거절 사유는 500자를 초과할 수 없습니다."),
    APPLICATION_INVALID_INPUT(HttpStatus.BAD_REQUEST, "E019", "신청 정보가 올바르지 않습니다. (%s)"),
    // 409 Conflict
    APPLICATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "E021", "이미 처리된 신청입니다. (status=%s)"),
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "E022", "이미 등록된 사업자번호입니다."),

    // ----- 공통 / 기타 도메인 -----
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "E023", "다른 사용자가 먼저 처리했습니다. 새로고침 후 다시 시도해 주세요."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "결제를 찾을 수 없습니다. (paymentId=%s)"),
    PAYMENT_ID_INVALID(HttpStatus.BAD_REQUEST, "E036", "결제 ID가 올바르지 않습니다."),
    PAYMENT_INVALID_STATUS(HttpStatus.CONFLICT, "E020", "결제 상태 오류. (%s)"),
    PAYMENT_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "E016", "결제 금액은 0보다 커야 합니다. (amount=%s)"),
    BILLING_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "E017", "결제 승인을 위해 빌링키가 필요합니다."),

    // ----- CardCompany 도메인 -----
    CARD_COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "E031", "카드사를 찾을 수 없습니다. (id=%s)"),
    CARD_COMPANY_DUPLICATE_CODE(HttpStatus.CONFLICT, "E032", "이미 등록된 카드사 코드입니다. (code=%s)"),
    CARD_COMPANY_API_ERROR(HttpStatus.BAD_GATEWAY, "E033", "카드사 연동 오류. %s"),
    CARD_REGISTER_SESSION_INVALID(HttpStatus.BAD_REQUEST, "E034", "유효하지 않거나 만료된 카드 등록 세션입니다. (token=%s)"),
    CARD_REGISTER_AUTH_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "E035", "유효한 인증 코드가 없습니다. 카드 등록을 다시 시도해 주세요."),
    BILLING_KEY_REGISTER_TOKEN_INVALID(HttpStatus.FORBIDDEN, "E038", "유효하지 않은 카드 등록 요청입니다."),
    BILLING_KEY_EXCHANGE_CODE_INVALID(HttpStatus.BAD_REQUEST, "E039", "유효하지 않거나 만료된 교환 코드입니다."),

    /** 결제 생성 실패 (Tx1 실패). 가맹점: "다시 결제하기" 안내 */
    PAYMENT_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E029", "결제 접수에 실패했습니다. 다시 시도해 주세요."),
    /** 승인 요청 실패 (Tx2 실패). 가맹점: "다시 결제하기" 또는 "같은 결제로 승인만 재시도" 안내 */
    AUTHORIZATION_START_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E030", "결제는 접수됐으나 승인 요청 전송에 실패했습니다. 다시 시도하거나 같은 결제로 승인만 재시도해 주세요."),

    // ----- Receipt 도메인 -----
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "E040", "영수증을 찾을 수 없습니다. (receiptId=%s)"),
    RECEIPT_ALREADY_VOIDED(HttpStatus.CONFLICT, "E041", "이미 무효화된 영수증입니다."),
    RECEIPT_CANNOT_ISSUE(HttpStatus.CONFLICT, "E042", "승인 완료된 결제에만 영수증을 발급할 수 있습니다."),

    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "E999", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String messageTemplate;

    public String formatMessage(Object... args) {
        if (args == null || args.length == 0) {
            return messageTemplate;
        }
        return String.format(messageTemplate, args);
    }
}
