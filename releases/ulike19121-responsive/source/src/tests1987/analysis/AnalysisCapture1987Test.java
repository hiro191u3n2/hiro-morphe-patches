package com.hiro.ulike;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.concurrent.CancellationException;

/** Actual production wrappers and CPU estimators. Controlled peers exercise
 * admission/fault paths; no fixture result represents Android GPU performance. */
public final class AnalysisCapture1987Test {
    static long assertions;static int expectedCalls;
    static void check(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
    static void pause(long nanos){if(nanos<=0)return;long end=System.nanoTime()+nanos;while(System.nanoTime()<end)Thread.yield();}
    static void reset(){GpuQualification1961.reset();GpuNoise1960.reset();}
    static int[] image(int width,int height,int seed){int[] a=new int[width*height];int state=seed;
        for(int i=0;i<a.length;i++){state=state*1664525+1013904223;a[i]=(i%47==0?0x80000000:0xff000000)|(state&0xffffff);}return a;}
    static final class Source implements SpatialNoise1934.Patches,StrongNoise1958.Patches {
        final int width,height;final int[] pixels;int reads,cancelAt,throwAt;RuntimeException failure;
        Source(int width,int height,int seed){this.width=width;this.height=height;pixels=image(width,height,seed);}
        public void read(int[] dst,int x,int y,int w,int h){reads++;if(reads==cancelAt)GpuQualification1961.current=false;
            if(reads==throwAt)throw failure;
            for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,dst,row*w,w);}
        Source copy(){Source c=new Source(width,height,1);System.arraycopy(pixels,0,c.pixels,0,pixels.length);return c;}
    }
    static int spatialCount(int width,int height){return Math.max(1,Math.min(13,(width+127)/128))*Math.max(1,Math.min(13,(height+127)/128));}
    static int regionCount(int width,int height){return ((width+63)/64)*((height+63)/64);}
    static float[] evidence(){float[] a=new float[16];for(int i=0;i<a.length;i++)a[i]=(i+1)*.21f;return a;}
    static GpuAnalysis1961.RegionsCpu regionCpu(final int width,final int height,final float[] ev,final long delay){
        return new GpuAnalysis1961.RegionsCpu(){public float[] compute(final StrongNoise1958.Patches source){
            StrongNoise1958.Patches reader=delay<=0?source:new StrongNoise1958.Patches(){int reads;public void read(int[] dst,int x,int y,int w,int h){if(++reads==2)pause(delay);source.read(dst,x,y,w,h);}};
            return StrongNoise1958.estimateRegionsSnapshot1961(reader,width,height,ev);}};
    }
    static boolean same(float[] a,float[] b){if(a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(Float.floatToRawIntBits(a[i])!=Float.floatToRawIntBits(b[i]))return false;return true;}
    static int[][] bits(float[] a){int[] out=new int[a.length];for(int i=0;i<a.length;i++)out[i]=Float.floatToRawIntBits(a[i]);return new int[][]{out};}
    static String regionKey(int w,int h){return "regions1961:v2:"+w+":"+h;}
    static String spatialKey(int w,int h){return "spatial1961:v2:"+w+":"+h;}
    static boolean reaches(Object value,Object target,IdentityHashMap<Object,Boolean> seen)throws Exception{
        if(value==null)return false;if(value==target)return true;if(seen.put(value,Boolean.TRUE)!=null)return false;
        Class<?> type=value.getClass();if(type.isArray()){if(type.getComponentType().isPrimitive())return false;for(int i=0;i<Array.getLength(value);i++)if(reaches(Array.get(value,i),target,seen))return true;return false;}
        if(!type.getName().startsWith("com.hiro.ulike."))return false;
        for(Field f:type.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&!f.getType().isPrimitive()){f.setAccessible(true);if(reaches(f.get(value),target,seen))return true;}return false;
    }
    static void firstRead(boolean negative)throws Exception{
        int[][] sizes=negative?new int[][]{{257,259}}:new int[][]{{1,1},{15,17},{65,67},{257,259},{511,513},{3060,4080}};
        for(int[] size:sizes){int w=size[0],h=size[1];reset();Source source=new Source(w,h,19),oracleSource=source.copy();
            SpatialNoise1934 expected=SpatialNoise1934.probeCpu1961(oracleSource,w,h),actual=GpuAnalysis1961.spatial(source,w,h);
            check(SpatialNoise1934.same1961(expected,actual),"all spatial sigma/global float bits preserved");
            check(source.reads==spatialCount(w,h),"FIRST_READ_CAPTURE_MISSING: spatial source read twice");
            check(GpuQualification1961.pending!=null&&GpuQualification1961.retained>0,"spatial immutable evidence queued");
            check(!reaches(GpuQualification1961.pending,source,new IdentityHashMap<Object,Boolean>()),"spatial proof retains no caller source");
            reset();source=new Source(w,h,41);float[] ev=evidence();float[] ref=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),w,h,ev);int[] selected={9};
            float[] output=GpuAnalysis1961.regions1982(source,w,h,ev,regionCpu(w,h,ev,0),selected);
            check(same(ref,output),"all regional evidence float bits preserved");
            check(selected[0]==0,"unqualified foreground remains CPU");
            check(source.reads==regionCount(w,h),"FIRST_READ_CAPTURE_MISSING: regional source read twice");
            check(GpuQualification1961.pending!=null&&GpuQualification1961.retained>0,"regional immutable evidence queued");
            check(!reaches(GpuQualification1961.pending,source,new IdentityHashMap<Object,Boolean>()),"regional proof retains no caller source");
        }
    }
    static void denials(){int w=129,h=131;float[] ev=evidence();
        for(int mode=0;mode<7;mode++){reset();switch(mode){case 0:GpuQualification1961.allow=false;break;case 1:GpuNoise1960.budget=false;break;case 2:GpuNoise1960.available=false;break;case 3:GpuNoise1960.busy=true;break;case 4:GpuQualification1961.background=true;break;case 5:GpuQualification1961.beginAllowed=false;break;case 6:GpuQualification1961.throwReserve=true;break;}
            Source source=new Source(w,h,mode+9);float[] expected=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),w,h,ev);
            float[] output=GpuAnalysis1961.regions1982(source,w,h,ev,regionCpu(w,h,ev,0),new int[1]);
            check(same(expected,output),"denied optional capture keeps exact CPU output "+mode);
            check(source.reads==regionCount(w,h)&&GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"denied capture reads once and releases every reservation "+mode);
            check(GpuQualification1961.exactFailures+GpuQualification1961.speedFailures==0,"denial spends no GPU rejection retry "+mode);
        }
        reset();GpuNoise1960.throwBudget=true;Source source=new Source(w,h,5);SpatialNoise1934 expected=SpatialNoise1934.probeCpu1961(source.copy(),w,h);
        SpatialNoise1934 output=GpuAnalysis1961.spatial(source,w,h);
        check(SpatialNoise1934.same1961(expected,output)&&source.reads==spatialCount(w,h),"optional allocation error preserves spatial CPU output and single read");
        check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"allocation-error ownership released");
    }
    static void cancellation(){int w=129,h=131;float[] ev=evidence();reset();Source source=new Source(w,h,8);source.cancelAt=2;
        float[] expected=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),w,h,ev);
        float[] output=GpuAnalysis1961.regions1982(source,w,h,ev,regionCpu(w,h,ev,0),new int[1]);
        check(same(expected,output)&&source.reads==regionCount(w,h),"new capture epoch cancels evidence only, keeping current CPU result");
        check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"epoch cancellation releases reserved captured pixels");
        reset();source=new Source(w,h,8);source.throwAt=2;source.failure=new IllegalStateException("source failure sentinel");RuntimeException actual=null;
        try{GpuAnalysis1961.regions1982(source,w,h,ev,regionCpu(w,h,ev,0),new int[1]);}catch(RuntimeException error){actual=error;}
        check(actual==source.failure&&source.reads==2,"source exception propagated unchanged without retrying source");
        check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"source exception closes optional allocation");
        reset();source=new Source(w,h,8);final Source owned=source;
        GpuAnalysis1961.RegionsCpu unusual=new GpuAnalysis1961.RegionsCpu(){public float[] compute(StrongNoise1958.Patches p){int[] a=new int[16];p.read(a,0,0,4,4);return new float[]{Float.intBitsToFloat(a[0])};}};
        float[] unexpected=unusual.compute(source.copy());output=GpuAnalysis1961.regions1982(owned,w,h,ev,unusual,new int[1]);
        check(same(unexpected,output)&&source.reads==1,"unrecognized optional traversal leaves CPU callback behavior unchanged");
        check(GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"unrecognized traversal cannot schedule partial proof");
        for(int extra=0;extra<2;extra++){
            reset();Source shaped=new Source(w,h,44);final int kind=extra;
            GpuAnalysis1961.RegionsCpu oddResult=new GpuAnalysis1961.RegionsCpu(){public float[] compute(StrongNoise1958.Patches p){float[] values=StrongNoise1958.estimateRegionsSnapshot1961(p,129,131,evidence());return kind==0?null:Arrays.copyOf(values,values.length+1024);}};
            float[] kept=GpuAnalysis1961.regions1982(shaped,w,h,ev,oddResult,new int[1]);
            check(kind==0?kept==null:kept.length==regionCount(w,h)*3+1024,"custom CPU result preserved outside fixed evidence shape");
            check(shaped.reads==regionCount(w,h)&&GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"unaccounted reference closure is not retained for proof");
        }
    }
    static void qualifiedFallback(){int w=129,h=131;float[] ev=evidence();
        for(int family=0;family<2;family++){reset();Source source=new Source(w,h,23);String key=family==0?spatialKey(w,h):regionKey(w,h);
            GpuQualification1961.records.put(key,new GpuQualification1961.Record(1000000000L,1,0));
            if(family==0){SpatialNoise1934 expected=SpatialNoise1934.probeCpu1961(source.copy(),w,h);SpatialNoise1934 actual=GpuAnalysis1961.spatial(source,w,h);
                check(SpatialNoise1934.same1961(expected,actual),"completed GPU gather reused for exact spatial CPU fallback");check(source.reads==spatialCount(w,h),"qualified failed spatial GPU source read once");
            }else{float[] expected=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),w,h,ev);int[] selected={9};float[] actual=GpuAnalysis1961.regions1982(source,w,h,ev,regionCpu(w,h,ev,0),selected);
                check(same(expected,actual)&&selected[0]==0,"completed GPU gather reused for exact regional CPU fallback");check(source.reads==regionCount(w,h),"qualified failed regional GPU source read once");}
            check(GpuNoise1960.executions==1&&GpuQualification1961.pending==null&&GpuQualification1961.retained==0,"failed execution neither repeats GPU nor queues extra image");
            check(GpuQualification1961.exactFailures==0&&GpuQualification1961.speedFailures==1,"transport failure is temporary speed failure only");
        }
        reset();Source source=new Source(w,h,23);float[] expected=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),w,h,ev);int[] selected={0};
        GpuQualification1961.records.put(regionKey(w,h),new GpuQualification1961.Record(1000000000L,1,0));GpuNoise1960.result=bits(expected);
        float[] actual=GpuAnalysis1961.regions1982(source,w,h,ev,regionCpu(w,h,ev,0),selected);
        check(same(expected,actual)&&selected[0]==1&&source.reads==regionCount(w,h),"existing qualified GPU still publishes its exact result");
    }
    static void detachedProofs()throws Exception{int w=65,h=67;float[] ev=evidence();
        for(int mode=0;mode<4;mode++){reset();Source source=new Source(w,h,13);float[] expected=StrongNoise1958.estimateRegionsSnapshot1961(source.copy(),w,h,ev);
            // Both CPU calls incur a controlled cost between patch reads. The
            // GPU peer returns controlled readback bits; setup stays separate.
            GpuAnalysis1961.regions1982(source,w,h,ev,regionCpu(w,h,ev,30000000L),new int[1]);
            check(source.reads==regionCount(w,h)&&GpuQualification1961.pending!=null,"detached proof receives first-traversal snapshot");Arrays.fill(source.pixels,0);
            GpuNoise1960.result=bits(expected);if(mode==1)GpuNoise1960.result[0][0]^=1;if(mode==2)GpuNoise1960.executionDelay=80000000L;
            GpuQualification1961.Cancellation cancellation=new GpuQualification1961.Cancellation();if(mode==3)cancellation.stopped=true;
            GpuQualification1961.Probe pending=GpuQualification1961.pending;
            pending.run(cancellation);
            if(mode==0){check(GpuQualification1961.qualified==1&&GpuNoise1960.executions==2,"two complete exact trials survive caller source mutation");check(GpuQualification1961.records.get(regionKey(w,h)).gpuNanos>0,"transfer-inclusive candidate cost recorded");}
            if(mode==1)check(GpuQualification1961.exactFailures==1&&GpuQualification1961.qualified==0,"single float-bit mismatch rejects candidate");
            if(mode==2)check(GpuQualification1961.speedFailures==1&&GpuQualification1961.qualified==0,"slow full transport fails unchanged five-percent gate");
            if(mode==3)check(GpuNoise1960.executions==0&&GpuQualification1961.qualified+GpuQualification1961.exactFailures+GpuQualification1961.speedFailures==0,"cancelled proof neither runs GPU nor spends retry");
            check(source.reads==regionCount(w,h),"background proof never reads caller source");GpuQualification1961.clear();check(GpuQualification1961.retained==0,"proof completion releases retained snapshot");
        }
    }
    public static void main(String[] args)throws Exception{
        firstRead(args.length>0);if(args.length>0)return;denials();cancellation();qualifiedFallback();detachedProofs();GpuQualification1961.clear();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"analysis87_single_source_traversal_verified\":true,\"analysis87_actual_cpu_floatbits_exact\":true,\"analysis87_reservation_fault_cancel_ownership\":true,\"analysis87_same_snapshot_transport_fallback\":true,\"analysis87_exact2_transfer_time5_preserved\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
