package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** NR9–NR12 single-image image-domain NR. Coarse corrections are prepared from an
 * actual half/quarter/eighth pyramid of the already corrected, single-frame image.
 * Regional random-noise and periodic-detail evidence improve flat/edge decisions;
 * same-image NLM is additionally run at half and original resolution where safe.
 * No camera burst, RAW surrogate, full-resolution guide copy, or generated detail
 * is used. The Java implementation is the optional JNI backend's oracle.
 */
public final class StrongNoise1958 {
    private StrongNoise1958() {}
    public static final int HALO=18;
    private static final int PREP_ROWS=128;
    private static final int REGION=64;
    private static final float[][] D8=dctBasis();
    private static volatile int nativeState;
    public interface Patches { void read(int[] pixels,int x,int y,int width,int height); }
    public interface Protection { int budgetQ8(int x,int y); int detailQ8(int x,int y); }

    /** Preparation peak: all original pyramid levels, the largest replacement
     * correction map, row read buffers and bounded optional JNI staging. */
    public static long modelMemoryBytes(int width,int height) {
        geometry(width,height);
        long half=pixels(half(width),half(height));
        long quarter=pixels(half(half(width)),half(half(height)));
        long eighth=pixels(half(half(half(width))),half(half(half(height))));
        return 4L*(half+quarter+eighth+half)+(long)width*PREP_ROWS*16L+2097152L+12L*((width+REGION-1)/REGION)*((height+REGION-1)/REGION);
    }
    /** Additional worker reservation beyond caller-owned input/output halo rows.
     * Includes Java policy, JNI copies of input/policy/coarse map rows, and the
     * transactional result. Model maps are copied by row region, never wholesale. */
    public static long workspaceBytes(int width,int coreRows) {
        if(width<1||coreRows<1)throw new IllegalArgumentException("strong NR workspace");
        return (long)width*(coreRows*48L+HALO*32L)+1048576L;
    }
    public static final class Model {
        public final int width,height;
        private final int[] halfMap,quarterMap,eighthMap;
        private final float[] evidence,runtimeEvidence;
        private boolean measuredActive;
        public final int fullNonlocalCandidates=8;
        /** Maximum configured candidate count when quarter evidence permits NLM;
         * this is a search bound, not a count of actually accepted/executed patches. */
        public final int nonlocalCandidates;
        private Model(int width,int height,int[] h,int[] q,int[] e,float[] evidence,int nl) {
            this.width=width;this.height=height;halfMap=h;quarterMap=q;eighthMap=e;
            this.evidence=evidence;nonlocalCandidates=nl;runtimeEvidence=new float[16+3*((width+REGION-1)/REGION)*((height+REGION-1)/REGION)];System.arraycopy(evidence,0,runtimeEvidence,0,16);for(float value:evidence)if(value>=.40f)measuredActive=true;
        }
        public long residentBytes() {return 4L*(halfMap.length+quarterMap.length+eighthMap.length+runtimeEvidence.length)+1024L;}
        public float sigmaY(int scale) {return average(evidence,scale*16);}
        public float sigmaC(int scale) {return average(evidence,scale*16+8);}
        private boolean active() {return measuredActive;}
        /** NR13 confidence derives from the same noisy-flat decision as NR9/10.
         * Caller applies its face/detail policy before storing or using this mask. */
        public int smoothingQ8(int[] input,int width,int rows,int x,int row,int validBegin,int validEnd,int originY) {return smoothingQ8(input,width,rows,x,row,validBegin,validEnd,originY,new float[2]);}
        public int smoothingQ8(int[] input,int width,int rows,int x,int row,int validBegin,int validEnd,int originY,float[] flat) {
            if(input==null||width!=this.width||rows<1||input.length<(long)width*rows||x<0||x>=width||row<validBegin||row>=validEnd||validBegin<0||validEnd>rows||originY+validBegin<0||originY+validEnd>height)throw new IllegalArgumentException("smooth NR confidence range");
            int p=input[row*width+x];if((p>>>24)!=255||!active())return 0;
            float y=luma(p),sy=regionalSigma(runtimeEvidence,this.width,height,x,row+originY,0),sc=regionalSigma(runtimeEvidence,this.width,height,x,row+originY,1);
            if(flat==null||flat.length<2)throw new IllegalArgumentException("smooth NR confidence scratch");guide(input,width,rows,x,row,validBegin,validEnd,sy,sc,flat);
            return Math.round(256*clamp((flat[0]-.65f)/.30f,0,1)*clamp((Math.max(sy,sc)-.60f)/1.8f,0,1)*(1-regionalSigma(runtimeEvidence,width,height,x,row+originY,2)));
        }
    }

