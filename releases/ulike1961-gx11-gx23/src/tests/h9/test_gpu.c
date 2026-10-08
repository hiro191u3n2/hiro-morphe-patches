#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
typedef struct {jsize length;jint *data;} Array;
static jsize get_length(JNIEnv *env,jarray value){(void)env;return ((Array *)value)->length;}
static jboolean same(JNIEnv *env,jobject a,jobject b){(void)env;return a==b;}
static void get_int(JNIEnv *env,jintArray value,jsize from,jsize n,jint *target){
    (void)env;memcpy(target,((Array *)value)->data+from,(size_t)n*sizeof(jint));
}
static void set_int(JNIEnv *env,jintArray value,jsize from,jsize n,const jint *source){
    (void)env;memcpy(((Array *)value)->data+from,source,(size_t)n*sizeof(jint));
}
static void get_byte(JNIEnv *env,jbyteArray value,jsize from,jsize n,jbyte *target){
    (void)env;memcpy(target,(jbyte *)((Array *)value)->data+from,(size_t)n);
}
static jboolean exception(JNIEnv *env){(void)env;return JNI_FALSE;}
static void *critical(JNIEnv *env,jarray value,jboolean *copy){
    (void)env;(void)copy;return ((Array *)value)->data;
}
static void release(JNIEnv *env,jarray value,void *data,jint mode){
    (void)env;(void)value;(void)data;(void)mode;
}
static const struct JNINativeInterface_ calls={get_length,same,get_int,set_int,get_byte,
    exception,critical,release};
extern jboolean Java_com_hiro_ulike_GpuInteger1949_finishIntoNative(JNIEnv *,jclass,
    jintArray,jintArray,jintArray,jintArray,jint,jint,jint,jint,jint,jint,jint,jint,
    jintArray,jint,jboolean,jint,jint,jint);
extern jboolean Java_com_hiro_ulike_GpuInteger1949_finishSavedIntoNative(JNIEnv *,jclass,
    jintArray,jintArray,jintArray,jintArray,jint,jint,jint,jint,jint,jint,jint,jint,
    jintArray,jint,jboolean,jint,jint,jint);
static uint32_t seed=0x19511949u;
static uint32_t random32(void){seed^=seed<<13;seed^=seed>>17;seed^=seed<<5;return seed;}
static int run_shape(JNIEnv *env,int w,int h,int begin,int end,int radius,int noise){
    size_t all=(size_t)w*h,n=(size_t)w*(end-begin);
    jint *src=malloc(all*4),*out1=malloc(all*4),*out2=malloc(all*4),
        *meta=calloc(n,4),*policy=calloc(n*3,4),*packed=calloc(n,4),
        *range=calloc(33u*256u,4);
    if(!src||!out1||!out2||!meta||!policy||!packed||!range)return 0;
    for(int i=0;i<33;i++)for(int d=0;d<256;d++)
        range[i*256+d]=i==0?0:(int)llround(256.0*exp(-(double)d*d/(2.0*i*i)));
    for(size_t i=0;i<all;i++){
        uint32_t r=random32();src[i]=(jint)(((i%11==0)?0x87000000u:0xff000000u)|
            (((r>>16)&255u)<<16)|(((r>>8)&255u)<<8)|(r&255u));
    }
    memcpy(out1,src,all*4);memcpy(out2,src,all*4);
    for(size_t i=0;i<n;i++){
        uint32_t r=random32();int y=begin+(int)(i/w),col=(int)(i%w);
        if((src[y*w+col]>>24)==-1 && i%5!=0){
            int sigma=2+(int)(r%31),threshold=3+(int)((r>>8)%25),
                tolerance=3+(int)((r>>16)%25);
            int pixel=src[y*w+col],yc=(77*((pixel>>16)&255)+150*((pixel>>8)&255)+29*(pixel&255)+128)>>8;
            meta[i]=(jint)(0x80000000u|(uint32_t)sigma|((uint32_t)threshold<<6)|
                ((uint32_t)tolerance<<12)|((uint32_t)yc<<18));
            int budget=(int)((r>>1)%257),base=(int)((r>>10)%257),skin=(int)((r>>19)%257);
            policy[i*3]=budget;policy[i*3+1]=base;policy[i*3+2]=skin;
            packed[i]=budget|(base<<9)|(skin<<18);
        }
    }
    Array a={(jsize)all,src},b={(jsize)n,meta},c={(jsize)(n*3),policy},
        d={(jsize)n,packed},e={(jsize)all,out1},f={(jsize)all,out2},g={33*256,range};
    int lo=0,hi=h,offset=begin*w;
    int old=Java_com_hiro_ulike_GpuInteger1949_finishIntoNative(env,0,&a,&b,&c,&e,
        offset,w,h,begin,end,lo,hi,radius,&g,noise,JNI_TRUE,256,192,24);
    int newer=Java_com_hiro_ulike_GpuInteger1949_finishSavedIntoNative(env,0,&a,&b,&d,&f,
        offset,w,h,begin,end,lo,hi,radius,&g,noise,JNI_TRUE,256,192,24);
    int equal=old&&newer&&memcmp(out1,out2,all*4)==0;
    if(!equal)fprintf(stderr,"shape %dx%d tile %d..%d radius%d noise%d old%d new%d\n",
        w,h,begin,end,radius,noise,old,newer);
    free(src);free(out1);free(out2);free(meta);free(policy);free(packed);free(range);
    return equal;
}
int main(void){
    JNIEnv env=&calls;int cases=0;
    for(int radius=1;radius<=4;radius++)for(int noise=1;noise<=4;noise++){
        if(!run_shape(&env,17,19,0,19,radius,noise))return 1;
        cases++;
        if(!run_shape(&env,43,67,9,56,radius,noise))return 1;
        cases++;
    }
    printf("GPU saved-vs-general pixel equality passed: %d tile cases\n",cases);
    return 0;
}
