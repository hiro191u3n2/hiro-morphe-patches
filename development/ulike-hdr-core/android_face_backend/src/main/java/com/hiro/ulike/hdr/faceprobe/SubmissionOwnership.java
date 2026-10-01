package com.hiro.ulike.hdr.faceprobe;

/** Exclusive resource transfer. Closing the original owner after transfer cannot free native input.
 * A failed join or failed releaser retains the resource in QUARANTINED; there is no reset/reuse API.
 */
public final class SubmissionOwnership<T> implements AutoCloseable {
    public enum State { CREATED, TRANSFERRED, SUBMITTED, CLOSED, QUARANTINED }
    public interface Releaser<T> { void release(T resource); }
    private T resource;
    private final Releaser<T> releaser;
    private State state=State.CREATED;
    private Claim<T> claim;
    public SubmissionOwnership(T resource,Releaser<T> releaser) {
        if(resource==null || releaser==null)throw new NullPointerException();
        this.resource=resource;this.releaser=releaser;
    }
    public synchronized State state(){return state;}
    /** Borrow only before transfer; callers must not mutate/release/retain this object. */
    public synchronized T beforeTransfer() {
        if(state!=State.CREATED)throw new IllegalStateException("Resource no longer caller-owned");return resource;
    }
    public synchronized Claim<T> transfer() {
        if(state!=State.CREATED)throw new IllegalStateException("Exactly one transfer required");
        Claim<T> next=new Claim<>(this);claim=next;state=State.TRANSFERRED;return next;
    }
    @Override public synchronized void close() {
        if(state==State.CREATED)release(); // Transferred/quarantined lifetime belongs solely to the probe.
    }
    private void release() {
        state=State.QUARANTINED; // Prevent reentrant release; retained until successful completion.
        try { releaser.release(resource);resource=null;state=State.CLOSED; }
        catch(RuntimeException|Error failure) { state=State.QUARANTINED;throw failure; }
    }
    public static final class Claim<T> {
        private final SubmissionOwnership<T> owner;
        private Claim(SubmissionOwnership<T> owner){this.owner=owner;}
        public T resource() { synchronized(owner) {
            if(owner.claim!=this || (owner.state!=State.TRANSFERRED && owner.state!=State.SUBMITTED))
                throw new IllegalStateException("Inactive transferred resource");return owner.resource;
        }}
        public void submitted(){synchronized(owner) {
            if(owner.claim!=this || owner.state!=State.TRANSFERRED)throw new IllegalStateException("Exactly one native submission required");
            owner.state=State.SUBMITTED;
        }}
        public void joined(boolean allNativeJoinsReturned){synchronized(owner) {
            if(owner.claim!=this || (owner.state!=State.TRANSFERRED && owner.state!=State.SUBMITTED))
                throw new IllegalStateException("Duplicate native lifetime completion");
            if(allNativeJoinsReturned)owner.release();else owner.state=State.QUARANTINED;
        }}
    }
}
