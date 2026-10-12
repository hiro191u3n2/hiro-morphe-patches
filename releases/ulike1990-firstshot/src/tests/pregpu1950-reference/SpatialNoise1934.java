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
    private SpatialNoise1934(int width,int height,int columns,int rows,float left,float top,
            float stepX,float stepY,float[] sigma,QualityPixels1932.NoiseStats global) {
        this.width=width;this.height=height;this.columns=columns;this.rows=rows;
        this.left=left;this.top=top;this.stepX=stepX;this.stepY=stepY;
        this.sigma=sigma;this.global=global;
    }
    public static SpatialNoise1934 probe(Patches source,int width,int height) {
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
    public float sigmaAt(float x,float y) {
        float fx=columns==1?0:Math.max(0,Math.min(columns-1,(x-left)/stepX));
        float fy=rows==1?0:Math.max(0,Math.min(rows-1,(y-top)/stepY));
        int ix=Math.min(columns-1,(int)fx),iy=Math.min(rows-1,(int)fy);
        int nx=Math.min(columns-1,ix+1),ny=Math.min(rows-1,iy+1);fx-=ix;fy-=iy;
        float a=sigma[iy*columns+ix]*(1-fx)+sigma[iy*columns+nx]*fx;
        float b=sigma[ny*columns+ix]*(1-fx)+sigma[ny*columns+nx]*fx;
        return a*(1-fy)+b*fy;
    }
    public int budgetQ8(float x,float y) {
        float measured=sigmaAt(x,y);
        // A processed clean patch should not be smoothed because another region is noisy.
        float t=Math.max(0f,Math.min(1f,(measured-.6f)/7.4f));
        return Math.round(256f*t*t*(3f-2f*t));
    }
}
