SwiftKey 1.8.26 / Hiro Morphe Patches 1.0.315

This release changes explicit soft Shift to use native Caps Lock and extends the existing related-word emoji dictionary. It starts from the published SwiftKey 1.8.25 and combined 1.0.314 packages, preserving the expanded Japanese dictionary and the complete runtime extension byte for byte.

Build with Java 17, plus MORPHE_JAVA pointing to Java 21:
python build1826.py --download --input-dir INPUT --tools TOOLS --work WORK --output DIST
WORK must be a new directory. Inputs and tools are pinned. Two independent builds and reviewed byte-identical artifacts gate publication.

The emoji dictionary builder preserves all existing associations and candidate order, normalizes curated Japanese variants, and tests the actual existing EmojiCandidates implementation. The emoji loader stage changes only the pinned asset digest in the JVM constant pool and the Android loader initializer. Asset capacity remains unchanged.

The Shift stage validates the native keyboard contract and changes the ordinary soft Shift transition from SHIFTED to CAPSLOCKED. Native typing preserves Caps Lock; the next Shift tap turns it off. Other state transitions, native automatic sentence capitalization, and hardware keyboard handling are unchanged in code. Because the requested tap behavior now enters Caps Lock, native long-press/double-tap handling observes that state. A second tap intentionally unlocks, rather than requiring a double tap to lock.

Only the specific native Shift instruction is transformed; the native input/gesture logic is not replaced. Published validation includes native method structure, state-path regressions, duplicate-apply/drift rejection, loader preservation, real Morphe loader selection and resource access. APK-derived structural evidence is distinguished from Android execution.

Limits: Android handset touch/display operation and a complete patch of an untouched APK are not verified. The source archive excludes APK and signing-key bytes. The published artifact is an MPP patch bundle; users must update the source in Morphe Manager, repatch untouched SwiftKey Beta 9.13.16.4, and install the generated APK using the same signing configuration.
