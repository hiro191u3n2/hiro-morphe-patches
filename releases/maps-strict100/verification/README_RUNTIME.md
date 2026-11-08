# Maps strict search runtime

## Entry point and integration

`app.hiro.mapsstrict.SearchFilter.filterResponse(Object response, String actualQuery, Object model)`
returns either an immutable filtered response or the original response. The caller
must supply the query from the same `Lavds` request that owns the response. No
process-wide current-query state exists.

`SearchFilter.reconcileModel(Object receiver, Object incoming)` runs only on the
accepted merge branch of `Lavdv.O`, after the native-layer merge and before the
instruction that overwrites the incoming-model register. The common final return
also serves rejected merges and is not an appropriate injection point.

Compile `SearchFilter.java` together with the final `StrictNameMatcher.java` and
the Android SDK. Do not package `RuntimeFilterTest.java`, the default-package mock
protobuf classes, or `test-stubs/android/util/Log.java` into the extension.

## Implemented paths

- Legacy ordinary places: `cqgm.d -> cqdn.l`.
- Modern ordinary places: `cqgm.L -> chaq(c=2).d -> cqdn.l`.
- Modern alternative places: `chaq(c=8).d -> ccfl.d`. The title mapping was
  verified through `Lokx.ab -> Lokx.W -> oli.R -> Lokx.a -> cqdn.l`.
- Modern supplemental groups: tag 5, `cfhm.l -> cffs.c -> cqdn.l`.
- Modern promoted and supplementary places: tags 6 and 7, respectively
  `cfmi.b -> cqdn.l` and `cfgz.b -> cqdn.l`.
- Legacy supplemental groups: `cqgm.i.c.b -> cfhm`.
- Legacy supplementary places: `cqgm.i.d.b -> cfgz`.
- Legacy promoted places: `cqgm.i.g.b -> cfmi`.

Both `d` and `L` are filtered because Maps selects the legacy list whenever `L`
becomes empty. A legitimate zero-match result remains empty; it does not revert
to unrelated original results.

For every enabled strict query, a present native search layer is removed from a
copy of `cqgm.o` by clearing `cjme.c` and bit 0 of `cjme.b`. This also applies when
all fetched names match. `Laveg.c` then uses Maps' existing placemark-list renderer
instead of the native `SEARCH_RESULTS_LAYER`, which can contain additional pins.
Other map metadata is retained.

### Reused model and merge handling

The DEX patch adds one private, transient, synthetic `Object` field to `Lavdv`:
`hiroStrictSearchResponse`, access flags `4226` (`0x1082`). It is excluded from
serialization and has no process-wide state. The runtime validates the field's
type, private visibility, and transient modifier; the DEX patch guard additionally
validates the synthetic bit.

On every `filterResponse` call, the marker is first invalidated. After all strict
filtering and immutable builds succeed, the existing model's `ag` native layer is
cleared and the marker records the exact returned response object. Marker reset
and commit use the model's existing `Lonf.e():Lone` write lock; `Lone.close()`
releases it. The host parser subsequently stores that same response in `Lavdv.S`.
This clears stale native layers on retries and request reuse as well as on fresh
models. Category and failed-filter calls invalidate only the patch marker and
preserve the existing native layer.

`reconcileModel` accepts incoming success only when its marker is non-null and
identical to its current `S` response. A successful accepted merge clears the
receiver's native layer and sets its marker to the receiver's current `S`.
Otherwise it invalidates the receiver marker while preserving ordinary native
merge behavior. The helper uses the receiver write lock and incoming read lock
already held by `Lavdv.O`; it does not acquire another model lock. Direct parser
reuse, clear/restore operations, and later failed calls cannot propagate a stale
success marker with a different response identity.

The model marker controls native-layer reuse. It does **not** certify that every
historical row or the receiver's stored response was refiltered: `Lavdv.O` does
not replace the receiver's `S`. Previously accumulated rows from an earlier
fail-open response are outside the error-recovery guarantee. A new search or the
host's normal model clear supplies a fresh filtered model.

