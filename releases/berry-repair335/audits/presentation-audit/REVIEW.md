# Berry presentation recovery 335 / 1.4.3 — independent review

Reviewed 2026-11-03 (Asia/Tokyo). Scope: the final source files identified below, the associated host tests, and the coordinator integration. This was a read-only implementation review; this reviewer did not execute Android or duplicate the owners' complete build runs.

## Result

No blocking implementation issue remained in the reviewed final source. The user reported that 334 / 1.4.2 resolved the stuck loading indication but that repeated Back operations still corrupt the display. This review does not establish the visual corruption's root cause or prove its resolution.

## Findings closed before freeze

- **OnDraw lifecycle:** an early implementation checked proof before entering its draw-cleanup guard, so a newly stale proof could attempt to remove an `OnDrawListener` from inside `onDraw`. Its geometry-retry branch could also request layout from inside that callback. The final implementation reads geometry and eligibility only during `onDraw`, then posts one coalesced action. Proof checks, layout requests, reporting/UI work, and observer removal occur after the traversal.
- **Observation-time eligibility:** the finalizer verifies eligibility and layout readiness captured during the observed traversal as well as their current values after posting. A hidden view that becomes visible before the queued action runs cannot retroactively qualify the earlier traversal.
- **Queued action cancellation:** the post-draw action is retained as a cancellable task and removed on cancellation or completion. New navigation invalidates old proof and removes owned timers/listeners/posts rather than relying solely on queued no-ops.
- **Diagnostic count and visibility:** one confirmed restore increments its counter once. Hiding the report control is process-scoped and survives Activity recreation. `VISUAL_TIMEOUT` and the explicitly labeled fallback draw phase can expose the report after a previously hidden restore becomes visible.

## Properties verified by source inspection

- Native loading completion is final independently of presentation recovery. Optional presentation or diagnostic failures do not return the tab to loading.
- Presentation waits for attachment, shown state, positive measured/actual dimensions, and stable pre-draw geometry. Recovery work has a 3-second deadline, at most two visual requests per recovery, and at most two layout requests.
- Layout repair is limited to inconsistency between the WebView's own positive measured size and its actual laid-out size. It uses `requestLayout()` through existing parent constraints; it does not assign dimensions, stretch a legitimately smaller child to its parent's size, alter zoom, or mutate DOM/CSS.
- Android does not provide cancellation for a pending `VisualStateCallback`. A per-WebView lease therefore permits at most one actual pending native visual request across navigation generations. Timeout does not release that lease or stack another request. A late callback can release its own lease but cannot redraw another navigation. The bounded fallback invalidation is separately labeled and is not reported as visual readiness or success.
- Presentation state holds weak references to WebView and the shared `ViewTreeObserver`; its lease does not retain the view. Owned listeners, watchdogs, and queued finalizers are cleaned up. The coordinator's proof does not capture a strong WebView reference.
- The coordinator requires the same state, navigation generation, commit token, native navigation identity, and handled restore event. It can follow an updated URL only when the native Page proof verifies the same document. With JavaScript-only proof, a changed URL conservatively ends presentation work.
- Diagnostic observations are bounded to four weakly referenced WebViews and 64 events per view. They contain fixed phase codes and numeric native geometry/scale/scroll state, not URLs, titles, document contents, or form input.
- Diagnostic UI posts are coalesced and weakly reference the target; navigation or timeline eviction cancels obsolete UI work. Copying requires the explicit dialog button. The overlay is outside the WebView, uses conditional visibility changes, hides with `INVISIBLE`, and does not repeatedly call `bringToFront`. Adding the small native overlay can still cause an ordinary root layout pass; no claim of zero window-level UI work is made.
- Activity lookup supports an Application-context WebView by inspecting up to 16 View ancestors and at most eight context wrappers per ancestor. Both loops are bounded; a missing or unsupported Activity/decor safely leaves numeric recording available without adding UI.

## Test evidence reviewed

The owners reported `PresentationRecoveryTest`: 24 scenarios / 154 assertions PASS, and `RenderDiagnosticsTest`: 11 scenarios / 62 assertions PASS. This reviewer inspected the regressions for stale proof during `onDraw`, geometry changes during `onDraw`, hidden-at-traversal eligibility, cancellation before the queued finalizer, pending native lease bounds across repeated restores, legitimate smaller children, and diagnostic privacy/copy/queue bounds. Final build and APK-application evidence are maintained by the parent release workflow and must match the hashes below.

The Android test observer rejects view/layout/observer mutations inside `onDraw`, modeling the API restriction documented at:
https://developer.android.com/reference/android/view/ViewTreeObserver.OnDrawListener

Visual readiness is a barrier for a subsequent draw, not a guarantee of correct pixels:
https://developer.android.com/reference/android/webkit/WebView#postVisualStateCallback(long,%20android.webkit.WebView.VisualStateCallback)

## Limits

- No physical Android device, GPU/compositor output, or real Berry 3.84.12 APK was verified by this review.
- `DRAW_TRAVERSAL` means a framework traversal was observed under the checked conditions. It does not validate rendered pixels, CSS correctness, or video output.
- Native `requestLayout()` repairs the Android View's layout state; it is not a general page-layout or renderer repair.
- A deadline or absent callback is not evidence of out-of-memory or renderer death. A pending native lease remains blocked until its callback arrives or the WebView is released.
- Rendering wait is deliberately finite. A view that stays hidden, detached, or unready beyond the deadline receives no indefinite polling loop.

## Reviewed source identities

| File | SHA-256 |
| --- | --- |
| `runtime/runtime_src/hiro/berrybfcache/PresentationRecovery.java` | `0dacaac56f6e3938c0b666684c7b85fb40c773aa32561d02ce4f8250d9bd1efa` |
| `runtime/runtime_src/hiro/berrybfcache/RenderDiagnostics.java` | `0e5301500ddd2541dd8faa3c613fd7f921c4c930b991b8df6019ed2e5a259f99` |
| `runtime/runtime_src/hiro/berrybfcache/RestoreCoordinator.java` | `2e451b9bdee341ad0df4989b510c36ab5b9429a1598685ad46b100c46e1e3755` |
| `runtime/runtime_tests/src/PresentationRecoveryTest.java` | `28d76f906638df9c3e94ea03194d4e3638bd65a5f9071e0301eb49d75ebabc85` |
| `runtime/runtime_tests/src/RenderDiagnosticsTest.java` | `b9103332d9b159587f29d256b2c0b640a790a1bacf2677a64bce0c5766ade8dc` |
