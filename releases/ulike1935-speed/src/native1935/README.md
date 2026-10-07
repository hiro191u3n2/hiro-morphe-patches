# ULike 1.9.35 exact ARM64 speed kernels

Build Android JNI library:

```
python3 build_native1935.py --ndk "$ANDROID_HOME/ndk/27.2.12479018" --output native-build
```

NDK r27c (27.2.12479018), API 26, ARM64 only. No runtime dependencies, no FMA/fast-math, 16 KB page-compatible ELF. The script checks exported JNI names and disassembly, emits SHA-256 and build evidence. Install `libulike_speed1935.so` at APK `lib/arm64-v8a/` and package it as a pinned MPP runtime resource. Java loads it with `System.loadLibrary("ulike_speed1935")`; ABI mismatch/missing library retains Java paths.

Host scalar and ARM64 NEON oracle/guard-page tests:

```
gcc -O3 -fno-fast-math -ffp-contract=off -fno-tree-vectorize kernels1935.c test_kernels1935.c -o tests-host
./tests-host
aarch64-linux-gnu-gcc -static -O3 -fno-fast-math -ffp-contract=off -fno-tree-vectorize kernels1935.c test_kernels1935.c -o tests-arm64
qemu-aarch64 ./tests-arm64
```

Host JNI integration (use system JDK include directory):

```
gcc -std=c11 -O3 -shared -fPIC -fno-fast-math -ffp-contract=off -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" kernels1935.c jni1935.c -o native-host/libulike_speed1935.so
```

S7 copies actual Y/U/V samples exactly, supports row/pixel strides and byte-buffer positions/limits, checks all bounds before output, and never consumes final-row padding. SIMD deinterleave reads only complete accessible groups; scalar tails cover unpadded final samples. Java bulk-row fallback supports non-direct buffers.

S8 vectorizes independent RGB lanes of each horizontal resampling tap and independent float components of the vertical row sum. Tap order, multiply then add, extrema and final Java rounding remain unchanged. ARM64 FPCR is temporarily set to round-nearest and gradual underflow then restored. This does not change capture frames, image dimensions, kernel coefficients, correction strength or compression.

The native kernels are tested separately on host and emulated ARM64. Those tests are not a physical Android/Galaxy timing or image-quality measurement.
