"""Small FP32 operator references; these functions do not load or guess vendor weights."""
from __future__ import annotations

import numpy as np


def bilinear_half_pixel_2x_nchw(x: np.ndarray) -> np.ndarray:
    """Static native evidence: dst*.5-.25, floor, zero-fraction edge clamp, 2x H/W.

    Evidence: libbytenn.so (cedda347...ad7853), function 0x6ff40,
    coordinate/interpolation setup at 0x70474..0x70528. N/C are not resized.
    This is an independent formula reference, not execution of the Android kernel.
    """
    if x.dtype != np.float32 or x.ndim != 4 or min(x.shape) <= 0:
        raise ValueError("nonempty NCHW float32 tensor required")
    if not np.isfinite(x).all():
        raise ValueError("nonfinite tensor")
    n, c, h, w = x.shape
    if x.size > 8_000_000:
        raise ValueError("reference tensor bound")

    def coordinates(size: int):
        p = np.arange(size * 2, dtype=np.float32) * np.float32(.5) - np.float32(.25)
        index = np.floor(p).astype(np.int64)
        fraction = p - index.astype(np.float32)
        edge = (index < 0) | (index >= size - 1)
        index = np.clip(index, 0, size - 1)
        fraction[edge] = 0
        return index, np.minimum(index + 1, size - 1), fraction

    y0, y1, fy = coordinates(h)
    x0, x1, fx = coordinates(w)
    a = x[:, :, y0, :][:, :, :, x0]
    b = x[:, :, y0, :][:, :, :, x1]
    d = x[:, :, y1, :][:, :, :, x0]
    e = x[:, :, y1, :][:, :, :, x1]
    top = a * (np.float32(1) - fx)[None, None, None, :] + b * fx[None, None, None, :]
    bottom = d * (np.float32(1) - fx)[None, None, None, :] + e * fx[None, None, None, :]
    return top * (np.float32(1) - fy)[None, None, :, None] + bottom * fy[None, None, :, None]


def resize_onnx_node(name: str, source: str, target: str, shape: tuple[int, ...]):
    """Return the verified 2x resize node and its explicit int64 sizes initializer."""
    import onnx
    from onnx import helper, numpy_helper
    if len(shape) != 4 or any(not isinstance(v, int) or v <= 0 for v in shape):
        raise ValueError("concrete positive NCHW shape required")
    sizes_name = name + ".sizes"
    sizes = numpy_helper.from_array(np.asarray([shape[0], shape[1], shape[2]*2, shape[3]*2],
                                               dtype=np.int64), sizes_name)
    node = helper.make_node("Resize", [source, "", "", sizes_name], [target], name=name,
                            mode="linear", coordinate_transformation_mode="half_pixel",
                            exclude_outside=0, antialias=0)
    # antialias is an explicit opset-18 setting; older opsets must not silently reinterpret it.
    return node, sizes
