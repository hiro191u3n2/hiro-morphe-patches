#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fenv.h>
#include <math.h>
typedef uint32_t uint;
static int findMSB(uint v){return 31-__builtin_clz(v);}
static uint floatBitsToUint(float f){uint x;memcpy(&x,&f,4);return x;}
static float uintBitsToFloat(uint x){float f;memcpy(&f,&x,4);return f;}
#define precise
#define umulExtended(a,b,hi,lo) do{uint64_t p1973=(uint64_t)(a)*(b);(hi)=(uint)(p1973>>32);(lo)=(uint)p1973;}while(0)
#define max(a,b) ((a)>(b)?(a):(b))
#include "ieee_div1973_c.h"
static uint64_t cases;
static uint rng=0x91ce83f5u;
static uint next(void){rng^=rng<<13;rng^=rng>>17;rng^=rng<<5;return rng;}
static void one(uint a,uint b){
    volatile float af=uintBitsToFloat(a),bf=uintBitsToFloat(b);
    float cpu=af/bf;uint expected=floatBitsToUint(cpu),actual=ieeeDivBits1973(a,b);cases++;
    if(isnan(cpu)){if((actual&0x7fffffffu)<=0x7f800000u){fprintf(stderr,"NaN class differs\n");exit(1);}return;}
    if(actual!=expected){fprintf(stderr,"division differs a=%08x b=%08x cpu=%08x helper=%08x\n",a,b,expected,actual);exit(1);}
}
int main(void){
    if(fesetround(FE_TONEAREST))return 2;
    uint edges[]={0,0x80000000u,1,2,3,0x007fffffu,0x00800000u,0x00800001u,0x00ffffffu,0x3f000000u,0x3f000001u,0x3f7fffffu,0x3f800000u,0x3f800001u,0x3fc00000u,0x40000000u,0x4b7fffffu,0x7f000000u,0x7f7ffffeu,0x7f7fffffu,0x7f800000u,0xff800000u,0x7fc00001u};
    for(unsigned i=0;i<sizeof(edges)/sizeof(*edges);i++)for(unsigned j=0;j<sizeof(edges)/sizeof(*edges);j++){one(edges[i],edges[j]);one(edges[i]^0x80000000u,edges[j]);}
    // Exact subnormal midpoint ties, quotient normalization and overflow edges.
    for(uint m=1;m<=10000;m++){one(m,0x40000000u);one(m^0x80000000u,0x40000000u);one(0x007fffffu-m,0x3f7fffffu);}
    for(int i=0;i<300000;i++){uint a=next(),b=next();if((a&0x7fffffffu)>=0x7f800000u)a&=0xff7fffffu;if((b&0x7fffffffu)>=0x7f800000u)b&=0xff7fffffu;one(a,b);}
    printf("{\"status\":\"passed\",\"cases\":%llu}\n",(unsigned long long)cases);return 0;
}
