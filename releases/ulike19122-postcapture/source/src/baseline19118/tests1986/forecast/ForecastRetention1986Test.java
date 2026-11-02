package com.hiro.ulike;

import com.hiro.ulike.GpuStrong1960.Forecast1983;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;

/** Explicit monotonic-clock inputs exercise the real production forecaster.
 * Controlled transport checks routing and all output words, not device speed
 * or native shader quality. The published .85 source executes the same gaps. */
public final class ForecastRetention1986Test {
    static final long MS=1000000L,SECOND=1000*MS;
    static int assertions,lossCases;static boolean legacy;
    static String routeOutput;
    static final Map<String,Integer> groups=new LinkedHashMap<String,Integer>();
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Object value(Class<?> type,Object owner,String name)throws Exception{return field(type,name).get(owner);}
    @SuppressWarnings("unchecked") static Map<String,Object> cache()throws Exception{return (Map<String,Object>)value(Forecast1983.class,null,"RECENT");}
    static void clear()throws Exception{cache().clear();ProcessingTiming1947.forecastFailure1986=0;ProcessingTiming1947.forecastCalls1986=0;}
    static String key(String route,long cpu,long gpu){return Forecast1983.key("driver-86",route,0,4,1,cpu,gpu);}
    static int samples(String key)throws Exception{
        Object entry=cache().get(key);if(entry==null)return 0;int count=0;
        for(long n:(long[])value(entry.getClass(),entry,"nanos"))if(n>0)count++;return count;
    }
    static long diagnostic(String key,long certified,long now,long current)throws Exception{
        return ((Long)StrongDiagnostics1982Test.invoke(Forecast1983.class,null,"cost1986",
            new Class<?>[]{String.class,long.class,long.class,long.class,ProcessingTiming1947.Trace.class},
            key,certified,now,current,ProcessingTiming1947.trace)).longValue();
    }
    static void observe(String key,long first,long second,long cpu,long firstAt,long secondAt){
        Forecast1983.completed(key,11,first,cpu,firstAt);Forecast1983.completed(key,12,second,cpu,secondAt);
    }
    static void gaps()throws Exception{
        long cpu=100*MS,gpu=10*MS,t=100*SECOND;
        for(long gap:new long[]{500*MS,1500*MS,3*SECOND,4675*MS,11*SECOND}){
            clear();String k=key("gap-"+gap,cpu,gpu);
            Forecast1983.completed(k,1,120*MS,cpu,t);
            // A later photo must ask for a route before it can produce a second
            // successful foreground completion. This ordering exposed .85 loss.
            check(Forecast1983.cost(k,gpu,t+gap)==gpu,"one capture remains insufficient at gap "+gap);
            boolean lost=legacy&&gap>=Forecast1983.SLOW_COOLING;
            check(samples(k)==(lost?0:1),"first observation retention before next completion at gap "+gap);
            Forecast1983.completed(k,2,110*MS,cpu,t+gap);
            check(samples(k)==(lost?1:2),"independent second completion at gap "+gap);
            long expected=lost?gpu:120*MS;
            check(Forecast1983.cost(k,gpu,t+gap)==expected,"next route uses the live conservative maximum at gap "+gap);
            check(Forecast1983.cost(k,gpu,t+gap+MS)==expected,"a second lookup cannot invent or discard a capture at gap "+gap);
            if(lost)lossCases++;
        }
        check(lossCases==(legacy?4:0),"published baseline loses four seconds-apart pairs; current retains all five pairs");
    }
    static void retentionAndExpiry()throws Exception{
        clear();long cpu=100*MS,gpu=10*MS,t=200*SECOND;String k=key("cooling-versus-retention",cpu,gpu);
        observe(k,120*MS,110*MS,cpu,t,t+500*MS);
        check(Forecast1983.cost(k,gpu,t+2*SECOND)==120*MS,"cooling boundary retains both still-fresh observations");
        Object entry=cache().get(k);check(entry!=null&&!field(entry.getClass(),"slow").getBoolean(entry),"only the transient cooling flag ends");
        check(field(entry.getClass(),"coolingExpired1986").getBoolean(entry),"ended cooling is available for scalar diagnostics");
        long[] timestamps=((long[])value(entry.getClass(),entry,"at")).clone();
        for(long age:new long[]{2100*MS,4*SECOND,8*SECOND,Forecast1983.TTL-1})
            check(Forecast1983.cost(k,gpu,t+age)==120*MS,"reads retain but never optimistically lower a live two-capture maximum");
        check(Arrays.equals(timestamps,(long[])value(entry.getClass(),entry,"at")),"routing reads do not refresh history timestamps");
        check(Forecast1983.cost(k,gpu,t+Forecast1983.TTL)==gpu,"exact TTL of the older sample leaves fewer than two and permits the original certificate");
        check(samples(k)==1,"expiry discards only the stale observation");
        check(Forecast1983.cost(k,gpu,t+Forecast1983.TTL+500*MS)==gpu&&cache().isEmpty(),"exact TTL of the final sample fully releases the scalar entry");
        check(Forecast1983.cost(k,gpu,t+100*SECOND)==gpu&&cache().isEmpty(),"reads after expiry cannot revive stale slow history");
        Forecast1983.completed(k,3,8*MS,cpu,t+100*SECOND);
        Forecast1983.completed(k,4,9*MS,cpu,t+101*SECOND);
        check(Forecast1983.cost(k,gpu,t+101*SECOND)==gpu,"fresh faster observations recover without predicting faster than the certificate");
        clear();observe(k,120*MS,110*MS,cpu,t,t+SECOND);
        check(Forecast1983.cost(k,gpu,t-1)==gpu&&cache().isEmpty(),"clock reversal fails closed to the existing certificate without stale samples");
    }
    static void independenceAndBounds()throws Exception{
        clear();long cpu=100*MS,gpu=10*MS,t=300*SECOND;String k=key("same-capture",cpu,gpu);
        for(int i=0;i<24;i++){
            Forecast1983.completed(k,71,(110+i)*MS,cpu,t+i*250*MS);
            check(Forecast1983.cost(k,gpu,t+i*250*MS)==gpu,"many completed strips remain one independent capture");
        }
        check(samples(k)==1,"one capture occupies one scalar slot");
        Forecast1983.completed(k,72,115*MS,cpu,t+6*SECOND);
        check(Forecast1983.cost(k,gpu,t+6*SECOND)==133*MS,"second capture confirms the maximum from the first capture");
        Forecast1983.completed(k,73,112*MS,cpu,t+7*SECOND);
        Forecast1983.completed(k,74,111*MS,cpu,t+8*SECOND);
        check(samples(k)==3&&Forecast1983.cost(k,gpu,t+8*SECOND)==115*MS,"bounded three-capture window drops the retired oldest maximum");
        Object entry=cache().get(k);
        for(Field f:entry.getClass().getDeclaredFields()){
            check(f.getType().isPrimitive()||f.getType()==long[].class,"history retains no pixels, model, session or trace: "+f.getName());
            if(f.getType()==long[].class){f.setAccessible(true);check(((long[])f.get(entry)).length==3,"each scalar observation array remains fixed at three");}
        }
        clear();
        for(int i=0;i<Forecast1983.LIMIT+10;i++)observe(key("bounded-"+i,cpu,gpu),120*MS,110*MS,cpu,t,t+500*MS);
        check(cache().size()==64,"slow histories obey the original 64-key LRU memory bound");
        check(Forecast1983.cost(key("bounded-0",cpu,gpu),gpu,t+2*SECOND)==gpu,"evicted history does not influence a route");
        check(Forecast1983.cost(key("bounded-73",cpu,gpu),gpu,t+2*SECOND)==120*MS,"newest slow history survives cooling within the bound");
    }
    static void scalarDiagnostics()throws Exception{
        clear();long cpu=100*MS,gpu=10*MS,t=400*SECOND;String k=key("diagnostic",cpu,gpu);
        check(diagnostic(k,gpu,t,20*MS)==20*MS&&ProcessingTiming1947.forecastState1986==0,"no history reports the existing current-stage floor");
        Forecast1983.completed(k,1,120*MS,cpu,t);
        check(diagnostic(k,gpu,t+500*MS,20*MS)==20*MS,"one slow capture cannot override the current-stage floor");
        check(ProcessingTiming1947.forecastState1986==1&&ProcessingTiming1947.forecastSamples1986==1&&ProcessingTiming1947.forecastObserved1986==120*MS,
            "diagnostic explicitly distinguishes the raw single observation from applied prediction");
        Forecast1983.completed(k,2,110*MS,cpu,t+500*MS);
        check(diagnostic(k,gpu,t+500*MS,20*MS)==120*MS&&ProcessingTiming1947.forecastState1986==3,"two captures apply their conservative maximum during short cooling");
        check(ProcessingTiming1947.forecastAge1986==500*MS&&ProcessingTiming1947.forecastSamples1986==2,"age describes the oldest valid observation used by this scan");
        check(diagnostic(k,gpu,t+2*SECOND,140*MS)==140*MS&&ProcessingTiming1947.forecastState1986==4,"cooling ends while history stays active and cannot lower a bank floor");
        check(ProcessingTiming1947.forecastCertified1986==gpu&&ProcessingTiming1947.forecastObserved1986==120*MS&&ProcessingTiming1947.forecastChosen1986==140*MS,
            "certificate, raw observed maximum, and final chosen interval are distinct exact scalar values");
        check(diagnostic(k,gpu,t+Forecast1983.TTL,20*MS)==20*MS&&ProcessingTiming1947.forecastState1986==5&&ProcessingTiming1947.forecastSamples1986==1,
            "TTL pruning below two reports bounded recovery without reducing the current-stage floor");
        check(diagnostic(k,gpu,t+Forecast1983.TTL+500*MS,gpu)==gpu&&ProcessingTiming1947.forecastState1986==5&&ProcessingTiming1947.forecastAge1986==-1,
            "complete expiry reports no remaining observation age");
        check(diagnostic(k,gpu,t+Forecast1983.TTL+SECOND,gpu)==gpu&&ProcessingTiming1947.forecastState1986==0,"subsequent uncached query is correctly observation-free");
        clear();observe(k,70*MS,80*MS,cpu,t,t+SECOND);
        check(diagnostic(k,gpu,t+SECOND,gpu)==80*MS&&ProcessingTiming1947.forecastState1986==2,"two profitable observations need no slow-cooling state");
        check(diagnostic(null,gpu,t,200*MS)==200*MS,"absent identity cannot lower a same-stage floor");
        for(int failure=1;failure<=4;failure++){
            ProcessingTiming1947.forecastFailure1986=failure;
            check(diagnostic(k,gpu,t+SECOND,90*MS)==90*MS,"optional diagnostic failure cannot alter the routing cost: "+failure);
        }
        ProcessingTiming1947.forecastFailure1986=0;
        check(ProcessingTiming1947.forecastCalls1986>4,"failure isolation exercises the diagnostic boundary, not a disabled path");
    }
    static String actualKey(StrongDiagnostics1982Test.Data d,GpuQualification1961.Record p,int parallel){
        return Forecast1983.key(GpuNoise1960.fingerprint(),d.key(0),p.variant,4,parallel,p.cpuNanos,p.gpuNanos);
    }
    static String digest(StrongDiagnostics1982Test.Data d)throws Exception{
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        for(int[] array:new int[][]{d.out,d.confidence})for(int n:array){hash.update((byte)(n>>>24));hash.update((byte)(n>>>16));hash.update((byte)(n>>>8));hash.update((byte)n);}
        StringBuilder s=new StringBuilder();for(byte b:hash.digest())s.append(String.format("%02x",b&255));return s.toString();
    }
    static void actualRouting()throws Exception{
        StrongDiagnostics1982Test.reset();clear();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();
        GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);StrongDiagnostics1982Test.seed(d,0,3);
        GpuQualification1961.Record proof=GpuQualification1961.restore(d.key(0));String k=actualKey(d,proof,1);long now=System.nanoTime();
        Forecast1983.completed(k,1001,proof.cpuNanos,proof.cpuNanos,now-3*SECOND);
        check(Forecast1983.cost(k,proof.gpuNanos,now)==proof.gpuNanos,"the second photo begins with only one independent observation");
        Forecast1983.completed(k,1002,proof.cpuNanos,proof.cpuNanos,now);
        GpuStrong1960.Route1981 route=d.route();
        check(legacy?route.profile==0:route.profile<0,"three-second history changes the actual preflight decision only after two captures");
        check(d.process(route),"selected foreground output completes through the unchanged oracle/transport");StrongDiagnostics1982Test.end(d);d.exact();
        check(ProcessingTiming1947.gpu==(legacy?1:0)&&ProcessingTiming1947.cpu==(legacy?0:1)&&ProcessingTiming1947.verification==0,
            "only final commits are counted and no foreground comparison is introduced");
        GpuQualification1961.Record after=GpuQualification1961.restore(d.key(0));
        check(after!=null&&after.cpuNanos==proof.cpuNanos&&after.gpuNanos==proof.gpuNanos&&after.variant==proof.variant&&!GpuQualification1961.exactRejected(d.key(0)),
            "a conservative forecast preserves its matching certificate and does not create quality rejection");
        routeOutput=digest(d);
    }
    static void integrationFloors()throws Exception{
        StrongDiagnostics1982Test.reset();clear();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();
        GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);StrongDiagnostics1982Test.seed(d,0,3);
        Object stage=value(GpuStrong1960.class,null,"active");Class<?> st=stage.getClass();
        GpuQualification1961.Record proof=GpuQualification1961.restore(d.key(0));long now=System.nanoTime();
        String k=actualKey(d,proof,1);observe(k,7*MS,8*MS,proof.cpuNanos,now-SECOND,now);
        @SuppressWarnings("unchecked") Map<String,Long> current=(Map<String,Long>)value(st,stage,"completeGpu1981");current.put(d.key(0),12*MS);
        long chosen=((Long)StrongDiagnostics1982Test.invoke(GpuStrong1960.class,null,"gpuCost1983",
            new Class<?>[]{st,String.class,GpuQualification1961.Record.class,int.class},stage,d.key(0),proof,-1)).longValue();
        check(chosen==12*MS&&ProcessingTiming1947.forecastChosen1986==chosen,"actual route diagnostic includes the same-photo maximum");
        boolean[] banks=(boolean[])value(st,stage,"banks");long[] durations=(long[])value(st,stage,"bankDuration");
        synchronized(stage){
            banks[0]=true;durations[0]=17*MS;
            StrongDiagnostics1982Test.invoke(GpuStrong1960.class,null,"bankForecast1983",
                new Class<?>[]{st,int.class,String.class,GpuQualification1961.Record.class,long.class},stage,0,d.key(0),proof,now);
            check(durations[0]==17*MS&&ProcessingTiming1947.forecastChosen1986==17*MS,"live-bank diagnostic includes its existing nondecreasing duration floor");
            observe(actualKey(d,proof,2),34*MS,35*MS,proof.cpuNanos,now-SECOND,now);
            banks[1]=true;durations[1]=3*MS;
            StrongDiagnostics1982Test.invoke(GpuStrong1960.class,null,"bankForecast1983",
                new Class<?>[]{st,int.class,String.class,GpuQualification1961.Record.class,long.class},stage,1,d.key(0),proof,now);
            check(durations[0]==35*MS&&durations[1]==35*MS,"actual overlap uses its own history and only extends both live-bank predictions");
            check(ProcessingTiming1947.forecastChosen1986==35*MS&&ProcessingTiming1947.forecastObserved1986==35*MS,"reported final bank interval comes from the same lookup");
            Arrays.fill(banks,false);
        }
        StrongDiagnostics1982Test.end(d);
    }
    static void optionalRoutingAndRecovery()throws Exception{
        String previous=null;
        for(int failure=0;failure<=4;failure++){
            StrongDiagnostics1982Test.reset();clear();ProcessingTiming1947.forecastFailure1986=failure;
            StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();
            GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);StrongDiagnostics1982Test.seed(d,0,3);
            GpuQualification1961.Record proof=GpuQualification1961.restore(d.key(0));long now=System.nanoTime();String k=actualKey(d,proof,1);
            observe(k,proof.cpuNanos,proof.cpuNanos,proof.cpuNanos,now-4*SECOND,now-2*SECOND);
            GpuStrong1960.Route1981 route=d.route();check(route.profile<0,"expired short cooling does not optimistically admit a retained slow route: "+failure);
            check(d.process(route),"diagnostic failure cannot stop the ordinary CPU fallback: "+failure);StrongDiagnostics1982Test.end(d);d.exact();
            check(ProcessingTiming1947.cpu==1&&ProcessingTiming1947.gpu==0&&GpuNoise1960.submits==0,"slow route neither dispatches GPU nor duplicates its CPU oracle");
            check(Forecast1983.cost(k,proof.gpuNanos,now+Forecast1983.TTL)==proof.gpuNanos&&cache().isEmpty(),"observation expiry recovers without changing the certificate");
            GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);
            route=d.route();check(route.profile==0,"the original fully certified GPU route becomes eligible after bounded expiry");
            check(d.process(route),"diagnostic failure cannot stop the recovered certified GPU route");StrongDiagnostics1982Test.end(d);d.exact();
            check(ProcessingTiming1947.cpu==1&&ProcessingTiming1947.gpu==1&&ProcessingTiming1947.verification==0&&d.calls==1,"recovery uses one real GPU commit with no additional foreground CPU proof");
            check(GpuQualification1961.restore(d.key(0))!=null&&!GpuQualification1961.exactRejected(d.key(0)),"CPU recovery and diagnostics preserve quality proof");
            check(ProcessingTiming1947.forecastCalls1986>0,"optional callback was exercised during actual routing");
            String output=digest(d);check(previous==null||previous.equals(output),"all ARGB/confidence output is identical with every injected diagnostic failure");previous=output;
        }
        ProcessingTiming1947.forecastFailure1986=0;
    }
    interface Case {void run()throws Exception;}
    static void run(String name,Case body)throws Exception{
        int before=assertions,external=StrongDiagnostics1982Test.assertions;body.run();
        assertions+=StrongDiagnostics1982Test.assertions-external;groups.put(name,assertions-before);
    }
    public static void main(String[] args)throws Exception{
        legacy=args.length>0&&"legacy".equals(args[0]);
        run("capture_gap_sequence",()->gaps());run("seconds_apart_actual_routing",()->actualRouting());
        if(!legacy){run("cooling_independent_ttl_recovery",()->retentionAndExpiry());run("distinct_captures_and_scalar_bounds",()->independenceAndBounds());
            run("same_scan_scalar_diagnostics",()->scalarDiagnostics());run("actual_current_stage_and_bank_floors",()->integrationFloors());
            run("optional_diagnostics_and_real_recovery",()->optionalRoutingAndRecovery());}
        StringBuilder out=new StringBuilder();for(Map.Entry<String,Integer> e:groups.entrySet()){if(out.length()>0)out.append(',');out.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"tests\":{"+out+"},\"baseline_loss_cases\":"+lossCases+",\"route_output_sha256\":\""+routeOutput+"\"}");
    }
}
