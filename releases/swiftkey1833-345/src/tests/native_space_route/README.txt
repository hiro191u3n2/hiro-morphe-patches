Native space route evidence, SwiftKey Beta 9.13.16.4

Source is the archived previously modified v2 APK identified by manifest.json.
Methods were extracted verbatim from apktool 3.0.3 disassembly. These are static
reference excerpts, not a new clean-APK application test or device recording.

fc0/s1.n constructs the ordinary spacebar through fc0/k1.i. This registers
fc0/d1 with two U+0020 strings, or ic0/t for cycle-reset behavior. The latter
also passes U+0020 strings when it actually inserts a space.

Ordinary touch callback chain:
  xd0/d1.onTouchEvent -> xd0/f0.f -> ef0/d.l -> tj0/e.Z/F
  -> gf0/a.c -> fc0/n2.c -> oc0/m.c -> oc0/b.c
  -> hc0/a.l/j -> ic0/e.a -> fc0/d1.b -> wb0/x.P
The actions are registered through hc0/b.i. Its list is wrapped by ic0/e.
The normal touch path contains direct calls, with no explicit Handler,
Executor or coroutine dispatch in these methods. The ordinary View callback
origin supports the existing main-looper guard; removing it is not justified
as a proven fix by this audit.

The existing P hook is already on this route. The previous NativeSpaceProbe
only tested inserted guard instructions with a supplied boolean; these
fixtures add route evidence and must not be described as reproducing an
old-version failure. The check script explicitly reports that limitation.

fc0/q0 and s40/d only reset key cycling counters before P. They do not commit
the composition. The alternate wb0/i1 implementation writes directly to an
input connection and has no prediction model; it is not this full-keyboard
path and remains unmodified.

Verify fixture hashes and expected native references:
  python tests/check_native_space_route.py
Optionally compare every excerpt with a fresh decode of the pinned APK:
  python tests/check_native_space_route.py --decoded PATH_TO_APKTOOL_DIRECTORY
