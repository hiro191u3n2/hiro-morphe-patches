Synthetic Yahoo patch scheduler fixture
======================================

All fixture XML and Java files are authored test data. No private APK, original
app resources, or original application DEX are inputs to this builder. The
resource topology comes from the existing generated NearMeLayoutTest and
FullPatchExecutionTest fixtures. The home layout adds the generated Data Binding
root and binding_1 tags needed to cover the previously skipped-ad-shield case.
The view_search_btn layout adds three independently authored Button fixtures.
Its middle synthetic_reverse_search ID is explicitly a test identifier, not a
claimed resource ID from Yahoo 8.5.6. The exact displayed label is resolved via
the synthetic_reverse_search string; the two neighbouring buttons must remain
unchanged. No route names, departure times or screenshot content are copied.

Build (Linux):

  python create_scheduler_fixture.py --tools /path/to/yahoo-tools \
    --work /path/to/empty/build-directory --output /path/to/synthetic.apk

Required tools directory: android.jar (Android platform 35_r02) and d8.jar
(Android build-tools 35). The builder checks their fixed SHA256 values.
The builder first uses aapt2 in --tools, or the binary selected by --aapt2.
If neither is available, it downloads the pinned Google build-tools 35
archive, verifies it and extracts only android-15/aapt2 into --tool-cache.
Supply --aapt2 /path/to/aapt2 for an entirely offline build; its hash is checked.
The JDK is selected from PATH, or from the optional --java-home directory.

Outputs under --work:

  synthetic-yahoo-transit-8.5.6.apk   unsigned scheduler input, never a release asset
  build-report.json                source/tool hashes, DEX descriptors, output hash
  build-commands.log                tool output for CI diagnosis

The APK contains a binary manifest, compiled resources and exactly two synthetic
class definitions: TransitApplication and CoreComponentFactory. It has no launch
activity and is not a functional app. ZIP entry ordering, timestamps, permissions,
and storage mode are normalized. Resources are compiled from stable relative
paths, and Java debug metadata is disabled for reproducible output.

The public repository may contain this Python builder and these authored text
fixtures. Do not commit or publish the generated APK or downloaded tool binaries.
This fixture cannot establish real-app or on-device behavior.
