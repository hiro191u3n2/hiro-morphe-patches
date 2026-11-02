/* Finite binary32 division with round-to-nearest, ties-to-even. Hardware
 * division supplies only a guess; every published quotient is verified with
 * exact integer products/remainders. Outlier guesses use integer long division.
 * NaN payload propagation is outside the finite contract; return a quiet NaN. */
bool divGreater1973(uint ah,uint al,uint bh,uint bl){return ah>bh||(ah==bh&&al>bl);}
uint divFloor1973(uint a,uint b,int shift){
    uint q=1u,r=a-b;
    for(int bit=0;bit<23;bit++)if(bit<shift){
        r<<=1;q<<=1;
        if(r>=b){r-=b;q|=1u;}
    }
    return q;
}
uint ieeeDivBits1973(uint ax,uint bx){
    uint sign=(ax^bx)&0x80000000u,a=ax&0x7fffffffu,b=bx&0x7fffffffu;
    if(a>0x7f800000u||b>0x7f800000u)return 0x7fc00000u;
    if(a==0x7f800000u)return b==0x7f800000u?0x7fc00000u:sign|0x7f800000u;
    if(b==0x7f800000u)return sign;
    if(b==0u)return a==0u?0x7fc00000u:sign|0x7f800000u;
    if(a==0u)return sign;
    int ea=int(a>>23)-127,eb=int(b>>23)-127;
    uint ma=(a&0x007fffffu)|0x00800000u,mb=(b&0x007fffffu)|0x00800000u;
    if((a>>23)==0u){int normal=23-findMSB(a);ma=a<<uint(normal);ea=-126-normal;}
    if((b>>23)==0u){int normal=23-findMSB(b);mb=b<<uint(normal);eb=-126-normal;}
    int exponent=ea-eb+127;
    if(ma<mb){ma<<=1;exponent--;}
    if(exponent>=255)return sign|0x7f800000u;
    if(exponent<-23)return sign;
    // Exactly half the minimum subnormal ties to zero. Greater ratios round
    // up; reduced precision is selected before rounding, never double-rounded.
    if(exponent==-23)return sign|(ma>mb?1u:0u);
    int shift=exponent>0?23:exponent+22;
    uint nh=shift==0?0u:ma>>uint(32-shift),nl=ma<<uint(shift);
    precise float guess=float(ma)/float(mb);
    precise float scaled=guess*uintBitsToFloat(uint(shift+127)<<23);
    uint q=uint(scaled);
    #ifdef GX_DIV_GUESS_PERTURB73
    q=uint(max(0,int(q)+GX_DIV_GUESS_PERTURB73));
    #endif
    uint ph=0u,pl=0u;
    bool verified=false;
    for(int correction=0;correction<8;correction++){
        umulExtended(mb,q,ph,pl);
        if(divGreater1973(ph,pl,nh,nl)){if(q==0u)break;q--;continue;}
        uint nextLo=pl+mb,nextHi=ph+(nextLo<pl?1u:0u);
        if(!divGreater1973(nextHi,nextLo,nh,nl)){q++;continue;}
        verified=true;break;
    }
    if(!verified){q=divFloor1973(ma,mb,shift);umulExtended(mb,q,ph,pl);}
    // The validated difference is <mb<2^24; modulo low-word subtraction is
    // exact even when the 48-bit subtraction borrows from its high word.
    uint remainder=nl-pl,twice=remainder<<1;
    if(twice>mb||(twice==mb&&(q&1u)!=0u))q++;
    if(exponent<=0)return sign|q;
    if(q==0x01000000u){q>>=1;exponent++;}
    if(exponent>=255)return sign|0x7f800000u;
    return sign|(uint(exponent)<<23)|(q&0x007fffffu);
}
float ieeeDiv1973(float a,float b){return uintBitsToFloat(ieeeDivBits1973(floatBitsToUint(a),floatBitsToUint(b)));}
