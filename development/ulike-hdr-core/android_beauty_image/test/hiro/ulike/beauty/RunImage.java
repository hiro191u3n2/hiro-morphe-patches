package hiro.ulike.beauty;

import hiro.ulike.model.PinnedModel;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public final class RunImage {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static final class Source implements BeautyImageEngine.SourceRgbFloat {
        final int w,h;final float[] pixels;final Object id=new Object();int reads;boolean lateBad;
        Source(int w,int h,float[] p){this.w=w;this.h=h;pixels=p;}
        public int width(){return w;}public int height(){return h;}public Object frameIdentity(){return id;}
        public void readPixel(int x,int y,float[] dst){reads++;System.arraycopy(pixels,(y*w+x)*3,dst,0,3);if(lateBad && y==h-1)dst[0]=Float.NaN;}
    }
    static final class Sink implements BeautyImageEngine.TransactionalSink {
        float[] pending,visible;int width,next,begins,aborts;String fail="";
        public void begin(int w,int h,Object id){begins++;width=w;pending=new float[w*h*3];next=0;if(fail.equals("begin"))throw new IllegalStateException("begin");}
        public void writeRows(int y,int n,float[] rgb){check(y==next,"row order");System.arraycopy(rgb,0,pending,y*width*3,n*width*3);next+=n;if(fail.equals("write"))throw new IllegalStateException("write");}
        public void commit(){if(fail.equals("commit"))throw new IllegalStateException("commit");visible=pending;pending=null;}
        public void abort(){aborts++;pending=null;}
    }
    static float[] floats(Path p)throws Exception{byte[] b=Files.readAllBytes(p);FloatBuffer f=ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();float[] a=new float[f.remaining()];f.get(a);return a;}
    static double[] doubles(Path p)throws Exception{DoubleBuffer f=ByteBuffer.wrap(Files.readAllBytes(p)).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();double[] a=new double[f.remaining()];f.get(a);return a;}
    static void save(Path p,float[] data)throws Exception{ByteBuffer b=ByteBuffer.allocate(data.length*4).order(ByteOrder.LITTLE_ENDIAN);b.asFloatBuffer().put(data);Files.write(p,b.array());}
    static void saveD(Path p,double[] data)throws Exception{ByteBuffer b=ByteBuffer.allocate(data.length*8).order(ByteOrder.LITTLE_ENDIAN);b.asDoubleBuffer().put(data);Files.write(p,b.array());}
    public static void main(String[] args)throws Exception{
        PinnedModel.Style style=PinnedModel.Style.valueOf(args[0]);Path root=Paths.get(args[5]),output=Paths.get(args[6]);Files.createDirectories(output);PinnedModel.CompiledModel model;
        try(InputStream m=new FileInputStream(args[1]);InputStream b=new FileInputStream(args[2]);InputStream e=new FileInputStream(args[3])){model=PinnedModel.compile(style,m,b,e);}
        PinnedAssets.FaceTemplate template=PinnedAssets.loadTemplate(new File(args[3]));PinnedAssets.NeuralMask mask=PinnedAssets.loadMask(new File(args[4]),style,false),flip=PinnedAssets.loadMask(new File(args[4]),style,true);
        double[] red=new double[320*320],flipped=new double[red.length];for(int y=0;y<320;y++)for(int x=0;x<320;x++){red[y*320+x]=mask.at(x,y);flipped[y*320+x]=flip.at(x,y);}saveD(output.resolve("mask.bin"),red);saveD(output.resolve("mask-flipped.bin"),flipped);saveD(output.resolve("template.bin"),template.copy());
        float[] photo=floats(root.resolve("source.bin"));int width=512,height=384;Source source=new Source(width,height,photo);double[] landmarks=doubles(root.resolve("landmarks.bin"));saveD(output.resolve("matrix.bin"),BeautyImageEngine.cropMatrix(style,landmarks,template.copy()));
        int cases=0;PreparedNeuralLayer retained=null;double[] retainedSample=new double[4];try(BeautyImageEngine engine=BeautyImageEngine.openHostForVerification(model)){
            for(String fixture:Arrays.asList("affine","rotated","reflected","border")){
                double[] matrix=doubles(root.resolve(fixture+".bin"));Sink sink=new Sink();BeautyImageEngine.Result r=engine.processWithTransform(source,matrix,mask,.63,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),sink);
                check(r.width==width && r.height==height && r.frameIdentity==source.id && !r.hdrPreserved && !r.completeStyle && !r.nativePixelParity,"result claims");check(sink.visible!=null && sink.pending==null && sink.aborts==0,"committed transaction");save(output.resolve(fixture+".bin"),sink.visible);cases++;
            }
            Sink pointSink=new Sink();engine.process(source,landmarks,template,mask,.63,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),pointSink);save(output.resolve("landmarks.bin"),pointSink.visible);cases++;
            Sink zero=new Sink();engine.processWithTransform(source,doubles(root.resolve("affine.bin")),mask,0,BeautyImageEngine.SDR_DOMAIN,new BeautyImageEngine.Budget(25000000,16384,1,8*1024*1024),zero);
            for(int i=0;i<photo.length;i++)check(Float.floatToRawIntBits(photo[i])==Float.floatToRawIntBits(zero.visible[i]),"zero identity");cases++;
            Sink tile=new Sink();engine.processWithTransform(source,doubles(root.resolve("affine.bin")),mask,.63,BeautyImageEngine.SDR_DOMAIN,new BeautyImageEngine.Budget(25000000,16384,1,8*1024*1024),tile);check(Arrays.equals(tile.visible,floats(output.resolve("affine.bin"))),"tile invariance");cases++;
            retained=engine.prepareLayer(source,doubles(root.resolve("affine.bin")),mask,.63,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard());
            check(retained.frameIdentity==source.id && retained.preparedFrom(source) && !retained.preparedFrom(new Source(width,height,photo)),"layer source identity");
            double[] sample=new double[4];float[] direct=floats(output.resolve("affine.bin"));
            for(int y=0;y<height;y++)for(int x=0;x<width;x++){retained.sampleAt(x,y,sample);for(int c=0;c<3;c++){int i=(y*width+x)*3+c;float expected=sample[3]==0?photo[i]:(float)((double)photo[i]*(1-sample[3])+sample[c]*sample[3]);check(Float.floatToRawIntBits(expected)==Float.floatToRawIntBits(direct[i]),"prepared layer reconstruction");}}
            retained.sampleAt(200,180,retainedSample);double[] exposed=retained.sourceToCrop();exposed[0]=Double.NaN;double[] repeat=new double[4];retained.sampleAt(200,180,repeat);check(Arrays.equals(retainedSample,repeat),"defensive layer matrix");cases++;
            try{retained.sampleAt(-1,0,sample);throw new AssertionError("negative layer coordinate accepted");}catch(IllegalArgumentException expected){}cases++;
            for(String failure:Arrays.asList("begin","write","commit")){Sink sink=new Sink();sink.fail=failure;boolean failed=false;try{engine.processWithTransform(source,doubles(root.resolve("affine.bin")),mask,.63,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),sink);}catch(IllegalStateException expected){failed=true;}check(failed && sink.visible==null && sink.pending==null && sink.aborts==1,"atomic "+failure);cases++;}
            Source invalid=new Source(width,height,photo);invalid.lateBad=true;Sink rejected=new Sink();try{engine.processWithTransform(invalid,new double[]{2,0,50,0,2,50,0,0,1},mask,.63,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),rejected);throw new AssertionError("bad source accepted");}catch(IllegalArgumentException expected){check(rejected.aborts==1 && rejected.visible==null,"late sample rollback");}cases++;
            Source oversize=new Source(16385,1,new float[0]);for(int mode=0;mode<4;mode++){Source s=mode==0?oversize:source;s.reads=0;Sink sink=new Sink();boolean failed=false;try{engine.processWithTransform(s,mode==2?new double[]{1,0,0,0,0,0,0,0,1}:doubles(root.resolve("affine.bin")),mask,.63,mode==3?"hlg":"encoded_sdr_full_range",mode==1?new BeautyImageEngine.Budget(25000000,16384,8,1):BeautyImageEngine.Budget.standard(),sink);}catch(IllegalArgumentException expected){failed=true;}check(failed && s.reads==0 && sink.begins==0,"preflight guard "+mode);cases++;}
        }
        double[] afterClose=new double[4];retained.sampleAt(200,180,afterClose);check(Arrays.equals(retainedSample,afterClose),"prepared layer survives engine close");cases++;
        System.out.println("PASS "+style+" "+cases+" cases; actual model, mask, template; zero-weight bits and transaction rollback");
    }
}
