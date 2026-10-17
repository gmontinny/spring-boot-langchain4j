# Aplicação Spring Boot LangChain4j + Google Gemini

Projeto Spring Boot com Java 25, LangChain4j, PostgreSQL, versionamento via Liquibase, autenticação Spring Security com JWT e Refresh Token, Rate Limiting com Bucket4j, ingestão de dados CSV e Chatbot de IA com Google Gemini.

---

## 🛠 Tecnologias e Recursos

- **Java 25** & **Spring Boot 4.x** (Spring Security, Spring Data JPA, Spring Validation)
- **LangChain4j 0.36.2** integrado ao modelo **Google Gemini (gemini-2.5-flash)**
- **PostgreSQL** para persistência dos dados
- **Liquibase** para versionamento estruturado do banco de dados
- **JWT (io.jsonwebtoken)** com Access Token e Refresh Token
- **Rate Limiting** com **Bucket4j** (Proteção por IP com cabeçalhos HTTP e resposta 429)
- **OpenCSV** para ingestão e parser do dataset de restaurantes (`data/Zomato Restaurant Dataset.csv`)
- **Swagger / OpenAPI 3 (SpringDoc OpenAPI UI)** para documentação interativa e testes dos endpoints
- **Docker Compose** com PostgreSQL e pgAdmin prontos

---

## 📖 Documentação Interativa com Swagger (OpenAPI)

A documentação interativa Swagger UI está configurada e disponível em:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) (ou `http://localhost:8080/swagger-ui/index.html`)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **OpenAPI YAML**: [http://localhost:8080/v3/api-docs.yaml](http://localhost:8080/v3/api-docs.yaml)

### 🔑 Autenticação no Swagger UI:
1. Acesse o endpoint de login `POST /api/auth/signin` na UI do Swagger ou cadastre um usuário em `POST /api/auth/signup`.
2. Copie o token retornado no campo `token` da resposta.
3. Clique no botão **Authorize** (cadeado verde no topo direito do Swagger UI).
4. No campo **Bearer Authentication**, informe o token gerado (o Swagger adiciona automaticamente o esquema Bearer).
5. Clique em **Authorize** e feche a janela modal.
6. Todos os endpoints protegidos (`/api/chat`, `/api/restaurants/**`) agora podem ser testados diretamente pela interface.

---

## 🗄️ Banco de Dados e Docker

Para iniciar o banco PostgreSQL:
```bash
docker compose up -d
```

Configurações padrão:
- **Host**: `localhost:5432`
- **Database**: `langchain4j_db`
- **User**: `gmontinny`
- **Password**: `Gmontinny2026`
- **pgAdmin**: `http://localhost:5050` (Email: `admin@admin.com`, Senha: `admin`)

---

## 🔐 Variáveis de Ambiente Suportadas

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/langchain4j_db` | URL de conexão com o PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `gmontinny` | Usuário do banco de dados |
| `SPRING_DATASOURCE_PASSWORD` | `Gmontinny2026` | Senha do banco de dados |
| `JWT_SECRET` | `404E6352...` | Chave secreta de assinatura JWT |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Expiração do token de acesso |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7d) | Expiração do refresh token |
| `GEMINI_API_KEY` | `AIzaSyDjvSqrrGcA7iwNIfkBz_B9vshmtbmEVFo` | Chave de API do Google Gemini |
| `GEMINI_MODEL_NAME` | `gemini-2.5-flash` | Modelo de IA do Google Gemini (ex: `gemini-2.5-flash`, `gemini-2.5-pro`) |
| `RATE_LIMIT_CAPACITY` | `20` | Capacidade máxima de requisições por janela |
| `RATE_LIMIT_REFILL_MINUTES` | `1` | Tempo de recarga de tokens do rate limit |
| `CSV_FILE_PATH` | `data/Zomato Restaurant Dataset.csv` | Caminho do arquivo de dados CSV |

---

## 🚀 Endpoints da API

### 1. Autenticação (`/api/auth`)
- **`POST /api/auth/signup`**: Cadastro de usuário com roles (`ROLE_USER`, `ROLE_ADMIN`).
- **`POST /api/auth/signin`**: Login gerando JWT e Refresh Token.
- **`POST /api/auth/refreshtoken`**: Renovação de token de acesso expirado.

### 2. Chat IA com Gemini (`/api/chat`)
- **`POST /api/chat`**: Conversa com IA consultando dinamicamente a base de restaurantes no banco de dados.
  - *Exemplo de Payload Request*:
    ```json
    {
      "message": "Quais são os 5 melhores restaurantes e qual a cidade deles?"
    }
    ```
  - *Exemplo de Response*:
    ```json
    {
      "response": "Aqui estão os melhores restaurantes...",
      "model": "gemini-2.5-flash",
      "timestamp": "2026-10-17T07:55:00Z"
    }
    ```

### 3. Gestão de Restaurantes (`/api/restaurants`)
- **`GET /api/restaurants`**: Listagem paginada e filtrada por cidade, culinária e nome.
- **`GET /api/restaurants/{id}`**: Detalhes de um restaurante.
- **`POST /api/restaurants`**: Cadastro de restaurante (Requer `ROLE_ADMIN`).
- **`PUT /api/restaurants/{id}`**: Atualização de restaurante (Requer `ROLE_ADMIN`).
- **`DELETE /api/restaurants/{id}`**: Exclusão de restaurante (Requer `ROLE_ADMIN`).
- **`POST /api/restaurants/ingest`**: Re-executar ingestão do arquivo CSV (Requer `ROLE_ADMIN`).
