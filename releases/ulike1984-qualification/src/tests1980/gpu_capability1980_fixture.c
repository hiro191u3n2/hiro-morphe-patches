/* Host-only JNI boundary. No GL/shader result or runtime binary is substituted. */
#include <jni.h>
#include <stdatomic.h>

static atomic_int selected_fault;
static atomic_int native_calls;

JNIEXPORT void JNICALL Java_com_hiro_ulike_GpuCapability1980Test_fault(JNIEnv *env,jclass type,jint value){
    (void)env;(void)type;atomic_store(&selected_fault,value);
}
JNIEXPORT jint JNICALL Java_com_hiro_ulike_GpuCapability1980Test_calls(JNIEnv *env,jclass type){
    (void)env;(void)type;return atomic_load(&native_calls);
}
JNIEXPORT jint JNICALL Java_com_hiro_ulike_GpuNoise1960_nativeAbi(JNIEnv *env,jclass type){
    (void)env;(void)type;return 19601;
}
JNIEXPORT jboolean JNICALL Java_com_hiro_ulike_GpuNoise1960_supportedNative(JNIEnv *env,jclass type,jint shader){
    (void)type;(void)shader;atomic_fetch_add(&native_calls,1);
    int failure=atomic_load(&selected_fault);
    if(failure==1||failure==3){
        jclass exception=(*env)->FindClass(env,failure==1?"java/lang/IllegalStateException":"java/lang/OutOfMemoryError");
        if(exception!=NULL)(*env)->ThrowNew(env,exception,"host owner capability fault");
        return JNI_FALSE;
    }
    return failure==2?JNI_FALSE:JNI_TRUE;
}
JNIEXPORT jlong JNICALL Java_com_hiro_ulike_GpuNoise1960_retainedNative(JNIEnv *env,jclass type){
    (void)env;(void)type;return 0;
}
JNIEXPORT jstring JNICALL Java_com_hiro_ulike_GpuNoise1960_environmentNative(JNIEnv *env,jclass type){
    (void)type;return (*env)->NewStringUTF(env,"gpu1980-capability-jni-fault-fixture");
}
