package com.hiro.ulike;
/** Deterministic host denoiser; production scheduling/mix/shadow code remain real. */
public final class DetailSerial186 {
 public static void filter(DetailPixels.Work w,int width,int rows,int start,int count,int noise,int sharp,boolean t,boolean h,boolean d){
  for(int y=start;y<start+count;y++)for(int x=0;x<width;x++){
   int sum=0;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)sum+=QualityPixels1932.luma(w.source[Math.max(0,Math.min(rows-1,y+dy))*width+Math.max(0,Math.min(width-1,x+dx))]);
   int n=(sum+4)/9;w.output[y*width+x]=0xff000000|(n<<16)|(n<<8)|n;
  }
 }
}
