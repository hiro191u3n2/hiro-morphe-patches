# Trip.com 1.10.15 / bundle 1.0.162 publication

These files prepare the existing GitHub release and Manager-feed publication path. They have not been executed for publication. Final MPP/QA hashes intentionally remain unset in `manifest.template.json`.

## Root build interface

```bash
python3 trip162/src/build.py --base BASE161 --single-base TRIP14 --tools TOOLS --work FRESH_WORK --dist trip162/dist
```

The workflow uses Java 21 and `py7zr==1.0.0`. Keep the local rebuild toolchain identical before recording final hashes.

The build/finalization phase supplies exactly these six release assets in `dist`:

- `Tripcom_Quiet_v1.10.15.mpp`
- `Hiro_Morphe_Patches_v1.0.162.mpp`
- `QA_Tripcom_v1.10.15.json`
- `Tripcom_v1.10.15_sources_and_QA.zip`
- `RELEASE_NOTES.txt`, copied exactly from this directory
- `SHA256SUMS.txt`, covering the other five assets

## Final review configuration

Copy `manifest.template.json` to `manifest.json` after final QA. Fill both MPP byte counts and SHA-256 values, plus `expected_qa_sha256`. Set `review_status` to `reviewed` only when these represent the final tested output. The source ZIP must then be rebuilt; it contains this exact finalized manifest. The manifest does not contain the source ZIP's digest, avoiding a self-reference.

`new_trip_extensions` is an explicit allowlist and initially contains `extensions/myplan_bundle_guard.mpe`. Adjust it if the reviewed native implementation chooses another path. Existing Trip-owned resources are taken from the independently pinned Trip1.10.14 standalone; every one matches the current bundle161. Only Trip classes under `app/hiro/tripcom/patches/`, existing Trip resources and the explicit new extension may change. The combined loader `classes.dex` and manifest are handled separately. QA must confirm non-Trip loader definitions are unchanged.

## QA contract

The final QA file must contain these top-level fields. It may contain additional detailed findings and evidence:

```json
{
  "schema": "trip162-qa-v1",
  "result": "PASS",
  "blocking_findings": [],
  "supported_package": "ctrip.english",
  "supported_version": "8.54.2",
  "trip_version": "1.10.15",
  "bundle_version": "1.0.162",
  "android_device_tested": false,
  "original_apk_apply_tested": true,
  "non_trip_resources_unchanged": true,
  "non_trip_loader_classes_unchanged": true,
  "standalone_and_bundle_trip_resources_identical": true,
  "js_regression_passed": true,
  "native_hook_host_tests_passed": true,
  "native_hook_original_apk_verified": true,
  "artifacts": {
    "Tripcom_Quiet_v1.10.15.mpp": {"bytes": 1, "sha256": "FINAL_SHA256"},
    "Hiro_Morphe_Patches_v1.0.162.mpp": {"bytes": 1, "sha256": "FINAL_SHA256"}
  }
}
```

Replace the example artifact records with the exact `expected_mpp` values. Boolean assertions must reflect actual checks. `native_hook_original_apk_verified` means the final patched APK was inspected for correct native hook injection; it does not claim device execution. Do not bundle the original or patched application APK/APKS in any publication asset.

The build validates the current rebuild and host checks against `src/reviewed-qa.json` and then copies that static independently recorded evidence into the public QA asset. The publisher verifies exact equality to this source file and its reviewed SHA-256. Keep machine-specific paths and dynamic host logs in local/CI evidence rather than changing the public QA during CI.

## Complete source archive

Preserve authored UTF-8 sources and useful QA evidence. The validator rejects application binaries, compiled tools, images and private keys. It also requires authored Java/Kotlin and JS/Node source and these exact current files, with either paths relative to `trip162` or one containing folder:

- `src/build.py`
- `src/reviewed-qa.json`
- `publication/publish162.py`
- `publication/manifest.json`
- `publication/RELEASE_NOTES.txt`
- `publication/trip162-publish.yml`

The archive must also contain every authored source/configuration input under the current `src/` checkout, byte-for-byte. The validator inventories these files so an omitted native helper, test stub or JS transform cannot be hidden by including only the entry-point script. Build can derive the modified CRN archive from the baseline; no original APK is needed on CI. Desktop original-APK validation is bound to the exact final MPP hashes and the independently reviewed QA hash.

The Home resource resolver tests require the hash-pinned decoded XML fixtures under `src/resource/fixtures/layout/` and their `metadata.json`. UTF-8 XML is explicitly accepted and included in the completeness inventory. Keep these fixtures in the source commit and source ZIP so CI can reproduce the actual four-layout resolution checks. `HomeResourceDocuments` and the amended Home execution closure remain under the existing Trip loader namespace; their addition/replacement requires no broader resource allowlist. The final APK/APKS application evidence must be regenerated after this loader change and bound to the new final MPP hashes.

## Validation and execution

```bash
python3 trip162/publication/publish162.py --dist trip162/dist --repo CHECKOUT --base BASE161 --single-base TRIP14 --config trip162/publication/manifest.json --local-only
```

`--local-only` performs no network/Git commands and validates assets, hashes, scope, QA and source completeness. `--preflight-only` performs remote reads and local Git `rev-parse` only; it never creates a release, branch, tag or commit. Both options are mutually exclusive. The preflight needs the established authenticated `gh` environment.

For publication, copy reviewed source files under `releases/trip162/` in the repository and copy `trip162-publish.yml` byte-for-byte to `.github/workflows/trip162-publish.yml`. Commit the complete source and workflow together on current `main` through the connector, with an expected-head lease. The workflow path trigger runs the build, verification, read-only preflight and publication. The release uses tag `tripcom-v1.10.15`; immutable MPP copies use branch `release/trip162` and the resulting commit SHA.

The script will reject an existing release/tag/distribution branch or prior local receipt instead of blindly retrying mutations. Inspect any interrupted attempt before recovery. Both Manager feeds advance in one atomic Git push with explicit expected-head leases. Existing branch history, all prior description/changelog history, other app resources and `releases/ULike_ACTIVE.json` are preserved. A moved baseline or concurrent branch write must be reconciled and reviewed before another attempt.

The shared workflow concurrency group remains `hiro-ulike-release-publication`, matching recent QuickSearch/ULike/SwiftKey releases. Current tool artifact `ulike1925-baseline-and-tools` from run `37412171927` expires on 2026-10-09 at 04:07:51 UTC; recheck availability if work resumes later.
