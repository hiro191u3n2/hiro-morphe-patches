package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

/** The same fixture is compiled in two separate JVM classpaths. Its .58
 * production inputs are byte-pinned snapshots, never wrappers around .59.
 * Binary records retain every map, evidence float bit, output and NR13 cell. */
public final class ExactBaseline1959 {
    private static long assertions,integers,floatBits,maskBytes;
    private static int cases,pipelineCases,halfHandoffs;
    private static boolean current;
    private static DataOutputStream out;
    private static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    private static int clip(int x){return Math.max(0,Math.min(255,x));}
    private static int rgb(int r,int g,int b){return 0xff000000|clip(r)<<16|clip(g)<<8|clip(b);}
    private static int[] fixture(int w,int h,int kind,long seed){
        Random random=new Random(seed);int[] p=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int v=kind==0?44:kind==1?35+x/11+y/17:kind==2?68+((x%13<2||y%19<2)?17:0):94;
            int coarse=(kind<2?(int)Math.round(5*Math.sin(x*.19)+4*Math.cos(y*.23)):0);
            int n=kind==3?0:random.nextInt(13)-6;
            int c=kind==3?0:random.nextInt(11)-5;
            p[y*w+x]=rgb(v+n+coarse+c,v+n+coarse,v+n+coarse-c);
            if(kind==4&&(x+5*y)%29==0)p[y*w+x]=(p[y*w+x]&0xffffff)|((x+y)%255)<<24;
        }
        return p;
    }
    private static StrongNoise1958.Patches patches(final int[] p,final int w,final int h){
        return new StrongNoise1958.Patches(){public void read(int[] dst,int x,int y,int width,int height){
            check(x>=0&&y>=0&&x+width<=w&&y+height<=h&&dst.length>=(long)width*height,"oracle patch bounds");
            for(int r=0;r<height;r++)System.arraycopy(p,(y+r)*w+x,dst,r*width,width);
        }};
    }
    private static StrongNoise1958.Protection protection(final int kind){
        return new StrongNoise1958.Protection(){public int budgetQ8(int x,int y){return kind==0?256:kind==1?0:(x*17+y*11)%329-32;}
            public int detailQ8(int x,int y){return kind==0?0:kind==1?256:(x*7+y*19)%313-25;}};
    }
    private static void recordInts(int[] p)throws Exception{out.writeInt(p.length);for(int value:p)out.writeInt(value);integers+=p.length;assertions+=p.length;}
    private static void recordFloats(float[] p)throws Exception{out.writeInt(p.length);for(float value:p)out.writeInt(Float.floatToRawIntBits(value));floatBits+=p.length;assertions+=p.length;}
    private static void recordBytes(byte[] p)throws Exception{out.writeInt(p.length);out.write(p);maskBytes+=p.length;assertions+=p.length;}
    private static Object field(Object obj,String name)throws Exception{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    private static void model(StrongNoise1958.Model m)throws Exception{
        out.writeInt(m.width);out.writeInt(m.height);out.writeInt(m.nonlocalCandidates);out.writeInt(m.fullNonlocalCandidates);
        for(String name:new String[]{"halfMap","quarterMap","eighthMap"})recordInts((int[])field(m,name));
        for(String name:new String[]{"evidence","runtimeEvidence"})recordFloats((float[])field(m,name));
        out.writeBoolean((Boolean)field(m,"measuredActive"));
    }
    private static int[] process(int[] p,int w,int h,int noise,boolean shadows,StrongNoise1958.Model m,StrongNoise1958.Protection policy){
        int[] a=p.clone();StrongNoise1958.processRange(p,a,w,h,0,h,0,h,0,noise,shadows,m,policy);return a;
    }
    private static void strong(int w,int h,int kind,int noise,boolean shadows,int policy)throws Exception{
        int[] p=fixture(w,h,kind,1959000L+cases),saved=p.clone();
        StrongNoise1958.Model m=StrongNoise1958.prepare(patches(p,w,h),w,h,noise,shadows);
        out.writeUTF("strong-"+cases);out.writeInt(w);out.writeInt(h);model(m);
        int[] expected=process(p,w,h,noise,shadows,m,protection(policy));recordInts(expected);
        check(Arrays.equals(saved,p),"strong source immutable");
        int[] confidence=new int[p.length];float[] scratch=new float[2];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)confidence[y*w+x]=m.smoothingQ8(p,w,h,x,y,0,h,0,scratch);
        recordInts(confidence);
        for(int chunk:new int[]{1,7,32,128,256}){
            int[] assembled=p.clone();
            for(int y=0;y<h;y+=chunk){int end=Math.min(h,y+chunk),origin=Math.max(0,y-StrongNoise1958.HALO),last=Math.min(h,end+StrongNoise1958.HALO);
                int[] input=Arrays.copyOfRange(p,origin*w,last*w),output=input.clone();
                StrongNoise1958.processRange(input,output,w,last-origin,y-origin,end-origin,0,last-origin,origin,noise,shadows,m,protection(policy));
                System.arraycopy(output,(y-origin)*w,assembled,y*w,(end-y)*w);
            }
            check(Arrays.equals(expected,assembled),"stripe exact "+w+"x"+h+" chunk="+chunk);recordInts(assembled);
        }
        final int[] parallel=p.clone();final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        final StrongNoise1958.Model fm=m;final int[] fp=p;final int fw=w,fh=h,fn=noise,fpolicy=policy;final boolean fs=shadows;
        Thread[] workers=new Thread[3];
        for(int k=0;k<3;k++){final int begin=k*h/3,end=(k+1)*h/3;workers[k]=new Thread(new Runnable(){public void run(){try{
            StrongNoise1958.processRange(fp,parallel,fw,fh,begin,end,0,fh,0,fn,fs,fm,protection(fpolicy));
        }catch(Throwable t){failure.compareAndSet(null,t);}}});workers[k].start();}
        for(Thread t:workers){t.join(30000);check(!t.isAlive(),"concurrent strip drains");}
        check(failure.get()==null,"concurrent strong failed "+failure.get());check(Arrays.equals(expected,parallel),"concurrent stripe exact");recordInts(parallel);
        int[] sentinel=new int[p.length];Arrays.fill(sentinel,0x12345678);boolean cancelled=false;
        Thread.currentThread().interrupt();try{StrongNoise1958.processRange(p,sentinel,w,h,0,h,0,h,0,noise,shadows,m,protection(policy));}catch(CancellationException e){cancelled=true;}finally{Thread.interrupted();}
        check(cancelled,"strong cancellation propagated");for(int v:sentinel)check(v==0x12345678,"cancel is transactional");
        boolean invalid=false;try{StrongNoise1958.processRange(p,p,w,h,0,h,0,h,0,noise,shadows,m,protection(policy));}catch(IllegalArgumentException e){invalid=true;}check(invalid,"aliased output rejected");
        cases++;
    }
    private static PhotoDetail.Settings options(boolean noise,boolean sharp){return new PhotoDetail.Settings(noise,3,sharp,4,true,true,true);}
    private static void bind(boolean noise,boolean sharp,boolean colour,int ow,int oh){
        PhotoDetail.REQUEST.set(options(noise,sharp));AsyncSave1935.CAPTURED.remove();AsyncSave1935.COLOUR.remove();
        ChromaPipeline177.enabled=colour;SaveQuality2.SIZE.set(new int[]{ow,oh});SaveQuality2.FALLBACK.set(0);HostAudit1932.EVENTS.get().clear();
    }
    private static void pipeline(int w,int h,int kind,int turn,int ow,int oh,boolean noise,boolean sharp,boolean colour,int config)throws Exception{
        int[] p=fixture(w,h,kind,1960000L+pipelineCases);Bitmap.Config c=config==1?Bitmap.Config.RGB_565:config==2?Bitmap.Config.RGBA_F16:Bitmap.Config.ARGB_8888;
        Bitmap input=Bitmap.from(w,h,p,c,true);input.setHasAlpha(kind==4);input.setPremultiplied(kind!=4);
        if(config==3)input.setColorSpaceForTest(new ColorSpace(false));if(config==4)input.setGainmapForTest(true);
        ShotContext1932.Snapshot shot=new ShotContext1932.Snapshot(800,30000000L,ShotContext1932.LENS_FRONT,true,.4f);ShotContext1932.SHOTS.put(input,shot);
        bind(noise,sharp,colour,ow,oh);Bitmap result=QualityPipeline1932.normalize(input,turn,false);
        check(Arrays.equals(input.snapshot(),p)&&!input.isRecycled(),"pipeline immutable original");
        out.writeUTF("pipeline-"+pipelineCases);out.writeInt(result.getWidth());out.writeInt(result.getHeight());out.writeUTF(result.getConfig().name());
        out.writeBoolean(result.isPremultiplied());out.writeBoolean(result.hasAlpha());out.writeInt(SaveQuality2.FALLBACK.get());recordInts(result.snapshot());
        int[] saved=result.snapshot();QualityPipeline1932.applyDetail(result,input,options(noise,sharp));
        check(Arrays.equals(saved,result.snapshot())||SaveQuality2.FALLBACK.get()!=0,"prepared output not processed twice");recordInts(result.snapshot());
        if(result!=input)result.recycle();input.recycle();pipelineCases++;
    }
    private static int[] half(int[] p,int w,int h){int hw=(w+1)/2,hh=(h+1)/2;int[] a=new int[hw*hh];
        for(int y=0;y<hh;y++)for(int x=0;x<hw;x++){int r=0,g=0,b=0,n=0;boolean opaque=true;
            for(int yy=y*2;yy<Math.min(h,y*2+2);yy++)for(int xx=x*2;xx<Math.min(w,x*2+2);xx++){int v=p[yy*w+xx];opaque&=v>>>24==255;r+=v>>>16&255;g+=v>>>8&255;b+=v&255;n++;}
            a[y*hw+x]=(opaque?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;
        }return a;
    }
    private static void staged(int w,int h,int kind,boolean shadows)throws Exception{
        int[] p=fixture(w,h,kind,1970000L+pipelineCases);Bitmap b=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);b.setHasAlpha(kind==4);b.setPremultiplied(kind!=4);
        QualityPixels1932.Plan plan=QualityPixels1932.plan(null,800,30000000L,ShotContext1932.LENS_FRONT,.4f,3,4,true,shadows,1f);
        Method single=QualityPipeline1932.class.getDeclaredMethod("singleNoiseInPlace1955",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class);single.setAccessible(true);
        Object raw=single.invoke(null,b,plan,3,shadows);recordInts(b.snapshot());
        if(current&&raw instanceof int[]){check(Arrays.equals((int[])raw,half(b.snapshot(),w,h)),"H45 committed half exact to reread");halfHandoffs++;}
        Method smooth=QualityPipeline1932.class.getDeclaredMethod("smoothNoiseInPlace1958",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class);smooth.setAccessible(true);
        Object mask=smooth.invoke(null,b,plan,3,shadows);recordInts(b.snapshot());recordBytes((byte[])field(mask,"confidence"));
        QualityPixels1932.SmoothMask sm=(QualityPixels1932.SmoothMask)mask;int[] samples=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)samples[y*w+x]=sm.smoothingQ8(x,y);recordInts(samples);
        Method transform=mask.getClass().getDeclaredMethod("outputMask",int.class,int.class,int.class);transform.setAccessible(true);
        for(int turn:new int[]{0,90,180,270}){QualityPixels1932.SmoothMask t=(QualityPixels1932.SmoothMask)transform.invoke(mask,turn,57,41);int[] q=new int[57*41];for(int y=0;y<41;y++)for(int x=0;x<57;x++)q[y*57+x]=t.smoothingQ8(x,y);recordInts(q);}
        b.recycle();pipelineCases++;
    }
    private static void failure(int type)throws Exception{
        int w=65,h=389;int[] p=fixture(w,h,0,1980000+type);Bitmap b=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);bind(true,true,false,91,577);
        if(type==0)Bitmap.failCopyOnce=true;if(type==1)Bitmap.failCreateOnce=true;if(type==2)Bitmap.failNextCopyWrite=true;
        Bitmap result=QualityPipeline1932.normalize(b,0,false);check(SaveQuality2.FALLBACK.get()==1,"failure route falls back");check(Arrays.equals(b.snapshot(),p)&&!b.isRecycled(),"failed attempt original retained");
        check(Bitmap.writesAfterRecycle.get()==0,"failed workers drain before recycle");out.writeUTF("failure-"+type);out.writeInt(SaveQuality2.FALLBACK.get());recordInts(result.snapshot());
        if(result!=b)result.recycle();b.recycle();pipelineCases++;
    }
    public static void main(String[] args)throws Exception{
        current=Boolean.parseBoolean(args[1]);out=new DataOutputStream(new FileOutputStream(args[0]));out.writeUTF("ULike .58 exact differential v1");
        int[][] sizes={{1,1},{2,3},{3,2},{7,9},{31,33},{64,65},{83,111},{35,259},{65,517}};
        for(int k=0;k<sizes.length;k++){int w=sizes[k][0],h=sizes[k][1];strong(w,h,k%5,4,true,k%3);strong(w,h,(k+1)%5,1,false,(k+2)%3);}
        strong(39,43,3,0,true,0);strong(97,67,0,3,true,0);strong(67,131,4,4,false,2);
        for(int turn:new int[]{0,90,180,270})pipeline(73,261,0,turn,turn%180==0?73:261,turn%180==0?261:73,true,true,false,0);
        pipeline(95,143,1,0,61,89,true,true,true,0);pipeline(95,143,4,90,67,43,true,true,true,0);
        pipeline(49,77,0,0,49,77,false,false,false,0);pipeline(49,77,0,0,49,77,true,false,false,1);
        for(int config:new int[]{2,3,4})pipeline(33,37,0,0,33,37,true,true,true,config);
        staged(65,389,0,true);staged(73,261,4,false);staged(35,517,2,true);
        for(int type=0;type<3;type++)failure(type);
        out.close();if(current)check(halfHandoffs>0,"H45 actual captured half executed");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"strongCases\":"+cases+",\"pipelineCases\":"+pipelineCases+",\"recordedIntegers\":"+integers+",\"recordedFloatBits\":"+floatBits+",\"recordedMaskBytes\":"+maskBytes+",\"committedHalfHandoffs\":"+halfHandoffs+",\"nativeAvailable\":"+StrongNoise1958.nativeAvailable()+"}");
    }
}
