package com.hiro.ulike.hdr.gainmap;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Objects;

/** Staged JPEG-free save. Takes an ALREADY PROCESSED, geometrically matched display-linear
 * HDR/SDR pair. It does not synthesize HDR from SDR or reuse a pre-beauty gainmap. */
public final class GainmapSave {
    private GainmapSave() {}
    public enum Role {
        SDR_BASE(1), NUMERICAL_GAINMAP(8);
        public final int transfer;
        Role(int transfer) { this.transfer=transfer; }
    }
    /** encode/decode must be synchronous and faithful, with no 8-bit fallback. The decoder is a
     * trusted implementation, not an assertion of decoded pixels supplied by the save caller. */
    public interface Codec {
        byte[] encode(GainmapMath.Codes input,Role role) throws IOException;
        Decoded decode(byte[] actualAnnexB,GainmapMath.Frame frame,Role role) throws IOException;
    }
    public interface Decoded extends AutoCloseable {
        GainmapMath.Codes codes();
        @Override void close() throws IOException;
    }
    public static final class QualityLimits {
        public final double maxSdrError,maxHdrError,hdrRmse;
        public QualityLimits(double maxSdrError,double maxHdrError,double hdrRmse) {
            if(!positive(maxSdrError) || maxSdrError>1 || !positive(maxHdrError) || !positive(hdrRmse))
                GainmapMath.fail("explicit positive SDR/HDR quality limits required");
            this.maxSdrError=maxSdrError; this.maxHdrError=maxHdrError; this.hdrRmse=hdrRmse;
        }
        private static boolean positive(double v) { return GainmapMath.finite(v) && v>0; }
    }
    public static final class Metrics {
        public final double maxSdrError,maxHdrError,hdrRmse;
        public final long samples;
        Metrics(double s,double h,double r,long n) { maxSdrError=s; maxHdrError=h; hdrRmse=r; samples=n; }
    }
    public static final class Result {
        public final GainmapContainer.Prepared file;
        public final IsoMetadata metadata;
        public final Metrics metrics;
        public final GainmapMath.Frame frame;
        Result(GainmapContainer.Prepared file,IsoMetadata metadata,Metrics metrics,GainmapMath.Frame frame) {
            this.file=file; this.metadata=metadata; this.metrics=metrics; this.frame=frame;
        }
    }
    public static Result prepare(GainmapMath.Image processedSdr,GainmapMath.Image processedHdr,
            double headroom,QualityLimits limits,Codec codec,Path scratchDirectory) throws IOException {
        Objects.requireNonNull(processedSdr); Objects.requireNonNull(processedHdr); Objects.requireNonNull(limits);
        Objects.requireNonNull(codec); Objects.requireNonNull(scratchDirectory); GainmapMath.headroom(headroom);
        GainmapMath.Frame f=processedSdr.frame;
        if(!f.equals(processedHdr.frame)) GainmapMath.fail("processed pair frame mismatch");
        if(f.width<2 || f.height<2 || (f.width&1)!=0 || (f.height&1)!=0)
            GainmapMath.fail("Main10 4:2:0 requires even dimensions; no silent crop/padding");
        if(!Files.isDirectory(scratchDirectory)) throw new IOException("scratch directory must exist");
        // Capture BOTH processed sources before a codec can run. Decoder/base analysis alone
        // cannot detect original-SDR mutation after its initial consumption by the encoder.
        byte[] originalPairBinding=processedDigest(processedSdr,processedHdr,headroom);
        // Codec reads the real processed SDR. Any loss/420 change is measured below.
        byte[] base=bounded(codec.encode(GainmapMath.baseCodes(processedSdr),Role.SDR_BASE));
        GainmapHevcProof.inspect(base,f.width,f.height,Role.SDR_BASE);
        byte[] baseBinding=sha(base);
        try(Decoded decodedBase=codec.decode(base.clone(),f,Role.SDR_BASE)) {
            GainmapMath.Codes baseCodes=Objects.requireNonNull(decodedBase.codes());
            if(!f.equals(baseCodes.frame)) GainmapMath.fail("decoder changed base frame");
            GainmapMath.Image actualBase=GainmapMath.linearBase(baseCodes);
            GainmapMath.Metadata mathematical=GainmapMath.analyze(actualBase,processedHdr,headroom);
            IsoMetadata serialized=new IsoMetadata(mathematical);
            try(CodeFile map=new CodeFile(scratchDirectory,f)) {
                GainmapMath.encode10(actualBase,processedHdr,mathematical,map::write);
                map.complete();
                byte[] encodedMap=bounded(codec.encode(map.codes(),Role.NUMERICAL_GAINMAP));
                GainmapHevcProof.inspect(encodedMap,f.width,f.height,Role.NUMERICAL_GAINMAP);
                try(Decoded decodedMap=codec.decode(encodedMap.clone(),f,Role.NUMERICAL_GAINMAP)) {
                    GainmapMath.Codes actualMap=Objects.requireNonNull(decodedMap.codes());
                    if(!f.equals(actualMap.frame)) GainmapMath.fail("decoder changed map frame");
                    Metrics metrics=metrics(processedSdr,processedHdr,actualBase,actualMap,serialized,mathematical,originalPairBinding);
                    if(metrics.maxSdrError>limits.maxSdrError || metrics.maxHdrError>limits.maxHdrError || metrics.hdrRmse>limits.hdrRmse)
                        throw new IOException("actual decoded SDR/HDR reconstruction exceeds explicit quality limits: "
                                +metrics.maxSdrError+", "+metrics.maxHdrError+", "+metrics.hdrRmse);
                    if(!MessageDigest.isEqual(baseBinding,sha(base))) throw new IOException("base bytes changed");
                    return new Result(GainmapContainer.prepare(base,encodedMap,f.width,f.height,serialized),serialized,metrics,f);
                }
            }
        }
    }
    private static Metrics metrics(GainmapMath.Image sdr,GainmapMath.Image hdr,GainmapMath.Image base,
            GainmapMath.Codes map,IsoMetadata serialized,GainmapMath.Metadata mathematical,byte[] originalPairBinding) throws IOException {
        int n=sdr.frame.width*3; double[] expectedSdr=new double[n],expectedHdr=new double[n],actual=new double[n];
        int[] codes=new int[n]; double maxSdr=0,maxHdr=0,sum=0,compensation=0;
        MessageDigest digest=GainmapMath.digest(),originalDigest=GainmapMath.digest();
        for(int y=0;y<sdr.frame.height;y++) {
            if(Thread.currentThread().isInterrupted()) throw new IOException("gainmap save interrupted");
            sdr.read(y,expectedSdr); hdr.read(y,expectedHdr); base.read(y,actual); map.read(y,codes);
            GainmapMath.update(digest,actual); GainmapMath.update(digest,expectedHdr);
            GainmapMath.update(originalDigest,expectedSdr); GainmapMath.update(originalDigest,expectedHdr);
            for(int x=0;x<n;x++) {
                if(expectedSdr[x]>1 || expectedHdr[x]>mathematical.headroom) GainmapMath.fail("processed range changed");
                maxSdr=Math.max(maxSdr,StrictMath.abs(actual[x]-expectedSdr[x]));
                double error=serialized.reconstruct(actual[x],codes[x],x%3)-expectedHdr[x];
                if(!GainmapMath.finite(error)) GainmapMath.fail("reconstruction overflow");
                maxHdr=Math.max(maxHdr,StrictMath.abs(error));
                double term=error*error-compensation,next=sum+term; compensation=(next-sum)-term; sum=next;
            }
        }
        if(!MessageDigest.isEqual(digest.digest(),mathematical.pairDigest())) GainmapMath.fail("decoded base or HDR changed before final verification");
        if(!MessageDigest.isEqual(originalDigest.digest(),originalPairBinding)) GainmapMath.fail("original processed SDR/HDR pair changed during save");
        long samples=(long)n*sdr.frame.height;
        return new Metrics(maxSdr,maxHdr,StrictMath.sqrt(sum/samples),samples);
    }
    private static byte[] processedDigest(GainmapMath.Image sdr,GainmapMath.Image hdr,double headroom) throws IOException {
        double[] s=new double[sdr.frame.width*3],h=new double[s.length]; MessageDigest digest=GainmapMath.digest();
        for(int y=0;y<sdr.frame.height;y++) {
            if(Thread.currentThread().isInterrupted()) throw new IOException("gainmap save interrupted");
            sdr.read(y,s); hdr.read(y,h);
            for(int x=0;x<s.length;x++) if(s[x]>1 || h[x]>headroom) GainmapMath.fail("processed pair outside declared display ranges");
            GainmapMath.update(digest,s); GainmapMath.update(digest,h);
        }
        return digest.digest();
    }
    private static byte[] bounded(byte[] bytes) throws IOException {
        if(bytes==null || bytes.length<6 || bytes.length>64*1024*1024) throw new IOException("encoded item byte bound");
        return bytes.clone();
    }
    private static byte[] sha(byte[] bytes) { return GainmapMath.digest().digest(bytes); }
    /** Private staged map uses at most width*6 bytes per I/O row and 6*pixelCount disk bytes. */
    private static final class CodeFile implements AutoCloseable {
        final Path path; final RandomAccessFile file; final GainmapMath.Frame frame; int nextRow=0; boolean complete=false;
        CodeFile(Path parent,GainmapMath.Frame frame) throws IOException {
            path=Files.createTempFile(parent,".ulike-gainmap-",".rgb10"); this.frame=frame;
            RandomAccessFile opened=null;
            try { opened=new RandomAccessFile(path.toFile(),"rw"); }
            catch(IOException e) { Files.deleteIfExists(path); throw e; }
            file=opened;
        }
        void write(int y,int[] row) {
            if(y!=nextRow++ || complete || row.length!=frame.width*3) GainmapMath.fail("map row order/shape");
            ByteBuffer bytes=ByteBuffer.allocate(row.length*2).order(ByteOrder.BIG_ENDIAN);
            for(int v:row) { if(v<0 || v>1023) GainmapMath.fail("map not ten-bit"); bytes.putShort((short)v); }
            try { file.write(bytes.array()); } catch(IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        void complete() throws IOException {
            if(nextRow!=frame.height || file.length()!=(long)frame.width*frame.height*6) throw new IOException("incomplete map staging");
            complete=true;
        }
        GainmapMath.Codes codes() {
            if(!complete) GainmapMath.fail("map not complete");
            return new GainmapMath.Codes(frame,(y,row)-> {
                byte[] bytes=new byte[row.length*2];
                try { synchronized(file) { file.seek((long)y*bytes.length); file.readFully(bytes); } }
                catch(IOException e) { throw new java.io.UncheckedIOException(e); }
                ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN); for(int i=0;i<row.length;i++) row[i]=b.getShort()&65535;
            });
        }
        @Override public void close() throws IOException { try { file.close(); } finally { Files.deleteIfExists(path); } }
    }
}
