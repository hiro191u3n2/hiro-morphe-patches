package com.hiro.ulike;
public final class FinishUnused1953Test {
    private static int assertions;
    private static void need(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        need(FinishPolicy1953.unused(0)==null && FinishPolicy1953.unused(-1)==null,"invalid count declined");
        SpeedWorkers1935.trim();int[] original=SpeedWorkers1935.borrowInts(4);
        for(int i=0;i<4;i++)original[i]=0x12345678+i;
        SpeedWorkers1935.release(original);
        for(int count:new int[]{1,4080*33,Integer.MAX_VALUE}){
            FinishPolicy1953.Band b=FinishPolicy1953.unused(count);
            need(b!=null && b.mode==FinishPolicy1953.CONSTANT4 && b.pixels==count,"unused policy dimensions");
            need(b.words.length==4 && b.policyBytes==16 && b.denseBytes==(long)count*16,"constant workspace independent of pixel count");
            need(!b.rawFallback,"unused policy never recomputes");
            for(int word:b.words)need(word==0,"pooled stale words zeroed");
            b.close();b.close();
        }
        need(NativeMoire1951.calls==0,"no per-pixel preparePolicy invocation");
        int[] same=SpeedWorkers1935.borrowInts(4),other=SpeedWorkers1935.borrowInts(4);
        need(same==original && other!=same,"pooled lease closes once");
        SpeedWorkers1935.release(same);SpeedWorkers1935.release(other);
        Thread.currentThread().interrupt();
        try {FinishPolicy1953.unused(4080*33);throw new AssertionError("interrupt ignored");}
        catch(IllegalStateException expected){need("quality interrupted".equals(expected.getMessage()),"interruption honored before lease");}
        need(Thread.interrupted(),"interrupt flag retained");
        need(NativeMoire1951.calls==0,"interruption still doesn't call policy");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"unused_sharp_policy_zero_prepare_calls\":true,\"unused_policy_constant_bytes\":16}");
    }
}
