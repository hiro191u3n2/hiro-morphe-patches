/* Header-only JNI ABI stub for syntax checking without an Android SDK. */
#ifndef H9_TEST_JNI_H
#define H9_TEST_JNI_H
#include <stdint.h>
typedef int32_t jint;
typedef int32_t jsize;
typedef int8_t jbyte;
typedef uint8_t jboolean;
typedef void *jobject;
typedef jobject jclass;
typedef jobject jarray;
typedef jarray jintArray;
typedef jarray jbyteArray;
struct JNINativeInterface_;
typedef const struct JNINativeInterface_ *JNIEnv;
struct JNINativeInterface_ {
    jsize (*GetArrayLength)(JNIEnv *,jarray);
    jboolean (*IsSameObject)(JNIEnv *,jobject,jobject);
    void (*GetIntArrayRegion)(JNIEnv *,jintArray,jsize,jsize,jint *);
    void (*SetIntArrayRegion)(JNIEnv *,jintArray,jsize,jsize,const jint *);
    void (*GetByteArrayRegion)(JNIEnv *,jbyteArray,jsize,jsize,jbyte *);
    jboolean (*ExceptionCheck)(JNIEnv *);
    void *(*GetPrimitiveArrayCritical)(JNIEnv *,jarray,jboolean *);
    void (*ReleasePrimitiveArrayCritical)(JNIEnv *,jarray,void *,jint);
};
#define JNIEXPORT
#define JNICALL
#define JNI_FALSE ((jboolean)0)
#define JNI_TRUE ((jboolean)1)
#define JNI_ABORT 2
#endif
