# payment

배달이요 PG(결제) 서버입니다.

## 서비스 사용 가이드 (가맹점/연동 개발자용)

### Base URL

- **운영 서버**: `http://13.209.84.21:8080`

이 문서의 모든 경로는 위 Base URL 기준입니다. (예: `POST http://13.209.84.21:8080/api/payments`)

### URL 구성

- **가맹점(서버-서버) API**: `/api/**`
- **브라우저 카드등록(UI) 플로우**: `/card-form/**`
- **관리자 UI**: `/admin/**`

### 가맹점 API 인증(공통)

대부분의 `/api/**`는 아래 헤더가 필요합니다.

- `X-API-KEY`
- `X-API-SECRET`

헤더 없이 호출 가능한 예외:

- `POST /api/merchant-applications` (가맹점 신청)
- `POST /api/merchant-applications/business-number` (신청 상태 조회)
- `POST /api/merchant-applications/delete` (신청 삭제)
- `POST /api/merchants/credentials` (사업자번호/비밀번호 기반 API 키 조회)

### 빠른 연동 플로우(권장 순서)

1) **가맹점 신청**: `POST /api/merchant-applications`  
2) **API 키/시크릿 조회**(신청 후): `POST /api/merchants/credentials`  
3) **빌링키 등록(UI)**: 가맹점 백엔드가 서명 토큰을 만들어 브라우저를 `GET /card-form/register?token=...`로 리다이렉트  
4) **빌링키 등록 완료 웹훅 수신(서버-서버)**: PG → 가맹점 `returnUrl`로 웹훅 `POST` + `X-PG-Signature`  
5) **결제 생성/승인 시작**: `POST /api/payments`  
6) **웹훅 수신 및 서명 검증**: PG → 가맹점 `callbackUrl`로 `POST` + `X-PG-Signature`

### 관리자 UI(운영자용)

- 로그인: `GET http://13.209.84.21:8080/admin/login`
- 관리자 UI: `http://13.209.84.21:8080/admin/**`

## Tech Stack

- Java 17
- Spring Boot 4.0.2
- Spring Security (관리자 로그인 + 가맹점 API 인증 필터)
- Spring Data JPA (MySQL)
- Spring Data Redis
- Thymeleaf (관리자/카드 등록 화면)

## Quick Start

### 1) 설정 파일 생성

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

### 2) 서버 실행

```bash
./gradlew bootRun
```

## URL/인증 규칙

> 이 섹션의 경로는 Base URL `http://13.209.84.21:8080` 기준으로 호출하면 됩니다.

### API Prefix

- **가맹점(서버-서버) API**: `/api/**`
- **브라우저 카드등록(UI) 플로우**: `/card-form/**`
- **관리자 UI**: `/admin/**`

주의: 실제 요청 경로는 **`/api/**` 규칙이 적용되도록** `WebMvcConfig`에서 `@RestController`에 한해 `/api` prefix를 자동 적용합니다.
즉, 문서에서 말하는 “가맹점(서버-서버) API”의 실제 경로는 **`/api/**`를 기준으로 보시면 됩니다.
반대로 `/card-form/**` 및 `/admin/**` 같은 HTML(UI) 라우트는 자동 prefix 대상이 아닙니다.

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

