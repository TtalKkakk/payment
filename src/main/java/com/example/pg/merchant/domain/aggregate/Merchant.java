package com.example.pg.merchant.domain.aggregate;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchant.domain.enumerate.MerchantStatus;
import com.example.pg.merchant.domain.vo.ApiKey;
import com.example.pg.merchant.domain.vo.ApiSecret;
import com.example.pg.merchant.domain.vo.MerchantName;
import com.example.pg.merchant.domain.converter.ApiKeyConverter;
import com.example.pg.merchant.domain.converter.ApiSecretConverter;
import com.example.pg.merchant.domain.converter.MerchantNameConverter;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant {

    @Getter
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    @Convert(converter = ApiKeyConverter.class)
    private ApiKey apiKey;

    @Column(nullable = false, length = 100)
    @Convert(converter = ApiSecretConverter.class)
    private ApiSecret apiSecret;

    @Column(nullable = false, length = 100)
    @Convert(converter = MerchantNameConverter.class)
    private MerchantName name;

    /** 신청서(application)와 1:1. null이면 관리자 직접 등록 등 신청 경로가 아닌 경우 */
    @Getter
    @Column(length = 36, name = "application_id")
    private String applicationId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private MerchantStatus status;

    public Merchant(String id, String apiKey, String apiSecret, String name, String applicationId, MerchantStatus status) {
        this.id = id;
        this.apiKey = new ApiKey(apiKey);
        this.apiSecret = new ApiSecret(apiSecret);
        this.name = MerchantName.of(name);
        this.applicationId = applicationId;
        this.status = status;
    }

    /** 신청 승인으로 생성 시 사용. name은 반드시 MerchantApplication의 이름(MerchantName)으로 전달하여 설계상 일치를 보장한다. */
    public static Merchant create(MerchantName name, String applicationId) {
        String id = UUID.randomUUID().toString();
        ApiKey apiKey = ApiKey.ofRandom();
        ApiSecret apiSecret = ApiSecret.ofRandom();
        return new Merchant(id, apiKey.value(), apiSecret.value(), name.value(), applicationId, MerchantStatus.ACTIVE);
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
        this.apiSecret = ApiSecret.ofRandom();
    }

    /**
     * 탈퇴(WITHDRAWN) 후 재승인 시 호출. ACTIVE로 되돌리고, name을 신청서와 동기화하며 apiKey·apiSecret을 새로 발급한다.
     * @param name 재승인된 MerchantApplication의 이름(MerchantName). Merchant.name = MerchantApplication.name 을 보장한다.
     */
    public void reactivate(MerchantName name) {
        if (this.status != MerchantStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.MERCHANT_CANNOT_REACTIVATE, this.status.name());
        }
        this.name = name;
        this.status = MerchantStatus.ACTIVE;
        this.apiKey = ApiKey.ofRandom();
        this.apiSecret = ApiSecret.ofRandom();
    }

    public boolean matchesSecret(String secret) {
        return this.apiSecret != null && this.apiSecret.matches(secret);
    }

    public String getApiKey() {
        return apiKey == null ? null : apiKey.value();
    }

    public String getApiSecret() {
        return apiSecret == null ? null : apiSecret.value();
    }

    public String getName() {
        return name == null ? null : name.value();
    }

}
