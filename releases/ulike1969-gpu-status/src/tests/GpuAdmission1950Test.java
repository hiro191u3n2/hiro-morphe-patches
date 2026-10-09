package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Runs the production gate. The backend is deliberately a host oracle, never
 * evidence of device GPU speed or GPU arithmetic; shaders have a separate suite. */
public final class GpuAdmission1950Test {
    private static final AtomicInteger assertions=new AtomicInteger();
    private static int scenarios;
    private static void req(boolean ok,String why){assertions.incrementAndGet();if(!ok)throw new AssertionError(why);}
    private static final class Timer extends GpuInteger1949.Clock {
        private final ThreadLocal<Integer> at=new ThreadLocal<Integer>(){protected Integer initialValue(){return 0;}};
        private final ThreadLocal<long[]> values=new ThreadLocal<long[]>(){protected long[] initialValue(){return new long[]{0,10,10,100};}};
        void fast(){at.set(0);values.set(new long[]{0,10,10,100});}
        void slow(){at.set(0);values.set(new long[]{0,100,100,110});}
        long now(){int i=at.get();at.set(i+1);long[] v=values.get();return v[Math.min(i,v.length-1)];}
    }
    private static final class Backend implements GpuInteger1949.Backend {
        volatile boolean bad,miss;final AtomicInteger aggregateCalls=new AtomicInteger(),finishCalls=new AtomicInteger(),finishIntoCalls=new AtomicInteger();
        volatile int[] directOutput;volatile int directOffset;
        public boolean available(){return true;}
        public boolean fusion(byte[] d,int w,int h,int step,int[] sums,int[] counts){
            GpuInteger1949.cpuSums(d,w,h,step,sums,counts);return true;
        }
        public boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,int begin,
                int end,int lo,int hi,int radius,int[] range){
            aggregateCalls.incrementAndGet();aggregateOracle(input,meta,out,w,begin,end);
            if(bad)out[0]^=1;return !miss;
        }
        public boolean finish(int[] input,int[] meta,int[] policy,int[] out,int w,int rows,int begin,
                int end,int lo,int hi,int radius,int[] range,int noise,boolean shadows,
                int global,int beauty,int smoothLimit){
            finishCalls.incrementAndGet();finishOracle(input,meta,policy,out,w,begin,end,noise,shadows,global,beauty,smoothLimit);
            if(bad)out[0]^=1;return !miss;
        }
        public boolean finishInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,
                int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
                boolean shadows,int global,int beauty,int smoothLimit){
            finishIntoCalls.incrementAndGet();directOutput=out;directOffset=outputOffset;
            finishOracleInto(input,meta,policy,out,outputOffset,w,begin,end,noise,shadows,global,beauty,smoothLimit);
            if(bad)out[outputOffset]^=1;return !miss;
        }
    }
    private static void aggregateOracle(int[] input,int[] meta,int[] out,int w,int begin,int end){
        for(int i=0;i<w*(end-begin);i++){
            if(meta[i]<0){out[3*i]=input[w*begin+i]^0x01a359;out[3*i+1]=input[w*begin+i]^0x057d43;out[3*i+2]=0x80030109;}
            else out[3*i+2]=0;
        }
    }
    private static void finishOracle(int[] input,int[] meta,int[] policy,int[] out,int w,int begin,int end,
            int noise,boolean shadows,int global,int beauty,int smoothLimit){
        finishOracleInto(input,meta,policy,out,0,w,begin,end,noise,shadows,global,beauty,smoothLimit);
    }
    private static void finishOracleInto(int[] input,int[] meta,int[] policy,int[] out,int outputOffset,int w,int begin,int end,
            int noise,boolean shadows,int global,int beauty,int smoothLimit){
        for(int i=0;i<w*(end-begin);i++)if(meta[i]<0)
            out[outputOffset+i]=input[w*begin+i]^policy[3*i]^policy[3*i+1]^policy[3*i+2]^noise^global^beauty^smoothLimit^(shadows?123:0);
    }
    private static final class Tile implements GpuInteger1949.CpuAggregate,GpuInteger1949.CpuFinish,GpuInteger1949.CpuFinishInto {
        final int w,rows,begin,end,lo,hi,radius;final int[] input,meta,policy,range=new int[33*256];
        final AtomicInteger aggregateCpu,finishCpu;boolean finishing;
        int noise=12,global=31,beauty=17,smoothLimit=44;boolean shadows=true;
        Tile(int w,int rows,int begin,int end,int lo,int hi,int radius,AtomicInteger aggregateCpu,AtomicInteger finishCpu){
            this.w=w;this.rows=rows;this.begin=begin;this.end=end;this.lo=lo;this.hi=hi;this.radius=radius;
            this.aggregateCpu=aggregateCpu;this.finishCpu=finishCpu;
            input=new int[w*rows];meta=new int[w*(end-begin)];policy=new int[meta.length*3];
            for(int i=0;i<input.length;i++)input[i]=(i*0x10203)&0xffffff;
            for(int i=0;i<meta.length;i++){meta[i]=i%3==0?0:0x80000123;policy[i*3]=i;policy[i*3+1]=i*3;policy[i*3+2]=i*7;}
        }
        public boolean run(int[] out){
            if(finishing){finishCpu.incrementAndGet();finishOracle(input,meta,policy,out,w,begin,end,noise,shadows,global,beauty,smoothLimit);}
            else {aggregateCpu.incrementAndGet();aggregateOracle(input,meta,out,w,begin,end);}
            return true;
        }
        public boolean run(int[] out,int outputOffset){
            finishCpu.incrementAndGet();finishOracleInto(input,meta,policy,out,outputOffset,w,begin,end,noise,shadows,global,beauty,smoothLimit);return true;
        }
        int[] seed(boolean finish){int[] out=new int[meta.length*(finish?1:3)+7];Arrays.fill(out,0x715273);return out;}
        boolean aggregate(GpuInteger1949.Engine e,int[] out){finishing=false;return e.aggregate(input,meta,out,w,rows,begin,end,lo,hi,radius,range,this);}
        boolean finish(GpuInteger1949.Engine e,int[] out){finishing=true;return e.finish(input,meta,policy,out,w,rows,begin,end,lo,hi,radius,range,noise,shadows,global,beauty,smoothLimit,this);}
        void equal(int[] out,boolean finish){int[] expected=seed(finish);
            if(finish)finishOracle(input,meta,policy,expected,w,begin,end,noise,shadows,global,beauty,smoothLimit);
            else aggregateOracle(input,meta,expected,w,begin,end);
            req(Arrays.equals(expected,out),"CPU-equivalent pixels, inactive seeds and tail");}
    }
    private static void positionsDoNotThrash(){scenarios++;
        Backend backend=new Backend();Timer clock=new Timer();GpuInteger1949.Engine engine=new GpuInteger1949.Engine(backend,clock);
        AtomicInteger cpu=new AtomicInteger(),finishCpu=new AtomicInteger();
        // More than LIMIT distinct global positions, each sharing the actual
        // uploaded 40-row geometry. A second capture changes global height.
        for(int capture=0;capture<2;capture++)for(int position=0;position<48;position++){
            int rows=1800+capture*160,begin=16+position*32;
            Tile tile=new Tile(9,rows,begin,begin+32,0,rows,4,cpu,finishCpu);
            int[] out=tile.seed(false);clock.fast();req(tile.aggregate(engine,out),"interior tile dispatch");tile.equal(out,false);
        }
        req(backend.aggregateCalls.get()==96,"one dispatch for each unique input");
        req(cpu.get()==4,"3 exact admission references then only the 64-call refresh across 96 positions");
        // Global rows and absolute begin/end no longer have a bearing on this
        // shape's rejection. Border/tail/radius geometry still differs.
        backend.bad=true;clock.fast();
        // Advance to the next required 64-call reference, then disable shape.
        int before=cpu.get();int attempts=0;
        while(cpu.get()==before&&attempts++<64){Tile t=new Tile(9,2100,64,96,0,2100,4,cpu,finishCpu);req(t.aggregate(engine,t.seed(false)),"advance admitted refresh");}
        req(cpu.get()==before+1,"refresh compares current capture");
        req(!engine.aggregateAvailable(9,2500,800,832,0,2500,4),"disabled interior reused at shifted position and global height");
        req(engine.aggregateAvailable(9,2500,0,32,0,2500,4),"top boundary has distinct compact halo");
        req(engine.aggregateAvailable(9,2500,2468,2500,0,2500,4),"bottom boundary has distinct compact halo");
        req(engine.aggregateAvailable(9,2500,800,816,0,2500,4),"tail output dimensions have independent gate");
        req(engine.aggregateAvailable(9,2500,800,832,0,2500,2),"smaller radius has distinct transfer geometry");
    }
    private static void finishAdmissionAndFallback(){scenarios++;
        Backend backend=new Backend();Timer clock=new Timer();GpuInteger1949.Engine engine=new GpuInteger1949.Engine(backend,clock);
        AtomicInteger aggregateCpu=new AtomicInteger(),finishCpu=new AtomicInteger();
        for(int i=0;i<50;i++){
            Tile tile=new Tile(13,2000,20+i*32,52+i*32,0,2000,4,aggregateCpu,finishCpu);
            int[] input=tile.input.clone(),meta=tile.meta.clone(),policy=tile.policy.clone(),out=tile.seed(true);
            clock.fast();req(tile.finish(engine,out),"finish accepted");tile.equal(out,true);
            req(Arrays.equals(input,tile.input)&&Arrays.equals(meta,tile.meta)&&Arrays.equals(policy,tile.policy),"finish input arrays unchanged");
        }
        req(finishCpu.get()==3&&aggregateCpu.get()==0,"finish cold plus two comparisons, no repeated CPU or old aggregate path");
        Tile changed=new Tile(13,2000,20,52,0,2000,4,aggregateCpu,finishCpu);changed.noise++;
        backend.bad=true;clock.fast();int[] out=changed.seed(true);req(changed.finish(engine,out),"changed options independent reference");changed.equal(out,true);
        int dispatches=backend.finishCalls.get();clock.fast();req(!changed.finish(engine,changed.seed(true)),"mismatched finish delegates CPU fallback");
        req(dispatches==backend.finishCalls.get(),"mismatch prevents repeated finish dispatch");
        backend.bad=false;Tile original=new Tile(13,2000,600,632,0,2000,4,aggregateCpu,finishCpu);clock.fast();
        out=original.seed(true);req(original.finish(engine,out),"original exact options remain admitted");original.equal(out,true);
    }
    private static void directFinishMeasuresWholeRoute(){scenarios++;
        Backend backend=new Backend();Timer clock=new Timer();GpuInteger1949.Engine engine=new GpuInteger1949.Engine(backend,clock);
        AtomicInteger aggregateCpu=new AtomicInteger(),finishCpu=new AtomicInteger();
        Tile legacy=new Tile(13,2000,20,52,0,2000,4,aggregateCpu,finishCpu);
        for(int i=0;i<3;i++){clock.fast();req(legacy.finish(engine,legacy.seed(true)),"legacy finish preadmission");}
        req(finishCpu.get()==3,"old tile-output gate fully admitted");
        for(int i=0;i<20;i++){
            Tile tile=new Tile(13,2000,20+i*32,52+i*32,0,2000,4,aggregateCpu,finishCpu);
            int outputOffset=17+i*13,outSize=outputOffset+tile.meta.length+37;
            int[] out=new int[outSize];Arrays.fill(out,0x715273);int[] expected=out.clone(),input=tile.input.clone();
            finishOracleInto(tile.input,tile.meta,tile.policy,expected,outputOffset,tile.w,tile.begin,tile.end,tile.noise,tile.shadows,tile.global,tile.beauty,tile.smoothLimit);
            clock.fast();req(engine.finishInto(tile.input,tile.meta,tile.policy,out,outputOffset,tile.w,tile.rows,
                    tile.begin,tile.end,tile.lo,tile.hi,tile.radius,tile.range,tile.noise,tile.shadows,tile.global,tile.beauty,tile.smoothLimit,tile),"complete destination route accepted");
            req(Arrays.equals(expected,out),"offset tile exact; inactive, prefix and suffix preserved");
            req(Arrays.equals(input,tile.input),"direct output route keeps source immutable");
            if(i>=3)req(backend.directOutput==out&&backend.directOffset==outputOffset,"admitted backend receives original full output, no Java copyback");
        }
        req(finishCpu.get()==6,"whole direct route receives 3 real proofs independent of old tile-only admission");
        req(backend.finishIntoCalls.get()==20,"3 probation and 17 admitted calls exercise identical direct endpoint");
        int before=backend.finishCalls.get()+backend.finishIntoCalls.get();
        req(!engine.finishInto(legacy.input,legacy.meta,legacy.policy,new int[legacy.meta.length],1,legacy.w,legacy.rows,
                legacy.begin,legacy.end,legacy.lo,legacy.hi,legacy.radius,legacy.range,legacy.noise,legacy.shadows,legacy.global,legacy.beauty,legacy.smoothLimit,legacy),"direct destination offset overflow rejected");
        req(!engine.finishInto(legacy.input,legacy.meta,legacy.policy,legacy.seed(true),-1,legacy.w,legacy.rows,
                legacy.begin,legacy.end,legacy.lo,legacy.hi,legacy.radius,legacy.range,legacy.noise,legacy.shadows,legacy.global,legacy.beauty,legacy.smoothLimit,legacy),"negative destination offset rejected");
        req(before==backend.finishCalls.get()+backend.finishIntoCalls.get(),"invalid direct geometry never dispatches");
        backend.bad=true;legacy.noise++;clock.fast();int[] out=new int[legacy.meta.length+32];Arrays.fill(out,0x715273);int[] expected=out.clone();
        finishOracleInto(legacy.input,legacy.meta,legacy.policy,expected,16,legacy.w,legacy.begin,legacy.end,legacy.noise,legacy.shadows,legacy.global,legacy.beauty,legacy.smoothLimit);
        req(engine.finishInto(legacy.input,legacy.meta,legacy.policy,out,16,legacy.w,legacy.rows,legacy.begin,legacy.end,
                legacy.lo,legacy.hi,legacy.radius,legacy.range,legacy.noise,legacy.shadows,legacy.global,legacy.beauty,legacy.smoothLimit,legacy),"mismatched direct candidate uses exact full CPU destination");
        req(Arrays.equals(expected,out),"candidate mismatch cannot publish any GPU pixels or alter neighbors");
    }
    private static void cpuReferenceDoesNotBlockOtherShapes()throws Exception{scenarios++;
        final Backend backend=new Backend();final Timer clock=new Timer();final GpuInteger1949.Engine engine=new GpuInteger1949.Engine(backend,clock);
        final CountDownLatch cpuEntered=new CountDownLatch(1),releaseCpu=new CountDownLatch(1),independentDone=new CountDownLatch(1),availabilityDone=new CountDownLatch(1),sameDone=new CountDownLatch(1);
        final Throwable[] failures=new Throwable[4];
        final Tile first=new Tile(11,100,20,52,0,100,4,new AtomicInteger(),new AtomicInteger());
        Thread blocked=new Thread(new Runnable(){public void run(){try{
            req(engine.aggregate(first.input,first.meta,first.seed(false),first.w,first.rows,first.begin,first.end,first.lo,first.hi,first.radius,first.range,
                new GpuInteger1949.CpuAggregate(){public boolean run(int[] out){cpuEntered.countDown();try{
                    if(!releaseCpu.await(10,TimeUnit.SECONDS))throw new AssertionError("CPU release timeout");
                }catch(InterruptedException error){throw new AssertionError(error);}first.aggregateCpu.incrementAndGet();aggregateOracle(first.input,first.meta,out,first.w,first.begin,first.end);return true;}}),"blocked CPU reference returned");
        }catch(Throwable failure){failures[0]=failure;}}});
        blocked.start();req(cpuEntered.await(2,TimeUnit.SECONDS),"reference reached CPU after dispatch");
        Thread other=new Thread(new Runnable(){public void run(){try{
            Tile second=new Tile(12,100,20,52,0,100,4,new AtomicInteger(),new AtomicInteger());int[] out=second.seed(false);
            req(second.aggregate(engine,out),"unrelated shape executes while first reference waits");second.equal(out,false);
        }catch(Throwable failure){failures[1]=failure;}finally{independentDone.countDown();}}});
        Thread availability=new Thread(new Runnable(){public void run(){try{
            req(engine.aggregateAvailable(first.w,first.rows,first.begin,first.end,first.lo,first.hi,first.radius),"same-shape availability read during probation");
            req(engine.fusionAvailable(17,19,4),"unrelated availability read during probation");
        }catch(Throwable failure){failures[2]=failure;}finally{availabilityDone.countDown();}}});
        Thread same=new Thread(new Runnable(){public void run(){try{
            Tile tile=new Tile(11,100,54,86,0,100,4,new AtomicInteger(),new AtomicInteger());
            int[] out=tile.seed(false),before=out.clone();
            req(!tile.aggregate(engine,out),"busy compact shape immediately delegates original CPU");
            req(Arrays.equals(before,out),"busy candidate never writes or shares another invocation result");
            req(tile.run(out),"same-shape caller completes independent CPU fallback");tile.equal(out,false);
        }catch(Throwable failure){failures[3]=failure;}finally{sameDone.countDown();}}});
        other.start();availability.start();same.start();
        boolean independent=independentDone.await(2,TimeUnit.SECONDS),available=availabilityDone.await(2,TimeUnit.SECONDS),sameFallback=sameDone.await(2,TimeUnit.SECONDS);
        releaseCpu.countDown();blocked.join(3000);other.join(3000);availability.join(3000);same.join(3000);
        req(independent,"CPU reference does not hold global or dispatch lock");req(available,"availability never waits for CPU verification");
        req(sameFallback,"same-shape caller never waits behind another CPU comparison");
        req(!blocked.isAlive()&&!other.isAlive()&&!availability.isAlive()&&!same.isAlive(),"all concurrency workers complete");
        for(Throwable failure:failures)if(failure!=null)throw new AssertionError(failure);
        // The skipped busy caller is not counted as a proof or speed win.
        // Once its owner releases the claim, two real comparisons admit it.
        for(int i=0;i<3;i++){
            Tile tile=new Tile(11,100,58,90,0,100,4,first.aggregateCpu,new AtomicInteger());int[] out=tile.seed(false);
            clock.fast();req(tile.aggregate(engine,out),"completed claim permits continued exact probation");tile.equal(out,false);
        }
        req(first.aggregateCpu.get()==3,"busy skip does not count as a probe; admitted after 3 actual comparisons");
    }
    private static void finishBounds(){scenarios++;
        Backend backend=new Backend();GpuInteger1949.Engine engine=new GpuInteger1949.Engine(backend,new Timer());
        Tile t=new Tile(13,50,4,20,0,50,4,new AtomicInteger(),new AtomicInteger());
        req(!engine.finish(t.input,t.meta,t.policy,t.input,t.w,t.rows,t.begin,t.end,t.lo,t.hi,t.radius,t.range,t.noise,t.shadows,t.global,t.beauty,t.smoothLimit,t),"finish rejects output alias");
        req(!engine.finish(t.input,t.meta,new int[1],t.seed(true),t.w,t.rows,t.begin,t.end,t.lo,t.hi,t.radius,t.range,t.noise,t.shadows,t.global,t.beauty,t.smoothLimit,t),"finish rejects truncated policy");
        req(!engine.finish(t.input,t.meta,t.policy,t.seed(true),t.w,t.rows,t.begin,t.end,t.lo,t.hi,t.radius,t.range,t.noise,t.shadows,t.global,t.beauty,t.smoothLimit,null),"finish requires independent CPU callback");
        req(backend.finishCalls.get()==0,"invalid finish calls never dispatch");
    }
    public static void main(String[] args)throws Exception{
        positionsDoNotThrash();finishAdmissionAndFallback();directFinishMeasuresWholeRoute();cpuReferenceDoesNotBlockOtherShapes();finishBounds();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions.get()+",\"scenarios\":"+scenarios+"}");
    }
}
