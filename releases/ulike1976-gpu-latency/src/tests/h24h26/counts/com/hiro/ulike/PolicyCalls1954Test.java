package com.hiro.ulike;
public final class PolicyCalls1954Test {
 private static long h24,h26,cases;
 private static void n24(boolean v,String m){h24++;if(!v)throw new AssertionError(m);}
 private static void n26(boolean v,String m){h26++;if(!v)throw new AssertionError(m);}
 private static void reset(){NativeMoire1951.calls=0;NativeMoire1951.overflowAt=Integer.MAX_VALUE;NativeMoire1951.uniformPrefix=0;NativeMoire1951.interruptAt=0;NativeMoire1951.throwAt=0;NativeMoire1951.constant=false;}
 private static int decode(FinishPolicy1953.Band b,int i,int k){return b.mode==FinishPolicy1953.CONSTANT4?b.words[k]:b.mode==FinishPolicy1953.RAW4?b.words[i*4+k]:(b.words[i*2+k/2]>>>((k&1)*16))&65535;}
 private static void constants(){
  QualityPixels1932.Plan p=new QualityPixels1932.Plan();reset();NativeMoire1951.constant=true;
  FinishPolicy1953.Band b=FinishPolicy1953.prepare(p,4080,512,0,512,77);
  n24(NativeMoire1951.calls==1,"proven constant prepares exactly once");n24(b.mode==FinishPolicy1953.CONSTANT4&&b.words.length==4,"constant storage");b.close();
  final int[] masks={0};p.faceRegions=new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){masks[0]++;return 7;}};
  p.texturePriority=false;reset();NativeMoire1951.constant=true;
  b=FinishPolicy1953.prepare(p,4080,256,0,256,77);
  n24(NativeMoire1951.calls==1&&masks[0]==0,"disabled mask permits constant shortcut");b.close();
  p.texturePriority=true;reset();NativeMoire1951.constant=true;
  b=FinishPolicy1953.prepare(p,31,17,0,17,7);
  n24(NativeMoire1951.calls==31*17&&masks[0]==31*17,"unknown constant mask evaluated at every coordinate");b.close();
  p.texturePriority=false;p.localNoise=new Object();p.outputScale=.5f;reset();NativeMoire1951.constant=true;
  b=FinishPolicy1953.prepare(p,31,17,0,17,7);
  n24(NativeMoire1951.calls==1,"inactive source noise map permits shortcut");b.close();
 }
 private static void promotions(){
  final int width=31,rows=17,n=width*rows;QualityPixels1932.Plan p=new QualityPixels1932.Plan();p.localNoise=new Object();
  for(int bad:new int[]{0,1,17,59,n-1})for(int uniform:new int[]{0,1,37,n-1}) {
   reset();NativeMoire1951.overflowAt=bad;NativeMoire1951.uniformPrefix=uniform;
   FinishPolicy1953.Band b=FinishPolicy1953.prepare(p,width,rows,0,rows,0);
   n26(NativeMoire1951.calls==n,"single-pass only one evaluation per coordinate");
   n26(b.mode==FinishPolicy1953.RAW4&&b.rawFallback,"raw promotion");int[] expected=new int[4];
   for(int i=0;i<n;i++){NativeMoire1951.values(i,expected,0);for(int k=0;k<4;k++)n26(decode(b,i,k)==expected[k],"rounded prefix preserved on promotion");}
   b.close();cases++;
  }
  final int[] masks={0};p.texturePriority=true;p.faceRegions=new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){return ++masks[0];}};
  reset();NativeMoire1951.overflowAt=n-1;
  FinishPolicy1953.Band b=FinishPolicy1953.prepare(p,width,rows,0,rows,0);
  n26(NativeMoire1951.calls==n*2&&masks[0]==n*2,"unknown masks retain original second pass");
  int[] e=new int[4];for(int i=0;i<n;i++){NativeMoire1951.values(n+i,e,0);for(int k=0;k<4;k++)n26(decode(b,i,k)==e[k],"unknown stateful second-pass values retained");}b.close();
 }
 private static void cancellation(){
  int w=31,h=17,n=w*h;QualityPixels1932.Plan p=new QualityPixels1932.Plan();p.localNoise=new Object();
  for(int mode=0;mode<2;mode++) {
   SpeedWorkers1935.trim();int[] s=SpeedWorkers1935.borrowInts(4),packed=SpeedWorkers1935.borrowInts(n*2),raw=SpeedWorkers1935.borrowInts(n*4);
   SpeedWorkers1935.release(s);SpeedWorkers1935.release(packed);SpeedWorkers1935.release(raw);
   reset();NativeMoire1951.overflowAt=37;if(mode==0)NativeMoire1951.interruptAt=77;else NativeMoire1951.throwAt=77;
   try{FinishPolicy1953.prepare(p,w,h,0,h,0);throw new AssertionError("failure lost");}
   catch(RuntimeException failure){n26(mode==0?"quality interrupted".equals(failure.getMessage()):failure==NativeMoire1951.FAILURE,"failure preserved");}
   if(mode==0)n26(Thread.interrupted(),"interrupt flag retained");
   int[] ss=SpeedWorkers1935.borrowInts(4),pp=SpeedWorkers1935.borrowInts(n*2),rr=SpeedWorkers1935.borrowInts(n*4);
   n26(s==ss&&packed==pp&&raw==rr,"all promotion leases returned on failure");
   SpeedWorkers1935.release(ss);SpeedWorkers1935.release(pp);SpeedWorkers1935.release(rr);
  }
 }
 public static void main(String[] args){constants();promotions();cancellation();System.out.println("{\"status\":\"passed\",\"h24_assertions\":"+h24+",\"h26_assertions\":"+h26+",\"promotion_cases\":"+cases+"}");}
}
