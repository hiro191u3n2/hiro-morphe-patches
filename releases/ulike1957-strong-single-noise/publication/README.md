# ULike v1.9.57 / Hiro Morphe Patches v1.0.190

NR5–NR8 add stronger single-image noise reduction on the immutable ULike1.9.56 / bundle1.0.189 baseline. The retained NR1–NR4 stage and H28–H33 helpers/native resources remain unchanged. NR8 is bounded quarter-scale same-image 3×3 patch NLM in measured-noise dark flat regions, not a complete NL-Bayes/BM3D implementation.

The declaration pins baseline bytes, toolchain, exact changed/added resources and production helper roots. A new optional `libulike_strong1957.so` is the tenth installer row; all nine inherited payloads and installer rows are preserved. No multi-frame capture is enabled.

The workflow downloads pinned official SDK tools and NDK r27c, uses Temurin21.0.8+9, builds packages from checked-out sources, executes current-source Java and fresh JNI algorithm/pipeline/fallback suites, validates source hashes and serialized DEX preservation, then finalizes six hash-pinned assets. Original-APKS application and physical Galaxy quality/speed are explicitly untested. A source archive excludes photographs, APKs, signing keys, compiler outputs and temporary host files.

`publish1957.py --local-only` validates local artifacts without remote writes. `--preflight-only` checks both active Manager feeds, lineage, current branch heads, asset inventory and update parsing without remote writes. Publication creates a draft Release, verifies all assets before exposing it, creates immutable download URLs, then updates main/dev Manager feeds atomically as direct descendants of their leased heads. Unrelated files and the prior changelog are retained. The verified receipt is committed to both branches and uploaded as an additional Release asset.

Run the build, `validate1957.py`, `finalize1957.py`, local publisher validation and preflight before publication. The archived workflow must exactly match the active workflow. Concurrent bundle advancement or partial prior publication fails closed for inspection.

After updating the Morphe Manager patch source, reapply to unmodified ULike5.6.2(740) and install the generated app. A source refresh alone does not modify the installed app.
