package com.hiro.ulike;
import java.util.*;

final class GpuQualification1961 {interface Cancellation {boolean cancelled();}}
final class ResidentProof1978 {static final int UNAVAILABLE=0,EXACT=1,MISMATCH=-1;}
final class StrongNoise1958 {
 static final class Model {final int width;final int[][] maps;private final float[] evidence,runtimeEvidence;
  Model(int w,int[][] m,float[] e,float[] r){width=w;maps=m;evidence=e;runtimeEvidence=r;}}
 static int[][] gpuMaps1960(Model model){return model.maps;}
}
final class QualityPixels1932 {
 static final class Mask {byte[] cheek,skin;Mask(byte[] a,byte[] b){cheek=a;skin=b;}}
 static final class Local {float amount=1;int level=2;float[] optionalWeights;}
 static final class Plan {int count=4;boolean enabled=true;float value=.5f;double exact=1.25;float[] weights={.25f,.5f};Object faceRegions;Local localNoise=new Local();}
}
final class GpuNoise1960 {
 static final int GEOMETRY=17;static boolean memory=true,supported=true;static int workspaceCalls;static long lastWorkspace;
 static boolean workspaceFits(long size){workspaceCalls++;lastWorkspace=size;return memory&&size>=0;}
 static boolean supports(int program){return supported&&program==GEOMETRY;}
 static void reset(){memory=true;supported=true;workspaceCalls=0;lastWorkspace=0;}
 static final class Batch {
  final Map<Integer,Long> allocations=new LinkedHashMap<Integer,Long>();int program=-1;int[] slots,words;int work;
  Batch allocate(int slot,long bytes){allocations.put(slot,bytes);return this;}
  Batch dispatch(int program,int[] slots,int[] words,float[] unused,int work){this.program=program;this.slots=slots.clone();this.words=words.clone();this.work=work;return this;}
 }
 static final class Lease1971 {final Session owner;Lease1971(Session s){owner=s;owner.liveLeases++;}boolean revalidate1971(){owner.revalidations++;return !owner.invalidate;}void close(){owner.closes++;owner.liveLeases--;}}
 static final class Session {
  final Map<Integer,int[]> values=new HashMap<Integer,int[]>();int reserve,allocations,dispatches,reads,releases,closes,revalidations,liveLeases;
  boolean deny,invalidate,failAllocate,failDispatch,nullRead,shortRead,throwRead,cancelOnRead,failRelease;StageDiagnostic19104Test.Cancel cancellation;
  final List<Integer> batchStarts=new ArrayList<Integer>(),batchRows=new ArrayList<Integer>();
  Lease1971 reserveCapacity1983(int[] slots,long[] limits,long[] previous,long javaPeak){reserve++;if(slots.length!=2||slots[0]!=22||slots[1]!=23||limits[0]!=4||limits[1]>32L*8192*4||javaPeak<=0)throw new AssertionError("unbounded or wrong scratch reservation");return deny?null:new Lease1971(this);}
  boolean run(Batch batch){
   if(!batch.allocations.isEmpty()){allocations++;if(failAllocate)return false;for(Map.Entry<Integer,Long> e:batch.allocations.entrySet())values.put(e.getKey(),new int[(int)(e.getValue()/4)]);return true;}
   dispatches++;if(failDispatch)return false;int[] u=batch.words;int width=u[1],height=u[2],from=u[15],to=u[16];
   if(batch.program!=GEOMETRY||u[0]!=0||u[4]!=width||u[5]!=height||u[20]!=1||from<0||to<=from||to>height||to-from>32||batch.work!=width*(to-from)||batch.slots.length!=6||batch.slots[1]!=23)throw new AssertionError("raw mode0 crop contract changed");
   for(int i=2;i<6;i++)if(batch.slots[i]!=22)throw new AssertionError("scratch slots changed");
   int[] source=values.get(batch.slots[0]),target=values.get(23);System.arraycopy(source,from*width,target,0,width*(to-from));batchStarts.add(from);batchRows.add(to-from);return true;
  }
  int[] readInts(int slot,int count){reads++;if(throwRead)throw new IllegalStateException("read failure");if(cancelOnRead&&cancellation!=null)cancellation.value=true;if(nullRead)return null;int[] result=Arrays.copyOf(values.get(slot),count);return shortRead?Arrays.copyOf(result,Math.max(0,count-1)):result;}
  boolean releaseSlots1991(int[] slots){releases++;if(liveLeases!=0)throw new AssertionError("scratch released while lease live");if(failRelease)return false;for(int slot:slots){if(slot!=22&&slot!=23)throw new AssertionError("source slot released");values.remove(slot);}return true;}
 }
}
