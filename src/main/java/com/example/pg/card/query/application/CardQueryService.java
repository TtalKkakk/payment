package com.example.pg.card.query.application;

import com.example.pg.card.domain.aggregate.CardRegistrationToken;
import com.example.pg.card.domain.repository.CardRegistrationTokenRepository;
import com.example.pg.exception.BusinessException;
import com.example.pg.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드 도메인 조회 전용 (CQRS - Query).
 * 상태를 변경하지 않는 읽기·검증만 수행한다.
 */
@Service
@RequiredArgsConstructor
public class CardQueryService {

    private final CardRegistrationTokenRepository tokenRepository;

    /**
     * 카드 등록 폼 진입 전 토큰 유효성 검증.
     * 토큰이 없거나 유효하지 않으면 예외를 던진다. 만료된 토큰은 CARD_REGISTRATION_TOKEN_EXPIRED로 구분한다.
     */
    @Transactional(readOnly = true)
    public void validateToken(String token) {
        CardRegistrationToken tokenEntity = tokenRepository.findById(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_REGISTRATION_TOKEN_INVALID, token));
        if (tokenEntity.isExpired()) {
            throw new BusinessException(ErrorCode.CARD_REGISTRATION_TOKEN_EXPIRED);
        }
    }
}
