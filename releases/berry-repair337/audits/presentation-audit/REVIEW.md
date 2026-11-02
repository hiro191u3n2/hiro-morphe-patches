# Berry post-scroll diagnostics 337 / 1.4.4 — independent review

Reviewed 2026-11-03 (Asia/Tokyo). Scope: the frozen runtime sources and host tests identified below. The working directory remains `work336`; the combined-bundle version is 337. This reviewer inspected the implementation, regression tests, and the runtime owner's final build report. This reviewer did not execute Android or independently repeat the owner's complete build runs.

## Result

No blocking implementation issue remained in the reviewed frozen source. This release extends diagnosis after a proven BFCache restore; it does not establish or fix the remaining display-corruption root cause.

The user's 1.4.3 record showed consistent native View geometry and completed visual/draw callbacks. The user clarified that corruption starts when scrolling after Back. The supplied screenshot retains the header and parts of the product grid but contains substantial blank regions. These observations do not distinguish missing page content from a renderer/compositor output problem and do not prove memory exhaustion. The prior recovery's first successful draw preceded the newly identified observation window.

## Findings closed before freeze

- **Exception isolation:** the first draft called diagnostic cancellation outside the existing cancellation guard and left optional measurements and UI completion callbacks able to throw through framework callbacks. Final cleanup tolerates callback/observer removal failures; measurement callbacks and optional completion work contain runtime exceptions.
- **Shared-tree notifications:** `ViewTreeObserver.OnScrollChangedListener` observes the whole shared view tree. The final implementation compares the target WebView's native X/Y scroll positions before counting a scroll or consuming the sampling budget. Foreign scroll notifications leave the budget intact.
- **Outstanding evaluation reporting:** an 800 ms watchdog cannot cancel Android's pending JavaScript evaluation. The report separates `waitingForResult` from `evaluationOutstanding`, so ending the local wait is not presented as cancelling the platform operation.
- **Automatic listener lifetime:** both the scroll and attachment observers are removed at the automatic deadline or sample limit. Manual inspection does not reinstall automatic listeners.
- **Asynchronous copy data:** manual sampling begins with an explicit pending message, then replaces the captured report only while badge revision, target, and dialog still match. Only the Copy button writes the clipboard; late callbacks never copy automatically.

## Verified implementation properties

- The coordinator shares its existing restore proof with independent presentation and diagnostic components. Loading completion is not made dependent on numeric sampling. `PresentationRecovery.java` remains source-identical to 1.4.3.
- Automatic observation lasts at most 45 seconds: one baseline, the first detected native scroll, and up to three settled-scroll attempts, debounced by 200 ms. There is no periodic polling loop.
- Diagnostic state is limited to four weakly keyed WebViews and eight sample/status rows per state. Existing native history remains limited to four weakly referenced WebViews and 64 events per timeline.
- Each WebView has at most one outstanding evaluation issued by this new component across restore generations. Timeout ends the local wait but retains the lease until the actual callback. Navigation, detach, eviction, or timeout cannot stack another diagnostic evaluation behind a lost callback. A stale callback releases only its own lease and cannot populate a newer generation's report.
- Evaluation callbacks/timers weakly reference WebView and state. Lease values do not reference WebView. The observer is weakly referenced. A stopped state may remain available for explicit inspection within the four-state limit and weak-key ownership, without automatic observers.
- Request and result acceptance require the current restore proof. For JavaScript-established restores, the query also checks the already-known document-marker ID and restore serial before reading measurements. Native-established restores retain the existing native navigation/Page proof. Same-document URL policy is unchanged.
- The new query does not read page URLs, titles, text, form values, image addresses, HTML, or cookies. It returns a fixed 30-number schema: viewport, scroll, pixel ratio, scrolling-element/root dimensions, VisualViewport dimensions/offsets, bounded image statistics, and numeric marker status. It does not mutate DOM/CSS, dispatch events, install page listeners, scroll, zoom, reload, or request drawing.
- At most the first 128 images are inspected. Total/scanned/capped values are explicit. Rectangle intersection, `complete`, and nonzero natural dimensions remain separate counts. Neither intersection nor completion is called visible-pixel success. The parser rejects excessive length, wrong field count, nonnumeric strings, nonfinite values, invalid flags/counts, and out-of-range values without retaining the rejected result.
- Each accepted sample includes native values before and after asynchronous evaluation, request-to-callback delay, and a movement indicator. These values are not claimed to be simultaneous or automatic evidence of a coordinate bug.
- Provider package/version and display density are reported. Provider strings use bounded allowlists. Existing statements that missing callbacks do not establish renderer death or memory exhaustion remain.
- The new component contains no invalidation, layout request, layer-type change, cache/history clearing, forced reload/zoom, or replacement of Berry's existing scroll listener. Existing presentation recovery remains separate.

