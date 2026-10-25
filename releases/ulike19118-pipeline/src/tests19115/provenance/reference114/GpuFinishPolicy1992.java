package com.hiro.ulike;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;
import java.util.Arrays;

/** Exact resident finishing policy. The caller already owns the private GPU
 * session and a whole-finish admission covering this versioned graph. Immutable
 * masks retain their original Java double sampling; one packed pair is uploaded
 * per coordinate. Float noise interpolation and all four final policy channels
 * are produced directly into the finish bank, without Java policy/readback. */
final class GpuFinishPolicy1992 {
    private GpuFinishPolicy1992() {}
    static final int GRID_SLOT=20,MASK_SLOT0=21,MASK_SLOT1=22;
    static final int CASES=24;
    static final String SUITE="finish-policy1998-exact-rowcache-packedmask-ieeediv-corpus-v1";
    private static volatile String verificationFailure="未検証";
    static String verificationFailure(){return verificationFailure;}
    static boolean ready(){return GpuCertificates1990.restore(SUITE)!=null;}
    private static Object field(Object owner,String name){
        try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(ReflectiveOperationException unavailable){throw new IllegalArgumentException("unsupported finish policy descriptor",unavailable);}
    }
    private static void check(){if(Thread.currentThread().isInterrupted())throw new CancellationException("resident finish policy interrupted");}
    private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
    /** Zero native commands on a declined domain. Source/output pixels and old
     * CPU mask callback traversal are never modified by this optional builder. */
    static Context open(GpuNoise1960.Session session,QualityPixels1932.Plan plan,int width,int height,int tileRows){
        if(session==null||!CpuFinishPolicy1978.immutable(plan)||width<1||height<1||tileRows<1||
                (long)width*Math.min(tileRows,height)>Integer.MAX_VALUE/4L||
                !finite(plan.sourceSigma)||plan.sourceSigma<0f||plan.sourceSigma>32f||
                !finite(plan.outputScale)||plan.outputScale<=0f||
                plan.sharpFloorQ8<0||plan.sharpFloorQ8>16384||plan.beautyQ8<0||plan.beautyQ8>256)return null;
        boolean noise=plan.localNoise!=null&&(plan.noiseMapAtOutput||Math.abs(plan.outputScale-1f)<.00001f);
        boolean face=plan.texturePriority&&plan.faceRegions!=null,smooth=plan.smoothedRegions!=null;
        if(!noise&&!face&&!smooth)return null; // original four-word constant is cheaper
        int[] u=new int[32];float[] f=new float[32];float[] grid=new float[]{0f};
        u[0]=3;u[1]=width;u[6]=noise?1:0;u[7]=plan.sharpFloorQ8;
        u[8]=Math.max(2,Math.round(Math.min(32f,plan.sourceSigma)*.85f));
        u[9]=Math.round((2f+Math.min(32f,plan.sourceSigma)*2.4f)*256f);u[10]=20+(plan.beautyQ8>>3);
        u[11]=face||smooth?0:1;
        f[0]=plan.sourceSigma;f[1]=!plan.noiseMapAtOutput&&plan.outputScale<1f?(float)Math.sqrt(plan.outputScale):1f;
        if(noise){
            SpatialNoise1934 map=plan.localNoise;
            if(map.width!=width||map.height!=height||map.columns<1||map.rows<1||(long)map.columns*map.rows>169)return null;
            try{
                grid=((float[])field(map,"sigma")).clone();
                if(grid.length!=map.columns*map.rows)return null;
                for(float value:grid)if(!finite(value)||value<0f||value>96f)return null;
                u[4]=map.columns;u[5]=map.rows;
                f[2]=((Float)field(map,"left")).floatValue();f[3]=((Float)field(map,"top")).floatValue();
                f[4]=((Float)field(map,"stepX")).floatValue();f[5]=((Float)field(map,"stepY")).floatValue();
                for(int i=2;i<=5;i++)if(!finite(f[i]))return null;
                if(map.columns>1&&f[4]<=0f||map.rows>1&&f[5]<=0f)return null;
            }catch(RuntimeException unavailable){return null;}
        }
        check();if(!GpuNoise1960.supports(GpuNoise1960.PROTECTION1961))return null;
        int capacity=Math.multiplyExact(width,Math.min(tileRows,height)),maskCapacity=face||smooth?capacity:1;
        // At most two masks, each with one X and one Y axis. Admit these arrays
        // before allocation and charge their real Java lifetime to this session.
        long axisBytes=face||smooth?32L*(width+height)+2048L:0;
        // Optional two horizontal double rows, for each of at most two masks
        // and four logical workers. Include arrays/objects conservatively. A
        // refused optional budget retries the exact pre-cache admission.
        long rowBytes=face||smooth?128L*width+4096L:0;
        GpuNoise1960.Lease1971 lease=session.reserveCapacity1983(new int[]{GRID_SLOT,MASK_SLOT0,MASK_SLOT1},
            new long[]{4L*grid.length,4L*maskCapacity,4L*maskCapacity},
            new long[]{4L*grid.length,4L*maskCapacity,4L*maskCapacity},4L*maskCapacity+axisBytes+512L+rowBytes);
        if(lease==null&&rowBytes!=0){
            rowBytes=0;lease=session.reserveCapacity1983(new int[]{GRID_SLOT,MASK_SLOT0,MASK_SLOT1},
                new long[]{4L*grid.length,4L*maskCapacity,4L*maskCapacity},
                new long[]{4L*grid.length,4L*maskCapacity,4L*maskCapacity},4L*maskCapacity+axisBytes+512L);
        }
        if(lease==null)return null;
        boolean transferred=false;
        try{
            int[] masks=new int[maskCapacity];lease.consumeJava1974(4L*maskCapacity);
            Sampler skin=face?sampler(plan.faceRegions,width,height,false):null;
            Sampler confidence=smooth?sampler(plan.smoothedRegions,width,height,true):null;
            if(axisBytes!=0)lease.consumeJava1974(axisBytes);
            RowSampler1998[] skinRows=rowBytes==0?null:rowSamplers1998(skin,width);
            RowSampler1998[] confidenceRows=rowBytes==0?null:rowSamplers1998(confidence,width);
            if(rowBytes!=0)lease.consumeJava1974(rowBytes);
            check();if(!lease.revalidate1971())return null;
            if(!session.run(new GpuNoise1960.Batch().uploadDirect(GRID_SLOT,grid)))throw new IllegalStateException("resident policy metadata upload failed");
            Context context=new Context(session,plan,width,height,capacity,u,f,masks,face,smooth,skin,confidence,lease,4L*grid.length,skinRows,confidenceRows);
            transferred=true;return context;
        }finally{if(!transferred)lease.close();}
    }
    static final class Context implements AutoCloseable {
        private final GpuNoise1960.Session session;private final QualityPixels1932.Plan plan;
        private final int width,height,capacity;private final int[] u,masks;private final float[] f;
        private final boolean face,smooth;private GpuNoise1960.Lease1971 lease;
        private final Sampler skinSampler,confidenceSampler;
        private final RowSampler1998[] skinRows,confidenceRows;
        final long metadataUploadBytes;private boolean closed,bandOpen;
        Context(GpuNoise1960.Session session,QualityPixels1932.Plan plan,int width,int height,int capacity,
                int[] u,float[] f,int[] masks,boolean face,boolean smooth,GpuNoise1960.Lease1971 lease,long uploaded){
            this(session,plan,width,height,capacity,u,f,masks,face,smooth,null,null,lease,uploaded);
        }
        Context(GpuNoise1960.Session session,QualityPixels1932.Plan plan,int width,int height,int capacity,
                int[] u,float[] f,int[] masks,boolean face,boolean smooth,Sampler skin,Sampler confidence,GpuNoise1960.Lease1971 lease,long uploaded){
            this(session,plan,width,height,capacity,u,f,masks,face,smooth,skin,confidence,lease,uploaded,null,null);
        }
        Context(GpuNoise1960.Session session,QualityPixels1932.Plan plan,int width,int height,int capacity,
                int[] u,float[] f,int[] masks,boolean face,boolean smooth,Sampler skin,Sampler confidence,GpuNoise1960.Lease1971 lease,long uploaded,
                RowSampler1998[] skinRows,RowSampler1998[] confidenceRows){
            this.session=session;this.plan=plan;this.width=width;this.height=height;this.capacity=capacity;this.u=u;this.f=f;
            this.masks=masks;this.face=face;this.smooth=smooth;this.lease=lease;metadataUploadBytes=uploaded;
            this.skinSampler=skin;this.confidenceSampler=confidence;this.skinRows=skinRows;this.confidenceRows=confidenceRows;
        }
        /** Caller has collected this bank before reuse; submit consumes this
         * single owned CPU array synchronously before Band.close allows refill. */
        synchronized Band prepare(int first,int last,int bank){
            if(closed||bandOpen||first<0||last<=first||last>height||bank<0||bank>1||
                    (long)width*(last-first)>capacity)throw new IllegalArgumentException("resident finish policy band ownership");
            check();long started=System.nanoTime();int count=width*(last-first);
            if(face||smooth){
                // Parallelize only immutable mask reads; SpeedWorkers joins all
                // writers before upload, including cancellation and exceptions.
                int workers=count<65536?1:Math.min(4,Math.min(last-first,SpeedWorkers1935.availableWorkers1944()));
                if(workers==1)pack(first,last,first,0);
                else {
                    Runnable[] jobs=new Runnable[workers];final int bandFirst=first;
                    for(int i=0;i<workers;i++){
                        final int from=first+(last-first)*i/workers,to=first+(last-first)*(i+1)/workers;
                        jobs[i]=new Runnable(){
                            private int worker1998;
                            Runnable configure1998(int worker){worker1998=worker;return this;}
                            public void run(){pack(from,to,bandFirst,worker1998);}
                        }.configure1998(i);
                    }
                    SpeedWorkers1935.run(jobs);
                }
            }else masks[0]=0;
            check();if(!lease.revalidate1971())throw new IllegalStateException("resident finish policy workspace unavailable");
            int[] parameters=u.clone();parameters[2]=last-first;parameters[3]=first;
            Band band=new Band(this,parameters,bank,count,System.nanoTime()-started,4L*masks.length);
            bandOpen=true;return band;
        }
        // Preserve the published private/synthetic access path. A legacy
        // three-argument caller has no logical-worker identity, so it retains
        // the original immutable sampler without any shared mutable cache.
        private void pack(int first,int last,int bandFirst){pack(first,last,bandFirst,-1);}
        private void pack(int first,int last,int bandFirst,int worker){
            int at=(first-bandFirst)*width;
            RowSampler1998 skinRow=worker<0||skinRows==null?null:skinRows[worker],confidenceRow=worker<0||confidenceRows==null?null:confidenceRows[worker];
            for(int y=first;y<last;y++){check();
                if(skinRow!=null)skinRow.prepare(y);if(confidenceRow!=null)confidenceRow.prepare(y);
                // The two extrema evaluate the original expression, with its
                // original rounding. Nonnegative interpolation is monotone:
                // if both extrema round alike, every pixel in this immutable
                // row has that same mask. Unknown callbacks retain traversal.
                int skinConstant=!face?0:skinRow!=null?skinRow.constant19114:
                    skinSampler!=null&&skinSampler.values==null?0:-1;
                int confidenceConstant=!smooth?0:confidenceRow!=null?confidenceRow.constant19114:
                    confidenceSampler!=null&&confidenceSampler.values==null?0:-1;
                if(skinConstant>256)throw new IllegalArgumentException("immutable skin outside exact packed domain");
                if(skinConstant>=0&&confidenceConstant>=0){
                    int packed=skinConstant|(confidenceConstant<<16),limit=at+width;
                    // Keep the old <=4096-pixel cancellation granularity even
                    // for a very wide admitted image.
                    while(at<limit){check();int next=at+Math.min(4096,limit-at);Arrays.fill(masks,at,next,packed);at=next;}
                    continue;
                }
                for(int x=0;x<width;x++,at++){
                if((x&4095)==0)check();
                int skin=skinConstant>=0?skinConstant:skinRow!=null?skinRow.sample(x):skinSampler==null?plan.skinAt(x,y):skinSampler.sample(x,y);
                if(skin<0||skin>256)throw new IllegalArgumentException("immutable skin outside exact packed domain");
                int confidence=confidenceConstant>=0?confidenceConstant:confidenceRow!=null?confidenceRow.sample(x):confidenceSampler==null?Math.max(0,Math.min(256,plan.smoothedRegions.smoothingQ8(x,y))):confidenceSampler.sample(x,y);
                masks[at]=skin|(confidence<<16);
            }}
        }
        public synchronized void close(){if(closed)return;closed=true;bandOpen=false;if(lease!=null){lease.close();lease=null;}}
    }
    /** Coordinate caches preserve the old Java-double expression order and
     * each original per-pixel bilinear/round operation. Only separable, pinned
     * immutable masks enter; unknown descriptors keep their old callbacks.
     * The optional worker-owned cache below reuses horizontal double
     * intermediates and proves constant rounded rows from conservative extrema.
     * No image pixel or mask result is reused across different coordinates
     * unless both original-rounded extrema prove that result identical. */
    interface Coordinate {double at(int index);}
    static final class Axis {
        final int[] lo,hi;final double[] fraction;final boolean horizontal;
        Axis(int length,boolean horizontal,int grid,int stride,Coordinate coordinate,boolean smoothing,int extent){
            this.horizontal=horizontal;lo=new int[length];hi=new int[length];fraction=new double[length];
            for(int i=0;i<length;i++){
                if((i&1023)==0)check();double p=coordinate.at(i);
                boolean invalid=smoothing?p<0||p>extent-1:p<-.5||p>grid-.5;
                if(Double.isNaN(p)||Double.isInfinite(p)||invalid){lo[i]=-1;continue;}
                p=smoothing?Math.max(0,Math.min(grid-1,(p-1.5)/4)):Math.max(0,Math.min(grid-1,p));
                int lower=(int)p;lo[i]=lower*stride;hi[i]=Math.min(grid-1,lower+1)*stride;fraction[i]=p-lower;
            }
        }
    }
    static final class Sampler {
        final byte[] values;final Axis x,y;final boolean smoothing;
        Sampler(byte[] values,Axis x,Axis y,boolean smoothing){this.values=values;this.x=x;this.y=y;this.smoothing=smoothing;}
        int sample(int column,int row){
            if(values==null)return 0;
            int xx=x.horizontal?column:row,yy=y.horizontal?column:row;
            int x0=x.lo[xx],y0=y.lo[yy];if(x0<0||y0<0)return 0;
            int x1=x.hi[xx],y1=y.hi[yy];double fx=x.fraction[xx],fy=y.fraction[yy];
            double top=(values[y0+x0]&255)*(1-fx)+(values[y0+x1]&255)*fx;
            double bottom=(values[y1+x0]&255)*(1-fx)+(values[y1+x1]&255)*fx;
            return smoothing?(int)Math.round(top*(1-fy)+bottom*fy):(int)Math.round((top*(1-fy)+bottom*fy)*256.0/255.0);
        }
    }
    static RowSampler1998[] rowSamplers1998(Sampler sampler,int width){
        if(sampler==null||sampler.values==null||!sampler.x.horizontal||sampler.y.horizontal||sampler.x.lo.length!=width)return null;
        RowSampler1998[] rows=new RowSampler1998[4];
        for(int i=0;i<rows.length;i++)rows[i]=new RowSampler1998(sampler,width);
        return rows;
    }
    /** One logical pack worker owns these two rows across bands. Context.prepare
     * is synchronized and SpeedWorkers joins every writer even on failure, so a
     * later band cannot race these arrays. Only the original horizontal double
     * expression is memoized. Vertical arithmetic and Math.round stay per pixel
     * except when the same expression on both extrema proves a constant row.
     * 90/270-degree axes and unknown/mutable callbacks never enter this class. */
    static final class RowSampler1998 {
        final Sampler source;double[] a,b,top,bottom;int keyA=-1,keyB=-1;double fy;boolean valid;
        double minimumA19114,maximumA19114,minimumB19114,maximumB19114;
        int constant19114=-1;
        long rowsBuilt; // One increment per filled row; no per-pixel telemetry.
        RowSampler1998(Sampler source,int width){this.source=source;a=new double[width];b=new double[width];}
        private void swap(){double[] values=a;a=b;b=values;int key=keyA;keyA=keyB;keyB=key;
            double minimum=minimumA19114,maximum=maximumA19114;
            minimumA19114=minimumB19114;maximumA19114=maximumB19114;
            minimumB19114=minimum;maximumB19114=maximum;}
        private void fill(double[] row,int offset){
            Axis x=source.x;byte[] values=source.values;double minimum=Double.POSITIVE_INFINITY,maximum=0;
            int column=0;
            for(;column<row.length;column++){
                if((column&4095)==0)check();int x0=x.lo[column];
                if(x0<0){row[column]=0;minimum=0;continue;}
                int x1=x.hi[column];double fx=x.fraction[column];
                double value=(values[offset+x0]&255)*(1-fx)+(values[offset+x1]&255)*fx;
                row[column]=value;minimum=Math.min(minimum,value);maximum=Math.max(maximum,value);
                if(maximum-minimum>1){
                    // A broad row is not worth scanning for a tight bound.
                    // Unsigned-byte interpolation with pinned [0,1] weights
                    // stays inside [0,256]. This wider interval can only
                    // decline shortcuts; finish with the old arithmetic loop.
                    minimum=0;maximum=256;column++;break;
                }
            }
            for(;column<row.length;column++){
                if((column&4095)==0)check();int x0=x.lo[column];
                if(x0<0){row[column]=0;continue;}
                int x1=x.hi[column];double fx=x.fraction[column];
                row[column]=(values[offset+x0]&255)*(1-fx)+(values[offset+x1]&255)*fx;
            }
            if(row==a){minimumA19114=minimum;maximumA19114=maximum;}
            else{minimumB19114=minimum;maximumB19114=maximum;}
            rowsBuilt++;
        }
        void prepare(int row){
            check();constant19114=-1;int y0=source.y.lo[row];valid=y0>=0;
            if(!valid){constant19114=0;return;}
            int y1=source.y.hi[row];fy=source.y.fraction[row];
            if(keyA!=y0){
                // Preserve either cached row needed by this pair, including
                // descending 180-degree traversal and nonconsecutive bands.
                if(keyB==y0||keyA==y1)swap();
                if(keyA!=y0){keyA=-1;fill(a,y0);keyA=y0;}
            }
            top=a;
            if(y1==y0)bottom=a;
            else{if(keyB!=y1){keyB=-1;fill(b,y1);keyB=y1;}bottom=b;}
            // Axis construction pins finite [0,1] fractions and unsigned-byte
            // masks. Keep the shortcut closed even if a corrupt descriptor
            // violates those bounds; NaNs fail these ordered comparisons.
            if(!(fy>=0&&fy<=1&&minimumA19114>=0&&maximumA19114<=256&&
                    (y1==y0||minimumB19114>=0&&maximumB19114<=256)))return;
            double lower=minimumA19114*(1-fy)+(y1==y0?minimumA19114:minimumB19114)*fy;
            double upper=maximumA19114*(1-fy)+(y1==y0?maximumA19114:maximumB19114)*fy;
            int low=source.smoothing?(int)Math.round(lower):(int)Math.round(lower*256.0/255.0);
            int high=source.smoothing?(int)Math.round(upper):(int)Math.round(upper*256.0/255.0);
            if(low==high)constant19114=low;
        }
        int sample(int column){
            if(!valid||source.x.lo[column]<0)return 0;
            double value=top[column]*(1-fy)+bottom[column]*fy;
            return source.smoothing?(int)Math.round(value):(int)Math.round(value*256.0/255.0);
        }
    }
    static Sampler sampler(Object mask,final int width,final int height,boolean smoothing){
        if(mask==null)return null;
        try {
            if(!smoothing){
                if(!(mask instanceof FaceRegions1934.Mask))return null;
                if(!((Boolean)field(mask,"reliable")).booleanValue())return new Sampler(null,null,null,false);
                if(((Integer)field(mask,"width")).intValue()!=width||((Integer)field(mask,"height")).intValue()!=height)return null;
                final double a=((Double)field(mask,"a")).doubleValue(),b=((Double)field(mask,"b")).doubleValue(),c=((Double)field(mask,"c")).doubleValue();
                final double d=((Double)field(mask,"d")).doubleValue(),e=((Double)field(mask,"e")).doubleValue(),f=((Double)field(mask,"f")).doubleValue();
                if(a!=0&&b!=0||d!=0&&e!=0)return null;
                boolean xHorizontal=b==0,yHorizontal=e==0;if(xHorizontal==yHorizontal)return null;
                int rw=((Integer)field(mask,"rw")).intValue(),rh=((Integer)field(mask,"rh")).intValue();
                byte[] values=(byte[])field(mask,"skin");if(rw<1||rh<1||values.length!=(long)rw*rh)return null;
                final boolean hx=xHorizontal,hy=yHorizontal;
                Axis x=new Axis(hx?width:height,hx,rw,1,new Coordinate(){public double at(int i){return a*(hx?i:0)+b*(hx?0:i)+c;}},false,rw);
                Axis y=new Axis(hy?width:height,hy,rh,rw,new Coordinate(){public double at(int i){return d*(hy?i:0)+e*(hy?0:i)+f;}},false,rh);
                return new Sampler(values,x,y,false);
            }
            boolean mapped=mask.getClass().getName().equals("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958$1");
            if(!mapped&&!mask.getClass().getName().equals("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958"))return null;
            Object owner=mapped?field(mask,"this$0"):mask;
            final int sw=((Integer)field(owner,"width")).intValue(),sh=((Integer)field(owner,"height")).intValue();
            int columns=((Integer)field(owner,"columns")).intValue(),rows=((Integer)field(owner,"rows")).intValue();
            final int turn=mapped?((Integer)field(mask,"val$turn")).intValue():0;
            if(mapped&&(((Integer)field(mask,"val$outputWidth")).intValue()!=width||((Integer)field(mask,"val$outputHeight")).intValue()!=height)||!mapped&&(sw!=width||sh!=height))return null;
            if(turn!=0&&turn!=90&&turn!=180&&turn!=270)return null;
            final boolean transformed=mapped;
            final double left=mapped?((Double)field(mask,"val$left")).doubleValue():0,top=mapped?((Double)field(mask,"val$top")).doubleValue():0;
            final double cw=mapped?((Double)field(mask,"val$cw")).doubleValue():width,ch=mapped?((Double)field(mask,"val$ch")).doubleValue():height;
            final boolean hx=turn==0||turn==180,hy=!hx;
            byte[] values=(byte[])field(owner,"confidence");if(columns<1||rows<1||values.length!=(long)columns*rows)return null;
            Axis x=new Axis(hx?width:height,hx,columns,1,new Coordinate(){public double at(int i){
                if(!transformed)return i;double p=hx?left+(i+.5)*cw/width-.5:top+(i+.5)*ch/height-.5;
                return turn==180||turn==270?sw-1-p:p;
            }},true,sw);
            Axis y=new Axis(hy?width:height,hy,rows,columns,new Coordinate(){public double at(int i){
                if(!transformed)return i;double p=hy?left+(i+.5)*cw/width-.5:top+(i+.5)*ch/height-.5;
                return turn==90||turn==180?sh-1-p:p;
            }},true,sh);
            return new Sampler(values,x,y,true);
        }catch(RuntimeException unavailable){return null;}
    }
    static final class Band implements AutoCloseable {
        final int mode=FinishPolicy1953.RAW4,pixels;final long cpuNanos,uploadBytes;
        private final Context owner;private final int[] u;private final int bank;private boolean appended,closed;
        Band(Context owner,int[] u,int bank,int pixels,long cpuNanos,long uploadBytes){this.owner=owner;this.u=u;this.bank=bank;this.pixels=pixels;this.cpuNanos=cpuNanos;this.uploadBytes=uploadBytes;}
        GpuNoise1960.Batch append(GpuNoise1960.Batch batch,int policySlot){
            synchronized(owner){
                if(batch==null||closed||appended||owner.closed||!owner.bandOpen||policySlot<0||policySlot>=25||
                        policySlot==GRID_SLOT||policySlot==MASK_SLOT0||policySlot==MASK_SLOT1)throw new IllegalArgumentException("resident policy append ownership");
                check();int maskSlot=bank==0?MASK_SLOT0:MASK_SLOT1;
                batch.uploadDirect(maskSlot,owner.masks).allocate(policySlot,16L*pixels)
                    .dispatch(GpuNoise1960.PROTECTION1961,new int[]{GRID_SLOT,policySlot,GRID_SLOT,GRID_SLOT,GRID_SLOT,maskSlot},u,owner.f,pixels);
                appended=true;return batch;
            }
        }
        public void close(){synchronized(owner){if(closed)return;closed=true;owner.bandOpen=false;}}
    }
    /** Bounded synthetic complete-policy proof included by the new whole-finish
     * startup suite. Every pixel and four channels are compared twice against
     * the original CPU builder, with actual immutable face/smoothing owners. */
    static boolean verify(GpuQualification1961.Cancellation cancellation){
        if(cancellation==null)return false;
        if(cancellation.cancelled())throw new CancellationException("finish policy verification interrupted");check();
        if(ready()){verificationFailure="";return true;}
        if(GpuCertificates1990.rejected(SUITE)){verificationFailure="policy prior mismatch";return false;}
        verificationFailure="policy verification incomplete";
        long cpuNanos=0,gpuNanos=0,pixels=0;int cases=0;
        try{
            for(int id=0;id<CASES;id++){
                if(cancellation==null||cancellation.cancelled())throw new CancellationException("finish policy verification interrupted");check();
                Fixture fixture=fixture(id);
                for(int pass=0;pass<2;pass++){
                    GpuNoise1960.Session session=GpuNoise1960.open();if(session==null){verificationFailure="policy session unavailable";return false;}
                    Context context=null;GpuNoise1960.Lease1971 outputLease=null;
                    try{
                        int tile=Math.min(fixture.height,pass==0?7:17);
                        long opened=System.nanoTime();context=open(session,fixture.plan,fixture.width,fixture.height,tile);gpuNanos+=System.nanoTime()-opened;
                        if(context==null){verificationFailure="policy descriptor unavailable case="+id;return false;}
                        outputLease=session.reserveCapacity1983(new int[]{3},new long[]{16L*fixture.width*tile},new long[]{0},16L*fixture.width*tile);
                        if(outputLease==null){verificationFailure="policy memory unavailable";return false;}
                        for(int first=0;first<fixture.height;first+=tile){
                            if(cancellation.cancelled())throw new CancellationException("finish policy verification interrupted");
                            int last=Math.min(fixture.height,first+tile),count=fixture.width*(last-first);
                            long started=System.nanoTime();Band band=context.prepare(first,last,0);int[][] actual;
                            try{actual=session.execute(band.append(new GpuNoise1960.Batch(),3),new int[]{3},new int[]{count*4});}
                            finally{band.close();}
                            gpuNanos+=System.nanoTime()-started;
                            if(actual==null){verificationFailure="policy transport unavailable";return false;}
                            started=System.nanoTime();FinishPolicy1953.Band expected=FinishPolicy1953.prepareCpu1961(fixture.plan,fixture.width,fixture.height,first,last,0);cpuNanos+=System.nanoTime()-started;
                            if(expected==null){verificationFailure="policy CPU reference unavailable";return false;}
                            try{for(int i=0;i<count;i++)for(int lane=0;lane<4;lane++){
                                int value=expected.mode==FinishPolicy1953.CONSTANT4?expected.words[lane]:expected.mode==FinishPolicy1953.RAW4?expected.words[i*4+lane]:expected.words[i*2+lane/2]>>>(16*(lane&1))&65535;
                                if(actual[0][i*4+lane]!=value){verificationFailure="policy mismatch case="+id+" x="+(i%fixture.width)+" y="+(first+i/fixture.width)+" channel="+lane;GpuCertificates1990.reject(SUITE,verificationFailure);return false;}
                            }}finally{expected.close();}
                            pixels+=count;
                        }
                        cases++;
                    }finally{if(outputLease!=null)outputLease.close();if(context!=null)context.close();session.close();}
                }
            }
            if(cancellation.cancelled())throw new CancellationException("finish policy verification interrupted");check();
            boolean saved=GpuCertificates1990.passed(SUITE,cases,pixels,cpuNanos,gpuNanos);
            verificationFailure=saved?"":"policy certificate unavailable";return saved;
        }catch(CancellationException cancelled){throw cancelled;}
        catch(ReflectiveOperationException unavailable){verificationFailure="policy fixture unavailable";return false;}
        catch(LinkageError unavailable){verificationFailure="policy backend unsupported";return false;}
        catch(OutOfMemoryError unavailable){verificationFailure="policy workspace unavailable";return false;}
        catch(RuntimeException unavailable){verificationFailure="policy verification exception";return false;}
    }
    static final class Fixture {final QualityPixels1932.Plan plan;final int width,height;Fixture(QualityPixels1932.Plan p,int w,int h){plan=p;width=w;height=h;}}
    static Fixture fixture(int id)throws ReflectiveOperationException{
        if(id<0||id>=CASES)throw new IllegalArgumentException("finish policy corpus id");
        return fixture(id,id==8?35:id%3==0?17:id%3==1?65:97,id==8?39:id%3==0?19:id%3==1?67:43);
    }
    static Fixture fixture(int id,int width,int height)throws ReflectiveOperationException{
        if(id<0||id>=CASES||width<1||height<1)throw new IllegalArgumentException("finish policy corpus shape");
        int sw=35,sh=39,turn=(id&3)*90;
        float sigma=new float[]{0f,.6f,3.8f,12f,24f,32f}[id%6],scale=id%5==0?.5f:id%5==1?1f:1.4f;
        QualityPixels1932.Plan plan=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(sigma,sigma,128f,0f,64),800,16666666L,0,.35f,4,1+id%4,true,true,scale);
        int columns=id%6==0?1:id%6==5?13:3,rows=id%6==1?1:id%6==5?13:3;
        float[] grid=new float[columns*rows];for(int i=0;i<grid.length;i++)grid[i]=new float[]{0f,.6000001f,3.8f,12.25f,31.999998f,64f,96f}[(i+id)%7];
        Constructor<SpatialNoise1934> noiseConstructor=SpatialNoise1934.class.getDeclaredConstructor(int.class,int.class,int.class,int.class,float.class,float.class,float.class,float.class,float[].class,QualityPixels1932.NoiseStats.class);
        noiseConstructor.setAccessible(true);
        SpatialNoise1934 noise=noiseConstructor.newInstance(width,height,columns,rows,.25f,.75f,columns==1?1f:(width-2f)/(columns-1),rows==1?1f:(height-3f)/(rows-1),grid,new QualityPixels1932.NoiseStats(sigma,sigma,128f,0f,64));
        plan=id%5==0?plan.withLocalNoise(noise,4):plan.withOutputNoise(noise);
        if(id%3!=0||id%5==0){
            Class<?> type=Class.forName("com.hiro.ulike.FaceRegions1934$Mask");
            Constructor<?> c=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,byte[].class,byte[].class,double.class,double.class,double.class,double.class,double.class,double.class,boolean.class);c.setAccessible(true);
            byte[] skin=new byte[63],detail=new byte[63];for(int i=0;i<skin.length;i++){skin[i]=(byte)((i*37+id*29)&255);detail[i]=(byte)((i*71+id)&255);}
            QualityPixels1932.RegionMask mask=(QualityPixels1932.RegionMask)c.newInstance(sw,sh,9,7,skin,detail,8.0/(sw-1),0.0,0.0,0.0,6.0/(sh-1),0.0,true);
            Method resample=type.getMethod("resample",int.class,int.class,int.class);plan=plan.withFaceRegions((QualityPixels1932.RegionMask)resample.invoke(mask,turn,width,height));
        }
        if(id%4!=0||id==8){
            Class<?> type=Class.forName("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958");Constructor<?> c=type.getDeclaredConstructor(int.class,int.class);c.setAccessible(true);Object mask=c.newInstance(sw,sh);
            byte[] confidence=(byte[])field(mask,"confidence");for(int i=0;i<confidence.length;i++)confidence[i]=(byte)((i*53+id*11)&255);
            Method output=type.getDeclaredMethod("outputMask",int.class,int.class,int.class);output.setAccessible(true);
            plan=plan.withSmoothedRegions((QualityPixels1932.SmoothMask)output.invoke(mask,turn,width,height));
        }
        return new Fixture(plan,width,height);
    }
}
