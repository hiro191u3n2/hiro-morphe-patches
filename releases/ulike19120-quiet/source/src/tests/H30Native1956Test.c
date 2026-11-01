#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "residual1944.h"
void residual1955_baseline(const int32_t*,const int32_t*,int32_t*,int,int,int,int,int,int,const int32_t*,int32_t*,const int32_t*);
extern uint64_t h30_taps,h30_skipped,h30_base_taps;
static uint32_t rng=1956;
static uint32_t next(void){rng=rng*1664525u+1013904223u;return rng;}
static void req(int ok,const char *why){if(!ok){fprintf(stderr,"%s\n",why);exit(1);}}
int main(void){
    uint64_t comparisons=0,protected_count=0,all_base=0,all_new=0,all_skipped=0;int cases=0;
    const int dims[][2]={{1,1},{7,9},{17,19},{33,31},{65,49}};
    int32_t ranges[33*256];for(int i=0;i<33*256;i++)ranges[i]=256/(1+(i%256)/4);
    for(int shape=0;shape<5;shape++)for(int radius=1;radius<=4;radius++)for(int mode=0;mode<7;mode++){
        int w=dims[shape][0],h=dims[shape][1],lo=h>12?1:0,hi=h>12?h-1:h;
        int begin=mode==6?lo+(hi-lo)/3:lo,end=mode==6?hi-(hi-lo)/4:hi,n=w*(end-begin),taps=radius==4?7:radius*2+1;
        int32_t *src=malloc(w*h*4),*meta=malloc(n*4),*marked=malloc(n*4),*a=malloc(n*12),*b=malloc(n*12),*seed=malloc(n*12),*ring=malloc(w*(radius*2+1)*4),*x=malloc(w*taps*4);
        req(src&&meta&&marked&&a&&b&&seed&&ring&&x,"allocation");
        for(int row=0;row<h;row++)for(int col=0;col<w;col++){
            int v=mode==1?((col+row)&1?38:68):mode==2?((col+row)&1?38:68)+(int)(next()%3):mode==3?(col<w/2?40:160):mode==4?127:40+(int)(next()%21);
            uint32_t alpha=mode==5?(col%4?255:128):255;src[row*w+col]=(int32_t)((alpha<<24)|(uint32_t)(v*0x010101));
        }
        for(int i=0;i<n;i++)meta[i]=(int32_t)((i%17?0x80000000u:0)|16u|(8u<<6)|(7u<<12));
        for(int i=0;i<n*3;i++)seed[i]=(int32_t)next();
        const int offsets[7]={-4,-2,-1,0,1,2,4};
        for(int col=0;col<w;col++)for(int t=0;t<taps;t++){int sx=col+(radius==4?offsets[t]:t-radius);x[col*taps+t]=sx<0?0:sx>=w?w-1:sx;}
        memcpy(a,seed,n*12);memcpy(b,seed,n*12);
        residual1955_baseline(src,meta,a,w,begin,end,lo,hi,radius,ranges,ring,x);
        residual1944_aggregate(src,meta,b,w,begin,end,lo,hi,radius,ranges,ring,x);
        req(!memcmp(a,b,n*12),"retained aggregate ABI changed");
        for(int i=0;i<n;i++)marked[i]=meta[i]|0x40000000;
        memcpy(a,seed,n*12);memcpy(b,seed,n*12);h30_taps=h30_skipped=h30_base_taps=0;
        residual1955_baseline(src,meta,a,w,begin,end,lo,hi,radius,ranges,ring,x);
        residual1944_aggregate(src,marked,b,w,begin,end,lo,hi,radius,ranges,ring,x);
        for(int i=0;i<n;i++){
            int skip=a[i*3+2]<0&&(((uint32_t)a[i*3+2]>>16)&511)==256;
            if(skip){req(b[i*3+2]==0,"protected summary not skipped");req(b[i*3]==seed[i*3]&&b[i*3+1]==seed[i*3+1],"skipped seed overwritten");protected_count++;}
            else req(!memcmp(a+i*3,b+i*3,12),"nonprotected summary mismatch");
            comparisons++;
        }
        all_base+=h30_base_taps;all_new+=h30_taps;all_skipped+=h30_skipped;cases++;
        free(src);free(meta);free(marked);free(a);free(b);free(seed);free(ring);free(x);
    }
    req(protected_count>0&&all_skipped>0&&all_new<all_base,"weighted native work not reduced");
    printf("{\"status\":\"passed\",\"cases\":%d,\"pixelSummaries\":%llu,\"protected\":%llu,\"baselineTapLanes\":%llu,\"candidateTapLanes\":%llu,\"skipped\":%llu,\"neon\":%s}\n",cases,(unsigned long long)comparisons,(unsigned long long)protected_count,(unsigned long long)all_base,(unsigned long long)all_new,(unsigned long long)all_skipped,
#ifdef __aarch64__
        "true"
#else
        "false"
#endif
    );return 0;
}
