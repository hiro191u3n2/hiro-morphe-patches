# X native Back boundary, version 12.19.1-release.0

Supplied APK: `Twitter-v12.19.1-release.0-patches-v3.10.0-dev.4.apk`.
SHA-256: `f6792d27f18ecb7e37fddcd1f67cf6f65f4d667fd3a8941e09c7ac266058e1ae`.
The findings below come from that APK's DEX, not a host UI fixture.

## Native decision and selected tab

`com.twitter.app.common.inject.p.onBackPressed()` obtains the native navigator,
which calls `com.twitter.app.main.l1.goBack()Z`. The latter first handles the
expanded FAB, open drawer, typeahead and onboarding gates. It obtains the
selected page's `ui.navigation.b` and calls its `goBack()`. A consumed child
branches to the existing handled return. Only an unconsumed child (or no child
handler) reaches original PC87, where the native code begins selecting Home.

The seven-instruction exit hook is inserted before PC87 (instruction 45).
Both incoming `IF_EQZ` branches, original PCs 77 and 83, must enter the hook.
The native false continuation still begins at the original Home URI load.
The true continuation returns the native constant `v1 = 1`. There are no native
try regions in this method. Register v0 retains the live pager; v2 and v3 are
scratch at the boundary, and v10 is the navigator receiver.

The hook reads the public-final inherited `legacy.h.b:androidx.fragment.app.x`
Activity and calls the live pager's `ui.viewpager.h.p():Uri`. The concrete pager
inherits `ui.viewpager.a.p()`, which reads `ViewPager.getCurrentItem()` and the
corresponding `ui.util.l.a` URI. It does not infer the selected tab from labels,
view geometry, cached observations or lifecycle readiness.

| User tab | Native constant | Exact selected URI |
| --- | --- | --- |
| Home | `main.api.b.a` | `twitter://timeline/home` |
| Search / Explore | `main.api.b.e` | `twitter://moments` |
| Notifications | `main.api.b.b` | `twitter://notifications` |
| Chat | `main.api.b.l` | `twitter://xchat` |

`twitter://dms` (`b.d`) is a distinct legacy DMS route. Explore's factory uses
`b.e`; the XChat factory uses `b.l` and `XChatTabFragment`.

## Actual Back ownership and startup

The same navigator also serves toolbar and internal navigation. The runtime
therefore requires a currently committed Android Back transaction for the same
Activity and session epoch. It deliberately does not reuse the old transaction's
`eligible` UI heuristic. Generic MainActivity classification defers to this
native boundary so it cannot exit before the native layer/child handlers.
Predictive gesture start and cancellation do not begin this transaction.

The existing launcher reset uses explicit `page=twitter://timeline/home` before
native onCreate/onNewIntent bodies. Native `l1.I3(Intent)` uses that explicit page
before its `last_tab_used` database fallback. Existing external intent handling
remains distinct from ordinary launcher reset; no app database is erased.

## Reproducible fingerprints and limits

* `l1.goBack()Z`: 11 registers, 123 original instructions, no try regions;
  digest `a213880a9ad8bb57e0882ffccdc87e1c6fbf3bd448c38db07c491ddf6dca48e7`.
* `ui.viewpager.a.p()Uri`: digest
  `29b22619b9e8778d2e8ea6b48efb2e2c4ab45c75eaaa444b3a3638c5ef4c7c65`.
* `main.api.b.<clinit>`: digest
  `d57468df55ce439b0ace68e9af975897ece9b221c55f8ed7df4c2ae340f22443`.

Digest format is `OneBackPatch.instagramNavigationDigest`, shared by the
preflight and inspection helper. Raw read-only excerpts were produced in
`work/x124-native/{exact-contract-digests,native-tab-contract,tab-uri-consumers,navigator-back-callers}.txt`.
The actual emitted DEX is independently audited by TwitterRootAudit and the
full-APK ActualXAudit; host tests cover runtime decisions. Physical device
execution is not established by these static/host checks.
