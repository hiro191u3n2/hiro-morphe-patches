package com.hiro.ulike.hdr.stillanalysis;
import android.content.Context;
import com.ss.android.vesdk.VERecorder;
import java.io.IOException;
import java.util.Arrays;

/** Runs the production pause/restore method against explicit host lifecycle fixtures. */
public final class PausedOutputOwnershipTest {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static final class Result implements AutoCloseable {
        final int[] diagnostic=new int[4080*3060];
        final Throwable closeFailure;int closed;
        Result(Throwable failure){closeFailure=failure;Arrays.fill(diagnostic,0xff19a370);}
        public void close()throws Exception{
            closed++;Arrays.fill(diagnostic,0);
            if(closeFailure instanceof Exception)throw (Exception)closeFailure;
            if(closeFailure instanceof Error)throw (Error)closeFailure;
        }
    }
    public static void main(String[] args)throws Exception {
        String scenario=args[0];
        RecorderAdmission.installed(RecorderAdmission.HOOK_CONTRACT);
        VERecorder recorder=new VERecorder();
        Throwable restoreFailure=scenario.equals("restore-error")?new AssertionError("restore"):
            scenario.equals("restore-interrupt")?new InterruptedException("restore"):
            scenario.startsWith("restore-")?new IOException("restore"):null;
        Throwable closeFailure=scenario.equals("restore-close-error")?new AssertionError("close"):
            scenario.equals("restore-close-exception")?new IOException("close"):
            scenario.equals("restore-same-failure")?restoreFailure:null;
        Result output=new Result(closeFailure);
        Throwable workFailure=scenario.equals("work-error")?new AssertionError("work"):
            scenario.equals("work-exception")?new IOException("work"):null;
        final int[] restored={0};Object delivered=null;Throwable seen=null;
        try{
            delivered=PausedStockPreview.run(new Context(),recorder,recorder.b,100,
                (original,timeout)->{
                    check(original==recorder && timeout==100,"restore exact original recorder");restored[0]++;
                    if(restoreFailure instanceof Exception)throw (Exception)restoreFailure;
                    if(restoreFailure instanceof Error)throw (Error)restoreFailure;
                },lease->{
                    lease.requireInitializedAndNoOtherRecorder();
                    if(workFailure instanceof Exception)throw (Exception)workFailure;
                    if(workFailure instanceof Error)throw (Error)workFailure;
                    return output;
                });
        }catch(Exception|Error failure){seen=failure;}
        check(recorder.stops==1 && recorder.starts==1 && restored[0]==1,"one full pause/restore sequence");
        if(restoreFailure!=null){
            check(delivered==null && seen==restoreFailure,"restore failure preserved, output never returned");
            check(output.closed==1,"undelivered native-sized output closed exactly once");
            for(int pixel:output.diagnostic)check(pixel==0,"undelivered pixels wiped");
            check(seen.getSuppressed().length==(closeFailure!=null && closeFailure!=restoreFailure?1:0),"suppressed cleanup failure count");
            if(closeFailure!=null && closeFailure!=restoreFailure)check(seen.getSuppressed()[0]==closeFailure,"original cleanup failure identity");
            try{RecorderAdmission.beforeApplicationLifecycle(recorder,"anything");throw new AssertionError("not quarantined");}
            catch(IllegalStateException expected){checks++;}
            check(Thread.currentThread().isInterrupted()==scenario.equals("restore-interrupt"),"interrupt state preserved");
        }else if(workFailure!=null){
            check(seen==workFailure && delivered==null,"work Error/Exception identity preserved");
            check(output.closed==0,"never acquired output is not closed");
            RecorderAdmission.beforeApplicationLifecycle(recorder,"anything");checks++;
            output.close();
        }else{
            check(seen==null && delivered==output && output.closed==0,"successful output transferred to caller");
            check(output.diagnostic[0]==0xff19a370,"successful pixels retained");
            RecorderAdmission.beforeApplicationLifecycle(recorder,"anything");checks++;
            output.close();check(output.closed==1 && output.diagnostic[0]==0,"caller releases successful result");
        }
        Thread.interrupted();
        System.out.println("{\"scenario\":\""+scenario+"\",\"checks\":"+checks+",\"result_pixels\":"+output.diagnostic.length+",\"device_execution\":false}");
    }
}
