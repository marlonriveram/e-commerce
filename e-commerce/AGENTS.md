# AGENTS.md

## Arquitectura

Hexagonal (DDD) — **modular por dominio** bajo `com.example.e_commerce`:

```
com.example.e_commerce/
├── ECommerceApplication.java
├── shared/                  ← cross-cutting (errores, config, security futuro)
│   ├── exception/           ApiError, GlobalExceptionHandler
│   └── config/              (vacío, para JWT/Security futuro)
├── claim/                   ← módulo Claim (dominio auto-contenido)
│   ├── domain/              model, enums, exceptions, repository (interfaces), validator
│   ├── application/         DTOs, mappers, services
│   ├── infrastructure/      entities JPA, JPA repos, repository impls
│   └── web/                 controller
└── user/                    ← módulo User (dominio auto-contenido)
    ├── domain/              model, enums, exceptions, repository (interface)
    ├── application/         DTOs, mapper, service
    ├── infrastructure/      entity JPA, JPA repo, repository impl
    └── web/                 controller
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
- `spring.jpa.hibernate.ddl-auto=update` — esquema auto-administrado (sin Flyway/Liquibase)

## Máquina de Estados del Claim

```
PENDING → IN_REVIEW → APPROVED → REFUNDED
                   ↘ REJECTED ←─────↗
```
- Transiciones validadas por `ClaimValidator.validateStatusTransition()` (static `EnumMap`)
- `REJECTED` y `REFUNDED` son estados terminales

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

## Manejo de Errores

Centralizado via `GlobalExceptionHandler` (`@RestControllerAdvice`) que retorna `ApiError`:

| Excepción | HTTP |
|---|---|
| `ClaimNotFoundException` | 404 |
| `UserNotFoundException` | 404 |
| `InvalidStatusTransitionException` | 400 |
| `InvalidRoleForTransitionException` | 403 |
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

## Pendiente

- [ ] Autenticación y autorización (JWT / Spring Security) — reemplazará `validateRoleForTransition` con `@PreAuthorize`
- [ ] Endpoints DELETE (los repos tienen `delete()` pero no hay controladores)
- [ ] Tests de integración (requieren DB)
- [ ] Dockerfile productivo (actualmente vacío)
- [ ] Agregar `@NotNull` en `ClaimRequest.orderId`

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
