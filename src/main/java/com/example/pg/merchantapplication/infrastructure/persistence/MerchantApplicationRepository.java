package com.example.pg.merchantapplication.infrastructure.persistence;

import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.domain.enumerate.MerchantApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface MerchantApplicationRepository extends JpaRepository<MerchantApplication, String> {
    boolean existsByBusinessNumberAndStatusIn(String businessNumber, Collection<MerchantApplicationStatus> statuses);

    Optional<MerchantApplication> findByBusinessNumber(String businessNumber);

    Page<MerchantApplication> findByStatus(MerchantApplicationStatus status, Pageable pageable);

    Page<MerchantApplication> findByBusinessNumberContainingAndStatus(String businessNumber, MerchantApplicationStatus status, Pageable pageable);

    Page<MerchantApplication> findAllByStatusNotIn(Collection<MerchantApplicationStatus> statuses, Pageable pageable);

    Page<MerchantApplication> findByBusinessNumberContainingAndStatusNotIn(String businessNumber, Collection<MerchantApplicationStatus> statuses, Pageable pageable);
}
