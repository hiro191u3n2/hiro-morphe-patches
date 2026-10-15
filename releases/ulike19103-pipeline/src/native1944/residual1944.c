#include "residual1944.h"
#if defined(__aarch64__) && !defined(ULIKE_RESIDUAL_SCALAR)
#include <arm_neon.h>
#define RESIDUAL_NEON1945 1
#else
#define RESIDUAL_NEON1945 0
#endif

static int mn(int a,int b){return a<b?a:b;}
static int mx(int a,int b){return a>b?a:b;}
static int ab(int a){return a<0?-a:a;}
static const int offsets[7]={-4,-2,-1,0,1,2,4};
static const int spatial[7]={1,2,3,4,3,2,1};
static const int wide[7]={1,3,5,6,5,3,1};
static uint32_t packed(uint32_t p){
    int r=(p>>16)&255,g=(p>>8)&255,b=p&255;
    return (uint32_t)((77*r+150*g+29*b+128)>>8)|
        (uint32_t)(r-g+255)<<8|(uint32_t)(b-g+255)<<17|
        (uint32_t)((p>>24)==255)<<26;
}
static void pack_row(const int32_t *src,int32_t *dst,int width){
    int col=0;
#if RESIDUAL_NEON1945
    const uint32x4_t mask=vdupq_n_u32(255);
    for(;col+4<=width;col+=4){
        uint32x4_t p=vld1q_u32((const uint32_t *)src+col);
        uint32x4_t r=vandq_u32(vshrq_n_u32(p,16),mask);
        uint32x4_t g=vandq_u32(vshrq_n_u32(p,8),mask),b=vandq_u32(p,mask);
        uint32x4_t y=vaddq_u32(vmlaq_n_u32(vmlaq_n_u32(vmulq_n_u32(r,77),g,150),b,29),vdupq_n_u32(128));
        uint32x4_t rg=vaddq_u32(vsubq_u32(r,g),mask),bg=vaddq_u32(vsubq_u32(b,g),mask);
        uint32x4_t opaque=vandq_u32(vceqq_u32(vshrq_n_u32(p,24),mask),vdupq_n_u32(1));
        uint32x4_t q=vorrq_u32(vorrq_u32(vshrq_n_u32(y,8),vshlq_n_u32(rg,8)),
            vorrq_u32(vshlq_n_u32(bg,17),vshlq_n_u32(opaque,26)));
        vst1q_u32((uint32_t *)dst+col,q);
    }
#endif
    for(;col<width;col++)dst[col]=(int32_t)packed((uint32_t)src[col]);
}
int residual1944_valid(const int32_t *meta,int meta_count,int width,int rows,
        int begin,int end,int lo,int hi,int radius,int input_count,int output_count,
        int range_count,int ring_count,int x_count){
    if(!meta || width<1 || rows<1 || radius<1 || radius>4 || lo<0 || hi>rows ||
       begin<lo || end<begin || end>hi || lo>=hi ||
       (int64_t)width*rows>input_count || range_count<33*256 ||
       (int64_t)width*(end-begin)>meta_count ||
       (int64_t)width*(end-begin)*3>output_count ||
       (int64_t)width*(radius*2+1)>ring_count ||
       (int64_t)width*(radius==4?7:radius*2+1)>x_count)return 0;
    int count=width*(end-begin);
    for(int i=0;i<count;i++)if((uint32_t)meta[i]>>31){
        int sigma=meta[i]&63,threshold=((uint32_t)meta[i]>>6)&63;
        if(sigma<2 || sigma>32 || threshold<3)return 0;
    }
    return 1;
}
static int texture(const int32_t *ring,int width,int x,int row,int lo,int hi,
        int threshold,int support,int slots){
    if(support<3 || x<support || x>=width-support || row<lo+support || row>=hi-support)return 0;
    int best=0,length=support*2+1,samples[9];
    for(int direction=0;direction<4;direction++){
        int dx=direction==0?1:direction==1?0:direction==2?1:-1;
        int dy=direction==0?0:1;
        int opaque=1,min=255,max=0,adjacent=0;
        for(int i=0;i<length;i++){
            int sy=row+(i-support)*dy,sx=x+(i-support)*dx;
            uint32_t p=(uint32_t)ring[(sy%slots)*width+sx];
            if(!(p&(1u<<26))){opaque=0;break;}
            int value=samples[i]=p&255;min=mn(min,value);max=mx(max,value);
            if(i>0)adjacent+=ab(value-samples[i-1]);
        }
        if(!opaque || max-min<threshold*2 || adjacent<(length-1)*threshold)continue;
        int average=(adjacent+(length-2)/2)/(length-1);
        for(int period=2;period<=mn(4,length-4);period++){
            int error=0,count=length-period;
            for(int i=0;i<count;i++)error+=ab(samples[i]-samples[i+period]);
            error=(error+count/2)/count;
            int confidence=mx(0,mn(256,(average*4-error*9)*256/mx(1,average*4)));
            confidence=confidence*confidence>>8;best=mx(best,confidence);
        }
    }
    return best;
}
/* Row addresses are resolved once, including the exact lo/hi edge clamp.
   Interior columns use contiguous addresses; edge columns retain the supplied
   x table and its original repeated-edge semantics. */
