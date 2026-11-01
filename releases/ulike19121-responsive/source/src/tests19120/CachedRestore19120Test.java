package com.hiro.ulike;

import java.security.MessageDigest;
import java.util.Arrays;

public final class CachedRestore19120Test {
    private static int checks;
    private static final GpuQualification1961.Cancellation LIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void pass(String key){GpuCertificates1990.pass.add(key);}
    private static void block(String key){GpuCertificates1990.reject.add(key);}
    private static String digest(String value)throws Exception{
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));StringBuilder out=new StringBuilder();
        for(byte b:bytes){out.append(Character.forDigit((b>>>4)&15,16));out.append(Character.forDigit(b&15,16));}return out.toString();
    }
    private static void quietRequests(){
        GpuStrongDefault1990.request1994();GpuStrongDefault1990.Adaptive1994.request();GpuStrongDefault1990.Adaptive1998.request();
        GpuStrongOptimized19119.request();GpuModelDefault1990.requestOptional1994();GpuFinishDefault1990.requestWide1995();
        check(GpuQualification1961.schedules==0,"all automatic request entries are closed");
    }
    private static void legacy(String scenario){
        boolean base=!scenario.equals("none"),full=scenario.equals("full"),rejected=scenario.equals("rejected");
        if(base){
            pass(GpuStrongDefault1990.SUITE);pass(GpuModelDefault1990.SUITE);pass(GpuFinishDefault1990.CPU_SUITE1992);
            pass(GpuStrongDefault1990.Adaptive1994.EXACT);pass(GpuStrongDefault1990.Adaptive1998.EXACT);
            pass(GpuModelDefault1990.layoutSuite1994(1));pass(GpuModelDefault1990.layoutSuite1994(2));
        }
        if(full||rejected){
            pass(GpuStrongDefault1990.Adaptive1994.FAST);pass(GpuStrongDefault1990.Adaptive1998.FAST);
            pass(GpuModelDefault1990.speedSuite1994(1));pass(GpuModelDefault1990.speedSuite1994(2));
            pass(GpuFinishDefault1990.wideSuite1993());
        }
        if(rejected){
            block(GpuStrongDefault1990.Adaptive1994.EXACT);block(GpuStrongDefault1990.Adaptive1998.EXACT);
            block(GpuModelDefault1990.layoutSuite1994(1));block(GpuModelDefault1990.layoutSuite1994(2));
            block(GpuFinishDefault1990.wideSuite1993());
        }
        check(GpuStrongDefault1990.restoreCached19120(LIVE)==base,"standard proof restored only if present");
        check(GpuModelDefault1990.restoreCached19120(LIVE)==base,"model proof restored only if present");
        check(GpuFinishDefault1990.restoreCached19120()==base,"finish proof restored only if present");
        check(GpuStrongDefault1990.Adaptive1994.faster()==full,"tile8 requires exact and speed and non-rejection");
        check(GpuStrongDefault1990.Adaptive1998.faster()==full,"stripe requires exact and speed and non-rejection");
        check(GpuModelDefault1990.admitted1994(1)==full,"model32 requires both certificates");
        check(GpuModelDefault1990.admitted1994(2)==full,"model128 requires both certificates");
        check(GpuFinishDefault1990.wideReady1993()==full,"wide proof cannot be invented");
        if(full)check(GpuNoise1960.prepared.containsAll(Arrays.asList(48,45,11,54,55,56,20,57,58,59)),"all certified process-local programs prepared");
        else check(GpuNoise1960.prepared.isEmpty(),"cold/exact-only/rejected optional programs not compiled");
        check(GpuNoiseOptimized19119.prepareCalls==0,"uncertified new arithmetic not prepared");
        quietRequests();
    }
    private static void optimized(String scenario)throws Exception{
        pass(GpuStrongDefault1990.SUITE);pass(GpuStrongDefault1990.Adaptive1994.EXACT);pass(GpuStrongDefault1990.Adaptive1994.FAST);
        GpuNoise1960.supports(48);
        String exact="strong19119-quotient-tile8-exact70-v1:"+digest(GpuNoiseOptimized19119.identity);
        String fast="strong19119-quotient-tile8-fullphoto-two-banks-two-trials-10pct-v2:"+digest(GpuNoiseOptimized19119.identity);
        boolean full=scenario.equals("optimized-full"),mismatch=scenario.equals("optimized-mismatch");
        if(!scenario.equals("optimized-none"))pass(exact);
        if(full||mismatch||scenario.equals("optimized-rejected"))pass(fast);
        if(scenario.equals("optimized-rejected"))block(exact);
        GpuNoiseOptimized19119.mutate=mismatch;
        check(GpuStrongOptimized19119.restoreCached19120(LIVE)==full,"optimized restore admits only complete matching proof");
        check(GpuStrongOptimized19119.ready(3060,4080)==full,"foreground admission matches restoration result");
        check(!GpuStrongOptimized19119.ready(1000,1000),"uncertified dimensions remain excluded");
        check(GpuNoiseOptimized19119.prepareCalls==(full||mismatch?1:0),"candidate shader only compiled after both saved proofs");
        quietRequests();
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("scenario");
        if(args[0].startsWith("optimized-"))optimized(args[0]);else legacy(args[0]);
        check(GpuCertificates1990.writes==0,"no new exact or speed certificates written");
        check(GpuQualification1961.schedules==0,"no verifier scheduled");
        System.out.println("PASS "+args[0]+" "+checks+" assertions; corpus/write/schedule=0");
    }
}
