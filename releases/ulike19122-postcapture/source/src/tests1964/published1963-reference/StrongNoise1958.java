package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** NR9–NR12 single-image image-domain NR. Coarse corrections are prepared from an
 * actual half/quarter/eighth pyramid of the already corrected, single-frame image.
 * Regional random-noise and periodic-detail evidence improve flat/edge decisions;
 * same-image NLM is additionally run at half and original resolution where safe.
 * No camera burst, RAW surrogate, full-resolution guide copy, or generated detail
 * is used. The Java implementation is the optional JNI backend's oracle.
 */
public final class StrongNoise1958 {
    private StrongNoise1958() {}
    public static final int HALO=18;
    private static final int PREP_ROWS=128;
    private static final int REGION=64;
    private static final float[][] D8=dctBasis();
    private static volatile int nativeState;
    public interface Patches { void read(int[] pixels,int x,int y,int width,int height); }
    public interface Protection { int budgetQ8(int x,int y); int detailQ8(int x,int y); }

    /** Exclusively owned by one strip worker. Logical prefixes are overwritten on
     * every lease; no image pixels or policy may be shared between active calls. */
    public static final class Workspace implements AutoCloseable {
        private int[] policy,confidence;
        private float[] colours;
        private final float[] nl=new float[3],flat=new float[2],coarse=new float[4];
        private final boolean captureConfidence;
        private boolean inUse,confidenceActive;
        public Workspace(){this(false);}
        public Workspace(boolean captureConfidence){this.captureConfidence=captureConfidence;}
        public int[] policy(){return policy;}
        public int[] confidence(){return confidenceActive?confidence:null;}
        private synchronized void acquire(){if(inUse)throw new IllegalStateException("strong NR workspace already leased");inUse=true;}
        private synchronized void relinquish(){inUse=false;}
        private int[] policy(int length){if(policy==null||policy.length<length){int[] previous=policy;policy=null;SpeedWorkers1935.release(previous);policy=SpeedWorkers1935.borrowInts(length);}return policy;}
        private int[] confidence(int length){if(confidence==null||confidence.length<length){int[] previous=confidence;confidence=null;SpeedWorkers1935.release(previous);confidence=SpeedWorkers1935.borrowInts(length);}Arrays.fill(confidence,0,length,0);return confidence;}
        private float[] colours(int length){if(colours==null||colours.length<length){float[] previous=colours;colours=null;SpeedWorkers1935.release(previous);colours=SpeedWorkers1935.borrowFloats(length);}return colours;}
        public synchronized void close(){if(inUse)throw new IllegalStateException("strong NR workspace still leased");SpeedWorkers1935.release(policy);SpeedWorkers1935.release(confidence);SpeedWorkers1935.release(colours);policy=null;confidence=null;colours=null;confidenceActive=false;}
    }

    /** Sixteen exact-colour rows cover the largest unchanged radius-seven NLM
     * patch. Entries use the original float expression, without reordering. */
    private static final class ColourRows {
        final int[] input;
        final int width,validBegin,validEnd,slots,radius;
        final float[] values;
        int next;
        ColourRows(int[] input,int width,int validBegin,int validEnd,int begin,int radius,Workspace workspace){
            this.input=input;this.width=width;this.validBegin=validBegin;this.validEnd=validEnd;this.radius=radius;slots=Math.min(16,validEnd-validBegin);next=Math.max(validBegin,begin-radius);
            values=workspace.colours(checkedElements((long)width*slots*3));
        }
        void advance(int row){int end=Math.min(validEnd,row+radius+1);while(next<end){checkInterrupted();int start=next*width,at=(next%slots)*width*3;for(int x=0;x<width;x++){int p=input[start+x];float y=luma(p);values[at++]=y;values[at++]=((p>>>16)&255)-y;values[at++]=(p&255)-y;}next++;}}
        int at(int x,int row){return (row%slots)*width*3+x*3;}
        float y(int x,int row){return values[at(x,row)];}
        float r(int x,int row){return values[at(x,row)+1];}
        float b(int x,int row){return values[at(x,row)+2];}
    }

    /** Preparation peak: all original and output pyramid levels, row read
     * buffers and bounded optional JNI staging. A carried half image remains
     * caller-owned throughout preparation and must not rely on GC liveness. */
    public static long modelMemoryBytes(int width,int height) {
        geometry(width,height);
        long half=pixels(half(width),half(height));
        long quarter=pixels(half(half(width)),half(half(height)));
        long eighth=pixels(half(half(half(width))),half(half(half(height))));
        return 8L*(half+quarter+eighth)+(long)SpeedWorkers1935.maxWorkers()*preparationWorkspaceBytes(half(width))+2097152L+12L*((width+REGION-1)/REGION)*((height+REGION-1)/REGION);
    }
    /** Additional worker reservation beyond caller-owned input/output halo rows.
     * Includes Java policy, JNI copies of input/policy/coarse map rows, and the
     * transactional result. Model maps are copied by row region, never wholesale. */
    public static long workspaceBytes(int width,int coreRows) {
        if(width<1||coreRows<1)throw new IllegalArgumentException("strong NR workspace");
        return (long)width*(coreRows*48L+HALO*32L+16L*24L)+1048576L;
    }
    private static long preparationWorkspaceBytes(int width){return (long)width*((PREP_ROWS+14L)*4L+PREP_ROWS*4L+16L*24L)+65536L;}
    public static final class Model {
        public final int width,height;
        private final int[] halfMap,quarterMap,eighthMap;
        private final float[] evidence,runtimeEvidence;
        private GpuAnalysis1961.Resident resident1961;
        private boolean measuredActive;
        public final int fullNonlocalCandidates=8;
        /** Maximum configured candidate count when quarter evidence permits NLM;
         * this is a search bound, not a count of actually accepted/executed patches. */
        public final int nonlocalCandidates;
        private Model(int width,int height,int[] h,int[] q,int[] e,float[] evidence,int nl) {
            this.width=width;this.height=height;halfMap=h;quarterMap=q;eighthMap=e;
            this.evidence=evidence;nonlocalCandidates=nl;runtimeEvidence=new float[16+3*((width+REGION-1)/REGION)*((height+REGION-1)/REGION)];System.arraycopy(evidence,0,runtimeEvidence,0,16);for(float value:evidence)if(value>=.40f)measuredActive=true;
        }
        public long residentBytes() {return 4L*(halfMap.length+quarterMap.length+eighthMap.length+runtimeEvidence.length)+1024L;}
        public float sigmaY(int scale) {return average(evidence,scale*16);}
        public float sigmaC(int scale) {return average(evidence,scale*16+8);}
        private boolean active() {return measuredActive;}
        /** NR13 confidence derives from the same noisy-flat decision as NR9/10.
         * Caller applies its face/detail policy before storing or using this mask. */
        public int smoothingQ8(int[] input,int width,int rows,int x,int row,int validBegin,int validEnd,int originY) {return smoothingQ8(input,width,rows,x,row,validBegin,validEnd,originY,new float[2]);}
        public int smoothingQ8(int[] input,int width,int rows,int x,int row,int validBegin,int validEnd,int originY,float[] flat) {
            if(input==null||width!=this.width||rows<1||input.length<(long)width*rows||x<0||x>=width||row<validBegin||row>=validEnd||validBegin<0||validEnd>rows||(long)originY+validBegin<0||(long)originY+validEnd>height)throw new IllegalArgumentException("smooth NR confidence range");
            int p=input[row*width+x];if((p>>>24)!=255||!active())return 0;
            float y=luma(p),sy=regionalSigma(runtimeEvidence,this.width,height,x,row+originY,0),sc=regionalSigma(runtimeEvidence,this.width,height,x,row+originY,1);
            if(flat==null||flat.length<2)throw new IllegalArgumentException("smooth NR confidence scratch");guide(input,width,rows,x,row,validBegin,validEnd,sy,sc,flat);
            return Math.round(256*clamp((flat[0]-.65f)/.30f,0,1)*clamp((Math.max(sy,sc)-.60f)/1.8f,0,1)*(1-regionalSigma(runtimeEvidence,width,height,x,row+originY,2)));
        }
    }

