package com.hiro.ulike;
/** Host-only primary NR surrogate, preserving channels and transparent pixels.
 * Production primary filter is dex-bound; pixel tests separately measure actual N1–N3.
 */
public final class DetailSerial186 {
 public static final ThreadLocal<Boolean> REACH_SIX=new ThreadLocal<Boolean>();
 public static void filter(DetailPixels.Work w,int width,int rows,int start,int count,int noise,int sharp,boolean t,boolean h,boolean d){
  if(Boolean.TRUE.equals(REACH_SIX.get()) && noise>0){
   // Two radius3 stages model production's maximum six-row source reach.
   for(int y=Math.max(0,start-3);y<Math.min(rows,start+count+3);y++)for(int x=0;x<width;x++){
    int rr=0,gg=0,bb=0;for(int dy=-3;dy<=3;dy++){int p=w.source[Math.max(0,Math.min(rows-1,y+dy))*width+x];rr+=(p>>>16)&255;gg+=(p>>>8)&255;bb+=p&255;}
    w.denoised[y*width+x]=0xff000000|(((rr+3)/7)<<16)|(((gg+3)/7)<<8)|((bb+3)/7);
   }
   for(int y=start;y<start+count;y++)for(int x=0;x<width;x++){
    int rr=0,gg=0,bb=0;for(int dy=-3;dy<=3;dy++){int p=w.denoised[Math.max(0,Math.min(rows-1,y+dy))*width+x];rr+=(p>>>16)&255;gg+=(p>>>8)&255;bb+=p&255;}
    w.output[y*width+x]=0xff000000|(((rr+3)/7)<<16)|(((gg+3)/7)<<8)|((bb+3)/7);
   }
   return;
  }
  for(int y=start;y<start+count;y++)for(int x=0;x<width;x++){
   int at=y*width+x,c=w.source[at];
   if(noise<=0 || (c>>>24)!=255){w.output[at]=c;continue;}
   int rr=0,gg=0,bb=0;
   for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
    int p=w.source[Math.max(0,Math.min(rows-1,y+dy))*width+Math.max(0,Math.min(width-1,x+dx))];
    rr+=(p>>>16)&255;gg+=(p>>>8)&255;bb+=p&255;
   }
   w.output[at]=(c&0xff000000)|(((rr+4)/9)<<16)|(((gg+4)/9)<<8)|((bb+4)/9);
  }
 }
}
