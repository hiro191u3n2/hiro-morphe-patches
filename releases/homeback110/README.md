# Home-only Back 1.1.0 / Integrated bundle 1.0.235

X/Twitter, TikTok and Instagram exit and remove their own recent tasks only at a positively recognized home screen. Native app back handling is retained everywhere else. Keyboard/dialog closure takes precedence. A single native Back that returns from a detail page to home must not also exit. Trip.com retains its prior behavior.

The home detector requires a known main Activity, a visible selected clickable bottom-home tab and no detected keyboard, other focused window, fragment back stack or detail overlay. Unknown screens delegate to the app. The production social APKs and physical Android device have NOT been tested; obfuscated/different UI versions may fail home recognition. These builds must not be represented as device-certified.

Repatch each original, unmodified application APK and reinstall the patched result. Updating the Manager source alone does not modify installed applications. Do not apply over the old OneBack APK: its original onBackPressed body was replaced, and a fresh APK is required to recover it.

Verification: 42 host cases /159 assertions; DEX structure, original bodies, exception paths and registration ABI; 10 real Morphe patcher executions against SYNTHETIC APK fixtures, not production social APKs; preservation of 246 unrelated loader classes and 69 unrelated resource entries.

Rebuild the source archive with JDK21, the pinned Android35/Morphe/D8 tools and the exact reviewed baseline234 MPP:

    python src/build.py --base /path/baseline.mpp --tools /path/tools --legacy legacy --work /path/fresh-work --dist /path/dist

SHA256 pins are embedded in src/build.py. No private application binary, user photo, credential, signing key or third-party APK is in this source archive. Synthetic fixtures exercise code generation, not the actual third-party UI.

Task finishing/removal is not Android settings Force stop and does not guarantee all services/processes cease. Login/search/browsing data is untouched.
