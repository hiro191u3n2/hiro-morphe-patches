package com.hiro.ulike.hdr.stillanalysis;

import java.util.ArrayDeque;

/** Per-call bookkeeping for the three exact RecordInvoker allocation/teardown
 * wrappers. It does not install or enable app-wide coverage. Entry rejection is
 * outside the original method catch region. Hook callers must match every exit.
 */
public final class NativeLifetimeBoundary {
    private static final ThreadLocal<ArrayDeque<Call>> calls=new ThreadLocal<>();
    private NativeLifetimeBoundary() {}
    public static void beforeInit(Object invoker) {
        begin(invoker,true);
    }
    public static void beforeUninit(Object invoker) {
        begin(invoker,false);
    }
    private static void begin(Object invoker,boolean init) {
        ArrayDeque<Call> stack=calls.get();
        if(stack==null){stack=new ArrayDeque<>();calls.set(stack);}
        RecorderAdmission.Ticket ticket=init?RecorderAdmission.beforeNativeInit(invoker):RecorderAdmission.beforeNativeUninit(invoker);
        try{stack.push(new Call(invoker,ticket,init));}
        catch(RuntimeException | Error e){RecorderAdmission.failed(ticket);if(stack.isEmpty())calls.remove();throw e;}
    }
    public static void returned(int status) {
        ArrayDeque<Call> stack=calls.get();Call c=stack==null?null:stack.peek();
        if(c==null)throw new IllegalStateException("Native return without admission entry");
        try {
            if(c.init) {
                long handle=(Long)c.invoker.getClass().getMethod("getHandler").invoke(c.invoker);
                RecorderAdmission.nativeInitialized(c.ticket,handle,status);
            } else RecorderAdmission.nativeUninitialized(c.ticket,status);
        } catch(RuntimeException | Error e) {RecorderAdmission.failed(c.ticket);throw e;}
        catch(ReflectiveOperationException e){RecorderAdmission.failed(c.ticket);throw new IllegalStateException("Cannot read original native handle",e);}
        finally {stack.pop();if(stack.isEmpty())calls.remove();}
    }
    /** Original throwable is rethrown by the injected DEX handler. Never replace
     * it, including if a completion hook itself already quarantined the ledger.
     */
    public static void failed(Object invoker) {
        // Quarantine first even for a missing/mismatched thread-local completion.
        // Do not consume an unrelated outer call's ticket.
        RecorderAdmission.failed(null);
        ArrayDeque<Call> stack=calls.get();if(stack==null)return;Call c=stack.peek();
        if(c!=null && c.invoker==invoker){stack.pop();RecorderAdmission.failed(c.ticket);}
        if(stack.isEmpty())calls.remove();
    }
    private static final class Call {
        final Object invoker;final RecorderAdmission.Ticket ticket;final boolean init;
        Call(Object invoker,RecorderAdmission.Ticket ticket,boolean init){this.invoker=invoker;this.ticket=ticket;this.init=init;}
    }
}
