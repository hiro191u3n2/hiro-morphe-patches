package com.hiro.ulike.hdr.input;

import com.hiro.ulike.hdr.face.StillFaceAnalysis;
import java.nio.ReadOnlyBufferException;

public final class StillFaceAnalysisTest {
    static int checks;
    static void ok(boolean condition) { if (!condition) throw new AssertionError(); checks++; }
    static HdrFrame frame(int width, int height) {
        Object id = new Object();
        CaptureMatch.Context c = new CaptureMatch.Context(id,id,id,id,id,1,"0",null);
        CaptureMatch.Result r = new CaptureMatch.Result(c,id,100,5,null,"5",1000L,100);
        return new HdrFrame(width,height,HdrFrame.Encoding.BT2020_NCL_HLG_FULL,c,r,
                new short[][]{new short[width*height],new short[width*height/4],new short[width*height/4]},
                width*height*3L);
    }
    interface Throwing { void run() throws Exception; }
    static void bad(Throwing f) throws Exception {
        try { f.run(); throw new AssertionError("not rejected"); }
        catch (IllegalArgumentException | NullPointerException expected) { checks++; }
    }
    static StillFaceAnalysis.ProxyRenderer renderer = new StillFaceAnalysis.ProxyRenderer() {
        public String policyName() { return "TEST_ONLY_COORDINATE_GRID"; }
        public int rgb8(HdrFrame frame, int x, int y) { return ((x & 255) << 16) | ((y & 255) << 8); }
    };
    public static void main(String[] args) throws Exception {
        HdrFrame frame=frame(4,2);
        int[][] expectedOrigins={{0,0},{0,1},{3,1},{3,0}};
        int[][] expectedMirrorOrigins={{3,0},{0,0},{0,1},{3,1}};
        for (int turn=0;turn<4;turn++) for (int mirror=0;mirror<2;mirror++) {
            StillFaceAnalysis.Image image=StillFaceAnalysis.prepare(frame,turn*90,mirror!=0,128,renderer);
            int[] expected=(mirror==0?expectedOrigins:expectedMirrorOrigins)[turn];
            double[] point=image.sourcePoint(0,0);
            ok(point[0]==expected[0] && point[1]==expected[1]);
            ok((image.rgb8().get(0)&255)==expected[0] && (image.rgb8().get(1)&255)==expected[1]);
            try { image.rgb8().put(0,(byte)9); throw new AssertionError("mutable image"); }
            catch (ReadOnlyBufferException good) { checks++; }
            float[] points=new float[212];
            StillFaceAnalysis.Face face=new StillFaceAnalysis.Face(points,0.8f);
            points[0]=900; // constructor must copy native callback arrays immediately
            StillFaceAnalysis.Result result=StillFaceAnalysis.analyse(image, submitted -> {
                ok(submitted==image); return new StillFaceAnalysis.Face[]{face};
            });
            float[] output=result.requireSource(frame)[0].points();
            ok(output[0]==expected[0] && output[1]==expected[1]);
            output[0]=999;
            ok(result.requireSource(frame)[0].points()[0]==expected[0]);
            bad(() -> result.requireSource(frame(4,2))); // identical metadata isn't same ownership
        }
        HdrFrame large=frame(512,256);
        StillFaceAnalysis.Image resized=StillFaceAnalysis.prepare(large,0,false,128,renderer);
        ok(resized.width==128 && resized.height==64);
        ok(resized.sourcePoint(0,0)[0]==1.5 && resized.sourcePoint(0,0)[1]==1.5);
        ok(resized.sourcePoint(127,63)[0]==509.5 && resized.sourcePoint(127,63)[1]==253.5);
        bad(() -> StillFaceAnalysis.prepare(frame,45,false,128,renderer));
        bad(() -> StillFaceAnalysis.prepare(frame,0,false,5000,renderer));
        bad(() -> new StillFaceAnalysis.Face(new float[210],1));
        bad(() -> new StillFaceAnalysis.Face(new float[212],Float.NaN));
        float[] nan=new float[212]; nan[0]=Float.NaN;
        bad(() -> new StillFaceAnalysis.Face(nan,1));
        bad(() -> StillFaceAnalysis.analyse(resized,x -> null));
        bad(() -> StillFaceAnalysis.analyse(resized,x -> new StillFaceAnalysis.Face[11]));
        bad(() -> StillFaceAnalysis.analyse(resized,x -> new StillFaceAnalysis.Face[]{null}));
        boolean[] invoked={false};
        try {
            StillFaceAnalysis.analyse(resized,x -> { invoked[0]=true; throw new Exception("backend failure"); });
            throw new AssertionError("backend failure swallowed");
        } catch (Exception expected) { ok(invoked[0] && expected.getMessage().equals("backend failure")); }
        ok(StillFaceAnalysis.analyse(resized,x -> new StillFaceAnalysis.Face[0]).requireSource(large).length==0);
        System.out.println("StillFaceAnalysisTest PASS ("+checks+" assertions)");
    }
}
