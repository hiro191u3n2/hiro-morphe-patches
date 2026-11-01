package com.hiro.ulike;
import java.util.Arrays;
import java.util.Random;

/** Pixel equality against the untouched published v1943 processed-domain filter. */
public final class NoiseCache1944Test {
    private static long compared,changed;private static int cases;
    private static final QualityPixels1932.RegionMask FACE=new QualityPixels1932.RegionMask() {
        public int skinQ8(int x,int y){return ((x*17+y*11)&3)==0?0:((x+y)&1)==0?256:112;}
        public int detailQ8(int x,int y){return (x*31+y*7)%293;}
    };
    private static int[] image(int w,int h,int mode,Random random) {
        int[] pixels=new int[w*h];
        for(int yy=0;yy<h;yy++)for(int x=0;x<w;x++) {
            int r,g,b;
            if(mode==0){r=random.nextInt(256);g=random.nextInt(256);b=random.nextInt(256);}
            else if(mode==1){r=37+random.nextInt(17);g=39+random.nextInt(17);b=40+random.nextInt(17);}
            else if(mode==2){r=((x+yy)%4<2?81:98);g=r+3;b=r+6;}
            else if(mode==3){r=x<w/2?24:167;g=r;b=r;}
            else if(mode==4){r=(x*255/Math.max(1,w-1));g=r;b=r;}
            else {r=254;g=1;b=128;}
            int alpha=((mode==0 || mode==1) && random.nextInt(17)==0)?random.nextInt(255):255;
            pixels[yy*w+x]=(alpha<<24)|(r<<16)|(g<<8)|b;
        }
        return pixels;
    }
    private static QualityPixels1932.Plan plan(final int[] image,final int w,final int h,int noise,int kind) {
        if(kind==0)return null;
        QualityPixels1932.NoiseStats stats=kind==4?new QualityPixels1932.NoiseStats(0,0,128,0,256)
            :new QualityPixels1932.NoiseStats(kind==3?32:6,8,88,.2f,2048);
        QualityPixels1932.Plan p=QualityPixels1932.plan(stats,1600,33000000L,QualityPixels1932.LENS_FRONT,
            kind==2?0:1,noise,3,true,true,1);
        if(kind>=2)p=p.withFaceRegions(FACE);
        if(kind==3 || kind==4) {
            SpatialNoise1934 map=SpatialNoise1934.probe(new SpatialNoise1934.Patches() {
                public void read(int[] out,int xx,int yy,int pw,int ph) {
                    for(int y=0;y<ph;y++)System.arraycopy(image,(yy+y)*w+xx,out,y*pw,pw);
                }
            },w,h);
            p=p.withOutputNoise(map);
        }
        return p;
    }
    private static void compare(int[] source,int w,int h,int lo,int hi,int begin,int end,int noise,
            boolean shadows,int radius,int kind,int origin) {
        int[] original=source.clone(),a=source.clone(),b=source.clone();
        QualityPixels1932.Plan p=plan(source,w,h,noise,kind);
        QualityShadowReference1943.smoothRange(source,a,w,h,begin,end,lo,hi,noise,shadows,radius,p,origin);
        QualityShadow1932.smoothRange(source,b,w,h,begin,end,lo,hi,noise,shadows,radius,p,origin);
        if(!Arrays.equals(source,original))throw new AssertionError("source mutated");
        for(int i=0;i<a.length;i++) {
            if(a[i]!=b[i])throw new AssertionError("pixel mismatch case="+cases+" x="+(i%w)+" row="+(i/w)+
                " width="+w+" height="+h+" radius="+radius+" kind="+kind+" origin="+origin+" old="+Integer.toHexString(a[i])+" new="+Integer.toHexString(b[i]));
            if(a[i]!=source[i])changed++;
            if((source[i]>>>24)!=255 && b[i]!=source[i])throw new AssertionError("nonopaque changed");
            if((i/w<begin || i/w>=end) && b[i]!=source[i])throw new AssertionError("out-of-range changed");
            compared++;
        }
        cases++;
    }
    public static void main(String[] args) {
        Random random=new Random(19441943L);
        int[][] sizes={{1,1},{2,7},{7,3},{9,13},{17,37},{31,67},{64,75},{193,139}};
        for(int[] size:sizes)for(int mode=0;mode<6;mode++)for(int radius=1;radius<=4;radius++) {
            int w=size[0],h=size[1],lo=h>12?2:0,hi=h>12?h-2:h;
            int[] source=image(w,h,mode,random);
            int noise=1+(mode+radius)%4,kind=(mode+radius)%5;
            compare(source,w,h,lo,hi,lo,hi,noise,(mode&1)==0,radius,kind,mode==0?-5:19);
        }
        for(int round=0;round<220;round++) {
            int w=1+random.nextInt(70),h=1+random.nextInt(77),radius=1+random.nextInt(4);
            int lo=random.nextInt(h),hi=lo+1+random.nextInt(h-lo);
            int begin=lo+random.nextInt(hi-lo),end=begin+1+random.nextInt(hi-begin);
            compare(image(w,h,round%6,random),w,h,lo,hi,begin,end,1+random.nextInt(4),random.nextBoolean(),radius,round%5,round%3==0?-17:23);
        }
        compare(image(4080,37,1,random),4080,37,1,36,2,35,4,true,4,3,0);
        // Parallel ranges share only immutable source data; each cache owns its lease.
        final int w=97,h=143;final int[] source=image(w,h,1,random),out=source.clone(),ref=source.clone();
        final QualityPixels1932.Plan p=plan(source,w,h,4,3);
        QualityShadowReference1943.smoothRange(source,ref,w,h,0,h,0,h,4,true,4,p,0);
        Thread[] threads=new Thread[4];
        for(int i=0;i<threads.length;i++) {final int first=i*h/4,last=(i+1)*h/4;
            threads[i]=new Thread(new Runnable(){public void run(){QualityShadow1932.smoothRange(source,out,w,h,first,last,0,h,4,true,4,p,0);}});threads[i].start();}
        for(Thread t:threads)try{t.join();}catch(InterruptedException failure){throw new AssertionError(failure);}
        if(!Arrays.equals(out,ref))throw new AssertionError("parallel pixel mismatch");
        // Reuse retained arrays for a different image: no preceding shot data may survive.
        compare(image(97,143,4,random),97,143,0,143,0,143,4,true,4,1,0);
        System.out.println("{\"suite\":\"noise-cache1944\",\"native\":"+NativeSpeed1944.available()+",\"cases\":"+cases+
            ",\"comparedPixels\":"+compared+",\"changedPixels\":"+changed+",\"parallelRanges\":4,\"equal\":true}");
    }
}
