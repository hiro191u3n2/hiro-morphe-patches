package com.hiro.ulike.hdr.stillanalysis;

import java.util.Set;

public final class StillAnalysisTest {
    private static int checks;
    private interface Throwing { void run() throws Exception; }
    private static void check(boolean condition) { checks++;if(!condition)throw new AssertionError("Check "+checks); }
    private static void rejects(Throwing action) throws Exception { checks++;try { action.run();throw new AssertionError("Expected rejection "+checks); }catch(IllegalArgumentException|IllegalStateException expected) {} }
    public static void main(String[] args) throws Exception {
        FakeFace f=new FakeFace();FakeInfo info=new FakeInfo(new FakeFace[]{f});
        f.ext.eyeCount=3;f.ext.eyeLeftPoints=new Point[]{new Point(2,3),new Point(4,5)}; // Preserve count; do not guess total/per-eye semantics.
        SdkFaceSnapshot s=SdkFaceSnapshot.copy(info);
        check(s.faces().length==1);check(!s.sourceCoordinateContractVerified);
        check(s.faces()[0].extraCounts().get("eyeCount")==3);check(s.faces()[0].extraPoints().get("eyeLeftPoints").length==4);
        f.points[0].x=99;f.ext.eyeLeftPoints[0].x=90;
        check(s.faces()[0].points106()[0]==0);check(s.faces()[0].extraPoints().get("eyeLeftPoints")[0]==2);
        s.faces()[0].points106()[0]=100;s.faces()[0].extraPoints().get("eyeLeftPoints")[0]=100;s.faces()[0].rect()[0]=100;
        check(s.faces()[0].points106()[0]==0);check(s.faces()[0].extraPoints().get("eyeLeftPoints")[0]==2);check(s.faces()[0].rect()[0]==0);
        s.faces()[0].visibility()[0]=99;check(s.faces()[0].visibility()[0]==0);
        check(SdkFaceSnapshot.copy(new FakeInfo(new FakeFace[0])).faces().length==0);
        rejects(()->SdkFaceSnapshot.copy(null));rejects(()->SdkFaceSnapshot.copy(new FakeInfo(new FakeFace[11])));
        f.points[1].x=Float.NaN;rejects(()->SdkFaceSnapshot.copy(info));f.points[1].x=0;
        f.points=new Point[105];rejects(()->SdkFaceSnapshot.copy(info));f=new FakeFace();
        FakeFace g=f;g.visibility=new float[1];rejects(()->SdkFaceSnapshot.copy(new FakeInfo(new FakeFace[]{g})));g.visibility=null;
        check(SdkFaceSnapshot.copy(new FakeInfo(new FakeFace[]{g})).faces()[0].visibility().length==0);
        g.score=2;rejects(()->SdkFaceSnapshot.copy(new FakeInfo(new FakeFace[]{g})));g.score=.8f;
        g.ext.lipCount=513;rejects(()->SdkFaceSnapshot.copy(new FakeInfo(new FakeFace[]{g})));g.ext.lipCount=0;
        transport();System.out.println("PASS "+checks+" still-analysis host checks; SDK execution=false");
    }
    private static StillMessageCollector collector() { return new StillMessageCollector(7,Set.of("A","B")); }
    private static void send(StillMessageCollector c,int n,String text) { c.accept(StillMessageCollector.MESSAGE_ID,7,n,text); }
    private static void transport() throws Exception {
        StillMessageCollector c=collector();c.accept(1,7,1,"ignored");c.accept(StillMessageCollector.MESSAGE_ID,8,1,"ignored");c.submitted();
        send(c,1,"S1|A|uniform|-1|Makeup,intensity,0.5");send(c,1,"S1|B|END|-1|0");send(c,2,"S1|A|END|-1|1");c.awaitComplete(1);
        StillMessageCollector.Snapshot result=c.finish();check(result.allExpectedExportsObserved);check(result.foreignMessages==2);check(result.records().get("A").size()==2);
        send(c,3,"S1|A|uniform|0|late");check(result.records().get("A").size()==2);rejects(c::finish);
        StillMessageCollector noFeatures=new StillMessageCollector(1,Set.of());noFeatures.submitted();noFeatures.awaitComplete(1);check(noFeatures.finish().allExpectedExportsObserved);
        rejects(()->new StillMessageCollector(0,Set.of()));rejects(()->new StillMessageCollector(1,Set.of("bad|feature")));
        String[] bad={"S1|A|END|-1|1","S1|C|END|-1|0","S1|A|other|0|1","S1|A|mvp|-1|1","S1|A|mvp|10|1","S1|A|mvp|x|1","S1|A|mvp|0|","S2|A|END|-1|0","S1|A|END|-1|0\n","x"};
        for(String b:bad) { StillMessageCollector x=collector();x.submitted();send(x,1,b);check(x.finish().failure!=null); }
        StillMessageCollector early=collector();send(early,1,"S1|A|END|-1|0");check(early.finish().failure.contains("before"));
        StillMessageCollector order=collector();order.submitted();send(order,2,"S1|A|END|-1|1");check(order.finish().failure!=null);
        StillMessageCollector dup=collector();dup.submitted();send(dup,1,"S1|A|END|-1|0");send(dup,2,"S1|A|END|-1|1");check(dup.finish().failure!=null);
        StillMessageCollector huge=collector();huge.submitted();send(huge,1,"x".repeat(8193));check(huge.finish().failure!=null);
        StillMessageCollector timeout=collector();timeout.submitted();rejects(()->timeout.awaitComplete(1));check(timeout.finish().failure.contains("timeout"));
        StillMessageCollector interrupted=collector();interrupted.submitted();Thread.currentThread().interrupt();try { interrupted.awaitComplete(100);throw new AssertionError(); }catch(InterruptedException expected){checks++;}finally{Thread.interrupted();}
        StillMessageCollector limit=new StillMessageCollector(7,Set.of("A"));limit.submitted();for(int i=1;i<=512;i++)send(limit,i,"S1|A|uniform|0|a,b,0");send(limit,513,"S1|A|END|-1|512");check(limit.finish().failure!=null);
    }
    public static final class Point { public float x,y;Point(float x,float y){this.x=x;this.y=y;} }
    public static final class Rect { public int left,top,right=8,bottom=8; }
    public static final class FakeExt { public int eyeCount,eyebrowCount,irisCount,lipCount;public Point[] eyeLeftPoints,eyeRightPoints,eyeBrowLeftPoints,eyeBrowRightPoints,irisLeftPoints,irisRightPoints,lipPoints; }
    public static final class FakeInfo { private final FakeFace[] data;FakeInfo(FakeFace[] d){data=d;}public FakeFace[] getInfo(){return data;} }
    public static final class FakeFace {
        public Point[] points=new Point[106];public float[] visibility=new float[106];public FakeExt ext=new FakeExt();public float score=.8f;
        FakeFace(){for(int i=0;i<points.length;i++)points[i]=new Point(0,i);}
        public Point[] getPoints(){return points;}public float[] getPointVisibility(){return visibility;}public Rect getRect(){return new Rect();}
        public float getScore(){return score;}public FakeExt getFaceExtInfo(){return ext;}public int getFaceID(){return 2;}public int getTrackCount(){return 1;}public int getAction(){return 0;}
        public float getEyeDistance(){return 3;}public float getYaw(){return 0;}public float getPitch(){return 0;}public float getRoll(){return 0;}
    }
}
