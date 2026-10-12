/* Host-only lane model for the exact intrinsics. ARM differential runs use the
 * cross compiler's real arm_neon.h, never this model. No target performance claim. */
#ifndef H28_HOST_NEON_H
#define H28_HOST_NEON_H
#include <stdint.h>
#include <string.h>
typedef uint32_t uint32x4_t __attribute__((vector_size(16)));
typedef int32_t int32x4_t __attribute__((vector_size(16)));
typedef double float64x2_t __attribute__((vector_size(16)));
static inline uint32x4_t vld1q_u32(const uint32_t *p){uint32x4_t v;memcpy(&v,p,16);return v;}
static inline int32x4_t vld1q_s32(const int32_t *p){int32x4_t v;memcpy(&v,p,16);return v;}
static inline float64x2_t vld1q_f64(const double *p){float64x2_t v;memcpy(&v,p,16);return v;}
static inline void vst1q_u32(uint32_t *p,uint32x4_t v){memcpy(p,&v,16);}
static inline void vst1q_s32(int32_t *p,int32x4_t v){memcpy(p,&v,16);}
static inline void vst1q_f64(double *p,float64x2_t v){memcpy(p,&v,16);}
static inline uint32x4_t vdupq_n_u32(uint32_t a){return (uint32x4_t){a,a,a,a};}
static inline int32x4_t vdupq_n_s32(int32_t a){return (int32x4_t){a,a,a,a};}
static inline float64x2_t vdupq_n_f64(double a){return (float64x2_t){a,a};}
static inline uint32x4_t vshrq_n_u32(uint32x4_t a,int n){for(int i=0;i<4;i++)a[i]>>=n;return a;}
static inline uint32x4_t vshlq_n_u32(uint32x4_t a,int n){for(int i=0;i<4;i++)a[i]<<=n;return a;}
static inline uint32x4_t vandq_u32(uint32x4_t a,uint32x4_t b){return a&b;}
static inline uint32x4_t vorrq_u32(uint32x4_t a,uint32x4_t b){return a|b;}
static inline uint32x4_t vaddq_u32(uint32x4_t a,uint32x4_t b){return a+b;}
static inline uint32x4_t vsubq_u32(uint32x4_t a,uint32x4_t b){return a-b;}
static inline uint32x4_t vmulq_n_u32(uint32x4_t a,uint32_t b){return a*vdupq_n_u32(b);}
static inline uint32x4_t vceqq_u32(uint32x4_t a,uint32x4_t b){uint32x4_t v;for(int i=0;i<4;i++)v[i]=a[i]==b[i]?UINT32_MAX:0;return v;}
static inline int32x4_t vreinterpretq_s32_u32(uint32x4_t a){int32x4_t v;memcpy(&v,&a,16);return v;}
static inline int32x4_t vaddq_s32(int32x4_t a,int32x4_t b){return a+b;}
static inline int32x4_t vsubq_s32(int32x4_t a,int32x4_t b){return a-b;}
static inline int32x4_t vmulq_s32(int32x4_t a,int32x4_t b){return a*b;}
static inline int32x4_t vabsq_s32(int32x4_t a){for(int i=0;i<4;i++)if(a[i]<0)a[i]=-a[i];return a;}
static inline int32x4_t vmaxq_s32(int32x4_t a,int32x4_t b){for(int i=0;i<4;i++)if(a[i]<b[i])a[i]=b[i];return a;}
static inline float64x2_t vaddq_f64(float64x2_t a,float64x2_t b){return a+b;}
static inline float64x2_t vsubq_f64(float64x2_t a,float64x2_t b){return a-b;}
static inline float64x2_t vmulq_f64(float64x2_t a,float64x2_t b){return a*b;}
static inline float64x2_t vmulq_n_f64(float64x2_t a,double b){return a*vdupq_n_f64(b);}
static inline float64x2_t vnegq_f64(float64x2_t a){return -a;}
#endif
