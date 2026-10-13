#ifndef H7_TEST_JNI_H
#define H7_TEST_JNI_H
#include <stdint.h>
typedef int32_t jint;
typedef int32_t jsize;
typedef unsigned char jboolean;
typedef void *jobject;
typedef jobject jclass;
typedef jobject jarray;
typedef jarray jintArray;
struct JNINativeInterface_;
typedef const struct JNINativeInterface_ *JNIEnv;
struct JNINativeInterface_ {
    jsize (*GetArrayLength)(JNIEnv *, jarray);
    jboolean (*IsSameObject)(JNIEnv *, jobject, jobject);
    void *(*GetPrimitiveArrayCritical)(JNIEnv *, jarray, jboolean *);
    void (*ReleasePrimitiveArrayCritical)(JNIEnv *, jarray, void *, jint);
};
#define JNIEXPORT
#define JNICALL
#define JNI_FALSE ((jboolean)0)
#define JNI_TRUE ((jboolean)1)
#define JNI_ABORT 2
#endif
