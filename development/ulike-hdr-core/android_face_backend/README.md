# Stock SDK single-still face diagnostic

This is an Android-callable diagnostic for one submitted analysis image. It is
**not yet a production `StillFaceAnalysis.Detector`** and is not installed by the
current MPP. It does not claim the face-coordinate contract, Android execution,
or the complete Natural blush / Purity2 pipeline is verified.

The original stock `RecordInvoker` provides a more explicit route than the
high-level null-camera recorder wrapper. This implementation supplies the
actual image width and height to `startPlay`, with rotation and mirror both
zero. Thirty checked native instructions establish that these arguments reach
the fields consumed by still processing. The high-level null-view wrapper
instead supplies `-1` values. `verify_stock_contract.py` also validates all 27
public SDK signatures used by reflection against the four pinned stock DEX
files, and resolves the native join target through the ELF import table. The
code compiles against the real Android SDK 36; reflection avoids redistributing
vendor API class files. It does not call private native methods directly.

`StockStillFaceProbe.run` performs real SDK calls when invoked inside the app:

1. Require the app's already initialized, normally licensed SDK and an app-wide
   exclusive idle lease. Verify the installed arm64 `libttvesdk.so` hash.
2. Own a separate, bounded sRGB ARGB8888 bitmap supplied explicitly for analysis.
   No HDR photograph is converted implicitly or replaced. No camera is attached.
3. Create a new recorder, set image-mode face detection, start with explicit
   geometry, and require a successful **actual initialization callback**.
4. Register a private face callback, submit exactly one bitmap, and deep-copy
   the 106-point arrays and scores. Require both image completion and one valid
   post-submission face callback. Early callbacks cannot satisfy the request.
5. Stop, unregister and uninitialize on the calling worker thread; never join a
   callback worker from itself. Keep input/callback references alive until these
   calls return. On teardown failure, retain them and refuse further probes.

The stock Java initialization callback sets an internal ready flag even after
a negative code; this probe never trusts that flag. The stock `uninitBeautyPlay`
also clears static listeners, so a second recorder beside active preview is
unsafe. **The application-wide idle lease is an integration requirement, not an
implementation supplied by this module.** The app must release its existing
recorder normally and hold its actual lifecycle lock for the whole probe. The
local `SERIAL` lock alone cannot coordinate other SDK users. No license,
resource-finder, or authorization state is modified or bypassed.

The result is copied diagnostic data, including raw landmarks, callback order,
and mean RGB errors for all eight exact rotations/reflections of the submitted
pixel grid. Dimension-incompatible comparisons are NaN. These image comparisons
help reveal rendering orientation; they do **not** prove that landmark coordinates
use the same grid, and they never silently select a landmark transform.
`sourceCoordinateContractVerified` is always false. The diagnostic writes no
photos or landmarks to files and uploads nothing. The requested fresh workspace
must be inside the app cache; the SDK may create its own temporary files there.

The callback deadline limits waiting for Java callbacks. Native `stopPlay` and
unregistration join native workers and can themselves block: this diagnostic
does not pretend that interrupting Java forcibly cancels vendor code. Device
runtime errors, unsupported model/license initialization, no callback, duplicate
callbacks, and teardown failures remain visible failures.

Still needed before production integration:

- Real app lifecycle lease / launch hook, and normal SDK initialization on device.
- Actual camera-free startup and runtime backend selection.
- A noncentral face fixture with known orientation, mirrored and rotated copies,
  confirming SDK106 ordering and the exact coordinate domain/scale against the
  submitted grid. Rendered-image similarity alone is insufficient.
- Repeated success, no-face, initialization-failure and timeout/teardown runs on
  the phone. There is no connected Android device in the host tests.

Run `test.sh` with `JAVA_HOME` and `ANDROID_JAR` set. The host suite has 66 checks
covering both callback orders, early/duplicate callbacks, timeouts, failed
initialization, bad dimensions/counts/nonfinite data, copied ownership,
termination, and hand-written asymmetric fixtures for all eight transforms.
These are synthetic callbacks, not native detection-quality tests.

For the independent static stock contract:

```sh
python verify_stock_contract.py /private/stock/base /private/libttvesdk.so
```

Dependencies are androguard, pyelftools and capstone. All original binaries,
model material and full decompiled classes remain caller-owned and undistributed.
