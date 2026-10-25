package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** The identical fixture executes in separate original/candidate classpaths.
 * Actual QualityPipeline and FastResize are used. Android Bitmap, camera and
 * stock beauty/colour entrypoints remain explicitly labelled host adapters. */
public final class Pipeline1961Test {
    private static int assertions,cases,pixels;
    private static boolean gpu;
    private static DataOutputStream out;
    private static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    private static long dispatches()throws Exception{Class<?> c=Class.forName("com.hiro.ulike.Native1960Test");Method m=c.getDeclaredMethod("faultFacts");m.setAccessible(true);return ((long[])m.invoke(null))[3];}
    private static void awaitBackground()throws Exception {if(!gpu)return;Class<?> service=Class.forName("com.hiro.ulike.GpuQualification1961");Method retained=service.getMethod("retainedBytes");long end=System.nanoTime()+10000000000L;while(((Long)retained.invoke(null)).longValue()>0&&System.nanoTime()<end)Thread.sleep(10);check(((Long)retained.invoke(null)).longValue()==0,"candidate real idle background probe drained");}
    private static void scenario(int turn,int ow,int oh,boolean noise,boolean sharp,boolean colour,int kind)throws Exception{
        int w=33,h=37;int[] source=new int[w*h];Random random=new Random(196000+cases);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int n=39+random.nextInt(21),c=random.nextInt(9)-4;source[y*w+x]=0xff000000|(n+c)<<16|n<<8|(n-c);if(kind==4&&(x+y)%13==0)source[y*w+x]&=0x00ffffff;}
        Bitmap.Config config=kind==1?Bitmap.Config.RGBA_F16:Bitmap.Config.ARGB_8888;Bitmap b=Bitmap.from(w,h,source,config,true);b.setHasAlpha(kind==4);b.setPremultiplied(kind!=4);
        if(kind==2)b.setColorSpaceForTest(new ColorSpace(false));if(kind==3)b.setGainmapForTest(true);
        PhotoDetail.REQUEST.set(new PhotoDetail.Settings(noise,3,sharp,4,true,true,true));AsyncSave1935.CAPTURED.remove();AsyncSave1935.COLOUR.remove();
        ChromaPipeline177.enabled=colour;SaveQuality2.SIZE.set(new int[]{ow,oh});SaveQuality2.FALLBACK.set(0);HostAudit1932.EVENTS.get().clear();
        ShotContext1932.SHOTS.put(b,new ShotContext1932.Snapshot(800,30000000L,ShotContext1932.LENS_FRONT,true,.4f));
        Bitmap result=QualityPipeline1932.normalize(b,turn,false);check(Arrays.equals(source,b.snapshot())&&!b.isRecycled(),"pipeline owns original");
        out.writeInt(result.getWidth());out.writeInt(result.getHeight());out.writeUTF(result.getConfig().name());out.writeBoolean(result.isPremultiplied());out.writeBoolean(result.hasAlpha());out.writeInt(SaveQuality2.FALLBACK.get());
        int[] actual=result.snapshot();out.writeInt(actual.length);for(int value:actual)out.writeInt(value);pixels+=actual.length;
        awaitBackground();if(result!=b)result.recycle();b.recycle();cases++;
    }
    public static void main(String[] args)throws Exception{
        gpu=Boolean.parseBoolean(args[1]);out=new DataOutputStream(new FileOutputStream(args[0]));
        if(gpu){android.os.Build.FINGERPRINT="pipeline-host-gx1962-qualification-fixture";Class<?> service=Class.forName("com.hiro.ulike.GpuQualification1961");service.getMethod("initialize",android.content.Context.class).invoke(null,new android.content.Context());Class<?> nativeFacade=Class.forName("com.hiro.ulike.GpuNoise1960");Method support=nativeFacade.getDeclaredMethod("supports",int.class);support.setAccessible(true);check(((Boolean)support.invoke(null,0)).booleanValue(),"explicit candidate host lifecycle warms real GPU driver identity");}
        for(int turn:new int[]{0,90,180,270})scenario(turn,turn%180==0?33:37,turn%180==0?37:33,true,true,false,0);
        scenario(90,23,27,true,true,true,0);scenario(0,45,51,true,false,true,0);scenario(0,33,37,false,false,false,0);
        for(int kind=1;kind<=4;kind++)scenario(0,33,37,true,true,true,kind);
        out.close();long dispatch=Boolean.parseBoolean(args[1])?dispatches():0;
        check(!Boolean.parseBoolean(args[1])||dispatch>0,"actual saved-image pipeline submits GPU compute");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"pixels\":"+pixels+",\"actualGpuDispatches\":"+dispatch+",\"androidSdkAndBeautyAreExplicitHostFixtures\":true}");
    }
}
