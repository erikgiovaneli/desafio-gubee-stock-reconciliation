# Gubee Stock Reconciliation Service

Serviço de reconciliação de estoque responsável por processar eventos de marketplaces, garantir a consistência dos saldos por conta/SKU e manter um histórico completo para auditoria.

## 🚀 Tecnologias Utilizadas

- **Java 17** (Utilização de *Java Records* para DTOs imutáveis)
- **Spring Boot 4.x** (Spring Data JPA, Spring Web)
- **PostgreSQL 15** (Banco de dados relacional)
- **Docker & Docker Compose** (Ambiente de banco de dados isolado)
- **Maven** (Gerenciador de dependências)
- **JUnit 5 & Mockito** (Suíte de testes automatizados)

## 📋 Pré-requisitos

Antes de iniciar, você precisará ter instalado em sua máquina:
- Java 17
- Docker e Docker Compose
- Maven (ou utilizar o wrapper `./mvnw`)

## 🛠️ Como Executar o Projeto

1. **Subir o Banco de Dados (PostgreSQL):**
   Na raiz do projeto, execute o comando para iniciar o container do banco de dados na porta `5433`:
   ```bash
   docker-compose up -d

2. **Rodar a Aplicação Spring Boot:**
   Com o banco de dados rodando, inicie o backend via Maven:
   ```bash
   ./mvnw spring-boot:run

  - A API estará disponível em http://localhost:8080.
  
3. **Como Rodar os Testes**

   Os testes automatizados cobrem os 8 cenários de negócio críticos exigidos, incluindo concorrência, eventos fora de ordem e idempotência. Eles utilizam Mockito e rodam puramente em memória, não necessitando do Docker ativo.

   Abra a classe StockReconciliationServiceTest no seu editor e clique no ícone de "Play" (verde) que aparece na margem esquerda, ao lado da definição da classe ou dos métodos individuais. Isso executará os testes com um relatório visual detalhado direto na sua interface ou para executar a suíte de testes no terminal, rode:

   ```Bash
   ./mvnw test

4. **Método, Endpoint - Descrição** 

       POST, /events - Recebe e processa eventos de estoque/pedidos.

       GET, /stocks/{accountId}/{sku} - Retorna a visão atual do saldo de estoque.

       GET, /stocks/{accountId}/{sku}/history - Retorna a linha do tempo (auditoria) do estoque.

       GET,/events?status={STATUS} - "Retorna eventos pendentes ou inconsistentes (ex: PENDING, INCONSISTENT)."

5. **Exemplos de Requisição (POST /events)**
Abaixo estão os payloads aceitos pela API para os diferentes fluxos de negócio.

   - Pedido Criado (Baixa estoque)
      JSON
      {
      "eventId": "evt-001",
      "type": "ORDER_CREATED",
      "occurredAt": "2026-05-28T10:00:00Z",
      "marketplace": "MERCADO_LIVRE",
      "accountId": "account-001",
      "externalOrderId": "ML-123456",
      "sku": "ABC-123",
      "quantity": 2
      }
   - Pedido Cancelado (Devolve estoque)
      JSON
      {
      "eventId": "evt-002",
      "type": "ORDER_CANCELLED",
      "occurredAt": "2026-05-28T10:05:00Z",
      "marketplace": "MERCADO_LIVRE",
      "accountId": "account-001",
      "externalOrderId": "ML-123456",
      "sku": "ABC-123",
      "quantity": 2
      }
   - Ajuste Manual de Estoque (Define saldo absoluto)
      JSON
      {
      "eventId": "evt-003",
      "type": "STOCK_ADJUSTED",
      "occurredAt": "2026-05-28T10:10:00Z",
      "accountId": "account-001",
      "sku": "ABC-123",
      "available": 10,
      "reason": "manual_adjustment"
      }
   - Sincronismo Enviado (Apenas auditoria)
      JSON
      {
      "eventId": "evt-004",
      "type": "STOCK_SYNC_SENT",
      "occurredAt": "2026-05-28T10:15:00Z",
      "marketplace": "MERCADO_LIVRE",
      "accountId": "account-001",
      "sku": "ABC-123",
      "quantitySent": 8
      }
   - Recomposição Automática (Devolve estoque via marketplace)
      JSON
      {
      "eventId": "evt-005",
      "type": "MARKETPLACE_STOCK_RESTORED",
      "occurredAt": "2026-05-28T10:20:00Z",
      "marketplace": "MERCADO_LIVRE",
      "accountId": "account-001",
      "externalOrderId": "ML-123456",
      "sku": "ABC-123",
      "quantity": 2
      }
   
   
6. **Limitações Conhecidas**
   
   Mensageria: Por simplificação de prazo, a fila de processamento (Kafka/RabbitMQ) foi emulada diretamente via chamadas síncronas na API REST.

   Processamento Assíncrono: Eventos classificados como PENDING (ex: um cancelamento que chega antes da criação do pedido) são persistidos com segurança no banco de dados, porém necessitam de um Worker (@Scheduled ou consumer em background) para reprocessá-los, mecanismo este não incluído no escopo inicial.

   **Detalhamento das Decisões:** Mais detalhes sobre a arquitetura e justificativas de trade-offs podem ser encontrados no arquivo [DECISIONS.md](DECISIONS.md).
