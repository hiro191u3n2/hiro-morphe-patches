# Local release380 tooling

No script here pushes or publishes. Existing Site files remain untouched until
root runs the installer after actual final QA.

## Required inputs

`dist/Hiro_Morphe_Patches_v1.0.380.mpp` and
`dist/NewX_UI_Options_v1.1.0.mpp` must match the exact `artifacts` map
(`bytes` and `sha256`) in both `dist/build-evidence.json` and
`verification/final-validation.json`. Both reports need `result: "PASS"`.
The application report requires `dev1`, `dev2`, `dev2-clone` and `standalone`
cases in `application_tests`, each reporting `result: "PASS"`.

The build report additionally needs:

```json
{"dex_preservation":{"result":"PASS","existing_non_ui_provider_classes_preserved":true}}
```

The new component replaces the published v1.0.0 component inside combined379.
The validator identifies old component entries/types from the immutable
`NewX_UI_Options_v1.0.0.mpp` and requires the final set to equal the unchanged
non-UI entries/types plus exactly the new component entries/types. Duplicate
or leftover old UI definitions fail validation.

## Prepare after final application QA

```bash
python3 release/prepare_site380.py \
  --site /workspace/scratch/aae17de0ebaa/site-checkout/.sites-checkout \
  --check-only

python3 release/prepare_site380.py \
  --site /workspace/scratch/aae17de0ebaa/site-checkout/.sites-checkout
```

The installer adds the new source/QA archive, combined recipe, standalone MPP,
page section, descriptor/changelog, bounded Worker, compiled delivery tests and
validator. The old release page sections, routes and artifacts are preserved.
Existing app settings remain in place; only the new full-count toggle defaults
on. The patch name is exactly:

`NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）`

The source archive uses a strict authored-source allowlist and never includes
decoded `*-work` directories, compiled files, APKs, `.txt` analysis dumps,
user media, tools or input artifacts. Java compile/test stubs are retained as
source. `views/` allows direct authored Java/Python/README/QA files only.

The new bounded recipe flattens v379 copies into backing spans from 355,
377, 378 and 379. It never recursively downloads a full previous bundle.
Target size/hash and reused asset identities are pinned independently in the
Worker. Standalone size/hash derive from the actual reviewed v1.1.0 MPP;
resume-range testing uses the actual file midpoint.

From the opened Site checkout:

```bash
python3 scripts/validate_fast_mpp.py public/patches-bundle.json
SITES_MPP_TEST_PATTERN='current descriptor download|NewX standalone' npm run build
node --test tests/rendered-html.test.mjs
```

The existing build freezes a fresh compact output with a 256 MiB archive
headroom check. Root performs native Sites publication and then updates GitHub
main CHANGELOG before its feed, fast-forwards dev, and validates update channels.
