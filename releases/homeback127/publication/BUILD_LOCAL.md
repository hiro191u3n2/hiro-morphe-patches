# OneBack v1.1.17 / bundle v1.0.275 publication

Baseline: published v1.0.274 at immutable commit `7e0c9d17f17231bada1db440edd21bf650471442`, SHA-256 `940253bb01ae17dacaab0ff0239544b1a5fcb539202d8bee324c4c7191064fd8`. Its OneBack runtime is125, so reference125 matches the baseline extension. Profile-return work126 was not present on main and is included in this release together with452 support. If the baseline advances, inspect and rebuild rather than only updating branch leases.

## Build

Use pinned Temurin21.0.8+9 and tools declared in `src/build.py`; immutable official sources and hashes are in the workflow. Do not add replacement patch classes to a distributed MPP's verification runtime classpath.

```sh
python3 releases/homeback127/src/build.py --base <baseline274.mpp> --tools <tools> --legacy releases/oneback163/src --work <fresh-work> --dist <empty-dist>
```

Required checks: production SDK compile; every retained runtime suite plus `Back126InstagramProfileTest` and `Back127Instagram452Test`; DEX and unrelated-class/resource preservation; exact prior runtime resources; official loader all-version metadata and shipped implementation origin; 30 synthetic full-patcher runs (2MPPs × 5apps × 3pinned Desktop hosts, including patcher2.0.0).

## Supplied Instagram452 audit

`local-apk-validation.json` must be newly written after final real-APK runs. Never relabel older-release logs as current results. Retain the input identity in `manifest-template.json`; the official Morphe merged input from the supplied452 APKM is SHA-256 `fbfeec57e2415478cbbd4941d33d0563bce071daba236539403790366903aa61`; its original base APK is SHA-256 `22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2`.

Required evidence:

- `schema: homeback127-local-apk-validation-v1`, `result: PASS`, `package_name: com.instagram.android`, `input_version: 452.1.0.53.64`, matching `input_apk_sha256`, boolean `input_already_patched`, `physical_android_tested: false`.
- `artifacts` equals the final QA artifact map; `runtime_sha256` matches the final runtime.
- `outputs` has both final MPP names. Every row has `result: PASS`, a signed `output_apk_sha256`, and true `native_back_boundaries_verified`, `runtime_classes_match`, `full_semantic_audit`, `noncode_preservation_audited`, `signature_verified`, `alignment_verified`.
- `evidence_sha256` binds at least three actual text records under `evidence/`, covering application, executable/runtime, resource preservation, and signed output checks.
- Record the actual Piko co-application scope and versions with their limitations. Do not imply verification of the user's unknown64 selections/options if only defaults were tested.

Keep input/outputAPK files and private signing keys local. Host tests and static/signedAPK validation do not prove device install, UI navigation or task-card behavior.

## Freeze and publication

After final artifacts and evidence are complete:

```sh
python3 releases/homeback127/publication/freeze_local.py --dist <dist>
python3 releases/homeback127/publication/package.py --dist <dist> --legacy releases/oneback163/src
python3 releases/homeback127/publication/publish.py --dist <dist> --repo . --base <baseline274.mpp> --local-only
```

The reviewed source commit must directly descend from the reviewed main head and preserve the reviewed dev head. CI repeats all build gates twice, requires byte-identicalMPP/QA versus the local freeze, packages authored source/evidence, verifies public baseline identity, and publishes only after those checks. Publication creates immutable downloads/release assets, then atomically updates main/dev feeds. The historical changelog, source descriptions, unrelated resources and `releases/ULike_ACTIVE.json` remain unchanged. Inspect partial receipts before retrying a failure.
