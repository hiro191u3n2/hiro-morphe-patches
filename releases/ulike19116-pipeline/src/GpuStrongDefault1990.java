package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Fixed production Strong route with versioned, device-local preflight.
 * This is finite corpus evidence for the tested implementation, not a claim
 * that a photograph was compared, or a timing prediction for an unseen size.
 * The CPU protection policy is uploaded unchanged; no mask inference occurs.
 * The old per-shape exact rejection remains authoritative for this program.
 */
public final class GpuStrongDefault1990 {
    private GpuStrongDefault1990() {}
    static final int PROFILE=4,VARIANT=0;
    static final String SUITE="strong1990-direct-ieee39-aligned-confidence-corpus68-v2";
    static final int CASES=68;
    private static volatile String status="起動時の画質検証待ち";
    private static final float[] SIGMA={0f,.39f,.40f,.59f,.60f,1.2f,3.7f,12f,24f};

    /** The same bounded integer geometry accepted by the CPU implementation.
     * This does not certify a new per-photo shape or relax a pixel comparison. */
    static boolean owns(int[] u) {
        if(u==null||u.length<20||u[10]!=3||u[0]<1||u[0]>8192||u[1]<1||u[7]<1||u[7]>8192||
                u[8]<1||u[8]>4||(u[9]!=0&&u[9]!=1)||(u[11]!=0&&u[11]!=1)||(u[12]!=0&&u[12]!=1)||
                u[4]<0||u[5]>u[1]||u[4]>=u[5]||u[2]<u[4]||u[3]>u[5]||u[3]<=u[2]||
                u[3]-u[2]>256||(long)u[0]*u[1]>Integer.MAX_VALUE||(long)u[6]+u[4]<0||
                (long)u[6]+u[5]>u[7])return false;
        int first=u[6]+u[2],last=u[6]+u[3];
        // Production native CPU and Java agree on this sparse-confidence
        // layout. Special misaligned partial cells retain the old CPU path;
        // a Java-only edge oracle must not certify a different native result.
        if(u[12]!=0&&((first&3)!=0||((u[3]-u[2])&3)!=0&&last!=u[7]))return false;
        return !(first>0&&u[2]-u[4]<Math.min(StrongNoise1958.HALO,first)||
                last<u[7]&&u[5]-u[3]<Math.min(StrongNoise1958.HALO,u[7]-last));
    }
    public static boolean ready() {
        return GpuCertificates1990.restore(SUITE)!=null;
    }
    static boolean accepts(int[] u) {
        return owns(u)&&ready()&&!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,PROFILE));
    }
    /** A stage snapshots this scalar once. The candidate never participates in
     * first-shot readiness and never borrows profile4's exact certificate. */
    static int profile1994(){return Adaptive1994.faster()?7:Adaptive1998.faster()?6:PROFILE;}
    static int profile1994(int width,int height){
        return adaptiveFaster19115(7,width,height)?7:adaptiveFaster19115(6,width,height)?6:PROFILE;
    }
    private static boolean testedShape19115(int width,int height){return width==3060&&height==4080||width==4080&&height==3060;}
    private static boolean adaptiveFaster19115(int profile,int width,int height){
        if(testedShape19115(width,height))return profile==7?Adaptive1994.faster():Adaptive1998.faster();
        String exact=profile==7?Adaptive1994.EXACT:Adaptive1998.EXACT;
        String fast=profile==7?Adaptive1994.FAST:Adaptive1998.FAST;
        return ready()&&GpuNoise1960.prepared1994(GpuStrong1960.program1973(profile,0))&&
            GpuCertificates1990.restore(exact)!=null&&GpuCertificates1990.restore(fast)!=null;
    }
    // Optional linkage preserves the independently compiled standard fixtures.
    // Foreground queries remain scalar; the helper's faster() never compiles.
    private static boolean regionalState19115(String method){
        try {java.lang.reflect.Method call=Class.forName("com.hiro.ulike.GpuRegionalDefault19115").getDeclaredMethod(method);
            call.setAccessible(true);return Boolean.TRUE.equals(call.invoke(null));}
        catch(Throwable unavailable){return false;}
    }
    static boolean regionalFaster19115(){return regionalState19115("faster");}
    private static String fastKey19115(String key){return regionalFaster19115()?key+":regional115":key;}
    private static void requestRegional19115(){
        try {java.lang.reflect.Method call=Class.forName("com.hiro.ulike.GpuRegionalDefault19115").getDeclaredMethod("request");
            call.setAccessible(true);call.invoke(null);}catch(Throwable unavailable){}
    }
    private static boolean regionalAccepts19115(int[] u){
        try {java.lang.reflect.Method call=Class.forName("com.hiro.ulike.GpuRegionalDefault19115").getDeclaredMethod("accepts",int[].class);
            call.setAccessible(true);return Boolean.TRUE.equals(call.invoke(null,(Object)u));}
        catch(Throwable unavailable){return false;}
    }
    static boolean accepts1994(int[] u,int profile){
        if(!accepts(u))return false;
        // Stage selection is a snapshot. Recheck the independent current proof
        // after the new path is marked and again before output is committed.
        if(u.length>23&&u[23]!=0&&(u[23]!=1||profile!=PROFILE||!regionalAccepts19115(u)))return false;
        return profile==PROFILE||profile==7&&adaptiveFaster19115(7,u[0],u[7])&&
            !GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,7))||
            profile==6&&adaptiveFaster19115(6,u[0],u[7])&&!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,6));
    }
    static boolean adaptivePending19115(){return Adaptive1994.pending19115()||Adaptive1998.pending19115();}
    static boolean regionalHeavyAllowed19115(){return Adaptive1998.completed19115()&&Adaptive1994.completed19115();}
    static void request1994(){
        requestRegional19115();
        if(regionalState19115("pending"))return;
        // Honor an independently certified tile route, including process-local
        // program preparation. Never queue two heavy Strong candidates.
        if(Adaptive1994.cached()){Adaptive1994.request();return;}
        if(Adaptive1998.cached()){Adaptive1998.request();return;}
        if(Adaptive1998.tileEligible())Adaptive1994.request();
        else Adaptive1998.request();
    }
    public static String summary(){return status;}
    private static void check(GpuQualification1961.Cancellation cancellation) {
        if(Thread.currentThread().isInterrupted()||cancellation.cancelled())throw new CancellationException("Strong startup cancelled");
    }
    /** Called only by the startup worker, before first-shot readiness is set. */
    public static boolean qualify(GpuQualification1961.Cancellation cancellation) {
        if(cancellation==null)return false;
        check(cancellation);
        if(ready()){status="端末・処理版が一致する起動時検証記録あり";return true;}
        if(GpuCertificates1990.rejected(SUITE)){status="起動時検証の画質不一致記録あり";return false;}
        if(!GpuNoise1960.supports(GpuStrong1960.program1973(PROFILE,VARIANT))){status="対象GPU処理を利用できません";return false;}
        long cpu=0,gpu=0,pixels=0;int cases=0;
        try {
            for(int id=0;id<CASES;id++) {
                check(cancellation);status="起動時の画質検証 "+id+"/"+CASES;
                Fixture fixture=fixture(id);
                int[] expected=new int[fixture.source.length];
                int confidenceCount=fixture.u[12]==0?0:((fixture.u[0]+3)/4)*((fixture.u[3]-fixture.u[2]+3)/4);
                int[] expectedConfidence=confidenceCount==0?null:new int[confidenceCount];
                long start=System.nanoTime();
                boolean ok=StrongNoise1958.gpuOracleSnapshot1961(fixture.source,expected,fixture.u[0],fixture.u[1],fixture.u[2],fixture.u[3],
                    fixture.u[4],fixture.u[5],fixture.u[6],fixture.u[8],fixture.u[9]!=0,fixture.model,fixture.policy,expectedConfidence);
                cpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);
                if(!ok){status="起動時検証のCPU比較結果を取得できません";return false;}
                start=System.nanoTime();
                GpuStrong1960.Read1971 actual=GpuStrong1960.ephemeral1973(fixture.source,StrongNoise1958.gpuEvidence1960(fixture.model),
                    StrongNoise1958.gpuMaps1960(fixture.model),fixture.policy,fixture.u,null,VARIANT,PROFILE);
                gpu+=Math.max(1L,System.nanoTime()-start);check(cancellation);
                if(actual==null||actual.failure!=null||actual.result==null){status="起動時GPU検証を完了できません: "+(actual==null?"no_result":actual.failure);return false;}
                int count=fixture.u[0]*(fixture.u[3]-fixture.u[2]),offset=fixture.u[0]*fixture.u[2];
                String mismatch=difference(actual.result.pixels,actual.result.confidence,expected,expectedConfidence,offset,count);
                if(mismatch!=null){
                    String detail="case="+id+" "+mismatch;status="起動時GPU画質不一致: "+detail;
                    GpuCertificates1990.reject(SUITE,detail);
                    try{CameraTrace1965.event("strong_preflight_mismatch1990",0,detail);}catch(Throwable optional){}
                    return false;
                }
                cases++;pixels+=count;
            }
            check(cancellation);
            boolean saved=GpuCertificates1990.passed(SUITE,cases,pixels,cpu,gpu);
            status=saved?"起動時検証"+CASES+"例・全画素と信頼度が一致（撮影画像の比較ではありません）":"起動時検証記録を保存できません";
            return saved;
        }catch(CancellationException cancelled){status="起動時検証中断";throw cancelled;}
        catch(RuntimeException unavailable){status="起動時検証を完了できません";return false;}
        catch(LinkageError unavailable){status="起動時検証の処理を利用できません";return false;}
        catch(OutOfMemoryError unavailable){status="起動時検証の作業メモリ不足";return false;}
    }
    static String difference(int[] actual,int[] actualConfidence,int[] expected,int[] expectedConfidence,int offset,int count) {
        return difference19112(actual,actualConfidence,expected,expectedConfidence,offset,count,null);
    }
    private static void checkOptional19112(GpuQualification1961.Cancellation cancellation){if(cancellation!=null)check(cancellation);}
    /** Diagnostics cannot change proof execution, comparison or admission. */
    private static void progress19112(String phase,int completed,int total){
        try{GpuQualification1961.class.getDeclaredMethod("progress1984",String.class,Integer.TYPE,Integer.TYPE)
            .invoke(null,phase,Integer.valueOf(completed),Integer.valueOf(total));}catch(Throwable optional){}
    }
    static String difference19112(int[] actual,int[] actualConfidence,int[] expected,int[] expectedConfidence,int offset,int count,GpuQualification1961.Cancellation cancellation) {
        checkOptional19112(cancellation);
        if(actual==null||actual.length!=count)return "output_shape";
        for(int i=0;i<count;i++){if((i&4095)==0)checkOptional19112(cancellation);if(actual[i]!=expected[offset+i])return "argb index="+i+" cpu="+Integer.toHexString(expected[offset+i])+" gpu="+Integer.toHexString(actual[i]);}
        if(actualConfidence!=expectedConfidence){
            if(actualConfidence==null||expectedConfidence==null||actualConfidence.length!=expectedConfidence.length)return "confidence_shape";
            for(int i=0;i<actualConfidence.length;i++){if((i&4095)==0)checkOptional19112(cancellation);if(actualConfidence[i]!=expectedConfidence[i])return "confidence index="+i+" cpu="+expectedConfidence[i]+" gpu="+actualConfidence[i];}
        }
        checkOptional19112(cancellation);return null;
    }
    static final class Fixture {
        final int[] source,policy,u;final StrongNoise1958.Model model;
        Fixture(int[] source,int[] policy,int[] u,StrongNoise1958.Model model){this.source=source;this.policy=policy;this.u=u;this.model=model;}
    }
    /** Synthetic pixels never originate from or retain a user photograph. */
    static Fixture fixture(int id) {return fixture19112(id,null);}
    /** Optional proofs check their capture token while generating the same corpus. */
    static Fixture fixture19112(int id,GpuQualification1961.Cancellation cancellation) {
        checkOptional19112(cancellation);
        if(id<0||id>=CASES)throw new IllegalArgumentException("strong corpus case");
        boolean wide=id>=64&&id<66,partial=id==66,duplicate=id==67;
        int width=partial||duplicate?65:wide?(id==64?3060:4080):new int[]{17,31,32,33,63,65,97,129}[(id>>>3)&7];
        int rows=duplicate?5:partial?65:wide?68:72+(id&7),origin=partial||duplicate?0:wide?1374:(id%3==0?0:64+(id&7)),height=wide?(id==64?4080:3060):origin+rows;
        int begin=duplicate?1:partial?36:wide?18:(id%3==0?0:18),end=duplicate?5:partial?37:wide?50:(id%3==0?rows:rows-18);
        int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=end;u[4]=0;u[5]=rows;u[6]=origin;u[7]=height;
        u[8]=id>=64?4:1+(id&3);u[9]=id>=64?1:(id>>>2)&1;u[10]=3;u[11]=id>=64?1:(id>>>3)&1;u[12]=id>=64?1:(id>>>4)&1;
        if(u[12]!=0&&(((origin+begin)&3)!=0||((end-begin)&3)!=0&&origin+end!=height))u[12]=0;
        int[] source=new int[width*rows];long random=0x5eed1990L+id;
        for(int y=0;y<rows;y++)for(int x=0;x<width;x++){
            if(x==0)checkOptional19112(cancellation);
            random=(random*1664525L+1013904223L)&0xffffffffL;int jitter=((int)(random>>>24)&15)-8;
            int mode=(id>>>2)%6,base=mode==0?16:mode==1?128:mode==2?240:mode==3?(x<width/2?32:224):mode==4?((x*17+y*31)&255):(int)(random>>>24);
            int r=clamp(base+jitter),g=clamp(base+(mode==5?37:0)-jitter),b=clamp(base+(mode==4?71:0)+jitter/2);
            int alpha=((id&7)==7&&(x+y)%11==0)?((x+y)&255):255;
            source[y*width+x]=(alpha<<24)|(r<<16)|(g<<8)|b;
        }
        // Exercise these special partial shapes for ARGB only. Their sparse
        // confidence convention is outside the production domain above.
        if(duplicate)Arrays.fill(source,0xff707070);
        checkOptional19112(cancellation);
        int count=width*(end-begin);int[] policy=u[11]==0?null:new int[count*2];
        if(policy!=null)for(int i=0;i<count;i++){if((i&4095)==0)checkOptional19112(cancellation);policy[i*2]=new int[]{0,1,64,127,128,191,255,256}[(i+id)&7];policy[i*2+1]=((i*37+id*17)%257);}
        if(duplicate)for(int i=0;i<count;i++){policy[i*2]=i/width==end-begin-1?0:256;policy[i*2+1]=0;}
        int[][] maps=new int[3][];
        for(int scale=0;scale<3;scale++){
            checkOptional19112(cancellation);
            int divisor=1<<(scale+1),mw=(width+divisor-1)/divisor,mh=(height+divisor-1)/divisor;maps[scale]=new int[mw*mh];
            for(int i=0;i<maps[scale].length;i++){
                if((i&4095)==0)checkOptional19112(cancellation);
                int guide=(i*7+id*13)&255,dr=((i+id)%15)-7,dg=((i*3+id)%15)-7,db=((i*7+id)%15)-7;
                maps[scale][i]=(guide<<24)|((dr&255)<<16)|((dg&255)<<8)|(db&255);
            }
        }
        float[] evidence=new float[64];for(int i=0;i<64;i++)evidence[i]=SIGMA[(i+id)%SIGMA.length];
        int regions=((width+63)/64)*((height+63)/64);float[] runtime=new float[16+regions*3];System.arraycopy(evidence,0,runtime,0,16);
        for(int i=0;i<regions;i++){runtime[16+i*3]=SIGMA[(id+i)%SIGMA.length];runtime[17+i*3]=SIGMA[(id+i+3)%SIGMA.length];runtime[18+i*3]=(i%5)*.25f;}
        if(duplicate)for(int i=0;i<regions;i++){runtime[16+i*3]=12f;runtime[17+i*3]=12f;runtime[18+i*3]=0;}
        checkOptional19112(cancellation);
        return new Fixture(source,policy,u,StrongNoise1958.model1961(width,height,maps[0],maps[1],maps[2],evidence,runtime));
    }
    private static int clamp(int value){return Math.max(0,Math.min(255,value));}

    /** .98: the existing stripe/power-of-two program is independently proven.
     * This candidate is benchmarked first. A completed unsuccessful probe may
     * enable the tile candidate only after a later capture. A proven fast6 is
     * not replaced by a new7 proof against4: that would not compare6 with7.
     * An existing tile certificate remains usable and is prepared separately.
     */
    static final class Adaptive1998 {
        static final String EXACT="strong1998-stripe-pow2-68cases-256rows-exact-v1";
        static final String FAST=EXACT+":both-widths-two-trials-10pct";
        private static final String JOB="strong-standard-stripe1998-v1";
        private static boolean requested,attempted;
        static synchronized boolean pending19115(){return requested;}
        static boolean completed19115(){
            synchronized(Adaptive1998.class){if(requested)return false;if(attempted)return true;}
            return GpuCertificates1990.rejected(EXACT);
        }
        private static long completedEpoch=Long.MAX_VALUE;
        static synchronized boolean completedBeforeCapture(){
            return attempted&&!requested&&ProcessingTiming1947.captureEpoch1953()>completedEpoch;
        }
        static boolean tileEligible(){
            // A restored sticky rejection has no active new heavy stripe trial.
            // A rejection discovered now still waits for the next capture.
            synchronized(Adaptive1998.class){
                if(requested)return false;
                if(attempted)return ProcessingTiming1947.captureEpoch1953()>completedEpoch;
            }
            return GpuCertificates1990.rejected(EXACT);
        }
        static boolean cached(){return GpuCertificates1990.restore(EXACT)!=null&&GpuCertificates1990.restore(fastKey19115(FAST))!=null;}
        static boolean faster(){return ready()&&GpuNoise1960.prepared1994(GpuStrong1960.program1973(6,0))&&cached();}
        static boolean qualify(GpuQualification1961.Cancellation cancellation){return Adaptive1994.qualify1998(6,cancellation,true);}
        static void request(){
            boolean cached=cached();
            if(!ready()||GpuCertificates1990.rejected(EXACT)||faster()||
                    !cached&&!GpuQualification1961.optionalBenchmarksAllowed1995())return;
            synchronized(Adaptive1998.class){if(requested||attempted)return;requested=true;}
            boolean transferred=false;
            try{transferred=GpuQualification1961.schedule(JOB,512,new GpuQualification1961.Probe(){
                private boolean heavy;
                GpuQualification1961.Probe configure(boolean value){heavy=value;return this;}
                public void run(GpuQualification1961.Cancellation cancellation){
                    boolean completed=false;
                    try{Adaptive1994.qualify1998(6,cancellation,heavy);completed=true;}
                    finally{if(completed&&!cancellation.cancelled()&&!Thread.currentThread().isInterrupted())
                        synchronized(Adaptive1998.class){attempted=true;completedEpoch=ProcessingTiming1947.captureEpoch1953();}}
                }
                public void close(){synchronized(Adaptive1998.class){requested=false;}}
            }.configure(!cached));}catch(RuntimeException unavailable){}
            catch(LinkageError unavailable){}
            catch(OutOfMemoryError unavailable){}
            finally{if(!transferred)synchronized(Adaptive1998.class){requested=false;}}
        }
    }

    /** Optional synthetic work is owned by the common idle queue. Capture
     * arrival cancels it, closes its GPU session and leaves the standard ready
     * flag intact. No photograph is retained or used to fabricate a proof. */
    static final class Adaptive1994 {
        static final String EXACT="strong1994-tile8-pow2-68cases-256rows-exact-v1";
        static final String FAST=EXACT+":both-widths-two-trials-10pct";
        private static final String JOB="strong-standard-layout1994-v1";
        private static boolean requested,attempted;
        static synchronized boolean pending19115(){return requested;}
        static boolean completed19115(){
            synchronized(Adaptive1994.class){if(requested)return false;if(attempted)return true;}
            return GpuCertificates1990.rejected(EXACT);
        }
        static boolean cached(){return GpuCertificates1990.restore(EXACT)!=null&&GpuCertificates1990.restore(fastKey19115(FAST))!=null;}
        static boolean faster(){return ready()&&GpuNoise1960.prepared1994(GpuStrong1960.program1973(7,0))&&cached();}
        static void request(){
            boolean cached=GpuCertificates1990.restore(EXACT)!=null&&GpuCertificates1990.restore(fastKey19115(FAST))!=null;
            if(!ready()||GpuCertificates1990.rejected(EXACT)||faster()||
                    !cached&&!GpuQualification1961.optionalBenchmarksAllowed1995())return;
            // Queue retirement closes probes under its own lock. Never enter
            // that queue while holding Adaptive1994.class (close takes it).
            synchronized(Adaptive1994.class){if(requested||attempted)return;requested=true;}
            boolean transferred=false;
            try{transferred=GpuQualification1961.schedule(JOB,512,new GpuQualification1961.Probe(){
                private boolean heavy1995;
                GpuQualification1961.Probe configure1995(boolean heavy){heavy1995=heavy;return this;}
                public void run(GpuQualification1961.Cancellation cancellation){
                    boolean completed=false;
                    try{qualify1995(cancellation,heavy1995);completed=true;}
                    finally{if(completed&&!cancellation.cancelled()&&!Thread.currentThread().isInterrupted())
                        synchronized(Adaptive1994.class){attempted=true;}}
                }
                public void close(){synchronized(Adaptive1994.class){requested=false;}}
            }.configure1995(!cached));}catch(RuntimeException unavailable){}
            catch(LinkageError unavailable){}
            catch(OutOfMemoryError unavailable){}
            finally{if(!transferred)synchronized(Adaptive1994.class){requested=false;}}
        }
        static boolean measuredWin(long baselineBest,long candidateWorst){
            return baselineBest>0&&candidateWorst>0&&baselineBest!=Long.MAX_VALUE&&
                candidateWorst<=baselineBest-baselineBest/10;
        }
        private static Read timed(Fixture f,int profile,GpuQualification1961.Cancellation cancellation){
            check(cancellation);
            if(profile==PROFILE&&regionalFaster19115())return regionalBaseline19115(f,cancellation);
            long start=System.nanoTime();
            GpuStrong1960.Read1971 actual=GpuStrong1960.ephemeral1973(f.source,StrongNoise1958.gpuEvidence1960(f.model),
                StrongNoise1958.gpuMaps1960(f.model),f.policy,f.u,null,0,profile);
            long elapsed=System.nanoTime()-start;check(cancellation);
            if(actual==null||actual.failure!=null||actual.result==null){
                GpuQualification1961.outcome1984(actual!=null&&"memory_budget".equals(actual.failure)?"memory_budget":"transport_failure");return null;
            }
            return new Read(actual.result,elapsed);
        }
        private static Read regionalBaseline19115(Fixture f,GpuQualification1961.Cancellation cancellation){
            try {
                java.lang.reflect.Method call=Class.forName("com.hiro.ulike.GpuRegionalDefault19115")
                    .getDeclaredMethod("timedBaseline19115",Fixture.class,GpuQualification1961.Cancellation.class);
                call.setAccessible(true);Object[] measured=(Object[])call.invoke(null,f,cancellation);
                if(measured==null)return null;
                if(measured.length!=2||!(measured[0] instanceof GpuStrong1960.Result)||!(measured[1] instanceof Long))
                    throw new IllegalStateException("regional baseline result");
                return new Read((GpuStrong1960.Result)measured[0],((Long)measured[1]).longValue());
            } catch(java.lang.reflect.InvocationTargetException failed){
                Throwable cause=failed.getCause();if(cause instanceof RuntimeException)throw (RuntimeException)cause;
                if(cause instanceof Error)throw (Error)cause;
                GpuQualification1961.outcome1984("transport_failure");return null;
            } catch(ReflectiveOperationException unavailable){GpuQualification1961.outcome1984("transport_failure");return null;}
        }
        private static final class Read {final GpuStrong1960.Result result;final long nanos;
            Read(GpuStrong1960.Result result,long nanos){this.result=result;this.nanos=nanos;}}
        /** Preserve the published private DEX method for strict ABI retention. */
        private static String compare(Fixture f,Read actual,int[] expected,int[] confidence){
            return compare(f,actual,expected,confidence,null);
        }
        private static String compare(Fixture f,Read actual,int[] expected,int[] confidence,GpuQualification1961.Cancellation cancellation){
            return actual==null?"output unavailable":difference19112(actual.result.pixels,actual.result.confidence,expected,confidence,
                f.u[0]*f.u[2],f.u[0]*(f.u[3]-f.u[2]),cancellation);
        }
        private static int[] confidence(Fixture f){return f.u[12]==0?null:new int[((f.u[0]+3)/4)*((f.u[3]-f.u[2]+3)/4)];}
        private static void reject(int profile,int id,String mismatch){
            GpuCertificates1990.reject(profile==PROFILE?SUITE:exact1998(profile),"layout="+profile+" case="+id+" "+mismatch);
            if(profile==PROFILE)GpuStartup1990.invalidateDomain();
            GpuQualification1961.outcome1984("quality_mismatch");
        }
        private static boolean oracle(Fixture f,int[] expected,int[] confidence){
            return StrongNoise1958.gpuOracleSnapshot1961(f.source,expected,f.u[0],f.u[1],f.u[2],f.u[3],f.u[4],f.u[5],
                f.u[6],f.u[8],f.u[9]!=0,f.model,f.policy,confidence);
        }
        static boolean qualify(GpuQualification1961.Cancellation cancellation){return qualify1995(cancellation,true);}
        private static String exact1998(int candidate){return candidate==6?Adaptive1998.EXACT:EXACT;}
        private static String fast1998(int candidate){return fastKey19115(candidate==6?Adaptive1998.FAST:FAST);}
        private static boolean qualify1995(GpuQualification1961.Cancellation cancellation,boolean heavy){
            return qualify1998(7,cancellation,heavy);
        }
        static boolean qualify1998(int candidate,GpuQualification1961.Cancellation cancellation,boolean heavy){
            if(candidate!=6&&candidate!=7)throw new IllegalArgumentException("Strong candidate profile");
            final String exact=exact1998(candidate),fast=fast1998(candidate);
            if(cancellation==null)return false;check(cancellation);
            if(!ready()){GpuQualification1961.outcome1984("baseline_unavailable");return false;}
            if(GpuCertificates1990.rejected(exact)){GpuQualification1961.outcome1984("quality_rejected");return false;}
            if(!GpuNoise1960.supports(GpuStrong1960.program1973(candidate,0))){GpuQualification1961.outcome1984("unsupported");return false;}
            if(GpuCertificates1990.restore(exact)!=null&&GpuCertificates1990.restore(fast)!=null){
                boolean selected=candidate==6?Adaptive1998.faster():faster();GpuQualification1961.outcome1984(selected?"cached_preparation":"execution_unavailable");return selected;}
            // EXACT without FAST is incomplete speed admission, not a proven
            // slow candidate. Re-run the unchanged full corpus after saving.
            // A cached-prepare job never silently becomes a heavy benchmark.
            if(!heavy||!GpuQualification1961.optionalBenchmarksAllowed1995())
                throw new CancellationException("optional Strong benchmark deferred until publication");
            long cpu=0,gpu=0,pixels=0;int cases=0;boolean speed=true;
            try{
                for(int id=0;id<CASES+2;id++){
                    check(cancellation);progress19112("strong_cpu_reference",id,CASES+2);
                    Fixture f=id<CASES?fixture19112(id,cancellation):benchmark19112(id-CASES+64,cancellation);
                    check(cancellation);
                    int[] expected=new int[f.source.length],confidence=confidence(f);
                    check(cancellation);long start=System.nanoTime();boolean computed=oracle(f,expected,confidence);cpu+=Math.max(1,System.nanoTime()-start);
                    check(cancellation);if(!computed){GpuQualification1961.outcome1984("baseline_unavailable");return false;}
                    progress19112("strong_profile",id,CASES+2);
                    Read actual=timed(f,candidate,cancellation);if(actual==null)return false;
                    String mismatch=compare(f,actual,expected,confidence,cancellation);
                    if(mismatch!=null){reject(candidate,id,mismatch);return false;}
                    gpu+=Math.max(1,actual.nanos);cases++;pixels+=(long)f.u[0]*(f.u[3]-f.u[2]);
                    if(id>=CASES){
                        // Warm BOTH complete upload/dispatch/readback routes.
                        // Their timings include model transfer and allocation;
                        // no GPU kernel-only figure is used as a save forecast.
                        progress19112("strong_speed_compare",id-CASES,2);
                        Read baseline=timed(f,PROFILE,cancellation);if(baseline==null)return false;
                        mismatch=compare(f,baseline,expected,confidence,cancellation);if(mismatch!=null){reject(PROFILE,id,mismatch);return false;}
                        long best=Long.MAX_VALUE,worst=0;
                        for(int trial=0;trial<2;trial++)for(int order=0;order<2;order++){
                            int profile=(order+trial)%2==0?PROFILE:candidate;Read measured=timed(f,profile,cancellation);if(measured==null)return false;
                            mismatch=compare(f,measured,expected,confidence,cancellation);if(mismatch!=null){reject(profile,id,mismatch);return false;}
                            if(profile==PROFILE)best=Math.min(best,measured.nanos);else worst=Math.max(worst,measured.nanos);
                        }
                        speed&=measuredWin(best,worst);
                        try{CameraTrace1965.event("strong_layout1994_speed",0,"profile="+candidate+" width="+f.u[0]+" rows=256 baseline_gpu_ns="+best+" candidate_gpu_ns="+worst);}catch(Throwable optional){}
                    }
                }
                check(cancellation);progress19112("strong_speed_compare",2,2);
                if(!fast.equals(fast1998(candidate))){GpuQualification1961.outcome1984("reference_unstable");return false;}
                // These fields remain actual CPU oracle / candidate GPU corpus
                // times. FAST additionally requires both measured GPU-vs-GPU
                // decisions above; the CPU field is never relabelled GPU time.
                if(!GpuCertificates1990.passed(exact,cases,pixels,cpu,gpu)){GpuQualification1961.outcome1984("comparison_incomplete");return false;}
                check(cancellation);
                if(speed&&!GpuCertificates1990.passed(fast,cases,pixels,cpu,gpu)){GpuQualification1961.outcome1984("comparison_incomplete");return false;}
                GpuQualification1961.outcome1984(speed?"qualified":"speed_condition");
                return speed;
            }catch(CancellationException cancelled){throw cancelled;}
            catch(RuntimeException unavailable){GpuQualification1961.exception1993(unavailable);GpuQualification1961.outcome1984("exception");return false;}
            catch(LinkageError unavailable){GpuQualification1961.outcome1984("unsupported");return false;}
            catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");return false;}
        }
        static Fixture benchmark(int id){return benchmark19112(id,null);}
        static Fixture benchmark19112(int id,GpuQualification1961.Cancellation cancellation){
            checkOptional19112(cancellation);
            if(id!=64&&id!=65)throw new IllegalArgumentException("strong layout benchmark width");
            Fixture seed=fixture19112(id,cancellation);checkOptional19112(cancellation);int width=seed.u[0];int[] u=seed.u.clone();u[1]=292;u[2]=18;u[3]=274;u[5]=292;
            int[] source=new int[width*u[1]],policy=new int[width*256*2];
            for(int y=0;y<u[1];y++){
                checkOptional19112(cancellation);
                if((y&15)==0&&Thread.currentThread().isInterrupted())throw new CancellationException("strong layout fixture interrupted");
                System.arraycopy(seed.source,(y%seed.u[1])*width,source,y*width,width);
            }
            for(int y=0;y<256;y++){checkOptional19112(cancellation);System.arraycopy(seed.policy,(y%(seed.u[3]-seed.u[2]))*width*2,policy,y*width*2,width*2);}
            checkOptional19112(cancellation);return new Fixture(source,policy,u,seed.model);
        }
    }
}
