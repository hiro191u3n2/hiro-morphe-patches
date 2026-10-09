# ULike v1.9.75 / bundle v1.0.208

Build from exact published .74/.207 with JDK Temurin 21.0.8+9, pinned Morphe/Android/D8 tools and NDK r27c. Only the GPU native transport library is rebuilt; its existing installer row is updated, eleven other native payloads are retained. CPU pixel math and GPU shader source are pinned unchanged. Physical Android and original APKS application remain untested.

Run declare1975.py, build1975.py --prepare-only, declare1975.py --reviewed-mpp DIST, then run a fresh full build, validate1975.py and finalize1975.py. CI reproduces the independently pinned local MPP hashes. publish1975.py --local-only validates all six assets/source bytes; --preflight-only checks both feeds without writes. Publication uses lease-protected atomic direct-descendant updates to both Manager feeds.

The publisher verifies authoritative Git refs with a bounded known-transition retry, immutable downloads, source archive and QA contract. APKs, photos, keys and incident logs are excluded from publication.
