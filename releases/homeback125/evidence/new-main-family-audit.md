# Separate X Lite host audit (X 12.19.1)

The provided APK contains a separate `com.x.android.main.MainActivity` for the `x-lite` application family. Its manifest default is disabled, with its own launcher aliases and Decompose/Essenty navigation. This is distinct from the legacy `com.twitter.app.main.MainActivity` and `l1.goBack` path.

The existing-user automatic switch is disabled in the **actual provided APK**, regardless of its server flag values: `com.twitter.x.lite.featureswitches.p.a()Z` executes `CONST #0 v2` at PC36 and returns v2. Legacy `com.twitter.app.main.c2` checks that result before scheduling the switcher. The warm-start callback `featureswitches.q$b.emit` reads its feature setting but does not use the result or invoke a switcher. Explicit switching and new-user/onboarding paths remain present; the device's current component overrides were not observed.

The legacy embedded X Lite adapter does **not** instantiate the new MainLanding tab host:

- `com.twitter.x.lite.stack.f0.invoke` identifies `MainLandingArgs` at PC1165 and branches to PC1440, which throws `Unsupported navigation args`.
- `stack.z.i` converts `MainLandingArgs.initialTab` into a legacy main-tab URI through `stack.g0.a`, then returns it through legacy navigation. `stack.y0` constructs HOME arguments only to leave a DM error through this adapter.

These references therefore do not establish that the user's four root tabs bypass the legacy host. The demonstrated legacy Back registration/dispatch fault remains the justified correction scope. No new-host behavior is claimed as physically verified, and this audit adds no new-host exit hook or forced application-family change.

For future diagnosis, the separate new host initializes a normal empty launch to `MainLandingArgs(TabType.HOME)` in `com.x.android.main.a2.invoke`; its tab component `com.x.main.s` initializes from that argument. Its non-Home native tab Back callback is `com.x.settings.privacy.chats.r0.invoke`, case2, registered at Essenty priority31; its Home handler is `f2$d`, priority30. These are separate contracts from the legacy URI hook and would require dedicated dispatch, child-navigation, and owner validation if the new family is observed on device.

Exact input-APK excerpts and cryptographic provenance are recorded in `new-main-family-excerpts.txt` and `new-main-family-provenance.json`. The full read-only scratch dumps are named and hashed in the provenance; no APK is included in the evidence.
