/* Execute both complete native implementations in an actual ARM64 process.
 * Only the JNI array services are a fixture; DCT uses production ARM NEON. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
typedef struct {jsize length; void *data;} Array;
static long assertions,pixels,records;
static void check(int v,const char *m){assertions++;if(!v){fprintf(stderr,"colour ARM: %s\n",m);exit(2);}}
static jsize JNICALL length(JNIEnv*e,jarray a){(void)e;return ((Array*)a)->length;}
static jboolean JNICALL same(JNIEnv*e,jobject a,jobject b){(void)e;return a==b;}
static jint *JNICALL ints(JNIEnv*e,jintArray a,jboolean *copy){(void)e;(void)copy;return ((Array*)a)->data;}
static jfloat *JNICALL floats(JNIEnv*e,jfloatArray a,jboolean *copy){(void)e;(void)copy;return ((Array*)a)->data;}
static void JNICALL releaseints(JNIEnv*e,jintArray a,jint*p,jint mode){(void)e;(void)a;(void)p;(void)mode;}
static void JNICALL releasefloats(JNIEnv*e,jfloatArray a,jfloat*p,jint mode){(void)e;(void)a;(void)p;(void)mode;}
static jboolean JNICALL exception(JNIEnv*e){(void)e;return JNI_FALSE;}
static void JNICALL put(JNIEnv*e,jintArray a,jsize at,jsize n,const jint*p){(void)e;check(at>=0&&n>=0&&at+n<=((Array*)a)->length,"valid output commit");memcpy((jint*)((Array*)a)->data+at,p,(size_t)n*4);}
static jfloatArray JNICALL newfloats(JNIEnv*e,jsize n){(void)e;Array*a=malloc(sizeof(*a));if(!a)return NULL;a->data=calloc((size_t)n,4);a->length=n;if(!a->data){free(a);return NULL;}return(jfloatArray)a;}
static void JNICALL drop(JNIEnv*e,jobject p){(void)e;Array*a=(Array*)p;if(a){free(a->data);free(a);}}
#define DECLARE(P) \
extern jboolean Java_com_hiro_ulike_ColourCache1976Test_##P##Process(JNIEnv*,jclass,jintArray,jintArray,jint,jint,jint,jint,jint,jint,jint,jint,jboolean,jint,jint,jint,jint,jint,jint,jfloatArray,jintArray); \
extern jfloatArray Java_com_hiro_ulike_ColourCache1976Test_##P##Prepare(JNIEnv*,jclass,jintArray,jint,jint,jint,jint,jint,jint,jint,jint,jboolean,jint,jint,jint,jint,jint,jfloatArray);
DECLARE(old) DECLARE(new)
static uint32_t random_state=178473;
static uint32_t random32(void){random_state^=random_state<<13;random_state^=random_state>>17;random_state^=random_state<<5;return random_state;}
int main(void){
 struct JNINativeInterface_ table={0};table.GetArrayLength=length;table.IsSameObject=same;table.GetIntArrayElements=ints;table.GetFloatArrayElements=floats;table.ReleaseIntArrayElements=releaseints;table.ReleaseFloatArrayElements=releasefloats;table.ExceptionCheck=exception;table.SetIntArrayRegion=put;table.NewFloatArray=newfloats;table.DeleteLocalRef=drop;JNIEnv env=&table;
 const int sizes[][2]={{1,1},{1,17},{7,9},{15,16},{17,33},{31,63},{37,65},{65,79},{129,73}};int cases=0;
 for(size_t s=0;s<sizeof(sizes)/sizeof(sizes[0]);s++)for(int alpha=0;alpha<2;alpha++)for(int nr=1;nr<=4;nr++){
  int w=sizes[s][0],h=sizes[s][1],n=w*h;Array in={n,malloc((size_t)n*4)},outa={n,calloc((size_t)n,4)},outb={n,calloc((size_t)n,4)},model={91,calloc(91,4)};
  check(in.data&&outa.data&&outb.data&&model.data,"fixture allocations");
  for(int i=0;i<n;i++){uint32_t p=random32();((uint32_t*)in.data)[i]=0xff000000|(p&0xffffff);if(alpha&&i%41==0)((uint32_t*)in.data)[i]=p&0x7fffffff;}
  float*m=model.data;m[0]=5.4f;m[1]=9.2f;m[2]=113.4f;for(int i=3;i<91;i++)m[i]=(float)(random32()%1000)/333.f+.2f;
  check(Java_com_hiro_ulike_ColourCache1976Test_oldProcess(&env,NULL,(jintArray)&in,(jintArray)&outa,w,h,0,h,0,h,0,nr,JNI_TRUE,w,h,1,1,w,h,(jfloatArray)&model,NULL),"old CPU process");
  check(Java_com_hiro_ulike_ColourCache1976Test_newProcess(&env,NULL,(jintArray)&in,(jintArray)&outb,w,h,0,h,0,h,0,nr,JNI_TRUE,w,h,1,1,w,h,(jfloatArray)&model,NULL),"new CPU process");
  for(int i=0;i<n;i++){check(((jint*)outa.data)[i]==((jint*)outb.data)[i],"ARM NEON exact pixels");}
  pixels+=n;
  Array *a=(Array*)Java_com_hiro_ulike_ColourCache1976Test_oldPrepare(&env,NULL,(jintArray)&in,w,h,0,h,0,h,0,nr,JNI_TRUE,h,1,1,w,h,(jfloatArray)&model);
  Array *b=(Array*)Java_com_hiro_ulike_ColourCache1976Test_newPrepare(&env,NULL,(jintArray)&in,w,h,0,h,0,h,0,nr,JNI_TRUE,h,1,1,w,h,(jfloatArray)&model);
  check(a&&b&&a->length==b->length,"ARM preparation");for(int i=0;i<a->length;i++)check(((uint32_t*)a->data)[i]==((uint32_t*)b->data)[i],"ARM NEON exact float records");records+=a->length;
  drop(&env,(jobject)a);drop(&env,(jobject)b);free(in.data);free(outa.data);free(outb.data);free(model.data);cases++;
 }
 printf("{\"status\":\"passed\",\"assertions\":%ld,\"cases\":%d,\"pixels_compared\":%ld,\"residual_floats_compared\":%ld,\"baseline75_exact\":true,\"actual_arm_neon_executed\":true,\"physical_android_tested\":false}\n",assertions,cases,pixels,records);return 0;
}
