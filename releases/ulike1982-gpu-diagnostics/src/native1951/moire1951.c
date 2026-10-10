#ifndef MOIRE1951_HOST
#include <jni.h>
#endif
#include <stdint.h>
#include <limits.h>
#include <string.h>

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

/* One Java-owned, bounded row band stores exact integer Y/U/V. Source RGB
 * remains immutable and alpha always comes from the original source. */
static inline int sy1976(const uint32_t *src,const uint32_t *cache,int base,int at) {
    return cache?(int)(cache[at-base]&255u):yy(src[at]);
}
static inline int su1976(const uint32_t *src,const uint32_t *cache,int base,int at) {
    return cache?(int)((cache[at-base]>>8)&511u)-255:u(src[at]);
}
static inline int sv1976(const uint32_t *src,const uint32_t *cache,int base,int at) {
    return cache?(int)((cache[at-base]>>17)&511u)-255:v(src[at]);
}
static const uint32_t *cache_band1976(const uint32_t *src,int32_t *scratch,
        int width,int rows,int first,int last,int radius,int *base) {
    int begin=first<radius?0:first-radius,end=last>rows-radius?rows:last+radius,count=end-begin;
    int old=scratch[0],oldcount=scratch[1],kept=0;
    uint32_t *cache=(uint32_t *)(scratch+2);
    if(old>=0 && old<=begin && oldcount>0 && oldcount<=80 &&
            old<=rows-oldcount && begin<old+oldcount) {
        kept=mn(count,old+oldcount-begin);
        memmove(cache,cache+(size_t)(begin-old)*width,(size_t)kept*width*sizeof(*cache));
    }
    for(int row=kept;row<count;row++)for(int x=0;x<width;x++) {
        uint32_t p=src[(begin+row)*width+x];
        cache[row*width+x]=(uint32_t)yy(p)|((uint32_t)(u(p)+255)<<8)|((uint32_t)(v(p)+255)<<17);
    }
    scratch[0]=begin;scratch[1]=count;*base=begin*width;return cache;
}

/* Literal integer translation of QualityPixels1932.removePeriodicChroma.
 * The tests/branch order, negative / rounding and best-direction ties match Java. */
