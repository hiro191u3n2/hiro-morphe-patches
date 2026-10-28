# homeback132 の適用時検査

The reported failure is the v131 complete-body fingerprint of `LX/003Q;->A04` under a prior patch's mutable class context. Original A04 has 234 instructions and 13 registers. Its three protected regions can be represented as five single-handler entries after `MutableClass` materialization without changing the protected instructions or handler behavior. Piko's navigation patch changes A07 in this class, making that representation relevant even though it leaves A04's instructions unchanged.

The v132 digest coalesces only entries with exactly identical protected start and length, preserving handler sequence, exception types, destinations, and all instruction/register/range checks. It does not merge merely adjacent or overlapping ranges, reorder handlers, or permit edited instructions. All previously pinned original hashes remain unchanged. Incoming classes already mutable at OneBack entry are captured as immutable definitions, so subsequent OneBack edits cannot alter the comparison input through shared references.

The original-APK audit includes `Instagram132MutableContextAudit` for this representation boundary and rejection cases. Actual Piko and Manager-host combinations must be recorded separately in the final QA; this contract note alone is not evidence that a combination was executed. The runtime contract below is inherited unchanged from published homeback131.

# Instagram 452 external-profile route and Back contracts

This note records a narrow static review of the original Instagram 452.1.0.53.64 APK used by this release. The original APK has SHA-256 `22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2` and size 143,237,227 bytes. It is not included in the release.

## Inherited runtime evidence and limits from 1.1.24

Published version 1.1.24 corrected two defects established from the pinned original APK and runtime probes: missing recognition of supported external profile handoffs, and consumption of an unrelated profile's native unhandled Back result. The user's live launch Intent and browser-to-app URI were not captured. The source-level defects do not establish which route caused the reported handset symptom, and the correction still needs a device retest. No physical Android 17 / Samsung SM-S948Q execution is claimed.

## Native route forms and the delegate envelope

The original `AZA` URL routing code can create a profile from three forms. The canonical HTTP(S) form supplies a Parcelable `uri`; the HTTP(S) `/_u/` alias and native `instagram://user` form omit that parser field. The external delegates `X.0Rts` / `X.003Q` retain the original source in an `original_url` envelope when merging the parsed route. Requiring `uri` for all three forms therefore rejected real profile creation routes already supported by the original APK.

The registration hook remains at the verified `X.02HW.A09` profile creation callsite and retains the exact created `UserDetailFragment` object. The revised runtime accepts only these forms, with `NAME` agreeing with the native `InstagramUrlLaunchConstants.EXTRA_USER_NAME`:

| Form | Parser field requirement |
| --- | --- |
| `http(s)://www.instagram.com/NAME/` | A matching `android.net.Uri` in `uri` is required. |
| `http(s)://www.instagram.com/_u/NAME/` | `uri` may be absent; any supplied value must be a matching `android.net.Uri`. |
| `instagram://user?username=NAME` | `uri` may be absent; any supplied value must be a matching `android.net.Uri`. |

The accepted web hosts are exactly `instagram.com`, `www.instagram.com`, and `m.instagram.com`; the final slash is optional. The native user form also permits an empty path or a single `/`. Every form requires a string `original_url`, Boolean `true` in `com.instagram.url.extra.IS_ON_CREATE`, and the appropriate value of `com.instagram.url.constants.ARGUMENTS_KEY_ANALYTICS_MODULE_NAME`:

| Host Activity | Required module |
| --- | --- |
| `InstagramMainActivity` or its subclass | `main_activity` |
| `UrlHandlerActivity` | `url_handler` |
| `UrlHandlerLauncherActivity` | `url_handler_launcher` |

The selected username must agree with the URL path or the native `username` query value. An existing `uri` must have the correct Parcelable type and exactly the same text as `original_url`. A parser result alone, a textual `"true"` in place of a Boolean, a missing/mismatched module, or URL text alone cannot register an entry.

