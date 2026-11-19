# CIC Insights Engine — Employer Branding & SEO Telemetry Platform

> **Stack:** Java 21 (Virtual Threads) • Spring Boot 3.4 • Spring Data JPA (Hibernate 6) • Flyway • PostgreSQL 16 • Caffeine Cache • RFC 7807 ProblemDetails • React 19 • TypeScript 5 • TanStack React Query • Tailwind CSS

Enterprise full-stack architecture modeled after **Company Insights & Connections (CIC)** systems supporting high-traffic employer branding pages, verified employee reviews, and automated Schema.org (`EmployerAggregateRating`) JSON-LD SEO structured data generation.

---

## 🏛️ Architectural Highlights (Why This isn't Junior Code)

1. **Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`):**
   High-concurrency non-blocking I/O throughput for database and cache reads without reactive callback complexity.
2. **Zero N+1 & $O(1)$ Domain Invariants (`CompanyProfile#recordNewReviewScore`):**
   Instead of loading `List<EmployerReview>` via `@OneToMany` (a classic junior anti-pattern that triggers `OutOfMemoryError` on popular companies), the aggregate root computes sliding weighted averages in $O(1)$ time with `BigDecimal` (`RoundingMode.HALF_UP`) and enforces concurrency safety with JPA `@Version` optimistic locking.
3. **Production JPA Posture (`open-in-view: false` & `ddl-auto: validate`):**
   OSIV is explicitly disabled to prevent database connections from being held across HTTP view rendering. All schema evolution is deterministically versioned via **Flyway** (`V1__init_company_insights_schema.sql`) with composite B-Tree indexes (`idx_company_profiles_industry_rating`).
4. **RFC 7807 Problem Details (`GlobalExceptionHandler`):**
   Standardized machine-readable HTTP error responses (`application/problem+json`) for validation failures (`400`), missing slugs (`404`), and optimistic lock conflicts (`409`).
5. **SEO Structured Data & Low-Latency Caching:**
   Every company profile response dynamically synthesizes Schema.org `EmployerAggregateRating` JSON-LD for search engine rich snippets, backed by an in-memory **Caffeine** cache (`@Cacheable` / `@CacheEvict`) and **Micrometer Prometheus** telemetry.

---

## 📂 Project Structure

```text
cic-insights-engine/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/exedev/cic/
│       │   │   ├── CicInsightsApplication.java
│       │   │   ├── api/
│       │   │   │   ├── CompanyInsightsController.java
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   └── dto/
│       │   │   │       ├── CompanyInsightDto.java
│       │   │   │       └── SubmitReviewRequest.java
│       │   │   ├── domain/
│       │   │   │   ├── CompanyProfile.java
│       │   │   │   └── EmployerReview.java
│       │   │   ├── repository/
│       │   │   │   ├── CompanyProfileRepository.java
│       │   │   │   └── EmployerReviewRepository.java
│       │   │   └── service/
│       │   │       └── CompanyInsightsService.java
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       │           └── V1__init_company_insights_schema.sql
│       └── test/
│           └── java/com/exedev/cic/domain/
│               └── CompanyProfileTest.java
└── frontend/
    └── src/
        └── CompanyInsightsDashboard.tsx
```

---

## 🚀 Running Locally

### 1. Backend (Spring Boot 3 + Java 21)
```bash
cd backend
mvn clean test
mvn spring-boot:run
```
- **REST API:** `http://localhost:8080/api/v1/companies`
- **Swagger UI (OpenAPI 3):** `http://localhost:8080/swagger-ui.html`
- **Actuator Health & Prometheus Metrics:** `http://localhost:8080/actuator/health`
