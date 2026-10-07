package com.hiro.ulike;

import android.graphics.Bitmap;

/** One geometry pass: exact quarter-turn/crop when possible, otherwise one
 * high-quality crop+resize. Never re-renders or approximates the beauty mesh. */
public final class FastResize1934Reference {
    private static final long RESERVE=32L*1024*1024;
    private static final int COLUMNS=32;
    private FastResize1934Reference() { }

    public static Bitmap resample(final Bitmap bitmap,final int rotation,final int width,final int height) {
        if(bitmap==null || bitmap.isRecycled() || width<1 || height<1)
            throw new IllegalArgumentException("fast bitmap dimensions");
        final int sw=bitmap.getWidth(),sh=bitmap.getHeight();
        final boolean quarter=rotation==90 || rotation==270;
        final int rw=quarter?sh:sw,rh=quarter?sw:sh;
        if(rotation==0 && rw==width && rh==height)return bitmap;
        if(rotation!=0 && rotation!=90 && rotation!=180 && rotation!=270)
            return QualityPipeline1932.resample(bitmap,rotation,width,height);
        // A true alpha flag does not imply transparent pixels (SDK output often
        // retains it). Inspect the actual pixels; never infer opacity from samples.
        if(!opaque(bitmap))return QualityPipeline1932.resample(bitmap,rotation,width,height);
        final double scale=Math.max((double)width/rw,(double)height/rh);
        final double cw=Math.min((double)rw,width/scale),ch=Math.min((double)rh,height/scale);
        final double left=Math.max(0,(rw-cw)*0.5),top=Math.max(0,(rh-ch)*0.5);
        final boolean exactCrop=cw==width && ch==height && left==Math.rint(left) && top==Math.rint(top);
        long outputBytes=(long)width*height*4;
        long perWorker=(exactCrop?0:(long)width*3*4*11)+(long)width*4+(long)rw*4+
            (quarter?(long)Math.min(COLUMNS,sw)*sh*4:0);
        long axes=exactCrop?0:((long)width+height+sw+sh)*96;
        long budget=available()-RESERVE-outputBytes-axes;
        if(budget<perWorker)throw new OutOfMemoryError("fast resize workspace");
        int count=Math.min(4,Math.max(1,Runtime.getRuntime().availableProcessors()));
        count=Math.max(1,Math.min(count,Math.max(1,height/128)));
        count=Math.max(1,Math.min(count,(int)Math.min(4,budget/Math.max(1,perWorker))));
        final FastPixels1934Reference.Plan plan=FastPixels1934Reference.prepare(rw,rh,width,height,left,top,cw,ch);
        Worker[] workers=new Worker[count];
        Thread[] threads=new Thread[Math.max(0,count-1)];
        final Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        final Group group;
        try {group=new Group(bitmap,result);}
        catch(OutOfMemoryError failed) {result.recycle();throw failed;}
        int started=0;
        boolean interrupted=false;
        try {
            result.setDensity(bitmap.getDensity());
            result.setHasAlpha(bitmap.hasAlpha());
            // Workers own disjoint output rows and small caches, never a complete
            // rotated bitmap or an extra full-resolution source copy.
            for(int i=0;i<count;i++)workers[i]=new Worker(group,plan,rotation,
                height*i/count,height*(i+1)/count);
            for(int i=0;i<threads.length;i++) {
                threads[i]=new Thread(workers[i+1],"ULikeResize1933");
                threads[i].setDaemon(true);
                try {threads[i].start();started++;}
                catch(OutOfMemoryError noThread) {
                    // Run every unstarted partition on the caller. No rows can
                    // silently disappear when the VM cannot allocate a thread.
                    for(int j=i+1;j<count;j++)workers[j].run();
                    break;
                }
            }
            workers[0].run();
        } catch(Throwable failure) {
            group.fail(failure);
        } finally {
            for(int i=0;i<started;i++)while(threads[i].isAlive()) {
                try {threads[i].join();}
                catch(InterruptedException cancelled) {
                    interrupted=true;group.fail(cancelled);
                    for(int j=0;j<started;j++)threads[j].interrupt();
                }
            }
            if(interrupted)Thread.currentThread().interrupt();
        }
        if(group.failure!=null) {
            result.recycle();
            if(group.failure instanceof OutOfMemoryError)throw (OutOfMemoryError)group.failure;
            if(group.failure instanceof RuntimeException)throw (RuntimeException)group.failure;
            throw new IllegalStateException("fast resize failed",group.failure);
        }
        return result;
    }

