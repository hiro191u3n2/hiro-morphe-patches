# X native Back re-audit for 1.1.15

The previous root-tab insertion was present in the applied APK, but it was insufficient: some Android Back entries never reached that insertion. This audit separates actual DEX facts, published-runtime reproductions, and host validation. No trace from the user's device was available, so the exact route used on that device is not established.

## Pinned target and native boundary

The user-provided APK is Twitter 12.19.1 / patches 3.10.0-dev.4, SHA-256 `f6792d27f18ecb7e37fddcd1f67cf6f65f4d667fd3a8941e09c7ac266058e1ae`. The applied published 124 APK is SHA-256 `558b030fe5bfacb9e3d4e60ca454d1b936742f9a86e2692da44a668cf084ca75`. `key-dispatch/provenance.json` pins all baseline inputs.

The active legacy host is `com.twitter.app.main.MainActivity`. Its inherited `com.twitter.app.common.inject.p.onBackPressed` enters the native navigator. The verified `com.twitter.app.main.l1.goBack` root insertion follows native FAB, drawer, typeahead, onboarding and child-navigation branches. The selected root URIs remain exactly `twitter://timeline/home`, `twitter://moments`, `twitter://notifications`, and `twitter://xchat`. The 125 fix preserves that insertion and native branch order.

Original `l1.goBack` has 123 instructions and method digest `a213880a9ad8bb57e0882ffccdc87e1c6fbf3bd448c38db07c491ddf6dca48e7`; published 124 has 130 instructions and digest `a31a6182b0b00e1052952a236ffaec02a35e293ab7ac9ef068372a16f995f7c1`. Full route digests and exact relevant DEX excerpts are retained under `key-dispatch/`.

## Confirmed omissions in the published runtime

1. **No X Main system observer.** Generic `action(MainActivity)` intentionally returns zero to preserve native guards. The old observer-registration predicate accepted only a nonzero action or Trip observation, so it never registered for this Main host. If AndroidX had no enabled native callback, Android could complete system Back without public `p.onBackPressed`, the root insertion, task removal, or the persistent next-launch Home marker. The baseline reports `appCallbackRegistered=false`, zero native-root calls, no removal, and no marker.

2. **AndroidX fallback bypasses the native navigator.** Actual `androidx.activity.l0.d` invokes an enabled child callback first. Without one it invokes its fallback Runnable; `androidx.activity.o.run` reaches `ComponentActivity.access$onBackPressed$s1027565324`, whose original body calls framework `Activity.onBackPressed`. Published 124 already redirects that terminal call through its kind-0 bridge, but its pre-native eligibility can reject a valid Back when an editor retains focus. The old probe reports one original callback, zero native-root calls, a finished host, no removal, and no marker. Calling `onResume` did not change which callback was selected.

3. **Legacy key release invalidates its own attempt.** Actual key dispatch passes through base `i`, AppCompat `g`, core `h`, then framework `Activity.dispatchKeyEvent`. The key-up wrapper captures an attempt before that superclass call. The injected `onUserInteraction` then invalidated its epoch, so nested native wrappers could neither use nor replace it. The old probe rejects both root and terminal hooks and creates no marker.

Android 16 framework evidence comes from the primary `Activity.java` source, blob `c129fde3f81916e3ed4e2e67b77bfeac2288a1dc`. `onCreate` installs a private system Back callback when enabled; that callback delegates to `ActivityClient` without using the public override. `dispatchKeyEvent` calls `onUserInteraction` without an ACTION_DOWN restriction. These source facts explain why testing only a direct public Back call missed both routes. Source URL: https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/android/app/Activity.java

## Corrections and preserved ordering

The exact legacy Main now gets a default-priority observer **only while no tracked native app callback exists**. A successful native callback registration removes the observer; removal of the last native callback restores it synchronously on the main thread. Failed native register/unregister operations retain the previous bookkeeping. Native callbacks keep their priority, ordering, and complete predictive-animation stream, including Compose child consumption and cancellation.

Observer eligibility retains focused/attached/visible-window, known-insets, IME-hidden, and competing-window guards. Only the editor-focus heuristic is skipped for this exact host: an editor can retain focus after its keyboard closes, and native child/typeahead handlers still run before the root gate. An unwrapped framework IME callback remains responsible for closing the visible keyboard.

A live, matching Back attempt at the verified kind-0 AndroidX fallback may visit public native `p.onBackPressed` once. The exact p wrapper now calls `enterTwitterBack`; its depth marker prevents a native p-to-dispatcher fallback from replaying p. Nested wrappers and exceptions restore that marker correctly. Enabled AndroidX/Compose handlers still run before any fallback reroute. An already-authorized native terminal operation (kinds 1–4) also no longer relies on stale pre-native UI eligibility; owner, epoch, liveness, thread and exact-host checks remain.

For legacy keys, a same-owner synchronous Back interaction retains its captured epoch. Later input, an unrelated owner, departure, or an ended attempt remains invalid. This is not a global relaxation of stale-attempt protection.

All successful root exits use the existing task-exclusion/removal path and persist the existing next-launch Home marker. Actual patched p.onCreate calls install before native dependency graph construction and the `l1.I3` explicit-page reader. `I3` remains byte-for-byte unchanged in the method contract. A missing marker therefore explains both Recents retention and the remembered tab after restart. Launcher routing and the disabled alternate Main family are audited separately in the neighboring evidence documents.

## Validation and limits

All three old failure outputs were reproduced byte-for-byte against the pinned 124 compiled classes. New host suites cover 43 system cases / 321 assertions, 19 key cases / 126 assertions, and 24 terminal cases / 234 assertions. Independent review ran all 21 host suites successfully; `runtime-independent-review125.json` records their results and frozen runtime source hashes. Final packaging and real-APK structural audits are separate release gates.

System cases include the no-native-callback observer route, native callbacks before binding and after resume, every root URI, native layers/child consumption, predictive cancellation, dialog and IME ownership, immediate callback restoration, register/unregister failures, wrong owner/thread, recursive fallback and exception cleanup. These are host models backed by actual DEX contracts, not physical Galaxy/Android 16 tests. They establish the repaired entry and ownership logic, but do not claim a device-observed result.
