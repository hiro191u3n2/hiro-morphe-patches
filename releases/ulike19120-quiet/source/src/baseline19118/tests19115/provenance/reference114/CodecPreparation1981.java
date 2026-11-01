package com.hiro.ulike;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CancellationException;

/** One owned codec constructor. Hardware notification, encoding and terminal
 * cleanup may race; exactly one of dispatch or cancellation owns the task. */
public final class CodecPreparation1981 {
    private static final ThreadLocal<CodecPreparation1981> STORAGE=new ThreadLocal<CodecPreparation1981>();
    private final CountDownLatch complete=new CountDownLatch(1);
    private final CountDownLatch codecReady=new CountDownLatch(1);
    private final boolean storage;
    private ExecutorService executor;
    private Runnable task;
    private boolean started,closed,codecGranted;
    private volatile Throwable failure;

    CodecPreparation1981(ExecutorService executor,Runnable task) {
        this(executor,task,false);
    }
    CodecPreparation1981(ExecutorService executor,Runnable task,boolean storage) {
        if(executor==null||task==null)throw new NullPointerException("codec preparation");
        this.executor=executor;this.task=task;this.storage=storage;
    }
    void start() {
        if(storage) {
            synchronized(this){if(closed)return;codecGranted=true;codecReady.countDown();}
            return;
        }
        dispatch();
    }
    void startStorage1981(){if(!storage)throw new IllegalStateException("storage preparation mode");dispatch();}
    private void dispatch() {
        final ExecutorService destination;
        final Runnable owned;
        synchronized(this) {
            if(started||closed)return;
            started=true;destination=executor;owned=task;
        }
        try {
            destination.execute(new Runnable(){public void run(){
                CodecPreparation1981 previous=null;boolean bound=false;
                try {
                    if(storage){previous=STORAGE.get();STORAGE.set(CodecPreparation1981.this);bound=true;}
                    owned.run();
                }
                catch(RuntimeException error){failure=error;throw error;}
                catch(Error error){failure=error;throw error;}
                finally {
                    try{if(bound){if(previous==null)STORAGE.remove();else STORAGE.set(previous);}}
                    finally {finished();}
                }
            }});
        } catch(RuntimeException error) {failure=error;finished();}
          catch(Error error) {failure=error;finished();}
    }
    private void finished() {
        synchronized(this){executor=null;task=null;}
        complete.countDown();
    }
    boolean idle(){synchronized(this){return (closed||started)&&complete.getCount()==0;}}
    void throwIfFailed() {
        Throwable error=failure;
        if(error instanceof RuntimeException)throw (RuntimeException)error;
        if(error instanceof Error)throw (Error)error;
    }
    void closeAndAwait() {
        synchronized(this) {
            closed=true;
            if(storage)codecReady.countDown();
            if(!started){executor=null;task=null;complete.countDown();}
        }
        boolean interrupted=false;
        for(;;)try{complete.await();break;}catch(InterruptedException wait){interrupted=true;}
        if(interrupted)Thread.currentThread().interrupt();
    }
    /** Inserted immediately before the original HeifWriter.Builder allocation.
     * Pending row/FD creation can precede this fence; real codec ownership cannot.
     * No binding means a standalone editor or the original non-async route. */
    public static void awaitCodec1981() {
        CodecPreparation1981 preparation=STORAGE.get();
        if(preparation==null)return;
        boolean interrupted=false;
        try {
            for(;;)try{preparation.codecReady.await();break;}catch(InterruptedException wait){interrupted=true;}
            synchronized(preparation) {
                if(preparation.closed||!preparation.codecGranted)throw new CancellationException("codec preparation abandoned");
            }
        } finally {if(interrupted)Thread.currentThread().interrupt();}
    }
}
