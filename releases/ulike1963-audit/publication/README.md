# ULike v1.9.63 / Hiro Morphe Patches v1.0.196

This release audits the full ULike modification on the exact published v1.9.62 / bundle v1.0.195 baseline. GPU, save, image-pipeline and camera/UI fixes are declared by the immutable source manifest and verified against the baseline before publication. Resolution, filter strengths, reference counts, precision, colour/beauty settings, save format and encoder configuration remain covered by the required QA contract. Physical Android capture, image quality and speed remain unverified.

Run declare1963.py, build1963.py, validate1963.py and finalize1963.py, then publish1963.py --local-only and --preflight-only. Publication checks artifact hashes, host-executed evidence, source/DEX consistency, standalone/bundle equality and non-ULike preservation. Release downloads are verified before atomically advancing main/dev Manager feeds with exact branch leases. The previous eleven native libraries remain byte-preserved; the existing GPU library can change only as declared.

The publication workflow uses the existing GitHub distribution and Manager feed; it does not modify a Sites project. Source refresh alone does not update an installed ULike application.
