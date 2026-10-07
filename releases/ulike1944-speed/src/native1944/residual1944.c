#include "residual1944.h"

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
void residual1944_aggregate(const int32_t *src,const int32_t *meta,int32_t *out,
        int width,int begin,int end,int lo,int hi,int radius,const int32_t *range,
        int32_t *ring,const int32_t *x){
    int slots=radius*2+1,taps=radius==4?7:slots,tags[9];
    for(int i=0;i<slots;i++)tags[i]=-1;
    for(int row=begin;row<end;row++){
        /* A ring retains overlap across rows; all texture directions use these
           same exact luma values. Sparse radius4 still fills its inner halo. */
        for(int sy=mx(lo,row-radius);sy<=mn(hi-1,row+radius);sy++){
            int slot=sy%slots;if(tags[slot]==sy)continue;
            for(int col=0;col<width;col++)ring[slot*width+col]=(int32_t)packed((uint32_t)src[sy*width+col]);
            tags[slot]=sy;
        }
        for(int col=0;col<width;col++){
            int i=(row-begin)*width+col,oi=i*3;out[oi+2]=0;
            if(!((uint32_t)meta[i]>>31))continue;
            uint32_t center=(uint32_t)src[row*width+col];if((center>>24)!=255)continue;
            uint32_t cp=(uint32_t)ring[(row%slots)*width+col];
            int yc=cp&255;
            int sigma=meta[i]&63,threshold=((uint32_t)meta[i]>>6)&63;
            int weight=0,rr=0,gg=0,bb=0,nw=0,nr=0,ng=0,nb=0;
            int left=0,right=0,up=0,down=0,lc=0,rc=0,uc=0,dc=0,minY=yc,maxY=yc;
            int llimit=mn(56,8+sigma*3),climit=mn(48,8+sigma*2);
            for(int yi=0;yi<taps;yi++){
                int dy=radius==4?offsets[yi]:yi-radius,sy=mx(lo,mn(hi-1,row+dy));
                for(int xi=0;xi<taps;xi++){
                    int dx=radius==4?offsets[xi]:xi-radius,sx=x[col*taps+xi];
                    uint32_t p=(uint32_t)ring[(sy%slots)*width+sx];
                    if(!(p&(1u<<26)))continue;
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
                    uint32_t rgb=(uint32_t)src[sy*width+sx];int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
                    weight+=w;rr+=w*r;gg+=w*g;bb+=w*b;
                    if(ab(dx)<=1 && ab(dy)<=1){nw+=w;nr+=w*r;ng+=w*g;nb+=w*b;}
                }
            }
            if(weight==0 || nw==0)continue;
            int edge=mx(lc==0||rc==0?0:ab(left/lc-right/rc),uc==0||dc==0?0:ab(up/uc-down/dc));
            int periodic=texture(ring,width,col,row,lo,hi,threshold,radius,slots);
            int r1=(nr+nw/2)/nw,g1=(ng+nw/2)/nw,b1=(nb+nw/2)/nw;
            int r2=(rr+weight/2)/weight,g2=(gg+weight/2)/weight,b2=(bb+weight/2)/weight;
            out[oi]=(r1<<16)|(g1<<8)|b1;out[oi+1]=(r2<<16)|(g2<<8)|b2;
            out[oi+2]=(int32_t)(0x80000000u|(uint32_t)periodic<<16|(uint32_t)edge<<8|(uint32_t)(maxY-minY));
        }
    }
}
