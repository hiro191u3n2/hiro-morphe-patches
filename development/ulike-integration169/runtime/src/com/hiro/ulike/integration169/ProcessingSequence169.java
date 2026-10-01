package com.hiro.ulike.integration169;

import java.io.InterruptedIOException;

/** Bounded one-shot orchestration. It supplies no native data and does not enable capture. */
public final class ProcessingSequence169 {
    public interface Work extends AutoCloseable {
        void validate() throws Exception;
        void analyse() throws Exception;
        void bind() throws Exception;
        void render() throws Exception;
        /** Return only after the photograph is committed, never a pending URI. */
        String publish() throws Exception;
        @Override void close() throws Exception;
    }
    public static final class Result {
        public final String committedPhoto;
        /** A post-commit cleanup failure must not be reported as an unsaved photo. */
        public final Exception cleanupFailure;
        Result(String uri,Exception cleanup){committedPhoto=uri;cleanupFailure=cleanup;}
    }
    private Object activeIdentity;
    private Thread activeThread;
    private boolean cancelled,committed,closing;
    /** Cancels only this exact accepted shot. No global-latest or next-shot cancellation. */
    public synchronized boolean cancel(Object identity){
        if(identity==null || identity!=activeIdentity || committed || closing)return false;
        cancelled=true;activeThread.interrupt();return true;
    }
    private synchronized void check()throws InterruptedIOException{
        if(cancelled || Thread.currentThread().isInterrupted())throw new InterruptedIOException("Captured photo processing cancelled");
    }
    /** Always attempts every owned cleanup, including after linkage/runtime Errors. */
    static void closeOwned(AutoCloseable... owners)throws Exception{
        Throwable first=null;
        for(AutoCloseable owner:owners)if(owner!=null)try{owner.close();}
        catch(Exception|Error failure){if(first==null)first=failure;else if(first!=failure)first.addSuppressed(failure);}
        if(first instanceof Exception)throw (Exception)first;if(first instanceof Error)throw (Error)first;
    }
    public Result execute(Object identity,Work work)throws Exception {
        if(identity==null || work==null)throw new NullPointerException();
        synchronized(this){
            if(activeIdentity!=null)throw new IllegalStateException("Another owned photo is processing");
            activeIdentity=identity;activeThread=Thread.currentThread();cancelled=false;committed=false;closing=false;
        }
        Throwable primary=null;String uri=null;Exception cleanup=null;
        try{
            check();work.validate();check();work.analyse();check();work.bind();check();work.render();check();
            uri=work.publish();
            if(uri==null || uri.isEmpty())throw new IllegalStateException("No committed photograph returned");
            // Publication owns its own commit linearization. Cancellation after a successful
            // return cannot relabel that committed photograph as cancelled/unsaved.
            synchronized(this){committed=true;}
        }catch(Exception|Error failure){primary=failure;throw failure;}
        finally{
            synchronized(this){closing=true;}
            boolean interrupted=Thread.interrupted();
            try{work.close();}
            catch(Exception failure){if(primary!=null){if(primary!=failure)primary.addSuppressed(failure);}else if(uri!=null && !uri.isEmpty())cleanup=failure;else throw failure;}
            catch(Error failure){if(primary!=null){if(primary!=failure)primary.addSuppressed(failure);}else throw failure;}
            finally{synchronized(this){activeIdentity=null;activeThread=null;cancelled=false;committed=false;closing=false;}if(interrupted)Thread.currentThread().interrupt();}
        }
        return new Result(uri,cleanup);
    }
}
