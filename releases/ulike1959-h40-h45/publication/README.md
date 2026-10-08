# ULike v1.9.59 / Hiro Morphe Patches v1.0.192

H40–H45 improve perceived speed on the exact published ULike1.9.58 / bundle1.0.191 baseline. They parallelize independent preparation, reuse exact luma/chroma values, add dedicated four-pixel ARM NEON, reuse bounded exclusive workspaces, share protection/NR13 calculations, and transfer the exact first-stage 2×2 integer pyramid. Pixel arithmetic, rounding, resolution, NR1–NR13 strength and candidate counts, compression quality, save format and publication order remain preserved. The new smooth library replaces its existing eleventh installer row; the other ten native payloads and transaction code stay byte-identical.

The workflow compiles pinned tools and current sources, executes the inherited NR9–NR13 acceptance/fallback/final-save suites, compares against frozen .58 Java/C oracles and runs actual AArch64 NEON versus scalar under QEMU. Source consistency, serialized DEX preservation and package resource deltas are hard gates. Original-APKS application and physical Galaxy quality/speed are untested.

Run `build1959.py`, `validate1959.py`, `finalize1959.py`, then `publish1959.py --local-only` and `--preflight-only`. Publication verifies hash-pinned draft assets and immutable URLs before advancing both Manager feeds with branch leases. Other application resources, unrelated repository paths and prior changelog are preserved. A concurrent bundle advancement fails closed.

Refresh the Morphe Manager patch source, reapply to unmodified ULike5.6.2(740), then install the generated app. Source refresh alone does not update the installed app.
