# Native ag lifecycle: pinned Maps 26.36.04 / microG v2.8.5

Read-only follow-up. APK SHA256: `7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063`.

## Conclusion

**A universal “new Lavdv or N() before each Lavds.f” invariant is false.** Ordinary fresh request factories and the main search reuse path initialize/clear the model, but retry and streaming paths can call f again on an existing request without N(). Removing `cjme.c` from a response therefore prevents a new native layer being installed; it does not actively clear any native layer already in that model.

For an enabled strict query filtered successfully from its first response onward, new request models start with null ag and keep it null. The concrete residual case is a previously populated ag (e.g. an earlier fail-open response or restored request state) followed by a successful strict response on a reused request. Null ag also cannot clear an existing destination ag through Lavdv.O.

## Exact evidence

### Fresh request factories are safe initially

- `Lbjsg.D(Lcqgi;Lonh;)Lavds;`: offset `00000 NEW_INSTANCE v0 Lavds`, then constructor at `00033`.
- `Lbjsg.E(Lcqgi;Lonh;Lxuu;Laila;)Lavds;`: same new-instance and constructor shape.
- `Lavds.<init>(ZLbeoq;Llvg;Lcqgi;Lonh;Lxuu;Laila;ImmutableList;)V`: `000c1 INVOKE_VIRTUAL {v7} Lbeoq.an()Lavdv`, `000c5 IPUT_OBJECT ... Lavds.g`.
- `Lbeoq.an()Lavdv;`: `00002 NEW_INSTANCE v0 Lavdv`, constructor at `0000d`, return at `00010`.
- Cached-response `Lavds` constructor delegates to that constructor (`00010`) before calling `f` (`00016`), so its response is parsed into a fresh model.

### Main search reuse explicitly clears

`Lauwt.am(Lcqgi;Lonh;Lavdr;I)V`: `000d5 IGET_OBJECT ... Lavds.g`; `000d7 INVOKE_VIRTUAL ... Lavdv.N()`; `000dc INVOKE_VIRTUAL ... Lavew.e(Lavds)`.

`Lavdv.N()V`: `00042 CONST_4 v1,0`; `00056 IPUT_OBJECT v1,v4 Lavdv.ag:Lawfr`.

### Definite request reuse without clear

`Lauxv.d()V`:
- `00003` gets existing result via `i()` and checks `Lavdv.ab()` (ab returns field N).
- `0000d INVOKE_DIRECT {v1} Lauxv.h()Lavds`.
- `00011 IPUT_OBJECT v1,v0 Lavds.h:Lavdr`.
- `00015 INVOKE_VIRTUAL {v1,v0} Lavew.e(Lavds)`.

`Lauxv.h()` retrieves `Lawvj.a():Serializable`, casts it to existing `Lavds`, and returns it. It allocates nothing and calls no N().

`Lavew.e(Lavds)` only clears/replaces dispatcher request references (`i()` / `h()`, then `k=request` at `0000b`) and starts transport; it does **not** clear `request.g`.

`Lavew.c(...)` later reads that same `k`, removes it from dispatcher state, and calls `d(...,request)`. `Lavew.d` calls `Lavew.f`, which calls `Lavds.f` at `00022`; none calls N(). Thus request submission is not itself a reset boundary.

### Streaming can apply multiple responses to same request

`Lavew.b(Lcqgm;Status.Code;ZZZ)V`:
- `00001 IGET_OBJECT v0,v1 Lavew.k:Lavds`.
- `00014 IF_NEZ v5 -> 0001c` bypasses `i()/h()` cancellation for an ongoing response.
- `00021 INVOKE_VIRTUAL {v1,v0,v2} Lavew.f(Lavds;Lcqgm)`.

There is no N() on this path. This appears to be the enroute transport (its diagnostic says "Received enroute response ..."). Do not generalize it into normal search behavior, but it disproves a universal per-response reset.

### Alternate search/pagination makes fresh request, aggregate state can persist

`Lasdl.e(Z)` creates a new request via `Lbjsg.D` at `00104`, assigns `l` at `00108`, creates `Lasdk(this,l)` at `00118`. `Lasdk.a` calls f on that new request. Query change `Lasdl.h(String)` calls `Lasdl.f()` at `0000c`; `Lasdl.f` clears aggregate `b` via N() and obtains a new request.

`Lasdl.b(Lavds)` merges incoming g into aggregate b and c using `Lavdv.O` at `00033` and `0003e`.

`Lavdv.O(...)`: `001af IGET_OBJECT v11,v10 Lavdv.ag`; when null, `001b1/001b3` jumps to `0020f`, leaving the old destination ag unchanged. If nonnull, merges or installs it.

### Existing serialization permits an old model to be restored

`Lavds.readObject(ObjectInputStream)` reads a serialized Lavdv at `0000b`, casts at `0000f`, and assigns g at `00014`. This method does not call N(). Runtime restoration usage was not exhaustively established in this short follow-up.

## Scope

No source modifications. Exact dumps are in `reuse-dispatchers.txt`, `reuse-factories.txt`, `reuse-state.txt`, `reuse-submit-xrefs.txt`, and earlier `v285-Lavds.txt`. Found 12 methods that submit `Lavew.e`; the counterexamples above are sufficient to reject the universal reset assumption. The original f and request schema were already verified equal in v2.8.5 and v2.8.7; this follow-up directly inspected v2.8.5 only.
