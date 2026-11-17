# Completed APK audits

These audits compare a completed patched APK with an otherwise identical baseline
using the same X input, Piko release, selected patches, Japanese-localization option,
and clone option. They perform static artifact verification, not Android execution.

## DEX audit

`CompletedUiApkAudit.java` canonicalizes every class into an independent DEX to
compare code, metadata, fields, annotations, debug information and exception ranges
without depending on APK entry or multidex ordering. If a class differs only because
DEX string indexes triggered `const-string/jumbo`, it normalizes both encodings and
their builder-adjusted offsets before comparison.

Only the chat lazy-list builder, common app-header scroll modifier, main navigation
scroll handler, Piko built-in settings registrar, and MainActivity may change. Their
complete resulting class definitions must equal applying the verified hooks to the
baseline. The three new runtime classes must match the separately compiled runtime
DEX. Duplicate classes, deleted classes, unexpected additions or other modifications
fail the audit. The compiled provider classes are required on the audit classpath.

Arguments:

```text
CompletedUiApkAudit BASELINE.apk COMPLETED.apk EXPECTED_RUNTIME.dex REPORT.json
```

Compile with Java 17 syntax, ECJ, Morphe Desktop 2.2.0/Patcher 2.4.0 and the newly built
provider classes. Running each comparison takes roughly three minutes on the current
workspace; use a 2–4 GiB Java heap.

## Compiled resources and ZIP payloads

```text
python3 audit_resources.py BASELINE.apk COMPLETED.apk COMPONENT.mpp REPORT.json
```

The portable `arsc.py` reads the actual `resources.arsc`, including qualified string
values. Every baseline string value must remain equal. All eight new resource names
must have the exact English and Japanese values packaged in the component (16 checks).
Every existing non-DEX ZIP payload, including AndroidManifest.xml, must remain
byte-identical, excluding the changed resource table and signing metadata.

Adding eight new names reallocates the 176 earlier Hiro Japanese helper-string IDs
by eight. `name_lookup_resources.json` is the exact set from the prior Japanese
patch's translation targets; each is loaded by resource name through
`ResourceUtils.getStringOrThrow(String)`, never an embedded integer. Only this exact
set and exact allocation shift are permitted. Reallocated IDs are separately recorded
in the report; all native and other Piko resource IDs must remain unchanged.

No APK input, reconstructed APK, screenshot, video, or decompiled application code is
included in these audit sources.
