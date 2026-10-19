# Launcher → legacy Main Home-restore contract

For a signed-in regular account, the provided X 12.19.1 APK's normal launcher route satisfies the Home-restore intent checks. No action/data rewrite or constructor ordering fault was found on this route.

1. Android launches StartActivity with MAIN + LAUNCHER. The injected `install` runs at onCreate entry and records the launcher task/deadline when the private exit marker is present.
2. StartActivity$a creates an empty Intent and calls dispatch.c.a. For a regular account without an explicit nested Intent, this calls dispatch.d.a, which creates main.api.b arguments using `b$b.b(null)` and navigates through the existing main-activity mapping.
3. The main argument builder starts from `new Intent()`; `page` is null, action/data remain null, and it adds only CLEAR_TOP (`0x04000000`). Default navigation options resolve to NONE. `args.b.a` resolves the destination class; `common.l.toIntent` only sets its explicit component. It does not set ACTION_VIEW or a data URI.
4. The final start uses the current Activity context. Neither the flags nor the manifest requests a separate task: StartActivity and MainActivity both use singleTop. A normal new launch after successful task removal therefore keeps the launcher and main activity in the same task under standard Android task routing. This is a static routing conclusion, not an observed device task ID.
5. The injected legacy `p.onCreate` calls `install` at PC0, before its superclass work and before the DI initialization call `b.e`. TwitterBack.installed accepts the main activity's null action/data while the same-task launcher ticket is live, then sets `page=twitter://timeline/home` before native initialization reads it. Duplicate install calls are idempotent.
6. Native initialization callback `main.x0.a` either explicitly selects the Home URI or passes the captured launch Intent to l1.I3. I3 reads the explicit page first; only a missing page falls through to the last-tab database lookup. Thus the marker + successful exit path selects Home on the next normal launch.

Explicit links, notification destinations, restored/reused tasks, and expired launcher tickets are separate cases. This investigation does not claim physical-device launch validation, and does not alter app preferences or the user's saved data. The exit fix must actually establish the marker and remove the old task; merely backgrounding the old task does not create this cold-launch path.

See `launcher-trampoline-excerpts.txt` for the native instructions and `launcher-trampoline-provenance.json` for input/applied APK hashes and scratch-source hashes.
