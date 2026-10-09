/* Host-only alias for the existing executed GL wait observer. Production engine
 * and shaders are unchanged; this file is never packaged into Android runtime. */
#include <jni.h>
extern jlongArray Java_com_hiro_ulike_Native1960Test_faultFacts(JNIEnv*,jclass);
JNIEXPORT jlongArray JNICALL Java_com_hiro_ulike_Batch1961Test_facts(JNIEnv *env,jclass cls){return Java_com_hiro_ulike_Native1960Test_faultFacts(env,cls);}

extern void Java_com_hiro_ulike_Native1960Test_setFault(JNIEnv*,jclass,jint);
JNIEXPORT void JNICALL Java_com_hiro_ulike_Batch1961Test_setFault(JNIEnv *env,jclass cls,jint mode){Java_com_hiro_ulike_Native1960Test_setFault(env,cls,mode);}
