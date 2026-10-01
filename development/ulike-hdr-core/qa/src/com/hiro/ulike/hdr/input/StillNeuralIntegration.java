package com.hiro.ulike.hdr.input;

import com.hiro.ulike.hdr.color.P010SceneSource;
import com.hiro.ulike.hdr.color.SdrRendition;
import com.hiro.ulike.hdr.face.StillFaceAnalysis;
import hiro.ulike.beauty.BeautyImageEngine;
import hiro.ulike.beauty.PinnedAssets;
import hiro.ulike.model.PinnedModel;
import java.io.*;
import java.nio.*;
import java.security.MessageDigest;

/** Synthetic host integration, NOT a camera capture, face detector or processed HDR image. */
public final class StillNeuralIntegration {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static String hash(HdrFrame frame) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for (HdrFrame.Component component:HdrFrame.Component.values()) {
            ShortBuffer samples=frame.samples(component);
            while(samples.hasRemaining()) { int sample=samples.get();digest.update((byte)(sample>>8));digest.update((byte)sample); }
        }
        StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format("%02x",b&255));return out.toString();
    }
    private static HdrFrame fixture(int w,int h) {
        short[][] data={new short[w*h],new short[w*h/4],new short[w*h/4]};
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)data[0][y*w+x]=(short)((x*3+y*7)%1024);
        for(int y=0;y<h/2;y++)for(int x=0;x<w/2;x++){
            data[1][y*w/2+x]=(short)(440+(x*5+y*3)%144);
            data[2][y*w/2+x]=(short)(464+(x*3+y*7)%96);
        }
        Object id=new Object();CaptureMatch.Context context=new CaptureMatch.Context(id,id,id,id,id,9,"0",null);
        CaptureMatch.Result result=new CaptureMatch.Result(context,id,100000,123,null,"5",10000000L,206);
        return new HdrFrame(w,h,HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED,context,result,data,(long)w*h*3);
    }
    private static final class Sink implements BeautyImageEngine.TransactionalSink {
        private final SdrRendition source;private final boolean mutateIdentity;
        int next,changed,unchanged,aborts;boolean committed;Object id;
        Sink(SdrRendition source,boolean mutateIdentity){this.source=source;this.mutateIdentity=mutateIdentity;}
        public void begin(int w,int h,Object id){check(w==source.width()&&h==source.height()&&id==source.frameIdentity(),"exact still and size");this.id=id;}
        public void writeRows(int first,int count,float[] pixels){
            check(first==next&&count>0&&first+count<=source.height(),"ordered complete rows");float[] expected=new float[3];
            for(int y=0;y<count;y++)for(int x=0;x<source.width();x++){
                source.readPixel(x,first+y,expected);
                for(int c=0;c<3;c++){
                    float value=pixels[(y*source.width()+x)*3+c];check(Float.isFinite(value)&&value>=0&&value<=1,"finite unit SDR");
                    if(Float.floatToRawIntBits(value)==Float.floatToRawIntBits(expected[c]))unchanged++;else changed++;
                }
            }
            next+=count;
        }
        public void commit(){check(next==source.height(),"no partial image commit");committed=true;}
        public void abort(){aborts++;committed=false;}
    }
    public static void main(String[] args)throws Exception {
        int w=512,h=384;HdrFrame original=fixture(w,h);String before=hash(original);
        P010SceneSource hdr=new P010SceneSource(original,P010SceneSource.ChromaLocation.COSITED,
                P010SceneSource.ChromaLocation.COSITED,"synthetic-test-fixture-top-left");
        SdrRendition source=new SdrRendition(hdr,3.0);
        check(source.frameIdentity()==original&&source.width()==w&&source.height()==h,"same-size owned frame");
        float[] pixel=new float[3];int fractional=0;
        for(int x=0;x<w;x++){source.readPixel(x,30,pixel);if(Math.abs(pixel[1]*255-Math.round(pixel[1]*255))>.01)fractional++;}
        check(fractional>100,"model input was not quantized to RGB8");
        StillFaceAnalysis.Image proxy=StillFaceAnalysis.prepare(original,90,true,256,source);
        check(proxy.width==192&&proxy.height==256&&proxy.sensorTimestampNs==original.timestampNs,"exact-source analysis proxy");
        check(proxy.rgb8().isReadOnly(),"proxy immutable");
        try{source.rgb8(fixture(w,h),0,0);throw new AssertionError("foreign still accepted");}catch(IllegalArgumentException expected){checks++;}
        // Explicit synthetic geometry. This does not pretend to detect faces in synthetic pixels.
        double[] transform={.85,.06,-77,-.06,.85,-18,0,0,1};
        for(int index=0;index<2;index++){
            PinnedModel.Style style=index==0?PinnedModel.Style.NATURAL_BLUSH:PinnedModel.Style.PURITY2;
            PinnedModel.CompiledModel model;
            try(InputStream m=new FileInputStream(args[index]);InputStream b=new FileInputStream(args[2]);InputStream e=new FileInputStream(args[3])){model=PinnedModel.compile(style,m,b,e);}
            PinnedAssets.NeuralMask mask=PinnedAssets.loadMask(new File(args[4+index]),style,false);
            try(BeautyImageEngine engine=BeautyImageEngine.openHostForVerification(model)){
                Sink sink=new Sink(source,false);
                BeautyImageEngine.Result result=engine.processWithTransform(source,transform,mask,.7,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),sink);
                check(sink.committed&&sink.aborts==0&&sink.changed>100&&sink.unchanged>100,"real model effect and untouched surroundings");
                check(!result.hdrPreserved&&!result.completeStyle&&result.frameIdentity==original,"result does not claim SDR beauty is HDR");
                check(hash(original).equals(before),"HDR sample codes unchanged by SDR model processing");
                // Simulate a caller changing frame identity after transaction begins.
                final Object other=new Object();
                final Sink failed=new Sink(source,true);
                BeautyImageEngine.SourceRgbFloat unstable=new BeautyImageEngine.SourceRgbFloat(){
                    public int width(){return source.width();}public int height(){return source.height();}
                    public Object frameIdentity(){return failed.id==null?original:other;}
                    public void readPixel(int x,int y,float[] dst){source.readPixel(x,y,dst);}
                };
                try{engine.processWithTransform(unstable,transform,mask,.7,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),failed);throw new AssertionError("changed frame accepted");}
                catch(IllegalArgumentException expected){check(failed.aborts==1&&!failed.committed,"changed frame rollback");}
                engine.close();
                try{engine.processWithTransform(source,transform,mask,.7,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),new Sink(source,false));throw new AssertionError("closed engine accepted");}
                catch(IllegalStateException expected){checks++;}
                System.out.println("PASS "+style+" P010->scene HDR->explicit SDR->real neural pass->same-size float rows; changed="+sink.changed+" unchanged="+sink.unchanged);
            }
        }
        System.out.println("PASS "+checks+" host integration assertions; HDR source sha256="+before);
    }
}
