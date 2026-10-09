package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import java.lang.reflect.Field;
import java.util.Arrays;

/** Executes the current saved-image entry and actual production JNI/GL shaders.
 * Bitmap is a host-owned buffer fixture, camera/SDK/encoder hardware is absent.
 * Native readback faults are injected after real compute work. */
public final class StrictRoute1965Test {
    static int assertions;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static Bitmap source(int width,int height,Bitmap.Config config) {
        int[] pixels=new int[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int value=56+((x*71+y*37+((x*y)%13))%15)-7;
            pixels[y*width+x]=0xff000000|value<<16|(value+2)<<8|value+1;
        }
        return Bitmap.from(width,height,pixels,config,false);
    }
    static void field(Object target,String name,Object value)throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    static Object job(Bitmap input,PhotoDetail.Settings settings,boolean colour)throws Exception {
        Class<?> type=Class.forName("com.hiro.ulike.AsyncSave1935$Job");
        Field unsafe=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");unsafe.setAccessible(true);
        Object allocator=unsafe.get(null);Object job=allocator.getClass().getMethod("allocateInstance",Class.class).invoke(allocator,type);
        field(job,"input",input);field(job,"detail",settings);field(job,"colour",colour);field(job,"fixed",false);
        Field current=AsyncSave1935.class.getDeclaredField("CURRENT");current.setAccessible(true);
        @SuppressWarnings("unchecked") ThreadLocal<Object> local=(ThreadLocal<Object>)current.get(null);local.set(job);
        return job;
    }
    static void clearJob()throws Exception {
        Field current=AsyncSave1935.class.getDeclaredField("CURRENT");current.setAccessible(true);((ThreadLocal<?>)current.get(null)).remove();
    }
    static Object get(Object target,String name)throws Exception {Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    static GpuRequiredFailure1965 expectFailure(Runnable work) {
        try{work.run();throw new AssertionError("GPU failure was converted to success");}
        catch(GpuRequiredFailure1965 expected){assertions++;return expected;}
    }
    static void formatFailure(Bitmap source,boolean colour,String stage)throws Exception {
        PhotoDetail.Settings settings=new PhotoDetail.Settings(true,2,true,2,false,true,true);
        Object job=job(source,settings,colour);int reads=source.reads;int[] original=source.snapshot();
        GpuRequiredFailure1965 early=expectFailure(new Runnable(){public void run(){
            AsyncSave1935.validateCaptureSource1965(source,0,false,colour,settings);
        }});
        check(early.stage.equals(stage)&&source.reads==reads,"original capture format rejected before watermark/copy");
        GpuRequiredFailure1965 failure=expectFailure(new Runnable(){public void run(){QualityPipeline1932.normalize(source,0,false);}});
        check(failure.stage.equals(stage),"unsupported format names exact reason: "+failure.stage);
        check(source.reads==reads&&source.writes==0,"unsupported format rejected before 8-bit pixel readback");
        check(!source.isRecycled()&&Arrays.equals(original,source.snapshot()),"source format/metadata/pixels stay owned and intact");
        check(get(job,"gpuFailure1965")!=null,"original controller catch cannot hide required failure");
        expectFailure(new Runnable(){public void run(){AsyncSave1935.encoding(source);}});
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});
        check(SaveQuality2.FALLBACK.get()==0,"no legacy normalizer entered on unsupported format");
        clearJob();
    }
    static void success(boolean noise,boolean sharp,boolean colour,int rotation,int[] size)throws Exception {
        Bitmap input=source(17,35,Bitmap.Config.ARGB_8888);int[] original=input.snapshot();
        PhotoDetail.Settings settings=new PhotoDetail.Settings(noise,2,sharp,2,false,true,true);
        Object job=job(input,settings,colour);SaveQuality2.SIZE.set(size);
        long[] before=GpuNoise1960.diagnostics1965();
        Bitmap result=QualityPipeline1932.normalize(input,rotation,false);
        check(result!=null&&!result.isRecycled(),"whole mandatory GPU route returns complete private image");
        check(result.getWidth()==size[0]&&result.getHeight()==size[1],"whole route geometry matches save request");
        check(input.writes==0&&!input.isRecycled()&&Arrays.equals(input.snapshot(),original),"every successful stage leaves source immutable");
        check(get(job,"gpuFailure1965")==null&&((java.lang.ref.WeakReference<?>)get(job,"gpuCompleted1965")).get()==result,"encoder receives exact completed identity proof");
        long[] after=GpuNoise1960.diagnostics1965();check(after[5]>before[5]&&after[6]>=after[5],"actual GL work completed before result publication");
        Bitmap detail=QualityPipeline1932.applyDetail(result,input,settings);check(detail==result,"post-normalize detail consumes proof without running filters twice");
        check(SaveQuality2.FALLBACK.get()==0,"no successful route invoked CPU legacy normalizer");
        int[] row=new int[result.getWidth()];Arrays.fill(row,0xff000000);result.setPixels(row,0,row.length,0,0,row.length,1);
        expectFailure(new Runnable(){public void run(){AsyncSave1935.encoding(result);}});
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});
        check(get(job,"gpuFailure1965")!=null,"mutating completed candidate invalidates encoder/publication proof");
        result.recycle();clearJob();SaveQuality2.SIZE.remove();
    }
    static void lateFailure()throws Exception {
        final Bitmap input=source(17,67,Bitmap.Config.ARGB_8888);int[] original=input.snapshot();
        PhotoDetail.Settings settings=new PhotoDetail.Settings(false,2,true,2,false,true,true);
        Object job=job(input,settings,false);long generation=GpuNoise1960.diagnostics1965()[0];
        Bitmap.afterWriteForTest=new Runnable(){public void run(){RequiredNative1965Test.fault(2);}};
        Bitmap.beforeRecycleForTest=new Runnable(){public void run(){RequiredNative1965Test.fault(0);}};
        GpuRequiredFailure1965 failure;
        try{failure=expectFailure(new Runnable(){public void run(){QualityPipeline1932.normalize(input,0,false);}});}
        finally{RequiredNative1965Test.fault(0);Bitmap.afterWriteForTest=null;Bitmap.beforeRecycleForTest=null;}
        check(failure!=null&&get(job,"gpuCompleted1965")==null,"late native readback failure never publishes incomplete result");
        check(input.writes==0&&!input.isRecycled()&&Arrays.equals(input.snapshot(),original),"all retries retain original source pixels");
        check(GpuNoise1960.diagnostics1965()[0]>=generation+2,"initial plus two safe retries use fresh private contexts");
        check(get(job,"gpuFailure1965")!=null,"late failure recorded on original save job");
        expectFailure(new Runnable(){public void run(){AsyncSave1935.encoding(input);}});
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});
        check(SaveQuality2.FALLBACK.get()==0,"late failure never enters legacy normalization");
        for(Bitmap candidate:Bitmap.ALL)if(candidate!=input)
            check(candidate.isRecycled(),"partial/retried private candidate recycled before terminal failure");
        clearJob();
    }
    static void off()throws Exception {
        Bitmap input=source(17,19,Bitmap.Config.RGBA_F16);input.setGainmapForTest(true);
        Object job=job(input,new PhotoDetail.Settings(false,2,false,2,false,true,true),false);
        Bitmap output=QualityPipeline1932.normalize(input,0,false);
        check(output==input&&input.reads==0&&input.writes==0,"user OFF no-op preserves high-depth HDR source without GPU conversion");
        check(((java.lang.ref.WeakReference<?>)get(job,"gpuCompleted1965")).get()==input,"OFF no-op has exact encoder proof");clearJob();
    }
    static void manual()throws Exception {
        clearJob();
        final Bitmap unsupported=source(17,19,Bitmap.Config.RGBA_F16);
        PhotoDetail.REQUEST.set(new PhotoDetail.Settings(true,2,false,2,false,true,true));
        // Reproduce a caller swallowing normalize's exception and attempting to
        // compress/publish its original. The receipt must remain sticky.
        try{QualityPipeline1932.normalize(unsupported,0,false);}catch(GpuRequiredFailure1965 swallowed){assertions++;}
        expectFailure(new Runnable(){public void run(){AsyncSave1935.encoding(unsupported);}});
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});
        expectFailure(new Runnable(){public void run(){QualityPipeline1932.applyDetail(unsupported,unsupported,PhotoDetail.snapshot1932());}});
        check(unsupported.reads==0&&!unsupported.isRecycled(),"manual swallowed failure cannot encode untreated original");
        PhotoDetail.REQUEST.remove();
        final Bitmap allowed=source(17,19,Bitmap.Config.RGBA_F16);
        Bitmap result=QualityPipeline1932.normalize(allowed,0,false);
        check(result==allowed,"next independent manual OFF shot receives new proof");
        final Throwable[] error=new Throwable[1];
        Thread encoder=new Thread(new Runnable(){public void run(){try {
            AsyncSave1935.encoding(allowed);AsyncSave1935.awaitPublication1956();
        }catch(Throwable failure){error[0]=failure;}}});encoder.start();encoder.join();
        check(error[0]==null,"weak bitmap receipt transfers to actual manual encoding thread");
        int[] row=new int[allowed.getWidth()];Arrays.fill(row,0xff101010);
        Bitmap mutable=allowed.copy(Bitmap.Config.RGBA_F16,true);
        // A copied original is not the identity admitted for this save.
        expectFailure(new Runnable(){public void run(){AsyncSave1935.encoding(mutable);}});
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});
    }
    static void sourceMutation()throws Exception {
        final Bitmap input=source(17,67,Bitmap.Config.ARGB_8888).copy(Bitmap.Config.ARGB_8888,true);
        Object job=job(input,new PhotoDetail.Settings(false,2,true,2,false,true,true),false);
        long generation=GpuNoise1960.diagnostics1965()[0];
        Bitmap.afterWriteForTest=new Runnable(){public void run(){
            Bitmap.afterWriteForTest=null;int[] row=new int[input.getWidth()];Arrays.fill(row,0xff010101);
            input.setPixels(row,0,row.length,0,0,row.length,1);
        }};
        GpuRequiredFailure1965 failure;
        try{failure=expectFailure(new Runnable(){public void run(){QualityPipeline1932.normalize(input,0,false);}});}
        finally{Bitmap.afterWriteForTest=null;}
        check(failure.stage.equals("source-generation")&&failure.fallbackForbidden,"source mutation is a terminal immutable-source failure");
        check(GpuNoise1960.diagnostics1965()[0]==generation,"changed source is never retried under a new generation");
        check(get(job,"gpuCompleted1965")==null&&!input.isRecycled(),"source owner remains alive, mixed-generation candidate cannot commit");
        expectFailure(new Runnable(){public void run(){AsyncSave1935.encoding(input);}});
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});clearJob();
    }
    static void capability()throws Exception {
        final Bitmap input=source(17,19,Bitmap.Config.ARGB_8888);int[] original=input.snapshot();
        Object job=job(input,new PhotoDetail.Settings(false,2,false,2,false,true,true),false);
        long generation=GpuNoise1960.diagnostics1965()[0];RequiredNative1965Test.fault(3);
        Bitmap result;
        try{result=QualityPipeline1932.normalize(input,90,false);}finally{RequiredNative1965Test.fault(0);}
        check(GpuNoise1960.diagnostics1965()[0]>generation,"first capability readback failure safely rebuilds private context");
        check(result.getWidth()==19&&result.getHeight()==17&&Arrays.equals(original,input.snapshot()),"capability retry completes exact GPU geometry, source intact");
        check(get(job,"gpuFailure1965")==null,"recovered capability is accepted before save proof");clearJob();result.recycle();
        check(GpuNoise1960.recoverPrivate1965(),"clear probe caches for persistent capability fault");
        final Bitmap failed=source(17,19,Bitmap.Config.ARGB_8888);
        Object failureJob=job(failed,new PhotoDetail.Settings(false,2,false,2,false,true,true),false);
        long before=GpuNoise1960.diagnostics1965()[0];RequiredNative1965Test.fault(2);
        GpuRequiredFailure1965 failure;
        try{failure=expectFailure(new Runnable(){public void run(){QualityPipeline1932.normalize(failed,90,false);}});}
        finally{RequiredNative1965Test.fault(0);}
        long after=GpuNoise1960.diagnostics1965()[0];
        check(failure.stage.equals("gpu-capability")&&after==before+2,"persistent capability fault gets initial plus two attempts only");
        check(failed.reads==0&&failed.writes==0&&!failed.isRecycled(),"failed capability never reads/converts original pixels");
        check(get(failureJob,"gpuCompleted1965")==null,"failed capability cannot produce encoder proof");
        expectFailure(new Runnable(){public void run(){AsyncSave1935.encoding(failed);}});
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});clearJob();
    }
    public static final class Manager {public Object a;}
    public static final class Notification {public int count;public void c(){count++;}}
    static void receiptUi()throws Exception {
        Bitmap input=source(17,19,Bitmap.Config.ARGB_8888);Object job=job(input,new PhotoDetail.Settings(false,2,false,2,false,true,true),false);
        Manager manager=new Manager();Object previous=new Object();manager.a=previous;
        Notification notification=new Notification();field(job,"manager",manager);field(job,"controller",new Object());
        field(job,"notification",notification);field(job,"released",true);
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});
        check(get(job,"gpuFailure1965")!=null,"swallowed publication receipt rejection still marks failed original job");
        field(job,"gpuFailure1965",GpuRequiredFailure1965.forbidden("test-receipt"));
        java.lang.reflect.Method finish=AsyncSave1935.class.getDeclaredMethod("finishOnMain",job.getClass());finish.setAccessible(true);
        int before=SaveQuality2.FAILURES.get();
        check(Boolean.TRUE.equals(finish.invoke(null,job)),"GPU failure receipt executes existing main callback");
        check(SaveQuality2.FAILURES.get()==before+1&&notification.count==0,"GPU failure shows save failure and never invokes saved notification");
        check(manager.a==previous,"failed receipt restores its private controller binding");
        field(job,"gpuFailure1965",null);finish.invoke(null,job);
        check(notification.count==1&&SaveQuality2.FAILURES.get()==before+1,"successful receipt preserves original notification behavior");clearJob();
    }
    static void detachedDetail()throws Exception {
        clearJob();final Bitmap input=source(17,35,Bitmap.Config.ARGB_8888);int[] original=input.snapshot();
        final PhotoDetail.Settings settings=new PhotoDetail.Settings(false,2,true,2,false,true,true);
        final Bitmap result=QualityPipeline1932.applyDetail(input,input,settings);
        check(result!=input&&!result.isRecycled()&&Arrays.equals(original,input.snapshot()),"detached final detail computes complete private GPU image");
        AsyncSave1935.encoding(result);AsyncSave1935.awaitPublication1956();
        int[] row=new int[result.getWidth()];Arrays.fill(row,0xff102030);result.setPixels(row,0,row.length,0,0,row.length,1);
        expectFailure(new Runnable(){public void run(){AsyncSave1935.awaitPublication1956();}});
        expectFailure(new Runnable(){public void run(){AsyncSave1935.encoding(input);}});
        result.recycle();
    }
    public static void main(String[] args)throws Exception {
        String mode=args[0];check(GpuNoise1960.available(),"actual production native compute engine loaded");
        if(!mode.equals("capability"))check(GpuNoise1960.soft64Verified1965(),"actual GPU software-binary64 conformance proof passed");
        if(mode.equals("format")) {
            formatFailure(source(17,19,Bitmap.Config.RGBA_F16),false,"format");
            Bitmap wide=source(17,19,Bitmap.Config.ARGB_8888);wide.setColorSpaceForTest(new ColorSpace(false));formatFailure(wide,false,"wide-colour");
            Bitmap hdr=source(17,19,Bitmap.Config.ARGB_8888);hdr.setGainmapForTest(true);formatFailure(hdr,false,"gainmap");
        } else if(mode.equals("ui"))receiptUi();
        else if(mode.equals("detail"))detachedDetail();
        else if(mode.equals("capability"))capability();
        else if(mode.equals("manual"))manual();
        else if(mode.equals("mutation"))sourceMutation();
        else if(mode.equals("late"))lateFailure();
        else if(mode.equals("off"))off();
        else if(mode.equals("sharp"))success(false,true,false,0,new int[]{17,35});
        else if(mode.equals("chroma"))success(false,true,true,0,new int[]{17,35});
        else if(mode.equals("noise"))success(true,true,true,0,new int[]{17,35});
        else if(mode.equals("geometry"))success(false,true,true,90,new int[]{41,21});
        else throw new IllegalArgumentException(mode);
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"mode\":\""+mode+"\",\"actualProductionStrictRoute\":true,\"actualJNICompute\":true,\"physicalAndroidTested\":false}");
    }
}
