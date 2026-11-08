# Strict search v1.0.1: native map pins

## Scope and conclusion

This contract covers Maps `26.36.04.973607363` and the two previously inspected APKs. It records why the v1.0.0 strategy of removing the native search layer is insufficient, and why v1.0.1 filters that layer's known pin records while preserving their native metadata.

| Input | SHA-256 |
|---|---|
| microG Maps v2.8.5 | `7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063` |
| microG Maps v2.8.7 | `238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71` |

The user reported correctly filtered RANDA/SNIDEL lists but no result pins. The screenshots contain multiple list entries. No live response payload or device trace was available for this inspection; the exact flags and coordinates of those responses were not observed. Static inspection nevertheless establishes concrete paths by which the v1.0.0 implementation loses every pin even when several list cards remain.

## Why the previous fallback is insufficient

v1.0.0 removes `cqgm.o:Lcjme.c:Lcbkh`, clears the corresponding layer-present bit, and clears the model's `Lavdv.ag`. `Laveg.c(Lauwz;)V` then cannot use that native layer and takes its legacy placemark path.

The modern UI list and legacy map candidate list are different collections:

1. `Lavdv.an(...)` appends a recognized modern `cqgm.L` result to its first list at code offset `0x00e2`. It then reads `chaq.b & 1` at `0x00e5..0x00e9`. When that bit is set it omits the result from the second list; only the other branch appends it at `0x00eb`.
2. The bit is the presence of `chaq.e:Lckwg`, consumed by `Lauwk.v` when constructing search-list cards. A visible modern card therefore does not imply inclusion in the legacy map list.
3. `Lavdv.aj(...)` consumes the first list for ordinary result models, but builds `ae` from the second list (`0x0282..0x02a1`). `Lavdv.j()` enumerates `ae` and returns non-null `Loku` placemarks. `Laveg.d` uses this result for `Lauwz.b`.
4. In `Laveg.c`, a null `Lauwz.a:Lcbkh` leads to the fallback at `0x017e`. It clears the native overlay with `Laveg.f()` at `0x0180`. A nonempty `Lauwz.b` goes to `Layxa.V` at `0x018d`; an empty list clears legacy shapes instead.

Thus a response with five visible list cards can supply zero fallback candidates. This is a structural failure of forcing fallback, independent of the additional singleton condition below.

The fallback also has independent rendering prerequisites:

| Stage | Verified restriction |
|---|---|
| `Layxa.V` | Deduplicates by `Laver.a() -> Loku.p() -> Lbjap.c:J`, the numeric low/CID component. Missing or unusable numeric IDs can collapse several results into one `(0,0)` identity. |
| `Layxa.aM`, `0x000a` | Returns without drawing when the deduplicated size is **less than or equal to 1**. The constant `1` is reused later, so changing that constant alone would corrupt other logic. |
| `Layxa.aM`, `0x0040..0x0044` | Skips an item when `Loku.q()` returns no coordinate. That getter uses its cached coordinate or `cqdn.b & 1` with `cqdn.g:Lcdsy`; it does not recover coordinates from the deleted native payload. |
| `Layxa.aM`, early loop setup | Uses a configured prefix cap, default 10 in the inspected supplier. |

Missing icon metadata alone is not an all-item skip: default icon styles exist. Changing only the singleton condition would leave the empty-candidate and missing-coordinate cases unresolved. No fallback renderer instructions are changed by v1.0.1.

Evidence: `renderer-builders-factory.txt`, `renderer-map-metadata.txt`, `renderer-chaq.txt`, `renderer-chaq-metadata-xrefs.txt`, `v285-Laveg.txt`, and `renderer-inner/RENDERER_FALLBACK.md` in the reverse-query evidence directory.

## Known native pin schema

`cqgm.o.c:Lcbkh` carries the native search-layer parameters. Its repeated `e:Lcnak` field is a list of `Lcbki` records. Each record's `c:Lccpa.d:J` is the low/CID component. List results expose the comparable identity through `cqdn.h:ckiu.c:ccff.c:String`, or through `ccfl.c:ccff.c` for the alternative result type. The host parses decimal and `0x...:0x...` IDs, and `Lbjap.q` compares the low component for place equality.

