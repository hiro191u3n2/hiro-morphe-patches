package com.hiro.ulike.composer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/** Exact ordered replay into a separate, freshly initialized, owned SDK object.
 * There is deliberately no production Preconditions or queue-barrier implementation
 * in this checkpoint. Successful Java setter returns do not supply either proof.
 */
public final class ReplayPlan {
    private static final SecureRandom RANDOM=new SecureRandom();
    public interface Preconditions {
        /** Recheck installed ALL-state-mutation coverage, observed original native
         * init configuration, exclusive paused source ownership, exact shot/style,
         * source queue completion, and all original resource content identities.
         * Must reject if any relevant messages/textures/tracking state are omitted.
         * A v164 ordered_api_model_complete flag is insufficient evidence. */
        void requireCompleteObservedState(ComposerJournal.Snapshot snapshot)throws Exception;
    }
    public static final Preconditions UNAVAILABLE=new Preconditions(){
        @Override public void requireCompleteObservedState(ComposerJournal.Snapshot snapshot){throw new IllegalStateException("Complete live composer observation and source barrier are not installed");}
    };
    public interface ResourceMapping {
        /** Resolve each exact argument to owned hash-checked resources. No guessed
         * selected-style-only graph, directory-prefix search or omitted history.
         * Historical removed/reloaded paths are also required by exact replay. */
        String mapExactArgument(String original,boolean mayContainInlineDescriptor)throws Exception;
    }
    public interface ReplayTarget {
        Object identity();
        /** Prove normal initialization used the identical SDK and init arguments,
         * and this owned target has no prior composer work or competing writers. */
        void requireFresh(String sourceInitConfigurationSha256)throws Exception;
        int apply(ComposerCommand command)throws Exception;
        /** Actual SDK-queue acknowledgement, NOT a setter return, UI-thread post,
         * timer, getHandler() or one unrelated render callback. */
        BarrierReceipt awaitSetupBarrier(BarrierRequest request)throws Exception;
        /** On any failure discard/quarantine this target. Do not restore or
         * continue rendering a partially replayed graph. */
        void abort()throws Exception;
    }
    public static final class BarrierRequest {
        public final Object targetIdentity,shotIdentity;
        public final long shotEpoch;
        public final int nonce;
        public final String settingsSha256,mappedCommandsSha256;
        private BarrierRequest(Object target,ComposerJournal.Snapshot snapshot,String mapped) {
            targetIdentity=target;shotIdentity=snapshot.shotIdentity;shotEpoch=snapshot.shotEpoch;
            nonce=RANDOM.nextInt(Integer.MAX_VALUE-1)+1;settingsSha256=snapshot.sha256;mappedCommandsSha256=mapped;
        }
    }
    public static final class BarrierReceipt {
        public final Object observedTargetIdentity,observedShotIdentity;
        public final long observedShotEpoch;
        public final int observedNonce;
        public final String observedSettingsSha256,observedMappedCommandsSha256;
        /** Values must be read from the actual registered acknowledgement and its
         * exclusively owned request, never synthesized from an arbitrary timer. */
        public BarrierReceipt(Object target,Object shot,long epoch,int nonce,String settings,String mapped) {
            observedTargetIdentity=target;observedShotIdentity=shot;observedShotEpoch=epoch;observedNonce=nonce;
            observedSettingsSha256=settings;observedMappedCommandsSha256=mapped;
        }
        private void requireMatches(BarrierRequest request) {
            ComposerJournal.require(observedTargetIdentity==request.targetIdentity && observedShotIdentity==request.shotIdentity &&
                observedShotEpoch==request.shotEpoch && observedNonce==request.nonce && request.settingsSha256.equals(observedSettingsSha256) &&
                request.mappedCommandsSha256.equals(observedMappedCommandsSha256),"stale/mismatched native setup barrier");
        }
    }
    public final ComposerJournal.Snapshot snapshot;
    private final Preconditions preconditions;
    private boolean consumed;
    private ReplayPlan(ComposerJournal.Snapshot snapshot,Preconditions preconditions){this.snapshot=snapshot;this.preconditions=preconditions;}
    public static ReplayPlan authorize(ComposerJournal.Snapshot snapshot,Preconditions preconditions)throws Exception {
        if(snapshot==null || preconditions==null)throw new NullPointerException();
        snapshot.requireCurrent();preconditions.requireCompleteObservedState(snapshot);snapshot.requireCurrent();
        return new ReplayPlan(snapshot,preconditions);
    }
    public byte[] canonicalBytes(){return snapshot.canonicalBytes();}
    public synchronized void execute(ReplayTarget target,ResourceMapping mapping)throws Exception {
        ComposerJournal.require(!consumed,"replay plan already consumed");consumed=true;
        if(target==null || mapping==null)throw new NullPointerException();
        boolean touched=false;
        try {
            snapshot.requireCurrent();preconditions.requireCompleteObservedState(snapshot);snapshot.requireCurrent();
            Object targetId=target.identity();
            ComposerJournal.require(targetId!=null && targetId!=snapshot.recorderIdentity,"replay must use a separate owned target");
            // Preflight every argument before the first native mutation. Resource
            // verification failures cannot leave a partly configured target.
            List<ComposerCommand> mapped=new ArrayList<>();
            for(ComposerCommand c:snapshot.commands)mapped.add(c.remap(mapping));
            target.requireFresh(snapshot.initConfigurationSha256);snapshot.requireCurrent();
            ComposerJournal.require(target.identity()==targetId,"target identity changed");
            for(ComposerCommand command:mapped) {
                snapshot.requireCurrent();ComposerJournal.require(target.identity()==targetId,"target replaced during replay");
                touched=true;
                ComposerJournal.require(target.apply(command)==0,"native composer replay rejected; owned target must be discarded");
            }
            snapshot.requireCurrent();
            BarrierRequest request=new BarrierRequest(targetId,snapshot,digest(mapped));
            // Even an empty transcript needs a real barrier: initialization and
            // externally observed state completion are not inferred from emptiness.
            touched=true;BarrierReceipt receipt=target.awaitSetupBarrier(request);
            ComposerJournal.require(receipt!=null,"native setup acknowledgement absent");receipt.requireMatches(request);
            ComposerJournal.require(target.identity()==targetId,"target replaced at barrier");
            snapshot.requireCurrent();preconditions.requireCompleteObservedState(snapshot);snapshot.requireCurrent();
        }catch(Exception | Error failure) {
            // A pristine target can remain caller-owned after a preflight failure;
            // any possible native replay or barrier activity requires disposal.
            if(touched)try{target.abort();}catch(Exception | Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}
            throw failure;
        }
    }
    private static String digest(List<ComposerCommand> mapped)throws Exception {
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);
        ComposerCommand.string(d,"ulike-mapped-composer-170-v1");d.writeInt(mapped.size());for(ComposerCommand c:mapped)c.encode(d);d.flush();
        return ComposerJournal.hex(MessageDigest.getInstance("SHA-256").digest(b.toByteArray()));
    }
}
