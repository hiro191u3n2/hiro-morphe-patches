package com.hiro.ulike.composer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;

/** Actual entry/exit hook backend. This observes Java/native-boundary requests;
 * it deliberately provides no all-state-coverage or native queue receipt. */
public final class NativeComposerBoundary {
    public interface HandlerReader { long read(Object recorder)throws Exception; }
    private static final int MAX_RECORDERS=32,MAX_DEPTH=16;
    private final HandlerReader handles;
    private final IdentityHashMap<Object,ComposerJournal> journals=new IdentityHashMap<>();
    private final IdentityHashMap<Object,Long> nativeHandles=new IdentityHashMap<>();
    private final ThreadLocal<ArrayDeque<Call>> calls=new ThreadLocal<ArrayDeque<Call>>() {
        @Override protected ArrayDeque<Call> initialValue(){return new ArrayDeque<>();}
    };
    public NativeComposerBoundary(HandlerReader handles){if(handles==null)throw new NullPointerException();this.handles=handles;}
    /** Hook must execute before the pinned terminal native allocation wrapper. */
    public synchronized void beforeInit(Object recorder,int width,int height,String workspace,int one,int two,String resource,int three,boolean a,boolean b,boolean c) {
        if(recorder==null)throw new NullPointerException();
        if(journals.containsKey(recorder)){journals.get(recorder).unrecordedMutation("repeated init");push(new Call(recorder,journals.get(recorder),null,2));return;}
        if(journals.size()>=MAX_RECORDERS)throw new IllegalStateException("observation recorder budget exhausted");
        ComposerJournal j=new ComposerJournal(recorder);journals.put(recorder,j);Call call=new Call(recorder,j,null,0);push(call);
        try {
            ComposerCommand.text(workspace);ComposerCommand.text(resource);
            if(handles.read(recorder)!=0)throw new IllegalStateException("late observation of live handle");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            ComposerCommand.string(out,"ulike-native-init-request-171-v1");
            // Version identity and native-library checks remain external proof requirements.
            out.writeInt(width);out.writeInt(height);ComposerCommand.string(out,workspace);out.writeInt(one);out.writeInt(two);
            ComposerCommand.string(out,resource);out.writeInt(three);out.writeBoolean(a);out.writeBoolean(b);out.writeBoolean(c);out.flush();
            j.beforeNativeInit(ComposerJournal.hex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray())));
        }catch(Exception failure){j.unrecordedMutation("initialization arguments/handle could not be observed");call.kind=2;}
    }
    /** Unknown initialization is rejected, never treated as the normal profile. */
    public synchronized void beforeUnsupportedInit(Object recorder) {
        ComposerJournal j=journals.get(recorder);
        if(j==null){if(journals.size()>=MAX_RECORDERS)throw new IllegalStateException("observation recorder budget exhausted");j=new ComposerJournal(recorder);journals.put(recorder,j);}
        j.unrecordedMutation("unsupported initialization profile");push(new Call(recorder,j,null,2));
    }
    public synchronized void before(Object recorder,ComposerCommand command) {
        ComposerJournal j=lookup(recorder);ComposerJournal.Ticket ticket=null;
        try {Long expected=nativeHandles.get(recorder);if(expected==null || expected.longValue()==0 || handles.read(recorder)!=expected.longValue())j.unrecordedMutation("mutation native handle changed/unobserved");ticket=j.begin(command);}
        catch(Exception failure){j.unrecordedMutation("entry arguments/handler not captured");}
        push(new Call(recorder,j,ticket,1));
    }
    public synchronized void malformed(Object recorder,String reason) {
        ComposerJournal j=lookup(recorder);j.unrecordedMutation(reason);push(new Call(recorder,j,null,2));
    }
    public synchronized void beforeUninit(Object recorder) {
        ComposerJournal j=lookup(recorder);j.disposed();push(new Call(recorder,j,null,3));
    }
    public synchronized void returned(int code) {
        Call call=pop();
        if(call.kind==0) {
            try {long handle=handles.read(call.recorder);call.journal.nativeInitResult(code,handle);if(code==0 && handle!=0)nativeHandles.put(call.recorder,handle);}
            catch(Exception failure){call.journal.unrecordedMutation("native init completion not observed");}
        } else if(call.kind==1){checkHandle(call.recorder,call.journal);call.journal.result(call.ticket,code);}
        else if(call.kind==3 && code==0){journals.remove(call.recorder);nativeHandles.remove(call.recorder);}
    }
    public synchronized void failed(Object recorder) {
        Call call=pop();
        if(call.recorder!=recorder){call.journal.unrecordedMutation("exception receiver mismatch");ComposerJournal j=journals.get(recorder);if(j!=null)j.unrecordedMutation("exception receiver mismatch");return;}
        call.journal.threw(call.ticket);
    }
    public synchronized ComposerJournal.Snapshot snapshot(Object recorder,Object shot,long epoch,String style) {
        // No implication that native init callbacks or queued style work completed.
        ComposerJournal j=lookup(recorder);checkHandle(recorder,j);return j.snapshot(shot,epoch,style);
    }
    public synchronized String invalidReason(Object recorder){return lookup(recorder).invalidReason();}
    /** Called by inventory-grounded unsupported writer hooks, not an allowlist guess. */
    public synchronized void unrecorded(Object recorder,String operation){lookup(recorder).unrecordedMutation(operation);}
    private void checkHandle(Object recorder,ComposerJournal journal) {
        try{Long expected=nativeHandles.get(recorder);if(expected==null || expected.longValue()==0 || expected.longValue()!=handles.read(recorder))journal.unrecordedMutation("observed native handle changed");}
        catch(Exception failure){journal.unrecordedMutation("native handle read failed");}
    }
    private ComposerJournal lookup(Object recorder) {
        if(recorder==null)throw new NullPointerException();ComposerJournal j=journals.get(recorder);
        if(j==null) {
            if(journals.size()>=MAX_RECORDERS)throw new IllegalStateException("observation recorder budget exhausted");
            j=new ComposerJournal(recorder);journals.put(recorder,j);j.unrecordedMutation("first observation after initialization entry");
        }return j;
    }
    private void push(Call call){ArrayDeque<Call> stack=calls.get();if(stack.size()>=MAX_DEPTH){call.journal.unrecordedMutation("nested hook budget exceeded");throw new IllegalStateException("nested hook budget exhausted");}stack.push(call);}
    private Call pop(){ArrayDeque<Call> stack=calls.get();if(stack.isEmpty())throw new IllegalStateException("unmatched hook completion");Call c=stack.pop();if(stack.isEmpty())calls.remove();return c;}
    private static final class Call { final Object recorder;final ComposerJournal journal;final ComposerJournal.Ticket ticket;int kind;Call(Object r,ComposerJournal j,ComposerJournal.Ticket t,int kind){recorder=r;journal=j;ticket=t;this.kind=kind;} }
}
