package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Conservative temporal noise reduction on tightly packed NV21 camera frames.
 * Inputs are immutable distinct camera captures, before ULike's beauty renderer.
 * A conservatively selected real frame owns geometry, photometry and uncertain regions.
 * Frame zero remains the owner on OFF, uncertainty, interruption and processing failure.
 * No tone lifting, synthetic detail, extra sharpening or post-beauty source blending.
 *
 * Alignment uses a small area-averaged image, full-resolution refinement, and a
 * smoothly interpolated local translation mesh. Registration/photometric outliers
 * are rejected before robust, structure-checked temporal averaging. Workspace is
 * one NV21 output, small registration meshes/thumbnails and at most four workers.
 */
public final class FusionPixels1933 {
    private FusionPixels1933() { }
    private static final int MAX_FRAMES = 4;
    private static final int GRID = 64;
    private static final int JOB_ROWS = 32;
    private static final int[] STRENGTH = {0, 96, 160, 224, 256};

    public static final class Motion {
        public final float score, globalShiftPixels, movingFraction, confidence;
        public final float darkness, noiseSigma;
        public final boolean reliable, duplicate;
        private Motion(float score, float shift, float moving, float confidence,
                       float darkness, float noise, boolean reliable, boolean duplicate) {
            this.score=unit(score);globalShiftPixels=shift;movingFraction=unit(moving);
            this.confidence=unit(confidence);this.darkness=unit(darkness);
            noiseSigma=noise;this.reliable=reliable;this.duplicate=duplicate;
        }
    }

    public static final class Result {
        /** May alias the selected real frame on selective-fusion fallback. */
        public final byte[] nv21;
        public final int referenceIndex, acceptedCount;
        /** Exact supplied sensor timestamp of referenceIndex; zero if unavailable. */
        public final long referenceTimestamp;
        public final float motionScore, movingFraction;
        public final long fusedPixels;
        public final boolean actualFusion, cancelled;
        private Result(byte[] pixels,int index,long timestamp,int count,float motion,float moving,long fused,boolean cancelled) {
            nv21=pixels;referenceIndex=index;referenceTimestamp=timestamp;acceptedCount=count;motionScore=unit(motion);
            movingFraction=unit(moving);fusedPixels=fused;actualFusion=count>1&&fused>0;
            this.cancelled=cancelled;
        }
    }

    /** Probe is a measurement only. Capture policy must use real timestamp spacing
     * to predict blur for its next requested exposure; this method never opens a camera. */
    public static Motion probeMotion(byte[] reference,byte[] candidate,int width,int height,
                                     int iso,long exposureNanos) {
        int size=validate(reference,width,height);
        if(candidate==null||candidate.length!=size||width<16||height<16)
            return unknown(reference,width,height);
        Stats a=statistics(reference,width,height);
        if(reference==candidate||Arrays.equals(reference,candidate))
            return new Motion(0,0,0,1,darkness(a.mean),a.ySigma,true,true);
        Stats b=statistics(candidate,width,height);
        Pair pair=align(reference,candidate,width,height,a,b,false);
        if(!pair.valid)return new Motion(1,pair.shift(),1,0,darkness(a.mean),a.ySigma,false,false);
        float score=unit(pair.movingFraction*2.0f+pair.shift()/Math.max(3f,Math.min(width,height)*0.006f));
        // 3x3 means still contain about sigma/3 noise. Require substantially
        // more variation before interpreting a fitted shift as camera motion.
        boolean reliable=pair.structure>Math.max(2f,a.ySigma*0.65f);
        if(!reliable)score=Math.max(score,0.45f);
        return new Motion(score,pair.shift(),pair.movingFraction,pair.confidence,
            darkness(a.mean),a.ySigma,reliable,false);
    }

    /** Metadata-free overload is intended for a caller that already validated one
     * same-exposure group. Real, positive, distinct timestamps are still required. */
    public static Result fuse(byte[][] frames,int width,int height,long[] timestamps,
                               int noiseLevel,boolean night,int workers) {
        return fuse(frames,width,height,timestamps,null,null,noiseLevel,night,workers);
    }

