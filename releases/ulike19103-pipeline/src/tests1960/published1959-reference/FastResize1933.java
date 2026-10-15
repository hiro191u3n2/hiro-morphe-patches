package com.hiro.ulike;

import android.graphics.Bitmap;

/** One geometry pass: exact quarter-turn/crop when possible, otherwise one
 * high-quality crop+resize. Never re-renders or approximates the beauty mesh. */
public final class FastResize1933 {
    private static final long RESERVE=32L*1024*1024;
    private static final int COLUMNS=32;
    private static final int BATCH_ROWS=16;
    private FastResize1933() { }

    public static Bitmap resample(final Bitmap bitmap,final int rotation,final int width,final int height) {
        if(bitmap==null || bitmap.isRecycled() || width<1 || height<1)
            throw new IllegalArgumentException("fast bitmap dimensions");
        if((long)width*height>Integer.MAX_VALUE)
            throw new OutOfMemoryError("fast resize output dimensions");
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
        long axes=exactCrop?0:((long)width+height+sw+sh)*96;
        if(outputBytes>available()-RESERVE-axes)throw new OutOfMemoryError("fast resize output/axes");
        FastPixels1933.Plan prepared=FastPixels1933.prepare(rw,rh,width,height,left,top,cw,ch);
        // Retained pool arrays are already live heap usage in available(). New
        // allocations include the support-sized cache, batch sink and rotation
        // cache. Input ownership and any second queued photo are accounted by
        // the capture queue; this operation never allocates a full source copy.
        long budget=available()-RESERVE-outputBytes;
        long rotationBytes=(quarter?(long)Math.min(COLUMNS,sw)*sh*4:0)+2048;
        long minimum=FastPixels1933.workspaceBytes(FastPixels1933.limitCache(prepared,1))+rotationBytes;
        final int batchRows=(int)Math.max(1,Math.min(Math.min(BATCH_ROWS,height),
            (budget-minimum)/Math.max(1,(long)width*4)));
        long extra=(long)width*batchRows*4+rotationBytes;
        if(!exactCrop) {
            long fixed=FastPixels1933.workspaceBytes(prepared)-(long)width*12*prepared.cacheRows+extra;
            int affordable=(int)Math.min(128,Math.max(1,(budget-fixed)/Math.max(1,(long)width*12+64)));
            prepared=FastPixels1933.limitCache(prepared,affordable);
        }
        final FastPixels1933.Plan plan=prepared;
        long perWorker=FastPixels1933.workspaceBytes(plan)+extra+(long)plan.cacheRows*64;
        if(budget<perWorker)throw new OutOfMemoryError("fast resize workspace");
        int count=Math.min(4,Math.max(1,Runtime.getRuntime().availableProcessors()));
        count=Math.max(1,Math.min(count,Math.max(1,height/128)));
        count=Math.max(1,Math.min(count,(int)Math.min(4,budget/Math.max(1,perWorker))));
        Worker[] workers=new Worker[count];
        final Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        final Group group;
        try {group=new Group(bitmap,result,!bitmap.isMutable(),batchRows);}
        catch(OutOfMemoryError failed) {result.recycle();throw failed;}
        try {
            result.setDensity(bitmap.getDensity());
            result.setHasAlpha(bitmap.hasAlpha());
            // Shared workers own disjoint output rows and exclusive leased
            // caches. run() waits for every submitted worker even on failure.
            for(int i=0;i<count;i++)workers[i]=new Worker(group,plan,rotation,
                height*i/count,height*(i+1)/count);
            SpeedWorkers1935.run(workers);
        } catch(Throwable failure) {
            group.fail(failure);
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
        int[] data=SpeedWorkers1935.borrowInts(width*rows);
        try {
            for(int y=0;y<height;y+=rows) {
                if(Thread.currentThread().isInterrupted())throw new IllegalStateException("alpha scan interrupted");
                int n=Math.min(rows,height-y);
                bitmap.getPixels(data,0,width,0,y,width,n);
                int alpha=-1;for(int i=0;i<n*width;i++)alpha&=data[i];
                if((alpha>>>24)!=255)return false;
            }
            return true;
        } finally { SpeedWorkers1935.release(data); }
    }

    private static final class Group {
        final Bitmap source,destination;final boolean provenOpaque;final int batchRows;
        final Object readLock=new Object(),writeLock=new Object();
        volatile Throwable failure;
        Group(Bitmap source,Bitmap destination,boolean provenOpaque,int batchRows){this.source=source;this.destination=destination;this.provenOpaque=provenOpaque;this.batchRows=batchRows;}
        synchronized void fail(Throwable error){if(failure==null)failure=error;}
    }
    private static final class Worker implements Runnable {
        final Group group;final FastPixels1933.Plan plan;final int rotation,begin,end;
        Worker(Group group,FastPixels1933.Plan plan,int rotation,int begin,int end) {
            this.group=group;this.plan=plan;this.rotation=rotation;this.begin=begin;this.end=end;
        }
        public void run() {
            Rows source=null;Batch sink=null;
            try {
                source=new Rows(group,rotation);
                sink=new Batch(group,plan.width,Math.min(group.batchRows,end-begin));
                FastPixels1933.runRows(plan,source,sink,begin,end,group.provenOpaque);
                sink.flush();
            } catch(Throwable failure){group.fail(failure);}
            finally { if(source!=null)source.close();if(sink!=null)sink.close(); }
        }
    }
    private static final class Batch implements FastPixels1933.RowSink {
        final Group group;final int width,capacity;int[] pixels;int first=-1,count;
        Batch(Group group,int width,int capacity) {
            this.group=group;this.width=width;this.capacity=Math.max(1,capacity);
            pixels=SpeedWorkers1935.borrowInts(width*this.capacity);
        }
        public void writeRow(int row,int[] data) {
            if(group.failure!=null || Thread.currentThread().isInterrupted())
                throw new IllegalStateException("resize cancelled");
            if(count==0)first=row;
            if(row!=first+count)throw new IllegalStateException("resize row sequence");
            System.arraycopy(data,0,pixels,count*width,width);count++;
            if(count==capacity)flush();
        }
        void flush() {
            if(count==0)return;
            if(group.failure!=null || Thread.currentThread().isInterrupted())
                throw new IllegalStateException("resize cancelled");
            synchronized(group.writeLock) {
                if(group.failure!=null)throw new IllegalStateException("resize cancelled");
                group.destination.setPixels(pixels,0,width,0,first,width,count);
            }
            count=0;
        }
        void close(){SpeedWorkers1935.release(pixels);pixels=null;}
    }
    private static final class Rows implements FastPixels1933.RowSource {
        final Group group;final Bitmap bitmap;final int rotation,width,height;
        final int[] block;int firstColumn=-1,columnCount;
        Rows(Group group,int rotation) {
            this.group=group;this.bitmap=group.source;this.rotation=rotation;
            width=bitmap.getWidth();height=bitmap.getHeight();
            block=rotation==90 || rotation==270?SpeedWorkers1935.borrowInts(Math.min(COLUMNS,width)*height):null;
        }
        void close(){SpeedWorkers1935.release(block);}
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
