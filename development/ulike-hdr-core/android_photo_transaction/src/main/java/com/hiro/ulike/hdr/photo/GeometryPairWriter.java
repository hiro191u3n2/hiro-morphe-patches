package com.hiro.ulike.hdr.photo;

import com.hiro.ulike.hdr.beauty.HdrBeautyProcessor;
import java.io.IOException;

/** Stages source HDR/SDR, then applies the exact same integer orientation/crop to both.
 * 90-degree rotation uses short block-verified spans, not full-row reads for each pixel. */
public final class GeometryPairWriter implements HdrBeautyProcessor.PairSink,AutoCloseable {
    private final PhotoTransaction transaction;private final PhotoGeometry geometry;
    private final PairedStore.Writer source;private PairedStore.Writer destination;
    private final HdrBeautyProcessor.PairSink sourceSink;
    public final double headroom;public final int tileRows;public final long tileArrayWorkspaceBound;
    private boolean begun,done;private PairedStore.Pair pair;
    GeometryPairWriter(PhotoTransaction transaction,PhotoGeometry geometry,double headroom,long maxWorkspace)throws IOException{
        if(geometry==null || maxWorkspace<1 || transaction.identity.frame.width!=geometry.width || transaction.identity.frame.height!=geometry.height
                || !transaction.identity.frame.geometryId.equals(geometry.id()))throw new IllegalArgumentException("final photo identity must describe actual geometry");
        this.transaction=transaction;this.geometry=geometry;this.headroom=headroom;
        int rows=Math.min(64,geometry.height);while(rows>1 && workspace(geometry.width,rows)>maxWorkspace)rows=Math.max(1,rows/2);
        tileArrayWorkspaceBound=workspace(geometry.width,rows);if(tileArrayWorkspaceBound>maxWorkspace)throw new IOException("one geometry row exceeds workspace budget");tileRows=rows;
        PhotoIdentity sourceIdentity=transaction.identity.sourceRaster(geometry.sourceWidth,geometry.sourceHeight);
        source=transaction.createPairFor(sourceIdentity,headroom);sourceSink=source.asHdrSink();
    }
    private static long workspace(int width,int rows){return (long)width*rows*3*8*2+(long)rows*3*8*2+16384;}
    public HdrBeautyProcessor.PairSink asHdrSink(){return this;}
    @Override public synchronized void begin(Object exactSource,int width,int height,String settingsSha256,String geometryId,double headroom)throws Exception{
        if(begun || done)throw new IOException("geometry pair already begun/closed");sourceSink.begin(exactSource,width,height,settingsSha256,geometryId,headroom);begun=true;
    }
    @Override public synchronized void writeRows(int firstRow,int rows,double[] sdr,double[] hdr)throws Exception{
        if(!begun || done)throw new IOException("geometry pair state");sourceSink.writeRows(firstRow,rows,sdr,hdr);
    }
    @Override public synchronized void commit()throws Exception{
        if(!begun || done)throw new IOException("geometry pair state");
        try{
            sourceSink.commit();PairedStore.Pair input=source.pair();destination=transaction.createPairFor(transaction.identity,headroom);
            transform(input);pair=destination.seal();source.abort();done=true;
        }catch(Exception|Error failure){try{abort();}catch(Exception close){failure.addSuppressed(close);}throw failure;}
    }
    private void transform(PairedStore.Pair input)throws IOException{
        int w=geometry.width,h=geometry.height;double[] sdr=new double[w*tileRows*3],hdr=new double[sdr.length];
        double[] sdrSpan=new double[tileRows*3],hdrSpan=new double[sdrSpan.length];int[] first=new int[2],last=new int[2];
        for(int start=0;start<h;start+=tileRows){
            if(Thread.currentThread().isInterrupted())throw new IOException("geometry transform cancelled");int count=Math.min(tileRows,h-start);
            if(geometry.rotationClockwise%180==0){
                for(int row=0;row<count;row++){
                    geometry.sourcePixel(0,start+row,first);geometry.sourcePixel(w-1,start+row,last);
                    if(first[1]!=last[1])throw new AssertionError("non-row integer mapping");int left=Math.min(first[0],last[0]),offset=row*w*3;
                    input.sdr.readSpan(first[1],left,w,sdr,offset);input.hdr.readSpan(first[1],left,w,hdr,offset);
                    if(first[0]>last[0]){reverse(sdr,offset,w);reverse(hdr,offset,w);}
                }
            }else{
                for(int x=0;x<w;x++){
                    geometry.sourcePixel(x,start,first);geometry.sourcePixel(x,start+count-1,last);
                    if(first[1]!=last[1])throw new AssertionError("non-column integer mapping");int left=Math.min(first[0],last[0]);
                    input.sdr.readSpan(first[1],left,count,sdrSpan,0);input.hdr.readSpan(first[1],left,count,hdrSpan,0);
                    for(int row=0;row<count;row++){
                        int src=(first[0]<=last[0]?row:count-1-row)*3,dst=(row*w+x)*3;
                        System.arraycopy(sdrSpan,src,sdr,dst,3);System.arraycopy(hdrSpan,src,hdr,dst,3);
                    }
                }
            }
            destination.writeRows(start,count,sdr,hdr);
        }
    }
    private static void reverse(double[] row,int offset,int pixels){for(int x=0;x<pixels/2;x++)for(int c=0;c<3;c++){
        int a=offset+x*3+c,b=offset+(pixels-1-x)*3+c;double tmp=row[a];row[a]=row[b];row[b]=tmp;
    }}
    public synchronized PairedStore.Pair pair(){if(!done || pair==null)throw new IllegalStateException("transformed pair not complete");return pair;}
    @Override public synchronized void abort()throws IOException{
        done=true;pair=null;IOException failure=null;try{source.abort();}catch(IOException e){failure=e;}
        if(destination!=null)try{destination.abort();}catch(IOException e){if(failure==null)failure=e;else failure.addSuppressed(e);}if(failure!=null)throw failure;
    }
    @Override public synchronized void close()throws IOException{if(!done)abort();}
}
