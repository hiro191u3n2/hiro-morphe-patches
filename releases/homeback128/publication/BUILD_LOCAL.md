# HomeBack128 local build and publication preparation

The release is v1.1.18 / aggregate v1.0.277, based on the immutable published aggregate v1.0.276. No earlier APK outcome is accepted as current evidence. Use fresh work/output directories.

```bash
source /workspace/scratch/e8538f87316e/instagram452/tools/env.sh
python3 releases/homeback128/src/build.py --base /workspace/scratch/8b9267bb333e/published114-verification/Hiro_Morphe_Patches_v1.0.276.mpp --tools /workspace/scratch/e8538f87316e/instagram452/tools --legacy /workspace/scratch/e8538f87316e/instagram452/repo/releases/oneback163/src --work /absolute/new-build-dir --dist /absolute/new-dist-dir
```

The build runs production SDK compilation, every checked-in host suite including Back128InstagramProfileTest, independent DEX structure checks, preservation of unrelated loader bodies and resource entries, executable JVM metadata checks, and 30 full synthetic patch/rebuild cases. The old runtime127 asset is the exact baseline276 extension and must remain pinned.

Apply the resulting standalone and aggregate MPP files to the original locally merged Instagram452 APK. Audit each rebuilt output, runtime identity, native Back boundaries, all unchanged executable bodies, manifest/noncode/resource semantics, and sign/alignment verification. Private inputs and the local signing key stay outside the repository. Place newly generated text/code records in `evidence/instagram452-release/`. Bind the final output to `fresh452-summary.json` and `signing-alignment.json` using the field conventions consumed by `create_local_evidence.py`; their release QA and MPP hashes must identify this exact build. Also run a real APK upgrade from the published127 unsigned output and save `upgrade127-summary.json`, bound to the input pin, final128 MPP/runtime and output. Verify all native Back boundaries and unchanged application code after replacing only the exact known runtime. Place newly run profile-host evidence in `evidence/runtime128/`.

```bash
python3 releases/homeback128/publication/create_local_evidence.py --dist /absolute/new-dist-dir
python3 releases/homeback128/publication/freeze_local.py --dist /absolute/new-dist-dir
python3 releases/homeback128/publication/package.py --dist /absolute/new-dist-dir --legacy /workspace/scratch/e8538f87316e/instagram452/repo/releases/oneback163/src
python3 releases/homeback128/publication/publish.py --dist /absolute/new-dist-dir --repo . --base /workspace/scratch/8b9267bb333e/published114-verification/Hiro_Morphe_Patches_v1.0.276.mpp --local-only
```

Review the current main/dev heads, unchanged active276 feed, current release notes, and frozen identities before submitting the source commit. The source commit must descend directly from the reviewed main head. The workflow copies itself into the publication source, runs two clean builds, requires identical MPP and QA bytes against the local freeze, packages the source/evidence, and performs the preflight before publishing. It verifies immutable downloads and releases, then updates main/dev feeds atomically with leases. A changed baseline/head is a reason to review the new state, not to weaken the checks.

This release also applied only the new OneBack v1.1.18 to the previously built Instagram452 APK containing Piko3.11.0-dev.1 all-nonclone65 and OneBack127. The current `evidence/instagram452-release/upgrade127-piko-summary.json` binds that input, final MPP/runtime and output; its audits confirm existing application/Piko code and native Back boundaries are retained. The existing OneBack manifest policy adds explicit `enableOnBackInvokedCallback=true` to seven Piko-added activities. All other manifest elements/attributes/numeric references and 24,530 other noncode entries remain unchanged. This is an update test on an already patched input, not a fresh application of all Piko patches or recreation of the unknown user64 selection. The upgrade output was unsigned; separately generated fresh standalone/aggregate outputs received signing/alignment validation. Actual Android screen navigation remains untested. APK bytes, keystores and credentials must not enter source archives, commits, release assets or CI uploads.
