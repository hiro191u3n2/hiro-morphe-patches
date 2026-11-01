package com.hiro.ulike;

/** Small immutable spatial noise map. Measurements are made on unscaled RGB patches;
 * bilinear interpolation has no tile-boundary jumps. Values attenuate a user-selected
 * NR level, never increase it. No colour or brightness correction occurs here. */
public final class SpatialNoise1934 {
    public interface Patches { void read(int[] pixels,int x,int y,int width,int height); }
    public final int width,height,columns,rows;
    public final QualityPixels1932.NoiseStats global;
    private final float[] sigma;
    private final float left,top,stepX,stepY;
    // H24: only coordinate geometry is cached. The four sigma samples and the
    // original float multiply/add order remain unchanged for every pixel.
    // Bound optional retained storage even for unusual caller dimensions.
    private static final int MAX_AXIS_COORDINATES = 32768;
    private final Axis xAxis,yAxis;
    private SpatialNoise1934(int width,int height,int columns,int rows,float left,float top,
            float stepX,float stepY,float[] sigma,QualityPixels1932.NoiseStats global) {
        this.width=width;this.height=height;this.columns=columns;this.rows=rows;
        this.left=left;this.top=top;this.stepX=stepX;this.stepY=stepY;
        this.sigma=sigma;this.global=global;
        Axis x=null,y=null;
        if(width>0 && height>0 && (long)width+height<=MAX_AXIS_COORDINATES) {
            try {
                x=new Axis(width,columns,left,stepX);
                y=new Axis(height,rows,top,stepY);
            } catch(OutOfMemoryError optionalCoordinates) { x=null;y=null; }
        }
        xAxis=x;yAxis=y;
    }
    public static SpatialNoise1934 probe(Patches source,int width,int height) {
        return GpuAnalysis1961.spatial(source,width,height);
    }
    /** Original traversal is the independent CPU admission oracle. */
    static SpatialNoise1934 probeCpu1961(Patches source,int width,int height) {
        if(source==null || width<1 || height<1)throw new IllegalArgumentException("noise map source");
        int pw=Math.min(64,width),ph=Math.min(64,height);
        int columns=Math.max(1,Math.min(13,(width+127)/128));
        int rows=Math.max(1,Math.min(13,(height+127)/128));
        float[] sigma=new float[columns*rows];
        int[] patch=new int[pw*ph];
        QualityPixels1932.NoiseProbe aggregate=new QualityPixels1932.NoiseProbe();
        float sumY=0,sumC=0,mean=0,detail=0;int valid=0;
        for(int gy=0;gy<rows;gy++)for(int gx=0;gx<columns;gx++) {
            int x=columns==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(columns-1));
            int y=rows==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(rows-1));
            source.read(patch,x,y,pw,ph);
            QualityPixels1932.NoiseStats s=QualityPixels1932.estimate(patch,pw,ph);
            sigma[gy*columns+gx]=s.samples>=48?Math.max(s.lumaSigma,s.chromaSigma*.42f):Float.NaN;
            if(s.samples>=48) {sumY+=s.lumaSigma;sumC+=s.chromaSigma;mean+=s.meanLuma;detail+=s.detailFraction;valid++;}
            aggregate.add(patch,pw,ph);
        }
        QualityPixels1932.NoiseStats fallback=aggregate.finish();
        QualityPixels1932.NoiseStats overall=valid==0?fallback:new QualityPixels1932.NoiseStats(
            sumY/valid,sumC/valid,mean/valid,detail/valid,valid*48);
        float baseline=Math.max(overall.lumaSigma,overall.chromaSigma*.42f);
        for(int i=0;i<sigma.length;i++)if(Float.isNaN(sigma[i]))sigma[i]=baseline;
        return new SpatialNoise1934(width,height,columns,rows,(pw-1)*.5f,(ph-1)*.5f,
            columns==1?1f:(float)(width-pw)/(columns-1),rows==1?1f:(float)(height-ph)/(rows-1),sigma,overall);
    }
    static SpatialNoise1934 fromGpu1961(int width,int height,int columns,int rows,
            float[] sigma,QualityPixels1932.NoiseStats global) {
        int pw=Math.min(64,width),ph=Math.min(64,height);
        return new SpatialNoise1934(width,height,columns,rows,(pw-1)*.5f,(ph-1)*.5f,
            columns==1?1f:(float)(width-pw)/(columns-1),rows==1?1f:(float)(height-ph)/(rows-1),sigma,global);
    }
    static boolean same1961(SpatialNoise1934 a,SpatialNoise1934 b) {
        if(a==null||b==null||a.width!=b.width||a.height!=b.height||a.columns!=b.columns||a.rows!=b.rows||
                a.sigma.length!=b.sigma.length||a.global.samples!=b.global.samples)return false;
        for(int i=0;i<a.sigma.length;i++)if(Float.floatToRawIntBits(a.sigma[i])!=Float.floatToRawIntBits(b.sigma[i]))return false;
        return bits1961(a.global.lumaSigma,b.global.lumaSigma)&&bits1961(a.global.chromaSigma,b.global.chromaSigma)&&
            bits1961(a.global.meanLuma,b.global.meanLuma)&&bits1961(a.global.detailFraction,b.global.detailFraction);
    }
    private static boolean bits1961(float a,float b){return Float.floatToRawIntBits(a)==Float.floatToRawIntBits(b);}
    public float sigmaAt(float x,float y) {
        int xx=(int)x,yy=(int)y,ix,nx,iy,ny;float fx,fy;
        if(xAxis!=null && xx>=0 && xx<width && x==xx) {
            ix=xAxis.index[xx];nx=xAxis.next[xx];fx=xAxis.fraction[xx];
        } else {
            fx=columns==1?0:Math.max(0,Math.min(columns-1,(x-left)/stepX));
            ix=Math.min(columns-1,(int)fx);nx=Math.min(columns-1,ix+1);fx-=ix;
        }
        if(yAxis!=null && yy>=0 && yy<height && y==yy) {
            iy=yAxis.index[yy];ny=yAxis.next[yy];fy=yAxis.fraction[yy];
        } else {
            fy=rows==1?0:Math.max(0,Math.min(rows-1,(y-top)/stepY));
            iy=Math.min(rows-1,(int)fy);ny=Math.min(rows-1,iy+1);fy-=iy;
        }
        float a=sigma[iy*columns+ix]*(1-fx)+sigma[iy*columns+nx]*fx;
        float b=sigma[ny*columns+ix]*(1-fx)+sigma[ny*columns+nx]*fx;
        return a*(1-fy)+b*fy;
    }
    private static final class Axis {
        final int[] index,next;
        final float[] fraction;
        Axis(int coordinates,int points,float start,float step) {
            index=new int[coordinates];next=new int[coordinates];fraction=new float[coordinates];
            for(int coordinate=0;coordinate<coordinates;coordinate++) {
                // int->float conversion, clamping and subtraction exactly match
                // sigmaAt(float,float), including NaN/infinity/zero-step inputs.
                float value=points==1?0:Math.max(0,Math.min(points-1,((float)coordinate-start)/step));
                int at=Math.min(points-1,(int)value);
                index[coordinate]=at;next[coordinate]=Math.min(points-1,at+1);
                value-=at;fraction[coordinate]=value;
            }
        }
    }
    public int budgetQ8(float x,float y) {
        float measured=sigmaAt(x,y);
        // A processed clean patch should not be smoothed because another region is noisy.
        float t=Math.max(0f,Math.min(1f,(measured-.6f)/7.4f));
        return Math.round(256f*t*t*(3f-2f*t));
    }
}
