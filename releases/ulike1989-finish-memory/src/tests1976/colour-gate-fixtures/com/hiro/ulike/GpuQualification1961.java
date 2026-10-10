package com.hiro.ulike;
import java.util.*;
public final class GpuQualification1961 {
 public interface Cancellation{boolean cancelled();}public interface Probe{void run(Cancellation c);void close();}
 public static final class Record{public final long cpuNanos,gpuNanos;public final int variant;Record(long a,long b,int v){cpuNanos=a;gpuNanos=b;variant=v;}}
 static final Map<String,Record> records=new HashMap<>();static final Set<String> exact=new HashSet<>(),slow=new HashSet<>();
 static String pendingKey;static Probe pending;static long pendingBytes;static boolean running;static int qualified;
 public static boolean background(){return running;}public static Record restore(String k){return exact.contains(k)?null:records.get(k);}
 public static boolean canQueue(String k,long bytes){return !running&&pending==null&&!exact.contains(k)&&!slow.contains(k)&&restore(k)==null&&bytes<=96L*1024*1024;}
 public static boolean schedule(String k,long bytes,Probe p){if(!canQueue(k,bytes)){p.close();return false;}pending=p;pendingKey=k;pendingBytes=bytes;return true;}
 public static void qualified(String k,long a,long b,int v){if(!running||!k.equals(pendingKey)||b>a-a/20)throw new AssertionError("invalid proof");records.put(k,new Record(a,b,v));qualified++;}
 public static void rejectExact(String k){exact.add(k);}public static void rejectSpeed(String k){slow.add(k);}
 static void run(int cancelAt){if(pending==null)throw new AssertionError("no queued proof");Probe p=pending;running=true;final int[] checks={0};try{p.run(()->cancelAt>0&&++checks[0]>=cancelAt);}finally{running=false;p.close();pending=null;pendingBytes=0;}}
 static void reset(){if(pending!=null)pending.close();pending=null;pendingKey=null;pendingBytes=0;running=false;qualified=0;records.clear();exact.clear();slow.clear();GpuNoise1960.fit=true;}
}
