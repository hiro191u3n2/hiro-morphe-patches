ULike v1.9.51 / Hiro Morphe Patches v1.0.184 publication

The publisher pins the released ULike 1.9.50 / bundle 1.0.183 baseline, verifies the six freshly generated output hashes, compares the source archive with the checked-out implementation and admits only the reviewed H6-H11 resource changes. The legacy speed1935 kernel and every non-ULike application resource must remain byte-identical. Native ELF resources are fingerprinted individually.

The original ULike 5.6.2 (740) APKS could not be materialized in this environment. This release's validation evidence, QA, active policy and receipt must all state `original_apk_apply_tested: false`. The v1.9.50 APKS result only describes the baseline; it cannot establish that v1.9.51 applies to the original app. Galaxy speed and camera image quality remain untested.

The checked-in workflow downloads both baselines and tools by fixed SHA, runs the host native/shader/pixel tests, generates fresh evidence and a package manifest, checks the generated manifest against the reviewed source declaration, and performs local and remote read-only preflights. If every gate passes, it creates immutable release assets and advances the main and dev Manager feeds atomically. The archived workflow must match the executed workflow byte-for-byte.

Read-only local verification after building and finalization:

    python3 publication/publish1951.py --dist DIST --repo REPO --expected GENERATED_MANIFEST --local-only --baseline INPUT/Hiro_Morphe_Patches_v1.0.183.mpp --standalone-baseline INPUT/ULike_HQ_Texture_Online_v1.9.50.mpp

The public release and Manager update occur only after successful preflight. Applying the patch to the unmodified APKS and measuring the Galaxy are separate follow-up checks.
