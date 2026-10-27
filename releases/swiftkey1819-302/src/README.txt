SwiftKey Beta v1.8.19 / Hiro Morphe Patches v1.0.302

Purpose
Follow up on the candidate-list jump reported after 1.8.18, and select a configured
Japanese layout when a normal text keyboard session starts. The candidate change
addresses result types, delayed smaller requests and changing result membership
that the previous append/anchor rules did not cover. Improvement on the user's
handset remains unverified.

Candidate updates while the expanded list is visible
The native expanded panel subscribes to every prediction result type. Therefore
an incoming result need not carry the EXPANDED label merely because that panel
is visible. Position preservation now accepts ordinary result types for a live,
visible expanded owner when the full existing semantic query comparison passes.
The query comparison itself remains strict. Changed composing text, context,
input mode, native ownership or unsupported layout state retains native behavior.

A result requested with a smaller candidate limit can arrive after the displayed
same-query result. Once the list has moved away from the beginning, such a late result must not
replace the larger active request and shrink the list under the user. The guard
skips that stale replacement only when the same query, current expanded owner
and native/pending viewport state agree. Initial and reopened views still accept
their initial results; no stale-result suppression is carried into a new view.

For a valid same-query replacement, candidates still present keep their relative
displayed order; candidates no longer returned are removed, and new candidates
follow the survivors. Every retained entry is the fresh incoming object, including
its current commit and long-press information. This also covers same-size and
shrinking replacements. A narrowly scoped native early-return bridge ensures that
an equal-size reorder stabilized to the old order does not accidentally discard
its fresh selection metadata.

The existing anchor transaction continues to cover both the adapter and panel
reset listener. The first surviving visible candidate retains its decorated
pixel offset. An owned pending viewport can be carried across updates before the
next layout. The native reset bookkeeping and span measurements still run, while
the same-query panel reset does not jump to zero or stop an ongoing fling.
A completed layout may emit onScrolled(0,0); the update's owned zero-delta callback
rebases the native request watermark so a layout refresh alone does not trigger
an extra fetch. Actual scrolling keeps the native request-more behavior.

Candidate-only ItemAnimator suppression from 1.8.16 remains active. Native
reset=true bypasses the append helper, performs its native refresh notification,
and then reaches the snapshot hook before deferred layout. Compatible append
updates continue to use fresh payload rebinds and inserted suffix notifications.
This release does not alter the scroll gesture recognizer or global animations.

Japanese at the start of a normal text session
When a normal or message text keyboard session starts, select an enabled Japanese
ROMAJI or HIRAGANA layout with an available language pack. If the keyboard was
previously English, closing and reopening it starts in Japanese again. The
configured QWERTY/kana layout remains configured as before; an already selected
Japanese layout is retained.

The hook changes the selected language only for a new input field or a keyboard
reopening; it does not override an English language selection during an active
restart of the same input session. The native startup behavior of a Japanese
layout's ABC_SECONDARY mode remains unchanged and may reset that secondary mode.

The policy leaves native email, URI, password, numeric and date requirements,
non-text editors and IME_FLAG_FORCE_ASCII unchanged. It also leaves native behavior
unchanged when no enabled Japanese layout has an available language pack. Selection
uses SwiftKey's language selection and layout rebuild path. It injects no text or
key events and adds no diagnostic logging.

Preserved behavior
The published 1.8.17 punctuation behavior is retained from the exact 1.8.18
baseline: ordinary taps on 。 and ー input those characters; their upward flick
and alternate actions select full-width ？ and ！ respectively. Secondary labels
match those alternatives. The punctuation normalizer and working upward-flick
recognizer are unchanged.

Candidate expand and collapse controls remain on the physical left. A normal
emoji-key short press opens emoji; long press toggles the toolbar, and a short
press while the toolbar is open closes it. Key swaps, input learning, dictionary
resources, Samsung emoji, newline behavior, paste/delete protections and existing
settings remain intact. The upward-flick diagnostic preference, collection and
copy functions stay removed. Other apps in the combined MPP are preserved.