The runtime reflects exactly these additional field contracts:

| Owner | Field | Type | Access flags |
|---|---|---|---:|
| `Lcbkh;` | `c` | `I` | 1 |
| `Lcbkh;` | `e` | `Lcnak;` | 1 |
| `Lcbkh;` | `d` | `Lcbkj;` | 1 |
| `Lcbkh;` | `i` | `Lcbke;` | 2 |
| `Lcbkh;` | `j` | `Lcbkg;` | 2 |
| `Lcbki;` | `c` | `Lccpa;` | 1 |
| `Lccpa;` | `d` | `J` | 1 |
| `Lcqdn;` | `h` | `Lckiu;` | 1 |
| `Lckiu;` | `c` | `Lccff;` | 1 |
| `Lccff;` | `c` | `Ljava/lang/String;` | 1 |
| `Lccfl;` | `c` | `Lccff;` | 1 |
| `Lcbkj;` | `c` | `Lcbki;` | 1 |
| `Lcbke;` | `c` | `Lcbki;` | 2 |
| `Lcbkg;` | `c` | `Lcbki;` | 2 |

The private fields are nonstatic, nonfinal instance fields and require accessible reflection. Their containing class APIs and complete canonical instruction bodies are frozen at patch time, including the protobuf `dynamicMethod` schema. `Lcbkh`, `Lcqdn`, and `Lccfl` were already covered by v1.0.0's complete class guards; v1.0.1 adds the seven newly reflected classes and freezes `Lccpb` as a supporting preserved-geometry contract. The runtime does not reflect geometry fields.

Retained `cbki` objects are kept intact. In particular, `cbki.d:Lccpb` retains native coordinates and `cbki.e/f` retain metadata and associations. The nested `cbki` objects in association structures can describe parent/floor relationships; they are not blindly pruned as if every nested point were a separate search result.

## v1.0.1 transformation and incomplete-identity policy

The response is cloned using the verified protobuf builder contract. The list filter collects matching and rejected place CIDs from all supported ordinary, modern-wrapper, and supplemental result containers. The native filter copies `cbkh.e`, preserving record order and each retained pin object.

Known single-point roots are handled as well as the repeated list:

| Root | Point | Presence mask removed with an unmatched wrapper |
|---|---|---:|
| `cbkh.d:Lcbkj` | `cbkj.c:Lcbki` | `0x2` |
| `cbkh.i:Lcbke` | `cbke.c:Lcbki` | `0x20` |
| `cbkh.j:Lcbkg` | `cbkg.c:Lcbki` | `0x1000` |

The map's `cjme` layer-present bit is retained. Other layer metadata is preserved; only the known excluded records and the presence bits of removed selected wrappers change.

The implemented incomplete-identity policy deliberately favors retaining usable native pins over silently blanking the map:

- A confirmed empty set of accepted list candidates removes all known repeated and selected pin records.
- A known accepted CID is retained. A known rejected CID is removed unless the same CID was also accepted elsewhere.
- When accepted candidates lack usable CIDs, unclassified native records are retained; a native pin with no usable CID is also retained unless the accepted set is confirmed empty.
- If candidates are absent, or filtering would remove every known native record despite a nonempty accepted list, the original native metadata is retained.
- Unsupported native data leaves that metadata intact while allowing the verified list filter to succeed. The runtime emits one fixed diagnostic without logging the query or place data.

Consequently, v1.0.1 does not guarantee that every unrelated native pin disappears when identities cannot be verified. The policy is explicit in `SearchFilter.Schema.filterNativeMetadata` and `NativeSelection`; it is not a claim of complete knowledge of opaque payloads.

## Model ownership and merge behavior

The hook remains at the request-owned boundary in `Lavds.f`, passing the response, its own `cqgi.e` query, and its request model. It uses no global current-query string.

