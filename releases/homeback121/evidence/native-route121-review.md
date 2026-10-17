# Independent review of the native Instagram Back integration

## Review result

No remaining concrete blocker was found in the frozen candidate source reviewed here. The compile collision, narrow-layer veto omission, real Reels subclass coverage, and real Reels pager-wrapper coverage identified during this review have been corrected. The last pager checks now include the exact cached adapter identity and page-count bounds.

`native-route121-review.json` records SHA-256 hashes for all 15 runtime source files and the exact native evidence files. The runtime source manifest hash is `e4ece78580b0a32b6c58499438ffdee2f34b4ee06110cddbdcceda7a150d408e`; its encoding is specified in the JSON. This ties the review to the source snapshot rather than merely a release directory name.

The key reviewed properties are: the shared OS entry can handle the native AndroidX bypass; exact Main and Reels layer guards remain in force; predictive action/object identity and outer native origin prevent a single layer/detail Back from subsequently exiting exposed Reels; inactive roots are excluded using verified native ownership; and other apps' handlers are unchanged. The user's physical-device callback and live scene remain unobserved.

## Reviewed input and scope

The native evidence comes from the user's uploaded, currently installed APK:

`/workspace/scratch/075f7970e1d9/upload/Instagram-v447.0.0.55.81-patches-v3.10.0.apk`

SHA-256: `d5291dcb5f3f5252dff05b98b4fefba121369d3a1c74635190cb8ed8047757a7`.

This is distinct from the earlier supplied APK. The uploaded APK contains the newer Main and Reels hooks. This review does not assume that the user failed to install an update.

The implementation review covers `InstagramBack.java`, `InstagramReels.java`, `ExitScreens.java`, `OneBackExit.java`, and their integration with `InstagramCurrent.java` and `InstagramActivityLayers.java` in the isolated `homeback121` candidate. It is a source review and a native-bytecode audit. Host fixtures exercise modeled Android behavior; they do not constitute execution of the uploaded APK on the user's Android device.

## Actual callback route and purpose of the shared fix

The uploaded APK still has an enabled native AndroidX path from the OS callback to `X.01h.A04`, then `X.0ct.A06`, then the owning `X.0dj` FragmentManager's native pop/completion. That path can return without entering either patched `InstagramMainActivity.A1s` or `X.8Zv.onBackPressed`.

The existing `NativeBack.Callback` surrounds the original callback with `enterFocusedBack` / `afterNative` / `leaveNative`. The root callback's native forwarding likewise uses `enterBack` and a `finally` cleanup. When a tracked, focused root supplies the Activity, terminal scope is preserved even on this native direct-pop path. The problem is therefore not explained by assuming an asynchronous scope loss.

The candidate's shared route classification can make `tryFocusedHome` or the owned root callback decide the initial Back before the original AndroidX callback pops. Repairing only either dedicated native boundary would leave the bypass intact. The new current-route adapter also supplies Home identity without requiring a visually selected accessibility tab.

The old/new decoded comparison and inserted branch-target checks are recorded in:

- `/workspace/scratch/075f7970e1d9/apk-audit/upload-back-path/native-method-comparison.json`
- `/workspace/scratch/075f7970e1d9/apk-audit/upload-back-path/injection-control-flow-check.json`
- `/workspace/scratch/075f7970e1d9/apk-audit/upload-back-path/report.md`

## Main Activity layers

Calling the Main Reels bridge at arbitrary OS entry would skip native guards. The candidate instead reads the states used by those guards:

| Native guard | Read-only contract | Interpretation |
| --- | --- | --- |
| `X.3MM.A09` | `Main.A08 -> 3MM.A02:WeakReference -> 9ld.isAdded()` | An added modal consumes native Back. |
| `X.0wS.A0p` from Main | `IgFragmentActivity.A00 -> X.16z.A0U` and observational `X.0wS.A0M()` | Enabled layer with current fragment consumes native Back. |
| Showing native dialog | Root manager `A0V.A01`, `X.07d.A00:Dialog`, `Dialog.isShowing()` | A showing dialog keeps its foreground Back. |

The Main layer flag is `A0U`. The separate Reels internal-layer flag is `A15`. `X.RxR` shadows `A00` with `X.8g8`; looking up the Dialog on the exact `X.07d` declaring class is necessary. The adapter never invokes `A09`, `A0p`, a dismiss method, or a terminal method during classification.

`InstagramActivityLayers.inspect` distinguishes `UNKNOWN`, `CLEAR`, and `BLOCKED`. Shared classification treats an unknown Main contract as unknown, without turning the mismatch itself into proof that comments are open. Focused adapter verification passed 34 cases and 60 assertions; the exact results and APK contract are saved as `native-activity121-host.txt`, `native-activity121-apk-contract.txt`, and `native-activity121-audit.json` in this evidence directory.

## Same-gesture and predictive Back review

`InstagramBack.protect` captures an `Origin` at the outer native Back entry. The captured record permits a dedicated Reels boundary only when that initial snapshot was clear, recognized Reels and identifies the same fragment object. A native pop that exposes Reels from a detail, modal, unknown state, or another object cannot reuse that Back to exit.

`OneBackExit.enterBack` creates this scope only when `TERMINAL_DEPTH` is absent. Nested native calls preserve the original record. The corresponding outer `leaveNative` clears it together with terminal scope, including exception cleanup paths. Clearing it on every nested return would have lost the protection; the candidate does not do that.

