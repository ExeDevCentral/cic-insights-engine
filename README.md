# cic-insights-engine

Backend-first company insights, search, and employer reputation service built with **Java 21**, **Spring Boot 3.4**, **PostgreSQL 16**, **Flyway**, and a lightweight **React 19 / TypeScript** client.

The service models a read-heavy company directory (employer profiles, search/facets, typeahead suggestions, employee reviews, and Schema.org `EmployerAggregateRating` JSON-LD output) where profile reads outnumber review writes by orders of magnitude.

## Stack

- **Backend:** Java 21 (Virtual Threads), Spring Boot 3.4, Spring Data JPA (Hibernate 6), Flyway, Caffeine, Micrometer / Prometheus, Springdoc OpenAPI 3
- **Database:** PostgreSQL 16 (docker-compose included)
- **Frontend:** React 19, TypeScript 5, Vite, TanStack Query

## Key Design Decisions

Rather than treating this as a generic CRUD demo, the codebase focuses on a few concrete production concerns:

1. **Composable Search & Safe Sorting (`CompanySearchCriteria`, `CompanySpecifications`):**
   - Multi-field filtering (`industry`, `minRating`, `verifiedOnly`, `headquarters`, `minActiveJobs`, tokenized `q`) built with Spring Data `Specification<CompanyProfile>` so Hibernate only emits active predicates.
   - User-supplied `LIKE` wildcards (`%`, `_`, `\`) are escaped explicitly.
   - Sort fields are validated against an explicit allowlist and page size is clamped (`cic.max-page-size`) before reaching JPA, preventing arbitrary property-path errors or unbounded page requests.
   - Prefix-prioritized typeahead (`/api/v1/companies/suggest`) and cached global facets (`/api/v1/companies/facets`) support fast search UIs.

2. **Constant-Time Score Aggregation with Optimistic Locking (`CompanyProfile`):**
   - Reviews are stored in `employer_reviews` without a `@OneToMany` collection on `CompanyProfile`.
   - Submitting a review updates sliding averages (`overallRating`, `cultureScore`, `compensationScore`) in O(1) using `BigDecimal` (`RoundingMode.HALF_UP`) and relies on JPA `@Version` optimistic locking.
   - Concurrent update conflicts (`ObjectOptimisticLockingFailureException` / `OptimisticLockException`) map cleanly to HTTP `409 Conflict` via RFC 7807 `ProblemDetail`.
   - Details and trade-offs (including rounding drift and reconciliation) are documented in [ADR-0001](docs/adr/0001-sliding-aggregates-and-optimistic-locking.md).

3. **Explicit JPA & Caching Posture:**
   - `spring.jpa.open-in-view=false` and `ddl-auto=validate`; schema changes live exclusively in Flyway migrations (`backend/src/main/resources/db/migration/`).
   - Named Caffeine caches (`companyBySlug`, `companyFacets`) with `.recordStats()` exposed via `/actuator/prometheus`, evicted atomically when a new review is recorded.
   - Typed configuration via `@ConfigurationProperties` (`CicProperties`) instead of hardcoded URLs or magic numbers.

See [docs/adr/](docs/adr/) for the full architecture notes and trade-offs.

## API Overview

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/companies` | Dynamic multi-criteria search (`industry`, `minRating`, `verifiedOnly`, `headquarters`, `minActiveJobs`, `q`, `page`, `size`, `sort`) |
| `GET` | `/api/v1/companies/suggest?q=ac&limit=8` | Prefix-ranked autocomplete suggestions |
| `GET` | `/api/v1/companies/facets` | Cached industry counts and global summary |
| `GET` | `/api/v1/companies/{slug}` | Single company profile + Schema.org JSON-LD |
| `POST` | `/api/v1/companies/{slug}/reviews` | Submit review and recompute aggregate ratings |

### Quick `curl` Examples

```bash
# Search verified cloud companies rated >= 4.2 sorted by rating
curl "http://localhost:8080/api/v1/companies?industry=Cloud&minRating=4.2&verifiedOnly=true&sort=overallRating,desc"

# Autocomplete suggestions
curl "http://localhost:8080/api/v1/companies/suggest?q=in&limit=5"

# Search facets
curl "http://localhost:8080/api/v1/companies/facets"
```

## Running Locally

### Start PostgreSQL
```bash
docker compose up -d
```

### Run Backend Tests & App (Java 21 + Maven 3.9+)
```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Useful local URLs once running:
- API base: `http://localhost:8080/api/v1/companies`
- OpenAPI / Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator & Prometheus: `http://localhost:8080/actuator/health`, `http://localhost:8080/actuator/prometheus`

### Run Frontend
```bash
cd frontend
npm install
npm run dev
```

## Known Limitations & Next Steps

- **Full-text ranking at scale:** `Specification` + `LOWER(...) LIKE` is clean and portable up to tens of thousands of company rows. Beyond that, moving `q` matching to PostgreSQL `tsvector` + GIN trigram indexes (`pg_trgm`) or OpenSearch is the natural next step (see [ADR-0002](docs/adr/0002-composable-search-and-sort-allowlist.md)).
- **Periodic aggregate reconciliation:** Running averages can accumulate minor `HALF_UP` rounding drift after thousands of writes; a scheduled reconciliation job recomputing aggregates from `employer_reviews` would keep long-lived profiles exact.
- **Distributed caching:** Caffeine is process-local. Multi-replica deployments should swap to Redis (or L1 Caffeine + L2 Redis) with pub/sub invalidation.