static void scalar_pixel(const int32_t *meta,int32_t *out,int col,int radius,
        const int32_t *range,const int32_t *x,const int32_t **pr,const int32_t **rgb,
        const int32_t *center,int interior,const int32_t *ring,int width,int row,
        int lo,int hi,int slots,int taps){
    int oi=col*3;out[oi+2]=0;
    if(!((uint32_t)meta[col]>>31))return;
    uint32_t cp=(uint32_t)center[col];if(!(cp&(1u<<26)))return;
    int yc=cp&255,sigma=meta[col]&63,threshold=((uint32_t)meta[col]>>6)&63;
    /* H30 is opt-in: bit30 is set only by the final-pixel residual caller.
       Ordinary aggregate clients retain all original summary words exactly. */
    int periodic=-1;
    if((uint32_t)meta[col]&(1u<<30)){
        periodic=texture(ring,width,col,row,lo,hi,threshold,radius,slots);
        if(periodic==256)return;
    }
    int weight=0,rr=0,gg=0,bb=0,nw=0,nr=0,ng=0,nb=0;
    int left=0,right=0,up=0,down=0,lc=0,rc=0,uc=0,dc=0,minY=yc,maxY=yc;
    int llimit=mn(56,8+sigma*3),climit=mn(48,8+sigma*2);
    for(int yi=0;yi<taps;yi++){
        int dy=radius==4?offsets[yi]:yi-radius;
        for(int xi=0;xi<taps;xi++){
            int dx=radius==4?offsets[xi]:xi-radius,sx=interior?col+dx:x[col*taps+xi];
            uint32_t p=(uint32_t)pr[yi][sx];if(!(p&(1u<<26)))continue;
            int yy=p&255;
            if(dx<0){left+=yy;lc++;}else if(dx>0){right+=yy;rc++;}
            if(dy<0){up+=yy;uc++;}else if(dy>0){down+=yy;dc++;}
            minY=mn(minY,yy);maxY=mx(maxY,yy);
            int ld=ab(yy-yc),cd=mx(ab((int)((p>>8)&511)-(int)((cp>>8)&511)),
                ab((int)((p>>17)&511)-(int)((cp>>17)&511)));
            if(ld>llimit || cd>climit)continue;
            int w=(radius==4?wide[xi]*wide[yi]:spatial[dx+3]*spatial[dy+3])*range[sigma*256+ld];
            if(cd>8)w=w*8/cd;
            if(w==0)continue;
            uint32_t p0=(uint32_t)rgb[yi][sx];int r=(p0>>16)&255,g=(p0>>8)&255,b=p0&255;
            weight+=w;rr+=w*r;gg+=w*g;bb+=w*b;
            if(ab(dx)<=1 && ab(dy)<=1){nw+=w;nr+=w*r;ng+=w*g;nb+=w*b;}
        }
    }
    if(weight==0 || nw==0)return;
    int edge=mx(lc==0||rc==0?0:ab(left/lc-right/rc),uc==0||dc==0?0:ab(up/uc-down/dc));
    if(periodic<0)periodic=texture(ring,width,col,row,lo,hi,threshold,radius,slots);
    int r1=(nr+nw/2)/nw,g1=(ng+nw/2)/nw,b1=(nb+nw/2)/nw;
    int r2=(rr+weight/2)/weight,g2=(gg+weight/2)/weight,b2=(bb+weight/2)/weight;
    out[oi]=(r1<<16)|(g1<<8)|b1;out[oi+1]=(r2<<16)|(g2<<8)|b2;
    out[oi+2]=(int32_t)(0x80000000u|(uint32_t)periodic<<16|(uint32_t)edge<<8|(uint32_t)(maxY-minY));
}

