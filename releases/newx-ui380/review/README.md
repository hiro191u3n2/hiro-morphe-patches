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

The baseline is the corresponding v1.0.379 rebuilt APK for each Piko/Clone configuration.
Only the inline count renderer class (`ViewCountHook.TYPE`) and `UiOptions` runtime
may change. The former must exactly equal the verified hook transformation applied to
the baseline; the latter must match the separately compiled new runtime. Exactly one
class may be added: `ViewCountFormat`, also compared to the compiled runtime. All four
runtime classes are compared, and the existing `RefreshRate` classes must additionally
remain exactly unchanged from v379. Every earlier native hook, Piko class and unrelated
class must remain unchanged. Duplicate, deleted and unexpected classes fail the audit.

Arguments:

```text
CompletedUiApkAudit BASELINE.apk COMPLETED.apk EXPECTED_RUNTIME.dex REPORT.json
```

Compile with Java 17 syntax, ECJ, Morphe Desktop 2.2.0/Patcher 2.4.0 and the newly built
provider classes. The cache reuses per-class canonical hashes only when the full raw
DEX SHA-256 is identical. Its key includes the canonicalizer version and API level;
every APK DEX is hashed again before cache lookup and cross-DEX duplicate detection
still runs. This saves repeated work for identical Piko configurations without
weakening preservation checks. Use a 2–4 GiB Java heap. Baseline caches may be prepared
with `CompletedUiApkAudit --cache-only BASELINE.apk ...`.

## Compiled resources and ZIP payloads

```text
python3 audit_resources.py BASELINE.apk COMPLETED.apk COMPONENT.mpp REPORT.json
```

The portable `arsc.py` reads the actual `resources.arsc`, including qualified string
values. Every baseline string value must remain equal except the existing category
summary in English and Japanese; those two must exactly match the new component.
All ten UI resource names must have the exact English and Japanese component values
(20 checks), with precisely two new resource names (four language values).
Every existing non-DEX ZIP payload, including AndroidManifest.xml, must remain
byte-identical, excluding the changed resource table and signing metadata.

Adding two new names reallocates the 176 earlier Hiro Japanese helper-string IDs
by two. `name_lookup_resources.json` is the exact set from the prior Japanese
patch's translation targets; each is loaded by resource name through
`ResourceUtils.getStringOrThrow(String)`, never an embedded integer. Only this exact
set and exact allocation shift are permitted. Reallocated IDs are separately recorded
in the report; all native and other Piko resource IDs must remain unchanged.

No APK input, reconstructed APK, screenshot, video, or decompiled application code is
included in these audit sources.
