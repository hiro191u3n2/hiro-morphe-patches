# Pinned Natural-blush and Purity2 model tensor replay

This module implements host FP32 inference for the acquired `tt_baoman` and
`tt_goodlike` models. It accepts only SHA-256-pinned models and matching original
libraries, reconstructs their mathematical networks, reads every serialized FP32
parameter, and executes standard ONNX operators. It contains no model, weights, recovered
graph text, ONNX file, vendor library, or face photograph.

| Model | Style | Compute nodes | FP32 weight/bias values |
| --- | --- | ---: | ---: |
| `tt_baoman` | Natural blush | 97 | 239,900 |
| `tt_goodlike` | Purity2 | 96 | 250,004 |

**This is a tested model-tensor implementation, not a completed Android beauty
engine.** Face alignment, channel mapping, normalization, output compositing,
the rest of the style, HDR handling, and device integration are separate work.
Natural and Purity have separate strict loaders. Purity additionally requires the
original matching `libeffect.so` for its ordinary graph-container decoding. Native
call tracing establishes that Espresso's `Thrustor` wrapper constructs
`ByteNNEngineImpl` and initializes the shared parser/CPU network; shared graph
syntax alone was not used as evidence that the engines have identical semantics.

## Implemented contracts

The pinned input is NCHW `[1,3,256,256]`; output is `[1,4,256,256]`. The API
consumes an **already prepared model tensor**. It does not infer that four output
channels are camera RGBA, apply a generic `2/255` normalization, clamp input HDR
pixels, or invent face masks. The 256-pixel dimensions describe the model's face
tensor and do not establish the resolution of a complete saved photograph.

| Native operation | Implemented contract |
| --- | --- |
| Convolution | Raw OHWI converted to ONNX OIHW; per-layer bias after weights; optional ReLU |
| Depthwise convolution | Raw HWC converted to C1HW; group equals channels; optional ReLU |
| Concat | Two inputs concatenated in declared channel order |
| Eltwise | Equal-shaped Add; optional ReLU |
| UpSampling | Spatial 2× bilinear, `source = destination / 2 - 0.25`, edge replication |
| Tanh | Standard mathematical Tanh, replacing the native approximate implementation |

Only square kernels, equal spatial strides, symmetric padding, depth multiplier
one and the pinned tensor declarations are admitted. Unknown operators, graph
changes, nonfinite values, shape mismatches and leftover/truncated weights fail.
Public loading verifies the original model, library and decoded graph hashes.

The native vector Tanh kernel approximates exponentiation and uses an unrefined
`FRECPE` reciprocal. Standard ONNX Tanh intentionally does not reproduce that
approximation. Therefore successful host arithmetic tests **do not prove native
numerical parity**, even within the tolerances used by host tests. FP32 replay
also does not prove that the model was trained for HDR or preserves HDR colors.

## Run locally

Validated dependencies: Python 3.12, NumPy 2.5.3, ONNX 1.23.1, ONNX Runtime 1.30.0.
Purity's ordinary container decoder additionally requires `cryptography`.
The model inspector sibling directory must remain alongside this module.

```sh
python -m unittest discover -s model_replay -v
python model_replay/verify_real_model.py /private/tt_baoman.model /private/libbytenn.so
python model_replay/verify_purity_model.py /private/tt_goodlike.model /private/libeffect.so /private/libbytenn.so
python model_replay/replay_tensor.py /private/tt_baoman.model /private/libbytenn.so /private/input.npy /private/output.npy
python model_replay/replay_tensor.py /private/tt_goodlike.model /private/libbytenn.so /private/input.npy /private/output.npy --model-kind goodlike --effect-library /private/libeffect.so
```

The input NPY must be finite FP32 with exact shape `[1,3,256,256]`. Its bounded
header and exact payload size are validated before array creation; object dtypes
are rejected. Output creation is exclusive and does not overwrite a file.
The output remains an uninterpreted model tensor. No model data is downloaded by
this module, no original engine is executed, and no license check is changed.

Programmatic use:

```python
from baoman_replay import load_pinned, run_tensor
plan, metadata = load_pinned(model_path, library_path)
result_nchw = run_tensor(plan, prepared_fp32_nchw)

from purity_replay import load_pinned_purity
purity_plan, metadata = load_pinned_purity(purity_model_path, effect_library_path, library_path)
purity_result_nchw = run_tensor(purity_plan, prepared_fp32_nchw)
```

`build_onnx` returns an in-memory model containing the caller's original weights.
Do not redistribute it or serialize it into the source repository.

## Verification scope

`QA_REAL_MODEL.json` and `QA_PURITY_MODEL.json` record three synthetic input tensors
per model executed with the actual acquired weights. Every layer is checked against an
independent NumPy FP64 implementation that uses raw NHWC/OHWI/HWC layouts,
without executing the converted ONNX model. Additional isolated operator checks
use identical FP32 intermediate inputs, distinguishing individual operator error
from accumulated rounding. Repeated execution is deterministic in the tested
host configuration, and final-only output matches the debug graph's final output.
The Conv/DW operator budget accounts for cancellation using the conservative
FP32 dot-product envelope `gamma_(2K+2) * (sum(abs(x*w)) + abs(bias))`, with
`u=2^-24` and `gamma_n=n*u/(1-n*u)`. Other operations retain a tight
`0.000002 * (1+abs(reference))` tolerance; all final outputs must stay within
`0.0001` absolute error. Reports retain observed errors and envelope fractions.

For the Natural fixtures, full-graph FP32-vs-FP64 final maximum absolute error was
`0.00006755`; a deep intermediate reached `0.00232411`. The latter is reported
explicitly rather than equating host FP32 and FP64 arithmetic. The independent
per-operator maximum absolute error was about `0.00004948`.
For Purity, the final maximum was `0.00002922`, the deepest intermediate maximum
was `0.00041939`, and the isolated per-operator maximum was `0.00005955`.

`evidence/` records the native instruction audit and separate ARM64 kernel
emulation: 18 Add/ReLU cases and four channel-concat cases matched synthetic
references exactly. A 4,096-sample native vector Tanh test differed from standard
Tanh by up to `0.00539036`, supporting the explicit approximation limitation.
These isolated kernels do not constitute original-engine or full-model execution.

The unit tests cover raw weight ordering, channels, bias, activation, spatial
sampling, residual/concat topology, malformed streams and invalid tensors.
Host timings are observations and are not Android performance measurements.
There is no original-engine execution, phone parity test, quality judgment on
portraits, app integration, or camera-to-HDR-save verification in this module.

ONNX/operator and runtime references:

- <https://onnx.ai/onnx/operators/onnx__Conv.html>
- <https://onnx.ai/onnx/operators/onnx__Resize.html>
- <https://onnxruntime.ai/docs/api/python/api_summary.html>
- <https://onnxruntime.ai/docs/performance/model-optimizations/graph-optimizations.html>
