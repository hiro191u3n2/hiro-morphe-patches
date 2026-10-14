package com.hiro.ulike;
import android.graphics.Bitmap;

/** The only fault seam is in a separately compiled copy of runFinish1976:
 * selecting128 may throw its existing memory-refusal exception before work.
 * All32/128 outputs, leases, policies and certificates use production code. */
public final class FinishSchedule1993Test {
    static boolean refuseWide;static int attempts32,attempts128,assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static void output(boolean refused,int expectedRows)throws Exception {
        GpuFinishDefault1990.Fixture fixture=GpuFinishDefault1990.fixture(24);Bitmap expected=null,actual=null;
        try {
            expected=GpuChain1961.cpuFinish(fixture.source,fixture.rotation,fixture.width,fixture.height,fixture.plan,fixture.refresh);
            int wide=attempts128,narrow=attempts32;refuseWide=refused;
            ProcessingTiming1947.Trace trace=new ProcessingTiming1947.Trace(1993,System.currentTimeMillis(),System.nanoTime());
            actual=GpuChain1961.runDefault1993(fixture.source,fixture.rotation,fixture.width,fixture.height,fixture.plan,fixture.refresh,trace);
            check(actual!=null&&GpuFinishDefault1990.difference(expected,actual)==null,"full exact output after memory decision");
            long bands=trace.finishWork1992[ProcessingTiming1947.FINISH_READBACK1992*4];
            check(bands==(fixture.height+expectedRows-1)/expectedRows,"actual completed bands match selected schedule");
            check(trace.correctionRows1994==expectedRows&&trace.geometryBands1994==bands&&trace.geometryMinRows1994==1&&trace.geometryMaxRows1994==expectedRows,"actual geometry and final rows follow selected schedule including fallback");
            if(expectedRows==128)check(attempts128==wide+1&&attempts32==narrow,"certified wide schedule selected");
            else check(attempts32==narrow+1,"actual narrow schedule selected");
            if(refused&&GpuFinishDefault1990.wideReady1993())check(attempts128==wide+1,"wide runtime refusal exercised");
        }finally{if(actual!=null)actual.recycle();if(expected!=null)expected.recycle();fixture.source.recycle();}
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"all resources retired");
    }
    public static void main(String[] args)throws Exception {
        FinishPolicy1992Test.certificateStore();
        check(GpuFinishDefault1990.qualify(GO),"real startup32 corpus passes");
        check(GpuFinishDefault1990.wideReady1993(),"independent real startup128 corpus passes");
        GpuCertificates1990.Evidence wide=GpuCertificates1990.restore(GpuFinishDefault1990.wideSuite1993());
        check(wide!=null&&wide.cases==28&&wide.pixels>2500000,"wide finite corpus includes production widths and257rows");
        output(false,128);output(true,32);
        // A fresh store must establish32 despite the injected128 refusal; no
        // positive evidence or output bytes are supplied by this test seam.
        FinishPolicy1992Test.certificateStore();refuseWide=true;int before=attempts128;
        check(GpuFinishDefault1990.qualify(GO),"startup128 memory refusal preserves32 qualification");
        check(attempts128>before&&GpuFinishDefault1990.ready()&&!GpuFinishDefault1990.wideReady1993(),"no wide certificate on128 refusal");
        before=attempts128;output(false,32);check(attempts128==before,"uncertified wide route never invoked");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_jni\":true,\"startup128_refusal_preserves32\":true,\"runtime128_refusal_uses32\":true,\"wide_exact_pixels\":"+wide.pixels+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
