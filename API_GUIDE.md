# PG 서버 API 가이드라인

## 목차

1. [인증](#1-인증)
2. [공통 응답 형식](#2-공통-응답-형식)
3. [에러 코드](#3-에러-코드)
4. [가맹점 가입](#4-가맹점-가입)
5. [가맹점 관리](#5-가맹점-관리)
6. [빌링키 등록 (카드 등록)](#6-빌링키-등록-카드-등록)
7. [결제](#7-결제)
8. [영수증](#8-영수증)
9. [웹훅](#9-웹훅)
10. [웹훅 서명 검증](#10-웹훅-서명-검증)
11. [결제 상태 머신](#11-결제-상태-머신)

---

## 1. 인증

모든 `/api/**` 요청은 헤더에 API 키와 시크릿을 포함해야 합니다.

| 헤더 | 설명 |
|------|------|
| `X-API-KEY` | 가맹점 API 키 (`pk_merchant_...` 형식) |
| `X-API-SECRET` | 가맹점 API 시크릿 (`sk_merchant_...` 형식) |

**인증 불필요 예외 경로** (사업자번호+비밀번호로 처리):
- `POST /api/merchant-applications`
- `POST /api/merchant-applications/business-number`
- `POST /api/merchant-applications/delete`
- `POST /api/merchants/credentials`

인증 실패 시 응답:
```
HTTP 401
{"error": "Invalid or missing API credentials"}
```

---

## 2. 공통 응답 형식

### 성공
각 API별 응답 참고.

### 에러
```json
{
  "code": "E002",
  "message": "결제를 찾을 수 없습니다. (paymentId=abc123)",
  "errors": null
}
```

**유효성 검증 실패** (`400 VALIDATION_FAILED`):
```json
{
  "code": "VALIDATION_FAILED",
  "message": "입력값 검증에 실패했습니다.",
  "errors": [
    { "field": "amount", "message": "amount는 0보다 커야 합니다." }
  ]
}
```

**결제 실패** (재시도 분기용 추가 필드):
```json
{
  "code": "E030",
  "message": "결제는 접수됐으나 승인 요청 전송에 실패했습니다.",
  "retryable": true,
  "action": "RETRY_AUTHORIZE",
  "paymentId": "pay_abc123"
}
```

| `action` 값 | 설명 |
|------------|------|
| `RETRY_PAYMENT` | 결제를 처음부터 다시 시도 |
| `RETRY_AUTHORIZE` | 같은 `paymentId`로 승인만 재시도 (`POST /payments/{paymentId}/authorize`) |

---

## 3. 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| `E001` | 404 | 가맹점을 찾을 수 없음 |
| `E002` | 404 | 결제를 찾을 수 없음 |
| `E003` | 404 | 가맹점 신청을 찾을 수 없음 |
| `E004` | 401 | 사업자번호 또는 비밀번호 불일치 |
| `E005` | 403 | 승인된 신청에만 API 키 조회 가능 |
| `E016` | 400 | 결제 금액이 0 이하 |
| `E017` | 400 | 빌링키 누락 |
| `E020` | 409 | 결제 상태 오류 (취소 불가 상태 등) |
| `E022` | 409 | 이미 등록된 사업자번호 |
| `E023` | 409 | 동시 수정 충돌 |
| `E029` | 500 | 결제 접수 실패 (Tx1 실패) → `RETRY_PAYMENT` |
| `E030` | 500 | 승인 요청 실패 (Tx2 실패) → `RETRY_AUTHORIZE` |
| `E033` | 502 | 카드사 연동 오류 |
| `E034` | 400 | 만료된 카드 등록 세션 |
| `E036` | 400 | 결제 ID 형식 오류 |
| `E040` | 404 | 영수증을 찾을 수 없음 |
| `E042` | 409 | 승인 완료된 결제에만 영수증 발급 가능 |
| `E043` | 409 | 멱등성 키 충돌 (동일 키로 다른 요청) |
| `E044` | 400 | 멱등성 키 길이 오류 (1~128자) |
| `E999` | 500 | 서버 내부 오류 |

---

## 4. 가맹점 가입

### 4-1. 가맹점 신청

```
POST /api/merchant-applications
Content-Type: application/json
```

**Request**
```json
{
  "name": "테스트 쇼핑몰",
  "businessNumber": "123-45-67890",
  "phone": "010-1234-5678",
  "email": "merchant@example.com",
  "password": "password1234"
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| `name` | string | Y | - |
| `businessNumber` | string | Y | `XXX-XX-XXXXX` 형식 |
| `phone` | string | Y | - |
| `email` | string | Y | 이메일 형식 |
| `password` | string | Y | 8자 이상 |

**Response** `201 Created`
```json
{
  "applicationId": "app_abc123",
  "status": "PENDING"
}
```

---

### 4-2. 신청 상태 조회

```
POST /api/merchant-applications/business-number
Content-Type: application/json
```

**Request**
```json
{
  "businessNumber": "123-45-67890",
  "password": "password1234"
}
```

**Response** `200 OK`
```json
{
  "name": "테스트 쇼핑몰",
  "businessNumber": "123-45-67890",
  "status": "APPROVED",
  "rejectReason": null,
  "createdAt": "2025-01-01T10:00:00",
  "processedAt": "2025-01-02T09:00:00"
}
```

| `status` 값 | 설명 |
|------------|------|
| `PENDING` | 심사 대기 중 |
| `APPROVED` | 승인 완료 → API 키 발급 가능 |
| `REJECTED` | 거절 (`rejectReason` 확인) |
| `CANCELLED` | 신청 취소 |
| `SUBSCRIPTION_ENDED` | 구독 종료 |

---

### 4-3. 신청 취소

```
POST /api/merchant-applications/delete
Content-Type: application/json
```

**Request**
```json
{
  "businessNumber": "123-45-67890",
  "password": "password1234"
}
```

**Response** `204 No Content`

---

### 4-4. API 키/시크릿 조회

신청이 `APPROVED` 상태일 때만 조회 가능합니다.

```
POST /api/merchants/credentials
Content-Type: application/json
```

**Request**
```json
{
  "businessNumber": "123-45-67890",
  "password": "password1234"
}
```

**Response** `200 OK`
```json
{
  "apiKey": "pk_merchant_abc123",
  "apiSecret": "sk_merchant_xyz789"
}
```

---

## 5. 가맹점 관리

> 이하 모든 API는 `X-API-KEY`, `X-API-SECRET` 헤더 필요

### 5-1. API Secret 재발급

기존 Secret은 즉시 무효화됩니다.

```
POST /api/merchants/regenerate-secret
X-API-KEY: pk_merchant_...
X-API-SECRET: sk_merchant_...
```

**Response** `200 OK`
```json
{
  "apiKey": "pk_merchant_abc123",
  "apiSecret": "sk_merchant_NEW_xyz789"
}
```

---

### 5-2. 가맹점 탈퇴

탈퇴 후 해당 API 키로 인증 불가합니다.

```
DELETE /api/merchants
X-API-KEY: pk_merchant_...
X-API-SECRET: sk_merchant_...
```

**Response** `204 No Content`

---

## 6. 빌링키 등록 (카드 등록)

빌링키는 카드 정보를 직접 받지 않고 **브라우저 리다이렉트 플로우**로 발급합니다.

### 플로우

```
가맹점 서버          PG 서버                      카드사
    │                  │                           │
    │  사용자를 카드 등록 페이지로 리다이렉트          │
    │────────────────> │                           │
    │  GET /card-form/register?token=...           │
    │                  │ 카드사 선택 화면 표시         │
    │                  │                           │
    │                  │ POST /card-form/register/start
    │                  │──────────────────────────>│
    │                  │ 카드사 등록 페이지로 리다이렉트 │
    │                  │<──────────────────────────│
    │                  │                           │ 사용자 카드 입력
    │                  │ GET /card-form/callback/register?authCode=...
    │                  │<──────────────────────────│
    │                  │ 빌링키 발급                 │
    │  webhookUrl로 POST (빌링키 포함)               │
    │<─────────────────│                           │
    │  returnUrl로 리다이렉트                        │
    │<─────────────────│                           │
```

**시작 URL** (가맹점이 사용자 브라우저를 리다이렉트):
```
GET {PG_HOST}/card-form/register?token={등록세션토큰}
```

> `token` 생성 방식 및 `returnUrl`, `webhookUrl` 전달 방법은 PG사에 별도 문의

---

## 7. 결제

### 7-1. 결제 생성 (단일 API)

결제 생성과 카드사 승인 요청을 한 번에 처리합니다.
- **Tx1**: 결제 객체 생성 (`READY`) → 실패 시 `E029` 즉시 반환
- **Tx2**: 승인 요청 시작 (`AUTHORIZING`) → 실패 시 `E030` 반환
- **비동기**: 카드사 응답 후 `AUTHORIZED` 또는 `AUTHORIZE_FAILED` → 웹훅 발송

```
POST /api/payments
X-API-KEY: pk_merchant_...
X-API-SECRET: sk_merchant_...
Content-Type: application/json
Idempotency-Key: {고유값}     (선택)
```

**Request**
```json
{
  "amount": 50000,
  "merchantOrderId": "ORDER-20250101-001",
  "orderName": "상품명",
  "customerName": "홍길동",
  "customerEmail": "user@example.com",
  "callbackUrl": "http://your-server.com/webhook/payment",
  "billingKey": "b8f57589-4890-4260-b0a1-e7e7cd4d544c",
  "cardCompanyCode": "CARD_COMPANY_A"
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| `amount` | number | Y | 양의 정수 |
| `merchantOrderId` | string | Y | 최대 100자, 가맹점 내 고유값 권장 |
| `orderName` | string | Y | 최대 200자 |
| `customerName` | string | Y | 최대 100자 |
| `customerEmail` | string | N | 이메일 형식, 최대 200자 |
| `callbackUrl` | string | Y | 결제 결과 수신 웹훅 URL, 최대 500자 |
| `billingKey` | string | Y | 카드 등록 플로우에서 발급된 빌링키 |
| `cardCompanyCode` | string | Y | 카드사 코드, 최대 50자 |

**멱등성 처리** (`Idempotency-Key` 헤더):
동일 가맹점 + 동일 키 + 동일 요청 본문 지문이면 기존 `paymentId`를 그대로 반환합니다.
키 길이는 1~128자이며, 다른 내용의 요청에 동일 키를 재사용하면 `E043` 반환.

**Response** `201 Created`
```json
{
  "paymentId": "pay_abc123xyz"
}
```

최종 승인 결과는 `callbackUrl` 웹훅 또는 상태 폴링으로 확인합니다.

---

### 7-2. 결제 조회

```
GET /api/payments/{paymentId}
X-API-KEY: pk_merchant_...
X-API-SECRET: sk_merchant_...
```

**Response** `200 OK`
```json
{
  "paymentId": "pay_abc123xyz",
  "status": "AUTHORIZED",
  "merchantId": "merchant_001",
  "merchantOrderId": "ORDER-20250101-001",
  "amount": 50000,
  "orderName": "상품명",
  "customerEmail": "user@example.com",
  "customerName": "홍길동",
  "createdAt": "2025-01-01T10:00:00",
  "updatedAt": "2025-01-01T10:00:05",
  "lastFailureCategory": null,
  "lastFailureCode": null,
  "lastFailureMessage": null,
  "lastFailureAt": null
}
```

> 자신의 가맹점 결제만 조회 가능. 타 가맹점 결제 조회 시 `404` 반환.

---

### 7-3. 결제 취소 (환불)

`AUTHORIZED` 또는 `CANCEL_FAILED` 상태의 결제만 취소 가능합니다.
취소 요청은 즉시 수락(`202`)되며, 결과는 웹훅으로 수신합니다.

```
POST /api/payments/{paymentId}/cancel
X-API-KEY: pk_merchant_...
X-API-SECRET: sk_merchant_...
```

**Response** `202 Accepted`

최종 결과(`CANCELED` 또는 `CANCEL_FAILED`)는 결제 생성 시 등록한 `callbackUrl` 웹훅으로 수신합니다.

---

### 7-4. 승인 재시도 (Deprecated)

`E030` (`RETRY_AUTHORIZE`) 응답 시, 동일 결제의 승인만 재시도합니다.
`READY` 상태일 때만 호출 가능하며, 삭제 예정 API입니다.

```
POST /api/payments/{paymentId}/authorize
X-API-KEY: pk_merchant_...
X-API-SECRET: sk_merchant_...
Content-Type: application/json
```

**Request**
```json
{
  "billingKey": "b8f57589-4890-4260-b0a1-e7e7cd4d544c"
}
```

**Response** `202 Accepted`

---

## 8. 영수증

```
GET /api/receipt/{paymentId}/pdf
X-API-KEY: pk_merchant_...
X-API-SECRET: sk_merchant_...
```

**Response** `200 OK`
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="receipt-{paymentId}.pdf"
```

> `AUTHORIZED` 상태의 결제에만 발급 가능. 그 외 상태는 `E042` 반환.

---

## 9. 웹훅

PG 서버는 결제 상태 변경 시 가맹점이 등록한 `callbackUrl`로 POST 요청을 보냅니다.

### 9-1. 결제 상태 변경 웹훅

**Headers**
```
Content-Type: application/json
X-PG-Signature: {HMAC 서명값}
```

**Body**
```json
{
  "paymentId": "pay_abc123xyz",
  "status": "AUTHORIZED",
  "merchantId": "merchant_001",
  "merchantOrderId": "ORDER-20250101-001",
  "amount": 50000,
  "orderName": "상품명",
  "occurredAt": "2025-01-01T10:00:05",
  "idempotencyKey": "pay_abc123xyz:AUTHORIZED:2025-01-01T10:00:05",
  "lastFailureCategory": null,
  "lastFailureCode": null,
  "lastFailureMessage": null,
  "lastFailureAt": null
}
```

| `status` 값 | 설명 |
|------------|------|
| `AUTHORIZED` | 승인 완료 |
| `AUTHORIZE_FAILED` | 승인 실패 (`lastFailureCategory`, `lastFailureCode` 확인) |
| `CANCELED` | 취소(환불) 완료 |
| `CANCEL_FAILED` | 취소 실패 |

| `lastFailureCategory` | 설명 |
|----------------------|------|
| `BUSINESS` | 카드사 비즈니스 거절 (잔액 부족 등) → 재시도 불가 |
| `TECHNICAL` | 기술적 오류 (네트워크 등) → PG에서 자동 재시도 |

**웹훅 재시도**: 전송 실패 시 최대 4회, 지수 백오프(초기 1초, 최대 30초)로 재시도합니다.

---

### 9-2. 빌링키 등록 완료 웹훅

카드 등록 완료 시 가맹점 서버로 빌링키를 전달합니다.

**Body**
```json
{
  "eventType": "BILLING_KEY_REGISTERED",
  "merchantId": "merchant_001",
  "cardCompanyCode": "CARD_COMPANY_A",
  "billingKeyToken": "b8f57589-4890-4260-b0a1-e7e7cd4d544c",
  "cardBrand": "VISA",
  "cardNumberMasked": "4111-****-****-1111",
  "expiryMasked": "12/27",
  "occurredAt": "2025-01-01T10:00:00Z"
}
```

> `billingKeyToken`을 안전하게 서버에 저장 후 결제 API 호출 시 사용합니다.

---

## 10. 웹훅 서명 검증

PG 서버는 모든 웹훅 요청에 `X-PG-Signature` 헤더를 포함합니다.
가맹점 서버는 이 값을 직접 계산한 서명과 비교해 위변조 여부를 검증해야 합니다.

### 서명 생성 방식

```
HMAC-SHA256(웹훅 body 전체 JSON 문자열, apiSecret)
→ Base64 인코딩 (표준 인코딩)
→ X-PG-Signature 헤더
```

| 항목 | 값 |
|------|-----|
| 알고리즘 | HmacSHA256 |
| 서명 대상 | 웹훅 request body 전체 (JSON 문자열 그대로) |
| 키 | 가맹점 `apiSecret` (`sk_merchant_...`) |
| 인코딩 | Base64 표준 인코딩 |
| 문자셋 | UTF-8 |

### 검증 예제

**Java**
```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public boolean verifySignature(String requestBody, String receivedSignature, String apiSecret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] hash = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
    String expected = Base64.getEncoder().encodeToString(hash);
    return expected.equals(receivedSignature);
}
```

**Node.js**
```js
const crypto = require('crypto');

function verifySignature(requestBody, receivedSignature, apiSecret) {
  const expected = crypto
    .createHmac('sha256', apiSecret)
    .update(requestBody, 'utf8')
    .digest('base64');
  return crypto.timingSafeEqual(
    Buffer.from(expected),
    Buffer.from(receivedSignature)
  );
}
```

**Python**
```python
import hmac
import hashlib
import base64

def verify_signature(request_body: str, received_signature: str, api_secret: str) -> bool:
    expected = base64.b64encode(
        hmac.new(
            api_secret.encode('utf-8'),
            request_body.encode('utf-8'),
            hashlib.sha256
        ).digest()
    ).decode('utf-8')
    return hmac.compare_digest(expected, received_signature)
```

### 주의사항

- 서명 검증 시 **타이밍 공격 방지**를 위해 단순 문자열 비교(`==`) 대신 상수 시간 비교(`timingSafeEqual`, `compare_digest` 등)를 사용하세요.
- 서명 대상은 **HTTP request body 원문 바이트**입니다. JSON 파싱 후 재직렬화하면 키 순서가 달라져 서명 불일치가 발생합니다.
- `X-PG-Signature` 헤더가 없거나 검증에 실패한 요청은 반드시 `400` 또는 `401`로 거부하세요.

---

## 11. 결제 상태 머신

```
                     ┌─────────────────────────────────────┐
                     │            결제 생성 (POST /payments)  │
                     └──────────────────┬──────────────────┘
                                        │ Tx1 성공
                                     [READY]
                                        │ Tx2 성공
                                  [AUTHORIZING]
                          ┌─────────────┴─────────────┐
                          │                           │
                    [AUTHORIZED]             [AUTHORIZE_FAILED]
                          │
              POST /{paymentId}/cancel
                          │
                    [CANCELLING]
                ┌─────────┴──────────┐
                │                   │
           [CANCELED]        [CANCEL_FAILED]
                                    │ 기술 실패 → PG 자동 재시도
                              [CANCELLING] ──────> ...
```

| 상태 | 설명 |
|------|------|
| `READY` | 결제 객체 생성 완료, 승인 대기 |
| `AUTHORIZING` | 카드사 승인 요청 진행 중 |
| `AUTHORIZED` | 승인 완료 (취소 가능) |
| `AUTHORIZE_FAILED` | 승인 실패 (종료 상태) |
| `CANCELLING` | 취소 요청 진행 중 |
| `CANCELED` | 취소 완료 (종료 상태) |
| `CANCEL_FAILED` | 취소 실패 (기술 오류 시 PG 자동 재시도) |
