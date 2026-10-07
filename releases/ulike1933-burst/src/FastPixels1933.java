package com.hiro.ulike;

/**
 * Bit-identical opaque-photo resampling with three channels instead of four.
 * FastResize1933 verifies every source alpha before using this path. Transparent
 * inputs retain the original premultiplied-alpha resampler without approximation.
 * Kernel coefficients, floating-point accumulation order and extrema clamps are
 * identical to QualityPixels1932; a Plan is immutable and shareable across rows.
 */
public final class FastPixels1933 {
    private FastPixels1933() { }
    public interface RowSource { void readRow(int row, int[] pixels); }
    public interface RowSink { void writeRow(int row, int[] pixels); }

    public static final class Plan {
        final int sourceWidth, sourceHeight, width, height;
        final Axis horizontal, vertical;
        final boolean identity;
        private Plan(int sw,int sh,int w,int h,double left,double top,double cw,double ch) {
            if(sw<1 || sh<1 || w<1 || h<1 || w>Integer.MAX_VALUE/3 ||
               !Double.isFinite(left) || !Double.isFinite(top) || !Double.isFinite(cw) ||
               !Double.isFinite(ch) || left<0 || top<0 || cw<=0 || ch<=0 ||
               left+cw>sw+0.000001 || top+ch>sh+0.000001)
                throw new IllegalArgumentException("fast resize crop");
            sourceWidth=sw;sourceHeight=sh;width=w;height=h;
            identity=left==0 && top==0 && cw==sw && ch==sh && w==sw && h==sh;
            horizontal=identity?null:new Axis(sw,w,left,cw);
            vertical=identity?null:new Axis(sh,h,top,ch);
        }
    }

    public static Plan prepare(int sw,int sh,int w,int h,double left,double top,double cw,double ch) {
        return new Plan(sw,sh,w,h,left,top,cw,ch);
    }
    public static void resizeCrop(RowSource source,int sw,int sh,RowSink sink,int w,int h,
                                  double left,double top,double cw,double ch) {
        runRows(prepare(sw,sh,w,h,left,top,cw,ch),source,sink,0,h);
    }
    public static void runRows(Plan plan,RowSource source,RowSink sink,int begin,int end) {
        if(plan==null || source==null || sink==null || begin<0 || end<begin || end>plan.height)
            throw new IllegalArgumentException("fast resize rows");
        final int width=plan.width;
        int[] out=new int[width];
        if(plan.identity) {
            for(int y=begin;y<end;y++) { checkInterrupted(y);source.readRow(y,out);checkOpaque(out);sink.writeRow(y,out); }
            return;
        }
        RowCache cache=new RowCache(source,plan.sourceWidth,width,plan.horizontal);
        float[] accum=new float[width*3], min=new float[width*3], max=new float[width*3];
        for(int y=begin;y<end;y++) {
            checkInterrupted(y);
            java.util.Arrays.fill(accum,0f);
            java.util.Arrays.fill(min,Float.POSITIVE_INFINITY);
            java.util.Arrays.fill(max,Float.NEGATIVE_INFINITY);
            for(int t=plan.vertical.offset[y];t<plan.vertical.offset[y+1];t++) {
                float weight=plan.vertical.weights[t];
                float[] row=cache.get(plan.vertical.indices[t]);
                for(int i=0;i<accum.length;i++) {
                    float value=row[i];accum[i]+=value*weight;
                    if(value<min[i])min[i]=value;
                    if(value>max[i])max[i]=value;
                }
            }
            for(int x=0;x<width;x++) {
                int i=x*3;
                int r=clamp(Math.round(clampFloat(accum[i],min[i],max[i])),0,255);
                int g=clamp(Math.round(clampFloat(accum[i+1],min[i+1],max[i+1])),0,255);
                int b=clamp(Math.round(clampFloat(accum[i+2],min[i+2],max[i+2])),0,255);
                out[x]=0xff000000|(r<<16)|(g<<8)|b;
            }
            sink.writeRow(y,out);
        }
    }
    private static void checkInterrupted(int row) {
        if((row&7)==0 && Thread.currentThread().isInterrupted())
            throw new IllegalStateException("fast resize interrupted");
    }
    private static void checkOpaque(int[] row) {
        int mask=-1;for(int p:row)mask&=p;
        if((mask>>>24)!=255)throw new IllegalArgumentException("opaque photo required");
    }

