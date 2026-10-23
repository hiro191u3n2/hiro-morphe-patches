# OneBack 1.1.21: TikTok 47.4.3 native navigation

This release adds the actual global TikTok 47.4.3 Home navigation contract while
retaining the verified 47.5.3 and legacy 46.2.3 contracts. The correction changes
the native method bindings, admission fingerprints and emitted-APK audit profile.
Adding 47.4.3 to the compatibility list alone does not repair these calls.

The previous 1.1.20 runtime fails the existing profile-to-Home scenario when run
against a fixture exposing only the method names found in the real 47.4.3 APK.
The new runtime passes that scenario and all 17 TikTok navigation scenarios for
both 47.4.3 and 47.5.3. These are host regressions, not physical Android tests.

## Native evidence

Both inspected APKs have package `com.zhiliaoapp.musically`. HomeTabAbility,
TabAbilityAssem and Hox are in `classes3.dex`; MainActivity is in `classes2.dex`.

| Original version | SHA-256 |
| --- | --- |
| 47.4.3 | `af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b` |
| 47.5.3 | `6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e` |

| Operation | Legacy 46.2.3 | Verified 47.4.3 | Verified 47.5.3 |
| --- | --- | --- | --- |
| Select a native tab | `HomeTabAbility.Lx0(Bundle,String)` | `tB0(Bundle,String)` | `IA0(Bundle,String)` |
| Selected child of `page_feed` | `HomeTabAbility.Mm2()` | `tv2()` | `Nu2()` |
| Read Hox tree | `HomeTabAbility.qE0()` | `jI0()` | `EH0()` |
| Test the active ancestor path for `HOME` | `Hox.fU2(String)` | `w33(String)` | `i33(String)` |
| First native Back/key owner in MainActivity hierarchy | Previously retained | `LX/03uJ;` | `LX/03dx;` |

These names were selected from native method bodies, not just their descriptors.
In 47.4.3, `tB0` calls `Hox.F33(bundle, tag, false)`, `tv2` asks
`Hox.p33("page_feed", false)` for the selected root child, and `jI0` delegates to
the Hox accessor `Cp`. `w33` walks the selected leaf's ancestor tags after
`Hox.o33` resolves that leaf. `Ly0()` is not suitable: it returns a child of
`HOME`, which does not identify the selected root tab. In 47.5.3 the corresponding
switch/read/leaf methods are `r33`, `b33` and `a33`; those existing fingerprints
are retained unchanged.

`TikTokNativeContract` requires the exact public MainActivity ability binding,
interface and implementation descriptors, and eight complete normalized method
bodies for each modern version. Pool-dependent string/goto widths are normalized;
references, registers, literals, branch destinations, exception regions and access
flags are retained. A missing dependency, changed body, incorrect version label
or unknown version is rejected before any native hook mutation. The two modern
profiles apply only to the audited global package. The legacy 46.2.3 contract is
retained for both previous package names; no 47.4.3/47.5.3 trill support is claimed.

## Runtime behavior and preservation

Only `runtime/TikTokHome.java` changes from 1.1.20. Compilation with the same
pinned JDK and Android SDK produced 85 runtime class files: TikTokHome differs,
and the other 84, including TikTokHome's Route class, are byte-identical.

The runtime resolves one complete version-specific set of four methods. It does
not combine partial contracts. Profile/root tab Back uses the app's own Hox Home
transition. The existing transaction guard consumes that gesture without ending
the task; the next Back on confirmed Home removes the task from Recents. Existing
layer/editor protections, bounded asynchronous navigation and visible-feed
confirmation remain unchanged. If a native transition fails, the existing native
Back fallback remains available.

Both new ABI fixtures deliberately omit the other two versions' method names.
The full legacy host suite also passes, including existing Instagram, X and
Trip.com behavior. Modern TikTok host fixtures exercise the runtime under both
known package names to protect shared logic; patch admission still restricts the
modern versions to `com.zhiliaoapp.musically`.

Previously OneBack-patched TikTok APKs require a clean original input. Existing
Instagram/X exact-upgrade resources and procedures are unchanged.

## Integration and actual-output checks

1. Compile `runtime/*.java` against `android.jar` and `patch/*.java` against the
   pinned Morphe SDK. Compile and run the existing host tests with `test-stubs`,
   excluding `Back4743TikTokTest` and `Back4753TikTokTest` from legacy execution.
2. For each modern version, compile its three replacement native stubs from
   `test-stubs4743` or `test-stubs4753` into a separate class directory, together
   with its `Back4743TikTokTest` or `Back4753TikTokTest`. Put that directory before
   the common host/runtime directories on the classpath and execute the test.
3. Compile `test-dex/TikTok4753ContractAudit.java` with the patch classes and SDK.
   The class name is retained for existing automation. Its CLI is:

   ```text
   TikTok4753ContractAudit original ORIGINAL_APK 47.4.3
   TikTok4753ContractAudit original ORIGINAL_APK 47.5.3
   TikTok4753ContractAudit output PATCHED_APK 47.4.3
   TikTok4753ContractAudit output PATCHED_APK 47.5.3
   ```

   Omitting the final version retains the previous 47.5.3 default. The
   `fingerprints` mode takes the same arguments and prints the selected profile's
   eight normalized native fingerprints.
4. The original audit verifies the native Home route and Back/key callback
   bodies. It rejects wrong version labels, the unaudited trill variant, each
   missing dependency, and each same-descriptor dependency with a changed body.
5. Run the output audit after the complete real-APK rebuild. It resolves the
   actual MainActivity superclass chain and requires its first Back/key handlers
   to reach `OneBackExit.tryBack`/`key`. It checks the complete bounded wrapper,
   the preserved native helper bodies, branch targets, native return propagation,
   normal cleanup and catch-all cleanup/rethrow. It also requires actual
   lifecycle bootstrap/resume hooks and the selected version's runtime bindings.

The output audit cannot pass an unchanged original APK or metadata-only output.
Its checks prove emitted DEX wiring and preservation, not successful UI execution
on a physical device. Full pipeline evidence is recorded separately by the
release's actual-APK validation.

Curated results are under `src/validation/oneback`. The original APKs and raw
native method dumps are local verification inputs and are not published. The
source release contains only implementation, fingerprints, tests and results.
