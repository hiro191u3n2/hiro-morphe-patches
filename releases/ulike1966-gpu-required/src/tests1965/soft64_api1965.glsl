/* Explicit public API. IEEE binary64 low word precedes high word. */
bool s64_eq(uvec2 a,uvec2 b){return __feq64(a,b);}
bool s64_lt(uvec2 a,uvec2 b){return __flt64(a,b);}
bool s64_le(uvec2 a,uvec2 b){return __fge64(b,a);}
bool s64_gt(uvec2 a,uvec2 b){return __flt64(b,a);}
bool s64_ge(uvec2 a,uvec2 b){return __fge64(a,b);}
bool s64_finite(uvec2 a){return (a.y&0x7ff00000u)!=0x7ff00000u;}
bool s64_zero(uvec2 a){return (a.x|(a.y&0x7fffffffu))==0u;}
uvec2 s64_neg(uvec2 a){return __fneg64(a);}
uvec2 s64_abs(uvec2 a){return __fabs64(a);}
uvec2 s64_add(uvec2 a,uvec2 b){return __fadd64(a,b);}
uvec2 s64_sub(uvec2 a,uvec2 b){return __fadd64(a,__fneg64(b));}
uvec2 s64_mul(uvec2 a,uvec2 b){return __fmul64(a,b);}
uvec2 s64_sqrt(uvec2 a){return __fsqrt64(a);}
uvec2 s64_min(uvec2 a,uvec2 b){
    if(__is_nan(a)||__is_nan(b))return __propagateFloat64NaN(a,b);
    return __flt64_nonnan_minmax(a,b)?a:b;
}
uvec2 s64_max(uvec2 a,uvec2 b){
    if(__is_nan(a)||__is_nan(b))return __propagateFloat64NaN(a,b);
    return __flt64_nonnan_minmax(a,b)?b:a;
}
uvec2 s64_trunc(uvec2 a){
    uvec2 z=__ftrunc64(a);
    return s64_zero(z)?uvec2(0u,a.y&0x80000000u):z;
}
uvec2 s64_floor(uvec2 a){return s64_zero(a)?a:__ffloor64(a);}
uvec2 s64_fromFloat(float a){return __fp32_to_fp64(a);}
float s64_toFloat(uvec2 a){return __fp64_to_fp32(a);}
uvec2 s64_fromInt(int a){return __int_to_fp64(a);}
uvec2 s64_fromUint(uint a){return __uint_to_fp64(a);}
int s64_toInt(uvec2 a){return __fp64_to_int(a);}

/* Exact restoring division of finite significands, with three rounding bits
 * plus sticky. Every loop uses uint32 pair arithmetic; no reciprocal estimate.
 * The normalized quotient [1,2) has 53 significant bits followed by G/R/S.
 * __roundAndPackFloat64 performs RNE and handles subnormal/overflow output. */
bool s64_wordsLess(uvec2 a,uvec2 b){return a.y<b.y||(a.y==b.y&&a.x<b.x);}
uvec2 s64_wordsShift(uvec2 a){return uvec2(a.x<<1,(a.y<<1)|(a.x>>31));}
uvec2 s64_wordsSubtract(uvec2 a,uvec2 b){return uvec2(a.x-b.x,a.y-b.y-uint(a.x<b.x));}
uvec2 s64_div(uvec2 a,uvec2 b){
    uint sign=(a.y^b.y)&0x80000000u;
    int ae=int((a.y>>20)&0x7ffu),be=int((b.y>>20)&0x7ffu);
    uint ahi=a.y&0xfffffu,bhi=b.y&0xfffffu,alo=a.x,blo=b.x;
    if(ae==0x7ff||be==0x7ff){
        if((ae==0x7ff&&(ahi|alo)!=0u)||(be==0x7ff&&(bhi|blo)!=0u))return __propagateFloat64NaN(a,b);
        if(ae==0x7ff&&be==0x7ff)return uvec2(0u,0x7ff80000u);
        return ae==0x7ff?uvec2(0u,sign|0x7ff00000u):uvec2(0u,sign);
    }
    bool az=(uint(ae)|ahi|alo)==0u,bz=(uint(be)|bhi|blo)==0u;
    if(az&&bz)return uvec2(0u,0x7ff80000u);
    if(bz)return uvec2(0u,sign|0x7ff00000u);
    if(az)return uvec2(0u,sign);
    if(ae==0)__normalizeFloat64Subnormal(ahi,alo,ae,ahi,alo);
    if(be==0)__normalizeFloat64Subnormal(bhi,blo,be,bhi,blo);
    ahi|=0x100000u;bhi|=0x100000u;
    uvec2 rem=uvec2(alo,ahi),den=uvec2(blo,bhi),quot=uvec2(0u);
    int exponent=ae-be+0x3fe;
    if(s64_wordsLess(rem,den)){rem=s64_wordsShift(rem);exponent--;}
    // quotient bits 55..0: leading bit, 52 fraction bits, three round bits.
    for(int bit=55;bit>=0;bit--){
        quot=s64_wordsShift(quot);
        if(!s64_wordsLess(rem,den)){rem=s64_wordsSubtract(rem,den);quot.x|=1u;}
        rem=s64_wordsShift(rem);
    }
    uint sticky=uint((rem.x|rem.y)!=0u);
    uint hi=quot.y>>3,lo=(quot.x>>3)|(quot.y<<29);
    uint extra=(quot.x&7u)<<29;extra|=sticky;
    return __roundAndPackFloat64(sign,exponent,hi,lo,extra);
}
