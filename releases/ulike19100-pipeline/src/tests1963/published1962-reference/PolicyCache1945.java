package com.hiro.ulike;

/**
 * One worker's optional, exact coordinate-policy lease. Only immutable shot-map
 * sigma and geometric masks are shared across primary and residual NR; source
 * RGB, processed neighbourhood ranges and filter results are never cached here.
 * A prefix of a strip is bounded to 2 MiB. Outside that prefix the same original
 * functions run, so large strips and low-memory devices retain exact behaviour.
 */
final class PolicyCache1945 implements AutoCloseable {
    private static final long MAX_BYTES=2L*1024*1024;
    private static final long RESERVE=64L*1024*1024;
    final QualityPixels1932.Plan owner;
    private final int width,first,rows;
    private final int[] values;
    private final byte[] ready;
    private final Thread worker=Thread.currentThread();
    private boolean closed;

    private PolicyCache1945(QualityPixels1932.Plan owner,int width,int first,int rows) {
        this.owner=owner;this.width=width;this.first=first;this.rows=rows;
        ready=new byte[rows];
        values=SpeedWorkers1935.borrowInts(width*rows*3);
    }

    static PolicyCache1945 borrow(QualityPixels1932.Plan plan,int width,int first,int limit) {
        if(plan==null || width<1 || first>=limit ||
                (plan.localNoise==null && plan.faceRegions==null))return null;
        // Include per-row validity bytes and a conservative object/header margin.
        long rowBytes=(long)width*12+1;
        int rows=(int)Math.min((long)limit-first,(MAX_BYTES-256)/rowBytes);
        if(rows<1)return null;
        Runtime runtime=Runtime.getRuntime();
        long available=runtime.maxMemory()-(runtime.totalMemory()-runtime.freeMemory());
        if(rowBytes*rows+256>available-RESERVE)return null;
        try{return new PolicyCache1945(plan,width,first,rows);}
        catch(OutOfMemoryError optionalWorkspace){SpeedWorkers1935.trim();return null;}
    }

    private int at(int x,int y,int component) {
        if(closed || Thread.currentThread()!=worker || x<0 || x>=width ||
                y<first || (long)y-first>=rows)return -1;
        int row=y-first;
        int flag=1<<component;
        if((ready[row]&flag)==0) {
            int base=row*width*3;
            // Every element is overwritten before this row is published ready.
            // Neither a previous lease nor a different shot's metadata is read.
            for(int col=0;col<width;col++) {
                int index=base+col*3;
                if(component==0) {
                    float sigma=owner.localNoise==null?owner.sourceSigma:owner.localNoise.sigmaAt(col,y);
                    values[index]=Float.floatToRawIntBits(sigma);
                } else if(component==1) {
                    values[index+1]=owner.faceRegions==null?0:owner.faceRegions.skinQ8(col,y);
                } else {
                    values[index+2]=owner.faceRegions==null?0:owner.faceRegions.detailQ8(col,y);
                }
            }
            ready[row]|=flag;
        }
        return (row*width+x)*3;
    }

    float sigmaAt(int x,int y) {
        int at=at(x,y,0);
        return at<0?(owner.localNoise==null?owner.sourceSigma:owner.localNoise.sigmaAt(x,y))
            :Float.intBitsToFloat(values[at]);
    }
    int skinQ8(int x,int y) {
        int at=at(x,y,1);
        return at<0?(owner.faceRegions==null?0:owner.faceRegions.skinQ8(x,y)):values[at+1];
    }
    int detailQ8(int x,int y) {
        int at=at(x,y,2);
        return at<0?(owner.faceRegions==null?0:owner.faceRegions.detailQ8(x,y)):values[at+2];
    }
    public void close() {
        if(closed)return;
        closed=true;SpeedWorkers1935.release(values);
    }
}
