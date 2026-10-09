package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.util.concurrent.CancellationException;

/** Saved-photo transaction. Every private stage reads an immutable source and
 * publishes a complete GPU result only. No photograph is used for a CPU oracle,
 * no CPU noise/correction fallback exists, and unsupported formats fail before
 * an ARGB readback can discard source depth, wide colour or HDR gain metadata. */
public final class GpuRequiredRoute1965 {
    private GpuRequiredRoute1965() {}

    static Bitmap normalize(Bitmap input,int rotation,boolean fixed245,
            PhotoDetail.Settings settings,ProcessingTiming1947.Trace trace) {
        if(settings==null)throw failure("settings","撮影時の補正設定を取得できません");
        if(input==null||input.isRecycled())throw failure("source","撮影元画像がありません");
        final int originalGeneration=input.getGenerationId();
        final int turn=QualityPipeline1932.normalizeRotation(rotation);
        final int[] size=SaveQuality2.output186(input.getWidth(),input.getHeight(),turn,fixed245);
        final boolean noise=settings.noiseOn&&settings.noiseLevel>0;
        final boolean sharp=settings.sharpOn&&settings.sharpLevel>0;
        final boolean chroma=AsyncSave1935.chroma(ChromaPipeline177.enabled1932());
        final boolean moved=turn!=0||size[0]!=input.getWidth()||size[1]!=input.getHeight();
        if(!noise&&!sharp&&!chroma&&!moved)return input;
        validateFormat(input);
        final Bitmap capabilitySource=input;
        GpuRequired1965.run("gpu-capability",originalGeneration,new GpuRequired1965.Work<Boolean>() {
            public Boolean run() {
                requireGeneration(capabilitySource,originalGeneration);
                if(!GpuNoise1960.fp64Verified1965()&&!GpuNoise1960.soft64Verified1965())
                    throw failure("fp64","画質を維持するGPU倍精度演算が利用できません");
                GpuRequiredPolicy1965.requireAvailable();
                requireGeneration(capabilitySource,originalGeneration);return Boolean.TRUE;
            }
        });
        ProcessingTiming1947.note(trace,"ノイズ・保護マスク・補正 GPU必須／SDK美顔・顔検出の内部GPU使用は保証対象外");
        ProcessingTiming1947.settings(trace,"ノイズ低減:"+(noise?settings.noiseLevel:0)+"／くっきり補正:"+(sharp?settings.sharpLevel:0)+"／GPU必須");
        final ShotContext1932.Snapshot shot=ShotContext1932.forBitmap(input);
        final int rw=turn==90||turn==270?input.getHeight():input.getWidth();
        final int rh=turn==90||turn==270?input.getWidth():input.getHeight();
        final float scale=Math.max((float)size[0]/rw,(float)size[1]/rh);
        final boolean reduceFirst=scale<.999999f;
        Bitmap working=input;
        try {
            QualityPixels1932.RegionMask face=null;
            if(noise||sharp&&settings.texturePriority) {
                ProcessingTiming1947.Token token=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.CORRECTION);
                try {face=GpuRequired1965.run("face-protection",originalGeneration,new GpuRequired1965.Work<QualityPixels1932.RegionMask>() {
                    public QualityPixels1932.RegionMask run() {
                        requireGeneration(capabilitySource,originalGeneration);GpuRequiredPolicy1965.requireAvailable();
                        QualityPixels1932.RegionMask mask=GpuRequiredPolicy1965.forBitmap(capabilitySource,turn);
                        requireGeneration(capabilitySource,originalGeneration);return mask;
                    }
                });}
                finally {ProcessingTiming1947.end(token);}
            }
            if(reduceFirst) {
                working=replace(working,input,geometry(working,turn,size[0],size[1],trace));
                if(face!=null)face=GpuRequiredPolicy1965.resampleFace(face,turn,size[0],size[1]);
            }
            SpatialNoise1934 spatial=noise||sharp?spatial(working,trace):null;
            QualityPixels1932.Plan plan;
            ProcessingTiming1947.Token policyTiming=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.CORRECTION);
            try {
                plan=GpuRequiredPlan1965.create(spatial==null?null:spatial.global,
                    shot.metadataReliable?shot.iso:0,shot.metadataReliable?shot.exposureNanos:0,
                    lens(shot),shot.beautyStrength,noise?settings.noiseLevel:0,sharp?settings.sharpLevel:0,
                    settings.texturePriority,settings.shadowPriority,reduceFirst?1f:scale,
                    settings.haloSuppression,spatial,face);
            } finally {ProcessingTiming1947.end(policyTiming);}
            if(chroma) {
                ProcessingTiming1947.Token token=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.CORRECTION);
                try {
                    int radius=Math.max(12,Math.min(128,(Math.min(working.getWidth(),working.getHeight())+16)/32));
                    working=replace(working,input,GpuRequiredChroma1965.apply(working,radius));
                } finally {ProcessingTiming1947.end(token);}
            }
            QualityPixels1932.SmoothMask smooth=null;
            if(noise) {
                working=replace(working,input,single(working,plan,settings.noiseLevel,settings.shadowPriority,trace));
                StrongResult stronger=strong(working,plan,settings.noiseLevel,settings.shadowPriority,trace);
                working=replace(working,input,stronger.bitmap);smooth=stronger.smooth;
            } else ProcessingTiming1947.skip(trace,ProcessingTiming1947.NOISE);
            final boolean joinedFinish=chroma&&plan.sharpLevel>0&&!reduceFirst&&!moved;
            SpatialNoise1934 joinedNoise=noise&&joinedFinish?spatial(working,trace):spatial;
            if(chroma)working=replace(working,input,finish(working,plan,true,false,trace));
            if(!reduceFirst&&moved) {
                working=replace(working,input,geometry(working,turn,size[0],size[1],trace));
                if(face!=null)face=GpuRequiredPolicy1965.resampleFace(face,turn,size[0],size[1]);
                if(smooth!=null)smooth=GpuRequiredPolicy1965.resampleSmooth(smooth,turn,size[0],size[1]);
            }
            if(plan.sharpLevel>0) {
                plan=plan.withFaceRegions(face).withOutputNoise(joinedFinish?joinedNoise:(moved||noise?spatial(working,trace):spatial))
                    .withSmoothedRegions(smooth);
                working=replace(working,input,finish(working,plan,false,true,trace));
            }
            if(working.getWidth()!=size[0]||working.getHeight()!=size[1])throw failure("dimensions","補正後の画像サイズが一致しません");
            requireGeneration(input,originalGeneration);
            ShotContext1932.copy(input,working);ProcessingTiming1947.transfer(input,working);
            ProcessingTiming1947.output(trace,working.getWidth(),working.getHeight(),"GPU画質補正");
            return working;
        } catch(RuntimeException error) {discard(working,input);throw error;}
          catch(Error error) {discard(working,input);throw error;}
    }

    static GpuRequiredFailure1965 failure(String stage,String message) {
        return new GpuRequiredFailure1965(stage,message);
    }
    static void validateFormat(Bitmap source) {
        if(source.getConfig()!=Bitmap.Config.ARGB_8888)
            throw failure("format","画像のビット深度を維持するGPU経路がありません");
        if(Build.VERSION.SDK_INT>=26) {
            ColorSpace space=source.getColorSpace();
            if(space!=null&&!space.isSrgb())throw failure("wide-colour","広色域を維持するGPU経路がありません");
        }
        if(Build.VERSION.SDK_INT>=34&&source.hasGainmap())throw failure("gainmap","HDRゲインマップを維持するGPU経路がありません");
    }
    private static int lens(ShotContext1932.Snapshot shot) {
        if(!shot.metadataReliable)return QualityPixels1932.LENS_UNKNOWN;
        if(shot.lensKind==ShotContext1932.LENS_FRONT)return QualityPixels1932.LENS_FRONT;
        if(shot.lensKind==ShotContext1932.LENS_BACK)return QualityPixels1932.LENS_WIDE;
        if(shot.lensKind==ShotContext1932.LENS_ULTRAWIDE)return QualityPixels1932.LENS_ULTRAWIDE;
        if(shot.lensKind==ShotContext1932.LENS_TELEPHOTO)return QualityPixels1932.LENS_TELE;
        return QualityPixels1932.LENS_UNKNOWN;
    }
    private static void check() {if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU shot cancelled");}
    private static void requireGeneration(Bitmap source,int generation) {
        check();if(source.isRecycled()||source.getGenerationId()!=generation)
            throw GpuRequiredFailure1965.forbidden("source-generation");
    }
    private static void read(Bitmap source,int generation,int[] pixels,int width,int x,int y,int height) {
        requireGeneration(source,generation);source.getPixels(pixels,0,width,x,y,width,height);
        requireGeneration(source,generation);
    }
    private static void need(boolean ok,String stage){check();if(!ok)throw failure(stage,"GPU処理を完了できません");}
    private static void discard(Bitmap candidate,Bitmap original){if(candidate!=null&&candidate!=original)QualityPipeline1932.recycle1954(candidate);}
    private static Bitmap replace(Bitmap old,Bitmap original,Bitmap candidate) {
        if(candidate==null)throw failure("publish","GPU処理の完全な画像がありません");
        if(old!=candidate)discard(old,original);return candidate;
    }
    private static Bitmap candidate(Bitmap source,int w,int h) {
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        out.setDensity(source.getDensity());out.setHasAlpha(source.hasAlpha());out.setPremultiplied(source.isPremultiplied());
        return out;
    }
    private static SpatialNoise1934 spatial(final Bitmap source,final ProcessingTiming1947.Trace trace) {
        final int generation=source.getGenerationId();
        return GpuRequired1965.run("noise-evidence",generation,new GpuRequired1965.Work<SpatialNoise1934>() {
            public SpatialNoise1934 run() {
                ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE);
                try {
                    int w=source.getWidth(),h=source.getHeight(),pw=Math.min(64,w),ph=Math.min(64,h);
                    int nx=Math.max(1,Math.min(13,(w+127)/128)),ny=Math.max(1,Math.min(13,(h+127)/128));
                    int[] patches=new int[nx*ny*pw*ph],patch=new int[pw*ph];
                    for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
                        check();int x=nx==1?(w-pw)/2:Math.round((float)(w-pw)*gx/(nx-1));
                        int y=ny==1?(h-ph)/2:Math.round((float)(h-ph)*gy/(ny-1));
                        read(source,generation,patch,pw,x,y,ph);System.arraycopy(patch,0,patches,(gy*nx+gx)*pw*ph,pw*ph);
                    }
                    SpatialNoise1934 result=GpuAnalysis1961.spatialCandidate1961(patches,w,h,0);
                    requireGeneration(source,generation);return result;
                } finally {ProcessingTiming1947.end(timing);}
            }
        });
    }
    private static Bitmap single(final Bitmap source,final QualityPixels1932.Plan plan,final int level,
            final boolean shadows,final ProcessingTiming1947.Trace trace) {
        final int generation=source.getGenerationId();
        return GpuRequired1965.run("single-noise",generation,new GpuRequired1965.Work<Bitmap>() {
            public Bitmap run() {
                ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE);
                Bitmap out=null;boolean done=false;
                try {
                    final int w=source.getWidth(),h=source.getHeight();
                    SingleNoise1955.Model model=GpuRequiredNoise1965.probe(new SingleNoise1955.Patches(){
                        public void read(int[] p,int x,int y,int width,int height){GpuRequiredRoute1965.read(source,generation,p,width,x,y,height);}
                    },w,h);
                    out=candidate(source,w,h);int core=32;
                    for(int first=0;first<h;first+=core) {
                        check();int last=Math.min(h,first+core),origin=Math.max(0,first-SingleNoise1955.HALO);
                        int rows=Math.min(h,last+SingleNoise1955.HALO)-origin;
                        int[] input=new int[w*rows],output=new int[input.length];
                        read(source,generation,input,w,0,origin,rows);
                        GpuRequiredNoise1965.processRange(input,output,w,rows,first-origin,last-origin,0,rows,
                            origin,level,shadows,model,plan);
                        out.setPixels(output,(first-origin)*w,w,0,first,w,last-first);
                    }
                    requireGeneration(source,generation);done=true;return out;
                } finally {if(!done)discard(out,source);ProcessingTiming1947.end(timing);}
            }
        });
    }
    private static final class StrongResult {
        final Bitmap bitmap;final QualityPixels1932.SmoothMask smooth;
        StrongResult(Bitmap bitmap,QualityPixels1932.SmoothMask smooth){this.bitmap=bitmap;this.smooth=smooth;}
    }
    private static StrongResult strong(final Bitmap source,final QualityPixels1932.Plan plan,
            final int noise,final boolean shadows,final ProcessingTiming1947.Trace trace) {
        final int generation=source.getGenerationId();
        return GpuRequired1965.run("strong-noise",generation,new GpuRequired1965.Work<StrongResult>() {
            public StrongResult run() {
                ProcessingTiming1947.Token timing=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE);
                Bitmap out=null;GpuNoise1960.Session session=null;boolean done=false;
                try {
                    GpuRequiredPolicy1965.requireAvailable();
                    int w=source.getWidth(),h=source.getHeight(),hw=(w+1)/2,hh=(h+1)/2;
                    float[] evidence=strongSourceEvidence(source,generation);
                    int pw=Math.min(32,w),ph=Math.min(32,h),nx=(w+63)/64,ny=(h+63)/64;
                    int[] patches=new int[Math.multiplyExact(nx*ny,pw*ph)],patch=new int[pw*ph];
                    for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
                        check();int x=Math.max(0,Math.min(w-pw,gx*64+32-pw/2));
                        int y=Math.max(0,Math.min(h-ph,gy*64+32-ph/2));
                        read(source,generation,patch,pw,x,y,ph);System.arraycopy(patch,0,patches,(gy*nx+gx)*pw*ph,pw*ph);
                    }
                    float[] regional=GpuAnalysis1961.regionsCandidate1961(patches,w,h,evidence,0);
                    need(regional!=null,"regional-noise");
                    float[] runtime=new float[16+regional.length];System.arraycopy(evidence,0,runtime,0,16);
                    System.arraycopy(regional,0,runtime,16,regional.length);
                    int[] full=new int[Math.multiplyExact(w,h)];read(source,generation,full,w,0,0,h);
                    int[] half=half(full,w,h);full=null;need(half!=null,"noise-pyramid");
                    int qw=(hw+1)/2,qh=(hh+1)/2,ew=(qw+1)/2,eh=(qh+1)/2;
                    int[] ws={hw,qw,ew},hs={hh,qh,eh},slots={0,14,15};
                    session=GpuNoise1960.open();need(session!=null,"strong-model-session");
                    long maps=(long)hw*hh+(long)qw*qh+(long)ew*eh;
                    need(GpuNoise1960.workspaceFits(12L*maps+8L*runtime.length+1048576L),"strong-model-workspace");
                    float[] initial=new float[64];System.arraycopy(evidence,0,initial,0,16);
                    GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,half)
                        .allocate(14,4L*qw*qh).allocate(15,4L*ew*eh)
                        .allocate(4,4L*hw*hh).allocate(5,4L*qw*qh).allocate(6,4L*ew*eh)
                        .upload(17,initial).upload(18,GpuPolicy1960.strongBasis8()).allocate(19,4);
                    int[] pyramid=GpuPolicy1960.pyramidUniforms(hw,hh);
                    b.dispatch(GpuNoise1960.ANALYSIS,new int[]{0,14,17,19,18,19},pyramid,null,qw*qh);
                    pyramid=GpuPolicy1960.pyramidUniforms(qw,qh);
                    b.dispatch(GpuNoise1960.ANALYSIS,new int[]{14,15,17,19,18,19},pyramid,null,ew*eh);
                    for(int level=0;level<3;level++) {
                        int width=ws[level],height=hs[level],offset=16+level*16;
                        int[] binding={slots[level],19,17,16,18,19};b.upload(16,new int[2064]);
                        if(width>1&&height>1) {
                            int step=Math.max(1,(int)Math.sqrt((long)width*height/24000.0));
                            int[] lag=GpuPolicy1960.lagUniforms(width,height,step,1,0,0,width,height,0);
                            b.dispatch(GpuNoise1960.ANALYSIS,binding,lag,null,GpuPolicy1960.lagInvocations(lag));
                        }
                        int[] median=new int[32];median[0]=3;median[10]=offset;
                        b.dispatch(GpuNoise1960.ANALYSIS,binding,median,null,16);
                        if(width>=8&&height>=8) {
                            b.upload(16,new int[2064]);int[] spectral=GpuPolicy1960.spectralUniforms(width,height,0);
                            b.dispatch(GpuNoise1960.ANALYSIS,binding,spectral,null,spectral[3]*spectral[4]);
                            median[11]=1;b.dispatch(GpuNoise1960.ANALYSIS,binding,median,null,16);
                        }
                        int[] u=new int[32];u[0]=width;u[1]=height;u[3]=height;u[5]=height;u[7]=height;
                        u[8]=noise;u[9]=shadows?1:0;u[10]=level;u[20]=offset;
                        b.dispatch(GpuNoise1960.strongProgram(level,0),new int[]{slots[level],4+level,17,19,4,5,6,19},u,null,width*height);
                    }
                    need(session.run(b)&&session.upload(2,runtime),"strong-model-compute");
                    out=candidate(source,w,h);byte[] cells=new byte[Math.multiplyExact((w+3)/4,(h+3)/4)];
                    for(int first=0;first<h;first+=32) {
                        check();int last=Math.min(h,first+32),origin=Math.max(0,first-StrongNoise1958.HALO);
                        int rows=Math.min(h,last+StrongNoise1958.HALO)-origin,count=w*(last-first);
                        int[] input=new int[w*rows];read(source,generation,input,w,0,origin,rows);
                        int cfCount=((w+3)/4)*((last-first+3)/4);
                        need(session.upload(0,input)&&session.allocate(1,4L*count)&&session.upload(7,new int[cfCount]),"strong-noise-buffers");
                        need(GpuRequiredPolicy1965.preparePolicy1965(session,plan,w,last-first,first,true,3,8,9,10,11,12),"strong-noise-policy");
                        int[] u=new int[32];u[0]=w;u[1]=rows;u[2]=first-origin;u[3]=last-origin;
                        u[5]=rows;u[6]=origin;u[7]=h;u[8]=noise;u[9]=shadows?1:0;u[10]=3;u[11]=1;u[12]=1;
                        u[18]=plan.beautyQ8;u[19]=plan.shadowBudgetQ8;
                        need(session.dispatch(GpuNoise1960.strongProgram(3,0),new int[]{0,1,2,3,4,5,6,7},u,null,count),"strong-noise-compute");
                        int[][] result=session.readMany(new int[]{1,3,7},new int[]{count,count*2,cfCount});
                        need(result!=null,"strong-noise-readback");
                        out.setPixels(result[0],0,w,0,first,w,last-first);
                        // NR13 reduction is also GPU work. Close the model session
                        // before opening its independent policy reduction session.
                        int[] protectedCells=GpuRequiredPolicy1965.smoothCells(session,input,w,rows,first-origin,last-origin,
                            origin,h,result[1],result[2]);
                        need(protectedCells!=null,"smooth-region-protection");
                        for(int i=0;i<protectedCells.length;i++)cells[(first/4)*((w+3)/4)+i]=(byte)protectedCells[i];
                    }
                    QualityPixels1932.SmoothMask smooth=GpuRequiredPolicy1965.smoothMask(w,h,cells);
                    requireGeneration(source,generation);done=true;return new StrongResult(out,smooth);
                } finally {
                    if(session!=null)session.close();if(!done)discard(out,source);ProcessingTiming1947.end(timing);
                }
            }
        });
    }
    private static float[] strongSourceEvidence(Bitmap source,int generation) {
        int w=source.getWidth(),h=source.getHeight(),pw=Math.min(48,w),ph=Math.min(48,h);
        int nx=Math.min(9,Math.max(1,(w+383)/384)),ny=Math.min(9,Math.max(1,(h+383)/384));
        GpuNoise1960.Session session=GpuNoise1960.open();need(session!=null,"source-noise-session");
        try {
            need(session.upload(1,new int[2064])&&session.upload(2,new float[16])&&session.allocate(3,4),"source-noise-buffers");
            int[] patch=new int[pw*ph],bindings={0,3,2,1,3,3};
            for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
                check();int x=nx==1?(w-pw)/2:(w-pw)*gx/(nx-1),y=ny==1?(h-ph)/2:(h-ph)*gy/(ny-1);
                read(source,generation,patch,pw,x,y,ph);
                int[] lag=GpuPolicy1960.lagUniforms(pw,ph,1,1,0,0,pw,ph,0);
                need(session.upload(0,patch)&&session.dispatch(GpuNoise1960.ANALYSIS,bindings,lag,null,
                    GpuPolicy1960.lagInvocations(lag)),"source-noise-evidence");
            }
            int[] median=new int[32];median[0]=3;
            need(session.dispatch(GpuNoise1960.ANALYSIS,bindings,median,null,16),"source-noise-median");
            int[] bits=session.readInts(2,16);need(bits!=null,"source-noise-readback");
            float[] result=new float[16];for(int i=0;i<16;i++)result[i]=Float.intBitsToFloat(bits[i]);return result;
        } finally {session.close();}
    }
    private static int[] half(int[] source,int width,int height) {
        int[] u=GpuPolicy1960.pyramidUniforms(width,height);int count=u[3]*u[4];
        need(GpuNoise1960.workspaceFits(8L*source.length+8L*count+65536),"pyramid-workspace");
        GpuNoise1960.Session session=GpuNoise1960.open();need(session!=null,"pyramid-session");
        try {
            need(session.upload(0,source)&&session.allocate(1,4L*count)&&session.allocate(2,4),"pyramid-buffers");
            need(session.dispatch(GpuNoise1960.ANALYSIS,new int[]{0,1,2,2,2,2},u,null,count),"pyramid-compute");
            return session.readInts(1,count);
        } finally {session.close();}
    }
    private static Bitmap geometry(final Bitmap source,final int rotation,final int width,final int height,
            final ProcessingTiming1947.Trace trace) {
        if(rotation==0&&width==source.getWidth()&&height==source.getHeight())return source;
        final int generation=source.getGenerationId();
        return GpuRequired1965.run("geometry",generation,new GpuRequired1965.Work<Bitmap>() {
            public Bitmap run() {
                ProcessingTiming1947.Token timer=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.CORRECTION);
                GpuNoise1960.Session session=null;Bitmap out=null;boolean done=false;
                try {
                    validateFormat(source);GpuRequiredPolicy1965.requireAvailable();
                    int sw=source.getWidth(),sh=source.getHeight();
                    // Axes are immutable interpolation command metadata. Every
                    // source observation, accumulation and output pixel is GPU.
                    GpuPolicy1960.GeometryData data=GpuPolicy1960.geometry(sw,sh,rotation,width,height);
                    int[] base=data.uniforms(0),input=new int[Math.multiplyExact(sw,sh)];
                    read(source,generation,input,sw,0,0,sh);
                    need(GpuNoise1960.workspaceFits(8L*input.length+8L*width*height+data.workspaceBytes()+1048576),"geometry-workspace");
                    session=GpuNoise1960.open();need(session!=null,"geometry-session");
                    need(session.upload(0,input)&&session.upload(4,data.weights)&&session.upload(5,data.tables),"geometry-source");
                    requireOpaque(session,0,input.length,6);input=null;
                    out=candidate(source,width,height);
                    int[] bindings={0,1,2,3,4,5};
                    for(int first=0;first<height;first+=32) {
                        requireGeneration(source,generation);int last=Math.min(height,first+32),lo=Integer.MAX_VALUE,hi=-1;
                        if(data.exactCrop){lo=first+base[7];hi=last+base[7];}
                        else {
                            for(int y=first;y<last;y++)for(int at=data.tables[base[10]+y];at<data.tables[base[10]+y+1];at++) {
                                int row=data.tables[base[11]+at];lo=Math.min(lo,row);hi=Math.max(hi,row);
                            }
                            hi++;
                        }
                        int count=Math.multiplyExact(width,last-first);
                        long intermediate=data.exactCrop?4:12L*width*(hi-lo);
                        need(lo>=0&&hi>lo&&hi<=base[14]&&GpuNoise1960.workspaceFits(intermediate+12L*count+65536),"geometry-band-workspace");
                        GpuNoise1960.Batch b=new GpuNoise1960.Batch().allocate(1,4L*count).allocate(2,intermediate).allocate(3,4);
                        int[] u=base.clone();u[15]=first;u[16]=last;u[17]=lo;u[18]=hi-lo;u[20]=1;u[21]=1;
                        if(data.exactCrop)b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,count);
                        else {
                            u[0]=1;b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,width*(hi-lo));
                            u[0]=2;b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,count);
                        }
                        int[][] result=session.execute(b,new int[]{1},new int[]{count});need(result!=null,"geometry-band-readback");
                        out.setPixels(result[0],0,width,0,first,width,last-first);
                    }
                    requireGeneration(source,generation);done=true;return out;
                } finally {if(session!=null)session.close();if(!done)discard(out,source);ProcessingTiming1947.end(timer);}
            }
        });
    }
    private static void requireOpaque(GpuNoise1960.Session session,int sourceSlot,int count,int flagSlot) {
        int[] u=new int[32];u[0]=1;u[2]=count;
        need(session.upload(flagSlot,new int[]{0})&&session.dispatch(GpuNoise1960.fp64Verified1965()?GpuNoise1960.FP64_PROBE1965:GpuNoise1960.SOFT64_PROBE1965,
            new int[]{sourceSlot,flagSlot},u,null,count),"geometry-alpha-admission");
        int[] flag=session.readInts(flagSlot,1);need(flag!=null,"geometry-alpha-readback");
        if(flag[0]!=0)throw failure("geometry-alpha","透明度を維持するGPU拡大縮小経路がありません");
    }
    private static Bitmap finish(final Bitmap source,final QualityPixels1932.Plan plan,final boolean moire,
            final boolean sharp,final ProcessingTiming1947.Trace trace) {
        final int generation=source.getGenerationId();
        return GpuRequired1965.run("output-correction",generation,new GpuRequired1965.Work<Bitmap>() {
            public Bitmap run() {
                ProcessingTiming1947.Token timer=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.CORRECTION);
                Bitmap out=null;GpuNoise1960.Session session=null;boolean done=false;
                try {
                    GpuRequiredPolicy1965.requireAvailable();
                    int w=source.getWidth(),h=source.getHeight();out=candidate(source,w,h);
                    session=GpuNoise1960.open();need(session!=null,"finish-session");
                    for(int first=0;first<h;first+=32) {
                        check();int last=Math.min(h,first+32),origin=Math.max(0,first-QualityPixels1932.HALO);
                        int rows=Math.min(h,last+QualityPixels1932.HALO)-origin,count=w*(last-first);
                        int[] input=new int[w*rows];read(source,generation,input,w,0,origin,rows);
                        FinishPolicy1953.Band policy=sharp?GpuRequiredPolicy1965.finishBand(session,plan,w,rows,first-origin,last-origin,origin):
                            FinishPolicy1953.unused(count);
                        need(policy!=null,"finish-policy");
                        try {
                            int[] u=new int[32];u[1]=w;u[2]=rows;u[3]=origin;u[4]=first-origin;u[5]=last-origin;
                            u[6]=first-origin;u[7]=first-origin;u[8]=moire?1:0;u[9]=sharp?1:0;
                            u[10]=plan.sharpGainQ8;u[11]=plan.sharpFloorQ8;u[12]=plan.sharpLimit;
                            u[13]=plan.texturePriority?1:0;u[14]=plan.haloSuppression?1:0;u[16]=policy.mode;
                            GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,input).allocate(1,4L*count).upload(3,policy.words)
                                .dispatch(GpuNoise1960.FINISH1961,new int[]{0,0,1,3},u,null,count);
                            int[][] result=session.execute(b,new int[]{1},new int[]{count});need(result!=null,"finish-readback");
                            out.setPixels(result[0],0,w,0,first,w,last-first);
                        } finally {policy.close();}
                    }
                    requireGeneration(source,generation);done=true;return out;
                } finally {if(session!=null)session.close();if(!done)discard(out,source);ProcessingTiming1947.end(timer);}
            }
        });
    }
}
