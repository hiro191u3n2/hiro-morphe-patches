# SwiftKey 1.8.25 dictionary runtime expansion

This source reconstructs the three WordCandidates types of the published
1.8.24 runtime and proves their executable DEX disassembly matches before
transplanting three implementations and two integer initial values.

Changes are bounded dictionary capacity (192 MiB and 3,000,000 readings),
complete exact-reading candidate extraction without the former 32-column
cutoff, and an OutOfMemoryError handler confined to the optional background
dictionary loader. The native candidate list remains usable if that optional
load cannot allocate memory. Prefix completion retains the existing 12-result,
64-reading, 32-rank bounds. The 1.8.24 prolonged-mark fix is preserved.

Run build_runtime.py with --baseline-mpp, --dictionary, --tools, --work and
--output. Set MORPHE_JAVA to Java 21 when the Morphe dependency requires it.
An optional --reference-apk replays symbolic native ABI inspection against the
locally available modified 9.13.16.4 reference APK. Without that private input,
the build validates the pinned tests/native-abi.json against the exact rebuilt
runtime, verifier source and Android API jar; it does not claim an ABI replay.

The host test executes the production Index, lookup, enhance and background
init methods against every merged reading and candidate, checks preservation
of existing ordered candidates, invalid input handling, prefix bounds, native
deduplication and simulated allocation-failure fallback. The platform and
native editor classes are host doubles; no Android handset execution or clean
original APK patch application is implied.

For the 148,629,104-byte merged dictionary with 2,786,924 readings, the retained
UTF-8 and integer offset arrays total 159,776,800 bytes before array headers and
small objects. This is an allocation estimate, not an Android RSS measurement.
No complete String-valued dictionary map is held by the production runtime.
