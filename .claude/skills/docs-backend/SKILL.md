---
name: docs-backend
description: Use this skill when creating or modifying any public Java element in the Spring Boot backend (controllers, services, repositories, mappers, DTOs, entities). Enforces JavaDoc + SpringDoc OpenAPI annotations per layer, in English. Trigger keywords — touching `controller/`, `service/`, `repository/`, `dto/`, `mapper/`, `*CommandService`, `*QueryService`, adding endpoint, adding DTO record, adding `@RestController`, adding `@Service`.
model: claude-sonnet-4-6
effort: medium
allowed-tools: Read, Edit, Write, Grep, Glob, Bash(./gradlew build*), Bash(./gradlew test*), Bash(docker compose*), Bash(curl http://localhost:8080/*)
---

# Backend documentation rules (JavaDoc + SpringDoc)

Apply these rules to every NEW or MODIFIED public Java element. **No exceptions.** Documentation is part of the same commit as the code change — never «later».

## Language
JavaDoc and OpenAPI text (`summary`, `description`, `@Schema` strings) MUST be in **English**, regardless of the chat language.

## JavaDoc — required per layer

| Layer | Class JavaDoc | Method JavaDoc |
|---|---|---|
| `controller/` | purpose, base path, auth requirement | summary, `@param`, `@return`, `@throws ResponseStatusException` with status codes |
| `service/*CommandService` | "Write-side CQRS service ...", `@Transactional`, `@PreAuthorize` | behaviour, ownership checks, `@param`, `@return`, `@throws` |
| `service/*QueryService` | "Read-side CQRS service ...", `@Transactional(readOnly = true)` | behaviour, validation rules (e.g. `0 ≤ page`, `1 ≤ size ≤ 50`), `@param`, `@return`, `@throws` |
| `service/*Mapper` | MapStruct intent + null policy | short line per `toResponse` / `toEntity` / `patchEntity` |
| `repository/` | one-line intent | required for non-trivial methods (`@Query`, JPQL aggregations, native queries, count-for-precondition methods) |
| `dto/` | record purpose | not required per accessor — covered by `@Schema` |

## SpringDoc OpenAPI — required on controllers + DTOs

### Controller class
- `@Tag(name = "<Resource>", description = "...")`
- `@SecurityRequirement(name = "bearerAuth")` — unless the endpoint is public.

### Controller method
- `@Operation(summary, description)`
- `@ApiResponses` covering EVERY realistic status code: 200 / 201 / 204 / 400 / 401 / 403 / 404 / 409.
- `@Parameter(description, example)` on path/query params.

### DTO record
- `@Schema(description = "...")` on the class.
- `@Schema(description, example, requiredMode)` on EVERY component:
  - `requiredMode = Schema.RequiredMode.REQUIRED` for `@NotNull` fields, `NOT_REQUIRED` for optional/patch fields.
  - `BigDecimal` amounts: state scale (e.g. "scale 4") + realistic example (`"99.99"` / `"99.9900"`).
  - `Instant`: state "UTC ISO-8601" + example `"2026-05-13T10:00:00Z"`.
  - `UUID`: example `"3fa85f64-5717-4562-b3fc-2c963f66afa6"`.
  - Enums: list allowed values inline (e.g. "INCOME or EXPENSE").

### Bearer-auth setup
`OpenApiConfig` already declares the `bearerAuth` scheme. New controllers only need `@SecurityRequirement(name = "bearerAuth")` at class level.

## Reference implementation (mimic these)
- Controller: `backend/src/main/java/com/company/expensetracker/controller/transaction/TransactionController.java`
- Services: `service/transaction/TransactionCommandService.java`, `TransactionQueryService.java`
- DTOs: `dto/transaction/TransactionRequest.java`, `TransactionResponse.java`, `TransactionPatchRequest.java`, `TransactionSummaryResponse.java`

When in doubt, open the corresponding `transactions` file and copy its structure and tone.

## Verification before commit
1. `./gradlew build -x test` must pass.
2. `docker compose up -d --build` → open `http://localhost:8080/swagger-ui/index.html` → confirm the new tag appears with full descriptions, examples, and response codes.

A PR that adds an endpoint/DTO but ships without these annotations is incomplete — do not consider the task done.
