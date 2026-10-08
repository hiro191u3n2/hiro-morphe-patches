#ifndef MOIRE1951_HOST
#include <jni.h>
#endif
#include <stdint.h>
#include <limits.h>

#ifndef MOIRE1951_HOST
#define JNI_FN(n) Java_com_hiro_ulike_NativeMoire1951_##n
#endif
static inline int mx(int a,int b){return a>b?a:b;}
static inline int mn(int a,int b){return a<b?a:b;}
static inline int ab(int a){return a<0?-a:a;}
static inline int clamp(int a,int lo,int hi){return mn(hi,mx(lo,a));}
static inline int r(uint32_t p){return (p>>16)&255;}
static inline int g(uint32_t p){return (p>>8)&255;}
static inline int b(uint32_t p){return p&255;}
static inline int yy(uint32_t p){return (77*r(p)+150*g(p)+29*b(p)+128)>>8;}
static inline int u(uint32_t p){return b(p)-g(p);}
static inline int v(uint32_t p){return r(p)-g(p);}
static inline int rounddiv(int n,int d){return n>=0?(n+d/2)/d:-((-n+d/2)/d);}
static inline int distance(int au,int av,int bu,int bv){return mx(ab(au-bu),ab(av-bv));}

/* Literal integer translation of QualityPixels1932.removePeriodicChroma.
 * The tests/branch order, negative / rounding and best-direction ties match Java. */
static uint32_t periodic(const uint32_t *src,int at,int width,uint32_t center){
    int cu=u(center),cv=v(center),l=yy(center);
    if(mx(ab(cu),ab(cv))>104)return center;
    int best=0,bu=cu,bv=cv;
    for(int direction=0;direction<4;direction++){
        int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
        uint32_t m1=src[at-step],p1=src[at+step];
        if((m1>>24)!=255 || (p1>>24)!=255)continue;
        uint32_t m2=src[at-2*step],p2=src[at+2*step];
        if((m2>>24)!=255 || (p2>>24)!=255)continue;
        int m1u=u(m1),p1u=u(p1),m1v=v(m1),p1v=v(p1);
        int m2u=u(m2),p2u=u(p2),m2v=v(m2),p2v=v(p2);
        int amp1=mx(ab(2*cu-m1u-p1u),ab(2*cv-m1v-p1v))/2;
        int amp2=mx(ab(2*cu-m2u-p2u),ab(2*cv-m2v-p2v))/2;
        if(amp1<8 && amp2<8)continue;
        uint32_t m4=src[at-4*step],p4=src[at+4*step];
        if((m4>>24)!=255 || (p4>>24)!=255)continue;
        int y1m=yy(m1),y1p=yy(p1),y2m=yy(m2),y2p=yy(p2);
        for(int period=2;period<=4;period+=2){
            int amp=period==2?amp1:amp2;
            if(amp<8 || amp>112)continue;
            int repeat=period==2?
                mx(ab(cu-m2u)+ab(cu-p2u),ab(cv-m2v)+ab(cv-p2v))/2:
                mx(ab(cu-u(m4))+ab(cu-u(p4)),ab(cv-v(m4))+ab(cv-v(p4)))/2;
            if(repeat>2+amp/5)continue;
            int opposition=period==2?mx(ab(m1u-p1u),ab(m1v-p1v)):
                mx(ab(m2u-p2u),ab(m2v-p2v));
            if(opposition>3+amp/4)continue;
            int lumaPattern=period==2?ab(2*l-y1m-y1p)/2:ab(2*l-y2m-y2p)/2;
            if(lumaPattern>3+amp/5)continue;
            int meanU=period==2?(2*cu+m1u+p1u)/4:
                (m2u+p2u+2*(m1u+cu+p1u))/8;
            int meanV=period==2?(2*cv+m1v+p1v)/4:
                (m2v+p2v+2*(m1v+cv+p1v))/8;
            if(mx(ab(meanU),ab(meanV))>76)continue;
            int amount=clamp((amp-6)*12,0,216);
            amount=amount*(amp+2-mn(amp,repeat*2))/(amp+2);
            amount=amount*(amp+4-mn(amp,lumaPattern*3))/(amp+4);
            if(amount>best){best=amount;bu=meanU;bv=meanV;}
        }
    }
    if(best<=0)return center;
    int ou=cu+rounddiv((bu-cu)*best,256),ov=cv+rounddiv((bv-cv)*best,256);
    int green=rounddiv(256*l-77*ov-29*ou,256),red=green+ov,blue=green+ou;
    if(red<0 || red>255 || green<0 || green>255 || blue<0 || blue>255)return center;
    return (center&0xff000000u)|((uint32_t)red<<16)|((uint32_t)green<<8)|(uint32_t)blue;
}