    public static Result fuse(byte[][] frames,int width,int height,long[] timestamps,
                               long[] exposureNanos,int[] isos,int noiseLevel,
                               boolean night,int workers) {
        if(frames==null||frames.length==0)throw new IllegalArgumentException("missing camera frames");
        int bytes=validate(frames[0],width,height);
        byte[] original=frames[0];
        long originalTime=timestamps!=null&&timestamps.length>0?Math.max(0,timestamps[0]):0;
        int level=clamp(noiseLevel,0,4),length=Math.min(MAX_FRAMES,frames.length);
        if(level==0||length<2||width<16||height<16)return single(original,0,originalTime,false);
        if(timestamps==null||timestamps.length<length||timestamps[0]<=0)return single(original,0,originalTime,false);
        if(exposureNanos!=null&&(exposureNanos.length<length||exposureNanos[0]<=0))return single(original,0,originalTime,false);
        if(isos!=null&&(isos.length<length||isos[0]<=0))return single(original,0,originalTime,false);
        if(Thread.currentThread().isInterrupted())return single(original,0,originalTime,true);
        Stats[] stats=new Stats[length];stats[0]=statistics(original,width,height);
        boolean[] eligible=new boolean[length];eligible[0]=true;
        Pair[] fromOriginal=new Pair[length];
        Small[] thumbnails=new Small[length];
        // Reference selection sees only distinct real captures in the original
        // same-exposure group. It never asks the camera for another capture.
        for(int i=1;i<length;i++) {
            if(Thread.currentThread().isInterrupted())return single(original,0,originalTime,true);
            byte[] frame=frames[i];
            if(frame==null||frame.length!=bytes||timestamps[i]<=0||timestamps[i]==timestamps[0])continue;
            boolean duplicate=false;
            for(int j=0;j<i;j++)if(timestamps[i]==timestamps[j]||frame==frames[j]||
                (frames[j]!=null&&frames[j].length==bytes&&Arrays.equals(frame,frames[j]))) {
                duplicate=true;break;
            }
            if(duplicate||!sameExposure(exposureNanos,isos,0,i))continue;
            eligible[i]=true;stats[i]=statistics(frame,width,height);
            if(thumbnails[0]==null)thumbnails[0]=new Small(original,width,height);
            thumbnails[i]=new Small(frame,width,height);
            fromOriginal[i]=align(original,frame,width,height,stats[0],stats[i],false,thumbnails[0],thumbnails[i]);
        }
        int referenceIndex=0;float bestGain=1.0f;
        for(int i=1;i<length;i++)if(eligible[i]) {
            if(Thread.currentThread().isInterrupted())return single(original,0,originalTime,true);
            float gain=sharpnessGain(original,frames[i],width,height,stats[0],stats[i],fromOriginal[i]);
            // A small/noisy/tied advantage is insufficient to change the moment
            // of capture. Both absolute evidence and a 25% ratio are required.
            if(gain>1.25f&&gain>bestGain*1.06f){referenceIndex=i;bestGain=gain;}
        }
        byte[] reference=frames[referenceIndex];Stats refStats=stats[referenceIndex];
        Pair[] candidates=new Pair[length-1];int count=0;
        float motion=0,moving=0;
        for(int i=0;i<length;i++) {
            if(Thread.currentThread().isInterrupted())return single(original,0,originalTime,true);
            if(i==referenceIndex||!eligible[i]||!sameExposure(exposureNanos,isos,referenceIndex,i))continue;
            Pair pair=referenceIndex==0?fromOriginal[i]:
                align(reference,frames[i],width,height,refStats,stats[i],false,thumbnails[referenceIndex],thumbnails[i]);
            if(pair==null||!pair.valid)continue;
            // Registration thumbnails are shared; only the chosen reference gets
            // a full local mesh. Choosing a frame never clones a full camera image.
            buildMesh(reference,pair,width,height,refStats);
            pair.frameIndex=i;candidates[count++]=pair;
            moving=Math.max(moving,pair.movingFraction);
            motion=Math.max(motion,unit(pair.movingFraction*2f+pair.shift()/Math.max(3f,Math.min(width,height)*0.006f)));
        }
        if(count==0)return single(reference,referenceIndex,timestamps[referenceIndex],false);
        byte[] result=reference.clone();
        Work work=new Work(reference,result,width,height,Arrays.copyOf(candidates,count),refStats,level,night);
        int workerCount=clamp(workers,1,4);
        workerCount=Math.min(workerCount,Math.max(1,(height+JOB_ROWS-1)/JOB_ROWS));
        Worker[] jobs=new Worker[workerCount];
        Thread[] threads=new Thread[Math.max(0,workerCount-1)];
        for(int i=0;i<workerCount;i++)jobs[i]=new Worker(work);
        int started=0;boolean interrupted=false;
        try {
            for(int i=0;i<threads.length;i++) {
                try {
                    threads[i]=new Thread(jobs[i+1],"ULikeBurst1933");
                    threads[i].setDaemon(true);threads[i].start();started++;
                }catch(OutOfMemoryError noThread){break;}
            }
            jobs[0].run();
        } finally {
            for(int i=0;i<started;i++)while(threads[i].isAlive()) {
                try{threads[i].join();}
                catch(InterruptedException stop){interrupted=true;work.failed=true;
                    for(int j=0;j<started;j++)threads[j].interrupt();}
            }
            if(interrupted)Thread.currentThread().interrupt();
        }
        if(work.failed||Thread.currentThread().isInterrupted())return single(original,0,originalTime,true);
        long fused=0;long[] participation=new long[count];
        for(Worker worker:jobs){fused+=worker.fused;
            for(int i=0;i<count;i++)participation[i]+=worker.participation[i];}
        int accepted=1;for(long n:participation)if(n>0)accepted++;
        return accepted>1?new Result(result,referenceIndex,timestamps[referenceIndex],accepted,motion,moving,fused,false):
            single(reference,referenceIndex,timestamps[referenceIndex],false);
    }

