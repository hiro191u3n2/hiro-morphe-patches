# ULike v1.9.35 / integrated v1.0.168 publication

Run build1935.py with the pinned v1.9.34 standalone, v1.0.167 integrated baseline, Java toolchain and NDK-built native artifact. Execute validate1935.py against the original APKS for both MPPs, then finalize1935.py and review_release1935.py. The publisher accepts only exact reviewed artifact fingerprints, source inventory, DEX delta and APK validation evidence.

The final workflow rebuilds the native code and MPP from source, runs host/JNI/ARM64 kernel regression tests and binds the resulting MPP fingerprints to the local original-APKS validation. CI does not pretend to run a Galaxy camera or original Android application. The main/dev Manager feeds are leased before release publication; concurrent changes cause a failure rather than overwrite.

The separate native bootstrap workflow only prepares testable compiler outputs for desktop validation. It does not update release assets or Manager feeds.
