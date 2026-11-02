# Berry BFCache corruption after scrolling — follow-up to 335

The user reported that loading completion now works, but repeated Back navigation still causes partial display loss. The follow-up screenshot and timing report narrow the observed trigger to **scrolling after returning**, rather than the first frame immediately after returning.

## Evidence supplied by the user

- Diagnostic version: Berry 1.4.3 / bundle 335; restore counter 10.
- WebView and parent measured and actual sizes: 1440 × 2833, attached and shown, no pending layout, progress 100.
- Six recorded visual requests completed in 52–82 ms, followed by observed draw traversals in 2–7 ms. No timeout or layout discrepancy appears in the supplied excerpt.
- The last recorded draw has native scrollY 4316; the report's later `Now` snapshot has scrollY 5752. These are different observation times, not proof of a coordinate mismatch.
- The screenshot retains the header, some product photos, carousel dots and sale-state labels, with substantial blank portions in the product list. Pale photos alone are not evidence of corruption; they may be normal sold-out styling.
- The user explicitly states that corruption appears when scrolling after Back.

The screenshot itself is not included in the public source release. Page URLs, titles, product descriptions, form values and image URLs are not collected by the new diagnostic runtime.

## What this establishes

The 335 first-frame presentation sequence ran through its observed traversal. Its listeners are removed at that point, so the later scrolling failure is outside that observation interval. Existing foreground-priority scroll hooks only record interaction times. The checked original 3.83.74 `onScrollChanged` invokes the WebView superclass and forwards coordinates to the app listener; the actual 3.84.12 APK has not been inspected.

## What remains unknown

The evidence does not distinguish missing or hidden page elements and lazy image handling from WebView paint/composition loss. Native geometry and a successful visual callback cannot certify pixels. Neither out-of-memory nor a GPU defect is established. Native scroll units, WebView scale and CSS content dimensions must not be compared without conversion; WebView `getScale()` also includes device density.

## Scope of the next diagnostic update

Capture bounded, same-document numeric viewport/scroll observations after restore, after scrolling, and when the user opens the report. Include sampling time and motion during asynchronous collection, bounded image readiness counts and the loaded WebView provider version. Preserve the earlier loading completion and first-frame presentation behavior. Do not add speculative reloads, zoom changes, CSS changes, layer toggles, cache/history clearing, or per-scroll forced redraws.

This is a diagnostic update, not a demonstrated repair of the screenshot symptom. Host tests and APK transformation checks do not replace physical Android rendering tests.