    private static Result single(byte[] reference,int index,long timestamp,boolean cancelled){
        return new Result(reference,index,timestamp,1,0,0,0,cancelled);
    }
    private static boolean sameExposure(long[] exposures,int[] isos,int reference,int i) {
        if(exposures!=null&&(exposures[i]<=0||ratio(exposures[reference],exposures[i])>1.03))return false;
        return isos==null||(isos[i]>0&&ratio(isos[reference],isos[i])<=1.03);
    }

    /** Compare edge widths, not raw high-frequency energy (which rewards noise).
     * Gradients of 3x3 means are measured at two scales on aligned, coherent edges.
     * Noise energy is removed, overall edge contrast is divided out, and evidence
     * must be spread over several image regions. No pixels are altered here. */
    private static float sharpnessGain(byte[] original,byte[] candidate,int w,int h,
                                        Stats a,Stats b,Pair pair) {
        if(pair==null||!pair.valid||pair.movingFraction>0.14f||pair.confidence<0.55f||
           pair.structure<Math.max(8f,a.ySigma*2.5f)||Math.abs(pair.biasY)>2.01f||
           Math.abs(pair.biasV)>2.01f||Math.abs(pair.biasU)>2.01f||
           Math.abs(a.mean-b.mean)>2.5f||b.ySigma>Math.max(2f,a.ySigma*1.18f+0.35f))return 1f;
        int[] support=new int[16];double fineA=0,coarseA=0,fineB=0,coarseB=0;int n=0;
        int step=Math.max(2,(int)Math.sqrt((long)w*h/4096.0));
        double noiseA=a.ySigma*a.ySigma,noiseB=b.ySigma*b.ySigma;
        double minimum=Math.max(100,Math.max(noiseA,noiseB)*12);
        for(int y=7;y<h-7;y+=step)for(int x=7;x<w-7;x+=step) {
            float bx=x+pair.dx,by=y+pair.dy;
            if(bx<6||bx>=w-7||by<6||by>=h-7)continue;
            float ax=mean3(original,w,x+4,y)-mean3(original,w,x-4,y);
            float ay=mean3(original,w,x,y+4)-mean3(original,w,x,y-4);
            float cx=mean3At(candidate,w,bx+4,by)-mean3At(candidate,w,bx-4,by);
            float cy=mean3At(candidate,w,bx,by+4)-mean3At(candidate,w,bx,by-4);
            double ac=ax*ax+ay*ay,bc=cx*cx+cy*cy,dot=ax*cx+ay*cy;
            if(ac<minimum||bc<minimum||dot<0||dot*dot<ac*bc*.94||bc<ac*.62||bc>ac*1.62)continue;
            float am=mean3(original,w,x,y),bm=mean3At(candidate,w,bx,by)-pair.biasY;
            if(am<12||am>243||bm<12||bm>243||Math.abs(am-bm)>Math.max(3,a.ySigma*0.8f))continue;
            float afx=mean3(original,w,x+1,y)-mean3(original,w,x-1,y);
            float afy=mean3(original,w,x,y+1)-mean3(original,w,x,y-1);
            float bfx=mean3At(candidate,w,bx+1,by)-mean3At(candidate,w,bx-1,by);
            float bfy=mean3At(candidate,w,bx,by+1)-mean3At(candidate,w,bx,by-1);
            double af=Math.max(0,afx*afx+afy*afy-noiseA*(24.0/81));
            double bf=Math.max(0,bfx*bfx+bfy*bfy-noiseB*(24.0/81));
            ac=Math.max(1,ac-noiseA*(36.0/81));bc=Math.max(1,bc-noiseB*(36.0/81));
            // Cap each edge's leverage; a newly moving bright point cannot own
            // the decision. A coherent edge contributes at either scale.
            double weight=1.0/Math.max(256,Math.max(ac,bc));
            fineA+=Math.min(af,ac)*weight;coarseA+=ac*weight;
            fineB+=Math.min(bf,bc)*weight;coarseB+=bc*weight;n++;
            support[Math.min(3,y*4/h)*4+Math.min(3,x*4/w)]++;
        }
        int regions=0,rows=0,columns=0;
        for(int i=0;i<16;i++)if(support[i]>=4){regions++;rows|=1<<(i/4);columns|=1<<(i%4);}
        if(n<48||regions<4||Integer.bitCount(rows)<2||Integer.bitCount(columns)<2||coarseA<=0||coarseB<=0)return 1f;
        double sa=fineA/coarseA,sb=fineB/coarseB;
        if(sa<0.015||sb-sa<0.025)return 1f;
        return (float)(sb/sa);
    }
    private static double ratio(long a,long b){return (double)Math.max(a,b)/Math.min(a,b);}
    private static Motion unknown(byte[] ref,int w,int h){Stats s=statistics(ref,w,h);
        return new Motion(1,0,1,0,darkness(s.mean),s.ySigma,false,false);}
    private static float darkness(float mean){return unit((104f-mean)/80f);}

