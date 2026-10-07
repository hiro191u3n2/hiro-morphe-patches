package com.hiro.ulike;

import android.graphics.Bitmap;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Frozen v1.9.34 comparison and ownership tests. Host fixture, not device timing. */
public final class ResizeSpeed1935Test {
    static int assertions,scenarios;
    static int originalReads,adaptiveReads;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static int[] pixels(int w,int h,boolean alpha) {
        Random random=new Random(w*719L+h*7133L);int[] p=new int[w*h];
        for(int i=0;i<p.length;i++)p[i]=alpha?random.nextInt():0xff000000|random.nextInt(0x1000000);
        return p;
    }
    static void same(int[] a,int[] b,String why) {
        check(a.length==b.length,why+" dimensions");
        for(int i=0;i<a.length;i++)check(a[i]==b[i],why+" pixel "+i);
    }
    static void bitExact() {
        final int[][] cases={{129,97,341,519},{257,513,49,73},{73,55,55,73},
            {73,55,31,19},{41,39,82,78},{17,13,1,1},{1,1,19,13}};
        for(int[] d:cases)for(int rotation:new int[]{0,90,180,270})
            for(boolean alpha:new boolean[]{false,true})for(boolean immutable:new boolean[]{false,true}) {
                int[] p=pixels(d[0],d[1],alpha);
                Bitmap input=immutable?Bitmap.fromImmutable(d[0],d[1],p):Bitmap.from(d[0],d[1],p);
                input.setDensity(420);
                Bitmap expected=FastResize1934Reference.resample(input,rotation,d[2],d[3]);
                Bitmap actual=FastResize1933.resample(input,rotation,d[2],d[3]);
                same(expected.pixels(),actual.pixels(),"v1934 "+rotation+" alpha="+alpha+" immutable="+immutable);
                same(p,input.pixels(),"input remains owned");
                check(actual.hasAlpha()==expected.hasAlpha()&&actual.getDensity()==expected.getDensity(),"metadata");
                if(!alpha&&actual!=input&&d[3]>16) {
                    check(actual.maxWriteRows>1,"S3 batched writes");
                    check(actual.writes<d[3],"S3 fewer bitmap calls");
                }
                if(expected!=input)expected.recycle();if(actual!=input)actual.recycle();input.recycle();scenarios++;
            }
    }
    static void adaptiveCache() {
        final int sw=319,sh=1021,w=43,h=79;final int[] p=pixels(sw,sh,false),old=new int[w*h],now=new int[w*h];
        final int[] reads={0,0};
        FastPixels1934Reference.resizeCrop(new FastPixels1934Reference.RowSource(){public void readRow(int y,int[] row){
            reads[0]++;System.arraycopy(p,y*sw,row,0,sw);}},sw,sh,new FastPixels1934Reference.RowSink(){
            public void writeRow(int y,int[] row){System.arraycopy(row,0,old,y*w,w);}},w,h,0,0,sw,sh);
        FastPixels1933.Plan plan=FastPixels1933.prepare(sw,sh,w,h,0,0,sw,sh);
        FastPixels1933.runRows(plan,new FastPixels1933.RowSource(){public void readRow(int y,int[] row){
            reads[1]++;System.arraycopy(p,y*sw,row,0,sw);}},new FastPixels1933.RowSink(){
            public void writeRow(int y,int[] row){System.arraycopy(row,0,now,y*w,w);}},0,h);
        same(old,now,"support cache exact");check(reads[1]<reads[0],"S4 fewer horizontal recalculations");
        check(reads[1]<=sh,"full support rows reused");check(plan.cacheRows>8,"support exceeds old cache");
        check(plan.cacheRows<=128&&(long)plan.cacheRows*w*12<=4L*1024*1024,"bounded support cache");
        originalReads=reads[0];adaptiveReads=reads[1];scenarios++;
        for(int capacity:new int[]{1,8,16}) {
            FastPixels1933.Plan limited=FastPixels1933.limitCache(plan,capacity);
            check(limited.cacheRows==capacity,"low-memory cache bound");
            FastPixels1933.runRows(limited,new FastPixels1933.RowSource(){public void readRow(int y,int[] row){
                System.arraycopy(p,y*sw,row,0,sw);}},new FastPixels1933.RowSink(){
                public void writeRow(int y,int[] row){System.arraycopy(row,0,now,y*w,w);}},0,h);
            same(old,now,"low-memory bounded cache exact");scenarios++;
        }
    }
    static void publicSafety() {
        final int[] a={0xff123456,0xff654321,0x40123456,0xffaabbcc};
        for(final boolean crop:new boolean[]{false,true}) {
            boolean rejected=false;
            try {FastPixels1933.resizeCrop(new FastPixels1933.RowSource(){public void readRow(int y,int[] row){
                System.arraycopy(a,y*2,row,0,2);}},2,2,new FastPixels1933.RowSink(){public void writeRow(int y,int[] row){}},
                crop?2:3,crop?2:3,0,0,2,2);
            } catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"public row API alpha validation "+crop);scenarios++;
        }
        Bitmap changing=Bitmap.from(41,39,pixels(41,39,false));
        // The first two reads form the full alpha scan; then the mutable source
        // changes before a filter row is read. Row validation must still run.
        changing.mutateAlphaAtRead=3;
        boolean rejected=false;
        try {FastResize1933.resample(changing,0,67,63);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"mutable input never receives opacity waiver");
        check(!changing.isRecycled(),"mutable failure does not recycle caller input");changing.recycle();scenarios++;
        Bitmap tiny=Bitmap.fromImmutable(1,1,new int[]{0xff123456});
        boolean bounded=false;
        try{FastResize1933.resample(tiny,0,Integer.MAX_VALUE,Integer.MAX_VALUE);}
        catch(OutOfMemoryError expected){bounded=true;}
        check(bounded&&!tiny.isRecycled(),"overflow-safe memory preflight");tiny.recycle();scenarios++;
    }
    static void concurrent() throws Exception {
        for(int round=0;round<4;round++) {
            final int[] pa=pixels(113+round,179,false),pb=pixels(97,153+round,false);
            final Bitmap a=Bitmap.fromImmutable(113+round,179,pa),b=Bitmap.from(97,153+round,pb);
            final Bitmap ea=FastResize1934Reference.resample(a,90,271,317),eb=FastResize1934Reference.resample(b,270,193,281);
            final Bitmap[] out=new Bitmap[2];final CountDownLatch start=new CountDownLatch(1);
            final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
            Thread[] threads=new Thread[2];
            for(int j=0;j<2;j++){final int i=j;threads[j]=new Thread(new Runnable(){public void run(){
                try{start.await();out[i]=i==0?FastResize1933.resample(a,90,271,317):FastResize1933.resample(b,270,193,281);}
                catch(Throwable error){failure.compareAndSet(null,error);}}});threads[j].start();}
            start.countDown();for(Thread thread:threads)thread.join();
            if(failure.get()!=null)throw new AssertionError("concurrent failure",failure.get());
            same(ea.pixels(),out[0].pixels(),"two saves A");same(eb.pixels(),out[1].pixels(),"two saves B");
            same(pa,a.pixels(),"two sources A");same(pb,b.pixels(),"two sources B");
            out[0].recycle();out[1].recycle();ea.recycle();eb.recycle();a.recycle();b.recycle();scenarios++;
        }
        final int[][] arrays=new int[8][];
        for(int i=0;i<arrays.length;i++){arrays[i]=SpeedWorkers1935.borrowInts(64);Arrays.fill(arrays[i],i);
            for(int j=0;j<i;j++)check(arrays[i]!=arrays[j],"exclusive lease");}
        for(int i=0;i<arrays.length;i++){for(int value:arrays[i])check(value==i,"lease data ownership");SpeedWorkers1935.release(arrays[i]);}
        check(SpeedWorkers1935.retainedBytes()<=24L*1024*1024,"aggregate retention bounded");
        SpeedWorkers1935.trim();check(SpeedWorkers1935.retainedBytes()==0,"pool trim releases retained references");scenarios++;
    }
    public static void main(String[] args)throws Exception {
        bitExact();adaptiveCache();publicSafety();concurrent();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+
            ",\"bit_identical_to_1934\":true,\"native_enabled\":"+NativeSpeed1935.available()+
            ",\"cache_reference_reads\":"+originalReads+",\"cache_adaptive_reads\":"+adaptiveReads+
            ",\"batched_writes_verified\":true,\"immutable_opacity_contract\":true,\"concurrent_ownership_verified\":true,\"device_tested\":false}");
    }
}
