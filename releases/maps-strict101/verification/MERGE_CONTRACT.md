> Historical v1.0.0 investigation retained for native control-flow evidence. In v1.0.1, the pre-parser stale layer reset remains, but the filtered native layer is retained in the response and the merge helper no longer clears ag. See PIN_CONTRACT.md and README_RUNTIME.md for current behavior.

# microG Maps result reuse and merge contract

Applies only to the two previously examined Maps **26.36.04.973607363** APKs. All offsets and instruction indices below refer to unpatched native code. The scope is the strict-search native-layer cleanup; no APK was modified by this investigation, and these findings do not constitute Android screen execution evidence.

## Why the response-only hook needed model handling

`Lavdv.aj` stores the native map layer in private field `ag:Lawfr`. When a response has no `cjme.c` native layer, `aj` does not actively clear an existing `ag`. Removing `cjme.c` from a filtered protobuf therefore prevents installation of a new native layer, but does not by itself remove one retained by a reused model.

`Lavdv.O` has the same inheritance behavior: at `0x01af` it reads incoming `ag`; when null, `0x01b1/0x01b3` jumps to `0x020f`, leaving the receiver's old `ag` unchanged.

There is no universal reset-before-response guarantee:

- New `Lbjsg.D/E` requests create a new `Lavds`, whose constructor calls `Lbeoq.an()` to allocate a new `Lavdv`. The cached-response constructor also parses into a fresh model.
- Main-search reuse `Lauwt.am` calls `request.g.N()` at `0x00d7` before submission. `N()` clears `ag` at `0x0056`.
- **`Lauxv.d()` reuses a saved `Lavds` without `N()`.** Its `h()` reads a stored `Lawvj` serializable; `d()` attaches a listener and invokes `Lavew.e` at `0x0015`.
- `Lavew.e` resets dispatcher/transport state, not `request.g`. Its result path eventually calls `Lavds.f` on that existing request.
- The enroute `Lavew.b` streaming path can retain its outstanding request and invoke `f` repeatedly without `N()`.
- `Lavds.readObject` restores a previously serialized model into `g` without calling `N()`.

Strict filtering from the first response on a fresh model keeps `ag` null. The failure case motivating this patch is an already populated native layer followed by successful strict filtering on a reused or merged model.

## Native model schema

| Native item | Required shape |
|---|---|
| `Lavdv;` | public class, flags `1`, superclass `Lonf;`, no direct interfaces |
| `Lavdv;->g:Ljava/lang/String;` | public instance, flags `1`; actual query copied by `Lavds.f` after parsing |
| `Lavdv;->S:Lcqgm;` | private transient instance, flags `130`; most recent native response for this model |
| `Lavdv;->ag:Lawfr;` | private instance, flags `2`; cached native map layer |
| `Lavdv;->hiroStrictSearchResponse:Ljava/lang/Object;` | added by patch; private transient synthetic, flags `4226` (`0x1082`), no explicit initial value |

The new field must be absent in an original native model. Adding it does not expose a user setting or change the query string; it is model-local bookkeeping excluded from default serialization.

### Actual query timing

`Lavds.f` reads `Lcqgi.e` at `0x0055`, then writes `Lavdv.g` at `0x005b`. It writes the same query to inherited `Lonf.b` at `0x006a`. Both occur after `Lavdv.aj`. Thus the f hook takes its query from the request proto, not from potentially stale model text.

### Response identity timing

`Lavdv.aj(Lcqgm;Application;I;Lawcl;String;I)V` begins by copying `this` to `v0` and response `p1` to `v1`. After acquiring the model write scope, `0x000e IPUT_OBJECT v1,v0,Lavdv.S:Lcqgm;` stores that exact response. No cloning occurs at this assignment.

`S` is transient. `O` explicitly handles a null incoming `S` at `0x0065`–`0x007e`, logging `SearchResult#merge called with deserialized searchResult.` and substituting the default response. A transient success marker is likewise null after restoration, so restoration alone cannot falsely authorize cleanup.

## Final patch behavior

### Request parsing: five inserted instructions

At original `Lavds.f` index **10**, load `cqgi.e` into dead register `v1`, load `request.g` into dead register `v2`, then call:

```text
SearchFilter.filterResponse(response=v10, actualQuery=v1, model=v2):Object
```

Write its return to `v10` and cast to `Lcqgm`. There are five inserted instructions total; registers remain **13**. The model parameter allows successful filtering to clear stale `ag` before the host parses the filtered response.

On a successful strict response, the helper stores the exact output response in `hiroStrictSearchResponse` and clears `ag`. The helper uses native `Lonf.e():Lone` around the model writes and closes the returned scope. Disabled/failed filtering invalidates the marker; stale success metadata must not authorize a later merge. Protobuf filtering still uses cloned immutable responses and returns the original response on unsupported schema/errors.

### Accepted merging: one inserted instruction

Target `Lavdv.O(Lavdv;Application;ZZ)V` at original instruction index **296**, code offset **0x025d**:

```smali
# Original index 294, 0x0259
iget-object v11, v10, Lavdv;->D:Lawfr;
# Original index 295, 0x025b
iput-object v11, v9, Lavdv;->D:Lawfr;

# Inserted at original index 296
invoke-static {v9, v10}, Lapp/hiro/mapsstrict/SearchFilter;->reconcileModel(Ljava/lang/Object;Ljava/lang/Object;)V

# Original index 296, 0x025d
iget-object v10, v10, Lavdv;->E:Lawfr;
# Original index 297, 0x025f
iput-object v10, v9, Lavdv;->E:Lawfr;
```

At the inserted call, **v9 = receiver** and **v10 = incoming**. The receiver's native write scope and the incoming model's native read scope are already held. The helper must mutate only the receiver and must not acquire a new model lock from this call site.

