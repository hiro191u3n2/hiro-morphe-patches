package com.hiro.ulike.composer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded lifecycle-bound record, with a sticky invalid state on ANY uncertain
 * mutation. A valid journal proves an observed API transcript only; live replay
 * additionally requires separately verified hook coverage and native barriers.
 */
public final class ComposerJournal {
    private static final int MAX_EVENTS=4096,MAX_BYTES=2097152;
    private final Object recorderIdentity;
    private final ArrayList<ComposerCommand> commands=new ArrayList<>();
    private long revision,handler; private int bytes; private boolean initialized,initStarted,disposed;
    private String initFingerprint,invalid;
    private Ticket pending;
    public ComposerJournal(Object recorderIdentity){if(recorderIdentity==null)throw new NullPointerException();this.recorderIdentity=recorderIdentity;}
    /** Must be called by a verified entry hook before any native allocation or configuration. */
    public synchronized void beforeNativeInit(String exactInitConfigurationSha256) {
        protocol(!initStarted && !initialized && !disposed && revision==0,"late/repeated native init observation");
        hash(exactInitConfigurationSha256);initFingerprint=exactInitConfigurationSha256;initStarted=true;revision++;
    }
    /** A zero return marks initialization only. It is never a composer queue barrier. */
    public synchronized void nativeInitResult(int result,long observedNativeHandler) {
        protocol(initStarted && !initialized && handler==0 && revision==1,"unmatched native init completion");revision++;
        if(result!=0 || observedNativeHandler==0){invalidate("native init failed or has no handler");return;}
        handler=observedNativeHandler;initialized=true;
    }
    public synchronized Ticket begin(ComposerCommand command) {
        require(command!=null,"command missing");
        if(!initialized || disposed){invalidate("mutation outside initialized lifecycle");return null;}
        if(pending!=null){invalidate("overlapping state mutations");return null;}
        if(invalid!=null)return null;
        revision++;pending=new Ticket(this,command,revision);return pending;
    }
    public synchronized void result(Ticket ticket,int nativeReturnCode) {
        if(ticket==null)return;
        protocol(ticket.owner==this && !ticket.finished && pending==ticket,"unknown or repeated result");
        ticket.finished=true;pending=null;revision++;
        // Native failure is not known to be atomic: do not replay an apparently
        // successful prefix as though the failed request never affected state.
        if(nativeReturnCode!=0){invalidate("failed mutation may have changed native state");return;}
        if(invalid!=null)return;
        int next=bytes+ticket.command.encodedSize();
        if(commands.size()>=MAX_EVENTS || next>MAX_BYTES){invalidate("journal bound exceeded; no eviction/reconstruction");return;}
        commands.add(ticket.command);bytes=next;
    }
    public synchronized void threw(Ticket ticket) {
        if(ticket!=null && ticket.owner==this && pending==ticket){ticket.finished=true;pending=null;}
        invalidate("mutation exception/unknown completion");
    }
    /** For direct generic effect messages, texture updates, runtime mutable
     * parameters or any unsupported setter. An unknown mutation is not ignored. */
    public synchronized void unrecordedMutation(String operation){ComposerCommand.text(operation);invalidate("unrecorded mutation: "+operation);}
    public synchronized void disposed(){disposed=true;invalidate("recorder teardown");}
    public synchronized String invalidReason(){return invalid;}
    private void protocol(boolean ok,String message){if(!ok){invalidate(message);throw new IllegalStateException(message);}}
    private void invalidate(String reason){if(invalid==null)invalid=reason;revision++;}
    public synchronized Snapshot snapshot(Object shotIdentity,long shotEpoch,String styleId) {
        require(shotIdentity!=null && shotEpoch>0,"shot identity required");
        require("7306041792770609665".equals(styleId) || "7307549491547083266".equals(styleId),"unsupported style");
        require(initialized && !disposed && invalid==null && pending==null,"journal is not a complete idle observed transcript");
        return new Snapshot(this,shotIdentity,shotEpoch,styleId);
    }
    private synchronized void requireCurrent(Snapshot snapshot) {
        require(snapshot.journal==this && snapshot.recorderIdentity==recorderIdentity && snapshot.nativeHandler==handler &&
                snapshot.revision==revision && initialized && !disposed && pending==null && invalid==null,"composer state changed or incomplete");
    }
    public static final class Ticket {
        private final ComposerJournal owner;private final ComposerCommand command;private final long revision;private boolean finished;
        private Ticket(ComposerJournal owner,ComposerCommand command,long revision){this.owner=owner;this.command=command;this.revision=revision;}
    }
    public static final class Snapshot {
        private final ComposerJournal journal;private final byte[] canonical;
        public final Object recorderIdentity,shotIdentity;
        public final long shotEpoch,revision,nativeHandler;
        public final String styleId,initConfigurationSha256,sha256;
        public final List<ComposerCommand> commands;
        /** Explicitly false: this object alone is not an execution or coverage receipt. */
        public final boolean nativeQueueBarrierProven=false,allMutationHooksProven=false;
        private Snapshot(ComposerJournal j,Object shot,long epoch,String style) {
            journal=j;recorderIdentity=j.recorderIdentity;shotIdentity=shot;shotEpoch=epoch;revision=j.revision;nativeHandler=j.handler;
            styleId=style;initConfigurationSha256=j.initFingerprint;commands=Collections.unmodifiableList(new ArrayList<>(j.commands));
            try {
                ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);
                ComposerCommand.string(out,"ulike-composer-transcript-170-v1");ComposerCommand.string(out,style);
                ComposerCommand.string(out,initConfigurationSha256);out.writeLong(epoch);out.writeLong(revision);
                out.writeInt(commands.size());for(ComposerCommand c:commands)c.encode(out);out.flush();canonical=b.toByteArray();
                sha256=hex(MessageDigest.getInstance("SHA-256").digest(canonical));
            }catch(Exception e){throw new IllegalStateException("canonical settings serialization",e);}
        }
        public byte[] canonicalBytes(){return canonical.clone();}
        public void requireCurrent(){journal.requireCurrent(this);}
    }
    static void hash(String s){require(s!=null && s.matches("[0-9a-f]{64}"),"exact SHA-256 required");}
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return s.toString();}
    static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
