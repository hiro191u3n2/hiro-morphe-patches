# Independent diagnostic-removal review — Berry 1.4.5 / bundle 339

REMOVAL_REVIEW_RESULT: PASS

Reviewed on 2026-11-03 independently of the runtime implementation. No blocking findings remain within the requested diagnostic-removal scope. This review approves the pinned source and evidence below; it does not assert that publication has completed or that the reported visual corruption has been fixed.

## Scope and source baseline

The request is to remove the diagnostic functionality added during investigation. The comparison baseline is the published 337 source at commit `7cca97f00f1fbfbf96c3360793d1b0127921a42a`. The active combined 338 archive has SHA-256 `a9d8713f9ce7fb1df98438c2f15782ec5b3b307309b0f7c315c28fb2837ca1e8`; its BFCache extension is byte-identical to the published 337 runtime, SHA-256 `be25832a7f7ab766b8a6c93ed0816562d05fba8dd4b5bd078a96922b17c64023`.

Review used the fresh, explicit runtime source inventory staged as `runtime/`, containing only CompletionTarget, NativeCompletion, NativeRestoreTracker, PresentationRecovery, and RestoreCoordinator plus their nested classes. This is important because obsolete files reappeared in the intermediate working directory. `runtime/build_runtime.py` rejects any additional Java runtime source before compiling.

## Removal and behavior review

- RenderDiagnostics and PostScrollDiagnostics, their nested classes, the draw-record badge/dialog/copy action, diagnostic ring buffers, provider-report collection, post-scroll listener, settle/deadline sampling, image/viewport numeric script, and BFCache diagnostic Log calls are absent from the final runtime source and DEX. Obsolete diagnostic tests and diagnostic-only Android stubs are excluded from the final source inventory.
- CompletionTarget and NativeCompletion source bytes are unchanged. NativeRestoreTracker retains identity proof, invalidation, disabled-state handling, page-handler cleanup, and fatal-error propagation; only logging fields, logging-only calls, and unused logging arguments were removed.
- RestoreCoordinator keeps native and JavaScript restoration proof, generation and current-document guards, loading completion, bounded retry behavior, and presentation start/cancel calls. The document-start bootstrap and restoration PROBE remain functional inputs, not post-scroll diagnostics.
- PresentationRecovery keeps its 3,000 ms deadline, 700 ms callback timeout, 100 ms scheduling interval, maximum two visual requests, maximum two layout requests, and a single actual native visual-request lease across generations. Attach, pre-draw, and draw observers remain necessary to this bounded recovery and still clean up. Changes remove only phase/event recording and the unused phase parameter passed to finish.
- Test changes replace assertions against removed diagnostic messages with direct state, invalidation, listener, and timer assertions. In particular, completion now requires all recovery timers to drain; the old exception for the longer diagnostic observation deadline is gone.
- Provider source differs from 337 only in the description of the BFCache repair patch. Existing generated loading/native hooks and the four legacy extensions are verified against applications of the actual combined 338 baseline. No new visual-corruption remedy is introduced.

The source-preservation verifier was inspected and independently run against the final five source files. It verifies pinned published source bytes, applies explicit diagnostic-only deletions with occurrence counts, and compares Java tokens while preserving string and character literals. It then binds those source hashes to the tested runtime build and shipped extension. This supports the DEX comparison, where changes in the three instrumented class families are allowed only alongside the exact source-deletion proof.

## Evidence inspected

The reviewed runtime extension is 35,748 bytes, SHA-256 `50fa3b5879ebf02c463f80cfba84adfe77080192c28ae8e29752486f2c7b82a4`. An independent DEX string/type-table inspection found 35 runtime classes and no diagnostic class names, scroll diagnostic listener, clipboard/dialog APIs, Android Log reference, numerical sampling script, or draw-record text.

The final RUNTIME_BUILD_QA report has SHA-256 `ecfbb7f971c8517a02727b8c4fd26ab5796a5c76a172bae43f76520c6f5ccad8`. Its five retained suites pass: coordinator 45 scenarios / 88 assertions; JavaScript 19 assertions; native tracking 5 / 40; native integration 15 / 34; presentation 24 / 153. This totals 89 JVM scenarios and 334 assertions including JavaScript.

