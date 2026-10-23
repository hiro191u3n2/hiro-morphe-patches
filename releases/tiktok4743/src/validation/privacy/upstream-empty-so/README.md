# Metra empty native resource interaction

The pre-fix all-144 probe exposed a real composition failure: Metra removed creation resources by retaining zero-byte native entries, and Hiro Privacy rejected the first one. The probe was partial (143 applied, Privacy failed) and is not release success evidence.

The retained upstream sources at the pinned 0.9.0-dev.1 commit describe four exact resource groups: P2P relay (4 native paths), content credentials/card scanner (6), creation tools (6), and LIVE extras (2). Their complete union is 18 paths, covering arm64-v8a and armeabi-v7a. The union was checked against the shipped upstream class constants and the actual empty entries in the discovery APK output. Startup libdex_*.so containers are explicitly prohibited upstream and are absent from this allowlist.

Hiro now preserves only these known zero-byte placeholders. It first checks the existing workspace file, so an already empty resource is never extracted again or restored. Each skipped path and the total are logged. Nonempty entries continue through the unchanged host rewrite and container integrity logic; unknown empty entries, absent/non-file entries, and oversized files still fail.

The host suite passed **367 assertions on each of the three SDK jars**, including both ABIs, all 18 known paths, unknown paths, case/ABI/path variants, missing and oversized inputs, unchanged empty-file metadata, and unchanged nonempty ELF behavior. The older host result of 292 assertions is superseded for this source.

This folder records provenance and host tests. Final original-APK application evidence is recorded separately for the exact rebuilt MPPs. The primary PrivacyApkAudit remains strict: it still verifies every original native entry and does not accept empty placeholders in the four primary configurations. No physical Android execution is claimed.
