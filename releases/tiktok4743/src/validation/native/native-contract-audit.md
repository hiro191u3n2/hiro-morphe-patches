# TikTok 47.4.3 / 47.5.3 native UI contract audit

The new release selects an independently verified native contract from the original
APK's package and version. It does not reuse 47.5.3 obfuscated names on 47.4.3.
This evidence covers the five UI patches in `TikTok4753LegacyPatches`; privacy and
OneBack have separate audits. The retained runtime namespace is not a version check.

## Inputs

| Item | TikTok 47.4.3 | TikTok 47.5.3 |
| --- | --- | --- |
| Package | `com.zhiliaoapp.musically` | `com.zhiliaoapp.musically` |
| Version code | `2024704030` | `2024705030` |
| SHA-256 | `af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b` | `6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e` |
| Original classes | 475,931 | 480,524 |

The 47.4.3 inventory contains 56 root DEX entries and no duplicate class descriptors.
No TikTok APK or extracted TikTok DEX is distributed in this source release.

## Native target mapping

Obfuscated owners below use their DEX descriptors. Both versions retain the same
stable `Aweme`, `FeedSkylightBubbleAssem`, and `InteractInfoAreaComponent` class names.

| Feature / method | 47.4.3 | 47.5.3 |
| --- | --- | --- |
| Left container ID method | `InteractInfoAreaComponent.p1()` | `InteractInfoAreaComponent.E1()` |
| Left lifecycle | `InteractInfoAreaComponent.onViewCreated(View)` | same |
| Repost binding | `UpvoteVideoAssemNew.xa(Object)` | same |
| Legacy anchor selection | `LX/07HS;->LIZIZ(...)String` | `LX/07Qm;->LIZIZ(...)String` |
| Bottom CTA selection | `LX/07Oc;->LIZJ(String,String,Priority)Set` | `LX/07Qi;->LIZJ(String,String,Priority)Set` |
| Modern anchor factory | `LX/0Dsv;->LIZIZ(Aweme,Z,LX/0Dsj;,LX/0Dbz;)List` | `LX/0BKK;->LIZIZ(Aweme,Z,LX/0BK9;,LX/0ApU;)List` |
| Progress / drag predicate | `LX/06tY;->LIZJ/LIZIZ(Aweme)Z` | `LX/06Xo;->LIZJ/LIZIZ(Aweme)Z` |
| Seekbar orchestration | `LX/06qw;->LJI(Aweme)Z` | `LX/06WL;->LJII(Aweme)Z` |
| Inverse experiment | `LX/09X8;->LIZ(Aweme)Z` | `LX/09jc;->LIZ(Aweme)Z` |
| Seekbar view / cached show type | `LX/06oF;->setSeekBarShowType(I)V`, `LLJJJIL:I` | `LX/06V9;->setSeekBarShowType(I)V`, `LLJJJIL:I` |
| Drag experiment boundary | `LX/09X3;->LIZ` | `LX/0B1p;->LIZ` |
| Final retained protected-video exclusion | `LX/09MK;->LJFF(Aweme)Z` | `LX/09Hp;->LJFF(Aweme)Z` |
| Thumbnail shared gate | `LX/06qw;->LJII()Z` | `LX/06WL;->LJIIIIZZ()Z` |
| Thumbnail availability orchestration | `LX/06qw;->LJIJI()Z` | `LX/06WL;->LJIJJ()Z` |
| Actual thumbnail data check | `LX/0F2U;->LIZ()Z` | `LX/0P1m;->LIZ()Z` |
| Thumbnail initializer | `LX/06s5;->invoke(Object)Object` | `LX/06Wk;->invoke(Object)Object` |
| Concrete thumbnail loader | `LX/0F2V;` | `LX/0P1n;` |
| Stories toggle / scene | `FeedSkylightBubbleAssem.ar(ZZ)V`, `Yq()I` | `FeedSkylightBubbleAssem.br(ZZ)V`, `Vq()I` |

The modern factory is located by four independent body anchors, with exactly one
match required: the `Aweme.getAnchors` call, the component-key lookup, an ArrayList
copy constructor, and a `VideoFeedMultiTagAssem` field reference. Removing either
of the first two anchors while retaining the method descriptor is rejected.

## Preserved behavior and concrete changes

- Left information and Stories: every original lifecycle instruction runs before
  its exit hook. The bound container is obtained from the original accessor at the
  exit; the incoming View argument is not assumed to remain intact. Both actual
  methods preserve their `this` register throughout their original body. An
  overwritten-register fixture is rejected.
- The 47.4.3 left lifecycle has branch blocks after its first return. The audit
  walks the whole method and verifies every original branch destination, rather
  than treating that return as the end of the method.
- Stories hiding applies only to scene 2, the following scene identified by the
  original scene predicate and following-skylight experiment. Other scenes enter
  the original animation method and return normally after full initialization.
- Seekbar display states 3 and 4 map to state 0. Requests for state 0 invalidate the
  stored equality cache before the native setter so an unchanged cached value
  cannot bypass rendering. Other requested states retain the native setter path.
- The drag edit occurs after all eight native exclusion calls and before the
  experiment/server-control restriction. LIVE, photo-mode, promoted-ad, protected
  and unsupported video exclusions retain their original false branches. The
  original enabled-experiment path already allows a normal video with a null
  VideoControl. The emitted predicate is interpreted across experiment, control,
  draft flag, null-item and every native exclusion combination.
- The preview edit opens the gate shared by the actual loader initializer and
  availability orchestration. The initializer and data checker are unchanged;
  videos still need real BigThumb entries with image URLs and image URIs.
- Legacy and modern anchor renderers, bottom CTA selection and repost binding are
  each changed at their concrete native method. No missing target is accepted as a
  successful zero-change patch.

## Fixed nonzero-offset label defect

The stronger output audit found an incorrect branch in the previous 47.5.3
release's Stories exit hook: the `if-ne` at code-unit offset `0x107` targeted
`0x00b`, while the intended final return was at `0x110`. A snippet-local label
inserted partway through the original method had retained the snippet's address.

The new release inserts the hook and then binds its conditional branch directly
to a label in the actual mutable method. The same correction applies to both APK
versions. The other middle-of-method edit adds a constant and return without a
branch. The entry hooks use either an origin-zero local label or an external label
bound to the existing native entry instruction.

`LegacyNativeAudit original ORIGINAL.apk` now applies the real lifecycle mutator
to both actual lifecycle methods, serializes the changed methods to DEX, reads
them back, and validates the original instruction and branch mapping. A negative
fixture that sends the Stories branch back into initialization must fail. The
full APK output audit also checks the Stories toggle and seekbar setter's exact
branch destinations.

Recorded results are in `original-4743.log` (837 assertions) and
`original-4753.log` (781 assertions). These are input-contract and serialized
lifecycle regression checks. The release's separate full-APK QA record covers
application of the complete selected patch sets. Neither result claims testing
on the user's physical Android device.
