package review;

import com.hiro.ulike.hdr.beauty.HdrAppearance;
import java.io.*;

/** Independent fixture oracle is Python Decimal/rational math, not this Java code. */
public final class AppearanceReview {
    private static long checks, compared;
    private static double maxAbs, maxRelative;
    private static void require(boolean value, String what) {
        checks++;
        if (!value) throw new AssertionError(what);
    }
    private interface Bad { void run() throws Exception; }
    private static void rejects(Bad action, String what) throws Exception {
        boolean failed=false;
        try { action.run(); } catch (IllegalArgumentException|NullPointerException expected) { failed=true; }
        require(failed, "accepted "+what);
    }
    private static void near(double got, double expected, String what) {
        require(Double.isFinite(got), what+" nonfinite");
        double error=Math.abs(got-expected);
        maxAbs=Math.max(maxAbs,error);
        maxRelative=Math.max(maxRelative,error/Math.max(1,Math.abs(expected)));
        require(error<=4e-12*Math.max(1,Math.abs(expected)),what+" "+got+" != "+expected);
        compared++;
    }
    public static void main(String[] args) throws Exception {
        int[] cases=new int[5];
        try(DataInputStream in=new DataInputStream(new FileInputStream(args[0]))) {
            int count=in.readInt();
            for(int i=0;i<count;i++) {
                int kind=in.readInt();cases[kind]++;
                double r=in.readDouble(),g=in.readDouble(),b=in.readDouble();
                double exposure=in.readDouble(),peak=in.readDouble(),storage=in.readDouble();
                int flag=in.readInt();
                double[] expected={in.readDouble(),in.readDouble(),in.readDouble()}, out=new double[3];
                HdrAppearance.Policy policy=new HdrAppearance.Policy(peak,storage);
                int actual=0;
                switch(kind) {
                    case 0: actual=HdrAppearance.sceneToDisplay(r,g,b,policy,out);break;
                    case 1: actual=HdrAppearance.generatedPatch(r,g,b,exposure,policy,out)?1:0;break;
                    case 2: actual=HdrAppearance.workingSrgb(r,g,b,out)?1:0;break;
                    case 3: HdrAppearance.deriveSdr(r,g,b,out);break;
                    case 4: HdrAppearance.srgbToBt2020(r,g,b,out);break;
                    default: throw new AssertionError("fixture kind");
                }
                require(actual==flag,"flags at fixture "+i+" actual="+actual+" expected="+flag);
                for(int c=0;c<3;c++)near(out[c],expected[c],"fixture "+i+" channel "+c);
            }
            require(in.read()==-1,"trailing fixture data");
        }
        HdrAppearance.Policy policy=HdrAppearance.Policy.standard();
        double[] out=new double[3];
        require(policy.generatedPeakNits==1000 && policy.storagePeakNits==10000,"declared standard policy");
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1,.0,202.999}) {
            rejects(()->new HdrAppearance.Policy(bad,10000),"invalid generated peak "+bad);
        }
        rejects(()->new HdrAppearance.Policy(1000,999),"generated above storage peak");
        rejects(()->new HdrAppearance.Policy(203,203),"no HDR storage headroom");
        rejects(()->new HdrAppearance.Policy(1000,10000.1),"storage above PQ maximum");
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-.01,1.01}) {
            rejects(()->HdrAppearance.generatedPatch(bad,.5,.5,1,policy,out),"invalid generated sample");
            rejects(()->HdrAppearance.decodeSrgb(bad),"invalid encoded sample");
            rejects(()->HdrAppearance.encodeSrgb(bad),"invalid linear sample");
        }
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,0,1e-7,1e7})
            rejects(()->HdrAppearance.generatedPatch(.5,.5,.5,bad,policy,out),"invalid exposure");
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            rejects(()->HdrAppearance.sceneToDisplay(bad,0,0,policy,out),"nonfinite scene");
            rejects(()->HdrAppearance.workingSrgb(bad,0,0,out),"nonfinite working gamut");
            rejects(()->HdrAppearance.deriveSdr(bad,0,0,out),"nonfinite final SDR");
        }
        rejects(()->HdrAppearance.sceneToDisplay(20,20,20,policy,out),"scene exceeds storage headroom");
        rejects(()->HdrAppearance.sceneToDisplay(1,1,1,policy,new double[2]),"short scene output");
        rejects(()->HdrAppearance.generatedPatch(.5,.5,.5,1,policy,new double[2]),"short generated output");
        rejects(()->HdrAppearance.workingSrgb(-.01,0,0,out),"negative display RGB");
        rejects(()->HdrAppearance.deriveSdr(-.01,0,0,out),"negative processed HDR");
        rejects(()->HdrAppearance.checkedDisplay(1,1,1,Double.POSITIVE_INFINITY),"unbounded public headroom");
        rejects(()->HdrAppearance.workingSrgb(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE,out),"working transform overflow");
        // Round trips across the transfer-function join, with continuous non-8bit values.
        for(int i=0;i<=4096;i++) {
            double code=i/4096.0,decoded=HdrAppearance.decodeSrgb(code),encoded=HdrAppearance.encodeSrgb(decoded);
            near(encoded,code,"sRGB round trip");
        }
        System.out.println("{\"checks\":"+checks+",\"numeric_comparisons\":"+compared+
                ",\"maximum_absolute_error\":"+maxAbs+",\"maximum_relative_error\":"+maxRelative+
                ",\"scene_cases\":"+cases[0]+",\"generated_patch_cases\":"+cases[1]+
                ",\"working_gamut_cases\":"+cases[2]+",\"final_sdr_cases\":"+cases[3]+
                ",\"matrix_cases\":"+cases[4]+"}");
    }
}
