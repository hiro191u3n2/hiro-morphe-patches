package com.hiro.ulike;

import android.content.Context;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** The same scenarios run against pinned .81 and current production sources.
 * Certificates and transport are controlled inputs, never real shader proof. */
public final class StrongDiagnostics1982Test {
    static int assertions,differentialCases;static boolean legacy;
    static final ByteArrayOutputStream decisions=new ByteArrayOutputStream();
    static final DataOutputStream bytes=new DataOutputStream(decisions);
    static final Map<String,Integer> tests=new LinkedHashMap<String,Integer>();
    static final String[] ROUTE={"route_unavailable1982","route_unqualified1982","route_exact_rejected1982",
        "route_unqualified_exact1982","route_speed_condition1982","route_unqualified_speed1982",
        "route_exact_speed1982","route_unqualified_exact_speed1982"};
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object value(Class<?> type,Object owner,String name)throws Exception{return field(type,name).get(owner);}
    static Object invoke(Class<?> type,Object owner,String name,Class<?>[] signature,Object...args)throws Exception{
        Method m=type.getDeclaredMethod(name,signature);m.setAccessible(true);
        try{return m.invoke(owner,args);}catch(InvocationTargetException failed){Throwable cause=failed.getCause();if(cause instanceof Exception)throw (Exception)cause;throw (Error)cause;}
    }
    static Object lock()throws Exception{return value(GpuQualification1961.class,null,"LOCK");}
    static Map<?,?> cache(String name)throws Exception{return (Map<?,?>)value(GpuQualification1961.class,null,name);}
    static void reset()throws Exception{
        check(value(GpuStrong1960.class,null,"active")==null,"previous Strong stage drained");
        check(GpuQualification1961.retainedBytes()==0,"previous detached ownership drained");
        SaveQueue1935.idle=false;WholeRoute1953.retained=0;GpuQualification1961.captureChanged();
        synchronized(lock()){cache("RECORDS").clear();cache("FAILURES").clear();}
        ((Map<?,?>)value(GpuStrong1960.class,null,"GATES")).clear();
        ProcessingTiming1947.epoch++;ProcessingTiming1947.reset();CameraTrace1965.events.clear();
        GpuNoise1960.reset();GpuStrongTuning1975.reset();GpuStrongRouting1978.mixed=false;
        GpuQualification1961.initialize(new Context());
    }
    static final class Data implements GpuStrong1960.Oracle {
        final StrongNoise1958.Model model=new StrongNoise1958.Model();
        final int width=8,rows=12,begin=2,end=10,origin=24;
        final int[] source=new int[width*rows],out=new int[width*rows+3],confidence=new int[9],policy=new int[width*(end-begin)*2];
        int calls;
        Data(){for(int i=0;i<source.length;i++)source[i]=0x80000000+i*0x01023457;Arrays.fill(out,0x13579bdf);Arrays.fill(confidence,0x2468ace0);}
        GpuStrong1960.Route1981 route(){return GpuStrong1960.route1981(width,rows,begin,end,0,rows,origin,4,true,model,true,true,null);}
        int[] uniforms(){int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=end;u[5]=rows;u[6]=origin;u[7]=model.height;u[8]=4;u[9]=1;u[10]=3;u[11]=1;u[12]=1;return u;}
        String key(int profile){return GpuStrong1960.profileKey1973(uniforms(),profile);}
        public boolean run(int[] destination,int[] cf){calls++;for(int i=begin*width;i<end*width;i++)destination[i]=StrongNoise1958.pixel(source[i]);if(cf!=null)Arrays.fill(cf,0,4,37);return true;}
        boolean process(GpuStrong1960.Route1981 route){return GpuStrong1960.process1981(source,out,width,rows,begin,end,0,rows,origin,4,true,model,policy,confidence,null,route,this);}
        void exact(){
            int[] expected=new int[out.length],cf=new int[confidence.length];Arrays.fill(expected,0x13579bdf);Arrays.fill(cf,0x2468ace0);
            for(int i=begin*width;i<end*width;i++)expected[i]=StrongNoise1958.pixel(source[i]);Arrays.fill(cf,0,4,37);
            check(Arrays.equals(out,expected),"all chosen ARGB and source padding unchanged");
            check(Arrays.equals(confidence,cf),"all NR13 confidence and pooled tail unchanged");
        }
    }
    static void seed(Data d,int profile,int kind){
        String key=d.key(profile);
        if(kind==1)GpuQualification1961.rejectExact1971(key,"argb_mismatch");
        else if(kind>=2)GpuQualification1961.qualifiedStrongPreferred1970(key,2000000000L,kind==2?2000000000L:(profile+1)*1000000L,profile%3);
    }
    static void record(String id,Data d,GpuStrong1960.Route1981 route)throws Exception{
        bytes.writeUTF(id);bytes.writeInt(route==null?-9:route.profile);bytes.writeInt(route==null?-9:route.variant);
        bytes.writeUTF(route==null||route.key==null?"":route.key);bytes.writeBoolean(route!=null&&route.rawPolicy());
        bytes.writeInt(d.calls);bytes.writeInt(ProcessingTiming1947.gpu);bytes.writeInt(ProcessingTiming1947.cpu);
        bytes.writeInt(ProcessingTiming1947.verification);bytes.writeInt(GpuNoise1960.opens);bytes.writeInt(GpuNoise1960.closes);
        bytes.writeInt(GpuNoise1960.submits);bytes.writeInt(GpuNoise1960.collects);bytes.writeInt(GpuStrongTuning1975.schedules);
        for(int[] array:new int[][]{d.source,d.out,d.confidence,d.policy}){bytes.writeInt(array.length);for(int n:array)bytes.writeInt(n);}
        for(int p=0;p<8;p++){String key=d.key(p);bytes.writeBoolean(GpuQualification1961.exactRejected(key));
            GpuQualification1961.Record r=GpuQualification1961.restore(key);bytes.writeBoolean(r!=null);
            if(r!=null){bytes.writeLong(r.cpuNanos);bytes.writeLong(r.gpuNanos);bytes.writeInt(r.variant);}}
        differentialCases++;
    }
    static void reason(String current){
        String expected=legacy?"deferred_quality1978":current;
        check(ProcessingTiming1947.reasons.size()==1&&Integer.valueOf(1).equals(ProcessingTiming1947.reasons.get(expected)),"actual CPU exclusion: "+expected+" got "+ProcessingTiming1947.reasons);
    }
    static void end(Data d)throws Exception{
        GpuStrong1960.endStage(d.model);
        check(GpuNoise1960.leases==0&&GpuNoise1960.active==0,"all deterministic native ownership released");
        check(GpuQualification1961.retainedBytes()==0,"a diagnostic label does not claim or create background ownership");
    }
    static void routeMatrix()throws Exception{
        for(int combination=0;combination<256;combination++){
            reset();Data d=new Data();GpuStrong1960.beginStage(d.model);int bits=0,expected=-1,n=combination;
            for(int p=0;p<4;p++){int mode=n&3;n>>>=2;seed(d,p,mode);if(mode==3&&expected<0)expected=p;if(mode<3)bits|=1<<mode;}
            GpuStrong1960.Route1981 r=d.route();check(r!=null&&r.profile==expected,"same lowest complete-cost certified profile: "+combination);
            check(d.process(r),"selected matrix route completes");end(d);d.exact();
            check(d.calls==(expected<0?1:0)&&ProcessingTiming1947.gpu==(expected<0?0:1)&&ProcessingTiming1947.cpu==(expected<0?1:0),"GPU counts only committed output; CPU runs once if selected");
            check(ProcessingTiming1947.verification==0,"diagnostics add no foreground CPU proof");
            check(GpuStrongTuning1975.schedules==1,"unchanged one idle scheduling opportunity");
            if(expected<0)reason(ROUTE[bits]);else check(ProcessingTiming1947.reasons.isEmpty(),"successful GPU is not reported as a CPU exclusion");
            record("matrix-"+combination,d,r);
        }
    }
    static void routeEdges()throws Exception{
        reset();Data d=new Data();GpuStrong1960.beginStage(d.model);for(int p=0;p<4;p++)seed(d,p,3);
        GpuNoise1960.available=false;GpuStrong1960.Route1981 r=d.route();check(r!=null&&r.profile<0,"environment decline keeps CPU");
        check(d.process(r),"environment fallback completes");end(d);d.exact();reason("route_unavailable1982");record("environment",d,r);
        for(int p=4;p<8;p++)for(int mismatch=0;mismatch<2;mismatch++){
            reset();d=new Data();GpuStrong1960.beginStage(d.model);for(int old=0;old<4;old++)seed(d,old,1);seed(d,p,3);
            GpuStrongTuning1975.tuned=new GpuStrongTuning1975.Choice(p,(p%3+mismatch)%3);
            r=d.route();check(r!=null&&r.profile==(mismatch==0?p:-1),"direct route respects its exact profile and workgroup");
            check(d.process(r),"direct or variant fallback completes");end(d);d.exact();
            if(mismatch!=0)reason("route_unqualified_exact1982");record("direct-"+p+"-"+mismatch,d,r);
        }
        reset();d=new Data();GpuStrong1960.beginStage(d.model);seed(d,0,3);for(int p=1;p<4;p++)seed(d,p,1);
        Object stage=value(GpuStrong1960.class,null,"active");
        @SuppressWarnings("unchecked") Map<String,Long> observations=(Map<String,Long>)value(stage.getClass(),stage,"completeGpu1981");
        observations.put(d.key(0),3000000000L);r=d.route();
        check(r.profile<0&&GpuQualification1961.restore(d.key(0))!=null,"same-photo complete-cost exclusion preserves exact certificate");
        check(d.process(r),"observed speed fallback completes");end(d);d.exact();reason("route_exact_speed1982");record("observed-speed",d,r);
    }
    static int claim(Object stage,String key,int variant,long started)throws Exception{
        return (Integer)invoke(GpuStrong1960.class,null,"claimBank1981",new Class<?>[]{stage.getClass(),String.class,int.class,long.class},stage,key,variant,started);
    }
    static String refusal(Object stage)throws Exception{return legacy?null:(String)value(stage.getClass(),stage,"bankRefusal1982");}
    static void bankRefusals()throws Exception{
        String[] expected={"bank_unavailable1982","bank_unavailable1982","bank_unavailable1982","bank_exact_rejected1982",
            "bank_unqualified1982","bank_unqualified1982","bank_speed_condition1982","bank_budget1982","bank_waiter1982",
            "bank_no_prediction1982","bank_no_prediction1982","bank_prediction_budget1982"};
        for(int scenario=0;scenario<expected.length;scenario++){
            reset();Data d=new Data();GpuStrong1960.beginStage(d.model);seed(d,0,3);
            Object stage=value(GpuStrong1960.class,null,"active");Class<?> type=stage.getClass();
            boolean[] banks=(boolean[])value(type,stage,"banks");long[] starts=(long[])value(type,stage,"bankStart"),durations=(long[])value(type,stage,"bankDuration");
            long started=System.nanoTime();int variant=0;
            synchronized(stage){
                if(scenario<3)field(type,new String[]{"closed","failed","unavailable"}[scenario]).setBoolean(stage,true);
                if(scenario==3)GpuQualification1961.rejectExact(d.key(0));
                if(scenario==4)variant=1;
                if(scenario==5){synchronized(lock()){cache("RECORDS").clear();}GpuQualification1961.initialize(new Context());}
                if(scenario==6)GpuQualification1961.qualifiedStrongPreferred1970(d.key(0),100,100,0);
                if(scenario==7)started-=100000000L;
                if(scenario>=8){Arrays.fill(banks,true);Arrays.fill(starts,started);Arrays.fill(durations,1000000L);}
                if(scenario==8)field(type,"waiter").setBoolean(stage,true);
                if(scenario==9)starts[0]=0;
                if(scenario==10)Arrays.fill(starts,started-1000000000L);
                if(scenario==11)Arrays.fill(durations,1000000000L);
                boolean[] before=banks.clone();int result=claim(stage,d.key(0),variant,started);
                check(result==-1&&Arrays.equals(before,banks),"refusal preserves bank ownership: "+scenario);
                if(!legacy)check(expected[scenario].equals(refusal(stage)),"precise refusal: "+scenario+" got "+refusal(stage));
                check(field(type,"waiter").getBoolean(stage)==(scenario==8),"immediate refusal neither queues nor steals another waiter");
                bytes.writeUTF("refusal-"+scenario);bytes.writeInt(result);bytes.writeBoolean(field(type,"waiter").getBoolean(stage));differentialCases++;
                Arrays.fill(banks,false);field(type,"closed").setBoolean(stage,false);field(type,"waiter").setBoolean(stage,false);
            }
            end(d);
        }
        // Reproduce the displayed bank_busy ambiguity through the public strip
        // facade as well as the private controlled admission boundary.
        for(int scenario=0;scenario<3;scenario++){
            reset();Data d=new Data();GpuStrong1960.beginStage(d.model);seed(d,0,3);GpuStrong1960.Route1981 r=d.route();
            Object stage=value(GpuStrong1960.class,null,"active");Class<?> type=stage.getClass();
            synchronized(stage){Arrays.fill((boolean[])value(type,stage,"banks"),true);
                Arrays.fill((long[])value(type,stage,"bankStart"),scenario==0?0:System.nanoTime());
                Arrays.fill((long[])value(type,stage,"bankDuration"),1000000000L);
                if(scenario==2)field(type,"waiter").setBoolean(stage,true);}
            check(d.process(r),"predicted bank refusal completes ordinary CPU");
            synchronized(stage){Arrays.fill((boolean[])value(type,stage,"banks"),false);field(type,"waiter").setBoolean(stage,false);}
            end(d);d.exact();String code=scenario==0?"bank_no_prediction1982":scenario==1?"bank_prediction_budget1982":"bank_waiter1982";
            check(ProcessingTiming1947.reasons.equals(Collections.singletonMap(legacy?"bank_busy":code,1)),"bank refusal is not asserted to have slept");
            check(d.calls==1&&GpuNoise1960.submits==0,"refused GPU performs no candidate dispatch or duplicate oracle");record("bank-facade-"+scenario,d,r);
        }
    }
    interface Condition {boolean ready()throws Exception;}
    static void until(Condition condition,String why)throws Exception{
        long limit=System.nanoTime()+3000000000L;while(!condition.ready()){if(System.nanoTime()>limit)throw new AssertionError(why);Thread.sleep(1);}check(true,why);
    }
    static void actualWait()throws Exception{
        for(int cancel=0;cancel<2;cancel++){
            reset();Data d=new Data();GpuStrong1960.beginStage(d.model);seed(d,0,3);
            final Object stage=value(GpuStrong1960.class,null,"active");final Class<?> type=stage.getClass();
            final boolean[] banks=(boolean[])value(type,stage,"banks");
            final AtomicInteger result=new AtomicInteger(-9);final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
            synchronized(stage){Arrays.fill(banks,true);Arrays.fill((long[])value(type,stage,"bankStart"),System.nanoTime());Arrays.fill((long[])value(type,stage,"bankDuration"),45000000L);}
            Thread thread=new Thread(()->{try{synchronized(stage){result.set(claim(stage,d.key(0),0,System.nanoTime()));}}catch(Throwable e){error.set(e);}},"diagnostic-bank-wait");
            thread.start();until(()->{synchronized(stage){return field(type,"waiter").getBoolean(stage);}},"real bounded waiter starts");
            if(cancel==1)thread.interrupt();else synchronized(stage){banks[0]=false;stage.notifyAll();}
            thread.join(3000);check(!thread.isAlive(),"bounded waiter terminates");
            check(cancel==1?error.get() instanceof CancellationException:error.get()==null&&result.get()==0,"release or cancellation keeps original result");
            check(!field(type,"waiter").getBoolean(stage),"waiter token released in finally");
            synchronized(stage){Arrays.fill(banks,false);}end(d);
            bytes.writeUTF("actual-wait-"+cancel);bytes.writeInt(result.get());bytes.writeBoolean(error.get() instanceof CancellationException);differentialCases++;
        }
    }
    static void monitorWait()throws Exception{
        reset();final Data d=new Data();GpuStrong1960.beginStage(d.model);seed(d,0,3);final GpuStrong1960.Route1981 r=d.route();
        final Object stage=value(GpuStrong1960.class,null,"active");final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        final AtomicBoolean completed=new AtomicBoolean();Thread thread=new Thread(()->{try{completed.set(d.process(r));}catch(Throwable e){failure.set(e);}},"diagnostic-monitor-wait");
        long held;
        synchronized(stage){thread.start();until(()->thread.getState()==Thread.State.BLOCKED,"strip blocks acquiring admission monitor");
            long started=System.nanoTime();Thread.sleep(80);held=System.nanoTime()-started;}
        thread.join(3000);check(!thread.isAlive()&&failure.get()==null&&completed.get(),"monitor contention retains certified GPU route");
        long oldCpu=field(stage.getClass(),"cpuWork1973").getLong(stage),oldGpu=field(stage.getClass(),"gpuWork1973").getLong(stage),oldBank=field(stage.getClass(),"waitWork1973").getLong(stage);
        end(d);d.exact();check(d.calls==0&&ProcessingTiming1947.gpu==1,"lock observation cannot cause a second oracle or drop committed GPU");
        check(ProcessingTiming1947.cpuWork==oldCpu&&ProcessingTiming1947.gpuWork==oldGpu&&ProcessingTiming1947.bankWork==oldBank,"legacy work timers are exported independently without lock-time addition");
        if(legacy)check(ProcessingTiming1947.lockCalls==0&&ProcessingTiming1947.lockWork==0,".81 did not report monitor acquisition wait");
        else check(ProcessingTiming1947.lockCalls==1&&ProcessingTiming1947.lockWork>=held,".82 separately reports the actual blocked monitor interval");
        record("monitor-wait",d,r);
    }
    static String failureName(String key)throws Exception{
        String env=(String)invoke(GpuQualification1961.class,null,"environment",new Class<?>[0]);
        return (String)invoke(GpuQualification1961.class,null,"recordKey",new Class<?>[]{String.class,String.class},key,env);
    }
    static Object failure(String key)throws Exception{return cache("FAILURES").get(failureName(key));}
    static String state()throws Exception{
        StringBuilder out=new StringBuilder();synchronized(lock()){
            for(Map.Entry<?,?> e:cache("RECORDS").entrySet()){GpuQualification1961.Record r=(GpuQualification1961.Record)e.getValue();out.append(e.getKey()).append(':').append(r.cpuNanos).append(':').append(r.gpuNanos).append(':').append(r.variant).append(';');}
            for(Map.Entry<?,?> e:cache("FAILURES").entrySet()){Object f=e.getValue();out.append(e.getKey()).append(':');for(String name:new String[]{"exact","retries","retryAfter","cause"})out.append(value(f.getClass(),f,name)).append(':');}
            for(String name:new String[]{"retained","lastLegacy1981","lastCapture","activeJob","QUEUED"})out.append(name).append('=').append(value(GpuQualification1961.class,null,name)).append(';');
        }return out.toString();
    }
    static void status()throws Exception{
        reset();String cooldown="single-cooldown",eligible="single-eligible",exhausted="single-exhausted",preferred="strong-gx1973-ieee-div-policy-bank-v1:3:diagnostic",exact="geometry-exact";
        for(String key:new String[]{cooldown,eligible,exhausted,preferred})GpuQualification1961.rejectSpeed(key);
        Object a=failure(eligible),b=failure(exhausted),p=failure(preferred);
        field(a.getClass(),"retryAfter").setLong(a,System.nanoTime()-1000000000L);
        field(b.getClass(),"retries").setInt(b,3);field(b.getClass(),"retryAfter").setLong(b,System.nanoTime()-1000000000L);
        field(p.getClass(),"retries").setInt(p,3);
        GpuQualification1961.rejectExact1971(exact,"confidence_mismatch");
        String exactName=failureName(exact);synchronized(lock()){cache("FAILURES").remove(exactName);}
        check(GpuQualification1961.exactRejected(exact),"signed exact rejection restores independently of diagnostic retry metadata");
        GpuQualification1961.qualified("accepted-record",1000,900,0);
        check(!GpuQualification1961.maySchedule(cooldown)&&GpuQualification1961.maySchedule(eligible)&&!GpuQualification1961.maySchedule(exhausted)&&GpuQualification1961.maySchedule(preferred)&&!GpuQualification1961.maySchedule(exact),"reported retry categories follow unchanged effective predicates including Strong exception");
        String before=state();int fingerprints=GpuNoise1960.fingerprints;String text=GpuQualification1961.status1967();
        check(before.equals(state())&&fingerprints==GpuNoise1960.fingerprints,"status changes neither state/LRU/retries nor environment queries");
        if(legacy)check(text.contains("速度条件待ち4件")&&text.contains("待ち0件 / 実行中0件"),".81 conflated four nonexact failure states as speed waiting");
        else{
            check(text.contains("予約済み0件 / 実行中0件")&&text.contains("確認済み記録1件 / 画質不一致の拒否1件"),"queue and actual cache counts are distinct");
            check(text.contains("再試行の時間条件内2件 / 再試行間隔待ち1件 / 再試行上限1件"),"cooldown, eligible and exhausted history are distinguished");
            check(text.contains("速度・利用失敗など")&&!text.contains("速度条件待ち"),"transport failures are not asserted to be measured slow output");
        }
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);final AtomicInteger closes=new AtomicInteger();
        GpuQualification1961.Probe blocking=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){entered.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}public void close(){closes.incrementAndGet();}};
        GpuQualification1961.Probe queued=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){throw new AssertionError("queued proof must remain queued during observation");}public void close(){closes.incrementAndGet();}};
        check(GpuQualification1961.schedule("diagnostic-active",512,blocking)&&GpuQualification1961.schedule("diagnostic-queued",256,queued),"two real detached owners are accepted into production queue");
        text=GpuQualification1961.status1967();check(text.contains((legacy?"待ち":"予約済み")+"2件 / 実行中0件"),"queued jobs count independently of four cache failures");
        synchronized(lock()){field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);SaveQueue1935.idle=true;GpuQualification1961.wake();}
        check(entered.await(3,TimeUnit.SECONDS),"production idle worker begins the first job");
        before=state();fingerprints=GpuNoise1960.fingerprints;text=GpuQualification1961.status1967();
        check(text.contains((legacy?"待ち":"予約済み")+"1件 / 実行中1件"),"active and queued ownership are separately observed");
        check(before.equals(state())&&fingerprints==GpuNoise1960.fingerprints&&GpuQualification1961.retainedBytes()==768,"observing active proof retains exact queue and image ownership");
        SaveQueue1935.idle=false;GpuQualification1961.captureChanged();release.countDown();
        until(()->GpuQualification1961.retainedBytes()==0,"canceled queued and active owners are released");
        check(closes.get()==2,"each production snapshot closes exactly once");
        check(GpuQualification1961.maySchedule(eligible)&&GpuQualification1961.maySchedule(preferred)&&!GpuQualification1961.maySchedule(exhausted)&&GpuQualification1961.exactRejected(exact),"status and cancellation do not alter retry or exact decisions");
        bytes.writeUTF("status-decisions");for(String key:new String[]{cooldown,eligible,exhausted,preferred,exact})bytes.writeBoolean(GpuQualification1961.maySchedule(key));differentialCases++;
    }
    interface Case {void run()throws Exception;}
    static void run(String name,Case operation)throws Exception{int before=assertions;operation.run();tests.put(name,assertions-before);}
    static String hex(byte[] input)throws Exception{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(input))out.append(String.format("%02x",b&255));return out.toString();}
    public static void main(String[] args)throws Exception{
        legacy=args.length!=0&&"legacy".equals(args[0]);
        run("all_candidate_reason_combinations",()->routeMatrix());
        run("environment_profile_variant_and_live_speed",()->routeEdges());
        run("bank_refusal_and_public_fallback",()->bankRefusals());
        run("bank_release_and_cancellation",()->actualWait());
        run("monitor_wait_and_legacy_timer_separation",()->monitorWait());
        run("queue_cache_retry_and_observation",()->status());
        bytes.flush();StringBuilder groups=new StringBuilder();for(Map.Entry<String,Integer> e:tests.entrySet()){if(groups.length()!=0)groups.append(',');groups.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"differential_cases\":"+differentialCases+",\"decision_output_sha256\":\""+hex(decisions.toByteArray())+"\",\"legacy\":"+legacy+",\"tests\":{"+groups+"}}");
    }
}
