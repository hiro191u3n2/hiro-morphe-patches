"""Host high-precision neural-image component with explicit supplied geometry.

This is a deliberate continuous-bilinear replacement of the legacy quantized
crop, not native GPU pixel parity. It retains a caller-supplied full-size SDR
channel image; there is no camera capture, face detector, complete style or HDR
appearance conversion. Image row/column indices denote integer pixel centres.
"""
from __future__ import annotations

from dataclasses import dataclass
from math import prod

import numpy as np

from contracts import (SDR_DOMAIN, natural_crop_matrix, composite_samples)
from prepared_face import run_natural_prepared_face, run_purity_prepared_face


@dataclass(frozen=True)
class HostBudget:
    """Explicit allocation ceilings, not a guarantee of available host RAM.

    max_output_bytes limits each prospective FP64 image/sample output. Source,
    destination, model and tile temporaries coexist and need additional memory.
    Array-like conversion itself is owned by the caller; supply ndarrays when
    pre-allocation rejection is required (including broadcast views).
    """
    max_pixels: int = 25_000_000
    max_output_bytes: int = 600_000_000
    max_sample_points: int = 262_144

    def __post_init__(self):
        for name in ("max_pixels", "max_output_bytes", "max_sample_points"):
            value = getattr(self, name)
            if not isinstance(value, int) or isinstance(value, bool) or value < 1:
                raise ValueError(name + " must be a positive integer")


DEFAULT_BUDGET = HostBudget()


def _budget(value):
    if not isinstance(value, HostBudget):
        raise ValueError("budget must be a HostBudget")
    return value


def _raster_budget(a, budget, name):
    """Check shape-derived allocation costs before scanning pixel contents."""
    budget = _budget(budget)
    pixels = prod(a.shape[:2])
    if pixels > budget.max_pixels or prod(a.shape) * 8 > budget.max_output_bytes:
        raise ValueError(name + " exceeds the configured host raster budget")


@dataclass(frozen=True)
class FullImageResult:
    image: np.ndarray
    source_to_crop: np.ndarray
    generated_crop: np.ndarray
    native_pixel_parity: bool = False
    complete_style: bool = False
    hdr_preserved: bool = False


def _unit(value, channels, name, budget):
    a = np.asarray(value)
    shape_ok = (a.ndim == 2 if channels is None else a.ndim == 3 and a.shape[-1] == channels)
    if not shape_ok or not a.shape[0] or not a.shape[1] or a.dtype.kind not in "fiu":
        raise ValueError(name + " has invalid shape/type")
    _raster_budget(a, budget, name)
    if not np.all(np.isfinite(a)) or np.any(a < 0) or np.any(a > 1):
        raise ValueError(name + " must contain finite unit-range samples")
    return a


def _affine(value):
    raw = np.asarray(value)
    if raw.shape != (3, 3) or raw.dtype.kind not in "fiu":
        raise ValueError("Expected a real numeric 3x3 affine matrix")
    m = raw.astype(np.float64)
    if not np.all(np.isfinite(m)):
        raise ValueError("Expected a finite 3x3 source-to-crop matrix")
    if not np.array_equal(m[2], [0., 0., 1.]):
        raise ValueError("Only an affine matrix is supported")
    scale = float(np.max(np.abs(m[:2, :2])))
    if scale == 0 or not np.isfinite(np.linalg.cond(m[:2, :2])) or np.linalg.cond(m[:2, :2]) > 1e12:
        raise ValueError("Singular or ill-conditioned crop transform")
    inv = np.linalg.inv(m)
    if not np.all(np.isfinite(inv)):
        raise ValueError("Nonfinite inverse crop transform")
    return m, inv


