# Discord Send Button 1.0.1 / Hiro 1.0.376

This revision makes the patch discoverable for original `com.discord` 351.11 and
applies it together with Venus 1.21.1. The old Manager instructions for adding a
patch to an already-patched Venus APK were incorrect and are withdrawn.

## Build

Use Java 21.0.13 and the exact tool hashes recorded in `verification/QA_build376.json`.
The immutable Hiro 1.0.374 baseline contains every unchanged non-Discord provider.
The new provider replaces the previous Discord implementation; the other 428
loader classes and 354 archive entries remain unchanged, including versus 1.0.375.

```sh
python3 src/build.py --base /path/to/Hiro_Morphe_Patches_v1.0.374.mpp --tools /path/to/tools --sdk /path/to/morphe-dev19.jar --out /path/to/fresh-output --timestamp 2026-11-16T07:56:06
```

`tools` contains `ecj.jar`, `d8.jar` and `classpath.jar`. The SDK is Morphe Desktop
2.2.0-dev.19 / Patcher 2.4.0-dev.8. Compile-only API copies never enter the MPP.
Two independent builds produced identical MPP bytes. The minimum Patcher version
is 2.3.0, verified by an actual original-APKM combined application and rebuild.

## Actual APK application

Use an unmodified Discord 351.11 - Stable (351011) APKM and the official Venus
1.21.1 MPP. The official MPP identity is recorded in the verification directory.
Select Venus settings and the desired Venus features together with the Hiro item.
For the full 79 Venus patches, enable its otherwise optional Custom branding:

```sh
java -Xmx2g -jar /path/to/morphe-dev19.jar patch -p /path/to/patches-1.21.1.mpp -e 'Custom branding' -p /path/to/Hiro_Morphe_Patches_v1.0.376.mpp -e '送信ボタンの表示消失を修正' --bytecode-mode=FULL --unsigned -o /path/to/output.apk /path/to/Discord-351.11.apkm
```

CLI `-e` options belong to the preceding `-p` bundle. The recorded full runs use
`--exclusive` and explicitly list all 79 Venus items before the Hiro bundle.
APKM merging creates temporary files beside the input, so use an input link in a
scratch directory. Do not distribute the supplied APKM or the unsigned test APKs.

`tests/integration-check.py` compares equivalent Venus-only and Venus+Hiro outputs.
For both MPP forms, the full configuration, the minimal configuration and the
Patcher 2.3.0 configuration, all 12,641 other APK entries are byte-identical.
Only four branch bytes and the SHA-1 footer in the HBC asset differ.
Compare outputs produced by the same Patcher version: 2.3.0 and 2.4.0-dev.8 encode
resources.arsc differently even without Hiro.

## Implementation and guards

A private raw-resource dependency acquires the bundle file without changing it.
The public bytecode finalizer checks Venus's complete asset loader and OTA guard
methods in the final mutable DEX, then validates HBC structure, the global entry,
the module factory and the complete two target functions before atomic replacement.
Hiro does not change DEX. The two leaf edits commute with Venus's final prelude
injection. Original Discord without the required Venus loader is rejected.

Compile `tests/GuardTest.java` and `tests/LoaderFixtureProbe.java` separately from
the provider, using the produced MPP and SDK as the classpath. Run `GuardTest` with
the supplied Venus bundle, stock bundle, stock base.apk, supplied Venus APK and a
fresh work directory; set `-Dexpected.provider=/absolute/path/to/produced.mpp`.
The recorded final-artifact test passed 126 assertions, including unchanged input
on rejection, malformed structures, mismatched runtime, partial/repeated patches,
and absence of temporary files after failure.

`tests/README.md` describes the 256-state host semantic checks for both original
and supplied Venus inputs. These mock React/React Native and do not execute Hermes.

No Android device/emulator UI test or installation test was performed. Structural
checks do not certify arbitrary unknown modifications elsewhere in an input APK.
