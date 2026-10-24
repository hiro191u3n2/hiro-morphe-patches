/* Test-only scalar observation of the included, otherwise unmodified engine.
 * Queries occur after synchronous owner calls, while no owner task is running. */
#include "engine1960.c"
JNIEXPORT jlongArray JNICALL Java_com_hiro_ulike_RegionalStage19115Test_probeNative(JNIEnv *env,jclass cls){
 (void)cls;jlong v[6]={state.attempted[REGIONAL_PASS19115],state.attempted[REGIONAL_MAIN_BASE19115],
  (jlong)state.capacity[12],(jlong)state.capacity[22],(jlong)state.total,(jlong)state.stagingCapacity};
 jlongArray r=(*env)->NewLongArray(env,6);if(r)(*env)->SetLongArrayRegion(env,r,0,6,v);return r;
}