    static GpuAnalysis1961.Resident takeResident1961(Model model) {
        if(model==null)return null;synchronized(model){GpuAnalysis1961.Resident lease=model.resident1961;model.resident1961=null;return lease;}
    }
    static void attachResident1961(Model model,GpuAnalysis1961.Resident lease){synchronized(model){model.resident1961=lease;}}
    static void discardResident1961(Model model){GpuAnalysis1961.Resident lease=takeResident1961(model);if(lease!=null)lease.close();}
    static Model model1961(int width,int height,int[] h,int[] q,int[] e,float[] evidence,float[] runtime) {
        int nl=0;for(int i=32;i<48;i++)if(evidence[i]>=.40f){nl=24;break;}
        Model result=new Model(width,height,h,q,e,evidence,nl);System.arraycopy(runtime,0,result.runtimeEvidence,0,runtime.length);
        for(int i=16;i<runtime.length;i+=3)if(runtime[i]>=.40f||runtime[i+1]>=.40f)result.measuredActive=true;return result;
    }
    static boolean sameModel1961(Model a,Model b) {
        if(a==null||b==null||a.width!=b.width||a.height!=b.height||a.nonlocalCandidates!=b.nonlocalCandidates||a.measuredActive!=b.measuredActive||
            !Arrays.equals(a.halfMap,b.halfMap)||!Arrays.equals(a.quarterMap,b.quarterMap)||!Arrays.equals(a.eighthMap,b.eighthMap)||a.evidence.length!=b.evidence.length||a.runtimeEvidence.length!=b.runtimeEvidence.length)return false;
        for(int i=0;i<a.evidence.length;i++)if(Float.floatToRawIntBits(a.evidence[i])!=Float.floatToRawIntBits(b.evidence[i]))return false;
        for(int i=0;i<a.runtimeEvidence.length;i++)if(Float.floatToRawIntBits(a.runtimeEvidence[i])!=Float.floatToRawIntBits(b.runtimeEvidence[i]))return false;return true;
    }