`InstagramUrlLaunchConstants.EXTRA_IS_INTERNAL_LINK` set to true in the route or incoming Intent vetoes registration. The query admission is limited to `igsh`, `igshid`, and `utm_` tracking keys, plus the single required matching `username` parameter for the native form. `utm_source=qr`, unknown query keys, embedded credentials, explicit ports, and fragments are rejected. Native controls for auto-launching a reel, selecting a starting tab, overriding the entrypoint, or opening a creation sheet are also rejected. Unsupported schemes, paths, actions and conflicting route values stay with native navigation.

These fields distinguish the reviewed URL-delegate path from parser-only in-app calls. Internal self-intents can also re-enter those delegates: the envelope is not universal proof that the original sender was a browser and does not exclude every internally initiated route. An explicit internal-link flag still vetoes registration. No particular URI emitted by the user's browser is claimed. Registration alone cannot authorize exit: Activity/Fragment identity, navigation and visibility checks, layers, input state, the before/after snapshots, and the original Back result still apply. IME handling and a native consumed result keep priority.

## Exact profile identity on return

The original profile Back hook applies to `UserDetailFragment`, including internally opened profiles that are different from the registered initial entry. In 1.1.23, `finish(fragment, false)` could consult the entry's state and return consumed for that different fragment, preventing its native caller from completing an ordinary return.

The unchanged version 1.1.24 runtime requires `Entry.profile` to resolve to the exact fragment being finished before using the initial-entry exit logic. A different profile's native `false` remains false, and its native `true` remains true. The existing `begin()` return marker remains active: if the caller pops an internal profile and synchronously dispatches to the newly revealed initial profile, that same Back cannot remove the task. A later independent Back may be eligible under the normal guards.

`Back131InstagramProfileRouteTest` and `Back131InstagramProfileIdentityTest` are mandatory host regressions. They exercise the admitted route envelopes, rejection boundaries and profile identity/return behavior. Their executed results belong in the final release QA; these host models are not a device test.

The inherited runtime contains diagnostic messages under `HiroBack131`. Registration logs only the admitted form number and host module; the exit branch logs a fixed marker before calling the existing terminal handling. Neither message includes the URL or username. A marker records code-path entry, not a verified physical-device outcome.

## Ordinary profile content

`UserDetailFragment` owns a child FragmentManager for its native profile ViewPager. Rejecting every child fragment would reject the ordinary media grid. The helper therefore identifies exactly the selected native tab before checking whether any additional layer blocks exit.

The reviewed chain is `UserDetailFragment.A0b` to `UserDetailTabController`, its `A0A` adapter (`X.0EIp`), and `viewHolder.A0F` (`NestableViewPager`). The controller's final `A0D()` getter reads the pager's current index and the adapter's existing tab cache. The adapter's final `A0J(int)` getter reads its weak-reference cache; it does not create a fragment. The helper also requires agreement with the native adapter's primary fragment and its FragmentManager.

The exact fields, types, modifiers, superclass relationships and getter instruction fingerprints are checked by `InstagramExternalProfileTabsContract`. The helper calls only the verified read-only selection getters. The native primary-fragment setter and scroll-state setter are fingerprinted to establish the meaning of the corresponding fields; the helper does not call those setters.

## Normal grid has no additional fragment wrapper

The original `UserDetailTabController` maps the `profile_media_grid` tab to provider `X.0ENL`. Its `B5A` method creates `ProfileMediaTabFragment`. That fragment's `onViewCreated` looks up the Android list view, uses a `RecyclerView`, sets a `FastScrollingLinearLayoutManager`, and installs its grid adapter. No method in this concrete fragment directly calls `getChildFragmentManager()`.

Its superclass `X.0CTn` only supplies the session delegate. The base `X.05Jd` has no child-manager use in its view-creation lifecycle; its direct child-manager reference is a volume-key forwarding routine, not a child-fragment creation path. The ordinary clips (`X.0BdU`) and reposts (`X.0Bnw`) profile tabs also use RecyclerView content and contain no direct child-manager call.

