# Release137 publication evidence

OneBack1.1.30 / bundle321 implements the requested profile→Reels→next Back exit
sequence from the exact318 baseline. Physical-device behavior is unconfirmed.

## Completed validation

-192 frozen source files;35 host suites passed. New suite32 cases /312 assertions.
- Two independent live-compatible builds have identical aggregate/single MPP and
  QA bytes. Aggregate48,067,555 bytes, SHA-256
  `b466108dcb72831986be9a9292cbbdb3432ebd7a44bf63065740a2ff69646901`.
- Original native contract57 assertions; independent Piko variant contract120
  assertions, including actual live append-catch reproduction and8 corrupt
  variants rejected. Global native digest checks are not relaxed.
-282 unrelated and17 owned loader definitions preserved;3 owned changed and2
  added,22 owned total. Runtime93 preserved,5 changed,6 added,104 total.
- Manager original-only1/1 and official Piko default64+Hiro65/65 in both requested
  source orders passed. Six independent audits per final output;12 native method
  bodies. Feature audit86 assertions per output. Additional Piko output audit173
  assertions for each mixed output. No claim of physical-device validation or all
  optional68-selection configurations. Actual Manager execution orders match;
  mixed APK byte identities differ and are verified separately.
- Final Manager summary68,572 bytes SHA-256
  `0d27ca266cd7e246a194f66c67cc9529007f1cb043679ebdd198850bd24d8792`.
  Additional Piko proof3,803 bytes SHA-256
  `419f81077549d6f8dcab9465d015872cf473e89feea141d7f4eb6ca2417c4c7f`.

## Exact restoration and publication

Baseline318:48,032,024 bytes, SHA-256
`170367733645b3ae3391a5d58f9426d7c43c01e02b6091b8c5cfc6948d1309af`, immutable
commit `9cb5e46a2df3d9eb3a4ad8700f0be22b4a694ecd`.
Forward-only saved321.patch:511,785 bytes, SHA-256
`d5b619f8e8bded704c6dc8a9318c3c7b8a1bdacdef57193994479cb2a7414f87`.
Both delta paths name downloads/Hiro_Morphe_Patches_v1.0.321.mpp; restoration
must match the exact aggregate bytes, without rebuilding or recompressing it.

assemble.py requires the completed three-case Manager summary, matching clean
builds, current source/tool/output identities and the additional two-output Piko
proof and logs. check.py binds every host/native/Piko/preservation transcript,
source/evidence file, exact loader/runtime scope and workflow mirror. Readiness
is enabled only when the full final gate succeeds.

Reviewed heads before the source publication commit are main
`6b38ebd4fc2498cb8fbc833688952a3bea0eb078` and dev
`5c3acfd1298ebebe575b6aa24a86e63abc89434a`. Parent orchestration refreshes these
leases again before publishing. The publisher retains exact-parent checks,
immutable download/asset verification and atomic main/dev feed updates. Only after
new feeds and bytes are verified does it add current321 notices to historical319
and320 releases, preserving their assets/titles and320 withdrawal status. A notice
failure reports the working feed commits and does not roll distribution back.

Source packaging excludes original APKs, native disassemblies and user videos.
No older release gates are modified.
