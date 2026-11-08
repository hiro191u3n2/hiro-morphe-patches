# Bicycle marker rendering contract — v1.0.1

## Input and scope

This repair targets Maps `26.36.04.973607363` (`1068763346`), in `org.ungoogled.android.apps.maps.microg` or `com.google.android.apps.maps`. Two previously supplied APKs were inspected:

- v2.8.5: SHA-256 `7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063`.
- v2.8.7: SHA-256 `238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71`.

The original method bodies and schemas used by the repair are identical in both inputs. `NativeContracts.java` preserves the v1.0.0 shared-labeler contracts; `RendererContracts.java` freezes the added contracts. Each method has two accepted hashes, corresponding only to the pinned current and 2.2 DexPool encodings of the same verified body. These are not learned from the APK being patched. Missing or changed methods, fields, wire schemas, callers, or cache configuration cause rejection before native code is changed.

This is host/DEX/native-code analysis. Android screen rendering, installation, and performance have not been tested on a device or emulator.

## Why the old filter could leave the screenshot markers

The old release intercepted only the shared-labeler Java factory. The APK also has a legacy Java renderer and a full GeoXP native renderer. The supplied screenshot shows numerous gray circular bicycle POIs; it does not identify which runtime renderer/configuration the phone chose.

`Lbjwu.m()Lbizp;` establishes the basemap separation. When `Lbjse.c()` is true, it returns `Lbkdx`, the Java no-op labeler used with native rendering. When c is false and `Lbjse.b()` is true, it constructs the `Lbkdm`/`Lbkdu` shared labeler using `Lbkbv`; `Lbkdm.M` creates `SharedLabelerJni`. When both are false it uses the legacy `Lbkbt`. The injected-annotation selector `Lbuou.f` independently agrees with those backends, using `Lbkui`, `Lbkuh`, or `Lbkug` respectively.

The native label-snapshot callback is for snapshots/logging and is not a drawing-factory interception point. Disabling only an annotation adapter would be inconsistent with the basemap renderer. This release keeps every backend selector unchanged and adds selective hooks at the verified legacy and native style boundaries.

## Exact native mutations

Only five existing classes are changed. The audit reconstructs each original class after removing the expected hook or restoring the one string and requires whole-class equivalence. All original instruction bodies, registers, exception handlers, native completion IDs, and unrelated methods remain intact.

| Owner | Target | Change |
| --- | --- | --- |
| `Lbkbv;` | `b(Lcift;Lbkeo;ILjava/util/List;Ljava/util/List;Lbkdc;Lbjiv;)Lbkba;` | Existing five-instruction shared-label prefix; add only for fresh input. |
| `Lbkex;` | `b(Lbken;Lcom/google/common/collect/ImmutableList;Lchvn;Lbkjj;Lbkeo;Lbkrf;Z)Lbkex;` | Five-instruction prefix checks the resolved complete legacy icon; returns the existing null result for a bicycle. |
| `Lbkup;` | `b(Ljava/lang/Object;)V` | Two instructions pass downloaded resource bytes through `BicycleLegendFilter.filter([B)[B` immediately before the original native success callback. |
| `Lcom/google/android/libraries/geo/mapcore/geoxp/jni/NativeMapFactory;` | `r(J[B)V` | Same two instructions before the original offline-cache success callback. |
| `Lcom/google/android/libraries/geo/mapcore/geoxp/jni/providers/MapCoreConfigProvider;` | `c(Lcmzk;)Lcdqi;` | Replace only the online cache directory string `geoxp_cache` with `geoxp_cache_hiro_bikes101`. |

No native `.so` bytes are modified. No map tiles, place names, coordinates, route mode, selected search-result data, or shared background assets are rewritten by the runtime filter. Suppression is based only on the exact verified bicycle icon paths.

## Shared-labeler contract retained from v1.0.0

```
Lcift.c == 2                 point label
Lcift.f                     List<Lcifq>, complete placed elements
Lcifq.c == 4                composite icon oneof
Lcifq.d                     Lcifs
Lcifs.b                     List<Lcifr>, icon layers
Lcifr.c                     Lcier, layer description
Lcier.b == 1                icon URL oneof
Lcier.c                     java.lang.String URL
```

`Lbkje.a(Lcifs;)Lbkje;` decodes exactly those fields. The factory is public final, with 35 registers, 435 instructions, 817 code units, and no try blocks. `this` is v27 and the placed label is v28. The original entry overwrites v0 before using it. The prefix calls `shouldHide(v28)`, stores the boolean in v0, branches to the original entry on false, or returns null on true. It adds five instructions/eight code units and does not alter the ABI.

