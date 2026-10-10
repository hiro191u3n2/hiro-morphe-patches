/* Inject the optional copied-output JNI behaviour in the actual production
 * entrypoint. Another worker writes outside the requested interval immediately
 * after the VM creates the copy. JNI_ABORT must preserve that worker's rows. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
typedef struct {jsize length;jint *data;} HostArray;
static HostArray *destination;
static jint *copied;
static int copied_released,release_mode,output_written;
static jsize JNICALL length(JNIEnv *e,jarray a){(void)e;return ((HostArray*)a)->length;}
static jboolean JNICALL same(JNIEnv *e,jobject a,jobject b){(void)e;return a==b;}
static void * JNICALL critical(JNIEnv *e,jarray a,jboolean *is_copy){
    (void)e;HostArray *v=(HostArray*)a;
    if(v==destination){
        copied=malloc((size_t)v->length*sizeof(jint));if(!copied)abort();
        memcpy(copied,v->data,(size_t)v->length*sizeof(jint));
        if(!is_copy){fprintf(stderr,"output copy detection not requested\n");abort();}
        *is_copy=JNI_TRUE;
        /* Other worker completed its own row concurrently with this operation. */
        for(int i=21;i<28;i++)v->data[i]=(jint)0xffa5c731u;
        return copied;
    }
    if(is_copy)*is_copy=JNI_FALSE;
    return v->data;
}
static void JNICALL release(JNIEnv *e,jarray a,void *p,jint mode){
    (void)e;HostArray *v=(HostArray*)a;
    if(v==destination){
        copied_released++;release_mode=mode;
        for(int i=7;i<14;i++)if(((jint*)p)[i]!=(jint)0x13579bdfu)output_written++;
        if(mode!=JNI_ABORT)memcpy(v->data,p,(size_t)v->length*sizeof(jint));
        free(copied);copied=0;
    }
}
extern jboolean Java_com_hiro_ulike_CorePixels1950_bilateralNative(JNIEnv*,jclass,jintArray,jintArray,jintArray,jint,jint,jint,jboolean,jboolean,jboolean,jint,jint,jintArray,jintArray,jintArray,jintArray);
int main(void){
    jint g[28],v[28],o[28],lr[1280],cc[1280],decoded[28],vy[28];
    for(int i=0;i<28;i++){g[i]=(jint)0xff333129u;v[i]=(jint)0xff34322au;o[i]=(jint)0x13579bdfu;decoded[i]=17;vy[i]=19;}
    for(int i=0;i<1280;i++)lr[i]=cc[i]=256;
    HostArray a[7]={{28,g},{28,v},{28,o},{1280,lr},{1280,cc},{28,decoded},{28,vy}};destination=&a[2];
    struct JNINativeInterface_ table={0};table.GetArrayLength=length;table.IsSameObject=same;table.GetPrimitiveArrayCritical=critical;table.ReleasePrimitiveArrayCritical=release;
    JNIEnv env=&table;
    jboolean ok=Java_com_hiro_ulike_CorePixels1950_bilateralNative(&env,0,(jintArray)&a[0],(jintArray)&a[1],(jintArray)&a[2],7,4,2,JNI_FALSE,JNI_TRUE,JNI_TRUE,1,2,(jintArray)&a[3],(jintArray)&a[4],(jintArray)&a[5],(jintArray)&a[6]);
    if(ok || copied_released!=1 || release_mode!=JNI_ABORT || output_written){fprintf(stderr,"copied output not rejected before writes\n");return 1;}
    for(int i=0;i<28;i++)if(o[i]!=(i>=21?(jint)0xffa5c731u:(jint)0x13579bdfu)){fprintf(stderr,"other worker row overwritten at %d\n",i);return 1;}
    puts("{\"status\":\"passed\",\"assertions\":32,\"copied_output_refused_before_writes\":true,\"concurrent_disjoint_rows_preserved\":true,\"production_c_jni_executed\":true}");return 0;
}
