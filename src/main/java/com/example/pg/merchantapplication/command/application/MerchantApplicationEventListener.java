package com.example.pg.merchantapplication.command.application;

import com.example.pg.merchantapplication.domain.event.MerchantApplicationApprovedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationCancelledEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationRejectedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationPendedEvent;
import com.example.pg.merchantapplication.domain.event.MerchantApplicationSubscriptionEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantApplicationEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantApplicationPended(MerchantApplicationPendedEvent event) {
        log.info("[MerchantApplication] event=Submitted applicationId={} name={} businessNumber={} occurredAt={}",
                event.applicationId(), event.name(), event.businessNumber(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantApplicationApproved(MerchantApplicationApprovedEvent event) {
        log.info("[MerchantApplication] event=Approved applicationId={} name={} occurredAt={}",
                event.applicationId(), event.name(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantApplicationRejected(MerchantApplicationRejectedEvent event) {
        log.info("[MerchantApplication] event=Rejected applicationId={} name={} rejectReason={} occurredAt={}",
                event.applicationId(), event.name(), event.rejectReason(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantApplicationCancelled(MerchantApplicationCancelledEvent event) {
        log.info("[MerchantApplication] event=Cancelled applicationId={} name={} businessNumber={} occurredAt={}",
                event.applicationId(), event.name(), event.businessNumber(), event.occurredAt());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMerchantApplicationSubscriptionEnded(MerchantApplicationSubscriptionEndedEvent event) {
        log.info("[MerchantApplication] event=SubscriptionEnded applicationId={} name={} previousStatus={} occurredAt={}",
                event.applicationId(), event.name(), event.previousStatus(), event.occurredAt());
    }
}
