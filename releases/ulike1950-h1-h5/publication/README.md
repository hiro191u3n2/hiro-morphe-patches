ULike v1.9.50 / Hiro Morphe Patches v1.0.183 publication

This publisher pins the exact released ULike 1.9.49 / bundle 1.0.182 baseline and all six reviewed assets. It verifies the permitted resource changes, compiled production/source fingerprints, executable host C/JNI and Mesa shader evidence, and both original-APKS application results.

The existing GPU library row is updated in place; one integrity-pinned ARM64 core-denoise library row is added. The previous speed1935 kernel and every non-ULike application resource remain byte-identical. Installer rollback, manifest and file-integrity safeguards remain enforced.

Read-only local verification:

    python3 publication/publish1950.py --dist DIST --repo REPO --expected manifest.json --local-only --baseline INPUT/Hiro_Morphe_Patches_v1.0.182.mpp --standalone-baseline INPUT/ULike_HQ_Texture_Online_v1.9.49.mpp

Remote read-only preflight:

    python3 publication/publish1950.py --dist DIST --repo REPO --expected manifest.json --preflight-only

Publication creates the pinned release assets and immutable download commit, then advances main and dev Manager feeds atomically from their leased current heads. It verifies public asset/feed bytes and stores a publication receipt. The archived workflow must match the executed workflow exactly.

Physical Galaxy speed and image quality are unverified. Host equivalence and original-APKS rebuild checks do not imply Android camera execution or a measured device speedup.
