package com.hiro.ulike;
import java.io.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Actual Strong facade, actual bounded worker pool, JNI and GLSL. CPU expected
 * pixels come from frozen published Java; delays/faults are explicit controls. */
public final class NativeSpeed1978Test {
    static int assertions;static native int dispatches();static native void delay(int ms);static native void failRead();static native void clearFault();
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> owner,String name)throws Exception{Field f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
    static final class Data {
        int[] u,want,confidence;int[][] input;
        StrongNoise1958.Model model(){StrongNoise1958.Model m=new StrongNoise1958.Model();m.height=u[7];m.maps=new int[][]{input[4],input[5],input[6]};m.evidence=new float[input[2].length];for(int i=0;i<m.evidence.length;i++)m.evidence[i]=Float.intBitsToFloat(input[2][i]);m.frozenPixels=want;m.frozenConfidence=confidence;return m;}
    }
    static Data data(String path)throws Exception{
        try(DataInputStream in=new DataInputStream(new FileInputStream(path))){check(in.readInt()==1960001,"frozen oracle format");
            for(;;){String name=in.readUTF();if(name.isEmpty())throw new AssertionError("Strong oracle absent");int shader=in.readInt();int[] u=NativeFacadeReplay1978.ints(in);in.readInt();int n=in.readInt();int[][] input=new int[n][];for(int i=0;i<n;i++)input[i]=NativeFacadeReplay1978.ints(in);int[] want=NativeFacadeReplay1978.ints(in),cf=NativeFacadeReplay1978.ints(in);
                if(shader==0&&u[10]==3&&want.length>32&&u[8]>0){Data d=new Data();d.u=u;d.input=input;d.want=want;d.confidence=cf;return d;}}
        }
    }
    static Object stage()throws Exception{return field(GpuStrong1960.class,"active").get(null);}
    static void reset()throws Exception{check(stage()==null,"previous stage drained");((Map<?,?>)field(GpuStrong1960.class,"GATES").get(null)).clear();GpuQualification1961.reset();GpuStrongTuning1975.chosen=null;ProcessingTiming1947.reset();WholeRoute1953.retained=0;StrongNoise1958.contendCpu=false;clearFault();}
    static String seed(Data d,int profile,int variant){String exact=GpuStrong1960.profileKey1973(d.u,profile);GpuQualification1961.qualifiedStrongPreferred1970(exact,100,200,variant);GpuStrongTuning1975.chosen=new GpuStrongTuning1975.Choice(profile,variant,exact);return exact;}
    static boolean process(Data d,StrongNoise1958.Model model,int[] out,int[] cf,GpuStrong1960.Oracle oracle){return GpuStrong1960.process(d.input[0],out,d.u[0],d.u[1],d.u[2],d.u[3],d.u[4],d.u[5],d.u[6],d.u[8],d.u[9]!=0,model,d.input[3],cf,null,oracle);}
    static void assertPixels(Data d,int[] out,int[] cf){check(Arrays.equals(d.want,Arrays.copyOfRange(out,d.u[2]*d.u[0],d.u[3]*d.u[0])),"all ARGB equal frozen CPU");check(Arrays.equals(d.confidence,Arrays.copyOf(cf,d.confidence.length)),"all confidence equal frozen CPU");for(int i=d.confidence.length;i<cf.length;i++)check(cf[i]==0x2468ace0,"pooled confidence tail preserved");}
    static void lifecycle(Data d,String mode)throws Exception{
        reset();final StrongNoise1958.Model model=d.model();final int[] calls={0};int[] out=new int[d.input[0].length],cf=new int[d.confidence.length+13];Arrays.fill(out,0x13579bdf);Arrays.fill(cf,0x2468ace0);
        boolean certified="reuse".equals(mode)||"readback".equals(mode)||"memory".equals(mode);String exact=certified?seed(d,7,0):GpuStrong1960.profileKey1973(d.u,7);
        GpuStrong1960.Oracle oracle=(p,c)->{calls[0]++;check(!Thread.holdsLock(stageUnchecked()),"CPU oracle outside stage monitor");if("false".equals(mode))return false;if("throw".equals(mode))throw new IllegalStateException("controlled CPU failure");System.arraycopy(d.want,0,p,d.u[2]*d.u[0],d.want.length);System.arraycopy(d.confidence,0,c,0,d.confidence.length);if("interrupt".equals(mode))Thread.currentThread().interrupt();return true;};
        boolean completed=false,cancelled=false;GpuNoise1960.Session observed=null;GpuStrong1960.beginStage(model);
        try{
            if("memory".equals(mode))WholeRoute1953.retained=Long.MAX_VALUE;
            if("readback".equals(mode))failRead();
            completed=process(d,model,out,cf,oracle);
            if("reuse".equals(mode)){
                check(completed,"certified first strip completes");Object owner=stage();Object[][] before=(Object[][])field(owner.getClass(),"readbacks1975").get(owner);Object bank=before[0][0];int[] pixels=((int[][])field(bank.getClass(),"values").get(bank))[0];
                Arrays.fill(out,0x13579bdf);Arrays.fill(cf,0x2468ace0);check(process(d,model,out,cf,(p,c)->{throw new AssertionError("certified GPU reran CPU proof");}),"second certified strip completes");
                Object[][] after=(Object[][])field(owner.getClass(),"readbacks1975").get(owner);check(after[0][0]==bank&&((int[][])field(bank.getClass(),"values").get(bank))[0]==pixels,"same-bank full native readback storage reused");
            }
        }catch(CancellationException expected){cancelled=true;}
        finally{Object owner=stage();if(owner!=null)observed=(GpuNoise1960.Session)field(owner.getClass(),"session").get(owner);WholeRoute1953.retained=0;GpuStrong1960.endStage(model);Thread.interrupted();clearFault();}
        check(GpuNoise1960.reservedBytes1971()==0&&!GpuNoise1960.sessionBusy(),"all native/Java leases and stage released");
        if(observed!=null){Object[] pending=(Object[])field(GpuNoise1960.Session.class,"pending").get(observed);check(pending[0]==null&&pending[1]==null,"both native ticket banks drained");}
        check(ProcessingTiming1947.verification==0,"no foreground proof counter");
        if("interrupt".equals(mode))check(cancelled&&!completed,"CPU cancellation propagated before commit");
        else if("false".equals(mode)||"throw".equals(mode)||"readback".equals(mode)||"memory".equals(mode))check(!completed&&!cancelled,"incomplete result requests existing caller repair");
        else{check(completed&&!cancelled,"complete valid result");assertPixels(d,out,cf);}
        check(calls[0]==(certified?0:1),"only ordinary deferred CPU calculation; certified routes do not prove");
        check((GpuQualification1961.restore(exact)!=null)==certified,"no forged certificate and transport errors preserve exact evidence");
        if("readback".equals(mode)||"memory".equals(mode))for(int pixel:out)check(pixel==0x13579bdf,"private failed GPU output not committed");
    }
    static Object stageUnchecked(){try{return stage();}catch(Exception e){throw new AssertionError(e);}}
    static void route(Data d,String mode)throws Exception{
        reset();StrongNoise1958.Model model=d.model();String exact=seed(d,7,0);
        GpuQualification1961.qualified(GpuStrongRouting1978.key(d.u,7,0,4,GpuQualification1961.restore(exact)),200,100,1);
        boolean mixed="route-mixed".equals(mode),resident="route-resident".equals(mode);final int[] calls={0};
        int[] out=new int[d.input[0].length],cf=new int[d.confidence.length+13];Arrays.fill(out,0x13579bdf);Arrays.fill(cf,0x2468ace0);
        GpuStrong1960.beginStage(model);GpuStrong1960.configureWorkers1978(model,"route-workers".equals(mode)?3:4);
        final Object owner=stage();final boolean[] banks=(boolean[])field(owner.getClass(),"banks").get(owner);Thread release=null;
        int nativeBefore=dispatches();
        try{
            int height=Math.max(d.u[7],d.u[6]+d.u[3]);if(resident)check(GpuStrong1960.enableResident1976(model,d.u[0],height),"resident slot24 allocated by actual facade");
            // The admission state is controlled here. The separate cohort test
            // exercises real simultaneous native bank use and worker joining.
            synchronized(owner){banks[0]=banks[1]=true;}
            if(!mixed){release=new Thread(()->{try{Thread.sleep(60);}catch(InterruptedException e){throw new AssertionError(e);}synchronized(owner){banks[0]=false;owner.notifyAll();}},"fixture-bank-release");release.start();}
            check(process(d,model,out,cf,(p,c)->{calls[0]++;check(!Thread.holdsLock(owner),"mixed CPU runs outside GPU monitor");System.arraycopy(d.want,0,p,d.u[2]*d.u[0],d.want.length);System.arraycopy(d.confidence,0,c,0,d.confidence.length);return true;}),"admitted route completes");
            if(resident){GpuNoise1960.Session session=(GpuNoise1960.Session)field(owner.getClass(),"session").get(owner);int[] pixels=session.readInts(24,d.u[0]*height);check(pixels!=null&&Arrays.equals(d.want,Arrays.copyOfRange(pixels,(d.u[6]+d.u[2])*d.u[0],(d.u[6]+d.u[3])*d.u[0])),"resident GPU output remains complete in native slot24");for(int value:out)check(value==0x13579bdf,"resident GPU never publishes stale Java output");}
            else assertPixels(d,out,cf);
            check(calls[0]==(mixed?1:0),"only matching ordinary route takes CPU when both GPU banks occupied");
            check(dispatches()-nativeBefore==(mixed?0:1),"actual native dispatch agrees with CPU/matching-worker/resident admission");
            check(ProcessingTiming1947.routeMode==(mixed?1:0),"mixed diagnostic describes actual admission only");
            check(ProcessingTiming1947.verification==0,"bank distribution never adds foreground proof");
        }finally{if(release!=null){release.join(2000);check(!release.isAlive(),"controlled occupied bank released");}synchronized(owner){banks[0]=banks[1]=false;owner.notifyAll();}GpuStrong1960.endStage(model);}
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"route releases all native leases");
    }
    static void cohortSafety(Data d,String mode)throws Exception{
        reset();final StrongNoise1958.Model model=d.model();final String exact=seed(d,7,0);final String route=GpuStrongRouting1978.key(d.u,7,0,4,GpuQualification1961.restore(exact));
        check(GpuNoise1960.supports(GpuStrong1960.program1973(7,0)),"candidate shader available for actual negative control");
        final AtomicBoolean stop=new AtomicBoolean();final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        int nativeBefore=dispatches(),cpuBefore=StrongNoise1958.oracleCalls.get();GpuQualification1961.busyBackground=true;
        try{
            if("cohort-memory".equals(mode)){
                WholeRoute1953.retained=Long.MAX_VALUE;
                GpuStrongRouting1978.prove(d.input[0],d.input[3],d.u,model,null,7,0,stop::get);
                check(GpuStrongRouting1978.compareCpu1978(d.input[0],d.input[3],d.u,model,null,7,0,4,stop::get)==null,"CPU fallback comparison also refuses insufficient memory");
                check(dispatches()==nativeBefore&&StrongNoise1958.oracleCalls.get()==cpuBefore,"cohort memory refusal precedes every CPU/GPU task");
            }else if("cohort-cancel".equals(mode)){
                delay(180);Thread proof=new Thread(()->{try{GpuStrongRouting1978.prove(d.input[0],d.input[3],d.u,model,null,7,0,stop::get);}catch(Throwable t){error.set(t);}},"fixture-idle-proof");proof.start();
                long until=System.nanoTime()+5000000000L;while(dispatches()==nativeBefore&&proof.isAlive()&&System.nanoTime()<until)Thread.sleep(2);
                check(dispatches()>nativeBefore,"cancel occurs during real native compute, after CPU reference");stop.set(true);GpuQualification1961.cancel=true;proof.interrupt();proof.join(5000);
                check(!proof.isAlive(),"cancelled actual worker cohort joins before source ownership release");check(error.get() instanceof RuntimeException,"cancel propagates from actual native/worker operation");
            }else{
                boolean confidence="cohort-confidence".equals(mode);if(confidence){model.frozenConfidence=d.confidence.clone();model.frozenConfidence[model.frozenConfidence.length-1]^=1;}else{model.frozenPixels=d.want.clone();model.frozenPixels[model.frozenPixels.length-1]^=1;}
                // Only the stable CPU reference is deliberately perturbed. The
                // original native shader must detect the last-output mismatch.
                GpuStrongRouting1978.prove(d.input[0],d.input[3],d.u,model,null,7,0,stop::get);
                check(GpuQualification1961.exactRejected(exact)&&GpuQualification1961.exactRejected(route),"last full-output mismatch rejects both child and route");
                check((confidence?"confidence_mismatch":"argb_mismatch").equals(GpuQualification1961.exactCauses.get(exact)),"native negative control classifies exact difference");
            }
        }finally{WholeRoute1953.retained=0;GpuQualification1961.busyBackground=GpuQualification1961.cancel=false;clearFault();}
        check(GpuQualification1961.restore(route)==null,"failed or cancelled cohort cannot publish routing proof");
        if("cohort-memory".equals(mode)||"cohort-cancel".equals(mode))check(GpuQualification1961.restore(exact)!=null&&!GpuQualification1961.exactRejected(exact),"memory/cancel does not fabricate an exact failure");
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0&&SpeedWorkers1935.cpuIdle1944(),"cohort releases native banks, memory and every production worker");
    }
    static void cohort(Data d,String mode)throws Exception{
        reset();StrongNoise1958.Model model=d.model();String exact=seed(d,7,0);GpuQualification1961.Record child=GpuQualification1961.restore(exact);String route=GpuStrongRouting1978.key(d.u,7,0,4,child);
        // The real idle tuner has already compiled and executed the selected
        // child twice before it reaches either cohort comparison.
        check(GpuNoise1960.supports(GpuStrong1960.program1973(7,0)),"selected candidate compiled before cohort, as in production tuner");
        boolean win=mode.endsWith("accept"),cpuBaseline=mode.startsWith("cpu-");
        if(win){StrongNoise1958.contendCpu=true;delay(cpuBaseline?5:35);}else delay(0);
        StrongNoise1958.cpuPeak.set(0);int cpuBefore=StrongNoise1958.oracleCalls.get();GpuQualification1961.busyBackground=true;long[] comparison=null;
        try{if(cpuBaseline)comparison=GpuStrongRouting1978.compareCpu1978(d.input[0],d.input[3],d.u,model,null,7,0,4,()->false);else GpuStrongRouting1978.prove(d.input[0],d.input[3],d.u,model,null,7,0,()->false);}finally{GpuQualification1961.busyBackground=false;StrongNoise1958.contendCpu=false;clearFault();}
        check(StrongNoise1958.oracleCalls.get()-cpuBefore==(cpuBaseline?8:12),"both measured cohorts execute every real CPU task");
        if(win)check(StrongNoise1958.cpuPeak.get()==4,"real production pool executes four CPU references concurrently");
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"cohort native banks and quotas released");
        if(cpuBaseline){check(comparison!=null,"actual parallel CPU baseline available without old GPU");check((comparison[1]<=comparison[0]-comparison[0]/20)==win,"measured CPU-only admission accepts/rejects controlled speed cpu="+comparison[0]+" gpu="+comparison[1]);return;}
        if("accept".equals(mode)){
            check(GpuStrongRouting1978.accepted(d.u,7,0,4),"both real mixed cohorts beat parallel CPU and queued GPU by5percent under controlled contention");
            check(!GpuStrongRouting1978.accepted(d.u,7,0,3),"worker-count mismatch cannot borrow proof");check(!GpuStrongRouting1978.accepted(d.u,7,1,4),"workgroup mismatch cannot borrow proof");
            GpuQualification1961.qualifiedStrongPreferred1970(exact,101,201,0);check(!GpuStrongRouting1978.accepted(d.u,7,0,4),"changed child timings retire stale cohort");
        }else{check(GpuQualification1961.restore(route)==null,"real fast parallel CPU prevents slower mixed promotion");check(!GpuQualification1961.exactRejected(exact),"speed rejection preserves pixel proof");}
    }
    public static void main(String[] args)throws Exception{
        check(GpuNoise1960.available()&&GpuNoise1960.warmEnvironment1973(),"actual JNI is available");Data d=data(args[0]);String mode=args[1];
        if(mode.startsWith("route-"))route(d,mode);else if(mode.startsWith("cohort-"))cohortSafety(d,mode);else if("accept".equals(mode)||"reject".equals(mode)||mode.startsWith("cpu-"))cohort(d,mode);else lifecycle(d,mode);
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_jni\":true,\"actual_bounded_workers\":true,\"controlled_contention\":true,\"physical_android_tested\":false}");
    }
}
