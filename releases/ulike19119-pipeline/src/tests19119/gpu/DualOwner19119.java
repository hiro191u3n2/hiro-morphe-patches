package com.hiro.ulike;
import java.util.Arrays;
import java.util.concurrent.*;

/** Real JNI/GLES transport test. Certificates, camera and Android speed are out of scope. */
public final class DualOwner19119 {
    static int assertions;
    static void require(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static int[] pixels(int seed){int[] x=new int[128*64];for(int i=0;i<x.length;i++)x[i]=0xff000000|((i*131+seed)&0xffffff);return x;}
    static void equal(int[] expected,int[] actual,String message){require(Arrays.equals(expected,actual),message);}
    static int[] uniforms(){int[] u=new int[32];u[1]=u[4]=128;u[2]=u[5]=u[16]=64;return u;}
    static GpuNoise1960.Batch rotate(int[] data){return new GpuNoise1960.Batch().upload(0,data).allocate(1,4L*data.length)
        .upload(2,new int[]{0}).upload(4,new float[]{0}).upload(5,new int[]{0})
        .dispatch(3,new int[]{0,1,2,3,4,5},uniforms(),null,data.length);}
    static long phase(boolean candidate,int seed,long otherPool)throws Exception {
        GpuNoise1960.Session s=candidate?GpuNoise1960.openOptimized19119():GpuNoise1960.open();
        require(s!=null,"open "+candidate);require(s.optimized19119()==candidate,"engine token "+candidate);
        require(GpuNoise1960.sessionBusy(),"global busy");
        require(GpuNoise1960.open()==null,"baseline second open refused");
        require(GpuNoise1960.openOptimized19119()==null,"candidate second open refused");
        // Opening the other engine trims its inactive predecessor first. Any
        // quarantined bytes would remain counted, but healthy pools go to zero.
        long[] initial=s.capacity1976();require(initial!=null,"initial capacity");
        otherPool=GpuNoise1960.retainedBytes()-initial[25]-initial[27];
        require(otherPool==0,"inactive other pool trimmed before switch");
        int[] data=pixels(seed);GpuNoise1960.Lease1971 lease=null;
        try {
            lease=s.reserveCapacity1971(new int[]{0,1,2,3,4,5,6},new long[]{4L*data.length,4L*data.length,4,4,4,4,4L*data.length},4L*data.length);
            require(lease!=null,"lease "+candidate);require(lease.revalidate1971(),"lease revalidate");
            require(s.upload(3,new int[]{0}),"dummy upload");
            int[][] direct=s.execute(rotate(data),new int[]{1},new int[]{data.length});
            require(direct!=null,"execute "+candidate);equal(data,direct[0],"actual geometry dispatch "+candidate);
            require(s.allocate(6,4L*data.length),"copy storage");require(s.copy1976(1,6,0,0,data.length),"GPU copy");
            equal(data,s.readInts(6,data.length),"copied words");
            int[] patch={0xff010203,0xffabcdef};require(s.uploadRange1976(6,23,patch,0,2),"range upload");
            int[] patched=data.clone();System.arraycopy(patch,0,patched,23,2);equal(patched,s.readInts(6,data.length),"range exact");
            int[] out=new int[data.length+4];Arrays.fill(out,0x12345678);
            require(s.executeInto(rotate(data),1,data.length,out,2),"execute into");
            equal(data,Arrays.copyOfRange(out,2,2+data.length),"direct destination");require(out[0]==0x12345678&&out[out.length-1]==0x12345678,"guard destination");
            for(int bank=0;bank<2;bank++) {
                GpuNoise1960.Ticket ticket=s.submit(rotate(data),bank);require(ticket!=null,"submit bank "+bank);
                long[] timing=new long[6];int[][] got=s.collectObserved19117(ticket,new int[]{1},new int[]{data.length},timing);
                require(got!=null,"observed collect "+bank);equal(data,got[0],"observed pixels "+bank);
            }
            long[] cap=s.capacity1976();require(cap!=null&&cap.length==28,"capacity contract");
            require(GpuNoise1960.retainedBytes()==otherPool+cap[25]+cap[27],"combined retained sums both contexts");
            require(s.failureCode1971()==0,"per engine failure code");
            lease.close();lease=null;require(GpuNoise1960.reservedBytes1971()==0,"no lease debt");
            require(s.releaseSlots1991(new int[]{6}),"phase release");require(s.capacity1976()[6]==0,"phase released capacity");
        } finally {if(lease!=null)lease.close();s.close();}
        require(!GpuNoise1960.sessionBusy(),"global released");require(GpuNoise1960.reservedBytes1971()==0,"closed lease debt");
        require(s.capacity1976()==null,"closed handle rejected");require(!s.upload(0,data),"closed upload rejected");
        require(s.failureCode1971()==0,"successful close code");return GpuNoise1960.retainedBytes()-otherPool;
    }
    public static void main(String[] args)throws Exception {
        require(GpuNoise1960.warmEnvironment1973(),"baseline environment");String baseline=GpuNoise1960.fingerprint();
        require(GpuNoiseOptimized19119.prepare(),"candidate prepare program48");
        require(!baseline.equals(GpuNoiseOptimized19119.fingerprint()),"independent identities");
        long old=phase(false,1,0);require(old>0,"baseline retained cache");
        long optimized=phase(true,2,old);require(optimized>0,"candidate retained cache");
        require(GpuNoiseOptimized19119.retainedBytes()==optimized,"candidate retained accessor");
        old=phase(false,3,optimized);require(GpuNoise1960.fingerprint().equals(baseline),"baseline identity unchanged");
        optimized=phase(true,4,old);
        GpuNoise1960.trimIdle();require(GpuNoise1960.retainedBytes()==0,"both pools trimmed");
        require(GpuNoiseOptimized19119.retainedBytes()==0,"candidate pool trimmed");
        Thread.currentThread().interrupt();require(GpuNoise1960.open()==null&&GpuNoise1960.openOptimized19119()==null,"interrupted open refused");Thread.interrupted();
        old=phase(false,5,0);phase(true,6,old);GpuNoise1960.trimIdle();
        require(GpuNoise1960.retainedBytes()==0,"trim after reuse");
        System.out.println("RESULT assertions="+assertions+" dualOwner=true physicalAndroid=false");
    }
}
