# Same-still 106-point face analysis boundary

This component verifies the actual stock `tt_face_v11.1.model` and binds
analysis pixels/results to one immutable captured `HdrFrame`. It **does not yet
provide a verified callable Android face detector or reproduce every style**.

`face_model.py` reads all 11 groups using the face feature's own normal loader
format. The ordinary loader data is read from the exact caller-supplied pinned
`libeffect.so`, not copied into this source. Every decoded group must match its
native checksum and pinned SHA-256. The model, library, loader material, decoded
graphs and weights are not distributed. Purity's loader data is not reused for
this different feature. The three landmark network configurations explicitly
describe 106 points and 120×120 input; this describes a neural substage, not the
saved photograph's resolution. Mixed `bits` values are retained as metadata;
weight layout/quantization and detector preprocessing/postprocessing are not
claimed to be implemented by this inspector.

`StillFaceAnalysis.prepare` samples an immutable captured frame through an
explicit caller-supplied analysis colour policy. It owns the resulting encoded
SDR RGB8 proxy, applies requested rotation/mirror, and retains the exact inverse
pixel-centre mapping to the original HDR grid. Only this analysis proxy may be
downscaled. The HDR photograph is retained unchanged. No HDR→SDR renderer is
silently inferred or supplied by this component.

`analyse` invokes a synchronous `Detector` on those exact proxy bytes, validates
106 ordered finite points, copies the results, and maps them back to the HDR
pixel grid. `Result.requireSource` requires the same `HdrFrame` object; matching
timestamps, camera IDs or face IDs alone cannot substitute another frame.
Detector errors propagate. There is no cached-preview or empty-success fallback.
The detector implementation itself must prove it processed the submitted image;
wrapping arbitrary old points in this interface would not establish that fact.

The current stock API investigation found the offline `VEImage` Java callback
wrapper, but its expected native registration methods were absent from all 58
shipped arm64 libraries. That Java class's existence is therefore insufficient
proof that the API is usable. The separate native recorder still-input route
has now been traced through effect processing to its 106-point callback; see
`STILL_FACE_ROUTE_FINDINGS.json`. This is conditional static evidence for one
identified native backend, not device execution. Its camera-free initialization,
actual backend selection, model/license success, callback worker teardown, and
landmark coordinate mapping remain unverified. In particular, JNI copies the
coordinates unchanged while the still renderer can rotate, mirror and resize
the image; public rotation setters do not establish that transformation for a
camera-free recorder. No production adapter guesses those values or consumes
latest-preview landmarks.

The Java host suite covers all eight rotation/mirror combinations, pixel and
landmark agreement, centre mapping after reduction, immutable input/output,
source ownership despite identical metadata, invalid counts/nonfinite values,
and propagated backend errors. It uses a synthetic detector and renderer, so it
does not measure face detection quality or establish device execution.

Run the Java suite with `JAVA_HOME` pointing to a JDK and `bash test.sh`. With the
sibling `model_inspect` and this directory on `PYTHONPATH`, run:

```sh
python face_model.py /private/tt_face_v11.1.model /private/libeffect.so
```

The CLI prints bounded metadata only. No authentication or license checks are
changed; an eventual Android SDK backend must use normal authorized initialization.
