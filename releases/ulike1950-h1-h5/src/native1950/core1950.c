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
static inline int divround(int n,int d){return n>=0?(n+d/2)/d:-((-n+d/2)/d);}
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
        vst1q_u32(d+i,vorrq_u32(yy,vorrq_u32(vshlq_n_u32(cbb,8),vshlq_n_u32(crr,17))));
    }
#endif
    for(;i<count;i++){int yy=y(p[i]);d[i]=(uint32_t)yy|((uint32_t)(blue(p[i])-yy+255)<<8)|((uint32_t)(red(p[i])-yy+255)<<17);}
}
static void lumas(const uint32_t *p,int32_t *d,int count){
    int i=0;
#if defined(__aarch64__)
    uint32x4_t mask=vdupq_n_u32(255);
    for(;i+4<=count;i+=4){
        uint32x4_t v=vld1q_u32(p+i),r=vandq_u32(vshrq_n_u32(v,16),mask),g=vandq_u32(vshrq_n_u32(v,8),mask),b=vandq_u32(v,mask);
        uint32x4_t yy=vshrq_n_u32(vaddq_u32(vaddq_u32(vmulq_n_u32(r,77),vmulq_n_u32(g,150)),vaddq_u32(vmulq_n_u32(b,29),vdupq_n_u32(128))),8);
        vst1q_s32(d+i,vreinterpretq_s32_u32(yy));
    }
#endif
    for(;i<count;i++)d[i]=y(p[i]);
}
static void bilateral(const uint32_t *guide,const uint32_t *values,uint32_t *out,
        int width,int rows,int noise,int horizontal,int texture,int shadows,int begin,int end,
        const int32_t *lr,const int32_t *cc,uint32_t *decoded,int32_t *value_y){
    static const int offsets[6]={-3,-2,-1,1,2,3},spatial[6]={1,6,15,15,6,1};
    static const int lm[5]={0,48,68,85,96},cm[5]={0,78,91,98,100};
    int top=maxv(0,begin-3),bottom=end>rows-3?rows:end+3,bias=top*width;
    decode(guide+bias,decoded,(bottom-top)*width);
    if(!horizontal)lumas(values+bias,value_y,(bottom-top)*width);
    lr+=noise*256;cc+=noise*256;
    for(int row=begin;row<end;row++){
        int columns[6];for(int t=0;t<6;t++)columns[t]=clamp(row+offsets[t],0,rows-1)*width;
        int rowbase=row*width,prev=maxv(0,row-1)*width,next=minv(rows-1,row+1)*width;
        for(int x=0;x<width;x++){
            int at=rowbase+x;uint32_t center=guide[at];
            if((center>>24)!=255){out[at]=center;continue;}
            uint32_t dc=decoded[at-bias];int yc=dc&255,cbb=cb(dc),crr=cr(dc);
            int dark=shadows?clamp((112-yc)*256/96,0,256):0,range=256-dark/6;
            int vy=horizontal?yc:value_y[at-bias];uint32_t vc=values[at];
            int sumy=vy*5120,sumb=(blue(vc)-vy)*5120,sumr=(red(vc)-vy)*5120,wy=5120,wc=5120;
            for(int t=0;t<6;t++){
                int index=horizontal?rowbase+clamp(x+offsets[t],0,width-1):columns[t]+x;
                if((guide[index]>>24)!=255)continue;
                uint32_t nd=decoded[index-bias];int yn=nd&255,dy=absval(yc-yn),dist=maxv(absval(cbb-cb(nd)),absval(crr-cr(nd)));
                if(dy>72 || dist>110)continue;
                int yw=spatial[t]*lr[maxv(dy,dist/3)*range/256];
                int cw=spatial[t]*cc[maxv(dy*2,dist)*range/256];
                uint32_t nv=values[index];int ny=horizontal?yn:value_y[index-bias];
                wy+=yw;sumy+=yw*ny;wc+=cw;sumb+=cw*(blue(nv)-ny);sumr+=cw*(red(nv)-ny);
            }
            int dy=divround(sumy,wy)-yc,db=divround(sumb,wc)-cbb,dr=divround(sumr,wc)-crr;
            if(!horizontal){
                int mixy=minv(100,lm[noise]+dark*12/256),mixc=minv(100,cm[noise]+dark*5/256);
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
JNIEXPORT jint JNICALL JNI_NAME(nativeAbi)(JNIEnv *e,jclass c){(void)e;(void)c;return 1950;}
JNIEXPORT jboolean JNICALL JNI_NAME(bilateralNative)(JNIEnv *e,jclass cls,
        jintArray guide,jintArray values,jintArray out,jint width,jint rows,jint noise,
        jboolean horizontal,jboolean texture,jboolean shadows,jint begin,jint end,
        jintArray lr,jintArray cc,jintArray decoded,jintArray value_y){
    (void)cls;
    if(!guide || !values || !out || !lr || !cc || !decoded || (!horizontal && !value_y) ||
            width<1 || rows<1 || noise<1 || noise>4 || begin<0 || end<begin || end>rows ||
            (int64_t)width*rows>INT_MAX)return JNI_FALSE;
    jobject arrays[7]={guide,values,out,lr,cc,decoded,value_y};
    int count=horizontal?6:7;int lengths[7]={0};
    for(int i=0;i<count;i++){lengths[i]=(*e)->GetArrayLength(e,(jarray)arrays[i]);for(int j=0;j<i;j++)if(i!=1 || j!=0)if((*e)->IsSameObject(e,arrays[i],arrays[j]))return JNI_FALSE;}
    int n=width*rows,needed=width*((end>rows-3?rows:end+3)-maxv(0,begin-3));
    if(lengths[0]<n || lengths[1]<n || lengths[2]<n || lengths[3]<1280 || lengths[4]<1280 || lengths[5]<needed || (!horizontal && lengths[6]<needed))return JNI_FALSE;
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
    bilateral((uint32_t*)p[0],(uint32_t*)p[1],(uint32_t*)p[2],width,rows,noise,horizontal,texture,shadows,begin,end,p[3],p[4],(uint32_t*)p[5],p[6]);ok=JNI_TRUE;
done:
    for(int i=count-1;i>=0;i--)if(p[i])(*e)->ReleasePrimitiveArrayCritical(e,(jarray)arrays[i],p[i],i==2&&ok?0:JNI_ABORT);
    return ok;
}