    private static final class Stats {
        float ySigma,uvSigma,mean;
        Stats(float y,float uv,float mean){ySigma=y;uvSigma=uv;this.mean=mean;}
    }
    private static Stats statistics(byte[] data,int w,int h) {
        int[] yHist=new int[512],uvHist=new int[512];
        int stride=Math.max(1,(int)Math.sqrt((long)w*h/8192.0)),n=0,un=0;long mean=0;int means=0;
        for(int y=0;y<h-1;y+=stride)for(int x=(y/stride)%stride;x<w-1;x+=stride) {
            int p=y*w+x,a=u(data[p]),b=u(data[p+1]),c=u(data[p+w]),d=u(data[p+w+1]);
            int m=(a+b+c+d+2)/4;mean+=m;means++;
            if(m<3||m>252||Math.abs(a+c-b-d)>80||Math.abs(a+b-c-d)>80)continue;
            yHist[Math.abs(a-b-c+d)]++;n++;
        }
        int start=w*h,cw=w/2,ch=h/2,step=Math.max(1,(int)Math.sqrt((long)cw*ch/4096.0));
        for(int y=0;y<ch-1;y+=step)for(int x=0;x<cw-1;x+=step)for(int c=0;c<2;c++) {
            int p=start+y*w+x*2+c;
            int d=Math.abs(u(data[p])-u(data[p+2])-u(data[p+w])+u(data[p+w+2]));
            uvHist[d]++;un++;
        }
        float ys=n>0?median(yHist,n)/1.34898f:1f;
        float cs=un>0?median(uvHist,un)/1.34898f:1f;
        return new Stats(Math.max(0.6f,Math.min(48f,ys)),Math.max(0.6f,Math.min(48f,cs)),means>0?(float)mean/means:0f);
    }

