package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;

/** Execute current SingleNoise CPU branch and inspect the actual native-call
 * policy boundary. Only that native consumer is controlled, not policy math. */
public final class FrontPolicy1994Test {
 static int checks,cases;static long words;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static QualityPixels1932.Plan plan(QualityPixels1932.RegionMask mask){return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(3,4,121,.2f,100),400,10000000L,2,.83f,4,4,true,true,1f).withFaceRegions(mask);}
 static SingleNoise1955.Model model(int width,int height)throws Exception {
  Constructor<?> c=null;for(Constructor<?> candidate:SingleNoise1955.Model.class.getDeclaredConstructors())if(candidate.getParameterTypes().length==13)c=candidate;if(c==null)throw new AssertionError("model constructor");c.setAccessible(true);
  float[] brightness=new float[8],ratios=new float[72];Arrays.fill(brightness,3);Arrays.fill(ratios,1);
  return (SingleNoise1955.Model)c.newInstance(width,height,1,1,new float[]{3},new float[]{4},new float[]{121},brightness,brightness,ratios,100,Math.min(48,width),Math.min(48,height));
 }
 static int[] expected(SingleNoise1955.Protection p,int w,int begin,int end,int origin){int[] out=new int[w*(end-begin)*2];
  for(int y=begin;y<end;y++)for(int x=0;x<w;x++){int at=((y-begin)*w+x)*2;out[at]=Math.max(0,Math.min(256,p.budgetQ8(x,y+origin)));out[at+1]=Math.max(0,Math.min(256,p.detailQ8(x,y+origin)));}return out;}
 static int[] invoke(SingleNoise1955.Protection p,int width,int rows,int begin,int end,int origin)throws Exception {
  int[] input=new int[width*rows],output=new int[input.length];Arrays.fill(input,0xff51719b);
  CpuSingle1978.seen=null;SingleNoise1955.processCpuRange(input,output,width,rows,begin,end,0,rows,origin,4,true,model(width,rows+origin),p);
  check(CpuSingle1978.seen!=null,"real CPU branch reaches native consumer");check(Arrays.equals(input,output),"controlled consumer owns successful output");return CpuSingle1978.seen;
 }
 static final class Unknown implements QualityPixels1932.RegionMask,PairedRegions1976 {
  final List<String> calls=new ArrayList<String>();
  public int skinQ8(int x,int y){calls.add("s"+x+":"+y);return (x+y)%3==0?Integer.MAX_VALUE:x*19-y*123;}
  public int detailQ8(int x,int y){calls.add("d"+x+":"+y);return x*1976-y*713;}
  public int samplePair1976(int x,int y){throw new AssertionError("unknown callback must not use paired fast path");}
 }
 public static void main(String[] args)throws Exception {
  Field state=SingleNoise1955.class.getDeclaredField("nativeState");state.setAccessible(true);state.setInt(null,1);
  Random random=new Random(1994);int[][] dims={{1,1},{7,9},{31,47},{65,69}};
  for(int[] d:dims)for(int turn:new int[]{0,90,180,270})for(int rotation:new int[]{0,90,180,270,-90,450}){
   int w=d[0],h=d[1],rw=Math.max(1,w/3),rh=Math.max(1,h/3);byte[] skin=new byte[rw*rh],detail=new byte[skin.length];random.nextBytes(skin);random.nextBytes(detail);
   FaceRegions1934.Mask face=FaceRegions1934.uprightRaster(w,h,rw,rh,skin,detail,turn).resample(rotation,w,h);
   GpuPolicy1960.Protection protection=new GpuPolicy1960.Protection(plan(face),false);int begin=Math.min(2,h-1),end=h,origin=0;
   int[] old=expected(protection,w,begin,end,origin),actual=invoke(protection,w,h,begin,end,origin);
   check(Arrays.equals(old,actual),"all rotations original per-pixel callbacks equal paired native policy");words+=actual.length;cases++;
  }
  Unknown old=new Unknown(),now=new Unknown();int[] reference=expected(new GpuPolicy1960.Protection(plan(old),false),9,8,14,7);
  int[] actual=invoke(new GpuPolicy1960.Protection(plan(now),false),9,22,8,14,7);
  check(Arrays.equals(reference,actual)&&old.calls.equals(now.calls),"unknown signed callback values and exact evaluation order unchanged");
  Unknown cachedMask=new Unknown();QualityPixels1932.Plan original=plan(cachedMask);PolicyCache1945 lease=PolicyCache1945.borrow(original,9,0,22);
  check(lease!=null,"scoped policy cache available for ownership test");
  try{GpuPolicy1960.Protection scoped=new GpuPolicy1960.Protection(original.withPolicyCache(lease),false);
   reference=expected(scoped,9,8,14,7);int before=cachedMask.calls.size();actual=invoke(scoped,9,22,8,14,7);
   check(Arrays.equals(reference,actual)&&cachedMask.calls.size()==before,"paired producer preserves scoped cache authority");
  }finally{lease.close();}
  final List<String> calls=new ArrayList<String>();SingleNoise1955.Protection arbitrary=new SingleNoise1955.Protection(){public int budgetQ8(int x,int y){calls.add("b"+x+":"+y);return x-y;}public int detailQ8(int x,int y){calls.add("d"+x+":"+y);return x+y+999;}};
  reference=expected(arbitrary,5,8,11,2);List<String> order=new ArrayList<String>(calls);calls.clear();actual=invoke(arbitrary,5,18,8,11,2);
  check(Arrays.equals(reference,actual)&&calls.equals(order),"unrecognized Protection retains callback ordering");
  CpuSingle1978.seen=null;Thread.currentThread().interrupt();boolean cancelled=false;
  try{invoke(new GpuPolicy1960.Protection(null,false),5,18,8,11,2);}catch(java.util.concurrent.CancellationException expected){cancelled=true;}finally{Thread.interrupted();}
  check(cancelled&&CpuSingle1978.seen==null,"cancel before native policy commit");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"policy_cases\":"+cases+",\"policy_words\":"+words+",\"controlled_native_consumer\":true,\"physical_android_tested\":false}");
 }
}

/** Declared policy-consumer test peer; never included in release classes. */
final class CpuSingle1978 {
 static int[] seen;
 static int[] geometry(int w,int h,int b,int e,int vb,int ve,int o,int n,boolean s,SingleNoise1955.Model m){return new int[]{w,h,b,e,vb,ve,o,n};}
 static String key(boolean n,int[] u,boolean p){return "policy-consumer94";}
 static boolean nativeCall(int[] input,int[] output,int[] u,float[] model,int[] policy,SingleNoise1955.Workspace workspace){seen=policy.clone();System.arraycopy(input,0,output,0,input.length);return true;}
 static void offerNative(String k,int[] s,int[] u,float[] m,int[] p){throw new AssertionError("null workspace test cannot offer optional proof");}
 static long create(int width,int rows){return 0;}static void release(long handle,int width,int rows){}
 static SingleNoise1955.Model probe(SingleNoise1955.Patches p,int w,int h){return SingleNoise1955.probe1978(p,w,h,false);}
 static void javaRange(int[] s,int[] o,int w,int h,int b,int e,int vb,int ve,int origin,int n,boolean shadow,SingleNoise1955.Model m,SingleNoise1955.Protection p,SingleNoise1955.Workspace ws){throw new AssertionError("unexpected Java fallback");}
}