- **POST** `/api/merchant-applications`
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
  "applicationId": "application-uuid",
  "status": "PENDING"
}
```

#### 2) 신청 상태 조회(사업자번호+비밀번호)

- **POST** `/api/merchant-applications/business-number`
- **인증**: 없음(예외)

요청 예시:

```json
{
  "businessNumber": "123-45-67890",
  "password": "plain-password"
}
```

#### 3) 신청 삭제(사업자번호+비밀번호)

- **POST** `/api/merchant-applications/delete`
- **인증**: 없음(예외)
- **응답**: `204 No Content` (본문 없음)

요청 예시:

```json
{
  "businessNumber": "123-45-67890",
  "password": "plain-password"
}
```

### Merchant

#### 1) API 키/시크릿 조회(사업자번호+비밀번호)

- **POST** `/api/merchants/credentials`
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

- **POST** `/api/merchants/regenerate-secret`
- **인증**: 필요 (`X-API-KEY`, `X-API-SECRET`)

응답(200) 예시:

```json
{
  "apiKey": "pk_merchant_xxxxxxxxxxxxxxxxxxxxxxxx",
  "apiSecret": "sk_merchant_yyyyyyyyyyyyyyyyyyyyyyyy"
}
```

#### 3) 가맹점 삭제(API 키 폐기)

- **DELETE** `/api/merchants`
- **인증**: 필요 (`X-API-KEY`, `X-API-SECRET`)

### 카드 등록(UI) & Billing Key 교환(서버-서버)

#### 1) 카드사 선택 페이지(UI)

- **GET** `/card-form/register?token=...`
- **인증**: **token 필수** (가맹점 서버가 `apiSecret`로 서명한 토큰)

token payload(개념) 예시:

```json
{
  "apiKey": "pk_merchant_xxx",
  "returnUrl": "https://merchant.com/billing-key/complete",
  "webhookUrl": "https://merchant.com/api/pg/billing-key/webhook",
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
- `returnUrl`: 카드 등록 완료 후 **브라우저가** 돌아갈 가맹점 URL(완료 화면/다음 화면)
- `webhookUrl`: 카드 등록 완료 후 PG가 **서버-서버 웹훅(POST)** 을 보낼 가맹점 엔드포인트
- `iat`: 발급 시각(초 단위 epoch)
- `exp`: 만료 시각(초 단위 epoch, **짧게 권장: 1~5분**)
- `nonce`: 랜덤 문자열(재사용 방지)
- `purpose`: **반드시** `billing_key_register`

노출/보안 주의:
- token(payload)은 브라우저 URL에 포함되므로 노출될 수 있습니다. 대신 **`apiSecret`은 절대 노출되면 안 됩니다.**
- PG는 `nonce`를 1회성으로 처리하므로, 동일 token을 재사용하면 거부될 수 있습니다.

#### 2) 카드사 선택 후 등록 시작(UI)

- **POST** `/card-form/register/start`
- **인증**: token 필수(폼 hidden)

form fields:
- `cardCompanyCode`
- `token`

#### 3) 카드사 → PG 콜백(UI)

- **GET** `/card-form/callback/register?token={sessionToken}&authCode={authCode}`
- `sessionToken`은 PG가 카드사 이동 전에 Redis에 저장한 값(`cardRegisterSession:{uuid}` 형태의 키, **TTL 900초**)이며, 만료되거나 이미 사용·삭제된 토큰이면 오류가 난다.

#### 4) BillingKey 등록 완료 웹훅(서버-서버)

카드 등록 완료 후 PG는 토큰 payload의 `webhookUrl`로 **빌링키 등록 완료 웹훅**을 보냅니다.

- **Method**: `POST`
- **Destination**: 카드 등록 시작 시 token payload에 넣은 `webhookUrl`
- **Content-Type**: `application/json`
- **Signature Header**: `X-PG-Signature`

본문 스키마(예시):

```json
{
  "eventType": "BILLING_KEY_REGISTERED",
  "merchantId": "merchant-uuid",
  "cardCompanyCode": "CARD_COMPANY_A",
  "billingKeyToken": "bk_token_xxx",
  "cardBrand": "VISA",
  "cardNumberMasked": "****1234",
  "expiryMasked": "12/30",
  "occurredAt": "2026-02-28T12:34:56Z"
}
```

서명 검증 규칙은 결제 웹훅과 동일합니다.

- **알고리즘**: HMAC-SHA256
- **키**: 가맹점의 `apiSecret`
- **data**: 웹훅 요청의 **raw JSON body 문자열(UTF-8)**
- **signature 인코딩**: `Base64( HMAC_SHA256(apiSecret, rawBody) )`

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

7) **PG → 가맹점(완료 처리)**
- PG는 빌링키를 **서버-서버 웹훅**으로 전달한 뒤,
- 브라우저는 가맹점 `returnUrl`로 리다이렉트됩니다(토큰/code 없이).

### Payment

#### 공통 인증

- **헤더**: `X-API-KEY`, `X-API-SECRET` (필수)

#### 1) 결제 생성(단일 API: 생성 + 승인 요청 시작)

