# Módulo de Claims & Returns — E-Commerce API

## Descripción general

API REST para gestionar reclamos y devoluciones de clientes en una plataforma de e-commerce. Construida con **Java 21 + Spring Boot 4** y **PostgreSQL 16**, con eventos asíncronos vía **RabbitMQ** y capacidades de IA vía **Spring AI + Groq**.

## Arquitectura del sistema

Hexagonal (DDD) — **modular por dominio** bajo `com.example.e_commerce`:

```
com.example.e_commerce/
├── ECommerceApplication.java
├── shared/                  ← cross-cutting (eventos, errores, infraestructura)
│   ├── event/               ClaimStatusChangedEvent, ClaimCreatedEvent, ClaimEventPublisher (publicador RabbitMQ)
│   ├── exception/           ApiError, GlobalExceptionHandler
│   └── config/              RabbitMQConfig (colas/exchange/DLQ), RabbitMQJsonConfig (serialización JSON)
├── claim/                   ← módulo Claim (dominio auto-contenido)
│   ├── domain/              model, enums, exceptions, repository (interfaces), validator
│   ├── application/         DTOs, mappers, services
│   ├── infrastructure/      entities JPA, JPA repos, repository impls
│   └── web/                 controller
├── user/                    ← módulo User (dominio auto-contenido)
│   ├── domain/              model, enums, exceptions, repository (interface)
│   ├── application/         DTOs, mapper, service
│   ├── infrastructure/      entity JPA, JPA repo, repository impl
│   └── web/                 controller
├── notification/            ← módulo Notification (sin entidad ni web)
│   ├── domain/              NotificationService (interfaz)
│   └── infrastructure/      EmailNotificationService (JavaMailSender + SMTP), ClaimStatusChangedConsumer (@RabbitListener)
└── ai/                      ← módulo AI (Spring AI + Groq): cada capacidad IA es un sub-paquete auto-contenido
    └── claimclassifier/     ← US-AI-02: categorización/urgencia/resumen de claims
        ├── domain/          model/ClaimAiMetadata (record), repository/ClaimClassifier (interfaz)
        ├── application/     service/ClaimCategorizationService
        └── infrastructure/  GroqClaimClassifierImp (impl con ChatClient), ClaimAiConsumer (@RabbitListener)
```

| Capa | Rol | Dependencias permitidas |
|---|---|---|
| `domain/` | Modelos, enums, interfaces repositorio, validador | solo Java puro (sin Spring), cross-module solo por interfaces |
| `application/` | Servicios, mappers, DTOs | `domain/` propio + interfaces de otros módulos |
| `infrastructure/` | Entidades JPA, implementaciones repositorio | `application/` (via mapper) + entidades de otros módulos (JPA FK) |
| `web/` | Controladores | `application/` propio |

**Reglas clave:**
- Cada módulo es **auto-contenido**: su domain, application, infrastructure y web están dentro de su carpeta
- Modelos de dominio = POJOs puros con Lombok, sin anotaciones JPA
- Mappers en `application/mapper/` = clases utilitarias estáticas
- Servicios siguen el patrón **un servicio = una capacidad**
- Cross-module dependencies se hacen **solo por interfaces**

---

## Ciclo de vida del Claim (Máquina de estados)

```
PENDING → IN_REVIEW → APPROVED → REFUNDED
                   ↘ REJECTED ←─────↗
PENDING → CANCELLED (solo el dueño del claim, endpoint /cancel)
```

- Transiciones validadas por `ClaimValidator.validateStatusTransition()`
- `CANCELLED`, `REJECTED` y `REFUNDED` son estados terminales
- Cancelación validada (solo desde `PENDING` y solo por el dueño del claim)

## Permisos por Rol

| Rol | Transiciones permitidas | Endpoint |
|---|---|---|
| SUPPORT | `IN_REVIEW`, `APPROVED`, `REJECTED` | `PATCH /claims/{id}/review` |
| FINANCE | `REFUNDED` | `PATCH /claims/{id}/refund` |
| CUSTOMER | ninguna (solo crear claims) | — |

- Sin Spring Security aún — la validación de rol se hace buscando al usuario por `changedByUser`

---

## Endpoints de la API

Base URL: `/api/v1`

