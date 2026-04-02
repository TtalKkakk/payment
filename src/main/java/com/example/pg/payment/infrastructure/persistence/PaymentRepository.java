package com.example.pg.payment.infrastructure.persistence;

import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.domain.vo.PaymentMerchantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA 리포지토리.
 * 서비스 로직(결제 생성/조회/승인/보상/취소)에 맞춘 조회 메서드를 제공한다.
 */
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * paymentId로 단건 조회. (startAuthorization, startCancellation, processAuthorization, 웹훅 발송 등)
     */
    default Optional<Payment> load(PaymentId paymentId) {
        return findById(paymentId.getValue());
    }

    /**
     * 가맹점별 결제 단건 조회. (GET /payments/{id} - 해당 가맹점 소유만 허용)
     */
    Optional<Payment> findByMerchantIdAndId(PaymentMerchantId merchantId, String id);

    /**
     * id + status로 조회. (보상: READY인 결제만 ABORTED 처리)
     */
    Optional<Payment> findByIdAndStatus(String id, PaymentStatus status);

    /**
     * CAS 상태 전이: READY -> AUTHORIZING
     * 성공 시 1, 실패(이미 전이됨/상태 불일치) 시 0
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
               set p.status = :to,
                   p.updatedAt = CURRENT_TIMESTAMP
             where p.id = :id
               and p.status = :from
            """)
    int transitionStatus(@Param("id") String id,
                         @Param("from") PaymentStatus from,
                         @Param("to") PaymentStatus to);

    /**
     * CAS 상태 전이(취소 시작): (AUTHORIZED or CANCEL_FAILED) -> CANCELLING
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
               set p.status = :to,
                   p.updatedAt = CURRENT_TIMESTAMP
             where p.id = :id
               and p.status in :fromStatuses
            """)
    int transitionStatusFromSet(@Param("id") String id,
                                @Param("fromStatuses") java.util.Set<PaymentStatus> fromStatuses,
                                @Param("to") PaymentStatus to);

    /**
     * CAS 승인 성공 반영: AUTHORIZING -> AUTHORIZED (+ 승인정보 저장)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
               set p.status = 'AUTHORIZED',
                   p.approvalNumber = :approvalNumber,
                   p.transactionId = :transactionId,
                   p.approvedAt = :approvedAt,
                   p.updatedAt = CURRENT_TIMESTAMP
             where p.id = :id
               and p.status = 'AUTHORIZING'
            """)
    int markAuthorized(@Param("id") String id,
                       @Param("approvalNumber") String approvalNumber,
                       @Param("transactionId") String transactionId,
                       @Param("approvedAt") LocalDateTime approvedAt);

    /**
     * CAS 승인 실패 반영: AUTHORIZING -> AUTHORIZE_FAILED
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
               set p.status = 'AUTHORIZE_FAILED',
                   p.updatedAt = CURRENT_TIMESTAMP
             where p.id = :id
               and p.status = 'AUTHORIZING'
            """)
    int markAuthorizeFailed(@Param("id") String id);

    /**
     * CAS 취소 성공 반영: CANCELLING -> CANCELED
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
               set p.status = 'CANCELED',
                   p.updatedAt = CURRENT_TIMESTAMP
             where p.id = :id
               and p.status = 'CANCELLING'
            """)
    int markCanceled(@Param("id") String id);

    /**
     * CAS 취소 실패 반영: CANCELLING -> CANCEL_FAILED
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
               set p.status = 'CANCEL_FAILED',
                   p.updatedAt = CURRENT_TIMESTAMP
             where p.id = :id
               and p.status = 'CANCELLING'
            """)
    int markCancelFailed(@Param("id") String id);

    /**
     * CAS 보상: READY -> ABORTED
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
               set p.status = 'ABORTED',
                   p.updatedAt = CURRENT_TIMESTAMP
             where p.id = :id
               and p.status = 'READY'
            """)
    int markAbortedIfReady(@Param("id") String id);
}
