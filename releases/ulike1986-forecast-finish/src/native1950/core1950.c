#include <jni.h>
#include <stdint.h>
#include <limits.h>
#if defined(__aarch64__)
#include <arm_neon.h>
#endif
#define JNI_NAME(n) Java_com_hiro_ulike_CorePixels1950_##n
static inline int minv(int a,int b){return a<b?a:b;}
static inline int maxv(int a,int b){return a>b?a:b;}
static inline int absval(int a){return a<0?-a:a;}
static inline int clamp(int x,int a,int b){return minv(maxv(x,a),b);}
static inline int red(uint32_t p){return (p>>16)&255;}
static inline int green(uint32_t p){return (p>>8)&255;}
static inline int blue(uint32_t p){return p&255;}
static inline int y(uint32_t p){return (red(p)*77+green(p)*150+blue(p)*29+128)>>8;}
static inline int cb(uint32_t d){return ((d>>8)&511)-255;}
static inline int cr(uint32_t d){return ((d>>17)&511)-255;}
#define OPAQUE1950 (1u<<26)
static inline int divround(int n,int d){return n>=0?(n+d/2)/d:-((-n+d/2)/d);}
/* The three variable-denominator divisions dominate each center. The table is
 * initialized during Java class initialization, before JNI output is pinned.
 * floor(2^32/d) yields a quotient at most one below the exact integer result
 * for these bounded accumulators; one multiply/subtract corrects it exactly. */
static uint32_t reciprocal[16385];
static inline int divround_weights(int n,int d){
    if(d<5120 || d>16384 || !reciprocal[d])return divround(n,d);
    uint32_t rounded=(uint32_t)(n<0?-n:n)+(uint32_t)d/2;
    uint32_t quotient=(uint32_t)(((uint64_t)rounded*reciprocal[d])>>32);
    quotient+=(uint32_t)((uint64_t)(quotient+1)*(uint32_t)d<=rounded);
    return n<0?-(int)quotient:(int)quotient;
}
static inline uint32_t rgb(int r,int g,int b){return 0xff000000u|((uint32_t)clamp(r,0,255)<<16)|((uint32_t)clamp(g,0,255)<<8)|(uint32_t)clamp(b,0,255);}
static int skin(int yy,int cbb,int crr){
    int a=clamp(crr*256/14,0,256)*clamp((90-crr)*256/20,0,256)/256;
    int b=clamp((20-cbb)*256/16,0,256)*clamp((cbb+80)*256/20,0,256)/256;
    return a*b/256*clamp((yy-10)*256/25,0,256)/256;
}
/* Exact four-pixel NEON luma/chroma predecode. All lanes are 32-bit integer;
 * arithmetic is bounded and has the same shifts and +128 rounding as Java. */
static void decode(const uint32_t *p,uint32_t *d,int count){
    int i=0;
#if defined(__aarch64__)
    uint32x4_t mask=vdupq_n_u32(255),bias=vdupq_n_u32(255);
    for(;i+4<=count;i+=4){
        uint32x4_t v=vld1q_u32(p+i),r=vandq_u32(vshrq_n_u32(v,16),mask),g=vandq_u32(vshrq_n_u32(v,8),mask),b=vandq_u32(v,mask);
        uint32x4_t yy=vshrq_n_u32(vaddq_u32(vaddq_u32(vmulq_n_u32(r,77),vmulq_n_u32(g,150)),vaddq_u32(vmulq_n_u32(b,29),vdupq_n_u32(128))),8);
        uint32x4_t cbb=vsubq_u32(vaddq_u32(b,bias),yy),crr=vsubq_u32(vaddq_u32(r,bias),yy);
        uint32x4_t opaque=vandq_u32(vceqq_u32(vshrq_n_u32(v,24),vdupq_n_u32(255)),vdupq_n_u32(OPAQUE1950));
        vst1q_u32(d+i,vorrq_u32(opaque,vorrq_u32(yy,vorrq_u32(vshlq_n_u32(cbb,8),vshlq_n_u32(crr,17)))));
    }
#endif
    for(;i<count;i++){uint32_t v=p[i];int yy=y(v);d[i]=(uint32_t)yy|((uint32_t)(blue(v)-yy+255)<<8)|((uint32_t)(red(v)-yy+255)<<17)|((v>>24)==255?OPAQUE1950:0);}
}
/* Each source value is needed by up to six nearby centers. Pack its luma and
 * chroma once, using the *guide* luma in the horizontal pass and the value
 * luma in the vertical pass. Their original integer subtraction is preserved. */
