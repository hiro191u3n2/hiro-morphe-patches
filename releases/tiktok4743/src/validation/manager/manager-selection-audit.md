# Morphe Manager 2.1.0-dev.2: selecting Metra Settings for TikTok 47.5.3

Audited immutable revision: `MorpheApp/morphe-manager@d8ceac809082ff23da0a0956f16ead11ac995ae8`.
This is a static audit of the Manager's real selection-to-patcher path, not an Android UI execution test.

## Conclusion

**Expert Mode permits manually selecting package-matching patches whose declared app versions differ.** With the Hiro source enabled and advertising TikTok 47.5.3, the APK admission check passes. Metra `Settings` remains visible and manually selectable even when its metadata advertises another app version. It is not discarded later.

## Verified Japanese UI instructions

1. Open **設定**. In the **パッチ処理** section, turn on **エキスパート モード**, then press **有効化** in its confirmation dialog.
2. Load/enable the new Hiro source and the tested Metra source, then select the original unpatched TikTok 47.5.3 APK.
3. In the patch-selection dialog, choose the Hiro source tab and enable the desired Hiro patches. For translated Metra settings, enable `TikTok設定を日本語化`.
4. In the Metra/TikTok source tab, manually enable `Settings`. A fresh default selection need not enable it because the metadata's app version differs.
5. If an unselected patch or its source tab seems absent, clear the search and turn off the selected-only filter by tapping the selection-count/filter badge; the full list can then be searched for `Settings`. Source muting is a separate possible cause.
6. Proceed with the reviewed selection. A multiple-source confirmation is expected when Hiro and Metra are selected together. The Japanese-only option is not a reason to enable every upstream patch.

The supported integration case is the tested `Settings` patch plus the Hiro selection. This selection-path finding does not establish compatibility of all Metra patches or physical-device behavior.

## Evidence by stage

All links below are fixed to the audited revision.

| Stage | Finding | Primary source |
|---|---|---|
| UI entry | Expert Mode is a switch in the main Settings page's Patching section. Its confirmation calls `setExpertMode(true)`. | [SettingsPage.kt, lines 115–153](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/ui/screen/settings/SettingsPage.kt#L115-L153) |
| Exact labels | Japanese resources define `設定`, `パッチ処理`, `エキスパート モード`, and `有効化`. | [Japanese strings](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/res/values-ja-rJP/strings.xml) |
| APK admission | `versionMismatch = !hasCompatible && hasIncompatible`, evaluated across enabled sources. Hiro provides compatible patches for 47.5.3, making the mismatch false. | [PatchFlow.kt, lines 812–888](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/ui/viewmodel/PatchFlow.kt#L812-L888) |
| Scope | `Scoped.patches` contains all package-matching patches. Version compatibility is tracked in separate lists. `patchSequence(true)` returns the complete scoped list. | [PatchBundleInfo.kt](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/patcher/patch/PatchBundleInfo.kt) |
| Listing | `listedWith` uses `bundle.patchSequence(true)`, not the default version-filtered sequence. | [PatchSelectionExtensions.kt, lines 61–80](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/util/PatchSelectionExtensions.kt#L61-L80) |
| Manual toggle | `toggle` obtains the patch from the complete scoped list and only checks installer/architecture lock rules. `runSelection` only omits muted sources. | [ExpertModeSelection.kt, lines 137–179](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/ui/viewmodel/ExpertModeSelection.kt#L137-L179) |
| Design intent | The reset-to-default documentation explicitly distinguishes default version-compatible patches from other-version patches which users can enable by hand. | [ExpertModeSelection.kt, lines 238–251](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/ui/viewmodel/ExpertModeSelection.kt#L238-L251) |
| Availability edits | `PatchTarget.applyTo` calls `applyAvailability` with installer type, architecture and all scoped patch names. This does not recheck app versions. | [PatchFlow.kt, lines 137–170](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/ui/viewmodel/PatchFlow.kt#L137-L170), [PatchSelectionExtensions.kt, lines 369–393](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/util/PatchSelectionExtensions.kt#L369-L393) |
| Final selection | `proceedExpertMode` forwards `expertMode.runSelection`; PatcherViewModel preflight checks name existence and required patcher version, then sends `runSelection` to the worker unchanged. | [PatchFlow.kt, lines 1270–1314](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/ui/viewmodel/PatchFlow.kt#L1270-L1314), [PatcherViewModel.kt, lines 594–655](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/ui/viewmodel/PatcherViewModel.kt#L594-L655), [worker argument construction, lines 912–925](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/ui/viewmodel/PatcherViewModel.kt#L912-L925) |
| Runtime request | The worker forwards `args.selectedPatches`. `Runtime.requestedBundles` copies those names directly into `PatchRequest.Bundle`. | [PatcherWorker.kt, lines 463–475](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/patcher/worker/PatcherWorker.kt#L463-L475), [Runtime.kt](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/app/src/main/java/app/morphe/manager/patcher/runtime/Runtime.kt) |
| Patcher handoff | PatchPipeline resolves patches by package and selected names, then invokes Session. `keyedFor` checks package name, not version. | [PatchPipeline.kt, lines 50–60](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/patcher-core/src/main/kotlin/app/morphe/manager/patcher/PatchPipeline.kt#L50-L60), [PatchKeys.kt](https://github.com/MorpheApp/morphe-manager/blob/d8ceac809082ff23da0a0956f16ead11ac995ae8/patcher-core/src/main/kotlin/app/morphe/manager/patcher/patch/PatchKeys.kt) |

The immutable primary-source links above identify the audited files. This report does not claim to include a copy of the Manager source tree or to verify physical-device UI behavior.
