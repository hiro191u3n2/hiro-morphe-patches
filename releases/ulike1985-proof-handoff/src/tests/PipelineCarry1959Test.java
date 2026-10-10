package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** H44/H45 integration checks against the unchanged committed-Bitmap contract.
 * Android commit rounding is supplied by the runner's explicit fixture. */
public final class PipelineCarry1959Test {
    private static int assertions,scenarios;
    private static long removedReads;
    private static void check(boolean value,String why) {assertions++;if(!value)throw new AssertionError(why);}
    private static Method method(String name,Class<?>... arguments)throws Exception {
        Method m=QualityPipeline1932.class.getDeclaredMethod(name,arguments);m.setAccessible(true);return m;
    }
    private static int[] grain(int width,int height,boolean alpha) {
        int[] p=new int[width*height];Random random=new Random(19594045L+width*height);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int coarse=((x/5+y/7)&1)*8;
            int r=70+coarse+random.nextInt(49)-24,g=75+coarse+random.nextInt(37)-18,b=80+coarse+random.nextInt(45)-22;
            int a=alpha&&(x+y)%19==0?((x+y)%3==0?0:127):255;
            p[y*width+x]=(a<<24)|(r<<16)|(g<<8)|b;
        }
        return p;
    }
    private static int[] half(int[] source,int width,int height) {
        int columns=(width+1)/2,rows=(height+1)/2;int[] result=new int[columns*rows];
        for(int y=0;y<rows;y++)for(int x=0;x<columns;x++) {
            int r=0,g=0,b=0,n=0;boolean alpha=true;
            for(int yy=2*y;yy<Math.min(height,2*y+2);yy++)for(int xx=2*x;xx<Math.min(width,2*x+2);xx++) {
                int p=source[yy*width+xx];alpha&=(p>>>24)==255;r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;n++;
            }
            result[y*columns+x]=(alpha?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;
        }
        return result;
    }
    private static byte[] mask(Object value)throws Exception {
        if(value==null)return null;Field f=value.getClass().getDeclaredField("confidence");f.setAccessible(true);
        return ((byte[])f.get(value)).clone();
    }
    private static QualityPixels1932.Plan plan(int[] pixels,int width,int height) {
        return QualityPixels1932.plan(QualityPixels1932.estimate(pixels,width,height),
            800,30000000L,QualityPixels1932.LENS_WIDE,.4f,3,4,true,true,1f);
    }
    private static void carryAndComposition(int width,int height,boolean alpha)throws Exception {
        int[] initial=grain(width,height,alpha);
        Bitmap first=Bitmap.from(width,height,initial,Bitmap.Config.ARGB_8888,true);
        QualityPixels1932.Plan plan=plan(initial,width,height);
        Method single=method("singleNoiseInPlace1955",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class);
        int[] carried=(int[])single.invoke(null,first,plan,3,true);
        check(carried!=null,"ARGB stage hands off only complete half image");
        int[] committed=first.snapshot();check(Arrays.equals(carried,half(committed,width,height)),
            "carried half equals exact post-commit 2x2 average incl alpha and odd geometry");
        Bitmap reread=Bitmap.from(width,height,committed,Bitmap.Config.ARGB_8888,true);
        Bitmap carriedImage=Bitmap.from(width,height,committed,Bitmap.Config.ARGB_8888,true);
        Method strong=method("smoothNoiseInPlace1958",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class);
        Method carriedStrong=method("smoothNoiseInPlace1958",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class,int[].class);
        Object expected=strong.invoke(null,reread,plan,3,true);
        Object actual=carriedStrong.invoke(null,carriedImage,plan,3,true,carried);
        check(Arrays.equals(reread.snapshot(),carriedImage.snapshot()),"carried pyramid preserves every final strong pixel");
        check(Arrays.equals(mask(expected),mask(actual)),"carried pyramid preserves every NR13 confidence byte");
        check(reread.reads-carriedImage.reads==(height+1)/2,"H45 removes exactly the full-image half preparation rereads");
        removedReads+=reread.reads-carriedImage.reads;
        check(Arrays.equals(first.snapshot(),committed),"strong preparation never mutates source stage image");
        first.recycle();reread.recycle();carriedImage.recycle();scenarios++;
    }
    private static void formatFallback()throws Exception {
        int width=71,height=137;int[] p=grain(width,height,false);
        Bitmap rgb=Bitmap.from(width,height,p,Bitmap.Config.RGB_565,true);
        Object carried=method("singleNoiseInPlace1955",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class)
            .invoke(null,rgb,null,3,true);
        check(carried==null&&rgb.writes>0,"RGB565 retains committed readback without disabling first NR");
        Class<?> type=Class.forName("com.hiro.ulike.QualityPipeline1932$CommittedHalf1959");
        Method create=type.getDeclaredMethod("create",Bitmap.class,long.class);create.setAccessible(true);
        Bitmap argb=Bitmap.from(width,height,p,Bitmap.Config.ARGB_8888,true);
        check(create.invoke(null,argb,0L)==null,"insufficient optional carry memory returns established reread");
        check(argb.reads==0&&argb.writes==0,"optional memory gate touches no source pixels");
        Method memory=type.getDeclaredMethod("memoryBytes",int.class,int.class);memory.setAccessible(true);
        long exact=(Long)memory.invoke(null,width,height);
        check(create.invoke(null,argb,exact-1)==null,"optional carry memory bound includes row scratch and coverage");
        Object capture=create.invoke(null,argb,exact);check(capture!=null,"exact optional memory bound admits carry");
        Method result=type.getDeclaredMethod("result");result.setAccessible(true);
        check(result.invoke(capture)==null,"uncommitted or partial pyramid never published");
        Method close=type.getDeclaredMethod("close");close.setAccessible(true);close.invoke(capture);close.invoke(capture);
        rgb.recycle();argb.recycle();scenarios++;
    }
    private static void firstStageFailure()throws Exception {
        int width=93,height=397;
        Bitmap.failNextCreatedWrite=true;Bitmap owned=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        // Independently completing private strips may commit before another
        // worker reports failure; the entire failed preparation is discarded.
        boolean rejected=false;
        try {method("singleNoiseInPlace1955",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class)
            .invoke(null,owned,null,3,true);}
        catch(InvocationTargetException e) {rejected=e.getCause() instanceof IllegalStateException;}
        check(rejected,"failed first NR commit cannot return a carried pyramid");
        check(SpeedWorkers1935.cpuIdle1944(),"failed first-stage preparation drains all worker leases");
        owned.recycle();scenarios++;
    }
    private static void sharedPolicyAndGuide()throws Exception {
        final int width=65,height=131,end=128;final int[] input=grain(width,height,true);
        StrongNoise1958.Model model=StrongNoise1958.prepare(new StrongNoise1958.Patches() {
            public void read(int[] output,int x,int y,int w,int h) {
                for(int r=0;r<h;r++)System.arraycopy(input,(y+r)*width+x,output,r*w,w);
            }
        },width,height,3,true);
        final int[] calls={0,0};
        StrongNoise1958.Protection protection=new StrongNoise1958.Protection() {
            public int budgetQ8(int x,int y) {calls[0]++;return (x*43+y*17)%377-51;}
            public int detailQ8(int x,int y) {calls[1]++;return (x*11+y*37)%369-41;}
        };
        Class<?> type=Class.forName("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958");
        java.lang.reflect.Constructor<?> constructor=type.getDeclaredConstructor(int.class,int.class);constructor.setAccessible(true);
        Object legacy=constructor.newInstance(width,height),shared=constructor.newInstance(width,height);
        Method original=type.getDeclaredMethod("record",int[].class,int.class,int.class,int.class,int.class,int.class,
            StrongNoise1958.Model.class,StrongNoise1958.Protection.class);original.setAccessible(true);
        original.invoke(legacy,input,width,height,0,end,0,model,protection);calls[0]=calls[1]=0;
        StrongNoise1958.Workspace workspace=new StrongNoise1958.Workspace(true);
        try {
            int[] output=input.clone();StrongNoise1958.processRange(input,output,width,height,0,end,0,height,0,3,true,model,protection,workspace);
            check(calls[0]==width*end&&calls[1]==width*end,"H44 computes each owned-pixel protection coefficient once");
            Method cached=type.getDeclaredMethod("record",int[].class,int.class,int.class,int.class,int.class,int.class,
                StrongNoise1958.Model.class,StrongNoise1958.Protection.class,int[].class,int[].class);cached.setAccessible(true);
            cached.invoke(shared,input,width,height,0,end,0,model,protection,workspace.policy(),workspace.confidence());
            check(calls[0]==width*end&&calls[1]==width*end,"NR13 shares the cached policy without evaluating it again");
            check(Arrays.equals(mask(legacy),mask(shared)),"shared main-pass guide exactly matches legacy 4x4 representative confidence");
        } finally {workspace.close();}
        scenarios++;
    }
    public static void main(String[] arguments)throws Exception {
        for(int[] dimensions:new int[][]{{1,1},{5,3},{69,129},{97,397},{91,515}})
            carryAndComposition(dimensions[0],dimensions[1],false);
        carryAndComposition(73,257,true);sharedPolicyAndGuide();formatFallback();firstStageFailure();
        check(Bitmap.writesAfterRecycle.get()==0,"all carry workers drain before bitmap disposal");
        SpeedWorkers1935.trim();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+
            ",\"h44_shared_nr13_confidence_exact\":true,\"h45_committed_half_exact\":true,\"h45_full_image_row_reads_removed\":"+removedReads+
            ",\"h45_rgb565_committed_fallback\":true,\"physical_android_tested\":false,\"nativeAvailable\":"+StrongNoise1958.nativeAvailable()+"}");
    }
}
