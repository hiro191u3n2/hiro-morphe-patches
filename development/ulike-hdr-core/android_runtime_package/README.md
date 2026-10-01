# Official inference runtime packaging

This module installs the exact ONNX Runtime 1.30.0 arm64 native libraries and their notices into Morphe's private resource workspace. The integration patch calls `IntegrationPayload169.install(ResourcePatchContext)` after the normal ULike target checks. The official Java classes are merged into application DEX separately by the integration builder.

All six resource lengths and SHA-256 hashes are fixed. Existing matching files are accepted; conflicts are rejected. The installer validates and stages all files before destination writes, rejects paths escaping the private workspace, and rolls back newly created files if a later write fails. Existing application files are preserved. The owning patcher must serialize workspace access.

The original AAR manifest is **not** merged. Its startup telemetry provider is explicitly rejected if encountered in the application manifest. The Android engine must set `ORT_DISABLE_TELEMETRY=1` before any native runtime loading, then disable telemetry through the runtime API. This packaging helper never starts the inference runtime and adds no permissions.

The helper sets `android:extractNativeLibs="true"` so native loading does not depend on the APK ZIP entry alignment. Both pinned libraries also have 16 KiB aligned ELF load segments. Actual APK installation and native initialization remain separate device checks.

Generate the private payload:

```sh
python3 prepare.py --aar /path/to/onnxruntime-android-1.30.0.aar \
  --official-notices /path/to/official/onnxruntime \
  --output /path/to/private/runtime_payload
```

Compile against the real Morphe API and test:

```sh
python3 verify.py --jdk-bin /path/to/jdk/bin --morphe-jar /path/to/morphe-1.16.jar \
  --payload /path/to/private/runtime_payload --report QA.json
```

The tests use the actual pinned dependency bytes and real filesystem operations. They cover hashes, sizes, repeat installation, corruption/truncation/oversize/read failures, preservation of conflicting files, path and symlink escapes, rollback after a later destination failure, and manifest guards. They do not instantiate Morphe's production resource context, install an APK, or execute Android/native inference. `QA.json` records those limits and binds the source/test hashes.

No ULike model bodies, supplied style assets, or vendor native libraries are included in this source module. The generated private package contains only the separately supplied official ONNX Runtime dependencies and notices.