static void prepare_values(const uint32_t *p,const uint32_t *guide,uint32_t *d,int count,int horizontal){
    int i=0;
#if defined(__aarch64__)
    uint32x4_t mask=vdupq_n_u32(255);
    for(;i+4<=count;i+=4){
        uint32x4_t v=vld1q_u32(p+i),r=vandq_u32(vshrq_n_u32(v,16),mask),g=vandq_u32(vshrq_n_u32(v,8),mask),b=vandq_u32(v,mask);
        uint32x4_t yy=horizontal?vandq_u32(vld1q_u32(guide+i),mask):vshrq_n_u32(vaddq_u32(vaddq_u32(vmulq_n_u32(r,77),vmulq_n_u32(g,150)),vaddq_u32(vmulq_n_u32(b,29),vdupq_n_u32(128))),8);
        uint32x4_t cbb=vsubq_u32(vaddq_u32(b,vdupq_n_u32(255)),yy),crr=vsubq_u32(vaddq_u32(r,vdupq_n_u32(255)),yy);
        vst1q_u32(d+i,vorrq_u32(yy,vorrq_u32(vshlq_n_u32(cbb,8),vshlq_n_u32(crr,17))));
    }
#endif
    for(;i<count;i++){int yy=horizontal?(int)(guide[i]&255):y(p[i]);d[i]=(uint32_t)yy|((uint32_t)(blue(p[i])-yy+255)<<8)|((uint32_t)(red(p[i])-yy+255)<<17);}
}
/* H28: four independent centers retain the six-tap sequence and exact scalar
 * lookup/division. Only bounded integer differences, masks and sums use NEON. */
