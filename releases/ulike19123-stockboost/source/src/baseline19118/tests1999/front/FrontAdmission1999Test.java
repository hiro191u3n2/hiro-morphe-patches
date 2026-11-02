package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/** Real admission owners and CPU pixels; the separately declared GPU transport
 * supplies already-known output so this verifies ownership, not device speed. */
public final class FrontAdmission1999Test {
    static int assertions,created,closed;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> cls,String name)throws Exception{Field f=cls.getDeclaredField(name);f.setAccessible(true);return f;}
    static final int W=33,H=97,B=7,E=80;
    static int[] input,expected;
    static SingleNoise1955.Model model;
    static void initialize()throws Exception {
        android.os.Build.FINGERPRINT="front-admission1999-host";
        GpuNoise1960.enabled=true;GpuQualification1961.initialize(new Context());
        field(SingleNoise1955.class,"nativeState").setInt(null,-1);
        input=new int[W*H];Random random=new Random(1999);
        for(int i=0;i<input.length;i++){int v=80+random.nextInt(65);input[i]=0xff000000|v<<16|v<<8|v;}
        model=SingleNoise1955.probe1978(new SingleNoise1955.Patches(){public void read(int[] out,int x,int y,int w,int h){
            for(int n=0;n<h;n++)System.arraycopy(input,(y+n)*W+x,out,n*w,w);
        }},W,H,false);
        expected=new int[input.length];Arrays.fill(expected,0x13579bdf);
        SingleNoise1955.processCpuRange(input,expected,W,H,B,E,0,H,0,4,true,model,null);
        GpuNoise1960.readback=Arrays.copyOfRange(expected,B*W,E*W);
        GpuStrongDefault1990.value=GpuFinishDefault1990.value=true;
    }
    static int[] run(SingleNoise1955.Workspace workspace){
        int[] actual=new int[input.length];Arrays.fill(actual,0x13579bdf);
        SingleNoise1955.processRange(input,actual,W,H,B,E,0,H,0,4,true,model,null,workspace);
        check(Arrays.equals(expected,actual),"complete CPU/GPU output remains exact including outside strip");
        return actual;
    }
    static void expiry(boolean residual)throws Exception {
        GpuNoise1960.single=!residual;
        SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
        run(workspace);GpuQualification1961.captureChanged();
        Map<?,?> gates=(Map<?,?>)field(residual?GpuResidual1961.class:GpuSingle1960.class,"GATES").get(null);
        check(gates.size()==1,"driver exact key observed");String key=(String)gates.keySet().iterator().next();
        String ref=SingleCpu1981.referenceKey(key,W,H,B,E,0,H,0,4,true,model,null);
        String speed=SingleCpu1981.speedKey(ref,false);
        GpuQualification1961.qualified(key,1000000000L,1,0);
        GpuQualification1961.qualified(speed,1000000000L,1,0);GpuNoise1960.fault=1;
        run(workspace);check(workspace.selectedBackend1982==1,"first valid GPU certificate used");
        GpuQualification1961.captureChanged();
        GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");
        check(GpuQualification1961.restore(key)==null,"actual original certificate revoked");
        int opens=GpuNoise1960.openAttempts;
        run(workspace);check(workspace.selectedBackend1982==0,"revoked certificate cannot use stale gate.admitted");
        check(workspace.selectedReason1989==PipelineDetail1988.PROOF_MISSING,"revocation classified as missing proof");
        check(GpuNoise1960.openAttempts==opens,"revocation does not attempt GPU execution");
        GpuQualification1961.captureChanged();
        GpuQualification1961.qualified(key,1000000000L,1,0);
        run(workspace);check(workspace.selectedBackend1982==1,"independent requalification reopens GPU admission");
        workspace.close();GpuQualification1961.captureChanged();
    }
    static void reasons(){
        SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
        GpuNoise1960.single=false;GpuNoise1960.residual=true;GpuNoise1960.knownFalse1999=false;
        run(workspace);check(workspace.primaryGpuReason1989==PipelineDetail1988.UNAVAILABLE,"unknown native answer stays unknown");
        GpuQualification1961.captureChanged();GpuNoise1960.knownFalse1999=true;
        run(workspace);check(workspace.primaryGpuReason1989==PipelineDetail1988.SINGLE_UNSUPPORTED1999,"cached native false identifies primary shader");
        GpuQualification1961.captureChanged();GpuNoise1960.residual=false;
        run(workspace);check(workspace.selectedReason1989==PipelineDetail1988.RESIDUAL_UNSUPPORTED1999,"residual unsupported is independent final CPU cause");
        GpuNoise1960.single=GpuNoise1960.residual=true;
        SingleNoise1955.processRange(input,new int[input.length],W,H,B,E,0,H,0,0,true,model,null,workspace);
        check(workspace.primaryGpuReason1989==0&&workspace.selectedReason1989==PipelineDetail1988.CPU_FIXED,"next fixed strip clears both reasons");
        workspace.close();GpuQualification1961.captureChanged();
    }
    static int reservations()throws Exception{return ((java.util.Collection<?>)field(GpuQualification1961.class,"RESERVED1984").get(null)).size();}
    static void proof(final String mode)throws Exception {
        final boolean cancel=mode.equals("proof-cancel"),qualified=mode.equals("proof-qualified"),failure=mode.equals("proof-exception");
        final String original="front1999-proof",reference="front1999-proof-reference";
        final long bytes=1048576L;
        SingleStage1981.Factory factory=new SingleStage1981.Factory(){public SingleStage1981.Operation create(){
            created++;
            try {check(reservations()==1,"queue capacity is held before first copied-input allocation");}
            catch(Exception failure){throw new AssertionError(failure);}
            if(cancel)GpuQualification1961.captureChanged();
            if(qualified)GpuQualification1961.qualified(SingleStage1981.key(reference),1000000000L,1L,1);
            if(failure)throw new IllegalStateException("snapshot creation failed");
            return new SingleStage1981.Operation(){
                public String referenceKey(){return reference;}
                public SingleStage1981.CpuSample cpu(int pass,GpuQualification1961.Cancellation c){throw new AssertionError("foreground proof executed");}
                public int[] run(SingleStage1981.Lease l,int pass){throw new AssertionError("foreground GPU executed");}
                public long preparationNanos(){return 0;}
                public void close(){closed++;}
            };
        }};
        boolean before=mode.equals("proof-interrupted")||mode.equals("proof-nullmodel")||mode.equals("proof-budget");
        long stripBefore=field(GpuSnapshotBudget1981.class,"stripBytes").getLong(null);
        if(mode.equals("proof-interrupted"))Thread.currentThread().interrupt();
        if(mode.equals("proof-budget"))GpuNoise1960.budget=false;
        try{SingleStage1981.offer(original,reference,mode.equals("proof-nullmodel")?null:model,SingleStage1981.SINGLE,bytes,bytes,factory);}
        finally{Thread.interrupted();GpuNoise1960.budget=true;}
        if(before){
            check(created==0&&reservations()==0&&GpuQualification1961.retainedBytes()==0,"precopy rejection retains no data or queue capacity");
            check(field(GpuSnapshotBudget1981.class,"stripBytes").getLong(null)==stripBefore,"precopy rejection does not consume copy budget");return;
        }
        check(created==1,"one bounded proof snapshot");check(reservations()==0,"reservation transfers or releases completely");
        if(cancel||qualified||failure){check(closed==(failure?0:1)&&GpuQualification1961.retainedBytes()==0,"cancel/reject/exception releases snapshot and capacity exactly once");}
        else {
            check(closed==0&&GpuQualification1961.retainedBytes()==bytes,"committed proof retains exact ownership");
            SingleStage1981.offer(original,reference,model,SingleStage1981.SINGLE,bytes,bytes,factory);
            check(created==1,"same queued proof denied before another snapshot");
            GpuQualification1961.captureChanged();check(closed==1&&GpuQualification1961.retainedBytes()==0,"queued proof later closes exactly once");
        }
    }
    public static void main(String[] args)throws Exception {
        initialize();
        if(args[0].equals("single"))expiry(false);
        else if(args[0].equals("residual")){System.load(args[1]);expiry(true);}
        else if(args[0].equals("reasons"))reasons();
        else proof(args[0]);
        check(GpuNoise1960.active==0,"all transport sessions closed");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"mode\":\""+args[0]+"\"}");
    }
}