## Reviewed test evidence

The owner's frozen `runtime/check-build/RUNTIME_BUILD_QA.json` reports all eight suites passing: **117 JVM scenarios and 505 total assertions including both JavaScript harnesses**. The new post-scroll suite contributes 17 scenarios / 85 assertions; its JavaScript harness contributes 20 assertions. Runtime extension: 64,656 bytes, SHA-256 `be25832a7f7ab766b8a6c93ed0816562d05fba8dd4b5bd078a96922b17c64023`.

This reviewer inspected coverage for sustained-scroll coalescing, preserving another observer, foreign-tree notifications, lost evaluations across repeated restore generations, late stale callbacks, proof invalidation, detach/deadline cleanup, manual inspection after deadline, bounded history, malformed/private-response rejection, native-getter/cleanup/UI-callback exceptions, and explicit asynchronous copying. The JavaScript harness denies page-content getters and mutation APIs, verifies marker/serial rejection, and verifies the image-scan cap even for a million-image mock collection.

Coordinator tests now distinguish the independent 45-second diagnostic window from bounded loading/presentation work and separately verify diagnostic cancellation. They do not suppress unresolved loading/presentation timers with a general pending-work exception.

Final MPP assembly, real-SDK compilation, APK-fixture application, unrelated-patch preservation, and publication belong to the release workflow and must use these source identities. Host tests do not exercise Android pixels or a device compositor.

## Limits

- No physical Android device, actual GPU/compositor output, or clean Berry 3.84.12 APK was tested in this review. The corruption is not claimed fixed.
- Read-only DOM geometry queries can cause ordinary style/layout computation and affect timing. Instrumentation is bounded, not zero-cost.
- Image statistics cover the first 128 images and simple rectangle intersection. They do not establish clipping, CSS visibility/opacity, occlusion, decode/display success, background images, canvas, text rendering, or every image on the page.
- Native/DOM coordinates have different units and are asynchronous. Native `getScale()` includes display density. `moved=false` does not certify that all DOM/viewport properties stayed unchanged.
- An outstanding evaluation makes manual inspection report `JS_BUSY` without promising fresh DOM values. Copying before sampling finishes can include the explicitly labeled pending state. Neither timeout nor busy status proves memory exhaustion.
- The one-outstanding-evaluation guarantee applies to the new diagnostic component, not all JavaScript issued by Berry, page scripts, or the pre-existing restore coordinator.
- Filtering by native WebView scroll coordinates intentionally does not observe every separately scrolling DOM element. Manual inspection still permits current numeric sampling within the proved document scope.
- `DRAW_TRAVERSAL` and `SAMPLE_OK` mean an observed framework traversal and an accepted numeric sample respectively. Neither means correct pixels.

## API references

- https://developer.android.com/reference/android/view/ViewTreeObserver.OnScrollChangedListener
- https://developer.android.com/reference/android/webkit/WebView

## Reviewed source identities

| File | SHA-256 |
| --- | --- |
| `runtime/runtime_src/hiro/berrybfcache/PostScrollDiagnostics.java` | `1ce41a490babb271bf9fcb999c40734da14bc792af1b1cde25120924081c1b6d` |
| `runtime/runtime_src/hiro/berrybfcache/RenderDiagnostics.java` | `f069498b450851ea6076ed3eeb0332693837638055b634360e87da64d25f810f` |
| `runtime/runtime_src/hiro/berrybfcache/RestoreCoordinator.java` | `f9b0b0e0a379be7b16f1170d43b3a50178b95547e7481c9bbaf16ec45e688ffa` |
| `runtime/runtime_src/hiro/berrybfcache/PresentationRecovery.java` | `0dacaac56f6e3938c0b666684c7b85fb40c773aa32561d02ce4f8250d9bd1efa` |
| `runtime/runtime_tests/src/PostScrollDiagnosticsTest.java` | `05603df99a355f2c9006d76c85872615c907add31bcc93a1c40e117175b2dddd` |
| `runtime/runtime_tests/src/RestoreCoordinatorTest.java` | `9d40c9eb629a7ede96aeee76c258152f91d995ae73fbc759deeea12f230bb088` |
| `runtime/runtime_tests/src/RenderDiagnosticsTest.java` | `b9103332d9b159587f29d256b2c0b640a790a1bacf2677a64bce0c5766ade8dc` |
| `runtime/runtime_tests/check_scroll_script.js` | `9c3114ea72ef9fc55d4adbb651626346a87985a515a5e99697daf8e30aaeb5e7` |
| `runtime/build_runtime.py` | `ced5cc784c0d21408040134f10048397af045081b60ad4093186e79b67dfecf6` |
