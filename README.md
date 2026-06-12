# Gubee Stock Reconciliation Service

Serviço de reconciliação de estoque responsável por processar eventos de marketplaces, garantir a consistência dos saldos por conta/SKU e manter um histórico completo para auditoria.

## 🚀 Tecnologias Utilizadas

- **Java 17** (Utilização de *Java Records* para DTOs imutáveis)
- **Spring Boot 4.x** (Spring Data JPA, Spring Web)
- **PostgreSQL 15** (Banco de dados relacional)
- **Docker & Docker Compose** (Ambiente de banco de dados isolado)
- **Maven** (Gerenciador de dependências)

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