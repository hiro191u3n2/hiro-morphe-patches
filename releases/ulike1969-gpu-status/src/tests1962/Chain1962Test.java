package com.hiro.ulike;

import android.graphics.Bitmap;
import java.io.*;
import java.util.Arrays;

/** Actual Mesa GLES production JNI execution against independently exported
 * full finishing-slice pixels. This is not physical Galaxy speed validation. */
public final class Chain1962Test {
    static long assertions,pixels;static int cases;
    static void check(boolean yes,String message){assertions++;if(!yes)throw new AssertionError(message);}
    static void equal(int[] expected,Bitmap candidate,String label) {
        check(candidate!=null,label+" absent");int[] actual=Chain1962Oracle.pixels(candidate);
        check(expected.length==actual.length,label+" length");
        for(int i=0;i<expected.length;i++){assertions++;pixels++;if(expected[i]!=actual[i])throw new AssertionError(label+" pixel "+i+" expected="+Integer.toHexString(expected[i])+" actual="+Integer.toHexString(actual[i]));}
        check(candidate.getDensity()==333,label+" density");check(candidate.hasAlpha(),label+" alpha metadata");
    }
    public static void main(String[] args)throws Exception {
        check(GpuNoise1960.supports(GpuNoise1960.FINISH1961),"actual finish");check(GpuNoise1960.supports(GpuNoise1960.GEOMETRY),"actual geometry");check(GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961),"actual analysis");
        if(args.length>1&&args[1].equals("failure")) {
            Bitmap source=Chain1962Oracle.image(95,97,2);int[] pristine=Chain1962Oracle.pixels(source);
            Native1960Test.setFault(2);Bitmap candidate=GpuChain1961.runFinish(source,90,131,139,Chain1962Oracle.plan(source,131,139,0),true);
            Native1960Test.setFault(0);check(candidate==null,"failed unmap candidate withheld");check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"failure preserved source");
            check(!GpuNoise1960.sessionBusy(),"failure released session");
            Bitmap recovery=GpuChain1961.cpuFinish(source,90,131,139,Chain1962Oracle.plan(source,131,139,0),true);check(recovery!=null,"CPU fallback remains available");recovery.recycle();source.recycle();
        } else {
            try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])))) {
                check(in.readInt()==19624,"frozen oracle header");int records=in.readInt();
                for(int record=0;record<records;record++) {
                    int sw=in.readInt(),sh=in.readInt(),w=in.readInt(),h=in.readInt(),rotation=in.readInt(),pattern=in.readInt(),mask=in.readInt();boolean refresh=in.readBoolean();
                    int[] expected=new int[in.readInt()];for(int i=0;i<expected.length;i++)expected[i]=in.readInt();
                    Bitmap source=Chain1962Oracle.image(sw,sh,pattern),actual=null,cpu=null;int[] pristine=Chain1962Oracle.pixels(source);
                    try {
                        QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,w,h,mask);
                        actual=GpuChain1961.runFinish(source,rotation,w,h,plan,refresh);equal(expected,actual,"resident "+record);
                        cpu=GpuChain1961.cpuFinish(source,rotation,w,h,plan,refresh);equal(expected,cpu,"whole oracle "+record);
                        check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"private source unchanged");
                        check(!GpuNoise1960.sessionBusy(),"completed session released");cases++;
                    } finally {if(actual!=null)actual.recycle();if(cpu!=null)cpu.recycle();source.recycle();}
                }
            }
            Bitmap source=Chain1962Oracle.image(95,97,0);
            QualityPixels1932.Plan ownedPlan=Chain1962Oracle.plan(source,95,97,0);
            Bitmap oracleOwner=source.copy(Bitmap.Config.ARGB_8888,true);
            Bitmap ownedResult=GpuChain1961.cpuFinishOwned1962(oracleOwner,0,95,97,ownedPlan,false);
            check(ownedResult==oracleOwner,"timed no-op CPU baseline does not make a full ownership copy");
            check(!source.isRecycled(),"CPU oracle ownership preserved original snapshot");ownedResult.recycle();
            oracleOwner=source.copy(Bitmap.Config.ARGB_8888,true);
            ownedResult=GpuChain1961.cpuFinishOwned1962(oracleOwner,90,131,139,Chain1962Oracle.plan(source,131,139,0),true);
            check(ownedResult!=oracleOwner&&oracleOwner.isRecycled(),"timed geometry retains its actual allocation/disposal cost");ownedResult.recycle();
            QualityPixels1932.Plan custom=Chain1962Oracle.plan(source,95,97,1);
            check(GpuChain1961.finish(source,0,95,97,custom,false)==null,"stateful/custom mask cannot enter detached admission");
            source.recycle();
            Bitmap unsupported=Chain1962Oracle.image(95,97,0);
            QualityPixels1932.Plan ordinary=Chain1962Oracle.plan(unsupported,95,97,0);
            unsupported.setGainmapForTest(true);
            check(GpuChain1961.finish(unsupported,0,95,97,ordinary,false)==null,"HDR gain map retains existing route");
            unsupported.setGainmapForTest(false);unsupported.setColorSpaceForTest(new android.graphics.ColorSpace(false));
            check(GpuChain1961.finish(unsupported,0,95,97,ordinary,false)==null,"wide color retains existing route");
            unsupported.setColorSpaceForTest(new android.graphics.ColorSpace(true));int[] transparent={0x00000000};unsupported.setPixels(transparent,0,1,0,0,1,1);
            check(GpuChain1961.runFinish(unsupported,0,95,97,ordinary,false)==null,"nonopaque source does not enter integer chain");
            java.lang.reflect.Method opaque=GpuChain1961.class.getDeclaredMethod("opaqueSnapshot1962",Bitmap.class);opaque.setAccessible(true);
            check(!((Boolean)opaque.invoke(null,unsupported)),"unsupported private snapshot is screened before qualification scheduling");
            unsupported.recycle();
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"exactPixels\":"+pixels+",\"residentPostNoiseSlice\":true,\"allProcessingGpu\":false,\"physicalAndroidTested\":false}");
    }
}
