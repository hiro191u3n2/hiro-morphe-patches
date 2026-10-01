package com.hiro.ulike.hdr.input;
import com.hiro.ulike.hdr.analysisinput.AnalysisInput;
import com.hiro.ulike.hdr.color.P010SceneSource;
import com.hiro.ulike.hdr.color.SdrRendition;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Actual immutable HdrFrame/SdrRendition integration, with synthetic camera samples. */
public final class AnalysisInputTest {
    private static int checks;
    private static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    private interface Task {void run()throws Exception;}
    private static void rejects(Task task,Class<? extends Throwable> type)throws Exception {
        try{task.run();throw new AssertionError("accepted invalid input");}catch(Throwable e){if(!type.isInstance(e))throw e;checks++;}
    }
    private static HdrFrame frame(int width,int height,long timestamp,int delta){
        short[][] codes={new short[width*height],new short[width*height/4],new short[width*height/4]};
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)codes[0][y*width+x]=(short)((x*3+y*7+delta)%1024);
        for(int y=0;y<height/2;y++)for(int x=0;x<width/2;x++){codes[1][y*width/2+x]=(short)(440+(x*5+y*3)%144);codes[2][y*width/2+x]=(short)(464+(x*3+y*7)%96);}
        Object o=new Object();CaptureMatch.Context c=new CaptureMatch.Context(o,o,o,o,o,9,"0",null);
        CaptureMatch.Result r=new CaptureMatch.Result(c,o,timestamp,123,null,"5",10000000L,206);
        return new HdrFrame(width,height,HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED,c,r,codes,(long)width*height*3);
    }
    private static SdrRendition rendition(HdrFrame f){return new SdrRendition(new P010SceneSource(f,P010SceneSource.ChromaLocation.COSITED,P010SceneSource.ChromaLocation.COSITED,"synthetic-analysis-test"),3.0);}
    private static AnalysisInput.Owned owned(SdrRendition r)throws Exception{return AnalysisInput.prepare(r,"sensor-raster",new byte[]{1,2,3},71,AnalysisInput.Budget.currentSdk());}
    private static class Target implements AnalysisInput.RowTarget {
        int width,height,next,begins,commits,aborts;int[] retained;
        @Override public void begin(int w,int h)throws Exception{begins++;width=w;height=h;}
        @Override public void row(int y,int[] row)throws Exception{check(y==next++ && row.length==width,"ordered full-width row");retained=row;}
        @Override public void complete()throws Exception{check(next==height,"complete grid");commits++;}
        @Override public void abort()throws Exception{aborts++;}
    }
    public static void main(String[] args)throws Exception {
        HdrFrame frame=frame(64,48,100000,0);SdrRendition rendition=rendition(frame);
        String before=AnalysisInput.sourceDigest(frame);byte[] settings={1,2,3};
        AnalysisInput.Owned input=AnalysisInput.prepare(rendition,"sensor-raster",settings,71,AnalysisInput.Budget.currentSdk());settings[0]=99;
        AnalysisInput.Descriptor d=input.descriptor();check(d.frameIdentity==frame && d.width==64 && d.height==48 && d.nonce==71 && d.sensorTimestampNs==100000,"exact input source");
        check(!d.resized && !d.nativeGeometryCalibrated,"no calibration inferred");
        check(d.settingsSha256.equals("039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81"),"owned settings digest");
        check(d.sourceSha256.equals(before),"actual source digest");
        try(DataOutputStream raw=new DataOutputStream(new FileOutputStream(new File(args[0],"proxy.argb")))) {
            Target target=new Target(){@Override public void row(int y,int[] row)throws Exception{super.row(y,row);for(int x=0;x<row.length;x++){check(row[x]==(0xff000000|rendition.rgb8(frame,x,y)),"actual same pixel");raw.writeInt(row[x]);}Arrays.fill(row,0);}};
            AnalysisInput.Submission sent=input.transfer(target);check(sent.descriptor==d,"descriptor retained through transfer");check(target.commits==1 && target.aborts==0,"single success");
            check(Arrays.stream(target.retained).allMatch(x->x==0),"borrowed row cleared");
        }
        check(!input.available(),"consumed success");rejects(()->input.transfer(new Target()),IllegalStateException.class);input.close();
        check(before.equals(AnalysisInput.sourceDigest(frame)),"original 10-bit pixels unchanged");
        try(PrintWriter report=new PrintWriter(new File(args[0],"fixture.json"),"UTF-8")){report.println("{\"sourceSha256\":\""+before+"\",\"proxySha256\":\""+d.proxySha256+"\"}");}
        check(!before.equals(AnalysisInput.sourceDigest(frame(64,48,100001,0))),"timestamp included");
        check(!before.equals(AnalysisInput.sourceDigest(frame(64,48,100000,1))),"pixel data included");
        check(before.equals(AnalysisInput.sourceDigest(frame(64,48,100000,0))),"digest deterministic but not Java identity proof");
        for(int failure=0;failure<3;failure++) {
            final int point=failure;AnalysisInput.Owned failed=owned(rendition);
            Target t=new Target(){@Override public void begin(int w,int h)throws Exception{super.begin(w,h);if(point==0)throw new IOException("begin");}
                @Override public void row(int y,int[] row)throws Exception{super.row(y,row);if(point==1&&y==2)throw new IOException("row");}
                @Override public void complete()throws Exception{if(point==2)throw new IOException("complete");super.complete();}};
            rejects(()->failed.transfer(t),IOException.class);check(t.aborts==1 && t.commits==0 && !failed.available(),"failure consumed and aborted");
            rejects(()->failed.transfer(t),IllegalStateException.class);
        }
        AnalysisInput.Owned cleanup=owned(rendition);try{cleanup.transfer(new Target(){@Override public void row(int y,int[] row)throws Exception{throw new IOException("primary");}@Override public void abort()throws Exception{throw new IOException("cleanup");}});throw new AssertionError();}catch(IOException e){check(e.getMessage().equals("primary")&&e.getSuppressed().length==1,"cleanup preserves primary");}
        AnalysisInput.Owned reentrant=owned(rendition);reentrant.transfer(new Target(){@Override public void row(int y,int[] row)throws Exception{super.row(y,row);rejects(reentrant::close,IllegalStateException.class);rejects(()->reentrant.transfer(this),IllegalStateException.class);}});
        AnalysisInput.Owned cancelled=owned(rendition);Thread.currentThread().interrupt();Target t=new Target();
        try{rejects(()->cancelled.transfer(t),IOException.class);check(Thread.currentThread().isInterrupted() && t.begins==0 && !cancelled.available(),"interrupted before target; ownership cleared");}finally{Thread.interrupted();}
        Thread.currentThread().interrupt();try{rejects(()->owned(rendition),IOException.class);check(Thread.currentThread().isInterrupted(),"prepare preserves interruption");}finally{Thread.interrupted();}
        AnalysisInput.Owned closed=owned(rendition);closed.close();rejects(()->closed.transfer(new Target()),IllegalStateException.class);
        rejects(()->AnalysisInput.prepare(rendition,"g",new byte[]{1},0,AnalysisInput.Budget.currentSdk()),IllegalArgumentException.class);
        rejects(()->AnalysisInput.prepare(rendition,"g\nbad",new byte[]{1},1,AnalysisInput.Budget.currentSdk()),IllegalArgumentException.class);
        rejects(()->AnalysisInput.prepare(rendition,"g",new byte[0],1,AnalysisInput.Budget.currentSdk()),IllegalArgumentException.class);
        rejects(()->AnalysisInput.prepare(rendition,"g",new byte[]{1},1,new AnalysisInput.Budget(3071,999999)),IllegalArgumentException.class);
        rejects(()->AnalysisInput.prepare(rendition,"g",new byte[]{1},1,new AnalysisInput.Budget(3072,3072L*4+64*4-1)),IllegalArgumentException.class);
        rejects(()->new AnalysisInput.Budget(4194305,999999999),IllegalArgumentException.class);
        HdrFrame nativeSize=frame(4080,3060,777,0);SdrRendition nativeRendition=rendition(nativeSize);
        rejects(()->owned(nativeRendition),IllegalArgumentException.class);check(nativeRendition.width()==4080 && nativeRendition.height()==3060,"native camera size refused without resize");
        System.out.println("PASS "+checks+" analysis ownership/provenance checks; Android execution=false");
    }
}
