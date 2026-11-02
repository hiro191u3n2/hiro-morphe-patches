# Berry BFCache restore repair 1.0.3 / combined bundle 1.0.329

This publication packages the reviewed five-patch Berry candidate into the existing
Hiro Morphe Patches update channel. The standalone candidate remains byte-identical
to the file supplied for device checking. Rendering improvement has not been
confirmed on a physical Android device.

The confirmed defect is a native lifecycle mismatch: a BFCache restore starts
loading but does not deliver the ordinary onPageFinished callback. The repair
requires both a persisted pageshow marker and a visible native commit before
completing the Berry/foreground-helper state and requesting a synchronized redraw.
It does not synthesize the full onPageFinished callback.

The active combined baseline is 1.0.328, SHA-256
`c2c5ae2773c4855aca14bbe6f232718326c2045a923d453f49c8eb2a97348f4e`.
The standalone repair is SHA-256
`ec31d178533b026302ff348c4e119b4242a3527c1f20c35f15ae8fbae82eae2c`.

`build/build_merge.py` preserves all existing JVM provider and resource entries,
compiles only the new provider classes, and merges their DEX definitions into the
existing provider DEX. Existing DEX-only providers must also be retained; rebuilding
the baseline solely from its JVM class entries would omit those providers.
`build/MergeDex.java` verifies the canonical definition bytes of every existing DEX
class before and after the merge. Independent DEX and real-APK application reports
are included in the frozen publication payload.

The local application tests use the official Manager 2.4.0-dev.4 / Patcher
2.2.0-dev.2 host. An actual unmodified 3.83.74 input is tested with all five patches.
The supplied modified 3.83.93.1 input was checked using exactly the same repair
implementation through a QA-only entry point. An unmodified 3.83.93.1 input is
unavailable here, so a full-five-patch application from that exact original version
is not claimed. Original APKs, signing keys and browsing data are excluded.

The push workflow repeats the combined merge twice, checks frozen artifact hashes,
verifies release assets and immutable download URLs, then atomically advances the
main/dev Manager feeds. The unrelated ULike policy is checked and preserved byte
for byte. Concurrent feed/branch changes stop publication for review.

Installation and device-test instructions are in `payload/RELEASE_NOTES.txt`.
The source archive retains the original candidate-build report; its statement that
publication had not yet occurred describes the earlier local candidate build.
