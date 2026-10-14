# Backend

Spring Boot 4.1 application for the Smart Inventory System — Java 21, SQLite, Maven. Runs on **port 8888**.

## Prerequisites

- JDK 21
- Maven 3.9+ (under Windows/MSYS prefer `mvn.cmd` — the bash wrapper breaks glob expansion; `run-backend.sh` handles this)

## Running

```bash
./run-backend.sh start     # also: stop | restart
# or directly:
mvn spring-boot:run
```

The SQLite database file (`sisie_database.db`) is created next to the application.

## API overview (port 8888)

| Area | Endpoint | Method | Access |
|---|---|---|---|
| Auth | `/api/auth/login` | POST | public — returns a JWT |
| Products | `GET /api/products`, `GET /api/products/{sku}` | GET | all roles |
| Products | `POST /api/products/create` | POST | MANAGER, ADMIN |
| Locations | `GET /api/locations` | GET | all roles |
| Locations | `POST /create`, `PUT /{id}`, `DELETE /{id}` | — | MANAGER (class default) |
| Logistics | `POST /api/logistics/receive` — body `{sku, loc, qty}` (`loc` = location **code**) | POST | MANAGER (class default); success = 201 + plain-text message |
| Logistics | `POST /api/logistics/issue` — body `{sku, qty}`, backend picks batches via **FIFO** | POST | MANAGER (class default) |
| Logistics | `GET /api/logistics/stock/{sku}` | GET | all roles |
| Dashboard | `GET /api/dashboard/stock-overview`, `/location-count`, `/total-units`, `/low-stock-count` | GET | all roles |

Roles: `WORKER` < `MANAGER` < `ADMIN`. Authorization is method-level `@PreAuthorize` (enabled via `@EnableMethodSecurity` in `config/SecurityConfig.java`); the JWT is validated by a filter in `security/`.

## Architecture notes

- **Thin controllers** — routing, DTO mapping and authorization only. Business logic (FIFO batch selection, stock aggregation, low-stock rules, validation with German messages) lives in the service layer (`com.sisie.core.service`).
- **Error contract** — `GlobalExceptionHandler` maps failures to 400 JSON `{ "message": ... }`; logistics success responses are plain text rendered directly by the UI.

## Testing

```bash
mvn test      # JUnit 5 + Mockito (unit tests for services, @WebMvcTest slices for controllers)
mvn verify    # additionally: JaCoCo coverage report + Pitest mutation testing
```

Results and coverage analysis: [QA report](../../docs/04_quality_assurance/testing_report.md).
