# microG Maps strict-search hook contract

Scope: the supplied microG Maps **26.36.04.973607363** APKs (グーグルマップ v2.8.5 and v2.8.7), version code **1068763346**. This is a static DEX inspection and patch-contract report, not Android execution proof. The investigation does not modify the inspected APKs.

The final patch has three narrowly scoped native changes: **five instructions** in the request-owned response parser, **one instruction** in the accepted result-merge path, and **one private transient synthetic Object field** on the result model. Runtime code is under `Lapp/hiro/mapsstrict/`.

## 1. Per-request response hook

- Class: `Lavds;` (SearchRequest), superclass `Lawcc;`, flags `1`.
- Method: `public final f(Lcqgm;Landroid/app/Application;Lawcl;)V`.
- Access flags: `17`, register count **13**, original instruction count **88**; after insertion **93** instructions.
- Parameters: `p0 = v9` request instance; `p1 = v10` response proto; `p2 = v11` Application; `p3 = v12` services.
- Insert at original instruction index **10**, immediately after `CHECK_CAST v0, Lcqgi;` at code-unit offset `0x13`, and before `IGET_OBJECT v1, v9, Lavds;->b:Lonh;` at `0x15`.

At that point `v0` is this request's parsed `Lcqgi;` request proto, `v10` is its response, and `v1/v2` are dead until their next original assignments. The final insertion is:

```smali
iget-object v1, v0, Lcqgi;->e:Ljava/lang/String;
iget-object v2, v9, Lavds;->g:Lavdv;
invoke-static {v10, v1, v2}, Lapp/hiro/mapsstrict/SearchFilter;->filterResponse(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;
move-result-object v10
check-cast v10, Lcqgm;
```

Arguments are **response, actual query, this request's result model**. Keep `v0` unchanged and reuse the existing registers; the original code later reloads `v1` and `v2`. The runtime must preserve null response input and return the original response on disabled queries or unsupported schema/errors. The return must remain an instance of the native response type whenever nonnull.

On successful strict filtering, the runtime clears the request model's old native layer and records the exact successfully filtered response in the model marker described below. Model writes in this f helper use the native `Lonf.e():Lone` write scope and close it before the host parser runs; the O helper executes under native scopes already held. Disabled/failed filtering must invalidate that marker and must not clear the native layer on the strength of a stale success state.

No original branch targets the insertion span. Existing try/catch regions start later (the nearest start is `0x5b`), so the insertion does not move execution into a native catch block. The runtime handles its own expected reflection/schema failures.

### Native instruction anchors

The first ten original instructions are:

```text
00 IGET_OBJECT v0, v9 Lavds;->l:Lawfr;
01 INVOKE_VIRTUAL {v0} Ljava/lang/Object;->getClass()Ljava/lang/Class;
02 SGET_OBJECT v1 Lcqgi;->a:Lcqgi;
03 CONST_CLASS v1 Lcqgi;
04 INVOKE_STATIC {v1} Lcmzs;->getParserForClass(Ljava/lang/Class;)Lcnbe;
05 MOVE_RESULT_OBJECT v1
06 SGET_OBJECT v2 Lcqgi;->a:Lcqgi;
07 INVOKE_VIRTUAL {v0,v1,v2} Lawfr;->d(Lcnbe;Lcom/google/protobuf/MessageLite;)Lcom/google/protobuf/MessageLite;
08 MOVE_RESULT_OBJECT v0
09 CHECK_CAST v0 Lcqgi;
```

Later, original offset `0x0040` copies `v10` to `v3`, and `0x0043` invokes:

```text
INVOKE_VIRTUAL_RANGE {v2 .. v8} Lavdv;->aj(Lcqgm;Landroid/app/Application;ILawcl;Ljava/lang/String;I)V
```

Replacing `v10` therefore changes the response before this method's native map/list parsing and notification work.

## 2. Result model marker and accepted-merge hook

Add exactly this field to `Lavdv;`:

```smali
.field private transient synthetic hiroStrictSearchResponse:Ljava/lang/Object;
```

