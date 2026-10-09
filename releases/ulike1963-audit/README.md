# ULike v1.9.63 / Hiro Morphe Patches v1.0.196

Whole-app audit on the exact published v1.9.62 / bundle v1.0.195.

Confirmed capture/save-generation, GPU cleanup/session, late metadata and renderer/viewport lifecycle bugs are repaired. Unused noise probes, unneeded face-mask neighbourhood scans, repeated immutable JNI strings and declined full-frame qualification snapshots are removed. Black-area double tap is consumed without camera switching, preserving native gestures outside that area.

Pixel algorithms, strengths, resolution, codec configuration, save format, single-image policy and ordered publication are preserved. Tests compare independently compiled byte-frozen v1.9.62 source output and execute real host JNI/Mesa routes plus fault/lifecycle regressions. Physical Galaxy capture, quality and speed and original APKS application remain untested.

Run declare1963.py, build1963.py, validate1963.py, finalize1963.py and publish1963.py --local-only. Publication verifies assets before guarded Manager main/dev feed updates. In Morphe Manager refresh the Hiro source, reapply to original ULike 5.6.2(740), then install the generated application.
