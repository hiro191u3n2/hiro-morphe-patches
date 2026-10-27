SwiftKey Beta v1.8.15 / Hiro Morphe Patches v1.0.298

Purpose
Move the native candidate expand and collapse controls to the physical left.
Replace the separate toolbar arrow with dedicated emoji-key access: a normal
short press opens emoji, a long press opens or closes the toolbar, and a short
press while the toolbar is open closes it. This extends the published 1.8.14
candidate-scroll/diagnostic-removal release without replacing its runtime code.

CandidateToolbar runtime
The native ob0.d1 wrapper owns the toolbar button and kd0.e0 controller. Bind it
to the KeyboardService reached through its actual ContextThemeWrapper chain.
The construction path a60.s -> he0.p0 -> ob0.d1 was traced to the service context.
Bindings and retired view references are weak. Transitions use kd0.e0.n so native
state persistence and observer notification remain responsible for opening and
closing. OPEN is state0, CLOSED is state2; unsupported states retain native access.

Only hide the old arrow after the dedicated emoji action has been configured and
the native ah0.w0.G() setting is enabled. Preserve the original click listeners.
Suppress rendering, input and accessibility while hidden; reclaim the enclosing
start slot so the candidate row reaches the former arrow position. Restore the
original marker/slot when the dedicated route is disabled or the native
microphone/emoji combination is selected. Off-main factory notifications update
view state through one main-looper callback with a weak service reference. There
is no periodic refresh or touch polling.

Move the original ExpandedResultsOverlayOpenButton using FrameLayout gravity
LEFT, retaining its listener and actual hit region. Shift candidate-list margins
from the right to the left. Resolve relative margins for RTL without reversing
candidate order, text direction or child order. Repeat only idempotent geometry
corrections at native measure boundaries. The expanded native close button uses
its existing key-height provider for its square size; move it left and reserve
that strip in the result grid so the first candidate cannot sit underneath it.

EmojiToolbar runtime
Hook fc0.h.invoke(Object) after its native action builder is populated. Only
factory discriminator0 is a dedicated emoji key. Discriminator1 keeps its
microphone/emoji action graph and restores the original toolbar access route.
Wrap only the known ic0.a0 emoji-opening action; preserve its original restriction
and native feedback/reset actors. When the toolbar is closed or not attached,
the original short action receives its original breadcrumb unchanged. While it
is open, the same short action closes it and consumes that one emoji action.

Add a native hc0.k long-press delegate using the existing fc0.k1.w timeout.
SwiftKey's own timer, pointer cancellation and post-longpress click suppression
remain in control. The timer action toggles the native toolbar. If a controller
is temporarily unavailable during keyboard recreation, its native long-release
path falls back to the original emoji action exactly once. Cancellation and
slide-out suppress that release. All builder replacements are prepared before
assignment; an unknown graph retains native actions. An already installed graph
re-enables access when reused after a combined-key mode. Separate native normal
and accessibility action routes were checked for duplicate invocation.

Exact patch hooks
- ob0.d1 constructor tail: bindToolbar
- ob0.e constructor tail: layoutContainer
- SequentialCandidateBarLayoutWithECWButton.onFinishInflate tail and
  onMeasure entry: layoutCollapsed
- ExpandedResultsCloseButton.onAttachedToWindow tail and onMeasure entry:
  layoutExpandedClose
- both fc0.h.invoke returns: configure(factory, original builder argument)

The7 target methods receive8 bridge calls. Signature, superclass, access,
register-count and complete normalized method hashes are checked before mutation.
Return hooks replace the original return MethodLocation and append the cloned
return, so branches originally targeting a return cannot bypass the hook. Removing
bridges recovers every original method contract. These native targets do not
overlap existing KeySwap, JapaneseGlide or CandidateScroll hooks.

Preservation and inputs
Inputs are exact published SwiftKey1.8.14 and bundle1.0.297 at immutable commit
3780eab12ce14f8d922193316ac250190bf2502d. Their bytes, tool bytes, and baseline
runtime are SHA256-pinned. All62 existing runtime classes remain canonically
byte-identical. Only new CandidateToolbar/EmojiToolbar helper families are added.
Flick recognition, candidate append/order stabilization, removed diagnostics,
key swaps, dictionaries and every other app payload remain the pinned baseline.

The MPP delta is limited to manifest, swiftkey_japanese.mpe, classes.dex, and the
existing JapaneseSamsungEmojiPatch JVM class. Add CandidateToolbarPatch.class.
The original JVM and Android main loaders each receive only2 additive bridge
calls; every other existing loader class, including CandidateScrollPatch, remains
identical. The combined loader changes276->277 classes; standalone17->18.

Build and verification
Use Java17 for javac/D8 and Java21 for actual Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build1815.py --download \
    --input-dir input --tools tools --work work --output output

For a fresh independent ABI review, first use --compile-only, then run:
  python tests/audit_toolbar_abi.py --help
The verifier uses the actual archived target APK, compiled extension, pinned
Morphe dexlib2 and R8 ASM, and the pinned Android API35 jar. It checks native and
inherited platform members, type access, descriptor/opcode compatibility and
class/interface dispatch. The resulting tests/native_abi_review1815.json must
match the exact extension and archived APK hashes before release packaging.

Production layout and gesture classes run in explicit host fixtures. They cover
geometry, real listener retention, reversible access, state transitions,
service lifetime, background notifications, native action restrictions,
longpress/cancel boundaries and release fallback. Native hook fixtures and the
archived APK separately verify exact bytecode edits. Actual old/current Morphe
loaders must select the same packaged extension and preserve all named patches.

The workflow validates source pins, publication regressions, two independent
complete builds, all six reviewed artifact hashes and exact preservation deltas.
Only then does the publisher upload, verify release and immutable download bytes,
and atomically advance main/dev Manager feeds using head leases. Unrelated app
metadata and ULike's active policy are preserved.

Scope and installation
Target: Microsoft SwiftKey Beta9.13.16.4 (1236271168), com.touchtype.swiftkey.beta.
Apply the updated patch from bundle1.0.298 to the unmodified target and install
using the same signing settings. Source updates alone do not replace the installed
keyboard. Do not uninstall or erase the keyboard's app data for this update.

Host geometry and native code inspection do not establish Android rendering,
handset touch feel, or a complete clean-original-APK patch application. Those
remain unverified. The archived APK used for ABI/hook inspection is the same
previously modified v2 archive used in prior reviews, not a clean apply test.
