#!/usr/bin/env python3
"""Pinned Natural-blush model-tensor FP32 replay; no image/style/HDR claim.

Vendor graph/weights are accepted only through the SHA-pinned local model reader.
No original model, decoded graph, weights, or generated ONNX is included here.
The returned in-memory ONNX contains caller-supplied model data: do not publish it.
"""
from __future__ import annotations

from dataclasses import dataclass
import hashlib
from pathlib import Path
import sys

import numpy as np
import onnx
from onnx import helper, numpy_helper, TensorProto
import onnxruntime as ort

from graph_schema import Graph, Node, UnsupportedGraph, parse_graph
from operator_reference import resize_onnx_node

INSPECTOR = Path(__file__).resolve().parents[1] / "model_inspect"
sys.path.insert(0, str(INSPECTOR))
from bm_graph_metadata import load_baoman


@dataclass(frozen=True)
class Layer:
    node: Node
    shape: tuple[int, int, int, int]  # NCHW
    raw_weights: np.ndarray | None = None
    bias: np.ndarray | None = None


@dataclass(frozen=True)
class Plan:
    graph: Graph
    layers: tuple[Layer, ...]
    input_shape: tuple[int, int, int, int]
    weight_count: int


def _plan(graph: Graph, weight_bytes: bytes) -> Plan:
    """Bounded statically audited subset, including direct raw-layout references.

    Internal generic function permits synthetic tests. Public load_pinned rejects
    every model/library/graph other than the independently audited hashes.
    """
    if len(weight_bytes) % 4 or len(weight_bytes) > 16_000_000:
        raise UnsupportedGraph("weight byte length")
    weights = np.frombuffer(weight_bytes, dtype="<f4")
    if not np.isfinite(weights).all():
        raise UnsupportedGraph("nonfinite weights")
    n, h, w, c = graph.input_nhwc
    shapes = {graph.input_name: (n, c, h, w)}
    layers = []
    cursor = 0
    total_elements = n*c*h*w
    for node in graph.nodes:
        xshape = shapes[node.inputs[0]]
        raw = bias = None
        if node.kind in ("Convolution", "DepthwiseSeparableConvolution"):
            o, kh, kw, sh, sw, ph, pw, has_bias, activation = node.parameters[:9]
            if kh != kw or sh != sw or ph != pw:
                raise UnsupportedGraph("asymmetric kernels/strides/padding were not audited")
            n, c, h, w = xshape
            depthwise = node.kind == "DepthwiseSeparableConvolution"
            if depthwise and o != c:
                raise UnsupportedGraph("only depth multiplier one was audited")
            count = kh*kw*c if depthwise else o*kh*kw*c
            consumed = count + o * has_bias
            if cursor + consumed > weights.size:
                raise UnsupportedGraph("truncated weight stream")
            raw = weights[cursor:cursor+count]
            raw = raw.reshape((kh, kw, c) if depthwise else (o, kh, kw, c))
            bias = weights[cursor+count:cursor+consumed] if has_bias else np.zeros(o,np.float32)
            cursor += consumed
            shape = (n, o, (h+2*ph-kh)//sh+1, (w+2*pw-kw)//sw+1)
        elif node.kind == "Concat":
            others = [shapes[name] for name in node.inputs]
            if any((v[0],v[2],v[3]) != (xshape[0],xshape[2],xshape[3]) for v in others):
                raise UnsupportedGraph("concat spatial/batch mismatch")
            shape = (xshape[0],sum(v[1] for v in others),xshape[2],xshape[3])
        elif node.kind == "Eltwise":
            if any(shapes[name] != xshape for name in node.inputs):
                raise UnsupportedGraph("elementwise broadcast is not supported")
            shape = xshape
        elif node.kind == "UpSampling":
            shape = (xshape[0],xshape[1],xshape[2]*2,xshape[3]*2)
        elif node.kind == "Tanh":
            shape = xshape
        else:
            raise UnsupportedGraph("operator not audited")
        count = int(np.prod(shape,dtype=np.int64))
        if any(d<=0 or d>4096 for d in shape) or count>8_000_000:
            raise UnsupportedGraph("intermediate tensor bound")
        total_elements += count
        if total_elements>64_000_000:
            raise UnsupportedGraph("aggregate tensor bound")
        shapes[node.output]=shape
        layers.append(Layer(node,shape,raw,bias))
    if cursor!=weights.size:
        raise UnsupportedGraph("unconsumed trailing weight values")
    return Plan(graph,tuple(layers),shapes[graph.input_name],cursor)


def build_onnx(plan: Plan, all_outputs: bool = False) -> onnx.ModelProto:
    """Build standard ONNX operators, preserving FP32 parameters and tensors."""
    nodes=[]
    initializers=[]
    shapes={plan.graph.input_name:plan.input_shape}
    for index,layer in enumerate(plan.layers):
        node=layer.node
        # Generated namespace is disjoint from the parser's allowed name alphabet.
        prefix=f"@replay/{index}"
        if node.kind in ("Convolution","DepthwiseSeparableConvolution"):
            o,kh,kw,sh,sw,ph,pw,has_bias,activation=node.parameters[:9]
            depthwise=node.kind=="DepthwiseSeparableConvolution"
            kernel=(layer.raw_weights.transpose(2,0,1)[:,None,:,:] if depthwise
                    else layer.raw_weights.transpose(0,3,1,2))
            wname,bname=prefix+"/weight",prefix+"/bias"
            initializers.extend([numpy_helper.from_array(np.ascontiguousarray(kernel),wname),
                                 numpy_helper.from_array(np.ascontiguousarray(layer.bias),bname)])
            target=prefix+"/pre_relu" if activation else node.output
            nodes.append(helper.make_node("Conv",[node.inputs[0],wname,bname],[target],
                         name=prefix+"/conv",kernel_shape=[kh,kw],strides=[sh,sw],
                         pads=[ph,pw,ph,pw],dilations=[1,1],group=o if depthwise else 1,
                         auto_pad="NOTSET"))
            if activation:
                nodes.append(helper.make_node("Relu",[target],[node.output],name=prefix+"/relu"))
        elif node.kind=="Concat":
            nodes.append(helper.make_node("Concat",list(node.inputs),[node.output],axis=1,name=prefix))
        elif node.kind=="Eltwise":
            activated=node.parameters[0]
            target=prefix+"/pre_relu" if activated else node.output
            nodes.append(helper.make_node("Add",list(node.inputs),[target],name=prefix+"/add"))
            if activated:
                nodes.append(helper.make_node("Relu",[target],[node.output],name=prefix+"/relu"))
        elif node.kind=="UpSampling":
            resize,sizes=resize_onnx_node(prefix,node.inputs[0],node.output,shapes[node.inputs[0]])
            nodes.append(resize)
            initializers.append(sizes)
        elif node.kind=="Tanh":
            nodes.append(helper.make_node("Tanh",list(node.inputs),[node.output],name=prefix))
        shapes[node.output]=layer.shape
    chosen=plan.layers if all_outputs else plan.layers[-1:]
    output_infos=[helper.make_tensor_value_info(v.node.output,TensorProto.FLOAT,list(v.shape)) for v in chosen]
    graph=helper.make_graph(nodes,"pinned-native-tensor-replay",
                           [helper.make_tensor_value_info(plan.graph.input_name,TensorProto.FLOAT,list(plan.input_shape))],
                           output_infos,initializer=initializers)
    model=helper.make_model(graph,opset_imports=[helper.make_opsetid("",18)],ir_version=10,
                            producer_name="local-pinned-model-replay")
    onnx.checker.check_model(model,full_check=True)
    return model


def load_pinned(model_path: Path, library_path: Path) -> tuple[Plan,dict]:
    decoded=load_baoman(model_path,library_path)
    plan=_plan(parse_graph(decoded["graph_text"]),decoded["weights"])
    if plan.input_shape!=(1,3,256,256) or plan.layers[-1].shape!=(1,4,256,256) or plan.weight_count!=239900:
        raise UnsupportedGraph("pinned topology/weight aggregate mismatch")
    return plan,decoded["metadata"]


def cpu_session(model: onnx.ModelProto) -> ort.InferenceSession:
    options=ort.SessionOptions()
    options.intra_op_num_threads=1
    options.inter_op_num_threads=1
    options.execution_mode=ort.ExecutionMode.ORT_SEQUENTIAL
    options.graph_optimization_level=ort.GraphOptimizationLevel.ORT_DISABLE_ALL
    session=ort.InferenceSession(model.SerializeToString(),options,providers=["CPUExecutionProvider"])
    session.disable_fallback()
    return session


def run_tensor(plan: Plan, x: np.ndarray, session: ort.InferenceSession | None = None) -> np.ndarray:
    """Input is an already-prepared NCHW float32 model tensor, not camera pixels.

    No normalization, RGB mapping, crop, alignment, mask interpretation, HDR
    transfer, or face compositing is guessed here. Four output channels remain
    an uninterpreted model tensor until separate native evidence establishes them.
    """
    if not isinstance(x,np.ndarray) or x.dtype!=np.float32 or x.shape!=plan.input_shape or not np.isfinite(x).all():
        raise ValueError("exact finite NCHW float32 model tensor required")
    runtime=session or cpu_session(build_onnx(plan))
    outputs=runtime.run([plan.graph.output_name],{plan.graph.input_name:np.ascontiguousarray(x)})
    y=outputs[0]
    if y.dtype!=np.float32 or y.shape!=plan.layers[-1].shape or not np.isfinite(y).all():
        raise ValueError("unexpected/nonfinite output tensor")
    return y