| Método | Ruta | Descripción | Request Body | Respuesta |
|---|---|---|---|---|
| `POST` | `/users` | Registrar usuario (rol por defecto: CUSTOMER) | `{ name, email }` | `201` — `UserResponse` |
| `POST` | `/claims` | Crear reclamo (estado: PENDING) | `{ orderId, description, userId }` | `201` — `ClaimResponse` |
| `GET` | `/claims` | Listar todos los reclamos (filtro opcional `?status=`) | — | `200` — `ClaimResponse[]` |
| `GET` | `/claims/user/{userId}` | Listar reclamos de un usuario | — | `200` — `ClaimResponse[]` |
| `GET` | `/claims/{claimId}` | Obtener un reclamo por ID | — | `200` — `ClaimResponse` |
| `GET` | `/claims/{claimId}/history` | Historial de cambios del reclamo | — | `200` — `ClaimHistoryResponse[]` |
| `PATCH` | `/claims/{claimId}/review` | Cambiar estado (solo SUPPORT) | `{ newStatus, changedByUser }` | `200` — `ClaimResponse` |
| `PATCH` | `/claims/{claimId}/refund` | Procesar reembolso (solo FINANCE) | `{ changedByUser }` | `200` — `ClaimResponse` |
| `PATCH` | `/claims/{claimId}/cancel` | Cancelar reclamo (solo dueño + estado PENDING) | `{ changedByUser }` | `200` — `ClaimResponse` |

`ClaimResponse` incluye además los campos IA (nullable): `category`, `urgency` y `summary`, rellenados de forma asíncrona tras la creación.

### Códigos HTTP

| Código | Cuándo |
|---|---|
| `200` | Éxito |
| `201` | Creado (POST) |
| `400` | Datos inválidos / transición de estado inválida |
| `403` | Rol no autorizado / no es dueño del claim |
| `404` | Usuario o Reclamo no encontrado |
| `409` | Email duplicado / violación de integridad |
| `500` | Error inesperado |

---

## Eventos Asíncronos (US-01 — RabbitMQ)

Flujo: cambio de estado de claim → `ClaimStatusChangedEvent` → RabbitMQ.

```
PATCH /claims/{id}/review|refund
  → ClaimReviewService/ClaimRefundService (@Transactional)
  → eventPublisher.publishEvent(ClaimStatusChangedEvent)  ← se difiere
  → COMMIT exitoso → ClaimEventPublisher (@EventListener)
  → rabbitTemplate.convertAndSend() → claim.exchange → claim.status.queue
```

**Garantía clave:** el evento se publica **solo tras el COMMIT**. Si hay rollback, el evento nunca llega a RabbitMQ.

**Componentes:**
- `shared/event/ClaimStatusChangedEvent.java` — payload `{claimId, customerUserId, changedByUser, previousStatus, newStatus, timestamp}`
- `shared/event/ClaimEventPublisher.java` — `@EventListener` + `RabbitTemplate`
- `shared/config/RabbitMQConfig.java` — exchange directo `claim.exchange`, cola `claim.status.queue`, Dead Letter (`claim.dlx`/`claim.dlq`)

## Notificaciones (US-02 — RabbitMQ)

```
claim.status.queue → ClaimStatusChangedConsumer (@RabbitListener)
  → UserRepository.findById(event.getCustomerUserId())  → user.getEmail()
  → NotificationService.sendClaimStatusNotification(email, event)
```

- `EmailNotificationService` construye y envía un correo HTML vía SMTP (Mailtrap en desarrollo)
- Si el usuario **ya no existe**: `log.warn` y se consume el mensaje igual (cola limpia, NO se llena la DLQ a propósito)
- Si el listener lanza una excepción: RabbitMQ reintenta y cae a la DLQ (`claim.dlq`)

---

## IA — Categorización de Claims (US-AI-02)

Flujo: creación de claim → `ClaimCreatedEvent` → RabbitMQ → módulo `ai/claimclassifier` → Groq → update del claim.

```
POST /claims
  → ClaimCreationService (@Transactional) → save en BD
  → publishEvent(ClaimCreatedEvent)  ← se difiere
  → COMMIT → ClaimEventPublisher.onClaimCreated (@EventListener)
  → claim.exchange → claim.ai.queue
  → ClaimAiConsumer (@RabbitListener)
  → ClaimCategorizationService.categorize(claimId, description)
  → GroqClaimClassifierImp (ChatClient + PromptTemplate + StructuredOutputConverter) → ClaimAiMetadata
  → claim.setCategory/urgency/summary → ClaimRepository.save
```

**Reglas de comportamiento:**
- Los campos `category`, `urgency`, `summary` son **NULLABLE** (V4) y se rellenan de forma **asíncrona** tras la creación
- Si el claim **ya no existe**: `log.warn` y se consume el mensaje igual (cola limpia, NO se llena la DLQ)
- Si la IA falla: excepción → reintentos → DLQ (`claim.dlq`)
- Urgencia como texto libre entre `LOW|MEDIUM|HIGH|CRITICAL`; categoría texto libre