    private static final class Small {
        final int w,h,step;final float[] p;
        Small(byte[] data,int width,int height) {
            int s=1;while(Math.max(width,height)/s>256)s*=2;step=s;
            w=(width+s-1)/s;h=(height+s-1)/s;p=new float[w*h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int right=Math.min(width,(x+1)*s),bottom=Math.min(height,(y+1)*s);
                long sum=0;int n=0;
                for(int yy=y*s;yy<bottom;yy++){int at=yy*width+x*s;
                    for(int xx=x*s;xx<right;xx++){sum+=u(data[at++]);n++;}}
                p[y*w+x]=(float)sum/n;
            }
        }
    }
    private static final class Pair {
        final byte[] data;
        final Stats stats;
        int frameIndex,gw,gh;
        boolean valid;
        float dx,dy,biasY,biasV,biasU,confidence,movingFraction,structure;
        int correctionY,correctionV,correctionU;
        float meanLimit,structureLimit,pixelLimit,colourLimit,baseWeight;
        float[] flowX,flowY,trust;
        Pair(byte[] data,Stats stats){this.data=data;this.stats=stats;}
        float shift(){return (float)Math.sqrt(dx*dx+dy*dy);}
    }
    private static Pair align(byte[] reference,byte[] candidate,int w,int h,Stats a,Stats b,boolean mesh) {
        return align(reference,candidate,w,h,a,b,mesh,new Small(reference,w,h),new Small(candidate,w,h));
    }
    private static Pair align(byte[] reference,byte[] candidate,int w,int h,Stats a,Stats b,boolean mesh,Small sa,Small sb) {
        Pair pair=new Pair(candidate,b);
        int range=Math.max(1,Math.min(6,(Math.min(96,Math.max(8,Math.min(w,h)/16))+sa.step-1)/sa.step));
        double best=Double.POSITIVE_INFINITY;int dx=0,dy=0;
        for(int yy=-range;yy<=range;yy++)for(int xx=-range;xx<=range;xx++) {
            double cost=smallCost(sa,sb,xx,yy)+0.00001*(xx*xx+yy*yy);
            if(cost<best){best=cost;dx=xx*sa.step;dy=yy*sa.step;}
        }
        for(int scale=Math.max(1,sa.step/2);;scale=Math.max(1,scale/2)) {
            double score=Double.POSITIVE_INFINITY;int nx=dx,ny=dy;
            for(int yy=-1;yy<=1;yy++)for(int xx=-1;xx<=1;xx++) {
                int tx=dx+xx*scale,ty=dy+yy*scale;
                double cost=globalCost(reference,candidate,w,h,tx,ty)+0.00001*(tx*tx+ty*ty);
                if(cost<score){score=cost;nx=tx;ny=ty;}
            }
            dx=nx;dy=ny;if(scale==1)break;
        }
        double center=globalCost(reference,candidate,w,h,dx,dy);
        float fx=subpixel(globalCost(reference,candidate,w,h,dx-1,dy),center,
            globalCost(reference,candidate,w,h,dx+1,dy));
        float fy=subpixel(globalCost(reference,candidate,w,h,dx,dy-1),center,
            globalCost(reference,candidate,w,h,dx,dy+1));
        pair.dx=dx+fx;pair.dy=dy+fy;
        float sigma=(float)Math.sqrt((a.ySigma*a.ySigma+b.ySigma*b.ySigma)*0.5);
        float[] photometry=photometry(reference,candidate,w,h,pair.dx,pair.dy,sigma);
        pair.biasY=photometry[0];pair.biasV=photometry[1];pair.biasU=photometry[2];
        pair.movingFraction=photometry[3];pair.structure=photometry[4];
        float tolerance=Math.max(4f,sigma*sigma*0.85f+3f);
        pair.valid=finite(center)&&center<tolerance*2.5&&Math.abs(pair.biasY)<=6.01f&&
            Math.abs(pair.biasV)<=6.01f&&Math.abs(pair.biasU)<=6.01f&&pair.movingFraction<0.65f;
        pair.confidence=pair.valid?unit(1f-(float)center/(tolerance*4f)):0f;
        if(!pair.valid)return pair;
        if(mesh)buildMesh(reference,pair,w,h,a);
        return pair;
    }
    private static float subpixel(double minus,double center,double plus) {
        double divisor=minus-2*center+plus;
        if(!finite(divisor)||divisor<0.0001)return 0f;
        float v=(float)Math.max(-0.5,Math.min(0.5,0.5*(minus-plus)/divisor));
        return Math.abs(v)<0.15f?0f:v;
    }
    private static double smallCost(Small a,Small b,int dx,int dy) {
        double sum=0,square=0;int n=0;
        for(int y=3;y<a.h-3;y+=2){int by=y+dy;if(by<1||by>=b.h-1)continue;
            for(int x=3;x<a.w-3;x+=2){int bx=x+dx;if(bx<1||bx>=b.w-1)continue;
                double d=a.p[y*a.w+x]-b.p[by*b.w+bx];
                d=Math.max(-12,Math.min(12,d));sum+=d;square+=d*d;n++;}}
        return n<8?Double.POSITIVE_INFINITY:Math.max(0,square/n-(sum/n)*(sum/n));
    }
    private static double globalCost(byte[] a,byte[] b,int w,int h,int dx,int dy) {
        double sum=0,square=0;int n=0;
        int sx=Math.max(3,w/24),sy=Math.max(3,h/20);
        for(int y=4;y<h-4;y+=sy){int by=y+dy;if(by<2||by>=h-2)continue;
            for(int x=4;x<w-4;x+=sx){int bx=x+dx;if(bx<2||bx>=w-2)continue;
                double d=mean3(a,w,x,y)-mean3(b,w,bx,by);
                // A moving face/hand must not dominate the camera transform.
                // Local motion is still rejected at mesh and output-cell gates.
                d=Math.max(-12,Math.min(12,d));sum+=d;square+=d*d;n++;}}
        return n<12?Double.POSITIVE_INFINITY:Math.max(0,square/n-(sum/n)*(sum/n));
    }
    private static float[] photometry(byte[] a,byte[] b,int w,int h,float dx,float dy,float sigma) {
        int[] hy=new int[511],hv=new int[511],hu=new int[511];int n=0;
        double mean=0,square=0;
        int sx=Math.max(2,w/40),sy=Math.max(2,h/32);
        for(int y=4;y<h-4;y+=sy)for(int x=4;x<w-4;x+=sx) {
            float bx=x+dx,by=y+dy;if(bx<2||bx>=w-3||by<2||by>=h-3)continue;
            int r=Math.round(mean3(a,w,x,y)),c=Math.round(mean3At(b,w,bx,by));
            hy[clamp(c-r+255,0,510)]++;mean+=r;square+=(double)r*r;n++;
            int uv=w*h+(y/2)*w+(x&~1);
            int cv=sampleUV(b,w,h,(x/2)+dx*0.5f,(y/2)+dy*0.5f,0);
            int cu=sampleUV(b,w,h,(x/2)+dx*0.5f,(y/2)+dy*0.5f,1);
            hv[clamp(cv-u(a[uv])+255,0,510)]++;hu[clamp(cu-u(a[uv+1])+255,0,510)]++;
        }
        if(n==0)return new float[]{255,255,255,1,0};
        int bias=median(hy,n)-255,bv=median(hv,n)-255,bu=median(hu,n)-255;
        int moving=0;float limit=Math.max(3.5f,sigma*1.05f);
        for(int i=0;i<hy.length;i++)if(Math.abs(i-255-bias)>limit)moving+=hy[i];
        float structure=(float)Math.sqrt(Math.max(0,square/n-(mean/n)*(mean/n)));
        return new float[]{bias,bv,bu,(float)moving/n,structure};
    }

