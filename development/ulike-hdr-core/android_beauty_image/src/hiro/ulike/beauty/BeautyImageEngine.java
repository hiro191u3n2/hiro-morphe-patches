package hiro.ulike.beauty;

import ai.onnxruntime.OrtException;
import hiro.ulike.model.AndroidTensorEngine;
import hiro.ulike.model.OrtTensorEngine;
import hiro.ulike.model.PinnedModel;

/** Same-size, continuous-float SDR neural component. Does not implement the whole style. */
public final class BeautyImageEngine implements AutoCloseable {
    public static final String SDR_DOMAIN="encoded_sdr_full_range";
    private static final int SIDE=256,N=SIDE*SIDE;
    private final PinnedModel.Style style;
    private final OrtTensorEngine engine;
    private boolean closed;
    private BeautyImageEngine(PinnedModel.CompiledModel model,OrtTensorEngine engine){this.style=model.style;this.engine=engine;}
    public static BeautyImageEngine openAndroid(PinnedModel.CompiledModel model)throws Exception{
        require(model!=null,"pinned model required");return new BeautyImageEngine(model,AndroidTensorEngine.open(model));
    }
    /** Host QA only: caller must set ORT_DISABLE_TELEMETRY=1 before runtime loading. */
    public static BeautyImageEngine openHostForVerification(PinnedModel.CompiledModel model)throws OrtException{
        require(model!=null,"pinned model required");return new BeautyImageEngine(model,new OrtTensorEngine(model));
    }
    public interface SourceRgbFloat {
        int width();int height();
        /** Immutable frame token, e.g. a matched capture frame, retained through output. */
        Object frameIdentity();
        /** Immutable source during process. Writes exactly RGB to dst[0..2], unit encoded SDR. */
        void readPixel(int x,int y,float[] dst)throws Exception;
    }
    public interface TransactionalSink {
        /** Must create private staging storage, never a visible final image. */
        void begin(int width,int height,Object frameIdentity)throws Exception;
        /** Borrowed packed RGB rows. Copy/consume before return; do not retain this buffer. */
        void writeRows(int firstRow,int rows,float[] rgb)throws Exception;
        /** Atomically publish all rows; if this throws, nothing may become visible. */
        void commit()throws Exception;
        /** Discard staging and leave existing outputs unchanged; called for every failed transaction. */
        void abort()throws Exception;
    }
    public static final class Budget {
        public final long maxPixels,maxJavaWorkspaceBytes;
        public final int maxDimension,tileRows;
        public Budget(long maxPixels,int maxDimension,int tileRows,long maxJavaWorkspaceBytes){
            require(maxPixels>0 && maxDimension>0 && maxDimension<=16384 && tileRows>=1 && tileRows<=64 && maxJavaWorkspaceBytes>0,"invalid budget");
            this.maxPixels=maxPixels;this.maxDimension=maxDimension;this.tileRows=tileRows;this.maxJavaWorkspaceBytes=maxJavaWorkspaceBytes;
        }
        public static Budget standard(){return new Budget(25000000,16384,8,8L*1024*1024);}
    }
    public static final class Result {
        public final int width,height;public final Object frameIdentity;public final long javaArrayWorkspaceBound;
        public final PinnedModel.Style style;
        public final boolean hdrPreserved=false,completeStyle=false,nativePixelParity=false;
        Result(int w,int h,Object id,long bytes,PinnedModel.Style s){width=w;height=h;frameIdentity=id;javaArrayWorkspaceBound=bytes;style=s;}
    }
    static void require(boolean b,String message){if(!b)throw new IllegalArgumentException(message);}
    private static void finite(double x,String what){require(Double.isFinite(x),what);}
    private static void unit(double x,String what){require(Double.isFinite(x) && x>=0 && x<=1,what);}

