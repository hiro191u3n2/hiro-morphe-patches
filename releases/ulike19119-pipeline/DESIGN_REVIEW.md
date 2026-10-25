# ULike 1.9.119: exact noise arithmetic candidates

This release adds two independently qualified computation candidates, without reducing image resolution or noise settings. Strong program 48 shares the exact luma-ratio quotient and selects the reviewed exact binary32 divider; the CPU front candidate skips only blocks whose blend is provably zero.

## Identity and fallback

The complete published 1.9.118 source graph is frozen under `src/baseline19118`, verified against the fixed published manifest. Its GPU, NR and smoothing natives are rebuilt byte-identically; all twelve legacy native payloads remain unchanged. This preserves the actual implementation identity used by existing first-shot tile8 certificates. Current bridge and candidate changes are separately included in each candidate implementation identity and in the full release source declaration. Old certificates cannot authorize either new candidate.

Two additional arm64 libraries use separate JNI classes and independent qualification. Candidate sessions have separate GPU ownership and explicit combined memory accounting; foreground refusal or failure falls back through the unchanged legacy native implementation. Front qualification compares exact output twice and candidate worst time against baseline best minus five percent. Strong qualification checks the independent corpus, then measures a synthetic complete 3060×4080 and 4080×3060 Strong stage with 256-row strips (16/12 strips), two GPU banks and complete readback. The candidate pays its model transfer once per stage, matching the actual workload; the legacy route excludes its already-resident model setup. Common input generation and full output comparison are included in both timings. Both orders and two trials remain required, with candidate worst at least ten percent below baseline best. Cancellation, exact output checks and incomplete-certificate rejection remain in force.

## Build and evidence

The build pins Temurin 21.0.8+9, NDK r27c, Android API jar, D8, Morphe, published standalone 1.9.118 and aggregate 1.0.285. Installer changes append exactly two rows while inverse proofs preserve all prior rows and code. Exact JNI declaration inventory is reviewed. DEX determinism, all helper linkage, inherited shutter/capture contracts, native alignment and dependency checks remain mandatory.

The inherited regression suite executes against the frozen published baseline and is labelled accordingly. Separate current-source tests execute the actual candidate shader and C sources, reject injected arithmetic faults, and verify the Java admission, ownership, memory, cancellation and fallback hooks. Actual original-APKS application binds both final MPP hashes. Neither desktop GLES nor QEMU establishes target Android speed, driver equivalence or physical capture latency.

## Publication

The user authorized automatic publication after completion. Fresh positive build, host and exact original-APKS evidence are required before publishing. Immutable download assets and atomic main/dev feed updates preserve unrelated app scopes, including the current Piko compatibility withdrawal. Device speed and quality validation remain explicitly unverified.
