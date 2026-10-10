# ULike v1.9.50 exact integer GPU backend

`libulike_gpu1949.so` is a separate arm64 Android26 library. The unchanged
`libulike_speed1935.so` remains the CPU implementation and fallback. GPU support
requires GLES3.1, five compute shader storage blocks, a64 invocation workgroup,
2KiB shared memory and sufficient storage buffer limits. Unsupported drivers,
allocation/compilation failures, busy external EGL contexts and failed GPU
operations return false so the existing CPU computation can finish the shot.

The GPU computes integer NV21 luma block sums for the existing temporal
thumbnail. The original CPU converts the exact sums/counts to float and retains
all registration, interpolation and fusion arithmetic. The second entrypoint
packs RGB into integer luma/chroma, then computes the exact residual1944 noise
summaries. The two dispatches share GPU storage with an explicit memory barrier;
the packed intermediate is never read back. Noise workgroups cooperatively load
an8x8 tile and four pixel halo into shared memory. Every invocation enters the
barrier, including image tails and inactive pixels. Radius4 retains the original
sparse7x7 weighted taps and dense9 sample periodic texture test.

A process wide mutex controls a private, unshared EGL pbuffer/context. Each call
releases that context before returning; an existing caller context is rejected
without being replaced. The shared EGL display is never terminated. Compiled
programs and buffer capacities persist across calls, while input RGB/NV21,
metadata and destination seeds are uploaded fresh for each call. Range tables
are reused only after value equality; per-call image staging capacities are
retained under the native mutex and overwritten before every dispatch.
The200ms GPU fence must signal before any readback mapping. A fence failure
disables the backend for the process. Results are read into private host storage
and committed to Java output arrays only after successful GPU completion and
readback. JNI critical sections contain final memcpy operations only.

`build_native1949.py --ndk <NDK-r27c> --output <directory>` pins
NDK27.2.12479018 and checks16KiB ELF LOAD alignment, arm64 architecture, JNI
exports and Android system library dependencies. `host_gpu1949.py --work <dir>`
executes the production shaders using software EGL and compares integer outputs
with the unchanged scalar residual1944 implementation. Physical Android/Galaxy
performance and camera completion require device testing and are not claimed by
these host checks. Java admission additionally compares complete GPU and CPU
results and upload/fence/readback inclusive timings before using the GPU path.


The v1950 `finishNative` entrypoint runs neighborhood aggregation and complete
integer residual pixel finalization in one shader. It preserves the original
rounding, luma bounds, tone, beauty coordination, alpha and inactive destination
pixels. Pixel metadata retains CPU float-derived sigma, skin and detail policy.
Only final pixels cross back to Java; no residual summary is read back or
uploaded into a second stage. The admitted `finishIntoNative` path commits into
the original destination slice with `SetIntArrayRegion` and no additional Java
seed/copyback or whole-frame critical-array copy/release. GPU
probation measures candidate allocation and seeding inside its timer and uses
the same original CPU finalization as a reference. CPU summary workspace is
lazily rented once for a strip and reused across its batches. `QualityShadow1932` submits at most128 rows and
bounds combined metadata/policy storage to12MiB. Null-plan legacy thread-local
policy remains on its original exact CPU finalization path.

`../host_gpu_finish1950.py --work <dir>` executes the production C/JNI and fused
shader on Mesa EGL with JNI checking. Its oracle uses the unchanged scalar
aggregate plus original Java finalization; it includes transparent/inactive
seeds, pooled tails, random beauty masks, borders, full4080-wide batches, zero
weights, foreign contexts and a forced timeout that never maps output storage.
The runtime GPU gate compares complete pixel outputs and transfer-inclusive
latency before admitting a shape. This remains separate from Android camera and
physical-device performance verification.