    private static final class Axis {
        final int[] offset;
        final int[] indices;
        final float[] weights;
        final boolean reduce;
        Axis(int source, int target, double start, double extent) {
            double ratio = extent / target;
            reduce = ratio > 1.0000001;
            double radius = 2.0 * Math.max(1.0, ratio);
            offset = new int[target + 1];
            long total = 0;
            for (int x = 0; x < target; x++) {
                double center = start + (x + 0.5) * ratio - 0.5;
                int first = Math.max(0, (int)Math.ceil(center - radius));
                int last = Math.min(source - 1, (int)Math.floor(center + radius));
                total += Math.max(1, last - first + 1);
                if (total > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("quality axis size");
                offset[x+1] = (int)total;
            }
            indices = new int[(int)total];
            weights = new float[(int)total];
            for (int x = 0; x < target; x++) {
                double center = start + (x + 0.5) * ratio - 0.5;
                int first = Math.max(0, (int)Math.ceil(center - radius));
                double sum = 0.0;
                for (int i = offset[x]; i < offset[x+1]; i++) {
                    int index = Math.min(source - 1, first + i - offset[x]);
                    indices[i] = index;
                    double distance = (index - center) / (reduce ? ratio : 1.0);
                    double value = reduce ? lanczos2(distance) : cubic(distance);
                    weights[i] = (float)value;
                    sum += value;
                }
                if (Math.abs(sum) < 0.000000001) {
                    java.util.Arrays.fill(weights, offset[x], offset[x+1], 0f);
                    weights[offset[x]] = 1f;
                } else {
                    for (int i = offset[x]; i < offset[x+1]; i++) weights[i] /= (float)sum;
                }
            }
        }
    }

    private static final class RowCache {
        final RowSource source;final Axis horizontal;final int width;
        final int[] raw, rowIds=new int[8];final long[] used=new long[8];
        final float[][] rows;long clock;
        RowCache(RowSource source,int sourceWidth,int width,Axis horizontal) {
            this.source=source;this.horizontal=horizontal;this.width=width;
            raw=new int[sourceWidth];rows=new float[8][width*3];
            java.util.Arrays.fill(rowIds,-1);
        }
        float[] get(int row) {
            for(int i=0;i<rowIds.length;i++)if(rowIds[i]==row){used[i]=++clock;return rows[i];}
            int slot=0;for(int i=1;i<rowIds.length;i++)if(used[i]<used[slot])slot=i;
            source.readRow(row,raw);checkOpaque(raw);
            float[] out=rows[slot];
            for(int x=0;x<width;x++) {
                float r=0f,g=0f,b=0f;
                float minR=Float.POSITIVE_INFINITY,minG=minR,minB=minR;
                float maxR=Float.NEGATIVE_INFINITY,maxG=maxR,maxB=maxR;
                for(int t=horizontal.offset[x];t<horizontal.offset[x+1];t++) {
                    int p=raw[horizontal.indices[t]];
                    float pr=(p>>>16)&255,pg=(p>>>8)&255,pb=p&255;
                    float weight=horizontal.weights[t];
                    r+=pr*weight;g+=pg*weight;b+=pb*weight;
                    minR=Math.min(minR,pr);maxR=Math.max(maxR,pr);
                    minG=Math.min(minG,pg);maxG=Math.max(maxG,pg);
                    minB=Math.min(minB,pb);maxB=Math.max(maxB,pb);
                }
                int i=x*3;
                out[i]=clampFloat(r,minR,maxR);out[i+1]=clampFloat(g,minG,maxG);out[i+2]=clampFloat(b,minB,maxB);
            }
            rowIds[slot]=row;used[slot]=++clock;return out;
        }
    }
    private static double cubic(double x) {
        x = Math.abs(x);
        if (x < 1.0) return ((1.5*x - 2.5)*x)*x + 1.0;
        if (x < 2.0) return ((-0.5*x + 2.5)*x - 4.0)*x + 2.0;
        return 0.0;
    }
    private static double lanczos2(double x) {
        x = Math.abs(x);
        if (x < 0.00000001) return 1.0;
        if (x >= 2.0) return 0.0;
        double p = Math.PI * x;
        return Math.sin(p) * Math.sin(p/2.0) * 2.0 / (p*p);
    }
    private static int clamp(int x,int lo,int hi){return Math.max(lo,Math.min(hi,x));}
    private static float clampFloat(float x,float lo,float hi){return Math.max(lo,Math.min(hi,x));}
}
