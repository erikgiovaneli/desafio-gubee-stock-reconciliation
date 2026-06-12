package com.gubee.Repository;

import com.gubee.model.EventStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventStoreRepository extends JpaRepository<EventStore, String> {

    List<EventStore> findByStatus(String status);

    // Ajuda a verificar se já processámos uma operação lógica para este pedido
    boolean existsByExternalOrderIdAndTypeAndSkuAndStatus(String externalOrderId, String type, String sku, String status);
}
