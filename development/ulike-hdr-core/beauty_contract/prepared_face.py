"""Execute the real Natural model between explicit prepared face boundaries."""
from __future__ import annotations

from dataclasses import dataclass

import numpy as np

from contracts import (prepare_natural_tensor, prepare_purity_tensor,
                       model_output_unit_channels, composite_samples)


@dataclass(frozen=True)
class PreparedFaceResult:
    model_input: np.ndarray
    model_output: np.ndarray
    generated_samples: np.ndarray
    composite: np.ndarray
    complete_style: bool = False
    hdr_preserved: bool = False
    native_pixel_parity: bool = False


def run_natural_prepared_face(model_path, library_path, prepared_channels,
                             source_rgba_samples, mask_red_samples, intensity,
                             *, domain):
    """Real model -> continuous post -> actual Natural shader blend.

    Add the sibling model_replay directory to PYTHONPATH. Model and library paths
    are passed to that module's pinned Natural loader; a Purity plan or arbitrary
    external session cannot be substituted. Images/mask must already be matched to
    the same 256x256 neural crop sampling coordinates. No crop, camera colour
    conversion, mask generation, later makeup, HDR extension or saving occurs.
    The original model alpha competes with mask.r using min, not multiplication.
    """
    from pathlib import Path
    from baoman_replay import load_pinned, run_tensor

    model_input = prepare_natural_tensor(prepared_channels, domain=domain)
    # Validate all external blend arguments before expensive model inference.
    empty = np.zeros((256, 256, 4), dtype=np.float64)
    composite_samples("natural_blush", source_rgba_samples, empty,
                      mask_red_samples, intensity, domain=domain)
    plan, metadata = load_pinned(Path(model_path), Path(library_path))
    if metadata["model_sha256"] != "e64f0772bb857e4d990789237c1007b62fb86020a7edcfb0c3a61a7bedc6e2c0":
        raise ValueError("The Natural wrapper requires the pinned tt_baoman model")
    raw = run_tensor(plan, model_input)
    generated = model_output_unit_channels(raw, domain=domain)
    composite = composite_samples("natural_blush", source_rgba_samples,
                                  generated, mask_red_samples, intensity,
                                  domain=domain)
    return PreparedFaceResult(model_input, raw, generated, composite)


def run_purity_prepared_face(model_path, effect_library_path, bytenn_library_path, prepared_channels,
                            source_rgba_samples, mask_red_samples, intensity,
                            *, domain):
    """Real pinned Purity v0 model with no-extra-channel normalization and blend.

    Has the same explicit prepared-coordinate/domain boundaries as the Natural
    wrapper. Purity's observed shader multiplies model alpha by external mask.r.
    The original 8-bit output conversion is replaced by continuous float values.
    """
    from pathlib import Path
    from purity_replay import load_pinned_purity
    from baoman_replay import run_tensor

    model_input = prepare_purity_tensor(prepared_channels, domain=domain)
    empty = np.zeros((256, 256, 4), dtype=np.float64)
    composite_samples("purity2", source_rgba_samples, empty,
                      mask_red_samples, intensity, domain=domain)
    plan, metadata = load_pinned_purity(Path(model_path), Path(effect_library_path),
                                      Path(bytenn_library_path))
    if metadata["model_sha256"] != "0d60ea7e684f32628daac031cb37fd32c50bf898aa3fb62bcbc25673b5725cad":
        raise ValueError("The Purity wrapper requires the pinned tt_goodlike model")
    raw = run_tensor(plan, model_input)
    generated = model_output_unit_channels(raw, domain=domain)
    composite = composite_samples("purity2", source_rgba_samples,
                                  generated, mask_red_samples, intensity,
                                  domain=domain)
    return PreparedFaceResult(model_input, raw, generated, composite)
