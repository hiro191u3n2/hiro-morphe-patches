#include <jni.h>

/* Host-only endpoint: the exact production DEX bilateral performs the two
 * passes. The GLES shader is separately compared with the integer oracle. */
JNIEXPORT jint JNICALL Java_com_hiro_ulike_CorePixels1950_nativeAbi(JNIEnv *env,jclass owner){(void)env;(void)owner;return 1950;}
JNIEXPORT jint JNICALL Java_com_hiro_ulike_CorePixels1950_bilateralPairGpuAbi(JNIEnv *env,jclass owner){(void)env;(void)owner;return 1951;}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_CorePixels1950_bilateralPairGpuNative(JNIEnv *env,jclass owner,
 jintArray source,jintArray output,jintArray horizontal,jint width,jint rows,jint noise,jboolean texture,
 jboolean shadows,jint first,jint last,jintArray luma,jintArray colour){
 (void)owner;(void)luma;(void)colour;
 jclass oracle=(*env)->FindClass(env,"com/hiro/ulike/H8DexOracle1951");
 if(!oracle)return JNI_FALSE;
 jmethodID method=(*env)->GetStaticMethodID(env,oracle,"gpuPair","([I[I[IIIIZZII)Z");
 if(!method)return JNI_FALSE;
 return (*env)->CallStaticBooleanMethod(env,oracle,method,source,output,horizontal,width,rows,noise,texture,shadows,first,last);
}
