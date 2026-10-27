SwiftKey 1.8.23 / combined bundle 1.0.306 — recent emoji continuous slide picker

Requested interaction
An upward flick from the dedicated face/emoji key opens a small recent-emoji picker. Keep the original finger down, slide over an emoji to highlight it, and release to insert exactly one selection and close. Releasing outside an emoji cancels. The picker shows up to 12 native recent emoji in four columns and up to three rows; empty history displays a message without substitute emoji.
The existing five-level upward-flick sensitivity is reused. Ordinary emoji-key tap and stationary long press retain their published panel/toolbar actions. The picker is drawn in the keyboard's ViewOverlay and does not take focus or replace the original touch target.

Architecture
EmojiRecentsSlide.handle(xd0.d1, lh0.a, MotionEvent) runs before the existing JapaneseGlide dispatch. Non-consumed events continue through the complete original dispatch body. The existing init and reset entry points call their original bodies and the new helper once each.
EmojiToolbar gains only emojiHoldMillis(fc0.n2). It verifies the dedicated key by the already-installed private LongPress actor and reads the exact native hc0.k.a deadline. No label matching or coordinate guesses identify the key. Existing tap and long-press method bodies remain unchanged.
EmojiRecentsBridge.snapshot(wb0.x, int) obtains the active keyboard's history from wb0.x.t -> fb0.b0.a -> ne0.f0.f. ne0.e0.b() supplies the native newest-first list. This model is constructed with the keyboard; the native lazy provider reads persisted recents when first needed, without requiring the full emoji panel to have been opened.
Complete ne0.g0.getContent() strings are copied for one gesture. Selection calls wb0.x.G(breadcrumb, text, oz.j2.a RECENTS), then xc.d(history, 14).l(new ne0.p0(text), 1), matching native insertion-before-history ordering. The native history and usage controllers retain their incognito guards. No direct InputConnection.commitText fallback, alternate preference store, or reflection is introduced.
The snapshot permits one insertion attempt and checks native model identity. The gesture additionally guards the current editor, InputConnection, layout, pointer, and native long-press deadline. Cancellation drains the already-captured pointer stream so its later release cannot open the ordinary panel.

Preservation contract
Baseline commit: eea97cb240c4306a3b1700c4ef4ce27cf14897f4
Baseline standalone: SwiftKeyBeta_v2_SamsungEmoji_v1.8.22.mpp
SHA256: 322f2ba927a4c53861e23197155e46af32e6e37017a7b6518f081a01ff8a9052
Baseline bundle: Hiro_Morphe_Patches_v1.0.305.mpp
SHA256: 52fe856d66e6b4b89a2280ccc237448905c1b1f2f67c2fea715b0a6d5acaf29d
Baseline runtime SHA256: eef461b0d8825be2c36d88ef694f62c7f8ec0d756bd8500022738206c0abcf06
The baseline has 84 runtime classes. 82 remain unchanged under canonical DEX comparison. EmojiToolbar adds one method; JapaneseGlide adds three wrappers while retaining the original bodies under release-specific names. Restoring those original names and removing only the additions reconstructs the original classes exactly.
Seven EmojiRecents helper types are added, including javac's synthetic nested-constructor access marker, for 91 total runtime classes. Their merged implementations must match the separately compiled overlay.
Both MPP archives change only META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe. No ZIP entry is added or removed. All 13 existing SwiftKey JVM loader classes and both Android classes.dex files are byte-identical to the baseline. Their Android class inventories remain 280 in the combined bundle and 21 in the standalone. All dictionaries, resources, existing footer actions, unrelated-app entries, and diagnostic-removal behavior are preserved.

Build
Use Java17 for source compilation and D8, and set MORPHE_JAVA to Java21 for the Morphe tools.
python build1823.py --download --input-dir INPUT --tools TOOLS --work WORK --output DIST
The script pins baseline/tool bytes, compiles the new runtime helpers, merges only the reviewed additions/wrappers, validates native ABI and history-review source pins, runs host regression tests, and invokes the real patch-selection probe with both supported Morphe tool archives.
--compile-only creates the runtime for the independent native ABI audit without producing release-ready artifacts.

Evidence and limitations
QA_SwiftKey_v1.8.23.json contains host behavior, native ABI, history-route review, runtime preservation, and real loader reports. The source archive includes the exact build/runtime/test/publisher sources and the generated changed/new runtime smali.
The reference archive is the existing modified 9.13.16.4 analysis APK, SHA256 e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7. It is not an untouched original APK. Native ABI evidence is generated independently against that archive and the exact compiled runtime; CI verifies its pinned report, verifier source, and runtime hashes without redistributing or re-running the APK audit.
Host test doubles validate gesture behavior and native delegation, including the native privacy contract. Host tests and static ABI analysis do not establish handset rendering, touch/ART execution, or a complete patch application to an untouched original APK. Device-tested and original-APK-apply-tested remain false. No diagnostic log function is introduced.

Publication
The workflow builds twice and requires both outputs to match the independently reviewed manifest.json artifact pins. publish1823.py verifies exact archive deltas, source pins, runtime inventory, method-preservation proofs, native/history test contracts, unchanged loaders, and the documented verification scope before creating a release.
The active Manager baseline must still be 1.0.305 with the pinned download URL. Both main and dev feeds use expected-head leases and an atomic update; a newer active baseline stops publication. ULike active policy and other application metadata are preserved.
Source and manifest staging precedes the scoped workflow-file push. Offline publisher_tests.py exercises the publication guards without contacting or writing to GitHub.
