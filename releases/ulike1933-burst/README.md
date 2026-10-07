# ULike 1.9.33 — proposals 47, 50 and 60

This release extends the approved ULike 1.9.32 payload from the 1.8.8 lineage.
The selected work is same-exposure multiframe noise reduction, motion-aware
dark-scene fusion and image-processing optimization. Existing image-quality
settings, HEIF output and camera-startup/recovery fixes are retained.

## Source layout

| Source | Responsibility |
| --- | --- |
| `FusionPixels1933.java` | NV21 image alignment, rejection and fusion kernel |
| `BurstCapture1933.java` | Additional frame acquisition, shot association and bounded fallback |
| `CapturePolicy1933.java` | Exposure, movement and frame-budget decisions |
| `FastPixels1933.java` | Pure-Java optimized pixel resampling |
| `FastResize1933.java` | Bitmap data access and bounded parallel resampling |
| `Transform1933.java` | Exact reviewed DEX changes and metadata updates |
| `VerifyBurst1933.java` | DEX change inventory and preservation checks |
| `host_burst1933.py` | Required fusion, capture, metadata, fast-path and real capture/fusion integration regression suites |
| `SeedBurst1933.java` / `extract_seed1933.py` | Pinned native-method extraction from the original APK |
| `build1933.py` | Reproducible standalone and integrated MPP construction |
| `validate1933.py` | Original-APKS application and emitted APK verification |
| `finalize1933.py` | Source/QA packaging bound to exact validated MPPs |

Compile-only stubs supply the existing application's Java interfaces to javac.
They are excluded from the Android runtime payload except explicitly reviewed
method templates copied into existing classes by the transformer. Host classes
and test fixtures are excluded from the payload. The source package contains
their sources for reproducibility.

## Quality and compatibility boundaries

Proposals 47 and 50 run when existing NR is enabled with strength greater than zero on supported high-resolution YUV still-image paths for front and rear cameras; when manual exposure is unavailable they use AE/AWB locks for same-exposure fusion, and when the required controls are unavailable they retain the existing single-shot path.

Frame availability, exposure matching and image motion determine whether
multiple frames can contribute. An unusable sequence must return to a valid
single-frame capture; extra frames must not be delivered to beauty processing
as separate user photos. The exact frame budget and rejection behavior are
defined in the capture and fusion source and exercised by the host suites.
Each fused group contains at most four frames. When the initial two-frame motion
probe leads to a different exposure, acquiring a new group can bring the total
sensor-capture count to six; the old and new exposure groups are not mixed.

The existing NR and sharpening settings remain relevant. The fast resampling
path is checked against the retained reference implementation. Processing uses
at most four workers. Additional image acquisition may increase total capture
time, so kernel optimization does not imply every shot saves faster.

The withdrawn 1.9.17/10bit branch and removed diagnostics or original-photo
archival features are not restored. Other application patches in the current
integrated baseline retain their bytes.

## Build and validation

Use the pinned JARs and Temurin 21.0.8+9. All commands require explicit input and
work paths. Build and validation directories must be fresh. The publication
manifest identifies the exact current integrated baseline.

```sh
python3 src/build1933.py --input INPUT --tools TOOLS --work BUILD --output DIST --jdk JDK
python3 src/validate1933.py --original ORIGINAL_APKS --build BUILD --dist DIST --tools TOOLS --work VALIDATION --jdk JDK
python3 src/finalize1933.py --dist DIST --source src --validation VALIDATION/validation.json
```

The full reviewed manifest must be populated only after successful validation.
The template is intentionally incapable of passing publication validation.
See `publication/README.md` for the exact-byte release and Manager-feed workflow.

Host pixel/control tests, original-APKS application, resource rebuilding and DEX
type analysis do not execute Android/ART or camera hardware. Galaxy capture,
fusion, beauty and HEIF behavior, visible image quality, and capture/save latency
are unverified on a physical device. No APK/APKS, user photo or signing key is a
public release asset.
