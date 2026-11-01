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
#define umulExtended(a,b,hi,lo) do{uint64_t p=(uint64_t)(a)*(b);(hi)=(uint)(p>>32);(lo)=(uint)p;}while(0)
#define max(a,b) ((a)>(b)?(a):(b))
#include "ieee_div1973_c.h"
static uint64_t cases,normal,fallback;static uint rng=0x18f779aeu;
static uint next(void){rng^=rng<<13;rng^=rng>>17;rng^=rng<<5;return rng;}
static void one(uint bits,uint shift){
    volatile float a=uintBitsToFloat(bits),b=uintBitsToFloat((shift+127u)<<23);
    float cpu=a/b;uint want=floatBitsToUint(cpu),actual=ieeePow2DivBits1978(bits,shift);cases++;
    uint exponent=(bits>>23)&255u;if(exponent>shift&&exponent<255u)normal++;else fallback++;
    if(isnan(cpu)){if((actual&0x7fffffffu)<=0x7f800000u)exit(2);return;}
    if(actual!=want){fprintf(stderr,"pow2 differs a=%08x n=%u cpu=%08x gpu=%08x\n",bits,shift,want,actual);exit(1);}
}
int main(void){
    if(fesetround(FE_TONEAREST))return 3;
    uint edges[]={0,0x80000000u,1,2,3,0x007fffffu,0x00800000u,0x00800001u,0x3f000000u,0x3f800000u,0x3f800001u,0x40000000u,0x4b7fffffu,0x7f7fffffu,0x7f800000u,0xff800000u,0x7fc00001u};
    for(uint n=0;n<=127;n++){
        for(unsigned i=0;i<sizeof(edges)/sizeof(*edges);i++){one(edges[i],n);one(edges[i]^0x80000000u,n);}
        uint boundary=(n+1u)<<23;
        for(int d=-1024;d<=1024;d++){one(boundary+(uint)d,n);one((boundary+(uint)d)^0x80000000u,n);}
    }
    for(int i=0;i<600000;i++){uint a=next(),n=next()&127u;one(a,n);}
    if(normal<1000||fallback<1000)return 4;
    printf("{\"status\":\"passed\",\"cases\":%llu,\"normal\":%llu,\"fallback\":%llu}\n",(unsigned long long)cases,(unsigned long long)normal,(unsigned long long)fallback);return 0;
}
