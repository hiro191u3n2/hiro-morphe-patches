package com.hiro.ulike.hdr.input;
import com.hiro.ulike.hdr.analysisinput.AnalysisInput;
import com.hiro.ulike.hdr.faceprobe.AnalysisCapacity;
import com.hiro.ulike.hdr.color.P010SceneSource;
import com.hiro.ulike.hdr.color.SdrRendition;
import java.io.*;
import java.util.*;

/** Real 12.5MP P010 -> full grid RGB8 row stream under a 64MiB host Java heap. No Android execution. */
public final class StreamingInputTest {
    private static int checks;
    private static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    private interface Task {void run()throws Exception;}
    private static void rejects(Task task,Class<? extends Throwable> kind)throws Exception {
        try{task.run();throw new AssertionError("Expected rejection");}catch(Throwable failure){if(!kind.isInstance(failure))throw failure;checks++;}
    }
    private static SdrRendition rendition(int w,int h) {
        short[][] planes={new short[w*h],new short[w*h/4],new short[w*h/4]};
        Arrays.fill(planes[0],(short)512);Arrays.fill(planes[1],(short)512);Arrays.fill(planes[2],(short)512);
        Object o=new Object();CaptureMatch.Context c=new CaptureMatch.Context(o,o,o,o,o,17,"0",null);
        CaptureMatch.Result result=new CaptureMatch.Result(c,o,777,18,null,"5",10000000L,206);
        HdrFrame frame=new HdrFrame(w,h,HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED,c,result,planes,3L*w*h);
        return new SdrRendition(new P010SceneSource(frame,P010SceneSource.ChromaLocation.COSITED,P010SceneSource.ChromaLocation.COSITED,"stream-test"),3);
    }
    private static AnalysisInput.Streaming prepare(SdrRendition r)throws Exception{return AnalysisInput.prepareStreaming(r,"native-grid",new byte[]{1,2,3},71,AnalysisCapacity.nativeSize4080x3060Candidate());}
    private static class Target implements AnalysisInput.RowTarget {
        int w,h,rows,begin,complete,abort;int[] last;
        public void begin(int w,int h)throws Exception{this.w=w;this.h=h;begin++;}
        public void row(int y,int[] values)throws Exception{check(y==rows++ && values.length==w,"ordered row");last=values;}
        public void complete()throws Exception{check(rows==h,"all rows");complete++;}
        public void abort()throws Exception{abort++;}
    }
    public static void main(String[] args)throws Exception {
        SdrRendition small=rendition(64,48);
        AnalysisInput.Owned old=AnalysisInput.prepare(small,"native-grid",new byte[]{1,2,3},71,AnalysisInput.Budget.currentSdk());
        String reference=old.descriptor().proxySha256;old.close();
        AnalysisInput.Streaming candidate=prepare(small);
        rejects(candidate::descriptor,IllegalStateException.class);
        Target first=new Target();AnalysisInput.Submission submit=candidate.transfer(first);
        check(submit.descriptor==candidate.descriptor(),"descriptor identity");
        check(reference.equals(candidate.descriptor().proxySha256),"stream and old exact pixel digest");
        check(Arrays.stream(first.last).allMatch(x->x==0),"borrowed row wiped");
        rejects(()->candidate.transfer(new Target()),IllegalStateException.class);
        for(int where=0;where<4;where++) {
            final int point=where;AnalysisInput.Streaming failed=prepare(small);
            Target t=new Target(){public void begin(int w,int h)throws Exception{super.begin(w,h);if(point==0)throw new IOException("begin");}
                public void row(int y,int[] row)throws Exception{super.row(y,row);if(point==1)throw new IOException("row");if(point==3)throw new OutOfMemoryError("synthetic target OOM");}
                public void complete()throws Exception{if(point==2)throw new IOException("complete");super.complete();}};
            rejects(()->failed.transfer(t),point==3?OutOfMemoryError.class:IOException.class);
            check(t.abort==1 && !failed.available(),"all failures consume and abort");rejects(failed::descriptor,IllegalStateException.class);
            if(t.last!=null)check(Arrays.stream(t.last).allMatch(x->x==0),"failed borrowed row wiped");
        }
        AnalysisInput.Streaming reentrant=prepare(small);
        reentrant.transfer(new Target(){public void row(int y,int[] row)throws Exception{super.row(y,row);rejects(reentrant::close,IllegalStateException.class);}});
        AnalysisInput.Streaming before=prepare(small);Target unstarted=new Target();Thread.currentThread().interrupt();
        try{rejects(()->before.transfer(unstarted),IOException.class);check(unstarted.begin==0 && !before.available() && Thread.currentThread().isInterrupted(),"cancel before target");}finally{Thread.interrupted();}
        AnalysisInput.Streaming during=prepare(small);Target interrupting=new Target(){public void row(int y,int[] row)throws Exception{super.row(y,row);Thread.currentThread().interrupt();}};
        try{rejects(()->during.transfer(interrupting),IOException.class);check(interrupting.abort==1 && interrupting.rows==1 && Thread.currentThread().isInterrupted(),"cancel during transfer");}finally{Thread.interrupted();}
        AnalysisInput.Streaming selfSuppressed=prepare(small);IOException same=new IOException("same");
        try{selfSuppressed.transfer(new Target(){public void row(int y,int[] row)throws Exception{throw same;}public void abort()throws Exception{throw same;}});throw new AssertionError();}catch(IOException e){check(e==same,"cleanup cannot replace primary through self-suppression");}
        rejects(()->AnalysisInput.prepareStreaming(small,"g",new byte[]{1},1,AnalysisCapacity.legacyDiagnostic()),IllegalArgumentException.class);
        AnalysisCapacity cap=AnalysisCapacity.nativeSize4080x3060Candidate();
        check(cap.requireGrid(4080,3060)==12484800 && cap.requireGrid(3060,4080)==12484800,"native oriented grids accepted");
        check(cap.requireTransferredPayload(4080,3060)==187288320L,"known exact 15P+row payload");
        check(!cap.comparePhotographicOrientation,"no photographic orientation full raster");
        rejects(()->cap.requireGrid(4082,3058),IllegalArgumentException.class);
        rejects(()->cap.requireGrid(4080,3061),IllegalArgumentException.class);
        rejects(()->cap.requireGrid(5712,4284),IllegalArgumentException.class);
        rejects(()->AnalysisCapacity.legacyDiagnostic().requireGrid(4080,3060),IllegalArgumentException.class);
        SdrRendition full=rendition(4080,3060);AnalysisInput.Streaming stream=prepare(full);
        final int[] firstPixel={0};final long[] count={0};final int[][] borrowedRow={null};
        Target sink=new Target(){public void row(int y,int[] pixels)throws Exception{
            super.row(y,pixels);if(y==0){firstPixel[0]=pixels[0];borrowedRow[0]=pixels;}check(borrowedRow[0]==pixels,"one reused row");
            for(int pixel:pixels){check(pixel==firstPixel[0],"uniform independent fixture");count[0]++;}
        }};
        stream.transfer(sink);AnalysisInput.Descriptor d=stream.descriptor();
        check(count[0]==12484800 && sink.rows==3060 && d.width==4080 && d.height==3060,"every unscaled native pixel transferred");
        check(d.frameIdentity==full.hdrSource().frameIdentity() && !d.resized && !d.nativeGeometryCalibrated,"same frame, no device calibration claim");
        check(Arrays.stream(borrowedRow[0]).allMatch(x->x==0),"full row wiped");
        try(PrintWriter report=new PrintWriter(new File(args[0],"stream-native.json"),"UTF-8")){
            report.println("{\"width\":4080,\"height\":3060,\"argb\":"+Integer.toUnsignedLong(firstPixel[0])+",\"sourceSha256\":\""+d.sourceSha256+"\",\"proxySha256\":\""+d.proxySha256+"\",\"host_max_heap\":"+Runtime.getRuntime().maxMemory()+"}");
        }
        System.out.println("PASS "+checks+" streaming checks; native grid=4080x3060; device execution=false");
    }
}
