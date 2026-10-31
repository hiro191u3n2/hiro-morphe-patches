# Release136 diagnostic publication evidence

OneBack **1.1.29** / aggregate **1.0.320** is diagnostic only. The plain two-line
`HB320` Toast and local `HiroBack320` log expose bounded codes from existing Back
callbacks. This does not establish the handset's route or claim to fix the
reported first-versus-second behavior. **実機未確認** remains explicit in README
and release notes. Earlier experimental changes and evidence are excluded.

## Completed verification

- Frozen diagnostic source: 194 files. All 36 host suites passed; the new
  `Back136InstagramDiagnosticTest` passed 20 cases and 182 assertions.
- Independent source AST inverse: 28 baseline files, 33 standalone trace calls
  removed and 15 identity wrappers unwrapped; original executable decisions
  retained. Four changed-source negatives were rejected. Privacy review binds
  the reviewed source hashes and confirms bounded codes with no user content or
  new reflective, lazy or current-screen reads.
- Two independent clean builds produced identical aggregate MPP, standalone MPP
  and QA JSON bytes. Aggregate: 48,069,357 bytes, SHA-256
  `aae60ca12e38990ad0ecec4cb71a224e3b25f4da51159ec94daa2076c5299745`.
- Complete aggregate loader DEX bytes are unchanged from319: all 21 owned and
  282 unrelated definitions retained, no additional native hook. Runtime: 95
  baseline definitions retained; only ExternalProfile, DispatcherBack and
  OneBackExit changed; three trace definitions added, 101 total.
- Real Manager2.3.0-dev.7 application: original Instagram452.1.0.53.64 alone and
  official Piko3.11.0-dev.3 default 64 plus Hiro in both requested source orders.
  Selected counts 1/65/65 all finished with `failure=null`. Actual Piko execution
  order was the same for both requests; mixed APK byte identities differ.
- Every final output passed initial-profile (59), native-Home (379), profile-link
  (129), manifest, resources and whole-APK audits. Each has 12 native hooks and
  101 exact final runtime classes. Mixed outputs were independently compared to
  the same pinned Piko-only baseline; no APK byte-identity claim is made.
- Final Manager summary: 54,006 bytes, SHA-256
  `d00d3610ece2b434e7c569eee25a7d64808666c23a23c11258e1608139000606`.

## Exact restoration and packaging

The baseline is published **1.0.319**, 48,051,087 bytes, SHA-256
`cd0f961cc549ee3da5e6222fd55dc46f80e760c05409318dde4903bdb5e17564`, at immutable
commit `96290380b0e265351dc2e47572c23dbce644a667`. The forward-only
`saved320.patch` restores exact final320 bytes; it never rebuilds or recompresses
an MPP. Delta: 120,859 bytes, SHA-256
`287b3dfd269334c8e07684627fdb8e3d2a7208a845908afdd43bdc388c6af848`.

`assemble.py` accepts only the completed three-case summary, matching independent
builds, current source and tool identities. It copies authored evidence and audit
sources, never APKs or native disassembly. `--finalize` sets readiness only after
final notes are complete and runs `check.py`; a failure resets readiness false.
The report's `source_and_evidence_sha256` comes from `check.py.inventory()` after
all notes, pins and evidence are final. Workflow and its publication mirror must
remain byte-identical.

Before publication, run `check.py`, `restore_saved.py --base PINNED319` and
`publish.py --local-only`. Parent orchestration owns source commit and external
publication. Reviewed heads are main `14f00b372c33bdd2d17fa618d87b7976dd593b8f`
and dev `76d42b3d90a41d7a22d4d0a5b08506a4f6bef773`, refreshed after final QA.
The publisher retains exact-parent checks, branch leases, atomic main/dev updates,
immutable download checks and public release-asset verification. Do not change
older release gates or add unverified device-success claims.
