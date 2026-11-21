# ADR-0001: O(1) Sliding Aggregates and Optimistic Locking on CompanyProfile

## Context

Company insight pages are overwhelmingly read-heavy, while review submissions arrive concurrently for popular employers. Two common approaches cause trouble in production:

1. Mapping `@OneToMany List<EmployerReview>` on `CompanyProfile` and recalculating averages in memory — this triggers N+1 queries and unbounded memory growth on high-volume companies.
2. Running `SELECT AVG(...)` across `employer_reviews` on every profile read — unnecessary load on read paths and harder to sort/filter efficiently in search queries.

## Decision

- Keep `employer_reviews` decoupled by `company_id` foreign key without a collection mapping on `CompanyProfile`.
- Maintain denormalized aggregate columns (`overall_rating`, `culture_score`, `compensation_score`, `review_count`) updated in O(1) via `CompanyProfile#recordNewReviewScore` using `BigDecimal` with `RoundingMode.HALF_UP`.
- Protect concurrent updates with JPA `@Version` optimistic locking, mapping both `OptimisticLockException` and Spring's `ObjectOptimisticLockingFailureException` to HTTP `409 Conflict` (RFC 7807 `ProblemDetail`).

## Trade-offs & Consequences

- **Pros:** Constant-time writes, indexed search/sort directly on `company_profiles`, zero collection hydration, predictable memory footprint.
- **Cons:**
  - Repeated `HALF_UP` rounding introduces tiny drift over thousands of reviews; a periodic background reconciliation job against `employer_reviews` is recommended for long-lived production datasets.
  - Under extreme write contention on a single company slug, callers may observe `409` responses and should apply bounded exponential retry (or shift to an async outbox/aggregation worker).
