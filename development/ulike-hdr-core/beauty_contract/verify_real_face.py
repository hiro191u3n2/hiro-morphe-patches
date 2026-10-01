"""Verify real Natural normalize -> model -> float post -> masked blend.

Uses synthetic prepared samples, not a real face or a claim of visual matching.
Writes only scalar QA and hashes; no weights/templates or image data.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np

from contracts import SDR_DOMAIN, NATURAL_MARGIN, natural_crop_matrix
from extract_natural_template import extract_template
from prepared_face import run_natural_prepared_face, run_purity_prepared_face


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("model", type=Path)
    p.add_argument("bytenn_library", type=Path)
    p.add_argument("effect_library", type=Path)
    p.add_argument("qa_json", type=Path)
    p.add_argument("--style", choices=("natural_blush", "purity2"), default="natural_blush")
    args = p.parse_args()
    yy, xx = np.mgrid[0:256, 0:256]
    samples = np.stack((xx + .125, yy + .25, (xx + yy) * .5 + .375), axis=-1)
    samples = np.minimum(samples, 255.)
    source = np.concatenate((samples / 255., np.ones((256, 256, 1))), axis=-1)
    mask = np.where(xx < 128, 0., np.minimum(1., (xx-128)/127.))
    if args.style == "natural_blush":
        result = run_natural_prepared_face(args.model, args.bytenn_library, samples,
                                          source, mask, .7, domain=SDR_DOMAIN)
    else:
        result = run_purity_prepared_face(args.model, args.effect_library, args.bytenn_library,
                                         samples, source, mask, .7, domain=SDR_DOMAIN)
    assert result.composite.shape == source.shape
    assert result.composite.dtype == np.float64
    assert np.all(np.isfinite(result.composite))
    assert np.all((result.composite >= 0) & (result.composite <= 1))
    assert np.array_equal(result.composite[:, :128], source[:, :128])
    assert not result.complete_style and not result.hdr_preserved and not result.native_pixel_parity
    assert np.any(result.generated_samples * 255 != np.rint(result.generated_samples * 255))
    # Independent per-sample shader expression, including model alpha and mask.
    chosen = [(20, 12), (100, 170), (180, 240), (255, 255)]
    for y, x in chosen:
        ma, ga = float(mask[y, x]), float(result.generated_samples[y, x, 3])
        a = (min(ma, ga) if args.style == "natural_blush" else ma * ga) * .7
        expected = source[y, x] * (1-a) + result.generated_samples[y, x] * a
        expected[3] = 1
        np.testing.assert_allclose(result.composite[y, x], expected, atol=1e-15)
    template = extract_template(args.effect_library)
    source_landmarks = (template - [.1, -.2]) / .002
    crop = natural_crop_matrix(source_landmarks, template)
    expected = (template.astype(np.float64)+NATURAL_MARGIN)*(256/(1+2*NATURAL_MARGIN)) + [0,15]
    observed = (crop @ np.c_[source_landmarks, np.ones(106)].T).T[:, :2]
    # Float32 synthetic landmarks introduce small represented-coordinate error.
    matrix_error = float(np.max(np.abs(expected-observed)))
    assert matrix_error < 1e-10
    qa = {"status":"PASS_REAL_PREPARED_FACE_COMPONENT", "style":args.style,
          "model_sha256":hashlib.sha256(args.model.read_bytes()).hexdigest(),
          "bytenn_library_sha256":hashlib.sha256(args.bytenn_library.read_bytes()).hexdigest(),
          "effect_library_sha256":hashlib.sha256(args.effect_library.read_bytes()).hexdigest(),
          "input":"synthetic fractional SDR code samples, explicitly prepared crop",
          "tensor_shape":list(result.model_output.shape),
          "raw_tanh_min":float(result.model_output.min()),
          "raw_tanh_max":float(result.model_output.max()),
          "output_precision":"FP64 post/composite after FP32 model; no uint8 intermediate",
          "output_sha256":hashlib.sha256(result.composite.tobytes()).hexdigest(),
          "zero_mask_exact_preservation_samples":128*256,
          "independent_composite_sample_checks":len(chosen),
          "additional_natural_crop_check":{
              "template_sha256":hashlib.sha256(template.tobytes()).hexdigest(),
              "template_points":106,"matrix_max_error":matrix_error,
              "not_part_of_prepared_face_inference":True},
          "complete_style":False,"face_detector_tested":False,"hdr_preserved":False,
          "native_pixel_parity":False,"android_integrated":False}
    args.qa_json.write_text(json.dumps(qa,indent=2)+"\n")
    print(json.dumps(qa,indent=2))


if __name__ == "__main__": main()
