# HomeBack v1.1.13 publication preparation

This directory prepares OneBack v1.1.13 / Hiro bundle v1.0.255 from the live reviewed bundle v1.0.253. It is not a publication receipt. The final MPP and QA hashes are frozen and both supplied-APK applications passed. The user explicitly approved GitHub publication on 2026-10-19 after reviewing the deliverables. CI and public feed verification must complete before publication is reported as successful.

The supplied Instagram APK is SHA-256 `153f84f6700fc5ebe29f918b4d59540dbd299e896fc6d04e4ca9b5d6014e7dd2`. Its runtime is the previous v1.1.12, with all 74 classes matching. This release must use new applications/audits of that exact input. The old v1.1.12 validation record must not be relabeled as new evidence.

The changes are at the native Reels boundary and immediately before the app's own CLIPS tab-history pop in `X.2xZ.A0V`. The source and host/DEX tests determine the final behavior. No physical Android result is claimed.

Before freezing, run both final MPPs locally against the supplied APK and write `publication/local-apk-validation.json` with schema `homeback123-local-apk-validation-v1`, `input_runtime_version: "1.1.12"`, the exact input hash, final MPP hashes, runtime hash, output APK hashes and both `native_entry_hook_verified` and `native_tab_pop_hook_verified`. Both standalone and combined outputs must pass the full runtime/code/resource audit. Only the audit records are packaged. The user APK is never committed or uploaded to CI.

Then the following commands perform local preparation only:

```sh
python3 releases/homeback123/publication/freeze_local.py --dist <dist>
python3 releases/homeback123/publication/package.py --dist <dist> --legacy releases/oneback163/src
python3 releases/homeback123/publication/publish.py --dist <dist> --repo . --base <baseline253.mpp> --local-only
```

`manifest-template.json` contains the frozen local artifact and QA hashes. `freeze_local.py` records only completed results and sets `ci_build_pair_verified` false. `verify_pair.py` will later require two fresh builds to match the final locally frozen MPP and QA bytes. The installed workflow enforces the CI gates before publishing; user approval does not replace those checks.

The identical workflow is installed at `.github/workflows/homeback123-publish.yml`. It must run from a reviewed source commit with the expected main parent and live dev head. The script verifies tag/branch/version vacancy, releases exact assets, creates immutable raw downloads, then atomically advances main/dev feeds with leases. Unrelated patch entries, descriptions, changelog history and the ULike active policy are retained. Only Instagram receives a new Manager changelog scope. A separate website is not deployed.

The source archive permits exactly three old runtime reference DEX files: `oneback-runtime120.dex`, `oneback-runtime121.dex`, and `oneback-runtime122.dex`, all pinned by SHA-256. It does not permit arbitrary APK or DEX attachments. The final local artifact-validation gate must pass after any packaging/documentation changes.
