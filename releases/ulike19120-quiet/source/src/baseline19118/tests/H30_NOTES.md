# H30 exact no-op residual optimization

The published 1.9.55 single-image NR1–NR4 route remains the normal eligible-image path. H30 changes only the retained residual cleanup fallback. It does not replace or reenable residual cleanup in the new route.

The existing periodic texture confidence is calculated before the 7×7 weighted aggregate. A value of exactly 256 forces the existing final flatness to exactly zero, independent of edge means, weights and face coordination. Other confidence values use the original operations and rounding. Original output seeds, alpha rejection and valid halo bounds are preserved.

* Java cached and uncached paths check the unchanged texture predicate first.
* The fused GPU finish shader checks after the workgroup barrier and after saved output seeding, so no invocation skips required synchronization.
* Native final-pixel calls opt in through unused metadata bit 30. Native scalar/NEON paths retain complete original summaries for ordinary callers; opted-in protected lanes report an inactive summary. Partly active NEON groups retain their vector arithmetic; fully protected groups skip all weighted taps.

`host_h30_1956.py` compares against byte-pinned published .55 sources. It executes Java cached/uncached code, actual JNI, native scalar C and actual Mesa GLES compute. Generated test-only counter probes measure executed weighted tap loops. The shipped classes/kernels have no counters. `ULIKE_REQUIRE_ARM_NEON=1` (also accepted: `ULIKE_ARM_QEMU_REQUIRED=1`) requires the ARM native differential harness under qemu-aarch64; the report explicitly states whether it ran. No host/emulator time is advertised as Galaxy speed.
