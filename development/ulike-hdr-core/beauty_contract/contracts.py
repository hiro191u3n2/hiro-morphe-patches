"""Model-boundary math from the pinned ULike assets and native implementation.

This module has no camera, face detector, texture decoder, or HDR appearance
transform. Inputs are explicitly prepared model-domain samples or corresponding
landmarks. It never labels a prepared tensor as a completed beauty pipeline.
"""
from __future__ import annotations

import numpy as np

NATURAL_SCALE = float(np.float32(0.007799999788403511))
NATURAL_MARGIN = float(np.float32(0.4))
SDR_DOMAIN = "encoded_sdr_full_range"


def _finite(value, name):
    arr = np.asarray(value)
    if arr.dtype.kind not in "fiu" or not np.all(np.isfinite(arr)):
        raise ValueError(name + " must contain finite real numeric values")
    return arr.astype(np.float64)


def _bounded(value, low, high, name):
    arr = _finite(value, name)
    if np.any(arr < low) or np.any(arr > high):
        raise ValueError(name + " is outside the declared domain")
    return arr


def _domain(domain):
    if domain != SDR_DOMAIN:
        raise ValueError("Only an explicitly asserted encoded SDR domain is supported")


def prepare_natural_tensor(prepared_channels, *, domain):
    """256x256x3 post-cvt_color samples in code units [0,255] -> FP32 NCHW.

    Retains caller channel order. No RGB/BGR swap or implicit range conversion.
    Fractional code values preserve newly supplied higher precision; they do not
    claim that the stock engine supplied more than eight bits to this boundary.
    The native asset's literal 0.0078 scale is intentionally not 2/255.
    """
    _domain(domain)
    x = _bounded(prepared_channels, 0, 255, "prepared_channels")
    if x.shape != (256, 256, 3):
        raise ValueError("Expected a prepared 256x256x3 face crop")
    y = x * NATURAL_SCALE - 1.0
    return np.ascontiguousarray(y.transpose(2, 0, 1)[None], dtype=np.float32)


def prepare_purity_tensor(prepared_channels, *, domain):
    """256x256x3 no-extra-channel IDream boundary -> FP32 NCHW.

    Uses the observed no-extra branch: subtract 127.5 then multiply by the
    float32 reciprocal. The alternative FMA/extra-channel branch is not selected
    or simulated here. Caller channel order and colour encoding remain explicit.
    """
    _domain(domain)
    x = _bounded(prepared_channels, 0, 255, "prepared_channels")
    if x.shape != (256, 256, 3):
        raise ValueError("Expected a prepared 256x256x3 face crop")
    x = x.astype(np.float32)
    y = (x - np.float32(127.5)) * np.float32(1.0 / 127.5)
    return np.ascontiguousarray(y.transpose(2, 0, 1)[None])


def model_output_unit_channels(tensor, *, domain):
    """Unquantized float64 equivalent of (127.5*x + 127.5)/255.

    Accepts the confirmed four-channel tanh model output. Keeps channel order;
    it does not infer primaries. This deliberately replaces the original byte
    conversion with continuous values and thus is not stock byte-exact output.
    """
    _domain(domain)
    # The verified ONNX runtime's FP32 Tanh can overshoot its mathematical
    # interval by one float32 ulp. Allow exactly that numerical tolerance;
    # larger excursions still fail instead of silently hiding a bad model.
    edge = float(np.nextafter(np.float32(1), np.float32(np.inf)))
    t = _bounded(tensor, -edge, edge, "tensor")
    if t.shape != (1, 4, 256, 256):
        raise ValueError("Expected [1,4,256,256] tanh model output")
    t = np.clip(t, -1.0, 1.0)
    return np.ascontiguousarray((t[0].transpose(1, 2, 0) + 1.0) * 0.5)