/* Literal integer translation of LongMoire1934.correct. */
static uint32_t long_wave(const uint32_t *src,int at,int width,uint32_t center){
    static const int periods[]={6,8,12,16};
    int cu=u(center),cv=v(center),cy=yy(center);
    if(mx(ab(cu),ab(cv))>90)return center;
    int best=0,bu=cu,bv=cv;
    for(int direction=0;direction<4;direction++){
        int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
        uint32_t cm=src[at-step],cp=src[at+step];
        if((cm>>24)!=255 || (cp>>24)!=255)continue;
        if(distance(u(cm),v(cm),u(cp),v(cp))>46)continue;
        for(int k=0;k<4;k++){
            int period=periods[k],half=period/2;
            uint32_t m=src[at-half*step],p=src[at+half*step];
            if((m>>24)!=255 || (p>>24)!=255)continue;
            int mu=u(m),mv=v(m),pu=u(p),pv=v(p);
            int amp=distance(cu*2,cv*2,mu+pu,mv+pv)/2;
            if(amp<10 || amp>96)continue;
            int opposition=distance(mu,mv,pu,pv);
            if(opposition>3+amp/6)continue;
            int lumaPattern=mx(ab(cy-yy(m)),ab(cy-yy(p)));
            if(lumaPattern>2+amp/10)continue;
            int repeat=0,valid=1;
            for(int sign=-1;sign<=1;sign+=2)for(int cycle=1;cycle<=2;cycle++){
                uint32_t q=src[at+sign*period*cycle*step];
                if((q>>24)!=255){valid=0;break;}
                repeat=mx(repeat,distance(cu,cv,u(q),v(q)));
                if(ab(cy-yy(q))>3+amp/10)valid=0;
            }
            if(!valid || repeat>2+amp/6)continue;
            int quarter=mx(1,period/4);
            uint32_t qm=src[at-quarter*step],qp=src[at+quarter*step];
            if((qm>>24)!=255 || (qp>>24)!=255)continue;
            int meanU=(2*cu+mu+pu)/4,meanV=(2*cv+mv+pv)/4;
            if(mx(ab(meanU),ab(meanV))>36)continue;
            int midpointError=distance(u(qm)+u(qp),v(qm)+v(qp),meanU*2,meanV*2)/2;
            if(midpointError>4+amp/4)continue;
            int nearChange=distance(u(cm)+u(cp),v(cm)+v(cp),cu*2,cv*2)/2;
            if(nearChange<1 || nearChange>3+amp*14/(period*period))continue;
            int lumaRange=mx(mx(yy(qm),yy(qp)),cy)-mn(mn(yy(qm),yy(qp)),cy);
            if(lumaRange>3+amp/10)continue;
            int amount=mn(176,(amp-7)*8);
            amount=amount*(amp+2-repeat)/(amp+2);
            if(amount>best){best=amount;bu=meanU;bv=meanV;}
        }
    }
    if(best==0)return center;
    int ou=cu+rounddiv((bu-cu)*best,256),ov=cv+rounddiv((bv-cv)*best,256);
    int green=rounddiv(256*cy-77*ov-29*ou,256),red=green+ov,blue=green+ou;
    if(red<0 || red>255 || green<0 || green>255 || blue<0 || blue>255)return center;
    return (center&0xff000000u)|((uint32_t)red<<16)|((uint32_t)green<<8)|(uint32_t)blue;
}

static void run_rows(const uint32_t *src,uint32_t *dst,int width,int rows,int first,int last) {
    for(int row=first;row<last;row++)for(int x=0;x<width;x++){
        int at=row*width+x;
        uint32_t original=src[at],p=original;
        if((p>>24)==255 && row>=4 && row<rows-4 && x>=4 && x<width-4){
            p=periodic(src,at,width,p);
            if(p==original && row>=32 && row<rows-32 && x>=32 && x<width-32)
                p=long_wave(src,at,width,p);
        }
        dst[at]=p;
    }
}
#ifdef MOIRE1951_HOST
void moire1951_host(const uint32_t *src,uint32_t *dst,int width,int rows,int first,int last) {
    run_rows(src,dst,width,rows,first,last);
}
#else
JNIEXPORT jint JNICALL JNI_FN(nativeAbi)(JNIEnv *env,jclass cls){
    (void)env;(void)cls;return 1951;
}
JNIEXPORT jboolean JNICALL JNI_FN(moireStripNative)(JNIEnv *env,jclass cls,
    jintArray source,jintArray target,jint width,jint rows,jint first,jint last){
    (void)cls;
    if(!source || !target || (*env)->IsSameObject(env,source,target) ||
        width<=0 || rows<=0 || first<0 || last<first || last>rows ||
        (int64_t)width*rows>INT_MAX)return JNI_FALSE;
    int count=width*rows;
    if((*env)->GetArrayLength(env,source)<count ||
       (*env)->GetArrayLength(env,target)<count)return JNI_FALSE;
    const jint *src=(*env)->GetPrimitiveArrayCritical(env,source,0);
    if(!src)return JNI_FALSE;
    jboolean output_is_copy=JNI_FALSE;
    jint *dst=(*env)->GetPrimitiveArrayCritical(env,target,&output_is_copy);
    if(!dst || output_is_copy) {
        if(dst)(*env)->ReleasePrimitiveArrayCritical(env,target,dst,JNI_ABORT);
        (*env)->ReleasePrimitiveArrayCritical(env,source,(void*)src,JNI_ABORT);
        return JNI_FALSE;
    }
    /* Pinned arrays are held for at most 16 rows. No JNI operations, allocations,
     * callbacks or blocking occur while critical arrays are held. */
    run_rows((const uint32_t*)src,(uint32_t*)dst,width,rows,first,last);
    (*env)->ReleasePrimitiveArrayCritical(env,target,dst,0);
    (*env)->ReleasePrimitiveArrayCritical(env,source,(void*)src,JNI_ABORT);
    return JNI_TRUE;
}
#endif
