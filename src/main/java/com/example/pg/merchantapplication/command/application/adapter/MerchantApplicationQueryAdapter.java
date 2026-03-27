package com.example.pg.merchantapplication.command.application.adapter;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
import com.example.pg.merchantapplication.presentation.port.MerchantApplicationQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantApplicationQueryAdapter implements MerchantApplicationQueryPort {

    private final MerchantApplicationRepository merchantApplicationRepository;

    @Override
    public MerchantApplication getByBusinessNumber(String businessNumber) {
        log.debug("[MerchantApplication] QueryPort getByBusinessNumber businessNumber={}", businessNumber);
        return merchantApplicationRepository.findByBusinessNumber(businessNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_PASSWORD_MISMATCH));
    }
}
