package com.hiro.ulike.hdr.input;

import com.hiro.ulike.hdr.analysisinput.AnalysisInput;
import com.hiro.ulike.hdr.color.P010SceneSource;
import com.hiro.ulike.hdr.color.SdrRendition;
import com.hiro.ulike.hdr.faceprobe.*;
import com.hiro.ulike.hdr.stillanalysis.StockStillAnalysis;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Independent numerical and ownership tests. Android Bitmap/native code is never simulated. */
public final class FullresReview {
    static long checks;
    static final AnalysisCapacity NATIVE=AnalysisCapacity.nativeSize4080x3060Candidate();
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    interface Action { void run()throws Exception; }
    static void rejects(Action action,Class<? extends Throwable> type)throws Exception{
        try{action.run();throw new AssertionError("Expected "+type.getSimpleName());}
        catch(Throwable e){if(!type.isInstance(e)){if(e instanceof Exception)throw (Exception)e;throw (Error)e;}checks++;}
    }
    static SdrRendition source(int w,int h){
        short[][] planes={new short[w*h],new short[w*h/4],new short[w*h/4]};
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)planes[0][y*w+x]=(short)((29*x+53*y)%1024);
        Arrays.fill(planes[1],(short)512);Arrays.fill(planes[2],(short)512);
        Object owner=new Object();CaptureMatch.Context c=new CaptureMatch.Context(owner,owner,owner,owner,owner,19,"0",null);
        CaptureMatch.Result r=new CaptureMatch.Result(c,owner,987654321L,73,null,"5",10000000L,206);
        HdrFrame frame=new HdrFrame(w,h,HdrFrame.Encoding.BT2020_NCL_HLG_FULL,c,r,planes,3L*w*h);
        return new SdrRendition(new P010SceneSource(frame,P010SceneSource.ChromaLocation.COSITED,
                P010SceneSource.ChromaLocation.COSITED,"independent neutral chroma fixture"),1);
    }
    static AnalysisInput.Streaming streaming(int w,int h)throws Exception{
        return AnalysisInput.prepareStreaming(source(w,h),"geometry-unverified",new byte[]{7,4,1},291,NATIVE);
    }
    static String hex(byte[] bytes){StringBuilder text=new StringBuilder();for(byte b:bytes)text.append(String.format("%02x",b&255));return text.toString();}
    static class Rows implements AnalysisInput.RowTarget {
        final int w,h;int rowCount,beginCount,endCount,abortCount;int[] borrowed;
        Rows(int w,int h){this.w=w;this.h=h;}
        public void begin(int width,int height)throws Exception{beginCount++;check(width==w&&height==h,"unchanged dimensions");}
        public void row(int y,int[] row)throws Exception{check(y==rowCount++&&row.length==w,"all rows full width in order");borrowed=row;}
        public void complete()throws Exception{endCount++;}
        public void abort()throws Exception{abortCount++;}
    }
    static int[] independentGray(){
        int[] values=new int[1024];double a=.17883277,b=1-4*a,c=.5-a*Math.log(4*a);
        for(int i=0;i<1024;i++){
            double encoded=i/1023.0,linear=encoded<=.5?encoded*encoded/3:(Math.exp((encoded-c)/a)+b)/12;
            double tone=linear/(1+linear),s=tone<=.0031308?12.92*tone:1.055*Math.pow(tone,1/2.4)-.055;
            int q=(int)Math.floor(255*s+.5);values[i]=0xff000000|(q<<16)|(q<<8)|q;
        }return values;
    }
    static void full(File output)throws Exception{
        final int w=4080,h=3060;final AnalysisInput.Streaming input=streaming(w,h);final int[] gray=independentGray();
        check(Runtime.getRuntime().maxMemory()<=64L*1024*1024,"bounded host heap, cannot hold both P010 and int[P]");
        rejects(input::descriptor,IllegalStateException.class);
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        ByteArrayOutputStream prefix=new ByteArrayOutputStream();DataOutputStream head=new DataOutputStream(prefix);
        head.write(new byte[]{'U','L','S','P',1});head.writeInt(w);head.writeInt(h);hash.update(prefix.toByteArray());
        final byte[] rowBytes=new byte[4*w];
        Rows target=new Rows(w,h){@Override public void row(int y,int[] pixels)throws Exception{
            super.row(y,pixels);
            for(int x=0;x<w;x++){
                int expected=gray[(29*x+53*y)%1024];check(pixels[x]==expected,"independent full-grid grayscale oracle");
                int offset=4*x;rowBytes[offset]=(byte)(pixels[x]>>>24);rowBytes[offset+1]=(byte)(pixels[x]>>>16);
                rowBytes[offset+2]=(byte)(pixels[x]>>>8);rowBytes[offset+3]=(byte)pixels[x];
            }
            hash.update(rowBytes);Arrays.fill(pixels,0xdeadbeef); // caller mutation cannot affect subsequent rows
        }};
        AnalysisInput.Descriptor descriptor=input.transfer(target).descriptor;
        check(target.rowCount==h&&target.beginCount==1&&target.endCount==1&&target.abortCount==0,"complete full native grid");
        check(descriptor==input.descriptor()&&!input.available()&&!descriptor.resized&&!descriptor.nativeGeometryCalibrated,"one exact noncalibrated descriptor");
        check(Arrays.stream(target.borrowed).allMatch(v->v==0),"borrowed row wiped");
        check(hex(hash.digest()).equals(descriptor.proxySha256),"independent streaming digest");
        rejects(()->input.transfer(target),IllegalStateException.class);input.close();
        try(PrintWriter out=new PrintWriter(output,"UTF-8")){
            out.println("{\"width\":"+w+",\"height\":"+h+",\"checks\":"+checks+",\"heap_limit\":"+Runtime.getRuntime().maxMemory()+
                ",\"source\":\""+descriptor.sourceSha256+"\",\"proxy\":\""+descriptor.proxySha256+"\",\"settings\":\""+descriptor.settingsSha256+"\"}");
        }
    }
    static void streamFailures()throws Exception{
        for(int point=0;point<6;point++){
            final int p=point;final AnalysisInput.Streaming input=streaming(32,24);
            final AssertionError same=new AssertionError("same cleanup throwable");
            Rows target=new Rows(32,24){
                @Override public void begin(int w,int h)throws Exception{super.begin(w,h);if(p==0)throw same;}
                @Override public void row(int y,int[] row)throws Exception{super.row(y,row);
                    check(!input.available(),"exclusive transfer");
                    rejects(input::close,IllegalStateException.class);
                    rejects(()->input.transfer(this),IllegalStateException.class);
                    rejects(input::descriptor,IllegalStateException.class);
                    if(p==1&&y==3)throw same;if(p==3&&y==3)Thread.currentThread().interrupt();
                }
                @Override public void complete()throws Exception{super.complete();if(p==2)throw same;}
                @Override public void abort()throws Exception{super.abort();throw same;}
            };
            if(p==4)Thread.currentThread().interrupt();
            if(p==5){input.close();rejects(()->input.transfer(target),IllegalStateException.class);check(target.beginCount==0,"closed never invokes sink");continue;}
            Throwable caught=null;try{input.transfer(target);}catch(Throwable e){caught=e;}
            check(caught!=null&&!input.available(),"every failed transfer consumed");
            check(target.abortCount==(p==4?0:1),"abort only begun sink");
            if(p==3||p==4)check(Thread.currentThread().isInterrupted(),"interruption preserved");
            if(p<3)check(caught==same&&caught.getSuppressed().length==0,"same cleanup throwable not self-suppressed");
            Thread.interrupted();rejects(input::descriptor,IllegalStateException.class);
            if(target.borrowed!=null)check(Arrays.stream(target.borrowed).allMatch(v->v==0),"row wiped after failure");
            input.close();
        }
    }
    static void capacity()throws Exception{
        check(NATIVE.requireGrid(4080,3060)==12484800L,"exact supported candidate");
        check(NATIVE.requireGrid(3060,4080)==12484800L,"rotated candidate");
        check(NATIVE.requireTransferredPayload(4080,3060)==187288320L,"15P plus one row, excludes native/GPU/app");
        rejects(()->NATIVE.requireGrid(4082,3060),IllegalArgumentException.class);
        rejects(()->NATIVE.requireGrid(4080,3061),IllegalArgumentException.class);
        rejects(()->NATIVE.requireGrid(5712,4284),IllegalArgumentException.class);
        rejects(()->NATIVE.requireGrid(Integer.MAX_VALUE,Integer.MAX_VALUE),IllegalArgumentException.class);
        rejects(()->NATIVE.requireGrid(-1,-1),IllegalArgumentException.class);
        rejects(()->AnalysisCapacity.legacyDiagnostic().requireGrid(4080,3060),IllegalArgumentException.class);
        rejects(()->new ProbeLedger(4080,3060),IllegalArgumentException.class);
        new ProbeLedger(4080,3060,NATIVE);checks++;
        rejects(()->AnalysisInput.prepareStreaming(source(32,24),"g",new byte[]{1},1,AnalysisCapacity.legacyDiagnostic()),IllegalArgumentException.class);
        rejects(()->PixelOrientationEvidence.measure(new int[4],2,2,new int[4],2,2,NATIVE),IllegalArgumentException.class);
        for(Field field:AnalysisInput.Streaming.class.getDeclaredFields())check(!field.getType().equals(int[].class),"stream never retains full integer raster");
    }
    static void ownership()throws Exception{
        for(boolean submitted:new boolean[]{false,true}){
            AtomicInteger release=new AtomicInteger();Object resource=new Object();SubmissionOwnership<Object> owner=new SubmissionOwnership<>(resource,r->release.incrementAndGet());
            check(owner.beforeTransfer()==resource,"resource initially owned");
            SubmissionOwnership.Claim<Object> claim=owner.transfer();owner.close();owner.close();
            check(release.get()==0&&claim.resource()==resource,"outer close cannot recycle transferred bitmap");
            rejects(owner::transfer,IllegalStateException.class);rejects(owner::beforeTransfer,IllegalStateException.class);
            if(submitted){claim.submitted();rejects(claim::submitted,IllegalStateException.class);}
            claim.joined(true);check(release.get()==1&&owner.state()==SubmissionOwnership.State.CLOSED,"only verified join releases once");
            owner.close();rejects(claim::resource,IllegalStateException.class);rejects(()->claim.joined(true),IllegalStateException.class);
            check(release.get()==1,"duplicate calls never release twice");
        }
        AtomicInteger releases=new AtomicInteger();SubmissionOwnership<Object> failed=new SubmissionOwnership<>(new Object(),r->releases.incrementAndGet());
        SubmissionOwnership.Claim<Object> claim=failed.transfer();claim.submitted();claim.joined(false);failed.close();
        check(releases.get()==0&&failed.state()==SubmissionOwnership.State.QUARANTINED,"failed join retains input");
        rejects(claim::resource,IllegalStateException.class);rejects(()->claim.joined(true),IllegalStateException.class);rejects(failed::transfer,IllegalStateException.class);
        SubmissionOwnership<Object> cleanup=new SubmissionOwnership<>(new Object(),r->{throw new AssertionError("release failed");});
        SubmissionOwnership.Claim<Object> cleanupClaim=cleanup.transfer();rejects(()->cleanupClaim.joined(true),AssertionError.class);
        cleanup.close();check(cleanup.state()==SubmissionOwnership.State.QUARANTINED,"release Error quarantines");
        for(int i=0;i<100;i++){
            AtomicInteger count=new AtomicInteger();SubmissionOwnership<Object> raced=new SubmissionOwnership<>(new Object(),r->count.incrementAndGet());
            AtomicReference<SubmissionOwnership.Claim<Object>> winner=new AtomicReference<>();CountDownLatch start=new CountDownLatch(1);
            Thread t1=new Thread(()->{try{start.await();winner.set(raced.transfer());}catch(IllegalStateException expected){}catch(InterruptedException e){throw new AssertionError(e);}});
            Thread t2=new Thread(()->{try{start.await();raced.close();}catch(InterruptedException e){throw new AssertionError(e);}});
            t1.start();t2.start();start.countDown();t1.join();t2.join();
            if(winner.get()!=null){check(count.get()==0,"concurrent original close respects transfer");winner.get().joined(true);}
            check(count.get()==1&&raced.state()==SubmissionOwnership.State.CLOSED,"transfer/close linearized");
        }
    }
    static StockStillAnalysis.RenderedDiagnostic diagnostic(int w,int h,int[] owned)throws Exception{
        Constructor<StockStillAnalysis.RenderedDiagnostic> ctor=StockStillAnalysis.RenderedDiagnostic.class.getDeclaredConstructor(int.class,int.class,int.class,int[].class,AnalysisCapacity.class);
        ctor.setAccessible(true);return ctor.newInstance(291,w,h,owned,NATIVE);
    }
    static void diagnostic()throws Exception{
        int[] large=new int[2500*2000];Arrays.fill(large,0xaabbccdd);StockStillAnalysis.RenderedDiagnostic d=diagnostic(2500,2000,large);
        rejects(d::pixels,IllegalStateException.class);
        int value=d.consume((data,w,h)->{check(data==large&&w==2500&&h==2000,"one owned callback array, no consuming clone");return data.length;});
        check(value==large.length&&Arrays.stream(large).allMatch(v->v==0),"large conversion wipes original owned array");
        rejects(()->d.consume((p,w,h)->null),IllegalStateException.class);d.close();
        for(int mode=0;mode<5;mode++){
            final int m=mode;final int[] buffer={1,2,3,4};final StockStillAnalysis.RenderedDiagnostic current=diagnostic(2,2,buffer);
            Throwable failure=null;
            if(m==3)Thread.currentThread().interrupt();
            try{current.consume((pixels,w,h)->{
                rejects(current::close,IllegalStateException.class);rejects(current::pixels,IllegalStateException.class);
                if(m==0)throw new AssertionError("consumer failed");if(m==1)return pixels;
                if(m==2)throw new IOException("consumer failed");return "ok";
            });}catch(Throwable e){failure=e;}
            check((m==4)==(failure==null),"consumer failure propagated");
            check(Arrays.stream(buffer).allMatch(v->v==0),"consumer success/error/alias/interruption all erase");
            if(m==3)check(Thread.currentThread().isInterrupted(),"diagnostic interrupt preserved");Thread.interrupted();current.close();
        }
    }
    static void ledger()throws Exception{
        ProbeLedger l=new ProbeLedger(4080,3060,NATIVE);l.initialized(0);l.submit();l.face(new float[0][],new float[0]);l.rendered(4080,3060,12484800);
        check(l.await(false,1),"full grid callback complete");l.rendered(4080,3060,12484800);check(!l.finish(true).callbacksObserved,"duplicate callback fails");
        ProbeLedger q=new ProbeLedger(4080,3060,NATIVE);q.initialized(0);q.submit();q.face(new float[0][],new float[0]);q.rendered(4080,3060,12484800);
        ProbeLedger.Snapshot before=q.finish(false);check(!before.callbacksObserved,"failed join never success");q.face(new float[0][],new float[0]);q.rendered(4080,3060,12484800);q.initialized(0);
        check(!before.callbacksObserved,"late callbacks cannot amend frozen failure");rejects(()->q.finish(true),IllegalStateException.class);
    }
    public static void main(String[] args)throws Exception{
        if(args[0].equals("full")){full(new File(args[1]));System.out.println("full-native-grid checks="+checks);return;}
        capacity();streamFailures();ownership();diagnostic();ledger();System.out.println("{\"checks\":"+checks+",\"ownership_races\":100,\"android_bitmap_or_native_execution\":false}");
    }
}
