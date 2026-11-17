package app.hiro.newxui;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

/** Host JVM microbenchmark; not an Android frame-time or device benchmark. */
public final class ViewCountFormatBenchmark {
    private static final int WARMUP=20000, CALLS=50000, ROUNDS=5;
    private static final Long[] COUNTS=new Long[256];
    private static volatile long sink;
    private static Object memoryBean;
    private static Method allocatedBytes;
    static {
        for(int i=0;i<COUNTS.length;i++) COUNTS[i]=Long.valueOf(1000L+i*1234567L);
        try {
            Class<?> extension=Class.forName("com.sun.management.ThreadMXBean");
            Object bean=ManagementFactory.getThreadMXBean();
            if(extension.isInstance(bean) && ((Boolean)extension.getMethod("isThreadAllocatedMemorySupported").invoke(bean)).booleanValue()) {
                if(!((Boolean)extension.getMethod("isThreadAllocatedMemoryEnabled").invoke(bean)).booleanValue())
                    extension.getMethod("setThreadAllocatedMemoryEnabled",boolean.class).invoke(bean,Boolean.TRUE);
                memoryBean=bean;
                allocatedBytes=extension.getMethod("getThreadAllocatedBytes",long.class);
            }
        } catch(ReflectiveOperationException ignored) {}
    }
    private static long bytes() {
        if(allocatedBytes==null) return -1;
        try { return ((Long)allocatedBytes.invoke(memoryBean,Long.valueOf(Thread.currentThread().getId()))).longValue(); }
        catch(ReflectiveOperationException ignored) { return -1; }
    }
    private static String baseline(Long rawCount) {
        // Same formatter path as v380; deliberately excludes discarded native compact text.
        if(rawCount==null || rawCount.longValue()<0 || !UiOptions.fullViewCounts()) return null;
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(rawCount.longValue());
    }
    private static long consume(boolean old,int calls) {
        long checksum=0;
        for(int i=0;i<calls;i++) {
            Long count=COUNTS[i & (COUNTS.length-1)];
            String value=old?baseline(count):ViewCountFormat.formatOrNull(count);
            checksum+=value.hashCode();
        }
        sink=checksum;
        return checksum;
    }
    private static long[] measure(boolean old) {
        long beforeBytes=bytes(),start=System.nanoTime();
        long checksum=consume(old,CALLS);
        long elapsed=System.nanoTime()-start,afterBytes=bytes();
        return new long[]{elapsed,beforeBytes<0 || afterBytes<0 ? -1 : afterBytes-beforeBytes,checksum};
    }
    private static long median(long[] values) { long[] copy=values.clone();Arrays.sort(copy);return copy[copy.length/2]; }
    private static String array(long[] values) { return Arrays.toString(values); }
    public static void main(String[] args) {
        Locale saved=Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);UiOptions.register();
            for(int i=0;i<3;i++) { consume(true,WARMUP);consume(false,WARMUP); }
            long[] oldTime=new long[ROUNDS],newTime=new long[ROUNDS],oldBytes=new long[ROUNDS],newBytes=new long[ROUNDS];
            for(int i=0;i<ROUNDS;i++) {
                long[] old,newer;
                if((i & 1)==0) { old=measure(true);newer=measure(false); }
                else { newer=measure(false);old=measure(true); }
                if(old[2]!=newer[2]) throw new AssertionError("formatter output checksum mismatch");
                oldTime[i]=old[0];oldBytes[i]=old[1];newTime[i]=newer[0];newBytes[i]=newer[1];
            }
            long oldAllocation=median(oldBytes),newAllocation=median(newBytes);
            if(oldAllocation>=0 && newAllocation>=oldAllocation) throw new AssertionError("cached formatter unexpectedly allocates as much as per-call construction");
            System.out.println("{\"environment\":\"host JVM\",\"java_version\":\""+System.getProperty("java.version")+"\",\"locale\":\"en-US\",\"calls_per_round\":"+CALLS+",\"rounds\":"+ROUNDS+",\"warmup_calls_per_variant\":"+(WARMUP*3)+",\"baseline_ns\":"+array(oldTime)+",\"optimized_ns\":"+array(newTime)+",\"baseline_allocated_bytes\":"+array(oldBytes)+",\"optimized_allocated_bytes\":"+array(newBytes)+",\"baseline_median_ns\":"+median(oldTime)+",\"optimized_median_ns\":"+median(newTime)+",\"baseline_median_allocated_bytes\":"+oldAllocation+",\"optimized_median_allocated_bytes\":"+newAllocation+",\"thread_allocation_measurement_supported\":"+(oldAllocation>=0)+",\"native_bridge_work_measured\":false,\"physical_device_tested\":false}");
        } finally { Locale.setDefault(saved); }
    }
}
