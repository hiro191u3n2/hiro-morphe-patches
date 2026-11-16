# Reproduce Discord Send Button 1.0.0 / Hiro 1.0.375

The build uses Java 21.0.13 and pinned tool hashes in src/build.py. It never modifies the supplied APK.

```sh
python3 src/build.py --base /path/to/Hiro_Morphe_Patches_v1.0.374.mpp --tools /path/to/tools --sdk /path/to/morphe-dev19.jar --out /path/to/empty-output --timestamp 2026-11-16T06:50:22
```

Required tools: ecj.jar, d8.jar, classpath.jar, Morphe Desktop 2.2.0-dev.19 (Patcher 2.4.0-dev.8). Exact hashes and preservation evidence are in verification/QA_build375.json. The compile-only API stub is excluded from the MPP.

Use tests/README.md for the instruction-level semantic check. GuardTest.java runs against the produced MPP, using the actual supplied bundle and a fresh work directory. APK application checks use --exclusive -e '送信ボタンの表示消失を修正' --bytecode-mode=FULL --unsigned; output is for structural verification only. Manager performs final signing for installation.

No APK, decompiled application, credentials or signing key is included in this package.
