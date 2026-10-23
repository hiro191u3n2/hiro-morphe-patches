# OneBack 1.1.20: TikTok 47.5.3 native navigation

This change repairs a concrete broken native contract. OneBack 1.1.19 used four
46.2.3 method names that do not exist in the original global TikTok 47.5.3 APK.
The old reflection catch returned `null`, leaving only UI heuristics and native
Back fallback even though the patcher had reported success.

## Native evidence

- Package: `com.zhiliaoapp.musically`
- Version: `47.5.3`
- Original APK SHA-256:
  `6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e`
- HomeTabAbility, TabAbilityAssem and Hox occur in `classes3.dex`; MainActivity
  occurs in `classes2.dex`.

| Operation | 46.2.3 | Verified 47.5.3 |
| --- | --- | --- |
| Select a native tab | `HomeTabAbility.Lx0(Bundle,String)` | `IA0(Bundle,String)` |
| Selected child of `page_feed` | `HomeTabAbility.Mm2()` | `Nu2()` |
| Read Hox tree | `HomeTabAbility.qE0()` | `EH0()` |
| Test the active path for `HOME` | `Hox.fU2(String)` | `i33(String)` |

The new names were not chosen only by their descriptors. Inspection of the
original method bodies confirms that `IA0` calls `Hox.r33(bundle, tag, false)`,
`Nu2` reads the selected child of `page_feed` using `Hox.b33`, `EH0` delegates to
the same Hox accessor `Cp`, and `i33` walks the active node's ancestor tags. These
are the same operations recorded in the earlier 46.2.3 audit. `Nx0()` is **not**
used: it reads a child of `HOME` rather than the selected root tab.

`TikTokNativeContract` requires the actual application binding, public interface
and implementation methods, and the complete normalized bodies of eight native
methods. Pool-dependent string/goto widths are normalized; references, registers,
literals, branch destinations, exception regions and method access are retained.
An unknown version, missing dependency or changed body fails before native hook
mutation. It cannot silently succeed with an ineffective native route.

47.5.3 admission is limited to the audited global package. The previous 46.2.3
contract remains available for both prior TikTok package names. Other
applications' metadata and implementation remain unchanged.

## Runtime behavior and scope

Only `runtime/TikTokHome.java` changed among the 85 runtime classes. Compiled
class comparison against 1.1.19 confirmed the other 84 class files are identical.
The native 47.5.3 selector returns profile/root tabs to Home. Existing transaction
guards continue to prevent that same Back gesture from exiting the task; the
next Back on confirmed Home removes it from Recents. Layer/editor protection,
bounded asynchronous navigation and visible-feed confirmation are retained.

The 47.5.3 runtime test uses replacement stubs that have only the actual new
method names. The old 1.1.19 runtime fails the profile-to-Home assertion against
those stubs; the new runtime passes all 17 existing TikTok navigation scenarios.
This is a host regression model. It is not Android device verification.

Previously OneBack-patched TikTok APKs still require a clean original input.
Existing Instagram/X exact-upgrade resources and procedures are unchanged.

## Integration checks

1. Compile `runtime/*.java` against `android.jar` and `patch/*.java` against the
   pinned Morphe SDK. Compile and run the existing host tests with `test-stubs`,
   excluding `Back4753TikTokTest` from this legacy-ABI execution.
2. Make a separate host output directory. For each file under `test-stubs`, use
   the same relative path under `test-stubs4753` instead when present. Compile
   the same tests with this 47.5.3-only ABI; run
   `app.hiro.oneback.runtime.Back4753TikTokTest`.
3. Compile `test-dex/TikTok4753ContractAudit.java` with the patch classes and
   Morphe SDK. Invoke `TikTok4753ContractAudit original ORIGINAL_APK`. This checks
   real original native bodies, rejects false versions and the unaudited trill
   variant, and mutates each dependency to prove changed/missing code is rejected.
4. After the full real APK patch pipeline, invoke
   `TikTok4753ContractAudit output PATCHED_APK`. This checks the same native
   contract in emitted DEX, the new runtime's four method names, and the first
   applicable MainActivity hierarchy entry for lifecycle, Back and key hooks.

The output audit accepts only an emitted APK containing actual runtime hooks;
metadata/listing tests or an unchanged original APK cannot pass it.

The original APK and decompiled method bodies are local verification inputs and
are not included in the published package. The patch includes only fingerprints,
implementation, tests and verification results.
