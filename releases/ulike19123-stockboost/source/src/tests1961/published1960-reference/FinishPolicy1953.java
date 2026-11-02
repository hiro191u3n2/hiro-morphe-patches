package com.hiro.ulike;

/** H17 lossless representation of already-rounded Java finishing policy.
 * The exact .52 float interpolation, Math.round and signed custom-mask wrapping
 * stay in NativeMoire1951.preparePolicy. GPU readers decode only these integers. */
public final class FinishPolicy1953 {
    private FinishPolicy1953() {}
    public static final int RAW4 = 0;
    public static final int PACKED2 = 1;
    public static final int CONSTANT4 = 2;

    /** Sharpening disabled: the shader never reads policy. Keep the .52 four-zero
     * shortcut without evaluating float maps or custom masks for unused pixels. */
    public static Band unused(int pixels) {
        if (pixels <= 0) return null;
        checkInterrupted();
        int[] words=SpeedWorkers1935.borrowInts(4);
        try {
            words[0]=words[1]=words[2]=words[3]=0;
            checkInterrupted();
            Band result=new Band(CONSTANT4,words,pixels,false);
            words=null;
            return result;
        } finally { SpeedWorkers1935.release(words); }
    }

    /** One owned policy lease for [first,last). All columns, including corners and
     * transparent-pixel coordinates, are prepared; no content-dependent omissions.
     * Invalid geometry or an unrepresentable Java array size declines the GPU path. */
    public static Band prepare(QualityPixels1932.Plan plan, int width, int rows,
            int first, int last, int originY) {
        if (plan == null || width <= 0 || rows <= 0 || first < 0 || last <= first || last > rows) return null;
        long count = (long) width * (last - first);
        // RAW4 must remain possible for every arbitrary signed/custom mask value.
        if (count > Integer.MAX_VALUE / 4L) return null;
        int pixels = (int) count;
        checkInterrupted();
        int[] scratch = null;
        int[] packed = null;
        int[] raw = null;
        try {
            scratch = SpeedWorkers1935.borrowInts(4);
            // H24: prove coordinate independence from the Plan, never from a
            // sample. Arbitrary/custom masks still receive every coordinate.
            boolean variableNoise=plan.localNoise!=null &&
                (plan.noiseMapAtOutput || Math.abs(plan.outputScale-1f)<.00001f);
            boolean variableMask=plan.texturePriority && plan.faceRegions!=null;
            boolean singlePass=!variableMask || knownImmutableMask(plan.faceRegions);
            if(!variableNoise && !variableMask && plan.smoothedRegions==null) {
                NativeMoire1951.preparePolicy(plan,0,first+originY,scratch,0);
                checkInterrupted();
                Band result=new Band(CONSTANT4,scratch,pixels,false);
                scratch=null;return result;
            }
            int a = 0, b = 0, c = 0, d = 0;
            boolean uniform = true, overflow = false;
            int index = 0;
            for (int y = first; y < last; y++) {
                checkInterrupted();
                for (int x = 0; x < width; x++, index++) {
                    if ((index & 4095) == 0) checkInterrupted();
                    NativeMoire1951.preparePolicy(plan, x, y + originY, scratch, 0);
                    if (index == 0) { a=scratch[0]; b=scratch[1]; c=scratch[2]; d=scratch[3]; }
                    else if (a!=scratch[0] || b!=scratch[1] || c!=scratch[2] || d!=scratch[3]) uniform=false;
                    boolean fits = fits16(scratch[0]) && fits16(scratch[1])
                        && fits16(scratch[2]) && fits16(scratch[3]);
                    if (!fits) overflow=true;
                    if (overflow && !singlePass) {
                        // Unknown masks may be stateful. Published .53 calls
                        // them again for RAW4; retain that observable contract.
                        SpeedWorkers1935.release(packed);packed=null;
                    }
                    if (overflow && !uniform && singlePass) {
                        if(raw==null) {
                            // H26: retain every already-rounded prefix value.
                            // No mask is called a second time, even if custom
                            // signed values force RAW4 very late in the band.
                            raw=SpeedWorkers1935.borrowInts(pixels*4);
                            for(int prior=0;prior<index;prior++) {
                                if((prior&4095)==0)checkInterrupted();
                                int at=prior*4;
                                if(packed==null) {
                                    raw[at]=a;raw[at+1]=b;raw[at+2]=c;raw[at+3]=d;
                                } else {
                                    int pair0=packed[prior*2],pair1=packed[prior*2+1];
                                    raw[at]=pair0&65535;raw[at+1]=(pair0>>>16)&65535;
                                    raw[at+2]=pair1&65535;raw[at+3]=(pair1>>>16)&65535;
                                }
                            }
                            SpeedWorkers1935.release(packed);packed=null;
                        }
                        int at=index*4;
                        raw[at]=scratch[0];raw[at+1]=scratch[1];
                        raw[at+2]=scratch[2];raw[at+3]=scratch[3];
                    }
                    if (!overflow && !uniform) {
                        if (packed == null) {
                            packed=SpeedWorkers1935.borrowInts(pixels*2);
                            int pair0=a|(b<<16), pair1=c|(d<<16);
                            // Previous entries were proved identical, so this copies
                            // their exact rounded integers without re-running masks.
                            for (int prior=0; prior<index; prior++) {
                                if ((prior & 4095) == 0) checkInterrupted();
                                packed[prior*2]=pair0;packed[prior*2+1]=pair1;
                            }
                        }
                        packed[index*2]=scratch[0]|(scratch[1]<<16);
                        packed[index*2+1]=scratch[2]|(scratch[3]<<16);
                    }
                }
            }
            checkInterrupted();
            if (uniform) {
                Band result=new Band(CONSTANT4,scratch,pixels,false);
                scratch=null;
                return result;
            }
            if (overflow) {
                if(!singlePass) {
                    raw=SpeedWorkers1935.borrowInts(pixels*4);
                    index=0;
                    for(int y=first;y<last;y++) {
                        checkInterrupted();
                        for(int x=0;x<width;x++,index++) {
                            if((index&4095)==0)checkInterrupted();
                            NativeMoire1951.preparePolicy(plan,x,y+originY,raw,index*4);
                        }
                    }
                    checkInterrupted();
                }
                Band result=new Band(RAW4,raw,pixels,true);
                raw=null;
                return result;
            }
            Band result=new Band(PACKED2,packed,pixels,false);
            packed=null;
            return result;
        } finally {
            SpeedWorkers1935.release(scratch);
            SpeedWorkers1935.release(packed);
            SpeedWorkers1935.release(raw);
        }
    }
    private static boolean knownImmutableMask(QualityPixels1932.RegionMask mask) {
        // This exact, pinned production class is final, has a private
        // constructor, owns its immutable raster and has a pure sample().
        // Do not infer purity for arbitrary RegionMask implementations or for
        // another class loader's similarly named implementation.
        Class<?> type=mask.getClass();
        return type.getClassLoader()==FinishPolicy1953.class.getClassLoader() &&
            "com.hiro.ulike.FaceRegions1934$Mask".equals(type.getName());
    }
    private static boolean fits16(int value) { return (value & ~65535) == 0; }
    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("quality interrupted");
    }

    public static final class Band implements java.io.Closeable {
        public final int mode;
        public final int[] words;
        public final int pixels;
        public final long policyBytes;
        public final long denseBytes;
        public final boolean rawFallback;
        private boolean closed;
        private Band(int mode,int[] words,int pixels,boolean rawFallback) {
            this.mode=mode;this.words=words;this.pixels=pixels;
            this.policyBytes=(long)words.length*4L;
            this.denseBytes=(long)pixels*16L;
            this.rawFallback=rawFallback;
        }
        /** GPU submit must have copied the words before the caller returns this lease. */
        public synchronized void close() {
            if (!closed) { closed=true;SpeedWorkers1935.release(words); }
        }
    }
}
