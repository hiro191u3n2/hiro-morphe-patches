package com.hiro.ulike;

/** v1941: two processed-domain residual scales, selected by coherent structure and
 * measured shot noise. Neither source RGB nor a brightness target is reintroduced.
 * A radius-three processed halo is sufficient for both scales, including their gate.
 */
public final class QualityShadow1932 {
    private QualityShadow1932() {}
    private static final int[] SPATIAL={1,2,3,4,3,2,1};
    private static final int[][] RANGE=new int[33][256];
    static {
        for(int sigma=1;sigma<=32;sigma++)
            for(int d=0;d<256;d++) RANGE[sigma][d]=(int)Math.round(256.0*Math.exp(-(double)d*d/(2.0*sigma*sigma)));
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
    /** Preserved ABI for older callers. Context-free calls keep bounded cleanup. */
    public static void smoothRange(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int noise,boolean shadows,int radius) {
        smoothRange(input,output,width,rows,begin,end,validBegin,validEnd,noise,shadows,radius,null,0);
    }
    /** validBegin/end contain computed processed pixels, never unread workspace halos.
     * The explicit Plan avoids depending on thread-local state in nested workers.
     */
    public static void smoothRange(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int noise,boolean shadows,int radius,
            QualityPixels1932.Plan plan,int originY) {
        if(noise<=0 || begin==end) return;
        if(input==null || output==null || input==output || width<1 || rows<1 || noise>4
                || begin<validBegin || end<begin || end>validEnd || validBegin<0 || validEnd>rows
                || radius<1 || radius>3 || (long)width*rows>input.length || (long)width*rows>output.length)
            throw new IllegalArgumentException("processed noise strip");
        for(int row=begin;row<end;row++) {
            if((row&15)==0 && Thread.currentThread().isInterrupted()) return;
            for(int col=0;col<width;col++) {
                int at=row*width+col,center=input[at],yc=y(center);
                if((center>>>24)!=255) continue;
                int cr=(center>>>16)&255,cg=(center>>>8)&255,cb=center&255;
                int absoluteY=row+originY;
                float measured=plan==null?3f+noise*1.8f:plan.localNoise==null?plan.sourceSigma:plan.localNoise.sigmaAt(col,absoluteY);
                int budget=plan==null?256:QualityPixels1932.shadowBudgetQ8(center,0,plan,col,absoluteY);
                if(plan!=null && plan.localNoise!=null)budget=budget*plan.localNoise.budgetQ8(col,absoluteY)>>8;
                if(plan!=null && plan.faceRegions!=null)
                    budget=budget*(256-(Math.max(0,Math.min(256,plan.faceRegions.detailQ8(col,absoluteY)))*208>>8))>>8;
                if(budget<=0 || measured<0.6f)continue;
                // Corrected pixels already passed the main NR. Its estimate remains an
                // upper bound for range weights; a small source floor never forces NR
                // onto an independently measured clean patch.
                int sigma=Math.max(2,Math.min(32,Math.round(measured*1.4f+2f)));
                int[] range=RANGE[sigma];
                int weight=0,rr=0,gg=0,bb=0,nearWeight=0,nr=0,ng=0,nb=0;
                int left=0,right=0,up=0,down=0,lc=0,rc=0,uc=0,dc=0;
                int minY=yc,maxY=yc;
                for(int dy=-radius;dy<=radius;dy++) {
                    int sy=Math.max(validBegin,Math.min(validEnd-1,row+dy));
                    for(int dx=-radius;dx<=radius;dx++) {
                        int sx=Math.max(0,Math.min(width-1,col+dx));
                        int p=input[sy*width+sx]; if((p>>>24)!=255)continue;
                        int yy=y(p),r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
                        // Use all neighbours for structure, including neighbours
                        // rejected by the range kernel. Otherwise a real step could
                        // look deceptively flat after its other side was discarded.
                        if(dx<0){left+=yy;lc++;}else if(dx>0){right+=yy;rc++;}
                        if(dy<0){up+=yy;uc++;}else if(dy>0){down+=yy;dc++;}
                        minY=Math.min(minY,yy);maxY=Math.max(maxY,yy);
                        int ld=Math.abs(yy-yc),cd=Math.max(Math.abs((r-g)-(cr-cg)),Math.abs((b-g)-(cb-cg)));
                        if(ld>Math.min(56,8+sigma*3) || cd>Math.min(48,8+sigma*2))continue;
                        int w=SPATIAL[dx+3]*SPATIAL[dy+3]*range[ld];
                        if(cd>8)w=w*8/cd;
                        if(w==0)continue;
                        weight+=w;rr+=w*r;gg+=w*g;bb+=w*b;
                        if(Math.abs(dx)<=1 && Math.abs(dy)<=1){nearWeight+=w;nr+=w*r;ng+=w*g;nb+=w*b;}
                    }
                }
                if(weight==0 || nearWeight==0)continue;
                int edge=Math.max(lc==0||rc==0?0:Math.abs(left/lc-right/rc),uc==0||dc==0?0:Math.abs(up/uc-down/dc));
                int periodic=textureQ8(input,width,rows,col,row,validBegin,validEnd,measured,radius);
                // Random grain cancels in opposite half-window means. A coherent
                // scene edge or repeated fabric pattern does not. Keep a smooth
                // transition, rather than hard tiles or intensity quantisation.
                int tolerance=3+Math.round(measured*.55f);
                int flat=Math.max(0,Math.min(256,(tolerance*3-edge)*256/Math.max(1,tolerance*2)));
                flat=flat*(256-periodic)>>8;
                if(flat==0)continue;
                int dark=shadows?tone(yc):0;
                int fine=(112+noise*32+(dark*16>>8))*flat>>8;
                int coarse=(80+noise*32+(dark*16>>8))*flat*flat>>16;
                // Face/beauty coordination is applied only once, with the real local
                // range. Eye/lip detail masks and spatial noise budgets remain intact.
                if(plan!=null){
                    int coordinated=QualityPixels1932.shadowBudgetQ8(center,maxY-minY,plan,col,absoluteY);
                    int base=QualityPixels1932.shadowBudgetQ8(center,0,plan,col,absoluteY);
                    if(base>0)budget=Math.min(256,budget*coordinated/base);
                }else budget=QualityPipeline1932.shadowBudgetQ8(center,maxY-minY,col,row);
                fine=Math.min(248,fine)*budget>>8;
                coarse=Math.min(232,coarse)*budget>>8;
                if(fine==0 && coarse==0)continue;
                int r1=(nr+nearWeight/2)/nearWeight,g1=(ng+nearWeight/2)/nearWeight,b1=(nb+nearWeight/2)/nearWeight;
                int r2=(rr+weight/2)/weight,g2=(gg+weight/2)/weight,b2=(bb+weight/2)/weight;
                // Fine residual = center - radius1. Coarse residual = radius1 -
                // radius3. Shrink both independently; neither stage samples writes
                // from this invocation, so parallel strips are deterministic.
                int fy=y(0xff000000|(r1<<16)|(g1<<8)|b1),cy=y(0xff000000|(r2<<16)|(g2<<8)|b2);
                int newY=yc+roundDiv((fy-yc)*fine+(cy-fy)*coarse,256);
                newY=Math.max(Math.min(yc,Math.min(fy,cy)),Math.min(Math.max(yc,Math.max(fy,cy)),newY));
                int colorFine=fine*7/8,colorCoarse=coarse*7/8;
                int or=clamp(cr+roundDiv((r1-cr)*colorFine+(r2-r1)*colorCoarse,256));
                int og=clamp(cg+roundDiv((g1-cg)*colorFine+(g2-g1)*colorCoarse,256));
                int ob=clamp(cb+roundDiv((b1-cb)*colorFine+(b2-b1)*colorCoarse,256));
                int delta=newY-y(0xff000000|(or<<16)|(og<<8)|ob);
                // Shared bounded offset protects saturated hue and black level.
                delta=Math.max(-Math.min(or,Math.min(og,ob)),Math.min(255-Math.max(or,Math.max(og,ob)),delta));
                output[at]=(center&0xff000000)|((or+delta)<<16)|((og+delta)<<8)|(ob+delta);
            }
        }
    }
    private static int roundDiv(int value,int divisor){return value<0?-((-value+divisor/2)/divisor):(value+divisor/2)/divisor;}

    /** Opposite phase/repeat agreement distinguishes periodic real luma texture
     * from incoherent random grain. It never reads beyond the processed halo. */
    static int textureQ8(int[] src,int width,int rows,int x,int row,int lo,int hi,float sigma,int radius){
        if(radius<3 || x<3 || x>=width-3 || row<lo+3 || row>=hi-3)return 0;
        int center=src[row*width+x];
        int c=y(center),best=0,threshold=Math.max(3,Math.round(sigma*.75f));
        for(int direction=0;direction<4;direction++){
            int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
            int at=row*width+x;
            int p1=src[at+step],m1=src[at-step],p2=src[at+2*step],m2=src[at-2*step],p3=src[at+3*step],m3=src[at-3*step];
            if((p1>>>24)!=255||(m1>>>24)!=255||(p2>>>24)!=255||(m2>>>24)!=255||(p3>>>24)!=255||(m3>>>24)!=255)continue;
            int a=y(p1),b=y(m1),d=y(p2),e=y(m2),f=y(p3),g=y(m3);
            int amplitude=Math.abs(2*c-a-b)/2;
            if(amplitude<threshold || (c-a)*(c-b)<=0)continue;
            int error=Math.abs(a-b)+Math.abs(d-c)+Math.abs(e-c)+Math.abs(f-a)+Math.abs(g-b);
            int allowed=5+amplitude/2;
            int confidence=Math.max(0,Math.min(256,(allowed-error)*256/Math.max(1,allowed)));
            best=Math.max(best,confidence);
        }
        return best;
    }
}
