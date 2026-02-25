package com.example.pg.card.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.example.pg.card.domain.aggregate.Card;
import com.example.pg.card.domain.repository.CardCommandRepository;
import com.example.pg.card.domain.vo.CardToken;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaCardCommandRepository implements CardCommandRepository {

    private final EntityManager em;

    @Override
    public void save(Card card) {
        em.persist(card);
    }

    @Override
    public Optional<Card> findByToken(CardToken token) {
        return em.createQuery("select c from Card c where c.token = :token", Card.class)
                .setParameter("token", token.getValue())
                .getResultStream()
                .findFirst();
    }
}

