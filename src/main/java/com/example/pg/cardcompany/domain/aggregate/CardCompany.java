package com.example.pg.cardcompany.domain.aggregate;

import com.example.pg.cardcompany.domain.enumerate.CardCompanyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * PG에서 이용할 수 있는 카드사.
 * 카드 등록 선택 화면·연동 URL 등에 사용한다.
 */
@Entity
@Table(name = "card_companies", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardCompany {

    @Id
    @Column(length = 36)
    private String id;

    /** 카드사 식별 코드 (예: CARD_COMPANY_A). 노출용·연동 키로 사용 */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** 노출명 (예: 카드사 A) */
    @Column(nullable = false, length = 100)
    private String name;

    /** 연동 API Base URL (선택). HTTP 연동 시 사용 */
    @Column(length = 500, name = "base_url")
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private CardCompanyStatus status;

    /** 노출 순서 (작을수록 먼저) */
    @Column(nullable = false)
    private int displayOrder = 0;

    public CardCompany(String id, String code, String name, String baseUrl, CardCompanyStatus status, int displayOrder) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.baseUrl = baseUrl;
        this.status = status;
        this.displayOrder = displayOrder;
    }

    public static CardCompany create(String code, String name, String baseUrl) {
        String id = UUID.randomUUID().toString();
        return new CardCompany(id, code, name, baseUrl, CardCompanyStatus.ACTIVE, 0);
    }

    public boolean isActive() {
        return this.status == CardCompanyStatus.ACTIVE;
    }
}
