# Local release381 tooling

No script here pushes or publishes. Existing Site files remain untouched until
root runs the installer after actual final application QA.

The release fixes duplicate view-count formatting and repeated formatter
creation. Its page and documentation describe those code changes without
claiming measured Android scrolling improvement. All four controls and saved
settings retain their names and keys. The provider name stays:

`NewX: 表示設定を追加（おすすめ・上下固定・120Hz・表示回数）`

## Required evidence

Both `dist/build-evidence.json` and `verification/final-validation.json` must
report `result: "PASS"` and an `artifacts` map with actual byte lengths and
SHA-256 values for combined `1.0.381` and component `1.1.1`.
Final application QA must include dev1, dev2, dev2-clone and standalone cases,
each PASS. The build's `dex_preservation` object must report PASS and
`existing_non_ui_provider_classes_preserved: true`.

The validator identifies the old UI payload/classes using immutable component
v1.1.0, verifies all non-UI data/classes remain and requires the resulting UI
payload to match the new component exactly. Old component classes are replaced,
not added a second time. Baseline380 and old component hashes are pinned.

## Prepare locally

```bash
python3 release/prepare_site381.py \
  --site /workspace/scratch/aae17de0ebaa/site-checkout/.sites-checkout \
  --approved-review performance-review/diagnosis.json \
  --approved-review performance-review/bars-static-review.md \
  --check-only

python3 release/prepare_site381.py \
  --site /workspace/scratch/aae17de0ebaa/site-checkout/.sites-checkout \
  --approved-review performance-review/diagnosis.json \
  --approved-review performance-review/bars-static-review.md
```

Optional reviewed summaries require an explicit repeated argument such as
`--approved-review performance-review/review.json`. Only relative JSON or Markdown paths
inside `performance-review/` or `count-review/` are accepted. No video-review,
recording, frame, screenshot, APK, decoded directory, `.txt` code dump or
compiler is collected. Existing host/APK QA and authored compile/test stubs are
included through the strict source allowlist.
The sanitized host-only `views-runtime/benchmark.json` is included by exact
filename. Other JSON files in that folder are not admitted by a broad glob.

The public381 README is also the source archive's root README, replacing the
inherited380 introduction so source users see the correct release.

The installer adds the exact new source/QA, descriptor/changelog, page section,
bounded Worker route, route tests and validator. Previous routes, assets and
page sections remain. The new recipe flattens380 copies directly to immutable
backing spans; the Worker never downloads a full base MPP recursively. Its
target size/hash and reused backing assets are independently pinned. The
standalone test derives its size/hash from the actual new MPP and uses a
file-midpoint resume range.

From the opened Site checkout:

```bash
python3 scripts/validate_fast_mpp.py public/patches-bundle.json
SITES_MPP_TEST_PATTERN='current descriptor download|NewX standalone' npm run build
node --test tests/rendered-html.test.mjs
```

The normal build checks all existing routes, the new381 route, source bindings
and final compact archive headroom. Root performs native Site publication and
then updates GitHub main CHANGELOG followed by its feed, fast-forwards dev,
and verifies the update channels.
