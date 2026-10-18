# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Inventory system for a neighborhood store ("Tienda de Barrio"): Spring Boot 3.3 / Java 21 REST API backed by PostgreSQL, plus an Angular 17 frontend in `frontend/`. Code, comments, identifiers, commit messages and API error messages are in Spanish; keep that convention. Comments reference requirement IDs from the project report (RF-xx functional, RNF-xx non-functional, RN-xx business rules, HUxx user stories). Keep those references when touching related code.

## Commands

Backend (run from repo root; use `mvnw.cmd` on Windows/PowerShell):

```bash
./mvnw spring-boot:run                     # runs with 'dev' profile (default), needs local PostgreSQL
./mvnw test                                # unit tests only (*Test.java, Surefire)
./mvnw clean verify                        # unit + integration (*IT.java, Failsafe) + JaCoCo check — what CI runs
./mvnw test -Dtest=MovimientoServiceTest   # single unit test class (append #metodo for one method)
./mvnw verify -Dit.test=MovimientoApiIT -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true   # single IT (skip coverage gate, which would fail on a partial run)
```

- Integration tests (`*IT.java`, extend `IntegracionBase`) use Testcontainers with `postgres:16`; **Docker must be running**. The container is a static singleton shared across all IT classes. Do not change that, or later classes hit a stopped database.
- `verify` fails if JaCoCo line coverage drops below 70%.
- Swagger UI: `http://localhost:8080/swagger-ui.html`. A Postman collection is in `postman/`.

Database defaults (overridable via `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`): `jdbc:postgresql://localhost:5432/inventario_tienda`, user/password `inventario`. Seeded users (V2 migration): `admin` / `Admin123*` (ADMIN), `tendero` / `Tendero123*` (TENDERO).

Frontend (from `frontend/`). Requires Node.js 18 or 20: Angular 17 does not support newer majors, so install Node 20 (`winget install OpenJS.NodeJS.20`) rather than the current LTS. Run `npm ci` first.

```bash
npm start          # ng serve on :4200; proxy.conf.json forwards /api to localhost:8080
npm run build
npm test           # Karma/Jasmine, watch mode
npm run test:ci    # headless, single run, with coverage
```

Formatting uses Prettier (`frontend/.prettierrc`).

## Architecture

### Backend layering
`controller` → `service` → `repository` (Spring Data JPA) with `dto` records at the API boundary and MapStruct `mapper`s (Lombok + MapStruct annotation processors are wired in `pom.xml`, with `lombok-mapstruct-binding`). Dynamic filtering uses JPA Specifications (`ProductoSpecs`, `MovimientoSpecs`). `MovimientoService` handles writes and `MovimientoConsultaService` handles read/query.

### Stock is derived, never stored
There is no stock column. Existence = sum of ENTRADA minus SALIDA movements, computed by the DB view `v_stock_producto` (projected via `ExistenciaView`) or by `MovimientoRepository.calcularExistencia`. `EstadoStock` (AGOTADO / BAJO_STOCK / EN_STOCK) is computed in services from existence vs. `stock_minimo`.

### Database enforces invariants (defense in depth)
`V1__esquema_inicial.sql` contains logic the Java code relies on, so changes to rules must stay consistent in both places:
- `movimientos` are **immutable**: a trigger rejects UPDATE/DELETE (RNF-04). There are no edit/delete endpoints for movements; `GlobalExceptionHandler` maps 405 with that explanation.
- A trigger rejects any SALIDA that would make stock negative, and a CHECK constraint enforces valid `tipo`/`motivo` pairs (ENTRADA→COMPRA; SALIDA→VENTA/AVERIA/VENCIMIENTO).
- `MovimientoService.registrar` locks the product row with `findByIdForUpdate` (pessimistic `SELECT ... FOR UPDATE`, RN-14) before reading existence. This is why tests use real PostgreSQL rather than H2. `MovimientoApiIT` tests the concurrent-sale race.
- `spring.jpa.hibernate.ddl-auto=validate`: schema changes require a new Flyway migration in `db/migration` (`V<n>__descripcion.sql`). Never edit an applied migration.

### Flyway locations per profile
- `dev` (default): `db/migration` + `db/demo` (demo data V100, edge-case data V101).
- `test`: `db/migration` only. Integration tests start from an empty DB and create their own data, so never make tests depend on demo data.

### Errors
Services throw domain exceptions (`BusinessRuleException` → 409, `ResourceNotFoundException` → 404, `SolicitudInvalidaException` → 400, `CredencialesInvalidasException` → 401). `GlobalExceptionHandler` turns them into a uniform `ApiError` body that includes `message`, `accion` (a corrective action for the user) and an optional `campos` map for `@Valid` failures. New business errors should supply an `accion`. 401 and 403 are produced by Spring Security in `SecurityConfig`, not by the advice. Don't catch security exceptions there.

### Security
Stateless JWT (`JwtService`, `JwtAuthFilter`). Roles are `ADMIN` and `TENDERO`. The whole permission matrix is in `SecurityConfig.authorizeHttpRequests`: ADMIN-only rules (PUT/DELETE proveedores, `/api/movimientos/export`, `/api/reportes/**`, `/api/analitica/**`) must come before `anyRequest().authenticated()`. CORS allows `http://localhost:4200` and exposes `Content-Disposition` for CSV downloads.

### Frontend
Standalone components with lazy routes (`app.routes.ts`). `core/` holds the API services, `models.ts` (mirrors backend DTOs), `authInterceptor` (adds Bearer token and logs out on 401), `authGuard`/`rolGuard`, and `errorDeApi()`, which pulls `message`/`accion`/`campos` from `ApiError`. All services build URLs from the `API` constant in `core/api.config.ts`; never hardcode the base URL. `features/` is organized by domain (auth, catalogo, inventario, reportes, dashboard, consola). The `/reportes` routes are ADMIN-only via `rolGuard('ADMIN')`. Charts use Chart.js through `shared/grafico.component.ts`.

`/consola` is an API test console for any role. It has an endpoint explorer (`features/consola/endpoints.ts`, one entry per controller route) and an automated end-to-end suite (`features/consola/pruebas.ts`) that checks business rules, validation and the role matrix against the running backend. `ConsolaApiService` builds its `HttpClient` on `HttpBackend`, so it deliberately skips `authInterceptor`: a 401 it provokes does not log the user out, and each request picks its own token (admin, tendero, none or forged). When you add or change an endpoint, update `endpoints.ts`, and `pruebas.ts` if a rule changes. The "Flujo de inventario" tests create `PRUEBA-…` products and permanent movements, so run them only against the dev database.

## CI
`.github/workflows/ci.yml` runs `./mvnw -B clean verify` on every push and on PRs to `develop`/`main`. It covers the backend only; frontend tests don't run in CI. Feature branches merge into `develop`, and `develop` merges into `main`.
