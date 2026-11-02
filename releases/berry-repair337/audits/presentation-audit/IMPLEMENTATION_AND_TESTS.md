# Historical implementation notes: 335 / 1.4.3

This file preserves the prior presentation-recovery implementation record. Its statements about native-only diagnostics and test totals describe 335 / 1.4.3, not the current 337 / 1.4.4 release. For the current post-scroll numeric DOM diagnostics, frozen source identities, final test counts, and limitations, see `REVIEW.md`, `CASE_REPORT.md`, and the release runtime QA report. The current release is diagnostic-only and does not claim that the display corruption has been fixed.

---

# Berry 1.4.3 presentation recovery: implementation and test evidence

This audit describes the new host-side implementation and its simulation tests. It is not an Android device test, an ART execution test, or a claim that the user's corrupted pixels have been repaired.

## Trigger and retained behavior

The user reported that 1.4.2 / bundle 334 stopped the indefinitely loading state, but repeated Back navigation still corrupts the display. The user approved independent presentation recovery, conditional layout repair, cancellation of old work, and bounded diagnostic records.

`RestoreCoordinator` retains its native Page/document restoration proof and loading-completion logic. After positively identified restoration completion, it starts `PresentationRecovery` with a proof callback bound to the current restore and navigation. A new navigation cancels the old presentation work. Presentation timeouts do not change the browser back to loading, reload a page, clear history/cache, or claim that pixels are correct.

## Readiness and layout contract

`PresentationRecovery` requires an attached, shown WebView with positive laid-out and measured dimensions. It waits for pending layout work, observes pre-draw readiness, and requires two consistent geometry samples before requesting a native visual-state callback.

Conditional `requestLayout()` compares the View's actual dimensions with its own positive measured dimensions after pending layout has completed. It does not assign dimensions, stretch a legitimately smaller child to match its parent, alter CSS, change scale, or dispatch fake size-change notifications. Parent geometry and offsets are part of the observed frame identity, so a later layout change invalidates earlier readiness.

The native visual callback is a readiness barrier. A subsequent redraw is requested, and a later draw traversal is observed. `onDraw` itself only observes geometry/readiness and posts follow-up work; cleanup, proof checks, UI changes, and listener removal occur after traversal. None of these observations is a pixel-correctness test.

## Work and retention bounds

- One restore has a 3,000 ms presentation deadline.
- Callback/draw waiting uses a 700 ms timeout; polling is coalesced at 100 ms.
- Each restore allows at most two layout requests and two visual-state requests.
- A lease allows at most one outstanding native visual request per WebView, including across newer restores. Android does not expose cancellation for this request. An absent callback therefore blocks additional requests rather than accumulating them.
- A late callback may release its own lease, but cannot draw for an expired or different restore. The new restore must pass its own current-page proof and readiness checks.
- The state keeps weak references to WebView and the view observer. Queued work/listeners are removed on termination or cancellation. The native callback retains weak references to its view/state.

## Diagnostics and user control

`RenderDiagnostics` keeps at most four tab records, each with the last 64 events. Fixed phase codes and native geometry, scale, and scroll values are recorded. Page URLs, titles, HTML, form text, and JavaScript-evaluated page data are not collected. Records are local, with no automatic transmission.

After a proved restoration, a small native `描画記録` control opens a report. Clipboard writes occur only after the user presses `コピー` in that dialog. `アプリ再起動まで隠す` hides the control for the rest of the process. Application data and browser settings are not cleared.

## Evidence included in this release

The exact source hashes and passing suite results are recorded in `payload/RUNTIME_BUILD_QA.json`; the publication build reruns these suites twice and requires the reviewed report and runtime bytes to match.

- Coordinator and native integration tests cover the separation of loading completion from presentation, navigation changes, native proof, same-document aliases, and cancellation.
- `PresentationRecoveryTest` covers ordinary readiness/draw traversal, delayed attachment/visibility, zero size, inconsistent measured geometry, legitimately smaller children, layout changes, missing visual callbacks, repeated restores with an outstanding native request, stale proof, hidden/detached callbacks, post-draw finalizer cancellation, missing draw traversal, limits, optional API exceptions, UI-thread dispatch, and coalescing.
- `RenderDiagnosticsTest` covers bounded records, user-triggered copy, local geometry data, control visibility/lifecycle, and omission of page content.
- `QA_apply335.json` records four applications: clean Berry 3.83.74 and a renamed structural fixture, each with combined and Berry-only artifacts. Its DEX audit checks hook/register integrity, current runtime references, clipboard reachability from explicit clicks, absence of page-data reads in diagnostics, and preservation of the original four runtime extensions against bundle 334.

Actual Berry 3.84.12, Android rendering, physical display correction, and memory-pressure behavior remain untested. Full report counts are taken from the frozen runtime report rather than from an earlier candidate build.