    private static long available() {
        Runtime r=Runtime.getRuntime();return r.maxMemory()-(r.totalMemory()-r.freeMemory());
    }
    static boolean opaque(Bitmap bitmap) {
        if(!bitmap.hasAlpha())return true;
        final int width=bitmap.getWidth(),height=bitmap.getHeight();
        final int rows=Math.min(32,height);
        if((long)width*rows*4>available()-RESERVE)throw new OutOfMemoryError("alpha scan workspace");
        int[] data=new int[width*rows];
        for(int y=0;y<height;y+=rows) {
            if(Thread.currentThread().isInterrupted())throw new IllegalStateException("alpha scan interrupted");
            int n=Math.min(rows,height-y);
            bitmap.getPixels(data,0,width,0,y,width,n);
            int alpha=-1;for(int i=0;i<n*width;i++)alpha&=data[i];
            if((alpha>>>24)!=255)return false;
        }
        return true;
    }

    private static final class Group {
        final Bitmap source,destination;
        final Object readLock=new Object(),writeLock=new Object();
        volatile Throwable failure;
        Group(Bitmap source,Bitmap destination){this.source=source;this.destination=destination;}
        synchronized void fail(Throwable error){if(failure==null)failure=error;}
    }
    private static final class Worker implements Runnable {
        final Group group;final FastPixels1934Reference.Plan plan;final int rotation,begin,end;
        Worker(Group group,FastPixels1934Reference.Plan plan,int rotation,int begin,int end) {
            this.group=group;this.plan=plan;this.rotation=rotation;this.begin=begin;this.end=end;
        }
        public void run() {
            try {
                FastPixels1934Reference.runRows(plan,new Rows(group,rotation),new FastPixels1934Reference.RowSink(){
                    public void writeRow(int row,int[] pixels) {
                        if(group.failure!=null)throw new IllegalStateException("resize cancelled");
                        synchronized(group.writeLock) {
                            group.destination.setPixels(pixels,0,plan.width,0,row,plan.width,1);
                        }
                    }
                },begin,end);
            } catch(Throwable failure){group.fail(failure);}
        }
    }
    private static final class Rows implements FastPixels1934Reference.RowSource {
        final Group group;final Bitmap bitmap;final int rotation,width,height;
        final int[] block;int firstColumn=-1,columnCount;
        Rows(Group group,int rotation) {
            this.group=group;this.bitmap=group.source;this.rotation=rotation;
            width=bitmap.getWidth();height=bitmap.getHeight();
            block=rotation==90 || rotation==270?new int[Math.min(COLUMNS,width)*height]:null;
        }
        public void readRow(int row,int[] pixels) {
            if(group.failure!=null || Thread.currentThread().isInterrupted())
                throw new IllegalStateException("resize cancelled");
            if(block==null) {
                synchronized(group.readLock) {
                    bitmap.getPixels(pixels,0,width,0,rotation==180?height-1-row:row,width,1);
                }
                if(rotation==180)reverse(pixels,width);
                return;
            }
            int column=rotation==90?row:width-1-row;
            int first=column/COLUMNS*COLUMNS;
            if(firstColumn!=first) {
                columnCount=Math.min(COLUMNS,width-first);
                synchronized(group.readLock) {
                    bitmap.getPixels(block,0,columnCount,first,0,columnCount,height);
                }
                firstColumn=first;
            }
            int offset=column-firstColumn;
            if(rotation==90)for(int x=0;x<height;x++)pixels[x]=block[(height-1-x)*columnCount+offset];
            else for(int x=0;x<height;x++)pixels[x]=block[x*columnCount+offset];
        }
        private static void reverse(int[] pixels,int count) {
            for(int l=0,r=count-1;l<r;l++,r--){int p=pixels[l];pixels[l]=pixels[r];pixels[r]=p;}
        }
    }
}
