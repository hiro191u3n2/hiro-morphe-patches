# ULike 1.9.32 — requested image quality changes

This release implements candidates 1, 2, 8, 12, 15 and 20 on the exact ULike 1.9.31 baseline. It retains the approved 1.8.8 lineage and the existing front/rear camera restart and gesture fixes.

## Processing

Anti-aliased reduction may run before denoising to bound large-input work. Enlargement always follows source-domain denoise and chroma cleanup. Crop-aware premultiplied-alpha resampling uses scaled Lanczos2 for reduction and clamped Catmull-Rom for enlargement. Noise plans combine actual image evidence with ISO, exposure and physical-lens metadata only when the capture image timestamp and callback/bitmap identity match. Successful post-tuning beauty updates supply an immutable per-shot smoothing snapshot. Existing user OFF settings remain OFF and selected levels remain upper bounds. The existing halo-suppression setting controls local-extremum clamping in the final sharp stage; disabling it still retains bounded gain and RGB channel limits.

Additional shadow smoothing gets a local skin/edge-aware budget. Short-period directional chroma patterns are handled conservatively before enlargement. Final luma sharpening uses the measured noise and output scale. Existing HEIF encoding and four-worker NR scheduling remain in place. Owned working bitmaps are released on failure, while the caller-owned input remains available to the prior normalization path.

## Reproduction

Use JDK21, the three jars pinned in manifest.json and the two baseline MPP files. No device or camera runtime is required for host regression tests. The source ZIP contains only the watermark method extracted from the original app as a pinned transformation seed; it does not contain the original APK/APKS.

```sh
python src/build1932.py --input /path/to/baseline --tools /path/to/tools --work /fresh/build --output /path/to/dist --jdk /path/to/jdk
python src/validate1932.py --original /path/to/original.apks --build /fresh/build --dist /path/to/dist --tools /path/to/tools --work /fresh/validation --jdk /path/to/jdk
python src/finalize1932.py --dist /path/to/dist --source src --validation /fresh/validation/validation.json
```

The complete publication manifest is committed beside this file. The packaged build manifest excludes artifact hashes to avoid a self-referential source ZIP. The publication workflow rebuilds, checks exact fingerprints against the original-APKS-tested bytes, then atomically updates main and development Manager feeds.

## Verification limits

Host tests cover numeric kernels, crop/rotation/order, bitmap ownership/fallbacks, worker strip boundaries and per-capture context isolation. DEX verification reconstructs pre-hook methods and verifies unrelated methods/classes remain unchanged. Both standalone and integrated MPPs must apply to original ULike5.6.2(740), rebuild successfully and pass method-contract/type checks. These are not Android/ART execution, device camera testing or Galaxy image-quality/performance measurements. Ambiguous low-saturation real periodic colors can resemble chroma moire; no general semantic classifier is claimed.
