package com.hiro.ulike;

import android.content.Context;
import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

/** Production route bodies with explicit Android/GPU transport fixtures. Every
 * image comparison visits all pixels; native coefficient cases visit raw bits. */
public final class PipelineRoutes1981Test {
    static long assertions,pixels,changed,naiveDifferences,cases,coefficientWords,moireChanged,sharpChanged;
    static void check(boolean okay,String message){assertions++;if(!okay)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static void same(int[] expected,int[] actual,String label){check(expected.length==actual.length,label+" length");for(int i=0;i<expected.length;i++){check(expected[i]==actual[i],label+" pixel "+i);pixels++;}}
    static int byteValue(int n){return Math.max(0,Math.min(255,n));}
    static int[] image(int width,int height,int kind) {
        int[] p=new int[width*height];Random random=new Random(198100L+width*103+height*17+kind);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int gray=kind==0?112:kind==4?40+(x*3+y*5)%160:112;
            int grain=kind==1?random.nextInt(51)-25:kind==4?random.nextInt(9)-4:0;
            int wave=kind==2?(x%4<2?14:-14):kind==3?(int)Math.round(14*Math.sin((x+y*.3)*Math.PI/8)):0;
            int red=byteValue(gray+grain+wave),green=byteValue(gray+grain),blue=byteValue(gray+grain-(int)Math.round(wave*2.62));
            p[y*width+x]=0xff000000|red<<16|green<<8|blue;
        }
        return p;
    }
    static QualityPixels1932.Plan plan(int width,int height,int kind) {
        QualityPixels1932.Plan p=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(2.7f,4.5f,112,.21f,100),
            200,10000000L,2,.8f,3,1+kind%4,true,true,1f).withHaloSuppression((kind&1)==0);
        if(kind>1) {
            float[] noise={.1f,1.1f,4.2f,7.7f,3.3f,.6f};
            p=p.withOutputNoise(SpatialNoise1934.fromGpu1961(width,height,3,2,noise,new QualityPixels1932.NoiseStats(2.1f,3.4f,112,.13f,200)));
        }
        if(kind>0) {
            byte[] skin=new byte[7*5],detail=new byte[skin.length];new Random(81).nextBytes(skin);new Random(19).nextBytes(detail);
            p=p.withFaceRegions(FaceRegions1934.uprightRaster(width,height,7,5,skin,detail,0));
        }
        return p;
    }
    static void initialize(){GpuNoise1960.enabled=true;GpuQualification1961.initialize(new Context());}
    static void force(String key){GpuQualification1961.qualified(key,1000000000L,1L,1);check(CpuExact1978.enabled(key),"test-local positive certificate");}
    static void identity()throws Exception {
        int[][] shapes={{1,1},{3,7},{31,65},{65,31},{97,129},{129,97},{65,257},{129,513},{257,259},{17,1025}};
        for(int[] shape:shapes)for(int kind=0;kind<5;kind++) {
            int width=shape[0],height=shape[1];int[] source=image(width,height,kind),pristine=source.clone();QualityPixels1932.Plan p=plan(width,height,kind);
            Bitmap reference=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);reference.setHasAlpha(false);
            QualityPipeline1932.finishInPlace(reference,p,true,false);int[] intermediate=reference.snapshot();
            QualityPipeline1932.finishInPlace(reference,p,false,true);
            int[] wanted=reference.snapshot();for(int i=0;i<wanted.length;i++)if(wanted[i]!=source[i])changed++;
            for(int i=0;i<wanted.length;i++){if(intermediate[i]!=source[i])moireChanged++;if(intermediate[i]!=wanted[i])sharpChanged++;}
            // A deliberately wrong shortcut omits the source-domain moire pass.
            int[] naive=new int[source.length];QualityPixels1932.finishStripAt(source,naive,width,height,0,height,p,false,true,0);
            for(int i=0;i<wanted.length;i++)if(naive[i]!=wanted[i])naiveDifferences++;
            for(int core:new int[]{7,64,256})for(int workers:new int[]{1,4}) {
                Bitmap actual=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);actual.setHasAlpha(false);actual.setDensity(237);
                CpuIdentityFinish1981.stream(actual,p,p,core,workers);
                same(wanted,actual.snapshot(),"identity "+width+"x"+height+"/"+kind+"/"+core+"/"+workers);
                check(actual.reads==(height+core-1)/core&&actual.writes==(height+core-1)/core,"one input/final output Bitmap boundary per band");
                check(actual.getDensity()==237&&!actual.hasAlpha()&&actual.isPremultiplied(),"bitmap metadata retained");
                same(pristine,source,"caller array remains pristine");actual.recycle();cases++;
            }
            reference.recycle();Bitmap.ALL.clear();GpuQualification1961.captureChanged();
        }
        check(changed>1000&&moireChanged>1000&&sharpChanged>1000,"both active filters exercised");
        check(naiveDifferences>1000,"omitting source moire is observably wrong");
        for(int core:new int[]{1,2,3,4,32,36}) {
            int width=19,height=73;int[] source=image(width,height,3);QualityPixels1932.Plan p=plan(width,height,3);
            Bitmap ref=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true),actual=ref.copy(Bitmap.Config.ARGB_8888,true);
            QualityPipeline1932.finishInPlace(ref,p,true,false);QualityPipeline1932.finishInPlace(ref,p,false,true);
            CpuIdentityFinish1981.stream(actual,p,p,core,4);same(ref.snapshot(),actual.snapshot(),"tiny core preserves both halo layers");ref.recycle();actual.recycle();
        }
        int width=65,height=129;QualityPixels1932.Plan p=plan(width,height,3);int[] source=image(width,height,3);
        Bitmap candidate=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);
        int count=Math.min((height+Math.min(256,height)-1)/Math.min(256,height),Math.min(SpeedWorkers1935.maxWorkers(),SpeedWorkers1935.availableWorkers1944()));
        String key=CpuIdentityFinish1981.key(p,width,height,Math.min(256,height),count,true);force(key);
        check(CpuIdentityFinish1981.tryRun(candidate,p,p),"qualified stream executes through real entry");
        Bitmap reference=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);
        QualityPipeline1932.finishInPlace(reference,p,true,false);QualityPipeline1932.finishInPlace(reference,p,false,true);
        same(reference.snapshot(),candidate.snapshot(),"qualified entry exact");candidate.recycle();reference.recycle();
        int[] alpha=source.clone();alpha[width*height/2]&=0x7fffffff;
        Bitmap transparent=Bitmap.from(width,height,alpha,Bitmap.Config.ARGB_8888,true);
        check(!CpuIdentityFinish1981.tryRun(transparent,p,p),"transparent image retains old premultiplied boundary");
        same(alpha,transparent.snapshot(),"declined alpha remains pristine");check(transparent.writes==0,"decline never writes");transparent.recycle();
        QualityPixels1932.Plan unknown=p.withFaceRegions(new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){throw new AssertionError("unknown callback evaluated");}public int detailQ8(int x,int y){throw new AssertionError("unknown callback evaluated");}});
        Bitmap held=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);
        check(!CpuIdentityFinish1981.tryRun(held,p,unknown)&&held.writes==0,"unknown callbacks never cross worker boundary");
        held.setColorSpaceForTest(new android.graphics.ColorSpace(false));check(!CpuIdentityFinish1981.tryRun(held,p,p),"wide color remains original");held.recycle();
        Bitmap owner=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);Bitmap.failNextCopyWrite=true;
        Bitmap failure=owner.copy(Bitmap.Config.ARGB_8888,true);boolean threw=false;
        try{CpuIdentityFinish1981.tryRun(failure,p,p);}catch(IllegalStateException expected){threw=true;}
        check(threw,"active write failure propagates, never falls through on damaged input");same(source,owner.snapshot(),"pristine owner survives failure");failure.recycle();owner.recycle();
        check(Bitmap.writesAfterRecycle.get()==0,"all workers join before release");
        Bitmap cancelled=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);boolean stopped=false;
        Thread.currentThread().interrupt();
        try{CpuIdentityFinish1981.stream(cancelled,p,p,7,4);}catch(IllegalStateException expected){stopped=true;}
        finally{Thread.interrupted();}
        check(stopped&&cancelled.writes==0,"cancelled stream does not publish a partially processed image");
        same(source,cancelled.snapshot(),"interrupted input stays pristine");cancelled.recycle();
        Class<?> proofClass=Class.forName("com.hiro.ulike.CpuIdentityFinish1981$Proof");
        Constructor<?> proofConstructor=proofClass.getDeclaredConstructor(Bitmap.class,QualityPixels1932.Plan.class,QualityPixels1932.Plan.class,int.class,int.class);
        proofConstructor.setAccessible(true);
        Bitmap privateSource=Bitmap.from(width,height,source,Bitmap.Config.ARGB_8888,true);Bitmap.ALL.clear();
        CpuExact1978.Work proof=(CpuExact1978.Work)proofConstructor.newInstance(privateSource,p,p,64,2);
        try {
            int[] measured=(int[])proof.run(true);Bitmap measuredCopy=Bitmap.ALL.get(Bitmap.ALL.size()-1);
            check(measured!=null&&measuredCopy.reads==(height+31)/32+(height+63)/64+1,
                "timed candidate includes full opacity scan, stream reads and final output copy");
            check(measuredCopy.isRecycled(),"timed proof returns pixels after releasing private Bitmap");
        }finally{proof.close();}
        check(privateSource.isRecycled(),"detached finish proof owns and releases source");
    }
    static SingleNoise1955.Model model(final int[] source,final int width,final int height) {
        return SingleNoise1955.probe1978(new SingleNoise1955.Patches(){public void read(int[] p,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(source,(y+row)*width+x,p,row*w,w);}},width,height,false);
    }
    static void fallback(boolean residual,boolean nativeMode)throws Exception {
        GpuNoise1960.single=!residual;
        if(!nativeMode)field(SingleNoise1955.class,"nativeState").setInt(null,-1);
        final int width=33,height=97,begin=7,end=80;int[] source=image(width,height,1),before=source.clone();SingleNoise1955.Model model=model(source,width,height);
        check(model.meanSigma()>=.30f||model.meanChromaSigma()>=.30f,"active Single model");
        int[] u=CpuSingle1978.geometry(width,height,begin,end,0,height,0,4,true,model);
        force(CpuSingle1978.key(nativeMode,u,false));
        SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
        int[] expected=new int[source.length];Arrays.fill(expected,0x13579bdf);
        SingleNoise1955.processCpuRange(source,expected,width,height,begin,end,0,height,0,4,true,model,null);
        try {
            Object first=null;
            for(int round=0;round<2;round++) {
                int[] output=new int[source.length];Arrays.fill(output,0x13579bdf);
                SingleNoise1955.processRange(source,output,width,height,begin,end,0,height,0,4,true,model,null,workspace);
                same(expected,output,"GPU cold CPU workspace output");
                Object retained=field(SingleNoise1955.Workspace.class,nativeMode?"nativeHandle":"javaKernel").get(workspace);
                check(nativeMode?((Long)retained).longValue()!=0:retained!=null,"R1 caller workspace must survive cold GPU dispatch");
                if(round==0)first=retained;else check(nativeMode?first.equals(retained):first==retained,"same owned capacity reused across strips");
                check(!field(SingleNoise1955.Workspace.class,"inUse").getBoolean(workspace),"caller lease released exactly once");
            }
            same(before,source,"Single input immutable");
        }finally{workspace.close();GpuQualification1961.captureChanged();}
        check(!field(SingleNoise1955.Workspace.class,"inUse").getBoolean(workspace),"closed worker quiescent");
        if(nativeMode)check(field(SingleNoise1955.class,"nativeState").getInt(null)>0,"real CPU JNI executed");
    }
    static void speed()throws Exception {
        String original="route81-original-exact",updated="route81-only-speed";
        GpuQualification1961.qualified(original,1000,100,0);
        SingleCpu1981.measured(original,updated,100,110,500,550);
        check(GpuQualification1961.restore(original)!=null&&!GpuQualification1961.exactRejected(original),"updated CPU speed preserves original GPU equality");
        check(SingleCpu1981.prefersCpu(updated),"two measured CPU wins select CPU independently");
        String noisy="route81-noisy-speed";SingleCpu1981.measured(original,noisy,100,900,200,1000);
        check(GpuQualification1961.restore(noisy)==null,"overlapping timing ranges do not fabricate certificate");
        check(!GpuQualification1961.exactRejected(original)&&GpuQualification1961.restore(original)!=null,"speed decline keeps exact record");
        GpuQualification1961.rejectExact(original);SingleCpu1981.measured(original,original,10000,11000,1,2);
        check(GpuQualification1961.exactRejected(original)&&GpuQualification1961.restore(original)==null,"true original mismatch cannot be erased");
    }
    static void stage()throws Exception {
        int width=17,height=65;int[] image=image(width,height,1);SingleNoise1955.Model model=model(image,width,height),other=model(image,width,height);
        String exact="route81-stage-fixture";SingleStage1981 stage=SingleStage1981.begin(model);
        check(stage!=null&&SingleStage1981.begin(model)==null,"one registered owner per model identity");
        check(SingleStage1981.acquire(model,SingleStage1981.SINGLE,exact)==null&&GpuNoise1960.opens==0,"unknown resource reuse never opens session");force(SingleStage1981.key(exact));
        GpuNoise1960.fault=1;
        SingleStage1981.Lease first=SingleStage1981.acquire(model,SingleStage1981.SINGLE,exact);
        check(first!=null&&first.model(model),"qualified stage uploads exact model");
        check(SingleStage1981.blocksLegacy(model),"occupied owner also blocks still-cold strip keys");
        check(!first.model(other),"equal-shape foreign model cannot bind");
        check(SingleStage1981.acquire(model,SingleStage1981.SINGLE,exact)==null,"busy worker declines without waiting");
        boolean premature=false;try{stage.close();}catch(IllegalStateException expected){premature=true;}check(premature,"cannot close before worker join");
        first.complete();first.close();first.close();
        SingleStage1981.Lease second=SingleStage1981.acquire(model,SingleStage1981.SINGLE,exact);
        check(second!=null&&second.model(model),"second strip reuses same session/model");second.complete();second.close();
        check(GpuNoise1960.opens==1&&GpuNoise1960.modelUploads==1&&GpuNoise1960.closes==0,"one model upload for multiple completed strips");
        check(SingleStage1981.acquire(model,SingleStage1981.RESIDUAL,exact)==null,"backend cannot repurpose resident model slot");
        SingleStage1981.Lease failed=SingleStage1981.acquire(model,SingleStage1981.SINGLE,exact);check(failed!=null,"fault lease acquired");failed.close();
        check(GpuNoise1960.closes==1&&SingleStage1981.acquire(model,SingleStage1981.SINGLE,exact)==null,"failed stage closes and cannot reuse stale resources");stage.close();
        check(!SingleStage1981.owns(model)&&GpuNoise1960.active==0,"photo close removes every owned reference");
        // Exercise the real facade while a different worker owns its selected
        // stage; even a failed legacy open would synchronously queue native work.
        int[] expected=new int[image.length],output=new int[image.length];
        SingleNoise1955.processCpuRange(image,expected,width,height,0,height,0,height,0,4,true,model,null);
        check(GpuSingle1960.process1981(image,output,width,height,0,height,0,height,0,4,true,model,null,null),"cold facade supplies CPU reference");
        GpuQualification1961.captureChanged();
        Map<?,?> gates=(Map<?,?>)field(GpuSingle1960.class,"GATES").get(null);
        String real=(String)gates.keySet().iterator().next();
        GpuQualification1961.qualified(real,1000000000L,1L,0);
        String cpuReference=SingleCpu1981.referenceKey(real,width,height,0,height,0,height,0,4,true,model,null);
        force(SingleStage1981.key(cpuReference));
        stage=SingleStage1981.begin(model);SingleStage1981.Lease held=SingleStage1981.acquire(model,SingleStage1981.SINGLE,cpuReference);
        check(held!=null&&held.model(model),"real facade stage occupied by another worker");
        int attempts=GpuNoise1960.openAttempts;
        SingleNoise1955.Workspace workspace=new SingleNoise1955.Workspace();
        try {
            SingleNoise1955.processRange(image,output,width,height,0,height,0,height,0,4,true,model,null,workspace);
            check(GpuNoise1960.openAttempts==attempts,"busy selected stage never calls legacy open");
            same(expected,output,"contending worker immediately computes original CPU pixels");
        }finally{workspace.close();held.complete();held.close();stage.close();}
        check(GpuNoise1960.active==0&&!SingleStage1981.owns(model),"contended photo closes after all leases");
    }
    static final class StageOperation implements SingleStage1981.Operation {
        final SingleNoise1955.Model model;final int mode;final String original;int oldCalls,newCalls,closes;
        int[][] latest=new int[2][];boolean cancelled;
        StageOperation(SingleNoise1955.Model model,int mode,String original){this.model=model;this.mode=mode;this.original=original;}
        public String referenceKey(){return mode==10||mode==11&&oldCalls>=3||mode==12&&newCalls>=6?original+":changed":original;}
        public SingleStage1981.CpuSample cpu(int pass,GpuQualification1961.Cancellation cancellation) {
            oldCalls++;if(mode==9)throw new SingleCpu1981.Mismatch("controlled completed CPU comparison mismatch");long start=System.nanoTime();
            try{Thread.sleep(mode==2?1:10);}catch(InterruptedException stop){Thread.currentThread().interrupt();return null;}
            long elapsed=System.nanoTime()-start;int[] output=new int[97];Arrays.fill(output,0xff132731);output[96]=0xff456700+pass;
            if(mode==6)output[95]=oldCalls;
            latest[pass]=output.clone();return new SingleStage1981.CpuSample(output,elapsed);
        }
        public long preparationNanos(){return 0;}
        public int[] run(SingleStage1981.Lease lease,int pass) {
            newCalls++;if(lease==null||!lease.model(model))return null;
            if(mode==3&&pass==1)return null;
            if(mode==4&&pass==1)throw new UnsatisfiedLinkError("controlled second-stage native failure");
            try{Thread.sleep(mode==2?10:1);}catch(InterruptedException stop){Thread.currentThread().interrupt();return null;}
            int[] output=latest[pass].clone();
            if(mode==1&&pass==1)output[96]=0xff456700;
            if(mode==7&&pass==1)GpuQualification1961.rejectExact(original);
            if(mode==8)cancelled=true;
            lease.complete();return output;
        }
        public void close(){closes++;}
    }
    static void stageGate()throws Exception {
        int[] source=image(17,65,1);SingleNoise1955.Model model=model(source,17,65);GpuNoise1960.fault=1;
        Class<?> proofType=Class.forName("com.hiro.ulike.SingleStage1981$Proof");
        Constructor<?> constructor=proofType.getDeclaredConstructor(String.class,String.class,SingleNoise1955.Model.class,int.class,long.class,SingleStage1981.Operation.class);
        constructor.setAccessible(true);
        for(int mode=0;mode<13;mode++) {
            String exact="pipeline81-resource-gate:"+mode,key=SingleStage1981.key(exact);
            if(mode!=0)GpuQualification1961.qualified(exact,1000000000L,1L,0);
            if(mode==5)GpuQualification1961.rejectExact(exact);
            final StageOperation operation=new StageOperation(model,mode,exact);
            GpuQualification1961.Probe proof=(GpuQualification1961.Probe)constructor.newInstance(exact,key,model,SingleStage1981.SINGLE,0L,operation);
            try{proof.run(new GpuQualification1961.Cancellation(){public boolean cancelled(){return operation.cancelled;}});}
            finally{proof.close();}
            check(operation.closes==1&&GpuNoise1960.active==0,"resource gate always closes operation and actual lease owner");
            if(mode==0) {
                check(CpuExact1978.enabled(key),"cold independent stage obtains its own exact2 and 5 percent admission");
                check(GpuQualification1961.restore(exact)==null&&!GpuQualification1961.exactRejected(exact),"cold stage never invents the original per-strip GPU certificate");
                check(operation.oldCalls==6&&operation.newCalls==6,"warmup and both complete two-input CPU equality trials executed");
            } else if(mode==5||mode==7) {
                check(GpuQualification1961.exactRejected(exact)&&!CpuExact1978.enabled(key),"original negative wins before execution and before stage certificate commit");
                if(mode==5)check(operation.oldCalls+operation.newCalls==0,"known original mismatch executes no stage work");
            } else {
                check(GpuQualification1961.restore(exact)!=null&&!GpuQualification1961.exactRejected(exact),"stage qualification never modifies existing original GPU proof");
                if(mode==1||mode==6||mode==9)check(GpuQualification1961.exactRejected(key)&&!CpuExact1978.enabled(key),"completed CPU or GPU mismatch rejects only stage reuse");
                else check(!CpuExact1978.enabled(key)&&!GpuQualification1961.exactRejected(key),"slow unavailable cancelled or failed stage is never fabricated exact evidence");
                if(mode==10)check(operation.oldCalls+operation.newCalls==0,"changed CPU profile prevents all qualification execution");
                if(mode==11)check(operation.oldCalls==3&&operation.newCalls==2,"CPU profile change stops the following comparison pass");
                if(mode==12)check(operation.oldCalls==6&&operation.newCalls==6,"CPU profile is rechecked after the final GPU result before certificate commit");
            }
        }
        final int[] factories={0};String rejected="pipeline81-rejected-offer";GpuQualification1961.rejectExact(rejected);
        SingleStage1981.offer(rejected,rejected,model,SingleStage1981.SINGLE,65536,1048576,new SingleStage1981.Factory(){
            public SingleStage1981.Operation create(){factories[0]++;return null;}
        });
        check(factories[0]==0,"original exact negative is checked before any stage snapshot factory");
    }
    static void cpuReference(boolean nativeMode)throws Exception {
        if(!nativeMode)field(SingleNoise1955.class,"nativeState").setInt(null,-1);
        final int width=33,rows=97,begin=7,end=80;int[] source=image(width,rows,1);
        SingleNoise1955.Model model=model(source,width,rows);String original="pipeline81-current-cpu-profile";
        GpuQualification1961.qualified(original,1000000000L,1,0);
        String before=SingleCpu1981.referenceKey(original,width,rows,begin,end,0,rows,0,4,true,model,null);
        int units=(end-begin+3)/4,workers=Math.min(units,SpeedWorkers1935.maxWorkers());
        check(workers>1,"actual parallel CPU profile has disjoint worker ranges");
        int last=begin+(units/workers)*4;
        int[] part=CpuSingle1978.geometry(width,rows,begin,last,0,rows,0,4,true,model);
        force(CpuSingle1978.key(nativeMode,part,false));
        String after=SingleCpu1981.referenceKey(original,width,rows,begin,end,0,rows,0,4,true,model,null);
        check(!before.equals(after),"subrange workspace qualification changes the complete CPU reference");
        Method colourKey=ColourCache1976.class.getDeclaredMethod("key",int.class,int[].class,boolean.class);colourKey.setAccessible(true);
        String colour=(String)colourKey.invoke(null,0,part,false);force(colour);
        String withColour=SingleCpu1981.referenceKey(original,width,rows,begin,end,0,rows,0,4,true,model,null);
        check(!after.equals(withColour),"subrange CPU colour qualification changes the complete CPU reference");
        check(GpuQualification1961.restore(original)!=null&&!GpuQualification1961.exactRejected(original),"CPU profile changes preserve original GPU quality evidence");
        boolean entered=SpeedWorkers1935.enterLegacy();
        try {
            check(withColour.equals(SingleCpu1981.referenceKey(original,width,rows,begin,end,0,rows,0,4,true,model,null)),"foreground worker and idle proof use the same full-pool profile");
            SingleCpu1981 held=new SingleCpu1981();
            try{check(held.sample(source,null,width,rows,begin,end,0,rows,0,4,true,model,null,new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}})==null,"occupied CPU permit declines optional timing before numerical work");}
            finally{held.close();}
        }finally{SpeedWorkers1935.leaveLegacy(entered);}
        int state=field(SingleNoise1955.class,"nativeState").getInt(null);
        try {
            field(SingleNoise1955.class,"nativeState").setInt(null,state==1?-1:1);
            check(!withColour.equals(SingleCpu1981.referenceKey(original,width,rows,begin,end,0,rows,0,4,true,model,null)),"native availability changes the CPU reference");
        }finally{field(SingleNoise1955.class,"nativeState").setInt(null,state);}
        force(CpuSingle1978.key(nativeMode,CpuSingle1978.geometry(width,rows,begin,end,0,rows,0,4,true,model),false));
        for(int i=0;i<workers;i++) {
            int first=begin+(int)((long)units*i/workers)*4,finish=i+1==workers?end:begin+(int)((long)units*(i+1)/workers)*4;
            force(CpuSingle1978.key(nativeMode,CpuSingle1978.geometry(width,rows,first,finish,0,rows,0,4,true,model),false));
        }
        int[] full=new int[source.length];SingleNoise1955.processCpuRange(source,full,width,rows,begin,end,0,rows,0,4,true,model,null);
        int[] expected=Arrays.copyOfRange(full,begin*width,end*width),wrong=expected.clone();wrong[wrong.length/2]^=1;
        SingleCpu1981 cpu=new SingleCpu1981();
        final GpuQualification1961.Cancellation live=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
        try {
            boolean mismatch=false;
            try{cpu.sample(source,wrong,width,rows,begin,end,0,rows,0,4,true,model,null,live);}catch(SingleCpu1981.Mismatch actual){mismatch=true;}
            check(mismatch,"completed captured-versus-current CPU pixel mismatch is explicit exact evidence");
            check(SpeedWorkers1935.cpuIdle1944(),"failed serial comparison releases its CPU permit");
            SingleStage1981.CpuSample measured=cpu.sample(source,expected,width,rows,begin,end,0,rows,0,4,true,model,null,live);
            check(measured!=null&&measured.nanos>0,"uncontended actual serial and full-pool CPU comparison completes");
            same(expected,measured.pixels,"current CPU reference full output");
            Map<?,?> owners=(Map<?,?>)field(SingleCpu1981.class,"workspaces").get(cpu);
            check(owners.size()==workers,"serial shares first parallel capacity within the four native slots");
            if(nativeMode)for(Object owner:owners.values())check(field(SingleNoise1955.Workspace.class,"nativeHandle").getLong(owner)!=0,"every qualified parallel CPU worker really owns its native workspace");
        }finally{cpu.close();}
        check(SpeedWorkers1935.cpuIdle1944(),"all serial and parallel timing permits released");
    }
    static void coldStage(boolean fault)throws Exception {
        final int width=17,height=65;int[] input=image(width,height,1);SingleNoise1955.Model model=model(input,width,height);
        int[] expected=new int[input.length],output=new int[input.length];
        SingleNoise1955.processCpuRange(input,expected,width,height,0,height,0,height,0,4,true,model,null);
        check(GpuSingle1960.process1981(input,output,width,height,0,height,0,height,0,4,true,model,null,null),"cold stage starts with original CPU output");
        GpuQualification1961.captureChanged();
        Map<?,?> gates=(Map<?,?>)field(GpuSingle1960.class,"GATES").get(null);
        final String original=(String)gates.keySet().iterator().next();
        GpuQualification1961.rejectSpeed(original);
        String reference=SingleCpu1981.referenceKey(original,width,height,0,height,0,height,0,4,true,model,null);
        force(SingleStage1981.key(reference));
        SingleStage1981 stage=SingleStage1981.begin(model);GpuNoise1960.fault=1;GpuNoise1960.readback=expected;
        try {
            int operations=GpuNoise1960.operations;
            check(GpuSingle1960.process1981(input,output,width,height,0,height,0,height,0,4,true,model,null,null),"independently qualified stage escapes old speed rejection");
            check(GpuNoise1960.operations>operations&&GpuNoise1960.active==1,"cold stage actually used the retained GPU owner");
            same(expected,output,"independent stage output committed exactly");
            check(GpuQualification1961.restore(original)==null&&!GpuQualification1961.exactRejected(original),"independent stage did not promote or erase original admission");
            Arrays.fill(output,0x12345678);
            if(fault) {
                GpuNoise1960.fault=2;
                check(!GpuSingle1960.process1981(input,output,width,height,0,height,0,height,0,4,true,model,null,null),"independent stage transport failure preserves caller ownership");
                String speedKey=SingleCpu1981.speedKey(reference,true);
                check(!GpuQualification1961.maySchedule(speedKey)&&!GpuQualification1961.exactRejected(speedKey),"stage-only admission gets bounded speed retry without original positive");
                for(int value:output)check(value==0x12345678,"failed independent stage commits no caller pixel");
                int before=GpuNoise1960.operations;
                check(GpuSingle1960.process1981(input,output,width,height,0,height,0,height,0,4,true,model,null,null),"independent stage cooldown supplies original CPU result");
                same(expected,output,"stage cooldown CPU exact");
                check(GpuNoise1960.operations==before,"stage-only cooldown submits no GPU command");
                return;
            }
            GpuNoise1960.afterRead=new Runnable(){public void run(){GpuQualification1961.rejectExact(original);}};
            check(!GpuSingle1960.process1981(input,output,width,height,0,height,0,height,0,4,true,model,null,null),"new original exact negative wins at output commit");
            for(int value:output)check(value==0x12345678,"negative-after-read leaves every caller pixel uncommitted");
            operations=GpuNoise1960.operations;
            check(!GpuSingle1960.process1981(input,output,width,height,0,height,0,height,0,4,true,model,null,null)&&GpuNoise1960.operations==operations,"known exact negative prevents all later GPU execution");
        }finally{GpuNoise1960.readback=null;GpuNoise1960.afterRead=null;stage.close();}
        check(GpuNoise1960.active==0&&!SingleStage1981.owns(model),"independent stage photo closes after negative transition");
    }
    static void copyDenial()throws Exception {
        final int[] closes={0},factories={0};
        int[] image=image(17,65,1);SingleNoise1955.Model model=model(image,17,65);GpuQualification1961.captureChanged();
        GpuQualification1961.Probe a=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){throw new AssertionError("pending proof ran before idle");}public void close(){closes[0]++;}};
        GpuQualification1961.Probe b=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){throw new AssertionError("pending proof ran before idle");}public void close(){closes[0]++;}};
        check(GpuQualification1961.schedule("strong-pipeline81:3:one",48L*1024*1024,a),"first old snapshot queued");
        check(GpuQualification1961.schedule("strong-pipeline81:3:two",48L*1024*1024,b),"second old snapshot queued");
        field(GpuQualification1961.class,"lastLegacy1981").setBoolean(null,true);
        GpuSnapshotBudget1981.offerStrip1981(GpuSnapshotBudget1981.STRONG_TUNING);
        GpuSnapshotBudget1981.Copy occupied=GpuSnapshotBudget1981.tryStripCopy1981(65536,GpuSnapshotBudget1981.STRONG_TUNING);
        check(occupied!=null,"another admitted snapshot owns the copy token");
        try {
            SingleStage1981.offer("copy-denied-original","copy-denied-stage",model,SingleStage1981.SINGLE,64L*1024*1024,1048576,new SingleStage1981.Factory(){
                public SingleStage1981.Operation create(){factories[0]++;return null;}
            });
            check(factories[0]==0,"unavailable copy token prevents source snapshot");
            check(closes[0]==0&&GpuQualification1961.retainedBytes()==96L*1024*1024,"copy rejection cannot evict already-owned opposite-family proofs");
        }finally{occupied.close();GpuQualification1961.captureChanged();}
        check(closes[0]==2&&GpuQualification1961.retainedBytes()==0,"explicit cancellation releases both preserved owners exactly once");
    }
    static void coefficients()throws Exception {
        SingleResidual1961.Buffers1981 buffers=new SingleResidual1961.Buffers1981();
        int[] shapeWidths={17,33,17,65,9};ByteBuffer previous=null;long words=0;
        try {
            for(int iteration=0;iteration<shapeWidths.length;iteration++) {
                int width=shapeWidths[iteration],height=145;int[] source=image(width,height,1);source[iteration]^=0x00010307;
                SingleNoise1955.Model model=model(source,width,height);
                SingleResidual1961.Preparation reused=new SingleResidual1961.Preparation(source,width,height,0,height,0,4,true,model,buffers);
                SingleResidual1961.Preparation fresh=new SingleResidual1961.Preparation(source,width,height,0,height,0,4,true,model);
                ByteBuffer cache=(ByteBuffer)field(SingleResidual1961.Preparation.class,"cache").get(reused);
                for(int i=0;i<80;i+=4)check(cache.getInt(i)==0,"new source clears native header before JNI");
                if(iteration==2)check(cache==previous,"smaller source reuses capacity");
                try {
                    for(int begin=0;begin<height;begin+=64) {
                        int end=Math.min(height,begin+64);ByteBuffer a=fresh.prepare(begin,end),b=reused.prepare(begin,end);
                        check(a!=null&&b!=null&&a.remaining()==b.remaining(),"native coefficient exact extent");
                        for(int i=0;i<a.remaining();i+=4){check(a.getInt(i)==b.getInt(i),"reused native coefficient bit "+i);words++;}
                    }
                    boolean blocked=false;try{buffers.close();}catch(IllegalStateException expected){blocked=true;}check(blocked,"direct storage cannot close before preparation joins");
                }finally{fresh.close();reused.close();}
                previous=cache;check(SingleResidual1961.retainedBytes1981()>0,"live direct capacity accounted");cases++;
            }
            GpuNoise1960.budget=false;boolean noMemory=false;try{buffers.acquire(95L*1024*1024);}catch(OutOfMemoryError expected){noMemory=true;}
            check(noMemory,"buffer growth respects live budget");GpuNoise1960.budget=true;
        }finally{GpuNoise1960.budget=true;buffers.close();}
        check(SingleResidual1961.retainedBytes1981()==0,"all direct capacity ownership released");coefficientWords=words;
    }
    public static void main(String[] args)throws Exception {
        initialize();String mode=args[0];boolean nativeMode=args.length>1&&Boolean.parseBoolean(args[1]);
        if(nativeMode)System.loadLibrary("ulike_nr1955");
        if(mode.equals("identity")) {
            identity();
            check(field(NativeMoire1951.class,"LOADED").getBoolean(null)==nativeMode,"requested real native/Java finishing executed");
            if(nativeMode)check(field(NativeMoire1951.class,"verified").getInt(null)>0,"actual JNI selfcheck admitted original kernel");
        }
        else if(mode.equals("single"))fallback(false,nativeMode);
        else if(mode.equals("residual"))fallback(true,nativeMode);
        else if(mode.equals("stage"))stage();
        else if(mode.equals("stage-gate"))stageGate();
        else if(mode.equals("cpu-reference"))cpuReference(nativeMode);
        else if(mode.equals("cold-stage"))coldStage(false);
        else if(mode.equals("stage-fault"))coldStage(true);
        else if(mode.equals("copy-denial"))copyDenial();
        else if(mode.equals("speed"))speed();
        else if(mode.equals("coefficients")){System.loadLibrary("residual1981_test");coefficients();}
        else throw new IllegalArgumentException(mode);
        GpuQualification1961.captureChanged();SpeedWorkers1935.trim();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"pixels\":"+pixels+",\"changed_pixels\":"+changed+",\"moire_changed_pixels\":"+moireChanged+",\"sharp_changed_pixels\":"+sharpChanged+",\"naive_differences\":"+naiveDifferences+",\"cases\":"+cases+",\"coefficient_words\":"+coefficientWords+",\"native\":"+nativeMode+",\"physical_android_tested\":false}");
    }
}
