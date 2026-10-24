# OneBack 1.1.22: TikTok 47.4.3 native Back repair

This release repairs the actual Back path in global TikTok 47.4.3
(`com.zhiliaoapp.musically`). TikTok 47.5.3 is removed from compatibility metadata,
production admission and runtime ABI recognition. The existing 46.2.3 path is
retained. Historical release directories keep their original evidence; they are
not evidence that this release supports 47.5.3.

## Why the previous implementation missed Home

The previous 1.1.21 implementation could bind the 47.4.3 Home navigation API but
still required visible feed evidence to authorize an exit. The actual
`MainRootFragment`/`MainFragment` names did not supply that evidence. A visible
recommendation label in the old host fixtures hid the failure when an upstream
patch removed that label or the native view tree exposed no matching text.

A separate AndroidX fallback could reach `Activity.onBackPressed` directly,
skipping TikTok's own SAFTikTokActivity Back chain. The session guard then blocked
task exit before the native root decision had run. The original native two-Back
timer was also still present.

## Native boundaries and one-action behavior

The new route keeps TikTok's native interceptor, comment and tutorial handling.
It adds two narrowly placed boundaries in the audited 47.4.3 implementation:

1. `LX/1AA5.onBackPressed()Z`: after the interceptor and comment guards, skip only
   the two-Back timer. Return to the caller so its remaining guards still run.
2. `BackRefreshStrategyAssem.onBackPress()Z`: after the remaining native guards,
   allow task exit only when this Back began at Home and the current native
   state is still Home in the same ability instance and outer feed page.

`Hox` selecting `HOME` alone is insufficient: the outer pager may still show a
profile, camera or discovery page. The runtime checks the native outer-page
state and uses the app's own guarded transition when that page owns Back.

The 47.4.3 path performs one native action per Back. Closing a comment, dialog,
child page or other transient screen does not schedule another Back or an
automatic jump to Home. Returning from another root tab to Home does not grant
exit permission to the gesture that began on that other tab. The next Back on
Home can exit and remove the task from Recents.

Physical Back key events retain TikTok's native KeyDown/KeyUp chain so its
profile/camera guards can act first. The captured session retains the gesture's
starting state, cancellation and repeat handling. AndroidX fallback can re-enter
the exact native main Back chain at most once. Child Activity finish remains
ordinary navigation.

## Exact input and admission

| Input | Role | SHA-256 |
| --- | --- | --- |
| Global 47.4.3 | Supported original APK | `af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b` |
| Global 47.5.3 | Unsupported-version rejection input only | `6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e` |

The 47.4.3 Home ABI binds `HomeTabAbility.tB0(Bundle,String)`, `tv2()`, `jI0()` and
`Hox.w33(String)`. Production admission checks the complete native contracts,
including the outer pager and both Back boundaries, before modifying their
bodies. Normalized fingerprints retain references, registers, literals, branch
destinations, exception regions and access flags. A missing or changed required
body is rejected. The modern path applies only to the global package; legacy
46.2.3 retains the previous two package names.

The internal filename `TikTok4753ContractAudit.java` remains for build continuity.
It is not a declaration of 47.5.3 support. The supported audit invocations are:

```text
TikTok4753ContractAudit original ORIGINAL_4743_APK 47.4.3
TikTok4753ContractAudit output PATCHED_4743_APK 47.4.3
```

## Verification and preservation

`build.py` compiles the production loader and runtime, runs the existing legacy
and other-app regressions, and adds the 47.4.3 native Back and 47.5.3 rejection
fixtures. The newer fixtures do not rely on a recommendation label to identify
Home. The output audit checks the emitted native gate positions, registers,
branch landing targets, normal and exception cleanup, preserved original bodies,
and required lifecycle/dispatch hooks after DEX serialization.

Curated native-boundary and regression evidence is under
`src/validation/backfix`. Full 47.4.3 application proofs, separate forced 47.5.3
rejections and the 139-patch upstream integration proof are recorded by the
release-level validation runners. Every proof identifies the exact tested MPP
bytes. Original and patched APKs, and full native method dumps, are not published.

Other applications' loader definitions, runtime behavior and resources are
subject to preservation checks. The previous Instagram/X upgrade resources are
retained. When recompiling moves only a nested class's debug line information,
the build first verifies identical instructions, registers, exception handlers,
fields, methods and annotations, then retains its complete baseline definition.
Unrelated implementation changes are rejected. TikTok testing uses a clean,
unpatched original input.

Host tests and emitted-APK audits do not establish physical Android operation.
Android 17 / Samsung SM-S948Q interaction is not tested in this environment.
