package com.hiro.ulike;

/** v1924: finish from immutable, already tone/chroma-corrected pixels.
 * No source-luminance target, gamma curve, exposure boost, or full-image copy.
 * The worker reuses its denoised workspace after the preceding stages finish.
 */
public final class ShadowDetail1923 {
    private ShadowDetail1923() {}
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
        if(noise<=0 || !shadows || begin==end) return;
        if(input==null || output==null || input==output || width<1 || rows<1 || noise>4
                || begin<validBegin || end<begin || end>validEnd || validBegin<0 || validEnd>rows
                || radius<1 || radius>3 || (long)width*rows>input.length || (long)width*rows>output.length)
            throw new IllegalArgumentException("processed shadow strip");
        int[] range=RANGE[noise];
        for(int row=begin;row<end;row++) {
            if((row&15)==0 && Thread.currentThread().isInterrupted()) return;
            for(int col=0;col<width;col++) {
                int at=row*width+col,center=input[at],yc=y(center);
                if((center>>>24)!=255 || yc>=144) continue;
                int cr=(center>>>16)&255,cg=(center>>>8)&255,cb=center&255;
                int weight=0,rr=0,gg=0,bb=0;
                int left=0,right=0,up=0,down=0,lc=0,rc=0,uc=0,dc=0;
                int minY=yc,maxY=yc;
                for(int dy=-radius;dy<=radius;dy++) {
                    int sy=Math.max(validBegin,Math.min(validEnd-1,row+dy));
                    for(int dx=-radius;dx<=radius;dx++) {
                        int sx=Math.max(0,Math.min(width-1,col+dx));
                        int p=input[sy*width+sx]; if((p>>>24)!=255)continue;
                        int yy=y(p),r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
                        int ld=Math.abs(yy-yc),cd=Math.max(Math.abs((r-g)-(cr-cg)),Math.abs((b-g)-(cb-cg)));
                        // An actual luminance step cannot leak a bright neighbour into black.
                        // Do not mistake modest random grain for a high-contrast edge.
                        if(ld>48+noise*3 || cd>64)continue;
                        int w=SPATIAL[dx+3]*SPATIAL[dy+3]*range[ld];
                        if(cd>12)w=w*12/cd;
                        weight+=w;rr+=w*r;gg+=w*g;bb+=w*b;
                        minY=Math.min(minY,yy);maxY=Math.max(maxY,yy);
                        if(dx<0){left+=yy;lc++;}else if(dx>0){right+=yy;rc++;}
                        if(dy<0){up+=yy;uc++;}else if(dy>0){down+=yy;dc++;}
                    }
                }
                if(weight==0)continue;
                int edge=Math.max(lc==0||rc==0?0:Math.abs(left/lc-right/rc),uc==0||dc==0?0:Math.abs(up/uc-down/dc));
                int protect=Math.max(40,256-edge*7);
                int amount=(tone(yc)*protect*(144+24*noise)+32768)>>>16;
                amount=Math.min(248,amount);
                int r=(rr+weight/2)/weight,g=(gg+weight/2)/weight,b=(bb+weight/2)/weight;
                int fy=y(0xff000000|(r<<16)|(g<<8)|b);
                // Convex averaging in the processed domain preserves constant tones,
                // black level and existing color corrections. Never inject raw RGB.
                int newY=mix(yc,fy,amount);
                // Less chroma mixing than luma; coherent/saturated color edges are rejected above.
                int colorAmount=amount*3/4;
                int or=mix(cr,r,colorAmount),og=mix(cg,g,colorAmount),ob=mix(cb,b,colorAmount);
                int delta=newY-y(0xff000000|(or<<16)|(og<<8)|ob);
                output[at]=(center&0xff000000)|(clamp(or+delta)<<16)|(clamp(og+delta)<<8)|clamp(ob+delta);
            }
        }
    }
    /** Legacy detail path: only computed output rows are sampled. A boundary taper
     * suppresses strip-edge steps without inventing or reading uncomputed halos. */
    public static void legacy(DetailPixels.Work w,int width,int rows,int start,int count,
            int noise,int sharp,boolean texture,boolean halos,boolean shadows) {
        if(w==null || noise<=0 || !shadows || count==0)return;
        int end=start+count;
        System.arraycopy(w.output,start*width,w.horizontal,start*width,count*width);
        smoothRange(w.horizontal,w.output,width,rows,start,end,start,end,noise,true,3);
        // Internal boundaries have no computed neighbours. Blend smoothly to the
        // existing result at the boundary rather than clamping a new filter there.
        for(int row=start;row<end;row++) {
            int distance=Math.min(start>0?row-start:3,end<rows?end-1-row:3);
            if(distance>=3)continue;
            int a=distance*256/3;
            for(int col=0;col<width;col++) {
                int i=row*width+col,p=w.horizontal[i],q=w.output[i];
                w.output[i]=(p&0xff000000)|(mix((p>>>16)&255,(q>>>16)&255,a)<<16)
                    |(mix((p>>>8)&255,(q>>>8)&255,a)<<8)|mix(p&255,q&255,a);
            }
        }
    }
    /** Preserve existing scheduling/parallelism. The extra processed halo fits
     * the already allocated (chroma radius + detail radius 7) source halo. */
    public static void run(ChromaPipeline186.State s,ChromaPipeline186.Buffer b) {
        try {
            while(!s.failed && !Thread.currentThread().isInterrupted()) {
                int first=s.read(b);if(first<0)return;
                int count=Math.min(s.core,s.height-first),top=Math.max(0,first-s.radius-7);
                int rows=Math.min(s.height,first+count+s.radius+7)-top,start=first-top;
                int extra=s.noise>0 && s.shadows?Math.min(3,s.radius):0;
                int lo=Math.max(0,start-extra),hi=Math.min(rows,start+count+extra);
                DetailPixels.Work w=b.pixels;int[] result;
                if(s.noise>0 || s.sharp>0) {
                    DetailSerial186.filter(w,s.width,rows,lo,hi-lo,s.noise,s.sharp,s.texture,s.halos,s.shadows);
                    Chroma186.finishWorkspace(w.source,w.output,w.horizontal,s.width,rows,lo,hi,s.radius,b.columns,b.covariance,s.nativeAllowed);
                    result=w.horizontal;
                } else {
                    Chroma186.finishWorkspace(w.source,w.source,w.output,s.width,rows,start,start+count,s.radius,b.columns,b.covariance,s.nativeAllowed);
                    result=w.output;
                }
                if(extra>0) {
                    // Denoised scratch is dead after detail+chroma. Reuse it per worker;
                    // source and neighbouring workers remain immutable/disjoint.
                    System.arraycopy(result,lo*s.width,w.denoised,lo*s.width,(hi-lo)*s.width);
                    smoothRange(w.denoised,result,s.width,rows,start,start+count,lo,hi,s.noise,true,extra);
                }
                if(Thread.currentThread().isInterrupted()){s.failed=true;return;}
                s.write(result,start,first,count);
            }
            s.failed=true;
        }catch(Throwable error){s.failed=true;}
    }
}