    private static void buildMesh(byte[] reference,Pair p,int w,int h,Stats a) {
        p.gw=(w+GRID-1)/GRID+1;p.gh=(h+GRID-1)/GRID+1;
        int n=p.gw*p.gh;p.flowX=new float[n];p.flowY=new float[n];p.trust=new float[n];
        float sigma=(float)Math.sqrt((a.ySigma*a.ySigma+p.stats.ySigma*p.stats.ySigma)*0.5);
        float allowed=Math.max(3f,sigma*sigma*0.62f+2f);
        for(int gy=0;gy<p.gh;gy++)for(int gx=0;gx<p.gw;gx++) {
            if(Thread.currentThread().isInterrupted())return;
            int at=gy*p.gw+gx,cx=clamp(gx*GRID,4,w-5),cy=clamp(gy*GRID,4,h-5);
            float global=patchCost(reference,p.data,w,h,cx,cy,p.dx,p.dy,p.biasY);
            float best=global,dx=p.dx,dy=p.dy;
            for(int yy=-2;yy<=2;yy++)for(int xx=-2;xx<=2;xx++) {
                if(xx==0&&yy==0)continue;
                float score=patchCost(reference,p.data,w,h,cx,cy,p.dx+xx,p.dy+yy,p.biasY)+
                    (xx*xx+yy*yy)*Math.max(0.08f,sigma*sigma*0.012f);
                if(score<best){best=score;dx=p.dx+xx;dy=p.dy+yy;}
            }
            // A local displacement substantially different from the camera motion
            // is a moving-object hypothesis, not permission to warp that object.
            float deviation=Math.abs(dx-p.dx)+Math.abs(dy-p.dy);
            boolean inconsistent=deviation>2.1f||global>allowed*5f;
            if(global-best<Math.max(0.5f,sigma*sigma*0.025f)){dx=p.dx;dy=p.dy;best=global;}
            p.flowX[at]=dx;p.flowY[at]=dy;
            p.trust[at]=!inconsistent&&best<allowed*2.5f?
                unit((1f-best/(allowed*3f))*p.confidence):0f;
        }
    }
    private static float patchCost(byte[] a,byte[] b,int w,int h,int cx,int cy,float dx,float dy,float bias) {
        double sum=0;int n=0;
        for(int oy=-18;oy<=18;oy+=6)for(int ox=-18;ox<=18;ox+=6) {
            int x=cx+ox,y=cy+oy;float bx=x+dx,by=y+dy;
            if(x<2||x>=w-2||y<2||y>=h-2||bx<2||bx>=w-3||by<2||by>=h-3)continue;
            float d=mean3(a,w,x,y)-(mean3At(b,w,bx,by)-bias);sum+=d*d;n++;
        }
        return n>=9?(float)(sum/n):Float.POSITIVE_INFINITY;
    }