On a successful filter, the helper clears an earlier model `ag` under the model write lock immediately before the host parser consumes the new response. `Lavdv.aj` stores the exact returned response in `S` and reconstructs `ag` from the retained, filtered native layer. This preliminary clear still prevents stale layer reuse when the next response has no layer.

The accepted-merge hook in `Lavdv.O` is unchanged. **The v1.0.1 `reconcileModel` must preserve the native layer that `O` has already merged.** It only propagates or invalidates the patch-owned response marker; the v1.0.0 post-merge `ag = null` behavior must not survive the runtime replacement. Retaining that old operation would erase the repaired pins again. The marker remains private, transient, synthetic, and response-identity-based.

## Native consumer boundary

`Laveg.c` selects native versus legacy rendering. A non-null native message alone is not a universal branch guarantee: the method also tests `Lawba.q()` and, on one branch, compares the backend's current payload. This contract preserves the host's dispatch; it does not override those unrelated choices.

Once `Laveh.c(Lcbkh;)V` consumes the supplied native message:

- Presence of selected/special roots determines the layer type. Without those branches, a nonempty `e` selects `GMM_CATEGORICAL_SEARCH_RESULTS_INJECTION`; empty `e` selects `GMM_SEARCH_RESULTS`.
- The newer backend inserts the **supplied `cbkh`** into the `Lchsl` extension at `0x00d0..0x00d2`.
- The older backend places the supplied message in `Lbkla.a` at `0x00fc`. `Lbklb.b` preserves that message when copying configuration. `Lbklb.equals` includes it in the store-reuse comparison. `Lbkmx.b` passes that same message into the search paint-request extension.
- `Lbehy.v` derives a key from the message hash; it does not reconstruct a point collection from another query or result model.
- The inspected Java layer-replacement path does not union an outside unfiltered result list into the supplied message.

These are bounded Java findings. They establish that the supplied filtered message reaches the paint request without a Java substitution from another result model. They do not establish the complete behavior of JNI, server responses, or lower tile caches.

## Frozen renderer guards

Both APKs and both pinned Morphe SDKs were checked in original and mutable-method forms. SDK-specific DexPool serialization explains the two accepted hashes; target APKs never supply new trusted hashes automatically.

| Method | Flags / registers / instructions / code units | `morphe.jar` SHA-256 | `morphe-2.2.jar` SHA-256 |
|---|---|---|---|
| `Laveg;->c(Lauwz;)V` | 17 / 9 / 203 / 420 | `cfb651c16b57a95aaa92a33458ede09365e9644918b410bcc219adc434fb0b88` | `68c7a07e2e2f138c28c9d9114b4bc8ccf02888177221d1eec674907a4803091e` |
| `Laveh;->c(Lcbkh;)V` | 17 / 9 / 192 / 381 | `964944c98a5ebed12cfb1f4e7ab57382a5b035b6bcabca8edf4b2191f2643140` | `e6855629d4d5f21ee39720fcb616b40c49c040fbab17cbdb2a447e43a5c2d1f3` |

Both owners are public final classes with flags 17, `java.lang.Object` superclass, and no interfaces. `NativeContracts.validate` checks these consumer boundaries for both fresh application and the normalized v1.0.0 upgrade path. The complete capture is in `NATIVE_RENDERER_CONTRACT.json`; the additional protobuf schemas are in `NATIVE_PIN_CONTRACT.json`.

## Remaining limits and release claims

The lack of declared Java fields is not proof that a native protobuf message has no data. Optimized constructors for some messages still call list initializers even though those fields are not declared, and protobuf unknown fields can survive cloning. In particular, `cbkh.g/h/k` must not be described as proven empty or harmless purely because their Java classes have no instance fields. v1.0.1 preserves opaque metadata and does not decode JNI-only payloads.

The verified result is a guarded native-pin transformation with tests and APK applicability checks. Actual Android map rendering for the reported searches remains unobserved in this environment. User-facing release notes should distinguish the static/unit/APK verification from on-device confirmation, and should not promise absolute exclusion of unrelated pins when the incomplete-identity fallback preserves native records.
