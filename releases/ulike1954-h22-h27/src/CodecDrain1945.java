package com.hiro.ulike;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;
import java.io.IOException;
import androidx.heifwriter.HeifWriter;
import android.media.MediaCodec;

/** One dedicated callback looper replaces the writer's otherwise unbounded
 * per-photo HandlerThreads. Muxer drain and codec callbacks stay untouched. */
public final class CodecDrain1945 {
    private CodecDrain1945() {}
    private static Handler handler;
    private static HandlerThread thread;
    private static volatile boolean failed;
    private static final Semaphore codec=new Semaphore(1,true);
    private static HeifWriter leasedWriter;
    private static MediaCodec leasedCodec;
    private static HeifWriter closingWriter;
    private static final ThreadLocal<BuildLease> constructing=new ThreadLocal<BuildLease>();
    private static final class BuildLease {MediaCodec codec;boolean releaseFailed;}
    public static synchronized Handler handler(){
        if(failed)throw new IllegalStateException("HEIF callback looper unavailable");
        if(thread==null){
            HandlerThread created=new HandlerThread("ULike-heif-drain1945",-2);
            created.start();
            thread=created;
        }
        if(handler==null){
            Looper looper=thread.getLooper();
            if(looper==null){failed=true;throw new IllegalStateException("HEIF callback looper missing");}
            handler=new Handler(looper);
        }
        if(!thread.isAlive()){failed=true;throw new IllegalStateException("HEIF callback looper stopped");}
        return handler;
    }
    /** All reviewed camera and editing builders share this final hardware
     * permit, including callers which do not have an AsyncSave Job. */
    public static HeifWriter build(HeifWriter.Builder builder)throws IOException {
        boolean interrupted=false,transferred=false;
        for(;;)try{codec.acquire();break;}catch(InterruptedException wait){interrupted=true;}
        BuildLease lease=null;
        try{
            builder.setHandler(handler());
            lease=new BuildLease();constructing.set(lease);
            HeifWriter made=builder.build();
            synchronized(CodecDrain1945.class){
                if(leasedWriter!=null)throw new IllegalStateException("HEIF codec ownership");
                leasedWriter=made;leasedCodec=lease.codec;
            }
            transferred=true;return made;
        }catch(IOException failure){cleanupBuild(lease);throw failure;}
        catch(RuntimeException failure){cleanupBuild(lease);throw failure;}
        catch(Error failure){cleanupBuild(lease);throw failure;}
        finally{constructing.remove();if(!transferred)codec.release();if(interrupted)Thread.currentThread().interrupt();}
    }
    /** The pinned AndroidX constructor can fall back after capability lookup
     * fails. Dispose the first returned codec before trying another factory. */
    private static BuildLease beforeFactory(){
        BuildLease lease=constructing.get();
        if(lease==null)return null;
        if(lease.releaseFailed||failed)throw new IllegalStateException("HEIF constructor cleanup failed");
        if(lease.codec!=null)releaseCodec(lease.codec);
        return lease;
    }
    public static MediaCodec createByCodecName(String name)throws IOException {
        BuildLease lease=beforeFactory();MediaCodec made=MediaCodec.createByCodecName(name);
        if(lease!=null)lease.codec=made;return made;
    }
    public static MediaCodec createEncoderByType(String type)throws IOException {
        BuildLease lease=beforeFactory();MediaCodec made=MediaCodec.createEncoderByType(type);
        if(lease!=null)lease.codec=made;return made;
    }
    public static void releaseCodec(MediaCodec made){
        BuildLease lease=constructing.get();
        try{made.release();if(lease!=null&&lease.codec==made)lease.codec=null;synchronized(CodecDrain1945.class){if(leasedCodec==made)leasedCodec=null;}}
        catch(RuntimeException failure){releaseFailure(lease,made);throw failure;}
        catch(Error failure){releaseFailure(lease,made);throw failure;}
    }
    private static void releaseFailure(BuildLease lease,MediaCodec made){
        synchronized(CodecDrain1945.class){if(lease!=null||leasedCodec==made){if(lease!=null)lease.releaseFailed=true;failed=true;}}
    }
    /** AndroidX catches stop failures and otherwise skips release. Always
     * attempt release before its unchanged exception handling continues. */
    public static void stopAndRelease(MediaCodec made){try{made.stop();}finally{releaseCodec(made);}}
    private static void cleanupBuild(BuildLease lease){
        if(lease!=null&&lease.codec!=null){
            try{releaseCodec(lease.codec);}
            catch(RuntimeException failure){failed=true;}
            catch(Error failure){failed=true;}
        }
        try{awaitClosed();}
        catch(RuntimeException failure){failed=true;}
        catch(Error failure){failed=true;}
    }
    /** Release ownership only once the actual encoder close has run. A close
     * failure poisons further builders so an unproven allocation is not reused. */
    public static void close(HeifWriter writer){
        if(writer==null)throw new NullPointerException("HEIF writer");
        final boolean owned;
        boolean interrupted=false;
        synchronized(CodecDrain1945.class){
            if(closingWriter==writer){
                while(closingWriter==writer)try{CodecDrain1945.class.wait();}catch(InterruptedException wait){interrupted=true;}
                if(interrupted)Thread.currentThread().interrupt();return;
            }
            owned=leasedWriter==writer;
            if(owned)closingWriter=writer;
        }
        try{
            writer.close();
            if(owned){
                awaitClosed();
                MediaCodec remaining;synchronized(CodecDrain1945.class){remaining=leasedWriter==writer?leasedCodec:null;}
                // Covers a failed writer/muxer cleanup before encoder.close.
                if(remaining!=null){releaseCodec(remaining);awaitClosed();}
            }
        }
        catch(RuntimeException failure){if(owned)failed=true;throw failure;}
        catch(Error failure){if(owned)failed=true;throw failure;}
        finally{if(owned)synchronized(CodecDrain1945.class){
            if(leasedWriter==writer){leasedWriter=null;codec.release();}
            if(closingWriter==writer){closingWriter=null;CodecDrain1945.class.notifyAll();}
        }}
    }
    /** Writer.close posts front-of-queue cleanup, which posts the codec close
     * at the front again. A normal-queue barrier observes both cleanup levels
     * and the current output callback (including buffer release) completed. */
    public static void awaitClosed(){
        final Handler active;
        synchronized(CodecDrain1945.class){active=handler;}
        if(active==null)return;
        if(failed)throw new IllegalStateException("HEIF callback looper failed");
        if(Looper.myLooper()==active.getLooper())throw new IllegalStateException("HEIF close wait on callback thread");
        final CountDownLatch done=new CountDownLatch(1);
        if(!active.post(new Runnable(){public void run(){done.countDown();}})){
            failed=true;throw new IllegalStateException("HEIF close barrier rejected");
        }
        boolean interrupted=false,complete=false;
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);
        try{
            while(!complete){
                long remaining=deadline-System.nanoTime();
                if(remaining<=0)break;
                try{complete=done.await(remaining,TimeUnit.NANOSECONDS);}
                catch(InterruptedException wait){interrupted=true;}
            }
        }finally{if(interrupted)Thread.currentThread().interrupt();}
        if(!complete){failed=true;throw new IllegalStateException("HEIF close barrier timed out");}
    }
}
