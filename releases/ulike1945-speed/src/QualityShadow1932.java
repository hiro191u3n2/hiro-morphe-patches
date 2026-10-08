package com.hiro.ulike;

/** v1944 caches the immutable processed neighbourhood, preserving v1942 arithmetic.
 * v1942: two processed-domain residual scales, selected by coherent structure and
 * measured shot noise. Neither source RGB nor a brightness target is reintroduced.
 * Four computed processed halo rows cover the wider sparse scale and texture gate.
 */
public final class QualityShadow1932 {
    private QualityShadow1932() {}
    private static final int[] SPATIAL={1,2,3,4,3,2,1};
    public static final int RESIDUAL_RADIUS=4;
    private static final int[] OFFSETS={-4,-2,-1,0,1,2,4};
    private static final int[] WIDE_SPATIAL={1,3,5,6,5,3,1};
    private static final ThreadLocal<int[]> TEXTURE_SAMPLES=new ThreadLocal<int[]>() {
        protected int[] initialValue(){return new int[9];}
    };
    private static final int[][] RANGE=new int[33][256];
    private static final int[] FLAT_RANGE=new int[33*256];
    static {
        for(int sigma=1;sigma<=32;sigma++)
            for(int d=0;d<256;d++) RANGE[sigma][d]=FLAT_RANGE[sigma*256+d]=(int)Math.round(256.0*Math.exp(-(double)d*d/(2.0*sigma*sigma)));
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
                || radius<1 || radius>RESIDUAL_RADIUS || (long)width*rows>input.length || (long)width*rows>output.length)
            throw new IllegalArgumentException("processed noise strip");
        if(NativeSpeed1944.available() && smoothNative(input,output,width,rows,begin,end,
                validBegin,validEnd,noise,shadows,radius,plan,originY))return;
        final int global=plan==null?256:plan.shadowBudgetQ8;
        final int stronger=global+((256-global)*noise>>2);
        final int fineBase=128+noise*30,coarseBase=96+noise*34;
        final int smoothLimit=plan==null?0:10+Math.round(Math.min(12f,plan.sourceSigma)*2f);
        final NoiseCache1944 cache=borrowCache(input,width,radius);
        try {
        final int taps=cache==null?(radius==RESIDUAL_RADIUS?OFFSETS.length:radius*2+1):cache.taps;
        final int[] samples=TEXTURE_SAMPLES.get();
        for(int row=begin;row<end;row++) {
            if((row&15)==0 && Thread.currentThread().isInterrupted()) return;
            if(cache!=null)cache.prepare(row,validBegin,validEnd);
            for(int col=0;col<width;col++) {
                int at=row*width+col,center=input[at],centerInfo=cache==null?packInfo(center):cache.packed[cache.centerOffset+col],yc=centerInfo&255;
                if((center>>>24)!=255) continue;
                int cr=(center>>>16)&255,cg=(center>>>8)&255,cb=center&255;
                int absoluteY=row+originY;
                float measured=plan==null?3f+noise*1.8f:plan.localSigmaAt(col,absoluteY);
                // This is residual cleanup after primary NR. The source-map ramp
                // was designed to attenuate the primary stage and reached full
                // budget only at sigma8, starving correlated sigma1–2 wall grain.
                // A dedicated confidence ramp opens only when measured grain is
                // present; a clean zero-noise patch still has exactly zero budget.
                int skin=plan==null || global==0 || plan.beautyQ8==0 || plan.faceRegions==null || absoluteY<0
                    ?0:plan.skinAt(col,absoluteY);
                int base=plan==null?256:budgetQ8(0,global,plan.beautyQ8,skin,smoothLimit);
                int budget=plan==null?256:global==0?0:base*stronger/global;
                if(plan!=null && plan.localNoise!=null)budget=budget*residualConfidenceQ8(measured)>>8;
                if(plan!=null && plan.faceRegions!=null)
                    budget=budget*(256-(Math.max(0,Math.min(256,plan.detailAt(col,absoluteY)))*208>>8))>>8;
                if(budget<=0 || measured<0.20f)continue;
                // Corrected pixels already passed the main NR. Its estimate remains an
                // upper bound for range weights; a small source floor never forces NR
                // onto an independently measured clean patch.
                int sigma=Math.max(2,Math.min(32,Math.round(measured*1.4f+2f)));
                int[] range=RANGE[sigma];
                int weight=0,rr=0,gg=0,bb=0,nearWeight=0,nr=0,ng=0,nb=0;
                int left=0,right=0,up=0,down=0,lc=0,rc=0,uc=0,dc=0;
                int minY=yc,maxY=yc;
                int lumaLimit=Math.min(56,8+sigma*3),chromaLimit=Math.min(48,8+sigma*2);
                int centerRG=(centerInfo>>>8)&511,centerBG=(centerInfo>>>17)&511;
                for(int yi=0;yi<taps;yi++) {
                    int dy=cache==null?(radius==RESIDUAL_RADIUS?OFFSETS[yi]:yi-radius):cache.dx[yi];
                    int sy=cache==null?Math.max(validBegin,Math.min(validEnd-1,row+dy)):cache.ys[yi];
                    int sourceRow=sy*width,cacheRow=cache==null?0:cache.ringBase[yi],columnBase=col*taps;
                    for(int xi=0;xi<taps;xi++) {
                        int dx=cache==null?(radius==RESIDUAL_RADIUS?OFFSETS[xi]:xi-radius):cache.dx[xi];
                        int sx=cache==null?Math.max(0,Math.min(width-1,col+dx)):cache.xs[columnBase+xi];
                        int info=cache==null?packInfo(input[sourceRow+sx]):cache.packed[cacheRow+sx];if((info&NoiseCache1944.OPAQUE)==0)continue;
                        int p=input[sourceRow+sx];
                        int yy=info&255,r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
                        // Use all neighbours for structure, including neighbours
                        // rejected by the range kernel. Otherwise a real step could
                        // look deceptively flat after its other side was discarded.
                        if(dx<0){left+=yy;lc++;}else if(dx>0){right+=yy;rc++;}
                        if(dy<0){up+=yy;uc++;}else if(dy>0){down+=yy;dc++;}
                        minY=Math.min(minY,yy);maxY=Math.max(maxY,yy);
                        int ld=Math.abs(yy-yc),cd=Math.max(Math.abs(((info>>>8)&511)-centerRG),Math.abs(((info>>>17)&511)-centerBG));
                        if(ld>lumaLimit || cd>chromaLimit)continue;
                        int w=(cache==null?(radius==RESIDUAL_RADIUS?WIDE_SPATIAL[xi]*WIDE_SPATIAL[yi]:SPATIAL[dx+3]*SPATIAL[dy+3]):cache.spatial[xi]*cache.spatial[yi])*range[ld];
                        if(cd>8)w=w*8/cd;
                        if(w==0)continue;
                        weight+=w;rr+=w*r;gg+=w*g;bb+=w*b;
                        if(Math.abs(dx)<=1 && Math.abs(dy)<=1){nearWeight+=w;nr+=w*r;ng+=w*g;nb+=w*b;}
                    }
                }
                if(weight==0 || nearWeight==0)continue;
                int edge=Math.max(lc==0||rc==0?0:Math.abs(left/lc-right/rc),uc==0||dc==0?0:Math.abs(up/uc-down/dc));
                int periodic=cache==null?textureQ8(input,width,rows,col,row,validBegin,validEnd,measured,radius)
                    :cache.texture(col,row,validBegin,validEnd,Math.max(3,Math.round(measured*.75f)),samples);
                // Random grain cancels in opposite half-window means. A coherent
                // scene edge or repeated fabric pattern does not. Keep a smooth
                // transition, rather than hard tiles or intensity quantisation.
                int tolerance=3+Math.round(measured*.55f);
                int flat=Math.max(0,Math.min(256,(tolerance*3-edge)*256/Math.max(1,tolerance*2)));
                flat=flat*(256-periodic)>>8;
                if(flat==0)continue;
                int dark=shadows?tone(yc):0;
                int fine=(fineBase+(dark*8>>8))*flat>>8;
                int coarse=(coarseBase+(dark*8>>8))*flat*flat>>16;
                // Face/beauty coordination is applied only once, with the real local
                // range. Eye/lip detail masks and spatial noise budgets remain intact.
                if(plan!=null){
                    int coordinated=budgetQ8(maxY-minY,global,plan.beautyQ8,skin,smoothLimit);
                    if(base>0)budget=Math.min(256,budget*coordinated/base);
                }else budget=QualityPipeline1932.shadowBudgetQ8(center,maxY-minY,col,row);
                fine=Math.min(248,fine)*budget>>8;
                coarse=Math.min(232,coarse)*budget>>8;
                if(fine==0 && coarse==0)continue;
                int r1=(nr+nearWeight/2)/nearWeight,g1=(ng+nearWeight/2)/nearWeight,b1=(nb+nearWeight/2)/nearWeight;
                int r2=(rr+weight/2)/weight,g2=(gg+weight/2)/weight,b2=(bb+weight/2)/weight;
                // Fine residual = center - radius1. Coarse residual = radius1 -
                // sparse radius4. Shrink both independently; neither stage samples writes
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
        } finally {if(cache!=null)cache.close();}
    }
    private static NoiseCache1944 borrowCache(int[] input,int width,int radius) {
        if((long)width*(radius*2+1)>Integer.MAX_VALUE)return null;
        try{return new NoiseCache1944(input,width,radius);}
        catch(OutOfMemoryError optionalWorkspace){SpeedWorkers1935.trim();return null;}
    }
    private static int packInfo(int p) {
        if((p>>>24)!=255)return 0;
        int r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
        return NoiseCache1944.OPAQUE|y(p)|((r-g+255)<<8)|((b-g+255)<<17);
    }
    /** One JNI aggregate per bounded tile, then exact Java policy/finalization. The
     * complete original input is immutable and native failure can rerun Java safely. */
    private static boolean smoothNative(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int noise,boolean shadows,int radius,
            QualityPixels1932.Plan plan,int originY) {
        int tileRows=Math.min(32,end-begin),capacity=width*tileRows;
        if(capacity>Integer.MAX_VALUE/3)return false;
        int[] meta=null,policy=null,summary=null;
        try {
            meta=SpeedWorkers1935.borrowInts(capacity);
            policy=SpeedWorkers1935.borrowInts(capacity*3);
            summary=SpeedWorkers1935.borrowInts(capacity*3);
            final int global=plan==null?256:plan.shadowBudgetQ8;
            final int stronger=global+((256-global)*noise>>2);
            final int fineBase=128+noise*30,coarseBase=96+noise*34;
            final int smoothLimit=plan==null?0:10+Math.round(Math.min(12f,plan.sourceSigma)*2f);
            for(int start=begin;start<end;start+=tileRows) {
                if(Thread.currentThread().isInterrupted())return true;
                int limit=Math.min(end,start+tileRows),count=(limit-start)*width;
                for(int row=start;row<limit;row++)for(int col=0;col<width;col++) {
                    int index=(row-start)*width+col,center=input[row*width+col];
                    meta[index]=0;if((center>>>24)!=255)continue;
                    int absoluteY=row+originY;
                    float measured=plan==null?3f+noise*1.8f:plan.localSigmaAt(col,absoluteY);
                    int skin=plan==null || global==0 || plan.beautyQ8==0 || plan.faceRegions==null || absoluteY<0
                        ?0:plan.skinAt(col,absoluteY);
                    int base=plan==null?256:budgetQ8(0,global,plan.beautyQ8,skin,smoothLimit);
                    int budget=plan==null?256:global==0?0:base*stronger/global;
                    if(plan!=null && plan.localNoise!=null)budget=budget*residualConfidenceQ8(measured)>>8;
                    if(plan!=null && plan.faceRegions!=null)
                        budget=budget*(256-(Math.max(0,Math.min(256,plan.detailAt(col,absoluteY)))*208>>8))>>8;
                    if(budget<=0 || measured<.20f)continue;
                    int sigma=Math.max(2,Math.min(32,Math.round(measured*1.4f+2f)));
                    int threshold=Math.max(3,Math.round(measured*.75f));
                    int tolerance=3+Math.round(measured*.55f);
                    meta[index]=Integer.MIN_VALUE|sigma|(threshold<<6)|(tolerance<<12)|(y(center)<<18);
                    policy[index*3]=budget;policy[index*3+1]=base;policy[index*3+2]=skin;
                }
                if(!NativeSpeed1944.aggregate(input,meta,summary,width,rows,start,limit,validBegin,validEnd,radius,FLAT_RANGE))return false;
                for(int index=0;index<count;index++) {
                    int stats=summary[index*3+2];if(stats>=0)continue;
                    int tolerance=(meta[index]>>>12)&63,edge=(stats>>>8)&255,periodic=(stats>>>16)&511;
                    int flat=Math.max(0,Math.min(256,(tolerance*3-edge)*256/Math.max(1,tolerance*2)));
                    flat=flat*(256-periodic)>>8;if(flat==0)continue;
                    int yc=(meta[index]>>>18)&255,dark=shadows?tone(yc):0;
                    int fine=(fineBase+(dark*8>>8))*flat>>8;
                    int coarse=(coarseBase+(dark*8>>8))*flat*flat>>16;
                    int row=start+index/width,col=index%width,at=row*width+col,center=input[at];
                    int budget=policy[index*3],base=policy[index*3+1];
                    if(plan!=null) {
                        int coordinated=budgetQ8(stats&255,global,plan.beautyQ8,policy[index*3+2],smoothLimit);
                        if(base>0)budget=Math.min(256,budget*coordinated/base);
                    }else budget=QualityPipeline1932.shadowBudgetQ8(center,stats&255,col,row);
                    fine=Math.min(248,fine)*budget>>8;coarse=Math.min(232,coarse)*budget>>8;
                    if(fine==0 && coarse==0)continue;
                    int near=summary[index*3],wide=summary[index*3+1];
                    int r1=(near>>>16)&255,g1=(near>>>8)&255,b1=near&255;
                    int r2=(wide>>>16)&255,g2=(wide>>>8)&255,b2=wide&255;
                    int fy=y(near),cy=y(wide);
                    int newY=yc+roundDiv((fy-yc)*fine+(cy-fy)*coarse,256);
                    newY=Math.max(Math.min(yc,Math.min(fy,cy)),Math.min(Math.max(yc,Math.max(fy,cy)),newY));
                    int colorFine=fine*7/8,colorCoarse=coarse*7/8;
                    int cr=(center>>>16)&255,cg=(center>>>8)&255,cb=center&255;
                    int or=clamp(cr+roundDiv((r1-cr)*colorFine+(r2-r1)*colorCoarse,256));
                    int og=clamp(cg+roundDiv((g1-cg)*colorFine+(g2-g1)*colorCoarse,256));
                    int ob=clamp(cb+roundDiv((b1-cb)*colorFine+(b2-b1)*colorCoarse,256));
                    int delta=newY-y(0xff000000|(or<<16)|(og<<8)|ob);
                    delta=Math.max(-Math.min(or,Math.min(og,ob)),Math.min(255-Math.max(or,Math.max(og,ob)),delta));
                    output[at]=(center&0xff000000)|((or+delta)<<16)|((og+delta)<<8)|(ob+delta);
                }
            }
            return true;
        } catch(OutOfMemoryError optionalWorkspace) {
            SpeedWorkers1935.trim();return false;
        } finally {
            SpeedWorkers1935.release(meta);SpeedWorkers1935.release(policy);SpeedWorkers1935.release(summary);
        }
    }
    /** Exact original beauty coordination, with the immutable per-pixel skin lookup reused. */
    private static int budgetQ8(int localRange,int global,int beauty,int skin,int smoothLimit) {
        if(global==0 || beauty==0 || skin==0)return global;
        int smooth=Math.max(0,Math.min(256,(smoothLimit-Math.max(0,localRange))*256/Math.max(1,smoothLimit)));
        int reduction=(int)((long)skin*smooth*beauty*112>>24);
        return global*(256-reduction)>>8;
    }
    private static int roundDiv(int value,int divisor){return value<0?-((-value+divisor/2)/divisor):(value+divisor/2)/divisor;}
    static int residualConfidenceQ8(float sigma){
        float t=Math.max(0f,Math.min(1f,(sigma-.20f)/1.0f));
        return Math.round(256f*t*t*(3f-2f*t));
    }

    /** Opposite phase/repeat agreement distinguishes periodic real luma texture
     * from incoherent random grain. It never reads beyond the processed halo. */
    static int textureQ8(int[] src,int width,int rows,int x,int row,int lo,int hi,float sigma,int radius){
        int support=Math.min(RESIDUAL_RADIUS,radius);
        if(support<3 || x<support || x>=width-support || row<lo+support || row>=hi-support)return 0;
        int best=0,threshold=Math.max(3,Math.round(sigma*.75f));
        int[] samples=TEXTURE_SAMPLES.get();int length=support*2+1,at=row*width+x;
        for(int direction=0;direction<4;direction++){
            int step=direction==0?1:direction==1?width:direction==2?width+1:width-1;
            boolean opaque=true;int min=255,max=0,adjacent=0;
            for(int i=0;i<length;i++){
                int p=src[at+(i-support)*step];if((p>>>24)!=255){opaque=false;break;}
                int value=samples[i]=y(p);min=Math.min(min,value);max=Math.max(max,value);
                if(i>0)adjacent+=Math.abs(value-samples[i-1]);
            }
            if(!opaque || max-min<threshold*2 || adjacent<(length-1)*threshold)continue;
            int average=(adjacent+(length-2)/2)/(length-1);
            // Real repeated cloth/mesh phase matches at period2/3/4. Random
            // grain has similar difference energy at adjacent and repeated taps.
            // Require repeat error below 45% of adjacent-phase contrast; strong
            // stable structure receives full protection even at low amplitude.
            for(int period=2;period<=Math.min(4,length-4);period++){
                int error=0,count=length-period;
                for(int i=0;i<count;i++)error+=Math.abs(samples[i]-samples[i+period]);
                error=(error+count/2)/count;
                int confidence=Math.max(0,Math.min(256,(average*4-error*9)*256/Math.max(1,average*4)));
                confidence=confidence*confidence>>8;
                best=Math.max(best,confidence);
            }
        }
        return best;
    }
}
