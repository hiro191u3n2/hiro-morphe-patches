# Partial recorder lifetime work: not installed

This is a development checkpoint for the exact original ULike v5.6.2 (740) APK.
It does not activate `RecorderAdmission`, install lifecycle deltas into either
MPP, change `CandidateGate169`, or assert that the original preview can safely be
paused and restored on a phone.

`Inventory169.java` scans every original DEX class and writes original method
identifiers, reference offsets and method hashes. `verify_inventory.py` pins the
original APK and checks two native allocations, three native-handle writes,
one native teardown, eight constructors with six delegating to two terminals,
and the actual asynchronous app-native-init dispatch points. `INVENTORY.json`
lists the remaining exact hook families and why native init is not a composer
restoration barrier. No vendor method body is published in these inventories.

`PrepareNativeLifetime169.java` creates a **separate private partial delta** for
three methods only: `RecordInvoker.initBeautyPlay(...,boolean,boolean,boolean)`,
`initBeautyPlayOnlyPreview` and `uninitBeautyPlay`. The referenced
`NativeLifetimeHooks` is compile-time disabled with no setter. Entry, all six
normal exits, and exceptional exit after original monitor cleanup are covered.
The transform preserves original registers and instructions; it catches only
the complement of existing catch-all regions, allowing original monitor cleanup
to run first. `VerifyNativeLifetime169.java` strips precisely the declared hooks
and reconstructs each exact original method hash after writing the result DEX.

The core `NativeLifetimeBoundary` uses per-thread nested calls and the app-wide
ledger. It does not infer release from `getHandler()==0`: the SDK sets that field
to zero *before* calling native teardown. The corresponding ledger now rejects
overlapping init/uninit on one invoker and double initialization of a live handle.

Run `validate.sh` with the local private prerequisites described by the parent
integration project. It generates only a private DEX and report. A phone/ART
verification, complete constructor/entry/worker hook coverage, current selected
composer replay and a real restoration/render barrier are still missing.

Only original `.java`, `.py`, `.sh`, this README and JSON reports belong to the
source checkpoint. Generated reference TSV files, `*.private.*`, `*.class`,
`private_build` and original decompiler output are local investigation material, not source
deliverables. No original APK or native library is included.

## Additional composer observation transform

`validate_composer.sh` builds a separate private
`composer-observation-partial.dex` using
`PrepareComposerObservation171.java`, then independently reconstructs all 13
original methods using `VerifyComposerObservation171.java`. Ten original native
composer writers have exactly one direct DEX caller each, all inside the hooked
wrappers. The verification covers the whole original multidex for those exact
writers; it does not cover reflection, internal native writers or other effect
APIs. The core `NativeComposerHooks` entry points are compile-time disabled, and
this delta is excluded from both MPPs.

The 13-method delta overlaps the three methods of the older lifetime-only delta.
They are alternatives and must not be blindly combined. Existing recorder
admission, constructor and worker-family coverage remains incomplete. Generic
composer argument defaults and full-array counts are checked conservatively;
unsupported requests invalidate the journal. Initialization arguments and native
handle observation do not establish asynchronous native initialization or final
render completion. Actual app initialization listeners enqueue more composer
work, so a normal listener return cannot close the restoration barrier.

`QA_COMPOSER_OBSERVATION.json` contains only authored-source hashes, method
identifiers, hashes and bounded findings. Private deltas, complete call TSVs,
original APKs and original method bodies remain excluded from source delivery.

`verify_restore_barrier.py` additionally pins the original native-init callback
method's raw code hash. Its only normal return follows an unconditional
`mIsRenderReady=true` store, including the negative-status branch. The Boolean
therefore cannot establish successful native initialization. The bounded
`RESTORATION_BARRIER_FINDINGS.json` also references the three already verified
app asynchronous restoration edges. Run with the existing caller-owned
`models168/python_deps`; it does not execute native code or device callbacks.
# Native initialization callback observer (checkpoint 182)

`PrepareNativeInitCallback182.java` produces a separate, disabled private method
delta for `RecordInvoker.onNativeCallback_Init(int)`. It observes the actual
callback entry before application listeners run and reconstructs the complete
original method when stripped. `validate_init_callback.sh` accepts the stock APK,
baseline MPP, tool directory and private build directory; only
`NATIVE_INIT_CALLBACK_EVIDENCE.json` is copied out as publishable evidence.

The core callback receipt correlates receiver, native handle, initialization
request, terminal return and one nonnegative callback status. It does not confirm
completion of listeners, queued style setup, rendering or preview restoration.
Because the callback carries no generation ID, reinitializing the same Java
receiver remains ineligible until a native teardown and callback drain can be
proved. Neither this delta nor the older composer/lifetime deltas are installed.
