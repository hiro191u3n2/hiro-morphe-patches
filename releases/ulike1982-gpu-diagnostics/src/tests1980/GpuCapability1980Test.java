package com.hiro.ulike;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Actual OWNER and facade caches; only the native capability answer is faulted. */
public final class GpuCapability1980Test {
    private static native void fault(int value);
    private static native int calls();
    private static int assertions;
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private static long bits(String name)throws Exception{
        Field field=GpuNoise1960.class.getDeclaredField(name);field.setAccessible(true);return field.getLong(null);
    }
    private static boolean support(String family)throws Exception{
        if("owner".equals(family))return GpuNoise1960.supports(GpuNoise1960.SINGLE);
        Class<?> type="single".equals(family)?GpuSingle1960.class:GpuResidual1961.class;
        Method method=type.getDeclaredMethod("supported");method.setAccessible(true);return (Boolean)method.invoke(null);
    }
    private static long workspace(String family){
        return "single".equals(family)?GpuSingle1960.workspaceBytes(16,16):GpuResidual1961.workspaceBytes(16,16);
    }
    public static void main(String[] args)throws Exception{
        String family=args[0];int failure=Integer.parseInt(args[1]);boolean published=Boolean.parseBoolean(args[2]);
        check(failure==1||failure==3,"fixture must inject an execution failure, not false");
        check(GpuNoise1960.available(),"actual JNI ABI accepted");
        int shader="residual".equals(family)?GpuNoise1960.RESIDUAL1961:GpuNoise1960.SINGLE;
        long bit=1L<<shader;
        fault(failure);
        check(!support(family),"failed owner capability returns safe false");
        check(calls()==1,"fault crossed actual OWNER and JNI exactly once");
        check((bits("knownSupported")&bit)==0,"failure never grants capability");
        check(((bits("knownUnsupported")&bit)!=0)==published,"unknown result is not cached as unsupported in current source");
        fault(0);
        check(support(family)!=published,"current facade retries after transient failure; published facade remains poisoned");
        check(calls()==(published?1:2),"successful recovery reaches native once");
        check(support(family)!=published,"repeated answer remains stable");
        check(calls()==(published?1:2),"true native answer is cached");
        if(!"owner".equals(family)){
            check((workspace(family)>0)!=published,"wrapper workspace recovers with actual capability");
            check(calls()==(published?1:2),"workspace uses common cached answer");
        }

        // A real native false remains authoritative and does not repeat work.
        int before=calls();fault(2);
        check(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS),"true native unsupported answer is false");
        check(calls()==before+1,"native unsupported queried once");
        check((bits("knownUnsupported")&(1L<<GpuNoise1960.ANALYSIS))!=0,"true unsupported bit retained");
        fault(0);
        check(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS),"true unsupported result survives later fixture availability");
        check(calls()==before+1,"true unsupported cache prevents repeat JNI");
        check(GpuNoise1960.supports(GpuNoise1960.GEOMETRY),"independent supported program admitted");
        check(GpuNoise1960.supports(GpuNoise1960.GEOMETRY),"independent positive cache retained");
        check(calls()==before+2,"positive capability queries native once");

        // Pre-existing cancellation never enters OWNER or poisons either cache.
        before=calls();Thread.currentThread().interrupt();
        check(!GpuNoise1960.supports(GpuNoise1960.PROTECTION1961),"cancelled capability request defers");
        check(Thread.currentThread().isInterrupted(),"cancellation signal preserved");
        check(calls()==before,"cancelled request submits no native work");
        check(((bits("knownUnsupported")|bits("knownSupported"))&(1L<<GpuNoise1960.PROTECTION1961))==0,"cancelled capability remains unknown");
        Thread.interrupted();
        check(GpuNoise1960.supports(GpuNoise1960.PROTECTION1961),"later non-cancelled capability may succeed");
        check(calls()==before+1,"cancelled request did not block recovery");
        before=calls();
        check(!GpuNoise1960.supports(-1)&&!GpuNoise1960.supports(GpuNoise1960.PROGRAMS),"invalid programs remain unavailable");
        check(calls()==before,"invalid program does not reach JNI");
        System.out.println("{\"status\":\"passed\",\"family\":\""+family+"\",\"fault\":"+failure+
            ",\"published79\":"+published+",\"assertions\":"+assertions+",\"native_calls\":"+calls()+
            ",\"actual_owner_executed\":true,\"physical_android_tested\":false}");
    }
}
