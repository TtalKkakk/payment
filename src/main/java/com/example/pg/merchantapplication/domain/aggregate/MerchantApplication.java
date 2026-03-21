package com.example.pg.merchantapplication.domain.aggregate;

import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.domain.vo.ApplicationName;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantApplication {

    public static final int REJECT_REASON_MAX_LENGTH = 500;

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20, name = "business_number")
    private String businessNumber;

    @Column(nullable = false, length = 50, name = "phone")
    private String contactPhone;

    @Column(nullable = false, length = 100, name = "email")
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MerchantApplicationStatus status;

    @Column(length = 500, name = "reject_reason")
    private String rejectReason;

    @Column(nullable = false, length = 60, name = "password_hash")
    private String passwordHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public MerchantApplication(String id, String name, String businessNumber,
                              String contactPhone, String contactEmail, String passwordHash) {
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
        this.version = 0L;
    }

    /**
     * 신청 애그리거트를 생성한다.
     * 검증은 Value Object(ApplicationName, BusinessNumber 등)에서 수행되므로, 여기서는 VO 값을 넣기만 한다.
     */
    public static MerchantApplication create(ApplicationName name, BusinessNumber businessNumber,
                                            ContactPhone contactPhone, ContactEmail contactEmail, PasswordHash passwordHash) {
        String id = UUID.randomUUID().toString();
        return new MerchantApplication(
                id,
                name.value(),
                businessNumber.value(),
                contactPhone.value(),
                contactEmail.value(),
                passwordHash.value()
        );
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
    public void reapply(ApplicationName name, ContactPhone contactPhone, ContactEmail contactEmail, PasswordHash passwordHash) {
        if (this.status != MerchantApplicationStatus.CANCELLED
                && this.status != MerchantApplicationStatus.SUBSCRIPTION_ENDED
                && this.status != MerchantApplicationStatus.REJECTED) {
            throw new BusinessException(ErrorCode.APPLICATION_ALREADY_PROCESSED, this.status.name());
        }
        this.name = name.value();
        this.contactPhone = contactPhone.value();
        this.contactEmail = contactEmail.value();
        this.passwordHash = passwordHash.value();
        this.status = MerchantApplicationStatus.PENDING;
        this.rejectReason = null;
        this.processedAt = null;
        this.updatedAt = LocalDateTime.now();
    }
}