    private static final class Work {
        final byte[] reference,output;final int w,h,level;final boolean night;
        final Pair[] pairs;final Stats stats;final AtomicInteger next=new AtomicInteger();
        volatile boolean failed;
        Work(byte[] r,byte[] out,int w,int h,Pair[] pairs,Stats stats,int level,boolean night){
            reference=r;output=out;this.w=w;this.h=h;this.pairs=pairs;this.stats=stats;this.level=level;this.night=night;
            for(Pair p:pairs) {
                float noise=(float)Math.sqrt(stats.ySigma*stats.ySigma+p.stats.ySigma*p.stats.ySigma);
                p.meanLimit=Math.max(3f,noise*(night?1.05f:1.18f));
                p.structureLimit=Math.max(5f,noise*2.5f);p.pixelLimit=Math.max(7f,noise*3.2f);
                float colourNoise=(float)Math.sqrt(stats.uvSigma*stats.uvSigma+p.stats.uvSigma*p.stats.uvSigma);
                p.colourLimit=Math.max(4f,colourNoise*3.2f);
                p.baseWeight=STRENGTH[level]*Math.min(1f,(stats.ySigma*stats.ySigma+1f)/(p.stats.ySigma*p.stats.ySigma+1f));
                p.correctionY=Math.round(p.biasY);p.correctionV=Math.round(p.biasV);p.correctionU=Math.round(p.biasU);
            }
        }
    }
    private static final class Worker implements Runnable {
        final Work work;final long[] participation;long fused;
        Worker(Work work){this.work=work;participation=new long[work.pairs.length];}
        public void run(){try{
            for(;;){if(work.failed||Thread.currentThread().isInterrupted()){work.failed=true;return;}
                int first=work.next.getAndAdd(JOB_ROWS);if(first>=work.h)return;
                rows(first,Math.min(work.h,first+JOB_ROWS));}
        }catch(Throwable failure){work.failed=true;}}
        void rows(int first,int end) {
            int w=work.w,h=work.h,uvStart=w*h;
            byte[] ref=work.reference,out=work.output;
            for(int y=first;y<end;y+=2)for(int x=0;x<w;x+=2) {
                int at=y*w+x,uv=uvStart+(y/2)*w+x;
                int a=u(ref[at]),b=u(ref[at+1]),c=u(ref[at+w]),d=u(ref[at+w+1]);
                int v=u(ref[uv]),u=u(ref[uv+1]);
                int sa=a*256,sb=b*256,sc=c*256,sd=d*256,sw=256;
                int sv=v*256,su=u*256,suw=256;boolean contributed=false;
                for(int k=0;k<work.pairs.length;k++) {
                    Pair pair=work.pairs[k];int gx=x/GRID,gy=y/GRID;
                    int i=gy*pair.gw+gx;float fx=(x%GRID)/(float)GRID,fy=(y%GRID)/(float)GRID;
                    float trust=interpolate(pair.trust,i,pair.gw,fx,fy);
                    if(trust<0.18f)continue;
                    float dx=interpolate(pair.flowX,i,pair.gw,fx,fy),dy=interpolate(pair.flowY,i,pair.gw,fx,fy);
                    float xx=x+dx,yy=y+dy;
                    if(xx<0||yy<0||xx>=w-2||yy>=h-2)continue;
                    int aa=sampleY(pair.data,w,xx,yy)-pair.correctionY;
                    int bb=sampleY(pair.data,w,xx+1,yy)-pair.correctionY;
                    int cc=sampleY(pair.data,w,xx,yy+1)-pair.correctionY;
                    int dd=sampleY(pair.data,w,xx+1,yy+1)-pair.correctionY;
                    int da=aa-a,db=bb-b,dc=cc-c,ddiff=dd-d;
                    float meanDiff=Math.abs(da+db+dc+ddiff)*0.25f;
                    float gxDiff=Math.abs(da-db+dc-ddiff)*0.5f;
                    float gyDiff=Math.abs(da+db-dc-ddiff)*0.5f;
                    float diagonalDiff=Math.abs(da-db-dc+ddiff)*0.5f;
                    if(meanDiff>pair.meanLimit||gxDiff>pair.structureLimit||gyDiff>pair.structureLimit||diagonalDiff>pair.structureLimit)continue;
                    int maxDiff=Math.max(Math.max(Math.abs(da),Math.abs(db)),Math.max(Math.abs(dc),Math.abs(ddiff)));
                    if(maxDiff>pair.pixelLimit)continue;
                    int weight=Math.round(pair.baseWeight*trust);
                    if(weight<8)continue;
                    // Reference-weighted robust average cannot add sharpening or
                    // fabricate clipped highlights. Motion gate above is all-or-none
                    // for a 2x2 luma/chroma cell to avoid colour-luma disagreement.
                    sa+=clamp(aa,0,255)*weight;sb+=clamp(bb,0,255)*weight;
                    sc+=clamp(cc,0,255)*weight;sd+=clamp(dd,0,255)*weight;sw+=weight;
                    participation[k]+=4;contributed=true;
                    int vv=sampleUV(pair.data,w,h,x*0.5f+dx*0.5f,y*0.5f+dy*0.5f,0)-pair.correctionV;
                    int uu=sampleUV(pair.data,w,h,x*0.5f+dx*0.5f,y*0.5f+dy*0.5f,1)-pair.correctionU;
                    if(Math.max(Math.abs(vv-v),Math.abs(uu-u))<=pair.colourLimit) {
                        sv+=clamp(vv,0,255)*weight;su+=clamp(uu,0,255)*weight;suw+=weight;
                    }
                }
                if(contributed){out[at]=(byte)((sa+sw/2)/sw);out[at+1]=(byte)((sb+sw/2)/sw);
                    out[at+w]=(byte)((sc+sw/2)/sw);out[at+w+1]=(byte)((sd+sw/2)/sw);
                    out[uv]=(byte)((sv+suw/2)/suw);out[uv+1]=(byte)((su+suw/2)/suw);fused+=4;}
            }
        }
    }