`QA_runtime_delta339.json` passes with 25 fully preserved canonical classes, 10 instrumented classes changed solely under the source-deletion proof, 15 diagnostic classes removed, and zero classes added. Loading completion terminal writes, current view, shared proof capture, and presentation start are verified. Debug line positions and the removed log-tag constant do not serve as behavior evidence; local-register renumbering is normalized by consistent first use.

`SDK_compile339.json` passes compilation against real Android API 35 without custom stubs and is bound to the final runtime source. The remaining compile-only stubs have zero constants requiring SDK comparison.

`QA_apply339.json` reports four successful applications: standalone and combined archives, each on the unmodified Berry 3.83.74 input and its synthetic renamed fixture. All four hook audits pass, including 27 expected hook kinds, native body/metadata/tail argument preservation, generated native hooks unchanged from 338, retained presentation runtime, diagnostic class/API absence, and method/field reference resolution. The synthetic fixture is not an actual Berry 3.84.12 APK.

## Publication checks and limits

The publication verifier binds this review to exactly the twelve source pins below, freezes the full release source inventory, checks the five retained runtime suites, real-SDK evidence, source-deletion proof and runner hash, DEX/hook evidence, candidate archive hashes, and unrelated application preservation. It retains exact baseline 338, fixed Site receipt, GitHub branch-head, atomic main/dev feed, release-history, immutable download, and ULike-policy protections. Both candidate APK runs are bound to the corresponding baseline-338 application input and output hashes.

Physical device and Android renderer testing were not performed. Actual Berry 3.84.12 execution, the reported multi-site Back/scroll corruption, memory exhaustion, and whether removing the diagnostics changes the symptom remain unverified. This is a diagnostic-removal release, not a demonstrated rendering fix.

## Reviewed source pins

Paths are relative to the final `releases/berry-repair339` source root. Any change to a pinned file invalidates this review until rechecked.

| File | SHA-256 |
| --- | --- |
| `runtime/runtime_src/hiro/berrybfcache/PresentationRecovery.java` | `09c51582b13ffd2d2008f1cfbbc371cff4680676b037d1c403933654d2c43055` |
| `runtime/runtime_src/hiro/berrybfcache/RestoreCoordinator.java` | `9f649b91bbd79c0822a2f1bfac617f6da2a5afc0562559fdeb2d0c9a7b254167` |
| `runtime/runtime_src/hiro/berrybfcache/NativeCompletion.java` | `6e46eb190cece94d4429ee3c402eaa05ada3934af4346f614aec4bb571481db6` |
| `runtime/runtime_src/hiro/berrybfcache/NativeRestoreTracker.java` | `84532bed77c8ce408c6d8b625f26556bb6e9a45a481c0cfa6cdb6d69c723b8f6` |
| `runtime/runtime_src/hiro/berrybfcache/CompletionTarget.java` | `e15b9d7639f0584d66cbbdb662ade67c1368a129e7d2a1e9ad97bc256fa3c44f` |
| `runtime/runtime_tests/src/RestoreCoordinatorTest.java` | `670b608ddbfcd9a610c69e627807c43f22769f46521c7fdf53da80499fe6117b` |
| `runtime/runtime_tests/src/PresentationRecoveryTest.java` | `977d251c72a4e4e2782390bf505e19e932d257bceb069de8f077de7736290e8c` |
| `runtime/build_runtime.py` | `64c7f8eb05e76adb249fb9b57fac468c1fdadf0998f47a421c79d2d597bd13c5` |
| `qa/VerifyRuntimeDelta.java` | `2662237c674de8dcd59b9dab2925482446b319d7f237599977f8ff7c4014efe7` |
| `qa/VerifyPatchedHooks.java` | `4f799be78127f3f4d0268ac11166d6a1cf07cccaa1c95bf5bd8f64f3278cd145` |
| `build/VerifyRuntime.java` | `37f7bf614e82a79fe4207d7a3e75c1cc184a8aa6b79aa2424f4da968aa822b3d` |
| `publication/publish.py` | `1f77327925710fdff0f13ba9c5d5c7a8af548f9675a5a8f531a5296938ae8fca` |
