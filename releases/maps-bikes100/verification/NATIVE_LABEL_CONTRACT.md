# Bicycle marker rendering contract

## Implemented provider scope

Production provider files are in `morphe-site/scripts/maps-bikes100/source/src/patch/`:

- `MapsHideBicycleMarksPatch.java`
- `NativeContracts.java`

Both verified APKs have byte-for-byte equivalent disassembly of the target. Their input SHA-256 values are frozen in NativeContracts comments; they are the previously supplied v2.8.5 and v2.8.7 inputs.

Target:

```
Lbkbv;->b(Lcift;Lbkeo;ILjava/util/List;Ljava/util/List;Lbkdc;Lbjiv;)Lbkba;
```

Native method: public final; registers 35; 435 instructions; 817 code units; zero try blocks. `this` is v27, placed-label p1 is v28. Original entry immediately overwrites v0, v1, v2 from v27, v28, v29. The hook adds exactly five instructions/eight code units. It calls the runtime with v28 using invoke-static/range, branches to original entry if false, and returns null if true. No original body or registers are changed.

## Concrete label and URL schema

```
Lcift.c == 2                 point label (coordinates in Lcigt)
Lcift.f                     List<Lcifq>, complete placed elements
Lcifq.c == 4                composite icon oneof (3 is text, 5 is another element)
Lcifq.d                     Lcifs
Lcifs.b                     List<Lcifr>, icon layers
Lcifr.c                     Lcier, layer description
Lcier.b == 1                icon URL oneof
Lcier.c                     java.lang.String URL
```

`Lbkje.a(Lcifs;)Lbkje;` reads precisely these layers/URLs and constructs the composed icon descriptor. The URL path is not a guessed unrelated UI drawable. NativeContracts freezes this decoder body plus complete fields/hierarchy schemas of cift/cifq/cifs/cifr/cier.

## Null-return safety

All three discovered factory call sites are checked:

- `SharedLabelerJni.nativeUpdatePlacedLabels([B)V` has two calls. Each branches on null and removes prior render, collision, and hit-test state for that label. Main call: invocation at original code-unit 0xc9, move-result 0xcc, if-nez 0xcd, null cleanup 0xcf–0x108. Second call 0x2fd, null branch at 0x301.
- `Lbjwc.run()V` call at original code-unit 0x548 checks/casts result then branches on null at 0x54e.

The caller full bodies, instruction counts, register counts, code-unit counts, try-block counts and immediate null guards are frozen. Unknown bodies/proto schemas reject before mutation.

Toolchain agent has run guards with both pinned SDKs on v2.8.5: PASS, 73,990 assertions each. It handles the production build and full APK preservation audit separately. Original and v2.8.7 fingerprints were independently captured with both SDKs; each SDK produces the same results across the two inputs.

## Findings that must not be used as bicycle hiding predicates

- `Lmjh` literal `bicycle_rental_service`: category-to-colour lookup. Its enum category also covers normal shops. Native corresponding literal is in ARLO CategoryColors, not proven 2D basemap style.
- Resource `bike_pin` 0x7f0802ee: plain white balloon background, no bicycle. Shared/generic resource; not the proven screenshot symbol.
- `Lbkdq.c` icon measurement failure callback: URL-specific but rejecting only a layer can leave a composed background/text. Whole-label factory rejection is more precise.
- `Lbkdk.ak()` / `Lbkaa.V == 20`: IS_BIKESHARING_VEHICLE metadata; `Lbkzs.k` is BIKESHARING_VEHICLE render layer. This does not prove fixed LUUP docks use the vehicle layer.
- `Lcjof.I`, BICYCLE_FACILITY value 82: an editable-property enum alongside NAME, ADDRESS, WEBSITE, ROAD attributes. It is not a label category.
- `Lcift.l` is Lchws extension metadata. `Lchwv.A` (extension 25000023) uses coarse style enum Lchzl (POI, BUSINESS, CAR_RENTAL, TRANSIT_STATION etc), with no bicycle-specific entry. `Lchwv.w` (25000019) is Lchvd generic name/ID/click metadata; no demonstrated bicycle category has been found.

## Symbol evidence and coverage limits

The actual bicycle glyph is server supplied and absent as a fixed APK literal. The assets investigation has now decoded the public Google Roadmap configuration and obtained `poi/quantum/pinlet_v3/bike_pinlet_v3-2-medium` from its style data. The official Google vt/icon URL returned a 104×104 PNG and was visually checked against the screenshot. The corresponding `modern_pinlet`, `pinlet_88`, and `road/icons` paths also occur in that style configuration. This supplies an observed symbol identifier for the runtime matcher; it does not establish on-device disappearance by itself. See the separately preserved asset investigation for the fetch evidence.

