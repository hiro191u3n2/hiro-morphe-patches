package com.hiro.ulike;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Host tests exercise the actual shipped pixel kernel, without Android/UI mocks. */
public final class QualityPixels1932Test {
    private static final Map<String, Double> metrics = new LinkedHashMap<String, Double>();
    private static int assertions;
    private static void require(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static int rgb(int r, int g, int b) {
        return 0xff000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }
    private static int gray(int v) { return rgb(v,v,v); }
    private static int clamp(int n) { return Math.max(0,Math.min(255,n)); }
    private static int r(int p) { return (p >>> 16) & 255; }
    private static int g(int p) { return (p >>> 8) & 255; }
    private static int b(int p) { return p & 255; }
    private static int y(int p) { return QualityPixels1932.luma(p); }
    private static double variance(int[] pixels, int width, int height, int border, boolean chroma) {
        double sum=0, square=0; int count=0;
        for(int yy=border;yy<height-border;yy++) for(int x=border;x<width-border;x++) {
            int p=pixels[yy*width+x];
            double v=chroma ? b(p)-g(p) : y(p);
            sum+=v;square+=v*v;count++;
        }
        return square/count-Math.pow(sum/count,2);
    }
    private static QualityPixels1932.Plan plan(QualityPixels1932.NoiseStats stats,int noise,int sharp,float beauty,float scale) {
        return QualityPixels1932.plan(stats,400,12000000L,QualityPixels1932.LENS_WIDE,
            beauty,noise,sharp,true,true,scale);
    }
    private static int[] noiseImage(int width,int height,double sigma,long seed) {
        Random random=new Random(seed); int[] image=new int[width*height];
        for(int i=0;i<image.length;i++) image[i]=gray((int)Math.round(112+sigma*random.nextGaussian()));
        return image;
    }
    private static int[] finish(int[] input,int width,int height,QualityPixels1932.Plan plan,boolean moire,boolean sharp) {
        int[] out=new int[input.length];
        QualityPixels1932.finishStrip(input,out,width,height,0,height,plan,moire,sharp);
        return out;
    }
    private static int[] resize(final int[] in,final int sw,final int sh,final int dw,final int dh) {
        return resizeCrop(in,sw,sh,dw,dh,0,0,sw,sh);
    }
    private static int[] resizeCrop(final int[] in,final int sw,final int sh,final int dw,final int dh,
                                    double left,double top,double cw,double ch) {
        final int[] out=new int[dw*dh];
        QualityPixels1932.resizeCrop(new QualityPixels1932.RowSource() {
            public void readRow(int row,int[] pixels) {System.arraycopy(in,row*sw,pixels,0,sw);}
        },sw,sh,new QualityPixels1932.RowSink() {
            public void writeRow(int row,int[] pixels) {System.arraycopy(pixels,0,out,row*dw,dw);}
        },dw,dh,left,top,cw,ch);
        return out;
    }
    private static void testNoiseEstimateAndPolicy() {
        for(int sigma:new int[]{0,2,8,16}) {
            int[] in=noiseImage(257,251,sigma,1932+sigma);
            QualityPixels1932.NoiseStats stats=QualityPixels1932.estimate(in,257,251);
            metrics.put("estimated_sigma_"+sigma,(double)stats.lumaSigma);
            require(stats.samples>=2000,"enough unscaled image-domain noise samples");
            require(Math.abs(stats.lumaSigma-sigma)<=Math.max(1.1,sigma*0.3),"robust noise estimate "+sigma);
            QualityPixels1932.Plan p=plan(stats,3,3,0.6f,1f);
            require(p.noiseLevel>=0 && p.noiseLevel<=3,"honour user NR upper bound");
            QualityPixels1932.Plan off=plan(stats,0,0,0.6f,1f);
            require(off.noiseLevel==0 && off.sharpGainQ8==0,"OFF remains OFF");
        }
        QualityPixels1932.NoiseStats clean=new QualityPixels1932.NoiseStats(0,0,140,0,1000);
        QualityPixels1932.Plan low=QualityPixels1932.plan(clean,100,10000000L,2,0,4,4,true,true,1);
        QualityPixels1932.Plan high=QualityPixels1932.plan(clean,12800,10000000L,1,0,4,4,true,true,1);
        require(high.sourceSigma-low.sourceSigma<=0.241f,"ISO cannot override clean processed pixels");
        QualityPixels1932.NoiseStats noisy=new QualityPixels1932.NoiseStats(8,12,112,0,1000);
        QualityPixels1932.Plan beauty=plan(noisy,3,2,1,1);
        QualityPixels1932.Plan bare=plan(noisy,3,2,0,1);
        require(beauty.noiseLevel==bare.noiseLevel,"beauty never globally weakens background NR");
        int smoothSkin=rgb(190,145,120), background=rgb(80,140,195);
        require(QualityPixels1932.shadowBudgetQ8(smoothSkin,1,beauty)<
            QualityPixels1932.shadowBudgetQ8(smoothSkin,1,bare),"smooth skin overlap budget reduced");
        require(QualityPixels1932.shadowBudgetQ8(background,1,beauty)==
            QualityPixels1932.shadowBudgetQ8(background,1,bare),"background budget retained");
        require(QualityPixels1932.shadowBudgetQ8(smoothSkin,90,beauty)==
            QualityPixels1932.shadowBudgetQ8(smoothSkin,90,bare),"textured skin not mistaken for already smooth");
        require(plan(noisy,3,3,0,1.8f).sharpGainQ8<plan(noisy,3,3,0,1f).sharpGainQ8,
            "upscale-aware sharpening gain");
    }
    private static void testConstantsRampsAndAlpha() {
        int w=83,h=67;
        for(int color:new int[]{gray(0),gray(23),gray(128),gray(255),rgb(208,155,96),rgb(230,130,120),rgb(5,220,17)}) {
            int[] in=new int[w*h];Arrays.fill(in,color);
            int[] out=finish(in,w,h,plan(QualityPixels1932.estimate(in,w,h),4,4,1,1),true,true);
            require(Arrays.equals(in,out),"constant tone/warm material unchanged");
            for(int[] size:new int[][]{{131,101},{27,19}}) {
                int[] resized=resize(in,w,h,size[0],size[1]);
                for(int p:resized) require(p==color,"constant resampling exact");
            }
        }
        int[] ramp=new int[w*h];
        for(int yy=0;yy<h;yy++) for(int x=0;x<w;x++) ramp[yy*w+x]=rgb(60+x,45+x,30+x);
        int[] out=finish(ramp,w,h,plan(QualityPixels1932.estimate(ramp,w,h),3,3,0.6f,1),true,true);
        require(Arrays.equals(ramp,out),"linear warm ramp unchanged by finish");
        int[] alpha=new int[w*h];Random random=new Random(9871);
        for(int i=0;i<alpha.length;i++) alpha[i]=(random.nextInt(255)<<24)|random.nextInt(1<<24);
        require(Arrays.equals(alpha,finish(alpha,w,h,plan(null,4,4,1,1),true,true)),
            "non-opaque filter pixels byte-exact");
        require(Arrays.equals(alpha,resize(alpha,w,h,w,h)),"identity resize byte-exact including invisible RGB");
        int[] transparentEdge=new int[16*16];
        for(int yy=0;yy<16;yy++) for(int x=0;x<16;x++)
            transparentEdge[yy*16+x]=x<8?0x00ff0000:0xff0080ff;
        int[] resized=resize(transparentEdge,16,16,39,39);
        for(int p:resized) if((p>>>24)>0) {
            require(r(p)==0,"invisible red does not contaminate visible resample");
            require(Math.abs(g(p)-128)<=1 && b(p)==255,"premultiplied colour retained");
        }
    }
    private static void testResampling() {
        int w=129,h=127;int[] checker=new int[w*h];
        for(int yy=0;yy<h;yy++) for(int x=0;x<w;x++) checker[yy*w+x]=gray(((x+yy)&1)==0?32:224);
        int[] small=resize(checker,w,h,31,29);
        double v=variance(small,31,29,3,false);
        metrics.put("downsample_checker_variance",v);
        require(v<1.0,"downsample antialias suppresses near-Nyquist checker");
        int[] edge=new int[24*20];
        for(int yy=0;yy<20;yy++) for(int x=0;x<24;x++) edge[yy*24+x]=gray(x<12?32:216);
        int[] large=resize(edge,24,20,71,59);
        for(int p:large) require(y(p)>=32 && y(p)<=216,"upsample no ringing overshoot");
        int[] ramp=new int[80*72];
        for(int yy=0;yy<72;yy++) for(int x=0;x<80;x++) ramp[yy*80+x]=rgb(20+2*x,30+2*yy,50+x+yy);
        double left=3.25,top=4.375,cw=69.5,ch=61.125;
        int dw=111,dh=97;int[] crop=resizeCrop(ramp,80,72,dw,dh,left,top,cw,ch);
        int maxError=0;
        for(int yy=5;yy<dh-5;yy++) for(int x=5;x<dw-5;x++) {
            double sx=left+(x+0.5)*cw/dw-0.5,sy=top+(yy+0.5)*ch/dh-0.5;
            int p=crop[yy*dw+x];
            maxError=Math.max(maxError,Math.abs(r(p)-(int)Math.round(20+2*sx)));
            maxError=Math.max(maxError,Math.abs(g(p)-(int)Math.round(30+2*sy)));
        }
        metrics.put("fractional_crop_ramp_max_error",(double)maxError);
        require(maxError<=1,"fractional crop coordinates preserved");
        for(int sw:new int[]{1,2,7}) for(int sh:new int[]{1,2,9}) {
            int[] tiny=new int[sw*sh];Arrays.fill(tiny,rgb(60,110,170));
            for(int p:resize(tiny,sw,sh,13,11)) require(p==rgb(60,110,170),"tiny source bounds");
        }
    }
    private static void testSharpenNoiseAndEdges() {
        int w=257,h=251;int[] input=noiseImage(w,h,8,81292);
        QualityPixels1932.Plan p=plan(QualityPixels1932.estimate(input,w,h),0,4,0,1);
        int[] out=finish(input,w,h,p,false,true);
        double before=variance(input,w,h,6,false),after=variance(out,w,h,6,false);
        metrics.put("noise_variance_before_sharp",before);
        metrics.put("noise_variance_after_sharp",after);
        require(after/before<=1.025,"output sharpening does not amplify flat grain");
        int ew=73,eh=49;int[] edge=new int[ew*eh];
        int[] values={40,40,40,44,60,94,146,180,196,200,200,200};
        for(int yy=0;yy<eh;yy++) for(int x=0;x<ew;x++) {
            int index=Math.max(0,Math.min(values.length-1,x-ew/2+6));
            int v=values[index];edge[yy*ew+x]=rgb(v+10,v,v-10);
        }
        QualityPixels1932.Plan clean=plan(new QualityPixels1932.NoiseStats(0,0,128,0,1000),0,4,0,1);
        int[] result=finish(edge,ew,eh,clean,false,true);
        int center=(eh/2)*ew+ew/2;
        int oldContrast=y(edge[center])-y(edge[center-1]);
        int newContrast=y(result[center])-y(result[center-1]);
        metrics.put("soft_edge_contrast_before",(double)oldContrast);
        metrics.put("soft_edge_contrast_after",(double)newContrast);
        require(newContrast>oldContrast,"soft edge receives useful final contrast");
        for(int i=0;i<edge.length;i++) {
            require(r(result[i])-g(result[i])==r(edge[i])-g(edge[i]),"sharp red/green hue exact");
            require(b(result[i])-g(result[i])==b(edge[i])-g(edge[i]),"sharp blue/green hue exact");
            require(y(result[i])>=40 && y(result[i])<=210,"bounded luma edge");
        }
    }
    private static void testHaloSetting() {
        int w=65,h=33;int[] step=new int[w*h];
        for(int row=0;row<h;row++)for(int x=0;x<w;x++)step[row*w+x]=gray(x<w/2?40:200);
        QualityPixels1932.Plan on=plan(new QualityPixels1932.NoiseStats(0,0,120,0,1000),0,4,0,1);
        QualityPixels1932.Plan off=on.withHaloSuppression(false);
        require(on.haloSuppression && !off.haloSuppression,"halo option copied immutably");
        require(on.sharpGainQ8==off.sharpGainQ8 && on.sharpFloorQ8==off.sharpFloorQ8 &&
            on.sharpLimit==off.sharpLimit && on.noiseLevel==off.noiseLevel,"halo copy changes only halo policy");
        require(on.withHaloSuppression(true)==on,"unchanged halo policy reuses immutable plan");
        int[] guarded=finish(step,w,h,on,false,true),unguarded=finish(step,w,h,off,false,true);
        require(Arrays.equals(step,guarded),"halo ON prevents hard-step overshoot");
        int edge=(h/2)*w+w/2;
        int onContrast=y(guarded[edge])-y(guarded[edge-1]);
        int offContrast=y(unguarded[edge])-y(unguarded[edge-1]);
        require(offContrast>onContrast,"halo OFF retains stronger hard-step sharpening");
        int maximumChange=0;
        for(int i=0;i<step.length;i++)maximumChange=Math.max(maximumChange,Math.abs(y(unguarded[i])-y(step[i])));
        require(maximumChange>0 && maximumChange<=off.sharpLimit,"halo OFF overshoot remains bounded by sharpLimit");
        metrics.put("hard_edge_contrast_halo_on",(double)onContrast);
        metrics.put("hard_edge_contrast_halo_off",(double)offContrast);
    }
    private static void testPeriodicChromaAndRealLines() {
        int w=96,h=88;
        for(int direction=0;direction<4;direction++) for(int period:new int[]{2,4}) {
            int[] in=new int[w*h];
            for(int yy=0;yy<h;yy++) for(int x=0;x<w;x++) {
                int position=direction==0?x:direction==1?yy:direction==2?x+yy:x-yy;
                int phase=Math.floorMod(position,period);
                int c=period==2?(phase==0?18:-18):(phase==0?18:phase==2?-18:0);
                // Near-isoluminant warm/cool false-colour band at fixed true brightness.
                in[yy*w+x]=rgb(150+c,150,150-3*c);
            }
            int[] out=finish(in,w,h,plan(null,0,0,0,1),true,false);
            double before=variance(in,w,h,8,true),after=variance(out,w,h,8,true);
            metrics.put("moire_variance_ratio_d"+direction+"_p"+period,after/before);
            require(after/before<0.36,"directional periodic chroma reduction "+direction+" "+period);
            int maxLuma=0;
            for(int i=0;i<in.length;i++) maxLuma=Math.max(maxLuma,Math.abs(y(in[i])-y(out[i])));
            require(maxLuma<=1,"moire luminance preserved");
        }
        int[] lines=new int[w*h];
        for(int yy=0;yy<h;yy++) for(int x=0;x<w;x++) {
            if(x==w/2)lines[yy*w+x]=rgb(186,146,101); // isolated warm strand
            else lines[yy*w+x]=gray(146);
        }
        require(Arrays.equals(lines,finish(lines,w,h,plan(null,0,0,0,1),true,false)),
            "isolated warm line preserved");
        int[] fabric=new int[w*h];
        for(int yy=0;yy<h;yy++) for(int x=0;x<w;x++)
            fabric[yy*w+x]=((x+yy)&1)==0?rgb(205,165,120):rgb(65,45,30);
        require(Arrays.equals(fabric,finish(fabric,w,h,plan(null,0,0,0,1),true,false)),
            "real periodic fabric with coherent luma is preserved");
        int[] saturated=new int[w*h];
        for(int yy=0;yy<h;yy++) for(int x=0;x<w;x++) saturated[yy*w+x]=(x&1)==0?rgb(230,32,22):rgb(22,32,230);
        require(Arrays.equals(saturated,finish(saturated,w,h,plan(null,0,0,0,1),true,false)),
            "saturated repeating colours preserved");
    }
    private static void testTileEquivalence() {
        int w=113,h=99;int[] in=noiseImage(w,h,5,18293);
        for(int yy=8;yy<h-8;yy++) for(int x=8;x<w-8;x++) if((x/11)%3==0) {
            int c=((x+yy)&1)==0?16:-16;in[yy*w+x]=rgb(155+c,155,155-3*c);
        }
        QualityPixels1932.Plan p=plan(QualityPixels1932.estimate(in,w,h),3,3,0.7f,1.3f);
        int[] entire=finish(in,w,h,p,true,true),tiled=new int[in.length];
        for(int first=0;first<h;first+=13) {
            int count=Math.min(13,h-first),top=Math.max(0,first-4),bottom=Math.min(h,first+count+4);
            int[] strip=Arrays.copyOfRange(in,top*w,bottom*w),out=new int[strip.length];
            QualityPixels1932.finishStrip(strip,out,w,bottom-top,first-top,first-top+count,p,true,true);
            System.arraycopy(out,(first-top)*w,tiled,first*w,count*w);
        }
        require(Arrays.equals(entire,tiled),"4px halo: tiled output byte-exact to full frame");
        int[] before=in.clone();finish(in,w,h,p,true,true);
        require(Arrays.equals(before,in),"input remains immutable");
        boolean aliasRejected=false;
        try {QualityPixels1932.finishStrip(in,in,w,h,0,h,p,true,true);}catch(IllegalArgumentException expected){aliasRejected=true;}
        require(aliasRejected,"in-place alias rejected to prevent feedback seams");
    }
    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        long started=System.nanoTime();
        testNoiseEstimateAndPolicy();
        testConstantsRampsAndAlpha();
        testResampling();
        testSharpenNoiseAndEdges();
        testHaloSetting();
        testPeriodicChromaAndRealLines();
        testTileEquivalence();
        metrics.put("host_test_seconds",(System.nanoTime()-started)/1e9);
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"metrics\":{");
        int index=0;
        for(Map.Entry<String,Double> e:metrics.entrySet()) {
            if(index++>0)System.out.println(",");
            System.out.printf("\"%s\":%.8f",e.getKey(),e.getValue());
        }
        System.out.println("}}");
    }
}
