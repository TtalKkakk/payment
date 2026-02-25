package com.example.pg.merchant.domain.aggregate;

import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import com.example.pg.merchant.domain.enumerate.MerchantStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "merchants",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "apiKey"),
                @UniqueConstraint(columnNames = "applicationId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String apiKey;

    @Column(nullable = false, length = 100)
    private String apiSecret;

    @Column(nullable = false, length = 100)
    private String name;

    /** 신청서(application)와 1:1. null이면 관리자 직접 등록 등 신청 경로가 아닌 경우 */
    @Column(length = 36, name = "application_id")
    private String applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private MerchantStatus status;

    public Merchant(String id, String apiKey, String apiSecret, String name, String applicationId, MerchantStatus status) {
        this.id = id;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.name = name;
        this.applicationId = applicationId;
        this.status = status;
    }

    /** 신청 승인으로 생성 시 사용. 재승인 시에는 사용하지 않고 기존 row를 reactivate 한다 */
    public static Merchant create(String name, String applicationId) {
        String id = UUID.randomUUID().toString();
        String apiKey = "pk_merchant_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String apiSecret = "sk_merchant_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return new Merchant(id, apiKey, apiSecret, name, applicationId, MerchantStatus.ACTIVE);
    }

    public void suspend() {
        if (this.status == MerchantStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.MERCHANT_CANNOT_SUSPEND);
        }
        this.status = MerchantStatus.SUSPENDED;
    }

    /** 정지 해제. SUSPENDED → ACTIVE */
    public void activate() {
        if (this.status != MerchantStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.MERCHANT_CANNOT_ACTIVATE);
        }
        this.status = MerchantStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = MerchantStatus.WITHDRAWN;
    }

    /**
     * 관리자용: 정지(SUSPENDED) 상태인 경우에만 탈퇴(WITHDRAWN)로 전환.
     * ACTIVE·이미 WITHDRAWN인 경우 예외.
     */
    public void withdrawFromSuspended() {
        if (this.status != MerchantStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.MERCHANT_CANNOT_WITHDRAW, this.status.name());
        }
        this.status = MerchantStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return this.status == MerchantStatus.ACTIVE;
    }

    public void regenerateSecret() {
        this.apiSecret = "sk_merchant_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * 탈퇴(WITHDRAWN) 후 재승인 시 호출. ACTIVE로 되돌리고 apiKey·apiSecret을 새로 발급해 기존 키를 무효화한다.
     */
    public void reactivate() {
        if (this.status != MerchantStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.MERCHANT_CANNOT_REACTIVATE, this.status.name());
        }
        this.status = MerchantStatus.ACTIVE;
        this.apiKey = "pk_merchant_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        this.apiSecret = "sk_merchant_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    public boolean matchesSecret(String secret) {
        return this.apiSecret.equals(secret);
    }
}
