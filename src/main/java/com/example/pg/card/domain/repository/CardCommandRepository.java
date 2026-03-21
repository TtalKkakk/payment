package com.example.pg.card.domain.repository;

import java.util.Optional;

import com.example.pg.card.domain.aggregate.Card;
import com.example.pg.card.domain.vo.CardToken;

public interface CardCommandRepository {

    void save(Card card);

    Optional<Card> findByToken(CardToken token);
}