Runtime and loader scope
The CandidateScroll runtime family may change, and the JapaneseStart runtime
family is added. Every other existing runtime class is compared against the exact
published 1.8.18 baseline in canonical smali. The QA report records the actual
changed and added inventory, and no existing runtime class may disappear.

The existing CandidateScrollPatch helper changes. The main JapaneseSamsungEmojiPatch
loader gains only the two reviewed JapaneseStart bridges, and JapaneseStartPatch
is added in both loader formats. The exact changed MPP entries are
META-INF/MANIFEST.MF, extensions/swiftkey_japanese.mpe, classes.dex,
app/hiro/swiftkey/patches/CandidateScrollPatch.class and
app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class.
The sole added archive entry is
app/hiro/swiftkey/patches/JapaneseStartPatch.class.
No archive entry is removed. The other nine existing SwiftKey JVM patch helpers
stay byte-identical. Standalone and combined packages have their own Android
loader DEX; their SwiftKey helpers and selected runtime must agree. Dictionaries
and every unrelated app payload remain byte-identical.

Inputs and reproducibility
The baseline is the exact published SwiftKey 1.8.18 and Hiro bundle 1.0.301 at
immutable commit 177e441b49f43f055758d32fba909b03b9c88a4b.
  standalone SHA256: 35251f48280adc69108d0218b3a8f464686deeb44314f59195698bc6bc8583ef
  bundle SHA256:     3fbc234e9248d0e2682ee996a31cd37d5b0b079a080ad2b8fc2b65ce6e65f163
Both inputs, build tools and reviewed source files are pinned. The builder rejects
unexpected archive changes and changes to unrelated runtime classes.

Verification
Production candidate code runs in host fixtures for native provider notification,
result type and request ordering, changing candidate membership, both listener
orders, equal-size metadata updates, repeated pre-layout anchors and layout-only
callbacks. JapaneseStart runs in lifecycle fixtures covering reopen, manual
switches, available configured layouts and special editor requirements. Reports
carry exact production-source hashes and actual scenario/assertion counts.

The static native request review in tests/native_query1819_review.json confirms
that ordinary ROMAJI/HIRAGANA requests select JAPANESE, that history/context use
semantic equality, and that the native expanded panel accepts all result types.
It does not claim observed device request values or a confirmed device root cause.
Native hook reviews and strict ABI checks are tied to the exact merged runtime,
source files and pinned archived target APK. The candidate and JapaneseStart ABI
reports are tests/native_abi_review1819.json and
 tests/native_japanese_start_abi1819.json.
Native hook probes run with both the old and current Morphe APIs. Actual Morphe
loaders must select the packaged extension and preserve patch registrations and
dependency structure.

Build with Java 17 for compilation/D8 and Java 21 for the Morphe loader:
  MORPHE_JAVA=/path/to/java21/bin/java python build1819.py --download \
    --input-dir input --tools tools --work work --output output

The --compile-only mode prepares the exact extension for independent ABI review.
The workflow verifies source pins and offline publication invariants, then runs
two independent complete builds. Both must match all six reviewed artifact hashes
and the preservation contract before publication.

The publisher verifies uploaded release bytes and immutable MPP downloads before
advancing main/dev Manager feeds atomically with expected-head leases. It then
checks committed metadata, unchanged ULike policy and public branch content.
Public raw URLs may lag after a commit; only that freshness check has a bounded
retry, with a fresh query and both head leases checked on every attempt. Exact
feed/changelog bytes remain required. Persistent mismatch or concurrent changes
stop completion. A verified publication receipt is saved to both branches and
uploaded after those checks pass.

Scope and installation
Target: com.touchtype.swiftkey.beta, 9.13.16.4 (1236271168).
Update the Manager source to 1.0.302 and apply the SwiftKey patch to the unmodified
target APK. Install with the same signing settings. Updating the source alone does
not replace the installed keyboard. No uninstall or data clearing is required.

Host checks and static native inspection do not establish handset scrolling
feel, frame timing, Japanese-start behavior on a handset or a complete
clean-original-APK patch application. Those remain unverified. The archived APK
used for native inspection is a previously modified archive, not proof of a
clean-original-APK apply test.
