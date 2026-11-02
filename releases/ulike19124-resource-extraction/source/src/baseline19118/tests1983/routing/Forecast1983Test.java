package com.hiro.ulike;

import com.hiro.ulike.GpuStrong1960.Forecast1983;
import java.lang.reflect.*;
import java.util.*;

/** Pure clock inputs for forecast retention, plus real dispatcher integration
 * with the unchanged synthetic transport. Scalar timing never certifies pixels. */
public final class Forecast1983Test {
    static int assertions;static final long MS=1000000L;
    static final Map<String,Integer> groups=new LinkedHashMap<String,Integer>();
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static Map<?,?> cache()throws Exception{return (Map<?,?>)field(Forecast1983.class,"RECENT").get(null);}
    static void clear()throws Exception{cache().clear();}
    static String key(String env,String route,int variant,int workers,int parallel,long cpu,long gpu){return Forecast1983.key(env,route,variant,workers,parallel,cpu,gpu);}
    static void samples(String key,long elapsed,long cpu,long now){Forecast1983.completed(key,101,elapsed,cpu,now-10);Forecast1983.completed(key,102,elapsed,cpu,now);}
    static void scopeAndExpiry()throws Exception{
        clear();long now=100000000000L,cpu=200*MS,gpu=2*MS;
        String base=key("driver-A","exact-shape-A",0,4,2,cpu,gpu);
        check(base!=null,"complete valid scalar key exists");
        Forecast1983.completed(base,1,8*MS,cpu,now);
        check(Forecast1983.cost(base,gpu,now)==gpu,"one photo does not turn a cold outlier into a later routing cost");
        Forecast1983.completed(base,1,9*MS,cpu,now+1);
        check(Forecast1983.cost(base,gpu,now+1)==gpu,"many strips in the same photo are one independent observation");
        Forecast1983.completed(base,2,7*MS,cpu,now+2);
        check(Forecast1983.cost(base,gpu,now+2)==9*MS,"two photos use the conservative recent maximum");
        String[] other={key("driver-B","exact-shape-A",0,4,2,cpu,gpu),key("driver-A","exact-shape-B",0,4,2,cpu,gpu),
            key("driver-A","exact-shape-A",1,4,2,cpu,gpu),key("driver-A","exact-shape-A",0,3,2,cpu,gpu),
            key("driver-A","exact-shape-A",0,4,1,cpu,gpu),key("driver-A","exact-shape-A",0,4,2,cpu+1,gpu),
            key("driver-A","exact-shape-A",0,4,2,cpu,gpu+1)};
        for(String alternative:other)check(!base.equals(alternative)&&Forecast1983.cost(alternative,gpu,now+2)==gpu,"different environment/route/variant/workers/overlap/certificate cannot inherit an observation");
        Forecast1983.completed(base,3,6*MS,cpu,now+3);Forecast1983.completed(base,4,5*MS,cpu,now+4);
        check(Forecast1983.cost(base,gpu,now+4)==7*MS,"rolling three-photo window drops old outliers");
        check(Forecast1983.cost(base,gpu,now+4+Forecast1983.TTL)==gpu&&cache().isEmpty(),"all expired samples are discarded");
        samples(base,MS,cpu,now);
        check(Forecast1983.cost(base,gpu,now)==gpu,"forecast never claims a GPU faster than its exact existing certificate");
        check(Forecast1983.cost(base,gpu,now-20)==gpu&&cache().isEmpty(),"clock reversal does not revive stale observations");
        check(key("","route",0,4,2,cpu,gpu)==null&&key("driver",null,0,4,2,cpu,gpu)==null&&key("driver","route",3,4,2,cpu,gpu)==null,"missing or invalid route identity is uncached");
        check(key("driver","route",0,0,2,cpu,gpu)==null&&key("driver","route",0,5,2,cpu,gpu)==null&&key("driver","route",0,4,3,cpu,gpu)==null,"unknown or incompatible worker/bank geometry is uncached");
        check(key("driver","route",0,4,2,0,gpu)==null&&key("driver","route",0,4,2,cpu,0)==null,"a forecast cannot manufacture missing measured certificate times");
    }
    static void coolingAndBounds()throws Exception{
        clear();long now=200000000000L,cpu=100*MS,gpu=10*MS;
        String slow=key("driver","slow-route",0,4,2,cpu,gpu);samples(slow,120*MS,cpu,now);
        check(Forecast1983.cost(slow,gpu,now)>cpu-cpu/20,"measured unprofitable parallel work chooses CPU while cooling");
        check(Forecast1983.cost(slow,gpu,now+Forecast1983.SLOW_COOLING)==gpu,"cooling expiry permits the original qualified path to be measured again");
        check(cache().isEmpty(),"speed hints do not become permanent quality rejection entries");
        for(int i=0;i<Forecast1983.LIMIT+9;i++)samples(key("driver","bounded-"+i,0,4,1,cpu,gpu),20*MS,cpu,now);
        check(cache().size()==Forecast1983.LIMIT,"strict scalar history bound is enforced");
        check(Forecast1983.cost(key("driver","bounded-0",0,4,1,cpu,gpu),gpu,now)==gpu,"oldest route is evicted");
        check(Forecast1983.cost(key("driver","bounded-"+(Forecast1983.LIMIT+8),0,4,1,cpu,gpu),gpu,now)==20*MS,"newest route retains its actual timing");
    }
    static void foregroundCollection()throws Exception{
        clear();String sampled=null;int[] values=new int[2];
        for(int frame=0;frame<2;frame++){
            StrongDiagnostics1982Test.reset();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();
            GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);StrongDiagnostics1982Test.seed(d,0,3);
            GpuQualification1961.Record proof=GpuQualification1961.restore(d.key(0));
            sampled=key(GpuNoise1960.fingerprint(),d.key(0),proof.variant,4,1,proof.cpuNanos,proof.gpuNanos);
            check(d.process(d.route()),"production GPU selection completes before timing can be cached");
            GpuStrong1960.endStage(d.model);d.exact();
            check(ProcessingTiming1947.gpu==1&&ProcessingTiming1947.cpu==0&&ProcessingTiming1947.verification==0,"forecasts do not add CPU proof or change valid output");
            Object entry=cache().get(sampled);check(entry!=null,"actual released foreground bank contributes a scalar observation");
            long[] durations=(long[])field(entry.getClass(),"nanos").get(entry);
            for(long duration:durations)if(duration>0)values[frame]++;
            check(GpuNoise1960.active==0&&GpuNoise1960.leases==0,"prediction does not retain native bank ownership");
        }
        check(values[0]==1&&values[1]==2,"two real stages are distinguished without retaining model or bitmap");
        Object entry=cache().get(sampled);
        for(Field f:entry.getClass().getDeclaredFields())check(f.getType().isPrimitive()||f.getType()==long[].class,"history entry contains scalar values only: "+f.getName());
    }
    static void routingHistory()throws Exception{
        StrongDiagnostics1982Test.reset();clear();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();
        GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);StrongDiagnostics1982Test.seed(d,0,3);
        GpuQualification1961.Record proof=GpuQualification1961.restore(d.key(0));long now=System.nanoTime();
        String single=key(GpuNoise1960.fingerprint(),d.key(0),proof.variant,4,1,proof.cpuNanos,proof.gpuNanos);
        samples(single,proof.cpuNanos,proof.cpuNanos,now);
        GpuStrong1960.Route1981 rejected=d.route();check(rejected.profile<0,"a currently unprofitable forecast rejects GPU before policy preparation");
        check(d.process(rejected),"speed-based selection completes the unchanged CPU oracle");GpuStrong1960.endStage(d.model);d.exact();
        check(ProcessingTiming1947.cpu==1&&ProcessingTiming1947.gpu==0&&GpuNoise1960.submits==0,"short-term slow observation is a real CPU routing decision");
        check(GpuQualification1961.restore(d.key(0))!=null&&!GpuQualification1961.exactRejected(d.key(0)),"adaptive CPU choice cannot erase or fabricate the exact pixel certificate");
        Forecast1983.cost(single,proof.gpuNanos,now+Forecast1983.SLOW_COOLING);
        GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);
        check(d.route().profile==0,"expired cooldown returns to the same certified GPU candidate");GpuStrong1960.endStage(d.model);
        // Peak two-bank costs must not disqualify an otherwise fast sole bank.
        clear();GpuStrong1960.beginStage(d.model);GpuStrong1960.configureWorkers1978(d.model,4);
        String parallel=key(GpuNoise1960.fingerprint(),d.key(0),proof.variant,4,2,proof.cpuNanos,proof.gpuNanos);
        samples(parallel,proof.cpuNanos,proof.cpuNanos,System.nanoTime());
        check(d.route().profile==0,"busy two-bank evidence cannot reject an empty single-bank route");
        check(d.process(d.route()),"submission re-check uses its own claimed bank count, not a fictitious extra requester");
        GpuStrong1960.endStage(d.model);d.exact();
        check(GpuQualification1961.restore(d.key(0))!=null,"throughput history preserves its underlying signed proof");
    }
    static void residentSeparation()throws Exception{
        StrongDiagnostics1982Test.reset();clear();StrongDiagnostics1982Test.Data d=new StrongDiagnostics1982Test.Data();
        GpuStrong1960.beginStage(d.model);StrongDiagnostics1982Test.seed(d,0,3);Object s=field(GpuStrong1960.class,"active").get(null);Class<?> type=s.getClass();
        field(type,"resident1976").setBoolean(s,true);field(type,"residentDeadline1981").setLong(s,System.nanoTime()-1);
        Method preferred=GpuStrong1960.class.getDeclaredMethod("claimPreferred1970",type,long.class);preferred.setAccessible(true);
        synchronized(s){check((Integer)preferred.invoke(null,s,System.nanoTime())<0,"whole-chain deadline remains authoritative even with a free bank");}
        check(cache().isEmpty(),"resident route does not import ordinary strip forecasts");
        field(type,"resident1976").setBoolean(s,false);GpuStrong1960.endStage(d.model);
    }
    interface Case {void run()throws Exception;}
    static void run(String name,Case test)throws Exception{int before=assertions;test.run();groups.put(name,assertions-before);}
    public static void main(String[] args)throws Exception{
        run("scope_expiry_and_monotonic_floor",()->scopeAndExpiry());run("slow_cooling_and_retention_bound",()->coolingAndBounds());
        run("real_foreground_collection",()->foregroundCollection());run("actual_routing_with_history",()->routingHistory());run("resident_deadline_separation",()->residentSeparation());
        StringBuilder text=new StringBuilder();for(Map.Entry<String,Integer> e:groups.entrySet()){if(text.length()!=0)text.append(',');text.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"tests\":{"+text+"}}");
    }
}
