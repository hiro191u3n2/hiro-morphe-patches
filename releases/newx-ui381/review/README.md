# Completed APK audits

These incremental checks compare v1.0.380 and v1.0.381 APKs built from the same X
input and the same Piko/Japanese/clone options. They verify artifacts statically;
they are not Android execution or frame-time tests.

## DEX preservation

`CompletedUiApkAudit.java` canonicalizes every class as an independent DEX, including
methods, fields, annotations, debug information, and exception ranges. If needed,
it normalizes only const-string/jumbo encoding and the resulting branch offsets.
Duplicate classes, unexpected changes, additions, and deletions fail the check.

Exactly two existing classes may change: `com.x.inlineactionbar.w` and
`app.hiro.newxui.ViewCountFormat`. Exactly one class may be added:
`app.hiro.newxui.ViewCountFormat$FormatterState`.

The audit clones the v380 renderer class and replaces only its existing private
bridge with `ViewCountHook.bridge()` from the final v381 provider build. A complete
class comparison proves that the caller and all other native code are preserved.
All five runtime classes must match the separately compiled runtime DEX. UiOptions
and both RefreshRate classes must additionally be exactly unchanged from v380.
Earlier bars/chat hooks, Piko functionality, and unrelated classes are unchanged.

```text
CompletedUiApkAudit BASELINE_V380.apk COMPLETED_V381.apk EXPECTED_RUNTIME.dex REPORT.json
```

The runner compiles the audit with Java 17/ECJ and Morphe Patcher 2.4.0. It loads the
hook only from the final provider build, never from a stale review-local class.
Per-class inventory caches from v380 are reused only when the full input DEX SHA-256
matches. Cache keys include the canonicalizer version and API level; every input
is hashed again, and cross-DEX duplicate detection still runs.

## Resource and payload preservation

```text
python3 audit_resources.py BASELINE_V380.apk COMPLETED_V381.apk COMPONENT.mpp REPORT.json
```

The complete resources.arsc must be byte-identical. No resource value, configuration,
or ID change is allowed. All twenty UI values are checked against the final component
in English and Japanese. Parsed checks may be cached only when the entire table,
expected values, and parser source have identical SHA-256 digests.

All other non-DEX ZIP payloads, including AndroidManifest.xml, must be byte-identical,
with no additions or deletions. Signing metadata is excluded. Unlike the v380 audit,
this version permits no helper ID shifts and no category-summary updates.

Run `python3 run_completed_audits.py` to audit dev1, dev2, dev2-clone, and standalone,
or pass one or more of those names. Baselines live in the sibling x-ui380 directory.

Native app disassembly, APK inputs, reconstructed APKs, and screenshots are not part
of these audit sources.
