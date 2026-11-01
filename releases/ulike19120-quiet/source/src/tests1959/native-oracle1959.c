/* Frozen published .58 scalar production kernel, separate translation unit. */
#define Java_com_hiro_ulike_StrongNoise1958_nativeAbi Oracle1959_nativeAbi
#define Java_com_hiro_ulike_StrongNoise1958_processNative Oracle1959_processNative
#include "published1958-reference/native1958/smooth_noise1958.c"
void oracle_prepare1959(const jint *pixels,int width,int height,int noise,int shadows,const float *ev,int mode,jint *out){
    Region src={(jint*)pixels,width,height,0,height};
    for(int y=0;y<height;y++)for(int x=0;x<width;x++)prepare_pixel(&src,x,y,noise,shadows,ev,mode,out+(size_t)y*width+x);
}
void oracle_process1959(const jint *pixels,int width,int height,int noise,int shadows,const float *ev,const jint *h,const jint *q,const jint *e,const jint *policy,jint *out){
    Region src={(jint*)pixels,width,height,0,height},maps[3];const jint *data[3]={h,q,e};int w=width,r=height;
    for(int k=0;k<3;k++){w=ceil_div2(w);r=ceil_div2(r);Region m={(jint*)data[k],w,r,0,r};maps[k]=m;}
    for(int y=0;y<height;y++)for(int x=0;x<width;x++){size_t at=(size_t)y*width+x;process_pixel(&src,maps,x,y,0,height,0,height,0,noise,shadows,ev,policy+at*2,out+at);}
}
void oracle_guide1959(const jint *pixels,int width,int height,int x,int y,float sy,float sc,float *out){
    Region src={(jint*)pixels,width,height,0,height};guide(&src,x,y,0,height,sy,sc,out);
    out[2]=1/(1+square(structure(&src,x,y,0,height)/(sy*3+3)));
}
int oracle_confidence1959(const jint *pixels,int width,int height,int x,int y,const float *ev){
    Region src={(jint*)pixels,width,height,0,height};if((pixel(&src,x,y)>>24)!=255)return 0;
    float sy=regional_sigma(ev,width,height,0,x,y,0),sc=regional_sigma(ev,width,height,0,x,y,1),flat[2];guide(&src,x,y,0,height,sy,sc,flat);
    return round_float(256*clampf((flat[0]-.65f)/.30f,0,1)*clampf((maxf(sy,sc)-.60f)/1.8f,0,1)*(1-regional_sigma(ev,width,height,0,x,y,2)));
}
