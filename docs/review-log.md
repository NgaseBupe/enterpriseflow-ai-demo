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
| [R-007](#r-007-file-names-could-hide-their-real-extension) | Security | Medium | File names could hide their real extension | Fixed |
| [R-008](#r-008-two-different-responses-for-too-large-uploads) | API | Low | Two different responses for "too large" uploads | Fixed |
| [R-009](#r-009-orphan-file-cleanup-was-untested) | Testing | Medium | Orphan-file cleanup was untested | Fixed |
| [R-010](#r-010-uploads-are-held-in-memory-and-written-inside-the-database-transaction) | Performance | Low | Uploads are held in memory and written inside the database transaction | Accepted |
| [R-011](#r-011-some-fixes-were-not-proven-by-the-tests-that-claimed-to-cover-them) | Testing | Medium | Some fixes were not proven by the tests that claimed to cover them | Fixed |

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

---

## Story #7 — Upload a purchase order

Reviewed before merge, after checking the endpoint by hand against a running instance.

### R-007: File names could hide their real extension

- **Found by:** code review, then confirmed with a failing test.
- **Problem:** `FileNames.sanitize` removed control characters but not invisible Unicode formatting characters (category `Cf`). The right-to-left override `U+202E` makes `po\u202Efdp.exe` display as `poexe.pdf`.
- **Impact:** a reviewer could be shown a misleading file name. This is a known technique for disguising executables. The file type itself was never at risk, because the type comes from the file's signature, not its name.
- **Fix:** remove characters in both `\p{Cntrl}` and `\p{Cf}`.
- **Validation:** new test `removesInvisibleFormattingCharactersUsedToDisguiseExtensions` failed before the fix and passes after.

### R-008: Two different responses for "too large" uploads

- **Found by:** manual testing against the running app.
- **Problem:** files just over 10 MB were rejected by the application (`urn:enterpriseflow:problem:file-too-large`), but files over the 11 MB servlet limit were rejected by Spring with a generic `about:blank` problem.
- **Impact:** clients would need to handle two shapes for the same error.
- **Fix:** override Spring's `MaxUploadSizeExceededException` handling to return the same problem type and title.
- **Validation:** new test `uploadRejectedByTheServletLimitUsesTheSameProblemTypeAsTheApplication`.

### R-009: Orphan-file cleanup was untested

- **Found by:** code review (test coverage).
- **Problem:** the file is written before the database transaction commits, and a rollback hook deletes it if the commit fails. That hook had no test.
- **Impact:** a regression would silently fill storage with files that no document points to.
- **Fix:** new test `DocumentUploadRollbackTest` forces a foreign-key failure at commit time and checks that neither a document row nor a file remains. The hook already worked, so no production code changed.

### R-010: Uploads are held in memory and written inside the database transaction

- **Found by:** code review.
- **Problem:** `MultipartFile.getBytes()` reads the whole file (up to 10 MB) into memory, and the file is written to disk while a database connection is held.
- **Impact:** with many simultaneous large uploads, memory use and connection hold time grow. At this demo's scale, the effect is negligible.
- **Decision:** accepted for the demo. The fix, if ever needed, is to stream the upload to a temporary file while computing the hash, then move it into place after the insert. To be covered in the performance review.

---

## Test coverage check of all findings

After story #7, every finding was checked against its tests. Each fix was then temporarily reverted to confirm its test fails without it (a manual *mutation test*).

### R-011: Some fixes were not proven by the tests that claimed to cover them

- **Found by:** reviewing the review: mapping each finding to its tests, then reverting each fix.
- **Problem:**
  - R-001 was covered only by database-backed tests, although the bug sat in two constructors that need no database.
  - R-002 had a test for the proxy case, but none for the basic equality rules.
  - R-004's test waited 2 ms between events, so the timestamps never tied. It would have passed without the fix.
- **Fix:**
  - `OrderExtractionTest` and `AuditEventTest`: unit tests for null values and immutability.
  - `BaseEntityTest`: unit tests for identity, equality, hash codes and "is new".
  - `breaksTimestampTiesByTheTimeOrderedId`: writes two events with *identical* timestamps, inserting the later ID first.
- **Validation (each fix reverted in turn):**

  | Fix reverted | Result |
  |---|---|
  | R-001 (`Map.copyOf` put back) | Both new unit tests fail |
  | R-002 (`getClass()` comparison put back) | The unit tests still pass, as expected, because they involve no proxies. `anEntityEqualsALazyProxyOfItself` fails. |
  | R-004 (tie-break removed) | **The new test still passes.** MySQL returns tied rows in ID order anyway, because the `(document_id, occurred_at)` index stores the primary key last. |

- **Conclusion on R-004:** the fix turns behaviour that depends on MySQL's choice of index into an explicit guarantee, but no test can currently demonstrate the failure. The test stays as a guard, with a comment explaining its limits.
- **Lesson:** a passing test proves nothing until it has been seen to fail. Future fixes are checked by reverting them before the finding is marked fixed.

