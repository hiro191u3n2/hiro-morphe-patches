# Android-compatible pinned model tensor engine

This module ports the already audited `tt_baoman` and `tt_goodlike` tensor reconstruction to Java and provides an ONNX Runtime Android FP32 CPU wrapper. It is a staged Android component, not a patched ULike release or a complete beauty pipeline.

## Implemented

- `PinnedModel.compile`: reads caller-owned original model and matching `libbytenn.so`; Purity2 additionally requires matching `libeffect.so`. SHA-256 pins must match the previously audited snapshots. Inputs are bounded and no network lookup, native vendor code loading, or directory search occurs.
- Natural blush: validates the Bach envelope, normal BM v2 XOR/substitution decoding, graph hash, graph/weight marker, and weight hash. The substitution table is read from the caller-supplied pinned library.
- Purity2: validates the legacy v3 envelope, reads the pinned native constructor's immediate data from the caller's library, uses Android/JCE `AES/ECB/NoPadding` to implement the observed loader format, and validates graph checksum, graph hash, weight hash and marker. No loader key or material is embedded in this source.
- `GraphOnnx`: strictly parses the audited graph subset, checks topology and tensor bounds, transforms original FP32 OHWI/HWC weights into ONNX OIHW, and emits ONNX IR 10 / opset 18 with a small protobuf writer. No protobuf runtime dependency is needed.
- `OrtTensorEngine`: uses official ORT Android 1.30.0 Java APIs, CPU FP32, one thread, sequential execution and disabled graph optimization. The process telemetry opt-out is required before native initialization; session telemetry is also disabled before creating sessions. Inputs are copied and validated; native results are copied before close. `run` and `close` are synchronized, close is idempotent, and use after close is rejected.

The input is **one already prepared `1×3×256×256` NCHW float32 tensor**. The output is **one `1×4×256×256` NCHW float32 tensor**. These are face-model tensors, not full-size camera photographs. The four output channels remain uninterpreted here. There is no arbitrary graph/model loading API.

The compiled ONNX returned in memory contains original caller-provided weights and graph data. Keep it local; it is not a publication artifact. The source contains no original model, original library, complete decoded graph, decoded weights or loader key. Stream ownership remains with the caller, who must close streams. Run compilation and inference on an application worker thread; do not do either on the UI thread.

## Android integration API

Use the official dependency `com.microsoft.onnxruntime:onnxruntime-android:1.30.0`. The production source was compiled against Android SDK 36, then converted with D8 at minimum API 26. Integration still needs normal Gradle dependency/JNI packaging for the application's ABIs and device testing. **Merge the provided `AndroidManifest.xml` removal into the higher-priority application manifest** so the ORT AAR cannot register `ai.onnxruntime.TelemetryInitializer`. Then call `AndroidTensorEngine.open` before any component initializes ORT. This sets the officially documented `ORT_DISABLE_TELEMETRY=1` process flag using `android.system.Os.setenv` before native runtime loading. Session API telemetry disable alone cannot suppress an initialization event in ORT 1.30.0. The wrapper rejects a missing process flag before runtime creation. Other application permissions are left intact.

```java
// The three streams are local, caller-owned snapshots, not remote URLs.
PinnedModel.CompiledModel compiled = PinnedModel.compile(
    PinnedModel.Style.PURITY2, modelStream, bytennStream, effectStream);
try (OrtTensorEngine engine = AndroidTensorEngine.open(compiled)) {
    float[] output = engine.run(preparedNchwFloat32);
}
```

For `NATURAL_BLUSH`, the `effectStream` argument can be `null`. This module does not grant redistribution rights to original model/library inputs. No vendor license or authentication check is executed or bypassed by this local data decoder.

## Verification

`QA_RUNTIME.json` records real SDK 36 Java compilation and D8 success. The **same classes compiled against the Android APIs** were exercised using a separately downloaded official **host Java ORT 1.30.0 runtime**, with `ORT_DISABLE_TELEMETRY=1` set before Python imports and inherited by Java subprocesses, plus explicit session telemetry disable. For both real pinned models, zero, signed-ramp and seeded-random inputs produce bit-exact tensors compared with the frozen Python ONNX implementation: 262,144 output floats for each fixture. The Java wrapper also checks rejection of a missing pre-initialization telemetry flag, invalid inputs, defensive copies, input preservation, repeatability and close behavior.

This is host execution of Android-compatible Java code. It does **not** establish that Android JNI loading or device inference works. The independent review additionally checks Java/Python graph structure, initializer bytes, topology and bounded rejection cases; see its report when present.

Reproduce with Python dependencies from `../model_replay`, JDK 21, Android SDK 36, R8 8.3.37, official ORT Android 1.30.0 AAR `classes.jar`, and official ORT host Java 1.30.0 jar:

```sh
ORT_DISABLE_TELEMETRY=1 python verify_runtime.py \
  --jdk /path/to/jdk/bin \
  --android-jar /path/to/platforms/android-36/android.jar \
  --ort-android-classes /path/to/onnxruntime-android-1.30.0-classes.jar \
  --ort-host-jar /path/to/onnxruntime-1.30.0.jar \
  --r8 /path/to/r8-8.3.37.jar \
  --baoman /local/models/tt_baoman.model \
  --goodlike /local/models/tt_goodlike.model \
  --bytenn /local/original/libbytenn.so \
  --effect /local/original/libeffect.so \
  --report /local/QA_RUNTIME.json
```

Generated ONNX, input and output tensors, classes and DEX stay in a temporary local directory and are removed by the verifier. Only source-hash-pinned summary metrics go into the report. The host QA source uses Java `java.nio.file`; those test classes are excluded from the production DEX.

## Remaining work and precision limits

There is no ULike app hook, Camera2 session, face detector/alignment, crop normalization, output-channel mapping, skin/face masks, full-style compositing, HDR color conversion, gainmap regeneration or image encoder integration in this module. Original active-device model hashes still require device evidence. A complete image must not be passed directly to this tensor API.

The imported model weights and tensors remain FP32. This does not prove that these SDR-trained original models are HDR-safe. Standard ONNX `Tanh` intentionally replaces the native approximate implementation, consistent with `../model_replay`; native vendor pixel or bitwise equivalence is not claimed. No native 24.5 MP input or full beauty style equivalence is claimed.

Primary API/schema references:

- [ONNX normative protobuf schema](https://github.com/onnx/onnx/blob/main/onnx/onnx.proto)
- [ONNX IR](https://github.com/onnx/onnx/blob/main/docs/IR.md)
- [Official ORT Java guide](https://onnxruntime.ai/docs/get-started/with-java.html)
- [OrtEnvironment](https://onnxruntime.ai/docs/api/java/ai/onnxruntime/OrtEnvironment.html)
- [OnnxTensor](https://onnxruntime.ai/docs/api/java/ai/onnxruntime/OnnxTensor.html)
- [Official ORT Android Maven artifact](https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.30.0/)

- [Official ORT 1.30.0 telemetry controls](https://github.com/microsoft/onnxruntime/blob/v1.30.0/docs/Privacy.md)
- [Android manifest merge rules](https://developer.android.com/build/manage-manifests)
