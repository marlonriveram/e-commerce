# Claims & Returns Module — E-Commerce API

## Overview

REST API for managing customer claims and returns in an e-commerce platform. Built with **Java 21 + Spring Boot 4** and **PostgreSQL 16**.

## System Architecture

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
| `domain/` | Modelos, enums, interfaces repositorio, validador | solo Java puro (sin Spring) |
| `application/` | Servicios, mappers, DTOs | `domain/` propio + interfaces de otros módulos |
| `infrastructure/` | Entidades JPA, implementaciones repositorio | `application/` (via mapper) + entidades de otros módulos (JPA FK) |
| `web/` | Controladores | `application/` propio |

---

## Claim Lifecycle (State Machine)

```
        ┌─────────┐
        │ PENDING │
        └────┬────┘
         ┌───┴───┐
         ▼       ▼
    ┌─────────┐  ┌──────────┐
    │IN_REVIEW│  │ REJECTED │◀──── Terminal
    └────┬────┘  └──────────┘
         │
         ▼
    ┌──────────┐
    │ APPROVED │
    └────┬─────┘
     ┌───┴───┐
     ▼       ▼
 ┌─────────┐ ┌──────────┐
 │ REFUNDED│ │ REJECTED │◀──── Terminal
 └─────────┘ └──────────┘
```

## Role Permissions

| Role | Can transition to |
|---|---|
| **SUPPORT** | `IN_REVIEW`, `APPROVED`, `REJECTED` |
| **FINANCE** | `REFUNDED` |
| **CUSTOMER** | Cannot change status (creates claims only) |

---

## API Endpoints

Base URL: `/api/v1`

### Users

| Method | Endpoint | Description | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/users` | Register a new user (default role: CUSTOMER) | `{ name, email }` | `201` — `UserResponse` |

### Claims

| Method | Endpoint | Description | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/claims` | Create a new claim (status: PENDING) | `{ orderId, description, userId }` | `201` — `ClaimResponse` |
| `GET` | `/claims` | List all claims (optional filter: `?status=`) | — | `200` — `ClaimResponse[]` |
| `GET` | `/claims/user/{userId}` | List all claims for a user | — | `200` — `ClaimResponse[]` |
| `GET` | `/claims/{claimId}` | Get a single claim by ID | — | `200` — `ClaimResponse` |
| `GET` | `/claims/{claimId}/history` | Get audit history for a claim | — | `200` — `ClaimHistoryResponse[]` |
| `PATCH` | `/claims/{claimId}/review` | Update claim status (SUPPORT only) | `{ newStatus, changedByUser }` | `200` — `ClaimResponse` |
| `PATCH` | `/claims/{claimId}/refund` | Refund a claim (FINANCE only) | `{ changedByUser }` | `200` — `ClaimResponse` |

### HTTP Status Codes

| Code | When |
|---|---|
| `200` | Success |
| `201` | Created (POST) |
| `400` | Invalid data / invalid status transition |
| `403` | Role not authorized for this action |
| `404` | User or Claim not found |
| `409` | Duplicate email / data integrity violation |
| `500` | Unexpected server error |

---

## Business Rules Implemented

1. **New claims** are created with status `PENDING`
2. **New users** are created with role `CUSTOMER` by default
3. **Status transitions** follow the state machine above — invalid transitions are rejected
4. **Role-based access** — only SUPPORT can review claims, only FINANCE can process refunds
5. **Audit trail** — every status change is recorded in `claim_history` with who changed it and when
6. **User existence** is validated before creating claims or updating status

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Validation (`@NotBlank`, `@NotNull`) |
| Build | Maven (Wrapper included) |
| Container | Docker + docker-compose (PostgreSQL) |
| Testing | JUnit 5 + Mockito |

---

## Getting Started

### Prerequisites

- Java 21+
- Docker Desktop

### Run

```bash
# Start PostgreSQL
docker compose up -d

# Run the application
./mvnw spring-boot:run
```

### Tests

```bash
# Run all unit tests
./mvnw test

# Compile only
./mvnw compile
```

---

## Pending / To Do

- [ ] Authentication & Authorization (JWT / Spring Security)
- [ ] Delete endpoints (users, claims, history)
- [ ] Tests de integración (requieren DB)
- [ ] Production Dockerfile
- [ ] Agregar `@NotNull` en `ClaimRequest.orderId`