def bilinear_samples(image, x, y, *, border, budget=DEFAULT_BUDGET):
    """Continuous bilinear sampling; integer coordinates are pixel centres.

    `zero` applies the border independently to each of the four neighbours.
    `clamp` extends edge texels. Unlike the original CPU warp, no 1/32 fraction
    quantization or output-byte conversion is performed.
    """
    a = np.asarray(image)
    x, y = np.asarray(x), np.asarray(y)
    budget = _budget(budget)
    if any(v.dtype.kind not in "fiu" for v in (a,x,y)):
        raise ValueError("Sampler values must be finite real numbers")
    if a.ndim not in (2, 3) or min(a.shape[:2]) < 1 or (a.ndim == 3 and a.shape[2] < 1):
        raise ValueError("Invalid sampler image")
    _raster_budget(a, budget, "Sampler image")
    broadcast_shape = np.broadcast_shapes(x.shape, y.shape)
    count = prod(broadcast_shape)
    channels = 1 if a.ndim == 2 else a.shape[2]
    if count > budget.max_sample_points or count * channels * 8 > budget.max_output_bytes:
        raise ValueError("Sampler output exceeds the configured host sampling budget")
    if not np.isfinite(a).all():
        raise ValueError("Sampler image must be finite")
    x, y = np.broadcast_arrays(x.astype(np.float64), y.astype(np.float64))
    if not np.isfinite(x).all() or not np.isfinite(y).all():
        raise ValueError("Invalid sampler input")
    h, w = a.shape[:2]
    if border not in ("zero", "clamp"):
        raise ValueError("Unknown sampler border")
    if border == "clamp":
        x, y = np.clip(x, 0, w-1), np.clip(y, 0, h-1)
    else:
        # Bound coordinate magnitude before integer conversion; these exterior
        # points have no contributing neighbour and must stay exactly zero.
        x, y = np.clip(x, -2, w+1), np.clip(y, -2, h+1)
    ix, iy = np.floor(x).astype(np.int64), np.floor(y).astype(np.int64)
    fx, fy = x-ix, y-iy
    result_shape = x.shape + (() if a.ndim == 2 else (a.shape[2],))
    out = np.zeros(result_shape, dtype=np.float64)
    for dx, dy, weight in ((0,0,(1-fx)*(1-fy)), (1,0,fx*(1-fy)),
                           (0,1,(1-fx)*fy), (1,1,fx*fy)):
        xx, yy = ix+dx, iy+dy
        valid = (xx >= 0) & (xx < w) & (yy >= 0) & (yy < h)
        samples = a[np.clip(yy,0,h-1), np.clip(xx,0,w-1)]
        weight = weight * valid if border == "zero" else weight
        if a.ndim == 3: weight = weight[..., None]
        out += samples * weight
    return out


def crop_image(image, source_to_crop, *, budget=DEFAULT_BUDGET):
    """256x256 inverse warp with continuous bilinear and zero border."""
    src = _unit(image, 3, "image", budget)
    _, inverse = _affine(source_to_crop)
    y, x = np.mgrid[:256, :256]
    sx = inverse[0,0]*x + inverse[0,1]*y + inverse[0,2]
    sy = inverse[1,0]*x + inverse[1,1]*y + inverse[1,2]
    return bilinear_samples(src, sx, sy, border="zero", budget=budget)


def purity_crop_matrix(landmarks106, template106):
    """Verified Purity source->template->margin->offset transform.

    Native IDream independently uses the same least-squares similarity algebra,
    followed by margin=float32(.2) padding and zero pixel offsets. Caller must
    supply its selected ordered image landmarks and matching face_point template.
    Reusing the pure matrix algebra does not reuse any unverified face detector.
    """
    return natural_crop_matrix(landmarks106,template106,
                               margin=float(np.float32(.2)),offset_x=0.,offset_y=0.)


