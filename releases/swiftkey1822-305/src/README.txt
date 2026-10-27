SwiftKey 1.8.22 / combined bundle 1.0.305 — native emoji footer

Requested layout
The emoji panel bottom row becomes abc, Enter, Delete, and the existing scrollable emoji category strip. Only the emoji panel is changed.

Architecture
EmojiFooter.install(je0.c, wb0.x) is called at the single successful return of the native footer constructor. The helper validates the emoji panel discriminator, preserves original abc/DeleteKeyButton/SwiftKeyTabLayout identities, and adds one Enter control.
The Enter control calls wb0.x.y(new lh0.a()), the same onEnterKey event used by the normal native Enter actor. It does not directly insert a newline or guess editor actions. This retains the native composition/editor handling.
The native DeleteKeyButton retains its own touch/long-press machinery and native category listeners remain installed. The layout helper is additive; no global view traversal or continuously polling watcher is introduced.

Preservation contract
Baseline standalone: SwiftKeyBeta_v2_SamsungEmoji_v1.8.21.mpp
SHA256: 000076673c9253acb1edb3a11ebf9889396c4977d8ea7788e52275b16190d774
Baseline bundle: Hiro_Morphe_Patches_v1.0.304.mpp
SHA256: 1523f42ee8597475c8d4d8b1625820fd874408090881c88f9e0107583fc00d7f
Baseline runtime SHA256: e436cdc71432ed7066335d9516d5ac2d233f4a39f4014ab7ce3787ba7d3a9593
Every one of the 81 existing runtime classes is preserved under canonical DEX comparison. Only EmojiFooter and its nested helper classes may be added. All MPP dictionary/resource/unrelated-app entries are preserved. Both loaders add EmojiFooterPatch and two bridge calls in the existing main patch; every unrelated loader class is preserved.

Build
Use Java17 for source compilation and D8, and set MORPHE_JAVA to Java21 for the Morphe tools.
python build1822.py --download --input-dir INPUT --tools TOOLS --work WORK --output DIST
The script pins all baseline/tool bytes, compiles an additive runtime, validates pinned native ABI evidence, runs host tests and native hook tests, updates both loaders, and runs the real patch selection probe with both supported Morphe tool archives.
--compile-only creates the runtime for the independent native ABI audit without producing release-ready artifacts.

Evidence and limitations
QA_SwiftKey_v1.8.22.json records the behavior, native hook, ABI, runtime preservation, and real loader reports. The source archive includes the exact build/patch/runtime/test source files and generated helper smali.
The native reference archive is the existing local modified 9.13.16.4 analysis APK, SHA256 e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7. It is not claimed to be an untouched original APK.
Host tests and static ABI/native hook analysis do not constitute handset UI/ART execution. Device-tested and clean-original-APK-apply-tested remain false. No diagnostic log function is reintroduced.

Publication
The workflow builds twice and requires both outputs to match externally reviewed manifest.json pins. publish1822.py verifies exact archive deltas and source pins before creating a release. Both Manager feeds use expected-head leases and an atomic update; a newer active baseline stops publication instead of overwriting it. ULike active policy and other application metadata are preserved.
The source and manifest are staged before the scoped workflow file push, keeping the automatic publication trigger explicit and reviewable.