All three discovered calls handle null. `SharedLabelerJni.nativeUpdatePlacedLabels([B)V` has two calls and removes previous render, collision, and hit-test state. `Lbjwc.run()V` has one call and branches on the null result. Their entire original bodies and immediate null guards remain frozen.

## Legacy full-icon contract

The legacy factory is public static, with 18 registers, 65 instructions, 100 code units, and no try blocks. Its arguments occupy v11–v17. The filter receives style v14 and per-feature override list v12. v0 is dead at entry and overwritten before its first original read on both paths. The prefix has the same five-instruction/eight-code-unit shape as the shared prefix, using a two-argument static call.

`BicycleMarkerFilter.shouldHideLegacy` reads `Lbkjj.t:Lbkje`, its direct URL `Lbkje.b`, and components `Lbkje.c` containing `Lbkjc.b` URLs. When feature overrides exist, it calls the APK's own public static `Lbken.j(ImmutableList,Lbkje)` before matching. This preserves the app's precedence for replacement images, inline bitmaps, and colors. Unknown reflection schemas or malformed objects return false. The resolver and all reflected schemas are frozen.

The two direct callers were identified with a complete DEX reference scan:

- `Lbkaw.p(Lchvo;Lbkdh;ILcom/google/common/collect/ImmutableList;Landroid/content/res/Resources;Lbken;Lbkfx;Lbkez;Lbkrf;Lbkeo;ZZ)Lbkaw;`: call at code unit `0xc8`; `move-result-object v3` at `0xcb`; `if-eqz v3` at `0xcc` returns null at `0xf4`. The whole parent label is discarded, including any previously assembled background.
- `Lbkcz.k(Lbkcx;Lbkdh;)Lbkcy;`: call at `0x785`; result v9 at `0x788`; `if-nez v9` at `0x789`; the null branch skips that feature at `0x6a7`.

Both callers' full bodies and immediate result guards are frozen. A bicycle glyph is not removed as an isolated texture layer, so its generic gray circle is not left behind.

## Selective CompactLegend wire transformation

The verified resource is a raw protobuf CompactLegend. The matching signature requires root field 11 `icon/name=assets/icons/` and root field 12 `.png&scale=4`. Root field 10 holds the icon-name string table; root field 9 contains `IconStyle` records. Within an icon record, field 5 selects a root-10 entry and field 1 supplies its suffix or a direct URL. `Lchyy.c(III)`, the generated `Lchuz` schema, and the decoder paths establish this mapping.

Root field 3 contains `LabelStyle` records (`Lchvx`). Field 8 contains packed or unpacked root-9 icon indices; field 3 contains inline icons. Field 5 is a `TextBoxStyle` (`Lchyj`) whose fields 6/9 hold inline icons or icon indices. Field 7 is the existing visibility-off boolean. The transformer appends `38 01` only to an eligible bicycle label style that is not already hidden, preserving protobuf last-scalar-wins semantics. Existing bytes are not mutated. Unchanged fields, order, data, and unrelated style records are copied verbatim.

This visibility flag is supported by both Java and native evidence:

- `Lbkjn.p(J)Lbkkn;` reads `Lchvx.m` and clears text (`bkji.n`), background (`bkji.o`), and icon (`bkji.p`) when true. `Lbkkp.i(Lchve;Lbkko;)V` makes the same visibility decision. Their complete methods and generated schemas are frozen.
- The native library is identical across the two supplied APKs: `libgoogle3.so`, 33,705,128 bytes, SHA-256 `71a3f31512d7f2e401e8eca320945b36d5923303cbe0746f92e938b27b65d393`. Its `maps_paint_client.LabelStyle` metadata is at `0x1f2e1b8`. The serializer at `0x16c3b38` reads object byte `+0x6c` and writes wire tag `0x38` (field 7); it writes the packed icon-index field with tag `0x42` (field 8). Native `legend_style_converter.cc` code sets this visibility byte at `0x19405c4` and reads it at `0x1940650` while handling hidden label styles. This confirms the same native field layout; no native patch is injected.

The exact bicycle asset families are the four observed `bike_pinlet_v3-2-medium` paths under `poi/quantum/modern_pinlet`, `poi/quantum/pinlet_88`, `poi/quantum/pinlet_v3`, and `road/icons`, plus the verified `transit/quantum/bikeshare` and `bikeshare_v2` sizes. The URI matcher accepts observed Google host aliases and exact bare `assets/icons/` paths, not names such as LUUP or generic bicycle text.

IconStyle field 8 is a per-feature replacement-image ID. A nonzero value makes the descriptor ineligible for global legend hiding, because its final image could be unrelated. Color-only field 13 does not replace the glyph. This guard avoids hiding a generic template whose bicycle asset may be replaced by a search-result pin. The legacy path checks after native per-feature resolution instead.

