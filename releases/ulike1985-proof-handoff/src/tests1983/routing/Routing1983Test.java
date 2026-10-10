package com.hiro.ulike;

import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual old/current dispatcher and certificate service with the unchanged
 * .82 transport holder. Its pixels prove output/ownership, not shader quality
 * or Android speed. Only the late-release case intentionally changes route. */
public final class Routing1983Test {
    static int assertions;static boolean legacy;
    static final Map<String,Integer> groups=new LinkedHashMap<String,Integer>();
    static final MessageDigest outputs;
    static {try{outputs=MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object value(Object owner,String name)throws Exception{return field(owner.getClass(),name).get(owner);}
    static Object stage()throws Exception{return field(GpuStrong1960.class,"active").get(null);}
    static Object call(String name,Class<?>[] types,Object...args)throws Exception{
        Method m=GpuStrong1960.class.getDeclaredMethod(name,types);m.setAccessible(true);
        try{return m.invoke(null,args);}catch(InvocationTargetException wrapped){Throwable e=wrapped.getCause();if(e instanceof Exception)throw (Exception)e;throw (Error)e;}
    }
    static void reset()throws Exception{
        StrongDiagnostics1982Test.reset();
        if(!legacy){Class<?> type=Class.forName("com.hiro.ulike.GpuStrong1960$Forecast1983");((Map<?,?>)field(type,"RECENT").get(null)).clear();}
    }
    static StrongDiagnostics1982Test.Data begin()throws Exception{
        reset();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();
        GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);StrongDiagnostics1982Test.seed(d,0,3);return d;
    }
    static void busy(Object stage,long duration,long age)throws Exception{
        synchronized(stage){Arrays.fill((boolean[])value(stage,"banks"),true);
            Arrays.fill((long[])value(stage,"bankStart"),System.nanoTime()-age);
            Arrays.fill((long[])value(stage,"bankDuration"),duration);}
    }
    static void free(Object stage)throws Exception{
        synchronized(stage){Arrays.fill((boolean[])value(stage,"banks"),false);field(stage.getClass(),"waiter").setBoolean(stage,false);stage.notifyAll();}
    }
    static void output(StrongDiagnostics1982Test.Data d){
        d.exact();for(int[] a:new int[][]{d.source,d.out,d.confidence,d.policy})for(int n:a){outputs.update((byte)(n>>>24));outputs.update((byte)(n>>>16));outputs.update((byte)(n>>>8));outputs.update((byte)n);}
    }
    static void end(StrongDiagnostics1982Test.Data d)throws Exception{
        Object s=stage();free(s);GpuStrong1960.endStage(d.model);
        check(GpuNoise1960.active==0&&GpuNoise1960.leases==0,"all actual holder bank/session ownership drains");output(d);
    }
    static void reason(String expected){check(Integer.valueOf(1).equals(ProcessingTiming1947.reasons.get(expected)),"CPU selection reason "+expected+" got "+ProcessingTiming1947.reasons);}
    static int claim(Object s,String key,long started)throws Exception{return (Integer)call("claimBank1981",new Class<?>[]{s.getClass(),String.class,int.class,long.class},s,key,0,started);}
    static String refusal(Object s)throws Exception{return (String)value(s,"bankRefusal1982");}

    static void lateRelease()throws Exception{
        StrongDiagnostics1982Test.Data d=begin();Object s=stage();GpuStrong1960.Route1981 route=d.route();
        busy(s,1000000L,1000000000L);
        CountDownLatch poised=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Thread release=new Thread(()->{poised.countDown();try{synchronized(s){((boolean[])value(s,"banks"))[0]=false;s.notifyAll();}}catch(Throwable e){error.set(e);}},"late-real-bank-release");
        synchronized(s){
            release.start();check(poised.await(2,TimeUnit.SECONDS),"release actor poised behind the real stage monitor");
            check(d.process(route),"late release produces a complete selected output");
        }
        release.join(2000);check(!release.isAlive()&&error.get()==null,"release actor completes");
        end(d);
        check(d.calls==(legacy?1:0),".82 selects CPU immediately; .83 uses the bounded real release");
        check(ProcessingTiming1947.gpu==(legacy?0:1)&&ProcessingTiming1947.cpu==(legacy?1:0),"intentional routing difference is explicit");
        check(ProcessingTiming1947.verification==0,"re-evaluation adds no foreground CPU oracle");
        if(legacy)reason("bank_no_prediction1982");else check(ProcessingTiming1947.reasons.isEmpty(),"successful GPU is not reported as a refusal");
    }
    static void loggedDistribution()throws Exception{
        StrongDiagnostics1982Test.Data d=begin();Object s=stage();GpuStrong1960.Route1981 route=d.route();
        for(int i=0;i<4;i++)check(d.process(route),"four actual GPU commits complete");
        busy(s,1000000L,1000000000L);
        for(int i=0;i<10;i++)check(d.process(route),"occupied overdue banks without real releases still use CPU");
        synchronized(s){field(s.getClass(),"waiter").setBoolean(s,true);}
        check(d.process(route),"single waiter ceiling still keeps remaining CPU available");
        synchronized(s){field(s.getClass(),"waiter").setBoolean(s,false);}
        // A distinct full source-row geometry has no child certificate. This
        // is a genuine unqualified key, not a forged CPU Route1981 object.
        int[] source=new int[d.width*(d.rows+1)],out=new int[source.length],policy=new int[d.policy.length];
        System.arraycopy(d.source,0,source,0,d.source.length);int[] cf=new int[d.confidence.length];
        boolean ok=GpuStrong1960.process1981(source,out,d.width,d.rows+1,d.begin,d.end,0,d.rows+1,d.origin,4,true,d.model,policy,cf,null,null,
            new GpuStrong1960.Oracle(){public boolean run(int[] target,int[] confidence){for(int i=d.begin*d.width;i<d.end*d.width;i++)target[i]=StrongNoise1958.pixel(source[i]);Arrays.fill(confidence,0,4,37);return true;}});
        check(ok,"unqualified shape performs the existing CPU route");
        end(d);
        check(ProcessingTiming1947.gpu==4&&ProcessingTiming1947.cpu==12,"original screenshot's 4 GPU / 12 CPU outcome is reproduced when banks do not release");
        check(Integer.valueOf(10).equals(ProcessingTiming1947.reasons.get("bank_no_prediction1982")),"ten expired-forecast refusals retain honest classification");
        reason("bank_waiter1982");reason("route_unqualified1982");
    }
    static void retainedGates()throws Exception{
        for(int scenario=0;scenario<6;scenario++){
            StrongDiagnostics1982Test.Data d=begin();Object s=stage();GpuStrong1960.Route1981 route=d.route();String expected;
            if(scenario==0){GpuQualification1961.rejectExact(d.key(0));route=d.route();expected="route_unqualified_exact1982";}
            else if(scenario==1){GpuQualification1961.qualifiedStrongPreferred1970(d.key(0),1000000L,1000000L,0);route=d.route();expected="route_unqualified_speed1982";}
            else if(scenario==2){field(s.getClass(),"failed").setBoolean(s,true);expected="bank_unavailable1982";}
            else if(scenario==3){busy(s,1000000000L,0);expected="bank_prediction_budget1982";}
            else if(scenario==4){busy(s,1000000L,1000000000L);field(s.getClass(),"waiter").setBoolean(s,true);expected="bank_waiter1982";}
            else {busy(s,1000000L,1000000000L);GpuStrongRouting1978.mixed=true;expected="cpu_faster1978";}
            check(d.process(route),"existing gate chooses a complete CPU output: "+scenario);end(d);reason(expected);
            check(d.calls==1&&ProcessingTiming1947.gpu==0&&ProcessingTiming1947.cpu==1,"no rejected or unprofitable GPU output bypasses CPU");
            check(GpuNoise1960.submits==0,"declined admission dispatches no unqualified candidate");
            if(scenario==0)check(GpuQualification1961.exactRejected(d.key(0)),"exact rejection survives routing fallback");
            if(scenario==5)check(ProcessingTiming1947.routeMode==1,"measured CPU/GPU cohort retains priority over retrying an overdue bank");
        }
    }
    static void budgetsAndCancellation()throws Exception{
        StrongDiagnostics1982Test.Data d=begin();Object s=stage();
        synchronized(s){check(claim(s,d.key(0),System.nanoTime()-60000000L)<0&&"bank_budget1982".equals(refusal(s)),"expired original 50ms deadline refuses even a free bank");}
        // No output was requested in the scalar admission checks.
        free(s);GpuStrong1960.endStage(d.model);
        if(legacy)return;
        d=begin();s=stage();busy(s,1000000L,1000000000L);long until,generation;
        synchronized(s){
            check(claim(s,d.key(0),System.nanoTime())<0&&"bank_no_prediction1982".equals(refusal(s)),"really stuck banks terminate finite re-evaluation");
            until=(Long)value(s,"overdueUntil1983");generation=(Long)value(s,"overdueGeneration1983");
            check(claim(s,d.key(0),System.nanoTime())<0,"a new caller cannot extend the same occupied-bank grace window");
            check((Long)value(s,"overdueUntil1983")==until&&(Long)value(s,"overdueGeneration1983")==generation,"grace is shared across callers without deadline reset");
            check(!field(s.getClass(),"waiter").getBoolean(s),"bounded exhaustion releases the waiter token");
        }
        free(s);GpuStrong1960.endStage(d.model);
        for(int scenario=0;scenario<3;scenario++){
            d=begin();s=stage();busy(s,1000000L,1000000000L);
            final Object owner=s;final String key=d.key(0);final int mode=scenario;
            CountDownLatch poised=new CountDownLatch(1);AtomicReference<Throwable> actorError=new AtomicReference<Throwable>();
            Thread caller=Thread.currentThread();
            Thread actor=new Thread(()->{poised.countDown();try{synchronized(owner){
                if(mode==0)caller.interrupt();else if(mode==1)field(owner.getClass(),"closed").setBoolean(owner,true);else GpuQualification1961.rejectExact(key);
                owner.notifyAll();
            }}catch(Throwable e){actorError.set(e);}},"bounded-recheck-cancel-or-close");
            Throwable observed=null;int result=-99;
            synchronized(s){actor.start();check(poised.await(2,TimeUnit.SECONDS),"cancellation/closure actor poised");
                try{result=claim(s,key,System.nanoTime());}catch(Throwable e){observed=e;}
                if(mode==0)check(observed instanceof CancellationException&&Thread.currentThread().isInterrupted(),"interrupt remains observable and terminates GPU re-evaluation");
                else check(result<0&&((mode==1?"bank_unavailable1982":"bank_exact_rejected1982").equals(refusal(s))),"live closure or revoked certificate stops waiting");
                check(!field(s.getClass(),"waiter").getBoolean(s),"all cancellations release their sole waiter slot");
            }
            Thread.interrupted();actor.join(2000);check(!actor.isAlive()&&actorError.get()==null,"cancellation actor drains");
            field(s.getClass(),"closed").setBoolean(s,false);free(s);GpuStrong1960.endStage(d.model);
        }
    }
    interface Case {void run()throws Exception;}
    static void run(String name,Case test)throws Exception{int before=assertions;test.run();groups.put(name,assertions-before);}
    static String hex(byte[] digest){StringBuilder text=new StringBuilder();for(byte b:digest)text.append(String.format("%02x",b&255));return text.toString();}
    public static void main(String[] args)throws Exception{
        legacy=args.length!=0&&"legacy".equals(args[0]);
        run("late_real_release",()->lateRelease());run("logged_distribution_without_release",()->loggedDistribution());
        run("quality_speed_cohort_and_capacity_gates",()->retainedGates());run("deadline_cooling_cancel_and_close",()->budgetsAndCancellation());
        StringBuilder summary=new StringBuilder();for(Map.Entry<String,Integer> e:groups.entrySet()){if(summary.length()!=0)summary.append(',');summary.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"legacy\":"+legacy+",\"output_sha256\":\""+hex(outputs.digest())+"\",\"tests\":{"+summary+"}}");
    }
}