def project_neural_crop(image, generated_rgba, source_to_crop, neural_mask_red,
                        intensity, *, style, domain, tile_rows=64, budget=DEFAULT_BUDGET):
    """Project neural output using an explicit integer-lattice HQ convention.

    Source pixel centre (x,y) is mapped by source_to_crop. Crop coverage is
    [-0.5,255.5) on each axis. Model texels use those integer crop centres; the
    externally oriented 320x320 mask uses matching normalized edge coordinates.
    This declared rasterization avoids an implicit half-pixel guess about the
    original GPU mesh/texture origin. It is not claimed to be that GPU's policy.
    """
    if domain != SDR_DOMAIN:
        raise ValueError("An explicit SDR model-domain image is required")
    budget = _budget(budget)
    src = _unit(image, 3, "image", budget)
    generated = _unit(generated_rgba, 4, "generated_rgba", budget)
    mask = _unit(neural_mask_red, None, "neural_mask_red", budget)
    if generated.shape != (256,256,4) or mask.shape != (320,320):
        raise ValueError("Expected a 256x256 neural output and oriented 320x320 mask")
    matrix, _ = _affine(source_to_crop)
    if not isinstance(tile_rows, int) or isinstance(tile_rows, bool) or not 1 <= tile_rows <= 256:
        raise ValueError("tile_rows must be an integer in [1,256]")
    # Also validate style and intensity before allocating full-size output.
    composite_samples(style, np.zeros((1,4)), np.zeros((1,4)), np.zeros(1), intensity, domain=domain)
    h, w = src.shape[:2]
    # Four-channel generated samples are the largest sampler output per tile.
    row_limit = min(budget.max_sample_points // w, budget.max_output_bytes // (w * 4 * 8))
    if row_limit < 1:
        raise ValueError("One image row exceeds the configured host sampling budget")
    tile_rows = min(tile_rows, row_limit)
    out = np.array(src, dtype=np.float64, copy=True)
    xx = np.arange(w, dtype=np.float64)[None, :]
    for start in range(0, h, tile_rows):
        end = min(start+tile_rows, h)
        yy = np.arange(start,end,dtype=np.float64)[:,None]
        cx = matrix[0,0]*xx + matrix[0,1]*yy + matrix[0,2]
        cy = matrix[1,0]*xx + matrix[1,1]*yy + matrix[1,2]
        if not np.all(np.isfinite(cx)) or not np.all(np.isfinite(cy)):
            raise ValueError("Nonfinite projected coordinates")
        inside = (cx >= -.5) & (cx < 255.5) & (cy >= -.5) & (cy < 255.5)
        if not np.any(inside):
            continue
        gen = bilinear_samples(generated,cx,cy,border="clamp", budget=budget)
        mr = bilinear_samples(mask,(cx+.5)*(320/256)-.5,
                              (cy+.5)*(320/256)-.5,border="clamp", budget=budget)
        mr = np.clip(mr, 0, 1) * inside
        gen = np.clip(gen, 0, 1)
        source_rgba = np.concatenate((src[start:end], np.ones((end-start,w,1))),axis=-1)
        composed = composite_samples(style,source_rgba,gen,mr,intensity,domain=domain)
        weight = ((np.minimum(mr, gen[...,3]) if style == "natural_blush"
                   else mr * gen[...,3]) * intensity)
        active = inside & (weight > 0)
        # Preserve the original FP64-converted samples, including negative zero,
        # for every zero-weight/outside pixel instead of recomputing source*1+0.
        out[start:end][active] = composed[..., :3][active]
    return out


def run_neural_image_component(style, model_path, bytenn_library_path,
                               effect_library_path, image, source_to_crop,
                               neural_mask_red, intensity, *, domain, budget=DEFAULT_BUDGET):
    """Full-size supplied-geometry SDR neural component, with the real model.

    Natural source_to_crop may come from `natural_crop_matrix` with supplied
    106 landmarks/template. Purity requires its independently established
    source-to-crop matrix; this function does not substitute Natural alignment.
    Input channel order is RGB. Mask row zero
    must match the declared upper row of the neural crop, not an assumed PNG or
    GPU upload orientation. Output dimensions are identical to the input.
    """
    if domain != SDR_DOMAIN:
        raise ValueError("Only explicit encoded SDR domain is implemented")
    src = _unit(image, 3, "image", budget)
    mask = _unit(neural_mask_red, None, "neural_mask_red", budget)
    if mask.shape != (320,320):
        raise ValueError("Expected the supplied correctly oriented 320x320 neural mask")
    matrix, _ = _affine(source_to_crop)
    if style not in ("natural_blush", "purity2"):
        raise ValueError("Unknown style")
    composite_samples(style,np.zeros((1,4)),np.zeros((1,4)),np.zeros(1),intensity,domain=domain)
    cropped = crop_image(src, matrix, budget=budget)
    # Numerical interpolation can overshoot the convex range by machine eps.
    cropped = np.clip(cropped,0,1)
    rgba = np.concatenate((cropped,np.ones((256,256,1))),axis=-1)
    zeros = np.zeros((256,256))
    # ORT 1.30's event API alone can be too late for startup telemetry. Require
    # its documented process-start opt-out before importing the runtime.
    import os
    if os.environ.get("ORT_DISABLE_TELEMETRY") != "1":
        raise RuntimeError("Start the process with ORT_DISABLE_TELEMETRY=1 before loading ONNX Runtime")
    import onnxruntime as ort
    ort.disable_telemetry_events()
    if style == "natural_blush":
        result = run_natural_prepared_face(model_path,bytenn_library_path,cropped*255,
                                          rgba,zeros,intensity,domain=domain)
    else:
        result = run_purity_prepared_face(model_path,effect_library_path,bytenn_library_path,
                                         cropped*255,rgba,zeros,intensity,domain=domain)
    projected = project_neural_crop(src,result.generated_samples,matrix,mask,intensity,
                                   style=style,domain=domain,budget=budget)
    return FullImageResult(projected,matrix.copy(),result.generated_samples)


def run_landmark_image_component(style, model_path, bytenn_library_path,
                                 effect_library_path, image, landmarks106,
                                 template106, neural_mask_red, intensity,
                                 *, domain, budget=DEFAULT_BUDGET):
    """Same component with the model-specific verified 106-point crop matrix.

    Natural landmarks/template must already reflect any required pose/template
    adjustment; neither style's face detection or dynamic selection is guessed.
    """
    if style == "natural_blush":
        matrix = natural_crop_matrix(landmarks106,template106)
    elif style == "purity2":
        matrix = purity_crop_matrix(landmarks106,template106)
    else:
        raise ValueError("Unknown style")
    return run_neural_image_component(style,model_path,bytenn_library_path,
                                      effect_library_path,image,matrix,neural_mask_red,
                                      intensity,domain=domain,budget=budget)
