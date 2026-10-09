# ULike v1.9.69 / Hiro Morphe Patches v1.0.202

GPU status visibility built from exact published ULike v1.9.68 / bundle v1.0.201. This update changes the processing diagnostics UI only: show an explicit unmeasured GPU row before the first photo and add a fourth diagnostic-menu option that opens the complete recorded processing details in a dialog. Recorded GPU/CPU counts remain scoped to the measured strong-noise sections; an unknown result is never presented as verified GPU execution.

Only the `ProcessingTiming1947` and `CameraTrace1965` Java families are replaced. The published v1.9.68 preview recovery, camera controls, GPU admission and runtime helpers, pixel algorithms, save encoding, all twelve native payloads, native method resources, unrelated apps, and executable patch loader code are retained.

The complete source graph, exact baseline archives, Java toolchain, and locally reviewed MPP bytes are pinned. GitHub Actions rebuilds both MPP packages, runs host UI/logging regressions, validates retained DEX and resource preservation, packages complete source and static QA, verifies downloaded release bytes, and atomically advances the main/dev Manager feeds under leased HEADs. User incident logs, photos, original applications, and signing material are excluded.

Host tests use the actual changed Java sources with modeled Android UI/storage. No fresh physical GPU execution, Galaxy speed measurement, device UI verification, quality measurement, or original APKS application is claimed. Manager source updates require reapplying one of the standalone or combined patch packages to the original supported ULike application and installing the rebuilt application.
