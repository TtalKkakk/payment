package com.example.pg.card_company.domain.aggergate;

import com.example.pg.card_company.domain.converter.BaseUrlConverter;
import com.example.pg.card_company.domain.converter.CardCompanyCodeConverter;
import com.example.pg.card_company.domain.converter.CardCompanyNameConverter;
import com.example.pg.card_company.domain.converter.DisplayOrderConverter;
import com.example.pg.card_company.domain.vo.BaseUrl;
import com.example.pg.card_company.domain.vo.CardCompanyCode;
import com.example.pg.card_company.domain.vo.CardCompanyName;
import com.example.pg.card_company.domain.vo.DisplayOrder;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.card_company.domain.enumerate.CardCompanyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PG가 연동하는 카드사.
 * 빌링키 발급 등 카드사 API 호출 시 baseUrl·전략(어댑터) 선택에 사용한다.
 */
@Entity
@Table(name = "card_companies", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardCompany {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    @Convert(converter = CardCompanyCodeConverter.class)
    private CardCompanyCode code;

    @Column(nullable = false, length = 100)
    @Convert(converter = CardCompanyNameConverter.class)
    private CardCompanyName name;

    @Column(length = 500, name = "base_url")
    @Convert(converter = BaseUrlConverter.class)
    private BaseUrl baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private CardCompanyStatus status;

    @Column(nullable = false)
    @Convert(converter = DisplayOrderConverter.class)
    private DisplayOrder displayOrder = new DisplayOrder(0);

    @OneToMany(mappedBy = "cardCompany", fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    public CardCompany(String id, String code, String name, String baseUrl, CardCompanyStatus status, int displayOrder) {
        this.id = id;
        this.code = new CardCompanyCode(code);
        this.name = new CardCompanyName(name);
        this.baseUrl = new BaseUrl(baseUrl);
        this.status = status;
        this.displayOrder = new DisplayOrder(displayOrder);
    }

    public static CardCompany create(String code, String name, String baseUrl) {
        String id = UUID.randomUUID().toString();
        return new CardCompany(id, code, name, baseUrl, CardCompanyStatus.ACTIVE, 0);
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code.value();
    }

    public String getName() {
        return name.value();
    }

    public BaseUrl getBaseUrl() {
        return baseUrl;
    }

    public CardCompanyStatus getStatus() {
        return status;
    }

    public int getDisplayOrder() {
        return displayOrder.value();
    }

    public boolean isActive() {
        return status == CardCompanyStatus.ACTIVE;
    }
}
