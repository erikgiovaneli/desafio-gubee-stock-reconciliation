package com.gubee.controller;

import com.gubee.Repository.EventStoreRepository;
import com.gubee.Repository.StockBalanceRepository;
import com.gubee.Repository.StockHistoryRepository;
import com.gubee.dto.EventRequestDTO;
import com.gubee.model.EventStore;
import com.gubee.model.StockBalance;
import com.gubee.model.StockHistory;
import com.gubee.model.enums.EventStatus;
import com.gubee.service.StockReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class StockReconciliationController {

    private final StockReconciliationService reconciliationService;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final EventStoreRepository eventStoreRepository;

    public StockReconciliationController(StockReconciliationService reconciliationService, StockBalanceRepository stockBalanceRepository, StockHistoryRepository stockHistoryRepository, EventStoreRepository eventStoreRepository) {
        this.reconciliationService = reconciliationService;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockHistoryRepository = stockHistoryRepository;
        this.eventStoreRepository = eventStoreRepository;
    }

    /**
     * Endpoint para receber e processar os eventos de estoque e pedidos.
     * Atende ao fluxo principal do desafio.
     */
    @PostMapping("/events")
    public ResponseEntity<String> receiveEvent(@RequestBody EventRequestDTO dto){
        EventStatus status = reconciliationService.processEvent(dto);
        return ResponseEntity.ok("Evento processado com o status: " + status);
    }

    /**
     * API: GET /stocks/{accountId}/{sku}
     * Retorna a visão atual do saldo de estoque de uma conta e SKU específicos.
     */
    @GetMapping("/stocks/{accountId}/{sku}")
    public ResponseEntity<StockBalance> getStockBalance(@PathVariable String accountId, @PathVariable String sku) {
        return stockBalanceRepository.findByAccountIdAndSku(accountId, sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // Retorna 404 caso o SKU nunca tenha recebido eventos
    }

    /**
     * API: GET /stocks/{accountId}/{sku}/history
     * Retorna a linha do tempo (auditoria) completa daquele estoque ordenado cronologicamente.
     */
    @GetMapping("/stocks/{accountId}/{sku}/history")
    public ResponseEntity<List<StockHistory>> getStockHistory(@PathVariable String accountId, @PathVariable String sku) {
        List<StockHistory> history = stockHistoryRepository.findByAccountIdAndSkuOrderByOccurredAtAsc(accountId, sku);
        return ResponseEntity.ok(history);
    }

    /**
     * API: GET /events?status=PENDING ou GET /events?status=INCONSISTENT
     * Permite consultar pendências, inconsistências ou históricos de eventos recebidos.
     */
    @GetMapping("/events")
    public ResponseEntity<List<EventStore>> getEventsByStatus(@RequestParam String status) {
        List<EventStore> events = eventStoreRepository.findByStatus(status.toUpperCase());
        return ResponseEntity.ok(events);
    }
}
