package com.hiro.ulike;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** The same driver runs with separately compiled, pinned .77 and actual .78
 * production classes. The candidate mode installs only test-local positive
 * records to exercise every new foreground branch; real admission has its own
 * queue/negative-control suite. No production source is replaced by this driver. */
public final class Cpu77Differential1978 {
    static boolean candidate,nativeMode;static long checks,pixels,floatBits,models,policies,singleCases,strongCases,changed,reads;
    static DataOutputStream out;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    static Method method(Class<?> c,String n,Class<?>... p)throws Exception{Method m=c.getDeclaredMethod(n,p);m.setAccessible(true);return m;}
    static Object invoke(Method m,Object owner,Object... a)throws Exception{
        try{return m.invoke(owner,a);}catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof Exception)throw (Exception)t;if(t instanceof Error)throw (Error)t;throw e;}
    }
    static void force(String key)throws Exception {
        if(!candidate)return;
        Class<?> q=Class.forName("com.hiro.ulike.GpuQualification1961");
        field(q,"base").set(null,"cpu1978-independent-test-only");
        Class<?> gpu=Class.forName("com.hiro.ulike.GpuNoise1960");field(gpu,"environment").set(null,"cpu1978-no-gpu-host");field(gpu,"loaded").setInt(null,-1);
        Object env=invoke(method(q,"environment"),null),name=invoke(method(q,"recordKey",String.class,String.class),null,key,env);
        Class<?> r=Class.forName("com.hiro.ulike.GpuQualification1961$Record");Constructor<?> ctor=r.getDeclaredConstructor(long.class,long.class,int.class);ctor.setAccessible(true);
        ((Map)field(q,"RECORDS").get(null)).put(name,ctor.newInstance(1000000L,1L,1));
    }
    static int[] image(int w,int h,int kind){Random r=new Random(197800L+w*17+h*31+kind);int[] a=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){
        int yy=kind==0?128:kind==1?12:kind==2?128+(x%16<8?17:-17)+(y%12<6?5:-5):32+(x*3+y*5)%192;
        int grain=kind==0?0:r.nextInt(25)-12;
        int red=Math.max(0,Math.min(255,yy+grain+(kind==3?23:0))),green=Math.max(0,Math.min(255,yy+grain)),blue=Math.max(0,Math.min(255,yy+grain+(kind==2?-11:0)));
        int alpha=kind==4&&(x+y*3)%23==0?(x*31+y*7)&254:255;a[y*w+x]=(alpha<<24)|(red<<16)|(green<<8)|blue;
    }return a;}
    static void ints(int[] values)throws Exception{out.writeInt(values.length);for(int v:values)out.writeInt(v);pixels+=values.length;}
    static void recordModel(Object model)throws Exception{
        Field[] fs=model.getClass().getDeclaredFields();Arrays.sort(fs,Comparator.comparing(Field::getName));
        for(Field f:fs){if(Modifier.isStatic(f.getModifiers()))continue;f.setAccessible(true);Class<?> t=f.getType();
            if(t==int.class){out.writeUTF(f.getName());out.writeInt(f.getInt(model));}
            else if(t==float.class){out.writeUTF(f.getName());out.writeInt(Float.floatToRawIntBits(f.getFloat(model)));floatBits++;}
            else if(t==boolean.class){out.writeUTF(f.getName());out.writeBoolean(f.getBoolean(model));}
            else if(t==int[].class){out.writeUTF(f.getName());ints((int[])f.get(model));}
            else if(t==float[].class){out.writeUTF(f.getName());float[] a=(float[])f.get(model);out.writeInt(a.length);for(float v:a){out.writeInt(Float.floatToRawIntBits(v));floatBits++;}}
        }out.writeUTF("END_MODEL");models++;
    }
    static final class Source implements SingleNoise1955.Patches,StrongNoise1958.Patches {
        final int[] source;final int width;int calls;Source(int[] a,int w){source=a;width=w;}
        public void read(int[] dst,int x,int y,int w,int h){calls++;for(int row=0;row<h;row++)System.arraycopy(source,(y+row)*width+x,dst,row*w,w);}
    }
    static QualityPixels1932.Plan plan(){return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(5.3f,7.4f,121f,.2f,100),200,10000000L,2,.83f,4,4,true,true,1f);}
    static void single()throws Exception {
        int[][] sizes={{7,9},{31,65},{65,129},{97,193},{513,81}};
        for(int[] size:sizes)for(int kind=0;kind<5;kind++) {
            int w=size[0],h=size[1];int[] src=image(w,h,kind),before=src.clone();Source source=new Source(src,w);
            force("cpu-single1978-probe-exact2-time5-v1:"+w+":"+h);
            SingleNoise1955.Model model=SingleNoise1955.probe(source,w,h);recordModel(model);out.writeInt(source.calls);reads+=source.calls;
            for(int nr:new int[]{0,1,4}) {
                int[] target=new int[src.length];Arrays.fill(target,0x13579bdf);int core=kind==0?7:kind==1?32:kind==2?64:kind==3?65:128;
                SingleNoise1955.Protection protection=kind==0?null:new GpuPolicy1960.Protection(plan(),false);
                Object workspace=null;Class<?> wc=null;Method process=null;
                if(candidate){wc=Class.forName("com.hiro.ulike.SingleNoise1955$Workspace");workspace=wc.getConstructor().newInstance();process=method(SingleNoise1955.class,"processRange",int[].class,int[].class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,boolean.class,SingleNoise1955.Model.class,SingleNoise1955.Protection.class,wc);}
                try{for(int first=0;first<h;first+=core){int last=Math.min(h,first+core);
                    if(candidate){Class<?> c=Class.forName("com.hiro.ulike.CpuSingle1978");int[] u=(int[])invoke(method(c,"geometry",int.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,boolean.class,SingleNoise1955.Model.class),null,w,h,first,last,0,h,0,nr,true,model);
                        force((String)invoke(method(c,"key",boolean.class,int[].class,boolean.class),null,nativeMode,u,protection!=null));
                        invoke(process,null,src,target,w,h,first,last,0,h,0,nr,true,model,protection,workspace);
                    }else SingleNoise1955.processCpuRange(src,target,w,h,first,last,0,h,0,nr,true,model,protection);
                }}finally{if(workspace!=null)((AutoCloseable)workspace).close();}
                check(Arrays.equals(src,before),"single immutable source");for(int i=0;i<src.length;i++){if((src[i]>>>24)!=255)check(src[i]==target[i],"single exact transparent pixels");if(src[i]!=target[i])changed++;}
                ints(target);singleCases++;
            }
        }
        check(changed>100,"active Single correction exercised");
        if(nativeMode)check(field(SingleNoise1955.class,"nativeState").getInt(null)>0,"actual Single JNI selected");
    }
    static void strong()throws Exception {
        int[][] sizes={{15,17},{32,33},{65,79},{97,131},{259,67}};
        for(int[] size:sizes)for(int kind=1;kind<5;kind++) {
            int w=size[0],h=size[1],nr=kind==1?1:4;int[] src=image(w,h,kind),before=src.clone();Source source=new Source(src,w);
            force("cpu-model1978-region-exact2-time5-v1:"+w+":"+h);int lw=w,lh=h;for(int i=0;i<3;i++){lw=(lw+1)/2;lh=(lh+1)/2;force("cpu-model1978-spectral-exact2-time5-v1:"+lw+":"+lh);}
            StrongNoise1958.Model model=nativeMode?StrongNoise1958.prepare(source,w,h,nr,true):StrongNoise1958.prepareJava(source,w,h,nr,true);
            recordModel(model);out.writeInt(source.calls);reads+=source.calls;
            int[] target=new int[src.length];Arrays.fill(target,0x13579bdf);
            StrongNoise1958.Workspace workspace=new StrongNoise1958.Workspace();
            try{for(int first=0;first<h;first+=37)StrongNoise1958.processRange(src,target,w,h,first,Math.min(h,first+37),0,h,0,nr,true,model,null,workspace);}
            finally{workspace.close();}
            check(Arrays.equals(src,before),"strong immutable source");for(int i=0;i<src.length;i++)if((src[i]>>>24)!=255)check(src[i]==target[i],"strong exact alpha");ints(target);strongCases++;
        }
        if(nativeMode)check(field(StrongNoise1958.class,"nativeState").getInt(null)>0,"actual Strong JNI selected");
    }
    static QualityPixels1932.SmoothMask smooth(int w,int h,int rotation)throws Exception{
        Class<?> c=Class.forName("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958");Constructor<?> k=c.getDeclaredConstructor(int.class,int.class);k.setAccessible(true);Object s=k.newInstance(w,h);byte[] values=(byte[])field(c,"confidence").get(s);new Random(1978+w).nextBytes(values);
        return (QualityPixels1932.SmoothMask)invoke(method(c,"outputMask",int.class,int.class,int.class),s,rotation,w,h);
    }
    static final class Unknown implements QualityPixels1932.RegionMask,QualityPixels1932.SmoothMask {
        int calls;long order;
        int next(int x,int y,int op){calls++;order=order*31+x*17L+y*7L+op;return calls%3==0?Integer.MAX_VALUE:x*1978-y*713+calls;}
        public int skinQ8(int x,int y){return next(x,y,1);}public int detailQ8(int x,int y){return next(x,y,2);}public int smoothingQ8(int x,int y){return next(x,y,3);}
    }
    static void finish()throws Exception {
        for(int rotation:new int[]{0,90,180,270})for(int kind=0;kind<5;kind++) {
            int w=81,h=67,first=3,last=64,origin=kind==4?-7:11;QualityPixels1932.Plan p=plan();Unknown unknown=null;
            if(kind!=0){float[] noise=new float[]{.0f,.31f,1.1f,3.3f,7.5f,32.0f};p=p.withOutputNoise(SpatialNoise1934.fromGpu1961(w,h,3,2,noise,new QualityPixels1932.NoiseStats(3.1f,4.2f,121f,.2f,100)));}
            if(kind==1||kind==2){byte[] skin=new byte[9*7],detail=new byte[skin.length];new Random(19).nextBytes(skin);new Random(78).nextBytes(detail);p=p.withFaceRegions(FaceRegions1934.uprightRaster(w,h,9,7,skin,detail,rotation));}
            if(kind>=2)p=p.withSmoothedRegions(smooth(w,h,rotation));
            if(kind==4){unknown=new Unknown();p=p.withFaceRegions(unknown).withSmoothedRegions(unknown);}
            if(candidate&&kind!=4){Class<?> c=Class.forName("com.hiro.ulike.CpuFinishPolicy1978");force((String)invoke(method(c,"key",QualityPixels1932.Plan.class,int.class,int.class,int.class,int.class,int.class,int.class,boolean.class),null,p,w,h,first,last,origin,4,false));}
            FinishPolicy1953.Band band=candidate?(FinishPolicy1953.Band)invoke(method(FinishPolicy1953.class,"prepareParallel1978",QualityPixels1932.Plan.class,int.class,int.class,int.class,int.class,int.class),null,p,w,h,first,last,origin):FinishPolicy1953.prepareCpu1961(p,w,h,first,last,origin);
            check(band!=null,"complete policy band");try{out.writeInt(band.mode);out.writeBoolean(band.rawFallback);int n=band.mode==FinishPolicy1953.CONSTANT4?4:band.pixels*(band.mode==FinishPolicy1953.RAW4?4:2);out.writeInt(n);for(int i=0;i<n;i++)out.writeInt(band.words[i]);policies+=band.pixels*4L;}finally{band.close();}
            if(unknown!=null){out.writeInt(unknown.calls);out.writeLong(unknown.order);check(unknown.calls>10000,"unknown stateful RAW4 traversal exercised");}
            // Exercise the real .77 finishing kernel with the admitted one-fetch
            // policy as well as the Java fallback when JNI is unavailable.
            if(kind==4){unknown.calls=0;unknown.order=0;}
            if(candidate&&kind!=4){Class<?> c=Class.forName("com.hiro.ulike.CpuFinishPolicy1978");force((String)invoke(method(c,"key",QualityPixels1932.Plan.class,int.class,int.class,int.class,int.class,int.class,int.class,boolean.class),null,p,w,h,first,last,origin,1,true));}
            int[] src=image(w,h,2),before=src.clone(),dst=new int[src.length];Arrays.fill(dst,0x2468ace0);
            QualityPixels1932.finishStripAt(src,dst,w,h,first,last,p,true,true,origin);check(Arrays.equals(src,before),"finish immutable source");ints(dst);
            if(unknown!=null){out.writeInt(unknown.calls);out.writeLong(unknown.order);}
        }
    }
    public static void main(String[] args)throws Exception {
        candidate=Boolean.parseBoolean(args[1]);nativeMode=Boolean.parseBoolean(args[2]);
        out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])));
        try{single();strong();finish();}finally{out.close();SpeedWorkers1935.trim();}
        System.out.println("{\"status\":\"passed\",\"assertions\":"+(checks+pixels+floatBits+policies)+",\"single_cases\":"+singleCases+",\"strong_cases\":"+strongCases+",\"models\":"+models+",\"pixels\":"+pixels+",\"float_bits\":"+floatBits+",\"policy_integers\":"+policies+",\"source_reads\":"+reads+",\"changed_single_pixels\":"+changed+",\"native\":"+nativeMode+"}");
    }
}
