# ULike 1.9.32 quality update publication

This directory prepares publication for the user's selected candidates 1, 2, 8,
12, 15 and 20. The publisher retains the previous verified workflow's exact asset
checks, original-APKS validation binding and atomic main/dev Manager feed updates.
It changes only ULike and preserves the current bundle's other applications.

## Build prerequisites

The baseline ULike is the approved 1.9.31 file with SHA-256
`646a4e78e1905c2172d50205c4058a16d0ef76d9bbfcbd8b888f7dc34cee678e`.
The provisional bundle base is 1.0.164, producing 1.0.165. The reviewed manifest
supplies the final current bundle version, byte count, digest and immutable URL.
If another application publishes meanwhile, first rebuild and validate against
that new exact bundle, increment its version and review a new manifest. Do not
overwrite an advanced feed with the previously built integrated file.

The build uses Temurin 21.0.8+9 and the same three pinned JARs as ULike 1.9.31:

| File | SHA-256 |
| --- | --- |
| morphe.jar | 82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c |
| android.jar | 4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b |
| d8.jar | 305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5 |

The JARs are available in Actions run `37412171927`, artifact
`11389880303`, name `ulike1925-baseline-and-tools`, under `tools/`.
That original artifact expires on 2026-10-09. The new workflow also preserves the
verified JARs with its build artifact for 30 days.
The Linux x64 JDK archive SHA-256 is
`f2dc5418092c43003db8f9005c4a286e1c0104fea96ccdd49e8ebd037cac9219`.

## Required verification and publication sequence

1. Build standalone and combined MPPs deterministically with `build1932.py`.
2. Apply each MPP to original ULike 5.6.2 (740) APKS with `validate1932.py`.
   Require successful resource reconstruction, arm64-v8a output, exact method
   contracts, DEX integrity and register/type analysis.
3. Run `finalize1932.py` using the exact validation evidence. Package source,
   QA, release notes and checksums. No APK, APKS, private signing material or user
   screenshot is included in the public assets.
4. Fill the independent `manifest.json` with the six exact asset fingerprints,
   complete archive entry delta, exact changed method/helper inventories and QA
   contract. The template intentionally cannot pass publication validation.
5. Run `publish1932.py --local-only` with both local baseline files. It performs
   no GitHub writes and requires all reviewed bytes and semantic assertions.
6. Upload the reviewed source, evidence, manifest and this publication directory
   to the current main branch while retaining all unrelated repository paths.
   Copy the archived workflow verbatim to
   `.github/workflows/ulike1932-quality-publish.yml` as the trigger.
7. The workflow reconstructs the same MPP bytes, verifies local artifacts and
   performs a read-only preflight before creating a release. It verifies public
   release bytes and immutable download bytes before updating both Manager
   branches atomically.
8. Confirm the final receipt, both feed versions and URLs, ULike update eligibility,
   unchanged other-application scope and all public SHA-256 values.

The exact previous main/dev heads are leased and every new commit is their
direct descendant. A concurrent publication fails closed and requires re-reading
the current state. An existing release or receipt is inspected before any retry.

Android/Galaxy execution and measured quality/save-time improvements remain
unverified unless separately tested on a device. Host image tests and APK
reconstruction must not be described as device camera tests.