    public static Model prepare(Patches source,int width,int height,int noise,boolean shadows) {
        return prepareInternal(source,width,height,noise,shadows,true,null);
    }
    public static Model prepare(Patches source,int width,int height,int noise,boolean shadows,int[] preparedHalf) {
        return prepareInternal(source,width,height,noise,shadows,true,preparedHalf);
    }
    static Model prepareJava(Patches source,int width,int height,int noise,boolean shadows) {
        return prepareInternal(source,width,height,noise,shadows,false,null);
    }
    /** A detached background qualification owns fresh scratch, never a returned
     * foreground worker's pooled arrays or its mutable protection callback. */
    static boolean gpuOracleSnapshot1961(int[] input,int[] output,int width,int rows,
            int begin,int end,int validBegin,int validEnd,int originY,int noise,
            boolean shadows,Model model,int[] policy,int[] confidence) {
        Workspace workspace=new Workspace(confidence!=null);
        try {return gpuOracle1960(input,output,width,rows,begin,end,validBegin,validEnd,
            originY,noise,shadows,model,null,workspace,policy,confidence);}
        finally {workspace.close();}
    }
    private static Model prepareInternal(Patches source,int width,int height,int noise,boolean shadows,boolean useNative,int[] preparedHalf) {
        geometry(width,height);if(source==null||noise<0||noise>4)throw new IllegalArgumentException("strong NR source/settings");
        checkInterrupted();int hw=half(width),hh=half(height),qw=half(hw),qh=half(hh),ew=half(qw),eh=half(qh);
        if(preparedHalf!=null&&preparedHalf.length!=checkedPixels(hw,hh))throw new IllegalArgumentException("strong NR prepared half geometry");
        if(noise==0)return new Model(width,height,new int[0],new int[0],new int[0],new float[64],0);
        int[] h=preparedHalf==null?new int[checkedPixels(hw,hh)]:preparedHalf;
        // Only two full-width source rows are materialized while making the half
        // image. Nonopaque pairs are excluded from all subsequent NR evidence.
        if(preparedHalf==null){int[] sourceRows=SpeedWorkers1935.borrowInts(checkedPixels(width,Math.min(2,height)));
        try{for(int y=0;y<hh;y++) {
            checkInterrupted();int rows=Math.min(2,height-y*2);source.read(sourceRows,0,y*2,width,rows);
            for(int x=0;x<hw;x++)h[y*hw+x]=average2(sourceRows,width,rows,x*2,0);
        }}finally{SpeedWorkers1935.release(sourceRows);}
        }
        float[] evidence=new float[64];estimateSource(source,width,height,evidence,0);
        float[] runtime=new float[16+3*((width+REGION-1)/REGION)*((height+REGION-1)/REGION)];System.arraycopy(evidence,0,runtime,0,16);
        estimateRegions(source,width,height,runtime,evidence,useNative);
        if(useNative){Model resident=GpuAnalysis1961.residentIfQualified(h,width,height,noise,shadows,evidence,runtime);if(resident!=null)return resident;}
        int[] rawHalf=h;long cpuStart=System.nanoTime();
        // GX20 owns admission for the whole resident model graph. Unknown graphs
        // execute this original CPU preparation once; separate level proofs must
        // not copy the same foreground half image or compete with that snapshot.
        int[] q=downsample(h,hw,hh,false),e=downsample(q,qw,qh,false);
        estimate(h,hw,hh,evidence,16,false);estimate(q,qw,qh,evidence,32,false);estimate(e,ew,eh,evidence,48,false);
        int nl=0;
        int[] maps=new int[h.length];prepareScaleCpu(h,maps,hw,hh,noise,shadows,evidence,16,0,useNative);h=maps;
        maps=new int[q.length];prepareScaleCpu(q,maps,qw,qh,noise,shadows,evidence,32,1,useNative);q=maps;
        maps=new int[e.length];prepareScaleCpu(e,maps,ew,eh,noise,shadows,evidence,48,2,useNative);e=maps;
        // NR8 search geometry is fixed. Its real execution/effect is tested by
        // comparing local-only and NLM quarter-map results, rather than this bound.
        for(int i=32;i<48;i++)if(evidence[i]>=.40f){nl=24;break;}
        Model result=model1961(width,height,h,q,e,evidence,runtime);
        if(useNative)GpuAnalysis1961.scheduleResident(rawHalf,width,height,noise,shadows,evidence,runtime,result,System.nanoTime()-cpuStart);
        return result;
    }
    static Model preparedSnapshotCpu1961(int[] halfSource,int width,int height,int noise,boolean shadows,float[] sourceEvidence,float[] runtime) {
        int hw=half(width),hh=half(height),qw=half(hw),qh=half(hh),ew=half(qw),eh=half(qh);
        int[] q=downsample(halfSource,hw,hh,false),e=downsample(q,qw,qh,false);float[] evidence=new float[64];System.arraycopy(sourceEvidence,0,evidence,0,16);
        estimateCpu1960(halfSource,hw,hh,evidence,16);estimateCpu1960(q,qw,qh,evidence,32);estimateCpu1960(e,ew,eh,evidence,48);
        int[] hmap=new int[halfSource.length],qmap=new int[q.length],emap=new int[e.length];
        prepareScaleCpu(halfSource,hmap,hw,hh,noise,shadows,evidence,16,0,true);prepareScaleCpu(q,qmap,qw,qh,noise,shadows,evidence,32,1,true);prepareScaleCpu(e,emap,ew,eh,noise,shadows,evidence,48,2,true);
        return model1961(width,height,hmap,qmap,emap,evidence,runtime);
    }
    private static int[] downsample(int[] src,int width,int height,boolean useGpu) {
        if(useGpu){int[] gpu=GpuPolicy1960.gpuPyramid(src,width,height);if(gpu!=null)return gpu;}
        int ow=half(width),oh=half(height);int[] out=new int[checkedPixels(ow,oh)];
        for(int y=0;y<oh;y++){checkInterrupted();for(int x=0;x<ow;x++)out[y*ow+x]=average2(src,width,height,x*2,y*2);}
        return out;
    }
    private static int average2(int[] src,int width,int height,int x,int y) {
        int r=0,g=0,b=0,n=0;boolean opaque=true;
        for(int yy=y;yy<Math.min(height,y+2);yy++)for(int xx=x;xx<Math.min(width,x+2);xx++) {
            int p=src[yy*width+xx];opaque&=(p>>>24)==255;r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;n++;
        }
        return (opaque?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;
    }
    private static void estimateSource(Patches source,int width,int height,float[] dst,int offset) {
        int pw=Math.min(48,width),ph=Math.min(48,height);int[] patch=new int[checkedPixels(pw,ph)];
        int[][] hist=new int[16][128];int[] counts=new int[16];
        int nx=Math.min(9,Math.max(1,(width+383)/384)),ny=Math.min(9,Math.max(1,(height+383)/384));
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
            checkInterrupted();int x=nx==1?(width-pw)/2:(width-pw)*gx/(nx-1),y=ny==1?(height-ph)/2:(height-ph)*gy/(ny-1);
            source.read(patch,x,y,pw,ph);estimateSamples(patch,pw,ph,hist,counts,1);
        }
        finishEstimate(hist,counts,dst,offset);
    }
    private static void estimate(final int[] src,final int width,final int height,float[] dst,int offset,boolean useGpu) {
        if(useGpu)try{
            float[] gpu=GpuPolicy1960.strongEvidence(src,width,height,new GpuPolicy1960.EvidenceCpu(){
                public float[] compute(){float[] values=new float[16];estimateCpu1960(src,width,height,values,0);return values;}
            });
            if(gpu!=null){System.arraycopy(gpu,0,dst,offset,16);return;}
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        estimateCpu1960(src,width,height,dst,offset);
    }
    /** Original lag/D8 sample geometry and float evidence, GPU oracle. */
    static float[] evidenceSnapshotCpu1961(int[] src,int width,int height) {
        float[] result=new float[16];estimateCpu1960(src,width,height,result,0);return result;
    }
    private static void estimateCpu1960(int[] src,int width,int height,float[] dst,int offset) {
        int step=Math.max(1,(int)Math.sqrt((long)width*height/24000.0));
        int[][] hist=new int[16][128];int[] counts=new int[16];
        estimateLag(src,width,height,hist,counts,step,1);finishEstimate(hist,counts,dst,offset);
        estimateSpectral(src,width,height,dst,offset);
    }
    private static void estimateSpectral(int[] src,int width,int height,float[] dst,int offset) {
        if(width<8||height<8)return;
        int[][] hist=new int[16][128];int[] counts=new int[16];
        int nx=Math.min(17,Math.max(1,width/16)),ny=Math.min(17,Math.max(1,height/16));
        float[][] planes={new float[64],new float[64],new float[64]};
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
            checkInterrupted();int x=nx==1?(width-8)/2:(width-8)*gx/(nx-1),y=ny==1?(height-8)/2:(height-8)*gy/(ny-1);
            float mean=0;boolean opaque=true;
            for(int py=0;py<8;py++)for(int px=0;px<8;px++){int p=src[(y+py)*width+x+px],at=py*8+px;opaque&=(p>>>24)==255;float yy=luma(p);mean+=yy;planes[0][at]=yy;planes[1][at]=((p>>>16)&255)-yy;planes[2][at]=(p&255)-yy;}
            mean/=64;if(!opaque||mean<3||mean>252)continue;int bin=Math.min(7,(int)mean/32);
            // A sparse real periodic texture occupies a few coefficients. The
            // median over the low-frequency band measures distributed coarse
            // grain while rejecting those sparse structured coefficients.
            for(int v=0;v<6;v++)for(int u=0;u<6;u++)if(u+v>=1&&u+v<=5)for(int plane=0;plane<3;plane++) {
                float coefficient=0;for(int py=0;py<8;py++)for(int px=0;px<8;px++)coefficient+=(planes[plane][py*8+px]- (plane==0?mean:0))*D8[u][px]*D8[v][py];
                int at=bin+(plane==0?0:8),value=Math.min(127,Math.round(Math.abs(coefficient)*4));hist[at][value]++;counts[at]++;
            }
        }
        float[] measured=new float[16];finishEstimate(hist,counts,measured,0);
        for(int i=0;i<16;i++)dst[offset+i]=Math.max(dst[offset+i],measured[i]);
    }
    private static void estimateSamples(int[] src,int width,int height,int[][] hist,int[] counts,int step) {
        estimateLag(src,width,height,hist,counts,step,1);
    }
    private static void estimateLag(int[] src,int width,int height,int[][] hist,int[] counts,int step,int lag) {
        for(int y=0;y+lag<height;y+=step) {
            checkInterrupted();for(int x=0;x+lag<width;x+=step) {
                int a=src[y*width+x],b=src[y*width+x+lag],c=src[(y+lag)*width+x],d=src[(y+lag)*width+x+lag];
                if((a>>>24)!=255||(b>>>24)!=255||(c>>>24)!=255||(d>>>24)!=255)continue;
                float ya=luma(a),yb=luma(b),yc=luma(c),yd=luma(d),mean=(ya+yb+yc+yd)*.25f;
                if(mean<3||mean>252)continue;
                int bin=Math.min(7,(int)mean/32);
                float dy=(ya-yb-yc+yd)*.5f;
                float cr=(((a>>>16)&255)-ya-(((b>>>16)&255)-yb)-(((c>>>16)&255)-yc)+(((d>>>16)&255)-yd))*.5f;
                float cb=((a&255)-ya-((b&255)-yb)-((c&255)-yc)+((d&255)-yd))*.5f;
                int sy=Math.min(127,Math.round(Math.abs(dy)*4)),sc=Math.min(127,Math.round((Math.abs(cr)+Math.abs(cb))*2));
                hist[bin][sy]++;counts[bin]++;hist[bin+8][sc]++;counts[bin+8]++;
            }
        }
    }
    private static void finishEstimate(int[][] hist,int[] count,float[] dst,int offset) {
        for(int b=0;b<16;b++)if(count[b]>=16) {
            int sum=0,target=(count[b]+1)/2;
            for(int i=0;i<128;i++){sum+=hist[b][i];if(sum>=target){dst[offset+b]=i/2.69796f;break;}}
        }
        // Fill unsupported brightness bins from actual measured bins only. A
        // quantization-clean image has all-zero evidence and remains unchanged.
        for(int plane=0;plane<2;plane++)for(int i=0;i<8;i++)if(count[plane*8+i]<16) {
            int best=-1,dist=99;for(int j=0;j<8;j++)if(count[plane*8+j]>=16&&Math.abs(i-j)<dist){best=j;dist=Math.abs(i-j);}
            if(best>=0)dst[offset+plane*8+i]=dst[offset+plane*8+best];
        }
    }

