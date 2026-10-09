# ULike v1.9.72 camera restart repair

This source-pinned release is built from the exact public v1.9.71 / bundle v1.0.204. Only FrontPreview1936, CameraSession1965, PreviewLayout1922 diagnostic sampling, and version-only CameraTrace1965 / ProcessingTiming1947 are recompiled. All other GPU, pixel, save and runtime classes, twelve native payloads and unrelated app resources are retained and compared against serialized DEX/MPP baselines.

Run declare1972.py, build1972.py, validate1972.py and finalize1972.py with pinned inputs/tools and fresh work directories. Publication requires exact locally reviewed MPP hashes. host_camera1972.py uses controlled Android boundaries, not a physical Galaxy. Original APKS application, camera preview behavior and device save/GPU speed remain untested. Runtime incident logs, images, APKs and signing keys are excluded.

The archived workflow must equal .github/workflows/ulike1972-camera-restart-publish.yml. publish1972.py verifies six assets, creates an immutable download commit, atomically advances both Manager feeds using leased HEADs and retains every unrelated repository path. Read-only --local-only and --preflight-only do not write GitHub.