#if defined(__aarch64__)
static void bilateral_neon4(const uint32_t *guide,uint32_t *out,int width,int rows,
        int row,int x,int noise,int horizontal,int texture,
        const int32_t *lr,const int32_t *cc,const uint32_t *decoded,int bias,
        const uint32_t *value_decoded,int value_bias,const uint16_t *range_by_y,
        const uint8_t *mix_y_by_y,const uint8_t *mix_c_by_y){
    static const int offsets[6]={-3,-2,-1,1,2,3},spatial[6]={1,6,15,15,6,1};
    int at=row*width+x;uint32_t centers[4],guides[4],values[4];
    vst1q_u32(centers,vld1q_u32(guide+at));
    uint32x4_t dc=vld1q_u32(decoded+(at-bias)),mask=vdupq_n_u32(255);
    vst1q_u32(guides,dc);vst1q_u32(values,vld1q_u32(value_decoded+(at-value_bias)));
    int32x4_t yc=vreinterpretq_s32_u32(vandq_u32(dc,mask));
    int32x4_t cb0=vsubq_s32(vreinterpretq_s32_u32(vandq_u32(vshrq_n_u32(dc,8),vdupq_n_u32(511))),vdupq_n_s32(255));
    int32x4_t cr0=vsubq_s32(vreinterpretq_s32_u32(vandq_u32(vshrq_n_u32(dc,17),vdupq_n_u32(511))),vdupq_n_s32(255));
    int32_t sy[4],sb[4],sr[4],wy0[4],wc0[4];
    for(int lane=0;lane<4;lane++){sy[lane]=(values[lane]&255)*5120;sb[lane]=cb(values[lane])*5120;sr[lane]=cr(values[lane])*5120;wy0[lane]=wc0[lane]=5120;}
    int32x4_t sumy=vld1q_s32(sy),sumb=vld1q_s32(sb),sumr=vld1q_s32(sr),wy=vld1q_s32(wy0),wc=vld1q_s32(wc0);
    for(int t=0;t<6;t++){
        int index=horizontal?at+offsets[t]:clamp(row+offsets[t],0,rows-1)*width+x;
        uint32x4_t nd=vld1q_u32(decoded+(index-bias)),vd=vld1q_u32(value_decoded+(index-value_bias));
        int32x4_t ny=vreinterpretq_s32_u32(vandq_u32(nd,mask));
        int32x4_t nb=vsubq_s32(vreinterpretq_s32_u32(vandq_u32(vshrq_n_u32(nd,8),vdupq_n_u32(511))),vdupq_n_s32(255));
        int32x4_t nr=vsubq_s32(vreinterpretq_s32_u32(vandq_u32(vshrq_n_u32(nd,17),vdupq_n_u32(511))),vdupq_n_s32(255));
        int32_t dys[4],distances[4],yw0[4],cw0[4];uint32_t neighbor[4];
        vst1q_s32(dys,vabsq_s32(vsubq_s32(yc,ny)));
        vst1q_s32(distances,vmaxq_s32(vabsq_s32(vsubq_s32(cb0,nb)),vabsq_s32(vsubq_s32(cr0,nr))));
        vst1q_u32(neighbor,nd);
        for(int lane=0;lane<4;lane++){
            int dy=dys[lane],dist=distances[lane],range=range_by_y[guides[lane]&255];
            if((centers[lane]>>24)!=255 || !(neighbor[lane]&OPAQUE1950) || dy>72 || dist>110){yw0[lane]=cw0[lane]=0;continue;}
            yw0[lane]=spatial[t]*lr[maxv(dy,dist/3)*range/256];
            cw0[lane]=spatial[t]*cc[maxv(dy*2,dist)*range/256];
        }
        int32x4_t yw=vld1q_s32(yw0),cw=vld1q_s32(cw0);
        int32x4_t vy=vreinterpretq_s32_u32(vandq_u32(vd,mask));
        int32x4_t vb=vsubq_s32(vreinterpretq_s32_u32(vandq_u32(vshrq_n_u32(vd,8),vdupq_n_u32(511))),vdupq_n_s32(255));
        int32x4_t vr=vsubq_s32(vreinterpretq_s32_u32(vandq_u32(vshrq_n_u32(vd,17),vdupq_n_u32(511))),vdupq_n_s32(255));
        wy=vaddq_s32(wy,yw);wc=vaddq_s32(wc,cw);
        sumy=vaddq_s32(sumy,vmulq_s32(yw,vy));sumb=vaddq_s32(sumb,vmulq_s32(cw,vb));sumr=vaddq_s32(sumr,vmulq_s32(cw,vr));
    }
    vst1q_s32(sy,sumy);vst1q_s32(sb,sumb);vst1q_s32(sr,sumr);vst1q_s32(wy0,wy);vst1q_s32(wc0,wc);
    for(int lane=0;lane<4;lane++){
        uint32_t center=centers[lane],dc0=guides[lane];int pos=at+lane;
        if((center>>24)!=255){out[pos]=center;continue;}
        int yy=dc0&255,cbb=cb(dc0),crr=cr(dc0);
        int dy=divround_weights(sy[lane],wy0[lane])-yy,db=divround_weights(sb[lane],wc0[lane])-cbb,dr=divround_weights(sr[lane],wc0[lane])-crr;
        if(!horizontal){
            int mixy=mix_y_by_y[yy],mixc=mix_c_by_y[yy],col=x+lane;
            int edge=absval((int)(decoded[row*width+minv(width-1,col+1)-bias]&255)-(int)(decoded[row*width+maxv(0,col-1)-bias]&255))
                +absval((int)(decoded[minv(rows-1,row+1)*width+col-bias]&255)-(int)(decoded[maxv(0,row-1)*width+col-bias]&255));
            edge=clamp((edge-12)*256/72,0,256);int protect=edge*12/256;
            if(texture)protect+=edge*skin(yy,cbb,crr)/256*20/256;
            mixy=mixy*(100-protect)/100;dy=divround(dy*mixy,100);db=divround(db*mixc,100);dr=divround(dr*mixc,100);
        }
        out[pos]=rgb(red(center)+dy+dr,green(center)+dy-divround(dr*77+db*29,150),blue(center)+dy+db);
    }
    (void)noise;
}
#endif

