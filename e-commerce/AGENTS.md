# AGENTS.md

## Arquitectura

Hexagonal (DDD) — **modular por dominio** bajo `com.example.e_commerce`:

```
com.example.e_commerce/
├── ECommerceApplication.java
├── shared/                  ← cross-cutting (eventos, errores, infraestructura)
│   ├── event/               ClaimStatusChangedEvent, ClaimEventPublisher (publicador RabbitMQ)
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
└── notification/            ← módulo Notification (sin entidad ni web)
    ├── domain/              NotificationService (interfaz)
    └── infrastructure/      LogNotificationService (mock), ClaimStatusChangedConsumer (@RabbitListener)
└── ai/                      ← módulo AI (Spring AI + Groq)
    ├── domain/              AiService (interfaz)
    ├── application/         AiChatService, DTOs (AiChatRequest/AiChatResponse)
    ├── infrastructure/      GroqAiService (implementación con ChatClient)
    └── web/                 AiChatController
```

| Capa | Rol | Dependencias permitidas |
|---|---|---|
| `domain/` | Modelos, enums, interfaces repositorio, validador | solo Java puro (sin Spring), cross-module solo por interfaces |
| `application/` | Servicios, mappers, DTOs | `domain/` propio + interfaces de otros módulos |
| `infrastructure/` | Entidades JPA, implementaciones repositorio | `application/` (via mapper) + entidades de otros módulos (JPA FK) |
| `web/` | Controladores | `application/` propio |

**Reglas clave:**
- Cada módulo es **auto-contenido**: su domain, application, infrastructure y web están dentro de su carpeta
- Modelos de dominio = POJOs puros con Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`), sin anotaciones JPA
- Mappers en `application/mapper/` = clases utilitarias estáticas
- Validador en `domain/validator/` = `final class` con constructor privado, todos métodos estáticos
- Servicios siguen el patrón **un servicio = una capacidad** (ej: `ClaimCreationService`, `ClaimReviewService`, `ClaimRefundService`)
- Interfaces repositorio en `domain/repository/`, JPA repos en `infrastructure/repository/`, implementaciones en `infrastructure/repository/impl/`
- Cross-module dependencies se hacen **solo por interfaces** (ej: `ClaimValidator` inyecta `UserRepository` del módulo user)
- Para agregar un nuevo dominio (ej: `product/`), crear carpeta con la misma estructura interna

## Build & Ejecución

```bash
docker compose up -d                              # inicia PostgreSQL 16
./mvnw spring-boot:run                            # servidor desarrollo
./mvnw compile                                    # solo compilar
```

- Context path: `/api/v1` (configurado en `application.yaml`)
- Archivo `.env` carga credenciales BD via `spring-dotenv`
- **Flyway controla el esquema y los datos** (`spring.jpa.hibernate.ddl-auto=validate`): las entidades se validan contra el esquema, NO lo crean ni modifican

## Migraciones y Datos de Prueba (Flyway)

Flyway es el estándar de migraciones: scripts SQL **versionados** que se ejecutan **una sola vez** (rastreados en `flyway_schema_history`).

**Ubicación:** `src/main/resources/db/migration/`

```
db/migration/
├── V1__create_tables.sql   # esquema: users, claims, claim_history
├── V2__seed_data.sql       # datos de prueba (usuarios, claims, historial)
└── V3__...sql              # siguiente cambio (cuando agregues una entidad)
```

**Reglas:**
- `ddl-auto` está en `validate` — NUNCA volver a `update`. Si cambias una entidad JPA, crea una migración nueva (`V3__...sql`)
- Los datos de prueba (seed) van en migraciones versionadas, NO en Postman a mano
- El seed actual incluye roles SUPPORT y FINANCE (que la API no puede crear) y claims en TODOS los estados (`PENDING`, `IN_REVIEW`, `APPROVED`, `REJECTED`, `REFUNDED`) con su historial
- Al insertar ids explícitos en el seed, resetea las secuencias con `setval()` al final para que la app siga generando ids nuevos sin conflictos
- Para resetear todo desde cero: `docker compose down -v && docker compose up -d` (borra volúmenes)

## Eventos Asíncronos (US-01 — RabbitMQ)

Flujo: cambio de estado de claim → `ClaimStatusChangedEvent` → RabbitMQ.

```
PATCH /claims/{id}/review|refund
  → ClaimReviewService/ClaimRefundService (@Transactional)
  → eventPublisher.publishEvent(ClaimStatusChangedEvent)  ← se difiere
  → COMMIT exitoso → ClaimEventPublisher (@EventListener)
  → rabbitTemplate.convertAndSend() → claim.exchange → claim.status.queue