The inspected pagination paths do not advance by the filtered list size.
`Lavdv.aj` stores the request's `cqgi.i` count in `Lavdv.L`, `Lavdv.O` adds the
old and incoming counts, and `Lauxv.g` / `Lasdl.e` set the following request's
`cqgi.h` from `Lavdv.s()` (that stored count). Filtering leaves these fields and
continuation metadata untouched.

## Original-object preservation

The runtime resolves the exact schema using the response class loader. It requires
the input message to be immutable. Changes use `cmzs.toBuilder`,
`cmzk.copyOnWrite`, `cmzk.instance`, and `cmzk.build`; each edited object is checked
to be distinct from its source. A changed repeated field is copied through
`cnak.e(original.size())` before clearing or adding to the new list.

The original protobuf message, nested messages, and lists are not edited. Retained items
keep their relative order and identity. Unchanged query, pagination, viewport,
IDs, wrapper flags, and other metadata remain on the copied response.

Model/schema access is validated before filtering. The layer clear is the last
potentially failing model-field write after all protobuf operations have
succeeded. The marker is written first, and is invalidated on a failure; category
or failed filtering does not clear the original model's native layer. Lock-release
diagnostics do not convert an already committed result into a return of the
original response.

## Deliberate preservation limits

Missing, empty, and whitespace-only names are retained because they cannot be
proved to name a different business. Empty original supplemental groups and
groups containing entries with unknown place names are also retained. Known
nonmatching entries are still removed from a mixed group; a group whose named
entries are all removed is itself removed. Unrecognized modern payload tags are
preserved as metadata instead of being assumed to be places. Consequently this
version cannot guarantee removal of a future or unknown place schema that does
not supply one of the verified display-name fields.

Category and other excluded queries are returned unchanged by the matcher after
model-state validation and marker invalidation, without protobuf-schema access.
An unsupported schema, malformed known payload, unexpected mutable
input, failed clone, or reflection error returns the original response. The
runtime emits at most one fixed diagnostic per process, with no query, place,
response, or exception text.

## Verification performed

1. Independent read-only dexlib2 verification of both Maps APKs, v2.8.5 and
   v2.8.7, version `26.36.04.973607363`: all 17 relevant classes, 21 fields,
   and 6 methods match the runtime reflection contract. Relevant API and method
   instruction fingerprints are identical between the two APKs. Exact evidence,
   APK SHA-256 values, and access flags are in `NATIVE_RUNTIME_CONTRACT.json`.
2. `RuntimeFilterTest`: 85 assertions passed using the actual reflected class,
   field, and method names and the real matcher. Cases cover RANDA-only, mixed
   results, all-mismatch empty results, modern-to-legacy fallback, all known
   supplemental routes, native layer removal, category bypass, unknown names,
   immutable originals, concurrent independent queries, and injected failures.
   Additional cases cover stale native layers on request reuse, exact response
   identity, direct parser reuse, cleared/restored state, category/fail-open
   merges, missing snapshots, helper schema exceptions, balanced filter-side
   locks, and absence of merge-side lock acquisition.
3. Production Java 8 compilation against `android.jar` passed, independently of
   host mocks.

The additional model/lock contract is recorded separately in
`schema-guard/MODEL_RUNTIME_CONTRACT.json`. The pinned original APKs do not contain
the patch-added marker; the final patched DEX must verify that field separately.

These are static APK checks and host behavioral tests. They do not constitute
execution of Google Maps on an Android device or a live server search.

## Host test command

Set `MATCHER_SOURCE` to the final matcher source file, then compile these four
files to an isolated test directory: `test-stubs/android/util/Log.java`,
`StrictNameMatcher.java`, `SearchFilter.java`, and `RuntimeFilterTest.java`.
Run the resulting default-package `RuntimeFilterTest` main class.

The mock protobuf classes exist solely to exercise immutable copy/freeze behavior
and the exact reflection ABI; they do not duplicate the search filtering logic.