The helper checks:

```text
incoming.marker != null && incoming.marker == incoming.S
```

On a valid identity, it clears receiver `ag` and sets receiver marker to receiver `S`. Otherwise it clears receiver marker and leaves `ag` untouched.

For a freshly filtered incoming model, the marker records exact successful filtering. After propagation through a merge, it records that the receiver's native layer was cleared while its current `S` snapshot remained current. This is a **native-layer state marker**; it must not be repurposed as proof that every field in the aggregate receiver's older `S` was newly filtered. Native `O` does not replace receiver `S` with incoming `S`.

A subsequent native `aj` parse replaces `S`; old marker identity then fails. This avoids using a stale boolean after direct parses, retries, or state restoration. A global current query is unnecessary.

## Why index 296 is the accepted completion boundary

- The native query compatibility guard precedes the body. Rejected merges close scopes and jump to common cleanup, bypassing index 296.
- All normally completed accepted merges reach index 296.
- Index 296 is not a branch target or an exception handler entry.
- At this point all native `ag` changes have completed, and no subsequent native instruction changes `ag` or `S`.
- The next native instruction destroys incoming model register `v10` by loading `E:Lawfr` into it. Using the common `RETURN_VOID` at original index **302** / offset **0x026a** would therefore pass the wrong object on accepted merges.
- That same return also serves rejected merges, whose `v9` has been repurposed by logging. A return-site hook would have neither the desired control-flow scope nor stable model registers.
- The insertion is inside native try range starting at `0x015a` and ending before `0x0261`, whose handler closes the acquired scopes. The helper handles expected reflection/schema failures internally so they do not escape into host behavior.

No register expansion, extra branch, or native return rewriting is required. Original method size is **324 instructions / 655 code units**; insertion adds one three-code-unit invocation, yielding **325 instructions / 658 code units**.

## Model scope contract

| Item | Required shape |
|---|---|
| `Lonf;` | flags `1025` (public abstract), superclass `Ljava/lang/Object;`, interfaces `[Ljava/io/Serializable;]` |
| `Lonf;->e()Lone;` | flags `17` (public final), registers `2`, instructions `9`, code units `18` |
| `Lone;` | flags `1537` (public interface abstract), superclass `Ljava/lang/Object;`, interfaces `[Ljava/lang/AutoCloseable;]` |
| `Lone;->close()V` | flags `1025` (public abstract), no parameters, no implementation |

`Lonf.e` gets its `Loyi` scope holder, reads its `ReentrantReadWriteLock`, calls `writeLock()` at `0x0008`, locks it at `0x000c`, and returns `Loyi.c` at `0x000f`/`0x0011`. `Lonf.d` analogously acquires a read lock.

The f helper is called before native `aj` acquires its write scope, so it acquires/closes its own write scope for model housekeeping. The O helper executes inside already acquired native scopes and does not reenter lock acquisition or attempt to upgrade the incoming read lock.

## Frozen native body fingerprints

These values use the same debug-normalized DexPool SHA-256 routine as `MapsStrictSearchPatch.fingerprint`. They were captured from both pinned APKs with both SDKs, in Original and MutableMethod forms.

| Method | Flags | Registers | Instructions | morphe.jar SHA-256 | morphe-2.2.jar SHA-256 |
|---|---:|---:|---:|---|---|
| `Lavdv.O(Lavdv;Application;ZZ)V` | 17 | 14 | 324 | `06098f15efc934e2863aba7b3c8a24ec2acc20ba1ccc289ffe98703a96c9b634` | `9f148b322cacee946f81aaf1fb91121c482b1e09aac1fabfe2c1db947153d868` |
| `Lavdv.aj(Lcqgm;Application;I;Lawcl;String;I)V` | 17 | 25 | 707 | `61357b3d3a3feb6e0fff81da3a11156161b8d8a7f13e4a3c16464dd8c4429d5f` | `7d145d10f4c8aec0a7b7164ddfaa57b302e684ceeed8e518afcbf5be9f8e2d79` |
| `Lonf.e()Lone;` | 17 | 2 | 9 | `034986e3efa2649553c29fb62545d5080a72543a668c7c3e2f91f26c281fffa2` | `034986e3efa2649553c29fb62545d5080a72543a668c7c3e2f91f26c281fffa2` |

`aj` has **1399 code units**. The scope getter serializes identically in both SDKs; O/aj produce the two known SDK-specific values.

Input hashes are fixed:

- v2.8.5: `7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063`
- v2.8.7: `238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71`

The fixture generators reject any other input APK hash. These fixed native allowlists must not be regenerated from a future or unknown APK during build or patch execution.

## Evidence files

- `AG_LIFECYCLE.md`: original stale-layer investigation and exact reuse paths.
- `reuse-dispatchers.txt`, `reuse-factories.txt`, `reuse-state.txt`, `reuse-submit-xrefs.txt`: native lifecycle dumps.
- `model-scope-classes.txt`: full Lonf/Lone class inspection.
- `GenerateMergeContractFingerprints.java`: pinned merge/parser capture.
- `{morphe.jar,morphe-2.2.jar}.{v285,v287}.merge-fingerprints.txt`: class/field shape, original/mutable hashes, and index 296 anchor proof.
- `GenerateModelScopeContractFingerprints.java`: pinned scope capture, using the same fingerprint helper.
- `{morphe.jar,morphe-2.2.jar}.{v285,v287}.scope-fingerprints.txt`: scope shape and original/mutable hashes.

The native bodies, relevant fields, and selected anchor agree across both input APKs. Patch-application audits and runtime fixture tests are separate release gates owned by the build/runtime workstreams.
