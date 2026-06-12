package com.gubee.Repository;

import com.gubee.model.StockBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByAccountIdAndSku(String accountId, String Sku);

}
