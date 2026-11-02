package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.*;

/** Actual resident admission and snapshot-budget code with controlled transport. */
public final class ResidentDiagnostics1982Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void reset()throws Exception {
        GpuQualification1961.drop();GpuQualification1961.records.clear();GpuQualification1961.negatives.clear();
        GpuQualification1961.background=GpuQualification1961.cancel=GpuQualification1961.decline=false;
        GpuQualification1961.queueCalls=GpuQualification1961.declineAt=GpuQualification1961.changeEpochAt=0;
        GpuNoise1960.busy=false;GpuNoise1960.fits=true;GpuNoise1960.unsupported=GpuNoise1960.supportThrows=0;GpuNoise1960.capabilities.clear();
        GpuChain1961.baseline="legacy:100:50";GpuChain1961.preflightFailure=false;GpuChain1961.closes=0;
        QualityPipeline1932.crash=QualityPipeline1932.unavailable=false;QualityPipeline1932.fault1982=0;
        ProcessingTiming1947.reason=-1;ProcessingTiming1947.retained=-1;ProcessingTiming1947.terminalCalls=0;ProcessingTiming1947.sinkFailure=false;
        Bitmap.copyFault=0;Bitmap.afterCopy=null;ProcessingTiming1947.epoch++;Thread.interrupted();
        for(Field f:GpuSnapshotBudget1981.class.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&!Modifier.isFinal(f.getModifiers())){
            f.setAccessible(true);if(f.getType()==boolean.class)f.setBoolean(null,false);else if(f.getType()==int.class)f.setInt(null,0);else if(f.getType()==long.class)f.setLong(null,f.getName().equals("epoch")?Long.MIN_VALUE:0);
        }
    }
    static QualityPixels1932.Plan plan(){return new QualityPixels1932.Plan();}
    static Bitmap image(){return new Bitmap(65,131);}
    static Bitmap invoke(Bitmap b){return GpuResident1976.finish1978(b,plan(),4,true,new int[]{7,8},0,91,183,plan(),false,new ProcessingTiming1947.Trace());}
    static String key(Bitmap b)throws Exception {
        Method m=GpuResident1976.class.getDeclaredMethod("key",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);
        m.setAccessible(true);return (String)m.invoke(null,b,plan(),4,true,0,91,183,plan(),false)+":"+GpuChain1961.baseline;
    }
    static void proof(Bitmap b)throws Exception {GpuQualification1961.records.put(key(b),new GpuQualification1961.Record(1000000000L,1));}
    static void terminal(int reason,String why){check(ProcessingTiming1947.reason==reason,why+" reason "+ProcessingTiming1947.reason);check(ProcessingTiming1947.terminalCalls==1,why+" exactly one terminal note");}
    static void pristine(Bitmap b,int first){check(!b.isRecycled()&&b.pixels[0]==first,"caller image is unchanged");}
    static void ordinary()throws Exception {
        reset();Bitmap b=image();check(invoke(b)==null,"unknown foreground stays ordinary");terminal(2,"queued proof");
        check(ProcessingTiming1947.retained>0&&GpuQualification1961.queued!=null,"real snapshot retention reported");
        check(GpuNoise1960.capabilities.equals(Arrays.asList(1,2,3)),"capabilities queried once in original order");GpuQualification1961.drop();
        reset();b=image();GpuChain1961.baseline=null;invoke(b);terminal(2,"missing dependency queued");check(GpuQualification1961.key.endsWith(":finish-dependency"),"dependency key preserved");GpuQualification1961.drop();
        reset();b=image();GpuNoise1960.busy=true;invoke(b);terminal(8,"busy session");check(GpuNoise1960.capabilities.isEmpty(),"busy short circuit unchanged");
        reset();b=image();GpuQualification1961.background=true;invoke(b);terminal(9,"background qualification");check(GpuNoise1960.capabilities.isEmpty(),"background short circuit unchanged");
        reset();b=image();GpuNoise1960.unsupported=2;invoke(b);terminal(3,"unsupported geometry");check(GpuNoise1960.capabilities.equals(Arrays.asList(1,2)),"unsupported short circuit unchanged");
        reset();invoke(null);terminal(3,"null input");reset();b=image();b.recycle();invoke(b);terminal(3,"recycled input");
        reset();b=image();GpuQualification1961.negatives.add(key(b));invoke(b);terminal(4,"old exact rejection");check(GpuQualification1961.queueCalls==0,"negative never queues");
        reset();b=image();GpuNoise1960.fits=false;invoke(b);terminal(5,"workspace unavailable");check(ProcessingTiming1947.retained>0,"attempted retained bytes remain visible");
        reset();b=image();b.slack=100*1024*1024;invoke(b);terminal(5,"retained bound");check(GpuQualification1961.queueCalls==0,"retained bound precedes queue");
    }
    static void snapshots()throws Exception {
        reset();Bitmap b=image();GpuSnapshotBudget1981.Copy held=GpuSnapshotBudget1981.tryCopy(1,GpuSnapshotBudget1981.FINISH);int before=Bitmap.copies;
        try{check(invoke(b)==null,"copy contention remains ordinary");terminal(10,"copy token contention");check(Bitmap.copies==before&&ProcessingTiming1947.retained>0,"copy was not taken but estimate retained");}finally{held.close();}
        reset();b=image();GpuQualification1961.declineAt=1;before=Bitmap.copies;invoke(b);terminal(6,"queue/retry declined");check(Bitmap.copies==before,"queue refusal precedes clone");
        reset();b=image();GpuQualification1961.changeEpochAt=1;before=Bitmap.copies;invoke(b);terminal(11,"epoch changed before begin");check(Bitmap.copies==before,"stale copy not started");
        for(int fault:new int[]{1,2,3}){reset();b=image();Bitmap.copyFault=fault;invoke(b);terminal(fault==3?11:12,"copy fault "+fault);pristine(b,0xff123456);}
        reset();b=image();Bitmap.afterCopy=new Runnable(){public void run(){ProcessingTiming1947.epoch++;}};int recycled=Bitmap.recycles;invoke(b);terminal(11,"epoch changed after clone");check(Bitmap.recycles==recycled+1,"stale clone released");
        reset();b=image();GpuQualification1961.declineAt=2;recycled=Bitmap.recycles;invoke(b);terminal(6,"queue changed after copy");check(Bitmap.recycles==recycled+1,"declined clone released");
        reset();b=image();GpuQualification1961.decline=true;recycled=Bitmap.recycles;invoke(b);terminal(6,"schedule refused");check(Bitmap.recycles==recycled+1,"schedule owns declined snapshot");
    }
    static void admitted()throws Exception {
        reset();Bitmap b=image();proof(b);Bitmap result=invoke(b);check(result!=null,"qualified GPU result remains selected");terminal(1,"adopted");check(ProcessingTiming1947.route==4&&GpuChain1961.closes==1,"route and lease close");pristine(b,0xff123456);result.recycle();
        reset();b=image();proof(b);GpuChain1961.preflightFailure=true;invoke(b);terminal(5,"resident capacity preflight");
        reset();b=image();proof(b);QualityPipeline1932.unavailable=true;invoke(b);terminal(14,"missing GPU result");pristine(b,0xff123456);
        reset();b=image();proof(b);QualityPipeline1932.crash=true;invoke(b);terminal(13,"GPU runtime failure");
        for(int fault=2;fault<=5;fault++){
            reset();b=image();proof(b);QualityPipeline1932.fault1982=fault;boolean threw=false;
            try{invoke(b);}catch(java.util.concurrent.CancellationException expected){check(fault==4,"only cancellation propagated here");threw=true;}catch(AssertionError expected){check(fault==5,"fatal error identity retained");threw=true;}
            terminal(fault==4?11:13,"GPU fault "+fault);check(threw==(fault>=4),"original exception behavior preserved");check(GpuChain1961.closes==1,"failed admitted lease closed");pristine(b,0xff123456);
        }
        reset();b=image();GpuNoise1960.supportThrows=1;boolean threw=false;try{invoke(b);}catch(IllegalStateException expected){threw=true;}check(threw,"capability error propagation preserved");terminal(13,"early capability exception");
        reset();b=image();proof(b);ProcessingTiming1947.sinkFailure=true;result=invoke(b);check(result!=null,"optional diagnostic sink cannot change selected image");result.recycle();
    }
    static void baseline()throws Exception {
        reset();Bitmap b=image();GpuSnapshotBudget1981.Copy held=GpuSnapshotBudget1981.tryCopy(1,GpuSnapshotBudget1981.FINISH);
        try{check(invoke(b)==null,"baseline copy declined");check(ProcessingTiming1947.reason==0&&ProcessingTiming1947.retained==0,".81 copy refusal leaves initial status and zero estimate");}finally{held.close();}
        reset();b=image();proof(b);QualityPipeline1932.unavailable=true;check(invoke(b)==null,"baseline result missing");check(ProcessingTiming1947.reason==0,".81 admitted failure leaves initial status");
    }
    public static void main(String[] args)throws Exception {
        if(args.length>0&&args[0].equals("baseline"))baseline();else{ordinary();snapshots();admitted();}
        GpuQualification1961.drop();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+"}");
    }
}
