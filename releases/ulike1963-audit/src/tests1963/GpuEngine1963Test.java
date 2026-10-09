package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import android.graphics.Bitmap;

/** Actual JNI/Mesa execution of cleanup faults; no Android timing claim. */
public final class GpuEngine1963Test {
    private static int assertions;
    private static native void setEngineFault(int mode);
    private static native long[] engineFacts();
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private static void normal(){
        GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"native session opened");
        try{
            String first=GpuNoise1960.fingerprint();check(first.length()>0,"immutable native fingerprint initialized");
            for(int i=0;i<8;i++){
                check(s.upload(0,new int[]{i,i+1,i+2,i+3}),"exact upload "+i);
                check(GpuNoise1960.fingerprint()==first,"strip refresh retains fingerprint object "+i);
                check(Arrays.equals(s.readInts(0,4),new int[]{i,i+1,i+2,i+3}),"unaltered pixels "+i);
            }
        }finally{s.close();}
        check(!GpuNoise1960.sessionBusy(),"ordinary close clears active session");
    }
    private static void mapError(boolean direct){
        GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"readback fault session opened");
        try{
            check(s.upload(0,new int[]{1,2,3,4}),"source uploaded before map fault");
            long unmapped=Native1960Test.faultFacts()[1];setEngineFault(1);
            if(direct){
                int[] privateTarget={73,74,75,76};
                check(!s.executeInto(s.newBatch().allocate(1,16),0,4,privateTarget,0),"mapped error rejects direct readback");
                check(Arrays.equals(privateTarget,new int[]{73,74,75,76}),"map error cannot touch target");
            }else check(s.readInts(0,4)==null,"mapped error rejects allocated readback");
            check(Native1960Test.faultFacts()[1]==unmapped+1,"successful map is unmapped even with GL error");
        }finally{setEngineFault(0);s.close();}
        check(!GpuNoise1960.sessionBusy(),"fault close clears photograph session");
        s=GpuNoise1960.open();check(s!=null,"recoverable map error permits exact fallback/retry");
        try{check(s.upload(0,new int[]{7,8}),"retry upload");check(Arrays.equals(s.readInts(0,2),new int[]{7,8}),"retry pixels exact");}
        finally{s.close();}
    }
    private static void timeout(){
        GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"timeout session opened");
        try{
            GpuNoise1960.Ticket ticket=s.submit(s.newBatch().uploadDirect(0,new int[]{1,2,3,4}),0);
            check(ticket!=null,"private ticket submitted");Native1960Test.setFault(1);
            check(!s.collectInto(ticket,0,4,new int[4],0),"unknown completion rejected");
        }finally{Native1960Test.setFault(0);s.close();}
        check(!GpuNoise1960.sessionBusy(),"closed timeout token cannot leave session busy forever");
        check(GpuNoise1960.retainedBytes()>0,"quarantined storage remains counted");
        check(GpuNoise1960.open()==null,"unsafe timed-out context cannot reopen");
        check(engineFacts()[2]==0,"shared default display never terminated");
    }
    private static void failedInit(){
        check(GpuNoise1960.available(),"native ABI loaded before capability fault");setEngineFault(2);
        check(GpuNoise1960.open()==null,"unsupported context fails closed");
        long[] facts=engineFacts();
        check(facts[0]==1,"failed initialization destroys its private context");
        check(facts[1]==1,"failed initialization destroys its private surface");
        check(facts[2]==0,"failed initialization preserves shared default display");
        check(GpuNoise1960.retainedBytes()==0&&!GpuNoise1960.sessionBusy(),"failed init retains no photograph owner");
    }
    private static void busyProof()throws Exception{
        check(GpuNoise1960.supports(GpuNoise1960.GEOMETRY),"geometry shader supported");
        check(GpuNoise1960.supports(GpuNoise1960.FINISH1961),"finish shader supported");
        java.lang.reflect.Field base=GpuQualification1961.class.getDeclaredField("base");base.setAccessible(true);base.set(null,"gpu-audit1963-pending");
        final CountDownLatch release=new CountDownLatch(1),closed=new CountDownLatch(1);
        SaveQueue1935.busy1953=true;
        check(GpuQualification1961.schedule("existing-proof",4,new GpuQualification1961.Probe(){
            public void run(GpuQualification1961.Cancellation cancellation){try{release.await();}catch(InterruptedException ignored){}}
            public void close(){closed.countDown();}
        }),"one existing proof owns snapshot");
        int[] pixels=new int[16];Arrays.fill(pixels,0xff787878);
        final Bitmap source=Bitmap.from(4,4,pixels,Bitmap.Config.ARGB_8888,false);
        QualityPixels1932.Plan plan=QualityPixels1932.plan(null,100,0,QualityPixels1932.LENS_WIDE,0f,2,2,true,true,1f);
        try{
            int bitmaps=Bitmap.ALL.size();
            check(GpuGeometry1960.resample(source,180,4,4,new GpuGeometry1960.Cpu(){public Bitmap run(){return source;}})==source,"busy proof preserves CPU geometry result");
            check(Bitmap.ALL.size()==bitmaps,"busy proof avoids geometry full-frame snapshot");
            check(GpuChain1961.moireGeometry(source,180,4,4)==null,"busy proof preserves moire geometry fallback");
            check(Bitmap.ALL.size()==bitmaps,"busy proof avoids moire geometry snapshot");
            check(GpuChain1961.finish(source,180,4,4,plan,false)==null,"busy proof preserves complete finish fallback");
            check(Bitmap.ALL.size()==bitmaps,"busy proof avoids finish snapshot and opacity scan");
            check(Arrays.equals(source.snapshot(),pixels)&&!source.isRecycled(),"snapshot avoidance preserves source pixels/ownership");
        }finally{
            release.countDown();GpuQualification1961.captureChanged();SaveQueue1935.busy1953=false;GpuQualification1961.wake();
            check(closed.await(5,java.util.concurrent.TimeUnit.SECONDS),"existing proof lease closes");source.recycle();
        }
    }
    public static void main(String[] args)throws Exception{
        String mode=args.length==0?"normal":args[0];
        if(mode.equals("normal"))normal();else if(mode.equals("map-array"))mapError(false);
        else if(mode.equals("map-direct"))mapError(true);else if(mode.equals("timeout"))timeout();
        else if(mode.equals("init"))failedInit();else if(mode.equals("busy-proof"))busyProof();else throw new IllegalArgumentException(mode);
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"mode\":\""+mode+"\",\"actual_host_gpu_execution\":true}");
    }
}
