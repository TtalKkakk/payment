package com.example.pg.merchant.infrastructure.persistence;

import com.example.pg.merchant.domain.aggregate.Merchant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA 기반 Merchant 저장소.
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {

    Optional<Merchant> findByApiKey(String apiKey);

    /** 신청서 기준 조회. 재승인 시 기존 Merchant 재활성화 여부 판단용 */
    Optional<Merchant> findByApplicationId(String applicationId);

    /** ID 부분 일치 검색 (관리자 목록용) */
    Page<Merchant> findByIdContaining(String id, Pageable pageable);
}
