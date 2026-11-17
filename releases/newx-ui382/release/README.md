# Local rollback382 release tooling

These scripts prepare local files and never publish or push. The current native
Site baseline is381. Version382 withdraws381 and restores380 code exactly; it
uses an increasing version only so Manager accepts it as an update.

## Evidence gates

- Current `verification/final-validation.json`: version1.0.382, resultPASS,
  actual artifact hashes and new `application_tests.dev2.result=PASS`.
- `dist/build-evidence.json`: resultPASS, actual artifact hashes,
  `rollback_payloads_match380=true`, `dex_preservation.result=PASS` and
  `existing_non_ui_provider_classes_preserved=true`.
- Archived380 QA: all four original cases PASS, with its original artifact
  identities; preserved in source as `verification/baseline380-validation.json`.
- Validator independently compares all combined382 entries against archived380
  and all component1.1.2 entries against1.1.0, excluding only MANIFEST.MF.
  It also checks prior381 non-UI preservation, DEX and source bindings.

The current dev2 application test is distinguished from historical380 evidence.
No physical-device improvement is claimed.

## Staging

From x-ui382 after actual final QA:

```bash
python3 release/prepare_site382.py \
  --site /workspace/scratch/aae17de0ebaa/site-checkout/.sites-checkout \
  --check-only

python3 release/prepare_site382.py \
  --site /workspace/scratch/aae17de0ebaa/site-checkout/.sites-checkout
```

The source allowlist includes rollback.py and the restored380 authored sources.
Root README is replaced with the current public README. No381 performance
reviews, benchmark files, video material, decoded directories or code dumps are
included. The feed timestamp is2026-11-17T10:00:26 UTC.

The existing381 page entry is retained as history and marked withdrawn. All old
assets/routes are retained. The382 delta flattens381 spans directly to immutable
backing assets; Worker requests never recursively download a full base MPP.
Target identities come from actual files. Standalone resume tests use midpoint.

From the opened Site checkout:

```bash
python3 scripts/validate_fast_mpp.py public/patches-bundle.json
SITES_MPP_TEST_PATTERN='current descriptor download|NewX standalone' npm run build
node --test tests/rendered-html.test.mjs
```

Root handles native Site publication and GitHub main/dev feed updates after the
build and compact delivery archive checks.
