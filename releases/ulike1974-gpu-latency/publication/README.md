# ULike v1.9.74 / bundle v1.0.207

Build from exact published .73/.206 with JDK Temurin 21.0.8+9 and pinned Morphe/Android/D8 tools. The GPU native and installer are retained byte for byte. Native regression suites use NDK r27c and Mesa software GLES; physical Android and original APKS application remain untested.

Run declare1974.py, build1974.py, validate1974.py, declare1974.py --reviewed-mpp DIST, then repeat the build/validation with the final manifest and finalize1974.py. The independently pinned local MPP hashes are reproduced in CI. publish1974.py --local-only validates all six assets and source bytes; --preflight-only checks both feeds without writes. Final publication uses lease-protected atomic direct-descendant updates to both Manager feeds.

The publisher verifies branch refs through Git to avoid stale REST HEAD results immediately after push. It verifies immutable downloads, source archive, QA contract and byte-preserved resources. It excludes APKs, user photos, keys and incident logs.