    private static void prepareScale(final int[] src,final int[] out,final int width,final int height,final int noise,final boolean shadows,final float[] all,final int offset,final int mode,final boolean useNative) {
        if(useNative&&noise>0)try{
            final float[] ev=Arrays.copyOfRange(all,offset,offset+16);
            if(GpuStrong1960.prepare(src,out,width,height,noise,shadows,ev,mode,new GpuStrong1960.Oracle(){
                public boolean run(int[] destination,int[] confidence){prepareScaleCpu(src,destination,width,height,noise,shadows,all,offset,mode,useNative);return true;}
            })){checkInterrupted();return;}
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        prepareScaleCpu(src,out,width,height,noise,shadows,all,offset,mode,useNative);
    }
    static boolean gpuPrepareOracleSnapshot1961(int[] src,int[] dst,int width,int height,
            int noise,boolean shadows,float[] evidence,int mode) {
        prepareScaleCpu(src,dst,width,height,noise,shadows,evidence,0,mode,true);return true;
    }
    /** Frozen CPU implementation used both for fallback and private GPU admission. */
    private static void prepareScaleCpu(int[] src,int[] out,int width,int height,int noise,boolean shadows,float[] all,int offset,int mode,boolean useNative) {
        final float[] ev=Arrays.copyOfRange(all,offset,offset+16);
        final boolean backend=useNative&&nativeAvailable();
        int stripes=(height+PREP_ROWS-1)/PREP_ROWS;
        Runtime runtime=Runtime.getRuntime();long free=runtime.maxMemory()-(runtime.totalMemory()-runtime.freeMemory());
        long retained=SpeedWorkers1935.nativeRetainedBytes1956();free=free>retained?free-retained:0;
        int workers=Math.min(stripes,Math.min(SpeedWorkers1935.maxWorkers(),SpeedWorkers1935.availableWorkers1944()));
        long room=Math.max(0,free-32L*1024*1024);workers=Math.max(1,Math.min(workers,(int)Math.min(Integer.MAX_VALUE,room/preparationWorkspaceBytes(width))));
        final int n=workers;
        Runnable[] tasks=new Runnable[n];
        for(int worker=0;worker<n;worker++){final int index=worker;tasks[worker]=new Runnable(){public void run(){
            Workspace workspace=new Workspace();try{
                for(int stripe=index;stripe<stripes;stripe+=n){
                    checkInterrupted();int begin=stripe*PREP_ROWS,end=Math.min(height,begin+PREP_ROWS);boolean complete=false;
                    if(backend)try{complete=processNative(src,out,width,height,begin,end,0,height,0,noise,shadows,ev,null,null,null,width,height,null,mode);checkInterrupted();}
                    catch(UnsatisfiedLinkError unavailable){nativeState=-1;}
                    if(!complete)prepareJavaRangeInternal(src,out,width,height,begin,end,noise,shadows,ev,mode,true,workspace);
                }
            }finally{workspace.close();}
        }};}
        // run() drains every submitted task before propagating cancellation or a
        // failure. The private maps can be published only after all stripes join.
        checkInterrupted();SpeedWorkers1935.run(tasks);checkInterrupted();
    }
    /** NR9/10/12 use coherent, independently measured luma and colour guides.
     * NR11 adds same-image bounded NLM to half resolution as well as quarter. */
    static void prepareJavaRange(int[] src,int[] out,int width,int height,int begin,int end,int noise,boolean shadows,float[] ev,int mode) {
        prepareJavaRangeInternal(src,out,width,height,begin,end,noise,shadows,ev,mode,true);
    }
    static void prepareJavaRangeLocalOnly(int[] src,int[] out,int width,int height,int begin,int end,int noise,boolean shadows,float[] ev,int mode) {
        prepareJavaRangeInternal(src,out,width,height,begin,end,noise,shadows,ev,mode,false);
    }
    private static void prepareJavaRangeInternal(int[] src,int[] out,int width,int height,int begin,int end,int noise,boolean shadows,float[] ev,int mode,boolean useNlm) {
        Workspace workspace=new Workspace();try{prepareJavaRangeInternal(src,out,width,height,begin,end,noise,shadows,ev,mode,useNlm,workspace);}finally{workspace.close();}
    }
    private static void prepareJavaRangeInternal(int[] src,int[] out,int width,int height,int begin,int end,int noise,boolean shadows,float[] ev,int mode,boolean useNlm,Workspace workspace) {
        float[] nl=workspace.nl,flat=workspace.flat;
        ColourRows colours=new ColourRows(src,width,0,height,begin,mode==1?7:5,workspace);
        for(int y=begin;y<end;y++) {
            checkInterrupted();colours.advance(y);for(int x=0;x<width;x++) {
                int p=src[y*width+x];float y0=colours.y(x,y),cR=colours.r(x,y),cB=colours.b(x,y);
                if((p>>>24)!=255){out[y*width+x]=0;continue;}
                float sy=sigma(ev,0,y0),sc=sigma(ev,8,y0);
                if(sy<.40f&&sc<.40f){out[y*width+x]=(Math.round(y0)<<24);continue;}
                guide(src,width,height,x,y,0,height,sy,sc,flat,colours);float legacyFlat=1/(1+square(structure(src,width,height,x,y,0,height,colours)/(sy*3+3)));
                float thresholdY=Math.max(3,sy*4.5f),thresholdC=Math.max(5,sc*5.5f);
                float sumY=0,sumR=0,sumB=0,weightY=0,weightC=0,varY=0;
                for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
                    int cy=clamp(y+dy,0,height-1),cx=clamp(x+dx,0,width-1);int q=src[cy*width+cx];if((q>>>24)!=255)continue;
                    float yy=colours.y(cx,cy),rr=colours.r(cx,cy),bb=colours.b(cx,cy);
                    float base=dx==0&&dy==0?4f:Math.abs(dx)<=1&&Math.abs(dy)<=1?2f:1f;
                    float wy=base/(1+square((yy-y0)/thresholdY));
                    float wc=base/(1+.12f*square((yy-y0)/thresholdY)+square((rr-cR)/thresholdC)+square((bb-cB)/thresholdC));
                    sumY+=wy*yy;weightY+=wy;varY+=wy*square(yy-y0);sumR+=wc*rr;sumB+=wc*bb;weightC+=wc;
                }
                float dark=clamp((144-y0)/112,0,1),strength=.60f+.08f*noise+(shadows?.10f*dark:0);
                float localVar=varY/Math.max(.001f,weightY);
                float ly=sy>=.40f?Math.min(.98f,strength)*legacyFlat*clamp(sy*sy/Math.max(sy*sy,localVar*.55f),.12f,1):0;
                float lc=sc>=.40f?Math.min(.99f,strength+.10f)*(mode==0?flat[1]:Math.max(flat[1],legacyFlat)):0;
                float targetY=sumY/Math.max(.001f,weightY),targetR=sumR/Math.max(.001f,weightC),targetB=sumB/Math.max(.001f,weightC);
                if(useNlm&&mode<=1&&(sy>=.60f||sc>=.60f)&&flat[0]>.72f) {
                    if(mode==1)nonlocal(src,width,height,x,y,sy,sc,nl,colours);
                    else nonlocalFine(src,width,height,x,y,0,height,sy,sc,nl,colours);
                    float blend=(mode==1?.65f:.78f)*flat[0];
                    targetY=targetY*(1-blend)+nl[0]*blend;targetR=targetR*(1-blend)+nl[1]*blend;targetB=targetB*(1-blend)+nl[2]*blend;
                    ly=Math.min(.98f,ly+.15f*flat[0]);lc=Math.min(.99f,lc+.10f*flat[1]);
                }
                float dy=(targetY-y0)*ly,dr=(targetR-cR)*lc,db=(targetB-cB)*lc;
                out[y*width+x]=pack(Math.round(y0),dy+dr,dy-(.299f*dr+.114f*db)/.587f,dy+db);
            }
        }
    }
    private static void nonlocal(int[] src,int width,int height,int x,int y,float sy,float sc,float[] result,ColourRows colours) {
        int p=src[y*width+x];float yy=colours.y(x,y),rr=colours.r(x,y),bb=colours.b(x,y);
        float total=1,sumY=yy,sumR=rr,sumB=bb;
        float variance=sy*sy+.22f*sc*sc,h2=Math.max(1.5f,variance*2.6f);
        for(int dy=-6;dy<=6;dy+=3)for(int dx=-6;dx<=6;dx+=3) {
            if(dx==0&&dy==0)continue;int cx=x+dx,cy=y+dy;if(cx<0||cy<0||cx>=width||cy>=height)continue;
            int q=src[cy*width+cx];if((q>>>24)!=255)continue;float distance=0;boolean valid=true;
            for(int py=-1;py<=1;py++)for(int px=-1;px<=1;px++) {
                int ax=clamp(x+px,0,width-1),ay=clamp(y+py,0,height-1),bx=clamp(cx+px,0,width-1),by=clamp(cy+py,0,height-1);
                int a=src[ay*width+ax],b=src[by*width+bx];
                if((a>>>24)!=255||(b>>>24)!=255){valid=false;continue;}
                float ya=colours.y(ax,ay),yb=colours.y(bx,by),ra=colours.r(ax,ay),rb=colours.r(bx,by),ba=colours.b(ax,ay),bc=colours.b(bx,by);
                distance+=square(ya-yb)+.11f*(square(ra-rb)+square(ba-bc));
            }
            if(!valid)continue;
            distance=Math.max(0,distance/9-2*variance);
            // Rational exponential approximation avoids architecture-dependent
            // libm exp and has a sharp cutoff for statistically dissimilar patches.
            float z=distance/h2;if(z>5)continue;float weight=1/(1+z+z*z*.5f+z*z*z/6);
            float qy=colours.y(cx,cy);sumY+=weight*qy;sumR+=weight*colours.r(cx,cy);sumB+=weight*colours.b(cx,cy);total+=weight;
        }
        result[0]=sumY/total;result[1]=sumR/total;result[2]=sumB/total;
    }

