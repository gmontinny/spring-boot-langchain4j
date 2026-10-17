# Construindo um Chatbot de IA com Spring Boot, LangChain4j e Google Gemini

## Uma API REST completa com autenticação JWT, Rate Limiting, ingestão de dados CSV e Function Calling sobre uma base de restaurantes

---

## Sumário

1. [Introdução](#1-introdução)
2. [Visão Geral da Arquitetura](#2-visão-geral-da-arquitetura)
3. [Stack Tecnológica](#3-stack-tecnológica)
4. [Estrutura do Projeto](#4-estrutura-do-projeto)
5. [Infraestrutura com Docker](#5-infraestrutura-com-docker)
6. [Versionamento de Banco com Liquibase](#6-versionamento-de-banco-com-liquibase)
7. [Ingestão de Dados CSV](#7-ingestão-de-dados-csv)
8. [Autenticação e Segurança com JWT](#8-autenticação-e-segurança-com-jwt)
9. [Rate Limiting com Bucket4j](#9-rate-limiting-com-bucket4j)
10. [Integração com LangChain4j e Google Gemini](#10-integração-com-langchain4j-e-google-gemini)
11. [Function Calling com Tools](#11-function-calling-com-tools)
12. [API REST e Documentação Swagger](#12-api-rest-e-documentação-swagger)
13. [Fluxo Completo de uma Requisição de Chat](#13-fluxo-completo-de-uma-requisição-de-chat)
14. [Configuração e Variáveis de Ambiente](#14-configuração-e-variáveis-de-ambiente)
15. [Conclusão](#15-conclusão)
16. [Referências](#16-referências)

---

## 1. Introdução

A integração de modelos de linguagem de grande escala (LLMs) em aplicações corporativas é um dos temas mais relevantes do desenvolvimento de software moderno. Frameworks como LangChain surgiram para abstrair a complexidade dessa integração, e o ecossistema Java não ficou para trás: o **LangChain4j** traz esse poder para o mundo Spring Boot.

Este artigo documenta a construção de uma API REST completa que combina:

- Um **chatbot de IA** capaz de responder perguntas em linguagem natural sobre uma base de dados real de restaurantes (dataset Zomato)
- **Autenticação stateless** com JWT e Refresh Token
- **Proteção contra abuso** com Rate Limiting por IP
- **Ingestão automatizada** de dados a partir de arquivo CSV
- **Documentação interativa** com Swagger/OpenAPI

O projeto utiliza Java 25, Spring Boot 4.1.1, LangChain4j 0.36.2 e o modelo **Google Gemini 2.5 Flash**, demonstrando como construir uma aplicação de IA production-ready sobre uma stack moderna.

---

## 2. Visão Geral da Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                        Cliente HTTP                          │
│                  (Swagger UI / Postman / App)                │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS Request
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Security Filter Chain              │
│  ┌─────────────────┐   ┌──────────────────────────────────┐ │
│  │ RateLimitFilter │──▶│       AuthTokenFilter (JWT)       │ │
│  │   (Bucket4j)    │   │  valida Bearer Token por request  │ │
│  └─────────────────┘   └──────────────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   ┌─────────────┐  ┌────────────┐  ┌──────────────────┐
   │AuthController│  │ChatController│  │RestaurantController│
   │ /api/auth/** │  │ /api/chat  │  │ /api/restaurants │
   └─────────────┘  └─────┬──────┘  └──────────────────┘
                          │
                          ▼
               ┌──────────────────────┐
               │  RestaurantAssistant │  ◀── LangChain4j AiService
               │  (interface + @Tool) │
               └──────────┬───────────┘
                          │ Function Calling
                          ▼
               ┌──────────────────────┐
               │   RestaurantTools    │  ◀── @Tool methods
               │  searchRestaurants() │
               │  getTopRated()       │
               │  getStats()          │
               └──────────┬───────────┘
                          │ JPA Query
                          ▼
               ┌──────────────────────┐
               │  RestaurantRepository│
               │     PostgreSQL       │
               └──────────────────────┘
                          │
               ┌──────────┴───────────┐
               │   Google Gemini API  │
               │  (gemini-2.5-flash)  │
               └──────────────────────┘
```

O fluxo central é: o usuário envia uma pergunta em linguagem natural → o modelo Gemini decide quais ferramentas (Tools) chamar → as ferramentas consultam o banco PostgreSQL → o modelo sintetiza a resposta final.

---

## 3. Stack Tecnológica

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 25 | Linguagem principal |
| Spring Boot | 4.1.1 | Framework base |
| Spring Security | (Boot managed) | Autenticação e autorização |
| Spring Data JPA | (Boot managed) | Persistência ORM |
| LangChain4j | 0.36.2 | Framework de integração com LLMs |
| Google Gemini | gemini-2.5-flash | Modelo de linguagem (LLM) |
| PostgreSQL | 16 | Banco de dados relacional |
| Liquibase | (Boot managed) | Versionamento de schema |
| jjwt | 0.12.6 | Geração e validação de JWT |
| Bucket4j | 8.10.1 | Rate Limiting em memória |
| OpenCSV | 5.9 | Parser de arquivos CSV |
| SpringDoc OpenAPI | 2.8.5 | Documentação Swagger UI |
| Lombok | (Boot managed) | Redução de boilerplate |
| Docker Compose | — | Infraestrutura local |

---

## 4. Estrutura do Projeto

```
src/main/java/br/com/langchain4j/
├── ai/
│   ├── LangChain4jConfig.java       # Configuração do modelo Gemini e AiServices
│   ├── RestaurantAssistant.java     # Interface do agente de IA (AiService)
│   └── RestaurantTools.java        # Ferramentas de consulta ao banco (@Tool)
├── config/
│   └── OpenApiConfig.java          # Configuração do Swagger/OpenAPI
├── controller/
│   ├── AuthController.java         # Endpoints de autenticação
│   ├── ChatController.java         # Endpoint do chatbot
│   └── RestaurantController.java   # CRUD de restaurantes
├── exception/
│   ├── GlobalExceptionHandler.java # Tratamento centralizado de erros
│   └── TokenRefreshException.java  # Exceção de refresh token inválido
├── model/
│   ├── RefreshToken.java           # Entidade de refresh token
│   ├── Restaurant.java             # Entidade principal de restaurante
│   ├── Role.java                   # Entidade de perfil/role
│   └── User.java                   # Entidade de usuário
├── payload/
│   ├── request/                    # DTOs de entrada
│   └── response/                   # DTOs de saída
├── repository/                     # Interfaces JPA Repository
├── security/
│   ├── jwt/                        # Filtro e utilitários JWT
│   ├── ratelimit/                  # Filtro e serviço de Rate Limiting
│   ├── services/                   # UserDetailsService e RefreshTokenService
│   └── WebSecurityConfig.java      # Configuração central do Spring Security
└── service/
    └── CsvDataIngestionService.java # Ingestão do dataset CSV
```

---

## 5. Infraestrutura com Docker

O projeto utiliza Docker Compose para provisionar o banco de dados PostgreSQL e o pgAdmin de forma isolada e reproduzível:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: langchain4j_db
      POSTGRES_USER: ${SPRING_DATASOURCE_USERNAME}
      POSTGRES_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gmontinny -d langchain4j_db"]

  pgadmin:
    image: dpage/pgadmin4:latest
    ports:
      - "5050:80"
    depends_on:
      postgres:
        condition: service_healthy
```

O `healthcheck` garante que o pgAdmin só sobe após o PostgreSQL estar pronto para aceitar conexões, evitando race conditions na inicialização.

Para subir o ambiente:

```bash
docker compose up -d
```

---

## 6. Versionamento de Banco com Liquibase

O Liquibase gerencia todas as migrações de schema de forma versionada e auditável. O arquivo mestre (`db.changelog-master.xml`) orquestra os changesets individuais, garantindo que o banco evolua de forma controlada em qualquer ambiente.

Configuração no `application.yml`:

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
```

Cada changeset cria tabelas como `users`, `roles`, `user_roles`, `refresh_tokens` e `restaurants`, com constraints e índices adequados.

---

## 7. Ingestão de Dados CSV

O `CsvDataIngestionService` carrega automaticamente o dataset Zomato na inicialização da aplicação usando a anotação `@PostConstruct`. A ingestão é idempotente: se o banco já contiver registros, ela é pulada.

**Pontos técnicos relevantes:**

- Leitura com `OpenCSV` usando encoding `ISO-8859-1` (necessário para o dataset Zomato)
- Processamento em lotes de 500 registros com `saveAll()` para performance
- Parsing defensivo de cada campo com tratamento de exceções por linha
- Configurável via propriedade `csv.auto-ingest-on-startup`

```java
@PostConstruct
public void init() {
    if (autoIngestOnStartup) {
        ingestCsvData(csvFilePath);
    }
}

@Transactional
public int ingestCsvData(String filePath) {
    long count = restaurantRepository.count();
    if (count > 0) {
        log.info("Base já contém {} registros. Ingestão pulada.", count);
        return (int) count;
    }
    // processa em batches de 500...
}
```

O endpoint `POST /api/restaurants/ingest` (restrito a `ROLE_ADMIN`) permite re-executar a ingestão manualmente quando necessário.

---

## 8. Autenticação e Segurança com JWT

A segurança é implementada com Spring Security em modo **stateless** (sem sessão HTTP), usando JWT para autenticação e controle de acesso baseado em roles.

### 8.1 Fluxo de Autenticação

```
1. POST /api/auth/signup  → cadastra usuário com roles
2. POST /api/auth/signin  → retorna { token, refreshToken, ... }
3. GET  /api/restaurants  → Authorization: Bearer <token>
4. POST /api/auth/refreshtoken → renova o access token expirado
```

### 8.2 Geração e Validação do JWT

O `JwtUtils` usa a biblioteca `jjwt 0.12.6` com assinatura HMAC-SHA256:

```java
public String generateTokenFromUsername(String username) {
    return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(key())
            .compact();
}
```

### 8.3 Refresh Token

O Refresh Token é persistido no banco de dados com data de expiração. Quando o Access Token expira (padrão: 24h), o cliente usa o Refresh Token (padrão: 7 dias) para obter um novo par de tokens sem precisar fazer login novamente.

### 8.4 Configuração do Security Filter Chain

```java
http.csrf(AbstractHttpConfigurer::disable)
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
        .anyRequest().authenticated()
    );

http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
```

Os filtros são encadeados na ordem: `RateLimitFilter` → `AuthTokenFilter` → `UsernamePasswordAuthenticationFilter`.

---

## 9. Rate Limiting com Bucket4j

O `RateLimitingService` implementa o algoritmo **Token Bucket** por IP usando Bucket4j, protegendo a API contra abuso e ataques de força bruta.

```java
@Service
public class RateLimitingService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, this::newBucket);
    }

    private Bucket newBucket(String key) {
        Refill refill = Refill.greedy(tokens, Duration.ofMinutes(refillMinutes));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
```

O `RateLimitFilter` intercepta cada requisição, resolve o bucket pelo IP do cliente e retorna HTTP `429 Too Many Requests` com os cabeçalhos `X-Rate-Limit-Remaining` e `X-Rate-Limit-Retry-After-Seconds` quando o limite é atingido.

Configuração padrão: **20 requisições por minuto por IP**, ajustável via variáveis de ambiente.

---

## 10. Integração com LangChain4j e Google Gemini

### 10.1 Configuração do Modelo

O `LangChain4jConfig` instancia o modelo Gemini e constrói o agente de IA usando o padrão `AiServices` do LangChain4j:

```java
@Bean
public ChatLanguageModel chatLanguageModel() {
    return GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)   // gemini-2.5-flash
            .temperature(temperature) // 0.3 (respostas mais determinísticas)
            .build();
}

@Bean
public RestaurantAssistant restaurantAssistant(
        ChatLanguageModel chatLanguageModel,
        RestaurantTools restaurantTools) {
    return AiServices.builder(RestaurantAssistant.class)
            .chatLanguageModel(chatLanguageModel)
            .tools(restaurantTools)
            .build();
}
```

A temperatura `0.3` foi escolhida para equilibrar criatividade e precisão nas respostas sobre dados estruturados.

### 10.2 O AiService: RestaurantAssistant

O `RestaurantAssistant` é uma interface simples que o LangChain4j implementa dinamicamente em runtime via proxy:

```java
public interface RestaurantAssistant {

    @SystemMessage({
        "Você é um assistente especialista sobre a base de dados de restaurantes do Zomato.",
        "Sempre utilize as ferramentas disponíveis quando precisar pesquisar restaurantes.",
        "Forneça respostas claras e úteis em português (ou no idioma solicitado pelo usuário)."
    })
    String chat(@UserMessage String userMessage);
}
```

O `@SystemMessage` define o comportamento e a personalidade do agente. O `@UserMessage` mapeia o parâmetro do método para a mensagem do usuário enviada ao modelo.

---

## 11. Function Calling com Tools

O **Function Calling** (ou Tool Use) é o mecanismo que permite ao LLM invocar funções Java reais para buscar dados atualizados, superando a limitação do conhecimento estático do modelo.

O `RestaurantTools` expõe três ferramentas ao modelo:

```java
@Tool("Busca restaurantes por cidade ou culinária no banco de dados")
public String searchRestaurants(String city, String cuisine) {
    var page = restaurantRepository.searchRestaurants(city, cuisine, null, PageRequest.of(0, 10));
    return page.getContent().stream()
            .map(this::formatRestaurantSummary)
            .collect(Collectors.joining("\n---\n"));
}

@Tool("Retorna os top 10 restaurantes com melhores avaliações")
public String getTopRatedRestaurants() {
    return restaurantRepository.findTop10Rated().stream()
            .map(this::formatRestaurantSummary)
            .collect(Collectors.joining("\n---\n"));
}

@Tool("Retorna estatísticas gerais da base de restaurantes")
public String getRestaurantStats() {
    return "Total de restaurantes cadastrados: " + restaurantRepository.count();
}
```

### Como o Function Calling funciona internamente:

```
1. Usuário: "Quais os melhores restaurantes indianos em Mumbai?"
2. LangChain4j envia a pergunta + descrições das tools ao Gemini
3. Gemini responde: "Preciso chamar searchRestaurants(city='Mumbai', cuisine='Indian')"
4. LangChain4j invoca o método Java correspondente
5. O resultado do banco é enviado de volta ao Gemini como contexto
6. Gemini gera a resposta final em linguagem natural
```

Esse ciclo pode se repetir múltiplas vezes (multi-step reasoning) até o modelo considerar que tem informação suficiente para responder.

---

## 12. API REST e Documentação Swagger

### 12.1 Endpoints

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| POST | `/api/auth/signup` | Público | Cadastro de usuário |
| POST | `/api/auth/signin` | Público | Login e geração de tokens |
| POST | `/api/auth/refreshtoken` | Público | Renovação do access token |
| POST | `/api/chat` | JWT | Conversa com o agente de IA |
| GET | `/api/restaurants` | JWT | Listagem paginada com filtros |
| GET | `/api/restaurants/{id}` | JWT | Detalhes de um restaurante |
| POST | `/api/restaurants` | ADMIN | Cadastro de restaurante |
| PUT | `/api/restaurants/{id}` | ADMIN | Atualização de restaurante |
| DELETE | `/api/restaurants/{id}` | ADMIN | Exclusão de restaurante |
| POST | `/api/restaurants/ingest` | ADMIN | Re-executa ingestão do CSV |

### 12.2 Swagger UI

A documentação interativa está disponível em `http://localhost:8080/swagger-ui.html`. Para testar endpoints protegidos:

1. Execute `POST /api/auth/signin` com suas credenciais
2. Copie o valor do campo `token` da resposta
3. Clique em **Authorize** e informe o token no campo Bearer Authentication
4. Todos os endpoints protegidos ficam disponíveis para teste

---

## 13. Fluxo Completo de uma Requisição de Chat

Abaixo o fluxo end-to-end de uma pergunta ao chatbot:

```
POST /api/chat
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "message": "Quais são os 5 melhores restaurantes e qual a cidade deles?"
}
```

**Internamente:**

```
1. RateLimitFilter     → verifica bucket do IP (consome 1 token)
2. AuthTokenFilter     → valida JWT, popula SecurityContext
3. ChatController      → recebe ChatRequest validado (@Valid)
4. RestaurantAssistant → encaminha ao LangChain4j runtime
5. Gemini API          → analisa a pergunta, decide chamar getTopRatedRestaurants()
6. RestaurantTools     → executa query: SELECT r FROM Restaurant r ORDER BY aggregateRating DESC LIMIT 10
7. Gemini API          → recebe os dados, gera resposta em português
8. ChatController      → retorna ChatResponse com resposta + modelo + timestamp
```

**Resposta:**

```json
{
  "response": "Aqui estão os 5 melhores restaurantes avaliados na base Zomato:\n\n1. **Toit** - Bangalore, Índia (Nota: 4.9 ⭐)\n...",
  "model": "gemini-2.5-flash",
  "timestamp": "2026-10-17T07:55:00Z"
}
```

---

## 14. Configuração e Variáveis de Ambiente

O projeto segue o princípio **12-Factor App**: toda configuração sensível é externalizada via variáveis de ambiente, com valores padrão para desenvolvimento local.

```yaml
# application.yml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/langchain4j_db}
    username: ${SPRING_DATASOURCE_USERNAME:gmontinny}
    password: ${SPRING_DATASOURCE_PASSWORD:Gmontinny2026}

security:
  jwt:
    secret: ${JWT_SECRET:404E635266556A586E...}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}
    refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:604800000}

gemini:
  api-key: ${GEMINI_API_KEY:sua-chave-aqui}
  model-name: ${GEMINI_MODEL_NAME:gemini-2.5-flash}
  temperature: ${GEMINI_TEMPERATURE:0.3}

ratelimit:
  capacity: ${RATE_LIMIT_CAPACITY:20}
  refill-minutes: ${RATE_LIMIT_REFILL_MINUTES:1}
```

O arquivo `.env.example` documenta todas as variáveis suportadas, e o `.env` (ignorado pelo git) contém os valores reais de desenvolvimento.

---

## 15. Conclusão

Este projeto demonstra que é totalmente viável construir aplicações de IA production-ready no ecossistema Java/Spring Boot, combinando:

**Sobre a integração com IA:**
- O LangChain4j abstrai toda a complexidade de comunicação com a API do Gemini, gerenciamento de contexto e orquestração de Function Calling
- O padrão `AiServices` com interfaces Java torna o código limpo, testável e desacoplado do modelo específico
- O Function Calling é o diferencial que transforma um chatbot genérico em um agente especializado, capaz de consultar dados reais em tempo real

**Sobre a arquitetura:**
- A separação em camadas (controller → service → repository) mantém o código organizado mesmo com a adição da camada de IA
- Spring Security com JWT stateless é a abordagem correta para APIs REST consumidas por múltiplos clientes
- Rate Limiting com Bucket4j é simples de implementar e essencial para proteger endpoints de IA, que tendem a ser computacionalmente caros

**Sobre as escolhas técnicas:**
- Java 25 + Spring Boot 4.x representam o estado da arte do ecossistema Java em 2025/2026
- Liquibase garante que o schema do banco evolua de forma controlada e auditável em todos os ambientes
- Docker Compose elimina a fricção de setup do ambiente de desenvolvimento

**Próximos passos naturais para evolução do projeto:**
- Adicionar memória de conversação (chat history) ao `RestaurantAssistant` com `@MemoryId`
- Implementar RAG (Retrieval-Augmented Generation) com embeddings para busca semântica
- Adicionar cache Redis para o Rate Limiting (substituindo o `ConcurrentHashMap` em memória)
- Implementar testes de integração com Testcontainers para PostgreSQL
- Adicionar observabilidade com Micrometer e exportação para Prometheus/Grafana

---

## 16. Referências

### Documentação Oficial

- [LangChain4j Documentation](https://docs.langchain4j.dev/) — Documentação oficial do framework, incluindo AiServices, Tools e integrações com modelos
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j) — Repositório oficial com exemplos e changelog
- [Google Gemini API Documentation](https://ai.google.dev/gemini-api/docs) — Referência da API do Google Gemini, modelos disponíveis e Function Calling
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/) — Documentação oficial do Spring Boot 4.x
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/) — Guia completo de configuração do Spring Security
- [Liquibase Documentation](https://docs.liquibase.com/) — Guia de uso e melhores práticas do Liquibase
- [Bucket4j Documentation](https://bucket4j.com/) — Documentação do Bucket4j com exemplos de Rate Limiting
- [SpringDoc OpenAPI](https://springdoc.org/) — Integração do Swagger UI com Spring Boot

### Artigos e Guias Técnicos

- [Baeldung — Spring Security JWT](https://www.baeldung.com/spring-security-oauth-jwt) — Implementação de autenticação JWT com Spring Security
- [Baeldung — LangChain4j with Spring Boot](https://www.baeldung.com/java-langchain4j) — Introdução ao LangChain4j no ecossistema Spring
- [Baeldung — Rate Limiting with Bucket4j](https://www.baeldung.com/spring-bucket4j) — Implementação de Rate Limiting com Bucket4j e Spring
- [Baeldung — Liquibase with Spring Boot](https://www.baeldung.com/liquibase-refactor-schema-of-java-app) — Versionamento de banco com Liquibase
- [Google AI for Developers — Function Calling](https://ai.google.dev/gemini-api/docs/function-calling) — Guia oficial de Function Calling com Gemini

### Dataset

- [Zomato Restaurant Dataset — Kaggle](https://www.kaggle.com/datasets/shrutimehta/zomato-restaurants-data) — Dataset público de restaurantes do Zomato utilizado no projeto

### Tecnologias e Bibliotecas

- [jjwt (Java JWT)](https://github.com/jwtk/jjwt) — Biblioteca Java para criação e validação de JWTs
- [OpenCSV](https://opencsv.sourceforge.net/) — Biblioteca Java para parsing de arquivos CSV
- [Lombok](https://projectlombok.org/) — Redução de boilerplate em Java com anotações
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/) — Driver oficial JDBC para PostgreSQL

---

*Projeto desenvolvido com Java 25, Spring Boot 4.1.1, LangChain4j 0.36.2 e Google Gemini 2.5 Flash.*
