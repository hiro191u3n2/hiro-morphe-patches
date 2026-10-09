# ULike v1.9.65 camera trace publication

`publish1965.py` publishes camera lifecycle safeguards and diagnostics from exact ULike v1.9.64 / Hiro bundle v1.0.197 to v1.9.65 / v1.0.198. It leases both Manager feed heads, preserves unrelated app resources and edits, verifies six release assets and updates both feeds atomically. Its `--local-only` and `--preflight-only` modes perform no remote writes.

Fresh CI executes the camera host tests and validates complete changed/new DEX inventories against the reviewed source graph. The only resource edits are the two version/loader metadata entries and the two DEX entries. All twelve native payloads, their installer, image algorithms, encoding helpers, save formats and unrelated runtime classes are checked against the pinned published .64 payload. GPU execution evidence belongs to .64; this release does not claim a new GPU benchmark or a physical-device fix. Original APK/APKS application and Android device behavior remain untested.

The trace covers intermittent camera symptoms with a bounded asynchronous queue, a rolling log that survives restarts, protected anomaly snapshots and a manual incident snapshot. From the existing 「直近の撮影・工程別処理時間」 row, the user can save and share diagnostic evidence. Runtime camera logs and user incident data are excluded from the source archive and CI artifacts. Tests use synthetic fixtures compiled separately from the shipped runtime.

The archived workflow must be identical to `.github/workflows/ulike1965-camera-trace-publish.yml`. Source and publication hashes are declared before CI. Changed declarations or concurrent feed/source updates fail closed. Completed identical retries verify the existing receipt and downloads without writing again.
