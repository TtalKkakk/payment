# payment 프론트엔드

PG(결제대행사) 백엔드(payment)와 연동되는 가맹점 포털 React 프론트엔드입니다.
배달의민족 디자인 시스템을 참고하여 구성하였습니다.

## 기술 스택

- React 18
- Vite 8
- React Router v6
- Axios

## 사전 준비

- Node.js 18 이상
- payment 백엔드 실행 중 (port **8081**)
- MySQL, Redis 실행 중

## 백엔드 실행

```bash
# payment 프로젝트 루트에서
./gradlew bootRun
```

`src/main/resources/application.yml` 이 없다면 아래를 참고하여 생성하세요.

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
  data:
    redis:
      host: localhost
      port: 6379

app:
  admin:
    username: admin
    password: admin1234
```

## 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 **http://localhost:5174** 접속

## 페이지 구성

| 경로 | 기능 |
|------|------|
| `/` | 가맹점 신청 |
| `/status` | 신청 상태 조회 |
| `/credentials` | API 키 발급 (로컬스토리지 자동 저장) |
| `/dashboard` | 결제 생성 / 결제 조회 / 결제 취소 |

## 인증 방식

가맹점 API 키(`X-API-KEY`, `X-API-SECRET`)는 `/credentials` 페이지에서 발급 후 브라우저 localStorage에 저장됩니다.
이후 모든 API 요청에 Axios interceptor를 통해 헤더가 자동 첨부됩니다.

## 연동 API (백엔드 자동 prefix: `/api`)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/merchant-applications` | 없음 | 가맹점 신청 |
| POST | `/api/merchant-applications/business-number` | 없음 | 신청 상태 조회 |
| POST | `/api/merchants/credentials` | 없음 | API 키 발급 |
| POST | `/api/payments` | API Key | 결제 생성 |
| GET | `/api/payments/{id}` | API Key | 결제 조회 |
| POST | `/api/payments/{id}/cancel` | API Key | 결제 취소 |
