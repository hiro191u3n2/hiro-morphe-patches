# ULike v1.9.61 / Hiro Morphe Patches v1.0.194

GX11–GX23 extend conditional GPU compute on byte-pinned ULike1.9.60 / bundle1.0.193. Existing calculations, precision, reference order, resolution, save settings and other application payloads stay preserved. Unsupported stages retain CPU fallback. Face detection and double geometry remain CPU; FP64 single-noise transforms remain CPU where hardware precision is unavailable; beauty-surface and direct encoder integration remain unsupported at the current boundary.

One existing GPU library is rebuilt and its twelfth installer-row fingerprint is updated; the other eleven native payloads, row count, transaction code, camera/save policies and unrelated application resources stay preserved. Hard gates require frozen .60 output comparisons, actual Mesa execution of new kernels, batch/ticket/pool/fault handling, full save/geometry pipeline parity, reviewed source graph, deterministic serialized DEX and JVM inverse audits, and exact package deltas. Physical Android quality/speed and original APKS application remain untested.

Run `declare1961.py`, `build1961.py`, `validate1961.py`, `finalize1961.py`, then `publish1961.py --local-only` and `--preflight-only`. Publication verifies draft assets and immutable downloads before atomically advancing both Manager feeds with branch leases; concurrent advancements fail closed.

Refresh the Morphe Manager source, reapply to unmodified ULike5.6.2(740), and install the generated app. Source refresh alone does not update the installed app.
