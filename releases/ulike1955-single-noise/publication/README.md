# ULike 1.9.55 / Hiro Morphe 1.0.188

NR1–NR4 use the single saved image after beauty and colour correction. The
published 1.9.54 / 1.0.187 baselines and toolchain are immutable and hash-pinned.
The specific `BurstCapture1933.beginImage` admission returns false before reading
its arguments. Remaining capture members and unrelated runtime classes are
independently compared after DEX serialization.

The new quality algorithm intentionally changes output pixels. Fresh Java and
C/JNI tests check noise reduction, periodic texture retention, colour-on/off,
noise-off, streaming halos, source ownership, geometry and failure rollback.
Five native/Java parity cases compare exact output. This does not establish
physical Android speed or image quality. Original APKS application and physical
Galaxy tests are unperformed.

All eight inherited native payloads and every non-ULike application resource
remain byte-identical. One optional arm64-v8a library is appended; the installer
retains the eight old rows and transactional safeguards. JNI exports, 16 KiB
ELF alignment and runtime dependencies are checked. The Java path handles an
unavailable native library. New integer NR excludes high-depth, wide-colour and
gainmap inputs before the existing fallback.

The scripts run in this order:

```sh
python3 src/build1955.py --input INPUT --tools TOOLS --ndk NDK_R27C --jdk JDK21 --work BUILD --output DIST
python3 src/validate1955.py --build BUILD --dist DIST --input INPUT --output VALIDATION.json
python3 src/finalize1955.py --dist DIST --source src --validation VALIDATION.json --manifest GENERATED-MANIFEST.json
python3 publication/publish1955.py --dist DIST --repo REPOSITORY --expected GENERATED-MANIFEST.json --local-only --baseline INPUT/Hiro_Morphe_Patches_v1.0.187.mpp --standalone-baseline INPUT/ULike_HQ_Texture_Online_v1.9.54.mpp
python3 publication/publish1955.py --dist DIST --repo REPOSITORY --expected GENERATED-MANIFEST.json --preflight-only
python3 publication/publish1955.py --dist DIST --repo REPOSITORY --expected GENERATED-MANIFEST.json
```

`INPUT` contains the two baseline MPPs. `TOOLS` contains the pinned Morphe,
Android SDK and D8 jars. Use Temurin 21.0.8+9-LTS, Android NDK r27c revision
27.2.12479018 and a host C compiler with the executing JDK's JNI headers.
Build directories must be fresh. `manifest.json` is the reviewed input and
resource declaration; finalization binds actual host results and all six assets.

The checked-in workflow and its archived copy are identical. A push to main
that changes `.github/workflows/ulike1955-single-noise-publish.yml` triggers a
fresh rebuild, verification, preflight and publication; workflow dispatch is
also supported. Actions are pinned to exact commits. Both Manager feeds advance
atomically from leased HEADs to direct descendants; their historical descriptions
and changelogs are preserved. No unrelated app update scope is added.

Publication creates a draft Release, uploads and re-downloads all assets, publishes
and verifies public hashes, creates immutable MPP URLs, updates main/dev together,
then commits and uploads a verified receipt. An exactly completed repeat only
reads and verifies the existing receipt and release. Partial or mismatching
publications fail closed for inspection. `--local-only` and `--preflight-only`
perform no remote writes. User photos, original APKs and signing keys are excluded.

Morphe Managerのパッチソース更新後、未改造 ULike 5.6.2（740）へ再適用して
作成されたアプリを更新インストールしてください。ソース更新だけでは
インストール済みULikeは変わりません。
