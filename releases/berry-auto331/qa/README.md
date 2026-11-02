# APK application regression tests

`payload/QA_apply331.json` records four successful full patch-and-rebuild runs: the combined and standalone bundles, each against the original Berry 3.83.74 base APK and a synthetic renamed copy. All five Berry patches were selected in each run. `VerifyPatchedHooks.java` independently checks 24 injected hook kinds in 19 application methods, resolves their method/field references, and checks the QueueInit provider and manifest entry.

The synthetic fixture changes all 8,068 root-package class descriptors, 90 selected fields, 18 selected methods and versionName to 9.99.99. It tests discovery without known class/member names. It is not a released Berry version and is not a runnable-device test. Berry 3.84.12 and physical-device behavior were not tested. No proprietary APK, extracted DEX, or full input mapping is distributed.

`baseline-rejection.json` binds the same synthetic input to the prior 1.0.330 bundle and records its expected unknown-WebView-structure rejection.

## Reproducing locally

Use a private working directory with these files in `qa/`, the original user-provided base APK in `inputs/Berry_3.83.74_original_base.apk`, and the bundles in a separate distribution directory. The original APK SHA-256 is `4a4da9029d1d71edf376b2d1edf3636e3fba946b49d1fafa5787d20252c8ad5b`.

Provide `tool-paths.json` in the working directory with `java`, `tools.classpath.jar.path`, and `tools.ecj.jar.path` pointing to Java 21, the Morphe Desktop 1.19.0-dev.19 executable classpath JAR (Patcher 2.2), and ECJ respectively. Compile `ReobfuscateFixture.java` for Java 11 with the host JAR on its classpath, into `qa/classes/`. Run its main class with the host JAR and `qa/classes/` on the classpath, passing three arguments: original APK path, `qa/Berry_synthetic_renamed_unsigned.apk`, and a private mapping JSON path. Then run:

```sh
python qa/run_validation.py --dist /path/to/distribution --out /path/to/new-private-output
```

The output directory must not exist. The runner compiles the hook auditor, invokes all five patches using FULL bytecode rebuild mode, checks every patch/build result, and audits every output APK. Outputs are private test fixtures, not release assets. For the baseline rejection check, also supply `inputs/Hiro_Morphe_Patches_v1.0.330.mpp` and run with `--baseline-only` instead of `--dist`.
