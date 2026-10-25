package com.hiro.ulike;
/** Optional sink for the separate actual-GpuStrong ownership proof. */
public final class ResidentTiming19111 {
 static int calls,cpuCalls,fault;static String key;static long[] values;
 public static void dispatch(String name,long[] counts){calls++;key=name;if(name.equals("cpuStrongCounts19115")){cpuCalls++;values=counts.clone();}if(fault==1)throw new IllegalStateException("controlled optional sink failure");if(fault==2)throw new LinkageError("controlled optional linkage failure");}
}