Prediction retains both the action and the authorized object identity. An exit prediction cannot transfer to a newly created Home/Reels object. A detail or unknown prediction with action zero and no authorized identity still rejects the dedicated native bridges if a native start animation exposes Reels before committed native dispatch captures its own origin.

No additional concrete same-gesture defect was found in these reviewed changes. The root-owned dispatch test covers native direct-pop modeling, known Main/Reels modal closure, subsequent distinct Back, prediction cancellation, changed identity, and retained-tree budgets.

## Explicit layer veto regression found and corrected during review

The first version of the native fast path bypassed `ExitScreens.scanViews`. Its generic comments/modal scan required minimum dimensions, while the previous exact resource veto did not. A narrow visible `comment_edit_text` or small identified sheet could therefore escape the new path's checks.

The root corrected this with the native-path `namedLayer` veto for `comment_edit_text`, `comment_sheet`, `comments_sheet`, `share_sheet`, `bottom_sheet`, and `design_bottom_sheet`, retaining the existing positive Reels-wrapper exemption. Six focused narrow-layer scenarios were added to `Back121InstagramDispatchTest`. The other previous explicit resource vetoes remain represented by `otherView`.

## Actual native Reels class family

The initial exact-class-only `X.8Zv` classification omitted actual factory outputs in this APK. The review identified these shapes:

| Shape | Uploaded APK evidence | Required classification |
| --- | --- | --- |
| `X.8Zv` | Public, non-final native Reels fragment. | Existing Reels content and internal guards. |
| `X.Ze9` | Public final subclass of `X.8Zv`; emitted by `X.8Zo.A05` at decoded PC 139. | Recognize the `X.8Zv` superclass and preserve its declaring-field guards. |
| `X.9sY` | Public final sibling wrapper; emitted by `X.8Zo.A03` at PC 95 and `X.8Zo.A05` at PC 157. | Descend through its verified current pager child; the wrapper alone is not authoritative OTHER. |

These are statically possible native routes in the actual uploaded bytes. The APK does not reveal which configuration or page was active on the user's device.

### `X.9sY` current child

Native `X.9sY.A00(9sY):Fragment` is public static final. It requires a host, reads `9sY.A00:ViewPager2`, reads `ViewPager2.A00:int`, then finds the child-manager tag `"f" + index`. A child with a non-null view is returned. Its fallback chooses the first resumed child with a view from the child store. This method also emits probe/logging calls, so the adapter uses its verified cached fields and the existing pure manager getter instead.

`9sY.onBackPressed()` sets pager index zero and returns true when the current index is nonzero. At index zero it delegates to the current `Peq` child. The root's intended exit policy overrides this ordinary pager navigation when the verified selected child is actual Reels; an OTHER child remains native. A wrapper or selected index alone does not authorize exit.

The following additional native contracts were verified for stale-adapter and retained-page checks:

- `9sY.A05:X.A9v` is public instance state. `X.A9v` is public final and extends `X.3e5`.
- `A9v.A04:java.util.List` is the adapter's cached page list. Native `getItemCount()` returns exactly `A04.size()`, with no added page.
- `9sY.onViewCreated` assigns the same `9sY.A02` list into `A9v.A04`, stores the adapter in `9sY.A05`, and passes it to `ViewPager2.setAdapter`.
- `X.3e5.getItemId(int)` returns the index converted to long; `A9v` does not override it.
- `ViewPager2` is final. Its public, non-final `getAdapter():X.8QJ` is a pure two-field getter: `pager.A05:RecyclerView -> RecyclerView.A0E:X.8QJ`. Comparing its result by identity to `9sY.A05` can reject a stale/replaced adapter.

The final adapter excludes retained canonical `fN` page roots only within this cached page range, with the existing same manager, same parent, same Activity, non-resumed, and owned-view checks. Merged-feed tags retain their separate native family. The adapter owner reported 23 passing cases and 78 assertions after this final change, including stale adapter identity, out-of-range retained tags, index-zero/index-one Reels positives, and an index-one OTHER negative. The coordinator owns combined execution.

Exact dumps are under `/workspace/scratch/075f7970e1d9/apk-audit/upload-back-path/`:

- `native-reels-class-family.txt`
- `native-reels-factories-current.txt`
- `native-reels-9sY-current.txt`
- `native-reels-9sY-field-contract.txt`
- `native-reels-9sY-view-current.txt`
- `native-reels-pager-adapter-contract.txt`
- `native-reels-pager-count.txt`
- `native-reels-pager-getAdapter.txt`

## Other apps and remaining evidence boundary

The source comparison against `homeback120` found `HomeScreen`, `NativeBack`, `BackNavigation`, `TikTokBack`, `TikTokHome`, `TikTokScreen`, `TripBack`, and `TwitterBack` byte-for-byte unchanged. `ExitScreens` adds Instagram-only snapshot handling. The sole `OneBackExit` change is Instagram origin cleanup in `leaveNative`.

Final compilation, combined host execution, DEX rebuilding, and full distribution-path validation are owned by the coordinating agents. This report identifies the native route, the reviewed safety invariants, and the concrete review corrections. It does not claim a captured device runtime trace or establish which one of the proven failure paths produced the user's observation.
