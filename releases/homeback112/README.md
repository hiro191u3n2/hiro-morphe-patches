# Back exit / Recents fix 1.1.2 — bundle 1.0.237

Trip.com native terminal Back now enters explicit task cleanup. Existing home recognition remains, and root platform BACK / root finish / moveTaskToBack callsites are bridged without changing their original virtual or super dispatch when the new guard declines. Only actual committed BACK contexts enable this fallback; unrelated keys, Home, lifecycle calls, keyboard/dialog closure and detail Activity finish are preserved. All known app-owned tasks are excluded from Recents before removal. Foreign-rooted tasks are not deleted.

Instagram additionally exits from positively recognized Reels tabs and full-screen Clips viewers. Stories (`reel_viewer`) are explicitly not Reels. TikTok additionally exits from recognized messages and profile screens. Normal navigation, keyboard and modal priority remain. X home policy is unchanged.

Tests: 108 host cases / 428 assertions; DEX wrapper/terminal bridge ABI, original body normalization, exception and high-register checks; 10 real Morphe patcher runs against synthetic fixtures (NOT production app APKs). 69 unrelated resources and 246 unrelated loader classes retained. Publication requires a fresh CI rebuild to exactly reproduce local-reviewed MPP and QA hashes.

Build with JDK21 and the SHA-pinned tools in src/build.py:

    python src/build.py --base baseline236.mpp --tools tools --legacy legacy --work fresh-work --dist dist

Production APK application and a physical Android device have NOT been tested. View markers/fragment names can differ between app versions. Task removal is not Force stop and does not guarantee stopping services. Repatch original APKs; do not layer over the old OneBack patch.
