package review;

import com.hiro.ulike.hdr.color.P010SceneSource;
import com.hiro.ulike.hdr.color.SdrRendition;
import com.hiro.ulike.hdr.face.StillFaceAnalysis;
import com.hiro.ulike.hdr.input.*;
import hiro.ulike.beauty.BeautyImageEngine;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/** Independent synthetic-frame review, using the public owned-frame reader. */
public final class ColorReview {
    static long checks, comparisons, fractional;
    static double maxError;
    interface Throwing { void run() throws Exception; }
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void rejected(Throwing call)throws Exception{
        checks++;try{call.run();}catch(IllegalArgumentException|NullPointerException e){return;}
        throw new AssertionError("Expected explicit argument rejection");
    }
    static void near(double actual,double expected,double tolerance,String name){
        comparisons++;double error=Math.abs(actual-expected);maxError=Math.max(maxError,error);
        if(!Double.isFinite(actual)||error>tolerance*(1+Math.abs(expected)))
            throw new AssertionError(name+": "+actual+" vs "+expected);
    }
    static HdrFrame frame(int w,int h,boolean limited,int[][] codes)throws Exception{
        Object owner=new Object(),session=new Object(),reader=new Object(),request=new Object(),callback=new Object();
        CaptureMatch.Context context=new CaptureMatch.Context(owner,session,reader,request,callback,3,"0",null);
        CaptureMatch.Source source=new CaptureMatch.Source(owner,session,reader,"0",null,CaptureMatch.TimestampConvention.SENSOR_START_OF_EXPOSURE);
        CaptureMatch.Result result=new CaptureMatch.Result(context,request,101,7,null,"5",1000L,200);
        HdrFrame.Encoding encoding=limited?HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED:HdrFrame.Encoding.BT2020_NCL_HLG_FULL;
        ByteBuffer[] bytes=new ByteBuffer[3];P010FrameReader.Plane[] planes=new P010FrameReader.Plane[3];
        for(int c=0;c<3;c++){
            int pw=c==0?w:w/2,ph=c==0?h:h/2,stride=c==0?2:4;
            bytes[c]=ByteBuffer.allocate(pw*ph*stride).order(ByteOrder.LITTLE_ENDIAN);
            for(int i=0;i<codes[c].length;i++)bytes[c].putShort(i*stride,(short)(codes[c][i]<<6));
            planes[c]=new P010FrameReader.Plane(bytes[c],pw*stride,stride);
        }
        P010FrameReader.Input input=new P010FrameReader.Input(54,w,h,0,0,w,h,101,encoding,source,planes);
        HdrFrame out=P010FrameReader.copy(input,new P010FrameReader.Request(w,h,encoding,(long)w*h*3),context,result,()->context);
        // Simulate camera recycling borrowed buffers after copy; owned frame must survive.
        for(ByteBuffer b:bytes)for(int i=0;i<b.capacity();i++)b.put(i,(byte)0);
        return out;
    }
    static short[][] snapshot(HdrFrame frame){
        short[][] s=new short[3][];for(HdrFrame.Component c:HdrFrame.Component.values()){
            ShortBuffer b=frame.samples(c);s[c.ordinal()]=new short[b.remaining()];b.get(s[c.ordinal()]);
        }return s;
    }
    static void unchanged(HdrFrame frame,short[][] before){short[][] after=snapshot(frame);for(int c=0;c<3;c++)check(Arrays.equals(before[c],after[c]),"HDR pixels mutated");}
    static void runFixture(DataInputStream in)throws Exception{
        int w=in.readInt(),h=in.readInt();boolean limited=in.readBoolean(),hx=in.readBoolean(),vy=in.readBoolean();
        int[][] codes={new int[w*h],new int[w*h/4],new int[w*h/4]};
        for(int[] p:codes)for(int i=0;i<p.length;i++)p[i]=in.readUnsignedShort();
        double[] expected=new double[w*h*3];for(int i=0;i<expected.length;i++)expected[i]=in.readDouble();
        HdrFrame frame=frame(w,h,limited,codes);short[][] before=snapshot(frame);
        P010SceneSource source=new P010SceneSource(frame,hx?P010SceneSource.ChromaLocation.MIDPOINT:P010SceneSource.ChromaLocation.COSITED,vy?P010SceneSource.ChromaLocation.MIDPOINT:P010SceneSource.ChromaLocation.COSITED,"review-synthetic-siting");
        check(source.frameIdentity()==frame && source.width()==w && source.height()==h,"scene frame identity");
        double[] pixel=new double[5];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            pixel[3]=17;pixel[4]=-9;source.readPixel(x,y,pixel);
            for(int c=0;c<3;c++)near(pixel[c],expected[(y*w+x)*3+c],3e-13,"independent BT2100 pixel");
            check(pixel[3]==17&&pixel[4]==-9,"write past RGB");
        }
        int stride=w*3+7,offset=3;double[] rows=new double[offset+stride*h+5];Arrays.fill(rows,-991);
        source.readRows(0,h,rows,offset,stride);
        for(int i=0;i<rows.length;i++){
            int row=(i-offset)/stride,col=(i-offset)%stride;
            boolean active=i>=offset&&row<h&&col>=0&&col<w*3;
            if(active)near(rows[i],expected[row*w*3+col],3e-13,"row tile");
            else check(rows[i]==-991,"row padding overwritten");
        }
        double[] lastRow=new double[w*3];source.readRows(h-1,1,lastRow,0,w*3);
        for(int i=0;i<lastRow.length;i++)near(lastRow[i],expected[(h-1)*w*3+i],3e-13,"partial row");
        int exposures=in.readInt();
        for(int e=0;e<exposures;e++){
            double exposure=in.readDouble();SdrRendition sdr=new SdrRendition(source,exposure);
            BeautyImageEngine.SourceRgbFloat modelSource=sdr;StillFaceAnalysis.ProxyRenderer renderer=sdr;
            check(modelSource.frameIdentity()==frame&&sdr.hdrSource()==source,"SDR/HDR identity");
            check(sdr.policyName().contains(Double.toHexString(exposure))&&sdr.policyName().length()<=256,"declared exposure policy");
            float[] fp=new float[4];fp[3]=77;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                modelSource.readPixel(x,y,fp);int packed=renderer.rgb8(frame,x,y);int want=in.readInt();
                check(packed==want,"separate RGB8 quantization");
                for(int c=0;c<3;c++){
                    double wantFloat=in.readDouble();near(fp[c],wantFloat,7e-8,"fractional SDR rendition");
                    check(Float.isFinite(fp[c])&&fp[c]>=0&&fp[c]<=1,"SDR bounds");
                    if(fp[c]>0&&fp[c]<1&&Math.abs(fp[c]*255-Math.rint(fp[c]*255))>.002)fractional++;
                }
                check(fp[3]==77,"SDR write past RGB");
            }
            rejected(()->sdr.rgb8(frame(w,h,limited,codes),0,0));
            rejected(()->sdr.rgb8(null,0,0));
            rejected(()->sdr.readPixel(0,0,new float[2]));
            rejected(()->sdr.readPixel(w,0,new float[3]));
        }
        rejected(()->source.readPixel(-1,0,pixel));rejected(()->source.readPixel(0,h,pixel));
        rejected(()->source.readPixel(0,0,null));rejected(()->source.readPixel(0,0,new double[2]));
        rejected(()->source.readRows(0,0,rows,0,stride));rejected(()->source.readRows(-1,1,rows,0,stride));
        rejected(()->source.readRows(h-1,2,rows,0,stride));rejected(()->source.readRows(0,1,rows,-1,stride));
        rejected(()->source.readRows(0,1,rows,0,w*3-1));rejected(()->source.readRows(0,h,rows,Integer.MAX_VALUE,stride));
        double[] sentinel={11,12,13};rejected(()->source.readRows(0,1,sentinel,0,w*3));check(Arrays.equals(sentinel,new double[]{11,12,13}),"validation before writes");
        rejected(()->new P010SceneSource(frame,null,source.vertical,"source"));
        rejected(()->new P010SceneSource(frame,source.horizontal,source.vertical," "));
        rejected(()->new P010SceneSource(frame,source.horizontal,source.vertical,new String(new char[81]).replace('\0','a')));
        for(double bad:new double[]{Double.NaN,Double.NEGATIVE_INFINITY,0,-1,1e-7,1000001})rejected(()->new SdrRendition(source,bad));
        unchanged(frame,before);
        if(w==1024){
            double[] v=new double[3];source.readPixel(limited?64:0,0,v);near(v[0],0,0,"black anchor");
            source.readPixel(limited?940:1023,0,v);near(v[0],1.0000000269348075,1e-13,"white anchor not rounded/clamped");
            if(limited){source.readPixel(63,0,v);check(v[0]<0,"subblack clipped");source.readPixel(941,0,v);check(v[0]>1,"superwhite clipped");}
        }else{
            boolean negative=false,over=false;for(double v:expected){negative|=v<0;over|=v>1;}check(negative&&over,"chromatic HDR range fixture");
        }
    }
    public static void main(String[] args)throws Exception{
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])))){
            int fixtures=in.readInt();check(fixtures==10,"fixture count");for(int i=0;i<fixtures;i++)runFixture(in);check(in.read()==-1,"fixture trailing bytes");
        }
        check(Double.doubleToRawLongBits(P010SceneSource.inverseHlg(-0.0))==Long.MIN_VALUE,"negative zero survives");
        near(P010SceneSource.inverseHlg(.5),1.0/12,0,"HLG knee");
        near(P010SceneSource.inverseHlg(.75),.26496256042100724,1e-15,"HLG 75percent");
        for(double x:new double[]{.01,.5,1,2,10})near(P010SceneSource.inverseHlg(-x),-P010SceneSource.inverseHlg(x),0,"explicit reflected extension");
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.MAX_VALUE})rejected(()->P010SceneSource.inverseHlg(bad));
        check(fractional>10000,"model path retains fractional float values");
        System.out.println("{\"checks\":"+checks+",\"comparisons\":"+comparisons+",\"fractional_sdr_values\":"+fractional+",\"maximum_absolute_error\":"+maxError+"}");
    }
}
