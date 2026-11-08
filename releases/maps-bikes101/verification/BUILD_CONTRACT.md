# Bicycle marker repair build and verification contract

The build produces Maps_Hide_Bicycle_Marks_v1.0.1.mpp and integrated bundle
Hiro_Morphe_Patches_v1.0.370.mpp from immutable bundle1.0.369, SHA-256
6914c6dd5878e4c3dfffeb6bc284ec73f0e1f9915310ef9b1da19eab4f1d01ad.

The user reported gray bicycle markers remaining after v1.0.0. That patch
covered one SharedLabeler factory and URL forms of glyph identifiers. This
repair supports verified bare identifiers and additional native boundaries
recorded in NATIVE_LABEL_CONTRACT.md. It does not force a different renderer.

## Preservation and upgrades

Only the bicycle provider namespace, runtime extension, manifest and merged
loader DEX are replaced. Every unrelated provider, runtime, resource and
contract remains byte-identical. BundleAudit replaces only the bicycle provider
DEX classes and canonically verifies every retained class. Both pinned Morphe
SDKs must expose the unchanged provider set with only bicycle metadata updated.

The exact published v1.0.0 runtime is embedded at
contracts/maps_hide_bicycle_marks_v1.0.0.dex. Its size is 6,420 bytes and SHA-256
73d1bab89799158d9d2908a5373be2bf25630c8f2e2497b50fcdf26fe4aa0a9c.
Upgrade guards compare complete old runtime classes, validate the old five
instruction SharedLabeler prefix, remove it on a detached validation copy,
and verify the original native body. Unknown or partial installations fail
before mutation. Existing runtime methods are explicitly replaced because
extension merging alone does not update their bodies.

## Actual APK verification matrix

| Label | Operation |
| --- | --- |
| standalone-v285 | Standalone repair applied to provided v2.8.5. |
| integrated-v285 | Same option from bundle370 applied to v2.8.5. |
| reported-v287 | Same option applied to previously provided v2.8.7. |
| additive-to-existing-four | Repair added to current four-option Maps APK. |
| all-five-maps | All five current options applied together to v2.8.5. |
| upgrade-old-single | Published v1.0.0-only output upgraded to v1.0.1. |
| upgrade-old-all-five | Published369 five-option output upgraded to v1.0.1. |

All input APKs and the four-option cache are pinned by SHA-256. The cache is
accepted only with its successful patch/rebuild report and matching input,
bundle, output, selected-option and report hashes. It retains strict-search
v1.0.1, startup-location v1.0.4, category hiding and back-exit implementations.

Standalone/integrated fresh outputs must have identical ZIP entries. Fresh and
upgrade outputs must have canonically identical classes, code, metadata and
non-DEX resources, both with and without the four existing Maps options.
The reject-current audit confirms repeat application is rejected. Native guards
freeze original methods and schemas. Hook audits inspect expected edits,
registers and branches, original bodies and exceptions, injected runtime and
all unrelated classes. Non-signature resources are byte-preserved.

## Runtime tests and publication

Host tests cover exact bicycle names, bare/URL identifiers, SharedLabeler and
legacy schemas, per-feature overrides, common background and ordinary icons,
malformed data and repeated calls. The legend filter is checked against an
actual saved official style resource and structured fixtures. Only statically
identified bicycle styles may change; all other records must remain identical.

Run src/build.py --help for paths. Include the exact published old single and
five-option APKs, a fresh work directory and an empty distribution directory.
The build records source and tool hashes and rejects changes during validation.
prepare_release.py stages audited outputs, QA and source archive. The370 storage
recipe reuses immutable355 chunks; executable preservation uses369. Existing
release URLs remain valid. Site deployment precedes main/dev source updates.

## Verification limits

Android device/emulator execution is unavailable in this session. DEX, APK,
host and style-data test success does not establish on-screen disappearance,
frame rate or future server format coverage. all_rendering_backends_verified
remains false. See the native contract and QA for exact inspected hook points.
