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
