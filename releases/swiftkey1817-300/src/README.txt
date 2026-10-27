SwiftKey Beta v1.8.17 / Hiro Morphe Patches v1.0.300

Purpose
Japanese QWERTY punctuation: ordinary tap on 。 still inputs 。, while its
upward flick and alternate action select only full-width ？. Ordinary tap on
ー still inputs ー, while its alternate selects only full-width ！. The small
key labels match those two alternate characters. The published 1.8.16 candidate
rendering correction and every unrelated runtime and resource are retained.

Why two native candidate hooks are required
The native period key is a specialized key. Its fc0.s1.q factory adds the
primary punctuation and language variants to the resource's alternate list.
The generic character factory fc0.s1.c also augments alternates from languages.
Changing only the serialized strings would therefore leave extra candidates or
select 。 when flicking. Do not change the native key type to work around this:
the original ordinary-tap actors must keep responsibility for text processing.

PunctuationKeys normalizes the selected Japanese QWERTY layout resources after
the existing KeySwap conversion. It changes the two alternate definitions and
their secondary labels and marks only those definitions with an internal tag.
The marker lives in extraTags rather than visible label or accessibility text.
Exact shape checks limit the transformation to recognized resources and keys.
The two native entry hooks ask the runtime for a tagged singleton factory and
fall back to the entire original native method when the runtime returns null.
The existing period/generic normal-tap action builders stay unchanged. Existing
FlickSymbols still recognizes the same native direct-release or popup actor;
none of its recognition, sensitivity, cancellation or editor checks are edited.

Runtime integration
Merge1817 preserves the 71-class baseline, adds only PunctuationKeys and its
nested helpers, and changes only the public KeySwap.wrap entry. The old body is
renamed to wrapBeforePunctuation1817 without any other alteration. A five-
instruction wrapper calls that original body once and the new postprocessor
once. Removing the wrapper and restoring the original name must reproduce the
complete original KeySwap class; the other 70 existing classes remain identical.

Native and loader integration
PunctuationPatch targets exactly two methods in fc0.s1:
  q(List,String,String,fc0.m1,fc0.v1): fb0.f2
  c(fc0.m1,fc0.v1): xc0.a
Complete method contracts, access flags, superclass and register counts are
verified before either method is changed. Each receives a five-instruction
entry prefix; the original bodies, branches and all other methods are retained.
A non-null singleton result returns immediately; null continues the old body.

UpdatePunctuationLoader adds exactly two calls to the existing main JVM/Android
loaders, after the previously published toolbar hooks, and adds one helper
class. Removing the bridges restores the normalized previous loader content.
The combined Android loader grows from 277 to 278 classes, standalone 18 to 19.
Every unrelated patch class and every named patch registration remains intact.

Inputs and reproducibility
Baseline: exact SwiftKey 1.8.16 and Hiro bundle 1.0.299 at immutable commit
 e52f3099ad3d312bdd7350a3b74e897193783588.
Both artifacts and build tools are SHA256 pinned. Packaging permits changes
only to the manifest, extension DEX, main JVM patch class and Android loader
DEX, plus the new PunctuationPatch JVM class. All dictionary resources and
all other app payloads remain byte-identical.

Verification
The production helper runs against saved native layout fixtures and modeled
platform objects. An independent reader checks key fields, exact output labels,
normal-tap definitions, existing key swaps and unaffected data. The production
FlickSymbols Java is used for the new alternate action paths. These are host
checks and native static analysis, not handset rendering tests.

The independent ABI verifier resolves the new helper and KeySwap references
against the actual archived APK, merged extension, and pinned Android API35
classes. Native hook probes validate both the saved exact-method fixture and
archived APK, and exercise old/current Morphe APIs. Actual Morphe loaders check
the selected extension supplier, all 20 named patches, target compatibility and
the original three-node dictionary/original-check dependency graph.

Build with Java17 for compilation/D8 and Java21 for the Morphe loader:
  MORPHE_JAVA=/path/to/java21/bin/java python build1817.py --download \
    --input-dir input --tools tools --work work --output output

For a fresh ABI review use --compile-only, then:
  python tests/audit_punctuation_abi.py --help
The proof in tests/native_abi_review1817.json must match the exact output DEX.
The publication workflow checks source pins, publisher regression tests, two
independent complete builds, reviewed asset hashes and preservation contracts.
It then verifies uploaded bytes and advances both Manager feeds atomically with
expected-head leases, preserving unrelated metadata and ULike policy.

Scope and installation
Target: com.touchtype.swiftkey.beta, 9.13.16.4 (1236271168).
Update the Manager source to 1.0.300 and apply the SwiftKey patch to the unmodified
target APK. Install using the same signing settings. Source update alone does
not replace the installed keyboard. Keep the existing upward-flick setting on.
Actual device appearance/feel and a complete clean-original-APK apply remain
unverified. The archived APK used for static analysis is a previous modified
archive, not an original-APK apply test. Do not represent these checks otherwise.
