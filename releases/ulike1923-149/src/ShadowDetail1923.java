package com.hiro.ulike;

/** Bounded, allocation-free shadow finishing. Runs after both native and Java
 * chroma paths; immutable source halos make strip/worker boundaries identical.
 * The user's noise-off and shadow-priority-off settings are respected.
 */
public final class ShadowDetail1923 {
    private ShadowDetail1923() {}
    private static final int[] SPATIAL = {1, 3, 4, 3, 1};
    private static final int[][] RANGE = new int[5][256];
    static {
        for (int level = 1; level <= 4; level++) {
            double sigma = 13.0 + 4.0 * level;
            for (int d = 0; d < 256; d++)
                RANGE[level][d] = (int)Math.round(256.0 * Math.exp(-(double)d*d/(2*sigma*sigma)));
        }
    }
    static int y(int p) { return (77*((p>>>16)&255)+150*((p>>>8)&255)+29*(p&255)+128)>>>8; }
    static int clamp(int x) { return x < 0 ? 0 : x > 255 ? 255 : x; }
    static int mix(int a,int b,int amount) { return (a*(256-amount)+b*amount+128)>>>8; }

    public static void smooth(int[] source,int[] output,int width,int rows,int begin,int end,int noise,boolean shadows) {
        if (noise <= 0 || !shadows || begin == end) return;
        if (source == null || output == null || source == output || width < 1 || rows < 1
                || begin < 0 || end < begin || end > rows || (long)width*rows > source.length
                || (long)width*rows > output.length || noise > 4) throw new IllegalArgumentException("shadow strip");
        int[] range = RANGE[noise];
        for (int row=begin; row<end; row++) {
            if ((row & 15)==0 && Thread.currentThread().isInterrupted()) return;
            for (int col=0; col<width; col++) {
                int index=row*width+col, center=source[index], base=output[index];
                if ((center>>>24)!=255 || (base>>>24)!=255) continue;
                int yc=y(center);
                // Smooth taper, no abrupt tonal threshold. Highlights remain exact.
                if (yc>=144) continue;
                int tone=Math.min(256,((144-yc)*256)/104);
                tone=(tone*tone*(768-2*tone)+32768)>>>16;
                int weight=0, rr=0,gg=0,bb=0;
                int left=0,right=0,up=0,down=0,lc=0,rc=0,uc=0,dc=0;
                int cr=(center>>>16)&255,cg=(center>>>8)&255,cb=center&255;
                for (int dy=-2;dy<=2;dy++) {
                    int sy=Math.max(0,Math.min(rows-1,row+dy)), offset=sy*width;
                    for (int dx=-2;dx<=2;dx++) {
                        int sx=Math.max(0,Math.min(width-1,col+dx)), p=source[offset+sx];
                        if ((p>>>24)!=255) continue;
                        int yy=y(p),r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
                        int lumadiff=Math.abs(yy-yc);
                        // Saturated color edges receive their own rejection, not just luma guidance.
                        int chromadiff=Math.max(Math.abs((r-g)-(cr-cg)),Math.abs((b-g)-(cb-cg)));
                        int w=SPATIAL[dx+2]*SPATIAL[dy+2]*range[lumadiff];
                        if (chromadiff>32) w=w*32/chromadiff;
                        weight+=w;rr+=w*r;gg+=w*g;bb+=w*b;
                        if (dx<0) {left+=yy;lc++;} else if (dx>0) {right+=yy;rc++;}
                        if (dy<0) {up+=yy;uc++;} else if (dy>0) {down+=yy;dc++;}
                    }
                }
                if (weight==0) continue;
                int edge=Math.max(lc==0||rc==0?255:Math.abs(left/lc-right/rc),uc==0||dc==0?255:Math.abs(up/uc-down/dc));
                // Coherent borders survive; isolated grain is not treated as detail to sharpen.
                int protect=Math.max(32,256-edge*6);
                int amount=(tone*protect*(128+24*noise)+32768)>>>16;
                amount=Math.min(240,amount);
                int r=(rr+weight/2)/weight,g=(gg+weight/2)/weight,b=(bb+weight/2)/weight;
                // Do not blend source RGB back into the color-corrected output: that
                // would partially undo Chroma186's existing color-noise/mura repair.
                // Smooth luminance only and keep the finishing stage's chroma.
                int filteredY=(77*r+150*g+29*b+128)>>>8;
                int baseY=y(base), delta=mix(baseY,filteredY,amount)-baseY;
                output[index]=(base&0xff000000)|(clamp(((base>>>16)&255)+delta)<<16)
                        |(clamp(((base>>>8)&255)+delta)<<8)|clamp((base&255)+delta);
            }
        }
    }
    /** Legacy fallback uses the same finishing policy, without extra work buffers. */
    public static void legacy(DetailPixels.Work w,int width,int rows,int start,int count,
            int noise,int sharp,boolean texture,boolean halos,boolean shadows) {
        if (w!=null) smooth(w.source,w.output,width,rows,start,start+count,noise,shadows);
    }
    /** Same scheduling, halo, memory and error contract as the existing worker. */
    public static void run(ChromaPipeline186.State s,ChromaPipeline186.Buffer b) {
        try {
            while (!s.failed && !Thread.currentThread().isInterrupted()) {
                int first=s.read(b); if (first<0) return;
                int count=Math.min(s.core,s.height-first);
                int top=Math.max(0,first-s.radius-7);
                int rows=Math.min(s.height,first+count+s.radius+7)-top;
                int start=first-top;
                DetailPixels.Work w=b.pixels;
                int[] result;
                if (s.noise>0 || s.sharp>0) {
                    DetailSerial186.filter(w,s.width,rows,start,count,s.noise,s.sharp,s.texture,s.halos,s.shadows);
                    Chroma186.finishWorkspace(w.source,w.output,w.horizontal,s.width,rows,start,start+count,
                            s.radius,b.columns,b.covariance,s.nativeAllowed);
                    result=w.horizontal;
                } else {
                    Chroma186.finishWorkspace(w.source,w.source,w.output,s.width,rows,start,start+count,
                            s.radius,b.columns,b.covariance,s.nativeAllowed);
                    result=w.output;
                }
                smooth(w.source,result,s.width,rows,start,start+count,s.noise,s.shadows);
                s.write(result,start,first,count);
            }
            s.failed=true;
        } catch (Throwable error) { s.failed=true; }
    }
}
