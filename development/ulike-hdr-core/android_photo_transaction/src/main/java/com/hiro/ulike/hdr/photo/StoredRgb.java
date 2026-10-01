package com.hiro.ulike.hdr.photo;

import com.hiro.ulike.hdr.gainmap.GainmapMath;
import hiro.ulike.beauty.BeautyImageEngine;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Private file-backed RGB staging. Sealing creates an immutable reader, never a public photo. */
public final class StoredRgb {
    private StoredRgb() {}
    public enum Precision { FP32(4),FP64(8); final int bytes;Precision(int bytes){this.bytes=bytes;} }
    public enum Domain { ENCODED_SRGB, DISPLAY_LINEAR_BT2020_SDR, DISPLAY_LINEAR_BT2020_HDR }
    private static final int HEADER=4096;
    static final int BLOCK_PIXELS=64;
    static long storageBytes(PhotoIdentity identity,Precision precision){
        long checksumBytes=(long)identity.frame.height*(1+(identity.frame.width+BLOCK_PIXELS-1)/BLOCK_PIXELS)*32;
        return HEADER+(long)identity.frame.width*identity.frame.height*3*precision.bytes+checksumBytes;
    }
    private static void cancel() throws IOException { if(Thread.currentThread().isInterrupted()) throw new IOException("photo staging interrupted"); }
    private static void sample(double v,Domain domain,double headroom) {
        double max=domain==Domain.DISPLAY_LINEAR_BT2020_HDR?headroom:1;
        if(!Double.isFinite(v) || v<0 || v>max) throw new IllegalArgumentException("RGB value outside explicit domain");
    }
    private static byte[] header(PhotoIdentity id,Precision precision,Domain domain,double headroom) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeLong(0x554c4b5252474231L);out.writeInt(2);out.writeInt(id.frame.width);out.writeInt(id.frame.height);
        out.writeInt(precision.bytes);out.writeUTF(domain.name());out.writeDouble(headroom);out.writeUTF(id.identitySha256);
        out.writeUTF(id.frame.captureId);out.writeUTF(id.frame.geometryId);out.writeUTF(id.frame.processingId);out.writeUTF(id.settingsSha256);
        out.flush();if(bytes.size()>HEADER) throw new IOException("RGB header overflow");return Arrays.copyOf(bytes.toByteArray(),HEADER);
    }
    public static final class Writer implements AutoCloseable {
        public final PhotoIdentity identity;
        public final Precision precision;
        public final Domain domain;
        public final double headroom;
        final Path path;
        private final RandomAccessFile file;
        private final byte[] expectedHeader;
        private byte[][] hashes,blockHashes;
        private int nextRow;
        private boolean closed,sealed,begun;
        private Reader reader;
        Writer(Path path,PhotoIdentity identity,Precision precision,Domain domain,double headroom,long maxBytes) throws IOException {
            if(identity==null || precision==null || domain==null || !Double.isFinite(headroom) || headroom<1 || headroom>10000.0/203.0
                    || (domain==Domain.DISPLAY_LINEAR_BT2020_HDR?headroom<=1:headroom!=1)) throw new IllegalArgumentException("explicit RGB storage contract");
            long size=storageBytes(identity,precision);
            if(maxBytes<1 || size>maxBytes) throw new IOException("RGB stage exceeds explicit disk budget");
            this.path=path;this.identity=identity;this.precision=precision;this.domain=domain;this.headroom=headroom;
            expectedHeader=header(identity,precision,domain,headroom);hashes=new byte[identity.frame.height][];blockHashes=new byte[identity.frame.height][];
            Files.createFile(path);
            RandomAccessFile opened=null;
            try { opened=new RandomAccessFile(path.toFile(),"rw");opened.write(expectedHeader); }
            catch(IOException failure){if(opened!=null)try{opened.close();}catch(IOException e){failure.addSuppressed(e);}Files.deleteIfExists(path);throw failure;}
            file=opened;
        }
        public synchronized void writeRows(int firstRow,int count,double[] rgb) throws IOException {
            requireWrite(firstRow,count,rgb==null?-1:rgb.length);int width=identity.frame.width*3;
            for(int row=0;row<count;row++) {
                cancel();ByteBuffer bytes=ByteBuffer.allocate(width*precision.bytes).order(ByteOrder.LITTLE_ENDIAN);
                for(int i=0;i<width;i++) {
                    double value=rgb[row*width+i];sample(value,domain,headroom);
                    if(precision==Precision.FP32)bytes.putFloat((float)value);else bytes.putDouble(value);
                }
                byte[] payload=bytes.array();file.write(payload);hashes[nextRow]=PhotoIdentity.digest().digest(payload);
                int blocks=(identity.frame.width+BLOCK_PIXELS-1)/BLOCK_PIXELS,blockBytes=BLOCK_PIXELS*3*precision.bytes;
                blockHashes[nextRow]=new byte[blocks*32];
                for(int block=0;block<blocks;block++){MessageDigest digest=PhotoIdentity.digest();digest.update(payload,block*blockBytes,Math.min(blockBytes,payload.length-block*blockBytes));System.arraycopy(digest.digest(),0,blockHashes[nextRow],block*32,32);}
                nextRow++;
            }
        }
        public synchronized void writeRows(int firstRow,int count,float[] rgb) throws IOException {
            requireWrite(firstRow,count,rgb==null?-1:rgb.length);int width=identity.frame.width*3;
            // One row, never promote the full image/tile to FP64.
            double[] row=new double[width];for(int y=0;y<count;y++){for(int i=0;i<width;i++)row[i]=rgb[y*width+i];writeRows(firstRow+y,1,row);}
        }
        private void requireWrite(int firstRow,int count,int length) {
            if(closed || sealed || firstRow!=nextRow || count<1 || (long)firstRow+count>identity.frame.height
                    || (long)count*identity.frame.width*3>length) throw new IllegalArgumentException("RGB write order/shape/state");
        }
        public synchronized Reader seal() throws IOException {
            if(closed || sealed || nextRow!=identity.frame.height) throw new IOException("incomplete RGB stage");
            cancel();for(byte[] hash:hashes)file.write(hash);for(byte[] hash:blockHashes)file.write(hash);file.getFD().sync();file.close();closed=true;
            reader=new Reader(path,identity,precision,domain,headroom,expectedHeader,hashes,blockHashes);sealed=true;return reader;
        }
        public synchronized Reader reader() {if(!sealed || reader==null)throw new IllegalStateException("RGB stage not sealed");return reader;}
        public BeautyImageEngine.TransactionalSink asBeautySink() {
            if(domain!=Domain.ENCODED_SRGB || precision!=Precision.FP32)throw new IllegalArgumentException("beauty sink is FP32 encoded SDR");
            return new BeautyImageEngine.TransactionalSink(){
                public void begin(int width,int height,Object frameIdentity)throws IOException {
                    synchronized(Writer.this){if(begun || width!=identity.frame.width || height!=identity.frame.height || frameIdentity!=identity.exactSource)
                        throw new IOException("beauty sink frame/settings/geometry mismatch");begun=true;}
                }
                public void writeRows(int firstRow,int rows,float[] rgb)throws IOException {
                    synchronized(Writer.this){if(!begun)throw new IOException("beauty sink has not begun");Writer.this.writeRows(firstRow,rows,rgb);}
                }
                public void commit()throws IOException {synchronized(Writer.this){if(!begun)throw new IOException("beauty sink has not begun");seal();}}
                public void abort()throws IOException {Writer.this.abort();}
            };
        }
        public synchronized void abort()throws IOException {try{close();}finally{Files.deleteIfExists(path);}}
        @Override public synchronized void close()throws IOException {
            if(reader!=null)reader.close();if(!closed){closed=true;file.close();}hashes=null;blockHashes=null;
        }
    }
    public static final class Reader implements AutoCloseable,BeautyImageEngine.SourceRgbFloat {
        public final PhotoIdentity identity;public final Precision precision;public final Domain domain;public final double headroom;
        private final RandomAccessFile file;private byte[][] rowHashes,blockHashes;private final int rowBytes;
        private boolean closed;
        // Four rows is explicit bounded random-access working storage, useful for 256px affine crops.
        private final LinkedHashMap<Integer,double[]> cache=new LinkedHashMap<Integer,double[]>(4,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<Integer,double[]> e){return size()>4;}
        };
        Reader(Path path,PhotoIdentity identity,Precision precision,Domain domain,double headroom,byte[] header,byte[][] hashes,byte[][] blockHashes)throws IOException {
            this.identity=identity;this.precision=precision;this.domain=domain;this.headroom=headroom;
            rowBytes=identity.frame.width*3*precision.bytes;rowHashes=hashes;this.blockHashes=blockHashes;
            file=new RandomAccessFile(path.toFile(),"r");
            try {
                long expected=storageBytes(identity,precision);
                if(file.length()!=expected)throw new IOException("RGB snapshot extent mismatch");
                byte[] actualHeader=new byte[HEADER];file.readFully(actualHeader);if(!Arrays.equals(header,actualHeader))throw new IOException("RGB snapshot identity mismatch");
                file.seek(HEADER+(long)rowBytes*identity.frame.height);
                byte[] hash=new byte[32];for(byte[] expectedHash:rowHashes){file.readFully(hash);if(!MessageDigest.isEqual(hash,expectedHash))throw new IOException("RGB row index changed");}
                for(byte[] expectedBlocks:blockHashes){byte[] actualBlocks=new byte[expectedBlocks.length];file.readFully(actualBlocks);if(!MessageDigest.isEqual(actualBlocks,expectedBlocks))throw new IOException("RGB block index changed");}
            }catch(IOException e){try{file.close();}catch(IOException close){e.addSuppressed(close);}throw e;}
        }
        public int width(){return identity.frame.width;}public int height(){return identity.frame.height;}
        public Object frameIdentity(){return identity.exactSource;}
        private double[] row(int y)throws IOException {
            if(closed || y<0 || y>=height())throw new IOException("RGB reader closed or row outside image");cancel();
            double[] cached=cache.get(y);if(cached!=null)return cached;
            byte[] payload=new byte[rowBytes];file.seek(HEADER+(long)y*rowBytes);file.readFully(payload);
            if(!MessageDigest.isEqual(PhotoIdentity.digest().digest(payload),rowHashes[y]))throw new IOException("RGB row integrity failure");
            ByteBuffer bytes=ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);double[] values=new double[width()*3];
            for(int i=0;i<values.length;i++){values[i]=precision==Precision.FP32?bytes.getFloat():bytes.getDouble();sample(values[i],domain,headroom);}
            cache.put(y,values);return values;
        }
        public synchronized void readRow(int y,double[] rgb)throws IOException {
            if(rgb==null || rgb.length!=width()*3)throw new IllegalArgumentException("RGB row destination length");System.arraycopy(row(y),0,rgb,0,rgb.length);
        }
        /** Bounded span read with fresh SHA checks on intersecting 64-pixel blocks. Rotation
         * accesses short spans instead of repeatedly reading a complete source row per pixel. */
        public synchronized void readSpan(int y,int firstPixel,int pixels,double[] destination,int offset)throws IOException{
            if(closed || y<0 || y>=height() || firstPixel<0 || pixels<1 || (long)firstPixel+pixels>width()
                    || destination==null || offset<0 || (long)offset+pixels*3L>destination.length)throw new IOException("RGB span bounds/state");
            cancel();int firstBlock=firstPixel/BLOCK_PIXELS,lastBlock=(firstPixel+pixels-1)/BLOCK_PIXELS;
            for(int block=firstBlock;block<=lastBlock;block++){
                int blockFirst=block*BLOCK_PIXELS,blockPixels=Math.min(BLOCK_PIXELS,width()-blockFirst);byte[] payload=new byte[blockPixels*3*precision.bytes];
                file.seek(HEADER+(long)y*rowBytes+(long)blockFirst*3*precision.bytes);file.readFully(payload);
                byte[] expected=Arrays.copyOfRange(blockHashes[y],block*32,block*32+32);
                if(!MessageDigest.isEqual(PhotoIdentity.digest().digest(payload),expected))throw new IOException("RGB block integrity failure");
                int from=Math.max(firstPixel,blockFirst),to=Math.min(firstPixel+pixels,blockFirst+blockPixels);
                ByteBuffer bytes=ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);bytes.position((from-blockFirst)*3*precision.bytes);
                for(int x=from;x<to;x++)for(int c=0;c<3;c++){double value=precision==Precision.FP32?bytes.getFloat():bytes.getDouble();sample(value,domain,headroom);destination[offset+(x-firstPixel)*3+c]=value;}
            }
        }
        @Override public synchronized void readPixel(int x,int y,float[] rgb)throws IOException {
            if(domain!=Domain.ENCODED_SRGB || x<0 || x>=width() || rgb==null || rgb.length<3)throw new IllegalArgumentException("encoded SDR pixel destination/domain");
            double[] values=row(y);for(int c=0;c<3;c++)rgb[c]=(float)values[x*3+c];
        }
        public GainmapMath.Image gainmapImage(){
            if(domain==Domain.ENCODED_SRGB)throw new IllegalArgumentException("encoded sRGB is not a display-linear gainmap source");
            return new GainmapMath.Image(identity.frame,(y,rgb)->{try{readRow(y,rgb);}catch(IOException e){throw new UncheckedIOException(e);}});
        }
        @Override public synchronized void close()throws IOException{if(!closed){closed=true;cache.clear();rowHashes=null;blockHashes=null;file.close();}}
    }
}
