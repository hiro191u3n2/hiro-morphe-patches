#define _POSIX_C_SOURCE 200809L
#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
typedef struct {jsize length;jint *data;} HostArray;
static jsize JNICALL array_length(JNIEnv *e,jarray a){(void)e;return ((HostArray*)a)->length;}
static jboolean JNICALL same(JNIEnv *e,jobject a,jobject b){(void)e;return a==b;}
static void *JNICALL critical(JNIEnv *e,jarray a,jboolean *copy){(void)e;if(copy)*copy=JNI_FALSE;return ((HostArray*)a)->data;}
static void JNICALL release(JNIEnv *e,jarray a,void *p,jint mode){(void)e;(void)a;(void)p;(void)mode;}
extern jboolean Java_com_hiro_ulike_CorePixels1950_bilateralNative(JNIEnv*,jclass,jintArray,jintArray,jintArray,jint,jint,jint,jboolean,jboolean,jboolean,jint,jint,jintArray,jintArray,jintArray,jintArray);
extern jint Java_com_hiro_ulike_CorePixels1950_nativeAbi(JNIEnv*,jclass);
extern jboolean Java_com_hiro_ulike_Baseline1950_bilateralNative(JNIEnv*,jclass,jintArray,jintArray,jintArray,jint,jint,jint,jboolean,jboolean,jboolean,jint,jint,jintArray,jintArray,jintArray,jintArray);
static uint32_t state=19500731;
static uint32_t rnd(void){state^=state<<13;state^=state>>17;state^=state<<5;return state;}
static double now(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return t.tv_sec+t.tv_nsec*1e-9;}
static jboolean invoke(int optimized,JNIEnv*env,HostArray *a,int width,int rows,int noise,int horizontal,int texture,int shadows,int first,int last){
    return (optimized?Java_com_hiro_ulike_CorePixels1950_bilateralNative:Java_com_hiro_ulike_Baseline1950_bilateralNative)(env,0,(jintArray)&a[0],(jintArray)&a[1],(jintArray)&a[2],width,rows,noise,horizontal,texture,shadows,first,last,(jintArray)&a[3],(jintArray)&a[4],(jintArray)&a[5],(jintArray)&a[6]);
}
static int run_case(JNIEnv *env,int width,int rows,int pattern,int *compared,double *baseline_time,double *optimized_time){
    int n=width*rows,halo=width*(rows<70?rows:70),sigma_l[5]={1,12,18,26,34},sigma_c[5]={1,18,28,38,48};
    jint *g=calloc(n,sizeof(jint)),*v=calloc(n,sizeof(jint)),*base=calloc(n,sizeof(jint)),*fast=calloc(n,sizeof(jint));
    jint *lr=calloc(1280,sizeof(jint)),*cc=calloc(1280,sizeof(jint)),*bd=calloc(halo,sizeof(jint)),*bv=calloc(halo,sizeof(jint)),*fd=calloc(halo,sizeof(jint)),*fv=calloc(halo,sizeof(jint));
    if(!g||!v||!base||!fast||!lr||!cc||!bd||!bv||!fd||!fv){fputs("allocation failed\n",stderr);return 0;}
    for(int level=1;level<=4;level++)for(int i=0;i<256;i++){
        lr[level*256+i]=(jint)floor(exp(-((double)i*i)/(2*sigma_l[level]*sigma_l[level]))*256.0+0.5);
        cc[level*256+i]=(jint)floor(exp(-((double)i*i)/(2*sigma_c[level]*sigma_c[level]))*256.0+0.5);
    }
    for(int i=0;i<n;i++){
        int x=i%width,y=i/width,r=(rnd()&255),b=(rnd()&255),z=(rnd()&255);
        if(pattern==0){r=(x*17+y*2)&255;b=(x*7+y*13)&255;z=(x*3+y*5)&255;}
        if(pattern==1){r=35+((int)(rnd()%19)-9);b=31+((int)(rnd()%19)-9);z=29+((int)(rnd()%19)-9);}
        if(pattern==2){r=205+((int)(rnd()%19)-9);b=199+((int)(rnd()%19)-9);z=195+((int)(rnd()%19)-9);}
        g[i]=(jint)(0xff000000u | (uint32_t)r<<16 | (uint32_t)z<<8 | (uint32_t)b);
        if(pattern==3 && (i%17==0))g[i]=(jint)((uint32_t)g[i]&0x7fffffffu);
        v[i]=(jint)(0xff000000u | (rnd()&0xffffff));
    }
    if(pattern==0){for(int i=0;i<n;i++)v[i]=g[i];}
    HostArray a[7]={{n,g},{n,v},{n,base},{1280,lr},{1280,cc},{halo,bd},{halo,bv}};
    for(int level=1;level<=4;level++)for(int flags=0;flags<8;flags++)for(int tile=0;tile<3;tile++){
        int begin=tile==0?0:tile==1?rows/3:rows>64?rows-64:0;
        int end=tile==0?rows:tile==1?(begin+7<rows?begin+7:rows):rows;
        /* The Java wrapper caps each JNI call to 64 output rows. */
        for(int first=begin;first<end;first+=64){int last=first+64<end?first+64:end;
            for(int i=0;i<n;i++)base[i]=fast[i]=(jint)0x13579bdf;
            double start=now();int good=invoke(0,env,a,width,rows,level,flags&1,flags&2,flags&4,first,last);*baseline_time+=now()-start;
            a[2].data=fast;a[5].data=fd;a[6].data=fv;
            start=now();good&=invoke(1,env,a,width,rows,level,flags&1,flags&2,flags&4,first,last);*optimized_time+=now()-start;
            a[2].data=base;a[5].data=bd;a[6].data=bv;
            if(!good){fprintf(stderr,"JNI failure width=%d rows=%d level=%d flags=%d first=%d last=%d\n",width,rows,level,flags,first,last);return 0;}
            for(int i=0;i<n;i++)if(base[i]!=fast[i]){fprintf(stderr,"pixel mismatch width=%d rows=%d pattern=%d level=%d flags=%d first=%d last=%d i=%d baseline=%08x optimized=%08x\n",width,rows,pattern,level,flags,first,last,i,(unsigned)base[i],(unsigned)fast[i]);return 0;}
            *compared+=n;
        }
    }
    free(g);free(v);free(base);free(fast);free(lr);free(cc);free(bd);free(bv);free(fd);free(fv);return 1;
}
int main(void){
    struct JNINativeInterface_ functions={array_length,same,critical,release};JNIEnv env=&functions;
    if(Java_com_hiro_ulike_CorePixels1950_nativeAbi(&env,0)!=1950)return 1;
    const int dimensions[][2]={{1,1},{2,7},{7,2},{17,13},{31,23},{7,69},{257,131},{512,256}};
    int compared=0;double baseline=0,optimized=0;
    for(unsigned i=0;i<sizeof(dimensions)/sizeof(*dimensions);i++)for(int pattern=0;pattern<4;pattern++)
        if(!run_case(&env,dimensions[i][0],dimensions[i][1],pattern,&compared,&baseline,&optimized))return 1;
    printf("{\"status\":\"passed\",\"compared_array_elements\":%d,\"native_jni_baseline_seconds\":%.5f,\"native_jni_h7_seconds\":%.5f}\n",compared,baseline,optimized);
    return 0;
}
