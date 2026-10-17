# Local build and evidence commands for v1.1.11

The active binary baseline is Hiro Morphe Patches v1.0.249, containing OneBack v1.1.10. The candidate is OneBack v1.1.11 / bundle v1.0.250. Keep ULike and every unrelated baseline resource and loader class unchanged. Earlier source-only work is not a replacement for the live binary baseline.

The exact immutable URL is pinned in `manifest-template.json`. Baseline MPP SHA-256 is `c10dda2839b733b54fab850b96f670bbf7cdd6ba687b120184a3103151bc2624`; its OneBack runtime is `d614e7ba8501af6ee65b3b237cf75034d3035a46c3556ef23365363b25955fc2`.

## Source and toolchain

`source-review.json` records the live main/dev review, immutable feed, absent proposed tag/release/branch and unused proposed bundle name. At review, main was `3f76e99d8de3956713a126a2142a11316499e33c`, dev was `778b2bd9670904bd23cef47df18461ec1186d5a5`. The old local checkout HEAD is not a valid parent for source submission. Stage only this release and its workflow on the latest reviewed main. If either reviewed branch changes, inspect the change and refresh the recorded lease before submission.

Use Temurin JDK 21.0.8+9, selected in CI as `21.0.8+9.0.LTS`. Local JDK is `/workspace/scratch/075f7970e1d9/build-inputs/jdk-21.0.8+9`; pinned Morphe, Android SDK and D8 jars are in `/workspace/scratch/075f7970e1d9/build-inputs/tools`. Generation and build reject a different JDK/compiler. Production compilation uses the real Android SDK jar; host Android stubs are introduced only for the models. Reflection contracts require separate actual-APK evidence.

## Freeze runtime and regenerate evidence

Finish production runtime, patch metadata, tests, audit sources and build gates before generation. Prepare the replacement runtime DEX with the same SDK/D8 command as `src/build.py`, then set `task_runtime` to that frozen DEX. Do not edit production/test/audit sources after this point. `prepare_evidence.py` recompiles production and requires its D8 bytes to match this exact DEX.

The generator reads the newly supplied current APK and all three older inputs in place. It verifies reference identity and every actual existing hook against the new runtime. Current installed guards are inspected against baseline v1.1.10; they are not inserted again. Baseline249 public ABI is checked separately.

Both scope and whole-dispatch differentials use the same compiled test class against each production runtime. Each host runtime is D8-verified against the actual baseline/frozen extension. Scope positive counterexamples and safety cases are checked distinctly. For whole-dispatch, named native entry/registration counterexamples are separated from intentional protected-forward behavior changes and other model differences. Do not describe every old-model failure as a user/device bug.

```bash
set -euo pipefail
cd /workspace/scratch/075f7970e1d9/hiro-morphe-patches
export PATH="/workspace/scratch/075f7970e1d9/build-inputs/jdk-21.0.8+9/bin:$PATH"
task_release=releases/homeback121
task_tools=/workspace/scratch/075f7970e1d9/build-inputs/tools
task_base=/workspace/scratch/075f7970e1d9/dist120-final/Hiro_Morphe_Patches_v1.0.249.mpp
# Set task_runtime to the actual frozen production runtime-dex/classes.dex.
test -n "${task_runtime:-}"
python3 "$task_release/publication/prepare_evidence.py" \
  --runtime "$task_runtime" --base "$task_base" --tools "$task_tools" \
  --java-home /workspace/scratch/075f7970e1d9/build-inputs/jdk-21.0.8+9 \
  --legacy releases/oneback163/src \
  --work /workspace/scratch/075f7970e1d9/evidence121-final \
  --reference116 /workspace/scratch/075f7970e1d9/build-inputs/reference116-runtime.dex \
  --current-instagram /workspace/scratch/075f7970e1d9/upload/Instagram-v447.0.0.55.81-patches-v3.10.0.apk \
  --old-instagram-a /workspace/scratch/075f7970e1d9/inputs/Instagram-v447.0.0.55.81-patches-v3.10.0.apk \
  --old-instagram-b '/workspace/scratch/075f7970e1d9/inputs/Instagram_v447.0.0.55.81(2147483647).apk' \
  --old-tiktok '/workspace/scratch/075f7970e1d9/inputs/TikTok_v46.2.3(2147483647).apk'
```

Fresh results go in `publication/evidence121/` and are bound to production/test/audit source hashes and the exact new runtime. Copied `evidence/` records remain historical v1.1.10 observations; do not edit old PASS/source hashes to imply a new run. No supplied APK is included in the source archive.

## Full build, freeze and package

Use fresh work and empty output directories. The build checks fresh evidence, repeats baseline249 ABI, runs existing and new host suites, and applies both distributed MPPs to five synthetic package fixtures each.

It also decodes `native-projection.json` and runs both actual distributed MPPs through the complete Morphe pipeline. This 27-class projection contains three exact old provided method bodies, hierarchy/field shells and synthetic seed classes. It retains the old `hiroOneBack$terminal$1` reference without its implementation. The original seed includes `Lapp/hiro/oneback/runtime/AlreadyLoaded;`, a synthetic Activity with only an empty `onResume()V`; this is not a shipped runtime implementation. Its complete canonical class hash is pinned in the fixture and auditor, and it must remain unchanged. No other runtime-prefixed input definition is allowed. The projection remains byte-identical to the investigation input. It is non-executable structural data, not a clean original APK. `PipelineProbe` requires exact input method hashes, actual distributed class origin, both emitted native guards, incoming branch targets, false-path continuation, complete method restoration and canonical equality of every distributed runtime class. The complete output runtime type set must equal the distributed extension plus the one original synthetic sentinel. Single and bundle output DEX must match. Other patches are not co-applied in this fixture.

```bash
task_work=/workspace/scratch/075f7970e1d9/build121-final
task_dist=/workspace/scratch/075f7970e1d9/dist121-final
python3 "$task_release/src/build.py" --base "$task_base" --tools "$task_tools" \
  --legacy releases/oneback163/src --work "$task_work" --dist "$task_dist"
python3 "$task_release/publication/freeze_local.py" --dist "$task_dist"
python3 "$task_release/publication/package.py" --dist "$task_dist" --legacy releases/oneback163/src
python3 "$task_release/publication/publish.py" --dist "$task_dist" --repo . --base "$task_base" --local-only
```

Manifests start explicitly unfrozen; old artifact hashes cannot authorize a new release. Freeze only completed successful builds. Finish README and release notes before final packaging. Copy the reviewed `publication/workflow.yml` into `.github/workflows/homeback121-publish.yml` as part of source submission.

CI checks the direct source-parent lease, current main/dev heads and active baseline. It runs two fresh full builds, checks MPP/QA equality against each other and the locally frozen identities, then packages and validates the exact source archive. Publication requires the same successful workflow source, unused release tag/distribution branch, unchanged Manager baseline feeds and atomically leased main/dev updates. ULike policy bytes and existing history are preserved. Do not bypass these gates.

No host, ABI, structural projection or CI result establishes physical Android behavior or application to a clean matching Instagram APK. Retain these limits in QA and release notes. “Recent tasks” means Android's Recents cards, not Instagram's server-side viewing history.
