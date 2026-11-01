/* One real kernel per translation unit, avoiding any baseline/candidate shared
 * function or static workspace. Both are linked into the same AArch64 process. */
#define JOIN19100_(a,b) a##b
#define JOIN19100(a,b) JOIN19100_(a,b)
#define Java_com_hiro_ulike_SingleNoise1955_nativeAbi JOIN19100(FRONT_ENTRY,_abi)
#define Java_com_hiro_ulike_SingleNoise1955_processNative JOIN19100(FRONT_ENTRY,_process)
#define Java_com_hiro_ulike_CpuSingle1978_createWorkspaceNative JOIN19100(FRONT_ENTRY,_create)
#define Java_com_hiro_ulike_CpuSingle1978_releaseWorkspaceNative JOIN19100(FRONT_ENTRY,_release)
#define Java_com_hiro_ulike_CpuSingle1978_beginWorkspaceNative JOIN19100(FRONT_ENTRY,_begin)
#define Java_com_hiro_ulike_CpuSingle1978_endWorkspaceNative JOIN19100(FRONT_ENTRY,_end)
#define Java_com_hiro_ulike_ColourCache1976_cpuNative JOIN19100(FRONT_ENTRY,_colour)
#include FRONT_KERNEL
typedef struct {jsize n;void *data;} Array19100;
static jsize JNICALL length19100(JNIEnv*e,jarray a){(void)e;return ((Array19100*)a)->n;}
static jboolean JNICALL same19100(JNIEnv*e,jobject a,jobject b){(void)e;return a==b;}
static jint *JNICALL ints19100(JNIEnv*e,jintArray a,jboolean*c){(void)e;(void)c;return ((Array19100*)a)->data;}
static jfloat *JNICALL floats19100(JNIEnv*e,jfloatArray a,jboolean*c){(void)e;(void)c;return ((Array19100*)a)->data;}
static void JNICALL ri19100(JNIEnv*e,jintArray a,jint*p,jint m){(void)e;(void)a;(void)p;(void)m;}
static void JNICALL rf19100(JNIEnv*e,jfloatArray a,jfloat*p,jint m){(void)e;(void)a;(void)p;(void)m;}
static jboolean JNICALL exception19100(JNIEnv*e){(void)e;return JNI_FALSE;}
static void JNICALL put19100(JNIEnv*e,jintArray a,jsize at,jsize n,const jint*p){(void)e;Array19100*v=(Array19100*)a;if(at<0||n<0||at+n>v->n)abort();memcpy((jint*)v->data+at,p,(size_t)n*4);}
int FRONT_ENTRY(int width,int height,const jint *input,jint *output,const float *model,const jint *policy) {
    struct JNINativeInterface_ t={0};t.GetArrayLength=length19100;t.IsSameObject=same19100;
    t.GetIntArrayElements=ints19100;t.GetFloatArrayElements=floats19100;t.ReleaseIntArrayElements=ri19100;
    t.ReleaseFloatArrayElements=rf19100;t.ExceptionCheck=exception19100;t.SetIntArrayRegion=put19100;
    JNIEnv env=&t;Array19100 in={width*height,(void*)input},out={width*height,output},m={115,(void*)model},p={width*height*2,(void*)policy};
    colour_enabled1976=1;
    return Java_com_hiro_ulike_SingleNoise1955_processNative(&env,NULL,(jintArray)&in,(jintArray)&out,
        width,height,0,height,0,height,0,4,JNI_TRUE,width,height,3,3,13,13,(jfloatArray)&m,(jintArray)&p);
}
