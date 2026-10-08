package com.hiro.ulike;

import java.util.Arrays;

/** H14: select the complete final image route, rather than a winning kernel.
 *
 * The callbacks own one photograph and its captured settings. They are never
 * retained here. gpu() must include candidate allocation/copy, policy work,
 * transfer, dispatch/context waiting, readback, and worker completion. cpu()
 * includes the complete original CPU stage. During probation only CPU output
 * is published; exact final pixels and two profitable complete calls after one
 * cold call are required before selecting GPU alone. This does not time the
 * identical capture, native beauty, encoder, or file-save stages.
 */
public final class WholeRoute1952 {
    private WholeRoute1952() { }
    private static final Engine ENGINE=new Engine(new Clock());

    static boolean run(int[] key,Work work) { return ENGINE.run(key,work); }

    /** Candidate storage must be disjoint from the immutable pre-stage input.
     * gpu() never mutates the CPU input, including on failure. equal() compares
     * every consumed final pixel, not a sample or a checksum. publishGpu() only
     * transfers the completed candidate's ownership; copying image data here
     * would hide cost outside the measured GPU route. discardGpu() releases
     * unpublished scratch/candidate storage and leaves a published image alive.
     */
    interface Work {
        boolean gpu();
        boolean cpu();
        boolean equal();
        void publishGpu();
        void discardGpu();
        /** A caller may exclude timings observed during changing contention. */
        default boolean timingReliable() { return true; }
    }

    static class Clock { long now() { return System.nanoTime(); } }

    /** Host tests use the actual gate with a scripted clock and owned mock
     * images. They do not claim to execute Android's physical GPU. */
    static final class Engine {
        private static final int LIMIT=24,MAX_KEY=64;
        private final Clock clock;
        private final State[] states=new State[LIMIT];
        private int victim;
        private volatile boolean unavailable;

        Engine(Clock clock) {
            if(clock==null)throw new NullPointerException("route clock");
            this.clock=clock;
        }

        boolean run(int[] key,Work work) {
            if(work==null)throw new NullPointerException("route work");
            if(unavailable||key==null||key.length<1||key.length>MAX_KEY)
                return work.cpu();
            State state;
            try { state=claim(key.clone()); }
            catch(OutOfMemoryError noAdmissionMemory) { return work.cpu(); }
            if(state==null)return work.cpu();
            int mode=state.mode();
            boolean complete=false;
            try {
                long started=clock.now();
                boolean candidate=candidate(work,state);
                long gpuNanos=clock.now()-started;
                if(mode==2&&candidate) {
                    // Compare observed complete work with the last reliable
                    // original route measurement. A slowdown affects the next
                    // photograph; this exact completed image needs no duplicate
                    // CPU calculation or periodic whole-photo validation pass.
                    state.observed(gpuNanos,reliable(work,state));
                    try {
                        work.publishGpu();
                        complete=true;
                        return true;
                    } catch(OutOfMemoryError failedCandidate) {
                        state.disable();
                    } catch(LinkageError failedCandidate) {
                        unavailable=true;state.disable();
                    } catch(RuntimeException failedCandidate) {
                        state.disable();
                    }
                }
                // The candidate never wrote the original, so even an admitted
                // GPU failure can execute the complete original CPU route.
                started=clock.now();
                boolean reference=work.cpu();
                long cpuNanos=clock.now()-started;
                complete=reference;
                if(mode==1&&candidate&&reference) {
                    boolean equal=false,reliable=false;
                    try {
                        equal=work.equal();
                        reliable=work.timingReliable();
                    } catch(OutOfMemoryError failedCandidate) {
                        state.disable();
                    } catch(LinkageError failedCandidate) {
                        unavailable=true;state.disable();
                    } catch(RuntimeException failedCandidate) {
                        state.disable();
                    }
                    state.probe(equal,gpuNanos,cpuNanos,reliable);
                } else if(!reference)state.disable();
                return reference;
            } finally {
                if(!complete)state.disable();
                // A failed release must not prevent the admission lease from
                // being returned or replace an already successful CPU result.
                try { work.discardGpu(); }
                catch(OutOfMemoryError failedCleanup) { state.disable(); }
                catch(LinkageError failedCleanup) { unavailable=true;state.disable(); }
                catch(RuntimeException failedCleanup) { state.disable(); }
                finally { state.release(); }
            }
        }

        private boolean candidate(Work work,State state) {
            if(unavailable)return false;
            try {
                boolean result=work.gpu();
                if(!result)state.disable();
                return result;
            } catch(OutOfMemoryError failedCandidate) {
                state.disable();return false;
            } catch(LinkageError failedCandidate) {
                unavailable=true;state.disable();return false;
            } catch(RuntimeException failedCandidate) {
                state.disable();return false;
            }
        }

        private boolean reliable(Work work,State state) {
            try { return work.timingReliable(); }
            catch(OutOfMemoryError failedTiming) { state.disable();return false; }
            catch(LinkageError failedTiming) { unavailable=true;state.disable();return false; }
            catch(RuntimeException failedTiming) { state.disable();return false; }
        }

        private synchronized State claim(int[] key) {
            for(State state:states)
                if(state!=null&&Arrays.equals(state.key,key))
                    return state.claim()>0?state:null;
            for(int i=0;i<LIMIT;i++)if(states[i]==null) {
                State state=new State(key);state.claim();states[i]=state;return state;
            }
            // Never evict a leased photograph: otherwise its shape could get
            // a second independent probation while its first probe is running.
            for(int i=0;i<LIMIT;i++) {
                int slot=(victim+i)%LIMIT;
                if(!states[slot].busy()) {
                    State state=new State(key);state.claim();states[slot]=state;
                    victim=(slot+1)%LIMIT;return state;
                }
            }
            return null;
        }
    }

    private static final class State {
        final int[] key;
        private boolean inFlight,disabled,admitted;
        private int probes,wins,mode;
        private long cpuBaseline;
        State(int[] key) { this.key=key; }
        synchronized boolean busy() { return inFlight; }
        synchronized int claim() {
            if(disabled||inFlight)return 0;
            inFlight=true;mode=admitted?2:1;return mode;
        }
        synchronized int mode() { return mode; }
        synchronized void release() { inFlight=false; }
        synchronized void disable() { disabled=true; }
        synchronized void probe(boolean equal,long gpu,long cpu,boolean reliable) {
            if(disabled)return;
            if(!equal) { disabled=true;return; }
            // Program/context construction in the first complete call is cold
            // work. It must be exact but cannot establish a steady-state win.
            if(probes++==0)return;
            if(!reliable)return;
            // A small margin prevents timer jitter from admitting a route with
            // a negligible or uncertain complete-photo improvement.
            if(gpu<=0||cpu<=0||gpu>=cpu||gpu>cpu-cpu/20) {
                disabled=true;return;
            }
            cpuBaseline=cpu;
            if(++wins>=2)admitted=true;
        }
        synchronized void observed(long gpu,boolean reliable) {
            if(disabled||!admitted||!reliable||cpuBaseline<=0||gpu<=0)return;
            long margin=cpuBaseline/10+(cpuBaseline%10==0?0:1);
            // Subtract instead of multiplying/adding timestamps to avoid
            // overflow even if an injected/test clock uses extreme durations.
            if(gpu>=cpuBaseline&&gpu-cpuBaseline>=margin)disabled=true;
        }
    }
}
