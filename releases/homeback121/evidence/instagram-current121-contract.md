# Installed Instagram 447: native API and current-screen scope audit

## Scope and conclusion

The audited installed APK is `/workspace/scratch/075f7970e1d9/upload/Instagram-v447.0.0.55.81-patches-v3.10.0.apk`, supplied SHA-256 `d5291dcb5f3f5252dff05b98b4fefba121369d3a1c74635190cb8ed8047757a7`.

The uploaded bytes preserve the native Fragment APIs, navigation getters, tab resources, and packed layout assets previously audited in Instagram 447. The pipeline audit separately confirms the current runtime and both native hooks. This investigation does **not** attribute the failure to a missed update or a failure to repatch.

There is a reproducible structural defect in the published120 runtime: the allegedly local Reels footer/caption check climbs back to the Activity decor, where an unrelated retained tree can exhaust its viewer-search budget. A larger retained tree also prevents the ordinary Home recognizer from succeeding. The counterexample reproduces both reported symptoms with the published runtime. It establishes a defect in the implementation; without a device hierarchy or event trace, it does not establish that the user's current device has exactly that retained-tree count.

The new `InstagramCurrent` adapter uses the actual native current route to establish which fragment owns the displayed Home or Reels content and which retained fragment roots are previous content. It does not change scan limits or issue navigation calls. The root agent integrates its result into the shared action and layer scans.

## Evidence files

All following paths are relative to `/workspace/scratch/075f7970e1d9/`.

| Evidence | File |
|---|---|
| Uploaded AndroidX Fragment/FragmentActivity declarations and implementations | `runtime-api-audit/upload/actual-fragment-api.txt` |
| Uploaded navigation and manager declarations | `runtime-api-audit/upload/actual-nav-api.txt` |
| Exact current-content getters, manager find/count/saved methods, merged feed selection | `runtime-api-audit/upload/pure-content-getters.txt` |
| Fragment owner, parent, visibility, and manager transition fields | `runtime-api-audit/upload/fragment-ownership-and-pending.txt` |
| Pending-action queue references and execution | `runtime-api-audit/upload/pending-actions-references.txt`, `manager-pending-and-wrapper-view.txt` |
| Main native tab selection and enum identity | `runtime-api-audit/upload/native-tab-current-identity.txt` |
| Wrapper cache construction | `runtime-api-audit/upload/tab-wrapper-identity-source.txt`, `tab-lazy-cache.txt`, `tab-lazy-cache-implementation.txt` |
| Home class and module, cached merged feed type | `runtime-api-audit/upload/home-content-structure.txt`, `selected-home-getters.txt` |
| Tab presenter/accessibility calls | `runtime-api-audit/upload/home-tab-apis.txt`, `home-tab-apis-old447.txt` |
| Native resources and packed asset hashes | `runtime-api-audit/upload/resources.txt`, `resource-asset-hashes.json` |
| Actual Reels factory subclasses and wrappers | `apk-audit/upload-back-path/native-reels-class-family.txt`, `native-reels-factories-current.txt` |
| Clips pager current/native Back methods and field contracts | `apk-audit/upload-back-path/native-reels-9sY-current.txt`, `native-reels-9sY-field-contract.txt` |
| Clips pager adapter count, index IDs, assignment and getter | `apk-audit/upload-back-path/native-reels-pager-count.txt`, `native-reels-pager-adapter-contract.txt`, `native-reels-9sY-view-current.txt`, `native-reels-pager-getAdapter.txt` |
| Earlier reproducible structural counterexample | `runtime-api-audit/NativeFooterProbe.java`, `native-footer-probe-published120.txt` |
| New adapter test execution | `runtime-api-audit/upload/current-adapter-test.log` |

## Native API contracts confirmed in the uploaded APK

The support FragmentManager does not expose ordinary AndroidX method names such as `getFragments`, `getBackStackEntryCount`, or `isStateSaved`. Those names alone are insufficient in this APK.

| Native contract | Exact behavior or qualification |
|---|---|
| `FragmentActivity.A1Y(): X.0dj` | Public final support-manager getter. |
| `MainActivity.A1e(): androidx.fragment.app.Fragment` | Public final; calls its `A07: X.2xZ` navigation object. |
| `X.2xZ.A08()` | Calls its `A02: X.2yE` current-fragment getter. |
| `X.2yE.A05()` | Under the native tab-host configuration, resolves the selected tab wrapper using `X.3fS.A00`; otherwise uses root manager `A0O(0x7f0b226f)`. |
| `X.2yE.A06(): X.2yl` | Public final; reads the navigation stack's current entry and returns its tab enum. |
| `X.2yE.A03: FragmentActivity` | Public final Activity owner. |
| `X.2yl.A0C` | Public static final FEED enum constant. |
| `X.2yl.A03: String` | Public final tab identity used by native wrapper lookup. |
| `X.0dj.A0O(int): Fragment` | Public final read-only find-by-ID; searches added fragments backwards and then active-state entries. |
| `X.0dj.A0Q(String): Fragment` | Public final read-only find-by-tag with analogous added/active lookup. |
| `X.0dj.A0L(): int` | Public final native stack count: `A0E.size()` plus one when `A05` is non-null. |
| `X.0dj.A14(): boolean` | Public final saved/stopped state: `A0J || A0K`. |
| `X.0dj.mExecutingActions: boolean` | Public execution-state field. |
| `X.0dj.A0W: ArrayList` | Public final pending-action queue; enqueued by native transaction logic and drained by execution. |
| `X.0dj.A0V: X.0dw` / `X.0dw.A01: ArrayList` | Public final store and added-fragment list. |
| `Fragment.getActivity()` | Public final, return type `FragmentActivity`; reads the attached host Activity. |
| `Fragment.getParentFragment()` / `getParentFragmentManager()` | Public final exact parent/manager ownership APIs. |
| `Fragment.isAdded()` / `isVisible()` / `isResumed()` | Public final. `isVisible` requires an added, nonhidden fragment, non-null view/window token and visible view; `isResumed` tests native state at least7. `isHidden` also checks a hidden parent. |
| `Fragment.getView()` | Public, return type `android.view.View`. |

