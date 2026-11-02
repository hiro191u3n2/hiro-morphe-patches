package com.hiro.ulike;
import android.content.Context;
import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.*;
/** Current Finish route with explicit transport peers; no device timing claim. */
public final class FinishPressure1997Test {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Object field(Class<?> type,Object owner,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    static void reset(){GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;SaveQueue1935.idle=false;FinishStageControl1988.reset(0,0);FinishMemoryControl1989.reset(0,0);ProcessingTiming1947.copies1997=ProcessingTiming1947.skipped1997=0;ProcessingTiming1947.memory1997=null;}
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        Context context=new Context();GpuQualification1961.initialize(context);GpuCertificates1990.initialize(context);
        check(GpuCertificates1990.passed(GpuFinishDefault1990.CPU_SUITE1992,1,1,100,100),"existing standard corpus certificate retained");
        check(GpuCertificates1990.passed(GpuFinishDefault1990.wideSuite1993(),1,1,100,100),"existing 128-row certificate retained");
        ThreadLocal<Boolean> scope=(ThreadLocal<Boolean>)field(GpuChain1961.class,null,"FINISH_PRESSURE1997");
        for(int mode:new int[]{2,7,0}) {
            reset();FinishMemoryControl1989.reset(mode,0);
            Bitmap source=FinishMemory1992Test.image(80,80),expected=FinishMemory1992Test.image(37,67),result=null;
            int[] input=source.snapshot();
            try {
                result=GpuChain1961.finish(source,0,37,67,FinishMemory1992Test.plan(),false,new ProcessingTiming1947.Trace());
                check(result!=null&&Arrays.equals(expected.snapshot(),result.snapshot()),"same complete output after foreground budget decision");
                check(Arrays.equals(input,source.snapshot()),"original fallback source stays unchanged");
                check(scope.get()==null,"per-call pressure removed after ordinary completion");
                if(mode!=0) {
                    check(FinishMemoryControl1989.trims==1&&FinishMemoryControl1989.checks==(mode==2?2:3),"at most one relief per refused admission; narrow fallback keeps pressure marker");
                    check(FinishStageControl1988.submitted==(mode==2?1:3),"recovery preserves 128 rows or safely uses 32 only after wide retry refused");
                    check(FinishStageControl1988.opens==1,"recovered wide graph avoids duplicate narrow processing");
                    check(ProcessingTiming1947.memory1997!=null&&ProcessingTiming1947.memory1997[6]==0&&ProcessingTiming1947.memory1997[4]==(mode==2?1:0),"foreground memory result reaches dedicated observer");
                    check(ProcessingTiming1947.skipped1997==1&&ProcessingTiming1947.copies1997==0&&GpuQualification1961.retainedBytes()==0,"pressure skips optional full-image copy and queue reservation");
                }else {
                    check(FinishMemoryControl1989.trims==0&&ProcessingTiming1947.memory1997==null,"ordinary admitted photo performs no optional trim/report");
                    check(ProcessingTiming1947.skipped1997==0&&ProcessingTiming1947.copies1997==1,"ordinary photograph qualification copy preserved in own log category");
                    check(GpuQualification1961.retainedBytes()==4L*80*80,"bounded owned snapshot remains eligible for post-save proof");
                    ArrayDeque<?> jobs=(ArrayDeque<?>)field(GpuQualification1961.class,null,"QUEUED");Object job=jobs.peekFirst(),probe=field(job.getClass(),job,"probe");
                    Bitmap owned=(Bitmap)field(probe.getClass(),probe,"val$owned");source.recycle();check(!owned.isRecycled(),"photo recycle cannot invalidate queued owned snapshot");
                    GpuQualification1961.captureChanged();check(owned.isRecycled()&&GpuQualification1961.retainedBytes()==0,"cancel closes normal snapshot and releases budget");
                }
            }finally {if(result!=null)result.recycle();if(!source.isRecycled())source.recycle();expected.recycle();GpuQualification1961.captureChanged();}
        }
        reset();scope.set(Boolean.TRUE);
        try {check(GpuChain1961.finish(null,0,37,67,FinishMemory1992Test.plan(),false,null)==null,"ineligible nested call returns original result");check(scope.get()==Boolean.TRUE,"outer pressure scope restored for nested call");}
        finally{scope.remove();}
        reset();Bitmap source=FinishMemory1992Test.image(80,80);
        try {
            FinishStageControl1988.reset(20,4);boolean cancelled=false;
            try{GpuChain1961.finish(source,0,37,67,FinishMemory1992Test.plan(),false,null);}catch(java.util.concurrent.CancellationException expected){cancelled=true;}
            check(cancelled&&scope.get()==null,"exceptional exit removes pressure scope");
        }finally{source.recycle();GpuQualification1961.captureChanged();}
        System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"pressure_skips_optional_finish_copy\":true,\"ordinary_photo_qualification_preserved\":true,\"same_finish_pixels_verified\":true,\"native_transport_mocked\":true}");
    }
}
