package com.example.pg.card.domain.aggregate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.example.pg.card.domain.vo.CardToken;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(nullable = false, length = 32)
    private String ownerId;

    @Column(nullable = false, length = 32)
    private String maskedNumber;

    @Column(length = 32)
    private String cardCompany;

    private LocalDateTime createdAt;

    public Card(CardToken token, String ownerId, String maskedNumber, String cardCompany) {
        this.token = token.getValue();
        this.ownerId = ownerId;
        this.maskedNumber = maskedNumber;
        this.cardCompany = cardCompany;
        this.createdAt = LocalDateTime.now();
    }

    public CardToken getCardToken() {
        return CardToken.from(token);
    }
}

