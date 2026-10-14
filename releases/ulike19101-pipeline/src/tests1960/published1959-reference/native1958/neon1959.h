/* H42: one independently ordered pixel in each AArch64 NEON lane. No
 * horizontal reduction, approximate reciprocal, FMA or tap reordering. */
#ifndef ULIKE_NEON1959_H
#define ULIKE_NEON1959_H
#if defined(__aarch64__) && !defined(ULIKE_DISABLE_NEON1959)
typedef float32x4_t V1959;
typedef struct {V1959 y,r,b;uint32x4_t opaque;} VC1959;
static V1959 vf1959(float a){return vdupq_n_f32(a);}
static V1959 va1959(V1959 a,V1959 b){return vaddq_f32(a,b);}
static V1959 vs1959(V1959 a,V1959 b){return vsubq_f32(a,b);}
static V1959 vm1959(V1959 a,V1959 b){return vmulq_f32(a,b);}
static V1959 vd1959(V1959 a,V1959 b){return vdivq_f32(a,b);}
static V1959 vmax1959(V1959 a,V1959 b){return vbslq_f32(vcgtq_f32(a,b),a,b);}
static V1959 vabs1959(V1959 a){return vabsq_f32(a);}
static V1959 vsq1959(V1959 a){return vm1959(a,a);}
static VC1959 vgather1959(const Region *r,int x,int y){
    float yy[4],rr[4],bb[4];uint32_t opaque[4];
    for(int i=0;i<4;i++){Color c=color(r,x+i,y);yy[i]=c.y;rr[i]=c.r;bb[i]=c.b;opaque[i]=(pixel(r,x+i,y)>>24)==255?UINT32_MAX:0;}
    VC1959 c={vld1q_f32(yy),vld1q_f32(rr),vld1q_f32(bb),vld1q_u32(opaque)};return c;
}
static void vstore1959(PixelWork1959 *out,V1959 value,int field){
    float lanes[4];vst1q_f32(lanes,value);
    for(int i=0;i<4;i++)switch(field){case 0:out[i].flat[0]=lanes[i];break;case 1:out[i].flat[1]=lanes[i];break;case 2:out[i].legacy_flat=lanes[i];break;case 3:out[i].sum_y=lanes[i];break;case 4:out[i].sum_r=lanes[i];break;case 5:out[i].sum_b=lanes[i];break;case 6:out[i].wy=lanes[i];break;case 7:out[i].wc=lanes[i];break;case 8:out[i].var=lanes[i];break;}
}
#ifdef ULIKE_TEST_NEON1959
static uint64_t neon_blocks1959;
#endif
static void neon_work1959(const Region *src,int x,int y,int valid_begin,int valid_end,int origin_y,int full_height,int first_grid_y,const float *ev,int mode,PixelWork1959 *out){
#ifdef ULIKE_TEST_NEON1959
    neon_blocks1959++;
#endif
    float ay[4],ac[4];VC1959 center=vgather1959(src,x,y);
    float centers[4];vst1q_f32(centers,center.y);
    for(int i=0;i<4;i++){
        ay[i]=mode==3?regional_sigma(ev,src->width,full_height,first_grid_y,x+i,y+origin_y,0):sigma(ev,0,centers[i]);
        ac[i]=mode==3?regional_sigma(ev,src->width,full_height,first_grid_y,x+i,y+origin_y,1):sigma(ev,8,centers[i]);out[i].sy=ay[i];out[i].sc=ac[i];
    }
    V1959 sy=vld1q_f32(ay),sc=vld1q_f32(ac),one=vf1959(1),zero=vf1959(0);
    V1959 sides[12];for(int i=0;i<12;i++)sides[i]=zero;
    uint32x4_t opaque=vdupq_n_u32(UINT32_MAX);
    for(int k=-1;k<=1;k++)for(int d=2;d<=3;d++){
        VC1959 c[4]={vgather1959(src,x-d,maxi(valid_begin,mini(valid_end-1,y+k))),vgather1959(src,x+d,maxi(valid_begin,mini(valid_end-1,y+k))),vgather1959(src,x+k,maxi(valid_begin,mini(valid_end-1,y-d))),vgather1959(src,x+k,maxi(valid_begin,mini(valid_end-1,y+d)))};
        for(int i=0;i<4;i++){opaque=vandq_u32(opaque,c[i].opaque);sides[i]=va1959(sides[i],c[i].y);sides[4+i]=va1959(sides[4+i],c[i].r);sides[8+i]=va1959(sides[8+i],c[i].b);}
    }
    V1959 cy=zero,cr=zero,cb=zero;
    for(int py=-1;py<=1;py++)for(int px=-1;px<=1;px++){
        VC1959 c=vgather1959(src,x+px,maxi(valid_begin,mini(valid_end-1,y+py)));opaque=vandq_u32(opaque,c.opaque);cy=va1959(cy,c.y);cr=va1959(cr,c.r);cb=va1959(cb,c.b);
    }
    V1959 cy43=vd1959(vm1959(cy,vf1959(4)),vf1959(3)),cr43=vd1959(vm1959(cr,vf1959(4)),vf1959(3)),cb43=vd1959(vm1959(cb,vf1959(4)),vf1959(3));
    V1959 curve_y=vd1959(vmax1959(vabs1959(vs1959(va1959(sides[0],sides[1]),cy43)),vabs1959(vs1959(va1959(sides[2],sides[3]),cy43))),vf1959(6));
    V1959 curve_c=vd1959(vmax1959(va1959(vabs1959(vs1959(va1959(sides[4],sides[5]),cr43)),vabs1959(vs1959(va1959(sides[8],sides[9]),cb43))),va1959(vabs1959(vs1959(va1959(sides[6],sides[7]),cr43)),vabs1959(vs1959(va1959(sides[10],sides[11]),cb43)))),vf1959(6));
    V1959 edge_y=vd1959(vmax1959(vabs1959(vs1959(sides[1],sides[0])),vabs1959(vs1959(sides[3],sides[2]))),vf1959(6));
    V1959 edge_c=vd1959(vmax1959(va1959(vabs1959(vs1959(sides[5],sides[4])),vabs1959(vs1959(sides[9],sides[8]))),va1959(vabs1959(vs1959(sides[7],sides[6])),vabs1959(vs1959(sides[11],sides[10])))),vf1959(6));
    edge_y=vmax1959(zero,vs1959(edge_y,vm1959(sy,vf1959(.85f))));edge_c=vmax1959(zero,vs1959(edge_c,vm1959(sc,vf1959(2))));
    V1959 flat_y=vd1959(one,va1959(va1959(one,vsq1959(vd1959(edge_y,va1959(vm1959(sy,vf1959(1.7f)),vf1959(2))))),vsq1959(vd1959(vmax1959(zero,vs1959(curve_y,vm1959(sy,vf1959(1.4f)))),va1959(vm1959(sy,vf1959(.85f)),one)))));
    V1959 flat_c=vd1959(one,va1959(va1959(one,vsq1959(vd1959(edge_c,va1959(vm1959(sc,one),vf1959(1.5f))))),vsq1959(vd1959(vmax1959(zero,vs1959(curve_c,vm1959(sc,vf1959(2.5f)))),va1959(vm1959(sc,vf1959(1.5f)),vf1959(2))))));
    vstore1959(out,vbslq_f32(opaque,flat_y,zero),0);vstore1959(out,vbslq_f32(opaque,flat_c,zero),1);
    if(mode!=3){
        V1959 axis[4]={zero,zero,zero,zero};uint32x4_t ok=vdupq_n_u32(UINT32_MAX);
        for(int k=-1;k<=1;k++){
            VC1959 c[4]={vgather1959(src,x-2,maxi(valid_begin,mini(valid_end-1,y+k))),vgather1959(src,x+2,maxi(valid_begin,mini(valid_end-1,y+k))),vgather1959(src,x+k,maxi(valid_begin,mini(valid_end-1,y-2))),vgather1959(src,x+k,maxi(valid_begin,mini(valid_end-1,y+2)))};
            for(int i=0;i<4;i++){ok=vandq_u32(ok,c[i].opaque);axis[i]=va1959(axis[i],c[i].y);}
        }
        V1959 st=vd1959(va1959(vabs1959(vs1959(axis[1],axis[0])),vabs1959(vs1959(axis[3],axis[2]))),vf1959(3));st=vbslq_f32(ok,st,vf1959(255));
        vstore1959(out,vd1959(one,va1959(one,vsq1959(vd1959(st,va1959(vm1959(sy,vf1959(3)),vf1959(3)))))),2);
    }
    V1959 ty=vmax1959(vf1959(mode==3?2.5f:3),vm1959(sy,vf1959(mode==3?4:4.5f))),tc=vmax1959(vf1959(5),vm1959(sc,vf1959(mode==3?5:5.5f)));
    V1959 sum_y=zero,sum_r=zero,sum_b=zero,wy_total=zero,wc_total=zero,var=zero;
    for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){
        VC1959 q=vgather1959(src,x+dx,maxi(valid_begin,mini(valid_end-1,y+dy)));
        V1959 delta=vs1959(q.y,center.y),dy2=vsq1959(vd1959(delta,ty));
        V1959 base=vf1959(dx==0&&dy==0?4:abs(dx)<=1&&abs(dy)<=1?2:1);
        V1959 wy=vd1959(base,va1959(one,dy2));
        V1959 wc=vd1959(base,va1959(va1959(va1959(one,vm1959(vf1959(.12f),dy2)),vsq1959(vd1959(vs1959(q.r,center.r),tc))),vsq1959(vd1959(vs1959(q.b,center.b),tc))));
        sum_y=vbslq_f32(q.opaque,va1959(sum_y,vm1959(wy,q.y)),sum_y);wy_total=vbslq_f32(q.opaque,va1959(wy_total,wy),wy_total);var=vbslq_f32(q.opaque,va1959(var,vm1959(wy,vsq1959(delta))),var);
        sum_r=vbslq_f32(q.opaque,va1959(sum_r,vm1959(wc,q.r)),sum_r);sum_b=vbslq_f32(q.opaque,va1959(sum_b,vm1959(wc,q.b)),sum_b);wc_total=vbslq_f32(q.opaque,va1959(wc_total,wc),wc_total);
    }
    vstore1959(out,sum_y,3);vstore1959(out,sum_r,4);vstore1959(out,sum_b,5);vstore1959(out,wy_total,6);vstore1959(out,wc_total,7);vstore1959(out,var,8);
}
#endif
#endif