    public static void processRange(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        Workspace workspace=new Workspace();
        try{processRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,workspace,false);}
        finally{workspace.close();}
    }
    /** Production strip workers share one policy with NR13 and reuse the same
     * guide at its exact representative pixel. Unaligned callers retain NR13's
     * independent oracle path by receiving a null confidence array. */
    public static void processRange(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,Workspace workspace) {
        if(workspace==null)throw new NullPointerException("strong NR workspace");
        workspace.acquire();try{processRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,workspace,true);}
        finally{workspace.relinquish();}
    }
    private static void processRangeInternal(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,Workspace workspace,boolean sharePolicy) {
        validate(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,model);checkInterrupted();
        boolean nativeWork=noise>0&&model.active()&&end>begin&&end-begin<=256&&(long)width*(end-begin)<=Integer.MAX_VALUE/2&&nativeAvailable();
        int[] policy=null,confidence=null;
        workspace.confidenceActive=false;
        if(sharePolicy||(nativeWork||noise>0&&model.active())&&protection!=null){
            policy=workspace.policy(checkedElements((long)width*(end-begin)*2));
            for(int row=begin;row<end;row++){checkInterrupted();for(int x=0;x<width;x++){int at=((row-begin)*width+x)*2;policy[at]=protection==null?256:clamp(protection.budgetQ8(x,row+originY),0,256);policy[at+1]=protection==null?0:clamp(protection.detailQ8(x,row+originY),0,256);}}
        }
        if(sharePolicy&&workspace.captureConfidence&&((originY+begin)&3)==0&&((end-begin)%4==0||originY+end==model.height)){
            confidence=workspace.confidence(checkedElements((long)((width+3)/4)*((end-begin+3)/4)));workspace.confidenceActive=true;
        }
        if(noise>0&&model.active()&&end>begin)try{
            GpuPolicy1960.Protection descriptor=protection instanceof GpuPolicy1960.Protection&&policy!=null?(GpuPolicy1960.Protection)protection:null;
            final int[] exactPolicy=policy;final Workspace exactWorkspace=workspace;
            if(GpuStrong1960.process(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,policy,confidence,descriptor,new GpuStrong1960.Oracle(){
                public boolean run(int[] destination,int[] mask){
                    return gpuOracle1960(input,destination,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,exactWorkspace,exactPolicy,mask);
                }
            })){checkInterrupted();return;}
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        if(nativeWork)try{
            boolean complete=confidence==null?processNative(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model.runtimeEvidence,model.halfMap,model.quarterMap,model.eighthMap,model.width,model.height,policy,3):processNativeShared(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model.runtimeEvidence,model.halfMap,model.quarterMap,model.eighthMap,model.width,model.height,policy,3,confidence);
            checkInterrupted();if(complete)return;
        }catch(UnsatisfiedLinkError unavailable){nativeState=-1;}
        processJavaRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,true,workspace,policy,confidence);
    }
    static float[] gpuEvidence1960(Model model){return model.runtimeEvidence;}
    static int[][] gpuMaps1960(Model model){return new int[][]{model.halfMap,model.quarterMap,model.eighthMap};}
    /** Exact CPU oracle bypasses the GPU adapter, so admission cannot recurse. */
    private static boolean gpuOracle1960(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,Workspace workspace,int[] policy,int[] confidence){
        if(end-begin<=256&&nativeAvailable())try{
            boolean complete=confidence==null?processNative(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model.runtimeEvidence,model.halfMap,model.quarterMap,model.eighthMap,model.width,model.height,policy,3):processNativeShared(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model.runtimeEvidence,model.halfMap,model.quarterMap,model.eighthMap,model.width,model.height,policy,3,confidence);
            checkInterrupted();if(complete)return true;
        }catch(UnsatisfiedLinkError unavailable){nativeState=-1;}
        processJavaRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,true,workspace,policy,confidence);return true;
    }
    static void processJavaRange(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        processJavaRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,true);
    }
    static void processJavaRangeLocalOnly(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        processJavaRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,false);
    }
    private static void processJavaRangeInternal(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,boolean useNlm) {
        Workspace workspace=new Workspace();try{processJavaRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,useNlm,workspace,null,null);}finally{workspace.close();}
    }
    private static void processJavaRangeInternal(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,boolean useNlm,Workspace workspace,int[] policy,int[] confidenceMask) {
        validate(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,model);checkInterrupted();
        if(noise==0||!model.active()){System.arraycopy(input,begin*width,output,begin*width,(end-begin)*width);return;}
        float[] coarse=workspace.coarse,flat=workspace.flat,nl=workspace.nl;
        ColourRows colours=new ColourRows(input,width,validBegin,validEnd,begin,5,workspace);
        for(int row=begin;row<end;row++) {
            checkInterrupted();colours.advance(row);int ay=row+originY;
            for(int x=0;x<width;x++) {
                int at=row*width+x,p=input[at];if((p>>>24)!=255){output[at]=p;continue;}
                int policyAt=((row-begin)*width+x)*2;int budget=policy!=null?policy[policyAt]:protection==null?256:clamp(protection.budgetQ8(x,ay),0,256),detail=policy!=null?policy[policyAt+1]:protection==null?0:clamp(protection.detailQ8(x,ay),0,256);
                if(budget==0){output[at]=p;continue;}
                float y0=colours.y(x,row),r0=colours.r(x,row),b0=colours.b(x,row);
                float sy=regionalSigma(model.runtimeEvidence,width,model.height,x,ay,0),sc=regionalSigma(model.runtimeEvidence,width,model.height,x,ay,1);
                guide(input,width,rows,x,row,validBegin,validEnd,sy,sc,flat,colours);
                if(confidenceMask!=null&&x==Math.min(width-1,(x/4)*4+1)&&ay==Math.min(model.height-1,(ay/4)*4+1))confidenceMask[((row-begin)/4)*((width+3)/4)+x/4]=Math.round(256*clamp((flat[0]-.65f)/.30f,0,1)*clamp((Math.max(sy,sc)-.60f)/1.8f,0,1)*(1-regionalSigma(model.runtimeEvidence,width,model.height,x,ay,2)));
                float dark=clamp((144-y0)/112,0,1),thresholdY=Math.max(2.5f,sy*4),thresholdC=Math.max(5,sc*5);
                float sumY=0,sumR=0,sumB=0,wyTotal=0,wcTotal=0,var=0;
                for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
                    int cy=clamp(row+dy,validBegin,validEnd-1),cx=clamp(x+dx,0,width-1);int q=input[cy*width+cx];if((q>>>24)!=255)continue;
                    float yy=colours.y(cx,cy),rr=colours.r(cx,cy),bb=colours.b(cx,cy),base=dx==0&&dy==0?4f:Math.abs(dx)<=1&&Math.abs(dy)<=1?2f:1f;
                    float wy=base/(1+square((yy-y0)/thresholdY)),wc=base/(1+.12f*square((yy-y0)/thresholdY)+square((rr-r0)/thresholdC)+square((bb-b0)/thresholdC));
                    sumY+=wy*yy;wyTotal+=wy;var+=wy*square(yy-y0);sumR+=wc*rr;sumB+=wc*bb;wcTotal+=wc;
                }
                float strength=(.56f+.09f*noise+(shadows?.12f*dark:0))*((160+budget*.375f)/256f),texture=(1-.55f*detail/256f)*(1-.85f*regionalSigma(model.runtimeEvidence,width,model.height,x,ay,2));
                float confidence=clamp((flat[0]-.62f)/.34f,0,1),residual=clamp((Math.max(sy,sc)-.60f)/3f,0,1);
                float ly=sy>=.40f?Math.min(.98f,strength*(.70f+.30f*confidence*residual))*flat[0]*texture*clamp(sy*sy/Math.max(sy*sy,var/Math.max(.001f,wyTotal)*.55f),.16f,1):0;
                float lc=sc>=.40f?Math.min(.995f,strength+.15f*residual)*flat[1]*texture:0;
                float targetY=sumY/Math.max(.001f,wyTotal),targetR=sumR/Math.max(.001f,wcTotal),targetB=sumB/Math.max(.001f,wcTotal);
                if(useNlm&&confidence>.45f&&residual>.15f&&detail<192&&texture>.6f) {
                    nonlocalFine(input,width,rows,x,row,validBegin,validEnd,sy,sc,nl,colours);
                    float by=.84f*confidence*residual,bc=.88f*confidence*residual*flat[1];
                    targetY=targetY*(1-by)+nl[0]*by;targetR=targetR*(1-bc)+nl[1]*bc;targetB=targetB*(1-bc)+nl[2]*bc;
                    ly=Math.min(.985f,ly+.18f*confidence*residual*texture);lc=Math.min(.995f,lc+.10f*confidence*residual*texture);
                }
                float dy=(targetY-y0)*ly,dr=(targetR-r0)*lc,db=(targetB-b0)*lc;
                float dR=dy+dr,dG=dy-(.299f*dr+.114f*db)/.587f,dB=dy+db;
                for(int k=0;k<3;k++) {
                    int scale=1<<(k+1);int[] map=k==0?model.halfMap:k==1?model.quarterMap:model.eighthMap;
                    sample(map,halfWidth(model.width,scale),halfWidth(model.height,scale),x,ay,scale,coarse);
                    float gate=1/(1+square((coarse[3]-y0)/(sy*5+16)));
                    float blend=(k==0?.48f:k==1?.32f:.22f)*Math.min(1,strength)*texture*gate;
                    float cy=.299f*coarse[0]+.587f*coarse[1]+.114f*coarse[2],cr=coarse[0]-cy,cb=coarse[2]-cy;
                    float by=blend*(.30f+.70f*flat[0]),bc=blend*flat[1];
                    dR+=cy*by+cr*bc;dG+=cy*by-(.299f*cr+.114f*cb)/.587f*bc;dB+=cy*by+cb*bc;
                }
                output[at]=(p&0xff000000)|(byteValue(((p>>>16)&255)+dR)<<16)|(byteValue(((p>>>8)&255)+dG)<<8)|byteValue((p&255)+dB);
            }
        }
    }
    private static void sample(int[] map,int width,int height,int x,int y,int scale,float[] out) {
        float fx=clamp((x+.5f)/scale-.5f,0,width-1),fy=clamp((y+.5f)/scale-.5f,0,height-1);int ix=(int)fx,iy=(int)fy;
        int nx=Math.min(width-1,ix+1),ny=Math.min(height-1,iy+1);fx-=ix;fy-=iy;
        int a=map[iy*width+ix],b=map[iy*width+nx],c=map[ny*width+ix],d=map[ny*width+nx];
        for(int plane=0;plane<4;plane++) {
            int shift=plane==3?24:16-plane*8;
            float av=plane==3?a>>>24:(byte)(a>>>shift),bv=plane==3?b>>>24:(byte)(b>>>shift),cv=plane==3?c>>>24:(byte)(c>>>shift),dv=plane==3?d>>>24:(byte)(d>>>shift);
            out[plane]=(av*(1-fx)+bv*fx)*(1-fy)+(cv*(1-fx)+dv*fx)*fy;
        }
    }
    /** The guide averages coherent opposite-side strips before testing edges.
     * A measured random-grain floor is removed; colour has its own boundary gate. */
    private static float structure(int[] src,int width,int height,int x,int y,int validBegin,int validEnd,ColourRows colours) {
        float left=0,right=0,up=0,down=0;int n=0;
        for(int k=-1;k<=1;k++) {
            int a=src[clamp(y+k,validBegin,validEnd-1)*width+clamp(x-2,0,width-1)],b=src[clamp(y+k,validBegin,validEnd-1)*width+clamp(x+2,0,width-1)];
            int c=src[clamp(y-2,validBegin,validEnd-1)*width+clamp(x+k,0,width-1)],d=src[clamp(y+2,validBegin,validEnd-1)*width+clamp(x+k,0,width-1)];
            if((a>>>24)!=255||(b>>>24)!=255||(c>>>24)!=255||(d>>>24)!=255)return 255;
            left+=colours.y(clamp(x-2,0,width-1),clamp(y+k,validBegin,validEnd-1));right+=colours.y(clamp(x+2,0,width-1),clamp(y+k,validBegin,validEnd-1));up+=colours.y(clamp(x+k,0,width-1),clamp(y-2,validBegin,validEnd-1));down+=colours.y(clamp(x+k,0,width-1),clamp(y+2,validBegin,validEnd-1));n++;
        }
        return (Math.abs(right-left)+Math.abs(down-up))/n;
    }
    private static void guide(int[] src,int width,int height,int x,int y,int validBegin,int validEnd,float sy,float sc,float[] out) {
        guide(src,width,height,x,y,validBegin,validEnd,sy,sc,out,null);
    }
    private static void guide(int[] src,int width,int height,int x,int y,int validBegin,int validEnd,float sy,float sc,float[] out,ColourRows colours) {
        float ly=0,ry=0,uy=0,dy=0,lr=0,rr=0,ur=0,dr=0,lb=0,rb=0,ub=0,db=0;
        for(int k=-1;k<=1;k++)for(int d=2;d<=3;d++) {
            int a=src[clamp(y+k,validBegin,validEnd-1)*width+clamp(x-d,0,width-1)],b=src[clamp(y+k,validBegin,validEnd-1)*width+clamp(x+d,0,width-1)];
            int c=src[clamp(y-d,validBegin,validEnd-1)*width+clamp(x+k,0,width-1)],e=src[clamp(y+d,validBegin,validEnd-1)*width+clamp(x+k,0,width-1)];
            if((a>>>24)!=255||(b>>>24)!=255||(c>>>24)!=255||(e>>>24)!=255){out[0]=out[1]=0;return;}
            float ya=colours==null?luma(a):colours.y(clamp(x-d,0,width-1),clamp(y+k,validBegin,validEnd-1)),yb=colours==null?luma(b):colours.y(clamp(x+d,0,width-1),clamp(y+k,validBegin,validEnd-1)),yc=colours==null?luma(c):colours.y(clamp(x+k,0,width-1),clamp(y-d,validBegin,validEnd-1)),ye=colours==null?luma(e):colours.y(clamp(x+k,0,width-1),clamp(y+d,validBegin,validEnd-1));ly+=ya;ry+=yb;uy+=yc;dy+=ye;
            lr+=colours==null?((a>>>16)&255)-ya:colours.r(clamp(x-d,0,width-1),clamp(y+k,validBegin,validEnd-1));rr+=colours==null?((b>>>16)&255)-yb:colours.r(clamp(x+d,0,width-1),clamp(y+k,validBegin,validEnd-1));ur+=colours==null?((c>>>16)&255)-yc:colours.r(clamp(x+k,0,width-1),clamp(y-d,validBegin,validEnd-1));dr+=colours==null?((e>>>16)&255)-ye:colours.r(clamp(x+k,0,width-1),clamp(y+d,validBegin,validEnd-1));
            lb+=colours==null?(a&255)-ya:colours.b(clamp(x-d,0,width-1),clamp(y+k,validBegin,validEnd-1));rb+=colours==null?(b&255)-yb:colours.b(clamp(x+d,0,width-1),clamp(y+k,validBegin,validEnd-1));ub+=colours==null?(c&255)-yc:colours.b(clamp(x+k,0,width-1),clamp(y-d,validBegin,validEnd-1));db+=colours==null?(e&255)-ye:colours.b(clamp(x+k,0,width-1),clamp(y+d,validBegin,validEnd-1));
        }
        float centerY=0,centerR=0,centerB=0;
        for(int py=-1;py<=1;py++)for(int px=-1;px<=1;px++) {int cy=clamp(y+py,validBegin,validEnd-1),cx=clamp(x+px,0,width-1),p=src[cy*width+cx];if((p>>>24)!=255){out[0]=out[1]=0;return;}float yy=colours==null?luma(p):colours.y(cx,cy);centerY+=yy;centerR+=colours==null?((p>>>16)&255)-yy:colours.r(cx,cy);centerB+=colours==null?(p&255)-yy:colours.b(cx,cy);}
        float curveY=Math.max(Math.abs(ly+ry-centerY*4/3),Math.abs(uy+dy-centerY*4/3))/6;
        float curveC=Math.max(Math.abs(lr+rr-centerR*4/3)+Math.abs(lb+rb-centerB*4/3),Math.abs(ur+dr-centerR*4/3)+Math.abs(ub+db-centerB*4/3))/6;
        float edgeY=Math.max(Math.abs(ry-ly),Math.abs(dy-uy))/6;
        float edgeC=Math.max(Math.abs(rr-lr)+Math.abs(rb-lb),Math.abs(dr-ur)+Math.abs(db-ub))/6;
        edgeY=Math.max(0,edgeY-sy*.85f);edgeC=Math.max(0,edgeC-sc*2.0f);
        out[0]=1/(1+square(edgeY/(sy*1.7f+2))+square(Math.max(0,curveY-sy*1.4f)/(sy*.85f+1)));out[1]=1/(1+square(edgeC/(sc*1.0f+1.5f))+square(Math.max(0,curveC-sc*2.5f)/(sc*1.5f+2)));
    }
    private static void estimateRegions(Patches source,final int width,final int height,float[] runtime,float[] ev,boolean useGpu) {
        final float[] sourceEvidence=Arrays.copyOf(ev,16);
        float[] values=useGpu?GpuAnalysis1961.regions(source,width,height,sourceEvidence,new GpuAnalysis1961.RegionsCpu(){
            public float[] compute(Patches detached){return estimateRegionsSnapshot1961(detached,width,height,sourceEvidence);}
        }):estimateRegionsSnapshot1961(source,width,height,sourceEvidence);
        System.arraycopy(values,0,runtime,16,values.length);
    }
    static float[] estimateRegionsSnapshot1961(Patches source,int width,int height,float[] ev) {
        float[] runtime=new float[16+3*((width+REGION-1)/REGION)*((height+REGION-1)/REGION)];
        estimateRegionsCpu1961(source,width,height,runtime,ev);return Arrays.copyOfRange(runtime,16,runtime.length);
    }
    /** Frozen regional histogram and signed autocorrelation traversal. */
    private static void estimateRegionsCpu1961(Patches source,int width,int height,float[] runtime,float[] ev) {
        int rw=(width+REGION-1)/REGION,rh=(height+REGION-1)/REGION,pw=Math.min(32,width),ph=Math.min(32,height);int[] patch=new int[pw*ph];
        int[][] hist=new int[16][128];int[] counts=new int[16];float[] measured=new float[16];
        for(int gy=0;gy<rh;gy++)for(int gx=0;gx<rw;gx++) {
            checkInterrupted();int px=clamp(gx*REGION+REGION/2-pw/2,0,width-pw),py=clamp(gy*REGION+REGION/2-ph/2,0,height-ph);source.read(patch,px,py,pw,ph);
            clearEstimate(hist,counts,measured);estimateSamples(patch,pw,ph,hist,counts,1);finishEstimate(hist,counts,measured,0);
            float sumY=0,sumC=0,total=0;for(int b=0;b<8;b++){sumY+=measured[b]*counts[b];sumC+=measured[b+8]*counts[b+8];total+=counts[b];}
            int at=16+3*(gy*rw+gx);runtime[at]=total>=16?sumY/total:average(ev,0);runtime[at+1]=total>=16?sumC/total:average(ev,8);runtime[at+2]=periodicTexture(patch,pw,ph);
            // The wider Haar residual admits correlated grain that is nearly
            // invisible to a one-pixel estimate. Real periodic structure is
            // independently protected by the measured signed texture prior.
            clearEstimate(hist,counts,measured);estimateLag(patch,pw,ph,hist,counts,1,4);finishEstimate(hist,counts,measured,0);
            sumY=0;sumC=0;total=0;for(int b=0;b<8;b++){sumY+=measured[b]*counts[b];sumC+=measured[b+8]*counts[b+8];total+=counts[b];}
            if(total>=16){runtime[at]=Math.max(runtime[at],.65f*sumY/total);runtime[at+1]=Math.max(runtime[at+1],.65f*sumC/total);}
        }
    }
    private static void clearEstimate(int[][] hist,int[] counts,float[] measured){for(int[] bins:hist)Arrays.fill(bins,0);Arrays.fill(counts,0);Arrays.fill(measured,0);}
    /** Repeated signed structure has a negative spatial autocorrelation lobe.
     * White grain has none; this measured regional prior prevents faint periodic
     * detail being reclassified merely because its gradient is locally zero. */
    private static float periodicTexture(int[] patch,int width,int height) {
        if(width<24||height<24)return 0;float mean=0,energy=0;int count=0;
        for(int p:patch)if((p>>>24)==255){mean+=luma(p);count++;}if(count<width*height*.9f)return 0;mean/=count;
        for(int p:patch)if((p>>>24)==255)energy+=square(luma(p)-mean);energy/=count;if(energy<.50f)return 0;
        float lowest=0;
        for(int lag=8;lag<=12;lag+=4)for(int direction=0;direction<3;direction++) {
            int dx=direction==1?0:lag,dy=direction==0?0:lag;float covariance=0;int n=0;
            for(int y=0;y+dy<height;y++)for(int x=0;x+dx<width;x++){int a=patch[y*width+x],b=patch[(y+dy)*width+x+dx];if((a>>>24)!=255||(b>>>24)!=255)continue;covariance+=(luma(a)-mean)*(luma(b)-mean);n++;}
            if(n>=64)lowest=Math.min(lowest,covariance/n/energy);
        }
        return clamp((-lowest-.18f)/.30f,0,1);
    }
    private static float regionalSigma(float[] ev,int width,int height,int x,int y,int plane) {
        int rw=(width+REGION-1)/REGION,rh=(height+REGION-1)/REGION;float fx=clamp((x+.5f)/REGION-.5f,0,rw-1),fy=clamp((y+.5f)/REGION-.5f,0,rh-1);int ix=(int)fx,iy=(int)fy,nx=Math.min(rw-1,ix+1),ny=Math.min(rh-1,iy+1);fx-=ix;fy-=iy;
        float a=ev[16+3*(iy*rw+ix)+plane],b=ev[16+3*(iy*rw+nx)+plane],c=ev[16+3*(ny*rw+ix)+plane],d=ev[16+3*(ny*rw+nx)+plane];return (a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy;
    }
    /** Eight same-image candidates, radius 4 with a five-point cross patch. The
     * furthest read is five pixels; no candidate can cross the valid halo. */
    private static void nonlocalFine(int[] src,int width,int height,int x,int y,int validBegin,int validEnd,float sy,float sc,float[] result,ColourRows colours) {
        int p=src[y*width+x];float yy=colours.y(x,y),rr=colours.r(x,y),bb=colours.b(x,y),total=1,sumY=yy,sumR=rr,sumB=bb;
        float variance=sy*sy+.22f*sc*sc,h2=Math.max(1.5f,variance*3.2f);
        for(int dy=-4;dy<=4;dy+=4)for(int dx=-4;dx<=4;dx+=4) {
            if(dx==0&&dy==0)continue;int cx=x+dx,cy=y+dy;if(cx<0||cy<validBegin||cx>=width||cy>=validEnd)continue;float distance=0,patchY=0,patchR=0,patchB=0;boolean valid=true;
            for(int k=0;k<5;k++) {
                int px=k==1?-1:k==2?1:0,py=k==3?-1:k==4?1:0;
                int ax=clamp(x+px,0,width-1),ay=clamp(y+py,validBegin,validEnd-1),bx=clamp(cx+px,0,width-1),by=clamp(cy+py,validBegin,validEnd-1);int a=src[ay*width+ax],b=src[by*width+bx];
                if((a>>>24)!=255||(b>>>24)!=255){valid=false;continue;}
                float ya=colours.y(ax,ay),yb=colours.y(bx,by),ra=colours.r(ax,ay),rb=colours.r(bx,by),ba=colours.b(ax,ay),bc=colours.b(bx,by);distance+=square(ya-yb)+.11f*(square(ra-rb)+square(ba-bc));patchY+=yb;patchR+=rb;patchB+=bc;
            }
            if(!valid)continue;distance=Math.max(0,distance/5-2*variance);float z=distance/h2;if(z>5)continue;float weight=1/(1+z+z*z*.5f+z*z*z/6);
            sumY+=weight*(patchY/5);sumR+=weight*(patchR/5);sumB+=weight*(patchB/5);total+=weight;
        }
        result[0]=sumY/total;result[1]=sumR/total;result[2]=sumB/total;
    }
    private static void validate(int[] in,int[] out,int width,int rows,int begin,int end,int validBegin,int validEnd,int origin,int noise,Model model) {
        if(in==null||out==null||in==out||model==null||width<1||rows<1||noise<0||noise>4||begin<validBegin||end>validEnd||begin>end||validBegin<0||validEnd>rows||validBegin>=validEnd||in.length<(long)width*rows||out.length<(long)width*rows||model.width!=width||(long)origin+validBegin<0||(long)origin+validEnd>model.height)throw new IllegalArgumentException("strong NR range");
        int absBegin=origin+begin,absEnd=origin+end;
        if(absBegin>0&&begin-validBegin<Math.min(HALO,absBegin)||absEnd<model.height&&validEnd-end<Math.min(HALO,model.height-absEnd))throw new IllegalArgumentException("strong NR missing halo");
    }
    static boolean nativeAvailable() {
        if(nativeState==0)synchronized(StrongNoise1958.class){if(nativeState==0)try{
            System.loadLibrary("ulike_smooth1958");nativeState=nativeAbi()==1958?1:-1;
            if(nativeState==1){nativeScratchBytes();SpeedWorkers1935.installScratchMemory1959(new SpeedWorkers1935.ScratchMemory(){
                public long retainedBytes(){if(nativeState!=1)return 0;try{return nativeScratchBytes();}catch(LinkageError unavailable){nativeState=-1;return 0;}}
                public void trim(){if(nativeState==1)try{nativeReleaseScratch();}catch(LinkageError unavailable){nativeState=-1;}}
            });}
        }catch(LinkageError unavailable){nativeState=-1;}
         catch(SecurityException unavailable){nativeState=-1;}}
        return nativeState==1;
    }
    private static native int nativeAbi();
    private static native boolean processNative(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,float[] evidence,int[] half,int[] quarter,int[] eighth,int fullWidth,int fullHeight,int[] policy,int mode);
    private static native boolean processNativeShared(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,float[] evidence,int[] half,int[] quarter,int[] eighth,int fullWidth,int fullHeight,int[] policy,int mode,int[] confidence);
    private static native long nativeScratchBytes();
    private static native void nativeReleaseScratch();
    private static int pack(int guide,float r,float g,float b){return clamp(guide,0,255)<<24|((clamp(Math.round(r),-127,127)&255)<<16)|((clamp(Math.round(g),-127,127)&255)<<8)|(clamp(Math.round(b),-127,127)&255);}
    private static float sigma(float[] values,int offset,float mean){float x=clamp((mean-16)/32,0,7);int a=(int)x,b=Math.min(7,a+1);return values[offset+a]*(1-(x-a))+values[offset+b]*(x-a);}
    private static float average(float[] values,int offset){float sum=0;for(int i=0;i<8;i++)sum+=values[offset+i];return sum/8;}
    private static int halfWidth(int value,int scale){for(int i=scale;i>1;i/=2)value=half(value);return value;}
    private static int half(int value){return value/2+(value&1);}
    private static void geometry(int width,int height){if(width<1||height<1||width>Integer.MAX_VALUE-REGION||height>Integer.MAX_VALUE-REGION||pixels(width,height)>Integer.MAX_VALUE)throw new IllegalArgumentException("strong NR geometry");}
    private static long pixels(int width,int height){return (long)width*height;}
    private static int checkedPixels(int width,int height){long count=pixels(width,height);if(count<0||count>Integer.MAX_VALUE)throw new IllegalArgumentException("strong NR array");return (int)count;}
    private static int checkedElements(long count){if(count<0||count>Integer.MAX_VALUE)throw new IllegalArgumentException("strong NR buffer");return (int)count;}
    private static float luma(int p){return .299f*((p>>>16)&255)+.587f*((p>>>8)&255)+.114f*(p&255);}
    private static float square(float v){return v*v;}
    private static float[][] dctBasis(){float[][] basis=new float[8][8];for(int k=0;k<8;k++)for(int x=0;x<8;x++)basis[k][x]=(float)((k==0?Math.sqrt(1.0/8):.5)*Math.cos(Math.PI*(2*x+1)*k/16));return basis;}
    private static int byteValue(float v){return clamp(Math.round(v),0,255);}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private static void checkInterrupted(){if(Thread.currentThread().isInterrupted())throw new CancellationException("strong single-image NR interrupted");}
}