The adapter validates these declaring classes, field types, method return types, public access, required final modifiers and instance/static shape. It never calls transaction execution, tab selection, or a pop method. Its additional wrapper scans do not directly invoke lazy initializers. Main.A1e is the app's navigation-read-only current getter; internally its native3fS path can evaluate a tab-key lazy value. This is not a claim that invoking the native getter performs absolutely no cache writes or instrumentation.

## Exact current-content traversal

1. The native family is recognized by the actual MainActivity → IgFragmentActivity → FragmentActivity hierarchy. A current-family signature/field failure is authoritative UNKNOWN. An older supported host without that family remains eligible for the existing fallback.
2. `Main.A1e` supplies the selected native root. Its owner and parent manager must match the Activity and root support manager. The current native tab comes from the native controller, whose owner must be the same Activity.
3. A selected `X.0jG` must have an already-cached tab key matching that selected enum. Other roots must match the native root-container `A0O` selection.
4. The adapter follows only the verified container rules below, with cycle/depth protection, ownership checks and added/visible/resumed state at each node.
5. The terminal fragment must have a displayed large view rooted in this Activity's decor. Native Home additionally requires the FEED enum. Native Reels accepts the audited `X.8Zv` base class, including its `X.Ze9` subclass.

| Wrapper | Native current-content rule |
|---|---|
| `X.0jG` | Prefer child `A0O(0x7f0b226f)` when non-null; otherwise use child `A0O(0x7f0b2275)`. |
| `X.3gY` | Child `A0O(0x7f0b226f)`. |
| `X.22c` | Require non-null cached `A05: X.eBE`; its public final `A04(): int` chooses tag `merged_feed_tab_{index}`, index0–4. |
| `X.9sY` | Read its exact final ViewPager2's `A00` selected index, verify the attached adapter is the same cached `A05: X.A9v`, read cached `A9v.A04: List` size, and find exact tag `f{index}` within that range. |

The resource names for the two native containers are `layout_container_main` (`0x7f0b226f`) and `layout_container_swipeable` (`0x7f0b2275`). The distinction matters: `X.0jG.A03()` returns only the swipeable base. Its native `EwD` and Back dispatch explicitly prefer the main-container override. A blind call to A03 would incorrectly identify Home behind an active profile/detail or Reels overlay.

`X.22c.A07()` has a lazy fallback when A05 is null. The adapter does not invoke it. Likewise, an extra direct call to the wrapper's `X.Esp.getValue()` could initialize an unused tab. Native0jG constructs an exact `X.4z8` NONE-mode lazy; the adapter's own wrapper-key scan reads only its public `A00: Object` cache and requires a String. The test lazy getter deliberately throws, ensuring that added scan cannot silently start using that getter. The fixture represents the result of Main.A1e; it does not execute Main's internal native lazy lookup.

## Reels factory and pager variants

The uploaded factory `X.8Zo` can construct `X.8Zv`, its public final subclass `X.Ze9`, or the public final `X.9sY` ClipsTabFragment wrapper. Restricting current identity to `getClass().getName().equals("X.8Zv")` would miss a real native factory result.

`X.9sY` uses ViewPager2. Its native current-fragment helper first resolves `f{currentIndex}` and requires a view; a fallback searches the first resumed child. The helper also emits probe/log calls. The adapter uses the exact field/tag branch only, validates the current child normally, and returns UNKNOWN when the pager or selected tag is unavailable instead of guessing a resumed child.

The pager count is not guessed. `X.A9v.getItemCount()` returns `A04.size()`; native creation assigns the same cached list to that field. `X.3e5.getItemId(index)` returns the index as a long, giving the `fN` tag family. The pure exact ViewPager2 `getAdapter(): X.8QJ` reads its RecyclerView adapter, allowing a strict identity check against the cached native adapter.

Native Back on a nonzero Clips pager page ordinarily resets to page0. The requested Reels exit intentionally overrides ordinary Reels tab/stack navigation. Therefore a verified current8Zv/Ze9 child is Reels at any valid page index; another current child remains OTHER. An index outside the cached adapter range, a replaced adapter, a null pager, or a stale current child remains UNKNOWN.

