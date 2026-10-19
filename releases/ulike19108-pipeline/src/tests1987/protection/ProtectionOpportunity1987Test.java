package com.hiro.ulike;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

public final class ProtectionOpportunity1987Test {
    private static final int W=67,H=43,FIRST=3,LAST=37;
    private static void check(boolean value,String message){ProtectionPeers1987.check(value,message);}
    private static QualityPixels1932.Plan plan(int mode) throws Exception {
        QualityPixels1932.NoiseStats stats=new QualityPixels1932.NoiseStats(3.73f,2.4f,90f,.2f,96);
        QualityPixels1932.Plan p=QualityPixels1932.plan(stats,320,14000000L,
            QualityPixels1932.LENS_WIDE,.61f,4,4,true,true,mode==1?.8f:1.4f);
        float[] sigma=new float[12];for(int i=0;i<sigma.length;i++)sigma[i]=.21f+(i*7%17)*.52f;
        SpatialNoise1934 noise=SpatialNoise1934.fromGpu1961(W,H,4,3,sigma,stats);
        p=p.withOutputNoise(noise);
        if(mode!=1) {
            Constructor<?> ctor=FaceRegions1934.Mask.class.getDeclaredConstructor(int.class,int.class,int.class,int.class,
                byte[].class,byte[].class,double.class,double.class,double.class,double.class,double.class,double.class,boolean.class);
            ctor.setAccessible(true);
            byte[] skin=new byte[W*H],detail=new byte[W*H];
            for(int i=0;i<skin.length;i++){skin[i]=(byte)(i*37);detail[i]=(byte)(i*17);}
            QualityPixels1932.RegionMask face=(QualityPixels1932.RegionMask)ctor.newInstance(
                W,H,W,H,skin,detail,1d,0d,0d,0d,1d,0d,true);
            p=p.withFaceRegions(face);
            Class<?> type=Class.forName("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958");
            Constructor<?> smoothCtor=type.getDeclaredConstructor(int.class,int.class);smoothCtor.setAccessible(true);
            Object smooth=smoothCtor.newInstance(W,H);
            Field confidence=type.getDeclaredField("confidence");confidence.setAccessible(true);
            byte[] raster=(byte[])confidence.get(smooth);for(int i=0;i<raster.length;i++)raster[i]=(byte)(i*29);
            p=p.withSmoothedRegions((QualityPixels1932.SmoothMask)smooth);
        }
        if(mode==2)p=p.withFaceRegions(new QualityPixels1932.RegionMask(){
            public int skinQ8(int x,int y){return ((x+y)&1)==0?Integer.MIN_VALUE:131071;}
            public int detailQ8(int x,int y){return -17;}
        });
        return p;
    }
    private static int[] oracle(QualityPixels1932.Plan p) {
        int[] result=new int[W*(LAST-FIRST)*4];
        for(int y=FIRST;y<LAST;y++)for(int x=0;x<W;x++)
            NativeMoire1951.preparePolicy(p,x,y,result,((y-FIRST)*W+x)*4);
        return result;
    }
    private static int[] expand(FinishPolicy1953.Band band) {
        check(band!=null,"Policy fallback must return a complete band");
        int[] result=new int[band.pixels*4];
        for(int i=0;i<band.pixels;i++)for(int lane=0;lane<4;lane++)
            result[i*4+lane]=band.mode==FinishPolicy1953.CONSTANT4?band.words[lane]:
                band.mode==FinishPolicy1953.RAW4?band.words[i*4+lane]:
                band.words[i*2+lane/2]>>>(16*(lane&1))&65535;
        return result;
    }
    private static void same(FinishPolicy1953.Band band,int[] expected) {
        try {int[] actual=expand(band);check(actual.length==expected.length,"Exact full band length");
            for(int i=0;i<actual.length;i++)check(actual[i]==expected[i],"Policy integer mismatch at "+i);
        }finally{if(band!=null)band.close();}
        check(SpeedWorkers1935.owned.isEmpty(),"Every policy lease returned after close");
    }
    private static void closeProbe() {
        if(GpuQualification1961.probe!=null){GpuQualification1961.probe.close();GpuQualification1961.probe=null;}
    }
    private static void blocked(boolean background) throws Exception {
        ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan(0);int[] expected=oracle(p);
        GpuQualification1961.inBackground=background;GpuQualification1961.held=background?0:65536;
        FinishPolicy1953.Band direct=GpuProtection1961.finishBand(p,W,H,FIRST,LAST,0);
        if(direct!=null)direct.close();
        check(direct==null,"Unqualified blocked proof must skip mask export and RAW4 oracle roundtrip");
        check(SpeedWorkers1935.borrowed==0,"No CPU oracle allocations in declined GPU preparation");
        check(GpuNoise1960.executions==0&&GpuQualification1961.queued==0,"No unqualified GPU or blocked probe");
        same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
    }
    private static void qualified(boolean background,boolean memoryOnly) throws Exception {
        ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan(0);int[] expected=oracle(p);
        GpuQualification1961.record=new GpuQualification1961.Record(100000000000L,1L,0);
        GpuNoise1960.output=expected;GpuQualification1961.held=65536;GpuQualification1961.inBackground=background;
        same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
        check(GpuNoise1960.executions==1,"Certified GPU remains usable with retained/background proof");
        check(GpuQualification1961.queued==0,"Admitted GPU does not request another proof");
        if(memoryOnly) {
            GpuQualification1961.record=null;
            same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
            check(GpuNoise1960.executions==2,"Existing admitted in-memory state preserved");
        }
    }
    private static void queueable() throws Exception {
        ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan(0);int[] expected=oracle(p);
        same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
        check(GpuQualification1961.queued==1&&GpuQualification1961.probe!=null,"An eligible detached proof still queues");
        check(GpuNoise1960.executions==0,"Cold foreground returns CPU policy without running GPU");
        closeProbe();
    }
    private static void negativeControls() throws Exception {
        for(int mode=0;mode<4;mode++) {
            ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan(0);int[] expected=oracle(p);
            if(mode==0)GpuQualification1961.allowed=false;
            else if(mode==1)GpuNoise1960.busy=true;
            else if(mode==2)GpuNoise1960.memory=false;
            else GpuNoise1960.enabled=false;
            check(GpuProtection1961.finishBand(p,W,H,FIRST,LAST,0)==null,"Rejected/unavailable GPU declines");
            check(SpeedWorkers1935.borrowed==0,"Refused GPU preparation never allocates oracle");
            same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
            check(GpuNoise1960.executions==0&&GpuQualification1961.queued==0,"Refusal never promotes GPU");
        }
        for(int mode=1;mode<=2;mode++) {
            ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan(mode);int[] expected=oracle(p);
            GpuQualification1961.held=1024;
            same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
        }
    }
    private static void failures() throws Exception {
        for(int failing=1;failing<=2;failing++) {
            ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan(0);int[] expected=oracle(p);
            SpeedWorkers1935.failBorrow=failing;
            same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
            check(GpuNoise1960.executions==0,"Failed cold allocation cannot run GPU");closeProbe();
        }
        ProtectionPeers1987.reset();QualityPixels1932.Plan p=plan(0);int[] expected=oracle(p);
        GpuQualification1961.record=new GpuQualification1961.Record(100000000000L,1L,0);
        GpuNoise1960.executeOom=true;
        same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
        check(GpuNoise1960.executions==1,"Injected admitted GPU failure reached exact CPU fallback");
        ProtectionPeers1987.reset();p=plan(0);expected=oracle(p);
        GpuQualification1961.failRestore=true;
        same(FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0),expected);
        check(GpuNoise1960.executions==0,"Unavailable optional history reaches exact CPU fallback");
        ProtectionPeers1987.reset();p=plan(0);Thread.currentThread().interrupt();
        boolean cancelled=false;
        try{FinishPolicy1953.prepare(p,W,H,FIRST,LAST,0);}catch(IllegalStateException expectedCancel){cancelled=true;}
        check(cancelled&&Thread.currentThread().isInterrupted(),"Caller cancellation is preserved");
        check(GpuNoise1960.executions==0&&SpeedWorkers1935.owned.isEmpty(),"Cancelled call owns no GPU output or policy");
        Thread.interrupted();
    }
    public static void main(String[] args) throws Exception {
        if(args.length>0&&args[0].equals("published86-repro")){blocked(false);return;}
        blocked(false);blocked(true);qualified(false,true);qualified(true,false);queueable();negativeControls();failures();
        check(SpeedWorkers1935.owned.isEmpty(),"No residual policy ownership");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+ProtectionPeers1987.assertions+
            ",\"protection_blocked_probe_skips_duplicate_work1987_verified\":true"+
            ",\"protection_complete_policy_exact1987_verified\":true"+
            ",\"protection_qualified_and_memory_handoff1987_verified\":true"+
            ",\"protection_queue_progress1987_verified\":true"+
            ",\"protection_rejection_and_allocation_fallback1987_verified\":true"+
            ",\"protection_cancellation_and_ownership1987_verified\":true}");
    }
}
