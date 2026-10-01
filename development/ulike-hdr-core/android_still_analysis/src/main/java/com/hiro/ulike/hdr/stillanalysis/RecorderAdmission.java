package com.hiro.ulike.hdr.stillanalysis;

import java.util.IdentityHashMap;
import java.util.Map;

/** App-wide hook ledger. Useful ONLY when the patcher has installed every hook
 * in HOOK_CONTRACT before the app constructs any recorder. The local monitor is
 * not itself proof of coverage. Failed/unknown native teardown quarantines it.
 */
public final class RecorderAdmission {
    public static final String HOOK_CONTRACT="ulike-v562-recorder-admission-1";
    private static final Map<Object,Boolean> recorders=new IdentityHashMap<>();
    private static final Map<Object,Boolean> natives=new IdentityHashMap<>();
    private static final Map<Ticket,Boolean> pending=new IdentityHashMap<>();
    private static boolean installed,broken;
    private static Session session;
    private RecorderAdmission() {}
    /** Only the fully verified integration patch's startup hook calls this. */
    public static synchronized void installed(String contract) {
        if(!HOOK_CONTRACT.equals(contract) || installed || !recorders.isEmpty() || !natives.isEmpty() || !pending.isEmpty())throw new IllegalStateException("Admission coverage must be installed once before SDK construction");
        installed=true;
    }
    private static void healthy() { if(!installed || broken)throw new IllegalStateException("Recorder admission unavailable/quarantined"); }
    private static Ticket ticket(int operation,Object owner) { Ticket t=new Ticket(operation,owner);pending.put(t,Boolean.TRUE);return t; }
    private static void consume(Ticket ticket,int expected) {
        if(ticket==null || ticket.operation!=expected || pending.remove(ticket)==null) { broken=true;throw new IllegalStateException("Missing/repeated SDK operation completion"); }
    }
    public static synchronized Ticket beforeConstruction() {
        healthy();if(session!=null)throw new IllegalStateException("New VERecorder during exclusive still analysis");return ticket(1,null);
    }
    public static synchronized void constructed(Ticket ticket,Object recorder) {
        consume(ticket,1);if(recorder==null){broken=true;throw new IllegalArgumentException("Null recorder");}recorders.put(recorder,Boolean.TRUE);
    }
    public static synchronized Ticket beforeDestroy(Object recorder) {
        healthy();if(session!=null)throw new IllegalStateException("Destroy during exclusive still analysis");
        if(!recorders.containsKey(recorder))throw new IllegalStateException("Unobserved recorder");return ticket(2,recorder);
    }
    public static synchronized void destroyed(Ticket ticket) { consume(ticket,2);recorders.remove(ticket.owner); }
    public static synchronized Ticket beforeNativeInit(Object invoker) {
        healthy();if(invoker==null)throw new IllegalArgumentException("Missing invoker");
        if(session!=null) {
            if(session.phase==Phase.ANALYSIS && Thread.currentThread()==session.caller && session.privateInvoker==null && invoker!=session.originalNative)session.privateInvoker=invoker;
            boolean original=session.phase==Phase.RESTORE && invoker==session.originalNative;
            boolean own=session.phase==Phase.ANALYSIS && invoker==session.privateInvoker && Thread.currentThread()==session.caller;
            if(!original && !own)throw new IllegalStateException("Unrelated native recorder initialization");
            if(natives.containsKey(invoker))throw new IllegalStateException("Native recorder already initialized");
        }
        return ticket(3,invoker);
    }
    public static synchronized void nativeInitialized(Ticket ticket,long handle,int status) {
        consume(ticket,3);
        // A negative init can still allocate a handle; retain until real uninit.
        if(handle!=0)natives.put(ticket.owner,Boolean.TRUE);
        else if(status==0){broken=true;throw new IllegalStateException("Successful native init without handle");}
    }
    public static synchronized Ticket beforeNativeUninit(Object invoker) {
        healthy();if(!natives.containsKey(invoker))throw new IllegalStateException("Unobserved native recorder");
        if(session!=null) {
            boolean original=session.phase==Phase.STOP && invoker==session.originalNative;
            boolean own=session.phase==Phase.ANALYSIS && invoker==session.privateInvoker;
            if(!original && !own)throw new IllegalStateException("Unrelated native teardown");
        }
        return ticket(4,invoker);
    }
    public static synchronized void nativeUninitialized(Ticket ticket,int status) {
        consume(ticket,4);if(status!=0){broken=true;throw new IllegalStateException("Native uninit failed");}natives.remove(ticket.owner);
    }
    /** Hook exceptional exits; never assume a partly executed SDK call is clean. */
    public static synchronized void failed(Ticket ticket) { pending.remove(ticket);broken=true; }

