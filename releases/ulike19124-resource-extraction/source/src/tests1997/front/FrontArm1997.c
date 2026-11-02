/* The unchanged production JNI kernel, executed as actual AArch64 code.
 * Compare old 256-row scheduling with every newly selectable 16-row boundary.
 * JNI array services are fixtures; all DCT/NEON and pixel arithmetic is real. */
#include "../../native1955/single_noise1955.c"
#include <stdio.h>
#if !defined(__aarch64__) || !defined(__ARM_NEON)
#error This differential must execute actual AArch64 NEON
#endif
typedef struct {jsize length;void *data;} Array1997;
static unsigned long checks1997,pixels1997,tiles1997;
static void check1997(int ok,const char *why){checks1997++;if(!ok){fprintf(stderr,"front ARM: %s\n",why);exit(2);}}
static jsize JNICALL length1997(JNIEnv*e,jarray a){(void)e;return ((Array1997*)a)->length;}
static jboolean JNICALL same1997(JNIEnv*e,jobject a,jobject b){(void)e;return a==b;}
static jint *JNICALL ints1997(JNIEnv*e,jintArray a,jboolean *copy){(void)e;(void)copy;return ((Array1997*)a)->data;}
static jfloat *JNICALL floats1997(JNIEnv*e,jfloatArray a,jboolean *copy){(void)e;(void)copy;return ((Array1997*)a)->data;}
static void JNICALL releaseints1997(JNIEnv*e,jintArray a,jint*p,jint mode){(void)e;(void)a;(void)p;(void)mode;}
static void JNICALL releasefloats1997(JNIEnv*e,jfloatArray a,jfloat*p,jint mode){(void)e;(void)a;(void)p;(void)mode;}
static jboolean JNICALL exception1997(JNIEnv*e){(void)e;return JNI_FALSE;}
static void JNICALL put1997(JNIEnv*e,jintArray a,jsize at,jsize n,const jint*p){(void)e;Array1997*v=(Array1997*)a;check1997(at>=0&&n>=0&&at+n<=v->length,"bounded JNI output commit");memcpy((jint*)v->data+at,p,(size_t)n*4);}
static uint32_t state1997=1997143;
static uint32_t random1997(void){state1997^=state1997<<13;state1997^=state1997>>17;state1997^=state1997<<5;return state1997;}
static void run1997(JNIEnv *env,const jint *input,jint *result,int width,int height,int core,
        int nr,int shadows,int colour,Array1997 *model,const jint *policy){
    for(int first=0;first<height;first+=core){
        int finish=mini(height,first+core),top=maxi(0,first-HALO),bottom=mini(height,finish+HALO);
        int rows=bottom-top,begin=first-top,end=finish-top,count=width*rows;
        jint *output=malloc((size_t)count*4);check1997(output!=NULL,"output allocation");
        for(int i=0;i<count;i++)output[i]=0x13579bdf;
        Array1997 in={count,(void*)(input+width*top)},out={count,output};
        Array1997 protection={(finish-first)*width*2,(void*)(policy+width*first*2)};
        colour_enabled1976=colour;
        check1997(Java_com_hiro_ulike_SingleNoise1955_processNative(env,NULL,
            (jintArray)&in,(jintArray)&out,width,rows,begin,end,0,rows,top,nr,shadows,
            width,height,1,1,width,height,(jfloatArray)model,(jintArray)&protection),"production JNI accepts exact halo and absolute origin");
        colour_enabled1976=0;
        for(int i=0;i<count;i++)if(i<begin*width||i>=end*width)check1997(output[i]==0x13579bdf,"halo output remains unmodified");
        memcpy(result+first*width,output+begin*width,(size_t)(finish-first)*width*4);
        free(output);tiles1997++;
    }
}
int main(void){
    struct JNINativeInterface_ table={0};table.GetArrayLength=length1997;table.IsSameObject=same1997;
    table.GetIntArrayElements=ints1997;table.GetFloatArrayElements=floats1997;
    table.ReleaseIntArrayElements=releaseints1997;table.ReleaseFloatArrayElements=releasefloats1997;
    table.ExceptionCheck=exception1997;table.SetIntArrayRegion=put1997;JNIEnv env=&table;
    int cases=0;
    for(int scene=0;scene<4;scene++){
        int width=scene==3?33:17,height=scene==3?259:513,n=width*height;
        jint *input=malloc((size_t)n*4),*copy=malloc((size_t)n*4),*gold=malloc((size_t)n*4),*actual=malloc((size_t)n*4),*policy=malloc((size_t)n*8);
        check1997(input&&copy&&gold&&actual&&policy,"fixture buffers");
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int i=y*width+x;uint32_t r=random1997();int v=80+(int)(r%57),alpha=scene==1&&i%41==0?0x70:0xff;
            input[i]=(jint)((uint32_t)alpha<<24|(uint32_t)(scene==2?(x<width/2?60:180):v)<<16|(uint32_t)v<<8|(uint32_t)(scene==3?(int)(r%256):v));
            policy[2*i]=i%29==0?0:(int)(r%257);policy[2*i+1]=(int)((r>>16)%257);
        }
        memcpy(copy,input,(size_t)n*4);float values[91];values[0]=5.4f;values[1]=9.2f;values[2]=113.4f;
        for(int i=3;i<91;i++)values[i]=(float)(random1997()%1000)/333.f+.2f;
        Array1997 model={91,values};
        for(int colour=0;colour<2;colour++){
            run1997(&env,input,gold,width,height,256,scene+1,scene%2,colour,&model,policy);
            for(int core=16;core<=256;core+=16){
                run1997(&env,input,actual,width,height,core,scene+1,scene%2,colour,&model,policy);
                for(int i=0;i<n;i++){check1997(gold[i]==actual[i],"all output ARGB bits equal to old 256-row split");pixels1997++;}
                check1997(memcmp(input,copy,(size_t)n*4)==0,"original source remains immutable");cases++;
            }
        }
        free(input);free(copy);free(gold);free(actual);free(policy);
    }
    printf("{\"status\":\"passed\",\"assertions\":%lu,\"pixel_cases\":%d,\"compared_pixels\":%lu,\"jni_strip_calls\":%lu,\"actual_arm_neon_executed\":true,\"all_16_row_boundaries_bitwise_equal\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}\n",checks1997,cases,pixels1997,tiles1997);
    return 0;
}
