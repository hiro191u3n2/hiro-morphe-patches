/* Optional, independently identified front backend. The published NR library
 * remains the CPU oracle and fallback. This DLL exports no legacy JNI symbol. */
#define COLOUR_CACHE_CPU_INCLUDED1976 1
#define Java_com_hiro_ulike_SingleNoise1955_nativeAbi front119_internal_abi
#define Java_com_hiro_ulike_SingleNoise1955_processNative Java_com_hiro_ulike_CpuFrontOptimized19119_processNative
#include "front_core19119.c"
#include "front_identity19119.h"

JNIEXPORT jint JNICALL Java_com_hiro_ulike_CpuFrontOptimized19119_nativeAbi(JNIEnv *env,jclass cls) {
    (void)env;(void)cls;return 19119;
}
JNIEXPORT jstring JNICALL Java_com_hiro_ulike_CpuFrontOptimized19119_nativeFingerprint(JNIEnv *env,jclass cls) {
    (void)cls;return (*env)->NewStringUTF(env,FRONT119_SOURCE_SHA256);
}