def composite_samples(style, source_rgba, generated_rgba, mask_red, intensity, *, domain):
    """Apply the two actual neural-image fragment-shader blend equations.

    All images/masks must already be sampled at the shader's correct respective
    coordinates. Purity's observed Lua sets mask=2 so the mask-enabled branch is
    used. No face geometry, mask synthesis, LUT, or later makeup pass is implied.
    """
    _domain(domain)
    src = _bounded(source_rgba, 0, 1, "source_rgba")
    gen = _bounded(generated_rgba, 0, 1, "generated_rgba")
    mask = _bounded(mask_red, 0, 1, "mask_red")
    strength = _bounded(intensity, 0, 1, "intensity")
    if src.shape != gen.shape or src.ndim < 1 or src.shape[-1] != 4:
        raise ValueError("Source and generated RGBA sample shapes must match")
    if mask.shape != src.shape[:-1] or strength.ndim != 0:
        raise ValueError("Mask shape or scalar intensity mismatch")
    if style == "natural_blush":
        weight = np.minimum(mask, gen[..., 3]) * strength
    elif style == "purity2":
        weight = mask * gen[..., 3] * strength
    else:
        raise ValueError("Unknown style")
    out = src * (1.0 - weight[..., None]) + gen * weight[..., None]
    out[..., 3] = 1.0
    return out


def similarity_fit(source_points, target_points):
    """Float64 version of libeffect 0xcba7a8 similarity fit, source -> target.

    Native accumulates in float32. This uses the same algebra with float64,
    rejects a degenerate set and returns a 3x3 homogeneous matrix. It does not
    select landmarks or establish their index ordering/coordinate convention.
    """
    src = _finite(source_points, "source_points")
    dst = _finite(target_points, "target_points")
    if src.shape != dst.shape or src.ndim != 2 or src.shape[1] != 2 or len(src) < 2:
        raise ValueError("Need corresponding N x 2 point arrays, N >= 2")
    sm, dm = src.mean(axis=0), dst.mean(axis=0)
    s, d = src - sm, dst - dm
    denom = np.sum(s * s)
    if not np.isfinite(denom) or denom <= np.finfo(np.float64).tiny:
        raise ValueError("Degenerate source landmarks")
    a = np.sum(s * d) / denom
    b = np.sum(s[:, 0] * d[:, 1] - s[:, 1] * d[:, 0]) / denom
    lin = np.array([[a, -b], [b, a]])
    translation = dm - lin @ sm
    result = np.eye(3)
    result[:2, :2] = lin
    result[:2, 2] = translation
    if not np.all(np.isfinite(result)) or a * a + b * b <= np.finfo(np.float64).tiny:
        raise ValueError("Degenerate fitted transform")
    return result


def natural_crop_matrix(landmarks106, template106, *, margin=NATURAL_MARGIN,
                        offset_x=0.0, offset_y=15.0):
    """NH crop_type=1 similarity -> margin scaling -> pixel offset matrix.

    Requires already selected and ordered 106 source-image landmarks and the
    matching native template; face detection/pose adjustment are not inferred.
    Default settings match acquired tt_baoman v1.0 face_align metadata. Inputs
    must use a common native image convention. There is no full-image resize.
    """
    src = _finite(landmarks106, "landmarks106")
    dst = _finite(template106, "template106")
    if src.shape != (106, 2) or dst.shape != (106, 2):
        raise ValueError("Exactly 106 corresponding landmarks are required")
    m = _finite(margin, "margin")
    ox = _finite(offset_x, "offset_x")
    oy = _finite(offset_y, "offset_y")
    if any(x.ndim != 0 for x in (m, ox, oy)) or m < 0:
        raise ValueError("Margin and offsets must be finite scalars; margin >= 0")
    denominator = 1.0 + 2.0 * float(m)
    if not np.isfinite(denominator):
        raise ValueError("Margin is too large")
    scale = 256.0 / denominator
    if not np.isfinite(scale) or scale <= 0:
        raise ValueError("Invalid crop scale")
    padding = np.array([[scale, 0, scale * float(m)],
                        [0, scale, scale * float(m)], [0, 0, 1.]])
    offset = np.array([[1., 0, float(ox)], [0, 1., float(oy)], [0, 0, 1.]])
    result = offset @ padding @ similarity_fit(src, dst)
    if not np.all(np.isfinite(result)):
        raise ValueError("Nonfinite crop matrix")
    return result
