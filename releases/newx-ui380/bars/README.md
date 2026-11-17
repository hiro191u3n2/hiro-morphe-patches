# NewX upper/lower bar pinning

Target: `com.twitter.android`, `12.43.0-prod.01` (312430001).

`PinBarsPatch.java` is a helper for the root provider in package `app.hiro.newxui.patches`.
It depends only on the runtime method `Lapp/hiro/newxui/UiOptions;->pinBars()Z`.

## Integration

Call `PinBarsPatch.verify(context.classDefBy(PinBarsPatch.TOP), context.classDefBy(PinBarsPatch.BOTTOM))`
before any edits, then `PinBarsPatch.apply(context.mutableClassDefBy(PinBarsPatch.TOP), context.mutableClassDefBy(PinBarsPatch.BOTTOM))`.
Alternatively `PinBarsPatch.apply(BytecodePatchContext)` performs the version check, guards and edits.

Both original method fingerprints were confirmed identical in the original APKM and the prior
Piko dev.2 + Hiro v1.0.378 Japanese rebuilt APK. Existing Piko patches do not overlap these methods.

## Native scope

- `Lcom/x/ui/common/p4;->t(Modifier,x4,Composer,I)Modifier`: preserves native Compose grouping
  (instructions 0–41), then when enabled resets the native appbar height offset to expanded `0F`
  via `rf.b(F)` and returns the incoming modifier without attaching the EnterAlways scroll behavior.
  This reaches home via `home/tabbed/a.e → p4.f → p4.t`. Its 15 call sites also include Explore,
  search results, the conversation list, chats and other app header scaffolds.
- `Lcom/x/main/tabs/u;->G(IJ)J`: substitutes `pinBars()` for the false visibility argument only
  in the downward-scroll hide request. ON follows the native show path; OFF leaves the hide
  request false. Upward scroll, thresholds and scroll consumption remain unchanged.

The global sidebar visibility setter, keyboard handling, fullscreen visibility and Android/
Material framework classes are not patched. Resetting the header offset also handles prior
collapsed saved state. The settings screen should request restart/recreation to recompose
all current screens immediately after a preference change.

## Validation

`PinBarsDexTest.java` compiles with ECJ against the actual Morphe Patcher 2.4.0 JAR.
It verifies original fingerprints, register layout, native member accessibility, exactly two
modified methods, native OFF branches, ON expanded state, the native bottom controller,
and duplicate patch rejection. PASS on stock and previous Piko dev.2 rebuilt APK DEX.

No device or emulator UI test was performed by this subtask. Final provider integration,
whole-APK rebuilding, runtime option tests and publication are the parent task's responsibility.

Evidence under `top/` and the `.txt` files are disassembly excerpts for private investigation;
do not include these native app excerpts in public source archives. Publish this helper,
the test and this implementation note as needed.
