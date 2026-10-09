# ULike mandatory-GPU publication

This workflow rebuilds ULike v1.9.66 / bundle v1.0.199 from the pinned v1.9.65 / v1.0.198 artifacts. It executes current strict-route, noise, protection/correction and native-recovery tests. Historical tests requiring CPU fallback are deliberately replaced with mandatory-GPU behavior tests; an isolated unchanged strong-noise shader regression remains.

GPU-required image stages must have a functional original GLES path. Unsupported required precision may be tested as a failure branch, but cannot substitute for positive GPU processing coverage. Desktop header adaptations are clearly labelled and cannot establish production GLES compatibility. Physical Galaxy capture, quality and speed remain untested.

`declare1966.py` pins the source and publication graph. `build1966.py` produces arm64-v8a native+DEX packages and executed reports; `validate1966.py` checks package/source/native provenance; `finalize1966.py` generates the six release assets and immutable expected manifest. `publish1966.py --local-only` validates without network writes, and `--preflight-only` verifies both live Manager branches read-only.

Actual publication creates a verified GitHub prerelease and immutable raw distribution commit, then updates main/dev Manager feeds atomically using leased HEADs with each new commit a direct descendant. Concurrent feed or policy advances fail closed. It retains all unrelated application resources and eleven inherited native payloads.

Review source and complete the required positive production-GLES gate before enabling the workflow. Original APKs, rebuilt APKs, private photographs and signing keys are excluded from the source archive.

The GPU binary64 implementation includes Berkeley SoftFloat derived BSD 3-clause code. The complete notice is shipped in each MPP as `ulike1965/THIRD_PARTY_LICENSES.txt`, and in the source archive as `THIRD_PARTY_LICENSES.txt`.
