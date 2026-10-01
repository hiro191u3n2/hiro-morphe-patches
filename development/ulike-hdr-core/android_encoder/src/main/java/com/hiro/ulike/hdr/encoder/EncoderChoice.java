package com.hiro.ulike.hdr.encoder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Pure selection logic. Android's hardware flag is a manufacturer report, not a fidelity proof. */
public final class EncoderChoice {
    private EncoderChoice() {}
    public enum Policy { HARDWARE_ONLY, HARDWARE_PREFERRED, ANY }
    public static final class Candidate {
        public final String name;
        public final boolean hardware, main10, p010, size, vbr;
        public Candidate(String name, boolean hardware, boolean main10, boolean p010, boolean size, boolean vbr) {
            this.name = Objects.requireNonNull(name); this.hardware = hardware;
            this.main10 = main10; this.p010 = p010; this.size = size; this.vbr = vbr;
        }
    }
    public static Candidate select(List<Candidate> choices, Policy policy, String exactName) {
        Objects.requireNonNull(policy); Objects.requireNonNull(choices);
        List<Candidate> accepted = new ArrayList<>();
        for (Candidate c : choices) if (c.main10 && c.p010 && c.size && c.vbr
                && (policy != Policy.HARDWARE_ONLY || c.hardware)
                && (exactName == null || exactName.equals(c.name))) accepted.add(c);
        accepted.sort(Comparator.comparingInt((Candidate c) ->
                policy == Policy.HARDWARE_PREFERRED && c.hardware ? 0 : 1).thenComparing(c -> c.name));
        if (accepted.isEmpty()) throw new IllegalArgumentException("no advertised HEVC Main10 / P010 / size / VBR encoder satisfies policy");
        return accepted.get(0);
    }
}
