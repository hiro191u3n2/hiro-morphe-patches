package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import java.lang.reflect.Field;

/** Mandatory GPU protection. Java copies detector/immutable descriptors only.
 * No image-dependent mask, interpolation, geometry, or finishing oracle executes
 * on the host. Unsupported custom callbacks are rejected explicitly. */
public final class GpuRequiredPolicy1965 {
    private GpuRequiredPolicy1965() {}
    private static final double[] TRIG_COEFFICIENTS={-1.0/121645100408832000.0,1.0/355687428096000.0,-1.0/1307674368000.0,1.0/6227020800.0,-1.0/39916800.0,1.0/362880.0,-1.0/5040.0,1.0/120.0,-1.0/6.0,1.0/2432902008176640000.0,-1.0/6402373705728000.0,1.0/20922789888000.0,-1.0/87178291200.0,1.0/479001600.0,-1.0/3628800.0,1.0/40320.0,-1.0/720.0,1.0/24.0,-1.0/2.0};
    private static final int HARDWARE_SHADER=40, SOFTWARE_SHADER_BASE=48;
    private static void require(boolean ok) {
        if(!ok)throw new GpuRequiredFailure1965("policy","Required FP64 GPU protection failed");
    }
    private static void capability() {
        require(!Thread.currentThread().isInterrupted());shader(1);
    }
    public static void requireAvailable(){capability();}
    private static int shader(int mode) {
        if(GpuNoise1960.fp64Verified1965()&&GpuNoise1960.supports(HARDWARE_SHADER))return HARDWARE_SHADER;
        if(GpuNoise1960.soft64Verified1965()&&GpuNoise1960.supports(SOFTWARE_SHADER_BASE+mode))return SOFTWARE_SHADER_BASE+mode;
        throw new GpuRequiredFailure1965("policy","Required GPU binary64 protection unavailable");
    }
    private static Object field(Object object,String name) {
        try {Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
        catch(ReflectiveOperationException error){throw new GpuRequiredFailure1965("policy","Unsupported immutable mask descriptor: "+name,error);}
    }
    private static int[] words(double[] values) {
        int[] result=new int[values.length*2];
        for(int i=0;i<values.length;i++){long b=Double.doubleToRawLongBits(values[i]);result[i*2]=(int)b;result[i*2+1]=(int)(b>>>32);}
        return result;
    }
    /** Pure detector metadata; sampling through the CPU interface is forbidden. */
    public static final class GpuFaceMask implements QualityPixels1932.RegionMask {
        final int width,height,sourceWidth,sourceHeight,rw,rh,turn;
        final int[] raster,chain;
        final boolean reliable;
        GpuFaceMask(int w,int h,int sw,int sh,int rw,int rh,int turn,int[] raster,int[] chain,boolean reliable) {
            width=w;height=h;sourceWidth=sw;sourceHeight=sh;this.rw=rw;this.rh=rh;
            this.turn=turn;this.raster=raster;this.chain=chain;this.reliable=reliable;
        }
        public int skinQ8(int x,int y){throw GpuRequiredFailure1965.forbidden("CPU face mask sample");}
        public int detailQ8(int x,int y){throw GpuRequiredFailure1965.forbidden("CPU detail mask sample");}
    }
    public static QualityPixels1932.RegionMask resampleFace(QualityPixels1932.RegionMask mask,int rotation,int w,int h) {
        if(mask==null)return null;
        if(!(mask instanceof GpuFaceMask))throw GpuRequiredFailure1965.forbidden("Unsupported face resize mask");
        GpuFaceMask m=(GpuFaceMask)mask;
        require(w>0&&h>0&&(rotation==0||rotation==90||rotation==180||rotation==270)&&m.chain.length<12);
        int[] c=new int[m.chain.length+3];System.arraycopy(m.chain,0,c,0,m.chain.length);
        c[c.length-3]=rotation;c[c.length-2]=w;c[c.length-1]=h;
        return new GpuFaceMask(w,h,m.sourceWidth,m.sourceHeight,m.rw,m.rh,m.turn,m.raster,c,m.reliable);
    }
    public static final class GpuSmoothMask implements QualityPixels1932.SmoothMask {
        final int width,height,turn,outputWidth,outputHeight;
        final byte[] confidence;
        GpuSmoothMask(int w,int h,byte[] c,int t,int ow,int oh){width=w;height=h;confidence=c;turn=t;outputWidth=ow;outputHeight=oh;}
        public int smoothingQ8(int x,int y){throw GpuRequiredFailure1965.forbidden("CPU smooth mask sample");}
    }
    public static QualityPixels1932.SmoothMask smoothMask(int width,int height,byte[] confidence) {
        require(width>0&&height>0&&confidence!=null&&confidence.length==(long)((width+3)/4)*((height+3)/4));
        return new GpuSmoothMask(width,height,confidence,0,width,height);
    }
    public static QualityPixels1932.SmoothMask resampleSmooth(QualityPixels1932.SmoothMask mask,int turn,int width,int height) {
        if(mask==null)return null;require(mask instanceof GpuSmoothMask&&width>0&&height>0&&(turn==0||turn==90||turn==180||turn==270));
        GpuSmoothMask s=(GpuSmoothMask)mask;
        require(s.turn==0&&s.outputWidth==s.width&&s.outputHeight==s.height);
        return new GpuSmoothMask(s.width,s.height,s.confidence,turn,width,height);
    }
    /** Android's existing detector is a separate native SDK stage. GPU computes
     * all anchor-derived protection geometry and source-pixel vetoes. */
    public static QualityPixels1932.RegionMask forBitmap(Bitmap input,int turn) {
        require(input!=null&&!input.isRecycled()&&(turn==0||turn==90||turn==180||turn==270));
        int w=input.getWidth(),h=input.getHeight();
        if(w<32||h<32)return new GpuFaceMask(w,h,w,h,0,0,turn,new int[]{0},new int[0],false);
        int uw=turn==90||turn==270?h:w,uh=turn==90||turn==270?w:h;
        // Probe dimensions and drawing are the existing Android detector input
        // contract, not an alternative CPU protection calculation.
        double scale=Math.min(1.0,640.0/Math.max(uw,uh));
        int pw=Math.max(2,((int)Math.round(uw*scale))&~1),ph=Math.max(2,(int)Math.round(uh*scale));
        Bitmap probe=Bitmap.createBitmap(pw,ph,Bitmap.Config.RGB_565);
        try {
            Matrix matrix=new Matrix();float sx=(float)pw/uw,sy=(float)ph/uh;
            float[] v=turn==90?new float[]{0,-sx,h*sx,sy,0,0,0,0,1}:
                turn==180?new float[]{-sx,0,w*sx,0,-sy,h*sy,0,0,1}:
                turn==270?new float[]{0,sx,0,-sy,0,w*sy,0,0,1}:new float[]{sx,0,0,0,sy,0,0,0,1};
            matrix.setValues(v);new Canvas(probe).drawBitmap(input,matrix,new Paint(Paint.FILTER_BITMAP_FLAG));
            FaceDetector.Face[] faces=new FaceDetector.Face[8];
            int count=new FaceDetector(pw,ph,8).findFaces(probe,faces);
            if(count<=0||count>8)return new GpuFaceMask(w,h,w,h,0,0,turn,new int[]{0},new int[0],false);
            FaceRegions1934Pixels.Anchor[] anchors=new FaceRegions1934Pixels.Anchor[count];
            for(int i=0;i<count;i++)if(faces[i]!=null){PointF p=new PointF();faces[i].getMidPoint(p);
                anchors[i]=new FaceRegions1934Pixels.Anchor(p.x,p.y,faces[i].eyesDistance(),faces[i].pose(FaceDetector.Face.EULER_Z),faces[i].pose(FaceDetector.Face.EULER_Y),faces[i].confidence());}
            int[] pixels=new int[pw*ph];probe.getPixels(pixels,0,pw,0,0,pw,ph);
            int[] raster=faceRaster(pixels,pw,ph,anchors);
            boolean reliable=raster[raster.length-1]!=0;
            return new GpuFaceMask(w,h,w,h,pw,ph,turn,raster,new int[0],reliable);
        } finally {probe.recycle();}
    }
    /** Returns skin/detail words plus one reliable word, computed on GPU. */
    public static int[] faceRaster(int[] source,int width,int height,FaceRegions1934Pixels.Anchor[] faces) {
        require(source!=null&&width>=2&&height>=2&&width<=640&&height<=640&&source.length==(long)width*height);
        require(faces!=null&&faces.length<=8);capability();
        double[] m=new double[128];System.arraycopy(TRIG_COEFFICIENTS,0,m,64,TRIG_COEFFICIENTS.length);
        for(int i=0;i<faces.length;i++){FaceRegions1934Pixels.Anchor a=faces[i];int p=i*6;
            if(a==null){m[p+5]=-1;continue;}m[p]=a.x;m[p+1]=a.y;m[p+2]=a.eyes;m[p+3]=a.roll;m[p+4]=a.yaw;m[p+5]=a.confidence;}
        int[] u=new int[32];u[0]=0;u[1]=width;u[2]=height;u[23]=10;u[24]=8;u[29]=faces.length;u[30]=source.length;
        return standalone(source,u,m,new int[]{0},new float[]{0},new int[]{0},source.length*2+1,source.length);
    }
    private static final class Descriptor {
        final int[] u=new int[32];final double[] m=new double[64];
        int[] raster=new int[]{0},smooth=new int[]{0};float[] grid=new float[]{0};
    }
    private static Descriptor descriptor(QualityPixels1932.Plan plan,int width,int rows,int firstY,boolean strong,int mode) {
        require(width>0&&rows>0&&firstY>=0&&(long)width*rows<=Integer.MAX_VALUE/4);
        Descriptor d=new Descriptor();int[] u=d.u;double[] m=d.m;
        u[0]=mode;u[1]=width;u[2]=rows;u[3]=firstY;u[30]=width*rows;
        m[6]=plan==null?0:plan.sourceSigma;m[7]=plan==null?1:plan.outputScale;
        u[12]=plan==null?0:plan.beautyQ8;u[13]=plan==null?256:plan.shadowBudgetQ8;u[14]=strong?1:0;
        u[15]=plan!=null&&plan.texturePriority?1:0;u[16]=plan==null?0:plan.sharpFloorQ8;u[17]=plan!=null&&plan.noiseMapAtOutput?1:0;
        if(plan!=null&&plan.faceRegions!=null) {
            Object mask=plan.faceRegions;
            if(mask instanceof GpuFaceMask) {
                GpuFaceMask f=(GpuFaceMask)mask;u[4]=f.sourceWidth;u[5]=f.sourceHeight;u[6]=f.rw;u[7]=f.rh;u[8]=f.reliable?1:0;
                u[27]=f.turn;u[28]=f.chain.length/3;d.raster=f.raster;
                for(int i=0;i<f.chain.length;i++)m[12+i]=f.chain[i];
            } else if(mask.getClass().getName().equals("com.hiro.ulike.FaceRegions1934$Mask")&&mask.getClass().getClassLoader()==GpuRequiredPolicy1965.class.getClassLoader()) {
                // Existing immutable coefficients are copied, never re-sampled.
                u[4]=(Integer)field(mask,"width");u[5]=(Integer)field(mask,"height");u[6]=(Integer)field(mask,"rw");u[7]=(Integer)field(mask,"rh");u[8]=(Boolean)field(mask,"reliable")?1:0;u[26]=1;
                String[] names={"a","b","c","d","e","f"};for(int i=0;i<6;i++)m[i]=(Double)field(mask,names[i]);
                if(u[8]!=0){byte[] s=(byte[])field(mask,"skin"),v=(byte[])field(mask,"detail");d.raster=new int[s.length*2];for(int i=0;i<s.length;i++){d.raster[i*2]=s[i]&255;d.raster[i*2+1]=v[i]&255;}}
            } else throw GpuRequiredFailure1965.forbidden("Unknown face mask callback");
        }
        if(plan!=null&&plan.localNoise!=null) {
            SpatialNoise1934 n=plan.localNoise;u[9]=n.columns;u[10]=n.rows;u[11]=1;
            d.grid=((float[])field(n,"sigma")).clone();m[8]=(Float)field(n,"left");m[9]=(Float)field(n,"top");m[10]=(Float)field(n,"stepX");m[11]=(Float)field(n,"stepY");
        }
        if(plan!=null&&plan.smoothedRegions!=null) {
            Object s=plan.smoothedRegions;String name=s.getClass().getName();
            if(s instanceof GpuSmoothMask) {
                GpuSmoothMask sm=(GpuSmoothMask)s;u[18]=sm.width;u[19]=sm.height;u[20]=(sm.width+3)/4;u[21]=(sm.height+3)/4;
                u[22]=sm.turn;u[23]=sm.outputWidth;u[24]=sm.outputHeight;u[25]=1;
                d.smooth=new int[sm.confidence.length];for(int i=0;i<sm.confidence.length;i++)d.smooth[i]=sm.confidence[i]&255;
                return d;
            }
            if(name.equals("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958$1")) {
                u[22]=(Integer)field(s,"val$turn");u[23]=(Integer)field(s,"val$outputWidth");u[24]=(Integer)field(s,"val$outputHeight");s=field(s,"this$0");
            } else if(!name.equals("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958"))throw GpuRequiredFailure1965.forbidden("Unknown smoothing mask callback");
            require(s.getClass().getClassLoader()==GpuRequiredPolicy1965.class.getClassLoader());
            u[18]=(Integer)field(s,"width");u[19]=(Integer)field(s,"height");u[20]=(Integer)field(s,"columns");u[21]=(Integer)field(s,"rows");u[25]=1;
            if(u[23]==0){u[23]=u[18];u[24]=u[19];}
            byte[] cells=(byte[])field(s,"confidence");d.smooth=new int[cells.length];for(int i=0;i<cells.length;i++)d.smooth[i]=cells[i]&255;
        }
        return d;
    }
    /** Caller owns all seven slots; arrays remain immutable through run(). */
    public static boolean preparePolicy1965(GpuNoise1960.Session session,QualityPixels1932.Plan plan,int width,int rows,int firstY,boolean strong,int policySlot,int sigmaSlot,int metadataSlot,int rasterSlot,int gridSlot,int smoothSlot) {
        capability();Descriptor d=descriptor(plan,width,rows,firstY,strong,1);
        require(session!=null);
        GpuNoise1960.Batch b=new GpuNoise1960.Batch();
        b.allocate(policySlot,8L*d.u[30]).allocate(sigmaSlot,4L*d.u[30]).upload(metadataSlot,words(d.m)).upload(rasterSlot,d.raster).upload(gridSlot,d.grid).upload(smoothSlot,d.smooth);
        int[] prep=d.u.clone();prep[0]=5;b.dispatch(shader(5),new int[]{rasterSlot,policySlot,metadataSlot,rasterSlot,gridSlot,smoothSlot,sigmaSlot},prep,new float[32],1)
            .dispatch(shader(d.u[0]),new int[]{rasterSlot,policySlot,metadataSlot,rasterSlot,gridSlot,smoothSlot,sigmaSlot},d.u,new float[32],d.u[30]);
        require(session.run(b));return true;
    }
    public static int[] protection(QualityPixels1932.Plan plan,int width,int rows,int firstY,boolean strong) {
        capability();Descriptor d=descriptor(plan,width,rows,firstY,strong,1);
        return standalone(new int[]{0},d.u,d.m,d.raster,d.grid,d.smooth,d.u[30]*2,d.u[30]);
    }
    public static int[] finishRaw(QualityPixels1932.Plan plan,int width,int rows,int first,int last,int originY) {
        require(plan!=null&&first>=0&&last>first&&last<=rows);
        capability();Descriptor d=descriptor(plan,width,last-first,first+originY,false,2);
        return standalone(new int[]{0},d.u,d.m,d.raster,d.grid,d.smooth,d.u[30]*4,d.u[30]);
    }
    public static FinishPolicy1953.Band finishBand(QualityPixels1932.Plan plan,int width,int rows,int first,int last,int originY) {
        int[] raw=finishRaw(plan,width,rows,first,last,originY);
        // Representation only; every actual policy value is already GPU output.
        return FinishPolicy1953.fromRaw1961(raw,width*(last-first));
    }
    public static FinishPolicy1953.Band finishBand(GpuNoise1960.Session session,QualityPixels1932.Plan plan,int width,int rows,int first,int last,int originY) {
        require(session!=null&&plan!=null&&first>=0&&last>first&&last<=rows);capability();
        Descriptor d=descriptor(plan,width,last-first,first+originY,false,2);
        GpuNoise1960.Batch b=new GpuNoise1960.Batch();
        b.allocate(14,4).allocate(15,16L*d.u[30]).upload(16,words(d.m)).upload(17,d.raster).upload(18,d.grid).upload(19,d.smooth).allocate(20,4);
        int[] prep=d.u.clone();prep[0]=5;b.dispatch(shader(5),new int[]{14,15,16,17,18,19,20},prep,new float[32],1)
            .dispatch(shader(d.u[0]),new int[]{14,15,16,17,18,19,20},d.u,new float[32],d.u[30]);
        int[][] result=session.execute(b,new int[]{15},new int[]{d.u[30]*4});require(result!=null&&result[0]!=null);
        return FinishPolicy1953.fromRaw1961(result[0],d.u[30]);
    }
    public static int[] smoothCells(int[] input,int width,int stripRows,int begin,int end,int origin,int fullHeight,int[] policy,int[] confidence) {
        require(input!=null&&policy!=null&&confidence!=null&&width>0&&begin>=0&&end>begin&&end<=stripRows&&origin>=0);
        capability();int[] u=new int[32];u[0]=3;u[1]=width;u[2]=stripRows;u[3]=begin;u[4]=end;u[5]=origin;u[6]=fullHeight;u[7]=(width+3)/4;u[30]=u[7]*((end-begin+3)/4);
        return standalone(input,u,new double[64],policy,new float[]{0},confidence,u[30],u[30]);
    }
    public static int[] smoothCells(GpuNoise1960.Session session,int[] input,int width,int stripRows,int begin,int end,int origin,int fullHeight,int[] policy,int[] confidence) {
        require(session!=null&&input!=null&&policy!=null&&confidence!=null&&width>0&&begin>=0&&end>begin&&end<=stripRows&&origin>=0);
        capability();int[] u=new int[32];u[0]=3;u[1]=width;u[2]=stripRows;u[3]=begin;u[4]=end;u[5]=origin;u[6]=fullHeight;u[7]=(width+3)/4;u[30]=u[7]*((end-begin+3)/4);
        GpuNoise1960.Batch b=new GpuNoise1960.Batch();
        b.upload(14,input).allocate(15,4L*u[30]).upload(17,policy).upload(18,confidence).allocate(19,4)
            .dispatch(shader(3),new int[]{14,15,19,17,19,18,19},u,new float[32],u[30]);
        int[][] read=session.execute(b,new int[]{15},new int[]{u[30]});require(read!=null&&read[0]!=null);return read[0];
    }
    private static int[] standalone(int[] source,int[] u,double[] m,int[] raster,float[] grid,int[] smooth,int count,int invocations) {
        GpuNoise1960.Session s=GpuNoise1960.open();require(s!=null);
        try {GpuNoise1960.Batch b=new GpuNoise1960.Batch();
            b.upload(0,source).allocate(1,4L*count).upload(2,words(m)).upload(3,raster).upload(4,grid).upload(5,smooth).allocate(6,4L*Math.max(1,invocations));
            if(u[0]==0&&u[29]>0){int[] prepare=u.clone();prepare[0]=4;b.dispatch(shader(4),new int[]{0,1,2,3,4,5,6},prepare,new float[32],u[29]);}
            if(u[0]==1||u[0]==2){int[] prepare=u.clone();prepare[0]=5;b.dispatch(shader(5),new int[]{0,1,2,3,4,5,6},prepare,new float[32],1);}
            b.dispatch(shader(u[0]),new int[]{0,1,2,3,4,5,6},u,new float[32],invocations);
            int[][] r=s.execute(b,new int[]{1},new int[]{count});require(r!=null&&r.length==1&&r[0]!=null);return r[0];
        } finally {s.close();}
    }
}
