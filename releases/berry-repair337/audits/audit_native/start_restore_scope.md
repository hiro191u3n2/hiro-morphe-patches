# Berry native restoration entry audit

Audit source: `work332/native_audit/core.txt`, old Berry 3.83.74 app bytecode with older restoration hooks. The dump is not the exact published 332 output. Original application instructions are useful evidence; its hook signatures are not evidence of 332 runtime. No clean 3.84.12 APK was inspected.

## Conclusion

Do not return from `onPageStarted` simply because native BFCache restoration is proven. The method contains necessary URL/title model setup, native start/error view removal, per-site appearance handling, and toolbar/tab/navigation state updates. A tab object is shared across navigations; keeping its previous fields preserves page B's state, not the restored page A's state.

The defensible narrow suppression is the transition into loading state (`tab.T0(3)` in this version) and the added `TabPriority.started` call, only after positive proof tied to this WebView/current navigation. Retain the rest of the original method, then synchronize native completion after its body. If the loading setter call cannot be uniquely proven in an unknown version, keep the original start body and synchronize completion at its tail instead. No inspected instruction demonstrates a confirmed rendering-corruption root cause; this is a constrained state-lifecycle repair, not proof that renderer corruption will disappear.

## Original onPageStarted instruction groups

Numbers below are instruction indexes in the audit dump, not byte offsets. This text dump omits branch targets, so do not use it alone as a branch-rewrite template.

| Index | Meaning | Recommendation |
| --- | --- | --- |
| 1 | Added TabPriority.started(WebView) | Suppress only for positively proven restoration; keep foreground priority feature otherwise. |
| 4–6 | Get owned tab, constant 3, invoke T0(I) | Only original-start candidate for narrow loading suppression. Prove tab receiver and literal on all incoming paths. |
| 9–14 | Adapter.getUrl to tab metadata ns5.n | Preserve; page B URL must not survive restored A. |
| 15–39 | Internal/home title or null to ns5.o | Preserve branch semantics; later completion must recover current WebView title if finished/title callbacks are missing. |
| 40–42 | I/F=null, J=false | Preserve. I participates in SSL/main-request decisions; retaining B state is not restoring A. |
| 43–69 | Start time, error URL H, flags, script-ID list Z, auth holder, request-host maps | No per-Page snapshots exist. Do not blindly retain previous-tab values. |
| 70–73 | Queue and LruCache clear | Meaning not sufficiently isolated to suppress safely. Memory reduction alone is not proof of cause. |
| 74–151 | Native start/error-screen removal/replacement and actual WebView insertion; W0 updates | Preserve. Whole-start skip may leave custom native content over the restored WebView. |
| 136 | tab.Q() → adapter.Q() | Adapter only calls Lst3.d(1) when its WebView is Lst3. No general setScale/clip/reset was found here. Preserve. |
| 152–189 | Current URL/site matching, pending site dark setting, ALGORITHMIC_DARKENING setting | Preserve for restored destination. |
| 194–226 | Update tab model/display, bitmap/title-associated fields, notify tabs UI | Preserve. |
| 232–263 | Built-in early page-script injection branches | Not isolated as rendering cause; several inject DOMContentLoaded handlers. User reproduces with userscripts off. Do not broaden suppression here without separate proof. |
| 264–294 | User start-phase scripts and executed-ID list | Not needed for current no-userscript reproduction. Page-specific script bookkeeping would be required before preserving Z rather than resetting it. |
| 295–350 | Visible tab preview work, toolbar, navigation transient UI, private/history-related coroutines | Preserve; many are important UI updates, not merely loading setup. |

`T0(I)` is not a plain field write: it writes tab.n0 and has enum-dependent side effects (including tab.X and metadata.r). Use its existing setter, not a guessed raw n0 update. Replacing 3 with 1 is not identical to omitting the start call; use the already extracted completion method at the proper post-body point.

## Native completion details

Original `onPageFinished` normal path sets T0(1), reads adapter URL/title into the metadata, resets holder ready/count, updates visible toolbar/progress, and additionally performs zoom, scripts, preview work and coroutines. Do not call the whole original callback artificially: it can replay scripts, consume zoom-reset flags, and duplicate side effects.

The existing `NativeCompletionStructure` correctly extracts a bounded native subset: complete setter, holder ready/count, tab progress=100, pull refresh completion, TabPriority.finished, visible toolbar and progress. It does not synchronize URL/title. In the missing-finished scenario, original start may set a normal page title to null. A current WebView URL/title synchronization is therefore a relevant addition, provided metadata fields and the tab-to-metadata chain are uniquely derived from original getUrl/getTitle write sequences, ownership is checked, and the committed native page/navigation is still current. The data should be read from the active WebView, not copied from stale callback text or the previous page. Do not replay doUpdateVisitedHistory to obtain this synchronization: it also writes browser history and can duplicate entries.

`finished` instruction 42 sets tab.Q=true. Uses in tab detach inspect Q, capture a bitmap, and clear Q/R when n0==1; it appears to mark the tab preview dirty. It is not required evidence for the loading-state repair and should not be copied merely because it is adjacent to other completion writes.

## Required fail-safe properties

- Early native proof must be synchronous and specific to the current WebView and current navigation/page; stale proof must not suppress normal loading.
- Absent/unsupported native features must keep original startup and the existing JS restoration path.
- Post-body completion must not be undone by subsequent original initialization. Completion before original start is insufficient.
- Version handling must identify exact calls and field data flow, unique matches, visibility, receiver ownership, and constants; obfuscated names cannot be extrapolated from 3.83.74 to 3.84.12.
- Never skip a method by broad name/URL pattern and never suppress initialization merely because the user pressed Back; non-BFCache history navigation still loads a new document.
- Existing four user modifications remain enabled; only the already-proven restored navigation may bypass the new-load priority transition.