```

**Garantía clave:** el evento se publica **solo tras el COMMIT**. Si hay rollback, el evento nunca llega a RabbitMQ (Spring difiere la entrega hasta que la transacción termina).

**Componentes:**
- `shared/event/ClaimStatusChangedEvent.java` — payload `{claimId, customerUserId, changedByUser, previousStatus, newStatus, timestamp}` (`customerUserId` = cliente dueño del claim; `changedByUser` = agente que hizo el cambio)
- `shared/event/ClaimEventPublisher.java` — `@EventListener` + `RabbitTemplate`
- `shared/config/RabbitMQConfig.java` — exchange directo `claim.exchange`, cola `claim.status.queue`, Dead Letter (`claim.dlx`/`claim.dlq`)
- `shared/config/RabbitMQJsonConfig.java` — serializa mensajes como JSON (**Jackson 3 / `JacksonJsonMessageConverter`**; NO usar `Jackson2JsonMessageConverter` que es de Jackson 2 y no está en el classpath de Spring Boot 4)

**Broker local:** RabbitMQ corre en Docker (Management UI: http://localhost:15672, guest/guest). Las colas/exchanges se crean perezosamente al abrirse la primera conexión (primer evento publicado).

## Notificaciones (US-02 — RabbitMQ)

Flujo: el consumidor lee el evento de la cola y notifica al cliente del claim.

```
claim.status.queue → ClaimStatusChangedConsumer (@RabbitListener)
  → UserRepository.findById(event.getCustomerUserId())  → user.getEmail()
  → NotificationService.sendClaimStatusNotification(email, event)
```

**Componentes:**
- `notification/domain/NotificationService.java` — interfaz `sendClaimStatusNotification(String recipientEmail, ClaimStatusChangedEvent event)`
- `notification/infrastructure/LogNotificationService.java` — implementación **MOCK**: solo `log.info`. Se reemplazará por `EmailNotificationService` (JavaMailSender + SMTP) implementando la misma interfaz, **sin tocar el consumidor**
- `notification/infrastructure/ClaimStatusChangedConsumer.java` — `@RabbitListener(queues = RabbitMQConfig.CLAIM_STATUS_QUEUE)`

**Reglas de comportamiento:**
- El evento lleva `customerUserId` (dueño del claim, incluido por el productor gratis) y `changedByUser` (agente que hizo el cambio). El email del cliente se resuelve en el consumidor — **no** se consulta el claim (1 sola consulta a BD)
- Si el usuario **ya no existe**: `log.warn` y se consume el mensaje igual (cola limpia, NO se llena la DLQ a propósito)
- Si el listener lanza una excepción: RabbitMQ reintenta y cae al DLQ (`claim.dlq`) tras agotar reintentos
- `@RabbitListener` funciona automáticamente con `spring-boot-starter-amqp` (no hace falta `@EnableRabbit`)

**Pendiente:** reemplazar `LogNotificationService` por `EmailNotificationService` usando `JavaMailSender` (requiere `spring-boot-starter-mail` + SMTP en `application.yaml`, ej. Mailtrap en desarrollo).

## Máquina de Estados del Claim

```
PENDING → IN_REVIEW → APPROVED → REFUNDED
                   ↘ REJECTED ←─────↗
PENDING → CANCELLED (solo el dueño del claim, endpoint /cancel)
```
- Transiciones validadas por `ClaimValidator.validateStatusTransition()` (static `EnumMap`)
- `CANCELLED`, `REJECTED` y `REFUNDED` son estados terminales
- Cancelación validada por `ClaimValidator.validateClaimCanBeCancelled()` (solo desde `PENDING`) y `ClaimValidator.validateUserIsClaimOwner()` (solo el dueño)

## Permisos por Rol

| Rol | Transiciones permitidas | Endpoint |
|---|---|---|
| SUPPORT | `IN_REVIEW`, `APPROVED`, `REJECTED` | `PATCH /claims/{id}/review` |
| FINANCE | `REFUNDED` | `PATCH /claims/{id}/refund` |
| CUSTOMER | ninguna (solo crear claims) | — |

- Validado por `ClaimValidator.validateRoleForTransition()` (static `EnumMap`)
- Sin Spring Security aún — la validación de rol se hace buscando al usuario por `changedByUser`

## Endpoints

Todos bajo `/api/v1`:

| Método | Ruta | Servicio | Notas |
|---|---|---|---|
| `POST` | `/users` | `UserCreationService` | Rol por defecto `CUSTOMER` |
| `POST` | `/claims` | `ClaimCreationService` | Estado por defecto `PENDING` |
| `GET` | `/claims` | `GetAllClaimsService` | Filtro opcional `?status=` |
| `GET` | `/claims/user/{userId}` | `GetCustomerClaimsService` | Valida que el usuario exista |
| `GET` | `/claims/{claimId}` | `GetAllClaimsService` | 404 si no existe |
| `GET` | `/claims/{claimId}/history` | `GetAuditHistoryService` | Historial de cambios del claim |
| `PATCH` | `/claims/{claimId}/review` | `ClaimReviewService` | Restringido a SUPPORT |
| `PATCH` | `/claims/{claimId}/refund` | `ClaimRefundService` | Restringido a FINANCE |
| `PATCH` | `/claims/{claimId}/cancel` | `ClaimCancellationService` | Solo dueño del claim + estado PENDING |

## IA (Spring AI + Groq)

Groq se integra **reutilizando el cliente OpenAI** de Spring AI (no existe starter propio de Groq): `spring-ai-starter-model-openai` + `base-url` apuntando a `https://api.groq.com/openai/v1`. La API key vive en `.env` (`GROQ_API_KEY`) y se referencia en `application.yaml` via `${GROQ_API_KEY}` — nunca se escribe en el yaml ni en código.

