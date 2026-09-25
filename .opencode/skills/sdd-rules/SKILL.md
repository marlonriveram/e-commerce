---
name: sdd-rules
description: Reglas simples de Spec-Driven Development para el e-commerce de claims. Usar al redactar, revisar o implementar Specs (archivos .spec.md bajo .opencode/specs/). Útil cuando el usuario pida "spec", "especificar una feature", "US-XX" o "criterios de aceptación".
---

# Reglas SDD para este proyecto

Workflow: **especificar → implementar → verificar**. Cada feature comienza escribiendo un spec en `.opencode/specs/`, luego se implementa siguiéndolo y se cierra marcando los criterios de aceptación.

## Dónde van los specs

- Ubicación: `.opencode/specs/`
- Un spec por feature, nombrado `US-XX-nombre.spec.md` (ej: `US-05-auth-jwt.spec.md`)
- `AGENTS.md` es la fuente de verdad de las convenciones del proyecto; los specs la complementan por feature, no la duplican

## Estructura mínima de un spec (6 secciones)

1. **Requisitos** — user story y contexto (¿qué necesidad resuelve?)
2. **Cambios propuestos** — qué módulos/paquetes/archivos se tocan (claim, user, notification, ai, shared...)
3. **Contrato** — endpoints, eventos RabbitMQ, migraciones Flyway, cambios de esquema
4. **Reglas de comportamiento** — casos borde y decisiones de negocio (máquina de estados, roles, nullables, DLQ...)
5. **Criterios de aceptación** — checklist verificable de "qué debe cumplirse para cerrar la feature"
6. **Tests esperados** — qué casos cubrir con el estándar del proyecto

## Reglas de estilo

- Escribir en **español** (coherente con AGENTS.md, README y tests)
- Respetar la arquitectura hexagonal modular de `AGENTS.md`:
  - `domain/` = Java puro; mappers estáticos; validadores `final class` con métodos estáticos
  - `application/` = un servicio = una capacidad
  - `infrastructure/` = entidades JPA + impls repositorio + listeners
  - `web/` = controladores; cross-module solo por interfaces
- Cambios de entidad → **migración Flyway nueva** (nunca tocar `ddl-auto=validate`)
- Eventos asíncronos → publicar solo tras COMMIT (patrón `@EventListener` de `AGENTS.md`)
- **Nunca** escribir secrets/API keys en specs ni código

## Estándar de tests (espejo de AGENTS.md)

- JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`, sin `@SpringBootTest`)
- Nombres `should[Comportamiento]When[Escenario]`
- Estructura AAA comentada: `// Given`, `// When`, `// Then`

## Definition of ready (spec listo para implementar)

- [ ] Criterios de aceptación verificables (no ambiguos)
- [ ] Contrato completo (endpoints, eventos, migraciones si aplica)
- [ ] Regiones de módulo existentes identificadas, sin inventar arquitectura nueva
- [ ] Tests esperados enumerados
- [ ] Referencias a `AGENTS.md` donde aplique