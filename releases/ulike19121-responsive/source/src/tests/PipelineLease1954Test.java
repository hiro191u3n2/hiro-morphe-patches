package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/** Current production pipeline/JNI, with explicit Bitmap counters. No physical
 * Android speed, camera, compression or device-quality claim is made here. */
public final class PipelineLease1954Test {
    static int h25,h27,cases;static long readPixels,fullPixels;
    static final Class<?> WORK;static final Constructor<?> NEW;
    static final Method GPU,CPU,SNAPSHOT,MUTABLE;static final Field CANDIDATE,CPU_BITMAP,FROZEN;
    static {
        try {
            WORK=Class.forName("com.hiro.ulike.QualityPipeline1932$FinishWork1953");
            NEW=WORK.getDeclaredConstructor(Bitmap.class,QualityPixels1932.Plan.class,QualityPixels1932.Plan.class,
                boolean.class,boolean.class,boolean.class,boolean.class);NEW.setAccessible(true);
            GPU=method("gpu");CPU=method("cpu");SNAPSHOT=method("snapshotProbe");
            CANDIDATE=WORK.getDeclaredField("candidate");CANDIDATE.setAccessible(true);
            CPU_BITMAP=WORK.getDeclaredField("cpuBitmap");CPU_BITMAP.setAccessible(true);
            FROZEN=QualityPipeline1932.class.getDeclaredField("FROZEN_1954");FROZEN.setAccessible(true);
            MUTABLE=QualityPipeline1932.class.getDeclaredMethod("mutableOwned",Bitmap.class,Bitmap.class);MUTABLE.setAccessible(true);
        }catch(Exception failed){throw new ExceptionInInitializerError(failed);}
    }
    static Method method(String name)throws Exception {Method m=WORK.getDeclaredMethod(name);m.setAccessible(true);return m;}
    static void p25(boolean v,String why){h25++;if(!v)throw new AssertionError(why);}
    static void p27(boolean v,String why){h27++;if(!v)throw new AssertionError(why);}
    static Bitmap image(int w,int h,int seed){
        Random random=new Random(seed);int[] pixels=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int n=32+random.nextInt(191);pixels[y*w+x]=((x+y)%19==0?0x88000000:0xff000000)
                |(Math.min(255,n+x%9)<<16)|(n<<8)|Math.max(0,n-y%7);
        }
        return Bitmap.from(w,h,pixels,Bitmap.Config.ARGB_8888,true);
    }
    static QualityPixels1932.Plan plan(Bitmap bitmap){return PipelineGpu1953Test.plan(bitmap,true,true);}
    static Object work(Bitmap bitmap,QualityPixels1932.Plan plan,boolean seq,boolean owned)throws Exception {
        return NEW.newInstance(bitmap,plan,seq?plan:null,true,true,seq,owned);
    }
    static Bitmap field(Object object,String name)throws Exception {Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return (Bitmap)f.get(object);}
    static WholeRoute1953.Probe probe(Object work)throws Exception {return (WholeRoute1953.Probe)SNAPSHOT.invoke(work);}
    static int leases()throws Exception {Map<?,?> map=(Map<?,?>)FROZEN.get(null);synchronized(map){return map.size();}}
    static int copies(){int count=0;for(String event:HostAudit1932.EVENTS.get())if(event.startsWith("copy:"))count++;return count;}
    static final WholeRoute1953.Cancellation NEVER=new WholeRoute1953.Cancellation(){public boolean cancelled(){return false;}};

    static void suffix(boolean seq,int height)throws Exception {
        Bitmap source=image(61,height,195425+height);QualityPixels1932.Plan plan=plan(source);
        Object work=work(source,plan,seq,false);source.pixelsRead=0;
        p25(Boolean.TRUE.equals(GPU.invoke(work)),"complete actual JNI route executes");
        long actual=source.pixelsRead;Bitmap candidate=(Bitmap)CANDIDATE.get(work);
        // Native session geometry is deterministic and queried from the actual
        // owner; no predicted device speed or guessed source coverage is used.
        GpuFinish1953.Session session=GpuFinish1953.open(61,height,plan,true,true,seq,512,32);
        p25(session!=null,"actual session reports source-halo geometry");
        long dense=0;int core=session.coreRows(),halo=session.halo();
        for(int first=0;first<height;first+=core){int last=Math.min(height,first+core),origin=Math.max(0,first-halo);dense+=(long)61*(Math.min(height,last+halo)-origin);}
        session.close();readPixels+=actual;fullPixels+=dense;
        p25(actual<=dense,"suffix reads never exceed full source-band reads");
        if(height>core)p25(actual<dense,"native-proved overlap reduces actual Java getPixels pixels");
        CPU.invoke(work);int[] expected=source.snapshot(),out=candidate.snapshot();
        for(int i=0;i<out.length;i++)p25(expected[i]==out[i],"fresh suffix preserves exact CPU final pixel "+i);
        ((WholeRoute1953.Work)work).discardGpu();QualityPipeline1932.recycle1954(source);cases++;
    }
    static void coverageFallback()throws Exception {
        Bitmap source=image(61,1703,981);QualityPixels1932.Plan plan=plan(source);
        GpuFinish1953.Session a=GpuFinish1953.open(61,1703,plan,true,true,true,256,32);
        p25(a!=null,"fallback coverage session exists");
        int core=a.coreRows(),halo=a.halo(),first=core,last=Math.min(1703,first+core),origin=Math.max(0,first-halo);
        p25(a.freshInputOrigin(first,last)==origin,"no source history declines suffix optimization");
        int rows=Math.min(1703,last+halo)-origin;int[] input=new int[61*rows],output=new int[61*(last-first)];
        source.getPixels(input,0,61,0,origin,61,rows);
        FinishPolicy1953.Band policy=FinishPolicy1953.prepare(plan,61,rows,first-origin,last-origin,origin);
        GpuFinish1953.Ticket ticket=a.submit(input,origin,rows,first,last,policy);policy.close();
        p25(ticket!=null&&a.collectCandidate(ticket,output,0),"full-source fallback completes exact private candidate transfer");
        a.close();
        GpuFinish1953.Session b=GpuFinish1953.open(61,1703,plan,true,true,true,256,32);
        p25(b!=null&&b.freshInputOrigin(first,last)==origin,"a new photo token never reuses another photograph's source coverage");
        b.close();QualityPipeline1932.recycle1954(source);cases++;
    }
    static void frozenReference()throws Exception {
        Bitmap source=image(63,789,195427);source.setDensity(320);source.setPremultiplied(false);
        int[] before=source.snapshot();QualityPixels1932.Plan plan=plan(source);Object work=work(source,plan,true,true);
        HostAudit1932.EVENTS.get().clear();WholeRoute1953.Probe proof=probe(work);
        Bitmap input=field(proof,"input"),reference=field(proof,"reference");
        p27(proof!=null&&input==source,"existing private source becomes frozen truth without source snapshot");
        p27(copies()==1,"one CPU destination replaces two pre-save calibration copies");
        p27(reference==(Bitmap)CPU_BITMAP.get(work)&&reference!=source,"CPU output itself owns the future exact reference");
        p27(!proof.captureReference(),"reference cannot become ready before CPU finishes");
        CPU.invoke(work);int[] expected=reference.snapshot();p27(proof.captureReference(),"completed CPU destination becomes exact reference without copy");
        p27(copies()==1,"capturing CPU reference allocates no image copy");
        p27(Arrays.equals(before,source.snapshot()),"CPU does not overwrite frozen input");
        Bitmap changed=(Bitmap)MUTABLE.invoke(null,reference,source);
        p27(changed!=reference,"a frozen reference is copied before a future writable stage");
        changed.setPixels(new int[]{0xffabcdef},0,1,0,0,1,1);
        p27(Arrays.equals(expected,reference.snapshot()),"subsequent mutable stage cannot overwrite retained exact CPU pixels");
        Bitmap rotated=QualityPipeline1932.resample(reference,90,reference.getHeight(),reference.getWidth());
        p27(Arrays.equals(expected,reference.snapshot()),"geometry reads leave frozen reference unchanged");
        QualityPipeline1932.recycle1954(changed);QualityPipeline1932.recycle1954(rotated);
        QualityPipeline1932.recycle1954(source);QualityPipeline1932.recycle1954(reference);
        p27(!source.isRecycled()&&!reference.isRecycled(),"pipeline/final-save disposal transfers to active proof leases");
        for(int mode:new int[]{32,64,0}){
            proof.setDispatchRows(mode);p27(proof.gpu(NEVER),"actual independent mode candidate completes "+mode);
            p27(proof.equal(NEVER),"every mode matches retained exact saved CPU pixels "+mode);
            proof.discardCandidate();p27(!reference.isRecycled()&&!source.isRecycled(),"candidate-only disposal retains shared proof source and reference");
        }
        proof.discard();proof.discard();
        p27(source.isRecycled()&&reference.isRecycled(),"last proof lease completes previously requested source/save disposal exactly once");
        p27(leases()==0,"all frozen ownership records released");cases++;
    }
    static void unknownAndFailures()throws Exception {
        Bitmap source=image(59,399,8754);QualityPixels1932.Plan plan=plan(source);
        Object unknown=work(source,plan,true,false);
        p27(((WholeRoute1953.Work)unknown).probeBytes()==0&&probe(unknown)==null,"unknown upstream mutable owner is never leased");
        Bitmap rgb=Bitmap.from(59,399,source.snapshot(),Bitmap.Config.RGB_565,true);
        Object legacyConfig=work(rgb,plan,true,true);
        p27(((WholeRoute1953.Work)legacyConfig).probeBytes()==0&&probe(legacyConfig)==null,"RGB565 keeps its original write-quantization route and is never reinterpreted as ARGB8888 proof");
        QualityPipeline1932.recycle1954(rgb);
        Object failing=work(source,plan,true,true);Bitmap.failCopyOnce=true;boolean rejected=false;
        try{probe(failing);}catch(InvocationTargetException expected){rejected=true;}
        p27(rejected&&leases()==0&&!source.isRecycled(),"CPU destination allocation/copy failure releases partial source lease");
        Object cpuFail=work(source,plan,true,true);Bitmap.failNextCopyWrite=true;WholeRoute1953.Probe proof=probe(cpuFail);
        Bitmap reference=field(proof,"reference");rejected=false;
        try{CPU.invoke(cpuFail);}catch(InvocationTargetException expected){rejected=true;}
        p27(rejected&&!proof.captureReference(),"failed CPU output never becomes verified saved reference");
        proof.discard();p27(reference.isRecycled()&&!source.isRecycled()&&leases()==0,"CPU failure disposes unpublished CPU target and preserves caller source");
        Object cancelWork=work(source,plan,true,true);proof=probe(cancelWork);CPU.invoke(cancelWork);
        reference=field(proof,"reference");final java.util.concurrent.atomic.AtomicInteger checks=new java.util.concurrent.atomic.AtomicInteger();
        rejected=false;
        try{proof.gpu(new WholeRoute1953.Cancellation(){public boolean cancelled(){return checks.incrementAndGet()>=4;}});}
        catch(IllegalStateException cancelled){rejected=true;}
        p27(rejected,"real queued GPU probe observes cancellation and drains");proof.discard();
        p27(!reference.isRecycled()&&!source.isRecycled(),"cancel before final-save disposal keeps caller output alive");
        QualityPipeline1932.recycle1954(reference);QualityPipeline1932.recycle1954(source);
        p27(leases()==0&&Bitmap.writesAfterRecycle.get()==0,"cancellation releases every lease after actual session completion");cases++;
    }
    static void waitReleased()throws Exception {
        for(int at=0;at<400;at++){if(WholeRoute1953.retainedBytes()==0&&leases()==0)return;Thread.sleep(5);}
        throw new AssertionError("actual finishing route proof ownership did not drain");
    }
    static void normalizeSave(int terminal)throws Exception {
        int w=71,h=399;PhotoDetail.Settings options=PipelineQuality1942Test.options(true,true);
        Bitmap original=image(w,h,77400+terminal),baseline=Bitmap.from(w,h,original.snapshot(),Bitmap.Config.ARGB_8888,true);
        ShotContext1932.Snapshot metadata=PipelineQuality1942Test.metadata(800);
        ShotContext1932.SHOTS.put(original,metadata);ShotContext1932.SHOTS.put(baseline,metadata);
        Field loaded=GpuFinish1953.class.getDeclaredField("loaded");loaded.setAccessible(true);int previous=loaded.getInt(null);
        PipelineQuality1942Test.bind(options,true,w,h);
        ProcessingTiming1947.Trace expectedTrace=ProcessingTiming1947.begin(baseline);
        Bitmap expected;
        loaded.setInt(null,-1);
        try{expected=QualityPipeline1932.normalize(baseline,0,false);}
        finally{loaded.setInt(null,previous);}
        QualityPipeline1932.applyDetail(expected,baseline,options);int[] pixels=expected.snapshot();int baselineCopies=copies();
        ProcessingTiming1947.finish(expectedTrace,true);
        QualityPipeline1932.recycle1954(expected);QualityPipeline1932.recycle1954(baseline);
        PipelineQuality1942Test.bind(options,true,w,h);SaveQueue1935.busy1953=true;
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(original);
        Bitmap actual=QualityPipeline1932.normalize(original,0,false);
        p27(Arrays.equals(pixels,actual.snapshot()),"actual normalize returns exact GPU-disabled CPU pixels "+terminal);
        p27(copies()==baselineCopies+1,"full normalize adds only one CPU destination for optional calibration "+terminal);
        p27(ProcessingTiming1947.traceFor(actual)==trace&&ShotContext1932.forBitmap(actual)==metadata,"returned CPU destination preserves exact trace and capture metadata "+terminal);
        Map<?,?> frozen=(Map<?,?>)FROZEN.get(null);
        synchronized(frozen){p27(frozen.containsKey(actual)&&!frozen.containsKey(original),"normalization returns leased CPU output and never freezes upstream capture "+terminal);}
        int applies=ChromaPipeline177.APPLIES.get();
        p27(QualityPipeline1932.applyDetail(actual,original,options)==actual&&ChromaPipeline177.APPLIES.get()==applies,"PREPARED identity consumes CPU destination without filtering twice "+terminal);
        p27(!original.isRecycled()&&leases()==2,"finishOwned transfers source disposal while keeping upstream capture alive "+terminal);
        if(terminal==2){
            ProcessingTiming1947.Trace next=ProcessingTiming1947.begin(new Object());
            p27(!actual.isRecycled()&&leases()==0,"new capture cancels queued proof without disposing still-saving CPU output");
            ProcessingTiming1947.finish(next,false);QualityPipeline1932.recycle1954(actual);
            ProcessingTiming1947.finish(trace,true);
        }else{
            // Executes the exact disposal helper called from the independently
            // inverse-verified final-save success/failure recycle call sites.
            QualityPipeline1932.recycle1954(actual);
            p27(!actual.isRecycled(),"save disposal remains deferred until exact proof owns no readers "+terminal);
            ProcessingTiming1947.finish(trace,terminal==0);
        }
        SaveQueue1935.busy1953=false;WholeRoute1953.wake();waitReleased();
        p27(actual.isRecycled()&&!original.isRecycled(),"save/cancel terminal drains frozen output while preserving original capture "+terminal);
        p27(ProcessingTiming1947.finished1953(trace)&&Bitmap.writesAfterRecycle.get()==0,"terminal exact trace and all GPU writes finish before disposal "+terminal);
        QualityPipeline1932.recycle1954(original);cases++;
    }
    public static void main(String[] args)throws Exception {
        p25(GpuFinish1953.available(),"current production native JNI library loaded");
        for(boolean seq:new boolean[]{false,true})for(int h:new int[]{37,789,1703})suffix(seq,h);
        coverageFallback();frozenReference();unknownAndFailures();
        for(int terminal=0;terminal<3;terminal++)normalizeSave(terminal);
        SpeedWorkers1935.trim();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+(h25+h27)+",\"h25_assertions\":"+h25
            +",\"h27_assertions\":"+h27+",\"cases\":"+cases+",\"java_source_pixels_read\":"+readPixels
            +",\"full_band_source_pixels\":"+fullPixels+",\"source_halo_suffix_reads_measured\":true,\"native_coverage_fallback_exact\":true"
            +",\"frozen_source_reference_leases_checked\":true,\"reference_snapshot_copies\":0,\"cpu_destination_copies_per_probe\":1"
            +",\"unknown_mutable_owner_never_leased\":true,\"recycle_resize_save_cancel_failure_cleanup_checked\":true"
            +",\"actual_normalize_cpu_destination_prepared_detail_save_cancel_checked\":true"
            +",\"actual_bitmap_gpu_candidate_executed\":true,\"physical_android_tested\":false,\"device_speed_measured\":false}");
    }
}
