# TikTok 47.5.3 upstream migration evidence

## Pinned inputs

- Original APK: `TikTok_47.5.3_original.apk` (original input, not redistributed), 476733983 bytes, SHA-256 `6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e`.
- Package `com.zhiliaoapp.musically`, versionName `47.5.3`, versionCode `2024705030`, arm64-v8a + armeabi-v7a, minSdk 23, targetSdk 36.
- Download page: https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok-including-musical-ly/tiktok-47-5-3-release/tiktok-47-5-3-2-android-apk-download/
- Google apksig verified APK signatures v1/v2/v3 with no errors (API 23 through 37). Signer certificate SHA-256 `9041803e91bcb814b4b4399fb5c85a91640b755e5e8ba76813814bf4cf2ab5ba` agrees with APKMirror. See `original-identity.json` and `original-signature-verification.txt`.
- Hushfeed v0.78.0 commit `3b6254c304b52136633606037f4cda74c2371a88`, checked out in the audit workspace at that exact commit (not bundled here).
- Hushfeed MPP: `patches-0.78.0.mpp`, 6644966 bytes, SHA-256 `d87ec98c85d5870c0604bf972372436d7ff20d282c1f8ad6c46614e6e9046417`.
- Hushfeed source archive SHA-256 `7c9aaf3b70ee3323a8e35309470bd0a9c78b24e94c41cfd4b9e18b894d0a7b67`.
- Primary release: https://github.com/SysAdminDoc/HushFeed/releases/tag/v0.78.0
- Migration comparison: https://github.com/SysAdminDoc/HushFeed/compare/68a467957e3cc163244f8304ed6078ccef50611e...3b6254c304b52136633606037f4cda74c2371a88

## Actual code changes required by upstream

The 47.5.3 update spans 32 commits after the previous target. It is not just the compatibility list. Forty-seven production files changed during this range, including generated feature gate catalogs.

| Area | Evidence commit | Change |
| --- | --- | --- |
| Native UI ids | `da834cf78a37b5bdc23ea3e421132c2de63a7ea0` | Feed controls, tabs, caption, comments, inbox and share-sheet view names move. Examples: Home `onh`→`op1`, Inbox `oni`→`op2`, Profile `onj`→`op3`, action rail `ljn`→`lke`, anchor row `bqr`→`bqj`. |
| Feed text | `a219ab0324fb011c3d6525b971b12e07e04b1379`, `897f35972aef9f9fb4ce3fee30f1abec5c3f851f` | Resolve the renamed text controller and revised no-argument binding route; update resource ids. |
| Feed mute | `60ef143da2f364031578922fb54ef651c1817185` | Find the session give-up branch from the checkStart block. |
| LIVE feed | `6b166eddb06729947c1364adb7b6a9d50798da45` | Feed page bridge acquires a fifth boolean flag. |
| Comment menu | `1342cfeb1f13500ae389336bcf0d00c476aeb8a4` | Resolve menu items via Report row structure. |
| Bio length | `c0edbf01976a70a90ffb1de1ad28cc30648f2afc` | Fourth limit now exists in cursor clamp. |
| Search suggestions | `a819f08539739119d669ec0cb5859e9f1951e247` | Resolve changed re-entry request. |
| Inbox native hooks/reflection | `2279d7b474f8da4fc76862f60645aadf36eeb656`, `1c3bb8c625ea20b154f9d77f01e2bcbb7123efb3` | PowerList and owner classes/fields moved; rely on log lines and types, update reviewed field/schema mappings. |
| DM translation | `24623269fd1c58b1d53af08a97818ed488bfeca0` | Native class names, service methods and icon ids move; both outgoing and received previews updated. |
| Profile badge | `d4e8922b5b30f610e8a1dcacc77ddba2c745c839` | Find decision by isVerified comparison instead of old method name. |
| Themes | `9d79346b22ef3bd331a26a79aa47c10ac6f26334` | Accent and AMOLED resource tokens move. |
| Feature Gate Lab | `fa70f9ae54c419f14b790ea51a889325bb027f4b` | Regenerate native catalogs from 47.5.3. |
| Resource optimizer | `b8942bec8a617d4c924e9c831f419f6a4e946daa` | Review asset hashes and split-bundle absence profiles. |

## Scope boundary

Hushfeed is a reference implementation for native target changes, not an authorized silent replacement for the user's icysymmetra Metra source. Hushfeed and Metra use overlapping extension classes and should not both be selected blindly. Hiro's existing Japanese companion targets the Metra 0.9.0-dev.1 localization entry point. Its exact 47.5.3 APK application and native settings-entry audit are recorded beside this file as `settings-ja-*.json`, `settings-ja-*.txt` and `settings-ja-apply.log`.

The upstream release receipt records upstream tests; it does not constitute testing of a modified Hiro bundle. Local signature verification proves APK identity, not app feature behavior. Device/runtime testing must be stated separately from patch application and emitted DEX inspection.
