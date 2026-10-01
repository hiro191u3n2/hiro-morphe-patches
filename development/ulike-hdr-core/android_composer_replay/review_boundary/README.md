# Independent native composer observer review

`verify.py` compiles the production observer against SDK 36 and D8, executes
independently written host cases, and inspects the caller-owned original APK and
private 13-method observation delta with Androguard. It writes source and input
hashes to `INDEPENDENT_REVIEW.json`.

Host cases cover 25 initialization outcome combinations, an independent UTF-8
initialization fingerprint, native handle read failures, sticky invalid states,
reentrant and nested calls, the 32-recorder and 16-call bounds, lifecycle reuse,
and 40 deterministic concurrent-call cases. Generic parameter tests verify raw
float bits, ordered node/tag/replacement ownership, exact list shapes, and rejection
of partial counts, nonfinite values, and ambiguous strings.

The independent DEX comparison removes only declared observer calls and the
appended failure handler, then compares every original opcode, register, literal,
reference, and branch destination. It separately verifies actual parameter and
return registers, all normal exits, and original rethrows into the added handler.
This complements the author's full-method reconstruction verifier.

An owned entry snapshot proves what the observer saw. The original native wrapper
still receives caller-owned arrays/objects, so concurrent caller mutation can change
what native code consumes. The snapshot does not prove native execution, queue
completion, complete mutation coverage, or original-style restoration. Those
requirements remain unavailable; the hooks are compile-time disabled and the
private delta is not installed.

Reproduce from the workspace root after creating the private delta with
`complete_integration169/lifecycle/validate_composer.sh`:

```sh
PYTHONPATH=ulike_work/models168/python_deps \
python3 ulike_work/hdr_rebuild167/core/android_composer_replay/review_boundary/verify.py
```

The tests do not run ART, the native ULike SDK, or a Samsung handset.
