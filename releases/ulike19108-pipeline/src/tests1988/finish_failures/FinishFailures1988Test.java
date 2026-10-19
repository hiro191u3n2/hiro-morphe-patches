package com.hiro.ulike;

import android.graphics.Bitmap;
import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real Finish admission and real bounded queue/certificate/history state.
 * Four renderer entry bodies and their clock are explicit test inputs. */
public final class FinishFailures1988Test {
    static int assertions;static boolean baseline;
    static final int W=37,H=23;
    static final Map<String,Integer> cases=new LinkedHashMap<String,Integer>();
    interface Task {void run(GpuQualification1961.Cancellation c)throws Exception;}
    static final class Probe implements GpuQualification1961.Probe {
        final Task task;final AtomicInteger closes=new AtomicInteger();final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Probe(Task task){this.task=task;}
        public void run(GpuQualification1961.Cancellation c){try{task.run(c);}catch(CancellationException cancelled){throw cancelled;}
            catch(Throwable fail){error.set(fail);throw new AssertionError(fail);}}
        public void close(){closes.incrementAndGet();}
    }
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(String name)throws Exception{return QualificationProgress1984Test.field(name);}
    static Field field(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
    static int integer(Object value,String name)throws Exception{return field(value,name).getInt(value);}
    static Object attempt()throws Exception{Object[] values=(Object[])field("ATTEMPTS1984").get(null);return values[(field("attemptCursor1984").getInt(null)+values.length-1)%values.length];}
    static Object failure(String key)throws Exception{Method env=GpuQualification1961.class.getDeclaredMethod("environment");env.setAccessible(true);
        Method name=GpuQualification1961.class.getDeclaredMethod("recordKey",String.class,String.class);name.setAccessible(true);
        synchronized(field("LOCK").get(null)){return ((Map<?,?>)field("FAILURES").get(null)).get(name.invoke(null,key,env.invoke(null)));}}
    static void reset()throws Exception{QualificationProgress1984Test.reset();FinishControl1986.reset();Bitmap.ALL.clear();}
    static void quiet()throws Exception{QualificationProgress1984Test.quiet();}
    static Probe start(String key,Task task)throws Exception{Probe p=new Probe(task);check(GpuQualification1961.schedule(key,1,p),"real queue accepts proof owner");quiet();return p;}
    static void end(Probe p)throws Exception{QualificationProgress1984Test.await(()->p.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"proof releases owner");
        check(p.error.get()==null,"no unexpected worker failure: "+p.error.get());check(p.closes.get()==1,"probe closed exactly once");}
    static QualityPixels1932.Plan plan(){return QualityPixels1932.plan(null,800,16666666L,0,0,4,4,true,true,1.2f).withHaloSuppression(true);}
    static String key(Bitmap b,QualityPixels1932.Plan p)throws Exception{Method m=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);m.setAccessible(true);return (String)m.invoke(null,b,0,W,H,p,true);}
    static String legacy(String key){return key.replace("|finish-chain1976:v2|","|finish-chain1962:v1|");}
    static String execute()throws Exception{
        Bitmap input=FinishControl1986.image(29,19,false);int[] pixels=input.snapshot();QualityPixels1932.Plan p=plan();String key=key(input,p);
        Probe probe=start(key,c->GpuChain1961.qualifyFinish1978(input,0,W,H,p,true,()->c.cancelled()||FinishControl1986.cancelled()));end(probe);
        check(!input.isRecycled()&&Arrays.equals(pixels,input.snapshot()),"qualification keeps caller-owned source and pixels unchanged");
        synchronized(Bitmap.ALL){for(Bitmap b:Bitmap.ALL)if(b!=input)check(b.isRecycled(),"every CPU and GPU temporary is recycled");}
        input.recycle();Bitmap.ALL.clear();return key;
    }
    static void allCompare(int fault){for(int i=0;i<4;i++)FinishControl1986.compareFault[i][0]=fault;}
    static int total(int[] values){int n=0;for(int v:values)n+=v;return n;}
    static int reason(int index)throws Exception{return(integer(attempt(),"finishReasons1986")>>>(index*4))&15;}
    static int trials(String field,int index)throws Exception{return(integer(attempt(),field)>>>(index*2))&3;}
    static void counts(int outcome,int speed,int nonSpeed)throws Exception{Object a=attempt();check(integer(a,"outcome")==outcome,"parent outcome "+outcome+" actual "+integer(a,"outcome"));
        check(integer(a,"speedFailures")==speed,"only measured speed failure counter changes");
        if(!baseline)check(integer(a,"nonSpeedFailures1988")==nonSpeed,"separate bounded non-speed failure counter");}
    static void section(String label,int start){cases.put(label,assertions-start);}
    static void unavailableNegative()throws Exception{
        int n=assertions;reset();allCompare(1);String key=execute();
        check(FinishControl1986.cpuCalls==2&&total(FinishControl1986.comparisons)==4&&total(FinishControl1986.outputs)==0,"same published failure has four incomplete comparisons and zero GPU outputs");
        for(int i=0;i<4;i++){check(reason(i)==3,"each unavailable candidate remains explicit");check(trials("finishExact1986",i)==0&&trials("finishSpeed1986",i)==0,"unmeasured candidates retain zero exact and timing trials");}
        counts(baseline?11:8,baseline?1:0,1);
        check(GpuQualification1961.restore(key)==null&&!GpuQualification1961.exactRejected(key),"unavailable execution creates neither certificate nor pixel rejection");
        String text=GpuQualification1961.attemptSummary1986();check(text.contains("候補GPU 未取得"),"unmeasured GPU time stays unavailable");
        if(!baseline)check(!text.contains("速度条件を満たさず")&&text.contains("GPU実行・取得を完了できず"),"unmeasured terminal no longer claims speed failure");
        section(baseline?"published87_zero_output_misclassified_speed":"current88_zero_output_non_speed",n);
    }
    static void failureMatrix()throws Exception{
        int n=assertions;
        for(int mode=0;mode<12;mode++){
            reset();allCompare(1);int expected=8,candidateReason=3;
            if(mode==0){allCompare(5);expected=6;candidateReason=4;}
            if(mode==1)allCompare(3);
            if(mode==2)allCompare(4);
            if(mode==3){FinishControl1986.compareFault[2][0]=5;expected=14;}
            if(mode==4){for(int i=1;i<4;i++)FinishControl1986.compareFault[i][0]=2;expected=12;candidateReason=2;}
            if(mode==5){allCompare(2);expected=12;candidateReason=2;}
            if(mode==6){allCompare(0);FinishControl1986.unstable=true;expected=9;candidateReason=10;}
            if(mode==7){FinishControl1986.cpuTimes[0]=0;expected=9;candidateReason=10;}
            if(mode==8){FinishControl1986.copyFailure=1;expected=9;candidateReason=10;}
            if(mode==9){FinishControl1986.copyFailure=2;expected=6;candidateReason=4;}
            if(mode==10){FinishControl1986.workspace=false;expected=6;candidateReason=4;}
            if(mode==11){allCompare(0);for(int i=0;i<4;i++)FinishControl1986.outputFault[i][0]=1;}
            String key=execute();counts(expected,0,1);check(GpuQualification1961.restore(key)==null,"failed inputs cannot certify mode "+mode);
            if(mode!=3)check(reason(1)==candidateReason,"candidate keeps actual failure reason mode "+mode);
            if(mode==5){check(GpuQualification1961.exactRejected(legacy(key)),"actual old-GPU mismatch keeps permanent exact rejection");check(integer(attempt(),"exactFailures")==1,"old-GPU actual mismatch remains one exact fact");}
            if(mode==4||mode==5)check(!GpuQualification1961.exactRejected(key),"failed family selection cannot invent whole-key permanent rejection");
            if(mode==11)check(trials("finishExact1986",1)==1&&trials("finishSpeed1986",1)==0,"failed output cannot manufacture measured timing");
            if(mode==0||mode==1||mode==2){int expectedFault=mode==0?3:mode==1?1:2;
                for(int i=0;i<4;i++)check(((integer(attempt(),"finishFaults1988")>>>(i*3))&7)==expectedFault,"typed exception scalar covers each independent candidate");}
            if(mode==6)check(FinishControl1986.comparisons[1]==1,"unstable second CPU reference blocks second pixel proof");
        }
        section("memory_reference_mismatch_runtime_linkage_and_missing_outputs",n);
    }
    static void measuredAndMixed()throws Exception{
        int n=assertions;
        for(int mode=0;mode<8;mode++){
            reset();FinishControl1986.compareFault[0][0]=1;
            for(int i=1;i<4;i++)FinishControl1986.gpuTimes[i]=new long[]{960,960};
            if(mode==1)FinishControl1986.compareFault[1][0]=1;
            if(mode==2)FinishControl1986.compareFault[1][0]=2;
            if(mode==3){for(int i=1;i<4;i++)FinishControl1986.outputFault[i][1]=1;}
            if(mode==4){FinishControl1986.gpuTimes[2]=new long[]{950,950};}
            if(mode==5){FinishControl1986.compareFault[0][0]=0;FinishControl1986.gpuTimes[0]=new long[]{1000,1000};}
            if(mode==6){FinishControl1986.cpuTimes=new long[]{1000,900};FinishControl1986.gpuTimes[2]=new long[]{800,860};}
            if(mode==7){FinishControl1986.gpuTimes[2]=new long[]{951,951};}
            String key=execute();
            if(mode==4){counts(1,0,0);GpuQualification1961.Record proof=GpuQualification1961.restore(key);
                check(proof!=null&&proof.cpuNanos==1000&&proof.gpuNanos==950&&proof.variant==1,"exact inclusive five-percent boundary still certifies selected output");
                check(FinishControl1986.comparedPixels[2]==2L*W*H&&FinishControl1986.outputs[2]==2,"certificate requires two full pixel comparisons and real destination timings");}
            else {int outcome=mode==1||mode==2?14:mode==3?8:11;int speed=mode==1||mode==2||mode==3?0:mode==5?2:1;
                counts(outcome,speed,speed==0?1:0);check(GpuQualification1961.restore(key)==null,"insufficient measured evidence cannot certify mode "+mode);}
            if(mode==1||mode==2)check(reason(1)==(mode==1?3:2)&&reason(2)==5,"mixed failure keeps independent measured-slow and failed candidate facts");
            if(mode==3)check(trials("finishExact1986",1)==2&&trials("finishSpeed1986",1)==1,"two exact comparisons with only one output timing are never speed failure");
            if(mode==6){check(reason(2)==5,"fastest CPU versus worst GPU gate retained");Object a=attempt();check(field(a,"finishCpu21986").getLong(a)==900&&field(a,"finishGpu21986").getLong(a)==860,"actual min CPU and max candidate times are preserved");}
        }
        section("only_complete_slow_family_is_speed_mixed_and_second_output_failure",n);
    }
    static void legacyAndProofs()throws Exception{
        int n=assertions;
        for(int mode=0;mode<4;mode++){
            reset();if(mode==1){for(int i=1;i<4;i++)FinishControl1986.compareFault[i][0]=1;}
            if(mode==2)FinishControl1986.gpuTimes=new long[][]{{800,800},{850,850},{900,900},{950,950}};
            if(mode==3){FinishControl1986.compareFault[0][0]=2;}
            String key=execute();GpuQualification1961.Record proof=GpuQualification1961.restore(mode==1||mode==2?legacy(key):key);
            check(proof!=null&&integer(attempt(),"outcome")==1,"actual valid new or legacy proof keeps parent certificate priority");
            check(integer(attempt(),"certificates")==1,"exactly one actual certificate is written");
            if(mode==0)check(proof.variant==1&&proof.cpuNanos==800&&proof.gpuNanos==700,"valid old GPU still supplies dual-baseline reference");
            if(mode==1)counts(1,0,1);
            if(mode==2)counts(1,1,0);
            if(mode==3)check(proof.cpuNanos==1000&&GpuQualification1961.exactRejected(legacy(key)),"old mismatch leaves exact CPU-faster candidate and history intact");
            check(FinishControl1986.comparedPixels[mode==1||mode==2?0:2]==2L*W*H,"all successful proof rows visited twice");
        }
        section("qualified_new_or_legacy_priority_and_exact_history",n);
    }
    static void cancellation()throws Exception{
        int n=assertions;
        for(int mode=1;mode<=7;mode++){
            reset();FinishControl1986.cancelMode=mode;String key=execute();counts(4,0,0);
            check(failure(key)==null&&GpuQualification1961.maySchedule(key),"cancellation does not spend retry or cooldown at boundary "+mode);
            check(GpuQualification1961.restore(key)==null&&!GpuQualification1961.exactRejected(key),"cancellation cannot create certificate or mismatch");
        }
        section("cancelled_cpu_compare_output_and_terminal_boundaries",n);
    }
    static void retryAndPersistence()throws Exception{
        int n=assertions;reset();String key=null;
        for(int attempt=0;attempt<4;attempt++){
            FinishControl1986.reset();allCompare(1);key=execute();Object failure=failure(key);
            check(failure!=null&&!field(failure,"exact").getBoolean(failure)&&integer(failure,"retries")==attempt,"non-speed retry count matches existing bounded policy "+attempt);
            long remaining=field(failure,"retryAfter").getLong(failure)-System.nanoTime();
            check(remaining>28000000000L&&remaining<=30000000000L,"non-speed failure retains actual thirty-second cooldown");
            check(GpuQualification1961.canQueue1983(key,1).reason==(attempt==3?10:9),"existing cooldown/limit decision is authoritative");
            field(failure,"retryAfter").setLong(failure,System.nanoTime()-1);
            check(GpuQualification1961.maySchedule(key)==(attempt<3),"only three retry attempts are permitted");
        }
        check(!GpuQualification1961.schedule(key,1,new Probe(c->{})),"fourth retry rejected even when time has elapsed");
        SharedPreferences prefs=(SharedPreferences)field("preferences").get(null);
        check(prefs.getAll().isEmpty(),"non-speed failures do not persist permanent pixel facts");
        reset();final String exact="finish-private-exact";GpuQualification1961.rejectExact(exact);
        GpuQualification1961.rejectNonSpeed1988(exact,"execution_unavailable");GpuQualification1961.qualified(exact,100,80,0);
        check(GpuQualification1961.exactRejected(exact)&&GpuQualification1961.restore(exact)==null,"non-speed retry never clears exact certificate veto");
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("FAILURES").get(null)).clear();}
        check(GpuQualification1961.exactRejected(exact),"signed exact journal still restores after cache clear");
        String positive="independent-positive";GpuQualification1961.qualified(positive,100,80,0);check(GpuQualification1961.restore(positive)!=null,"unrelated valid certificate exists");
        GpuQualification1961.rejectNonSpeed1988(positive,"memory_budget");check(GpuQualification1961.restore(positive)==null&&!GpuQualification1961.exactRejected(positive),"non-speed invalidation removes only target certificate");
        for(int i=0;i<100;i++)GpuQualification1961.rejectNonSpeed1988("bounded-failure-"+i,"execution_unavailable");
        check(((Map<?,?>)field("FAILURES").get(null)).size()<=64,"failure state remains bounded to original LRU capacity");
        check(GpuQualification1961.exactRejected(exact),"failure LRU eviction cannot lose signed exact history");
        section("thirty_second_three_retry_limit_exact_persistence_and_bounded_state",n);
    }
    static void telemetryAndSnapshot()throws Exception{
        int n=assertions;reset();final String key="fixture|finish-chain1976:v2|private-condition";
        Probe probe=start(key,c->{
            GpuQualification1961.finishCpu1988(1,15);GpuQualification1961.finishCpu1988(5,15);
            for(int slot=0;slot<4;slot++){
                GpuQualification1961.finishBegin1988(slot-1,2);GpuQualification1961.finishStage1988(10+slot);
                GpuQualification1961.finishFault1988(slot,0);GpuQualification1961.finishEnd1988();
                GpuQualification1961.finishCandidate1986(slot-1,3,0,0,1,1000,0,0);
            }
            String stable=GpuQualification1961.attemptSummary1986();int events=CameraTrace1965.events.size();
            GpuQualification1961.finishBegin1988(-2,2);GpuQualification1961.finishBegin1988(3,2);GpuQualification1961.finishBegin1988(0,0);
            GpuQualification1961.finishCpu1988(6,15);GpuQualification1961.finishCpu1988(2,16);GpuQualification1961.finishStage1988(32);GpuQualification1961.finishFault1988(5,15);
            check(stable.equals(GpuQualification1961.attemptSummary1986())&&events==CameraTrace1965.events.size(),"malformed/no-active diagnostics cannot mutate candidate snapshots or emit records");
            CameraTrace1965.fail=true;GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");
        });end(probe);counts(8,0,1);
        String ended=GpuQualification1961.attemptSummary1986();Object value=attempt();Map<String,Object> scalars=new TreeMap<String,Object>();
        for(Field f:value.getClass().getDeclaredFields()){f.setAccessible(true);check(f.getType().isPrimitive(),"retained history contains primitive fields only");scalars.put(f.getName(),f.get(value));}
        GpuQualification1961.finishCpu1988(2,15);GpuQualification1961.finishBegin1988(0,3);GpuQualification1961.finishStage1988(24);GpuQualification1961.finishFault1988(4,15);GpuQualification1961.finishEnd1988();
        check(ended.equals(GpuQualification1961.attemptSummary1986()),"late callbacks cannot rewrite terminal summary");
        for(Field f:value.getClass().getDeclaredFields()){f.setAccessible(true);check(Objects.equals(scalars.get(f.getName()),f.get(value)),"late callback preserves frozen scalar "+f.getName());}
        check(ended.contains("GPUセッション開始")&&ended.contains("入力転送・初期GPU処理"),"per-candidate final engineering stages are rendered through existing summary");
        check(!ended.contains("private-condition")&&!CameraTrace1965.events.toString().contains("private-condition"),"no key/pixel/exception detail enters stage output");
        boolean stage=false;for(String event:CameraTrace1965.events)if(event.startsWith("gpu_finish88_stage ")){stage=true;int split=event.indexOf(' ',event.indexOf(' ')+1);check(event.length()-split-1<=160,"scalar stage payload fits existing event cap");}
        check(stage,"new finite stage record reaches existing event sink");CameraTrace1965.fail=false;
        reset();CameraTrace1965.fail=true;String admitted=execute();check(GpuQualification1961.restore(admitted)!=null,"throwing optional event sink cannot alter valid exact proof or output timing");CameraTrace1965.fail=false;
        section("optional_telemetry_scalar_privacy_and_immutable_terminal",n);
    }
    static void faultApi()throws Exception{
        int n=assertions;reset();String key=execute();GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        check(proof!=null&&proof.cpuNanos==800&&proof.gpuNanos==700&&proof.variant==1,"throwing stage API cannot affect selected pixels or baseline timing");
        reset();allCompare(1);key=execute();counts(8,0,1);check(GpuQualification1961.restore(key)==null,"throwing stage API cannot turn unavailable outputs into certificates");
        section("throwing_optional_api_does_not_change_operational_control",n);
    }
    public static void main(String[] args)throws Exception{
        baseline=args.length>0&&args[0].equals("published87");boolean fault=args.length>0&&args[0].startsWith("fault-");
        if(fault)faultApi();else{unavailableNegative();if(!baseline){failureMatrix();measuredAndMixed();legacyAndProofs();cancellation();retryAndPersistence();telemetryAndSnapshot();}}
        reset();StringBuilder text=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"cases\":{");boolean comma=false;
        for(Map.Entry<String,Integer> e:cases.entrySet()){if(comma)text.append(',');comma=true;text.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println(text.append("}}").toString());
    }
}
