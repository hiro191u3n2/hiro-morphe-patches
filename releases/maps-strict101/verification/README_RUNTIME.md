# Maps strict search 1.0.1: retain filtered native map pins

## Change and observed problem

Version 1.0.0 filtered the search rows and removed the complete native search
layer. Device feedback showed that matching RANDA and SNIDEL rows remained while
all map pins disappeared. The app's alternative renderer is not an equivalent
replacement: modern native-only result flags can leave its input list empty, and
its downstream renderer skips zero or one deduplicated result. Version 1.0.1
therefore keeps Maps' native renderer and filters its verified feature list.

The name matcher, its brand aliases, and category-query rules are unchanged.
The public runtime API remains:

```java
public static Object filterResponse(Object response, String actualQuery, Object model);
public static void reconcileModel(Object receiver, Object incoming);
```

## Native data that is filtered

The search metadata is `cqgm.o:cjme`, whose `c:cbkh` is the native search layer.
`cbkh.e:cnak` holds the native `cbki` result objects. Maps calls this metadata
`SearchPipeMetadata`; `Lavpz.pY` rejects an empty `e` as empty results, and the
native layer consumer chooses the ordinary result-layer mode based on `e.size()`.

Each `cbki.c:ccpa` holds a feature identifier. The low 64-bit CID is `ccpa.d`.
The ordinary place-model ID comes from `cqdn.h:ckiu -> c:ccff -> c:String`;
alternative modern results use `ccfl.c:ccff -> c:String`. The runtime collects
these IDs from both main representations and every supported supplemental path.
It does not use the fallback-only visible list, so modern native-only rows retain
their native pins.

Matching native result objects are retained whole, by identity and in their
original order. The code does not regenerate their coordinates, labels, style,
related-feature references, or opaque metadata. It copies only the changed
protobuf parents and repeated lists, preserving the original immutable graph.

The same native layer can also carry selected feature roots:

| Root field | Contained point | Presence bit |
|---|---|---:|
| `cbkh.d:cbkj` | `cbkj.c:cbki` | `0x2` |
| `cbkh.i:cbke` | `cbke.c:cbki` | `0x20` |
| `cbkh.j:cbkg` | `cbkg.c:cbki` | `0x1000` |

These roots can take precedence over `e` when Maps selects a native layer mode.
A rejected selected feature therefore removes its wrapper and clears only its
corresponding presence bit. Accepted selected objects remain unchanged. The
runtime preserves all other native fields and the layer-present bit in `cjme.b`.

## CID formats and API 23 compatibility

The runtime accepts validated 64-bit hexadecimal feature pairs, unsigned decimal
CIDs, and signed decimal CIDs. It compares the low 64-bit bit pattern, following
Maps' own `Lbjap.q(Lbjap)` implementation, which compares `Lbjap.c` only. This
allows a decimal CID to match a native point whose feature ID also has a nonzero
high component.

`BigInteger` performs the bounded conversion. No `Long.parseUnsignedLong` or
other API-26-only unsigned helper is used. Oversized, malformed, zero, missing,
and non-CID IDs are treated as unresolved rather than thrown into a query-wide
failure. The runtime does not log actual identifiers or query text.

## Complete and partial pin filtering

The response has separate list-filter and native-pin outcomes:

1. With complete retained-result ID coverage, native pins with identified IDs are
   restricted to accepted CIDs. An accepted title takes precedence if the same
   CID also occurs under a rejected title.
2. If a retained result lacks a usable CID, known rejected CIDs are removed, while
   unknown or otherwise unclassified native points are preserved.
3. Native points with missing feature IDs are conservatively retained unless all
   known candidate rows were conclusively excluded.
4. If accepted rows exist but filtering would remove every original native point,
   the original native metadata is preserved. The list filtering still applies.
   This avoids repeating the reported all-pins-disappear failure when ID mapping
   is incomplete or disagrees with the native response.
5. If every known candidate row is excluded, the native result list becomes empty
   and rejected selected-feature roots are cleared. This is a legitimate empty
   result, not an ID-mapping fallback.
6. An unsupported native point shape or native-copy failure preserves the original
   native metadata without undoing successful list filtering.

When pin filtering is partial, one fixed, query-free diagnostic is emitted after
the response/model commit succeeds: list filtering succeeded, while some native
pins were preserved because their IDs could not be verified. This diagnostic is
separate from the existing complete schema-failure diagnostic. Neither diagnostic
can change the return value of an already committed response.

"Complete" here refers to the verified Java-visible ID roots. It does not claim
that every possible opaque native payload has been interpreted.

## Model reuse and pagination

The private transient synthetic `avdv.hiroStrictSearchResponse:Object` field is
retained, with access flags `4226`. `filterResponse` still invalidates this marker
at entry and clears a stale `model.ag` only after successful strict filtering,
under the existing model write lock. The following host parser immediately
installs the retained, filtered `cjme.c` as the new native layer.

`reconcileModel` no longer clears the receiver's native layer. Maps' accepted
merge has already appended incoming `cbkh.e` to the existing layer. The helper
only propagates or invalidates the response-identity marker and relies on the
host's already-held locks. An additional page containing zero accepted results
therefore cannot erase accepted native pins from an earlier page.

The marker is native-layer reuse control, not proof that every historical row or
the receiver's stored response has been refiltered. Historical fail-open results
remain outside the error-recovery guarantee. Category queries return the original
response and native metadata by identity; their marker reset does not clear the
normal layer.

## Opaque native metadata boundary

The Java-visible result roots are `e`, `d`, `i`, and `j`. The inspected Java layer
passes the supplied `cbkh` to native paint requests rather than rebuilding an
unfiltered result set from another search model. However, R8 protobuf shrinking
and `unknownFields` mean an apparently empty Java message can still carry native
wire data. This version preserves those bytes, as discarding them could break
geometry or styling. Static Java analysis cannot prove how native/server code
interprets every opaque field. Device verification of the final map remains
necessary; host tests are not a claim of Android rendering execution.

## Verification

- `RuntimeFilterTest`: **144 assertions PASS** with the unchanged production name
  matcher and mocks using the exact reflected class, field, and method names.
- Production Java 8 compilation against `android.jar`: **PASS**.
- Regression cases include multiple native-only rows, a single accepted native
  pin, explicit zero matches, empty additional pages preserving older pins,
  selected roots and presence bits, eight unsigned/hex/decimal CID forms,
  malformed/oversized/missing IDs, unknown native point types, whole-pin identity,
  coordinates, label tokens, opaque bytes, original immutable graphs, category
  identity, independent concurrent queries, and helper exceptions.
- `NATIVE_PIN_CONTRACT.json` records independent static verification against both
  supported original APKs, including the exact fields and class fingerprints.
- No original APK or published project source was edited in this work directory.

Only `SearchFilter.java` and the unchanged `StrictNameMatcher.java` are production
runtime sources. Never package `RuntimeFilterTest.java`, its default-package
mock protobuf classes, or `test-stubs/android/util/Log.java` into the extension.
