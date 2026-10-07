# ULike 1.9.33 burst processing publication

The selected proposals are 47 (same-exposure multiframe noise reduction), 50
(motion-aware dark-scene fusion) and 60 (image-processing optimization). The
standalone patch and integrated bundle must be validated before any publication.
The publisher retains the prior release's exact-byte checks and atomic main/dev
Manager-feed update procedure. It changes only ULike.

## Pinned inputs

The approved ULike baseline is 1.9.32, 729478 bytes, SHA-256
`010a8dcaa1ba37467fafe92183d19930f59f87dea3e93dfe94cc84825c80b2d0`.
The initial integrated baseline is 1.0.165, 17376489 bytes, SHA-256
`de603911aa3608736e8246b6ffdd89f234d2b0979b66bbacb71db13ba7ea873c`.
Both immutable input URLs use distribution commit
`c9706ca15e4e2f2f75ec3322c0a34b8719e2b987`.

The manifest supplies the reviewed current bundle. If it advances, rebuild and
revalidate against that exact current bundle and increment its version once.
Do not update either Manager branch with an outdated integrated bundle.

Build with Temurin 21.0.8+9; GitHub setup-java's catalog spelling is
`21.0.8+9.0.LTS`. The existing three JAR pins remain mandatory:

| File | SHA-256 |
| --- | --- |
| morphe.jar | 82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c |
| android.jar | 4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b |
| d8.jar | 305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5 |

The verified tools are in Actions run `37577403294`, artifact `11463173899`,
named `ulike1932-quality-verified-publication`, under `staged/tools/`.
The artifact expires on 2026-11-06; this workflow preserves the same JARs again.
The previously reviewed Linux x64 JDK archive SHA-256 remains
`f2dc5418092c43003db8f9005c4a286e1c0104fea96ccdd49e8ebd037cac9219`.

The source contains a 1076-byte native-method seed extracted from the pinned
original APKS. Its SHA-256 is
`6b0c2a6af686d764a1bb3bab8ab4405b760c07f7ffdcbb0d645d9b2a72eb902a`.
It contains only the original `a$g.onImageAvailable(ImageReader)` method needed
for the full-resolution still-reader hook. The extraction script requires both
the original APKS and approved standalone MPP (for `MethodContract`), checks
their hashes and runs extraction twice. CI uses only this reviewed seed.

## Required sequence

1. Build both patches with `build1933.py`, using a fresh work directory and the
   reviewed manifest. Require the real fusion, capture, metadata, fast-path and
   capture/fusion integration host regression suites, deterministic DEX/ZIP output, an exact method inventory
   and preservation of protected camera-startup/recovery and other-app code.
2. Use `validate1933.py` to apply each MPP to the pinned original ULike 5.6.2
   (740) APKS. Require resource rebuilding, arm64-v8a-only libraries, exact
   method contracts, DEX integrity and register/type checks.
3. Use `finalize1933.py` to bind the package to those exact validated MPP bytes.
   Source, QA, release notes and checksums are public; APK/APKS files, user
   photos, signing material and test runtime classes are not public assets.
4. Review and fill the complete publication manifest with the six fingerprints,
   exact resource/method/helper changes, source inventory and QA contract.
   `manifest.template.json` is intentionally incomplete and cannot authorize
   publication. A successful host test is required; placeholders do not count.
5. Run `publish1933.py --local-only` with both baseline files. This is read-only.
6. Upload only the reviewed release source/evidence/manifest/publication paths
   to current main, retaining every other path. Copy the archived workflow to
   `.github/workflows/ulike1933-burst-publish.yml` as the final trigger.
7. The workflow must rebuild the same release bytes, check the latest active
   bundle and policy, and pass read-only preflight before publishing. It verifies
   release and immutable-download bytes before atomically advancing main/dev.
8. Inspect the final receipt, both feeds, ULike update eligibility, unchanged
   other-app scope and public checksums. Inspect an existing tag or receipt
   before retrying a partially completed publication.

Both old branch heads are leased and every new commit is their direct child.
Concurrent publication fails closed. Original-APKS application, host regressions
and DEX analysis do not execute Android/ART or a physical camera. Galaxy image
quality and capture/save latency remain unmeasured until tested on the device.
