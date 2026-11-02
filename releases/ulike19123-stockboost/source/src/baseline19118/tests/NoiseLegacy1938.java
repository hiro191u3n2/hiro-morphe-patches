package com.hiro.ulike;

/** v1932: budget the existing all-tone/shadow cleanup against actual shot noise and beauty; finish from immutable, already tone/chroma-corrected pixels.
 * No source-luminance target, gamma curve, exposure boost, or full-image copy.
 * The worker reuses its denoised workspace after the preceding stages finish.
 */
public final class NoiseLegacy1938 {
    private NoiseLegacy1938() {}
    private static final int[] SPATIAL={1,2,3,4,3,2,1};
    private static final int[][] RANGE=new int[5][256];
    static {
        for(int level=1;level<=4;level++) {
            double sigma=10.0+4.5*level;
            for(int d=0;d<256;d++) RANGE[level][d]=(int)Math.round(256.0*Math.exp(-(double)d*d/(2*sigma*sigma)));
        }
    }
    static int y(int p) {return (77*((p>>>16)&255)+150*((p>>>8)&255)+29*(p&255)+128)>>>8;}
    static int clamp(int x) {return Math.max(0,Math.min(255,x));}
    static int mix(int a,int b,int amount) {return (a*(256-amount)+b*amount+128)>>>8;}
    static int tone(int luminance) {
        int t=Math.max(0,Math.min(256,(144-luminance)*256/112));
        return (t*t*(768-2*t)+32768)>>>16;
    }
    /** Compatibility entry: input must be an immutable processed-pixel snapshot. */
    public static void smooth(int[] input,int[] output,int width,int rows,int begin,int end,int noise,boolean shadows) {
        smoothRange(input,output,width,rows,begin,end,0,rows,noise,shadows,3);
    }
    /** validBegin/end describe computed processed pixels, never unread workspace halos. */
    public static void smoothRange(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int noise,boolean shadows,int radius) {
        if(noise<=0 || begin==end) return;
        if(input==null || output==null || input==output || width<1 || rows<1 || noise>4
                || begin<validBegin || end<begin || end>validEnd || validBegin<0 || validEnd>rows
                || radius<1 || radius>3 || (long)width*rows>input.length || (long)width*rows>output.length)
            throw new IllegalArgumentException("processed shadow strip");
        int[] range=RANGE[noise];
        for(int row=begin;row<end;row++) {
            if((row&15)==0 && Thread.currentThread().isInterrupted()) return;
            for(int col=0;col<width;col++) {
                int at=row*width+col,center=input[at],yc=y(center);
                if((center>>>24)!=255) continue;
                int dark=shadows ? tone(yc) : 0;
                int support=dark>0 ? radius : Math.min(radius,2);
                int cr=(center>>>16)&255,cg=(center>>>8)&255,cb=center&255;
                int weight=0,rr=0,gg=0,bb=0;
                int left=0,right=0,up=0,down=0,lc=0,rc=0,uc=0,dc=0;
                int minY=yc,maxY=yc;
                for(int dy=-support;dy<=support;dy++) {
                    int sy=Math.max(validBegin,Math.min(validEnd-1,row+dy));
                    for(int dx=-support;dx<=support;dx++) {
                        int sx=Math.max(0,Math.min(width-1,col+dx));
                        int p=input[sy*width+sx]; if((p>>>24)!=255)continue;
                        int yy=y(p),r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
                        int ld=Math.abs(yy-yc),cd=Math.max(Math.abs((r-g)-(cr-cg)),Math.abs((b-g)-(cb-cg)));
                        // An actual luminance step cannot leak a bright neighbour into black.
                        // Do not mistake modest random grain for a high-contrast edge.
                        if(ld>48+noise*3 || cd>64)continue;
                        int w=SPATIAL[dx+3]*SPATIAL[dy+3]*range[ld];
                        if(Math.abs(dx)==3 || Math.abs(dy)==3)w=(w*dark+128)>>>8;
                        if(cd>12)w=w*12/cd;
                        if(w==0)continue;
                        weight+=w;rr+=w*r;gg+=w*g;bb+=w*b;
                        minY=Math.min(minY,yy);maxY=Math.max(maxY,yy);
                        if(dx<0){left+=yy;lc++;}else if(dx>0){right+=yy;rc++;}
                        if(dy<0){up+=yy;uc++;}else if(dy>0){down+=yy;dc++;}
                    }
                }
                if(weight==0)continue;
                int edge=Math.max(lc==0||rc==0?0:Math.abs(left/lc-right/rc),uc==0||dc==0?0:Math.abs(up/uc-down/dc));
                int protect=Math.max(40,256-edge*7);
                // A small all-tone floor removes residual grain without lifting black.
                // Shadow priority adds strength, rather than disabling global cleanup.
                int toneWeight=shadows ? 80+((176*dark+128)>>>8) : 120;
                int amount=(toneWeight*protect*(152+24*noise)+32768)>>>16;
                amount=Math.min(248,amount);
                // Coordinate only after the existing chroma/tone corrections.
                // No source RGB/luminance is reintroduced. Unknown contexts preserve v1925.
                amount=(amount*QualityPipeline1932.shadowBudgetQ8(center,maxY-minY,col,row)+128)>>>8;
                int r=(rr+weight/2)/weight,g=(gg+weight/2)/weight,b=(bb+weight/2)/weight;
                int fy=y(0xff000000|(r<<16)|(g<<8)|b);
                // Convex averaging in the processed domain preserves constant tones,
                // black level and existing color corrections. Never inject raw RGB.
                int newY=mix(yc,fy,amount);
                // Less chroma mixing than luma; coherent/saturated color edges are rejected above.
                int colorAmount=amount*7/8;
                int or=mix(cr,r,colorAmount),og=mix(cg,g,colorAmount),ob=mix(cb,b,colorAmount);
                int delta=newY-y(0xff000000|(or<<16)|(og<<8)|ob);
                output[at]=(center&0xff000000)|(clamp(or+delta)<<16)|(clamp(og+delta)<<8)|clamp(ob+delta);
            }
        }
    }
}
