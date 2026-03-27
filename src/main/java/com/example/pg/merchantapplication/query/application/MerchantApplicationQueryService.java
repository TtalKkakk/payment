package com.example.pg.merchantapplication.query.application;

import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import com.example.pg.merchantapplication.infrastructure.persistence.MerchantApplicationRepository;
import com.example.pg.merchantapplication.query.application.dto.PagedApplicationsResultDto;
import com.example.pg.merchantapplication.query.application.dto.SearchType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantApplicationQueryService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int PAGE_BLOCK_SIZE = 10;

    private final MerchantApplicationRepository merchantApplicationRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ID로 조회. 없으면 BusinessException.
     */
    @Transactional(readOnly = true)
    public MerchantApplication findById(String id) {
        log.debug("[MerchantApplication] Query findById applicationId={}", id);
        return merchantApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_APPLICATION_NOT_FOUND, id));
    }

    /**
     * 사업자번호 + 비밀번호로 심사 상세 조회.
     */
    @Transactional(readOnly = true)
    public MerchantApplication getByBusinessNumberAndPassword(String businessNumber, String rawPassword) {
        log.debug("[MerchantApplication] Query getByBusinessNumber businessNumber={}", businessNumber);
        MerchantApplication merchantApplication = merchantApplicationRepository.findByBusinessNumber(businessNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_PASSWORD_MISMATCH));
        if (!passwordEncoder.matches(rawPassword, merchantApplication.getPasswordHash())) {
            throw new BusinessException(ErrorCode.APPLICATION_PASSWORD_MISMATCH);
        }
        if (merchantApplication.getStatus() != MerchantApplicationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_APPROVED);
        }
        return merchantApplication;
    }

    /**
     * 페이지네이션된 신청 목록 + 블록 정보 반환.
     * status: 필터 (null/blank/잘못된 값이면 전체)
     * businessNumber: 사업자번호 부분 검색 (null/blank면 검색 안 함)
     */
    @Transactional(readOnly = true)
    public PagedApplicationsResultDto findPaged(String status, String businessNumber, int pageNumber) {
        log.debug("[MerchantApplication] Query findPaged status={} businessNumber={} page={}", status, businessNumber, pageNumber);
        SearchType key = SearchType.from(status, businessNumber);
        Pageable pageable = PageRequest.of(pageNumber, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MerchantApplication> page = search(key, status, businessNumber, pageable);
        return PagedApplicationsResultDto.of(page, PAGE_BLOCK_SIZE);
    }

    private static final List<MerchantApplicationStatus> EXCLUDED_FROM_DEFAULT_LIST =
            List.of(MerchantApplicationStatus.CANCELLED, MerchantApplicationStatus.SUBSCRIPTION_ENDED);

    private Page<MerchantApplication> search(SearchType key, String status, String businessNumber, Pageable pageable) {
        return switch (key) {
            case ALL -> merchantApplicationRepository.findAllByStatusNotIn(EXCLUDED_FROM_DEFAULT_LIST, pageable);
            case BY_STATUS -> merchantApplicationRepository.findByStatus(MerchantApplicationStatus.valueOf(status), pageable);
            case BY_BUSINESS_NUMBER -> merchantApplicationRepository.findByBusinessNumberContainingAndStatusNotIn(
                    businessNumber, EXCLUDED_FROM_DEFAULT_LIST, pageable);
            case BY_BOTH -> merchantApplicationRepository.findByBusinessNumberContainingAndStatus(
                    businessNumber, MerchantApplicationStatus.valueOf(status), pageable);
        };
    }
}