static inline __attribute__((always_inline)) uint32_t periodic(const uint32_t *src,int at,int width,uint32_t center,
        const uint32_t *cache,int base){
    int cu=u(center),cv=v(center),l=yy(center);
    if(mx(ab(cu),ab(cv))>104)return center;
    int best=0,bu=cu,bv=cv;
    for(int direction=0;direction<4;direction++){
        int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
        uint32_t m1=src[at-step],p1=src[at+step];
        if((m1>>24)!=255 || (p1>>24)!=255)continue;
        uint32_t m2=src[at-2*step],p2=src[at+2*step];
        if((m2>>24)!=255 || (p2>>24)!=255)continue;
        int m1u=su1976(src,cache,base,at-step),p1u=su1976(src,cache,base,at+step),m1v=sv1976(src,cache,base,at-step),p1v=sv1976(src,cache,base,at+step);
        int m2u=su1976(src,cache,base,at-2*step),p2u=su1976(src,cache,base,at+2*step),m2v=sv1976(src,cache,base,at-2*step),p2v=sv1976(src,cache,base,at+2*step);
        int amp1=mx(ab(2*cu-m1u-p1u),ab(2*cv-m1v-p1v))/2;
        int amp2=mx(ab(2*cu-m2u-p2u),ab(2*cv-m2v-p2v))/2;
        if(amp1<8 && amp2<8)continue;
        uint32_t m4=src[at-4*step],p4=src[at+4*step];
        if((m4>>24)!=255 || (p4>>24)!=255)continue;
        int y1m=sy1976(src,cache,base,at-step),y1p=sy1976(src,cache,base,at+step),y2m=sy1976(src,cache,base,at-2*step),y2p=sy1976(src,cache,base,at+2*step);
        for(int period=2;period<=4;period+=2){
            int amp=period==2?amp1:amp2;
            if(amp<8 || amp>112)continue;
            int repeat=period==2?
                mx(ab(cu-m2u)+ab(cu-p2u),ab(cv-m2v)+ab(cv-p2v))/2:
                mx(ab(cu-su1976(src,cache,base,at-4*step))+ab(cu-su1976(src,cache,base,at+4*step)),ab(cv-sv1976(src,cache,base,at-4*step))+ab(cv-sv1976(src,cache,base,at+4*step)))/2;
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
static inline __attribute__((always_inline)) uint32_t long_wave(const uint32_t *src,int at,int width,uint32_t center,
        const uint32_t *cache,int base){
    static const int periods[]={6,8,12,16};
    int cu=u(center),cv=v(center),cy=yy(center);
    if(mx(ab(cu),ab(cv))>90)return center;
    int best=0,bu=cu,bv=cv;
    for(int direction=0;direction<4;direction++){
        int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
        uint32_t cm=src[at-step],cp=src[at+step];
        if((cm>>24)!=255 || (cp>>24)!=255)continue;
        if(distance(su1976(src,cache,base,at-step),sv1976(src,cache,base,at-step),su1976(src,cache,base,at+step),sv1976(src,cache,base,at+step))>46)continue;
        for(int k=0;k<4;k++){
            int period=periods[k],half=period/2;
            uint32_t m=src[at-half*step],p=src[at+half*step];
            if((m>>24)!=255 || (p>>24)!=255)continue;
            int mu=su1976(src,cache,base,at-half*step),mv=sv1976(src,cache,base,at-half*step),pu=su1976(src,cache,base,at+half*step),pv=sv1976(src,cache,base,at+half*step);
            int amp=distance(cu*2,cv*2,mu+pu,mv+pv)/2;
            if(amp<10 || amp>96)continue;
            int opposition=distance(mu,mv,pu,pv);
            if(opposition>3+amp/6)continue;
            int lumaPattern=mx(ab(cy-sy1976(src,cache,base,at-half*step)),ab(cy-sy1976(src,cache,base,at+half*step)));
            if(lumaPattern>2+amp/10)continue;
            int repeat=0,valid=1;
            for(int sign=-1;sign<=1;sign+=2)for(int cycle=1;cycle<=2;cycle++){
                uint32_t q=src[at+sign*period*cycle*step];
                if((q>>24)!=255){valid=0;break;}
                repeat=mx(repeat,distance(cu,cv,su1976(src,cache,base,at+sign*period*cycle*step),sv1976(src,cache,base,at+sign*period*cycle*step)));
                if(ab(cy-sy1976(src,cache,base,at+sign*period*cycle*step))>3+amp/10)valid=0;
            }
            if(!valid || repeat>2+amp/6)continue;
            int quarter=mx(1,period/4);
            uint32_t qm=src[at-quarter*step],qp=src[at+quarter*step];
            if((qm>>24)!=255 || (qp>>24)!=255)continue;
            int meanU=(2*cu+mu+pu)/4,meanV=(2*cv+mv+pv)/4;
            if(mx(ab(meanU),ab(meanV))>36)continue;
            int midpointError=distance(su1976(src,cache,base,at-quarter*step)+su1976(src,cache,base,at+quarter*step),sv1976(src,cache,base,at-quarter*step)+sv1976(src,cache,base,at+quarter*step),meanU*2,meanV*2)/2;
            if(midpointError>4+amp/4)continue;
            int nearChange=distance(su1976(src,cache,base,at-step)+su1976(src,cache,base,at+step),sv1976(src,cache,base,at-step)+sv1976(src,cache,base,at+step),cu*2,cv*2)/2;
            if(nearChange<1 || nearChange>3+amp*14/(period*period))continue;
            int lumaRange=mx(mx(sy1976(src,cache,base,at-quarter*step),sy1976(src,cache,base,at+quarter*step)),cy)-mn(mn(sy1976(src,cache,base,at-quarter*step),sy1976(src,cache,base,at+quarter*step)),cy);
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

/* Exact QualityShadow1932.textureQ8 luma-neighbourhood test. */
static inline __attribute__((always_inline)) int texture_q8(const uint32_t *src,int width,int rows,int x,int row,int threshold,
        const uint32_t *cache,int base) {
    const int support=4,length=9;
    if(x<support || x>=width-support || row<support || row>=rows-support)return 0;
    int best=0,at=row*width+x,samples[9];
    for(int direction=0;direction<4;direction++) {
        int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
        int opaque=1,low=255,high=0,adjacent=0;
        for(int i=0;i<length;i++) {
            uint32_t p=src[at+(i-support)*step];
            if((p>>24)!=255){opaque=0;break;}
            int value=samples[i]=sy1976(src,cache,base,at+(i-support)*step);low=mn(low,value);high=mx(high,value);
            if(i>0)adjacent+=ab(value-samples[i-1]);
        }
        if(!opaque || high-low<threshold*2 || adjacent<(length-1)*threshold)continue;
        int average=(adjacent+(length-2)/2)/(length-1);
        for(int period=2;period<=4;period++) {
            int error=0,count=length-period;
            for(int i=0;i<count;i++)error+=ab(samples[i]-samples[i+period]);
            error=(error+count/2)/count;
            int confidence=clamp((average*4-error*9)*256/mx(1,average*4),0,256);
            confidence=confidence*confidence>>8;
            best=mx(best,confidence);
        }
    }
    return best;
}
/* Java int multiplication wraps before the arithmetic right shift. */
static int mul_shift(int a,int b,int bits) {
    uint32_t product=(uint32_t)a*(uint32_t)b;
    uint32_t shifted=product>>bits;
    if(product&0x80000000u)shifted|=~0u<<(32-bits);
    return (int32_t)shifted;
}
static inline __attribute__((always_inline)) uint32_t sharpen(const uint32_t *src,int at,int width,int rows,
        int x,int row,uint32_t corrected,const int32_t *policy,
        int gain,int base_floor,int limit,int texture_priority,int halo,
        const uint32_t *cache,int base) {
    int center=sy1976(src,cache,base,at),left=sy1976(src,cache,base,at-1),right=sy1976(src,cache,base,at+1);
    int up=sy1976(src,cache,base,at-width),down=sy1976(src,cache,base,at+width);
    if((src[at-1]>>24)!=255 || (src[at+1]>>24)!=255 ||
       (src[at-width]>>24)!=255 || (src[at+width]>>24)!=255 ||
       (src[at-width-1]>>24)!=255 || (src[at-width+1]>>24)!=255 ||
       (src[at+width-1]>>24)!=255 || (src[at+width+1]>>24)!=255)return corrected;
    int nw=sy1976(src,cache,base,at-width-1),ne=sy1976(src,cache,base,at-width+1);
    int sw=sy1976(src,cache,base,at+width-1),se=sy1976(src,cache,base,at+width+1);
    int detail_q8=(center*12-(left+right+up+down)*2-nw-ne-sw-se)*16;
    /* Identical result-bound gate: do not change noise/mask policy or quality. */
    if(ab(detail_q8)<=base_floor)return corrected;
    int edge=mx(ab(nw+2*left+sw-ne-2*right-se),ab(nw+2*up+ne-sw-2*down-se))/4;
    int tolerance=policy[0],texture=texture_q8(src,width,rows,x,row,policy[1],cache,base);
    int flat=clamp((tolerance*2-edge)*256/mx(1,tolerance*2),0,256);
    flat=flat*(256-texture)>>8;
    int floor=base_floor+((policy[2]-base_floor)*flat>>8);
    int magnitude=ab(detail_q8)-floor;
    if(magnitude<=0)return corrected;
    gain=gain*(256-(flat*176>>8))>>8;
    if(center<64)gain=gain*(96+center*160/64)>>8;
    if(texture_priority)gain=mul_shift(gain,policy[3],8);
    int64_t amount=((int64_t)magnitude*gain+32768)>>16;
    int delta=(int)(amount<limit?amount:limit);
    if(delta==0)return corrected;
    if(detail_q8<0)delta=-delta;
    int low=mn(center,mn(mn(left,right),mn(up,down)));
    int high=mx(center,mx(mx(left,right),mx(up,down)));
    if(halo)delta=clamp(delta,low-center,high-center);
    int red=r(corrected),green=g(corrected),blue=b(corrected);
    delta=clamp(delta,-mn(red,mn(green,blue)),255-mx(red,mx(green,blue)));
    return (corrected&0xff000000u)|((uint32_t)(red+delta)<<16)|
        ((uint32_t)(green+delta)<<8)|(uint32_t)(blue+delta);
}
static void run_rows(const uint32_t *src,uint32_t *dst,const int32_t *policy,
        int variable,int width,int rows,int first,int last,int moire,int sharp,
        int gain,int floor,int limit,int texture_priority,int halo,int32_t *scratch) {
    int base=0;
    const uint32_t *cache=scratch?cache_band1976(src,scratch,width,rows,first,last,moire?32:4,&base):0;
    for(int row=first;row<last;row++)for(int x=0;x<width;x++) {
        int at=row*width+x;
        uint32_t original=src[at],p=original;
        if((p>>24)==255) {
            if(moire && row>=4 && row<rows-4 && x>=4 && x<width-4) {
                p=periodic(src,at,width,p,cache,base);
                if(p==original && row>=32 && row<rows-32 && x>=32 && x<width-32)
                    p=long_wave(src,at,width,p,cache,base);
            }
            if(sharp && row>0 && row<rows-1 && x>0 && x<width-1)
                p=sharpen(src,at,width,rows,x,row,p,
                    policy+(variable?((row-first)*width+x)*4:0),
                    gain,floor,limit,texture_priority,halo,cache,base);
        }
        dst[at]=p;
    }
}
#ifdef MOIRE1951_HOST
void moire1951_host(const uint32_t *src,uint32_t *dst,int width,int rows,int first,int last) {
    run_rows(src,dst,0,0,width,rows,first,last,1,0,0,0,0,0,0,0);
}
void finish1951_host(const uint32_t *src,uint32_t *dst,const int32_t *policy,
        int variable,int width,int rows,int first,int last,int moire,int sharp,
        int gain,int floor,int limit,int texture_priority,int halo) {
    run_rows(src,dst,policy,variable,width,rows,first,last,moire,sharp,
        gain,floor,limit,texture_priority,halo,0);
}
void finish1976_host(const uint32_t *src,uint32_t *dst,const int32_t *policy,
        int variable,int width,int rows,int first,int last,int moire,int sharp,
        int gain,int floor,int limit,int texture_priority,int halo,int32_t *scratch) {
    run_rows(src,dst,policy,variable,width,rows,first,last,moire,sharp,
        gain,floor,limit,texture_priority,halo,scratch);
}
#else
JNIEXPORT jint JNICALL JNI_FN(nativeAbi)(JNIEnv *env,jclass cls) {
    (void)env;(void)cls;return 19512;
}
static jboolean finish_entry1976(JNIEnv *env,jclass cls,
    jintArray source,jintArray target,jintArray settings,jboolean variable,
    jint width,jint rows,jint first,jint last,jboolean moire,jboolean sharp,
    jint gain,jint floor,jint limit,jboolean texture_priority,jboolean halo,jintArray scratch) {
    (void)cls;
    if(!source || !target || (*env)->IsSameObject(env,source,target) ||
        width<=0 || rows<=0 || first<0 || last<first || last>rows ||
        last-first>16 || (int64_t)width*rows>INT_MAX ||
        (sharp && (!settings || (*env)->IsSameObject(env,settings,source) ||
                    (*env)->IsSameObject(env,settings,target))))return JNI_FALSE;
    int count=width*rows;
    int64_t policy_count=variable?(int64_t)width*(last-first)*4:4;
    if((*env)->GetArrayLength(env,source)<count ||
       (*env)->GetArrayLength(env,target)<count ||
       (sharp && (policy_count>INT_MAX || (*env)->GetArrayLength(env,settings)<policy_count)))return JNI_FALSE;
    jint *cache=0;
    if(scratch) {
        int64_t needed=2+(int64_t)width*mn(rows,16+(moire?64:8));
        if(needed>INT_MAX || (*env)->GetArrayLength(env,scratch)<needed ||
                (*env)->IsSameObject(env,scratch,source) || (*env)->IsSameObject(env,scratch,target) ||
                (settings && (*env)->IsSameObject(env,scratch,settings)))return JNI_FALSE;
        cache=(*env)->GetPrimitiveArrayCritical(env,scratch,0);
        if(!cache)return JNI_FALSE;
    }
    const jint *src=(*env)->GetPrimitiveArrayCritical(env,source,0);
    if(!src){if(cache)(*env)->ReleasePrimitiveArrayCritical(env,scratch,cache,0);return JNI_FALSE;}
    const jint *policy=0;
    if(sharp) {
        policy=(*env)->GetPrimitiveArrayCritical(env,settings,0);
        if(!policy) {
            (*env)->ReleasePrimitiveArrayCritical(env,source,(void*)src,JNI_ABORT);
            if(cache)(*env)->ReleasePrimitiveArrayCritical(env,scratch,cache,0);
            return JNI_FALSE;
        }
    }
    jboolean output_is_copy=JNI_FALSE;
    jint *dst=(*env)->GetPrimitiveArrayCritical(env,target,&output_is_copy);
    if(!dst || output_is_copy) {
        if(dst)(*env)->ReleasePrimitiveArrayCritical(env,target,dst,JNI_ABORT);
        if(policy)(*env)->ReleasePrimitiveArrayCritical(env,settings,(void*)policy,JNI_ABORT);
        (*env)->ReleasePrimitiveArrayCritical(env,source,(void*)src,JNI_ABORT);
        if(cache)(*env)->ReleasePrimitiveArrayCritical(env,scratch,cache,0);
        return JNI_FALSE;
    }
    /* At most 16 disjoint output rows. No JNI operations, allocations, callbacks
     * or blocking occur while source, integer policy and target are pinned. */
    run_rows((const uint32_t*)src,(uint32_t*)dst,(const int32_t*)policy,variable,
        width,rows,first,last,moire,sharp,gain,floor,limit,texture_priority,halo,(int32_t*)cache);
    (*env)->ReleasePrimitiveArrayCritical(env,target,dst,0);
    if(policy)(*env)->ReleasePrimitiveArrayCritical(env,settings,(void*)policy,JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env,source,(void*)src,JNI_ABORT);
    if(cache)(*env)->ReleasePrimitiveArrayCritical(env,scratch,cache,0);
    return JNI_TRUE;
}
JNIEXPORT jboolean JNICALL JNI_FN(finishStripNative)(JNIEnv *env,jclass cls,
    jintArray source,jintArray target,jintArray settings,jboolean variable,
    jint width,jint rows,jint first,jint last,jboolean moire,jboolean sharp,
    jint gain,jint floor,jint limit,jboolean texture_priority,jboolean halo) {
    return finish_entry1976(env,cls,source,target,settings,variable,width,rows,first,last,
        moire,sharp,gain,floor,limit,texture_priority,halo,0);
}
JNIEXPORT jboolean JNICALL JNI_FN(finishStripCached1976)(JNIEnv *env,jclass cls,
    jintArray source,jintArray target,jintArray settings,jboolean variable,
    jint width,jint rows,jint first,jint last,jboolean moire,jboolean sharp,
    jint gain,jint floor,jint limit,jboolean texture_priority,jboolean halo,jintArray scratch) {
    if(!scratch)return JNI_FALSE;
    return finish_entry1976(env,cls,source,target,settings,variable,width,rows,first,last,
        moire,sharp,gain,floor,limit,texture_priority,halo,scratch);
}

#endif
