# 트러블슈팅: 가맹점 신청(POST) 시 409 Optimistic Lock 및 DB 미저장

## 1. 문제 정의

### 1.1 현상

- **API**: `POST /api/merchant-applications` (가맹점 신청 접수)
- **조건**: 요청 DTO에 맞춰 body를 입력하여 **최초 신청**(해당 사업자번호로 DB에 기존 신청 건 없음)으로 한 번만 호출
- **결과**:
  - HTTP **409 Conflict** 응답
  - 로그에 `OptimisticLockFailureException` (또는 `ObjectOptimisticLockingFailureException`) 발생
  - 에러 메시지: `Row was already updated or deleted by another transaction for entity [MerchantApplication with id '...']`
  - **DB에는 해당 요청으로 저장된 레코드가 존재하지 않음**

### 1.2 기대 동작

- 최초 신청 시 **201 Created** 반환 및 `merchant_applications` 테이블에 **1건 INSERT**되어 입력한 신청 데이터가 저장됨.

### 1.3 영향

- 신규 가맹점이 사업자번호로 최초 신청을 해도 데이터가 저장되지 않고 409만 반환되어, **신청 접수 자체가 불가능**한 상태가 됨.

---

## 2. 원인

### 2.1 직접 원인: 새 엔티티에 대해 `persist()`가 아닌 `merge()`가 호출됨

- `MerchantApplication`은 **생성 시점에 이미 `id`와 `version`을 세팅**함.
  - `MerchantApplication.create()` 내부에서 `id = UUID.randomUUID().toString()`, 생성자에서 `version = 0L` 할당.
- Spring Data JPA의 `save()`는 **엔티티가 “신규”인지 여부**에 따라 동작을 나눔.
  - **신규로 판단** → `EntityManager.persist()` 호출 → **INSERT** 실행.
  - **신규가 아니로 판단** → `EntityManager.merge()` 호출 → 기존 행 갱신 가정 하에 **SELECT/UPDATE** 경로로 동작.
- “신규” 판단은 기본적으로 **식별자(id)가 null인지**로 이루어짐.
  - `MerchantApplication`은 **객체 생성 시점부터 id가 null이 아님**.
  - 따라서 **최초 신청으로 만든 새 객체도 “신규가 아니다”로 간주**되어, `save()` 시 **항상 `merge()`가 호출**됨.

### 2.2 `merge()` 동작으로 인한 예외

- `merge()`는 “**이미 DB에 존재하는 엔티티를 수정한다**”는 전제로 동작함.
- 이번 요청에서는 해당 `id`로 **DB에 행이 존재하지 않음**(최초 신청이므로 INSERT가 되어야 하는 상황).
- 이 상태에서 `merge()`가 **UPDATE**를 시도하면, `WHERE id = ? AND version = ?` 조건에 맞는 행이 **0건**이 됨.
- `@Version`이 있는 엔티티에서 **0 rows updated**는 “다른 트랜잭션에 의해 이미 수정되었거나 삭제됨”으로 해석되어 **낙관적 락(Optimistic Lock) 위반**으로 처리됨.
- 그 결과 **OptimisticLockFailureException**이 발생하고, 전역 예외 처리에서 409 Conflict로 매핑됨.

### 2.3 INSERT가 수행되지 않는 이유

- `persist()`는 호출되지 않고, **`merge()` 경로만 타면서 UPDATE만 시도**됨.
- INSERT 쿼리는 **한 번도 실행되지 않으므로**, DB에는 사용자가 입력한 DTO에 해당하는 **레코드가 생성되지 않음**.

### 2.4 요약

| 구분 | 내용 |
|------|------|
| **근본 원인** | 애플리케이션에서 id를 부여한 **새 엔티티**가 Spring Data JPA에 의해 **신규가 아닌 엔티티**로 판단됨. |
| **결과적 동작** | `save()` → `merge()` 호출 → 존재하지 않는 행에 대한 UPDATE 시도 → 0 rows updated → 낙관적 락 예외. |
| **DB 상태** | INSERT 미발생으로, 해당 신청 데이터가 DB에 없음. |

---

## 3. 해결 방안

### 3.1 채택한 방법: 신규 엔티티는 id를 할당하지 않음

**요지**: 새로 만드는 엔티티는 생성 시점에 **id를 넣지 않고 null로 두고**, INSERT 직전에만 id를 부여한다. 그러면 Spring Data JPA가 엔티티를 “신규”로 인식해 `persist()`를 호출하고, INSERT가 정상적으로 수행된다.

### 3.2 구현 내용

1. **`MerchantApplication.create()`**
   - 기존: `id = UUID.randomUUID().toString()` 후 생성자에 id 전달.
   - 변경: id를 전달하지 않고 **`null`**을 넘김.  
     → `new MerchantApplication(null, name.value(), businessNumber.value(), ...)`

2. **생성자**
   - `id`가 null이면 **`version`도 null**로 둠.  
     → `this.version = (id == null) ? null : 0L;`  
   - 영속화 전까지 id·version이 없으므로 “신규” 판단이 유지됨.

3. **`@PrePersist`**
   - persist 직전에 JPA가 호출하는 콜백에서 id·version이 없을 때만 채움.  
     - `id == null` → `id = UUID.randomUUID().toString()`  
     - `version == null` → `version = 0L`  
   - DB에 INSERT되는 시점에는 id와 version이 세팅된 상태가 됨.

### 3.3 결과

- **신규 신청**: `create()`로 만든 객체는 id=null → `save()` 시 `persist()` 호출 → `@PrePersist`에서 id 부여 → **INSERT 정상 수행** → 201 응답 및 DB에 1건 저장.
- **재신청(reapply)**: DB에서 조회한 기존 엔티티는 이미 id·version이 있음 → `save()` 시 `merge()`/dirty checking → 기존대로 UPDATE만 수행.
- 409 Optimistic Lock 및 “DB에 레코드 없음” 현상 해소.

### 3.4 참고: 다른 해결 방법

| 방법 | 설명 |
|------|------|
| **Persistable 구현** | `Persistable<String>` 구현 후 `isNew()`를 직접 정의. id를 미리 넣어도 “신규”일 때만 true 반환하면 persist() 호출됨. |
| **신규일 때만 persist()** | 서비스에서 최초 신청 분기 시 `repository.save()` 대신 `EntityManager.persist()`만 호출. |
