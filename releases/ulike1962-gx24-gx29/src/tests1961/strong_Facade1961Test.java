package com.hiro.ulike;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Host-only certified-branch coverage. Admission metadata is controlled by
 * reflection in this test, never by a production switch. Every pixel comes
 * from real production GL commands and is checked against the separately
 * compiled frozen .60 Java oracle. This is not a physical-device speed proof. */
final class StrongFacade1961Test {
    private static long assertions,pixels,confidenceValues,dispatches;
    private static int fullCases,inactiveCases,bankCases,compareCases;
    private static final class Record {
        String name;int[] u,expected,confidence;int[][] b;
    }
    private static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    private static int[] ints(DataInputStream in)throws Exception{int n=in.readInt();int[] a=new int[n];for(int i=0;i<n;i++)a[i]=in.readInt();return a;}
    private static Object field(Object o,String name)throws Exception{
        Class<?> type=o instanceof Class?(Class<?>)o:o.getClass();Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o instanceof Class?null:o);
    }
    private static void set(Object o,String name,Object v)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,v);}
    private static void same(int[] expected,int[] actual,String why,boolean confidence){
        check(actual!=null&&actual.length>=expected.length,why+" length");
        for(int i=0;i<expected.length;i++){assertions++;if(confidence)confidenceValues++;else pixels++;
            if(expected[i]!=actual[i])throw new AssertionError(why+" at "+i+" expected "+Integer.toHexString(expected[i])+" actual "+Integer.toHexString(actual[i]));}
    }
    private static void released(String why){
        check(!GpuNoise1960.sessionBusy(),why+" releases session token");
        check(GpuNoise1960.retainedBytes()<=128L*1024*1024,why+" bounded known-complete workspace pool");
        GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()==0,why+" idle trim frees native workspace");
    }
    @SuppressWarnings("unchecked") private static void certificate(int[] u,int variant)throws Exception{
        Method key=GpuStrong1960.class.getDeclaredMethod("key",int[].class);key.setAccessible(true);
        String name=(String)key.invoke(null,(Object)u);
        Class<?> type=Class.forName("com.hiro.ulike.GpuStrong1960$Gate");Constructor<?> constructor=type.getDeclaredConstructor();constructor.setAccessible(true);Object gate=constructor.newInstance();
        set(gate,"accepted",true);set(gate,"consecutive",2);set(gate,"cpuBaseline",Long.MAX_VALUE/1024);set(gate,"variant",variant);
        Map<String,Object> gates=(Map<String,Object>)field(GpuStrong1960.class,"GATES");synchronized(gates){gates.put(name,gate);}
    }
    private static StrongNoise1958.Model model(final Record r)throws Exception{
        final int w=r.u[0],h=r.u[1];final int[] source=r.b[0];
        StrongNoise1958.Model result=StrongNoise1958.prepareJava(new StrongNoise1958.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(source,(y+row)*w+x,a,row*width,width);}},w,h,r.u[8],r.u[9]!=0);
        int[][] maps=StrongNoise1958.gpuMaps1960(result);for(int i=0;i<3;i++)same(r.b[4+i],maps[i],"frozen model map "+r.name,false);
        float[] ev=StrongNoise1958.gpuEvidence1960(result);int[] bits=new int[ev.length];for(int i=0;i<bits.length;i++)bits[i]=Float.floatToRawIntBits(ev[i]);same(r.b[2],bits,"frozen model evidence "+r.name,false);return result;
    }
    private static boolean plain(Record r){for(int i=0;i<r.b[3].length;i+=2)if(r.b[3][i]!=256||r.b[3][i+1]!=0)return false;return true;}
    private static StrongNoise1958.Protection protection(final Record r){
        if(plain(r))return new GpuPolicy1960.Protection(null,true);
        final int w=r.u[0];return new StrongNoise1958.Protection(){public int budgetQ8(int x,int y){return r.b[3][(y*w+x)*2];}public int detailQ8(int x,int y){return r.b[3][(y*w+x)*2+1];}};
    }
    private static void full(Record r,StrongNoise1958.Model model,int variant)throws Exception{
        int[] source=r.b[0],before=source.clone(),actual=source.clone();StrongNoise1958.Workspace workspace=new StrongNoise1958.Workspace(true);
        certificate(r.u,variant);long start=Native1960Test.faultFacts()[3];GpuStrong1960.beginStage(model);
        try{StrongNoise1958.processRange(source,actual,r.u[0],r.u[1],0,r.u[1],0,r.u[1],0,r.u[8],r.u[9]!=0,model,protection(r),workspace);
            same(r.expected,actual,"certified facade "+r.name+" layout"+variant,false);same(r.confidence,workspace.confidence(),"certified confidence "+r.name,true);
        }finally{GpuStrong1960.endStage(model);workspace.close();}
        long commands=Native1960Test.faultFacts()[3]-start;check(commands>0,"certified facade issued actual GL dispatch "+r.name);dispatches+=commands;
        if(plain(r)){check(commands>=3,"policy generation + compare + Strong dispatch");compareCases++;}
        same(before,source,"immutable foreground source",false);released("certified stage");fullCases++;
    }
    private static void banks(final Record r,final StrongNoise1958.Model model,int variant)throws Exception{
        final int w=r.u[0],h=r.u[1],split=64;final int[] actual=r.b[0].clone(),before=r.b[0].clone();
        final StrongNoise1958.Workspace[] workspaces={new StrongNoise1958.Workspace(true),new StrongNoise1958.Workspace(true)};
        final CountDownLatch start=new CountDownLatch(1);final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] workers=new Thread[2];
        for(int bank=0;bank<2;bank++){
            int[] u=r.u.clone();u[2]=bank==0?0:split;u[3]=bank==0?split:h;certificate(u,variant);
            final int index=bank,begin=u[2],end=u[3];workers[bank]=new Thread(new Runnable(){public void run(){
                try{start.await();StrongNoise1958.processRange(r.b[0],actual,w,h,begin,end,0,h,0,r.u[8],r.u[9]!=0,model,protection(r),workspaces[index]);}
                catch(Throwable problem){failure.compareAndSet(null,problem);}
            }},"strong-proof-bank"+bank);
        }
        long beforeDispatch=Native1960Test.faultFacts()[3];boolean both=false;GpuStrong1960.beginStage(model);
        try{
            // The fault wrapper delays only fence readiness. Real GPU commands
            // execute unchanged; release the wrapper before actual readback.
            Native1960Test.setFault(1);for(Thread worker:workers)worker.start();start.countDown();
            long deadline=System.nanoTime()+2000000000L;
            while(System.nanoTime()<deadline&&failure.get()==null){Object stage=field(GpuStrong1960.class,"active");
                if(stage!=null)synchronized(stage){boolean[] busy=(boolean[])field(stage,"banks");both=busy[0]&&busy[1];}
                if(both)break;Thread.sleep(1);
            }
            Native1960Test.setFault(0);for(Thread worker:workers)worker.join(6000);
            for(Thread worker:workers)check(!worker.isAlive(),"both bank workers completed");
            if(failure.get()!=null)throw new AssertionError("bank worker failed",failure.get());
            check(both,"two production banks leased concurrently");same(r.expected,actual,"two-bank frozen output",false);
            int cw=(w+3)/4,offset=0;for(int bank=0;bank<2;bank++){int rows=bank==0?split:h-split,count=cw*((rows+3)/4);int[] expected=Arrays.copyOfRange(r.confidence,offset,offset+count);same(expected,workspaces[bank].confidence(),"two-bank confidence "+bank,true);offset+=count;}
        }finally{
            Native1960Test.setFault(0);for(Thread worker:workers)if(worker.isAlive()){worker.interrupt();worker.join(6000);}
            GpuStrong1960.endStage(model);for(StrongNoise1958.Workspace workspace:workspaces)workspace.close();
        }
        long commands=Native1960Test.faultFacts()[3]-beforeDispatch;check(commands>=(plain(r)?6:2),"both bank submissions used real GPU commands");dispatches+=commands;
        released("two-bank stage");same(before,r.b[0],"two-bank immutable source",false);bankCases++;
    }
    private static void rejectMismatchedPolicy(Record r,StrongNoise1958.Model model)throws Exception{
        GpuPolicy1960.PolicyData descriptor=GpuPolicy1960.sourcePlan(null,r.u[0],r.u[1],0,true);
        descriptor.u[7]=256;for(int at=0;at<descriptor.masks.length;at+=2)descriptor.masks[at]=256;
        Method method=GpuStrong1960.class.getDeclaredMethod("ephemeral",int[].class,float[].class,int[][].class,int[].class,int[].class,GpuPolicy1960.PolicyData.class,int.class);method.setAccessible(true);
        long start=Native1960Test.faultFacts()[3];Object result=method.invoke(null,r.b[0],StrongNoise1958.gpuEvidence1960(model),StrongNoise1958.gpuMaps1960(model),r.b[3],r.u,descriptor,0);
        check(result==null,"GPU integer mismatch flag rejects complete candidate");long commands=Native1960Test.faultFacts()[3]-start;check(commands>=3,"mismatch used actual generation + GPU compare + Strong");dispatches+=commands;
        released("mismatch candidate");compareCases++;
    }
    public static void main(String[] args)throws Exception{
        check(GpuNoise1960.available(),"real GLES JNI available");for(int variant=0;variant<3;variant++)check(GpuNoise1960.supports(GpuNoise1960.variant(GpuNoise1960.STRONG,variant)),"real supported layout "+variant);
        ArrayList<Record> records=new ArrayList<Record>();
        try(DataInputStream in=new DataInputStream(new FileInputStream(args[0]))){check(in.readInt()==1960001,"independent frozen .60 corpus");
            for(;;){String name=in.readUTF();if(name.isEmpty())break;int shader=in.readInt();int[] u=ints(in);in.readInt();int nb=in.readInt();int[][] b=new int[nb][];for(int i=0;i<nb;i++)b[i]=ints(in);int[] expected=ints(in),confidence=ints(in);
                if(shader==0&&u[10]==3){Record r=new Record();r.name=name;r.u=u;r.b=b;r.expected=expected;r.confidence=confidence;records.add(r);}
            }
        }
        boolean bankDone=false,compareDone=false;
        for(Record r:records){StrongNoise1958.Model model=model(r);if(!((Boolean)field(model,"measuredActive"))){inactiveCases++;continue;}
            for(int variant=0;variant<3;variant++)full(r,model,variant);
            if(!compareDone&&plain(r)){rejectMismatchedPolicy(r,model);compareDone=true;}
            if(!bankDone&&r.u[1]==131&&plain(r)){for(int variant=0;variant<3;variant++)banks(r,model,variant);bankDone=true;}
        }
        check(fullCases>0&&bankCases==3&&compareDone,"all certified production branches covered");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"pixels\":"+pixels+",\"confidenceValues\":"+confidenceValues+",\"certifiedFullCases\":"+fullCases+",\"inactiveCpuCases\":"+inactiveCases+",\"twoBankCases\":"+bankCases+",\"gpuPolicyCompareCases\":"+compareCases+",\"actualGpuDispatches\":"+dispatches+",\"controlledAdmissionMetadata\":true,\"actualProductionGpuCommands\":true,\"physicalSpeedClaim\":false,\"physicalAndroidTested\":false}");
    }
}
