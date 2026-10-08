# LipariBank Day 06 — JWT & OAuth2: Protezione Stateless

Benvenuto nel progetto **liparibank-day06-broken**.

Applicazione Spring Boot 3.3.4 (Java 21) con flusso JWT completo:
`POST /api/v1/auth/register` → `POST /api/v1/auth/login` → `GET /api/v1/accounts`
con `Authorization: Bearer <token>` → `POST /api/v1/auth/refresh`.

Il progetto contiene **3 bug intenzionali** da trovare e correggere.

---

## Prerequisiti

- Java 21
- Maven (o usa il wrapper incluso)

---

## Come avviare il progetto

```bash
./mvnw spring-boot:run
```

Su Windows:

```cmd
mvnw.cmd spring-boot:run
```

---

## Credenziali di test

| Username   | Password      | Ruolo          |
|------------|---------------|----------------|
| `admin`    | `admin123`    | `ROLE_ADMIN`   |
| `customer` | `customer123` | `ROLE_CUSTOMER`|

---

## Le 3 Missioni

### Missione 1 — L'applicazione si avvia ma il login restituisce HTTP 500

L'applicazione parte senza errori di startup. La chiamata
`POST /api/v1/auth/login` con credenziali valide restituisce però `500 Internal Server Error`.
Il log riporta un'eccezione legata alla generazione del JWT.

**Sintomo:** HTTP 500 al primo tentativo di login. Nessun token viene generato.
Il log mostra un errore relativo alla configurazione della firma JWT.

---

### Missione 2 — Il token funziona ma tutti gli endpoint danno 403

Dopo aver risolto la Missione 1, il login restituisce correttamente un `access_token`.
Aggiungendo il token all'header `Authorization: Bearer <token>`, la chiamata
`GET /api/v1/accounts` restituisce `403 Forbidden`. I log di Spring Security mostrano
che l'utente viene autenticato (il SecurityContext viene popolato) ma l'accesso viene
negato. Il metodo `DELETE /api/v1/accounts/{id}` annotato con `@PreAuthorize("hasRole('ADMIN')")`
dà 403 anche con l'utente admin.

**Sintomo:** token JWT valido accettato, autenticazione OK, ma authorities sempre vuote.
Qualsiasi endpoint con `@PreAuthorize` o `hasRole()` restituisce 403.

---

### Missione 3 — Il secret JWT è nel codice sorgente

Durante la code review, il Tech Lead nota che il secret usato per firmare i JWT
è hardcoded direttamente nel sorgente Java. Il file è già nel repository Git.
Qualsiasi persona con accesso al repository può leggere il secret e generare token
JWT validi per qualsiasi utente.

**Sintomo:** secret sensibile visibile nel codice sorgente. Il Tech Lead ferma
la review e richiede una correzione immediata prima del merge.

---

## Come testare (dopo aver risolto le missioni)

```bash
# Missione 1 — login (deve rispondere 200 con token)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer","password":"customer123"}'

# Crea conti di test (salva il token dalla risposta del login)
TOKEN="<incolla_qui_il_access_token>"

curl -X POST http://localhost:8080/api/v1/accounts/seed \
  -H "Authorization: Bearer $TOKEN"

# Missione 2 — lista conti (deve rispondere 200)
curl http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer $TOKEN"

# Login come admin e testa delete (deve rispondere 204)
ADMIN_TOKEN="<incolla_qui_il_token_admin>"
curl -X DELETE http://localhost:8080/api/v1/accounts/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Refresh token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<incolla_il_refresh_token>"}'
```

H2 Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
(JDBC URL: `jdbc:h2:mem:liparibankdb`)

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Struttura del progetto

```
src/main/java/com/lipari/bank/
├── LipariBankApplication.java
├── auth/
│   ├── model/BankUser.java
│   ├── model/Role.java
│   ├── BankUserRepository.java
│   ├── RoleRepository.java
│   ├── LipariBankUserDetailsService.java
│   ├── AuthService.java
│   ├── AuthController.java
│   └── dto/
│       ├── AuthRequest.java
│       ├── RegisterRequest.java
│       ├── RefreshRequest.java
│       └── AuthResponse.java
├── account/
│   ├── Account.java
│   ├── AccountRepository.java
│   ├── AccountService.java
│   └── AccountController.java
└── shared/
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── OpenApiConfig.java
    │   └── DataInitializer.java
    └── security/
        ├── JwtService.java
        ├── JwtAuthenticationFilter.java
        ├── LipariBankAccessDeniedHandler.java
        └── LipariBankAuthenticationEntryPoint.java
```

---

## Bonus Mission — Feature da Implementare (opzionale, ~1 ora)

Una volta risolti i 3 bug, implementa la seguente feature per consolidare i concetti del giorno.

### Logout con invalidazione del refresh token

Il sistema attuale non ha un meccanismo di logout: una volta ottenuto un `refresh_token`, esso rimane valido fino alla scadenza naturale anche se l'utente vuole disconnettersi. In un sistema bancario questo è inaccettabile.

**Cosa implementare:**

Aggiungi un endpoint `POST /api/v1/auth/logout` che riceve nel body il `refreshToken` da invalidare. Dopo la chiamata, quel refresh token specifico non deve più essere accettato da `POST /api/v1/auth/refresh`.

Per implementare l'invalidazione puoi scegliere l'approccio che preferisci: aggiungere un campo `refreshToken` all'entity `BankUser` (il sistema salva l'ultimo refresh token valido e lo cancella al logout), oppure mantenere una lista in memoria dei token invalidati. Entrambi gli approcci sono validi per questo esercizio.

L'endpoint di logout deve essere accessibile solo agli utenti autenticati (richiede `Authorization: Bearer <access_token>` valido).

**Criteri di accettazione:**

- `POST /api/v1/auth/logout` con access token valido e refresh token valido risponde `200 OK`.
- Dopo il logout, `POST /api/v1/auth/refresh` con lo stesso refresh token risponde `401 Unauthorized`.
- Un refresh token mai usato per il logout continua a funzionare normalmente.
- L'endpoint `/logout` senza access token risponde `401`.
- Il login rimane funzionante: un nuovo login genera nuovi token validi indipendentemente dal logout precedente.

---

*LipariBank Prompt Bootcamp — JWT & OAuth2: Protezione Stateless — Day 06*
