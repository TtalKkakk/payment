package com.example.pg.cardcompany.infrastructure;

import com.example.pg.cardcompany.domain.BillingKeyRecord;
import com.example.pg.cardcompany.domain.BillingKeyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 카드사 빌링키를 메모리에 보관. (분리 시 DB 저장소로 교체)
 */
@Repository
public class InMemoryBillingKeyRepository implements BillingKeyRepository {

    private final ConcurrentHashMap<String, BillingKeyRecord> byCardToken = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> cardTokensByMerchant = new ConcurrentHashMap<>();

    @Override
    public void save(BillingKeyRecord record) {
        byCardToken.put(record.getCardToken(), record);
        cardTokensByMerchant
                .computeIfAbsent(record.getMerchantId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(record.getCardToken());
    }

    @Override
    public Optional<BillingKeyRecord> findByCardToken(String cardToken) {
        return Optional.ofNullable(byCardToken.get(cardToken));
    }

    @Override
    public void deleteByMerchantId(String merchantId) {
        List<String> tokens = cardTokensByMerchant.remove(merchantId);
        if (tokens != null) {
            tokens.forEach(byCardToken::remove);
        }
    }
}
