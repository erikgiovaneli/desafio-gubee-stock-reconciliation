package com.gubee.service;

import com.gubee.Repository.EventStoreRepository;
import com.gubee.Repository.StockBalanceRepository;
import com.gubee.Repository.StockHistoryRepository;
import com.gubee.dto.EventRequestDTO;
import com.gubee.model.StockBalance;
import com.gubee.model.enums.EventStatus;
import com.gubee.model.enums.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReconciliationServiceTest {

	@Mock
	private EventStoreRepository eventStoreRepository;

	@Mock
	private StockBalanceRepository stockBalanceRepository;

	@Mock
	private StockHistoryRepository stockHistoryRepository;

	@InjectMocks
	private StockReconciliationService service;

	private EventRequestDTO baseDto;

	@BeforeEach
	void setUp() {
		// Configuração de um DTO padrão para os testes
		baseDto = new EventRequestDTO(
				"evt-123",
				EventType.STOCK_ADJUSTED,
				Instant.now(),
				"MERCADO_LIVRE",
				"account-001",
				"ML-123456",
				"ABC-123",
				2,   // quantity
				10,  // available
				"Teste",
				0    // quantitySent
		);
	}

	@Test
	@DisplayName("Cenário 1: Ajuste inicial de estoque - STOCK_ADJUSTED available = 10 deve resultar em estoque 10")
	void cenario1_ajusteInicialEstoque() {
		// Given
		EventRequestDTO dto = new EventRequestDTO(
				"evt-001", EventType.STOCK_ADJUSTED, Instant.now(), "MERCADO_LIVRE",
				"account-001", null, "ABC-123", 0, 10, "Ajuste Inicial", 0
		);

		when(eventStoreRepository.existsById(dto.eventId())).thenReturn(false);
		when(stockBalanceRepository.findByAccountIdAndSku(dto.accountId(), dto.sku()))
				.thenReturn(Optional.empty()); // Começa com 0

		// When
		EventStatus status = service.processEvent(dto);

		// Then
		assertEquals(EventStatus.PROCESSED, status);
		verify(stockBalanceRepository).save(argThat(balance -> balance.getQuantity() == 10));
		verify(stockHistoryRepository).save(any());
	}

	@Test
	@DisplayName("Cenário 2: Pedido baixa estoque - STOCK_ADJUSTED (10) + ORDER_CREATED (2) deve resultar em estoque 8")
	void cenario2_pedidoBaixaEstoque() {
		// Given
		EventRequestDTO dto = new EventRequestDTO(
				"evt-002", EventType.ORDER_CREATED, Instant.now(), "MERCADO_LIVRE",
				"account-001", "ML-123456", "ABC-123", 2, 0, null, 0
		);

		StockBalance saldoExistente = new StockBalance("account-001", "ABC-123", 10, Instant.now());

		when(eventStoreRepository.existsById(dto.eventId())).thenReturn(false);
		when(stockBalanceRepository.findByAccountIdAndSku(dto.accountId(), dto.sku()))
				.thenReturn(Optional.of(saldoExistente));

		// When
		EventStatus status = service.processEvent(dto);

		// Then
		assertEquals(EventStatus.PROCESSED, status);
		assertEquals(8, saldoExistente.getQuantity());
		verify(stockBalanceRepository).save(saldoExistente);
	}

	@Test
	@DisplayName("Cenário 3: Cancelamento devolve estoque - Após pedido quantity = 2 e cancelamento, estoque volta ao valor correto")
	void cenario3_cancelamentoDevolveEstoque() {
		// Given
		EventRequestDTO dto = new EventRequestDTO(
				"evt-003", EventType.ORDER_CANCELLED, Instant.now(), "MERCADO_LIVRE",
				"account-001", "ML-123456", "ABC-123", 2, 0, null, 0
		);

		StockBalance saldoExistente = new StockBalance("account-001", "ABC-123", 8, Instant.now());

		when(eventStoreRepository.existsById(dto.eventId())).thenReturn(false);

		// Configura as checagens do bloco unificado (Requisito 5.2/5.6) para dizer que NÃO é duplicado
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus("ML-123456", "ORDER_CANCELLED", "ABC-123", "PROCESSED")).thenReturn(false);
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus("ML-123456", "MARKETPLACE_STOCK_RESTORED", "ABC-123", "PROCESSED")).thenReturn(false);

		// Garante que o pedido original existe para não ficar PENDING
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus("ML-123456", "ORDER_CREATED", "ABC-123", "PROCESSED")).thenReturn(true);

		when(stockBalanceRepository.findByAccountIdAndSku(dto.accountId(), dto.sku()))
				.thenReturn(Optional.of(saldoExistente));

		// When
		EventStatus status = service.processEvent(dto);

		// Then
		assertEquals(EventStatus.PROCESSED, status);
		assertEquals(10, saldoExistente.getQuantity()); // Devolveu os 2, voltou para 10
	}

	@Test
	@DisplayName("Cenário 4: Evento duplicado por eventId - O mesmo eventId recebido duas vezes retorna IGNORED")
	void cenario4_eventoDuplicadoPorEventId() {
		// Given
		when(eventStoreRepository.existsById(baseDto.eventId())).thenReturn(true);

		// When
		EventStatus status = service.processEvent(baseDto);

		// Then
		assertEquals(EventStatus.IGNORED, status);
		verify(stockBalanceRepository, never()).save(any());
	}

	@Test
	@DisplayName("Cenário 5: Cancelamento duplicado - Dois cancelamentos do mesmo pedido não recompõem estoque indevidamente")
	void cenario5_cancelamentoDuplicado() {
		// Given
		EventRequestDTO dto = new EventRequestDTO(
				"evt-005", EventType.ORDER_CANCELLED, Instant.now(), "MERCADO_LIVRE",
				"account-001", "ML-123456", "ABC-123", 2, 0, null, 0
		);

		when(eventStoreRepository.existsById(dto.eventId())).thenReturn(false);
		// Simula que JÁ EXISTE um cancelamento processado para esse pedido
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus(
				"ML-123456", "ORDER_CANCELLED", "ABC-123", "PROCESSED")).thenReturn(true);

		// When
		EventStatus status = service.processEvent(dto);

		// Then
		assertEquals(EventStatus.IGNORED, status);
		verify(stockBalanceRepository, never()).save(any());
	}

	@Test
	@DisplayName("Cenário 6: Cancelamento antes do pedido - Deve marcar como PENDING para reprocessar depois")
	void cenario6_cancelamentoAntesDoPedido() {
		// Given
		EventRequestDTO dto = new EventRequestDTO(
				"evt-006", EventType.ORDER_CANCELLED, Instant.now(), "MERCADO_LIVRE",
				"account-001", "ML-123456", "ABC-123", 2, 0, null, 0
		);

		when(eventStoreRepository.existsById(dto.eventId())).thenReturn(false);

		// Checagem de duplicidade retorna falso para ambos
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus("ML-123456", "ORDER_CANCELLED", "ABC-123", "PROCESSED")).thenReturn(false);
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus("ML-123456", "MARKETPLACE_STOCK_RESTORED", "ABC-123", "PROCESSED")).thenReturn(false);

		// Simula que o pedido ORDER_CREATED ainda NÃO existe no banco
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus(
				"ML-123456", "ORDER_CREATED", "ABC-123", "PROCESSED")).thenReturn(false);

		// When
		EventStatus status = service.processEvent(dto);

		// Then
		assertEquals(EventStatus.PENDING, status);
		verify(stockBalanceRepository, never()).save(any());
	}

	@Test
	@DisplayName("Cenário 7: Concorrência / Proteção contra estoque negativo acidental")
	void cenario7_protecaoEstoqueNegativo() {
		// Given
		EventRequestDTO dto = new EventRequestDTO(
				"evt-007", EventType.ORDER_CREATED, Instant.now(), "MERCADO_LIVRE",
				"account-001", "ML-777", "ABC-123", 5, 0, null, 0
		);

		// Estoque atual é de apenas 3 unidades. Pedido pede 5.
		StockBalance saldoInsuficiente = new StockBalance("account-001", "ABC-123", 3, Instant.now());

		when(eventStoreRepository.existsById(dto.eventId())).thenReturn(false);
		when(stockBalanceRepository.findByAccountIdAndSku(dto.accountId(), dto.sku()))
				.thenReturn(Optional.of(saldoInsuficiente));

		// When
		EventStatus status = service.processEvent(dto);

		// Then
		assertEquals(EventStatus.INCONSISTENT, status);
		verify(stockBalanceRepository, never()).save(saldoInsuficiente);
	}

	@Test
	@DisplayName("Cenário 8: Recomposição do marketplace seguida de cancelamento - Tratado sem duplicar recomposição")
	void cenario8_recomposicaoMarketplaceSeguidaDeCancelamento() {
		// Given
		EventRequestDTO dtoCancelamento = new EventRequestDTO(
				"evt-009", EventType.ORDER_CANCELLED, Instant.now(), "MERCADO_LIVRE",
				"account-001", "ML-999", "ABC-123", 2, 0, null, 0
		);

		when(eventStoreRepository.existsById(dtoCancelamento.eventId())).thenReturn(false);

		// Primeiro checa se já foi cancelado (falso)
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus("ML-999", "ORDER_CANCELLED", "ABC-123", "PROCESSED")).thenReturn(false);

		// Depois checa se já foi restaurado pelo marketplace (verdadeiro, simulando a ordem do cenário do PDF)
		when(eventStoreRepository.existsByExternalOrderIdAndTypeAndSkuAndStatus(
				"ML-999", "MARKETPLACE_STOCK_RESTORED", "ABC-123", "PROCESSED")).thenReturn(true);

		// When
		EventStatus status = service.processEvent(dtoCancelamento);

		// Then
		assertEquals(EventStatus.IGNORED, status);
		verify(stockBalanceRepository, never()).save(any());
	}
}