These observations support allowing the selected grid tab while continuing to reject a visible nested fragment, a nested back stack, a dialog, a pending transaction, saved state, or a moving pager. They are static evidence for the reviewed original APK, not a claim that every server-selected feature or every possible profile child is covered.

## Selection and visibility boundaries

The runtime requires the selected tab to be added, visible and resumed, to belong to the registered profile and its child manager, to share its Activity, and to have its view inside the native pager and profile view. Offscreen retained sibling tabs are permitted only when they do not represent another visible layer. A different visible child or dialog vetoes exit.

The selected grid itself need not intersect the current screen rectangle: the profile header can place ordinary grid content below the fold. The registered profile and its Activity still pass the outer visibility and input-state checks. Missing fields, stale caches, unknown ownership or inconsistent selection fail closed to Instagram's native Back handling.

The host regression fixture contains populated native-pager selection, adjacent retained tabs, nonzero current index, and rejection cases for conflicting selection, moving pager, wrong manager or parent, additional visible content and nested state. Final build and original-APK audit results are supplied separately in the release QA files. Physical Android operation has not been verified in this environment.

## Verified existing Main modal route

When Instagram is already running, the native external-profile navigator can use the active Main modal's child manager. The reviewed path is `X.02HW.A09` through `X.01PZ` to the modal service's `A06()` manager getter. The active modal is the exact `X.03CG` referenced by `InstagramMainActivity.A09` / `X.0A4v.A06`. The profile is selected in that modal's native content container, `0x7f0b0aba`.

Native Back reaches `InstagramMainActivity.A1u`, then `X.0A4v.A0B`, then `X.03CG.onBackPressed`. The modal checks its own layer controller before delegating to the selected profile, and can pop its child stack afterward. The external-profile native hook still runs the original profile Back first. It can exit only for the registered initial profile when that original handler did not consume Back and both the before/after guards permit exit.

`InstagramExternalProfileModal` requires the current service reference, modal owner, root-manager membership and selection, direct profile-parent relationship, selected child-manager relationship, lifecycle, visible view ancestry and stable manager state. A second modal, another visible sibling, a dialog, pending operations or inconsistent selection veto recognition. Historical parent back-stack entries are permitted because the native navigator replaces the previous child while pushing the external profile; a deeper selected child remains ordinary native navigation.

The ordinary Main tab can remain visible behind this separate modal. Native insertion either detaches prior same-container content or conditionally limits the other-container source to STARTED. The exact current Main background can therefore be STARTED or RESUMED. It is recognized using the native Main/controller selection identity, Activity/root-manager relationship and disjoint view ancestry. Nonselected retained native tabs additionally require an existing valid cached tab key and a non-resumed state. Other visible roots are not treated as background.

The modal helper checks every root tab's cached key before calling the native selection chain, because the native lookup can otherwise initialize a lazy value. The lookup and lazy getter have complete instruction fingerprints. A live registered modal whose selection cannot be verified yields an authoritative UNKNOWN snapshot: eligibility checks skip the underlying Main selection and generic fragment/module scanning, and leave Back to the original app. An old or detached modal reference does not suppress the existing ordinary Main behavior.

The extension deliberately excludes two-pane/right-pane flags, source-FragmentManager routing, and zero-child-depth native auto-close configuration. The `InstagramActivityLayers452.inspect(Activity)` default behavior is retained; only the registered modal path supplies the exact permitted content modal to the new overload. That overload still checks the modal's own layer controller, Activity layers and dialogs. The specialized result remains OTHER and cannot grant generic Home/Reels exit permission.

`InstagramExternalProfileModalContract` fingerprints the native selection chain, profile-navigation constructors, modal insertion and native Back callers without editing those methods. Host regressions cover valid root-modal routes, native input/return identity, both selected-background lifecycle variants, cached tabs, rejected modal configuration/ownership/state, and refusal without lazy getter or module reads. The release QA records the final executed test inventory. Own-profile alternate factories, custom URI schemes other than the strictly admitted `instagram://user?username=NAME` form above, and unverified restored/nested modal ancestry remain outside this extension.