    private static float interpolate(float[] values,int at,int stride,float fx,float fy) {
        float a=values[at]+(values[at+1]-values[at])*fx;
        float b=values[at+stride]+(values[at+stride+1]-values[at+stride])*fx;
        return a+(b-a)*fy;
    }
    private static float mean3(byte[] d,int w,int x,int y) {
        int i=y*w+x;
        return (u(d[i-w-1])+u(d[i-w])+u(d[i-w+1])+u(d[i-1])+u(d[i])+u(d[i+1])+
            u(d[i+w-1])+u(d[i+w])+u(d[i+w+1]))/9f;
    }
    private static float mean3At(byte[] d,int w,float x,float y) {
        int ix=(int)x,iy=(int)y;float fx=x-ix,fy=y-iy;
        if(fx==0f&&fy==0f)return mean3(d,w,ix,iy);
        float a=mean3(d,w,ix,iy),b=mean3(d,w,ix+1,iy);
        float c=mean3(d,w,ix,iy+1),e=mean3(d,w,ix+1,iy+1);
        return (a+(b-a)*fx)*(1-fy)+(c+(e-c)*fx)*fy;
    }
    private static int sampleY(byte[] d,int w,float x,float y) {
        int ix=(int)x,iy=(int)y;float fx=x-ix,fy=y-iy;
        int at=iy*w+ix;float a=u(d[at]),b=u(d[at+1]),c=u(d[at+w]),e=u(d[at+w+1]);
        return Math.round((a+(b-a)*fx)*(1-fy)+(c+(e-c)*fx)*fy);
    }
    private static int sampleUV(byte[] d,int w,int h,float x,float y,int channel) {
        x=Math.max(0,Math.min(w/2-1,x));y=Math.max(0,Math.min(h/2-1,y));
        int ix=(int)x,iy=(int)y,nx=Math.min(w/2-1,ix+1),ny=Math.min(h/2-1,iy+1);
        float fx=x-ix,fy=y-iy;int start=w*h;
        float a=u(d[start+iy*w+ix*2+channel]),b=u(d[start+iy*w+nx*2+channel]);
        float c=u(d[start+ny*w+ix*2+channel]),e=u(d[start+ny*w+nx*2+channel]);
        return Math.round((a+(b-a)*fx)*(1-fy)+(c+(e-c)*fx)*fy);
    }
    private static int median(int[] histogram,int count){int target=(count+1)/2,total=0;
        for(int i=0;i<histogram.length;i++){total+=histogram[i];if(total>=target)return i;}return 0;}
    private static int validate(byte[] frame,int w,int h) {
        long pixels=(long)w*h,bytes=pixels+pixels/2;
        if(w<2||h<2||(w&1)!=0||(h&1)!=0||bytes>Integer.MAX_VALUE||frame==null||frame.length!=bytes)
            throw new IllegalArgumentException("tight even-sized NV21 required");
        return (int)bytes;
    }
    private static int u(byte b){return b&255;}
    private static boolean finite(double v){return !Double.isInfinite(v)&&!Double.isNaN(v);}
    private static int clamp(int n,int low,int high){return Math.max(low,Math.min(high,n));}
    private static float unit(float n){return Math.max(0f,Math.min(1f,n));}
}
