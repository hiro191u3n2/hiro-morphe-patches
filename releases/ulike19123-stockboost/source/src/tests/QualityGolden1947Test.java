package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Random;

/** Golden output oracle and explicit cross-thread shot attribution. */
public final class QualityGolden1947Test {
    static int assertions, scenarios;
    static void check(boolean value,String message) { assertions++;if(!value)throw new AssertionError(message); }
    static String rendered(ProcessingTiming1947.Trace trace)throws Exception {
        Method method=ProcessingTiming1947.class.getDeclaredMethod("render",ProcessingTiming1947.Trace.class);
        method.setAccessible(true);return (String)method.invoke(null,trace);
    }
    static PhotoDetail.Settings settings(boolean noise,boolean sharp) {
        return new PhotoDetail.Settings(noise,4,sharp,4,true,true,true);
    }
    static void put(MessageDigest digest,int value) {
        digest.update((byte)(value>>>24));digest.update((byte)(value>>>16));digest.update((byte)(value>>>8));digest.update((byte)value);
    }
    static String golden(boolean instrumented)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        final int width=97,height=113;
        int[] original=new int[width*height];Random random=new Random(194745L);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int luma=85+(x*3+y)%91+random.nextInt(31)-15;
            int red=Math.min(255,luma+random.nextInt(7));
            int green=Math.max(0,luma-random.nextInt(7));
            int blue=Math.min(255,luma+random.nextInt(5));
            original[y*width+x]=0xff000000|red<<16|green<<8|blue;
        }
        for(boolean noise:new boolean[]{false,true})for(boolean sharp:new boolean[]{false,true})
        for(boolean chroma:new boolean[]{false,true})for(int turn:new int[]{0,90,180,270})
        for(int geometry=0;geometry<4;geometry++) {
            int rw=turn==90||turn==270?height:width,rh=turn==90||turn==270?width:height;
            int ow=geometry==0?rw:geometry==1?rw/2:geometry==2?rw*3/2:rw-13;
            int oh=geometry==0?rh:geometry==1?rh/2:geometry==2?rh*3/2:rh-19;
            PhotoDetail.Settings options=settings(noise,sharp);
            PipelineQuality1942Test.bind(options,chroma,ow,oh);
            Bitmap source=Bitmap.from(width,height,original,Bitmap.Config.ARGB_8888,true);
            ShotContext1932.SHOTS.put(source,PipelineQuality1942Test.metadata(1200));
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(source);
            ProcessingTiming1947.Trace outer=ProcessingTiming1947.begin(new Object());
            ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(outer);
            Bitmap out;
            try {
                out=QualityPipeline1932.normalize(source,turn,false);
                check(out.getWidth()==ow && out.getHeight()==oh,"output dimensions");
                check(ProcessingTiming1947.current()==outer,"outer timing scope restored");
                if(instrumented)check(ProcessingTiming1947.traceFor(out)==trace,"output bitmap retains original shot identity");
                int applies=ChromaPipeline177.APPLIES.get();
                out=QualityPipeline1932.applyDetail(out,source,options);
                check(applies==ChromaPipeline177.APPLIES.get(),"completion avoids duplicate pixel filters");
                put(digest,ow);put(digest,oh);for(int pixel:out.snapshot())put(digest,pixel);
                for(int pixel:source.snapshot())put(digest,pixel);
                check(java.util.Arrays.equals(source.snapshot(),original),"source remains unchanged");
            } finally { ProcessingTiming1947.restore(scope); }
            ProcessingTiming1947.finish(trace,true);ProcessingTiming1947.finish(outer,true);
            if(out!=source)out.recycle();source.recycle();scenarios++;
        }
        StringBuilder text=new StringBuilder();for(byte value:digest.digest())text.append(String.format("%02x",value&255));return text.toString();
    }
    static ChromaPipeline186.State state(ProcessingTiming1947.Trace trace,QualityPixels1932.Plan plan,int[] pixels) {
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(trace);
        HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").set(plan);
        try { return new ChromaPipeline186.State(pixels,77,181,31,3); }
        finally { HostAudit1932.local("CURRENT").remove();ProcessingTiming1947.restore(scope); }
    }
    static void workerAttribution()throws Exception {
        int[] pixels=PipelineQuality1942Test.grain(77,181,24,119,931L);
        QualityPixels1932.Plan plan=QualityPixels1932.plan(QualityPixels1932.estimate(pixels,77,181),800,30000000L,1,0,3,0,false,true,1);
        ProcessingTiming1947.Trace a=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Trace b=ProcessingTiming1947.begin(new Object());
        final ChromaPipeline186.State first=state(a,plan,pixels),second=state(b,plan,pixels);
        // The work thread explicitly holds B while it executes the State for A.
        // Timing must follow State A, then restore B for subsequent unrelated work.
        ProcessingTiming1947.Scope wrong=ProcessingTiming1947.enter(b);
        try {
            QualityPipeline1932.run(first,new ChromaPipeline186.Buffer(first));
            check(!first.failed,"cross-shot worker succeeds");
            check(ProcessingTiming1947.current()==b,"worker restores the prior thread scope");
            String beforeB=rendered(b),afterA=rendered(a);
            check(beforeB.contains("ノイズ除去: 未計測"),"A worker never records noise against B");
            check(!afterA.contains("ノイズ除去: 未計測"),"A worker noise interval is recorded on A");
            check(!afterA.contains("補正: 未計測"),"A worker correction interval is recorded on A");
            QualityPipeline1932.run(second,new ChromaPipeline186.Buffer(second));
            check(!second.failed,"second shot succeeds on reused thread");
            check(!rendered(b).contains("ノイズ除去: 未計測"),"second shot has independent intervals");
        }finally {ProcessingTiming1947.restore(wrong);ProcessingTiming1947.finish(a,true);ProcessingTiming1947.finish(b,true);}
        scenarios++;
    }
    static void unknownFallback()throws Exception {
        int[] pixels=PipelineQuality1942Test.grain(97,113,21,119,932L);
        Bitmap source=Bitmap.from(97,113,pixels,Bitmap.Config.ARGB_8888,true);
        PipelineQuality1942Test.bind(settings(true,true),true,117,143);
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(source);
        Bitmap.failCopyOnce=true;
        Bitmap out=QualityPipeline1932.normalize(source,0,false);
        check(ProcessingTiming1947.traceFor(out)==trace,"fallback output retains trace identity");
        check(rendered(trace).contains("ノイズ除去: 未計測"),"failed quality attempt never gives a complete noise duration");
        check(rendered(trace).contains("補正: 未計測"),"failed quality attempt never gives a complete correction duration");
        check(rendered(trace).contains("従来経路へ切替"),"fallback explicitly disclosed");
        check(java.util.Arrays.equals(source.snapshot(),pixels),"fallback retains original pixels");
        ProcessingTiming1947.finish(trace,true);if(out!=source)out.recycle();source.recycle();scenarios++;
    }
    public static void main(String[] args)throws Exception {
        boolean instrumented=args.length>0 && "instrumented".equals(args[0]);
        String digest=golden(instrumented);
        if(instrumented){workerAttribution();unknownFallback();}
        SpeedWorkers1935.trim();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"output_sha256\":\""+digest+"\",\"physical_device_verified\":false}");
    }
}
