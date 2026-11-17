# NewX chat recommendation boundary

Target: unmodified `com.twitter.android` `12.43.0-prod.01` (the user-supplied APKM).

`ChatRecommendationsHook.java` is an additive helper in `app.hiro.newxui.patches`.
The unified bytecode provider should call `ChatRecommendationsHook.apply(context)`.
It requires a runtime method `Lapp/hiro/newxui/UiOptions;->hideChatRecommendations()Z`.
The provider must establish the exact package/version guard and bundle this runtime.

## Behavior

The native `Lcom/x/dm/convlist/j0;->invoke(Ljava/lang/Object;)Ljava/lang/Object;`
builds the conversation lazy list. It first adds existing conversations, notification
prompts, pagination and their callbacks, then adds the recommendations block.

Native instruction 136 (`IF_EQZ v22`) already skips the entire final block to
instruction 181 (`Kotlin Unit` followed by return). This helper adds three
instructions immediately before that native guard:

1. Read `hideChatRecommendations()`.
2. Store its result in `v6` (dead at this point).
3. If enabled, jump to the same native section end.

ON removes the `cold-inbox-suggestions-title`, `cold-inbox-find-friends`,
`cold-inbox-invite-friends`, `cold-inbox-suggestions-loading` lazy items and the
subsequent suggested-user items. OFF executes the complete original section.
No Compose group is skipped without its matching close: this is the lazy-list
builder, and the native branch already proves this entire block is optional.

The recommendation data model, actual conversations, search, new-chat button,
message delivery, contact synchronization settings, errors, pagination and existing
empty-inbox messaging are unchanged. This is a UI suppression feature; recommendation
network fetching is not disabled by this hook.

## Guard and verification

- One unique class and one exact `invoke(Object)Object` method.
- Original 27 registers / 183 instructions.
- Full instruction fingerprint:
  `1df1b2d39a21823e0b9be1161d8af5ab60e15f79997101b41649103a844e2435`.
- Native branch register/offset, section item keys, end return verified separately.
- Duplicate or structurally changed methods are rejected.
- Compiled using ECJ 3.46.100 with actual Morphe Desktop 2.2.0 / Patcher 2.4.0.
- Host test on the input DEX confirms exact existing prefix/tail preservation,
  toggle call, dead scratch register, exact skip destination, one modified method,
  and duplicate rejection. See `test.log`.
- Real-device and emulator visual behavior are not tested by this subtask.

Compile and test:

```sh
java -jar ../tools/ecj.jar -source 8 -target 8 -encoding UTF-8 \
  -cp /workspace/scratch/aae17de0ebaa/tools/morphe.jar -d classes \
  ChatRecommendationsHook.java ChatRecommendationsTest.java
java -cp classes:/workspace/scratch/aae17de0ebaa/tools/morphe.jar \
  ChatRecommendationsTest /workspace/scratch/aae17de0ebaa/analysis-x/dex
```
