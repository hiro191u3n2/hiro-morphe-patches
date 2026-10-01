"""Independent array reference over the native raw NHWC/HWC/OHWI layouts.

This does not call ONNX or use the converted OIHW weights. It implements ordinary
cross-correlation directly from the audited serialized layouts. Float64 mode is
a numerical reference, not an assertion that the native engine runs float64.
"""
from collections import Counter
import numpy as np


def convolution_roundoff_envelope(layer,input_nchw):
    """Conservative FP32 sequential-dot error budget, with FP64 magnitude sums.

    gamma_(2*K+2) * (sum(abs(x*w)) + abs(bias)) includes separate multiply/add
    roundings and bias. It responds to cancellation rather than treating a small
    result as evidence that the operands were small. This is a host QA budget,
    not proof of any optimized Android kernel's numerical accuracy.
    """
    node=layer.node
    if node.kind not in ("Convolution","DepthwiseSeparableConvolution"):
        raise ValueError("convolution layer required")
    o,kh,kw,sh,sw,ph,pw,has_bias,activation=node.parameters[:9]
    x=np.abs(input_nchw.transpose(0,2,3,1).astype(np.float64))
    raw=np.abs(layer.raw_weights.astype(np.float64))
    n,c,h,w=input_nchw.shape
    _,_,oh,ow=layer.shape
    padded=np.pad(x,((0,0),(ph,ph),(pw,pw),(0,0)),mode="constant")
    total=np.zeros((n,oh,ow,o),np.float64)
    depthwise=node.kind=="DepthwiseSeparableConvolution"
    for ky in range(kh):
        for kx in range(kw):
            patch=padded[:,ky:ky+oh*sh:sh,kx:kx+ow*sw:sw,:]
            if depthwise: total+=patch*raw[ky,kx,:]
            else: total+=np.einsum("nhwc,oc->nhwo",patch,raw[:,ky,kx,:],optimize=False)
    total+=np.abs(layer.bias.astype(np.float64))
    terms=kh*kw*(1 if depthwise else c)
    operations=2*terms+2
    unit_roundoff=2.0**-24
    gamma=(operations*unit_roundoff)/(1-operations*unit_roundoff)
    return (gamma*total).transpose(0,3,1,2)


def reference_layers(plan, input_nchw, dtype=np.float64, input_overrides=None):
    if dtype not in (np.float32,np.float64):
        raise ValueError("reference dtype")
    if input_nchw.shape!=plan.input_shape or input_nchw.dtype!=np.float32 or not np.isfinite(input_nchw).all():
        raise ValueError("finite FP32 input tensor required")
    tensors={plan.graph.input_name:input_nchw.transpose(0,2,3,1).astype(dtype)}
    uses=Counter(source for layer in plan.layers for source in layer.node.inputs)
    for layer in plan.layers:
        node=layer.node
        if input_overrides is not None:
            # Operator-isolated QA supplies identical FP32 inputs from the
            # runtime, separating accumulated rounding from operator errors.
            for source in node.inputs:
                tensors[source]=input_overrides[source].transpose(0,2,3,1).astype(dtype)
        x=tensors[node.inputs[0]]
        if node.kind in ("Convolution","DepthwiseSeparableConvolution"):
            o,kh,kw,sh,sw,ph,pw,has_bias,activation=node.parameters[:9]
            raw=layer.raw_weights.astype(dtype)
            n,channels,oh,ow=layer.shape
            padded=np.pad(x,((0,0),(ph,ph),(pw,pw),(0,0)),mode="constant")
            y=np.zeros((n,oh,ow,o),dtype=dtype)
            # This spatial loop avoids dependence on ONNX kernel/tensor transforms.
            for ky in range(kh):
                for kx in range(kw):
                    patch=padded[:,ky:ky+oh*sh:sh,kx:kx+ow*sw:sw,:]
                    if node.kind=="DepthwiseSeparableConvolution":
                        y += patch*raw[ky,kx,:]
                    else:
                        y += np.einsum("nhwc,oc->nhwo",patch,raw[:,ky,kx,:],optimize=False)
            y += layer.bias.astype(dtype)
            if activation:
                np.maximum(y,dtype(0),out=y)
        elif node.kind=="Concat":
            y=np.concatenate([tensors[source] for source in node.inputs],axis=3)
        elif node.kind=="Eltwise":
            y=x+tensors[node.inputs[1]]
            if node.parameters[0]:
                np.maximum(y,dtype(0),out=y)
        elif node.kind=="UpSampling":
            n,h,w,c=x.shape
            # The audited 2x coordinate is j/2 - 1/4, with edge replication.
            yy=np.clip(np.arange(2*h,dtype=dtype)/dtype(2)-dtype(.25),0,h-1)
            xx=np.clip(np.arange(2*w,dtype=dtype)/dtype(2)-dtype(.25),0,w-1)
            iy=np.floor(yy).astype(np.int64)
            ix=np.floor(xx).astype(np.int64)
            fy=(yy-iy.astype(dtype))[None,:,None,None]
            fx=(xx-ix.astype(dtype))[None,None,:,None]
            ny=np.minimum(iy+1,h-1)
            nx=np.minimum(ix+1,w-1)
            tl=x[:,iy,:][:,:,ix,:]
            tr=x[:,iy,:][:,:,nx,:]
            bl=x[:,ny,:][:,:,ix,:]
            br=x[:,ny,:][:,:,nx,:]
            y=(tl*(dtype(1)-fx)+tr*fx)*(dtype(1)-fy)+(bl*(dtype(1)-fx)+br*fx)*fy
        elif node.kind=="Tanh":
            y=np.tanh(x)
        else:
            raise ValueError("unknown reference operator")
        if not np.isfinite(y).all():
            raise ValueError("nonfinite reference output")
        tensors[node.output]=y
        yield layer,y.transpose(0,3,1,2)
        for source in node.inputs:
            uses[source]-=1
            if uses[source]==0:
                del tensors[source]