- **POST** `/api/payments`
- 성공 시 결제 엔티티가 생성되고 승인 플로우가 시작된다(비동기로 카드사 응답 후 최종 상태 확정).

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
  "cardCompanyCode": "CARD_COMPANY_A"
}
```

응답(201) 예시:

```json
{
  "paymentId": "payment-uuid"
}
```

#### 2) 결제 조회

- **GET** `/api/payments/{paymentId}`

#### 3) 같은 결제로 승인만 재시도(Tx2)

- **POST** `/api/payments/{paymentId}/authorize`

요청 예시:

```json
{
  "billingKey": "bk_token_xxx"
}
```

#### 4) 결제 취소(환불)

- **POST** `/api/payments/{paymentId}/cancel`

#### 5) 영수증 PDF 다운로드

- **GET** `/api/receipt/{paymentId}/pdf`
- **인증**: 필요 (`X-API-KEY`, `X-API-SECRET`), 해당 결제가 본 가맹점 소유일 때만 허용

#### Webhook (PG → 가맹점 callbackUrl)

PG는 결제 상태가 **`AUTHORIZE_FAILED`** 또는 **`CANCELED`**로 바뀐 뒤, 가맹점이 결제 생성 시 넣은 `callbackUrl`로 웹훅을 보낸다(트랜잭션 커밋 이후 비동기).

- **Method**: `POST`
- **Destination**: 결제 생성 요청의 `callbackUrl`
- **Content-Type**: `application/json`
- **Signature Header**: `X-PG-Signature`

##### 서명 검증 규칙

- **알고리즘**: HMAC-SHA256
- **키**: 가맹점의 `apiSecret` (PG에서 발급받은 값)
- **data**: 웹훅 요청의 **raw JSON body 문자열(UTF-8)**  
  (PG는 `PaymentWebhookDto`를 JSON으로 직렬화한 문자열 그대로 서명합니다.)
- **signature 인코딩**: `Base64( HMAC_SHA256(apiSecret, rawBody) )`

가맹점 서버는 `X-PG-Signature` 헤더 값을 위 규칙으로 재계산해 일치하는지 확인한 뒤 처리해야 합니다.

##### Webhook Payload 스키마

```json
{
  "paymentId": "payment-uuid",
  "status": "AUTHORIZE_FAILED",
  "merchantId": "merchant-uuid",
  "merchantOrderId": "ord-001",
  "amount": 10000,
  "orderName": "테스트 주문",
  "occurredAt": "2026-02-28T12:34:56"
}
```

`status`는 `PaymentStatus` enum 이름과 동일하다(예: `AUTHORIZE_FAILED`, `CANCELED`). `occurredAt`은 JSON 직렬화 설정에 따라 ISO-8601 문자열 등으로 내려간다.

##### 가맹점 처리 가이드(권장)

- **멱등 처리**: `paymentId + status` 기준으로 중복 웹훅이 와도 한 번만 처리
- **응답**: 서명 검증 성공 후 `200 OK` 반환
- **실패 처리**: 4xx/5xx 응답 시 PG가 재시도 정책을 가질 수 있으므로(향후), 일시적 오류는 재처리 가능하도록 구현 권장

## 테스트

```bash
./gradlew test
```


## 운영 가이드 (EC2 + Docker Compose 기준)

이 섹션은 “서버를 운영 환경에서 안정적으로 돌리기”를 목적으로 합니다. (배포, 설정 주입, 로그/모니터링, 장애 대응)

### 운영 아키텍처 개요

- **실행 형태**: `pg`(Spring Boot) + `mysql` + `redis`를 Docker Compose로 한 호스트(예: EC2)에서 구동
- **프로파일**: `SPRING_PROFILES_ACTIVE=prod`
- **로그 파일**: 운영 프로파일에서 `/var/log/pg/pg.log` (컨테이너 내부 경로)
- **헬스체크/관리**: Spring Boot Actuator 사용 (`/actuator/**`는 가맹점 인증 필터 제외)

### 필수 운영 환경변수(.env)

운영 배포에서는 `docker-compose.ec2.yml`이 `.env`를 읽습니다.

- **권장**: `.env`는 **서버(EC2)에만** 두고, Git에는 템플릿만 유지하세요.
- **템플릿**: 레포의 `.env.ec2`는 “운영용 값 채우기” 템플릿입니다. EC2 배포 디렉터리(예: `/home/ubuntu/pg-deploy/.env`)로 복사해 값만 채우는 방식이 가장 단순합니다.

핵심 변수(요약):

- **포트**: `PG_PORT` (기본 8080)
- **DB(MySQL)**: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- **Redis**: `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`
- **관리자 계정**: `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`
- **스키마 전략**: `SPRING_JPA_HIBERNATE_DDL_AUTO` (운영 기본은 `validate` 권장)
- **장기 재시도 워커**: `APP_RETRY_ENABLED=true|false` (운영에서 켤지 반드시 결정)

### EC2 1대 수동 배포 절차(Compose)

사전 준비(EC2):

- Docker / Docker Compose 설치
- 배포 디렉터리 생성 (예: `/home/ubuntu/pg-deploy`)
- `docker-compose.ec2.yml`과 `.env` 준비

배포(EC2에서 실행):

```bash
cd /home/ubuntu/pg-deploy

# 이미지 빌드 (Jenkins를 쓰면 보통 이 단계까지 자동화됩니다)
docker build -t pg-app:latest .

# 전체 기동
docker compose -f docker-compose.ec2.yml up -d

# 상태 확인
docker compose -f docker-compose.ec2.yml ps
```

롤링에 가까운 재기동(앱만):

```bash
docker compose -f docker-compose.ec2.yml up -d --no-deps pg
```

### Jenkins 배포 파이프라인(현재 레포 기준)

`Jenkinsfile`은 아래 흐름으로 배포합니다.

- `./gradlew clean bootJar -x test`로 jar 생성
- EC2로 `app.jar`, `Dockerfile`, `docker-compose.ec2.yml` 전송
- `.env`는 아래 중 하나로 전송
  - **권장**: Jenkins Credentials의 “Secret file”로 `.env`를 관리 후 EC2로 복사
  - 대안: 레포의 `.env.ec2`를 `.env`로 전송(운영 시크릿이 레포에 들어가지 않도록 주의)
- EC2에서 `docker build ... -t pg-app:latest .` 후 `docker compose ... up -d --no-deps pg`

운영에서는 `EC2_HOST`, `EC2_PATH`, Credential ID 들을 Jenkins 환경에 맞게 관리하세요.

### DB 스키마/마이그레이션(중요)

운영 프로파일(`application-prod.yml`)의 기본은 `ddl-auto: validate`입니다.

- **권장**: 운영은 `validate` 유지 + 별도의 마이그레이션 도구(Flyway/Liquibase) 또는 DBA 승인 스키마 변경 프로세스를 사용
- **예외(MVP/초기)**: 테이블 생성이 필요하면 일시적으로 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`로 올릴 수 있으나,
  - 변경 이력을 남기기 어렵고
  - 예기치 않은 DDL이 발생할 수 있어
  - **운영 장기 사용은 비권장**입니다.

### Redis 운영 포인트

- 카드 등록 플로우에서 세션 토큰을 Redis에 저장하며 TTL은 `APP_PAYMENT_CARD_REGISTER_SESSION_TTL_SECONDS`(기본 900초)로 제어됩니다.
- Redis는 AOF(`--appendonly yes`)로 구동됩니다. 디스크 용량/성능 모니터링을 하세요.

### “장기 재시도(워크 큐)” 운영 포인트

이 프로젝트는 DB에 저장되는 장기 재시도 Job과 워커(`RetryJobWorker`)를 제공합니다.

- **On/Off**: `APP_RETRY_ENABLED=true`일 때만 워커가 동작합니다(스케줄러 폴링).
- **동작 방식**: 주기적으로 due Job을 가져와 DB에서 claim(락) 후 처리하며, 실패 시 백오프 정책으로 다음 실행 시각을 재스케줄합니다.
- **수평 확장**: 워커는 DB 락/lease로 동시 처리 충돌을 피하는 구조입니다. 다만 운영에서는 다음을 모니터링하세요.
  - Job 적체(대기 건수 증가)
  - 평균 처리 시간 증가
  - DEAD 전환 비율(소진/만료)

### 웹훅 운영 포인트(가맹점 콜백)

- 결제 상태 변화 후 가맹점 `callbackUrl`로 웹훅을 POST하며, `X-PG-Signature`로 서명 검증이 가능합니다.
- 운영 권장:
  - 가맹점은 **멱등 처리**(예: `paymentId + status`)를 반드시 구현
  - 네트워크 오류/타임아웃을 고려해 재시도(가맹점 측) 또는 수신 실패 시 재처리 가능하게 설계

### 운영 체크리스트(권장)

- **시크릿 관리**: `.env`는 서버/시크릿 스토어에만, Git 커밋 금지
- **Actuator 보호**: 외부 노출 금지(보안그룹/리버스프록시 인증/사설망)
- **DB 백업/복구**: RPO/RTO 정의 및 정기 백업/복구 리허설
- **로그/모니터링**: 로그 로테이션 + 에러율/응답시간/DB 커넥션/Redis 메모리/재시도 적체 모니터링
- **스키마 변경**: 운영은 `validate` 기준으로 변경 프로세스 확립
