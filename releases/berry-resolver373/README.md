# Berry one-hand zoom resolver investigation — unreleased candidate

Reported input: Berry Browser 3.84.21, Hiro Morphe Patches 1.0.372,
Morphe Manager 2.6.0-dev.5 / Patcher 2.4.0-dev.7. The reported failure is
`GenericBerryResolver.resolveCore`: no unique WebView/touch graph (0 candidates).

**Status: the reported 3.84.21 APK is unavailable; its failure has not been
reproduced or fixed conclusively. Do not publish this candidate as a verified
3.84.21 fix. The active release and both update feeds remain 1.0.372.**

The current resolver requires seven foreground-tab lifecycle methods and a
scroll callback even when only one-hand zoom is requested. This candidate
resolves those methods only when their complete logical descriptors are
requested. It retains unique ownership, concrete touch handlers, public
instance fields, platform call checks, and the original snapshot hashes.
No runtime extension or other patch provider changes.

## Verified on 2026-11-15

- Authenticated original Berry 3.84.12 / 384120 / target API 37: APK signature
  and source stamp verified with official Android Build Tools 37.
- Current 1.0.372 already applies one-hand zoom to that APK with the old host,
  Patcher 2.3.0, and the user's Patcher 2.4.0-dev.7. Thus 3.84.12 is a regression
  input, not a reproduction of the user's failure.
- Morphe Desktop 2.2.0-dev.18 supplies Patcher 2.4.0-dev.7; its 410 embedded
  patcher classes match the classes embedded in Manager 2.6.0-dev.5 byte-for-byte.
- Host structural mutation comparison: old resolver 16 checks, candidate
  32 checks. Missing unrelated methods no longer reject zoom; requesting the
  missing methods still rejects, as do missing touch handlers, private fields,
  and ambiguous ownership graphs. These are in-memory fixtures, not released APKs.
- Candidate combined MPP applies all four original Berry patches and rebuilds
  the APK with Patcher 2.4.0-dev.7. Candidate standalone MPP applies zoom alone
  and rebuilds successfully.
- Packaging preserves all five runtime extensions, 402 unrelated DEX classes,
  and all unrelated archive entries. Only four resolver classes change.
- No Android device/emulator gesture test and no 3.84.21 test were performed.

## Reproduce

Use the pinned URLs and hashes in `verification/tool-sources.json`. Keep the
original input APK and binary build products outside the source repository.

```sh
python3 build/build.py --base /path/to/baseline372.mpp --src src \
  --tools /path/to/tools --out /path/to/candidate-dist
java -jar /path/to/tools/ecj.jar -source 11 -target 11 \
  -cp /path/to/tools/classpath.jar:/path/to/baseline372.mpp \
  -d /path/to/test-classes qa/ResolverIsolationTest.java
java -cp /path/to/test-classes:/path/to/baseline372.mpp:/path/to/tools/classpath.jar \
  ResolverIsolationTest /path/to/Berry-3.84.12.apk legacy
java -cp /path/to/test-classes:/path/to/candidate-dist/Berry_Browser_v1.4.6.mpp:/path/to/tools/classpath.jar \
  ResolverIsolationTest /path/to/Berry-3.84.12.apk
python3 qa/run_apk_checks.py --apk /path/to/Berry-3.84.12.apk \
  --host /path/to/tools/morphe24.jar --dist /path/to/candidate-dist --out /path/to/apk-checks
```

The next required input is the exact **unmodified Berry 3.84.21 APK/APKS used
in the failed patch attempt**. First reproduce its old resolver failure and
inspect which structural constraint fails; then revise or retain this
candidate based on that evidence and verify the requested patch set. Version
numbers 1.0.373 / 1.4.6 in this directory are tentative candidate identifiers,
not released versions; recheck the live baseline before any publication.
