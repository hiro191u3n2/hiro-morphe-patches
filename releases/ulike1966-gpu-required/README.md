# Withdrawn: ULike v1.9.66 / bundle v1.0.199

ユーザーから実動作の不具合報告があり、本版の採用を取り消しました。復帰先と今後の改造基準は **ULike v1.9.65 / Hiro Morphe Patches v1.0.198** です。本ディレクトリのソースと検証記録は撤回前の履歴です。

[復帰先の公開版](https://github.com/hiro191u3n2/hiro-morphe-patches/releases/tag/ulike-v1.9.65)

Build candidate on the exact published ULike v1.9.65 / bundle v1.0.198. Enabled noise, protection-mask and additional correction image calculations must use GPU. CPU orchestration, copies, logging and file/codec management remain. Existing ULike beauty SDK and face-detection internals are not proved GPU-only.

GPU precision or processing failure preserves the original source, permits bounded GPU recovery, then fails processing/save. The candidate does not return to CPU image arithmetic or publish untreated pixels as success. Hardware FP64 or the exact GPU integer binary64 implementation must pass an executed precision probe; unsupported precision must not be described as successful GPU migration.

Sources and tests are under `src`; publication operations and atomic main/dev feed guards are under `publication`. The workflow only publishes after all fresh compilation, source-pin, native-linkage, test and package checks succeed. A functional original-GLES path is required before this candidate can be published.

Physical Android capture, saved-image quality and device speed remain unverified. Software host GPU execution does not establish Galaxy support.

Separately from CI, the exact published standalone MPP was applied to the private original ULike 5.6.2 (740) APKS and the APK was rebuilt. [Original APKS application evidence](original-APKS-application_ULike_v1.9.66.json) records the input/output hashes, matching published artifacts, retained camera hooks, native payload checks and limits. CI's original-APKS flag remains false because this application test ran locally; the bundle was checked for identical ULike payloads rather than separately applied.

The GPU binary64 implementation includes Berkeley SoftFloat derived BSD 3-clause code. The complete notice is shipped in each MPP as `ulike1965/THIRD_PARTY_LICENSES.txt`, and in the source archive as `THIRD_PARTY_LICENSES.txt`.
