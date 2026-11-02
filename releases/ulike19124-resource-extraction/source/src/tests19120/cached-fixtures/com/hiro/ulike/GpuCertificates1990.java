package com.hiro.ulike;
import java.util.Set;import java.util.HashSet;
final class GpuCertificates1990 {
 static final Set<String> pass=new HashSet<String>(),reject=new HashSet<String>();static int writes;
 static final class Evidence {final int cases=70;final long pixels=1,cpuNanos=100,gpuNanos=70;}
 static Evidence restore(String key){return pass.contains(key)&&!reject.contains(key)?new Evidence():null;}
 static boolean rejected(String key){return reject.contains(key);}
 static boolean passed(String key,int cases,long pixels,long cpu,long gpu){writes++;throw new AssertionError("unexpected certificate write");}
 static void reject(String key,String cause){throw new AssertionError("unexpected comparison rejection");}
}
