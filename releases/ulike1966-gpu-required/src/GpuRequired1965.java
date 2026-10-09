package com.hiro.ulike;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;

/** Transaction boundary for required GPU stages. Every attempt owns a private
 * candidate and closes its own session before returning/throwing. The source
 * image stays immutable and owned by the caller until full-route completion.
 * Recovery never shares, resets, or terminates the camera's EGL contexts. */
public final class GpuRequired1965 {
    private GpuRequired1965(){ }
    public interface Work<T>{T run();}
    private static final int MAX_RETRIES=2,MAX_LOG_BYTES=128*1024,MAX_RING=96;
    private static final Object LOG_LOCK=new Object();
    private static final AtomicLong SEQUENCE=new AtomicLong();
    private static final ArrayDeque<String> RING=new ArrayDeque<String>();
    private static volatile File logDirectory;
    private static final ThreadLocal<Long> CURRENT_SHOT=new ThreadLocal<Long>();
    public static void initialize(Context context){
        if(context==null)return;
        try{Context app=context.getApplicationContext();if(app==null)app=context;File dir=app.getFilesDir();
            if(dir!=null){synchronized(LOG_LOCK){logDirectory=dir;}}}
        catch(RuntimeException unavailable){ }
    }
    /** Multiple stages in one saved capture share this ID when the route calls
     * beginShot/endShot. A standalone stage still gets a distinct event ID. */
    public static long beginShot(long sourceGeneration){long id=SEQUENCE.incrementAndGet();CURRENT_SHOT.set(Long.valueOf(id));note("shot","begin sourceGeneration="+sourceGeneration);return id;}
    public static void endShot(){note("shot","end");CURRENT_SHOT.remove();}
    public static <T> T run(String stage,long sourceGeneration,Work<T> work){
        if(stage==null||work==null)throw new IllegalArgumentException("GPU required stage");
        Long capture=CURRENT_SHOT.get();long shot=capture==null?SEQUENCE.incrementAndGet():capture.longValue();
        Throwable last=null;
        for(int attempt=0;attempt<=MAX_RETRIES;attempt++){
            long start=System.nanoTime();long[] before=GpuNoise1960.diagnostics1965();
            T candidate=null;
            try{
                if(Thread.currentThread().isInterrupted())throw new GpuRequiredFailure1965(stage,"capture interrupted");
                candidate=work.run();
                if(candidate==null)throw new GpuRequiredFailure1965(stage,"GPU candidate incomplete: "+GpuNoise1960.failure1965());
                event(shot,stage,sourceGeneration,attempt,"complete",start,before,GpuNoise1960.diagnostics1965(),"");
                return candidate;
            }catch(RuntimeException failure){last=failure;}
            catch(LinkageError failure){last=failure;}
            catch(OutOfMemoryError failure){last=failure;}
            long[] after=GpuNoise1960.diagnostics1965();
            event(shot,stage,sourceGeneration,attempt,"failed",start,before,after,
                (last==null?"unknown":last.getClass().getSimpleName())+" native="+GpuNoise1960.failure1965());
            if(last instanceof GpuRequiredFailure1965&&((GpuRequiredFailure1965)last).fallbackForbidden)break;
            if(attempt>=MAX_RETRIES||Thread.currentThread().isInterrupted())break;
            /* A live session, timeout, lost context with outstanding commands,
             * or unknown fence completion cannot be made safe by retrying. */
            boolean recovered=GpuNoise1960.recoverPrivate1965();
            note(stage,"shot="+shot+" retry="+(attempt+1)+" privateRecovery="+recovered);
            if(!recovered)break;
        }
        if(last instanceof GpuRequiredFailure1965&&((GpuRequiredFailure1965)last).fallbackForbidden)throw (GpuRequiredFailure1965)last;
        throw new GpuRequiredFailure1965(stage,"GPU補正に失敗しました。CPU処理や補正なしの保存は行いません。",last);
    }
    public static void note(String stage,String message){
        Long shot=CURRENT_SHOT.get();append("timeMs="+System.currentTimeMillis()+" shot="+(shot==null?0:shot.longValue())+" stage="+safe(stage,80)+" event="+safe(message,1400));
    }
    /** Platform exception messages may contain private photo paths or bitmap
     * coordinates. Persist only the type; the thrown failure retains its cause. */
    public static void fail(String stage,Throwable failure){note(stage,"failure="+(failure==null?"unknown":failure.getClass().getSimpleName()));}
    private static long value(long[] facts,int index){return facts!=null&&index<facts.length?facts[index]:0;}
    private static long delta(long[] before,long[] after,int index){return Math.max(0,value(after,index)-value(before,index));}
    private static void event(long shot,String stage,long sourceGeneration,int retry,String status,long start,long[] before,long[] after,String error){
        append("timeMs="+System.currentTimeMillis()+" shot="+shot+" stage="+safe(stage,80)+" sourceGeneration="+sourceGeneration+
            " contextGeneration="+value(after,0)+" retry="+retry+" status="+status+" elapsedNs="+(System.nanoTime()-start)+
            " uploadNs="+delta(before,after,1)+" dispatchNs="+delta(before,after,2)+" readNs="+delta(before,after,3)+
            " waitNs="+delta(before,after,4)+" submitted="+value(after,5)+" completed="+value(after,6)+
            " unknownCompletion="+value(after,7)+" disabled="+value(after,8)+" residentBytes="+value(after,9)+
            " glError="+value(after,10)+" hardwareFp64Probe="+value(after,11)+" gpuSoftwareBinary64Probe="+value(after,14)+" error="+safe(error,600)+
            " fingerprint="+safe(GpuNoise1960.fingerprint(),1200));
    }
    private static String safe(String value,int limit){
        if(value==null)return "";String s=value.replace('\n',' ').replace('\r',' ').replace('\t',' ');return s.length()>limit?s.substring(0,limit):s;
    }
    private static void append(String line){
        synchronized(LOG_LOCK){
            if(RING.size()>=MAX_RING)RING.removeFirst();RING.addLast(line);
            File directory=logDirectory;if(directory==null)return;
            File current=new File(directory,"ulike-gpu-required1965.log"),previous=new File(directory,"ulike-gpu-required1965.previous.log");
            try{byte[] bytes=(line+"\n").getBytes("UTF-8");
                if(bytes.length>MAX_LOG_BYTES)return;
                if(current.length()>MAX_LOG_BYTES-bytes.length){
                    if(previous.exists()&&!previous.delete())return;
                    if(current.exists()&&!current.renameTo(previous))return;
                }
                FileOutputStream out=new FileOutputStream(current,true);try{out.write(bytes);}finally{out.close();}
            }catch(IOException unavailable){ }
            catch(SecurityException unavailable){ }
        }
    }
    /** Include in the existing diagnostic ZIP/text export. No image values,
     * paths, face coordinates, or photo contents are ever written to this log. */
    public static String diagnosticText1965(){
        synchronized(LOG_LOCK){StringBuilder text=new StringBuilder();
            File directory=logDirectory;
            if(directory!=null){read(new File(directory,"ulike-gpu-required1965.previous.log"),text);read(new File(directory,"ulike-gpu-required1965.log"),text);}
            if(text.length()==0)for(String line:RING)text.append(line).append('\n');return text.toString();}
    }
    private static void read(File file,StringBuilder text){
        if(!file.isFile()||file.length()>MAX_LOG_BYTES)return;
        try{FileInputStream in=new FileInputStream(file);try{byte[] bytes=new byte[(int)file.length()];int at=0,count;while(at<bytes.length&&(count=in.read(bytes,at,bytes.length-at))>0)at+=count;text.append(new String(bytes,0,at,"UTF-8"));}finally{in.close();}}
        catch(IOException unavailable){ }
        catch(SecurityException unavailable){ }
    }
}