- **Dependencia:** `spring-ai-starter-model-openai`, versión gestionada por el BOM `spring-ai-bom` (2.0.x = compatible con Spring Boot 4.x)
- **Endpoint:** `POST /api/v1/ai/chat` con `{ "message": "..." }` → `{ "message": "<respuesta>" }`
- **Capas:** `ai/domain/AiService` (interfaz pura) → `ai/infrastructure/GroqAiService` (impl con `ChatClient`) → `ai/application/AiChatService` (DTOs) → `ai/web/AiChatController`
- **Pendiente:** poner la API key real en `.env` antes de probar; el modelo actual es `llama-3.3-70b-versatile`

## Manejo de Errores

Centralizado via `GlobalExceptionHandler` (`@RestControllerAdvice`) que retorna `ApiError`:

| Excepción | HTTP |
|---|---|
| `ClaimNotFoundException` | 404 |
| `UserNotFoundException` | 404 |
| `InvalidStatusTransitionException` | 400 |
| `InvalidRoleForTransitionException` | 403 |
| `InvalidCancellationException` | 400 (claim no está en PENDING para cancelar) |
| `NotClaimOwnerException` | 403 (solo el dueño puede cancelar) |
| `DuplicateEmailException` | 409 |
| `MethodArgumentNotValidException` | 400 (con subErrors por campo) |
| `DataIntegrityViolationException` | 409 |
| `Exception` (catch-all) | 500 |

## Completado

- [x] Modelos de dominio, enums, interfaces repositorio
- [x] Entidades JPA + implementaciones repositorio
- [x] Servicios (creación, revisión, reembolso, consultas)
- [x] Controladores + manejador global de errores
- [x] Validación de transiciones de estado (`ClaimValidator`)
- [x] Validación de roles (`ClaimValidator`)
- [x] Trazabilidad (`ClaimHistory`) en cada cambio de estado
- [x] **US-01**: publicación asíncrona de eventos en RabbitMQ (`ClaimStatusChangedEvent`, `@EventListener` + `RabbitTemplate`, solo tras COMMIT)
- [x] **US-02**: consumidor (`@RabbitListener`) de eventos para notificar al cliente (implementación mock `LogNotificationService`)
- [x] **US-03**: cancelación de claims (`PATCH /claims/{id}/cancel`), estado `CANCELLED`, validaciones de estado y dueño, evento publicado
- [x] **Flyway**: migraciones versionadas + seed de datos de prueba (`ddl-auto=validate`)

## Pendiente

- [ ] Autenticación y autorización (JWT / Spring Security) — reemplazará `validateRoleForTransition` con `@PreAuthorize`
- [ ] Endpoints DELETE (los repos tienen `delete()` pero no hay controladores)
- [ ] Tests de integración (requieren DB)
- [ ] Dockerfile productivo (actualmente vacío)
- [ ] Agregar `@NotNull` en `ClaimRequest.orderId`
- [ ] **US-02 email real**: reemplazar `LogNotificationService` por `EmailNotificationService` (JavaMailSender + SMTP + `spring-boot-starter-mail`)
- [ ] Tests unitarios de US-03 (`ClaimCancellationServiceTest`, endpoint `/cancel` en `ClaimControllerTest`, handlers en `GlobalExceptionHandlerTest`)

## 🧪 Estándar de Pruebas Unitarias (Spring Boot)
Cuando te pida crear pruebas unitarias, debes seguir estas reglas simples:

- **Framework:** JUnit 5 (`@Test`) y Mockito para simular dependencias.
- **Ubicación:** Guardar en la carpeta existente `src/test/java/`, usando la misma estructura de paquetes que la clase original.
- **Nombramiento de la Clase:** Nombre de la clase a probar + la palabra `Test` (Ejemplo: `CreateClaimUseCaseTest`).
- **Aislamiento:** Usa `@ExtendWith(MockitoExtension.class)`. Usa `@Mock` para las dependencias y `@InjectMocks` para la clase que estamos probando. NO uses `@SpringBootTest`.
- **Nombramiento de Métodos:** Formato claro `should[ComportamientoEsperado]_When[Escenario]` (Ejemplo: `shouldCreateClaim_WhenDataIsValid`).
- **Estructura Interna del Test (AAA):** Divide cada método en tres pasos sencillos y comentados:
    1. `// Given` (Preparar los datos de entrada y comportamiento de los mocks)
    2. `// When` (Ejecutar el método del caso de uso)
    3. `// Then` (Verificar el resultado con assertions)
