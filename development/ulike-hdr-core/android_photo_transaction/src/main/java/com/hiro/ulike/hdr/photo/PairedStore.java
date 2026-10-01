package com.hiro.ulike.hdr.photo;

import com.hiro.ulike.hdr.gainmap.GainmapMath;
import com.hiro.ulike.hdr.beauty.HdrBeautyProcessor;
import java.io.IOException;

/** Both rows must be derived from the same completed HDR appearance processing and geometry. */
public final class PairedStore {
    private PairedStore(){}
    public static final class Pair {
        public final PhotoIdentity identity;public final double headroom;
        final StoredRgb.Reader sdr,hdr;
        private Pair(PhotoIdentity identity,double headroom,StoredRgb.Reader sdr,StoredRgb.Reader hdr){this.identity=identity;this.headroom=headroom;this.sdr=sdr;this.hdr=hdr;}
        public GainmapMath.Image sdr(){return sdr.gainmapImage();}public GainmapMath.Image hdr(){return hdr.gainmapImage();}
    }
    public static final class Writer implements AutoCloseable {
        public final PhotoIdentity identity;public final double headroom;
        private final StoredRgb.Writer sdr,hdr;private int next;private boolean done,begun;private Pair pair;
        Writer(PhotoIdentity identity,double headroom,StoredRgb.Writer sdr,StoredRgb.Writer hdr){this.identity=identity;this.headroom=headroom;this.sdr=sdr;this.hdr=hdr;}
        public synchronized void writeRows(int firstRow,int count,double[] processedSdr,double[] processedHdr)throws IOException{
            if(done || firstRow!=next || count<1)throw new IOException("paired row order/state");
            sdr.writeRows(firstRow,count,processedSdr);hdr.writeRows(firstRow,count,processedHdr);next+=count;
        }
        public synchronized Pair seal()throws IOException{
            if(done || next!=identity.frame.height)throw new IOException("incomplete processed pair");
            try{StoredRgb.Reader s=sdr.seal(),h=hdr.seal();pair=new Pair(identity,headroom,s,h);done=true;return pair;}
            catch(IOException|RuntimeException e){try{abort();}catch(IOException close){e.addSuppressed(close);}throw e;}
        }
        public synchronized Pair pair(){if(!done || pair==null)throw new IllegalStateException("pair not sealed");return pair;}
        public HdrBeautyProcessor.PairSink asHdrSink(){return new HdrBeautyProcessor.PairSink(){
            public void begin(Object exactSource,int width,int height,String settingsSha256,String geometryId,double declaredHeadroom)throws IOException{
                synchronized(Writer.this){if(begun || done || exactSource!=identity.exactSource || width!=identity.frame.width || height!=identity.frame.height
                        || !identity.settingsSha256.equals(settingsSha256) || !identity.frame.geometryId.equals(geometryId) || declaredHeadroom!=headroom)
                    throw new IOException("HDR pair source/settings/geometry/headroom mismatch");begun=true;}
            }
            public void writeRows(int firstRow,int rows,double[] processedSdr,double[] processedHdr)throws IOException{
                synchronized(Writer.this){if(!begun)throw new IOException("HDR pair sink has not begun");Writer.this.writeRows(firstRow,rows,processedSdr,processedHdr);}
            }
            public void commit()throws IOException{synchronized(Writer.this){if(!begun)throw new IOException("HDR pair sink has not begun");seal();}}
            public void abort()throws IOException{Writer.this.abort();}
        };}
        public synchronized void abort()throws IOException{done=true;pair=null;IOException first=null;try{sdr.abort();}catch(IOException e){first=e;}try{hdr.abort();}catch(IOException e){if(first==null)first=e;else first.addSuppressed(e);}if(first!=null)throw first;}
        @Override public synchronized void close()throws IOException{IOException first=null;try{sdr.close();}catch(IOException e){first=e;}try{hdr.close();}catch(IOException e){if(first==null)first=e;else first.addSuppressed(e);}if(first!=null)throw first;}
    }
}
