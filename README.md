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

`@RestController`에는 자동으로 `/api` prefix가 붙습니다.

예시:
- 코드상 `@RequestMapping("/payments")`
- 실제 경로 `/api/payments`

### 가맹점 API 인증

대부분의 `/api/**`는 아래 헤더가 필요합니다.

- `X-API-KEY`
- `X-API-SECRET`

필터 예외(헤더 없이 호출 가능):
- `POST /api/merchant-applications`
- `POST /api/merchant-applications/business-number`
- `POST /api/merchant-applications/delete`
- `POST /api/merchants/credentials`

### 관리자 페이지 인증

- 로그인 페이지: `GET /admin/login`
- 로그인 처리: `POST /admin/login` (form login)
- `/admin/**`는 관리자 계정 필요

## 주요 API

### Merchant Application

- `POST /api/merchant-applications` (신청)
- `POST /api/merchant-applications/business-number` (신청 상태 조회)
- `POST /api/merchant-applications/delete` (신청 삭제)

### Merchant

- `POST /api/merchants/credentials` (사업자번호/비밀번호 기반 API 키 조회)
- `POST /api/merchants/regenerate-secret` (API Secret 재발급)
- `DELETE /api/merchants` (가맹점 삭제)

### Card/Billing Key

- `POST /api/card-registration-sessions` (카드 등록 세션 생성)
- `GET /card/register?token=...` (카드 등록 폼)
- `POST /card/register` (카드 등록 완료 + 상점 redirect)
- `POST /api/billing-keys/exchange` (authCode -> billingKey 교환)

### Payment

- `POST /api/payments` (결제 생성)
- `GET /api/payments/{paymentId}` (결제 조회)
- `POST /api/payments/{paymentId}/authorize` (승인 시작)
- `POST /api/payments/{paymentId}/cancel` (취소)

## 테스트

```bash
./gradlew test
```

## Troubleshooting

- `Could not resolve placeholder 'app.admin.username'` : `application.yml`의 `app.admin.*` 누락
- `Communications link failure` : MySQL 미기동/접속정보 불일치
- `Redis connection refused` : Redis 미기동