static void bilateral_prepared(const uint32_t *guide,uint32_t *out,
        int width,int rows,int noise,int horizontal,int texture,int shadows,int begin,int end,
        const int32_t *lr,const int32_t *cc,const uint32_t *decoded,int bias,
        const uint32_t *value_decoded,int value_bias){
    static const int offsets[6]={-3,-2,-1,1,2,3},spatial[6]={1,6,15,15,6,1};
    static const int lm[5]={0,48,68,85,96},cm[5]={0,78,91,98,100};
    uint16_t range_by_y[256];
    uint8_t mix_y_by_y[256],mix_c_by_y[256];
    for(int luminance=0;luminance<256;luminance++){
        int dark=shadows?clamp((112-luminance)*256/96,0,256):0;
        range_by_y[luminance]=(uint16_t)(256-dark/6);
        mix_y_by_y[luminance]=(uint8_t)minv(100,lm[noise]+dark*12/256);
        mix_c_by_y[luminance]=(uint8_t)minv(100,cm[noise]+dark*5/256);
    }
    lr+=noise*256;cc+=noise*256;
    for(int row=begin;row<end;row++){
        int columns[6];for(int t=0;t<6;t++)columns[t]=clamp(row+offsets[t],0,rows-1)*width;
        int rowbase=row*width,prev=maxv(0,row-1)*width,next=minv(rows-1,row+1)*width;
        for(int x=0;x<width;x++){
#if defined(__aarch64__)
            if(x>=3 && x+6<width){
                bilateral_neon4(guide,out,width,rows,row,x,noise,horizontal,texture,
                    lr,cc,decoded,bias,value_decoded,value_bias,range_by_y,mix_y_by_y,mix_c_by_y);
                x+=3;continue;
            }
#endif
            int at=rowbase+x;uint32_t center=guide[at];
            if((center>>24)!=255){out[at]=center;continue;}
            uint32_t dc=decoded[at-bias];int yc=dc&255,cbb=cb(dc),crr=cr(dc);
            int range=range_by_y[yc];
            uint32_t vd=value_decoded[at-value_bias];int vy=vd&255;
            int sumy=vy*5120,sumb=cb(vd)*5120,sumr=cr(vd)*5120,wy=5120,wc=5120;
            int at_edge=x<3 || x>=width-3;
            for(int t=0;t<6;t++){
                int index=horizontal?(at_edge?rowbase+clamp(x+offsets[t],0,width-1):at+offsets[t]):columns[t]+x;
                uint32_t nd=decoded[index-bias];if(!(nd&OPAQUE1950))continue;
                int yn=nd&255,dy=absval(yc-yn),dist=maxv(absval(cbb-cb(nd)),absval(crr-cr(nd)));
                if(dy>72 || dist>110)continue;
                int yw=spatial[t]*lr[maxv(dy,dist/3)*range/256];
                int cw=spatial[t]*cc[maxv(dy*2,dist)*range/256];
                uint32_t vn=value_decoded[index-value_bias];
                wy+=yw;sumy+=yw*(vn&255);wc+=cw;sumb+=cw*cb(vn);sumr+=cw*cr(vn);
            }
            int dy=divround_weights(sumy,wy)-yc,db=divround_weights(sumb,wc)-cbb,dr=divround_weights(sumr,wc)-crr;
            if(!horizontal){
                int mixy=mix_y_by_y[yc],mixc=mix_c_by_y[yc];
                int edge=absval((int)(decoded[rowbase+minv(width-1,x+1)-bias]&255)-(int)(decoded[rowbase+maxv(0,x-1)-bias]&255))
                    +absval((int)(decoded[next+x-bias]&255)-(int)(decoded[prev+x-bias]&255));
                edge=clamp((edge-12)*256/72,0,256);int protect=edge*12/256;
                if(texture)protect+=edge*skin(yc,cbb,crr)/256*20/256;
                mixy=mixy*(100-protect)/100;
                dy=divround(dy*mixy,100);db=divround(db*mixc,100);dr=divround(dr*mixc,100);
            }
            out[at]=rgb(red(center)+dy+dr,green(center)+dy-divround(dr*77+db*29,150),blue(center)+dy+db);
        }
    }
}
static void bilateral(const uint32_t *guide,const uint32_t *values,uint32_t *out,
        int width,int rows,int noise,int horizontal,int texture,int shadows,int begin,int end,
        const int32_t *lr,const int32_t *cc,uint32_t *decoded,uint32_t *value_decoded){
    int top=maxv(0,begin-3),bottom=end>rows-3?rows:end+3,bias=top*width;
    int count=(bottom-top)*width;
    decode(guide+bias,decoded,count);
    prepare_values(values+bias,decoded,value_decoded,count,horizontal);
    bilateral_prepared(guide,out,width,rows,noise,horizontal,texture,shadows,begin,end,
        lr,cc,decoded,bias,value_decoded,bias);
}
/* H29 retains exact rounded horizontal RGB and reuses its source guide for the
 * following vertical pass. The horizontal guide == values, so packed values are
 * exactly the same lower 26 bits as decoded; the opacity bit is never consumed as
 * a value component. One bounded pair handles at most 64 requested rows. */
