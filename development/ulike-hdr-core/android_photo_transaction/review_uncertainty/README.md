# Independent publication outcome review

The production `PhotoTransaction` was exercised with 70 independently arranged
provider outcomes: successful calls and I/O, runtime, or `Error` failures before
and after publication, followed by valid, unavailable, absent, or mismatching
inspection results. The focused suite passed 2,927 checks against the source
hashes recorded in `INDEPENDENT_REVIEW.json`.

The review found that a failed pre-publication save could otherwise be attempted
again with the same UUID. The production author added the one-save-attempt guard;
the independent write-failure/retry case now verifies rejection before additional
encoding, insertion, or publication. An explicit verified receipt accessor was
also tested before saving, during unknown/pending outcomes, and after commit and
close.

Other checks cover read-only reconciliation, reused exception objects, a durable
checksummed `PUBLISHING` journal before the provider call, uncertain close without
media cleanup, cancellation on both sides of publication, and recovery from all
five version-1 journal phases with both pending and already-visible rows.

The suite compiles the real Android adapter against pinned SDK 36 and runs D8.
Two real x265 Main10 encodes and FFmpeg decodes seed a test-only codec cache; each
reuse checks every RGB10 input value and the encoded bytes. This isolates provider
failure handling without repeating unrelated neural inference. Provider behavior
is simulated on the host. Android MediaStore and the Samsung handset were not run.

Reproduce with:

```sh
python3 review_uncertainty/verify.py \
  --jdk-bin <JDK21-bin> \
  --android-jar <SDK36-android.jar> \
  --ort-classes <ORT-1.30.0-android-classes.jar> \
  --d8 <r8-8.3.37.jar>
```

This validates the saving protocol, not a finished app integration or release.
