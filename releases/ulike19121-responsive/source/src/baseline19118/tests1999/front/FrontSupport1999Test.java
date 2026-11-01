package com.hiro.ulike;
import java.lang.reflect.Field;
/** Executes the real scalar cache accessor without a native capability query. */
public final class FrontSupport1999Test {
    static void check(boolean b){if(!b)throw new AssertionError("cached support observation");}
    static Field field(String n)throws Exception{Field f=GpuNoise1960.class.getDeclaredField(n);f.setAccessible(true);return f;}
    public static void main(String[] args)throws Exception {
        field("knownSupported").setLong(null,0);field("knownUnsupported").setLong(null,0);
        check(!GpuNoise1960.unsupported1999(GpuNoise1960.SINGLE));
        field("knownUnsupported").setLong(null,1L<<GpuNoise1960.SINGLE);
        check(GpuNoise1960.unsupported1999(GpuNoise1960.SINGLE));
        check(!GpuNoise1960.unsupported1999(GpuNoise1960.RESIDUAL1961));
        check(!GpuNoise1960.unsupported1999(-1));check(!GpuNoise1960.unsupported1999(127));
        field("knownSupported").setLong(null,1L<<GpuNoise1960.RESIDUAL1961);
        check(!GpuNoise1960.unsupported1999(GpuNoise1960.RESIDUAL1961));
        check(field("knownUnsupported").getLong(null)==1L<<GpuNoise1960.SINGLE);
        check(field("knownSupported").getLong(null)==1L<<GpuNoise1960.RESIDUAL1961);
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":8,\"mode\":\"real-cached-support\"}");
    }
}
