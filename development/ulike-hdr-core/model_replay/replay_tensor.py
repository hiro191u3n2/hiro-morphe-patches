#!/usr/bin/env python3
"""Run a caller-prepared NCHW FP32 tensor; this is not a photo beautification CLI."""
import argparse
import hashlib
import io
import json
from pathlib import Path

import numpy as np

from baoman_replay import load_pinned,run_tensor


def read_input(path):
    with path.open("rb") as handle:
        data=handle.read(1_048_577)
    if not 0<len(data)<=1_048_576:
        raise ValueError("input NPY exceeds bounded 256x256x3 FP32 payload")
    stream=io.BytesIO(data)
    version=np.lib.format.read_magic(stream)
    if version==(1,0):
        shape,fortran,dtype=np.lib.format.read_array_header_1_0(stream,max_header_size=4096)
    elif version==(2,0):
        shape,fortran,dtype=np.lib.format.read_array_header_2_0(stream,max_header_size=4096)
    else:
        raise ValueError("only NPY v1/v2 float32 tensors are supported")
    # Validate dimensions/type before allocating an array: a small malicious NPY
    # may declare a huge shape. The dtype is never allowed to contain objects.
    if shape!=(1,3,256,256) or fortran or dtype!=np.dtype("float32") or dtype.hasobject:
        raise ValueError("exact finite NCHW [1,3,256,256] float32 NPY required")
    offset=stream.tell()
    if len(data)-offset!=3*256*256*4:
        raise ValueError("truncated/trailing input tensor payload")
    value=np.frombuffer(data,dtype=dtype,offset=offset).reshape(shape)
    if not np.isfinite(value).all():
        raise ValueError("nonfinite input tensor")
    return value


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("model",type=Path)
    p.add_argument("library",type=Path)
    p.add_argument("input_npy",type=Path)
    p.add_argument("output_npy",type=Path)
    p.add_argument("--model-kind",choices=("baoman","goodlike"),default="baoman")
    p.add_argument("--effect-library",type=Path,
                   help="Required with --model-kind goodlike; original SHA-pinned libeffect.so")
    args=p.parse_args()
    if args.model_kind=="goodlike":
        if args.effect_library is None:
            p.error("--effect-library is required for the pinned Purity2 loader")
        from purity_replay import load_pinned_purity
        plan,metadata=load_pinned_purity(args.model,args.effect_library,args.library)
    else:
        if args.effect_library is not None:
            p.error("--effect-library is only used with --model-kind goodlike")
        plan,metadata=load_pinned(args.model,args.library)
    x=read_input(args.input_npy)
    y=run_tensor(plan,x)
    # Exclusive creation: never overwrite input/original model/user data.
    with args.output_npy.open("xb") as handle:
        np.save(handle,y,allow_pickle=False)
    print(json.dumps({"status":"HOST_MODEL_TENSOR_REPLAY_COMPLETE",
                      "model_kind":args.model_kind,
                      "model_sha256":metadata["model_sha256"],
                      "input_shape_nchw":list(x.shape),"output_shape_nchw":list(y.shape),
                      "tensor_dtype":"float32","output_tensor_sha256":hashlib.sha256(y.tobytes()).hexdigest(),
                      "native_parity_verified":False,"complete_style_replication":False,
                      "camera_to_hdr_save_pipeline":False},indent=2))


if __name__=="__main__": main()
