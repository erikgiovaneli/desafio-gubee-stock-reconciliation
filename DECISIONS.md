# Decisões Arquiteturais e Técnicas

Este documento descreve as premissas, padrões de projeto e soluções de engenharia adotadas no desenvolvimento do `gubee-stock-reconciliation` para atender aos requisitos de negócio e mitigar os riscos técnicos mapeados.

## 🏗️ Arquitetura e Organização do Código

1. **Camada de Serviço Direta (Sem Interfaces Anêmicas):**
   Seguindo os princípios modernos de *Clean Code* e YAGNI (*You Ain't Gonna Need It*), optou-se por construir a classe `StockReconciliationService` diretamente, sem criar uma interface `StockReconciliationServiceImpl`. Como o sistema possui apenas uma estratégia de reconciliação de estoque bem definida, a criação de uma interface geraria código redundante (*boilerplate*) sem ganho real de abstração neste estágio do projeto.

2. **Java 17 Records para DTOs:**
   Os dados que trafegam na rede através da API REST são mapeados utilizando *Java Records* (`EventRequestDTO`). Isso garante a imutabilidade total dos dados de entrada, thread-safety na camada de transporte e elimina a necessidade de verbosidade manual ou de dependências pesadas de geração de código para leitura de dados.

3. **Isolamento por Conta (Multi-Account):**
   Para atender de forma nativa e performática o isolamento de estoques entre diferentes clientes da plataforma, toda a modelagem de dados e os métodos de consulta utilizam chaves lógicas cruzando o `accountId` e o `sku`. Isso mantém o design de tabelas simples e indexável.

---

## 🧠 Resolução dos Riscos de Negócio (Requisitos da Seção 5)

### 5.1 Idempotência
Cada requisição HTTP que chega ao endpoint é validada contra a tabela `EventStore`. O `eventId` enviado pelo parceiro é utilizado diretamente como a Chave Primária (Primary Key) da tabela. Se o ID do evento já existir no banco de dados, o processamento é interrompido imediatamente e o status retornado é `IGNORED`, impedindo baixas ou acréscimos duplicados de estoque causados por retentativas de rede (*network retries*).

### 5.2 e 5.6 Duplicidade Lógica (Cancelamentos e Recomposições)
O motor de regras intercepta eventos do tipo `ORDER_CANCELLED` e `MARKETPLACE_STOCK_RESTORED` e verifica no histórico de transações se o pedido (`externalOrderId`) já sofreu alguma ação prévia de devolução de estoque com sucesso (`PROCESSED`). Caso positivo, o novo evento idêntico é classificado como `IGNORED`, blindando o saldo contra comportamentos instáveis ou duplicados dos Marketplaces.

### 5.3 Eventos Fora de Ordem (Race Conditions de Fluxo)
Caso um evento de `ORDER_CANCELLED` chegue ao sistema antes do respectivo `ORDER_CREATED` (um cenário comum de assincronismo na ponta do parceiro), o sistema identifica que o pedido original ainda não foi processado. Em vez de rejeitar ou descartar o dado, o evento é persistido na `EventStore` com o estado `PENDING`, isolando-o de forma segura para reprocessamento futuro.

### 5.4 Garantia Contra Estoque Negativo
O sistema protege a consistência do inventário físico impedindo que uma venda (`ORDER_CREATED`) reduza o estoque para valores abaixo de zero. Se a quantidade solicitada for superior ao saldo disponível na conta, a operação de alteração é abortada, o evento é gravado com o status `INCONSISTENT` para fins de auditoria, e o saldo do estoque permanece intacto.

### 5.5 Rastreabilidade Total (Padrão Ledger / Livro Razão)
Toda e qualquer alteração matemática aplicada ao saldo de um SKU gera, na mesma transação, um registro imutável na tabela `StockHistory`. O histórico armazena o impacto exato da operação (ex: `-2` para vendas, `+5` para ajustes) e o saldo consolidado imediatamente após o evento (`balanceAfter`), permitindo reconstruir e auditar perfeitamente a linha do tempo do estoque.

---

## ⚖️ Trade-offs e Evolução Arquitetural (Próximos Passos para Produção)

Para o escopo atual do desafio técnico, foram assumidas simplificações estratégicas que, em um cenário de produção de altíssima escala, evoluiriam da seguinte forma:

1. **Concorrência Concorrente (Race Conditions):**
   A entidade `StockBalance` conta com a anotação `@Version` do JPA para Controle de Concorrência Otimista (*Optimistic Locking*). Para cenários de altíssimo volume de vendas simultâneas no mesmo SKU, a estratégia seria evoluída para **Pessimistic Locking** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) no momento da busca do saldo, garantindo o bloqueio da linha a nível de banco de dados durante a escrita.

2. **Migração para Mensageria Assíncrona:**
   O processamento atual é síncrono via HTTP POST. Para suportar milhões de eventos por minuto, a porta de entrada seria substituída por tópicos do **Apache Kafka** ou filas do **RabbitMQ**, utilizando o `accountId` ou `sku` como chave de particionamento (*partition key*), garantindo ordenação natural por produto e alto throughput.

3. **Mecanismo de Retry para Pendências (`PENDING`):**
   Os eventos que caem no status `PENDING` necessitam de um componente agendador (um Worker assíncrono utilizando `@Scheduled` do Spring ou um consumidor de Dead Letter Queue) para tentar reprocessá-los periodicamente após o intervalo de alguns minutos.