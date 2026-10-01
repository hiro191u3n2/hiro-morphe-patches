package com.hiro.ulike.hdr.input;

import com.hiro.ulike.hdr.analysisinput.AnalysisInput;
import com.hiro.ulike.hdr.color.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/** Synthetic source fixtures and independent buffer-lifecycle adversarial tests. */
public final class InputReview {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static HdrFrame frame(int fixture) {
        int w=16,h=8;short[][] planes={new short[w*h],new short[w*h/4],new short[w*h/4]};
        for(int c=0;c<3;c++)for(int i=0;i<planes[c].length;i++)planes[c][i]=(short)((i*97+fixture*23+c*331)%1024);
        Object owner=new Object();CaptureMatch.Context context=new CaptureMatch.Context(owner,owner,owner,owner,owner,fixture+1,"camera\u03c0",fixture%2==0?null:"physical5");
        CaptureMatch.Result result=new CaptureMatch.Result(context,owner,100001L+fixture,fixture+100,null,fixture%3==0?null:"active7",fixture%2==0?null:1234567L,fixture%3==0?null:217);
        return new HdrFrame(w,h,fixture%2==0?HdrFrame.Encoding.BT2020_NCL_HLG_FULL:HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED,context,result,planes,w*h*3L);
    }
    static SdrRendition rendition(int fixture) {
        P010SceneSource.ChromaLocation horizontal=fixture%2==0?P010SceneSource.ChromaLocation.COSITED:P010SceneSource.ChromaLocation.MIDPOINT;
        P010SceneSource.ChromaLocation vertical=fixture%3==0?P010SceneSource.ChromaLocation.MIDPOINT:P010SceneSource.ChromaLocation.COSITED;
        return new SdrRendition(new P010SceneSource(frame(fixture),horizontal,vertical,"review sample convention"),Math.pow(2,fixture-3));
    }
    static AnalysisInput.Owned input(int fixture)throws Exception {return AnalysisInput.prepare(rendition(fixture),"g",new byte[]{4,9,3},fixture+1,new AnalysisInput.Budget(128,576));}
    static int[] retained(AnalysisInput.Owned in)throws Exception {Field f=AnalysisInput.Owned.class.getDeclaredField("pixels");f.setAccessible(true);return (int[])f.get(in);}
    static class Target implements AnalysisInput.RowTarget {
        int begins,rows,completes,aborts;int[] borrowed;
        public void begin(int w,int h)throws Exception{begins++;check(w==16&&h==8,"no raster change");}
        public void row(int y,int[] pixels)throws Exception{check(y==rows++&&pixels.length==16,"ordered full-width row");borrowed=pixels;}
        public void complete()throws Exception{completes++;}
        public void abort()throws Exception{aborts++;}
    }
    static void fixtures(File dir)throws Exception {
        for(int index=0;index<8;index++){
            AnalysisInput.Owned owned=input(index);AnalysisInput.Descriptor d=owned.descriptor();int[] retained=retained(owned);
            try(DataOutputStream out=new DataOutputStream(new FileOutputStream(new File(dir,"fixture"+index+".argb")))) {
                Target target=new Target(){public void row(int y,int[] pixels)throws Exception{super.row(y,pixels);for(int p:pixels){check((p>>>24)==255,"opaque analysis pixel");out.writeInt(p);}Arrays.fill(pixels,0xdeadbeef);}};
                AnalysisInput.Submission submission=owned.transfer(target);check(submission.descriptor==d,"submission immutable descriptor");
                check(Arrays.stream(target.borrowed).allMatch(v->v==0),"all borrowed row bytes wiped on success");
            }
            check(Arrays.stream(retained).allMatch(v->v==0)&&!owned.available(),"owned pixels wiped on success");
            check(d.frameIdentity.width==16&&!d.resized&&!d.nativeGeometryCalibrated,"actual source identity separate from calibration");
            try(PrintWriter out=new PrintWriter(new File(dir,"fixture"+index+".json"),"UTF-8")){out.println("{\"source\":\""+d.sourceSha256+"\",\"proxy\":\""+d.proxySha256+"\",\"settings\":\""+d.settingsSha256+"\"}");}
        }
    }
    static void failures()throws Exception {
        for(int point=0;point<4;point++){
            final int p=point;AnalysisInput.Owned owned=input(1);int[] retained=retained(owned);
            Target target=new Target(){
                public void begin(int w,int h)throws Exception{super.begin(w,h);if(p==0)throw new AssertionError("begin");}
                public void row(int y,int[] row)throws Exception{super.row(y,row);if(p==1&&y==4)throw new AssertionError("row");if(p==3&&y==2)Thread.currentThread().interrupt();}
                public void complete()throws Exception{super.complete();if(p==2)throw new AssertionError("complete");}
                public void abort()throws Exception{super.abort();throw new IOException("cleanup");}
            };
            Throwable failure=null;try{owned.transfer(target);}catch(Throwable f){failure=f;}
            check(failure!=null&&failure.getSuppressed().length==1,"Error/interruption keeps primary plus cleanup");
            if(point==3)check(Thread.currentThread().isInterrupted()&&target.rows==3,"interruption halts before following row");
            Thread.interrupted();check(target.aborts==1&&!owned.available(),"every begun failure consumes and aborts");
            check(Arrays.stream(retained).allMatch(v->v==0),"owned pixels wiped on Error/interruption");
            if(target.borrowed!=null)check(Arrays.stream(target.borrowed).allMatch(v->v==0),"borrowed row wiped on Error/interruption");
        }
        AnalysisInput.Owned closed=input(2);int[] bytes=retained(closed);closed.close();closed.close();check(Arrays.stream(bytes).allMatch(v->v==0)&&!closed.available(),"idempotent close wipes retained pixels");
        AnalysisInput.Owned reentrant=input(3);Target t=new Target(){public void row(int y,int[] row)throws Exception{super.row(y,row);check(!reentrant.available(),"not available while transferring");
            try{reentrant.transfer(this);throw new AssertionError("reentrant accepted");}catch(IllegalStateException expected){checks++;}
            try{reentrant.close();throw new AssertionError("reentrant close accepted");}catch(IllegalStateException expected){checks++;}
        }};reentrant.transfer(t);check(t.rows==8&&t.completes==1&&t.aborts==0,"failed reentrancy preserves outer transfer");
    }
    public static void main(String[] args)throws Exception {fixtures(new File(args[0]));failures();System.out.println("{\"checks\":"+checks+",\"fixtures\":8,\"android_execution\":false}");}
}
