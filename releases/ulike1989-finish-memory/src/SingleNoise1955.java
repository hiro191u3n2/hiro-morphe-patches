package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Single-frame, image-domain NR. The two DCT scales always read the same immutable
 * processed image; they are alternative estimates, never successive blur passes.
 * This class has no Android dependency and is also the native backend's oracle.
 */
public final class SingleNoise1955 {
    private SingleNoise1955() {}
    public static final int HALO=7;
    private static final int BATCH_ROWS=64, BINS=8, BANDS=3, HIST=1024;
    private static volatile int nativeState;
    private static final double[][] D4=basis(4), D8=basis(8);
    private static final float[] WINDOW8={.03806023f,.30865828f,.6913417f,.96193975f,
        .96193975f,.6913417f,.30865828f,.03806023f};
    public interface Patches {void read(int[] pixels,int x,int y,int width,int height);}
    public interface Protection {int budgetQ8(int x,int y);int detailQ8(int x,int y);}
    /** Additional reservation beyond the caller's source/destination halo arrays.
     * Covers Java policy, JNI optional policy/input copies, transactional native
     * result, the 64-row float accumulator, and small transform/model temporaries.
     */
    public static long workspaceBytes(int width,int coreRows) {
        if(width<1||coreRows<1)throw new IllegalArgumentException("single NR workspace geometry");
        return (long)width*(Math.min(BATCH_ROWS,coreRows)*16L+coreRows*24L+HALO*8L)+65536L+
            GpuSingle1960.workspaceBytes(width,coreRows);
    }

    /** Small immutable per-shot evidence. Units are 8-bit processed RGB code values,
     * not RAW sensor variance. Quantization-clean and unsupported patches stay clean.
     */
    public static final class Model {
        public final int width,height,columns,rows,samples;
        private final float[] luma,chroma,mean;
        private final float[] brightnessY,brightnessC,ratios;
        private final float stepX,stepY,left,top,globalY,globalC;
        private final int patchWidth,patchHeight;
        private final float[] nativeData;
        private Model(int w,int h,int columns,int rows,float[] luma,float[] chroma,
                float[] mean,float[] brightnessY,float[] brightnessC,float[] ratios,int samples,
                int patchW,int patchH) {
            this.width=w;this.height=h;this.columns=columns;this.rows=rows;this.samples=samples;
            this.luma=luma;this.chroma=chroma;this.mean=mean;
            this.brightnessY=brightnessY;this.brightnessC=brightnessC;this.ratios=ratios;
            left=(patchW-1)*.5f;top=(patchH-1)*.5f;
            stepX=columns==1?1:(float)(w-patchW)/(columns-1);
            stepY=rows==1?1:(float)(h-patchH)/(rows-1);
            float sy=0,sc=0;for(int i=0;i<luma.length;i++){sy+=luma[i];sc+=chroma[i];}
            globalY=sy/luma.length;globalC=sc/chroma.length;
            patchWidth=patchW;patchHeight=patchH;
            nativeData=new float[luma.length*3+16+72];int offset=0;
            System.arraycopy(luma,0,nativeData,offset,luma.length);offset+=luma.length;
            System.arraycopy(chroma,0,nativeData,offset,chroma.length);offset+=chroma.length;
            System.arraycopy(mean,0,nativeData,offset,mean.length);offset+=mean.length;
            System.arraycopy(brightnessY,0,nativeData,offset,8);offset+=8;
            System.arraycopy(brightnessC,0,nativeData,offset,8);offset+=8;
            System.arraycopy(ratios,0,nativeData,offset,72);
        }
        public float sigmaAt(int x,int y){return interpolate(luma,x,y);}
        public float chromaSigmaAt(int x,int y){return interpolate(chroma,x,y);}
        public float meanSigma(){return globalY;}
        public float meanChromaSigma(){return globalC;}
        /* Immutable shot evidence shared with the true-fp64 GPU backend. */
        float[] gpuData1960(){return nativeData;}
        int gpuPatchWidth1960(){return patchWidth;}
        int gpuPatchHeight1960(){return patchHeight;}
        private float interpolate(float[] values,float x,float y) {
            float xx=columns==1?0:clamp((x-left)/stepX,0,columns-1);
            float yy=rows==1?0:clamp((y-top)/stepY,0,rows-1);
            int ix=(int)xx,iy=(int)yy,nx=Math.min(columns-1,ix+1),ny=Math.min(rows-1,iy+1);
            float fx=xx-ix,fy=yy-iy;
            float a=values[iy*columns+ix]*(1-fx)+values[iy*columns+nx]*fx;
            float b=values[ny*columns+ix]*(1-fx)+values[ny*columns+nx]*fx;
            return a*(1-fy)+b*fy;
        }
        private float sigma(int x,int y,float blockMean,boolean color) {
            float base=interpolate(color?chroma:luma,x,y);
            if(base<.30f)return 0;
            float sampledMean=interpolate(mean,x,y);
            float[] b=color?brightnessC:brightnessY;
            float reference=brightness(b,sampledMean), current=brightness(b,blockMean);
            // The shot's own brightness bins may adjust only existing evidence.
            // An unmeasured bright/dark bin cannot force NR into a clean area.
            return base*clamp(reference>.3f?current/reference:1f,.65f,1.55f);
        }
        private float spectral(int plane,int band,float blockMean) {
            int bin=Math.max(0,Math.min(BINS-1,(int)blockMean/32));
            return ratios[(bin*3+plane)*BANDS+band];
        }
    }

