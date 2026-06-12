package com.gubee.Repository;

import com.gubee.model.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    // Retorna a linha do tempo ordenada para a auditoria do SKU
    List<StockHistory> findByAccountIdAndSkuOrderByOccurredAtAsc(String accountId, String sku);
}