## Scope of retained-background exclusion

The adapter returns fragment View roots to exclude, not an unrestricted permission to ignore Activity siblings. Every excluded fragment must be added, nonresumed, owned by this Activity, attached to the same exact parent Fragment and manager frame, and rooted in this Activity's decor. A root containing the selected terminal view is removed from the exclusion set.

The positive routing evidence is one of:

- Previous content in the same verified native container as the current find-by-ID result.
- The inactive swipeable base when0jG's native main-container override is selected.
- Another exact root0jG tab wrapper whose cached key is a different **actual X.2yl enum key**. Unmapped cached strings do not qualify.
- Another known merged-feed tag in the same selected merged-feed child manager.
- Another canonical `fN` tag in the same Clips pager child manager, with N inside the verified cached adapter count.

Resumed fragments, different/unmatched containers, uninitialized or unmapped tab keys, arbitrary tags such as `f9999` outside the actual adapter range, and unrelated visible overlays retain their scan scope. This avoids the old requirement that retained page views be immediate drawing-order siblings; the native routing relationship can correctly identify a retained branch several view ancestors away.

`Result.stateClear` separately exposes saved/stopped, executing and queued native manager state. `Result.stackClear` separately exposes the native stack count across the active path and its terminal child manager. Home requires an empty stack; a Reels screen may have a stack because opening Reels from another screen is an intended exit case. The adapter itself never exits an Activity or modifies these managers.

## Home selection and resource findings

The uploaded Home presenter/accessibility bytecode is identical to the old447 comparison. The ordinary presenter forwards selection to `View.setSelected`. The alternate Litho presenter updates its model rather than setting the outer tab view selected. Native `X.2xZ.A0O` also updates the tab's accessibility delegate through `X.3AS.A01`, and `X.8ZH` reports the captured selected state through `AccessibilityNodeInfo.setSelected`.

The published Home recognizer already reads accessibility selection. Consequently, “the real tab never supplies selected state” is not a proven universal defect. There is an initialization-order possibility where the Main tab setup delegate captures false before a later selection refresh; without actual runtime state that is not a device diagnosis. The new Home authority uses the current native FEED route plus the actual Home fragment, so its positive recognition does not depend on that UI detail or the total count of retained background views.

Native `feed_tab` remains `0x7f0b17e1`; `clips_tab` remains `0x7f0b0c58`. All three audited447 APKs contain identical860,112-byte `assets/layouts.bin.xz`, SHA-256 `46861f3826ecaa66e72c1214ee006be1bb691536d94082a44e0e399e7a34bf99`. Native layout-footer assumptions remain applicable to the uploaded APK; the resource table as a whole differs because patched resources differ.

## Verification and limits

New files owned by this work are:

- `hiro-morphe-patches/releases/homeback121/src/runtime/InstagramCurrent.java`
- `hiro-morphe-patches/releases/homeback121/src/test-runtime/InstagramCurrent121Fixture.java`
- `hiro-morphe-patches/releases/homeback121/src/test-runtime/Back121InstagramCurrentTest.java`

The focused suite passes23 cases and78 assertions. It covers native Home authority without a selected tab View, overlay precedence, large retained base/tab trees, same-container previous content, exclusion limits, merged/pager selection, native Reels subclasses, stale/null/foreign/detached state, manager stack/transition status, native-family signature failures, and legacy fallback.

Reproduce the saved adapter test using:

```sh
/workspace/scratch/075f7970e1d9/build-inputs/jdk-21.0.8+9/bin/java -cp /workspace/scratch/075f7970e1d9/runtime-api-audit/upload/current-classes:/workspace/scratch/075f7970e1d9/build120-final/test:/workspace/scratch/075f7970e1d9/build120-final/runtime app.hiro.oneback.runtime.Back121InstagramCurrentTest
```

The saved class directory includes the new adapter, its fixture/tests and the current Dialog test stub. The remainder of that classpath uses the published120 runtime/test classes, isolating the adapter test from root's shared-action changes. Full121 SDK compilation, shared-action tests, callback tests and APK hook verification are performed by the root/pipeline agents separately.

The earlier published-runtime structural counterexample can be reproduced using:

```sh
/workspace/scratch/075f7970e1d9/build-inputs/jdk-21.0.8+9/bin/java -cp /workspace/scratch/075f7970e1d9/runtime-api-audit/probe:/workspace/scratch/075f7970e1d9/build120-final/test:/workspace/scratch/075f7970e1d9/build120-final/runtime app.hiro.oneback.runtime.NativeFooterProbe
```

Its601-sibling case falsely marks the permanent footer as comments and declines native Reels exit. Its1,801-sibling case also leaves later selected Home unrecognized. Those are explicit fixture sizes. They demonstrate the interaction of the published code's local/global predicates and budgets; they do not measure the user's installed runtime hierarchy.

No universal false predicate was found in the real Android window/decor identity, focus, IME inset or Fragment lifecycle APIs. This audit also does not prove real-device callback ordering or complete device behavior. It provides actual uploaded native API evidence and deterministic counterexamples for a targeted implementation repair.