static void bilateral_pair1956(const uint32_t *guide,uint32_t *out,uint32_t *horizontal,
        int width,int rows,int noise,int texture,int shadows,int begin,int end,
        const int32_t *lr,const int32_t *cc,uint32_t *decoded,uint32_t *value_decoded){
    int hfirst=maxv(0,begin-3),hlast=end>rows-3?rows:end+3;
    int top=maxv(0,hfirst-3),bottom=hlast>rows-3?rows:hlast+3,bias=top*width;
    decode(guide+bias,decoded,(bottom-top)*width);
    bilateral_prepared(guide,horizontal,width,rows,noise,1,texture,shadows,hfirst,hlast,
        lr,cc,decoded,bias,decoded,bias);
    int vbias=hfirst*width;
    prepare_values(horizontal+vbias,decoded+(vbias-bias),value_decoded,(hlast-hfirst)*width,0);
    bilateral_prepared(guide,out,width,rows,noise,0,texture,shadows,begin,end,
        lr,cc,decoded,bias,value_decoded,vbias);
}

JNIEXPORT jint JNICALL JNI_NAME(nativeAbi)(JNIEnv *e,jclass c){
    (void)e;(void)c;
    for(int d=5120;d<=16384;d++)reciprocal[d]=(uint32_t)((1ull<<32)/(uint32_t)d);
    return 1950;
}
JNIEXPORT jboolean JNICALL JNI_NAME(bilateralNative)(JNIEnv *e,jclass cls,
        jintArray guide,jintArray values,jintArray out,jint width,jint rows,jint noise,
        jboolean horizontal,jboolean texture,jboolean shadows,jint begin,jint end,
        jintArray lr,jintArray cc,jintArray decoded,jintArray value_y){
    (void)cls;
    if(!guide || !values || !out || !lr || !cc || !decoded || !value_y ||
            width<1 || rows<1 || noise<1 || noise>4 || begin<0 || end<begin || end>rows ||
            (int64_t)width*rows>INT_MAX)return JNI_FALSE;
    jobject arrays[7]={guide,values,out,lr,cc,decoded,value_y};
    int count=7;int lengths[7]={0};
    for(int i=0;i<count;i++){lengths[i]=(*e)->GetArrayLength(e,(jarray)arrays[i]);for(int j=0;j<i;j++)if(i!=1 || j!=0)if((*e)->IsSameObject(e,arrays[i],arrays[j]))return JNI_FALSE;}
    int n=width*rows,needed=width*((end>rows-3?rows:end+3)-maxv(0,begin-3));
    if(lengths[0]<n || lengths[1]<n || lengths[2]<n || lengths[3]<1280 || lengths[4]<1280 || lengths[5]<needed || lengths[6]<needed)return JNI_FALSE;
    jint *p[7]={0};jboolean ok=JNI_FALSE,output_is_copy=JNI_FALSE;
    /* No JNI callback, allocation, locks, system IO or blocking occurs with
     * pinned arrays. The allocation and full bounds admission happen above. */
    for(int i=0;i<count;i++){
        p[i]=(*e)->GetPrimitiveArrayCritical(e,(jarray)arrays[i],i==2?&output_is_copy:0);
        if(!p[i])goto done;
        /* A copied shared output would commit the entire Java array on release,
         * potentially overwriting a different worker's disjoint rows. Refuse it
         * before any writes; the retained Java kernel commits only its own rows. */
        if(i==2 && output_is_copy)goto done;
    }
    for(int i=noise*256;i<(noise+1)*256;i++)if(p[3][i]<0 || p[3][i]>256 || p[4][i]<0 || p[4][i]>256)goto done;
    bilateral((uint32_t*)p[0],(uint32_t*)p[1],(uint32_t*)p[2],width,rows,noise,horizontal,texture,shadows,begin,end,p[3],p[4],(uint32_t*)p[5],(uint32_t*)p[6]);ok=JNI_TRUE;
done:
    for(int i=count-1;i>=0;i--)if(p[i])(*e)->ReleasePrimitiveArrayCritical(e,(jarray)arrays[i],p[i],i==2&&ok?0:JNI_ABORT);
    return ok;
}

