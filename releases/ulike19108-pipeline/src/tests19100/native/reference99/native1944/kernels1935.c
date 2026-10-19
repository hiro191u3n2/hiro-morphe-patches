#include "kernels1935.h"
#if defined(__aarch64__)
#include <arm_neon.h>
#define SIMD1935 1
#else
#define SIMD1935 0
#endif
/* -ffp-contract=off and -fno-fast-math are mandatory. No reduction reassociation. */
static int finite1935(float v) { union { float f; uint32_t u; } x={v}; return (x.u&0x7f800000u)!=0x7f800000u; }
#if SIMD1935
static uint64_t enter_ieee(void) { uint64_t old,now; __asm__ volatile("mrs %0, fpcr":"=r"(old)); now=old&~((3ull<<22)|(1ull<<24)|(1ull<<25)); if(now!=old)__asm__ volatile("msr fpcr, %0"::"r"(now)); return old; }
static void leave_ieee(uint64_t old) { __asm__ volatile("msr fpcr, %0"::"r"(old)); }
#endif
int speed1935_plane_valid(int start,int limit,int row_stride,int pixel_stride,int width,int height) {
    if(start<0 || limit<start || row_stride<=0 || pixel_stride<=0 || width<=0 || height<=0)return 0;
    int64_t span=(int64_t)(width-1)*pixel_stride+1;
    return row_stride>=span && (int64_t)start+(int64_t)(height-1)*row_stride+span<=limit;
}
static void copy_y(const uint8_t *src,int pixel_stride,uint8_t *dst,int count) {
    int x=0;
#if SIMD1935
    if(pixel_stride==1)for(;x+16<=count;x+=16)vst1q_u8(dst+x,vld1q_u8(src+x));
    /* vld2 loads 32 bytes. Strict plane validation permits only 31 for the last
       16 samples of a stride-2 row; retain the final group for scalar copying. */
    else if(pixel_stride==2)for(;x+16<count;x+=16)vst1q_u8(dst+x,vld2q_u8(src+2*x).val[0]);
#endif
    for(;x<count;x++)dst[x]=src[(size_t)x*pixel_stride];
}
void speed1935_pack(const uint8_t *y,int ys,int yr,int yp,const uint8_t *u,int us,int ur,int up,const uint8_t *v,int vs,int vr,int vp,int width,int height,uint8_t *out) {
    for(int row=0;row<height;row++)copy_y(y+ys+(size_t)row*yr,yp,out+(size_t)row*width,width);
    uint8_t *chroma=out+(size_t)width*height;
    const int cw=width/2;
    for(int row=0;row<height/2;row++) {
        const uint8_t *uu=u+us+(size_t)row*ur,*vv=v+vs+(size_t)row*vr;
        uint8_t *dst=chroma+(size_t)row*width;int x=0;
#if SIMD1935
        if(up==1 && vp==1)for(;x+16<=cw;x+=16){uint8x16x2_t uv={{vld1q_u8(vv+x),vld1q_u8(uu+x)}};vst2q_u8(dst+2*x,uv);}
        else if(up==2 && vp==2)for(;x+16<cw;x+=16){uint8x16x2_t uv={{vld2q_u8(vv+2*x).val[0],vld2q_u8(uu+2*x).val[0]}};vst2q_u8(dst+2*x,uv);}
#endif
        for(;x<cw;x++){dst[2*x]=vv[(size_t)x*vp];dst[2*x+1]=uu[(size_t)x*up];}
    }
}
int speed1935_horizontal_valid(const int32_t *raw,int raw_count,const int32_t *offsets,int offset_count,const int32_t *indices,int index_count,const float *weights,int weight_count,int out_count,int width) {
    (void)raw;
    if(width<=0 || width>0x7fffffff/3 || width>=offset_count || out_count<width*3 || raw_count<=0 || offsets[0]<0)return 0;
    int last=offsets[width];if(last>index_count || last>weight_count)return 0;
    for(int x=0;x<width;x++)if(offsets[x]>=offsets[x+1])return 0;
    for(int t=offsets[0];t<last;t++)if(indices[t]<0 || indices[t]>=raw_count || !finite1935(weights[t]))return 0;
    return 1;
}
void speed1935_horizontal(const int32_t *raw,const int32_t *offsets,const int32_t *indices,const float *weights,float *out,int width) {
#if SIMD1935
    uint64_t old=enter_ieee();
    for(int x=0;x<width;x++) {
        float32x4_t sum=vdupq_n_f32(0.f),mn=vdupq_n_f32(__builtin_inff()),mx=vdupq_n_f32(-__builtin_inff());
        for(int t=offsets[x];t<offsets[x+1];t++) {
            uint32_t p=(uint32_t)raw[indices[t]];
            uint32x4_t ints={p>>16&255u,p>>8&255u,p&255u,0u};float32x4_t value=vcvtq_f32_u32(ints);
            float32x4_t product=vmulq_n_f32(value,weights[t]);sum=vaddq_f32(sum,product);
            mn=vminq_f32(mn,value);mx=vmaxq_f32(mx,value);
        }
        float32x4_t value=vmaxq_f32(mn,vminq_f32(mx,sum));
        vst1q_lane_f32(out+3*x,value,0);vst1q_lane_f32(out+3*x+1,value,1);vst1q_lane_f32(out+3*x+2,value,2);
    }
    leave_ieee(old);
#else
    for(int x=0;x<width;x++) {
        float sum[3]={0,0,0},mn[3]={__builtin_inff(),__builtin_inff(),__builtin_inff()},mx[3]={-__builtin_inff(),-__builtin_inff(),-__builtin_inff()};
        for(int t=offsets[x];t<offsets[x+1];t++) {
            uint32_t p=(uint32_t)raw[indices[t]];float values[3]={(float)(p>>16&255u),(float)(p>>8&255u),(float)(p&255u)};
            for(int c=0;c<3;c++){float value=values[c],product=value*weights[t];sum[c]=sum[c]+product;if(value<mn[c])mn[c]=value;if(value>mx[c])mx[c]=value;}
        }
        for(int c=0;c<3;c++){float value=sum[c];if(value>mx[c])value=mx[c];if(value<=mn[c])value=mn[c];out[3*x+c]=value;}
    }
#endif
}
void speed1935_vertical(float *accum,float *minimum,float *maximum,const float *row,float weight,int count) {
    int i=0;
#if SIMD1935
    uint64_t old=enter_ieee();
    for(;i+4<=count;i+=4){float32x4_t value=vld1q_f32(row+i),a=vld1q_f32(accum+i),mn=vld1q_f32(minimum+i),mx=vld1q_f32(maximum+i);
        float32x4_t product=vmulq_n_f32(value,weight);a=vaddq_f32(a,product);
        mn=vbslq_f32(vcltq_f32(value,mn),value,mn);mx=vbslq_f32(vcgtq_f32(value,mx),value,mx);
        vst1q_f32(accum+i,a);vst1q_f32(minimum+i,mn);vst1q_f32(maximum+i,mx);}
#endif
    for(;i<count;i++){float value=row[i],product=value*weight;accum[i]=accum[i]+product;if(value<minimum[i])minimum[i]=value;if(value>maximum[i])maximum[i]=value;}
#if SIMD1935
    leave_ieee(old);
#endif
}