The access flags are **4226** (`0x1082`: private, transient, synthetic), with no explicit initial value. The field must be absent in the unpatched APK; an existing field is a duplicate/unknown-state rejection. It stores response identity for this model, never a global current query. Transience ensures restored objects cannot retain a success marker from a prior process.

Native `Lavdv;->S:Lcqgm;` (private transient, flags **130**) holds the exact response supplied to `aj`: original `aj` offset `0x000e` writes its response parameter to `S`. Thus, after successful filtering and native parsing, `model.S == model.hiroStrictSearchResponse` proves that the current parsed response is the recorded successful response. Merely classifying `model.g` as a strict query is insufficient because an earlier attempt may have failed open.

The merge hook is:

- Method: `Lavdv;->O(Lavdv;Landroid/app/Application;ZZ)V`.
- Flags **17**, registers **14**, original instruction count **324**; after insertion **325**.
- Insert one instruction at original index **296**, original code-unit offset **0x025d**, immediately before `IGET_OBJECT v10, v10, Lavdv;->E:Lawfr;`.
- At this point `v9` is the receiver model and `v10` is the incoming model.

```smali
invoke-static {v9, v10}, Lapp/hiro/mapsstrict/SearchFilter;->reconcileModel(Ljava/lang/Object;Ljava/lang/Object;)V
```

Only accepted merges reach this point. Both native model scopes are still held; no branch or handler targets the insertion point. The incoming model is still live in `v10`, which the next original instruction overwrites with its `E` field. The remaining native work writes that bookkeeping field and closes scopes; it does not rewrite `ag` or `S`.

Reconciliation requires a nonnull incoming marker identical to incoming `S`. On success it clears receiver `ag` and propagates the native-layer-cleared state using receiver `S` as receiver marker. On a failed identity check it invalidates the receiver marker without clearing `ag`. See **MERGE_CONTRACT.md** for identity semantics, native lifecycle evidence, and exact method fingerprints.

Do **not** put this call at the common `RETURN_VOID` (`0x026a`): rejected merges also reach that return, and accepted merges have already overwritten the incoming-model register.

## 3. Query identity and schema guards

- `Lcqgi;` is a final protobuf class extending `Lcmzs;`, implementing `Lcnaz;`, flags `17`.
- `Lcqgi;->e:Ljava/lang/String;` is a public instance field, flags `1`, initialized to the empty string.
- `Lcqgi;->a:Lcqgi;` is its public static final default instance, flags `25`.
- `Lavds;->l:Lawfr;` is the private request-proto wrapper, flags `2`.
- `Lavds;->g:Lavdv;` is the request-owned result model, flags `1`.
- `Lavds;->c()Lcqgi;` is public final, flags `17`, registers **3**, instructions **10**. It parses `this.l` using the `Lcqgi` parser.
- `Lavds;->sk()Lbwol;` records `c().e` under literal **params.getQuery**, identifying it as the actual query.
- `Lauwt;->T(String,int,Lcimq,Lonh,Lcqgh)` assigns its String parameter to `Lcqgi.e` and sets request bit `b |= 1`.

Do not substitute `Lavds.d()`: that method can produce an empty string when `Lonh.h` is set or use display override `Lonh.j`.

`Lavds.f` copies `cqgi.e` into `Lavdv.g` at original offset `0x005b` and into inherited `Lonf.b` at `0x006a`, **after** `aj` parses the response. Therefore `Lavdv.g` can be unset/stale at parser entry. The response hook uses the request's `cqgi.e`; the merge hook uses model-local response identity, so neither depends on mutable global search text.

## 4. Request/response routes and race isolation

The observed response routes into `Lavds.f` are:

1. `Lavew.f(Lavds;Lcqgm;)V`: online dispatcher calls the attached request's parser.
2. `Lasdk.a(Lcqgm;Status$Code;Z)V`: callback uses its own `Lasdk.b:Lavds`.
3. The `Lavds` constructor accepting cached `Lcqgm`: constructs a fresh model before calling `f`.

Late responses remain paired with their own query and model. Repeated/paginated responses through `f` are also paired correctly. A model is not guaranteed to be fresh for every response: `Lauxv.d()` resubmits a saved request without `N()`, and enroute streaming can retain the outstanding request across several responses. Native `aj` does not clear existing `ag` when the incoming native-layer field is absent; native merge `O` also preserves destination `ag` when incoming `ag` is null. These are the reasons for the model argument and accepted-merge hook.

