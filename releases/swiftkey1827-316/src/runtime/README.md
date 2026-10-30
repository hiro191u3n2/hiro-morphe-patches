English upward-flick repair for SwiftKey Beta 9.13.16.4

The published v1.8.26 FlickSymbols implementation required both ROMAJI (qj0.a.x4)
and JAPANESE prediction mode before tracking or committing a gesture. English
QWERTY and ROMAJI alphabet mode therefore could never reach its recognizer.

The replacement accepts exactly ROMAJI/JAPANESE, ROMAJI/NORMAL, and
QWERTY/NORMAL (qj0.a.N1). NORMAL on ROMAJI is intentionally accepted for
alphabet/English input while that layout remains active. Layout kind and search
type are captured separately per DOWN; changing either aborts insertion, even
when the new combination is also allowed. Already owned streams still drain.

Existing direct secondary and native popup-default insertion actors are reused.
No hardcoded key mapping or native popup-open call is added. Recognition geometry,
five sensitivity settings, timers, cancellation, editor checks, ordinary tap,
native long press, and Japanese glide dispatch remain unchanged. Horizontal-start
English paths continue to the native handler. A captured upward path keeps its
existing priority and inserts the original key's alternate once on release.

Build usage:
python build_runtime.py --runtime BASELINE.mpe --tools TOOLS --work FRESH --output OUT

The builder verifies that the restored baseline Java recompiles to all five exact
published FlickSymbols DEX classes, then replaces only that class family. Only
FlickSymbols and its Stroke nested class differ; the other 89 of 91 runtime
classes match canonically. Public entry points and consumers are verified. Native
ABI evidence is pinned to the exact runtime, verifier sources, and archived APK;
--apk replays the audit locally. The build runs the same English traces against
old and new code, tests repeated direct/popup flicks, and reruns the Japanese,
sensitivity, ownership, and glide suites.

Native enum, factory, Flow cancellation, and composition review is recorded in
tests/native_route_review.json. tests/native-abi.json verifies native references,
access, invocation kinds, and callbacks. The APK is a previously modified
9.13.16.4 reference, not an untouched distribution. These are host/static checks;
handset touch/ART behavior, native Flow-engine execution, and full application of
the patch to an untouched APK are not certified by this work.
