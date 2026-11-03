SwiftKey Beta 1.8.32 / Hiro Morphe Patches 1.0.344

The native c0 prediction implementation rejects setTrailingSeparator by
throwing UnsupportedOperationException. The former SpaceConversion helper
called this setter before native candidate commitment, caught the exception,
and returned to ordinary space handling. This release removes that mutation.
It carries an identity-scoped separator override during the native commit and
rewrites seven separator reads in six native methods to the helper accessor.
Other candidates keep their own native separator. The existing space P entry
hook, native candidate identity, candidate ranking, dictionaries, Japanese-mode
checks, cursor guards and native commit methods remain in use.

Build from published SwiftKey 1.8.31 and combined 1.0.343 MPPs at immutable
commit bc057500507fd8877adff8bc5f85694b6d9cb5a5. Input artifacts and tools are
pinned by URL and SHA-256 in build1832.py. Java17 compiles sources; Java21
executes Morphe loader and DEX tools.

  MORPHE_JAVA=/path/to/jdk21/bin/java python build1832.py --download \
    --input-dir INPUT --tools TOOLS --work BUILD_ONE --output DIST_ONE
  MORPHE_JAVA=/path/to/jdk21/bin/java python build1832.py \
    --input-dir INPUT --tools TOOLS --work BUILD_TWO --output DIST_TWO

Fresh distinct work/output pairs are required. Initial ABI verification for
changed Java source requires --apk PATH to the pinned archived reference; its
verified native-abi.json is then saved under runtime/tests for independent
reproduction without distributing application bytes.

Both builds rerun behavior, immutable-prediction regression, cache preservation,
native route and native separator fixture tests. They verify runtime ABI,
apply the actual new JVM loader to fingerprinted native method fixtures under
both Morphe SDKs, verify Android loader preservation, load final resources,
and inspect the dependency/selection graph. Their six artifacts must match
the reviewed publication manifest. CI independently performs both builds again.

Only META-INF/MANIFEST.MF, extensions/swiftkey_japanese.mpe, classes.dex and
app/hiro/swiftkey/patches/SpaceConversionPatch.class may change in each MPP.
Within the runtime only SpaceConversion may differ. Within the Android loader
only SpaceConversionPatch may differ. All other runtime classes, JVM/Android
loader classes, dictionaries, resources and other-app patches are preserved.
Source archives contain no application or signing-key bytes.

The native reference is a previously modified SwiftKey Beta 9.13.16.4 APK.
The setter failure is established by native bytecode and the corresponding
immutable host fixture. The user's installed APK and on-device execution
were not inspected. No clean-APK application or device success is claimed.

Publication pins immutable download commits, checks the active baseline and
uses expected-HEAD leases and atomic main/dev feed commits. A newer active
bundle or source head stops publication. Other-app policies remain unchanged.