#if RESIDUAL_NEON1945
/* Four independent centers keep the original yi/xi tap order. Integer NEON
   performs masks/differences/sums; lookup and division remain exact scalar
   operations rather than approximate reciprocals or floating conversion. */
static void neon_pixels(const int32_t *meta,int32_t *out,int col,int radius,
        const int32_t *range,const int32_t **pr,const int32_t **rgb,
        const int32_t *center,const int32_t *ring,int width,int row,int lo,int hi,
        int slots,int taps){
    const uint32x4_t mask255=vdupq_n_u32(255),mask511=vdupq_n_u32(511),zero=vdupq_n_u32(0);
    uint32x4_t m=vld1q_u32((const uint32_t *)meta+col),cp=vld1q_u32((const uint32_t *)center+col);
    uint32x4_t active=vandq_u32(vceqq_u32(vshrq_n_u32(m,31),vdupq_n_u32(1)),
        vceqq_u32(vandq_u32(cp,vdupq_n_u32(1u<<26)),vdupq_n_u32(1u<<26)));
    for(int lane=0;lane<4;lane++)out[(col+lane)*3+2]=0;
    if(vmaxvq_u32(active)==0)return;
    uint32_t sigma[4],live[4];vst1q_u32(sigma,vandq_u32(m,vdupq_n_u32(63)));vst1q_u32(live,active);
    int periodic[4]={-1,-1,-1,-1};
    for(int lane=0;lane<4;lane++)if(live[lane] && ((uint32_t)meta[col+lane]&(1u<<30))){
        periodic[lane]=texture(ring,width,col+lane,row,lo,hi,((uint32_t)meta[col+lane]>>6)&63,radius,slots);
        if(periodic[lane]==256)live[lane]=0;
    }
    active=vld1q_u32(live);
    if(vmaxvq_u32(active)==0)return;
    for(int lane=0;lane<4;lane++)if(!live[lane])sigma[lane]=2;
    uint32x4_t sig=vld1q_u32(sigma),yc=vandq_u32(cp,mask255);
    uint32x4_t crc=vandq_u32(vshrq_n_u32(cp,8),mask511),cbc=vandq_u32(vshrq_n_u32(cp,17),mask511);
    uint32x4_t ll=vminq_u32(vdupq_n_u32(56),vaddq_u32(vmulq_n_u32(sig,3),vdupq_n_u32(8)));
    uint32x4_t cl=vminq_u32(vdupq_n_u32(48),vaddq_u32(vmulq_n_u32(sig,2),vdupq_n_u32(8)));
    uint32x4_t weight=zero,rr=zero,gg=zero,bb=zero,nw=zero,nr=zero,ng=zero,nb=zero;
    uint32x4_t left=zero,right=zero,up=zero,down=zero,lc=zero,rc=zero,uc=zero,dc=zero,minY=yc,maxY=yc;
    for(int yi=0;yi<taps;yi++){
        int dy=radius==4?offsets[yi]:yi-radius;
        for(int xi=0;xi<taps;xi++){
            int dx=radius==4?offsets[xi]:xi-radius;
            uint32x4_t p=vld1q_u32((const uint32_t *)pr[yi]+col+dx);
            uint32x4_t opaque=vceqq_u32(vandq_u32(p,vdupq_n_u32(1u<<26)),vdupq_n_u32(1u<<26));
            uint32x4_t yy=vandq_u32(p,mask255),val=vandq_u32(yy,opaque),count=vshrq_n_u32(opaque,31);
            if(dx<0){left=vaddq_u32(left,val);lc=vaddq_u32(lc,count);}
            else if(dx>0){right=vaddq_u32(right,val);rc=vaddq_u32(rc,count);}
            if(dy<0){up=vaddq_u32(up,val);uc=vaddq_u32(uc,count);}
            else if(dy>0){down=vaddq_u32(down,val);dc=vaddq_u32(dc,count);}
            minY=vbslq_u32(opaque,vminq_u32(minY,yy),minY);maxY=vbslq_u32(opaque,vmaxq_u32(maxY,yy),maxY);
            uint32x4_t ld=vabdq_u32(yy,yc);
            uint32x4_t cd=vmaxq_u32(vabdq_u32(vandq_u32(vshrq_n_u32(p,8),mask511),crc),
                vabdq_u32(vandq_u32(vshrq_n_u32(p,17),mask511),cbc));
            uint32x4_t eligible=vandq_u32(vandq_u32(active,opaque),vandq_u32(vcleq_u32(ld,ll),vcleq_u32(cd,cl)));
            /* Edge/range statistics above still include every opaque tap.
               Only the already rejected weighted RGB work is omitted. */
            if(vmaxvq_u32(eligible)==0)continue;
            uint32_t lds[4],cds[4],ok[4],ws[4];
            vst1q_u32(lds,ld);vst1q_u32(cds,cd);vst1q_u32(ok,eligible);
            int spatial_weight=radius==4?wide[xi]*wide[yi]:spatial[dx+3]*spatial[dy+3];
            for(int lane=0;lane<4;lane++){
                uint32_t w=ok[lane]?(uint32_t)(spatial_weight*range[sigma[lane]*256+lds[lane]]):0;
                if(w!=0 && cds[lane]>8)w=w*8/cds[lane];
                ws[lane]=w;
            }
            uint32x4_t w=vld1q_u32(ws),p0=vld1q_u32((const uint32_t *)rgb[yi]+col+dx);
            uint32x4_t r=vandq_u32(vshrq_n_u32(p0,16),mask255),g=vandq_u32(vshrq_n_u32(p0,8),mask255),b=vandq_u32(p0,mask255);
            weight=vaddq_u32(weight,w);rr=vmlaq_u32(rr,w,r);gg=vmlaq_u32(gg,w,g);bb=vmlaq_u32(bb,w,b);
            if(ab(dx)<=1 && ab(dy)<=1){nw=vaddq_u32(nw,w);nr=vmlaq_u32(nr,w,r);ng=vmlaq_u32(ng,w,g);nb=vmlaq_u32(nb,w,b);}
        }
    }
    uint32_t sums[18][4];
    vst1q_u32(sums[0],weight);vst1q_u32(sums[1],rr);vst1q_u32(sums[2],gg);vst1q_u32(sums[3],bb);
    vst1q_u32(sums[4],nw);vst1q_u32(sums[5],nr);vst1q_u32(sums[6],ng);vst1q_u32(sums[7],nb);
    vst1q_u32(sums[8],left);vst1q_u32(sums[9],right);vst1q_u32(sums[10],up);vst1q_u32(sums[11],down);
    vst1q_u32(sums[12],lc);vst1q_u32(sums[13],rc);vst1q_u32(sums[14],uc);vst1q_u32(sums[15],dc);
    vst1q_u32(sums[16],minY);vst1q_u32(sums[17],maxY);
    for(int lane=0;lane<4;lane++){
        int weight0=sums[0][lane],nw0=sums[4][lane];if(!live[lane] || weight0==0 || nw0==0)continue;
        int horizontal=sums[12][lane]==0||sums[13][lane]==0?0:ab((int)(sums[8][lane]/sums[12][lane])-(int)(sums[9][lane]/sums[13][lane]));
        int vertical=sums[14][lane]==0||sums[15][lane]==0?0:ab((int)(sums[10][lane]/sums[14][lane])-(int)(sums[11][lane]/sums[15][lane]));
        int periodic0=periodic[lane]<0?texture(ring,width,col+lane,row,lo,hi,((uint32_t)meta[col+lane]>>6)&63,radius,slots):periodic[lane];
        int oi=(col+lane)*3;
        out[oi]=((sums[5][lane]+nw0/2)/nw0)<<16|((sums[6][lane]+nw0/2)/nw0)<<8|(sums[7][lane]+nw0/2)/nw0;
        out[oi+1]=((sums[1][lane]+weight0/2)/weight0)<<16|((sums[2][lane]+weight0/2)/weight0)<<8|(sums[3][lane]+weight0/2)/weight0;
        out[oi+2]=(int32_t)(0x80000000u|(uint32_t)periodic0<<16|(uint32_t)mx(horizontal,vertical)<<8|(sums[17][lane]-sums[16][lane]));
    }
}
#endif

