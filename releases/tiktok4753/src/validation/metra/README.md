# Real TikTok 47.5.3 / Metra audit

These are captured results from the original global TikTok 47.5.3 APK, not synthetic fixtures. The original APK is not redistributed here.

## Inputs

- APK: `com.zhiliaoapp.musically`, version 47.5.3 / code 2024705030, SHA-256 `6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e`.
- Metra v0.9.0-dev.1: SHA-256 `45dc0c2244f8cf6973de18542d2d9a0cc25512f5b1de52438c685b82edb7c4cd`.
- Hiro Japanese v1.0.0: SHA-256 `b3e226db0bf87c7f7c9ff2c2eaa5d03ee9f2080c1dc457fb8cc82fddd49ed572`.
- Morphe Desktop 1.19.0 / Patcher 2.0.0: SHA-256 `deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5`.
- JDK 21.0.8+9.

## What passed

`settings-ja-audit-summary.json` summarizes the Settings + Japanese companion application. Both patches applied and the full APK rebuilt. `SettingsHookAudit.java` resolves the actual OpenDebug state, compose row, click wrapper, top-tab binder and settings gear from the original APK, then checks the corresponding output DEX hooks. The original manifest and resource table remain byte-identical (`settings-ja-host-resources-audit.json`).

This shows the Japanese companion is connected to real 47.5.3 native settings entry points. It does not assert that Android screen interaction was tested. `device_runtime_verified` is false.

## What failed in the external upstream bundle

`metra-all-audit-summary.json` records an exploratory forced application of every one of Metra's 136 TikTok patches. Twelve patches failed and 124 were reported applied. This was deliberately run with `--continue-on-error` to collect failures. The output is an invalid partial result for release; its existence, CLI exit code 0 and top-level `success: true` must not be interpreted as full success. The report's PATCHING step is false and its failedPatches array is nonempty.

An Applied result alone does not prove UI functionality: the 47.4.3 runtime retains obfuscated view names and reflection targets that changed in 47.5.3. `upstream-migration-audit.md` lists primary Hushfeed migration commits for reference. Hushfeed was not silently substituted for Metra in these runs.

## Reproduce native settings inspection

With the pinned `morphe200.jar`, `patches-0.9.0-dev.1.mpp`, original APK and Settings+Japanese output APK available:

```sh
javac -cp morphe200.jar:patches-0.9.0-dev.1.mpp SettingsHookAudit.java
java -Xmx1500m -cp .:morphe200.jar:patches-0.9.0-dev.1.mpp SettingsHookAudit original-47.5.3.apk settings-japanese-output.apk
javac -cp morphe200.jar VerifyOriginal.java
java -cp .:morphe200.jar VerifyOriginal original-47.5.3.apk
```

The complete patch commands, selection lists and results are represented by `settings-ja-apply.log`, `settings-ja-patcher-result.json`, `metra-all-selected-patches.json`, `metra-all-apply.log` and `metra-all-patcher-result.json`.

## External fork build assessment

Moving all of Metra to 47.5.3 requires both patch-time native hooks and runtime mappings/catalogs to change; merely editing declared target versions would leave failures and silent no-ops. The reference Hushfeed migration touches 47 production files. A full fork should retain Metra branding, settings identity and notices and review each migration against the real APK, with separate validation for all selected patches.

Metra's normal Gradle build uses `app.morphe.patches` 1.3.4 from GitHub Packages and `app.morphe:morphe-patcher:2.0.0`. Anonymous access to the latter POM returned HTTP 401 during this audit. Kotlin compiler 2.4.21 and Gson 2.14.0 POMs were publicly reachable from Maven Central (HTTP 200). This is a build infrastructure dependency, not a reason to change version metadata or bypass any check. The already pinned public Morphe Desktop release includes the patcher classes and can support an explicitly assembled build pipeline, but that pipeline and resulting payload would need their own reproducibility and runtime validation.

No external upstream repair or replacement is included in this evidence directory.