    static synchronized Session acquire(Object recorder,Object originalNative) {
        healthy();if(session!=null || !pending.isEmpty() || recorders.size()!=1 || !recorders.containsKey(recorder) || natives.size()!=1 || !natives.containsKey(originalNative))throw new IllegalStateException("One fully observed original recorder required");
        session=new Session(recorder,originalNative,Thread.currentThread());return session;
    }
    static synchronized void analysis(Session s) {
        require(s);if(s.phase!=Phase.STOP || !pending.isEmpty() || !natives.isEmpty())throw new IllegalStateException("Original native recorder is not released");s.phase=Phase.ANALYSIS;
    }
    static synchronized void requireIdle(Session s) {
        require(s);if(s.phase!=Phase.ANALYSIS || !pending.isEmpty())throw new IllegalStateException("Not in exclusive analysis phase");
        if(!recorders.containsKey(s.originalRecorder) || recorders.size()!=1)throw new IllegalStateException("Recorder set changed");
        for(Object active:natives.keySet())if(active!=s.privateInvoker)throw new IllegalStateException("Another native recorder exists");
    }
    static synchronized void restore(Session s) {
        require(s);if(s.phase!=Phase.ANALYSIS || !pending.isEmpty() || !natives.isEmpty())throw new IllegalStateException("Private native recorder still active");s.phase=Phase.RESTORE;
    }
    static synchronized void complete(Session s) {
        require(s);if(s.phase!=Phase.RESTORE || !pending.isEmpty() || natives.size()!=1 || !natives.containsKey(s.originalNative))throw new IllegalStateException("Original native recorder was not restored");s.phase=Phase.DONE;session=null;
    }
    static synchronized void quarantine(Session s) { if(session==s)broken=true; }
    private static void require(Session s) { healthy();if(s==null || session!=s || Thread.currentThread()!=s.caller)throw new IllegalStateException("Exclusive lease owner mismatch"); }
    /** App/SDK start, stop, pause, resume, destroy and surface-change entries must
     * call this before side effects. Only the original caller's top-level stop
     * or restart request is admitted during the corresponding phase. Internal
     * asynchronous workers use separate native-init/uninit hooks above. */
    public static synchronized void beforeApplicationLifecycle(Object recorder,String operation) {
        healthy();if(session==null)return;
        if(recorder!=session.originalRecorder || Thread.currentThread()!=session.caller ||
                !(session.phase==Phase.STOP && operation.equals("stopPreviewAsync") || session.phase==Phase.RESTORE && operation.equals("startPreviewAsync")))
            throw new IllegalStateException("Concurrent recorder lifecycle mutation");
    }
    public static final class Ticket { private final int operation;private final Object owner;private Ticket(int op,Object owner){operation=op;this.owner=owner;} }
    enum Phase { STOP,ANALYSIS,RESTORE,DONE }
    static final class Session { final Object originalRecorder,originalNative;final Thread caller;Phase phase=Phase.STOP;Object privateInvoker;Session(Object recorder,Object invoker,Thread caller){originalRecorder=recorder;originalNative=invoker;this.caller=caller;} }
}
