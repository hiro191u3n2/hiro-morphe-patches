package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/** Executes current production final-save orchestration and SingleNoise1955.
 * Android bitmap, stock colour correction and camera bindings are explicit host
 * fixtures. These checks establish integration and ownership, not device speed.
 */
public final class PipelineSingleNoise1955Test {
    private static int assertions, scenarios;
    private static final Map<String,Number> metrics = new LinkedHashMap<String,Number>();
    private static void check(boolean value,String why) {
        assertions++;
        if(!value)throw new AssertionError(why);
    }
    private static PhotoDetail.Settings options(boolean noise,boolean sharp) {
        return new PhotoDetail.Settings(noise,3,sharp,4,true,true,true);
    }
    private static int[] grain(int width,int height,int spread,int mean,long seed) {
        int[] pixels=new int[width*height];Random random=new Random(seed);
        for(int i=0;i<pixels.length;i++) {
            int y=mean+random.nextInt(spread*2+1)-spread;
            pixels[i]=0xff000000|(y<<16)|(y<<8)|y;
        }
        return pixels;
    }
    private static Bitmap bitmap(int width,int height,int[] pixels) {
        return Bitmap.from(width,height,pixels,Bitmap.Config.ARGB_8888,true);
    }
    private static ShotContext1932.Snapshot metadata(int iso) {
        return new ShotContext1932.Snapshot(iso,30000000L,ShotContext1932.LENS_FRONT,true,.4f);
    }
    private static void bind(PhotoDetail.Settings settings,boolean colour,int width,int height) {
        PhotoDetail.REQUEST.set(settings);AsyncSave1935.CAPTURED.remove();AsyncSave1935.COLOUR.remove();
        ChromaPipeline177.enabled=colour;SaveQuality2.SIZE.set(new int[]{width,height});
        SaveQuality2.FALLBACK.set(0);HostAudit1932.EVENTS.get().clear();
    }
    private static double variance(int[] pixels,int width,int height) {
        int border=Math.min(12,Math.min(width,height)/4);double sum=0,sum2=0;int count=0;
        for(int y=border;y<height-border;y++)for(int x=border;x<width-border;x++) {
            int v=QualityPixels1932.luma(pixels[y*width+x]);sum+=v;sum2+=(double)v*v;count++;
        }
        return Math.max(0,sum2/count-(sum/count)*(sum/count));
    }
    private static void recycle(Bitmap output,Bitmap input) {
        if(output!=input)output.recycle();input.recycle();
    }
    private static void geometry(boolean colour,boolean noise,boolean sharp,int turn,int ow,int oh) {
        int width=192,height=145;int[] pixels=grain(width,height,18,119,1955);
        Bitmap input=bitmap(width,height,pixels);ShotContext1932.Snapshot shot=metadata(800);
        ShotContext1932.SHOTS.put(input,shot);PhotoDetail.Settings settings=options(noise,sharp);
        bind(settings,colour,ow,oh);
        QualityPixels1932.Plan outer=QualityPixels1932.plan(null,0,0,0,0,0,0,false,false,1);
        PhotoDetail.Settings old=options(false,false);
        HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").set(outer);
        HostAudit1932.<PhotoDetail.Settings>local("LEGACY").set(old);
        Bitmap output=QualityPipeline1932.normalize(input,turn,false);
        check(output.getWidth()==ow&&output.getHeight()==oh,"final geometry retained "+turn);
        check(Arrays.equals(input.snapshot(),pixels)&&!input.isRecycled(),"original image never rewritten/recycled");
        check(ShotContext1932.forBitmap(output)==shot,"source metadata follows output");
        check(HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").get()==outer,"outer plan restored");
        check(HostAudit1932.<PhotoDetail.Settings>local("LEGACY").get()==old,"outer options restored");
        check(SaveQuality2.FALLBACK.get()==0,"valid single-image preparation did not fall back");
        PhotoDetail.Settings downstream=ChromaPipeline177.LAST_SETTINGS.get();
        check(downstream!=null&&!downstream.noiseOn&&!downstream.sharpOn,
            "legacy main/residual NR and early sharpening disabled for new save path");
        if(!noise&&!sharp&&!colour&&turn==0&&ow==width&&oh==height)
            check(Arrays.equals(output.snapshot(),pixels),"captured filters OFF is pixel-identical");
        int before=ChromaPipeline177.APPLIES.get();int[] saved=output.snapshot();
        check(QualityPipeline1932.applyDetail(output,input,settings)==output,"prepared output is reused exactly once");
        check(before==ChromaPipeline177.APPLIES.get()&&Arrays.equals(saved,output.snapshot()),
            "detail completion does not apply NR/correction twice");
        check(!HostAudit1932.map("PREPARED").containsKey(output),"completion entry consumed");
        HostAudit1932.local("CURRENT").remove();HostAudit1932.local("LEGACY").remove();
        recycle(output,input);scenarios++;
    }
    private static void activeAndFinalSharp(boolean colour) {
        int width=224,height=389;int[] pixels=grain(width,height,24,75,7711955);
        Bitmap input=bitmap(width,height,pixels);ShotContext1932.SHOTS.put(input,metadata(1600));
        bind(options(true,false),colour,width,height);
        Bitmap denoised=QualityPipeline1932.normalize(input,0,false);
        QualityPipeline1932.applyDetail(denoised,input,options(true,false));
        double before=variance(pixels,width,height),clean=variance(denoised.snapshot(),width,height);
        check(SaveQuality2.FALLBACK.get()==0,"active NR did not silently fall back");
        check(clean<before*.80,"single-image NR reduces dark flat-field grain with colour "+colour);
        bind(options(true,true),colour,width,height);
        Bitmap sharp=QualityPipeline1932.normalize(input,0,false);
        QualityPipeline1932.applyDetail(sharp,input,options(true,true));
        double finalNoise=variance(sharp.snapshot(),width,height);
        check(finalNoise<=clean*1.10+.25,"final sharpen does not substantially re-amplify flat-field grain");
        check(Arrays.equals(input.snapshot(),pixels),"NR and final sharp keep input immutable");
        metrics.put("dark_original_variance_colour"+colour,before);
        metrics.put("dark_nr_variance_colour"+colour,clean);
        metrics.put("dark_nr_sharp_variance_colour"+colour,finalNoise);
        denoised.recycle();sharp.recycle();input.recycle();scenarios++;
    }
    private static void capturedOptions() {
        int width=144,height=113;int[] pixels=grain(width,height,21,112,8221955);
        Bitmap input=bitmap(width,height,pixels);bind(options(true,true),true,width,height);
        PhotoDetail.Settings captured=options(false,false);
        AsyncSave1935.CAPTURED.set(captured);AsyncSave1935.COLOUR.set(Boolean.FALSE);
        Bitmap output=QualityPipeline1932.normalize(input,0,false);
        check(Arrays.equals(output.snapshot(),pixels),"captured OFF settings override later live ON");
        check(!ChromaPipeline177.LAST_SETTINGS.get().noiseOn&&!ChromaPipeline177.LAST_SETTINGS.get().sharpOn,
            "same captured settings govern legacy stages");
        QualityPipeline1932.applyDetail(output,input,captured);
        AsyncSave1935.CAPTURED.remove();AsyncSave1935.COLOUR.remove();recycle(output,input);scenarios++;
    }
    private static void failure(boolean oom) {
        int width=193,height=145;int[] pixels=grain(width,height,19,111,551955);
        Bitmap input=bitmap(width,height,pixels);bind(options(true,false),false,320,241);
        if(oom)Bitmap.failCreateOnce=true;else Bitmap.failCopyOnce=true;
        Bitmap output=QualityPipeline1932.normalize(input,0,false);
        check(SaveQuality2.FALLBACK.get()==1,"injected failure returns to established normalization");
        check(Arrays.equals(input.snapshot(),pixels)&&!input.isRecycled(),"failed attempt leaves original intact");
        check(HostAudit1932.current()==null&&HostAudit1932.local("LEGACY").get()==null,
            "failed attempt clears thread state");
        check(!HostAudit1932.map("PREPARED").containsKey(output),"failed output not recorded as complete");
        int before=ChromaPipeline177.APPLIES.get();
        QualityPipeline1932.applyDetail(output,input,options(false,false));
        check(ChromaPipeline177.APPLIES.get()==before+1,"fallback detail remains reachable");
        check(Bitmap.writesAfterRecycle.get()==0,"failure drains workers before cleanup");
        recycle(output,input);scenarios++;
    }
    private static void extendedFormats() {
        int width=48,height=37;int[] pixels=grain(width,height,4,111,8);
        Bitmap gain=bitmap(width,height,pixels),wide=bitmap(width,height,pixels);
        Bitmap f16=Bitmap.from(width,height,pixels,Bitmap.Config.RGBA_F16,true);
        gain.setGainmapForTest(true);wide.setColorSpaceForTest(new ColorSpace(false));
        for(Bitmap input:new Bitmap[]{gain,wide,f16}) {
            bind(options(true,true),true,width,height);int before=ChromaPipeline177.APPLIES.get();
            int created=Bitmap.ALL.size();Bitmap output=QualityPipeline1932.normalize(input,0,false);
            check(SaveQuality2.FALLBACK.get()==1&&ChromaPipeline177.APPLIES.get()==before,
                "F16/wide-colour/gainmap stays outside new integer NR path");
            check(Bitmap.ALL.size()==created+1,"new NR creates no integer intermediate before legacy special-format fallback");
            check(Arrays.equals(input.snapshot(),pixels)&&!input.isRecycled(),"special-format source retained");
            check(!HostAudit1932.map("PREPARED").containsKey(output),"special-format fallback not falsely marked complete");
            recycle(output,input);scenarios++;
        }
    }
    private static void nrWriteFailure() {
        int width=193,height=389;int[] pixels=grain(width,height,24,75,271955);
        Bitmap input=bitmap(width,height,pixels);bind(options(true,false),false,width,height);
        int before=Bitmap.ALL.size();Bitmap.failNextCopyWrite=true;
        Bitmap output=QualityPipeline1932.normalize(input,0,false);
        check(SaveQuality2.FALLBACK.get()==1,"NR destination write failure reaches established fallback");
        check(Arrays.equals(input.snapshot(),pixels)&&!input.isRecycled(),"failed NR never publishes partial pixels to capture");
        check(Bitmap.ALL.size()>=before+2&&Bitmap.ALL.get(before).isRecycled(),
            "failed private NR bitmap discarded before fallback output");
        check(!HostAudit1932.map("PREPARED").containsKey(output),"failed NR cannot suppress fallback detail");
        check(HostAudit1932.current()==null&&HostAudit1932.local("LEGACY").get()==null,
            "failed NR drains worker scope and restores capture thread state");
        check(Bitmap.writesAfterRecycle.get()==0,"NR failure drains all writers before recycling private bitmap");
        recycle(output,input);scenarios++;
    }
    private static void completionIdentity() {
        int width=72,height=65;int[] pixels=grain(width,height,8,117,88);
        Bitmap input=bitmap(width,height,pixels),other=bitmap(width,height,pixels);
        bind(options(false,false),false,width,height);Bitmap output=QualityPipeline1932.normalize(input,0,false);
        int before=ChromaPipeline177.APPLIES.get();QualityPipeline1932.applyDetail(output,other,options(false,false));
        check(ChromaPipeline177.APPLIES.get()==before+1,"another capture cannot consume saved completion");
        check(!HostAudit1932.map("PREPARED").containsKey(output),"mismatched completion is removed");
        recycle(output,input);other.recycle();scenarios++;
    }
    private static void immutableStreamingHalos()throws Exception {
        final int width=89,height=789;final int[] pixels=grain(width,height,23,91,61955);
        SingleNoise1955.Model model=SingleNoise1955.probe(new SingleNoise1955.Patches() {
            public void read(int[] out,int x,int y,int w,int h) {
                for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,out,row*w,w);
            }
        },width,height);
        int[] expected=pixels.clone();
        SingleNoise1955.processRange(pixels,expected,width,height,0,height,0,height,0,3,true,model,null);
        Bitmap privateImage=bitmap(width,height,pixels);
        Method streaming=QualityPipeline1932.class.getDeclaredMethod("singleNoiseInPlace1955",
            Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class);
        streaming.setAccessible(true);streaming.invoke(null,privateImage,null,3,true);
        check(Arrays.equals(privateImage.snapshot(),expected),
            "in-place bounded multiworker pipeline retains immutable halos and equals whole-image NR");
        check(!privateImage.isRecycled(),"successful streaming stage retains owned final image");
        check(Bitmap.writesAfterRecycle.get()==0,"streaming stage drains all writers before caller ownership resumes");
        privateImage.recycle();scenarios++;
    }
    private static void concurrent()throws Exception {
        final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        final Bitmap[] inputs=new Bitmap[2],outputs=new Bitmap[2];final int[][] pixels=new int[2][];
        for(int k=0;k<2;k++) {
            pixels[k]=grain(112,389,k==0?24:3,k==0?75:155,991955+k);
            inputs[k]=bitmap(112,389,pixels[k]);ShotContext1932.SHOTS.put(inputs[k],metadata(k==0?1600:100));
        }
        Thread[] threads=new Thread[2];
        for(int k=0;k<2;k++) {
            final int index=k;
            threads[k]=new Thread(new Runnable(){public void run(){try {
                PhotoDetail.REQUEST.set(options(true,true));AsyncSave1935.CAPTURED.set(options(index==0,index==0));
                AsyncSave1935.COLOUR.set(Boolean.FALSE);SaveQuality2.SIZE.set(new int[]{112,389});
                SaveQuality2.FALLBACK.set(0);
                outputs[index]=QualityPipeline1932.normalize(inputs[index],0,false);
                QualityPipeline1932.applyDetail(outputs[index],inputs[index],options(true,true));
                if(SaveQuality2.FALLBACK.get()!=0)throw new AssertionError("parallel fallback");
            }catch(Throwable failure){error.set(failure);}}});threads[k].start();
        }
        for(Thread thread:threads){thread.join(20000);check(!thread.isAlive(),"parallel save preparation drains");}
        check(error.get()==null,"parallel save preparation completes");
        check(Arrays.equals(outputs[1].snapshot(),pixels[1]),"OFF shot does not inherit ON shot model/options");
        check(variance(outputs[0].snapshot(),112,389)<variance(pixels[0],112,389)*.80,
            "ON shot retains its own single-image NR");
        for(int k=0;k<2;k++) {
            check(Arrays.equals(inputs[k].snapshot(),pixels[k]),"parallel source remains immutable");
            check(ShotContext1932.forBitmap(outputs[k])==ShotContext1932.forBitmap(inputs[k]),"parallel metadata stays image-bound");
            recycle(outputs[k],inputs[k]);
        }
        check(Bitmap.writesAfterRecycle.get()==0,"parallel workers never write recycled images");scenarios++;
    }
    public static void main(String[] args)throws Exception {
        for(boolean colour:new boolean[]{false,true}) {
            geometry(colour,false,false,0,192,145);geometry(colour,true,false,0,192,145);
            geometry(colour,false,true,90,145,192);geometry(colour,true,true,270,145,192);
            geometry(colour,true,true,0,288,218);geometry(colour,true,true,90,97,128);
            activeAndFinalSharp(colour);
        }
        capturedOptions();failure(false);failure(true);nrWriteFailure();extendedFormats();completionIdentity();immutableStreamingHalos();concurrent();
        check(Bitmap.writesAfterRecycle.get()==0,"all pipeline workers honor final bitmap disposal");
        SpeedWorkers1935.trim();StringBuilder json=new StringBuilder("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"physical_android_tested\":false,\"nativeAvailable\":"+SingleNoise1955.nativeAvailable()+",\"metrics\":{");
        boolean comma=false;for(Map.Entry<String,Number> entry:metrics.entrySet()) {
            if(comma)json.append(',');comma=true;json.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
        }
        json.append("}}");System.out.println(json.toString());
    }
}
