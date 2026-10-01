package com.hiro.ulike.hdr.gainmapcodec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Pure deterministic selection. Each formatSupported value must be for the exact role and dimensions. */
public final class CodecSelection {
    private CodecSelection() {}
    public enum Policy { PREFER_HARDWARE, REQUIRE_HARDWARE, ANY }
    public static final class Candidate {
        public final String name;
        public final boolean hardware,main10,p010,formatSupported;
        public Candidate(String name,boolean hardware,boolean main10,boolean p010,boolean formatSupported) {
            if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("codec name");
            this.name=name;this.hardware=hardware;this.main10=main10;this.p010=p010;this.formatSupported=formatSupported;
        }
    }
    public static Candidate select(List<Candidate> candidates,Policy policy,String exactName) {
        Objects.requireNonNull(candidates);Objects.requireNonNull(policy);
        if(exactName!=null && exactName.trim().isEmpty()) throw new IllegalArgumentException("empty exact codec");
        List<Candidate> eligible=new ArrayList<>();
        for(Candidate c:candidates) {
            Objects.requireNonNull(c);
            if(c.main10 && c.p010 && c.formatSupported && (policy!=Policy.REQUIRE_HARDWARE || c.hardware)
                    && (exactName==null || exactName.equals(c.name))) eligible.add(c);
        }
        if(eligible.isEmpty()) throw new IllegalArgumentException("no advertised Main10/P010 codec for exact role and geometry");
        eligible.sort(Comparator.comparingInt((Candidate c)->policy==Policy.PREFER_HARDWARE && c.hardware?0:1).thenComparing(c->c.name));
        return eligible.get(0);
    }
}
