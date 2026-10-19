# OneBack v1.1.15 / bundle v1.0.259 publication

The baseline is public bundle v1.0.258, SHA-256 `819ac9522cbe163acbb95839915ccf503618cbc95a528ed0892ed727732eea5d`, at immutable commit `9aa9259c3eaaf518964fc5a491042a7154dc16b9`. Reviewed feed heads are main `e331acb4a33ef22d5c39dfb280e61d435cd04758` and dev `7e55ffd0a2791eca8e7013b6b8976315378706b9`. Preserve the concurrently published Fossify Gallery changes, current ULike v1.9.108, Instagram behavior and all unrelated patch resources. If the live baseline advances, stop and rebuild on the new bundle instead of only changing the expected heads.

The target is X/Twitter (`com.twitter.android`), with Back exit from the four top-level Home, Search, Notifications and Chat tabs, recent-task removal, and Home on the next normal launch. The supplied input contains other Morphe patches but no prior OneBack runtime classes. The supplied input SHA-256 is `f6792d27f18ecb7e37fddcd1f67cf6f65f4d667fd3a8941e09c7ac266058e1ae`.

`local-apk-validation.json` is deliberately absent while verification is pending. It must be replaced by newly generated X evidence; prior Instagram PASS results are not reusable. Required fields are:

- `schema: "homeback125-local-apk-validation-v1"`, `result: "PASS"`, `package_name: "com.twitter.android"`.
- Exact `input_apk_sha256`, `input_already_patched: true` (other Morphe patches), `input_has_prior_oneback: false`, `physical_android_tested: false`.
- `artifacts` identical to final QA (both MPP filenames mapped to bytes/SHA-256), and the same `runtime_sha256`.
- `outputs` for both MPP filenames, each with `result: "PASS"`, `output_apk_sha256`, `four_tab_exit_hook_verified: true`, `normal_launch_home_hook_verified: true`, `runtime_classes_match: true`, and `full_semantic_audit: true`.

Additional source, app-code and resource comparison evidence should explain the exact scope and any expected rebuilding deltas. These records do not establish physical Android behavior. Do not commit or package the user's APK or either output APK.

After final source/build/audits complete:

```sh
python3 releases/homeback125/publication/freeze_local.py --dist <dist>
python3 releases/homeback125/publication/package.py --dist <dist> --legacy releases/oneback163/src
python3 releases/homeback125/publication/publish.py --dist <dist> --repo . --base <baseline258.mpp> --local-only
```

The prepared workflow lives both here and at `.github/workflows/homeback125-publish.yml`; the files must have identical bytes. Only the root agent performs remote publication. Source must be one direct child of the reviewed main head and leave feeds unchanged initially. The push triggers two fresh pinned Temurin 21.0.8 / SDK builds. MPP and QA bytes must match the frozen local outputs. `verify_pair.py` records the actual CI run and source IDs, then packaging, local validation and read-only preflight precede remote mutation.

Publication refuses an occupied tag, release, distribution branch or output filename. It creates exact release assets and immutable raw MPP downloads, then atomically updates both main/dev feeds with explicit leases. Feed description and changelog history are preserved; only X and Twitter app-name scopes are marked updated. `releases/ULike_ACTIVE.json` must remain byte-identical. Failed partial publication must be inspected through its receipt before retrying.

Existing runtime120/121/122/123 reference assets are preserved from baseline258 with exact bytes. The new runtime124 reference is pinned to `559d89a6e7c3008cfb14b1e2c5e46285cf3a48365725d87524195a466c02b07f`. Any further runtime reference must be deliberately added with a pinned hash before freezing. No arbitrary binary files enter the source archive. The current prepared files are not evidence that CI, publication or device testing has already succeeded.

The new platform-ordering regressions must reproduce both published v1.1.14 failures: framework `onUserInteraction()` invalidating the key-up Back attempt, and API 33 AndroidX platform fallback bypassing native X root navigation after child handlers finish. The v125 host suites must then prove task exclusion/removal and persistent next-launch Home routing for these paths. This strengthens dispatch coverage without claiming physical device execution.
