# Exact composer request journal and guarded replay

This checkpoint adds an **original implementation**, not copied SDK source. It preserves the exact ordered composer API requests that a future verified hook layer observes. It does not make the existing v164 export replayable, enable a new camera path, or claim native/visual/device equivalence.

The native request observation backend and a private disabled hook transform now exist. They are **not installed**. All-state coverage and native queue barrier providers remain **unimplemented**. `ReplayPlan.UNAVAILABLE` rejects authorization. The concrete `RecordInvokerCommands` adapter is deliberately not a `ReplayTarget`: ordinary setter access alone cannot prove native setup completion.

## What was found in the actual supplied files

| Export | API events | Nonzero returns | Set/reset event | Composer resource path |
| --- | ---: | ---: | --- | --- |
| Natural blush | 165 | Four updates return `-105` | None | Unobserved (`null`) |
| Purity2 | 102 | None | None | Unobserved (`null`) |

Both record mode `(1,0)` and report `ordered_api_model_complete=false`. Their histories start with mode/append. The shadow node lists have 187/132 entries and the export retains only two filtered latest updates for each style. Those lists are not a native resolved graph or complete parameter set. V164 also collapses single-update and one-element batch-update requests into one opcode, does not capture native initialization, and exports active roots rather than all historical resources needed to replay a whole transcript. `V164ReplayAudit` rejects even a legacy manifest whose approximate graph-complete flag has been changed to true.

The checked stock SDK routes tagged operations through public `RecordInvoker.setVEEffectParams` with separate path/tag arrays. The adapter preserves this route and verifies the public mutable static opcode constants against pinned DEX values. It does not substitute a plain `setComposerNodes` call.

## Callable contracts

- `ComposerJournal` is tied to one exact SDK object and one observed native initialization. Each `begin(command)` must precede native side effects; the corresponding `result(ticket, status)` must run after the real return. `threw(ticket)` records exceptional completion. State is invalidated permanently on failure, uncertain/unrecorded mutation, overlap, dropped/bounded data, unmatched completion or teardown. No successful-prefix reconstruction, default insertion, event eviction, or last-node graph flattening is attempted.
- `ComposerCommand` owns arrays and exact float bits and retains all 13 method variants, including single/batch update, tags, mode/resource, remove/reload/replace order, and raw inline parameter literals. Unknown state-changing calls require `unrecordedMutation` and block replay.
- `Snapshot` binds the immutable successful transcript, initialization fingerprint, selected style, source object/handler, shot object/epoch and observer revision. Canonical bytes preserve all requests; source object identity is checked separately rather than serialized as an unstable hash.
- `ReplayPlan.authorize(snapshot, preconditions)` checks the current source and an external, audited proof of full mutation coverage, exact initialization, native queue completion, exclusive source ownership, same-shot settings, and content identities. No production implementation of that proof exists here. It cannot be replaced by `ShotStyleSettings.requestedGraphComplete`, API return zero, or a caller setting a completeness boolean.
- `execute(ownedTarget, mapping)` preflights every resource reference including removed/reloaded historical paths, verifies a fresh separate target initialized with the same audited configuration fingerprint, replays operations in order, and requires a matching target/shot/epoch/nonce/settings/mapped-transcript native barrier receipt. A stale source, SDK error, bad receipt or exception discards/quarantines the partially changed owned target. The original preview object cannot be the target.
- `ExactResourceMap` owns an exact map and preserves `:key:float` literals. It proves only string relocation; the caller's asset installer and preconditions must establish content hashes and owned resource lifetime. It does not search directories, guess prefixes, or treat selected-style-only assets as the entire graph.

A native initialization return and handle alone do not prove its asynchronous initialization callback succeeded. The required preconditions must verify that callback and native queue completion as well. An analysis recorder with intentionally different dimensions/configuration would need a separately proved configuration compatibility profile; this checkpoint does not guess one.

## Remaining live requirements

`HOOK_REQUIREMENTS.json` lists the observer surface and missing proofs. In addition to the named composer calls, generic effect-parameter paths, effect messages, external textures, gesture/tracking/animation state and direct lower-layer writers must be observed or reject the request. Observing a subset of `VERecorder` wrappers cannot establish all-state coverage. Recorder pause/restore ownership and hook coverage belong to the separate admission implementation, whose existence does not by itself supply a composer barrier.

A nonce-bearing message from one instrumented feature proves that feature reached its callback. It does not by itself prove all ordered graph nodes completed setup or establish the source's earlier queue state. Replay remains unavailable until those facts can be bound to the shot.

## Verification

From the workspace root, with the caller-owned inputs and existing tools:

```sh
PYTHONPATH=ulike_work/models168/python_deps python3 ulike_work/hdr_rebuild167/core/android_composer_replay/verify.py
```

`QA.json` binds all production/test sources and the exact stock DEX files. It records adversarial host protocol/dispatch tests, checks both actual export histories, validates 15 public SDK methods and ten public fields/constants, verifies four actual tagged dispatch routes, and compiles production sources against SDK36 and D8 API26. Test-only preconditions and barrier receipts are explicitly synthetic. No actual Android replay, hardware test or camera enable occurs.

Neither caller-owned styles, model files, SDK code, nor archive private paths are copied into this module's QA output.

## Native request observation checkpoint 171

`NativeComposerBoundary` now receives original method entry, real return and
exception events. It binds an observed terminal initialization request to the
exact receiver and native handle; the input fingerprint includes all ten native
initialization wrapper arguments. Commands are owned before native execution.
Handle changes during a call or before a snapshot, overlap, malformed arguments,
reinitialization, failed native mutations and teardown permanently invalidate the
observed transcript. Native-init notification and queue completion are still
separate, unproved events; the initialization fingerprint is a **request**
fingerprint, not proof of identical SDK/environment state.

`ObservedComposerArguments` preserves each supported generic tagged variant and
checks all 19 public instance fields of the pinned `VEEffectParams`. Other fields
must match the independently checked constructor state (zero primitives, empty
strings and empty lists). Nondefault generic fields, unsupported opcodes and
counts that differ from full array lengths are rejected, rather than omitted or
replayed with guessed values. Concurrent caller mutation and native-internal
state are outside the current observation proof.

`NativeComposerHooks` is compiled with a private `ENABLED=false`, without an
enable setter or installer. Its entry points are used only by the separate
`PrepareComposerObservation171` private delta. That delta covers ten composer
wrappers, both terminal init profiles (the scan profile is rejected), and native
teardown. It reconstructs all 13 original method hashes after stripping the
hooks, including 26 normal exits and 15 exception cleanup regions. All direct
original DEX calls to the ten native composer writers originate in those ten
wrappers. This bounded static finding is not reflection/native-internal/all-state
coverage. The delta overlaps three methods of the older lifetime-only delta;
those are alternative private transforms, not two deltas that may be blindly
merged into a patch bundle.

The actual app native-init listener schedules more work through an app task
scheduler, `Handler.post` and `Handler.postDelayed`. Neither returning from that
listener, setter success, nor posting another UI task proves the original style
was restored on the native render queue. `ReplayPlan.UNAVAILABLE` remains the
production default, no `PlanProvider` is supplied, and no complete restoration
receipt is synthesized. Live integration still requires a verified, shot-bound
native setup/render acknowledgement and full relevant mutation coverage.
