package com.hiro.ulike.hdr.beauty;

/** Declared HDR appearance replacement math, not recovered native ULike HDR behavior. */
public strictfp final class HdrAppearance {
    public static final double SDR_WHITE_NITS=203.0,HLG_REFERENCE_PEAK_NITS=1000.0,SYSTEM_GAMMA=1.2;
    public static final String POLICY="generated-reinhard-expansion-hlg1000-postneural-normalized-srgb-v1";
    private static final double K=HLG_REFERENCE_PEAK_NITS/SDR_WHITE_NITS;
    private HdrAppearance(){}
    public static final class Policy {
        public final double generatedPeakNits,storagePeakNits;
        public Policy(double generatedPeakNits,double storagePeakNits){
            require(Double.isFinite(generatedPeakNits) && Double.isFinite(storagePeakNits)
                    && generatedPeakNits>=SDR_WHITE_NITS && generatedPeakNits<=storagePeakNits
                    && storagePeakNits>SDR_WHITE_NITS && storagePeakNits<=10000,"explicit generated/storage peaks required");
            this.generatedPeakNits=generatedPeakNits;this.storagePeakNits=storagePeakNits;
        }
        public static Policy standard(){return new Policy(1000,10000);}
        public double headroom(){return storagePeakNits/SDR_WHITE_NITS;}
    }
    static void require(boolean b,String message){if(!b)throw new IllegalArgumentException(message);}
    static void finite(double v){require(Double.isFinite(v),"finite HDR values required");}
    static void target(double[] out){require(out!=null && out.length>=3,"three-channel destination required");}
    public static void checkedDisplay(double r,double g,double b,double headroom){
        require(Double.isFinite(headroom) && headroom>1 && headroom<=10000.0/SDR_WHITE_NITS
                && Double.isFinite(r) && Double.isFinite(g) && Double.isFinite(b)
                && r>=0 && g>=0 && b>=0 && r<=headroom && g<=headroom && b<=headroom,
                "display BT2020 exceeds explicit nonnegative storage headroom; no hidden clipping");
    }
    /** Source capture is never changed. Return flags: 1=gamut desaturation, 2=nonpositive-Y black. */
    public static int sceneToDisplay(double r,double g,double b,Policy policy,double[] out){
        finite(r);finite(g);finite(b);target(out);require(policy!=null,"policy required");
        double y=.2627*r+.678*g+.0593*b;finite(y);
        if(y<=0){out[0]=out[1]=out[2]=0;return (r!=0 || g!=0 || b!=0)?2:0;}
        int flags=0;double low=Math.min(r,Math.min(g,b));
        if(low<0){double t=y/(y-low);r=Math.max(0,y+(r-y)*t);g=Math.max(0,y+(g-y)*t);b=Math.max(0,y+(b-y)*t);flags=1;}
        double factor=K*Math.pow(y,SYSTEM_GAMMA-1);
        out[0]=r*factor;out[1]=g*factor;out[2]=b*factor;
        checkedDisplay(out[0],out[1],out[2],policy.headroom());return flags;
    }
    public static double decodeSrgb(double encoded){
        require(Double.isFinite(encoded) && encoded>=0 && encoded<=1,"unit encoded sRGB required");
        return encoded<=.04045?encoded/12.92:Math.pow((encoded+.055)/1.055,2.4);
    }
    public static double encodeSrgb(double linear){
        require(Double.isFinite(linear) && linear>=0 && linear<=1,"unit linear sRGB required");
        return linear<=.0031308?12.92*linear:1.055*Math.pow(linear,1/2.4)-.055;
    }
    public static void srgbToBt2020(double r,double g,double b,double[] out){
        target(out);finite(r);finite(g);finite(b);
        out[0]=.627403895934699*r+.32928303837788364*g+.04331306568741723*b;
        out[1]=.06909728935823208*r+.9195403950754587*g+.011362315566309178*b;
        out[2]=.016391438875150276*r+.08801330787722575*g+.8955952532476239*b;
        finite(out[0]);finite(out[1]);finite(out[2]);
    }
    /** NEW generated patch only. Never reads original capture RGB, gain, residual or geometry.
     * The global inverse applies to the new appearance and does not recover clipped capture.
     * Return true when the explicit generated peak bounds the patch.
     */
    public static boolean generatedPatch(double er,double eg,double eb,double exposure,Policy policy,double[] out){
        require(Double.isFinite(exposure) && exposure>=1e-6 && exposure<=1e6 && policy!=null,"explicit source-rendition exposure/policy required");
        srgbToBt2020(decodeSrgb(er),decodeSrgb(eg),decodeSrgb(eb),out);
        double r=out[0],g=out[1],b=out[2],yp=.2627*r+.678*g+.0593*b,max=Math.max(r,Math.max(g,b));
        if(yp<=0 || max<=0){out[0]=out[1]=out[2]=0;return false;}
        double cap=policy.generatedPeakNits/SDR_WHITE_NITS,capScale=cap/max,scale;boolean limited;
        if(yp>=1){scale=capScale;limited=true;}
        else {double logWanted=Math.log(K)+(SYSTEM_GAMMA-1)*Math.log(yp)-SYSTEM_GAMMA*Math.log(exposure*(1-yp));
            limited=logWanted>=Math.log(capScale);scale=limited?capScale:Math.exp(logWanted);}
        out[0]=r*scale;out[1]=g*scale;out[2]=b*scale;
        // Floating multiplication at the explicitly declared generated cap can
        // round one ULP upward; setting the largest channel to cap implements
        // that very cap, rather than introducing a new hidden scene clip.
        if(limited){if(r==max)out[0]=cap;if(g==max)out[1]=cap;if(b==max)out[2]=cap;}
        checkedDisplay(out[0],out[1],out[2],policy.headroom());return limited;
    }
    /** Convert CURRENT processed HDR to a nonnegative sRGB working gamut.
     * Return true for explicit luma-preserving desaturation; no captured residual is added.
     */
    public static boolean workingSrgb(double r,double g,double b,double[] out){
        target(out);finite(r);finite(g);finite(b);require(r>=0 && g>=0 && b>=0,"nonnegative display RGB required");
        double x=1.6604910021084345*r-.5876411387885495*g-.07284986331988488*b;
        double y=-.12455047452159074*r+1.1328998971259603*g-.008349422604369477*b;
        double z=-.018150763354905303*r-.10057889800800739*g+1.1187296613629127*b;
        finite(x);finite(y);finite(z);double low=Math.min(x,Math.min(y,z));boolean mapped=low<0;
        if(mapped){double lum=.21263900587151036*x+.7151686787677559*y+.07219231536073371*z;
            require(lum>=0 && Double.isFinite(lum),"invalid working gamut luminance");
            if(lum==0){x=y=z=0;}else{double t=lum/(lum-low);x=Math.max(0,lum+(x-lum)*t);y=Math.max(0,lum+(y-lum)*t);z=Math.max(0,lum+(z-lum)*t);}}
        finite(x);finite(y);finite(z);out[0]=x;out[1]=y;out[2]=z;return mapped;
    }
    /** Final SDR is derived from the fully processed HDR; a fresh gainmap carries highlights. */
    public static void deriveSdr(double r,double g,double b,double[] out){
        target(out);finite(r);finite(g);finite(b);require(r>=0 && g>=0 && b>=0,"nonnegative processed HDR required");
        double scale=Math.max(1,Math.max(r,Math.max(g,b)));out[0]=r/scale;out[1]=g/scale;out[2]=b/scale;
    }
}