JNIEXPORT jint JNICALL JNI_NAME(bilateralPairCpuAbi)(JNIEnv *e,jclass c){(void)e;(void)c;return 1956;}
JNIEXPORT jboolean JNICALL JNI_NAME(bilateralPairCpuNative)(JNIEnv *e,jclass cls,
        jintArray guide,jintArray output,jintArray horizontal,jint width,jint rows,jint noise,
        jboolean texture,jboolean shadows,jint begin,jint end,jintArray lr,jintArray cc,
        jintArray decoded,jintArray value_packed){
    (void)cls;
    if(!guide || !output || !horizontal || !lr || !cc || !decoded || !value_packed ||
       width<1 || rows<1 || noise<1 || noise>4 || begin<0 || end<=begin || end>rows ||
       end-begin>64 || (int64_t)width*rows>INT_MAX)return JNI_FALSE;
    jobject arrays[7]={guide,output,horizontal,lr,cc,decoded,value_packed};int lengths[7];
    for(int i=0;i<7;i++){
        lengths[i]=(*e)->GetArrayLength(e,(jarray)arrays[i]);
        for(int j=0;j<i;j++)if((*e)->IsSameObject(e,arrays[i],arrays[j]))return JNI_FALSE;
    }
    int n=width*rows,hfirst=maxv(0,begin-3),hlast=end>rows-3?rows:end+3;
    int needed=width*((hlast>rows-3?rows:hlast+3)-maxv(0,hfirst-3));
    if(lengths[0]<n || lengths[1]<n || lengths[2]<n || lengths[3]<1280 || lengths[4]<1280 ||
        lengths[5]<needed || lengths[6]<width*(hlast-hfirst))return JNI_FALSE;
    jint *p[7]={0};jboolean ok=JNI_FALSE;
    for(int i=0;i<7;i++){
        jboolean copied=JNI_FALSE;p[i]=(*e)->GetPrimitiveArrayCritical(e,(jarray)arrays[i],&copied);
        if(!p[i] || ((i==1 || i==2) && copied))goto done;
    }
    for(int i=noise*256;i<(noise+1)*256;i++)if(p[3][i]<0 || p[3][i]>256 || p[4][i]<0 || p[4][i]>256)goto done;
    bilateral_pair1956((uint32_t*)p[0],(uint32_t*)p[1],(uint32_t*)p[2],width,rows,noise,texture,shadows,
        begin,end,p[3],p[4],(uint32_t*)p[5],(uint32_t*)p[6]);ok=JNI_TRUE;
done:
    for(int i=6;i>=0;i--)if(p[i])(*e)->ReleasePrimitiveArrayCritical(e,(jarray)arrays[i],p[i],(i==1||i==2)&&ok?0:JNI_ABORT);
    return ok;
}