void residual1944_aggregate(const int32_t *src,const int32_t *meta,int32_t *out,
        int width,int begin,int end,int lo,int hi,int radius,const int32_t *range,
        int32_t *ring,const int32_t *x){
    int slots=radius*2+1,taps=radius==4?7:slots,tags[9],canonical=1;
    /* Keep the retained C/JNI contract for noncanonical but in-bounds x tables.
       Production tables are canonical; check once per tile, not per row/tap. */
    for(int col=radius;col<width-radius && canonical;col++)for(int xi=0;xi<taps;xi++){
        int dx=radius==4?offsets[xi]:xi-radius;
        if(x[col*taps+xi]!=col+dx){canonical=0;break;}
    }
    for(int i=0;i<slots;i++)tags[i]=-1;
    for(int row=begin;row<end;row++){
        for(int sy=mx(lo,row-radius);sy<=mn(hi-1,row+radius);sy++){
            int slot=sy%slots;if(tags[slot]==sy)continue;
            pack_row(src+sy*width,ring+slot*width,width);tags[slot]=sy;
        }
        const int32_t *pr[7],*rgb[7],*center=ring+(row%slots)*width;
        int vertical_interior=row>=lo+radius && row<hi-radius;
        for(int yi=0;yi<taps;yi++){
            int dy=radius==4?offsets[yi]:yi-radius,sy=vertical_interior?row+dy:mx(lo,mn(hi-1,row+dy));
            pr[yi]=ring+(sy%slots)*width;rgb[yi]=src+sy*width;
        }
        const int32_t *m=meta+(row-begin)*width;int32_t *o=out+(row-begin)*width*3;
        int col=0,first=canonical?mn(radius,width):width,last=canonical?mx(first,width-radius):width;
        for(;col<first;col++)scalar_pixel(m,o,col,radius,range,x,pr,rgb,center,0,ring,width,row,lo,hi,slots,taps);
#if RESIDUAL_NEON1945
        for(;col+4<=last;col+=4)neon_pixels(m,o,col,radius,range,pr,rgb,center,ring,width,row,lo,hi,slots,taps);
#endif
        for(;col<last;col++)scalar_pixel(m,o,col,radius,range,x,pr,rgb,center,1,ring,width,row,lo,hi,slots,taps);
        for(;col<width;col++)scalar_pixel(m,o,col,radius,range,x,pr,rgb,center,0,ring,width,row,lo,hi,slots,taps);
    }
}
