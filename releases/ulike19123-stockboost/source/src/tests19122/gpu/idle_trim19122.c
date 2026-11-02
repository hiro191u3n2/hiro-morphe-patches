#include <jni.h>
#define T(n) Java_com_hiro_ulike_GpuIdleTrim19122Test_##n
#define G(n) Java_com_hiro_ulike_GpuNoise1960_##n
static jlong bytes;
static jint trims;
static jboolean refuse;
JNIEXPORT void JNICALL T(configure)(JNIEnv *e,jclass c,jlong n,jboolean blocked){(void)e;(void)c;bytes=n;refuse=blocked;trims=0;}
JNIEXPORT jint JNICALL T(trimCalls)(JNIEnv *e,jclass c){(void)e;(void)c;return trims;}
JNIEXPORT jlong JNICALL G(retainedNative)(JNIEnv *e,jclass c){(void)e;(void)c;return bytes;}
JNIEXPORT jboolean JNICALL G(trimNative)(JNIEnv *e,jclass c){(void)e;(void)c;trims++;if(refuse)return JNI_FALSE;bytes=0;return JNI_TRUE;}
