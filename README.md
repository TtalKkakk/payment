# payment

배달이요 PG(결제) 서버입니다.

## Tech Stack

- Java 17
- Spring Boot 4.0.2
- Spring Security (관리자 로그인 + 가맹점 API 인증 필터)
- Spring Data JPA (MySQL)
- Spring Data Redis
- Thymeleaf (관리자/카드 등록 화면)

## Quick Start

### 1) 로컬 인프라 실행 (예시)

```bash
docker run -d --name payment-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=payment \
  -p 3306:3306 mysql:8.0

docker run -d --name payment-redis -p 6379:6379 redis:7
```

### 2) 설정 파일 생성

`src/main/resources/application.yml`

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payment?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
  data:
    redis:
      host: localhost
      port: 6379

app:
  admin:
    username: admin
    password: change-me
```

`app.admin.username`, `app.admin.password`는 필수입니다.

### 3) 서버 실행

```bash
./gradlew bootRun
```

## URL/인증 규칙

### API Prefix

- **가맹점(서버-서버) API**: `/api/**`
- **브라우저 카드등록(UI) 플로우**: `/card-form/**`
- **관리자 UI**: `/admin/**`

주의: `@RestController`에 자동 prefix가 붙는 구조가 아니라, **컨트롤러의 `@RequestMapping`이 곧 실제 경로**입니다.

### 가맹점 API 인증

대부분의 `/api/**`는 아래 헤더가 필요합니다.

- `X-API-KEY`
- `X-API-SECRET`

필터 예외(헤더 없이 호출 가능):
- `POST /api/merchant-applications` (신청)
- `POST /api/merchant-applications/business-number` (신청 상태 조회)
- `POST /api/merchant-applications/delete` (신청 삭제)
- `POST /api/merchants/credentials` (사업자번호/비밀번호 기반 API 키 조회)

### 관리자 페이지 인증

- 로그인 페이지: `GET /admin/login`
- 로그인 처리: `POST /admin/login` (form login)
- `/admin/**`는 관리자 계정 필요

## 주요 API

### Merchant Application (신청/조회/삭제)

#### 1) 신청 접수

- **POST** ` /api/merchant-applications`
- **인증**: 없음(예외)

요청 예시:

```json
{
  "name": "가맹점명",
  "businessNumber": "123-45-67890",
  "phone": "010-1234-5678",
  "email": "merchant@example.com",
  "password": "plain-password"
}
```

응답(201) 예시:

```json
{
  "id": "application-uuid",
  "status": "PENDED",
  "name": "가맹점명",
  "businessNumber": "123-45-67890",
  "createdAt": "2026-02-28T12:34:56"
}
```

#### 2) 신청 상태 조회(사업자번호+비밀번호)

- **POST** ` /api/merchant-applications/business-number`
- **인증**: 없음(예외)

요청 예시:

```json
{
  "businessNumber": "123-45-67890",
  "password": "plain-password"
}
```

#### 3) 신청 삭제(사업자번호+비밀번호)

- **POST** ` /api/merchant-applications/delete`
- **인증**: 없음(예외)

요청 예시:

```json
{
  "businessNumber": "123-45-67890",
  "password": "plain-password"
}
```

### Merchant

#### 1) API 키/시크릿 조회(사업자번호+비밀번호)

- **POST** ` /api/merchants/credentials`
- **인증**: 없음(예외)

요청 예시:

```json
{
  "businessNumber": "123-45-67890",
  "password": "plain-password"
}
```

응답(200) 예시:

```json
{
  "apiKey": "pk_merchant_xxxxxxxxxxxxxxxxxxxxxxxx",
  "apiSecret": "sk_merchant_xxxxxxxxxxxxxxxxxxxxxxxx"
}
```

#### 2) API Secret 재발급

- **POST** ` /api/merchants/regenerate-secret`
- **인증**: 필요 (`X-API-KEY`, `X-API-SECRET`)

응답(200) 예시:

```json
{
  "apiKey": "pk_merchant_xxxxxxxxxxxxxxxxxxxxxxxx",
  "apiSecret": "sk_merchant_yyyyyyyyyyyyyyyyyyyyyyyy"
}
```

#### 3) 가맹점 삭제(API 키 폐기)

- **DELETE** ` /api/merchants`
- **인증**: 필요 (`X-API-KEY`, `X-API-SECRET`)

### 카드 등록(UI) & Billing Key 교환(서버-서버)

#### 1) 카드사 선택 페이지(UI)

- **GET** ` /card-form/register?token=...`
- **인증**: **token 필수** (가맹점 서버가 `apiSecret`로 서명한 토큰)

token payload(개념) 예시:

```json
{
  "apiKey": "pk_merchant_xxx",
  "returnUrl": "https://merchant.com/billing-key/complete",
  "iat": 1730000000,
  "exp": 1730000300,
  "nonce": "random",
  "purpose": "billing_key_register"
}
```

##### 서명 방식(토큰 생성 규칙)

PG는 `/card-form/register`에서 token을 아래 규칙으로 검증합니다.

- **알고리즘**: HMAC-SHA256
- **서명 키**: 가맹점의 `apiSecret` (절대 브라우저에 노출 금지)
- **토큰 포맷**:  
  `token = base64url(payloadJson) + "." + base64url(hmac_sha256(apiSecret, base64url(payloadJson)))`
- **인코딩 규칙**:
  - `payloadJson`은 UTF-8 바이트로 직렬화
  - `base64url`은 URL-safe Base64이며 **padding("=") 없이** 사용

의사코드:

```text
payloadJson = JSON.stringify(payload)
payloadB64  = base64url(payloadJson)
sigRaw      = HMAC_SHA256(apiSecret, payloadB64)      // data는 payloadB64 문자열(UTF-8)
sigB64      = base64url(sigRaw)
token       = payloadB64 + "." + sigB64
```

필수 payload 필드:
- `apiKey`: 가맹점 식별자
- `returnUrl`: 카드 등록 완료 후 돌아갈 가맹점 URL
- `iat`: 발급 시각(초 단위 epoch)
- `exp`: 만료 시각(초 단위 epoch, **짧게 권장: 1~5분**)
- `nonce`: 랜덤 문자열(재사용 방지)
- `purpose`: **반드시** `billing_key_register`

노출/보안 주의:
- token(payload)은 브라우저 URL에 포함되므로 노출될 수 있습니다. 대신 **`apiSecret`은 절대 노출되면 안 됩니다.**
- PG는 `nonce`를 1회성으로 처리하므로, 동일 token을 재사용하면 거부될 수 있습니다.

#### 2) 카드사 선택 후 등록 시작(UI)

- **POST** ` /card-form/register/start`
- **인증**: token 필수(폼 hidden)

form fields:
- `cardCompanyCode`
- `token`

#### 3) 카드사 → PG 콜백(UI)

- **GET** ` /card-form/callback/register?token={sessionToken}&authCode={authCode}`
- `sessionToken`은 PG가 카드사 이동 전에 내부 세션 저장소(Redis)에 저장한 값이며 TTL 후 자동 만료됨.

#### 4) BillingKey 교환(서버-서버)

카드 등록 완료 후 PG는 가맹점 returnUrl로 `billingKeyToken`을 직접 전달하지 않고, **1회용 `code`만 전달**합니다.
가맹점 서버는 아래 API로 `code → billingKeyToken`을 교환해야 합니다.

- **POST** ` /api/billing-keys/exchange`
- **인증**: 필요 (`X-API-KEY`, `X-API-SECRET`)

요청 예시:

```json
{
  "code": "uuid-code"
}
```

응답(200) 예시:

```json
{
  "billingKeyToken": "bk_token_xxx",
  "cardCompanyCode": "SHINHAN"
}
```

#### 사용 파이프라인(가맹점/PG/카드사)

아래 흐름을 기준으로 가맹점 프론트/백엔드가 연동하면 됩니다.

1) **가맹점 프론트 → 가맹점 백엔드 (카드 등록 시작)**
- 사용자가 “카드 등록” 버튼 클릭
- 권장 구현:
  - 프론트가 가맹점 백엔드 엔드포인트로 이동(또는 API 호출)
  - 가맹점 백엔드가 **서명 토큰(token)** 생성 후 **302로 PG로 리다이렉트**

2) **가맹점 백엔드 → 브라우저 → PG (카드사 선택 페이지 요청)**
- 브라우저 이동:
  - `GET /card-form/register?token={token}`
- token payload에는 최소한 아래를 포함해야 합니다.
  - `apiKey` (가맹점 식별)
  - `returnUrl` (완료 후 가맹점으로 돌아갈 URL)
  - `exp` (짧은 만료, 예: 1~5분)
  - `nonce` (재사용 방지)
  - `purpose=billing_key_register`

3) **PG (token 검증 후 UI 렌더링)**
- PG는 `apiKey`로 Merchant 조회 후, 해당 Merchant의 `apiSecret`로 token 서명 검증
- 검증 실패 시 403

4) **브라우저 → PG (카드사 선택 후 등록 시작)**
- 사용자가 카드사를 선택하면:
  - `POST /card-form/register/start` (form submit)
  - form에는 `token`이 hidden으로 포함됩니다.

5) **PG → 카드사 (등록 세션 생성) + 브라우저 리다이렉트**
- PG가 카드사에 등록 세션 생성 API를 호출하고,
- 카드사 등록 페이지로 브라우저를 리다이렉트합니다.

6) **카드사 → PG (등록 완료 콜백)**
- 카드사는 PG 콜백 URL로 이동:
  - `GET /card-form/callback/register?token={sessionToken}&authCode={authCode}`

7) **PG → 가맹점 프론트 (완료 리다이렉트: code만 전달)**
- PG는 billingKeyToken을 브라우저에 노출하지 않기 위해,
  - 가맹점 `returnUrl`로 **1회용 `code`만** 전달합니다:
  - `302 {returnUrl}?code={code}`

8) **가맹점 백엔드 → PG (code 교환: 서버-서버)**
- 가맹점 백엔드는 프론트로부터 `code`를 전달받아,
  - `POST /api/billing-keys/exchange` 를 호출해 `billingKeyToken`을 교환합니다.
- 교환 성공 후, 가맹점 백엔드는 billingKeyToken을 저장하고 이후 결제 승인에 사용합니다.

### Payment

#### 공통 인증

- **헤더**: `X-API-KEY`, `X-API-SECRET` (필수)

#### 1) 결제 생성(READY)

- **POST** ` /api/payments`

요청 예시:

```json
{
  "amount": 10000,
  "merchantOrderId": "ord-001",
  "orderName": "테스트 주문",
  "customerEmail": "test@example.com",
  "customerName": "홍길동",
  "callbackUrl": "https://merchant.com/payment/webhook",
  "billingKey": "bk_token_xxx",
  "cardCompanyCode": "SHINHAN"
}
```

응답(201) 예시:

```json
{
  "paymentId": "payment-uuid"
}
```

#### 2) 결제 조회

- **GET** ` /api/payments/{paymentId}`

#### 3) 같은 결제로 승인만 재시도(Tx2)

- **POST** ` /api/payments/{paymentId}/authorize`

요청 예시:

```json
{
  "billingKey": "bk_token_xxx"
}
```

#### 4) 결제 취소(환불)

- **POST** ` /api/payments/{paymentId}/cancel`

#### 5) 영수증 PDF 다운로드

- **GET** ` /api/payments/{paymentId}/receipt/pdf`

## 테스트

```bash
./gradlew test
```

## Troubleshooting

- `Could not resolve placeholder 'app.admin.username'` : `application.yml`의 `app.admin.*` 누락
- `Communications link failure` : MySQL 미기동/접속정보 불일치
- `Redis connection refused` : Redis 미기동