    public static Model prepare(Patches source,int width,int height,int noise,boolean shadows) {
        return prepareInternal(source,width,height,noise,shadows,true);
    }
    static Model prepareJava(Patches source,int width,int height,int noise,boolean shadows) {
        return prepareInternal(source,width,height,noise,shadows,false);
    }
    private static Model prepareInternal(Patches source,int width,int height,int noise,boolean shadows,boolean useNative) {
        geometry(width,height);if(source==null||noise<0||noise>4)throw new IllegalArgumentException("strong NR source/settings");
        checkInterrupted();int hw=half(width),hh=half(height),qw=half(hw),qh=half(hh),ew=half(qw),eh=half(qh);
        if(noise==0)return new Model(width,height,new int[0],new int[0],new int[0],new float[64],0);
        int[] h=new int[checkedPixels(hw,hh)];
        // Only two full-width source rows are materialized while making the half
        // image. Nonopaque pairs are excluded from all subsequent NR evidence.
        int[] sourceRows=new int[checkedPixels(width,Math.min(2,height))];
        for(int y=0;y<hh;y++) {
            checkInterrupted();int rows=Math.min(2,height-y*2);source.read(sourceRows,0,y*2,width,rows);
            for(int x=0;x<hw;x++)h[y*hw+x]=average2(sourceRows,width,rows,x*2,0);
        }
        int[] q=downsample(h,hw,hh),e=downsample(q,qw,qh);
        float[] evidence=new float[64];estimateSource(source,width,height,evidence,0);
        estimate(h,hw,hh,evidence,16);estimate(q,qw,qh,evidence,32);estimate(e,ew,eh,evidence,48);
        int nl=0;
        int[] maps=new int[h.length];prepareScale(h,maps,hw,hh,noise,shadows,evidence,16,0,useNative);h=maps;
        maps=new int[q.length];prepareScale(q,maps,qw,qh,noise,shadows,evidence,32,1,useNative);q=maps;
        maps=new int[e.length];prepareScale(e,maps,ew,eh,noise,shadows,evidence,48,2,useNative);e=maps;
        // NR8 search geometry is fixed. Its real execution/effect is tested by
        // comparing local-only and NLM quarter-map results, rather than this bound.
        for(int i=32;i<48;i++)if(evidence[i]>=.40f){nl=24;break;}
        Model result=new Model(width,height,h,q,e,evidence,nl);estimateRegions(source,width,height,result.runtimeEvidence,evidence);for(int i=16;i<result.runtimeEvidence.length;i+=3)if(result.runtimeEvidence[i]>=.40f||result.runtimeEvidence[i+1]>=.40f)result.measuredActive=true;return result;
    }
    private static int[] downsample(int[] src,int width,int height) {
        int ow=half(width),oh=half(height);int[] out=new int[checkedPixels(ow,oh)];
        for(int y=0;y<oh;y++){checkInterrupted();for(int x=0;x<ow;x++)out[y*ow+x]=average2(src,width,height,x*2,y*2);}
        return out;
    }
    private static int average2(int[] src,int width,int height,int x,int y) {
        int r=0,g=0,b=0,n=0;boolean opaque=true;
        for(int yy=y;yy<Math.min(height,y+2);yy++)for(int xx=x;xx<Math.min(width,x+2);xx++) {
            int p=src[yy*width+xx];opaque&=(p>>>24)==255;r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;n++;
        }
        return (opaque?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;
    }
    private static void estimateSource(Patches source,int width,int height,float[] dst,int offset) {
        int pw=Math.min(48,width),ph=Math.min(48,height);int[] patch=new int[checkedPixels(pw,ph)];
        int[][] hist=new int[16][128];int[] counts=new int[16];
        int nx=Math.min(9,Math.max(1,(width+383)/384)),ny=Math.min(9,Math.max(1,(height+383)/384));
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
            checkInterrupted();int x=nx==1?(width-pw)/2:(width-pw)*gx/(nx-1),y=ny==1?(height-ph)/2:(height-ph)*gy/(ny-1);
            source.read(patch,x,y,pw,ph);estimateSamples(patch,pw,ph,hist,counts,1);
        }
        finishEstimate(hist,counts,dst,offset);
    }
    private static void estimate(int[] src,int width,int height,float[] dst,int offset) {
        int step=Math.max(1,(int)Math.sqrt((long)width*height/24000.0));
        int[][] hist=new int[16][128];int[] counts=new int[16];
        estimateLag(src,width,height,hist,counts,step,1);finishEstimate(hist,counts,dst,offset);
        estimateSpectral(src,width,height,dst,offset);
    }
    private static void estimateSpectral(int[] src,int width,int height,float[] dst,int offset) {
        if(width<8||height<8)return;
        int[][] hist=new int[16][128];int[] counts=new int[16];
        int nx=Math.min(17,Math.max(1,width/16)),ny=Math.min(17,Math.max(1,height/16));
        float[][] planes={new float[64],new float[64],new float[64]};
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
            checkInterrupted();int x=nx==1?(width-8)/2:(width-8)*gx/(nx-1),y=ny==1?(height-8)/2:(height-8)*gy/(ny-1);
            float mean=0;boolean opaque=true;
            for(int py=0;py<8;py++)for(int px=0;px<8;px++){int p=src[(y+py)*width+x+px],at=py*8+px;opaque&=(p>>>24)==255;float yy=luma(p);mean+=yy;planes[0][at]=yy;planes[1][at]=((p>>>16)&255)-yy;planes[2][at]=(p&255)-yy;}
            mean/=64;if(!opaque||mean<3||mean>252)continue;int bin=Math.min(7,(int)mean/32);
            // A sparse real periodic texture occupies a few coefficients. The
            // median over the low-frequency band measures distributed coarse
            // grain while rejecting those sparse structured coefficients.
            for(int v=0;v<6;v++)for(int u=0;u<6;u++)if(u+v>=1&&u+v<=5)for(int plane=0;plane<3;plane++) {
                float coefficient=0;for(int py=0;py<8;py++)for(int px=0;px<8;px++)coefficient+=(planes[plane][py*8+px]- (plane==0?mean:0))*D8[u][px]*D8[v][py];
                int at=bin+(plane==0?0:8),value=Math.min(127,Math.round(Math.abs(coefficient)*4));hist[at][value]++;counts[at]++;
            }
        }
        float[] measured=new float[16];finishEstimate(hist,counts,measured,0);
        for(int i=0;i<16;i++)dst[offset+i]=Math.max(dst[offset+i],measured[i]);
    }
    private static void estimateSamples(int[] src,int width,int height,int[][] hist,int[] counts,int step) {
        estimateLag(src,width,height,hist,counts,step,1);
    }
    private static void estimateLag(int[] src,int width,int height,int[][] hist,int[] counts,int step,int lag) {
        for(int y=0;y+lag<height;y+=step) {
            checkInterrupted();for(int x=0;x+lag<width;x+=step) {
                int a=src[y*width+x],b=src[y*width+x+lag],c=src[(y+lag)*width+x],d=src[(y+lag)*width+x+lag];
                if((a>>>24)!=255||(b>>>24)!=255||(c>>>24)!=255||(d>>>24)!=255)continue;
                float ya=luma(a),yb=luma(b),yc=luma(c),yd=luma(d),mean=(ya+yb+yc+yd)*.25f;
                if(mean<3||mean>252)continue;
                int bin=Math.min(7,(int)mean/32);
                float dy=(ya-yb-yc+yd)*.5f;
                float cr=(((a>>>16)&255)-ya-(((b>>>16)&255)-yb)-(((c>>>16)&255)-yc)+(((d>>>16)&255)-yd))*.5f;
                float cb=((a&255)-ya-((b&255)-yb)-((c&255)-yc)+((d&255)-yd))*.5f;
                int sy=Math.min(127,Math.round(Math.abs(dy)*4)),sc=Math.min(127,Math.round((Math.abs(cr)+Math.abs(cb))*2));
                hist[bin][sy]++;counts[bin]++;hist[bin+8][sc]++;counts[bin+8]++;
            }
        }
    }
    private static void finishEstimate(int[][] hist,int[] count,float[] dst,int offset) {
        for(int b=0;b<16;b++)if(count[b]>=16) {
            int sum=0,target=(count[b]+1)/2;
            for(int i=0;i<128;i++){sum+=hist[b][i];if(sum>=target){dst[offset+b]=i/2.69796f;break;}}
        }
        // Fill unsupported brightness bins from actual measured bins only. A
        // quantization-clean image has all-zero evidence and remains unchanged.
        for(int plane=0;plane<2;plane++)for(int i=0;i<8;i++)if(count[plane*8+i]<16) {
            int best=-1,dist=99;for(int j=0;j<8;j++)if(count[plane*8+j]>=16&&Math.abs(i-j)<dist){best=j;dist=Math.abs(i-j);}
            if(best>=0)dst[offset+plane*8+i]=dst[offset+plane*8+best];
        }
    }

    private static void prepareScale(int[] src,int[] out,int width,int height,int noise,boolean shadows,float[] all,int offset,int mode,boolean useNative) {
        float[] ev=Arrays.copyOfRange(all,offset,offset+16);
        for(int begin=0;begin<height;begin+=PREP_ROWS) {
            checkInterrupted();int end=Math.min(height,begin+PREP_ROWS);boolean complete=false;
            if(useNative&&nativeAvailable())try {
                complete=processNative(src,out,width,height,begin,end,0,height,0,noise,shadows,ev,null,null,null,width,height,null,mode);
                checkInterrupted();
            }catch(UnsatisfiedLinkError unavailable){nativeState=-1;}
            if(!complete)prepareJavaRange(src,out,width,height,begin,end,noise,shadows,ev,mode);
        }
    }
    /** NR9/10/12 use coherent, independently measured luma and colour guides.
     * NR11 adds same-image bounded NLM to half resolution as well as quarter. */
    static void prepareJavaRange(int[] src,int[] out,int width,int height,int begin,int end,int noise,boolean shadows,float[] ev,int mode) {
        prepareJavaRangeInternal(src,out,width,height,begin,end,noise,shadows,ev,mode,true);
    }
    static void prepareJavaRangeLocalOnly(int[] src,int[] out,int width,int height,int begin,int end,int noise,boolean shadows,float[] ev,int mode) {
        prepareJavaRangeInternal(src,out,width,height,begin,end,noise,shadows,ev,mode,false);
    }
    private static void prepareJavaRangeInternal(int[] src,int[] out,int width,int height,int begin,int end,int noise,boolean shadows,float[] ev,int mode,boolean useNlm) {
        float[] nl=new float[3],flat=new float[2];
        for(int y=begin;y<end;y++) {
            checkInterrupted();for(int x=0;x<width;x++) {
                int p=src[y*width+x];float y0=luma(p),cR=((p>>>16)&255)-y0,cB=(p&255)-y0;
                if((p>>>24)!=255){out[y*width+x]=0;continue;}
                float sy=sigma(ev,0,y0),sc=sigma(ev,8,y0);
                if(sy<.40f&&sc<.40f){out[y*width+x]=(Math.round(y0)<<24);continue;}
                guide(src,width,height,x,y,0,height,sy,sc,flat);float legacyFlat=1/(1+square(structure(src,width,height,x,y,0,height)/(sy*3+3)));
                float thresholdY=Math.max(3,sy*4.5f),thresholdC=Math.max(5,sc*5.5f);
                float sumY=0,sumR=0,sumB=0,weightY=0,weightC=0,varY=0;
                for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
                    int q=src[clamp(y+dy,0,height-1)*width+clamp(x+dx,0,width-1)];if((q>>>24)!=255)continue;
                    float yy=luma(q),rr=((q>>>16)&255)-yy,bb=(q&255)-yy;
                    float base=dx==0&&dy==0?4f:Math.abs(dx)<=1&&Math.abs(dy)<=1?2f:1f;
                    float wy=base/(1+square((yy-y0)/thresholdY));
                    float wc=base/(1+.12f*square((yy-y0)/thresholdY)+square((rr-cR)/thresholdC)+square((bb-cB)/thresholdC));
                    sumY+=wy*yy;weightY+=wy;varY+=wy*square(yy-y0);sumR+=wc*rr;sumB+=wc*bb;weightC+=wc;
                }
                float dark=clamp((144-y0)/112,0,1),strength=.60f+.08f*noise+(shadows?.10f*dark:0);
                float localVar=varY/Math.max(.001f,weightY);
                float ly=sy>=.40f?Math.min(.98f,strength)*legacyFlat*clamp(sy*sy/Math.max(sy*sy,localVar*.55f),.12f,1):0;
                float lc=sc>=.40f?Math.min(.99f,strength+.10f)*(mode==0?flat[1]:Math.max(flat[1],legacyFlat)):0;
                float targetY=sumY/Math.max(.001f,weightY),targetR=sumR/Math.max(.001f,weightC),targetB=sumB/Math.max(.001f,weightC);
                if(useNlm&&mode<=1&&(sy>=.60f||sc>=.60f)&&flat[0]>.72f) {
                    if(mode==1)nonlocal(src,width,height,x,y,sy,sc,nl);
                    else nonlocalFine(src,width,height,x,y,0,height,sy,sc,nl);
                    float blend=(mode==1?.65f:.78f)*flat[0];
                    targetY=targetY*(1-blend)+nl[0]*blend;targetR=targetR*(1-blend)+nl[1]*blend;targetB=targetB*(1-blend)+nl[2]*blend;
                    ly=Math.min(.98f,ly+.15f*flat[0]);lc=Math.min(.99f,lc+.10f*flat[1]);
                }
                float dy=(targetY-y0)*ly,dr=(targetR-cR)*lc,db=(targetB-cB)*lc;
                out[y*width+x]=pack(Math.round(y0),dy+dr,dy-(.299f*dr+.114f*db)/.587f,dy+db);
            }
        }
    }
    private static void nonlocal(int[] src,int width,int height,int x,int y,float sy,float sc,float[] result) {
        int p=src[y*width+x];float yy=luma(p),rr=((p>>>16)&255)-yy,bb=(p&255)-yy;
        float total=1,sumY=yy,sumR=rr,sumB=bb;
        float variance=sy*sy+.22f*sc*sc,h2=Math.max(1.5f,variance*2.6f);
        for(int dy=-6;dy<=6;dy+=3)for(int dx=-6;dx<=6;dx+=3) {
            if(dx==0&&dy==0)continue;int cx=x+dx,cy=y+dy;if(cx<0||cy<0||cx>=width||cy>=height)continue;
            int q=src[cy*width+cx];if((q>>>24)!=255)continue;float distance=0;boolean valid=true;
            for(int py=-1;py<=1;py++)for(int px=-1;px<=1;px++) {
                int a=src[clamp(y+py,0,height-1)*width+clamp(x+px,0,width-1)];
                int b=src[clamp(cy+py,0,height-1)*width+clamp(cx+px,0,width-1)];
                if((a>>>24)!=255||(b>>>24)!=255){valid=false;continue;}
                float ya=luma(a),yb=luma(b),ra=((a>>>16)&255)-ya,rb=((b>>>16)&255)-yb,ba=(a&255)-ya,bc=(b&255)-yb;
                distance+=square(ya-yb)+.11f*(square(ra-rb)+square(ba-bc));
            }
            if(!valid)continue;
            distance=Math.max(0,distance/9-2*variance);
            // Rational exponential approximation avoids architecture-dependent
            // libm exp and has a sharp cutoff for statistically dissimilar patches.
            float z=distance/h2;if(z>5)continue;float weight=1/(1+z+z*z*.5f+z*z*z/6);
            float qy=luma(q);sumY+=weight*qy;sumR+=weight*(((q>>>16)&255)-qy);sumB+=weight*((q&255)-qy);total+=weight;
        }
        result[0]=sumY/total;result[1]=sumR/total;result[2]=sumB/total;
    }

    public static void processRange(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        validate(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,model);checkInterrupted();
        if(noise>0&&model.active()&&end>begin&&end-begin<=256&&(long)width*(end-begin)<=Integer.MAX_VALUE/2&&nativeAvailable()) {
            int[] policy=null;
            if(protection!=null){policy=new int[checkedPixels(width,end-begin)*2];for(int row=begin;row<end;row++){checkInterrupted();for(int x=0;x<width;x++){int at=((row-begin)*width+x)*2;policy[at]=clamp(protection.budgetQ8(x,row+originY),0,256);policy[at+1]=clamp(protection.detailQ8(x,row+originY),0,256);}}}
            try{boolean complete=processNative(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model.runtimeEvidence,model.halfMap,model.quarterMap,model.eighthMap,model.width,model.height,policy,3);checkInterrupted();if(complete)return;}catch(UnsatisfiedLinkError unavailable){nativeState=-1;}
        }
        processJavaRange(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection);
    }
    static void processJavaRange(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        processJavaRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,true);
    }
    static void processJavaRangeLocalOnly(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection) {
        processJavaRangeInternal(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,false);
    }
    private static void processJavaRangeInternal(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,Model model,Protection protection,boolean useNlm) {
        validate(input,output,width,rows,begin,end,validBegin,validEnd,originY,noise,model);checkInterrupted();
        if(noise==0||!model.active()){System.arraycopy(input,begin*width,output,begin*width,(end-begin)*width);return;}
        float[] coarse=new float[4],flat=new float[2],nl=new float[3];
        for(int row=begin;row<end;row++) {
            checkInterrupted();int ay=row+originY;
            for(int x=0;x<width;x++) {
                int at=row*width+x,p=input[at];if((p>>>24)!=255){output[at]=p;continue;}
                int budget=protection==null?256:clamp(protection.budgetQ8(x,ay),0,256),detail=protection==null?0:clamp(protection.detailQ8(x,ay),0,256);
                if(budget==0){output[at]=p;continue;}
                float y0=luma(p),r0=((p>>>16)&255)-y0,b0=(p&255)-y0;
                float sy=regionalSigma(model.runtimeEvidence,width,model.height,x,ay,0),sc=regionalSigma(model.runtimeEvidence,width,model.height,x,ay,1);
                guide(input,width,rows,x,row,validBegin,validEnd,sy,sc,flat);
                float dark=clamp((144-y0)/112,0,1),thresholdY=Math.max(2.5f,sy*4),thresholdC=Math.max(5,sc*5);
                float sumY=0,sumR=0,sumB=0,wyTotal=0,wcTotal=0,var=0;
                for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
                    int q=input[clamp(row+dy,validBegin,validEnd-1)*width+clamp(x+dx,0,width-1)];if((q>>>24)!=255)continue;
                    float yy=luma(q),rr=((q>>>16)&255)-yy,bb=(q&255)-yy,base=dx==0&&dy==0?4f:Math.abs(dx)<=1&&Math.abs(dy)<=1?2f:1f;
                    float wy=base/(1+square((yy-y0)/thresholdY)),wc=base/(1+.12f*square((yy-y0)/thresholdY)+square((rr-r0)/thresholdC)+square((bb-b0)/thresholdC));
                    sumY+=wy*yy;wyTotal+=wy;var+=wy*square(yy-y0);sumR+=wc*rr;sumB+=wc*bb;wcTotal+=wc;
                }
                float strength=(.56f+.09f*noise+(shadows?.12f*dark:0))*((160+budget*.375f)/256f),texture=(1-.55f*detail/256f)*(1-.85f*regionalSigma(model.runtimeEvidence,width,model.height,x,ay,2));
                float confidence=clamp((flat[0]-.62f)/.34f,0,1),residual=clamp((Math.max(sy,sc)-.60f)/3f,0,1);
                float ly=sy>=.40f?Math.min(.98f,strength*(.70f+.30f*confidence*residual))*flat[0]*texture*clamp(sy*sy/Math.max(sy*sy,var/Math.max(.001f,wyTotal)*.55f),.16f,1):0;
                float lc=sc>=.40f?Math.min(.995f,strength+.15f*residual)*flat[1]*texture:0;
                float targetY=sumY/Math.max(.001f,wyTotal),targetR=sumR/Math.max(.001f,wcTotal),targetB=sumB/Math.max(.001f,wcTotal);
                if(useNlm&&confidence>.45f&&residual>.15f&&detail<192&&texture>.6f) {
                    nonlocalFine(input,width,rows,x,row,validBegin,validEnd,sy,sc,nl);
                    float by=.84f*confidence*residual,bc=.88f*confidence*residual*flat[1];
                    targetY=targetY*(1-by)+nl[0]*by;targetR=targetR*(1-bc)+nl[1]*bc;targetB=targetB*(1-bc)+nl[2]*bc;
                    ly=Math.min(.985f,ly+.18f*confidence*residual*texture);lc=Math.min(.995f,lc+.10f*confidence*residual*texture);
                }
                float dy=(targetY-y0)*ly,dr=(targetR-r0)*lc,db=(targetB-b0)*lc;
                float dR=dy+dr,dG=dy-(.299f*dr+.114f*db)/.587f,dB=dy+db;
                for(int k=0;k<3;k++) {
                    int scale=1<<(k+1);int[] map=k==0?model.halfMap:k==1?model.quarterMap:model.eighthMap;
                    sample(map,halfWidth(model.width,scale),halfWidth(model.height,scale),x,ay,scale,coarse);
                    float gate=1/(1+square((coarse[3]-y0)/(sy*5+16)));
                    float blend=(k==0?.48f:k==1?.32f:.22f)*Math.min(1,strength)*texture*gate;
                    float cy=.299f*coarse[0]+.587f*coarse[1]+.114f*coarse[2],cr=coarse[0]-cy,cb=coarse[2]-cy;
                    float by=blend*(.30f+.70f*flat[0]),bc=blend*flat[1];
                    dR+=cy*by+cr*bc;dG+=cy*by-(.299f*cr+.114f*cb)/.587f*bc;dB+=cy*by+cb*bc;
                }
                output[at]=(p&0xff000000)|(byteValue(((p>>>16)&255)+dR)<<16)|(byteValue(((p>>>8)&255)+dG)<<8)|byteValue((p&255)+dB);
            }
        }
    }
    private static void sample(int[] map,int width,int height,int x,int y,int scale,float[] out) {
        float fx=clamp((x+.5f)/scale-.5f,0,width-1),fy=clamp((y+.5f)/scale-.5f,0,height-1);int ix=(int)fx,iy=(int)fy;
        int nx=Math.min(width-1,ix+1),ny=Math.min(height-1,iy+1);fx-=ix;fy-=iy;
        int a=map[iy*width+ix],b=map[iy*width+nx],c=map[ny*width+ix],d=map[ny*width+nx];
        for(int plane=0;plane<4;plane++) {
            int shift=plane==3?24:16-plane*8;
            float av=plane==3?a>>>24:(byte)(a>>>shift),bv=plane==3?b>>>24:(byte)(b>>>shift),cv=plane==3?c>>>24:(byte)(c>>>shift),dv=plane==3?d>>>24:(byte)(d>>>shift);
            out[plane]=(av*(1-fx)+bv*fx)*(1-fy)+(cv*(1-fx)+dv*fx)*fy;
        }
    }
    /** The guide averages coherent opposite-side strips before testing edges.
     * A measured random-grain floor is removed; colour has its own boundary gate. */
    private static float structure(int[] src,int width,int height,int x,int y,int validBegin,int validEnd) {
        float left=0,right=0,up=0,down=0;int n=0;
        for(int k=-1;k<=1;k++) {
            int a=src[clamp(y+k,validBegin,validEnd-1)*width+clamp(x-2,0,width-1)],b=src[clamp(y+k,validBegin,validEnd-1)*width+clamp(x+2,0,width-1)];
            int c=src[clamp(y-2,validBegin,validEnd-1)*width+clamp(x+k,0,width-1)],d=src[clamp(y+2,validBegin,validEnd-1)*width+clamp(x+k,0,width-1)];
            if((a>>>24)!=255||(b>>>24)!=255||(c>>>24)!=255||(d>>>24)!=255)return 255;
            left+=luma(a);right+=luma(b);up+=luma(c);down+=luma(d);n++;
        }
        return (Math.abs(right-left)+Math.abs(down-up))/n;
    }
    private static void guide(int[] src,int width,int height,int x,int y,int validBegin,int validEnd,float sy,float sc,float[] out) {
        float ly=0,ry=0,uy=0,dy=0,lr=0,rr=0,ur=0,dr=0,lb=0,rb=0,ub=0,db=0;
        for(int k=-1;k<=1;k++)for(int d=2;d<=3;d++) {
            int a=src[clamp(y+k,validBegin,validEnd-1)*width+clamp(x-d,0,width-1)],b=src[clamp(y+k,validBegin,validEnd-1)*width+clamp(x+d,0,width-1)];
            int c=src[clamp(y-d,validBegin,validEnd-1)*width+clamp(x+k,0,width-1)],e=src[clamp(y+d,validBegin,validEnd-1)*width+clamp(x+k,0,width-1)];
            if((a>>>24)!=255||(b>>>24)!=255||(c>>>24)!=255||(e>>>24)!=255){out[0]=out[1]=0;return;}
            float ya=luma(a),yb=luma(b),yc=luma(c),ye=luma(e);ly+=ya;ry+=yb;uy+=yc;dy+=ye;
            lr+=((a>>>16)&255)-ya;rr+=((b>>>16)&255)-yb;ur+=((c>>>16)&255)-yc;dr+=((e>>>16)&255)-ye;
            lb+=(a&255)-ya;rb+=(b&255)-yb;ub+=(c&255)-yc;db+=(e&255)-ye;
        }
        float centerY=0,centerR=0,centerB=0;
        for(int py=-1;py<=1;py++)for(int px=-1;px<=1;px++) {int p=src[clamp(y+py,validBegin,validEnd-1)*width+clamp(x+px,0,width-1)];if((p>>>24)!=255){out[0]=out[1]=0;return;}float yy=luma(p);centerY+=yy;centerR+=((p>>>16)&255)-yy;centerB+=(p&255)-yy;}
        float curveY=Math.max(Math.abs(ly+ry-centerY*4/3),Math.abs(uy+dy-centerY*4/3))/6;
        float curveC=Math.max(Math.abs(lr+rr-centerR*4/3)+Math.abs(lb+rb-centerB*4/3),Math.abs(ur+dr-centerR*4/3)+Math.abs(ub+db-centerB*4/3))/6;
        float edgeY=Math.max(Math.abs(ry-ly),Math.abs(dy-uy))/6;
        float edgeC=Math.max(Math.abs(rr-lr)+Math.abs(rb-lb),Math.abs(dr-ur)+Math.abs(db-ub))/6;
        edgeY=Math.max(0,edgeY-sy*.85f);edgeC=Math.max(0,edgeC-sc*2.0f);
        out[0]=1/(1+square(edgeY/(sy*1.7f+2))+square(Math.max(0,curveY-sy*1.4f)/(sy*.85f+1)));out[1]=1/(1+square(edgeC/(sc*1.0f+1.5f))+square(Math.max(0,curveC-sc*2.5f)/(sc*1.5f+2)));
    }
    private static void estimateRegions(Patches source,int width,int height,float[] runtime,float[] ev) {
        int rw=(width+REGION-1)/REGION,rh=(height+REGION-1)/REGION,pw=Math.min(32,width),ph=Math.min(32,height);int[] patch=new int[pw*ph];
        for(int gy=0;gy<rh;gy++)for(int gx=0;gx<rw;gx++) {
            checkInterrupted();int px=clamp(gx*REGION+REGION/2-pw/2,0,width-pw),py=clamp(gy*REGION+REGION/2-ph/2,0,height-ph);source.read(patch,px,py,pw,ph);
            int[][] hist=new int[16][128];int[] counts=new int[16];float[] measured=new float[16];estimateSamples(patch,pw,ph,hist,counts,1);finishEstimate(hist,counts,measured,0);
            float sumY=0,sumC=0,total=0;for(int b=0;b<8;b++){sumY+=measured[b]*counts[b];sumC+=measured[b+8]*counts[b+8];total+=counts[b];}
            int at=16+3*(gy*rw+gx);runtime[at]=total>=16?sumY/total:average(ev,0);runtime[at+1]=total>=16?sumC/total:average(ev,8);runtime[at+2]=periodicTexture(patch,pw,ph);
            // The wider Haar residual admits correlated grain that is nearly
            // invisible to a one-pixel estimate. Real periodic structure is
            // independently protected by the measured signed texture prior.
            hist=new int[16][128];counts=new int[16];measured=new float[16];estimateLag(patch,pw,ph,hist,counts,1,4);finishEstimate(hist,counts,measured,0);
            sumY=0;sumC=0;total=0;for(int b=0;b<8;b++){sumY+=measured[b]*counts[b];sumC+=measured[b+8]*counts[b+8];total+=counts[b];}
            if(total>=16){runtime[at]=Math.max(runtime[at],.65f*sumY/total);runtime[at+1]=Math.max(runtime[at+1],.65f*sumC/total);}
        }
    }
    /** Repeated signed structure has a negative spatial autocorrelation lobe.
     * White grain has none; this measured regional prior prevents faint periodic
     * detail being reclassified merely because its gradient is locally zero. */
    private static float periodicTexture(int[] patch,int width,int height) {
        if(width<24||height<24)return 0;float mean=0,energy=0;int count=0;
        for(int p:patch)if((p>>>24)==255){mean+=luma(p);count++;}if(count<width*height*.9f)return 0;mean/=count;
        for(int p:patch)if((p>>>24)==255)energy+=square(luma(p)-mean);energy/=count;if(energy<.50f)return 0;
        float lowest=0;
        for(int lag=8;lag<=12;lag+=4)for(int direction=0;direction<3;direction++) {
            int dx=direction==1?0:lag,dy=direction==0?0:lag;float covariance=0;int n=0;
            for(int y=0;y+dy<height;y++)for(int x=0;x+dx<width;x++){int a=patch[y*width+x],b=patch[(y+dy)*width+x+dx];if((a>>>24)!=255||(b>>>24)!=255)continue;covariance+=(luma(a)-mean)*(luma(b)-mean);n++;}
            if(n>=64)lowest=Math.min(lowest,covariance/n/energy);
        }
        return clamp((-lowest-.18f)/.30f,0,1);
    }
    private static float regionalSigma(float[] ev,int width,int height,int x,int y,int plane) {
        int rw=(width+REGION-1)/REGION,rh=(height+REGION-1)/REGION;float fx=clamp((x+.5f)/REGION-.5f,0,rw-1),fy=clamp((y+.5f)/REGION-.5f,0,rh-1);int ix=(int)fx,iy=(int)fy,nx=Math.min(rw-1,ix+1),ny=Math.min(rh-1,iy+1);fx-=ix;fy-=iy;
        float a=ev[16+3*(iy*rw+ix)+plane],b=ev[16+3*(iy*rw+nx)+plane],c=ev[16+3*(ny*rw+ix)+plane],d=ev[16+3*(ny*rw+nx)+plane];return (a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy;
    }
    /** Eight same-image candidates, radius 4 with a five-point cross patch. The
     * furthest read is five pixels; no candidate can cross the valid halo. */
    private static void nonlocalFine(int[] src,int width,int height,int x,int y,int validBegin,int validEnd,float sy,float sc,float[] result) {
        int p=src[y*width+x];float yy=luma(p),rr=((p>>>16)&255)-yy,bb=(p&255)-yy,total=1,sumY=yy,sumR=rr,sumB=bb;
        float variance=sy*sy+.22f*sc*sc,h2=Math.max(1.5f,variance*3.2f);
        for(int dy=-4;dy<=4;dy+=4)for(int dx=-4;dx<=4;dx+=4) {
            if(dx==0&&dy==0)continue;int cx=x+dx,cy=y+dy;if(cx<0||cy<validBegin||cx>=width||cy>=validEnd)continue;float distance=0,patchY=0,patchR=0,patchB=0;boolean valid=true;
            for(int k=0;k<5;k++) {
                int px=k==1?-1:k==2?1:0,py=k==3?-1:k==4?1:0;
                int a=src[clamp(y+py,validBegin,validEnd-1)*width+clamp(x+px,0,width-1)],b=src[clamp(cy+py,validBegin,validEnd-1)*width+clamp(cx+px,0,width-1)];
                if((a>>>24)!=255||(b>>>24)!=255){valid=false;continue;}
                float ya=luma(a),yb=luma(b),ra=((a>>>16)&255)-ya,rb=((b>>>16)&255)-yb,ba=(a&255)-ya,bc=(b&255)-yb;distance+=square(ya-yb)+.11f*(square(ra-rb)+square(ba-bc));patchY+=yb;patchR+=rb;patchB+=bc;
            }
            if(!valid)continue;distance=Math.max(0,distance/5-2*variance);float z=distance/h2;if(z>5)continue;float weight=1/(1+z+z*z*.5f+z*z*z/6);
            sumY+=weight*(patchY/5);sumR+=weight*(patchR/5);sumB+=weight*(patchB/5);total+=weight;
        }
        result[0]=sumY/total;result[1]=sumR/total;result[2]=sumB/total;
    }
    private static void validate(int[] in,int[] out,int width,int rows,int begin,int end,int validBegin,int validEnd,int origin,int noise,Model model) {
        if(in==null||out==null||in==out||model==null||width<1||rows<1||noise<0||noise>4||begin<validBegin||end>validEnd||begin>end||validBegin<0||validEnd>rows||validBegin>=validEnd||in.length<(long)width*rows||out.length<(long)width*rows||model.width!=width||origin+validBegin<0||origin+validEnd>model.height)throw new IllegalArgumentException("strong NR range");
        int absBegin=origin+begin,absEnd=origin+end;
        if(absBegin>0&&begin-validBegin<Math.min(HALO,absBegin)||absEnd<model.height&&validEnd-end<Math.min(HALO,model.height-absEnd))throw new IllegalArgumentException("strong NR missing halo");
    }
    static boolean nativeAvailable() {
        if(nativeState==0)synchronized(StrongNoise1958.class){if(nativeState==0)try{System.loadLibrary("ulike_smooth1958");nativeState=nativeAbi()==1958?1:-1;}catch(LinkageError unavailable){nativeState=-1;}}
        return nativeState==1;
    }
    private static native int nativeAbi();
    private static native boolean processNative(int[] input,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int originY,int noise,boolean shadows,float[] evidence,int[] half,int[] quarter,int[] eighth,int fullWidth,int fullHeight,int[] policy,int mode);
    private static int pack(int guide,float r,float g,float b){return clamp(guide,0,255)<<24|((clamp(Math.round(r),-127,127)&255)<<16)|((clamp(Math.round(g),-127,127)&255)<<8)|(clamp(Math.round(b),-127,127)&255);}
    private static float sigma(float[] values,int offset,float mean){float x=clamp((mean-16)/32,0,7);int a=(int)x,b=Math.min(7,a+1);return values[offset+a]*(1-(x-a))+values[offset+b]*(x-a);}
    private static float average(float[] values,int offset){float sum=0;for(int i=0;i<8;i++)sum+=values[offset+i];return sum/8;}
    private static int halfWidth(int value,int scale){for(int i=scale;i>1;i/=2)value=half(value);return value;}
    private static int half(int value){return value/2+(value&1);}
    private static void geometry(int width,int height){if(width<1||height<1||width>Integer.MAX_VALUE-REGION||height>Integer.MAX_VALUE-REGION||pixels(width,height)>Integer.MAX_VALUE)throw new IllegalArgumentException("strong NR geometry");}
    private static long pixels(int width,int height){return (long)width*height;}
    private static int checkedPixels(int width,int height){long count=pixels(width,height);if(count<0||count>Integer.MAX_VALUE)throw new IllegalArgumentException("strong NR array");return (int)count;}
    private static float luma(int p){return .299f*((p>>>16)&255)+.587f*((p>>>8)&255)+.114f*(p&255);}
    private static float square(float v){return v*v;}
    private static float[][] dctBasis(){float[][] basis=new float[8][8];for(int k=0;k<8;k++)for(int x=0;x<8;x++)basis[k][x]=(float)((k==0?Math.sqrt(1.0/8):.5)*Math.cos(Math.PI*(2*x+1)*k/16));return basis;}
    private static int byteValue(float v){return clamp(Math.round(v),0,255);}
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private static void checkInterrupted(){if(Thread.currentThread().isInterrupted())throw new CancellationException("strong single-image NR interrupted");}
}