The APK has multiple rendering backends. `Lbuou.f(Lchxs;)Lbjjh;` selects legacy (`Lbkug`) when Lbjse.b is false, a full GeoXP renderer (`Lbkui` / Lblcb) when Lbjse.c is true, otherwise GeoXP labeler adapter (`Lbkuh`). The effective full-GeoXP gate is configuration driven: Lbjse.c returns its memoized b and c suppliers; the observed c supplier (Lbetc case 5) is true. The b supplier (Lbjcs case 5) reads startup flag `Layyn.bl`, `ENABLE_GEOXP_MAPCORE` / `b519033541`, when startup-flag migration is enabled, otherwise `Lahfn.b().Lbjhu.T`. The GPU allow/block-list branch in Lbjse.c is guarded by `Layyn.bI`, `AGMM_ENABLE_FEATURE_LEVEL_CHECK_LOGGING`; it must not be confused with the effective renderer gate. No fixed shared-renderer override was found in these inspected methods. This does not determine the user's downloaded configuration or actual backend.

The full-GeoXP Java entry is `Lblcb` → `NativeMapFactory`. Its native factory receives network/URL fetchers, observers and caches, not a SharedLabelerJni instance. The only observed Java construction of SharedLabelerJni is in `Lbkdm.M` at original code-units 0x75/0x7b. All observed consumers of Lcifs are in the Java renderer classes, not the full-GeoXP controller. The full-GeoXP label snapshot path is `NativeLabelSnapshotCallbackHandler.onLabelSnapshotUpdate` → `Lbjmq.run` → Lcidg parsing → Lbjmp/Consumer; this is a logging snapshot, not the Lbkbv placed-label factory. Its URL path is `NativeUrlLoader.loadUrl` → `Lbjmf.run` → `Lcamt.y`, which fetches image bytes and does not expose a verified whole-label rejection contract.

Native inspection also found the Java `nativeUpdatePlacedLabels([B)V` callback method ID being acquired by SharedLabelerJni.nativeInitClass (0x1b26d10), stored at 0x20cc4d0 and read by the SharedLabeler callback cluster at 0x1b2804c. The inspected NativeMapFactory/NativeMapController initializers register their native methods without acquiring that Java drawing callback. These observations support the separation; they do not prove a common full-native interception path. This provider therefore proves the shared-labeler Java factory path, not every full-native GeoXP label. No backend is forcibly changed, and generic icon layers are not blanked.

## Resolved-icon and list-argument review

`Lbken.j(ImmutableList,Lbkje)` can replace an icon using `List<Lchue>` custom-image data and wrap a bare asset name as `icon/name=...&scale=4`. That resolver is called by the separate Lchvn/Lbkjj style-renderer entries `Lbken.e` and `Lbken.h`.

The target Lbkbv point branch instead calls `Lbken.d(Lcifs,...)` or `Lbken.i(Lcifs,...)`. Both decode their icon URLs directly with `Lbkje.a(Lcifs)` and do not call Lbken.j. Its p4 and p5 lists are read by `Lbilf.v` as `List<Lchsz>` and `List<Lchyp>` for the layered/animated rendering choice, not as Lchue image overrides. Its asynchronous Lbkaz reload path repeats that same decision and d/i decoder calls. Passing those lists to an override-image matcher or guessing bare asset names would not follow the verified target contract.

## URL aliases and independent review

The original narrow host matcher missed aliases that exist in the input APK. These were reported before the final build and the runtime was revised to parse the URI, allow exact Google hosts and match exact observed asset paths after extracting the icon name. Primary APK examples include:

- `Laoup.f(Lazge;)Z` at 0x9f: `https://mts0.google.com/vt/icon/name=assets/icons/poi/quantum/container_background-2-medium.png,assets/icons/poi/quantum/container-2-medium.png,assets/icons/poi/quantum/parking-2-medium.png&highlight=ffffff,ffffff,4285f4&scale=4`.
- `Lauaa.ep(Lbzpg;)String` at 0x10: `https://mt0.google.com/vt/icon/name=icons/spotlight/stickers/%s_M_4x.png&scale=4`.
- `Lbjzc.Q(Lbjin;)V` at 0x19d: `http://mt0.google.com/vt/icon/name=icons/spotlight/measle_spotlight_L.png&scale=4`.
- `Lmiq.a(Laqgw;Laqix;Context)Lcbdi;` at 0xd3: `https://www.google.com/maps/vt/icon?name=assets/icons/poi/tactile/iamhere/work.png&scale=4`.

These demonstrate supported URL grammars, not a captured bicycle payload from the user's phone. Review of the revised runtime found its schema/oneof checks, exact glyph identifiers, URI host checks, rejection of user info/ambiguous duplicate name parameters, decode-after-parameter-splitting, bounded traversal and read-only inputs consistent with the verified contract. The five-instruction hook preserves the original register ABI and branches to the unchanged body on false. Returning null follows the existing caller cleanup path. No additional provider/runtime change was made during this independent review.

No Android device/runtime visual test has been performed.
