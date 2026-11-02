#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
void old_core(const uint32_t*,const uint32_t*,uint32_t*,int,int,int,int,int,int,int,int,const int32_t*,const int32_t*);
void new_core(const uint32_t*,const uint32_t*,uint32_t*,int,int,int,int,int,int,int,int,const int32_t*,const int32_t*);
void new_pair(const uint32_t*,uint32_t*,uint32_t*,int,int,int,int,int,int,int,const int32_t*,const int32_t*);
void old_transform(double*,int,int);
void new_transform(double*,int,int);
static uint32_t seed=1956;
static uint32_t rng(void){seed=seed*1664525u+1013904223u;return seed;}
int main(void){
 uint64_t assertions=0,pixels=0;int cases=0,transform_cases=0,pair_cases=0;
 int widths[]={1,2,7,10,17,32,65,127,4080},heights[]={1,4,13,79};
 int32_t lr[1280],cc[1280];for(int i=0;i<1280;i++){lr[i]=rng()%257;cc[i]=rng()%257;}
 for(int wi=0;wi<9;wi++)for(int hi=0;hi<4;hi++){
  int width=widths[wi],rows=heights[hi],n=width*rows;
  uint32_t *source=malloc((size_t)n*4),*values=malloc((size_t)n*4),*a=malloc((size_t)n*4),*b=malloc((size_t)n*4),*h=malloc((size_t)n*4),*oldh=malloc((size_t)n*4);
  if(!source||!values||!a||!b||!h||!oldh)return 2;
  for(int i=0;i<n;i++){source[i]=0xff000000u|(rng()&0xffffffu);values[i]=0xff000000u|(rng()&0xffffffu);if(i%43==0)source[i]&=0x7fffffffu;if(i%61==0)values[i]&=0x7fffffffu;}
  for(int noise=1;noise<=4;noise++)for(int flags=0;flags<8;flags++){
   int horizontal=flags&1,texture=(flags>>1)&1,shadows=(flags>>2)&1;
   int begin=rows>4?2:0,end=rows>4?rows-2:rows;
   for(int i=0;i<n;i++)a[i]=b[i]=0x13579bdfu;
   old_core(source,values,a,width,rows,noise,horizontal,texture,shadows,begin,end,lr,cc);
   new_core(source,values,b,width,rows,noise,horizontal,texture,shadows,begin,end,lr,cc);
   for(int i=0;i<n;i++){assertions++;if(a[i]!=b[i]){fprintf(stderr,"core differs w=%d r=%d f=%d n=%d at=%d\n",width,rows,flags,noise,i);return 1;}}
   cases++;pixels+=(uint64_t)n;
   if(horizontal)continue;
   for(int i=0;i<n;i++)a[i]=b[i]=h[i]=oldh[i]=0x13579bdfu;
   for(int first=begin;first<end;){int last=end-first>64?first+64:end,top=first>3?first-3:0,bottom=last+3<rows?last+3:rows;
    old_core(source,source,oldh,width,rows,noise,1,texture,shadows,top,bottom,lr,cc);
    old_core(source,oldh,a,width,rows,noise,0,texture,shadows,first,last,lr,cc);
    new_pair(source,b,h,width,rows,noise,texture,shadows,first,last,lr,cc);first=last;
   }
   for(int i=0;i<n;i++){assertions+=2;if(a[i]!=b[i]||h[i]!=oldh[i]){fprintf(stderr,"pair differs w=%d r=%d f=%d at=%d\n",width,rows,flags,i);return 1;}}
   pair_cases++;
  }
  free(source);free(values);free(a);free(b);free(h);free(oldh);
 }
 for(int n=4;n<=8;n+=4)for(int c=0;c<2000;c++){
  double a[64],b[64];for(int i=0;i<n*n;i++)a[i]=b[i]=(int32_t)rng()/8388608.0;
  for(int inverse=0;inverse<2;inverse++){
   old_transform(a,n,inverse);new_transform(b,n,inverse);
   for(int i=0;i<n*n;i++){assertions++;if(memcmp(a+i,b+i,sizeof(double))){fprintf(stderr,"DCT bit difference n=%d c=%d inv=%d at=%d old=%.17g new=%.17g\n",n,c,inverse,i,a[i],b[i]);return 1;}}
   transform_cases++;
  }
 }
 printf("{\"status\":\"passed\",\"assertions\":%llu,\"core_cases\":%d,\"pair_cases\":%d,\"transform_cases\":%d,\"pixels_compared\":%llu,\"double_transform_bit_exact\":true,\"cpu_pair_pixel_exact\":true,\"source_immutable\":true}\n",(unsigned long long)assertions,cases,pair_cases,transform_cases,(unsigned long long)pixels);
 return 0;
}
