package com.hiro.ulike;

import android.graphics.Bitmap;

/** Executes the real foreground producer, with only its external peers controlled. */
public final class ResidentFinishProfile19112Test {
    static int assertions,cases;
    static void check(boolean ok,String reason){assertions++;if(!ok)throw new AssertionError(reason);}
    static void run(int initial,int preferredAfter,int planRows,boolean expectedExecution){
        GpuChain1961.preferred=initial;GpuChain1961.afterPreferred=preferredAfter;GpuChain1961.planned=planRows;
        GpuChain1961.created=GpuChain1961.closed=0;GpuChain1961.active=null;
        GpuQualification1961.proofRows=initial;GpuQualification1961.restores=GpuQualification1961.writes=0;
        QualityPipeline1932.calls=QualityPipeline1932.executedRows=0;ProcessingTiming1947.route=0;PipelineDiagnostics1982.exitReason=0;
        Bitmap source=new Bitmap(5,7);QualityPixels1932.Plan plan=new QualityPixels1932.Plan();
        Bitmap result=GpuResident1976.finish1978(source,plan,4,true,new int[]{1},0,5,7,plan,true,new ProcessingTiming1947.Trace());
        check(GpuQualification1961.restores==1,"one whole proof restored");
        check(GpuQualification1961.restoredKey.contains(";resident-finish="+initial+":"),"proof key retains initial profile");
        check(GpuChain1961.created==1&&GpuChain1961.closed==1&&GpuChain1961.active==null,"preflight closes exactly once");
        check(!source.isRecycled()&&source.pixels[0]==0xff123456,"immutable source remains available for fallback");
        check((result!=null)==expectedExecution,"foreground result reflects profile isolation");
        check(QualityPipeline1932.calls==(expectedExecution?1:0),"only admitted profile enters pixel producer");
        check(QualityPipeline1932.executedRows==(expectedExecution?planRows:0),"actual executed rows observed");
        check(PipelineDiagnostics1982.exitReason==(expectedExecution?1:13),"terminal result distinguishes fallback");
        check(ProcessingTiming1947.route==(expectedExecution?4:0)&&GpuQualification1961.writes==0,"no new qualification or false adopted route");
        if(result!=null)result.recycle();source.recycle();cases++;
    }
    public static void main(String[] args){
        boolean faulty=args.length==1&&"faulty".equals(args[0]);
        if(faulty)run(64,128,128,true); // Removing only the new guard must reproduce the reviewed bug.
        else {
            run(64,64,64,true);run(128,128,128,true);
            run(64,128,128,false); // independent wide proof arrives during preflight
            run(64,64,128,false); // frozen preflight differs even if preference changes back
            run(128,64,64,false); // wide proof disappears during preflight
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"faulty_guard_removed\":"+faulty+"}");
    }
}
