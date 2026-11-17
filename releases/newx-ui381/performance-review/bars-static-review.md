# Existing bar hooks: static scroll-path review

Reviewed the v1.0.380 helper, runtime source, and X 12.43.0-prod.01 native DEX.
This is a control-flow review, not a device frame-time measurement.

## Result

Keep the existing upper and lower bar hooks unchanged. The inspected paths do not
support a claim that pinning creates a layout, animation, or recomposition loop.
The view-count renderer has a separate, concrete allocation opportunity: v380
constructs a locale-aware NumberFormat on each full-count formatting call and
computes the compact native text before deciding whether it needs that text.
Removing this duplicate work is narrower than changing native scroll physics.

## Upper bar

The enabled branch in `com.x.ui.common.p4.t` returns the incoming modifier instead
of attaching the native collapsing nested-scroll connection. It resets the saved
height offset to zero through the existing Material state setter.

The setter chain `rf.b` → `s1.k` compares the stored float with the requested value
and returns immediately when they match. That return precedes the state lock,
write, and observer notification. Thus an already expanded bar is not written
again. The retained native Compose effect also only resets the same value; it
does not start an opposing collapse animation or timer.

Adding a separate read-before-write guard would be counterproductive here: the
public getter tracks Compose reads, while the native setter uses an unobserved
snapshot read for its existing equality check.

## Lower bar

The modification in `com.x.main.tabs.u.G` changes only the Boolean supplied to the
existing downward-scroll visibility request after the native distance threshold.
It preserves direction accumulation, thresholds, the upward-scroll branch, and
the native zero scroll-consumption result. It neither scales deltas nor changes
fling velocity or friction.

Repeated dispatch already exists in the native method. The enabled branch asks
the same controller to remain visible instead of asking it to hide. The inspected
concrete visibility setters write Boolean Compose state; Compose checks its
equality policy before acquiring a mutation lock or notifying observers. No new
layout request or animator is introduced by the hook. There is insufficient
evidence to justify changing the controller or its native gating behavior.

## Settings and refresh-rate work

The option lookup is an in-memory settings-map lookup after registry freeze,
followed by access to the BooleanSetting's stored value. It does not read
SharedPreferences or files on every scroll callback.

The existing 120 Hz helper runs after Activity resume, not on scroll frames. It
also avoids rewriting window attributes when the requested mode and rate already
match. There is no evidence here that changing fling friction or adding a
per-frame refresh-rate request would address the reported sluggishness.

## Limits

No Android device or emulator frame trace was available. These findings exclude
the specific suspected loops from the inspected code paths; they do not establish
the device-level cause of dropped frames. Native app disassembly excerpts are
intentionally excluded from this report.
