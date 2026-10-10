package com.hiro.ulike;

import com.hiro.ulike.GpuStrong1960.Forecast1983;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Map;

/** Same production and frozen matrix assertion in both modes. Controlled
 * history eliminates dependence on host/JIT speed; no sleeping, altered route
 * expectation, production clock or foreground policy is used. */
public final class StrongCaseIsolation1986Test {
    static final long MS=1000000L,CPU=2000*MS;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Map<?,?> recent()throws Exception{return (Map<?,?>)field(Forecast1983.class,"RECENT").get(null);}
    static StrongDiagnostics1982Test.Data stage()throws Exception{
        StrongDiagnostics1982Test.reset();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();
        GpuStrong1960.beginStage(d.model);StrongDiagnostics1982Test.seed(d,0,3);StrongDiagnostics1982Test.seed(d,1,3);return d;
    }
    static String key(StrongDiagnostics1982Test.Data d){
        GpuQualification1961.Record proof=GpuQualification1961.restore(d.key(0));
        // The frozen matrix uses beginStage, whose worker count is one, and
        // no bank is owned during route selection: the actual production key.
        return Forecast1983.key(GpuNoise1960.fingerprint(),d.key(0),proof.variant,1,1,proof.cpuNanos,proof.gpuNanos);
    }
    static String digest(int[] output,int[] confidence)throws Exception{
        MessageDigest sha=MessageDigest.getInstance("SHA-256");
        for(int[] values:new int[][]{output,confidence})for(int value:values){
            sha.update((byte)(value>>>24));sha.update((byte)(value>>>16));sha.update((byte)(value>>>8));sha.update((byte)value);
        }
        StringBuilder text=new StringBuilder();for(byte value:sha.digest())text.append(String.format("%02x",value&255));return text.toString();
    }
    public static void main(String[] args)throws Exception{
        boolean isolated="isolated".equals(args[0]);check(isolated||"unisolated".equals(args[0]),"explicit fixture mode required");
        recent().clear();StrongDiagnostics1982Test.Data d=stage();String forecast=key(d);
        check(d.route().profile==0,"without history the original p0=1ms / p1=2ms scenario chooses p0");
        long now=System.nanoTime();
        Forecast1983.completed(forecast,201,5*MS,CPU,now-2*MS);
        Forecast1983.completed(forecast,202,5*MS,CPU,now-MS);
        Object entry=recent().get(forecast);
        check(entry!=null&&!field(entry.getClass(),"slow").getBoolean(entry),"two 5ms observations do not enter slow cooling against the 2s CPU proof");
        check(Forecast1983.cost(forecast,MS,System.nanoTime())==5*MS,"the actual two-capture forecast remains authoritative within one scenario");
        check(d.route().profile==1&&recent().get(forecast)==entry,"the isolated fixture does not clear history on route lookup or suppress legitimate p1 selection");
        long sequence=Forecast1983.capture();StrongDiagnostics1982Test.end(d);
        d=stage();
        check(forecast.equals(key(d)),"next scenario recreates exactly the same driver/profile/variant/workers/bank/proof key");
        check(Forecast1983.capture()>sequence,"case isolation never rewinds production capture identity");
        check(isolated?recent().isEmpty():recent().get(forecast)==entry,"only the added scenario boundary removes earlier-case forecast ownership");
        GpuStrong1960.Route1981 route=d.route();
        boolean oldAssertionFailed=false;
        try{
            // This is the original, unchanged case15 predicate and message.
            StrongDiagnostics1982Test.check(route!=null&&route.profile==0,"same lowest complete-cost certified profile: 15");
        }catch(AssertionError expected){
            if(!"same lowest complete-cost certified profile: 15".equals(expected.getMessage()))throw expected;
            oldAssertionFailed=true;
        }
        check(oldAssertionFailed!=isolated,"old case15 assertion fails without reset isolation and passes with it");
        check(route!=null&&route.profile==(isolated?0:1),"seeded failure selects the legitimate alternative profile, not a missing or CPU route");
        check(d.process(route),"the chosen profile completes through the actual dispatcher");d.exact();
        String output=digest(d.out,d.confidence);StrongDiagnostics1982Test.end(d);
        check(d.calls==0&&ProcessingTiming1947.gpu==1&&ProcessingTiming1947.cpu==0&&ProcessingTiming1947.verification==0,"isolation changes neither committed GPU accounting nor foreground comparison policy");
        check(GpuQualification1961.retainedBytes()==0&&GpuNoise1960.leases==0&&GpuNoise1960.active==0,"both fixture modes release every real ownership path");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"mode\":\""+args[0]+"\",\"old_expected_assertion_failed\":"+oldAssertionFailed+",\"within_case_forecast_preserved\":true,\"selected_profile\":"+route.profile+",\"output_sha256\":\""+output+"\"}");
    }
}
