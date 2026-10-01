package com.hiro.ulike.hdr.gainmap;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Adversarial integration review. Uses real host HEVC solely as a test adapter. */
public final class GainmapReview {
    static int checks;
    interface Work { void run() throws Exception; }
    static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    static void reject(Work call,String message)throws Exception{
        checks++;try{call.run();}catch(IllegalArgumentException|IOException|CancellationException e){
            if(message!=null && (e.getMessage()==null||!e.getMessage().contains(message)))throw new AssertionError(e);
            return;
        }throw new AssertionError("Expected failure: "+message);
    }
    static GainmapMath.Image image(GainmapMath.Frame frame,double[] source){
        return new GainmapMath.Image(frame,(y,row)->System.arraycopy(source,y*row.length,row,0,row.length));
    }
    static double[] filled(int count,double value){double[] out=new double[count];Arrays.fill(out,value);return out;}
    static void mathTests(Path output)throws Exception{
        GainmapMath.Frame f=new GainmapMath.Frame(32,16,"cap1","geometry1","processing1");
        double[] s=new double[f.width*f.height*3],h=new double[s.length];Random random=new Random(951308L);
        for(int i=0;i<s.length;i++){s[i]=random.nextDouble();h[i]=random.nextDouble()*8;}
        s[0]=h[0]=0;s[1]=0;h[1]=7;s[2]=1;h[2]=0;
        GainmapMath.Metadata m=GainmapMath.analyze(image(f,s),image(f,h),8);
        IsoMetadata iso=new IsoMetadata(m);Files.write(output.resolve("independent-random.iso"),iso.copyPayload());
        double[] low={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},high={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(int i=0;i<s.length;i++){double ratio=Math.log((h[i]+1.0/64)/(s[i]+1.0/64))/Math.log(2);low[i%3]=Math.min(low[i%3],ratio);high[i%3]=Math.max(high[i%3],ratio);}
        for(int c=0;c<3;c++){check(Math.abs(low[c]-m.low(c))<2e-14,"independent minimum log ratio");check(Math.abs(high[c]-m.high(c))<2e-14,"independent maximum log ratio");}
        GainmapMath.encode10(image(f,s),image(f,h),m,(y,codes)->{
            for(int x=0;x<codes.length;x++){
                int i=y*codes.length+x,c=x%3;double restored=iso.reconstruct(s[i],codes[x],c);
                // Metadata numerator rounding contributes <=2^-25 to each log endpoint.
                double logError=(high[c]-low[c])/(2*1023)+Math.scalb(1.0,-25)+2e-14;
                double bound=(h[i]+1.0/64)*Math.expm1(logError*Math.log(2));
                check(Math.abs(restored-h[i])<=bound+2e-13,"10bit+serialized-metadata independent error bound");
            }
        });
        for(GainmapMath.Frame mismatch:new GainmapMath.Frame[]{
                new GainmapMath.Frame(32,16,"cap2","geometry1","processing1"),
                new GainmapMath.Frame(32,16,"cap1","geometry2","processing1"),
                new GainmapMath.Frame(32,16,"cap1","geometry1","processing2"),
                new GainmapMath.Frame(16,32,"cap1","geometry1","processing1")})
            reject(()->GainmapMath.analyze(image(f,s),image(mismatch,h),8),"mismatch");
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-.01}){
            double[] invalid=s.clone();invalid[0]=bad;reject(()->GainmapMath.analyze(image(f,invalid),image(f,h),8),"finite and nonnegative");
        }
        double[] tooS=s.clone();tooS[0]=Math.nextUp(1.0);reject(()->GainmapMath.analyze(image(f,tooS),image(f,h),8),"declared range");
        double[] tooH=h.clone();tooH[0]=Math.nextUp(8.0);reject(()->GainmapMath.analyze(image(f,s),image(f,tooH),8),"declared range");
        for(double bad:new double[]{0,1,-1,Double.NaN,10000.0/203+1e-8})reject(()->GainmapMath.analyze(image(f,s),image(f,h),bad),"headroom");
        byte[] before=m.pairDigest();before[0]^=127;check(!Arrays.equals(before,m.pairDigest()),"owned digest");
        byte[] metadata=iso.copyPayload();metadata[0]=99;check(iso.copyPayload()[0]==0,"owned ISO payload");
        for(double[] changing:new double[][]{s,h}){
            double old=changing[50];changing[50]=Math.nextUp(old);
            reject(()->GainmapMath.encode10(image(f,s),image(f,h),m,(y,row)->{}),"changed");changing[50]=old;
        }
        Thread.currentThread().interrupt();try{reject(()->GainmapMath.analyze(image(f,s),image(f,h),8),"interrupted");}finally{Thread.interrupted();}
    }
    public static void main(String[] args)throws Exception{
        Path work=Paths.get(args[0]);mathTests(work);
        int w=32,h=32;GainmapMath.Frame f=new GainmapMath.Frame(w,h,"review-capture","review-geometry","review-processing");
        double[] s=new double[w*h*3],hdr=new double[s.length];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)for(int c=0;c<3;c++){
            int i=(y*w+x)*3+c;double ramp=(x+2*y)/93.0;s[i]=.04+.55*ramp;hdr[i]=s[i]*(1.1+4*ramp+.1*c);
        }
        GainmapSave.QualityLimits permissive=new GainmapSave.QualityLimits(.5,1,.5);
        GainmapSaveTest.HostCodec host=new GainmapSaveTest.HostCodec(work,"independent",false);
        GainmapSave.Result result=GainmapSave.prepare(image(f,s),image(f,hdr),8,permissive,host,work);
        Path destination=work.resolve("independent.heic");Files.write(destination,new byte[]{9,8,7});
        result.file.saveAtomic(destination);check(Files.size(destination)==result.file.fileBytes,"atomic replace exact size");
        Files.write(work.resolve("independent.iso"),result.metadata.copyPayload());
        byte[] saved=Files.readAllBytes(destination);
        reject(()->GainmapSave.prepare(image(f,s),image(f,hdr),8,new GainmapSave.QualityLimits(1e-15,1e-15,1e-15),host,work),"quality limits");
        check(Arrays.equals(saved,Files.readAllBytes(destination)),"quality failure leaves published file unchanged");
        final boolean[] changed={false};
        GainmapSave.Codec mutating=new GainmapSave.Codec(){
            public byte[] encode(GainmapMath.Codes input,GainmapSave.Role role)throws IOException{
                byte[] actual=host.encode(input,role);if(role==GainmapSave.Role.SDR_BASE){s[31]=Math.nextUp(s[31]);changed[0]=true;}return actual;
            }
            public GainmapSave.Decoded decode(byte[] bytes,GainmapMath.Frame frame,GainmapSave.Role role)throws IOException{return host.decode(bytes,frame,role);}
        };
        double original=s[31];reject(()->GainmapSave.prepare(image(f,s),image(f,hdr),8,permissive,mutating,work),"original processed SDR/HDR pair changed");s[31]=original;
        check(changed[0],"adversarial original SDR mutation executed");
        // A real, validly tagged Main10 map carrying deliberately wrong numerical gain.
        GainmapSave.Codec wrongMap=new GainmapSave.Codec(){
            public byte[] encode(GainmapMath.Codes input,GainmapSave.Role role)throws IOException{
                if(role==GainmapSave.Role.NUMERICAL_GAINMAP)input=new GainmapMath.Codes(input.frame,(y,row)->Arrays.fill(row,1023));
                return host.encode(input,role);
            }
            public GainmapSave.Decoded decode(byte[] bytes,GainmapMath.Frame frame,GainmapSave.Role role)throws IOException{return host.decode(bytes,frame,role);}
        };
        reject(()->GainmapSave.prepare(image(f,s),image(f,hdr),8,new GainmapSave.QualityLimits(.5,.1,.05),wrongMap,work),"quality limits");
        try(java.util.stream.Stream<Path> paths=Files.list(work)){check(!paths.anyMatch(p->p.getFileName().toString().startsWith(".ulike-gainmap-")||p.getFileName().toString().startsWith(".ulike-tmap-")),"scratch cleanup after success and injected failures");}
        System.out.println("{\"independent_checks\":"+checks+",\"original_sdr_mutation_rejected\":true,\"actually_wrong_encoded_map_rejected\":true}");
    }
}
