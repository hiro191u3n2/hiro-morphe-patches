package com.hiro.ulike.hdr.gainmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Host-only x265/FFmpeg codec fixture. No host executable appears in production classes. */
public final class GainmapSaveTest {
    private static int checks;
    private static void check(boolean okay,String label) { checks++; if(!okay) throw new AssertionError(label); }
    private static void reject(Runnable work,String label) {
        try { work.run(); throw new AssertionError("accepted "+label); } catch(IllegalArgumentException expected) { checks++; }
    }
    private static GainmapMath.Image image(GainmapMath.Frame frame,double[] flat) {
        return new GainmapMath.Image(frame,(y,row)->System.arraycopy(flat,y*row.length,row,0,row.length));
    }
    private static void run(List<String> command) throws IOException {
        Process p=new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try { if(p.waitFor()!=0) throw new IOException("fixture codec failed"); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
    }
    static final class HostCodec implements GainmapSave.Codec {
        final Path work; final String name; final boolean alteredBase;
        HostCodec(Path work,String name,boolean alteredBase) { this.work=work; this.name=name; this.alteredBase=alteredBase; }
        private Path file(GainmapSave.Role role,String suffix) { return work.resolve(name+"_"+role.name()+suffix); }
        public byte[] encode(GainmapMath.Codes input,GainmapSave.Role role) throws IOException {
            Yuv42010 yuv=Yuv42010.fromRgb(input); GainmapMath.Frame f=input.frame;
            ByteBuffer raw=ByteBuffer.allocate(f.width*f.height*3).order(ByteOrder.LITTLE_ENDIAN);
            for(int c=0;c<3;c++) {
                ShortBuffer plane=yuv.plane(c);
                while(plane.hasRemaining()) {
                    int code=plane.get(); if(c==0 && role==GainmapSave.Role.SDR_BASE && alteredBase) code=Math.min(1023,code+32);
                    raw.putShort((short)code);
                }
            }
            Files.write(file(role,".yuv"),raw.array());
            run(Arrays.asList("ffmpeg","-hide_banner","-loglevel","error","-y","-f","rawvideo","-pix_fmt","yuv420p10le",
                    "-video_size",f.width+"x"+f.height,"-framerate","1","-i",file(role,".yuv").toString(),"-frames:v","1",
                    "-c:v","libx265","-profile:v","main10","-preset","medium","-x265-params",
                    "lossless=1:repeat-headers=1:info=0:pools=1:frame-threads=1:log-level=error",
                    "-color_primaries","bt2020","-color_trc",role==GainmapSave.Role.SDR_BASE?"bt709":"linear",
                    "-colorspace","bt2020nc","-color_range","pc","-f","hevc",file(role,".hevc").toString()));
            return Files.readAllBytes(file(role,".hevc"));
        }
        public GainmapSave.Decoded decode(byte[] encoded,GainmapMath.Frame frame,GainmapSave.Role role) throws IOException {
            Path source=file(role,".decode_source.hevc"),decoded=file(role,".decoded.yuv"); Files.write(source,encoded);
            run(Arrays.asList("ffmpeg","-hide_banner","-loglevel","error","-y","-threads","1","-i",source.toString(),
                    "-frames:v","1","-pix_fmt","yuv420p10le","-f","rawvideo",decoded.toString()));
            byte[] raw=Files.readAllBytes(decoded);
            if(raw.length!=frame.width*frame.height*3) throw new IOException("wrong decoded byte count");
            ByteBuffer b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN); ShortBuffer all=b.asShortBuffer();
            int n=frame.width*frame.height; short[] y=new short[n],cb=new short[n/4],cr=new short[n/4];
            all.get(y); all.get(cb); all.get(cr);
            Yuv42010 actual=new Yuv42010(frame,ShortBuffer.wrap(y),ShortBuffer.wrap(cb),ShortBuffer.wrap(cr));
            GainmapMath.Codes rgb=actual.rgb();
            ByteBuffer rgbBytes=ByteBuffer.allocate(n*6).order(ByteOrder.LITTLE_ENDIAN); int[] row=new int[frame.width*3];
            for(int yy=0;yy<frame.height;yy++) { rgb.read(yy,row); for(int v:row) rgbBytes.putShort((short)v); }
            Files.write(file(role,".decoded.rgb10"),rgbBytes.array());
            return new GainmapSave.Decoded() {
                public GainmapMath.Codes codes() { return rgb; }
                public void close() {}
            };
        }
    }
    private static void binaryDouble(Path path,double[] values) throws IOException {
        ByteBuffer bytes=ByteBuffer.allocate(values.length*8).order(ByteOrder.LITTLE_ENDIAN);
        for(double value:values) bytes.putDouble(value); Files.write(path,bytes.array());
    }
    public static void main(String[] argv) throws Exception {
        Path work=Paths.get(argv[0]); List<String> reports=new ArrayList<>();
        GainmapMath.Frame one=new GainmapMath.Frame(2,2,"capture","geometry","beauty-v1");
        double[] sdr=new double[12],hdr=new double[12]; Arrays.fill(sdr,0.2); Arrays.fill(hdr,0.8);
        GainmapMath.Metadata m=GainmapMath.analyze(image(one,sdr),image(one,hdr),4);
        check(Math.abs(m.low(0)-(StrictMath.log((0.8+1.0/64)/(0.2+1.0/64))/StrictMath.log(2)))<1e-14,"log gain");
        GainmapMath.encode10(image(one,sdr),image(one,hdr),m,(y,row)-> {
            for(int code:row) check(code==0,"constant map zero code");
        });
        for(int c=0;c<3;c++) check(Math.abs(new IsoMetadata(m).reconstruct(.2,0,c)-.8)<1e-7,"constant gain reconstruction");
        reject(()->new GainmapMath.Frame(32768,32768,"c","g","p"),"memory budget before allocation");
        reject(()->new GainmapMath.Image(one,(y,row)->row[0]=1).read(0,new double[6]),"incomplete floating row");
        reject(()->new GainmapMath.Codes(one,(y,row)->row[0]=1).read(0,new int[6]),"incomplete tenbit row");
        reject(()->GainmapMath.analyze(image(one,sdr),image(new GainmapMath.Frame(2,2,"c","g","p"),hdr),4),"mismatched frame");
        double[] negative=sdr.clone(); negative[0]=-.1;
        reject(()->GainmapMath.analyze(image(one,negative),image(one,hdr),4),"negative RGB");
        reject(()->GainmapMath.analyze(image(one,sdr),image(one,hdr),1),"no explicit headroom");
        double[] varied=sdr.clone(); varied[0]=.1; varied[1]=.3;
        GainmapMath.Metadata changed=GainmapMath.analyze(image(one,varied),image(one,hdr),4); varied[2]=.21;
        reject(()->GainmapMath.encode10(image(one,varied),image(one,hdr),changed,(y,row)->{}),"changed pair inside gain range");
        check(GainmapMath.quantize(0)==0 && GainmapMath.quantize(1)==1023,"tenbit endpoints");
        for(int code=0;code<1024;code++) check(GainmapMath.quantize(code/1023.0)==code,"1024 code preservation");
        for(String name:Arrays.asList("smooth","edited_color","changed_base","black")) {
            int w=64,h=48; GainmapMath.Frame f=new GainmapMath.Frame(w,h,name,"rotated-crop-1","all-edits-v1");
            double[] source=new double[w*h*3],target=new double[source.length];
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) for(int c=0;c<3;c++) {
                int i=(y*w+x)*3+c; double ramp=(x+0.4*y)/(w-1+0.4*(h-1));
                double v=.02+.65*ramp;
                if(name.equals("edited_color")) v*=new double[]{.8,1,.6}[c];
                if(name.equals("black")) v=0;
                source[i]=v;
                target[i]=name.equals("black")?0:v*(1.3+3*ramp+0.15*c)+0.02*StrictMath.sin(x*.05)*StrictMath.sin(y*.08);
            }
            HostCodec codec=new HostCodec(work,name,name.equals("changed_base"));
            GainmapSave.Result result=GainmapSave.prepare(image(f,source),image(f,target),8,
                    new GainmapSave.QualityLimits(.2,.4,.08),codec,work);
            result.file.saveAtomic(work.resolve(name+".heic")); binaryDouble(work.resolve(name+".sdr"),source); binaryDouble(work.resolve(name+".hdr"),target);
            reports.add("\""+name+"\":{\"max_sdr_error\":"+result.metrics.maxSdrError+",\"max_hdr_error\":"+result.metrics.maxHdrError+
                    ",\"hdr_rmse\":"+result.metrics.hdrRmse+",\"samples\":"+result.metrics.samples+"}");
            check(result.file.fileBytes==Files.size(work.resolve(name+".heic")),"staged size");
            check(result.metrics.maxHdrError<.4,"actual decoded quality gate");
            if(name.equals("smooth")) {
                GainmapSave.Codec mutating=new GainmapSave.Codec() {
                    public byte[] encode(GainmapMath.Codes codes,GainmapSave.Role role) throws IOException {
                        byte[] actual=codec.encode(codes,role);
                        if(role==GainmapSave.Role.SDR_BASE) source[0]+=.0001;
                        return actual;
                    }
                    public GainmapSave.Decoded decode(byte[] encoded,GainmapMath.Frame frame,GainmapSave.Role role) throws IOException {
                        return codec.decode(encoded,frame,role);
                    }
                };
                try {
                    GainmapSave.prepare(image(f,source),image(f,target),8,new GainmapSave.QualityLimits(.2,.4,.08),mutating,work);
                    throw new AssertionError("accepted original SDR mutation within quality limit");
                } catch(IllegalArgumentException expected) {
                    check(expected.getMessage().contains("original processed SDR/HDR pair changed"),"initial source pair bound across codec calls");
                } finally { source[0]-=.0001; }
                Path directoryTarget=work.resolve("protected-destination"); Files.createDirectory(directoryTarget);
                Path sentinel=directoryTarget.resolve("sentinel"); byte[] marker={1,2,3,4}; Files.write(sentinel,marker);
                try { result.file.saveAtomic(directoryTarget); throw new AssertionError("replaced directory destination"); }
                catch(IOException expected) { check(Arrays.equals(marker,Files.readAllBytes(sentinel)),"atomic destination failure preserves existing data"); }
                java.io.OutputStream failing=new java.io.OutputStream() {
                    int count;
                    public void write(int value) throws IOException { if(++count>100) throw new IOException("injected I/O failure"); }
                };
                try { result.file.writeTo(failing); throw new AssertionError("ignored I/O failure"); }
                catch(IOException expected) { check(expected.getMessage().equals("injected I/O failure"),"partial stream failure propagated"); }
                byte[] base=Files.readAllBytes(work.resolve(name+"_SDR_BASE.hevc"));
                reject(()->GainmapHevcProof.inspect(base,w,h,GainmapSave.Role.NUMERICAL_GAINMAP),"SDR cannot masquerade as linear map");
                byte[] map=Files.readAllBytes(work.resolve(name+"_NUMERICAL_GAINMAP.hevc"));
                reject(()->GainmapHevcProof.inspect(map,w,h,GainmapSave.Role.SDR_BASE),"linear map cannot masquerade as SDR base");
                try {
                    GainmapSave.prepare(image(f,source),image(f,target),8,new GainmapSave.QualityLimits(1e-12,1e-12,1e-12),codec,work);
                    throw new AssertionError("accepted unrealistically strict quality");
                } catch(IOException expected) { check(expected.getMessage().contains("quality limits"),"explicit quality failure"); }
            }
        }
        try(java.util.stream.Stream<Path> paths=Files.list(work)) {
            check(!paths.anyMatch(p->p.getFileName().toString().startsWith(".ulike-gainmap-")),"staged maps cleaned after success/failure");
        }
        System.out.println("{\"checks\":"+checks+",\"fixtures\":{"+String.join(",",reports)+"},\"android_device_executed\":false}");
    }
}
