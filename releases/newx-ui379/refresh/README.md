# NewX 120 Hz window preference

`app.hiro.newxui.RefreshRate.bind(Activity)` runs on the main thread after the native `MainActivity.onResume()` body. `UiOptions.force120Hz()` is supplied by the parent runtime and defaults to true. When settings close and X resumes, the enabled state is applied/restored. A focus hook may call it again safely; repeated calls do not rewrite unchanged attributes.

Only supported modes within 0.6 Hz of 120 Hz at the currently active physical resolution are considered. Exact 120 Hz wins, then the nearest rate; the active mode wins a tie. No compatible mode means no preference is added. Only `preferredDisplayModeId` and `preferredRefreshRate` are changed; every other window field is copied. OFF restores previous preferences per window, while retaining any later native write. A weak map avoids retaining destroyed windows. No global display settings, services, permission, timer, hidden API, or frame invalidation is added.

Compile `RefreshRate.java` with the supplied Android stubs or the Android SDK and the parent's real UiOptions. **Do not package `stubs`, `test-stubs`, test classes, or QA into the runtime DEX.** The test UiOptions is only a host test implementation.

`python3 test.py ../tools/ecj.jar` compiles against host stubs and runs behavior tests. It is not an Android installation/display test.

## API contract checked 2026-11-17

- https://developer.android.com/reference/android/view/WindowManager.LayoutParams#preferredDisplayModeId
- https://developer.android.com/reference/android/view/WindowManager.LayoutParams#preferredRefreshRate
- https://developer.android.com/media/optimize/performance/frame-rate

These APIs express a preference, and the OS may ignore it for device policy/capabilities. Power saving, thermal throttling, hardware/firmware, and competing windows can prevent actual 120 Hz. `preferredRefreshRate` is ignored when a mode ID is set; both fields carry matching values, with the compatible exact mode taking precedence. Public SDK min/max display refresh fields are not used. Video Surface frame-rate votes are untouched.
