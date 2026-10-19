package com.hiro.ulike;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/** Full pixel equality against the separately copied published v1944 arithmetic. */
public final class PolicyCache1945Test {
    private static final AtomicLong pixels=new AtomicLong(),floatBits=new AtomicLong();
    private static final AtomicInteger cases=new AtomicInteger();
    private static final class Face implements QualityPixels1932.RegionMask {
        final int rotation,width,height;
        int skinCalls,detailCalls;
        Face(int rotation,int width,int height){this.rotation=rotation;this.width=width;this.height=height;}
        private int coordinate(int x,int y) {
            int a=x,b=y;
            if(rotation==90){a=y;b=width-1-x;}
            else if(rotation==180){a=width-1-x;b=height-1-y;}
            else if(rotation==270){a=height-1-y;b=x;}
            return a*71+b*43;
        }
        public int skinQ8(int x,int y){skinCalls++;return Math.floorMod(coordinate(x,y),257);}
        public int detailQ8(int x,int y){detailCalls++;return Math.floorMod(coordinate(x,y)*13,293)-17;}
    }
    private static int[] image(int w,int h,int mode,Random random) {
        int[] out=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int r,g,b;
            if(mode==0){r=random.nextInt(256);g=random.nextInt(256);b=random.nextInt(256);}
            else if(mode==1){r=22+random.nextInt(13);g=27+random.nextInt(13);b=29+random.nextInt(13);}
            else if(mode==2){r=(x+y)%4<2?65:104;g=r+2;b=r+4;}
            else if(mode==3){r=x<w/2?12:215;g=r;b=r;}
            else if(mode==4){r=x*250/Math.max(1,w-1);g=r;b=r;}
            else {r=255;g=3;b=128;}
            int alpha=mode==0 && random.nextInt(13)==0?random.nextInt(255):255;
            out[y*w+x]=(alpha<<24)|(r<<16)|(g<<8)|b;
        }
        return out;
    }
    private static SpatialNoise1934 map(final int[] image,final int w,final int h) {
        return SpatialNoise1934.probe(new SpatialNoise1934.Patches() {
            public void read(int[] out,int x,int y,int pw,int ph) {
                for(int row=0;row<ph;row++)System.arraycopy(image,(y+row)*w+x,out,row*pw,pw);
            }
        },w,h);
    }
    private static void equal(int[] a,int[] b,String label) {
        for(int i=0;i<a.length;i++) {
            if(a[i]!=b[i])throw new AssertionError(label+" mismatch case="+cases+" at="+i+
                " old="+Integer.toHexString(a[i])+" new="+Integer.toHexString(b[i]));
            pixels.incrementAndGet();
        }
    }
    private static void sameFloat(float a,float b) {
        if(Float.floatToRawIntBits(a)!=Float.floatToRawIntBits(b))throw new AssertionError("sigma bits changed");
        floatBits.incrementAndGet();
    }
    private static void compare(int w,int h,int mode,int rotation,int noise,int origin,Random random) {
        int[] source=image(w,h,mode,random),original=source.clone();
        SpatialNoise1934 local=map(source,w,h);
        Face face=new Face(rotation,w,h);
        float scale=mode%3==0?.5f:mode%3==1?1f:2f;
        QualityPixels1932.Plan plan=QualityPixels1932.plan(local.global,1600,33000000L,
            QualityPixels1932.LENS_FRONT,mode%2==0?1f:0f,noise,4,true,true,scale)
            .withLocalNoise(local,noise).withFaceRegions(mode==5?null:face);
        int lo=h>10?2:0,hi=h>10?h-2:h;
        PolicyCache1945 cache=PolicyCache1945.borrow(plan,w,origin+lo,origin+hi);
        if(cache==null)throw new AssertionError("expected available policy lease");
        QualityPixels1932.Plan scoped=plan.withPolicyCache(cache);
        try {
            // Compare exact metadata, including negative origins and cache misses.
            for(int y=-1;y<=h;y++)for(int x=-1;x<=w;x++) {
                sameFloat(scoped.localSigmaAt(x,y+origin),local.sigmaAt(x,y+origin));
                if(scoped.localNoiseBudgetAt(x,y+origin)!=local.budgetQ8(x,y+origin))
                    throw new AssertionError("noise budget changed");
                int skin=plan.faceRegions==null?0:face.skinQ8(x,y+origin);
                int detail=plan.faceRegions==null?0:face.detailQ8(x,y+origin);
                if(scoped.skinAt(x,y+origin)!=skin || scoped.detailAt(x,y+origin)!=detail)
                    throw new AssertionError("face mask changed");
            }
            int[] filtered=image(w,h,(mode+1)%6,random),a=filtered.clone(),b=filtered.clone();
            QualityPixelsReference1944.localDenoiseMix(source,a,w,h,lo,hi,origin,plan);
            QualityPixels1932.localDenoiseMix(source,b,w,h,lo,hi,origin,scoped);
            equal(a,b,"primary NR");
            // The processed neighbourhood differs from primary source RGB. Only
            // unchanged coordinate evidence, never local ranges, is reused.
            int[] processed=a.clone(),old=processed.clone(),actual=processed.clone();
            QualityShadowReference1944.smoothRange(processed,old,w,h,lo,hi,lo,hi,noise,true,4,plan,origin);
            QualityShadow1932.smoothRange(processed,actual,w,h,lo,hi,lo,hi,noise,true,4,scoped,origin);
            equal(old,actual,"residual NR");
            int[] sharpOld=old.clone(),sharpActual=actual.clone();
            QualityPixelsReference1944.finishStripAt(old,sharpOld,w,h,lo,hi,plan,true,true,origin);
            QualityPixels1932.finishStripAt(actual,sharpActual,w,h,lo,hi,scoped,true,true,origin);
            equal(sharpOld,sharpActual,"final sharpening");
            // Geometry and output-noise changes create new policies; a previous
            // coordinate cache must not survive either public copy operation.
            SpatialNoise1934 changed=map(image(w,h,(mode+3)%6,random),w,h);
            QualityPixels1932.Plan output=scoped.withOutputNoise(changed);
            Face rotated=new Face((rotation+90)%360,w,h);
            QualityPixels1932.Plan moved=scoped.withFaceRegions(rotated);
            for(int y=lo;y<hi;y++)for(int x=0;x<w;x++) {
                sameFloat(output.localSigmaAt(x,y+origin),changed.sigmaAt(x,y+origin));
                if(moved.skinAt(x,y+origin)!=rotated.skinQ8(x,y+origin) ||
                    moved.detailAt(x,y+origin)!=rotated.detailQ8(x,y+origin))
                    throw new AssertionError("geometry cache leaked");
            }
            if(!Arrays.equals(source,original))throw new AssertionError("source mutated");
        } finally {cache.close();cache.close();}
        sameFloat(scoped.localSigmaAt(0,lo+origin),local.sigmaAt(0,lo+origin));
        cases.incrementAndGet();
    }
    private static void sharing() {
        int w=37,h=29;int[] image=image(w,h,1,new Random(45));SpatialNoise1934 map=map(image,w,h);
        Face face=new Face(90,w,h);
        QualityPixels1932.Plan plan=QualityPixels1932.plan(map.global,800,10000000L,1,1,4,4,true,true,1)
            .withLocalNoise(map,4).withFaceRegions(face);
        PolicyCache1945 cache=PolicyCache1945.borrow(plan,w,0,h);
        try {
            QualityPixels1932.Plan scoped=plan.withPolicyCache(cache);
            for(int round=0;round<4;round++)for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                scoped.skinAt(x,y);scoped.detailAt(x,y);scoped.localNoiseBudgetAt(x,y);scoped.localSigmaAt(x,y);
            }
            if(face.skinCalls!=w*h || face.detailCalls!=w*h)throw new AssertionError("metadata recomputed");
            boolean rejected=false;
            try{plan.withFaceRegions(new Face(180,w,h)).withPolicyCache(cache);}
            catch(IllegalArgumentException wrongOwner){rejected=true;}
            if(!rejected)throw new AssertionError("another plan accepted policy ownership");
            final QualityPixels1932.Plan nestedPlan=scoped;
            final int expected=scoped.skinAt(0,0),before=face.skinCalls;
            final Throwable[] failure=new Throwable[1];
            Thread nested=new Thread(new Runnable(){public void run(){
                try{if(nestedPlan.skinAt(0,0)!=expected)throw new AssertionError("nested mask changed");}
                catch(Throwable error){failure[0]=error;}
            }});
            nested.start();
            try{nested.join();}catch(InterruptedException interrupted){throw new AssertionError(interrupted);}
            if(failure[0]!=null || face.skinCalls!=before+1)
                throw new AssertionError("mutable lease shared by nested worker",failure[0]);
        } finally {cache.close();}
        // Reusing pooled storage on the next shot must replace every value.
        Face next=new Face(180,w,h);
        QualityPixels1932.Plan nextPlan=plan.withFaceRegions(next);
        cache=PolicyCache1945.borrow(nextPlan,w,0,h);
        try {
            QualityPixels1932.Plan scoped=nextPlan.withPolicyCache(cache);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)
                if(scoped.skinAt(x,y)!=next.skinQ8(x,y))throw new AssertionError("preceding shot survived");
        } finally {cache.close();}
        if(PolicyCache1945.borrow(plan,Integer.MAX_VALUE,0,1)!=null)throw new AssertionError("unbounded lease");
        // Actual pooled array lengths obey the stated per-worker pixel-byte cap.
        try {
            java.lang.reflect.Field values=PolicyCache1945.class.getDeclaredField("values");
            java.lang.reflect.Field validity=PolicyCache1945.class.getDeclaredField("ready");
            values.setAccessible(true);
            validity.setAccessible(true);
            cache=PolicyCache1945.borrow(plan,4080,0,512);
            if(cache==null || ((int[])values.get(cache)).length*4L+
                    ((byte[])validity.get(cache)).length+256>2L*1024*1024)
                throw new AssertionError("policy cache exceeds byte cap");
            cache.close();
        } catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static void allocationFallback() {
        int w=37,h=29;int[] source=image(w,h,1,new Random(145));SpatialNoise1934 local=map(source,w,h);
        QualityPixels1932.Plan plan=QualityPixels1932.plan(local.global,800,10000000L,1,1,4,4,true,true,1)
            .withLocalNoise(local,4).withFaceRegions(new Face(90,w,h));
        if(PolicyCache1945.borrow(plan,w,0,h)!=null)throw new AssertionError("fault did not reject optional cache");
        int[] a=source.clone(),b=source.clone();
        QualityShadowReference1944.smoothRange(source,a,w,h,0,h,0,h,4,true,4,plan,0);
        QualityShadow1932.smoothRange(source,b,w,h,0,h,0,h,4,true,4,plan,0);
        equal(a,b,"allocation fallback");
        cases.incrementAndGet();
    }
    public static void main(String[] args) throws Exception {
        if(args.length>0 && ("oom".equals(args[0]) || "lowmemory".equals(args[0])))allocationFallback();
        else {
        sharing();Random random=new Random(19451944L);
        int[][] sizes={{1,1},{7,9},{17,23},{65,83},{137,75}};
        for(int[] size:sizes)for(int mode=0;mode<6;mode++)for(int rotation=0;rotation<360;rotation+=90)
            compare(size[0],size[1],mode,rotation,1+(mode+rotation/90)%4,mode==0?-7:19,random);
        compare(4080,57,1,270,4,0,random);
        Thread[] workers=new Thread[4];final Throwable[] failures=new Throwable[4];
        for(int i=0;i<workers.length;i++){final int index=i;workers[i]=new Thread(new Runnable(){public void run(){
            try{compare(83,97,index,90*index,4,index*41,new Random(index));}catch(Throwable failure){failures[index]=failure;}
        }});workers[i].start();}
        for(Thread worker:workers)worker.join();
        for(Throwable failure:failures)if(failure!=null)throw new AssertionError("parallel policy",failure);
        }
        System.out.println("{\"suite\":\"policy-cache1945\",\"cases\":"+cases.get()+",\"comparedPixels\":"+pixels.get()+
            ",\"sigmaBitComparisons\":"+floatBits.get()+",\"native\":"+NativeSpeed1944.available()+
            ",\"fourMaskQueriesReducedToOne\":"+(args.length==0)+
            ",\"allocationFallbackVerified\":"+(args.length>0)+",\"equal\":true}");
    }
}
