import com.hiro.ulike.*;
import java.util.*;
public final class EnhancedDetailTest {
 static int checks=0;static void check(boolean ok,String s){checks++;if(!ok)throw new AssertionError(s);}
 static int clip(int v){return Math.max(0,Math.min(255,v));}
 static int rgb(int r,int g,int b){return 0xff000000|(clip(r)<<16)|(clip(g)<<8)|clip(b);}
 static int[] run(int[] a,int w,int h,int n,int s,int flags,int strip){
  int[] out=new int[a.length];DetailPixels.Work wk=new DetailPixels.Work(w*Math.min(h,strip+2*DetailPixels.HALO));
  for(int y=0;y<h;y+=strip){int count=Math.min(strip,h-y),top=Math.max(0,y-DetailPixels.HALO),end=Math.min(h,y+count+DetailPixels.HALO);
   System.arraycopy(a,top*w,wk.source,0,(end-top)*w);DetailPixels.filter(wk,w,end-top,y-top,count,n,s,(flags&1)!=0,(flags&2)!=0,(flags&4)!=0);
   System.arraycopy(wk.output,(y-top)*w,out,y*w,count*w);
  }return out;
 }
 static int[] old(int[] a,int w,int h,int n,int s){PriorDetailPixels.Work wk=new PriorDetailPixels.Work(a.length);System.arraycopy(a,0,wk.source,0,a.length);PriorDetailPixels.filter(wk,w,h,0,h,n,s);return wk.output;}
 static double mse(int[] a,int[] b){double s=0;for(int i=0;i<a.length;i++)for(int sh=0;sh<=16;sh+=8){int d=((a[i]>>>sh)&255)-((b[i]>>>sh)&255);s+=d*d;}return s/(3*a.length);}
 static int contrast(int[] a,int w,int h){int i=h/2*w+w/2;return(a[i+2]&255)-(a[i-2]&255);}
 public static void main(String[] args){
  Random rng=new Random(158);int w=41,h=85;int[] a=new int[w*h];
  for(int i=0;i<a.length;i++)a[i]=rng.nextInt(6)==0?rng.nextInt():rgb(rng.nextInt(256),rng.nextInt(256),rng.nextInt(256));
  for(int n=0;n<=4;n++)for(int s=0;s<=4;s++)for(int flags=0;flags<8;flags++){
   int[] full=run(a,w,h,n,s,flags,h);check(Arrays.equals(full,run(a,w,h,n,s,flags,13)),"flag/strip seam "+n+"/"+s+"/"+flags);
   for(int i=0;i<a.length;i++)check((a[i]>>>24)==(full[i]>>>24),"flag alpha unchanged");
   if(n==0&&s==0)check(Arrays.equals(a,full),"protection-only no-op");
  }
  w=144;h=96;a=new int[w*h];int[] truth=new int[a.length];
  for(int brightness:new int[]{40,128,200}){
   for(int i=0;i<a.length;i++){int l=(int)Math.round(rng.nextGaussian()*8);truth[i]=rgb(brightness,brightness,brightness);a[i]=rgb(brightness+l+(int)Math.round(rng.nextGaussian()*6),brightness+l+(int)Math.round(rng.nextGaussian()*6),brightness+l+(int)Math.round(rng.nextGaussian()*6));}
   double orig=mse(a,truth),prior=mse(old(a,w,h,3,0),truth),strong=mse(run(a,w,h,3,0,7,64),truth),maximum=mse(run(a,w,h,4,0,7,64),truth);
   check(strong<prior,"new strong improves synthetic flat noise vs prior strong");check(maximum<strong,"maximum smoother than strong");
   double combined=mse(run(a,w,h,4,4,7,64),truth);check(combined<prior,"max sharp does not undo denoise improvement");
   System.out.printf(Locale.ROOT,"Synthetic flat %d: input MSE %.3f; prior strong %.3f; new strong %.3f; new maximum %.3f; max+max %.3f%n",brightness,orig,prior,strong,maximum,combined);
   if(brightness==40)check(mse(run(a,w,h,2,0,7,64),truth)<mse(run(a,w,h,2,0,3,64),truth),"shadow priority reduces dark synthetic noise");
   if(brightness==200)check(Arrays.equals(run(a,w,h,2,0,7,64),run(a,w,h,2,0,3,64)),"shadow priority leaves bright areas unchanged");
  }
  // A softly blurred edge: larger contrast without new extrema when halo protection on.
  for(int yy=0;yy<h;yy++)for(int x=0;x<w;x++){int v=(int)Math.round(40+180/(1+StrictMath.exp(-(x-w/2.0)/2)));a[yy*w+x]=rgb(v,v,v);}
  int base=contrast(a,w,h),previous=contrast(old(a,w,h,0,3),w,h),strong=contrast(run(a,w,h,0,3,7,64),w,h),max=contrast(run(a,w,h,0,4,7,64),w,h);
  check(strong>previous&&max>strong,"stronger coherent transition than prior");
  System.out.println("Synthetic blurred edge 4px contrast: input="+base+" prior strong="+previous+" new strong="+strong+" new maximum="+max);
  for(int p:run(a,w,h,0,4,7,64))check((p&255)>=40&&(p&255)<=220,"protected global extrema");
  // Each optional protection changes its intended path, not a decorative switch.
  for(int yy=0;yy<h;yy++)for(int x=0;x<w;x++){int v=x<w/2?60:190;a[yy*w+x]=rgb(v,v,v);}
  check(Arrays.equals(a,run(a,w,h,0,4,2,64)),"hard edge halo-protected identity");
  check(!Arrays.equals(run(a,w,h,0,4,2,64),run(a,w,h,0,4,0,64)),"halo switch acts");
  for(int yy=0;yy<h;yy++)for(int x=0;x<w;x++){int v=(int)Math.round(32*Math.sin(x*0.85));a[yy*w+x]=rgb(180+v,140+v,110+v);}
  int[] tex=run(a,w,h,3,0,3,64),smooth=run(a,w,h,3,0,2,64);
  check(!Arrays.equals(tex,smooth),"skin-like texture priority changes denoise");check(mse(tex,a)<mse(smooth,a),"skin-like texture priority keeps more source structure");
  check(mse(run(a,w,h,0,4,3,64),a)<mse(run(a,w,h,0,4,2,64),a),"skin-like texture priority reduces harsh sharp");
  // Neutral source coloured areas: sharp adds the same RGB offset to all channels.
  int[] sharp=run(a,w,h,0,4,7,64);for(int i=0;i<a.length;i++){
   int d=(sharp[i]&255)-(a[i]&255);check(((sharp[i]>>>8)&255)-((a[i]>>>8)&255)==d&&((sharp[i]>>>16)&255)-((a[i]>>>16)&255)==d,"luma only sharp");
  }
  System.out.println("PASS "+checks+" enhanced assertions; 25 levels x 8 protection combinations; relative synthetic metrics only, NOT real-photo quality proof.");
 }
}
