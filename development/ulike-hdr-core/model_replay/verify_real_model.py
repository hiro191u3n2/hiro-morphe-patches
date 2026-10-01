#!/usr/bin/env python3
"""Run synthetic input tensors through actual pinned vendor weights.

Only compact QA metadata is emitted. Original graph, weights, ONNX and tensors
remain in memory. These are mathematical tests, not photographic quality tests.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import time

import numpy as np
import onnx
import onnxruntime as ort

from baoman_replay import load_pinned,build_onnx,cpu_session,run_tensor
from reference_nhwc import reference_layers,convolution_roundoff_envelope


def fixtures(shape):
    yield "zero",np.zeros(shape,np.float32)
    n,c,h,w=shape
    ramp=np.linspace(-1,1,h*w,dtype=np.float32).reshape(h,w)
    yield "signed_ramps",np.stack([ramp,ramp[:,::-1],ramp[::-1,:]],axis=0)[None]
    yield "seeded_signed_random",np.random.default_rng(168).uniform(-1,1,shape).astype(np.float32)


def verify(model,library):
    plan,metadata=load_pinned(model,library)
    return verify_plan(plan,metadata,"tt_baoman","Natural blush")


def verify_plan(plan,metadata,model_name,style_name):
    session=cpu_session(build_onnx(plan,all_outputs=True))
    final_session=cpu_session(build_onnx(plan))
    results=[]
    # Differences are measured against FP64 direct raw-layout arithmetic, not
    # merely against another executor of the same converted ONNX graph.
    for label,x in fixtures(plan.input_shape):
        start=time.monotonic()
        outputs=session.run(None,{plan.graph.input_name:x})
        elapsed=time.monotonic()-start
        comparisons=[]
        for index,(layer,reference) in enumerate(reference_layers(plan,x,np.float64)):
            actual=outputs[index]
            if actual.shape!=reference.shape or actual.dtype!=np.float32 or not np.isfinite(actual).all():
                raise AssertionError("invalid output")
            absolute=np.abs(actual.astype(np.float64)-reference)
            # Per-value tolerance retains sensitivity for near-zero values.
            # Deep FP32 accumulation can amplify roundoff (the zero fixture
            # is the strongest observed case). Check isolated kernels below
            # with a cancellation-aware FP32 budget, and final Tanh separately.
            allowed=1e-3+1e-3*np.abs(reference)
            if not np.all(absolute<=allowed):
                raise AssertionError(f"layer {index} {layer.node.kind}: max normalized tolerance error {float(np.max(absolute/allowed))}")
            comparisons.append({"index":index,"kind":layer.node.kind,
                                "max_abs_error":float(absolute.max()),
                                "max_tolerance_fraction":float(np.max(absolute/allowed)),
                                "elements":actual.size})
        if comparisons[-1]["max_abs_error"]>1e-4:
            raise AssertionError("final FP32 vs FP64 output error exceeds 0.0001")
        overrides={plan.graph.input_name:x}
        overrides.update({layer.node.output:y for layer,y in zip(plan.layers,outputs)})
        isolated=[]
        for index,(layer,reference) in enumerate(reference_layers(plan,x,np.float64,overrides)):
            error=np.abs(outputs[index].astype(np.float64)-reference)
            allowed=2e-6+2e-6*np.abs(reference)
            envelope_fraction=None
            if layer.node.kind in ("Convolution","DepthwiseSeparableConvolution"):
                envelope=convolution_roundoff_envelope(layer,overrides[layer.node.inputs[0]])
                allowed=np.maximum(allowed,envelope)
                envelope_fraction=float(np.max(error/np.maximum(envelope,np.finfo(np.float64).tiny)))
            if not np.all(error<=allowed):
                raise AssertionError(f"isolated operator {index} failed")
            isolated.append({"index":index,"kind":layer.node.kind,"max_abs_error":float(error.max()),
                             "max_scaled_error":float(np.max(error/(1+np.abs(reference)))),
                             "max_roundoff_envelope_fraction":envelope_fraction,
                             "max_tolerance_fraction":float(np.max(error/allowed))})
        repeated=session.run(None,{plan.graph.input_name:x})[-1]
        if not np.array_equal(repeated,outputs[-1]):
            raise AssertionError("repeated session changed output")
        standalone=run_tensor(plan,x,final_session)
        np.testing.assert_array_equal(standalone,outputs[-1])
        worst=max(comparisons,key=lambda v:v["max_abs_error"])
        final=outputs[-1]
        results.append({"fixture":label,"layers_compared":len(comparisons),
                        "values_compared":sum(v["elements"] for v in comparisons),
                        "maximum_layer_absolute_error":worst,
                        "max_tolerance_fraction":max(v["max_tolerance_fraction"] for v in comparisons),
                        "final_max_abs_error":comparisons[-1]["max_abs_error"],
                        "isolated_operator_max_absolute_error":max(isolated,key=lambda v:v["max_abs_error"]),
                        "isolated_operator_max_tolerance_fraction":max(v["max_tolerance_fraction"] for v in isolated),
                        "isolated_operator_max_scaled_error":max(v["max_scaled_error"] for v in isolated),
                        "isolated_conv_max_roundoff_envelope_fraction":max(v["max_roundoff_envelope_fraction"] for v in isolated if v["max_roundoff_envelope_fraction"] is not None),
                        "final_shape":list(final.shape),"final_dtype":str(final.dtype),
                        "final_min":float(final.min()),"final_max":float(final.max()),
                        "final_sha256":hashlib.sha256(final.tobytes()).hexdigest(),
                        "deterministic_repeat":True,"final_only_matches_debug_outputs":True,
                        "host_inference_seconds_observed":elapsed})
    files={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(Path(__file__).parent.glob("*.py"))}
    return {"status":"PASS_REAL_WEIGHT_HOST_TENSOR_REPLAY",
            "model_name":model_name,"style_name":style_name,
            "model_sha256":metadata["model_sha256"],"library_sha256":metadata["library_sha256"],
            **({"effect_library_sha256":metadata["effect_library_sha256"]} if "effect_library_sha256" in metadata else {}),
            **({"loader_library_sha256":metadata["loader_library_sha256"]} if "loader_library_sha256" in metadata else {}),
            "graph_sha256":metadata["graph_sha256"],"weight_values_sha256":metadata["weight_values_sha256"],
            "weight_count":plan.weight_count,"native_graph_operator_count":len(plan.layers),
            "native_operator_counts":dict(Counter(layer.node.kind for layer in plan.layers)),
            "input_shape_nchw":list(plan.input_shape),"output_shape_nchw":list(plan.layers[-1].shape),
            "input_output_channels_semantics":"not interpreted by tensor replay",
            "onnx_version":onnx.__version__,"onnxruntime_version":ort.__version__,"numpy_version":np.__version__,
            "onnx_opset":18,"execution_provider":"CPUExecutionProvider",
            "onnx_optimization":"ORT_DISABLE_ALL","independent_reference":"NumPy FP64 NHWC with raw OHWI/HWC weights",
            "tolerance":{"whole_graph_all_layers":"abs_error <= 0.001 + 0.001 * abs(FP64_reference)",
                         "isolated_operators_same_fp32_inputs":"non-conv: abs_error <= 0.000002*(1+abs(reference)); Conv/DW: max of that floor and gamma_(2K+2)*(sum(abs(x*w))+abs(bias)), u=2^-24, gamma_n=n*u/(1-n*u)",
                         "final_output":"max abs_error <= 0.0001"},
            "fixtures":results,"source_sha256":files,
            "native_numerical_parity_verified":False,"android_integration":False,
            "complete_style_equivalence":False,"hdr_preservation_verified":False,
            "limitations":["Static native semantics plus independent host arithmetic checks; original Android engine was not executed.",
                           "Native vector Tanh uses approximate exp and unrefined FRECPE reciprocal. Standard mathematical ONNX Tanh deliberately replaces that approximation; native output need not agree bitwise or within host-reference tolerances.",
                           "Inputs are synthetic model tensors; face crop, alignment, normalization and output compositing are not reproduced here.",
                           "FP32 model replay does not prove HDR training/support or HDR-safe beauty processing.",
                           "Only this report's named/hash-pinned model is covered; no unobserved active-device model version is assumed.",
                           "Observed host execution times are not phone focus, lens-switch, capture or save benchmarks."]}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("model",type=Path)
    p.add_argument("library",type=Path)
    p.add_argument("--report",type=Path)
    args=p.parse_args()
    result=verify(args.model,args.library)
    encoded=json.dumps(result,indent=2,allow_nan=False)+"\n"
    if args.report:
        args.report.write_text(encoded)
    else:
        print(encoded,end="")


if __name__=="__main__":
    main()
