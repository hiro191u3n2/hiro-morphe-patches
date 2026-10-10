package com.hiro.ulike;

/** Stable work contract retained for WholeRoute1953. The superseded synchronous
 * .52 GPU/CPU probation engine has no callers in the shipped runtime or patch
 * methods. Removing it cannot change the active .53 proof/dispatch policy. */
public final class WholeRoute1952 {
    private WholeRoute1952() { }

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

}
