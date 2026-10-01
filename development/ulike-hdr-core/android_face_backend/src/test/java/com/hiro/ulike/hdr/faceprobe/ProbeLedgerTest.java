package com.hiro.ulike.hdr.faceprobe;

public final class ProbeLedgerTest {
    private static int assertions;
    private static void check(boolean yes) { assertions++; if (!yes) throw new AssertionError(); }
    private static float[][] points() { float[][] a = new float[1][212]; for (int i=0;i<212;i++) a[0][i]=i/10f; return a; }
    private static ProbeLedger start() { ProbeLedger p=new ProbeLedger(640,480);p.initialized(0);p.submit();return p; }
    private static void completed(ProbeLedger p) { p.face(points(),new float[]{.9f});p.rendered(640,480,640*480); }
    public static void main(String[] args) throws Exception {
        ProbeLedger a = start(); completed(a); check(a.await(false,1));
        ProbeLedger.Snapshot ok=a.finish(true);check(ok.callbacksObserved);check(!ok.sourceCoordinateContractVerified);
        float[][] original=points(); ProbeLedger b=start();b.face(original,new float[]{.5f});original[0][0]=99;
        b.rendered(640,480,640*480);ProbeLedger.Snapshot copy=b.finish(true);check(copy.rawPoints()[0][0]==0);
        float[][] exposed=copy.rawPoints();exposed[0][0]=88;check(copy.rawPoints()[0][0]==0);
        float[] scores=copy.rawScores();scores[0]=0;check(copy.rawScores()[0]==.5f);
        copy.events().clear();check(!copy.events().isEmpty());
        ProbeLedger early=new ProbeLedger(640,480);early.face(points(),new float[]{.5f});early.initialized(0);early.submit();
        early.rendered(640,480,640*480);check(!early.await(false,1));check(!early.finish(true).callbacksObserved);
        ProbeLedger twice=start();completed(twice);twice.face(points(),new float[]{.5f});check(!twice.finish(true).callbacksObserved);
        ProbeLedger badInit=new ProbeLedger(640,480);badInit.initialized(-1);check(!badInit.await(true,1));
        try{badInit.submit();throw new AssertionError();}catch(IllegalStateException expected){assertions++;}
        ProbeLedger missing=new ProbeLedger(640,480);check(!missing.await(true,1));check(!missing.finish(true).callbacksObserved);
        ProbeLedger noImage=start();noImage.face(points(),new float[]{.5f});check(!noImage.await(false,1));check(!noImage.finish(true).callbacksObserved);
        ProbeLedger badDims=start();badDims.rendered(480,640,640*480);check(!badDims.finish(true).callbacksObserved);
        ProbeLedger badLength=start();badLength.rendered(640,480,1);check(!badLength.finish(true).callbacksObserved);
        ProbeLedger badPoints=start();float[][] nan=points();nan[0][99]=Float.NaN;badPoints.face(nan,new float[]{.5f});check(!badPoints.finish(true).callbacksObserved);
        ProbeLedger badCount=start();badCount.face(new float[][]{new float[210]},new float[]{.5f});check(!badCount.finish(true).callbacksObserved);
        ProbeLedger badScore=start();badScore.face(points(),new float[]{Float.POSITIVE_INFINITY});check(!badScore.finish(true).callbacksObserved);
        ProbeLedger empty=start();empty.face(new float[0][],new float[0]);empty.rendered(640,480,640*480);check(empty.finish(true).callbacksObserved);
        ProbeLedger cleanup=start();completed(cleanup);check(!cleanup.finish(false).callbacksObserved);
        ProbeLedger initRepeat=new ProbeLedger(640,480);initRepeat.initialized(0);initRepeat.initialized(0);check(!initRepeat.await(true,1));
        ProbeLedger status=start();status.status(1,-3);check(!status.finish(true).callbacksObserved);
        ProbeLedger async=start();Thread thread=new Thread(()->{async.rendered(640,480,640*480);async.face(points(),new float[]{.8f});});
        thread.start();check(async.await(false,2000));thread.join();check(async.finish(true).callbacksObserved);
        a.face(points(),new float[]{.9f});check(ok.rawPoints()[0][0]==0);
        try{a.submit();throw new AssertionError();}catch(IllegalStateException expected){assertions++;}
        try{a.finish(true);throw new AssertionError();}catch(IllegalStateException expected){assertions++;}
        try{new ProbeLedger(Integer.MAX_VALUE,Integer.MAX_VALUE);throw new AssertionError();}catch(IllegalArgumentException expected){assertions++;}
        // Hand-written asymmetric 3x2 grid and its eight rotations/reflections.
        int[][] transforms={{1,2,3,4,5,6},{4,1,5,2,6,3},{6,5,4,3,2,1},{3,6,2,5,1,4},
                {3,2,1,6,5,4},{1,4,2,5,3,6},{4,5,6,1,2,3},{6,3,5,2,4,1}};
        for (int i=0;i<8;i++) {
            double[] errors=PixelOrientationEvidence.measure(transforms[0],3,2,transforms[i],i%2==0?3:2,i%2==0?2:3);
            check(errors[i]==0);
            for (int j=0;j<8;j++) if(j!=i && !Double.isNaN(errors[j]))check(errors[j]>0);
        }
        ProbeLedger orient=start();double[] errors={0,Double.NaN,1,Double.NaN,2,Double.NaN,3,Double.NaN};
        orient.orientationEvidence(errors);errors[0]=99;completed(orient);ProbeLedger.Snapshot os=orient.finish(true);
        check(os.renderOrientationRgbMae()[0]==0);double[] oe=os.renderOrientationRgbMae();oe[0]=22;check(os.renderOrientationRgbMae()[0]==0);
        ProbeLedger repeatOrient=start();repeatOrient.orientationEvidence(errors);repeatOrient.orientationEvidence(errors);check(!repeatOrient.finish(true).callbacksObserved);
        System.out.println("PASS ProbeLedger assertions="+assertions+"; synthetic callbacks only; native execution=false");
    }
}
