#define _GNU_SOURCE
#include "residual1944.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

/* Independent direct-source oracle: no packed ring, SIMD, or row-address cache.
   Both canonical and arbitrary in-bounds retained x tables are exercised. */
static const int off[7]={-4,-2,-1,0,1,2,4},sp[7]={1,2,3,4,3,2,1},wide[7]={1,3,5,6,5,3,1};
static int min(int a,int b){return a<b?a:b;}
static int max(int a,int b){return a>b?a:b;}
static int abs0(int a){return a<0?-a:a;}
static int luminance(uint32_t p){return (77*((p>>16)&255)+150*((p>>8)&255)+29*(p&255)+128)>>8;}
static unsigned random_state=19451007;
static unsigned random0(void){random_state^=random_state<<13;random_state^=random_state>>17;random_state^=random_state<<5;return random_state;}
static long long checks,pixels;static int cases;
static void require(int ok,const char *why){checks++;if(!ok){fprintf(stderr,"FAIL %s case=%d\n",why,cases);exit(1);}}
typedef struct {void *base;size_t bytes;int32_t *p;} Guard;
static Guard alloc_guard(size_t count){
    size_t page=(size_t)sysconf(_SC_PAGESIZE),bytes=count*4,capacity=((bytes+page-1)/page)*page;
    void *p=mmap(0,capacity+page,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);
    require(p!=MAP_FAILED,"mmap");require(mprotect((char *)p+capacity,page,PROT_NONE)==0,"mprotect");
    Guard result={p,capacity+page,(int32_t *)((char *)p+capacity-bytes)};return result;
}
static void free_guard(Guard a){require(munmap(a.base,a.bytes)==0,"munmap");}
static int periodic(const int32_t *src,int width,int col,int row,int lo,int hi,int threshold,int radius){
    if(radius<3 || col<radius || col>=width-radius || row<lo+radius || row>=hi-radius)return 0;
    int best=0,length=radius*2+1,values[9];
    for(int direction=0;direction<4;direction++){
        int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
        int opaque=1,mn=255,mx=0,adjacent=0;
        for(int j=0;j<length;j++){
            uint32_t p=(uint32_t)src[row*width+col+(j-radius)*step];
            if((p>>24)!=255){opaque=0;break;}
            int y=values[j]=luminance(p);mn=min(mn,y);mx=max(mx,y);
            if(j>0)adjacent+=abs0(y-values[j-1]);
        }
        if(!opaque || mx-mn<threshold*2 || adjacent<(length-1)*threshold)continue;
        int average=(adjacent+(length-2)/2)/(length-1);
        for(int period=2;period<=min(4,length-4);period++){
            int error=0,n=length-period;
            for(int j=0;j<n;j++)error+=abs0(values[j]-values[j+period]);
            error=(error+n/2)/n;
            int confidence=max(0,min(256,(average*4-error*9)*256/max(1,average*4)));
            best=max(best,confidence*confidence>>8);
        }
    }
    return best;
}
static void oracle(const int32_t *src,const int32_t *meta,int32_t *out,int width,
        int begin,int end,int lo,int hi,int radius,const int32_t *range,const int32_t *x){
    int taps=radius==4?7:radius*2+1;
    for(int row=begin;row<end;row++)for(int col=0;col<width;col++){
        int i=(row-begin)*width+col;out[i*3+2]=0;
        if(meta[i]>=0)continue;
        uint32_t center=(uint32_t)src[row*width+col];if((center>>24)!=255)continue;
        int yc=luminance(center),cr=(center>>16)&255,cg=(center>>8)&255,cb=center&255;
        int sigma=meta[i]&63,weight=0,rr=0,gg=0,bb=0,nw=0,nr=0,ng=0,nb=0;
        int l=0,r=0,u=0,d=0,lc=0,rc=0,uc=0,dc=0,mn=yc,mx=yc;
        for(int yi=0;yi<taps;yi++){
            int dy=radius==4?off[yi]:yi-radius,sy=max(lo,min(hi-1,row+dy));
            for(int xi=0;xi<taps;xi++){
                int dx=radius==4?off[xi]:xi-radius;uint32_t p=(uint32_t)src[sy*width+x[col*taps+xi]];
                if((p>>24)!=255)continue;
                int yy=luminance(p),red=(p>>16)&255,green=(p>>8)&255,blue=p&255;
                if(dx<0){l+=yy;lc++;}else if(dx>0){r+=yy;rc++;}
                if(dy<0){u+=yy;uc++;}else if(dy>0){d+=yy;dc++;}
                mn=min(mn,yy);mx=max(mx,yy);
                int ld=abs0(yy-yc),cd=max(abs0((red-green)-(cr-cg)),abs0((blue-green)-(cb-cg)));
                if(ld>min(56,8+sigma*3) || cd>min(48,8+sigma*2))continue;
                int w=(radius==4?wide[xi]*wide[yi]:sp[dx+3]*sp[dy+3])*range[sigma*256+ld];
                if(cd>8)w=w*8/cd;
                if(w==0)continue;
                weight+=w;rr+=w*red;gg+=w*green;bb+=w*blue;
                if(abs0(dx)<=1 && abs0(dy)<=1){nw+=w;nr+=w*red;ng+=w*green;nb+=w*blue;}
            }
        }
        if(weight==0 || nw==0)continue;
        int edge=max(lc==0||rc==0?0:abs0(l/lc-r/rc),uc==0||dc==0?0:abs0(u/uc-d/dc));
        int p=periodic(src,width,col,row,lo,hi,((uint32_t)meta[i]>>6)&63,radius);
        out[i*3]=((nr+nw/2)/nw)<<16|((ng+nw/2)/nw)<<8|(nb+nw/2)/nw;
        out[i*3+1]=((rr+weight/2)/weight)<<16|((gg+weight/2)/weight)<<8|(bb+weight/2)/weight;
        out[i*3+2]=(int32_t)(0x80000000u|(uint32_t)p<<16|(uint32_t)edge<<8|(uint32_t)(mx-mn));
    }
}
static void fixture(int width,int rows,int radius,int flavor,int weird_x){
    int lo=flavor%4==0?0:(int)(random0()%rows),hi=lo+1+(int)(random0()%(rows-lo));
    if(flavor%4==0)hi=rows;
    int begin=lo+(int)(random0()%(hi-lo)),end=begin+1+(int)(random0()%(hi-begin));
    if(flavor%3==0){begin=lo;end=hi;}
    int count=width*(end-begin),taps=radius==4?7:radius*2+1;
    Guard sg=alloc_guard(width*rows),mg=alloc_guard(count),og=alloc_guard(count*3),
        rg=alloc_guard(width*(radius*2+1)),xg=alloc_guard(width*taps),tg=alloc_guard(33*256);
    int32_t *src=sg.p,*meta=mg.p,*actual=og.p,*ring=rg.p,*x=xg.p,*range=tg.p;
    int32_t *expected=malloc((size_t)count*12),*before=malloc((size_t)width*rows*4);
    require(expected!=0 && before!=0,"malloc");
    for(int i=0;i<width*rows;i++){
        unsigned yy=flavor%5==0?64+random0()%21:flavor%5==1?(i%width%2?82:65):random0()%256;
        if(flavor%5<=1)src[i]=(int32_t)(0xff000000u|yy<<16|yy<<8|yy);
        else if(flavor%5==2){unsigned r=(yy+random0()%31)%256,g=(yy+random0()%31)%256,b=(yy+random0()%31)%256;src[i]=(int32_t)(0xff000000u|r<<16|g<<8|b);}
        else src[i]=(int32_t)(0xff000000u|(random0()&0xffffff));
        if(flavor%7==0 && random0()%6==0)src[i]=(int32_t)((uint32_t)src[i]&(0x00ffffffu|(random0()%255)<<24));
    }
    for(int i=0;i<count;i++){
        /* Inactive metadata may contain out-of-range sigma/threshold bits.
           The vector path must never use them to index a range table. */
        meta[i]=random0()%6==0?(int32_t)(random0()&0x7fffffffu):
            (int32_t)(0x80000000u|(2+random0()%31)|(3+random0()%61)<<6);
    }
    for(int col=0;col<width;col++)for(int xi=0;xi<taps;xi++){
        int dx=radius==4?off[xi]:xi-radius;x[col*taps+xi]=max(0,min(width-1,col+dx));
    }
    if(weird_x)for(int i=0;i<width*taps;i+=3)x[i]=random0()%width;
    for(int i=0;i<33*256;i++)range[i]=flavor%3==0?256:flavor%3==1?max(0,256-(i%256)*17):(int)(random0()%257);
    memcpy(before,src,(size_t)width*rows*4);
    for(int i=0;i<count*3;i++)actual[i]=expected[i]=(int32_t)0xa53c18f7u;
    for(int i=0;i<width*(radius*2+1);i++)ring[i]=(int32_t)0xfebea8c7u;
    require(residual1944_valid(meta,count,width,rows,begin,end,lo,hi,radius,width*rows,count*3,33*256,width*(radius*2+1),width*taps),"valid fixture");
    oracle(src,meta,expected,width,begin,end,lo,hi,radius,range,x);
    residual1944_aggregate(src,meta,actual,width,begin,end,lo,hi,radius,range,ring,x);
    for(int i=0;i<count*3;i++){
        if(actual[i]!=expected[i]){fprintf(stderr,"mismatch width=%d rows=%d radius=%d flavor=%d weird_x=%d index=%d expected=%08x actual=%08x\n",width,rows,radius,flavor,weird_x,i,(unsigned)expected[i],(unsigned)actual[i]);require(0,"pixel parity");}
        checks++;
    }
    require(memcmp(before,src,(size_t)width*rows*4)==0,"immutable source");pixels+=count;cases++;
    free(expected);free(before);free_guard(sg);free_guard(mg);free_guard(og);free_guard(rg);free_guard(xg);free_guard(tg);
}
int main(void){
    for(int width=1;width<=97;width++)for(int radius=1;radius<=4;radius++)
        for(int flavor=0;flavor<3;flavor++)fixture(width,1+(int)(random0()%27),radius,width+flavor,flavor==2 && width%11==0);
    for(int radius=1;radius<=4;radius++)for(int flavor=0;flavor<15;flavor++)fixture(128+flavor,25,radius,flavor,flavor==14);
    for(int radius=1;radius<=4;radius++)fixture(4080,32,radius,0,0);
    printf("{\"schema\":\"ulike-native1945-residual-tests-v1\",\"passed\":true,\"cases\":%d,\"exact_int_comparisons_and_checks\":%lld,\"pixels\":%lld,\"guard_pages\":true,\"noncanonical_x_retained\":true,\"neon\":%s}\n",cases,checks,pixels,
#if defined(__aarch64__) && !defined(ULIKE_RESIDUAL_SCALAR)
        "true"
#else
        "false"
#endif
    );return 0;
}
