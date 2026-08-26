# OGC API - Processes ETS Compliance Notes

This document tracks findings from running the OGC `ets-ogcapi-processes10` conformance
test suite against the `ogcprocesses` API, for tests that fail due to issues in the test
suite itself rather than defects in this implementation.

Standard: https://docs.ogc.org/bp/20-089r1.html
Test suite: https://github.com/opengeospatial/ets-ogcapi-processes10
Echo process used by the suite: `hellowasdiworld`

---

## test Job Results Async Raw Value One

**Requirement:** `/req/core/job-results-async-raw-value-one`

**Symptom:** `java.lang.AssertionError: Server did not return result in 20 seconds.`

**Root cause (in the test suite, not in our API):**

`Jobs.loopOverStatus()` polls the job's `monitor` link every 5 seconds, up to
`MAX_ATTEMPTS = 4` (nominally a 20 second budget). However, the very first thing the
method does on each recursive call is:

```java
if (attempts >= MAX_ATTEMPTS) {
    throw new Exception(String.format("Server did not return result in %d seconds.", ...));
}
```

This check happens **before** the method inspects the status document it just fetched.
So if a job's completion is observed on the 4th (last) poll, that data is discarded and
the test throws a timeout error anyway - even though the server responded correctly and
in time. In practice this shrinks the *effective* usable budget to roughly 10-15 seconds
of real completion time, not the nominal 20.

**What we verified:**

- `hellowasdiworld` consistently completes in ~11-14 seconds end-to-end (queueing +
  execution), measured with an isolated PowerShell timing script (`time-hellowasdiworld.ps1`)
  hitting `main01.wasdi.net` directly, several runs in a row.
- Even after tuning the WASDI scheduling cycle to complete the job in ~10 seconds, the
  test still failed intermittently - confirming this is inherent to the test's polling
  logic, not something fixable by making the echo process faster.

**Conclusion:** accepted as an ETS/environment limitation, not an `ogcprocesses` defect.
Our implementation is compliant; the job does complete and does expose the correct
`results` link.

**Related:** `test Job Creation Input Validation` occasionally fails with the exact same
`"Server did not return result in 20 seconds"` message. Its first phase performs a normal
job execution and also calls `loopOverStatus()`, so it is subject to the same timing
quirk described above. When phase 1 lands within budget, the test proceeds to its second
phase (see below) and passes.

---

## test Job Creation Sync Raw Value One

**Requirement:** `/req/core/job-creation-sync-raw-value-one`

**Symptom (from the TeamEngine console):** `expected [true] but found [false]`

**Root cause (in the test suite, not in our API):**

```java
public void testJobCreationSyncRawValueOne() {
    JsonNode executeNode = createExecuteJsonNodeOneOutput(echoProcessId, RESPONSE_VALUE_RAW);
    HttpResponse httpResponse = sendPostRequestSync(executeNode, true);
    Assert.assertTrue(parseRawResponse(httpResponse).contains(TEST_STRING_INPUT));
}
```

This test asserts, unconditionally, that the raw execute response body contains the
literal string `"teststring"`. That assumption is only valid if the server actually
responded **synchronously** (`200`, with the raw output value as the body). There is no
branch for the case where the server responds `201 Created` (async job accepted), and no
guard/`SkipException` for processes that only support asynchronous execution - unlike
sibling tests such as `testJobResultsAsyncRawValueOne` (which explicitly branches on
`statusCode == 200` vs `201`) or `testJobCreationAutoExecutionMode` (which checks
`SupportedExecutionModes` first).

Since `hellowasdiworld` declares only `"jobControlOptions": ["async-execute"]`, our server
correctly returns `201 Created` with a job status document (no `"teststring"` in the
body) - this is compliant behavior per OGC 18-062r2 §7.11.4, which does not require a
server to support synchronous execution.

**Conclusion:** requirement does not meaningfully apply to an async-only process; the
test should skip in that case but does not. This is a gap in the test suite's
implementation, not an `ogcprocesses` defect.

---

## test Job Creation Input Inline Binary

**Requirement:** `/req/core/job-creation-input-binary`

**Symptom:** `Test Result: CANTTELL`, `java.lang.NullPointerException: Cannot invoke
"java.net.URL.getFile()" because "fileUrl" is null`

**Root cause (in the test suite/TeamEngine deployment, not in our API):**

```java
private void addBinaryInput(Input input, ObjectNode inputsNode) {
    URL fileUrl = getClass().getClassLoader().getResource("org/opengis/cite/testdata/testgeotiff.tiff");
    File inputFile = new File(fileUrl.getFile()); // NPE here: fileUrl is null
    ...
}
```

The test suite tries to load its own bundled binary test fixture
(`testgeotiff.tiff`) from the classpath, and that resource is missing from this
TeamEngine/ETS installation. The request never even reaches our server with valid
binary content.

**Conclusion:** test harness/packaging problem in the local ETS deployment, not an
`ogcprocesses` defect. `CANTTELL` (rather than `FAILED`) reflects that this is a
test-setup error. Fixing it would require repairing the ETS/TeamEngine installation's
test resource bundle (e.g. re-pulling the Docker image or checking the `testdata`
folder), which is out of scope for this API.

---

## test Job Results Sync

**Requirement:** `/req/core/job-results-sync`

**Symptom (after our `Link` header fix, see below):**
```
Illegal character in scheme name at index 0: <https://main01.wasdi.net/ogcprocesses/rest/jobs/{jobId}>
```

**Root cause (in the test suite, not in our API):**

```java
if (headerValue.contains("rel=monitor")) {
    foundRelMonitorHeader = true;
    statusUrl = headerValue.split(";")[0];   // e.g. "<https://.../jobs/{jobId}>"
    break;
}
...
httpResponse = sendGetRequest(statusUrl, ...); // -> new HttpGet(statusUrl)
```

Per RFC 8288, a `Link` header target must be wrapped in angle brackets:
`<https://...>; rel=monitor`. The test extracts everything before the first `;`, which
includes the angle brackets, and passes that string directly into `HttpGet(...)`
without stripping them - `<` is not a valid URI scheme character, so this throws
`IllegalArgumentException`. This will happen for **any** RFC 8288-compliant `Link`
header, regardless of server implementation.

**Conclusion:** parsing bug in the test suite (doesn't strip `<`/`>` before building the
request). Not fixable on our side without violating the `Link` header syntax mandated
by RFC 8288.

---

## Summary for ESA/APEx sign-off

Of the remaining CORE failures, none represent a defect in `ogcprocesses`:

| Test | Status | Reason |
|---|---|---|
| testJobResultsAsyncRawValueOne | Known ETS timing quirk | discard-last-poll bug in `loopOverStatus` |
| testJobCreationInputValidation | Intermittent (shares timing quirk in phase 1) | same as above; passes when phase 1 lands in time |
| testJobCreationSyncRawValueOne | ETS test gap | no branch/skip for async-only processes |
| testJobCreationInputInlineBinary | ETS environment issue | missing test fixture file, CANTTELL |
| testJobResultsSync | ETS parsing bug | doesn't strip `<`/`>` from `Link` header value |

Latest known result: **Pass 40 / Fail 5 / Skip 0 / Total 45.** All 5 remaining failures
are accounted for above and are test-suite issues, not `ogcprocesses` defects.