**Stack de IA:** `spring-ai-starter-model-openai` + `base-url` apuntando a `https://api.groq.com/openai/v1` (Groq expone una API compatible con OpenAI). Modelo: `openai/gpt-oss-120b`. La API key se resuelve desde la variable de entorno del sistema `${GROQ_API_KEY}`.

---

## Migraciones (Flyway)

Flyway controla el esquema y los datos (`spring.jpa.hibernate.ddl-auto=validate`): las entidades JPA se validan contra el esquema, NO lo crean ni modifican.

**Ubicación:** `src/main/resources/db/migration/`

```
V1__create_tables.sql   # esquema: users, claims, claim_history
V2__seed_data.sql       # datos de prueba (usuarios, claims, historial)
V3__add_cancelled_status.sql   # estado CANCELLED
V4__add_ai_metadata_to_claims.sql  # category, urgency, summary
```

**Reglas:**
- Nunca se cambia `ddl-auto` de `validate`. Si cambias una entidad, crea una migración nueva (`V5__...sql`)
- Los datos de prueba van en migraciones versionadas, NO en Postman a mano
- Al insertar ids explícitos en el seed, se resetean las secuencias con `setval()`
- Para resetear todo desde cero: `docker compose down -v && docker compose up -d` (borra volúmenes)

---

## Reglas de negocio implementadas

1. **Reclamos nuevos** se crean con estado `PENDING`
2. **Usuarios nuevos** se crean con rol `CUSTOMER` por defecto
3. **Transiciones de estado** siguen la máquina de estados — las inválidas se rechazan
4. **Acceso por rol** — solo SUPPORT revisa, solo FINANCE reembolsa
5. **Traza de auditoría** — cada cambio de estado se registra en `claim_history`
6. **Cancelación** — solo el dueño del claim y solo desde `PENDING`
7. **Notificaciones asíncronas** — cada cambio de estado dispara un evento a RabbitMQ y un correo al cliente
8. **IA asíncrona** — cada claim nuevo se categoriza automáticamente con Groq

---

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Base de datos | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Migraciones | Flyway |
| Mensajería | RabbitMQ |
| Email | Spring Mail + SMTP (Mailtrap en desarrollo) |
| IA | Spring AI (cliente OpenAI) + Groq (`gpt-oss-120b`) |
| Validación | Jakarta Validation |
| Build | Maven (Wrapper incluido) |
| Contenedores | Docker + docker-compose (PostgreSQL, RabbitMQ) |
| Testing | JUnit 5 + Mockito |

---

## Empezando

### Prerrequisitos

- Java 21+
- Docker Desktop

### Variables de entorno

Se cargan como variables de entorno del sistema (via `setx`) — Spring Boot 4 no soporta `spring-dotenv`:

```bash
# IA (Groq)
setx GROQ_API_KEY "tu_api_key"

# SMTP (Mailtrap en desarrollo)
setx MAIL_HOST "sandbox.smtp.mailtrap.io"
setx MAIL_PORT "2525"
setx MAIL_USERNAME "tu_usuario"
setx MAIL_PASSWORD "tu_password"
```

### Ejecución

```bash
# Inicia PostgreSQL + RabbitMQ
docker compose up -d

# Ejecuta la aplicación
./mvnw spring-boot:run
```

- RabbitMQ Management UI: http://localhost:15672 (guest/guest)

### Tests

```bash
# Todos los tests unitarios
./mvnw test

# Compilar solo
./mvnw compile
```

---

## Uso de opencode

Este proyecto se desarrolla con **opencode** (asistente CLI para ingeniería de software). El archivo `AGENTS.md` es la fuente de verdad de las convenciones del proyecto (arquitectura hexagonal, patrón "un servicio = una capacidad", estándar de pruebas unitarias, eventos asíncronos y migraciones Flyway) y es la guía que opencode sigue al implementar cambios y nuevos User Stories.

---

## Pendiente / To Do

- [ ] Autenticación y autorización (JWT / Spring Security)
- [ ] Endpoints DELETE
- [ ] Tests de integración (requieren DB)
- [ ] Dockerfile productivo
- [ ] Agregar `@NotNull` en `ClaimRequest.orderId`
- [ ] Tests unitarios de US-03 (`ClaimCancellationServiceTest`, endpoint `/cancel` en `ClaimControllerTest`, handlers en `GlobalExceptionHandlerTest`)
- [ ] Tests unitarios de US-AI-02 (`ClaimCategorizationServiceTest`)
- [ ] Extraer infra IA compartida a `ai/shared/` (un `ChatClientFactory` que centralice el `ChatClient` para las futuras capacidades IA)