The pinned 2,115,516-byte Roadmap fixture contains 8,799 icon descriptors, 13,700 label styles, and 943 icon path entries. Fifteen exact bicycle descriptors are referenced by 60 label styles; 15 were already hidden and 45 gain the two-byte flag. The resulting size is 2,115,606 bytes and SHA-256 `cfeeaa10c623a16b9df085db1358f11f186c3edcb9ed6540c2066e1a116474ab`. Independent comparison of 92,576 root records found only those 45 eligible style payloads changed; all others remained byte-identical. A second filter call returns the same output object, establishing idempotence for the fixture. The actual fixture contains no root field 31/ClientLegendDag; support for a different future native legend format is not asserted.

The parser bounds input size, record counts, component counts, and string lengths. Non-legend resources, unsupported/malformed data, unrecognized wire types, invalid icon indices, and exceptions return the original array. Unknown compressed application envelopes are not guessed or re-encoded. The verified fixture is raw protobuf; no captured response headers or on-device payload are available to establish every possible compression variant.

## Native resource delivery and stale cache handling

The normal native URL path is `NativeUrlLoader.loadUrl` → `Lbjmf.run` → `Lcamt.y` → the URL HTTP resource future → `Lbkup.b(Object)`. The transport callback `Laynj` extends `org.chromium.net.UrlRequest.Callback`: `onReadCompleted` writes the Cronet response buffers to `ByteArrayOutputStream`; `onSucceeded` constructs `Layno.c` from `toByteArray()` through `Lcmyn.y`. The inspected Java transport introduces no custom compression envelope. The callback extracts `Layno.c:Lcmyn`, calls `Lcmyn.K()[B`, and leaves its returned array in v5. The filter is inserted at original instruction index 15, immediately before `NativeMapFactory.nativeLoadUrlSuccess(JJ[B)V`. The original native handle v0/v1 and request ID v2/v3 remain intact. The callback still executes once, then returns.

Offline resources use `NativeOfflineDiskCache.fetchResource`, the existing backend callback, and `NativeMapFactory.r(J[B)V`. Its bytes are v5 and the original request ID is v3/v4. The filter is inserted at original instruction index 6, immediately before `nativeOnOfflineCacheFetchSuccess(JJ[B)V`; native handle v0/v1 and completion flow remain intact. The native failure paths are untouched.

Native online storage is persistent, so adding a network callback alone cannot establish coverage of previously cached style bytes. `MapCoreConfigProvider.c` builds internal/external cache-directory candidates, checks `map_cache.db`, and supplies the selected directory as `Lcdqi.c`. At instruction index 132/code unit `0x10b`, the single `const-string v8, geoxp_cache` feeds all those candidate paths. The new constant is `geoxp_cache_hiro_bikes101`. This deliberately causes a one-time online tile/resource cache refill so old native style bytes cannot bypass the repair's ingress hooks. The old cache is retained and can be evicted by Android normally; it is not deleted. This namespace should remain stable in future fixes unless another cache migration is necessary.

A complete DEX reference scan finds exactly one `geoxp_cache` string reference, at that configuration instruction. Neither `geoxp_cache` nor `geoxp_secure` occurs as a string in the native library, consistent with the native consumer receiving the selected path through configuration. The separate `geoxp_secure` path is unchanged, and offline cache storage/callbacks are not renamed or cleared. Both full original configuration bodies have the same frozen fingerprint and the string's register/index/code-unit location is also checked explicitly.

No renderer backend is forced. The first run may download online map data again; subsequent runs use the dedicated normal cache. Actual device latency, network usage, and all server-side style variants remain unmeasured.

## Exact old-version upgrade and preservation

The guard runs before extension merge. Fresh inputs must satisfy every original shared and added renderer contract. An upgrade is allowed only when every installed bicycle runtime class is byte-equivalent to the published v1.0.0 runtime and the shared hook is the exact original five-instruction prefix. The guard validates a detached copy with that prefix removed, including all new native targets. Partial or unknown installs and reapplication of the new version are rejected before mutation.

During application, the exact existing shared hook is retained; all new hooks are inserted once. Explicit runtime replacement is required because extension merging alone may keep old method bodies. The current extension replaces all of its own runtime classes. Other Morphe patches, native owners, resources, MPP providers, and payloads are checked separately by the full build audits.

The release QA records actual guard/application/audit results. The adjacent `NATIVE_LEGEND_VISIBILITY_EVIDENCE.txt`, `NATIVE_HTTP_RESOURCE_CALLBACK.txt`, and `RENDERER_CONTRACTS_CURRENT.txt` / `RENDERER_CONTRACTS_SDK22.txt` preserve the native snippets, response writer, and captured original fingerprints. Static and host results do not assert that the user's phone has already hidden the icons.
