# ADR-0002: Composable Search Specifications, Wildcard Escaping, and Sort Allowlist

## Context

The initial search endpoint used a static JPQL query with `(:param IS NULL OR ...)` branches and passed Spring's `Pageable` directly to the repository. That creates three practical issues:

1. Query planners handle `OR :param IS NULL` poorly compared to queries containing only active predicates.
2. Unescaped `%` or `_` characters in user input behave as SQL wildcards.
3. Passing arbitrary `sort=` parameters straight to Hibernate lets clients probe internal entity fields (`version`, `id`) or trigger `500` errors on unknown properties.

## Decision

- Encapsulate filter inputs in an immutable Java 21 `record` (`CompanySearchCriteria`) that normalizes blank strings, validates numeric ranges (`minRating` in `[0, 5]`, `minActiveJobs >= 0`), and escapes `LIKE` wildcards.
- Build queries dynamically via `CompanySpecifications` (`JpaSpecificationExecutor`) with multi-token matching across `displayName`, `industry`, `headquarters`, and `slug`.
- Sanitize `Pageable` before hitting the repository: clamp `size` to `cic.max-page-size` and validate every `Sort.Order` against `CompanySearchCriteria.ALLOWED_SORT_FIELDS`.
- Add dedicated `/suggest` (prefix-prioritized projection) and cached `/facets` endpoints for search UI ergonomics.

## Trade-offs & Consequences

- **Pros:** Clean SQL predicates, safe user input handling, predictable 400 ProblemDetail errors on invalid filters/sort fields, zero extra infrastructure required to run locally.
- **Cons:** Case-insensitive `LIKE '%token%'` cannot leverage standard B-Tree indexes for leading wildcards. Once the dataset grows beyond ~50k profiles, migrating the free-text predicate to PostgreSQL `pg_trgm` GIN indexes / `tsvector` or OpenSearch is the clear upgrade path without changing the HTTP contract.
