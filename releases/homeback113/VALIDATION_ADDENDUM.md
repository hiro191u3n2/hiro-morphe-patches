# Back v1.1.3 / bundle v1.0.238 — final validation clarification

The immutable release notes describe the preparation-stage validation plan. Their sentence about not implying a local SDK build applies to that initial host-only plan; the final validation is broader. A complete local Android SDK/D8/Morphe build was subsequently executed successfully before publication. Its MPP and QA hashes were pinned in publication/manifest-template.json. Both clean GitHub Actions builds matched those local hashes exactly. The canonical final record is publication_HomeBack_v1.1.3.json, which correctly sets local_sdk_build=true.

Workflow run 38625393139 and job 116063127824 completed successfully, including both clean SDK builds, 137 host cases / 559 assertions, DEX structural audits, 10 synthetic APK applications per build, unrelated resource/loader preservation, release-byte verification and atomic main/dev Manager-feed publication. The standalone patch is 1.1.3; the integrated bundle is 1.0.238. The six published release assets remain unchanged.

No production TikTok or Trip.com APK application and no physical Android device behavior were tested. In particular, elimination of the reported Trip.com Recents symptom on the user's phone remains unconfirmed. Compilation, host doubles and synthetic APK tests must not be interpreted as device validation.

TikTok Messages/Profile root-tab Back now requests the app's real Home tab; a later distinct Home Back exits. Trip.com observes bounded delayed native Back termination/stop and retries removal of verified original own tasks. Instagram Home/Reels and X Home-only behavior are preserved.
