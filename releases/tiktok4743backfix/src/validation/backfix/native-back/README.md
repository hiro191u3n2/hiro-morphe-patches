# TikTok 47.4.3: native Back boundary audit

This audit examines the real global APK (`com.zhiliaoapp.musically`) and emitted DEX. It does not claim execution on an Android device. APKs and native implementation dumps are not redistributed here.

## Why a valid Home API fingerprint was insufficient

The old runtime could classify a visible Home screen as unknown when feed labels or view names were absent. It then entered a navigation-protection session and blocked terminal activity operations. An unchanged native Home navigation API therefore did not establish that the Back gesture could reach the exit path.

The actual 47.4.3 AndroidX fallback adds a separate failure path. `OnBackPressedDispatcher.LJ()` invokes `LX/004V.run()`, which delegates through `LX/02Ft` to `ComponentActivity.access$onBackPressed$s1027565324`. That method invokes `android.app.Activity.onBackPressed` through `invoke-super`; it does not dynamically invoke TikTok's `LX/03uJ.onBackPressed`. Forwarding directly to the dispatcher could consequently bypass TikTok's native Back guards and reach the old runtime's protected terminal operation instead.

The repair marks the verified SAF wrapper entry with `enterTikTokBack`, permitting the runtime to distinguish a genuine TikTok main dispatch from an AndroidX platform fallback. The runtime's separate tests cover the once-only reroute and gesture state. The DEX audit verifies the wrapper and preservation of the original callback body and exception cleanup.

## Guarded native hooks

The two methods below have identical original-body fingerprints in the original 47.4.3 APK and the previous v4.1.0 all-144-patch output. The native interceptor, comment handling, and tutorial guards must run before an exit decision.

| Original method | Original insertion point | Added behavior |
| --- | --- | --- |
| `LX/1AA5.onBackPressed()Z` | Instruction 25, code-unit address 47 | After `InterceptHomeBackPressService.LIZ` and `CommentService.LJLL`, call `skipTikTokDoubleBack(Activity)`. A true result returns false to the caller, bypassing only the two-Back timer. A false result resumes the original timer. |
| `BackRefreshStrategyAssem.onBackPress()V` | Instruction 46, code-unit address 91 | After the helper/comment handling and `MainBusinessAbility.f7` / `Pn` guards, call `tryTikTokRootBack(Activity)`. A true result reaches the existing cleanup at original instruction 27; false resumes native tab handling. |

`f7` and `Pn` reach the native `swipe_up_guide` and `not_interested_tutorial` checks. Placing an exit immediately after the helper would skip these guards, so it is deliberately later.

The helper has 18 registers, and `v16 = 0` dominates the inserted return. It passes its existing `TiktokBaseMainHelper.LIZ` Activity field. The strategy has 13 registers and passes the existing Activity in `v3`. Neither method gains registers or replaces its original implementation.

## Inner Home and outer page

The native Hox tab can remain `HOME` while the outer pager shows `page_profile`, `page_discover`, or `page_camera`. Production admission therefore checks the public `MainActivity.stateManager` binding, the exact `ScrollSwitchStateManager` inheritance, and the `HomePageViewPagerAbility` binding and implementation. The runtime queries `BaseScrollSwitchStateManager.s33("page_feed")` before allowing root exit. Known outer pages retain `CommonPageFragment.LJLI()` and the native `Ir1(null, true)` transition. `LJLI` is public and virtual in the original APK, so reflective invocation preserves a concrete page's override.

The outer-page contract adds twelve full method-body fingerprints: six state-manager methods, the page guard, the outer ability implementation and its delegate, and three `CurrentPagerItem` operations that read or change the actual pager state. All twelve are identical in the original APK and the old all-144-patch output. Together with the eight existing Home methods, admission verifies twenty native routing bodies, in addition to the two Back boundary bodies. `outer-method-fingerprints.txt` and `outer-body-parity.log` record this check.

## Serialized branch checks

Instruction indices below refer to emitted methods, after insertion.

| Edge | Required target | Reason |
| --- | --- | --- |
| Helper inserted `if-eqz`, index 28 | Index 30, original timer call | The inactive gate must not jump back into itself. |
| Strategy native `if-eqz`, indices 41 and 44 | Index 46, new root gate | Existing native branches must enter the gate rather than skip it. |
| Strategy inserted `if-nez`, index 48 | Index 27, existing cleanup | A handled Back must not trigger a second native transition. |

The second condition detected a real pre-release emitter defect. dexlib kept incoming labels attached to the original instruction after insertion, so both existing branches initially landed at index 49 and bypassed the new gate. The production emitter now explicitly rebinds both branches. The post-serialization audit rejects that defect.

The audit removes only the five helper instructions or three strategy instructions from the emitted body and recomputes its full normalized SHA-256. It must exactly match the original method, including every branch and exception region. It also checks all inserted signatures, operands, and register usage. Merely locating a hook name in DEX is not considered success.

## Regression input and tests

`input-sha256.json` identifies the original positive APK, the genuine 47.5.3 negative APK, and the old v4.1.0 all-144-patch output. `native-method-fingerprints.txt` records the small set of native methods examined for this route; pool-dependent string/goto widths are normalized by `TikTokNativeContract.digest`.

`TikTok4743NativeBoundaryAudit` reads the real old patched APK, proves its old helper fails the new boundary audit, invokes the production `TikTokNativeBoundaries.apply`, writes three changed classes to DEX, and reloads that DEX for inspection. It also injects two deliberate defects: a helper fallback self-loop and a native branch that skips the root gate. Both must be rejected.

`TikTok4753ContractAudit` retains its historical filename for the existing QA command interface. Its positive mode accepts only 47.4.3, checks native method fingerprints, and rejects missing or changed methods. Its `reject4753` mode proves the input contains the real 47.5.3 Home ABI, then rejects both its real version and a false 47.4.3 label.

The final original-input audit also rejects missing, static, or incorrectly typed outer bindings, an incorrect state-manager superclass, an implementation without the outer interface, and removal of the required outer operation. The final positive audit reports 94 assertions, and the actual 47.5.3 rejection reports 19. The serialized native-boundary regression reports 410 assertions. These counts include structural checks and fixture reads; they are not counts of device test scenarios.

These checks establish actual bytecode admission, preserved native guards, emitted branch destinations, and deliberate rejection of 47.5.3. Device-specific gesture dispatch, task removal in Samsung's launcher, and screen rendering still require execution on that device; these logs do not assert those observations.
