#include <jni.h>
/* Controlled JNI completion is executed; this is not the production NR kernel. */
static jint control_mode,ordinary_calls,shared_calls;
JNIEXPORT void JNICALL Java_com_hiro_ulike_CpuBackend19115Test_nativeMode(JNIEnv* e,jclass c,jint mode){(void)e;(void)c;control_mode=mode;ordinary_calls=shared_calls=0;}
JNIEXPORT jint JNICALL Java_com_hiro_ulike_CpuBackend19115Test_nativeCalls(JNIEnv* e,jclass c,jboolean shared){(void)e;(void)c;return shared?shared_calls:ordinary_calls;}
JNIEXPORT jint JNICALL Java_com_hiro_ulike_StrongNoise1958_nativeAbi(JNIEnv* e,jclass c){(void)e;(void)c;return 1958;}
JNIEXPORT jlong JNICALL Java_com_hiro_ulike_StrongNoise1958_nativeScratchBytes(JNIEnv* e,jclass c){(void)e;(void)c;return 0;}
JNIEXPORT void JNICALL Java_com_hiro_ulike_StrongNoise1958_nativeReleaseScratch(JNIEnv* e,jclass c){(void)e;(void)c;}
static jboolean execute(JNIEnv* e,jintArray input,jintArray output,jint width,jint begin,jint end){
 if(control_mode==2){jclass error=(*e)->FindClass(e,"java/lang/UnsatisfiedLinkError");if(error)(*e)->ThrowNew(e,error,"controlled native link failure");return JNI_FALSE;}
 if(control_mode==1)return JNI_FALSE;
 jint* values=(*e)->GetIntArrayElements(e,input,0);if(!values)return JNI_FALSE;
 (*e)->SetIntArrayRegion(e,output,width*begin,width*(end-begin),values+width*begin);(*e)->ReleaseIntArrayElements(e,input,values,JNI_ABORT);return (*e)->ExceptionCheck(e)?JNI_FALSE:JNI_TRUE;
}
#define PARAMETERS JNIEnv* e,jclass c,jintArray input,jintArray output,jint width,jint rows,jint begin,jint end,jint vb,jint ve,jint origin,jint noise,jboolean shadows,jfloatArray evidence,jintArray half,jintArray quarter,jintArray eighth,jint fw,jint fh,jintArray policy,jint mode
#define UNUSED (void)c;(void)rows;(void)vb;(void)ve;(void)origin;(void)noise;(void)shadows;(void)evidence;(void)half;(void)quarter;(void)eighth;(void)fw;(void)fh;(void)policy;(void)mode
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_StrongNoise1958_processNative(PARAMETERS){UNUSED;ordinary_calls++;return execute(e,input,output,width,begin,end);}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_StrongNoise1958_processNativeShared(PARAMETERS,jintArray confidence){UNUSED;(void)confidence;shared_calls++;return execute(e,input,output,width,begin,end);}
