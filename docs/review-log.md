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
| [R-012](#r-012-multipart-uploads-failed-in-frontend-tests-only) | Testing | Medium | Multipart uploads failed in frontend tests only | Fixed (workaround) |
| [R-013](#r-013-the-page-number-was-lost-on-refresh-and-back) | Frontend | Medium | The page number was lost on refresh and Back | Fixed |
| [R-014](#r-014-changing-page-flashed-a-loading-state) | Frontend | Low | Changing page flashed a loading state | Fixed |
| [R-015](#r-015-valid-files-with-no-reported-type-were-rejected) | Frontend | Medium | Valid files with no reported type were rejected | Fixed |
| [R-016](#r-016-the-api-client-could-not-handle-empty-responses) | Frontend | Low | The API client could not handle empty responses | Fixed |
| [R-017](#r-017-the-upload-confirmation-reappears-after-a-refresh) | Frontend | Low | The upload confirmation reappears after a refresh | Accepted |
| [R-018](#r-018-conflict-messages-used-raw-status-names) | API | Low | Conflict messages used raw status names | Fixed |
| [R-019](#r-019-a-document-can-get-stuck-in-processing) | Reliability | Medium | A document can get stuck in Processing | Open (Sprint 5) |
| [R-020](#r-020-a-failed-extraction-returns-200-ok) | API | Low | A failed extraction returns 200 OK | Accepted |
| [R-021](#r-021-ai-output-is-not-yet-validated-before-it-is-saved) | Extraction | Medium | AI output is not yet validated before it is saved | Open (Sprint 2) |
| [R-022](#r-022-the-download-header-used-email-style-encoding) | API | Low | The download header used email-style encoding | Fixed |
| [R-023](#r-023-amounts-crowded-and-then-overflowed-the-line-items-table) | Frontend | Low | Amounts crowded, and then overflowed, the line-items table | Fixed |
| [R-024](#r-024-pdf-rendering-cannot-be-checked-automatically) | Testing | Low | PDF rendering cannot be checked automatically | Checked by hand (Chrome) |
| [R-025](#r-025-the-mock-ai-does-not-read-the-document) | Demo | Low | The mock AI does not read the document | Open (Sprint 6) |
| [R-026](#r-026-negative-numbers-got-a-confusing-message) | Frontend | Low | Negative numbers got a confusing message | Fixed |
| [R-027](#r-027-a-confirmed-extraction-could-be-saved-without-changes) | Extraction | Medium | A confirmed extraction could be "saved" without changes | Fixed (test with #12) |
| [R-028](#r-028-a-late-version-clash-would-return-500) | Extraction | Medium | A late version clash would return 500 | Fixed (no automated test) |
| [R-029](#r-029-edit-audit-events-record-field-names-only) | Audit | Low | Edit audit events record field names only | Open (Sprint 4) |
| [R-030](#r-030-validation-rules-exist-on-both-client-and-server) | Maintainability | Low | Validation rules exist on both client and server | Accepted |
| [R-031](#r-031-a-lint-warning-passed-local-checks-and-failed-ci) | Process | Medium | A lint warning passed local checks and failed CI | Fixed |

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

---

## Story #8 — See my uploaded documents

This story also delivered the upload screen, which story #7 needed but did not include. Reviewed before merge, after using the screens in a browser against the running backend.

### R-012: Multipart uploads failed in frontend tests only

- **Found by:** failing upload tests.
- **Problem:** in tests, `fetch` threw `Cannot read properties of undefined (reading '_bytes')` for any upload. The same code works in a real browser.
- **Root cause:** tests run in jsdom, whose `File` and `FormData` differ from the `fetch` Node provides. Vitest 5.0 bridges them by reading a hidden internal property of jsdom's `Blob`, and **jsdom 30 no longer exposes that property**. This was confirmed by inspecting a `Blob` in jsdom 29.1.1 (property present) and 30.1.1 (absent).
- **Fix:** pin jsdom to `~29.1.1`, and add a Dependabot rule that blocks jsdom major upgrades, with a comment explaining why. A first attempt with a custom test environment was abandoned once the real cause was found.
- **Residual limitation:** the same bridge sends uploaded files as plain `Blob`s, so **the file name is lost in tests**. The upload test therefore checks the request by content size. Browsers always send the name, and the backend tests cover name handling.
- **Revisit:** when Vitest supports jsdom 30, remove the pin.

### R-013: The page number was lost on refresh and Back

- **Found by:** code review.
- **Problem:** the current page was held in component state only. Refreshing, sharing the link or pressing Back returned the user to page 1, and `?page=99` showed a blank screen.
- **Fix:** the page lives in the address (`?page=2`, counted from 1 for people). Invalid values fall back to the first page, and a page past the end shows a message with a link back.
- **Validation:** three new tests. Two failed before the fix. The third ("invalid page number") passed before the fix only because the address was ignored entirely.

### R-014: Changing page flashed a loading state

- **Found by:** code review.
- **Problem:** each page is a separate query, so moving to the next page briefly replaced the table with "Loading documents…".
- **Fix:** keep the previous page on screen while the next loads (`placeholderData: keepPreviousData`), and disable the paging buttons meanwhile.
- **Validation:** `keeps showing the current page while the next one loads` passes with the fix and fails when it is reverted.

### R-015: Valid files with no reported type were rejected

- **Found by:** code review.
- **Problem:** the upload screen checked only `File.type`. Some systems report an empty type, so a genuine PDF would be refused before it reached the server.
- **Fix:** when the type is empty, fall back to the file extension. The server still checks the real content by signature.
- **Validation:** new test with an empty-type `po-1004.PDF` failed before the fix.

### R-016: The API client could not handle empty responses

- **Found by:** code review, looking ahead to logout in Sprint 3.
- **Problem:** `request()` always parsed a JSON body, so a `204 No Content` response would throw.
- **Fix:** return `undefined` for 204 responses.
- **Validation:** new API client tests (success, 204, Problem Details, non-JSON error, network failure). The 204 test failed before the fix.

### R-017: The upload confirmation reappears after a refresh

- **Found by:** code review.
- **Problem:** the "…was uploaded" message is passed in the browser's history state, which survives a page refresh, so the message shows again.
- **Decision:** accepted. It is harmless and accurate, and clearing history state would add code for little benefit.

---

## Story #9 — Extract order data with AI

Reviewed before merge, after extracting documents through the running app (success, repeat request and simulated provider failure).

Designed in from the start rather than found in review: the AI call runs **between** two short transactions, so a slow provider never holds a database connection. And starting extraction is a single conditional `UPDATE`, so when two requests race, exactly one wins (`onlyOneRequestCanClaimADocumentForExtraction`).

### R-018: Conflict messages used raw status names

- **Found by:** manual testing, then a failing test.
- **Problem:** the 409 message was built from the enum name, producing text such as "Cannot complete extraction while the document is extraction failed."
- **Fix:** every status has a human label ("Extraction failed", "In review", …). The message now reads: *Cannot complete extraction while the document's status is "Extraction failed".* The machine-readable `currentStatus` property is unchanged.
- **Validation:** `explainsTheConflictInPlainLanguage` failed with the old wording and passes now. `everyStatusHasAReadableLabel` guards future statuses.

### R-019: A document can get stuck in Processing

- **Found by:** code review.
- **Problem:** the document is marked Processing before the AI call. If the server stops during the call, or recording the failure itself fails, the document stays Processing. Retry is only allowed from Extraction failed, and the screen offers no action.
- **Impact:** rare in a synchronous demo (the mock answers instantly), but real with a slow provider.
- **Planned fix (Sprint 5):** the stuck-job sweeper from design §2.4 marks documents that have been Processing longer than a timeout as failed, so they can be retried.

### R-020: A failed extraction returns 200 OK

- **Found by:** code review.
- **Observation:** `POST /process` returns 200 with status `EXTRACTION_FAILED` when the AI fails, rather than an error code.
- **Decision:** accepted. The request itself succeeded: the attempt was made and recorded, and its outcome is part of the document's state, which the client displays. Errors that stop the attempt from starting (unknown document, wrong state) are still 404 and 409. The endpoint changes to `202 Accepted` when extraction becomes asynchronous in Sprint 5.

### R-021: AI output is not yet validated before it is saved

- **Found by:** code review.
- **Problem:** the result is saved as returned. The database rejects some bad values (for example a zero quantity, tested in `dataTheDatabaseRejectsLeavesNoPartialExtraction`), but an extraction with **no line items** would be stored, and it would be rejected with a generic message rather than a specific one (see R-003).
- **Planned fix (Sprint 2):** the validator from design §7.3 (structure, field rules and arithmetic checks) runs before anything is stored.

---

## Story #10 — Review the extracted order next to the document

Reviewed before merge using screenshots of the running app (headless Chrome) and a check by hand in a desktop browser.

### R-022: The download header used email-style encoding

- **Found by:** inspecting real response headers with `curl`, then failing tests.
- **Problem:** Spring's `ContentDisposition` builder, given a charset, wrote `filename="=?UTF-8?Q?po-3101.pdf?="`: RFC 2047 email encoding, which RFC 6266 says not to use in HTTP. Browsers recover by using `filename*`, but the header was non-standard even for plain ASCII names.
- **Fix:** build the header directly: an ASCII-only fallback in `filename` (non-ASCII, quotes and backslashes replaced) and the exact name, percent-encoded as UTF-8, in `filename*`.
- **Validation:** `namesThePlainFileInTheStandardFormat` and `encodesUnusualFileNamesSafelyInTheHeader` failed before the fix and pass after.

### R-023: Amounts crowded, and then overflowed, the line-items table

- **Found by:** screenshots of the review screen.
- **Problem:** repeating the currency in every cell ("ZMW 1,020.00") made amounts wrap onto two lines. The first fix (keep amounts on one line) made the table **wider than its panel**. The next screenshot caught that regression.
- **Fix:** the currency is shown once, in the column headings ("Line total (ZMW)"); the product code moved under the description, removing a column; and the table scrolls sideways on very narrow screens instead of overflowing.
- **Lesson:** layout changes need to be looked at, not only tested. The second screenshot caught what the unit tests could not.

### R-024: PDF rendering cannot be checked automatically

- **Found by:** trying to verify the strict content security policy (`default-src 'none'; frame-ancestors 'self'`) on the file endpoint.
- **Observation:** headless Chrome shows an empty PDF viewer **even with no policy at all**, as a side-by-side comparison showed, so screenshots cannot prove whether the policy blocks rendering.
- **Result:** checked by hand in desktop Chrome: the PDF renders with the policy in place.
- **Follow-up (Sprint 6):** check Safari and Firefox before the final demo.

### R-025: The mock AI does not read the document

- **Found by:** comparing the screen with the original document (Kasonde Engineering) and the extracted data (Chanda Hardware).
- **Explanation:** the mock returns a fixed order chosen from the file's bytes, not its text, which is expected for a stand-in. But in a demo, a mismatch with the visible document looks like a defect.
- **Planned fix (Sprint 6):** ship synthetic sample purchase orders whose content matches what the mock returns for them, as design §7.2 intends.

---

## Story #11 — Correct the extracted data

Reviewed before merge. The API was also exercised against the running app: a valid correction, the same request again with the old version, and invalid values.

### R-026: Negative numbers got a confusing message

- **Found by:** reading the test output. A quantity of `-1` produced *"Enter a number with up to 3 decimals"*, which is misleading because -1 is a number.
- **Root cause:** the format rule rejected the minus sign before the range rule could run.
- **Fix:** the format rule accepts a leading minus, so negative values reach the range rule: *"Must be greater than 0"* for quantities, *"Must be 0 or more"* for amounts.
- **Validation:** the test expectations were changed first; both tests failed, then passed after the fix.

### R-027: A confirmed extraction could be "saved" without changes

- **Found by:** code review of the first version of `updateExtraction`.
- **Problem:** the status check happened only when something had changed, so sending unchanged data for a confirmed extraction returned 200 instead of refusing the edit.
- **Fix:** the status (Extracted or In review) is checked before anything else.
- **Validation:** `refusesEditsBeforeExtraction` covers the Uploaded case. The Confirmed case needs the confirm action, so its test is added with story #12.

### R-028: A late version clash would return 500

- **Found by:** code review.
- **Problem:** the explicit version check catches most stale edits, but if two saves pass the check at the same moment, Hibernate's own version check fails at write time with `ObjectOptimisticLockingFailureException`, which the catch-all handler turned into a 500.
- **Fix:** that exception is converted into the same 409 "Changed by someone else".
- **Gap:** the race is hard to reproduce reliably in a test, so this path has **no automated test**. To revisit in Sprint 2 with a test that holds two transactions open.

### R-029: Edit audit events record field names only

- **Observation:** `EXTRACTION_EDITED` records *which* fields changed (for example `customerName`, `lines[2].quantity`), not the old and new values.
- **Planned (Sprint 4):** record values as well, together with the audit timeline screen.

### R-030: Validation rules exist on both client and server

- **Observation:** limits and formats (lengths, decimals, currency code, email) are defined in Bean Validation on the server and in a Zod schema in the browser.
- **Decision:** accepted. The browser copy gives instant feedback, and the server stays the authority. Drift is caught by tests on both sides: `rejectsInvalidValuesWithAMessagePerField` on the server, and `applies the same rules as the server` in the client. Server messages are also shown next to the matching field, so a missed rule still produces a useful message.

---

## Found by CI

### R-031: A lint warning passed local checks and failed CI

- **Found by:** the CI run for PR #20 (story #10). The frontend job failed on `oxlint --deny-warnings`:
  *"Fast refresh only works when a file only exports components"* in `DocumentViewer.tsx`.
- **Problem in the code:** the component file also exported a helper function (`contentUrl`). React's fast refresh can only hot-reload files that export components alone.
- **Problem in the process (the more important one):** the warning was there locally too, but the local check filtered the linter's output for error patterns that did not match oxlint's `Warning:` lines, and **ignored the exit code**. The check reported success while the linter had failed.
- **Fix (code):** the URL helper moved to the documents API module (`documentContentUrl`), where API addresses belong. Fixed on the story #10 branch and merged into #11.
- **Fix (process):** checks now pass or fail **on the command's exit code** (`npm run lint && npm run typecheck && …`), never on searching its output.
- **Lesson:** CI is the safety net that exposed this. A check that inspects text can be wrong about success; the exit code cannot.

