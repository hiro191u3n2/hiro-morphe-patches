SwiftKey Beta 1.8.31 / Hiro Morphe Patches 1.0.343

This release changes only the SpaceConversion runtime class. A candidate's
native request input is matched exactly against the current native raw input
before optional kana normalization. Japanese mode, active composition state,
selection/cursor, request/candidate identity and pre-commit revalidation remain
required. A normalization failure alone must not reject a matching current
Japanese candidate. No patch-loader entry point is changed.

The native space route audit establishes that the existing P hook is already
on the ordinary software-space path. It explicitly does not reproduce an
old-version failure. The new regression cases model valid native-state raw
request matching; they do not establish which internal raw value occurred on
the user's device. Device behavior remains unverified.

Build from pinned published SwiftKey 1.8.30 and combined 1.0.342 MPPs.
All input artifacts and tools are pinned by URL and SHA-256 in build1831.py.
Java17 compiles sources; Java21 executes Morphe loader and DEX tools.

  MORPHE_JAVA=/path/to/jdk21/bin/java python build1831.py --download \
    --input-dir INPUT --tools TOOLS --work BUILD_ONE --output DIST_ONE
  MORPHE_JAVA=/path/to/jdk21/bin/java python build1831.py \
    --input-dir INPUT --tools TOOLS --work BUILD_TWO --output DIST_TWO

Fresh distinct work/output pairs are required. The initial ABI verification
for changed Java source requires --apk PATH to the pinned archived reference;
its verified native-abi.json is then saved under runtime/tests for independent
reproduction without distributing application bytes.

Both builds rerun behavior, cache-preservation and native-route fixture tests,
verify ABI evidence bound to the exact runtime and verifier, execute both real
Morphe loaders, and inspect final resources. Their six artifacts must match the
reviewed publication manifest. CI performs both builds independently again.

Only META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe may change in
MPPs. Within the runtime only SpaceConversion may differ. The class inventory,
all other runtime classes, dictionaries, both loader representations and all
other apps are preserved. Source archives contain no APK or signing keys.

The native reference is a previously modified SwiftKey Beta 9.13.16.4 APK.
Static native evidence and ABI checking do not establish clean-APK application
or Android-device behavior. The prior video is not included in distribution.

Publication pins immutable download commits, checks the active baseline and
uses expected-HEAD leases and atomic main/dev feed commits. A newer active
bundle or source head stops publication. Other-app policies remain unchanged.
