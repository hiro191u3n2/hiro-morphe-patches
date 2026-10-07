# ULike 1.9.34 — Q1, Q4, Q6, Q7, Q11 and Q13

This release extends approved ULike 1.9.33 and integrated bundle 1.0.166,
retaining the 1.8.8 lineage. The requested scope is best-reference fusion,
face-feature-aware correction, local noise estimation, longer-period chroma
moire handling, precise YUV conversion and combined image transforms.

| Candidate | Implemented scope | Boundary |
| --- | --- | --- |
| Q1 | Noise-aware sharp reference selection from the existing same-exposure group | Ties, unreliable evidence, motion and exposure/colour mismatch retain the initial frame; no added captures |
| Q4 | Exact saved-image eye geometry, conservative cheek/forehead zones and eye/mouth/outline protection | Geometric approximation; precise lip contours and semantic hair segmentation are not implemented |
| Q6 | Source-domain spatial noise estimates and locally bounded NR, shared with shadow correction | User NR level is an upper bound; OFF remains OFF |
| Q7 | Period 6/8/12/16 chroma-pattern detection in addition to short periods | Conservative repeated-pattern gates; real isoluminant colour motifs cannot always be distinguished from false colour |
| Q11 | Byte-exact three-plane YUV packing shared by single-frame and burst paths | Preserves sample values and SDK colour conversion; does not guess matrix/range metadata |
| Q13 | Exact pixel permutation for eligible quarter-turn/integer-crop saves | Existing one-pass resampling handles scaling; native beauty mesh processing is preserved |

Face detection is bounded to a 640-pixel analysis image, derived from the exact
bitmap entering the save pipeline. Facial regions are conservative geometry,
not semantic parsing. Weak/absent detections, uncertain profiles, overlaps,
unsupported operation and insufficient memory contribute no extra facial prior.
No stale preview landmarks are substituted for the actual saved image.

The Q11 fix removes the single-frame path's assumption that V-plane memory also
contains the interleaved U plane. All three plane row/pixel strides, buffer
position and limits are validated before destination modification. Full-frame,
even-sized YUV input is required; invalid input uses the existing error path.

The new image analysis can add work. These changes do not promise a shorter total
save time; the exact Q13 cases avoid interpolation while other quality work may
increase CPU cost. No hardware speed improvement is claimed.

## Build and validation

Use the pinned JARs and Temurin 21.0.8+9. Build and validation directories must be
fresh. The manifest supplies the exact baseline and toolchain fingerprints.

```sh
python3 src/build1934.py --input INPUT --tools TOOLS --work BUILD --output DIST --jdk JDK
python3 src/validate1934.py --original ORIGINAL_APKS --build BUILD --dist DIST --tools TOOLS --work VALIDATION --jdk JDK
python3 src/finalize1934.py --dist DIST --source src --validation VALIDATION/validation.json
```

`validate1934.py` applies standalone and integrated patches to the pinned original
ULike 5.6.2 (740) APKS. It requires arm64-v8a-only output, exact payload method
contracts, complete DEX integrity and register/type analysis. The new quality
DEX inventory, existing native burst ABI and front camera lifecycle ABI are
checked against the real APK. Compile stubs and host fixtures are not runtime
payload classes.

The publication manifest must be filled only after successful validation and
independent review of the exact changes. `manifest.template.json` is deliberately
unable to pass publication validation. See `publication/README.md` for the
reproducible build, exact-asset checks and atomic main/dev Manager-feed update.

Host regressions, original-APKS application and DEX analysis do not execute
Android/ART or a physical camera. Galaxy capture, visible quality, beauty/HEIF
output and capture/save latency remain device-unverified. The prior burst frame
budget remains up to four frames per fused group, and up to six total captures
when an initial two-frame motion probe requires a new exposure group.

The withdrawn 1.9.17/10bit branch and removed diagnostics/photo archival are not
restored. Other applications retain the exact current integrated baseline.
No APK/APKS, user photo or signing key is a public release asset.
