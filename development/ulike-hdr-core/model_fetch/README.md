# Reproducible model acquisition

On 2026-10-01, the app's model metadata endpoint returned HTTP 200 and
`status_code=0` for both target names, with no API key, cookie or authentication
header supplied. Both returned CDN files were downloaded successfully.
This is an observed result for these two requests, not a claim about all app
endpoints or future availability.

| Name | Server version | Bytes | SHA-256 |
|---|---|---:|---|
| tt_baoman | 1.0 | 972,989 | e64f0772bb857e4d990789237c1007b62fb86020a7edcfb0c3a61a7bedc6e2c0 |
| tt_goodlike | 1.0 | 1,015,321 | 0d60ea7e684f32628daac031cb37fd32c50bf898aa3fb62bcbc25673b5725cad |

The MD5 of each body also matches its metadata `file_url.uri`. SHA-256 pins
provide a second content identity. Neither proves a phone is using the same
cached version. The metadata reports `matched_sdk_version=0.0.0` for the
requested SDK version 14.5.0.

Run `python3 fetch_target_models.py --output /path/outside/source/tree` to repeat
the acquisition. A server version/content change is an error requiring review;
the script does not silently replace the pin. The actual script was run and
reproduced both exact model files. No model binary is included in this source
repository or in a patch.

`model_inspect` examines container metadata. Acquiring a weight file does not
execute its inference engine, identify every tensor/operator, establish HDR
semantics or prove output matching the original app.
