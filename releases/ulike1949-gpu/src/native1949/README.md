# ULike v1.9.49 exact integer GPU backend

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
metadata, summary seeds and range tables are uploaded fresh for each call.
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
