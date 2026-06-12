package com.gubee.service;

import com.gubee.Repository.EventStoreRepository;
import com.gubee.Repository.StockBalanceRepository;
import com.gubee.Repository.StockHistoryRepository;
import com.gubee.dto.EventRequestDTO;
import com.gubee.model.EventStore;
import com.gubee.model.StockBalance;
import com.gubee.model.StockHistory;
import com.gubee.model.enums.EventStatus;
import com.gubee.model.enums.EventType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class StockReconciliationService {

    private final EventStoreRepository eventStoreRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockHistoryRepository stockHistoryRepository;

    public StockReconciliationService(EventStoreRepository eventStoreRepository,
                                      StockBalanceRepository stockBalanceRepository,
                                      StockHistoryRepository stockHistoryRepository) {
        this.eventStoreRepository = eventStoreRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockHistoryRepository = stockHistoryRepository;
    }

    @Transactional
    public EventStatus processEvent(EventRequestDTO dto){
        //REQUISITO 5.1: Idempotência por eventId
        if (eventStoreRepository.existsById(dto.eventId())) {
            return EventStatus.IGNORED;
        }

        EventStore eventStore = new EventStore();
        eventStore.setEventId(dto.eventId());
        eventStore.setType(dto.type().name());
        eventStore.setOccurredAt(dto.occurredAt());
        eventStore.setMarketplace(dto.marketplace());
        eventStore.setAccountId(dto.accountId());
        eventStore.setExternalOrderId(dto.externalOrderId());
        eventStore.setSku(dto.sku());
        eventStore.setQuantity(dto.quantity());
        eventStore.setAvailable(dto.available());
        eventStore.setReason(dto.reason());
        eventStore.setQuantitySent(dto.quantitySent());

        if (dto.type() == EventType.ORDER_CANCELLED || dto.type() == EventType.MARKETPLACE_STOCK_RESTORED) {
            //REQUISITO 5.2 & 5.6: Duplicidade Lógica / Cenário 5 e 8 (Cancelamentos e Recomposições duplicadas)
            boolean jaFoiDevolvido = eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus(
                    dto.externalOrderId(), EventType.ORDER_CANCELLED.name(), dto.sku(), EventStatus.PROCESSED.name()
            ) || eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus(
                    dto.externalOrderId(), EventType.MARKETPLACE_STOCK_RESTORED.name(), dto.sku(), EventStatus.PROCESSED.name()
            );

            if (jaFoiDevolvido) {
                eventStore.setStatus(EventStatus.IGNORED.name());
                eventStoreRepository.save(eventStore);
                return EventStatus.IGNORED; // Ignora para não duplicar a recomposição de estoque
            }
        }
        //REQUISITO 5.3: Eventos fora de ordem (Cancelamento antes da Criação do pedido)
        if (dto.type() == EventType.ORDER_CANCELLED) {
            boolean existePedidoCriado = eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus(
                    dto.externalOrderId(), EventType.ORDER_CREATED.name(), dto.sku(), EventStatus.PROCESSED.name()
            );
            if (!existePedidoCriado) {
                eventStore.setStatus(EventStatus.PENDING.name());
                eventStoreRepository.save(eventStore);
                return EventStatus.PENDING; // Mantém pendente para reprocessar depois
            }
        }

        StockBalance balance = stockBalanceRepository.findByAccountIdAndSku(dto.accountId(), dto.sku())
                .orElseGet(() -> new StockBalance(dto.accountId(), dto.sku(), 0, Instant.now()));

        int quantityChanged = 0;

        switch (dto.type()){
            case STOCK_ADJUSTED:
                // Cenário 1: Ajuste manual define o valor absoluto absoluto
                quantityChanged = dto.available() - balance.getQuantity();
                balance.setQuantity(dto.available());
                break;

            case ORDER_CREATED:
                // Cenário 2: Pedido baixa estoque
                if (balance.getQuantity() < dto.quantity()) {
                    // REQUISITO 5.4: Evitar estoque negativo acidentalmente
                    eventStore.setStatus(EventStatus.INCONSISTENT.name());
                    eventStoreRepository.save(eventStore);
                    return EventStatus.INCONSISTENT;
                }
                quantityChanged = -dto.quantity();
                balance.setQuantity(balance.getQuantity() - dto.quantity());
                break;

            case ORDER_CANCELLED:
            case MARKETPLACE_STOCK_RESTORED:
                //Cenário 3 e 8: Devolvem estoque
                quantityChanged = dto.quantity();
                balance.setQuantity(balance.getQuantity() + dto.quantity());
                break;

            case STOCK_SYNC_SENT:
                // Sincronismo enviado apenas notifica o marketplace, não altera o saldo interno da Gubee (Fonte da Verdade)
                eventStore.setStatus(EventStatus.PROCESSED.name());
                eventStoreRepository.save(eventStore);
                return EventStatus.PROCESSED;
        }

        // Atualizar data de modificação do saldo
        balance.setLastUpdated(Instant.now());
        stockBalanceRepository.save(balance);

        //REQUISITO 5.5: Rastreabilidade (Gravar a Linha do Tempo / Auditoria)
        StockHistory history = new StockHistory(
                dto.accountId(),
                dto.sku(),
                dto.eventId(),
                dto.type().name(),
                quantityChanged,
                balance.getQuantity(),
                dto.occurredAt()
        );
        stockHistoryRepository.save(history);

        eventStore.setStatus(EventStatus.PROCESSED.name());
        eventStoreRepository.save(eventStore);

        return EventStatus.PROCESSED;
    }
}
