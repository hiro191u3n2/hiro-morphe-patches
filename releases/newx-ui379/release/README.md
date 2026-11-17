# Local release379 tooling

No script here pushes or publishes. The existing Site checkout has not been
modified. Scripts require the actual final application QA and artifact hashes.

## Inputs

`x-ui/dist/` must contain:

- `Hiro_Morphe_Patches_v1.0.379.mpp`
- `NewX_UI_Options_v1.0.0.mpp`
- `build-evidence.json`, with `result: "PASS"` and an `artifacts` map for both
  names containing exact `bytes` and `sha256` fields.

`x-ui/verification/final-validation.json` must contain `result: "PASS"`, the
same `artifacts` map, and `application_tests` with `dev1`, `dev2`, `dev2-clone`
and `standalone` cases each reporting `result: "PASS"`. Extra cases/fields are
preserved. The documents explicitly identify Android runtime as unverified.

The patch name expected by the release text is
`NewX: おすすめ非表示・上下タブ固定・120Hz設定`. Adjust the text to the actual
final provider name before staging if necessary.

## Prepare only after actual QA

```bash
python3 /workspace/scratch/440e6cc46323/x-ui/release/prepare_site379.py \
  --site /workspace/scratch/aae17de0ebaa/site-checkout/.sites-checkout \
  --check-only

python3 /workspace/scratch/440e6cc46323/x-ui/release/prepare_site379.py \
  --site /workspace/scratch/aae17de0ebaa/site-checkout/.sites-checkout
```

The installer stages the source and QA archive, a delta for the new combined
MPP, the standalone MPP, the Worker route and route tests, the v379 validator,
the page and source-card entry, and the descriptor/changelog. It preserves the
existing release page sections, prior MPPs and backing assets. It rejects an
already staged release so that a partial rerun cannot silently replace data.

The v379 recipe flattens copies of v378 directly into the previous 355, 377 and
378 backing spans. No request reconstructs an entire v378 bundle in memory.
The Worker source pins the exact new size/hash and all reused asset identities.

Run from the existing opened Site checkout:

```bash
python3 scripts/validate_fast_mpp.py public/patches-bundle.json
SITES_MPP_TEST_PATTERN='current descriptor download|NewX standalone' npm run build
node --test tests/rendered-html.test.mjs
```

The build automatically materializes and compacts v379, tests its compiled
Worker using only staged backing assets, and freezes a fresh `dist` tree with
the existing 256 MiB archive headroom check. The new dedicated tests cover full
content, HEAD, conditional responses, ranges/resume and damaged metadata/parts.
The installer copies the previous route test implementation but binds its
version, byte length and SHA to the actual reviewed v379 artifact.

After native Sites publication succeeds, root must update GitHub main
`CHANGELOG.md` first, then `patches-bundle.json`, fast-forward `dev`, and run
`python3 scripts/validate_update_channels.py`. Do not claim a device update
badge was observed from metadata verification alone.

## Tooling validation already performed

- Python syntax checks passed for all three release scripts.
- The bounded storage helper reconstructed a synthetic ZIP derived from the
  v378 baseline byte for byte while reusing over 48 MB. This tests storage only;
  its `storage-test/` fixtures are excluded from the source archive and are
  never application QA or publishable patch files.

Final Worker/application validation remains dependent on the actual v379 files.
