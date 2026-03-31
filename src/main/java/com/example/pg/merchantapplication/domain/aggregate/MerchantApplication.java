package com.example.pg.merchantapplication.domain.aggregate;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.domain.converter.MerchantNameConverter;
import com.example.pg.merchant.domain.vo.MerchantName;
import com.example.pg.merchantapplication.domain.converter.BusinessNumberConverter;
import com.example.pg.merchantapplication.domain.converter.ContactEmailConverter;
import com.example.pg.merchantapplication.domain.converter.ContactPhoneConverter;
import com.example.pg.merchantapplication.domain.converter.PasswordHashConverter;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.domain.vo.BusinessNumber;
import com.example.pg.merchantapplication.domain.vo.ContactEmail;
import com.example.pg.merchantapplication.domain.vo.ContactPhone;
import com.example.pg.merchantapplication.domain.vo.PasswordHash;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "merchant_applications",
        uniqueConstraints = @UniqueConstraint(columnNames = "business_number")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantApplication {

    public static final int REJECT_REASON_MAX_LENGTH = 500;

    @Getter
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    @Convert(converter = MerchantNameConverter.class)
    private MerchantName name;

    @Column(nullable = false, length = 20, name = "business_number")
    @Convert(converter = BusinessNumberConverter.class)
    private BusinessNumber businessNumber;

    @Column(nullable = false, length = 50, name = "phone")
    @Convert(converter = ContactPhoneConverter.class)
    private ContactPhone contactPhone;

    @Column(nullable = false, length = 100, name = "email")
    @Convert(converter = ContactEmailConverter.class)
    private ContactEmail contactEmail;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MerchantApplicationStatus status;

    @Getter
    @Column(length = 500, name = "reject_reason")
    private String rejectReason;

    @Column(nullable = false, length = 60, name = "password_hash")
    @Convert(converter = PasswordHashConverter.class)
    private PasswordHash passwordHash;

    @Getter
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Getter
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Getter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Getter
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public MerchantApplication(
            String id,
            MerchantName name,
            BusinessNumber businessNumber,
            ContactPhone contactPhone,
            ContactEmail contactEmail,
            PasswordHash passwordHash
    ) {
        this.id = id;
        this.name = name;
        this.businessNumber = businessNumber;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.passwordHash = passwordHash;
        this.status = MerchantApplicationStatus.PENDING;
        this.rejectReason = null;
        this.createdAt = LocalDateTime.now();
        this.processedAt = null;
        this.updatedAt = LocalDateTime.now();
        this.version = (id == null) ? null : 0L;
    }

    /**
     * persist 직전에 id·version이 없으면 채운다.
     * 신규 엔티티는 id=null로 두어 Spring Data JPA가 persist()를 호출하도록 하고, INSERT 직전에 여기서 id를 부여한다.
     */
    @PrePersist
    void generateIdIfNew() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }

    /**
     * 신청 애그리거트를 생성한다.
     * name은 MerchantName을 사용하여 Merchant 도메인과 설계상 동일한 타입으로 일치시킨다.
     */
    public static MerchantApplication create(
            MerchantName name,
            BusinessNumber businessNumber,
            ContactPhone contactPhone,
            ContactEmail contactEmail,
            PasswordHash passwordHash
    ) {
        return new MerchantApplication(null, name, businessNumber, contactPhone, contactEmail, passwordHash);
    }

    /**
     * 신청을 승인한다.
     * @throws BusinessException APPLICATION_ALREADY_PROCESSED 이미 처리된 경우
     */
    public void approve() {
        if (this.status != MerchantApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED, this.status.name());
        }
        this.status = MerchantApplicationStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 신청을 거절한다.
     *
     * @param reason 거절 사유 (null 가능, 500자 초과 시 예외)
     * @throws BusinessException APPLICATION_ALREADY_PROCESSED 이미 처리된 경우
     * @throws BusinessException REJECT_REASON_TOO_LONG 거절 사유가 500자 초과인 경우
     */
    public void reject(String reason) {
        if (this.status != MerchantApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED, this.status.name());
        }
        if (reason != null && reason.length() > REJECT_REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.REJECT_REASON_TOO_LONG);
        }
        this.status = MerchantApplicationStatus.REJECTED;
        this.rejectReason = reason;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == MerchantApplicationStatus.PENDING;
    }

    /**
     * Merchant 생성/재활성화를 위한 사전 조건 검증.
     * 신청 상태가 APPROVED가 아니면 예외를 던진다.
     */
    public void assertApproved() {
        if (this.status != MerchantApplicationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_APPROVED);
        }
    }

    /**
     * 신청을 취소한다 (심사 전 사용자 철회). 상태를 CANCELLED로 변경.
     * @throws BusinessException APPLICATION_ALREADY_PROCESSED PENDING이 아닌 경우
     */
    public void cancel() {
        if (this.status != MerchantApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED, this.status.name());
        }
        this.status = MerchantApplicationStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 구독 종료로 전이. 승인(APPROVED) 상태에서 구독 취소 시 호출.
     * @throws BusinessException APPLICATION_ALREADY_PROCESSED APPROVED가 아닌 경우
     */
    public void subscriptionEnded() {
        if (this.status != MerchantApplicationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED, this.status.name());
        }
        this.status = MerchantApplicationStatus.SUBSCRIPTION_ENDED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재신청으로 되살린다. CANCELLED / SUBSCRIPTION_ENDED / REJECTED 일 때만 가능.
     * 기존 id·business_number·created_at 유지, 나머지 갱신 후 PENDING으로 전이.
     */
    public void reapply(
            MerchantName name,
            ContactPhone contactPhone,
            ContactEmail contactEmail,
            PasswordHash passwordHash
    ) {
        if (this.status != MerchantApplicationStatus.CANCELLED
                && this.status != MerchantApplicationStatus.SUBSCRIPTION_ENDED
                && this.status != MerchantApplicationStatus.REJECTED) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED, this.status.name());
        }
        this.name = name;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.passwordHash = passwordHash;
        this.status = MerchantApplicationStatus.PENDING;
        this.rejectReason = null;
        this.processedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** Thymeleaf·이벤트·API 응답용. */
    public String getName() {
        return name == null ? null : name.value();
    }

    public String getBusinessNumber() {
        return businessNumber == null ? null : businessNumber.value();
    }

    public String getContactPhone() {
        return contactPhone == null ? null : contactPhone.value();
    }

    public String getContactEmail() {
        return contactEmail == null ? null : contactEmail.value();
    }

    public String getPasswordHash() {
        return passwordHash == null ? null : passwordHash.value();
    }

    /**
     * 승인 시 Merchant 생성에 전달할 이름. Merchant.name = MerchantApplication.name 을 설계상 보장하기 위해 동일 타입(MerchantName)을 반환한다.
     */
    public MerchantName getMerchantName() {
        if (this.name == null) {
            throw new IllegalStateException("merchant application name must be set");
        }
        return this.name;
    }
}
