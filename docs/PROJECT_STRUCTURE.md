# pg 프로젝트 구조 요약

- **스택**: Spring Boot 4.0.2, Java 17, Gradle, MySQL, Redis, JPA, Thymeleaf, Security
- **패키지**: `com.example.pg` — common, merchant, merchantapplication, payment, receipt

---

## common

- **exception**: `BusinessException`, `ErrorCode`(E001 등), `ErrorResponse`, `FieldErrorDetail`
- **config**: `SecurityConfig`, `HttpClientConfig`(RestTemplate), `WebMvcConfig`, `RedisConfig`, `PasswordEncoderConfig`, `AdminUserDetailsConfig`, `GlobalExceptionAdvice`, `MerchantAuthFilter`
- **util**: `HttpOutbound` — `post(url, bodyJson, extraHeaders)`, `postForObject(url, requestBody, responseType)` (카드사·웹훅 공용)

---

## merchant

- **도메인**: `Merchant` 애그리거트, VO `MerchantName`, `ApiKey`, `ApiSecret`
- **포트**: `MerchantPort` — `createFromApprovedApplication(applicationId, MerchantName)`, `findApprovedApplicationIdByBusinessNumberAndPassword`, `getMerchantName`
- **구현**: `MerchantPortAdapter` (command/application/adapter)
- **서비스**: `MerchantService`, `MerchantQueryService`
- **진입점**: `MerchantController`, `MerchantAdminController`
- **이벤트**: `MerchantEventListener` — `MerchantSuspendedEvent`, `MerchantActivatedEvent`, `MerchantDeletedEvent`, `MerchantSecretRegeneratedEvent`

---

## merchantapplication

- **도메인**: `MerchantApplication` 애그리거트, VO `BusinessNumber`, `ContactEmail`, `ContactPhone`, `PasswordHash`, (deprecated) `ApplicationName`
- **포트**: `MerchantApplicationPort` — `createFromApprovedApplication(applicationId, MerchantName)`, `markSubscriptionEnded(applicationId)`
- **구현**: `MerchantApplicationPortAdapter`
- **서비스**: `MerchantApplicationService` (submit, approve, reject, cancel, deleteByBusinessNumberAndPassword), `MerchantApplicationQueryService`
- **진입점**: `MerchantApplicationController`, `MerchantApplicationAdminController`
- **이벤트**: `MerchantApplicationEventListener` — Submitted, Approved, Rejected, Cancelled, SubscriptionEnded, Pended 등

---

## payment

- **아웃바운드(Connect)**:  
  - `CardCompanyConnect` — approve, createRegistrationSession, issueBillingKey, requestRefund  
  - `FranchiseConnect` — send(url, bodyJson, signatureValue) 웹훅
- **구현**: `HttpCardCompanyConnectImpl`, `HttpWebhookSenderConnectImpl` (둘 다 `common.util.HttpOutbound` 사용)
- **레지스트리**: `CardCompanyPortRegistry` (payment/domain/repository) — ACTIVE 카드사별 `CardCompanyConnect` 등록
- **서비스**: `PaymentService` (createPayment, startAuthorization, cancelPayment, sendWebhook), `PaymentAuthorizationProcessor`, `BillingKeyService`, `PaymentQueryService`, `BillingKeyQueryService`
- **포트(영수증용)**: `PaymentPort` — existsPayment, findAuthorizedPayment
- **구현**: `PaymentAdapter` (command/application/adapter)
- **진입점**: `PaymentController`, `CardCompanyRegistrationController`
- **이벤트**: `PaymentDomainEventListener` — AuthorizationStarted, PaymentCreated, PaymentStatusChanged 시 웹훅 등

---

## receipt

- **도메인**: `Receipt` 애그리거트, `ReceiptId`, `ReceiptStatus`
- **포트**: `ReceiptPdfPort` (PDF 생성)
- **구현**: `ReceiptPdfAdapter`
- **서비스**: `ReceiptService`, `ReceiptQueryService`, `ReceiptPdfService`
- **리스너**: `ReceiptOnPaymentApprovedListener` (승인 시 영수증 발급)

---

## 설정·기타

- **application.yml**: port 8080, MySQL, Redis, JPA, logging.file.name=logs/pg.log
- **테스트**: JUnit 5, Mockito, excludeTags 'Integration', JaCoCo 커버리지
- **엔트리**: `PgApplication` (@SpringBootApplication)
