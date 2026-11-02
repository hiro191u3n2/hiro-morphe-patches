/* Host-only scalar visibility; production C is compiled verbatim. */
#include "engine1960.c"
JNIEXPORT jintArray JNICALL Java_com_hiro_ulike_DivisionGpu1973Test_scalarSnapshot1973(JNIEnv *env,jclass cls){
 (void)cls;int programs=0,buffers=0;for(int i=0;i<PROGRAMS;i++)programs+=state.attempted[i]!=0;for(int i=0;i<SLOTS;i++)buffers+=state.capacity[i]!=0;
 jint values[5]={state.initialized,programs,buffers,state.stagingCapacity!=0,state.token!=0};
 jintArray result=(*env)->NewIntArray(env,5);if(result)(*env)->SetIntArrayRegion(env,result,0,5,values);return result;
}
