package com.hiro.ulike;

import java.util.Arrays;

/** Execute the production mapped transfer and direct readback ownership paths. */
public final class Transfer1962Test {
    private static int assertions;
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private static int[] values(int n){int[] out=new int[n];for(int i=0;i<n;i++)out[i]=i*0x19abcdef^0x87654321;return out;}
    private static void sentinels(int[] out,int offset,int[] expected){
        for(int i=0;i<out.length;i++)check(out[i]==(i>=offset&&i<offset+expected.length?expected[i-offset]:0x42424242),"exact transfer/offset sentinel "+i);
    }
    private static void normal(){
        check(GpuNoise1960.available(),"production JNI available");
        for(int n:new int[]{1,63,64,65,513,4097}){
            GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"private transfer session");
            try{
                int[] source=values(n),target=new int[n+10];Arrays.fill(target,0x42424242);
                check(s.executeInto(s.newBatch().uploadDirect(0,source),0,n,target,5),"mapped integers/direct readback");
                sentinels(target,5,source);
                check(!s.executeInto(s.newBatch().uploadDirect(0,source),0,n,target,-1),"negative offset rejected");
                check(!s.executeInto(s.newBatch().uploadDirect(0,source),0,n,target,11),"overflow range rejected");
                float[] floats=new float[n];int[] raw=new int[n];
                for(int i=0;i<n;i++){raw[i]=(i&1)==0?0x3eaaaaab+(i&255):0x80000000;floats[i]=Float.intBitsToFloat(raw[i]);}
                Arrays.fill(target,0x42424242);
                check(s.executeInto(s.newBatch().uploadDirect(1,floats),1,n,target,5),"mapped float bytes/direct readback");
                sentinels(target,5,raw);
            }finally{s.close();}
        }
        GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"two bank session");
        try{
            int[] a=values(211),b=values(317),expectedA=a.clone(),expectedB=b.clone();
            GpuNoise1960.Ticket first=s.submit(s.newBatch().uploadDirect(0,a),0);
            GpuNoise1960.Ticket second=s.submit(s.newBatch().uploadDirect(14,b),1);
            check(first!=null&&second!=null,"two disjoint uploads submitted");
            Arrays.fill(a,0);Arrays.fill(b,0); // submit has finished all source snapshots
            int[] outA=new int[221],outB=new int[327];Arrays.fill(outA,0x42424242);Arrays.fill(outB,0x42424242);
            check(s.collectInto(first,0,211,outA,5),"first ticket exact direct collection");
            check(s.collectInto(second,14,317,outB,5),"second ticket exact direct collection");
            sentinels(outA,5,expectedA);sentinels(outB,5,expectedB);
            check(!s.collectInto(first,0,211,outA,5),"ticket cannot be collected twice");
        }finally{s.close();}
    }
    private static void fault(String mode){
        GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"fault session available");
        int[] external={71,72,73,74},privateCandidate=new int[4];
        try{
            if("upload".equals(mode)){
                Native1960Test.setFault(2);
                check(!s.run(s.newBatch().uploadDirect(0,new int[]{1,2,3,4})),"failed mapped input cannot publish");
            }else{
                check(s.upload(0,new int[]{1,2,3,4}),"old upload baseline ready");
                GpuNoise1960.Ticket t=s.submit(s.newBatch().allocate(1,16),0);check(t!=null,"private ticket ready");
                Native1960Test.setFault("timeout".equals(mode)?1:2);
                boolean ok=s.collectInto(t,0,4,privateCandidate,0);
                if(ok)System.arraycopy(privateCandidate,0,external,0,4);
                check(!ok,"late direct readback failure rejected");
            }
            check(Arrays.equals(external,new int[]{71,72,73,74}),"external image not committed on transfer failure");
        }finally{Native1960Test.setFault(0);s.close();}
    }
    public static void main(String[] args){
        if(args.length==0)normal();else fault(args[0]);
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"mapped_transfer_executed\":true,\"device_speedup_verified\":false}");
    }
}
