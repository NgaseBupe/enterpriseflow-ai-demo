# Review Log

Every story is reviewed before or shortly after it is merged. Each finding is recorded here as it happens: what was wrong, why, what it would have caused, how it was fixed, and how the fix was proven. Findings are never edited away afterwards; resolved ones stay as a record.

The topic reviews (`architecture-review.md`, `database-review.md`, `security-review.md`, and so on) are written from this log at the end of the project.

**Severity:** High = wrong results or failures in normal use. Medium = failures in realistic edge cases, or a security weakness. Low = maintainability, clarity or robustness.

| ID | Area | Severity | Summary | Status |
|---|---|---|---|---|
| [R-001](#r-001-ai-snapshot-and-audit-details-rejected-null-values) | Persistence | High | AI snapshot and audit details rejected null values | Fixed |
| [R-002](#r-002-entity-equality-failed-for-hibernate-proxies) | Persistence | Medium | Entity equality failed for Hibernate proxies | Fixed |
| [R-003](#r-003-mysql-check-violations-are-not-classified-as-integrity-errors) | Persistence / API | Medium | MySQL CHECK violations are not classified as integrity errors | Open (Sprint 2) |
| [R-004](#r-004-audit-events-in-the-same-microsecond-had-no-defined-order) | Audit | Low | Audit events in the same microsecond had no defined order | Fixed |
| [R-005](#r-005-ai-snapshot-numbers-lose-decimal-precision) | Extraction | Medium | AI snapshot numbers lose decimal precision | Open (Sprint 2) |
| [R-006](#r-006-catch-all-error-handler-would-hide-authorisation-failures) | Security / API | Medium | Catch-all error handler would hide authorisation failures | Open (Sprint 3) |

---

## Story #6 — Database schema and module structure

Reviewed after merge (PR #14). Fixes delivered on branch `fix/review-6-persistence`.

### R-001: AI snapshot and audit details rejected null values

- **Found by:** code review, then confirmed with a failing test.
- **Problem:** `OrderExtraction` and `AuditEvent` copied their JSON maps with `Map.copyOf(...)`, which throws `NullPointerException` for any null value.
- **Root cause:** `Map.copyOf` was chosen for immutability without checking the data it would hold. The AI is deliberately instructed to return `null` for fields it cannot find (design §7.2), and audit diffs contain `null` old values.
- **Impact:** almost every real extraction has at least one missing field, so extraction would have crashed in normal use. The mock data used in the first tests had no nulls, so the tests passed.
- **Fix:** copy into an unmodifiable `LinkedHashMap`, which keeps immutability and allows nulls. A comment explains why `Map.copyOf` must not be used here.
- **Validation:** new tests `keepsNullValuesInTheAiSnapshot` and an audit test with a null detail value. The first failed with `NullPointerException` before the fix and passes after.
- **Lesson:** test data must include the awkward cases the design already predicts, not just the happy path.

### R-002: Entity equality failed for Hibernate proxies

- **Found by:** code review.
- **Problem:** `BaseEntity.equals` compared `getClass()` values. Hibernate often returns a lazy proxy (a generated subclass), so an entity and a proxy of the same row were considered different.
- **Impact:** collections and comparisons involving lazily loaded associations (for example, a line item's parent extraction) would behave incorrectly, which is a subtle source of duplicate or missing items.
- **Fix:** compare the persistent class, taken from the proxy's lazy initializer when the object is a proxy, and read the other entity's ID through its getter. Neither step loads the proxy from the database.
- **Validation:** new test `anEntityEqualsALazyProxyOfItself` using a detached, uninitialised proxy. A first attempt used `Hibernate.getClass()`, and the test showed that it loads the proxy (failing outside a session), so the final fix avoids it.

### R-003: MySQL CHECK violations are not classified as integrity errors

- **Found by:** failing repository tests.
- **Problem:** MySQL reports CHECK-constraint violations with the generic error code 3819 (SQL state `HY000`). Spring therefore wraps them in `JpaSystemException`, not `DataIntegrityViolationException` as it does for unique and foreign-key violations.
- **Impact:** code that catches `DataIntegrityViolationException` to return a clean 400 or 409 would miss CHECK violations, and the client would get a 500.
- **Current mitigation:** repository tests assert on the specific constraint name, which proves the right rule fired.
- **Planned fix (Sprint 2):** validate input in the application before it reaches the database, so CHECK constraints remain a last line of defence, never the normal validation path. Revisit error translation if a violation can still reach the API.

### R-004: Audit events in the same microsecond had no defined order

- **Found by:** code review.
- **Problem:** the audit history was ordered by `occurred_at` only. Two events recorded in the same microsecond could come back in either order.
- **Fix:** order by `occurred_at`, then by ID. IDs are time-ordered UUIDv7, so the result is deterministic.
- **Residual risk:** UUIDv7 IDs created within the same millisecond differ only in random bits, so the tie-break is stable but not guaranteed to be causal. That is acceptable for a demo with one writer per document.

### R-005: AI snapshot numbers lose decimal precision

- **Found by:** code review.
- **Problem:** the AI snapshot is stored as `Map<String, Object>`. When JSON is read back, Jackson turns decimals such as `2.50` into `Double` values.
- **Impact:** Sprint 2 compares current values with the snapshot to decide whether a field was *AI extracted* or *human edited*. A `Double` versus `BigDecimal` comparison, or binary rounding, could mark untouched money fields as edited.
- **Planned fix (Sprint 2):** store the snapshot as a typed record with `BigDecimal` fields, or read JSON decimals as `BigDecimal`, with a test that round-trips a value such as `0.10`.

### R-006: Catch-all error handler would hide authorisation failures

- **Found by:** code review, looking ahead to Sprint 3.
- **Problem:** `ApiExceptionHandler` handles every `Exception` as a 500. When method-level security is added, Spring Security's `AccessDeniedException` and `AuthenticationException` would be caught here and turned into a 500 instead of a 403 or 401.
- **Impact:** clients would see "internal error" instead of "forbidden", and security tests would fail in confusing ways.
- **Planned fix (Sprint 3):** let security exceptions propagate to Spring Security's handlers (rethrow them or handle them explicitly), with tests for 401 and 403 responses.