The other observed `Lavdv.aj` caller is `Laxfl.am` (`SearchSuggestFragment.onSuggestionClick`) assembling a directly selected place. It bypasses `Lavds.f`, preserving direct selection behavior. A subsequent direct parse replaces `S`, so an old response-identity marker cannot authorize clearing on the basis of an unrelated new response.

## 5. Category metadata limits

No universal typed-query-is-category flag was established. A multi-result/categorical-results screen can also show several branches of one brand, so it is not a reliable category discriminator.

Request fields `w:Lcimq` and `y:Lcqgh` hold context. The code identifies `b & 0x40000000` / `B:Lcisa` as a **SearchAlongRouteContext**, not a generic category flag. Request `i:I` is passed into the result parser; it must not be treated as a category discriminator.

The runtime matcher conservatively preserves generic category vocabulary such as 靴屋 and 喫煙所, addresses/routes, and unsupported query interpretations. Category preservation belongs to query classification; it must not be inferred from the number of places returned.

## 6. Frozen native fingerprints

Fingerprints below use the exact `MapsStrictSearchPatch.fingerprint` algorithm: debug-normalized method wrapped in an immutable DEX class, DexPool serialization, SHA-256. Values differ between the two pinned SDK serializers. Both Original and MutableMethod forms, from both pinned APKs, produced the matching value for each SDK.

| Native method | morphe.jar | morphe-2.2.jar |
|---|---|---|
| `Lavds.f` | `83fa0214fb0bd0ecfb88c625171083568f69328c4b66985974c26b25cff316a3` | `fee07e85b6baf8b250cd67a24a13e3337416226820972ae20ca5d78132f8ab42` |
| `Lavds.c` | `aaa4ec60e19ee506aa22f5e235ad521b77c40f4d7c92429d455acbf1eac23c33` | `e568b0644664b75c893066607091c61bdfcd905a10b55eca8ba44a5e18bace9d` |
| `Lavdv.O` | `06098f15efc934e2863aba7b3c8a24ec2acc20ba1ccc289ffe98703a96c9b634` | `9f148b322cacee946f81aaf1fb91121c482b1e09aac1fabfe2c1db947153d868` |
| `Lavdv.aj` | `61357b3d3a3feb6e0fff81da3a11156161b8d8a7f13e4a3c16464dd8c4429d5f` | `7d145d10f4c8aec0a7b7164ddfaa57b302e684ceeed8e518afcbf5be9f8e2d79` |
| `Lonf.e` | `034986e3efa2649553c29fb62545d5080a72543a668c7c3e2f91f26c281fffa2` | `034986e3efa2649553c29fb62545d5080a72543a668c7c3e2f91f26c281fffa2` |

Approved input APK SHA-256 values:

- v2.8.5: `7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063`
- v2.8.7: `238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71`

Never refresh the allowlist automatically from an arbitrary target APK. Patch-time schema/body mismatch must reject before native mutation; runtime schema/query failure returns original Maps results.

## 7. Inspection evidence

- `DexQuery.java`: DEX class/method/xref inspection.
- `v285-Lavds.txt` / `v287-Lavds.txt`: identical complete textual request-class dumps (SHA-256 `65cc559508826b86277c42c77a6df2f6c89b75e436b93004eb0e633da6bb757c`).
- `Lavds-f.txt` / `Lavds-c.txt`: original request hook and query decoder.
- `context-result-classes.txt`: model, callback, options dumps.
- `result-query-boundary-xrefs.txt`: observed `Lavds.f` and `Lavdv.aj` callers.
- `query-builders.txt` / `query-field-xrefs.txt`: query assignments and references.
- `AG_LIFECYCLE.md`: native reuse evidence behind the additional model handling.
- `MERGE_CONTRACT.md`: accepted merge location and model identity contract.
- `GenerateMergeContractFingerprints.java` and `{morphe.jar,morphe-2.2.jar}.{v285,v287}.merge-fingerprints.txt`: hash-pinned fixture capture of merge/parser bodies, native field shape, and exact insertion index.
