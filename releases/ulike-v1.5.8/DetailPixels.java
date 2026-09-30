package com.hiro.ulike;

/** Full-resolution spatial denoise and bounded, two-scale luminance sharpening.
 * Y and colour differences have separate bilateral weights. Original source is
 * guidance in both axes. This is NOT RAW, multi-frame fusion or AI reconstruction.
 * All working arrays are strip sized; no source mutation or per-pixel allocation.
 */
public final class DetailPixels {
    /** Three rows of denoise support plus four rows of local contrast support. */
    public static final int HALO = 7;
    private static final int[] SPATIAL = {1,6,15,20,15,6,1};
    private static final int[] GAUSS = {1,4,6,4,1};
    private static final int[] LUMA_MIX = {0,48,68,85,96};
    private static final int[] CHROMA_MIX = {0,78,91,98,100};
    private static final int[] GAIN = {0,60,95,135,175};
    private static final int[] CLARITY = {0,10,18,28,40};
    private static final int[] LIMIT = {0,5,8,12,16};
    private static final int[][] LUMA_RANGE = ranges(new int[]{1,12,18,26,34});
    private static final int[][] COLOUR_RANGE = ranges(new int[]{1,18,28,38,48});
    private DetailPixels() {}
    private static int[][] ranges(int[] sigma) {
        int[][] table=new int[5][256];
        for(int k=1;k<=4;k++)for(int d=0;d<256;d++)
            table[k][d]=(int)Math.round(256*StrictMath.exp(-(double)d*d/(2*sigma[k]*sigma[k])));
        return table;
    }
    public static final class Work {
        public final int[] source,horizontal,denoised,output;
        public Work(int pixels){
            if(pixels<=0)throw new IllegalArgumentException("pixels");
            source=new int[pixels];horizontal=new int[pixels];denoised=new int[pixels];output=new int[pixels];
        }
    }
    private static int clamp(int v,int lo,int hi){return v<lo?lo:(v>hi?hi:v);}
    private static int div(int v,int d){return v>=0?(v+d/2)/d:-((-v+d/2)/d);}
    private static int r(int p){return(p>>>16)&255;}
    private static int g(int p){return(p>>>8)&255;}
    private static int b(int p){return p&255;}
    private static int y(int p){return(77*r(p)+150*g(p)+29*b(p)+128)>>8;}
    private static int cb(int p){return b(p)-y(p);}
    private static int cr(int p){return r(p)-y(p);}
    private static boolean opaque(int p){return(p>>>24)==255;}
    private static int rgb(int red,int green,int blue){return 0xff000000|(clamp(red,0,255)<<16)|(clamp(green,0,255)<<8)|clamp(blue,0,255);}
    private static int colourDistance(int a,int b){return Math.max(Math.abs(cb(a)-cb(b)),Math.abs(cr(a)-cr(b)));}
    private static int skinWeight(int p){
        // Broad continuous colour heuristic, not face/skin segmentation. It can
        // also protect similarly coloured background objects. No image leaves device.
        int rr=cr(p),bb=cb(p),light=y(p);
        int a=clamp(rr*256/14,0,256)*clamp((90-rr)*256/20,0,256)/256;
        int c=clamp((20-bb)*256/16,0,256)*clamp((bb+80)*256/20,0,256)/256;
        return a*c/256*clamp((light-10)*256/25,0,256)/256;
    }
    private static int gradient(int[] pixels,int width,int rows,int x,int yy,int radius){
        int left=y(pixels[yy*width+clamp(x-radius,0,width-1)]),right=y(pixels[yy*width+clamp(x+radius,0,width-1)]);
        int top=y(pixels[clamp(yy-radius,0,rows-1)*width+x]),bottom=y(pixels[clamp(yy+radius,0,rows-1)*width+x]);
        return Math.abs(right-left)+Math.abs(bottom-top);
    }
    /** Compatibility entry used by older helper callers. Protection defaults on. */
    public static void filter(Work w,int width,int rows,int coreStart,int coreRows,int noise,int sharp){
        filter(w,width,rows,coreStart,coreRows,noise,sharp,true,true,true);
    }
    /** core rows only are valid in output. Caller supplies HALO rows where present. */
    public static void filter(Work w,int width,int rows,int coreStart,int coreRows,int noise,int sharp,
                              boolean preserveTexture,boolean suppressHalos,boolean preferShadows){
        if(w==null||width<=0||rows<=0||noise<0||noise>4||sharp<0||sharp>4||coreStart<0||coreRows<0||
           (long)coreStart+coreRows>rows||(long)width*rows>w.source.length)
            throw new IllegalArgumentException("strip or levels");
        int n=width*rows;int[] src=w.source,den=src;
        if(noise==0&&sharp==0){System.arraycopy(src,0,w.output,0,n);return;}
        if(noise>0){
            bilateral(src,src,w.horizontal,width,rows,noise,true,preserveTexture,preferShadows);
            bilateral(src,w.horizontal,w.denoised,width,rows,noise,false,preserveTexture,preferShadows);
            den=w.denoised;
        }
        if(sharp==0){System.arraycopy(den,coreStart*width,w.output,coreStart*width,coreRows*width);return;}
        // Pack two horizontal sums (12 bits each). Negative means transparency
        // occurred within the complete neighbourhood; never sharpen across it.
        for(int yy=0;yy<rows;yy++)for(int x=0;x<width;x++){
            int small=0,large=0;boolean alpha=false;
            for(int k=-4;k<=4;k++){
                int p=den[yy*width+clamp(x+k,0,width-1)],v=y(p);
                if(!opaque(p))alpha=true;
                large+=v;if(k>=-2&&k<=2)small+=GAUSS[k+2]*v;
            }
            w.horizontal[yy*width+x]=alpha?-1:(small|(large<<12));
        }
        for(int yy=coreStart;yy<coreStart+coreRows;yy++)for(int x=0;x<width;x++){
            int i=yy*width+x,p=den[i];w.output[i]=p;if(!opaque(p))continue;
            int small=0,large=0,min=y(p),max=min;boolean alpha=false;
            for(int k=-4;k<=4;k++){
                int packed=w.horizontal[clamp(yy+k,0,rows-1)*width+x];
                if(packed<0){alpha=true;continue;}
                large+=packed>>>12;if(k>=-2&&k<=2)small+=(packed&4095)*GAUSS[k+2];
            }
            if(alpha)continue;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                int value=y(den[clamp(yy+dy,0,rows-1)*width+clamp(x+dx,0,width-1)]);
                min=Math.min(min,value);max=Math.max(max,value);
            }
            int center=y(p),fineGradient=gradient(den,width,rows,x,yy,1);
            int wideGradient=gradient(den,width,rows,x,yy,2);
            // An isolated impulse has zero opposite-neighbour gradient. Require
            // structure at two scales before allowing sharpening or clarity.
            int structure=Math.min(fineGradient,wideGradient);
            int gate=noise==0?10:(noise==1?8:6);
            int mask=clamp((structure-gate)*256/30,0,256);
            if(mask==0)continue;
            int fine=center*256-small;
            int detail=div(Math.max(0,Math.abs(fine)-160)*GAIN[sharp],25600);
            if(fine<0)detail=-detail;
            int broad=center*81-large;
            int local=div(Math.max(0,Math.abs(broad)-81)*CLARITY[sharp],8100);
            if(broad<0)local=-local;
            local=clamp(local,-sharp,sharp);
            int delta=div((detail+local)*mask,256);
            // Skin-colour detail protection reduces hard-looking skin sharpening.
            if(preserveTexture)delta=div(delta*(256-skinWeight(p)*35/100),256);
            delta=clamp(delta,-LIMIT[sharp],LIMIT[sharp]);
            int allowance=suppressHalos?0:sharp;
            delta=clamp(delta,min-center-allowance,max-center+allowance);
            // Uniform offset preserves chroma; no RGB clipping even with halo switch off.
            delta=clamp(delta,-Math.min(r(p),Math.min(g(p),b(p))),255-Math.max(r(p),Math.max(g(p),b(p))));
            w.output[i]=rgb(r(p)+delta,g(p)+delta,b(p)+delta);
        }
    }
    private static void bilateral(int[] guide,int[] values,int[] target,int width,int rows,int level,
                                  boolean horizontal,boolean preserveTexture,boolean preferShadows){
        int[] lw=LUMA_RANGE[level],cw=COLOUR_RANGE[level];
        for(int yy=0;yy<rows;yy++)for(int x=0;x<width;x++){
            int i=yy*width+x,p=guide[i],py=y(p),pcb=cb(p),pcr=cr(p);
            if(!opaque(p)){target[i]=p;continue;}
            int shadow=preferShadows?clamp((112-py)*256/96,0,256):0;
            int distanceScale=256-shadow/6;
            int sy=0,scb=0,scr=0,wy=0,wc=0;
            for(int k=-3;k<=3;k++){
                int j=horizontal?yy*width+clamp(x+k,0,width-1):clamp(yy+k,0,rows-1)*width+x;
                int q=guide[j];if(!opaque(q))continue;
                int ld=Math.abs(py-y(q)),cd=colourDistance(p,q);
                // Explicit gate prevents stronger levels from washing over strong boundaries.
                if(ld>72||cd>110)continue;
                int yl=clamp(Math.max(ld,cd/3)*distanceScale/256,0,255);
                int cl=clamp(Math.max(ld*2,cd)*distanceScale/256,0,255);
                int ylWeight=SPATIAL[k+3]*lw[yl],clWeight=SPATIAL[k+3]*cw[cl],v=values[j];
                wy+=ylWeight;sy+=ylWeight*y(v);wc+=clWeight;scb+=clWeight*cb(v);scr+=clWeight*cr(v);
            }
            // k=0 always contributes a nonzero weight, including one-pixel images.
            int dy=div(sy,wy)-py,dcb=div(scb,wc)-pcb,dcr=div(scr,wc)-pcr;
            if(!horizontal){
                int l=Math.min(100,LUMA_MIX[level]+shadow*12/256);
                int c=Math.min(100,CHROMA_MIX[level]+shadow*5/256);
                int edge=clamp((gradient(guide,width,rows,x,yy,1)-12)*256/72,0,256);
                // Retain some source luminance at structured contours. With texture
                // priority, use a little more on skin-like colours; no fabricated detail.
                int retain=edge*12/256;
                if(preserveTexture)retain+=edge*skinWeight(p)/256*20/256;
                l=l*(100-retain)/100;
                dy=div(dy*l,100);dcb=div(dcb*c,100);dcr=div(dcr*c,100);
            }
            target[i]=rgb(r(p)+dy+dcr,g(p)+dy-div(77*dcr+29*dcb,150),b(p)+dy+dcb);
        }
    }
}