    public synchronized Result process(SourceRgbFloat source,double[] landmarks106,PinnedAssets.FaceTemplate template,
                          PinnedAssets.NeuralMask mask,double intensity,String domain,Budget budget,TransactionalSink sink)throws Exception{
        require(template!=null,"pinned template required");
        return processWithTransform(source,cropMatrix(style,landmarks106,template.copy()),mask,intensity,domain,budget,sink);
    }
    /** Explicit geometry API: caller owns same-frame/orientation validity. No inferred preview landmarks. */
    public synchronized Result processWithTransform(SourceRgbFloat source,double[] sourceToCrop,PinnedAssets.NeuralMask mask,
                       double intensity,String domain,Budget budget,TransactionalSink sink)throws Exception{
        if(closed)throw new IllegalStateException("closed");
        require(source!=null && sink!=null && budget!=null && mask!=null && mask.style==style,"required source/sink/style mask");
        require(SDR_DOMAIN.equals(domain),"explicit encoded SDR model domain required");unit(intensity,"intensity");
        int w=source.width(),h=source.height();Object id=source.frameIdentity();
        require(w>0 && h>0 && w<=budget.maxDimension && h<=budget.maxDimension && (long)w*h<=budget.maxPixels && id!=null,"source dimensions/provenance budget");
        int rows=Math.min(h,budget.tileRows);
        // Bounds simultaneous module arrays (including input retained during ORT run,
        // raw output, FP64 generated crop, mask, template/matrix, pixel scratch and tile).
        // Does NOT include caller source/sink storage, model compilation, direct input,
        // JVM headers, ORT graph/activation/native allocations or allocator overhead.
        long workspace=3L*N*4+4L*N*4+4L*N*8+(long)w*rows*3*4+320L*320+16384;
        require(workspace<=budget.maxJavaWorkspaceBytes,"Java array workspace budget");
        double[] m=affine(sourceToCrop),inverse=invert(m);float[] pixel=new float[3];
        float[] tensor=new float[3*N];double[] rgb=new double[3];
        for(int y=0;y<SIDE;y++)for(int x=0;x<SIDE;x++){
            double sx=inverse[0]*x+inverse[1]*y+inverse[2],sy=inverse[3]*x+inverse[4]*y+inverse[5];
            sampleSource(source,w,h,sx,sy,pixel,rgb);
            for(int c=0;c<3;c++){double code=clamp(rgb[c],0,1)*255.0;float v=style==PinnedModel.Style.NATURAL_BLUSH?(float)(code*(double)0.0078f-1.0):((float)code-127.5f)*(1.0f/127.5f);tensor[c*N+y*SIDE+x]=v;}
        }
        float[] raw=engine.run(tensor);double[] generated=new double[4*N];float edge=Math.nextUp(1.0f);
        for(int c=0;c<4;c++)for(int i=0;i<N;i++){float v=raw[c*N+i];require(Float.isFinite(v) && v>=-edge && v<=edge,"Tanh output bounds");generated[i*4+c]=(clamp(v,-1,1)+1)*.5;}
        float[] tile=new float[w*rows*3];boolean begun=false;
        try {
            begun=true;sink.begin(w,h,id);
            for(int start=0;start<h;start+=rows){int count=Math.min(rows,h-start);
                for(int row=0;row<count;row++)for(int x=0;x<w;x++){
                    int y=start+row,off=(row*w+x)*3;read(source,x,y,pixel);
                    tile[off]=pixel[0];tile[off+1]=pixel[1];tile[off+2]=pixel[2];
                    double cx=m[0]*x+m[1]*y+m[2],cy=m[3]*x+m[4]*y+m[5];finite(cx,"project x");finite(cy,"project y");
                    if(cx<-.5 || cx>=255.5 || cy<-.5 || cy>=255.5 || intensity==0)continue;
                    double mr=sampleMask(mask,(cx+.5)*1.25-.5,(cy+.5)*1.25-.5);
                    double alpha=sampleGenerated(generated,cx,cy,3);
                    double weight=(style==PinnedModel.Style.NATURAL_BLUSH?Math.min(mr,alpha):mr*alpha)*intensity;
                    if(weight>0)for(int c=0;c<3;c++)tile[off+c]=(float)((double)pixel[c]*(1-weight)+sampleGenerated(generated,cx,cy,c)*weight);
                }
                sink.writeRows(start,count,tile);
            }
            require(source.width()==w && source.height()==h && source.frameIdentity()==id,"source identity/dimensions changed");
            sink.commit();begun=false;
        } catch(Exception|Error failure){if(begun)try{sink.abort();}catch(Exception|Error abort){failure.addSuppressed(abort);}throw failure;}
        return new Result(w,h,id,workspace,style);
    }
    private static void read(SourceRgbFloat src,int x,int y,float[] dst)throws Exception{
        // NaN poison catches a source failing to write all channels.
        dst[0]=dst[1]=dst[2]=Float.NaN;src.readPixel(x,y,dst);for(float v:dst)unit(v,"source SDR RGB sample");
    }
    private static void sampleSource(SourceRgbFloat source,int w,int h,double x,double y,float[] pixel,double[] out)throws Exception{
        finite(x,"crop x");finite(y,"crop y");x=clamp(x,-2,w+1);y=clamp(y,-2,h+1);int ix=(int)Math.floor(x),iy=(int)Math.floor(y);double fx=x-ix,fy=y-iy;out[0]=out[1]=out[2]=0;
        for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++){int xx=ix+dx,yy=iy+dy;double weight=(dx==0?1-fx:fx)*(dy==0?1-fy:fy);if(xx<0 || xx>=w || yy<0 || yy>=h)continue;read(source,xx,yy,pixel);for(int c=0;c<3;c++)out[c]+=pixel[c]*weight;}
    }
    private static double sampleGenerated(double[] data,double x,double y,int c){x=clamp(x,0,255);y=clamp(y,0,255);int ix=(int)Math.floor(x),iy=(int)Math.floor(y);double fx=x-ix,fy=y-iy,out=0;for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)out+=data[(Math.min(iy+dy,255)*256+Math.min(ix+dx,255))*4+c]*(dx==0?1-fx:fx)*(dy==0?1-fy:fy);return clamp(out,0,1);}
    private static double sampleMask(PinnedAssets.NeuralMask mask,double x,double y){x=clamp(x,0,319);y=clamp(y,0,319);int ix=(int)Math.floor(x),iy=(int)Math.floor(y);double fx=x-ix,fy=y-iy,out=0;for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)out+=mask.at(Math.min(ix+dx,319),Math.min(iy+dy,319))*(dx==0?1-fx:fx)*(dy==0?1-fy:fy);return clamp(out,0,1);}
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    static double[] affine(double[] value){require(value!=null && value.length==9,"3x3 affine matrix required");double[] m=value.clone();for(double v:m)finite(v,"finite affine");require(m[6]==0 && m[7]==0 && m[8]==1,"affine last row");double scale=Math.max(Math.max(Math.abs(m[0]),Math.abs(m[1])),Math.max(Math.abs(m[3]),Math.abs(m[4])));require(scale>0,"singular affine");double a=m[0]/scale,b=m[1]/scale,c=m[3]/scale,d=m[4]/scale,det=a*d-b*c;double norm=a*a+b*b+c*c+d*d;double maxEig=(norm+Math.sqrt(Math.max(0,norm*norm-4*det*det)))*.5;require(det!=0 && maxEig/Math.abs(det)<=1e12,"ill-conditioned affine");return m;}
    private static double[] invert(double[] m){double scale=Math.max(Math.max(Math.abs(m[0]),Math.abs(m[1])),Math.max(Math.abs(m[3]),Math.abs(m[4])));double a=m[0]/scale,b=m[1]/scale,c=m[3]/scale,d=m[4]/scale,det=a*d-b*c;double[] inv={d/det/scale,-b/det/scale,0,-c/det/scale,a/det/scale,0,0,0,1};inv[2]=-(inv[0]*m[2]+inv[1]*m[5]);inv[5]=-(inv[3]*m[2]+inv[4]*m[5]);for(double v:inv)finite(v,"inverse affine");return inv;}
    public static double[] cropMatrix(PinnedModel.Style style,double[] landmarks,double[] template){
        require(style!=null && landmarks!=null && template!=null && landmarks.length==212 && template.length==212,"106 point arrays required");double[] src=landmarks.clone(),dst=template.clone();double sx=0,sy=0,tx=0,ty=0;
        for(int i=0;i<106;i++){finite(src[2*i],"landmark x");finite(src[2*i+1],"landmark y");finite(dst[2*i],"template x");finite(dst[2*i+1],"template y");sx+=src[2*i];sy+=src[2*i+1];tx+=dst[2*i];ty+=dst[2*i+1];}sx/=106;sy/=106;tx/=106;ty/=106;
        double den=0,numA=0,numB=0;for(int i=0;i<106;i++){double x=src[2*i]-sx,y=src[2*i+1]-sy,u=dst[2*i]-tx,v=dst[2*i+1]-ty;den+=x*x+y*y;numA+=x*u+y*v;numB+=x*v-y*u;}require(Double.isFinite(den) && den>Double.MIN_NORMAL,"degenerate landmarks");double a=numA/den,b=numB/den;require(a*a+b*b>Double.MIN_NORMAL,"degenerate fit");double margin=style==PinnedModel.Style.NATURAL_BLUSH?(double).4f:(double).2f,k=256/(1+2*margin);return affine(new double[]{k*a,-k*b,k*(tx-a*sx+b*sy+margin),k*b,k*a,k*(ty-b*sx-a*sy+margin)+(style==PinnedModel.Style.NATURAL_BLUSH?15:0),0,0,1});
    }
    @Override public synchronized void close()throws OrtException{if(!closed){closed=true;engine.close();}}
}
