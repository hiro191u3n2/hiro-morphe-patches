# Published 1.1.14 Back-route failures

These three host probes deliberately assert the previous failures, using the pinned **published 124 runtime**, not the new 125 implementation. Re-running all three reproduced the retained outputs byte-for-byte. They are not tests to run against 125: the fixed behavior should violate their old-failure assertions.

`provenance.json` pins original/applied APKs, published patch, old Java sources, tool inputs and the compiled class manifest. `route-contracts.txt` records exact original/applied native method implementation digests. Raw method excerpts are trimmed to relevant methods without changing instruction text. `XBackAudit.java` and `X125RouteContracts.java` retain the extraction code.

From the workspace used to build the published baseline:

```sh
sh evidence/key-dispatch/run-baseline.sh   /workspace/scratch/102008d926fb/build124-release/runtime   /workspace/scratch/102008d926fb/build124-release/test   /workspace/scratch/102008d926fb/work/x125-route-audit/reproduced124
```

The required old classes can also be built from the pinned `homeback124` sources; the source/runtime manifest identifies the exact evidence build. The host fixture is `Back124TwitterTest.Scene`; runtime classes must precede host classes on the classpath.

For actual method fingerprints, compile `X125RouteContracts.java` with the pinned `build124-release/patch.jar` and `build-tools/morphe.jar`, then invoke it on the two APK paths listed in `provenance.json`. `XBackAudit` takes APK, method-name regular expression, and exact DEX class descriptors. Java class compilation uses JDK 21.0.8+9. No APK or SDK binary is included here.

The observer probe substitutes host `Activity.finish()` for Android's private default system callback. It proves no app callback/hook/marker is reached; it does not claim the device uses that exact terminal method. The API33 probe's two rows differ only by a lifecycle resume. Both retain the same decorated callback in 124 because the old observer was never registered.

The framework source was inspected at Android 16 `Activity.java`, blob `c129fde3f81916e3ed4e2e67b77bfeac2288a1dc`:
https://android.googlesource.com/platform/frameworks/base/+/android16-release/core/java/android/app/Activity.java

See `../native-architecture125.md` for the connection between these reproductions, actual APK contracts and the 125 correction.
