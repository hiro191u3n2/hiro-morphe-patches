#define _GNU_SOURCE
#include "kernels1935.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <sys/mman.h>
#include <unistd.h>
static uint32_t rng=1935;static uint32_t rnd(void){rng^=rng<<13;rng^=rng>>17;rng^=rng<<5;return rng;}
static long long assertions;
static void require(int ok,const char *msg){assertions++;if(!ok){fprintf(stderr,"FAIL %s\n",msg);exit(1);}}
static void equal(const void *a,const void *b,size_t n,const char *msg){require(memcmp(a,b,n)==0,msg);}
static float clamp_java(float v,float lo,float hi){float t=v>hi?hi:v;return t<=lo?lo:t;}
static void horizontal_ref(const int32_t *raw,const int32_t *off,const int32_t *idx,const float *wt,float *out,int width){
 for(int x=0;x<width;x++){float s[3]={0,0,0},lo[3]={INFINITY,INFINITY,INFINITY},hi[3]={-INFINITY,-INFINITY,-INFINITY};for(int t=off[x];t<off[x+1];t++){uint32_t p=(uint32_t)raw[idx[t]];float v[3]={(float)(p>>16&255),(float)(p>>8&255),(float)(p&255)};for(int c=0;c<3;c++){volatile float product=v[c]*wt[t];s[c]=s[c]+product;if(v[c]<lo[c])lo[c]=v[c];if(v[c]>hi[c])hi[c]=v[c];}}for(int c=0;c<3;c++)out[3*x+c]=clamp_java(s[c],lo[c],hi[c]);}}
static void test_float(void){
 int32_t raw[259],offsets[130],indices[129*65];float weights[129*65],want[387],got[387];
 for(int trial=0;trial<3000;trial++){int w=1+rnd()%129,nr=1+rnd()%259,n=0;for(int i=0;i<nr;i++)raw[i]=0xff000000u|rnd();for(int x=0;x<w;x++){offsets[x]=n;int taps=1+rnd()%65;for(int t=0;t<taps;t++){indices[n]=rnd()%nr;weights[n]=(float)((int)(rnd()%10001)-5000)/4096.f;n++;}}offsets[w]=n;
 require(speed1935_horizontal_valid(raw,nr,offsets,w+1,indices,n,weights,n,3*w,w),"valid horizontal");horizontal_ref(raw,offsets,indices,weights,want,w);speed1935_horizontal(raw,offsets,indices,weights,got,w);equal(want,got,3*w*sizeof(float),"horizontal float bits");
 indices[n-1]=nr;require(!speed1935_horizontal_valid(raw,nr,offsets,w+1,indices,n,weights,n,3*w,w),"invalid last index");indices[n-1]=0;weights[n-1]=NAN;require(!speed1935_horizontal_valid(raw,nr,offsets,w+1,indices,n,weights,n,3*w,w),"NaN weights rejected");
 }
 float a[1027],lo[1027],hi[1027],b[1027],bl[1027],bh[1027],row[1027];
 for(int trial=0;trial<2000;trial++){int n=rnd()%1028;for(int i=0;i<n;i++){a[i]=b[i]=(i%7==0?-0.f:0.f);lo[i]=bl[i]=INFINITY;hi[i]=bh[i]=-INFINITY;}
 for(int t=0;t<17;t++){float wt=(float)((int)(rnd()%10001)-5000)/8192.f;for(int i=0;i<n;i++){row[i]=(float)(rnd()%1048576)/4096.f;volatile float product=row[i]*wt;b[i]=b[i]+product;if(row[i]<bl[i])bl[i]=row[i];if(row[i]>bh[i])bh[i]=row[i];}speed1935_vertical(a,lo,hi,row,wt,n);equal(a,b,n*sizeof(float),"vertical sum bits");equal(lo,bl,n*sizeof(float),"vertical min bits");equal(hi,bh,n*sizeof(float),"vertical max bits");}}
}
static void test_yuv(void){
 uint8_t y[120000],u[50000],v[50000],want[60000],got[60000];for(size_t i=0;i<sizeof(y);i++)y[i]=rnd();for(size_t i=0;i<sizeof(u);i++){u[i]=rnd();v[i]=rnd();}
 for(int trial=0;trial<6000;trial++){int w=2*(1+rnd()%96),h=2*(1+rnd()%64),yp=1+rnd()%3,up=1+rnd()%4,vp=1+rnd()%4;int yr=(w-1)*yp+1+rnd()%31,ur=(w/2-1)*up+1+rnd()%31,vr=(w/2-1)*vp+1+rnd()%31;int ys=rnd()%31,us=rnd()%31,vs=rnd()%31;int yl=ys+(h-1)*yr+(w-1)*yp+1,ul=us+(h/2-1)*ur+(w/2-1)*up+1,vl=vs+(h/2-1)*vr+(w/2-1)*vp+1;
 require(speed1935_plane_valid(ys,yl,yr,yp,w,h),"Y valid");require(speed1935_plane_valid(us,ul,ur,up,w/2,h/2),"U valid");require(speed1935_plane_valid(vs,vl,vr,vp,w/2,h/2),"V valid");require(!speed1935_plane_valid(ys,yl-1,yr,yp,w,h),"Y truncated");
 memset(want,0x7e,sizeof(want));memset(got,0x7e,sizeof(got));for(int r=0;r<h;r++)for(int x=0;x<w;x++)want[r*w+x]=y[ys+r*yr+x*yp];for(int r=0;r<h/2;r++)for(int x=0;x<w/2;x++){want[w*h+r*w+2*x]=v[vs+r*vr+x*vp];want[w*h+r*w+2*x+1]=u[us+r*ur+x*up];}speed1935_pack(y,ys,yr,yp,u,us,ur,up,v,vs,vr,vp,w,h,got);equal(want,got,sizeof(got),"YUV bytes and output sentinels");}
 require(!speed1935_plane_valid(0,2147483647,2147483647,2147483647,2147483647,2147483647),"64bit bounds");
}
static void guard_yuv(void){
 long page=sysconf(_SC_PAGESIZE);uint8_t *maps[4];for(int i=0;i<4;i++){maps[i]=mmap(0,page*2,PROT_READ|PROT_WRITE,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);require(maps[i]!=MAP_FAILED,"mmap");require(mprotect(maps[i]+page,page,PROT_NONE)==0,"mprotect");}
 for(int w=2;w<=192;w+=2)for(int p=1;p<=3;p++){int h=2,yl=(w-1)*p+1+w*p,cl=(w/2-1)*p+1;uint8_t *y=maps[0]+page-yl,*u=maps[1]+page-cl,*v=maps[2]+page-cl,*out=maps[3]+page-w*3;for(int i=0;i<yl;i++)y[i]=(uint8_t)i;for(int i=0;i<cl;i++){u[i]=33+i;v[i]=88+i;}speed1935_pack(y,0,w*p,p,u,0,cl,p,v,0,cl,p,w,h,out);for(int x=0;x<w/2;x++){require(out[2*w+2*x]==v[x*p],"guard V");require(out[2*w+2*x+1]==u[x*p],"guard U");}}
 for(int i=0;i<4;i++)munmap(maps[i],page*2);
}
int main(void){test_float();test_yuv();guard_yuv();printf("{\"schema\":\"ulike-native1935-tests-v1\",\"assertions\":%lld,\"passed\":true,\"neon\":%s}\n",assertions,
#if defined(__aarch64__)
"true"
#else
"false"
#endif
);return 0;}
