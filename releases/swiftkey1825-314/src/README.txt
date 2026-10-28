SwiftKey 1.8.25 / combined 1.0.314: pinned dictionary union

Start from published SwiftKey1.8.24 and combined1.0.313. All upstream dictionaries
are pinned by URL, SHA-256 and byte length in dictionaries/source_manifest.json.
The dynamic JMdict2026-10-28 data and its license are mirrored at an immutable
repository commit; rebuilding does not silently download tomorrow's dictionary.

Build prerequisites: Python3.12, Java17 for source compilation/D8, Java21 for the
Morphe SDK. Set MORPHE_JAVA to the latter java executable. No device APK is needed
for a normal reproduction; the pinned static native ABI evidence is replayed.

python build1825.py --download --input-dir INPUT --tools TOOLS --work NEW_WORK --output DIST
Repeat with a new work and output directory. All six release assets must match.
The source manifest and artifact manifest are outside src/ and pinned for CI.
Python semantic fixtures and publisher boundary tests are part of CI.

Dictionary policy and limitations are detailed in RELEASE_NOTES.txt. Dictionary
source attributions and original notices are appended to words_LICENSE.txt, copied
to the app by the existing loader. Existing license text and all prior candidates
remain. Only candidate identity Unicode NFC and runtime reading normalization
participate in duplicate comparison. No candidate-count truncation is applied.

Runtime changes are confined to WordCandidates capacity constants and three
method bodies: background load (capacity/OOM), Index.fromBytes (capacity), and
Index.words (remove exact-row candidate cutoff). The prefix rank bound remains.
Baseline source is independently compiled and must match published executable
DEX. Only those fields/methods are transplanted, retaining class/method metadata.
The asset size ceiling and two dictionary SHA strings are updated in the one
existing loader class, in Android DEX and JVM form. All unrelated loader classes,
runtime classes, assets, and other app patches remain byte-identical/canonically
identical as appropriate. The existing glide binary is not regenerated.

Public source commit triggers the dedicated Actions workflow only after review.
It performs two builds, source/artifact pin checks and public-byte verification.
Both main/dev feed commits use expected-HEAD leases and preserve other releases.
If either source advances, publication stops rather than replacing newer work.