    /** NR1: spatial and luminance-binned robust transform noise spectra. Flat block
     * selection rejects clipping/strong edges; coefficient medians reject sparse
     * lettering and regular wallpaper. No thumbnail or other frame is sampled.
     */
    public static Model probe(Patches source,int width,int height) {
        return CpuSingle1978.probe(source,width,height);
    }
    static Model probe1978(Patches source,int width,int height,boolean reused) {
        if(source==null||width<1||height<1)throw new IllegalArgumentException("single NR source");
        checkInterrupted();
        int pw=Math.min(48,width),ph=Math.min(48,height);
        int columns=Math.max(1,Math.min(13,(width+255)/256));
        int rows=Math.max(1,Math.min(13,(height+255)/256));
        float[] sy=new float[columns*rows],sc=new float[sy.length],means=new float[sy.length];
        int[][] global=new int[BINS*3*BANDS][HIST];int[] counts=new int[global.length];
        int[] patch=new int[pw*ph];double[][] planes={new double[64],new double[64],new double[64]};
        double[] work=new double[64];int accepted=0;
        int[][] retainedLocal=reused?new int[2][HIST]:null;int[] retainedCount=reused?new int[2]:null;
        for(int gy=0;gy<rows;gy++)for(int gx=0;gx<columns;gx++) {
            checkInterrupted();
            int px=columns==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(columns-1));
            int py=rows==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(rows-1));
            source.read(patch,px,py,pw,ph);
            int[][] local=reused?retainedLocal:new int[2][HIST];int[] localCount=reused?retainedCount:new int[2];
            if(reused){Arrays.fill(local[0],0);Arrays.fill(local[1],0);Arrays.fill(localCount,0);}
            double sumMean=0;int blocks=0;
            if(pw>=8&&ph>=8)for(int by=0;by+8<=ph;by+=8)for(int bx=0;bx+8<=pw;bx+=8) {
                boolean opaque=true;double m=0,min=255,max=0;
                for(int yy=0;yy<8;yy++)for(int xx=0;xx<8;xx++) {
                    int p=patch[(by+yy)*pw+bx+xx],at=yy*8+xx;
                    if((p>>>24)!=255)opaque=false;
                    double y=luma(p);planes[0][at]=y;planes[1][at]=((p>>>16)&255)-y;planes[2][at]=(p&255)-y;
                    m+=y;min=Math.min(min,y);max=Math.max(max,y);
                }
                m/=64;sumMean+=m;blocks++;
                if(!opaque||m<3||m>252||max-min>100)continue;
                // Opposite half means measure coherent edges without mistaking a
                // single high/low noise pixel for the outline of a real object.
                double dx=0,dy=0;for(int yy=0;yy<8;yy++)for(int xx=0;xx<8;xx++) {
                    double v=planes[0][yy*8+xx];dx+=(xx<4?-v:v);dy+=(yy<4?-v:v);
                }
                if(Math.abs(dx)/32>18||Math.abs(dy)/32>18)continue;
                int bin=Math.max(0,Math.min(BINS-1,(int)m/32));
                for(int plane=0;plane<3;plane++) {
                    transform(planes[plane],work,8,false);
                    for(int v=0;v<8;v++)for(int u=0;u<8;u++) {
                        int frequency=u+v;if(frequency<2)continue;
                        int band=frequency<4?0:frequency<8?1:2;
                        int value=Math.min(HIST-1,(int)Math.round(Math.abs(planes[plane][v*8+u])*4));
                        int slot=(bin*3+plane)*BANDS+band;
                        global[slot][value]++;counts[slot]++;
                        if(frequency>=4){int p=plane==0?0:1;local[p][value]++;localCount[p]++;}
                    }
                }
                accepted++;
            }
            int at=gy*columns+gx;
            sy[at]=localCount[0]>=32?Math.min(32,median(local[0],localCount[0])/2.69796f):0;
            sc[at]=localCount[1]>=64?Math.min(48,median(local[1],localCount[1])/2.69796f):0;
            means[at]=blocks==0?128:(float)(sumMean/blocks);
        }
        float[] by=new float[BINS],bc=new float[BINS],ratios=new float[BINS*3*BANDS];
        Arrays.fill(ratios,1f);
        for(int bin=0;bin<BINS;bin++) {
            int yslot=(bin*3)*BANDS+1,rslot=(bin*3+1)*BANDS+1,bslot=(bin*3+2)*BANDS+1;
            by[bin]=sigma(global[yslot],counts[yslot]);
            bc[bin]=(sigma(global[rslot],counts[rslot])+sigma(global[bslot],counts[bslot]))*.5f;
            for(int plane=0;plane<3;plane++) {
                int refslot=(bin*3+plane)*BANDS+1;float reference=sigma(global[refslot],counts[refslot]);
                for(int band=0;band<BANDS;band++) {
                    int slot=(bin*3+plane)*BANDS+band;float measured=sigma(global[slot],counts[slot]);
                    // Variance shape accounts for correlated ISP grain. The lowest
                    // band is capped to keep lighting gradients and broad detail.
                    float ratio=reference>.30f&&counts[slot]>=24?measured/reference:1f;
                    ratios[slot]=clamp(ratio,band==0?.5f:.45f,band==0?1.7f:2.4f);
                }
            }
        }
        fillMissing(by);fillMissing(bc);
        return new Model(width,height,columns,rows,sy,sc,means,by,bc,ratios,accepted,pw,ph);
    }
    private static float sigma(int[] hist,int count){return count>=24?Math.min(48,median(hist,count)/2.69796f):0;}
    private static int median(int[] hist,int count){int n=0,target=(count+1)/2;for(int i=0;i<hist.length;i++){n+=hist[i];if(n>=target)return i;}return 0;}
    private static void fillMissing(float[] values){for(int i=0;i<values.length;i++)if(values[i]==0){int best=-1,d=99;for(int j=0;j<values.length;j++)if(values[j]>0&&Math.abs(i-j)<d){best=j;d=Math.abs(i-j);}if(best>=0)values[i]=values[best];}}
    private static float brightness(float[] values,float mean){float x=clamp((mean-16)/32,0,BINS-1);int a=(int)x,b=Math.min(BINS-1,a+1);return values[a]*(1-(x-a))+values[b]*(x-a);}

    /** NR2/NR3. begin/end and validBegin/validEnd are local row coordinates;
     * originY is the absolute Y of input[0]. HALO source rows are mandatory except
     * at physical image borders. Caller must commit only after successful return.
     * Nonopaque pixels are preserved and are never evidence for neighboring blocks.
     */
    public static void processRange(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        validate(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,model);
        checkInterrupted();
        if(noise>0&&end>begin&&(model.meanSigma()>=.30f||model.meanChromaSigma()>=.30f)&&
                GpuSingle1960.process(input,output,width,rows,begin,end,validBegin,validEnd,
                    originY,noise,shadows,model,protection))return;
        processCpuRange(input,output,width,rows,begin,end,validBegin,validEnd,
            originY,noise,shadows,model,protection);
    }
    /** One exclusive lifetime for the existing pipeline worker. Input, model and
     * callback objects are never retained here; all logical prefixes are reset. */
    public static final class Workspace implements AutoCloseable {
        private Kernel javaKernel;private int kernelWidth;private int[] policy;
        private long nativeHandle;private int nativeWidth,nativeRows;
        private boolean inUse,closed;
        // A true GPU facade result may be its already-completed CPU fallback.
        // This scalar describes only the output selected by the enclosing call.
        int selectedBackend1982=-1;
        // Reset by each enclosing strip. The primary handoff is independent
        // of the final Residual/CPU selection and never aliases its cause.
        int selectedReason1989,primaryGpuReason1989;
        public Workspace() {}
        synchronized void acquire(){if(closed||inUse)throw new IllegalStateException("single NR workspace lease");inUse=true;}
        synchronized void relinquish(){inUse=false;}
        int[] policy(int count){if(policy==null||policy.length<count){policy=new int[count];}return policy;}
        private Kernel kernel(int width,int rows){dropNative();if(javaKernel==null||kernelWidth!=width||javaKernel.weight.length<(long)width*rows){javaKernel=new Kernel(width,rows);kernelWidth=width;}return javaKernel;}
        long nativeHandle(int width,int rows){javaKernel=null;kernelWidth=0;if(nativeHandle!=0&&(nativeWidth!=width||nativeRows<rows))dropNative();if(nativeHandle==0){nativeHandle=CpuSingle1978.create(width,rows);if(nativeHandle!=0){nativeWidth=width;nativeRows=rows;}}return nativeHandle;}
        void dropNative(){if(nativeHandle!=0){CpuSingle1978.release(nativeHandle,nativeWidth,nativeRows);nativeHandle=0;nativeWidth=nativeRows=0;}}
        void original(){dropNative();javaKernel=null;kernelWidth=0;policy=null;}
        public synchronized void close(){if(inUse)throw new IllegalStateException("single NR workspace still leased");if(closed)return;original();closed=true;}
    }
    public static void processRange(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,Workspace workspace) {
        if(workspace==null){processRange(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection);return;}
        validate(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,model);checkInterrupted();workspace.acquire();
        try {
            workspace.selectedBackend1982=-1;
            workspace.selectedReason1989=PipelineDetail1988.CPU_FIXED;
            workspace.primaryGpuReason1989=PipelineDetail1988.UNKNOWN;
            if(noise>0&&end>begin&&(model.meanSigma()>=.30f||model.meanChromaSigma()>=.30f)&&
                    GpuSingle1960.process1981(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,workspace))return;
            processCpuRange1978(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,workspace);
            workspace.selectedBackend1982=0;
        }finally{workspace.relinquish();}
    }
    /* Original native/Java route remains both the fallback and frozen oracle. */
    static void processCpuRange(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        processCpuRange1978(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,null);
    }
    static void processCpuRange1978(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,Workspace workspace) {
        validate(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,model);
        checkInterrupted();
        if(noise>0&&end>begin&&end-begin<=256&&
                (model.meanSigma()>=.30f||model.meanChromaSigma()>=.30f)&&nativeAvailable()) {
            int[] u=CpuSingle1978.geometry(width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model);
            String key=CpuSingle1978.key(true,u,protection!=null);
            boolean reused=workspace!=null&&CpuExact1978.enabled(key);
            if(workspace!=null&&!reused)workspace.original();
            int[] policy=null;
            if(protection!=null) {
                long count=(long)(end-begin)*width;
                if(count>Integer.MAX_VALUE/2)throw new IllegalArgumentException("single NR native policy");
                policy=reused?workspace.policy((int)count*2):new int[(int)count*2];
                for(int row=begin;row<end;row++) {
                    checkInterrupted();
                    for(int col=0;col<width;col++) {
                        int at=((row-begin)*width+col)*2;
                        policy[at]=Math.max(0,Math.min(256,protection.budgetQ8(col,row+originY)));
                        policy[at+1]=Math.max(0,Math.min(256,protection.detailQ8(col,row+originY)));
                    }
                }
            }
            try {
                boolean completed=CpuSingle1978.nativeCall(input,output,u,model.nativeData,policy,reused?workspace:null);
                checkInterrupted();
                if(completed){if(workspace!=null&&!reused)CpuSingle1978.offerNative(key,input,u,model.nativeData,policy);return;}
            } catch(UnsatisfiedLinkError staleNative) {nativeState=-1;}
        }
        if(workspace!=null){workspace.dropNative();CpuSingle1978.javaRange(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,workspace);}
        else processJavaRange(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection);
    }
    static void processJavaRange(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        processJavaRange1978(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,null);
    }
    static void processJavaRange1978(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,Workspace workspace) {
        validate(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,model);
        checkInterrupted();
        if(begin==end)return;
        if(noise==0||model.meanSigma()<.30f&&model.meanChromaSigma()<.30f) {
            System.arraycopy(input,begin*width,output,begin*width,(end-begin)*width);return;
        }
        Kernel w=workspace==null?new Kernel(width,Math.min(BATCH_ROWS,end-begin)):workspace.kernel(width,Math.min(BATCH_ROWS,end-begin));
        for(int start=begin;start<end;start+=BATCH_ROWS) {
            checkInterrupted();int finish=Math.min(end,start+BATCH_ROWS),count=(finish-start)*width;
            Arrays.fill(w.delta,0,count*3,0f);Arrays.fill(w.weight,0,count,0f);
            int absoluteStart=start+originY,absoluteEnd=finish+originY;
            int first=Math.floorDiv(absoluteStart-7,4)*4;
            for(int by=first;by<absoluteEnd;by+=4) {
                checkInterrupted();
                for(int bx=-4;bx<width;bx+=4) {
                    block(input,width,validBegin,validEnd,originY,model,protection,noise,shadows,
                        bx,by,8,start,finish,w);
                    if(by+4>absoluteStart&&by<absoluteEnd&&bx>=0)
                        block(input,width,validBegin,validEnd,originY,model,protection,noise,shadows,
                            bx,by,4,start,finish,w);
                }
            }
            checkInterrupted();
            for(int row=start;row<finish;row++)for(int col=0;col<width;col++) {
                int at=row*width+col,local=(row-start)*width+col,p=input[at];
                if((p>>>24)!=255||w.weight[local]==0){output[at]=p;continue;}
                float denom=w.weight[local],dy=w.delta[local*3]/denom;
                float dr=w.delta[local*3+1]/denom,db=w.delta[local*3+2]/denom;
                // Inverse Y,R-Y,B-Y; DC coefficients were retained at both scales.
                int r=byteValue(((p>>>16)&255)+dy+dr),b=byteValue((p&255)+dy+db);
                int g=byteValue(((p>>>8)&255)+dy-(.299f*dr+.114f*db)/.587f);
                output[at]=(p&0xff000000)|(r<<16)|(g<<8)|b;
            }
        }
    }
    private static void validate(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,Model model) {
        if(input==null||output==null||input==output||model==null||width<1||rows<1||
            width!=model.width||noise<0||noise>4||validBegin<0||validEnd>rows||begin<validBegin||
            end<begin||end>validEnd||(long)width*rows>input.length||(long)width*rows>output.length||
            (long)originY+validBegin<0||(long)originY+validEnd>model.height)
            throw new IllegalArgumentException("single NR strip");
        if(noise>0&&begin<end&&(originY+validBegin>Math.max(0,originY+begin-HALO)||
                originY+validEnd<Math.min(model.height,originY+end+HALO)))
            throw new IllegalArgumentException("single NR requires immutable halo");
    }
    private static final class Kernel {
        final float[] delta,weight;final double[][] source,coefficient;
        final double[] temp,residual;final float[] sigma=new float[3],variance=new float[9];
        Kernel(int width,int rows) {
            long size=(long)width*rows;if(size>Integer.MAX_VALUE/3)throw new IllegalArgumentException("single NR workspace");
            delta=new float[(int)size*3];weight=new float[(int)size];
            source=new double[][]{new double[64],new double[64],new double[64]};
            coefficient=new double[][]{new double[64],new double[64],new double[64]};
            temp=new double[64];residual=new double[64];
        }
    }
    private static void block(int[] input,int width,int validBegin,int validEnd,int originY,Model model,
            Protection protection,int noise,boolean shadows,int bx,int by,int n,int start,int finish,Kernel w) {
        int absoluteStart=start+originY,absoluteEnd=finish+originY;
        if(by+n<=absoluteStart||by>=absoluteEnd||bx+n<=0)return;
        double mean=0;boolean opaque=true;
        for(int yy=0;yy<n;yy++)for(int xx=0;xx<n;xx++) {
            int gx=Math.max(0,Math.min(width-1,bx+xx));
            int gy=Math.max(0,Math.min(model.height-1,by+yy))-originY;
            if(gy<validBegin||gy>=validEnd)throw new IllegalArgumentException("single NR incomplete halo");
            int p=input[gy*width+gx],at=yy*n+xx;double y=luma(p);
            if((p>>>24)!=255)opaque=false;
            w.source[0][at]=w.coefficient[0][at]=y;
            w.source[1][at]=w.coefficient[1][at]=((p>>>16)&255)-y;
            w.source[2][at]=w.coefficient[2][at]=(p&255)-y;mean+=y;
        }
        if(!opaque)return;
        mean/=n*n;int cx=Math.max(0,Math.min(width-1,bx+n/2)),cy=Math.max(0,Math.min(model.height-1,by+n/2));
        float sy=model.sigma(cx,cy,(float)mean,false),sc=model.sigma(cx,cy,(float)mean,true);
        if(sy<.30f&&sc<.30f)return;
        float strength=.35f+noise*.21f;
        if(shadows)strength*=1f+.12f*clamp((128-(float)mean)/96,0,1);
        w.sigma[0]=sy;w.sigma[1]=w.sigma[2]=sc;
        for(int plane=0;plane<3;plane++) {
            for(int band=0;band<BANDS;band++) {
                float shape=model.spectral(plane,band,(float)mean);
                w.variance[plane*BANDS+band]=w.sigma[plane]*w.sigma[plane]*shape*shape*strength;
            }
            transform(w.coefficient[plane],w.temp,n,false);
            for(int v=0;v<n;v++)for(int u=0;u<n;u++) {
                if(u+v==0)continue;
                int at=v*n+u;double coefficient=w.coefficient[plane][at];
                // Frequency bins refer to physical cycles/pixel at either scale.
                int frequency=(u+v)*8/n,band=frequency<4?0:frequency<8?1:2;
                float variance=w.variance[plane*BANDS+band];
                if(plane==0&&u+v==1)variance*=.32f;
                double power=coefficient*coefficient;
                // Strong sparse coefficients are real detail evidence. Protecting
                // them exactly avoids softening text, hair and repeated mesh.
                if(variance<=.03f||power>variance*36)continue;
                double gain=power/(power+variance*1.25);
                w.coefficient[plane][at]=coefficient*gain;
            }
            transform(w.coefficient[plane],w.temp,n,true);
        }
        for(int i=0;i<n*n;i++)w.residual[i]=w.source[0][i]-w.coefficient[0][i];
        // NR3 tests the removed signal itself. Repeated alternating residuals,
        // and coherent removed outlines, reduce the complete candidate's amount.
        float safeguard=residualSafeguard(w.source[0],w.residual,n,sy);
        for(int yy=0;yy<n;yy++) {
            int ay=by+yy;if(ay<absoluteStart||ay>=absoluteEnd||ay<0||ay>=model.height)continue;
            for(int xx=0;xx<n;xx++) {
                int ax=bx+xx;if(ax<0||ax>=width)continue;
                int at=yy*n+xx,local=(ay-originY-start)*width+ax;
                float blend=safeguard;
                if(protection!=null) {
                    int budget=Math.max(0,Math.min(256,protection.budgetQ8(ax,ay)));
                    int detail=Math.max(0,Math.min(256,protection.detailQ8(ax,ay)));
                    blend*=budget/256f*(1-detail/256f);
                }
                float weight=n==8?.82f*WINDOW8[xx]*WINDOW8[yy]:.18f;
                w.weight[local]+=weight;
                for(int plane=0;plane<3;plane++)
                    w.delta[local*3+plane]+=(float)(w.coefficient[plane][at]-w.source[plane][at])*weight*blend;
            }
        }
    }
    private static float residualSafeguard(double[] source,double[] residual,int n,float sigma) {
        double energy=0;for(int i=0;i<n*n;i++)energy+=residual[i]*residual[i];
        if(energy<.0001)return 1;
        double r1=lag(residual,n,1),r2=lag(residual,n,2),r4=n==8?lag(residual,n,4):0;
        double periodic=Math.max(r2>.25&&r1<-.15?r2:0,r4>.25&&r2<-.15?r4:0);
        float keep=1f-clamp((float)(periodic-.25)/.5f,0,1)*.88f;
        // Signed half-window residual agreement detects a removed real contour.
        // Opposite directions in uncorrelated grain cancel in these row/col means.
        for(int vertical=0;vertical<2;vertical++) {
            double coherent=0,removed=0;int count=0;
            for(int k=0;k<n;k++) {
                double a=0,b=0,ra=0,rb=0;
                for(int j=0;j<n/2;j++) {
                    int ia=vertical==0?k*n+j:j*n+k,ib=vertical==0?k*n+j+n/2:(j+n/2)*n+k;
                    a+=source[ia];b+=source[ib];ra+=residual[ia];rb+=residual[ib];
                }
                double edge=(b-a)/(n/2),lost=(rb-ra)/(n/2);
                if(Math.abs(edge)>Math.max(3,sigma*1.4)){
                    coherent+=edge;removed+=lost;count++;
                }
            }
            if(count>=n/2&&Math.abs(coherent)>Math.max(8,sigma*n)&&
                removed*coherent>0&&Math.abs(removed)>Math.abs(coherent)*.015)
                keep=Math.min(keep,.18f);
        }
        return keep;
    }
    private static double lag(double[] values,int n,int lag) {
        double numerator=0,a=0,b=0;
        for(int y=0;y<n;y++)for(int x=0;x<n-lag;x++) {
            double p=values[y*n+x],q=values[y*n+x+lag];numerator+=p*q;a+=p*p;b+=q*q;
        }
        for(int y=0;y<n-lag;y++)for(int x=0;x<n;x++) {
            double p=values[y*n+x],q=values[(y+lag)*n+x];numerator+=p*q;a+=p*p;b+=q*q;
        }
        return a*b<1e-9?0:numerator/Math.sqrt(a*b);
    }
    private static double[][] basis(int n) {
        double[][] basis=new double[n][n];for(int k=0;k<n;k++)for(int x=0;x<n;x++)
            basis[k][x]=(k==0?Math.sqrt(1.0/n):Math.sqrt(2.0/n))*Math.cos(Math.PI*(x+.5)*k/n);
        return basis;
    }
    /** Orthonormal separable DCT-II/III, symmetric pairs halve multiplies. */
    private static void transform(double[] values,double[] temp,int n,boolean inverse) {
        double[][] basis=n==8?D8:D4;
        if(!inverse) {
            for(int row=0;row<n;row++)for(int k=0;k<n;k++) {
                double sum=0;for(int x=0;x<n/2;x++)sum+=(values[row*n+x]+((k&1)==0?values[row*n+n-1-x]:-values[row*n+n-1-x]))*basis[k][x];
                temp[row*n+k]=sum;
            }
            for(int col=0;col<n;col++)for(int k=0;k<n;k++) {
                double sum=0;for(int y=0;y<n/2;y++)sum+=(temp[y*n+col]+((k&1)==0?temp[(n-1-y)*n+col]:-temp[(n-1-y)*n+col]))*basis[k][y];
                values[k*n+col]=sum;
            }
        } else {
            for(int col=0;col<n;col++)for(int y=0;y<n/2;y++) {
                double even=0,odd=0;for(int k=0;k<n;k++){double v=values[k*n+col]*basis[k][y];if((k&1)==0)even+=v;else odd+=v;}
                temp[y*n+col]=even+odd;temp[(n-1-y)*n+col]=even-odd;
            }
            for(int row=0;row<n;row++)for(int x=0;x<n/2;x++) {
                double even=0,odd=0;for(int k=0;k<n;k++){double v=temp[row*n+k]*basis[k][x];if((k&1)==0)even+=v;else odd+=v;}
                values[row*n+x]=even+odd;values[row*n+n-1-x]=even-odd;
            }
        }
    }
    private static double luma(int p){return .299*((p>>>16)&255)+.587*((p>>>8)&255)+.114*(p&255);}
    private static int byteValue(double value){return Math.max(0,Math.min(255,(int)Math.floor(value+.5)));}
    private static float clamp(float value,float lo,float hi){return Math.max(lo,Math.min(hi,value));}
    private static void checkInterrupted(){if(Thread.currentThread().isInterrupted())throw new CancellationException("single NR interrupted before strip commit");}
    public static boolean nativeAvailable() {
        if(nativeState==0)synchronized(SingleNoise1955.class) {
            if(nativeState==0)try {
                System.loadLibrary("ulike_nr1955");nativeState=nativeAbi()==1955?1:-1;
            } catch(LinkageError unavailable){nativeState=-1;}
              catch(SecurityException unavailable){nativeState=-1;}
        }
        return nativeState==1;
    }
    private static native int nativeAbi();
    private static native boolean processNative(int[] input,int[] output,int width,int rows,int begin,int end,
        int validBegin,int validEnd,int originY,int noise,boolean shadows,int modelWidth,int modelHeight,
        int columns,int modelRows,int patchWidth,int patchHeight,float[] modelData,int[] protection);
}
