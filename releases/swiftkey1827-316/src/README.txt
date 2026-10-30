SwiftKey Beta 1.8.27 / Hiro Morphe Patches 1.0.316

This release fixes English QWERTY and Japanese-QWERTY ABC up-flick handling.
The existing switch and sensitivity setting control the gesture; their saved
values remain unchanged and the switch remains off by default. Native long-press
characters can include numbers, symbols, and accented letters. The runtime resolves the native
long-press action of the touched key, captures only recognized upward intent,
and commits at release after validating the editor and keyboard snapshot.

Build from the published 1.8.26 and 1.0.315 MPPs and pinned tools:

  MORPHE_JAVA=/path/to/jdk21/bin/java python build1827.py --download \
    --input-dir input --tools tools --work build-one --output dist-one
  MORPHE_JAVA=/path/to/jdk21/bin/java python build1827.py \
    --input-dir input --tools tools --work build-two --output dist-two

Use Java 17 for compilation and Java 21 for the Morphe loader probes. The work
and output directories must be fresh. Each invocation builds the runtime and
runs the host regression, DEX preservation, and loader/resource checks.

Only META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe may change in
either package. The runtime class inventory remains unchanged, and only the
FlickSymbols family may differ. All dictionary files, emoji mappings, native
patch loaders, the sticky Shift patch, and other bundled app patches remain
byte-identical to the published baseline. Source, test, QA, and checksum files
are packaged reproducibly. Publication requires two identical builds and pins
all source files and output assets before any distribution feed update.

The supported target is unmodified Microsoft SwiftKey Beta 9.13.16.4
(com.touchtype.swiftkey.beta, versionCode 1236271168). The archived APK used for
native symbol inspection was previously modified. Host and native structural
checks do not establish Android handset behavior or clean-APK patch success;
those are explicitly marked untested in QA.
