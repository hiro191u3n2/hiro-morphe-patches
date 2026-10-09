package com.hiro.ulike;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

/** Standalone tests with known clean truth; does not duplicate the filter's math. */
public final class SingleNoise1955Test {
    private static int assertions, cases, nativeParityCases;
    private static double darkInput, darkOutput, wallInput, wallOutput, wallTransfer, meshTransfer;
    private static double maximumMeanDrift, correlatedBefore, correlatedAfter, largeStripMillis;
    private static final SingleNoise1955.Protection UNPROTECTED = new SingleNoise1955.Protection() {
        public int budgetQ8(int x, int y) { return 256; }
        public int detailQ8(int x, int y) { return 0; }
    };
    private static final SingleNoise1955.Protection PROTECTED = new SingleNoise1955.Protection() {
        public int budgetQ8(int x, int y) { return 0; }
        public int detailQ8(int x, int y) { return 256; }
    };
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static int clamp(int value) { return Math.max(0, Math.min(255, value)); }
    private static int rgb(int r, int g, int b) { return 0xff000000 | clamp(r)<<16 | clamp(g)<<8 | clamp(b); }
    private static int channel(int pixel, int c) { return (pixel >>> (16-8*c)) & 255; }
    private static int[] constant(int w, int h, int pixel) {
        int[] image = new int[w*h]; Arrays.fill(image, pixel); return image;
    }
    private static int[] noisy(int[] clean, double sigma, long seed) {
        Random random = new Random(seed); int[] image = clean.clone();
        for (int i=0;i<image.length;i++) {
            int r=channel(clean[i],0)+(int)Math.round(sigma*random.nextGaussian());
            int g=channel(clean[i],1)+(int)Math.round(sigma*random.nextGaussian());
            int b=channel(clean[i],2)+(int)Math.round(sigma*random.nextGaussian());
            image[i]=rgb(r,g,b);
        }
        return image;
    }
    private static SingleNoise1955.Model model(final int[] image, final int w, final int h) {
        return SingleNoise1955.probe(new SingleNoise1955.Patches() {
            public void read(int[] pixels,int x,int y,int width,int height) {
                if (x<0 || y<0 || x+width>w || y+height>h || pixels.length<width*height)
                    throw new AssertionError("invalid probe read "+x+","+y+","+width+","+height);
                for (int row=0;row<height;row++)
                    System.arraycopy(image,(y+row)*w+x,pixels,row*width,width);
            }
        },w,h);
    }
    private static int[] filter(int[] image,int w,int h,int noise,boolean shadows,
            SingleNoise1955.Model m,SingleNoise1955.Protection p) {
        int[] before=image.clone(), out=image.clone();
        SingleNoise1955.processRange(image,out,w,h,0,h,0,h,0,noise,shadows,m,p);
        check(Arrays.equals(before,image),"filter mutated source"); cases++; return out;
    }
    private static double rmse(int[] image,int[] clean,int w,int x0,int y0,int x1,int y1) {
        double sum=0; long n=0;
        for (int y=y0;y<y1;y++) for (int x=x0;x<x1;x++) for (int c=0;c<3;c++) {
            double d=channel(image[y*w+x],c)-channel(clean[y*w+x],c); sum+=d*d; n++;
        }
        return Math.sqrt(sum/n);
    }
    /** Least-squares contrast against the true texture, after subtracting its mean. */
    private static double transfer(int[] image,int[] clean,int w,int x0,int y0,int x1,int y1) {
        double meanClean=0,meanImage=0; int n=(x1-x0)*(y1-y0);
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++) {
            meanClean+=channel(clean[y*w+x],1);meanImage+=channel(image[y*w+x],1);
        }
        meanClean/=n;meanImage/=n;double cross=0,energy=0;
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++) {
            double target=channel(clean[y*w+x],1)-meanClean;
            cross+=target*(channel(image[y*w+x],1)-meanImage);energy+=target*target;
        }
        return cross/energy;
    }
    private static void exactConstantsAndNoiseOff() {
        int[][] sizes={{1,1},{2,3},{7,9},{13,11},{33,35}};
        int[] values={rgb(0,0,0),rgb(255,255,255),rgb(24,31,39),rgb(132,71,204)};
        for(int[] size:sizes)for(int pixel:values) {
            int[] src=constant(size[0],size[1],pixel);
            SingleNoise1955.Model m=model(src,size[0],size[1]);
            int[] out=filter(src,size[0],size[1],4,true,m,UNPROTECTED);
            check(Arrays.equals(src,out),"constant not exact "+size[0]+"x"+size[1]);
            check(m.meanSigma()<1.0,"constant estimated as noisy: "+m.meanSigma());
        }
        int w=31,h=23;int[] src=noisy(constant(w,h,rgb(42,48,53)),12,1955),before=src.clone();
        int[] out=new int[src.length];Arrays.fill(out,0x12345678);
        SingleNoise1955.processRange(src,out,w,h,3,19,0,h,0,0,true,model(src,w,h),UNPROTECTED);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)
            check(out[y*w+x]==(y>=3 && y<19?src[y*w+x]:0x12345678),"noise OFF range copy");
        check(Arrays.equals(before,src),"noise OFF input mutation");cases++;
    }
    private static void estimatedNoiseAndFlatDenoising() {
        int w=160,h=128;int[] clean=constant(w,h,rgb(42,46,52));
        int[] low=noisy(clean,0.65,7301);SingleNoise1955.Model lowModel=model(low,w,h);
        check(lowModel.meanSigma()<2.0,"nearly clean flat estimated high: "+lowModel.meanSigma());
        int[] lowOut=filter(low,w,h,4,true,lowModel,UNPROTECTED);
        check(rmse(lowOut,low,w,8,8,w-8,h-8)<1.1,"nearly clean flat modified heavily");
        int[] src=noisy(clean,10,7302);SingleNoise1955.Model m=model(src,w,h);
        check(m.meanSigma()>2 && m.meanSigma()<22,"implausible flat sigma: "+m.meanSigma());
        check(m.sigmaAt(w/2,h/2)>0,"missing local noise estimate");
        check(m.chromaSigmaAt(w/2,h/2)>=0,"negative chroma sigma");
        int[] out=filter(src,w,h,4,true,m,UNPROTECTED);
        darkInput=rmse(src,clean,w,8,8,w-8,h-8);darkOutput=rmse(out,clean,w,8,8,w-8,h-8);
        check(darkOutput<darkInput*0.90,"dark flat noise not reduced sufficiently: "+darkInput+" -> "+darkOutput);
        int[] protectedOut=filter(src,w,h,4,true,m,PROTECTED);
        check(Arrays.equals(protectedOut,src),"zero budget/full detail protection changed pixels");
    }
    private static int[] scene(int w,int h) {
        int[] clean=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            // A dim wall with two genuine low-contrast texture frequencies.
            int value=54+(int)Math.round(8*Math.sin(2*Math.PI*x/11.0)+5*Math.sin(2*Math.PI*(x+y)/19.0));
            // Repeated chair weave at lower contrast than the printed lettering.
            if(x>=w/2) value=55+((x%8<2 || y%8<2)?16:0);
            // High-contrast, two-pixel-wide glyph-like strokes and a dark object boundary.
            if(y>=h-28) {
                value=143;
                if(x<12 || (x>=22 && x<24) || (x>=32 && x<34) ||
                   (x>=22 && x<34 && (y==h-25 || y==h-14))) value=24;
            }
            clean[y*w+x]=rgb(value,value+2,value+4);
        }
        return clean;
    }
    private static void textureAndText() {
        int w=192,h=128;int[] clean=scene(w,h);
        int[] cleanOut=filter(clean,w,h,4,true,model(clean,w,h),UNPROTECTED);
        check(rmse(cleanOut,clean,w,8,8,w-8,h-36)<1.2,"clean wall/mesh erased");
        check(transfer(cleanOut,clean,w,8,8,w/2-8,h-36)>0.94,"clean wall contrast lost");
        int[] src=noisy(clean,6,7303),out=filter(src,w,h,4,true,model(src,w,h),UNPROTECTED);
        wallInput=rmse(src,clean,w,8,8,w/2-8,h-36);wallOutput=rmse(out,clean,w,8,8,w/2-8,h-36);
        wallTransfer=transfer(out,clean,w,8,8,w/2-8,h-36);
        meshTransfer=transfer(out,clean,w,w/2+8,8,w-8,h-36);
        check(wallOutput<=wallInput*1.02,"wall error increased: "+wallInput+" -> "+wallOutput);
        check(wallTransfer>0.94,"wall texture contrast lost: "+wallTransfer);
        check(meshTransfer>0.94,"mesh texture contrast lost: "+meshTransfer);
        double edgeBefore=0,edgeAfter=0;int n=0;
        for(int y=h-23;y<h-3;y++) {edgeBefore+=channel(src[y*w+14],1)-channel(src[y*w+10],1);
            edgeAfter+=channel(out[y*w+14],1)-channel(out[y*w+10],1);n++;}
        check(edgeAfter/n>105,"high-contrast printed edge softened: "+edgeAfter/n);
        check(edgeAfter>edgeBefore*0.94,"text/object edge lost contrast");
    }
    private static void alphaAndRangeOwnership() {
        int w=43,h=39;int[] src=noisy(constant(w,h,rgb(47,53,61)),10,7304);
        for(int i=0;i<src.length;i+=13)src[i]=(i%255)<<24 | (src[i]&0x00ffffff);
        int[] before=src.clone(),out=new int[src.length];Arrays.fill(out,0x98765432);
        SingleNoise1955.processRange(src,out,w,h,4,32,0,h,0,4,true,model(src,w,h),UNPROTECTED);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int i=y*w+x;
            if(y<4 || y>=32)check(out[i]==0x98765432,"outside row ownership changed");
            else if(src[i]>>>24!=255)check(out[i]==src[i],"nonopaque pixel changed");
            else check(out[i]>>>24==255,"opaque alpha changed");
        }
        check(Arrays.equals(before,src),"range source changed");cases++;
    }
    private static void stripeEquality() {
        int w=47,h=79;int[] src=noisy(scene(w,h),7,7305),snapshot=src.clone();
        SingleNoise1955.Model m=model(src,w,h);int[] expected=filter(src,w,h,4,true,m,UNPROTECTED);
        for(int chunk:new int[]{1,3,7,16,31}) {
            int[] assembled=new int[src.length];
            for(int y=0;y<h;y+=chunk) {
                int end=Math.min(h,y+chunk),origin=Math.max(0,y-SingleNoise1955.HALO);
                int last=Math.min(h,end+SingleNoise1955.HALO),rows=last-origin;
                int[] input=Arrays.copyOfRange(src,origin*w,last*w),out=input.clone();
                SingleNoise1955.processRange(input,out,w,rows,y-origin,end-origin,0,rows,origin,4,true,m,UNPROTECTED);
                System.arraycopy(out,(y-origin)*w,assembled,y*w,(end-y)*w);
            }
            check(Arrays.equals(expected,assembled),"stripe differs from full, chunk="+chunk+" first="+firstDifference(expected,assembled));cases++;
        }
        final int[] concurrent=src.clone();final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] threads=new Thread[4];
        for(int i=0;i<threads.length;i++) {final int begin=i*h/4,end=(i+1)*h/4;
            threads[i]=new Thread(new Runnable(){public void run(){try {
                SingleNoise1955.processRange(src,concurrent,w,h,begin,end,0,h,0,4,true,m,UNPROTECTED);
            }catch(Throwable t){failure.compareAndSet(null,t);}}});threads[i].start();}
        for(Thread thread:threads)try{thread.join();}catch(InterruptedException e){throw new AssertionError(e);}
        check(failure.get()==null,"parallel failed "+failure.get());
        check(Arrays.equals(concurrent,expected),"parallel ranges differ");
        check(Arrays.equals(src,snapshot),"stripes/parallel mutated source");cases++;
    }
    private static int firstDifference(int[] a,int[] b){for(int i=0;i<a.length;i++)if(a[i]!=b[i])return i;return -1;}
    private static void assertMeans(int[] before,int[] after,String label) {
        for(int c=0;c<3;c++) {
            double drift=0;for(int i=0;i<before.length;i++)drift+=channel(after[i],c)-channel(before[i],c);
            drift=Math.abs(drift/before.length);maximumMeanDrift=Math.max(maximumMeanDrift,drift);
            check(drift<.5,label+" RGB channel "+c+" mean changed by "+drift);
        }
    }
    private static void meanAndLightingPreservation() {
        int w=192,h=128;
        for(int mode=0;mode<3;mode++) {
            int[] truth=new int[w*h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int value=mode==0?(x<w/2?29:188):mode==1?24+180*x/(w-1):56;
                truth[y*w+x]=rgb(value,value+8,value+16);
            }
            int[] src=mode==1?truth:noisy(truth,8,8600+mode);
            int[] out=filter(src,w,h,4,true,model(src,w,h),UNPROTECTED);
            assertMeans(src,out,new String[]{"sharp lighting boundary","clean lighting ramp","colored flat noise"}[mode]);
            if(mode==1)check(rmse(out,truth,w,0,0,w,h)<.6,"clean lighting ramp altered");
        }
    }
    private static void correlatedNoiseAndSpatialProbe() {
        int w=192,h=160;int[] clean=constant(w,h,rgb(46,52,60));
        int[] raw=noisy(constant(w+1,h+1,rgb(46,52,60)),16,8700),src=new int[w*h];
        // A moving 2x2 mean produces spatially correlated color grain, unlike AWGN.
        // Each channel's post-average standard deviation is approximately 8 values.
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int[] sum=new int[3];
            for(int yy=0;yy<2;yy++)for(int xx=0;xx<2;xx++)for(int c=0;c<3;c++)
                sum[c]+=channel(raw[(y+yy)*(w+1)+x+xx],c);
            src[y*w+x]=rgb((sum[0]+2)/4,(sum[1]+2)/4,(sum[2]+2)/4);
        }
        int[] out=filter(src,w,h,4,true,model(src,w,h),UNPROTECTED);
        correlatedBefore=rmse(src,clean,w,8,8,w-8,h-8);correlatedAfter=rmse(out,clean,w,8,8,w-8,h-8);
        check(correlatedAfter<correlatedBefore*.95,"correlated noise not reduced: "+correlatedBefore+" -> "+correlatedAfter);
        assertMeans(src,out,"correlated color noise");

        // Measured local evidence must distinguish actual low/high noise, even when
        // brightness differs. Both regions use one frame and preserve their means.
        final int pw=768,ph=256;int[] mixed=new int[pw*ph];Random random=new Random(8701);
        for(int y=0;y<ph;y++)for(int x=0;x<pw;x++) {
            int value=x<pw/2?40:180;double sigma=x<pw/2?2:8;
            mixed[y*pw+x]=rgb(value+(int)Math.round(sigma*random.nextGaussian()),
                value+4+(int)Math.round(sigma*random.nextGaussian()),
                value+8+(int)Math.round(sigma*random.nextGaussian()));
        }
        SingleNoise1955.Model m=model(mixed,pw,ph);float dimSigma=m.sigmaAt(24,ph/2),brightSigma=m.sigmaAt(pw-25,ph/2);
        check(dimSigma>.30 && brightSigma>dimSigma*2,"spatial sigma failed to distinguish 2 vs 8 grain: "+dimSigma+" / "+brightSigma);
        float dimChroma=m.chromaSigmaAt(24,ph/2),brightChroma=m.chromaSigmaAt(pw-25,ph/2);
        check(brightChroma>dimChroma*2,"spatial color sigma failed to distinguish 2 vs 8 grain");cases++;
    }
    private static long mix(long value) {
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);
    }
    private static double uniform(long value) { return (mix(value)>>>11)*0x1.0p-53; }
    private static int generatedPixel(int x,int y) {
        long location=(long)y*4080+x, seed=location*0x9e3779b97f4a7c15L;int[] c=new int[3];
        for(int i=0;i<3;i++) {
            // Four uniform variables give a bounded Gaussian-like grain without
            // retaining a 12.5-megapixel image or depending on the filter's math.
            long s=seed+i*0xd1b54a32d192ed03L;
            double gaussian=(uniform(s)+uniform(s+1)+uniform(s+2)+uniform(s+3)-2)*Math.sqrt(3);
            c[i]=48+6*i+(int)Math.round(8*gaussian);
        }
        return rgb(c[0],c[1],c[2]);
    }
    private static void realImageGeometryWithBoundedMemory() {
        final int w=4080,h=3060;final int[] reads={0},largestRead={0};long start=System.nanoTime();
        SingleNoise1955.Model m=SingleNoise1955.probe(new SingleNoise1955.Patches() {
            public void read(int[] pixels,int x,int y,int width,int height) {
                check(x>=0&&y>=0&&x+width<=w&&y+height<=h,"full-resolution probe bounds");
                reads[0]++;largestRead[0]=Math.max(largestRead[0],width*height);
                for(int yy=0;yy<height;yy++)for(int xx=0;xx<width;xx++)
                    pixels[yy*width+xx]=generatedPixel(x+xx,y+yy);
            }
        },w,h);
        check(m.width==w&&m.height==h,"full-resolution model geometry");
        check(reads[0]>1&&largestRead[0]<=48*48,"model unexpectedly needed large pixel buffer");
        check(m.samples>0&&m.meanSigma()>.30,"full-resolution probe found no noise evidence");
        int origin=733,rows=82,begin=7,end=71;int[] input=new int[w*rows];
        for(int yy=0;yy<rows;yy++)for(int x=0;x<w;x++)input[yy*w+x]=generatedPixel(x,origin+yy);
        int[] before=input.clone(),output=new int[input.length];Arrays.fill(output,0x10203040);
        SingleNoise1955.processRange(input,output,w,rows,begin,end,0,rows,origin,4,true,m,UNPROTECTED);
        for(int yy=0;yy<rows;yy++)for(int x=0;x<w;x++) {
            int at=yy*w+x;
            if(yy<begin||yy>=end)check(output[at]==0x10203040,"full-resolution strip ownership");
            else check(output[at]>>>24==255,"full-resolution strip alpha");
        }
        check(Arrays.equals(before,input),"full-resolution strip changed source");
        int[] truth=constant(w,rows,rgb(48,54,60));
        check(rmse(output,truth,w,8,begin,w-8,end)<rmse(input,truth,w,8,begin,w-8,end)*.95,
            "representative full-width strip failed to reduce noise");
        largeStripMillis=(System.nanoTime()-start)/1e6;
        check(largeStripMillis<60000,"representative strip took over one minute on this host");cases++;
    }
    private static void cancellationAndBounds() {
        final int w=29,h=23;final int[] src=noisy(constant(w,h,rgb(50,52,55)),8,7306);
        final SingleNoise1955.Model m=model(src,w,h);final int[] out=new int[src.length];Arrays.fill(out,0x10203040);
        int[] before=src.clone(),destinationBefore=out.clone();boolean cancelled=false;
        Thread.currentThread().interrupt();
        try{SingleNoise1955.processRange(src,out,w,h,0,h,0,h,0,4,true,m,UNPROTECTED);}
        catch(CancellationException expected){cancelled=true;}finally{Thread.interrupted();}
        check(cancelled,"preinterrupted call did not cancel");
        check(Arrays.equals(src,before),"cancel mutated source");
        check(Arrays.equals(out,destinationBefore),"preinterrupt committed destination");
        expectInvalid(new Runnable(){public void run(){SingleNoise1955.processRange(src,src,w,h,0,h,0,h,0,4,true,m,UNPROTECTED);}},"aliased source/dest");
        expectInvalid(new Runnable(){public void run(){SingleNoise1955.processRange(src,out,0,h,0,h,0,h,0,4,true,m,UNPROTECTED);}},"zero width");
        expectInvalid(new Runnable(){public void run(){SingleNoise1955.processRange(src,out,w,h,-1,h,0,h,0,4,true,m,UNPROTECTED);}},"negative first row");
        expectInvalid(new Runnable(){public void run(){SingleNoise1955.processRange(src,out,w,h,0,h+1,0,h,0,4,true,m,UNPROTECTED);}},"last row beyond buffer");
        expectInvalid(new Runnable(){public void run(){SingleNoise1955.processRange(src,out,w,h,0,h,1,h,0,4,true,m,UNPROTECTED);}},"requested row outside valid input");
        expectInvalid(new Runnable(){public void run(){SingleNoise1955.processRange(new int[1],out,w,h,0,h,0,h,0,4,true,m,UNPROTECTED);}},"short input");
        check(Arrays.equals(out,destinationBefore),"invalid argument committed destination");cases++;
    }
    private static void expectInvalid(Runnable action,String label) {
        boolean failed=false;try{action.run();}catch(IllegalArgumentException e){failed=true;}
        check(failed,"invalid arguments accepted: "+label);
    }
    private static void nativeJavaParityWhenAvailable() {
        if(!SingleNoise1955.nativeAvailable())return;
        SingleNoise1955.Protection mask=new SingleNoise1955.Protection() {
            public int budgetQ8(int x,int y){return (x*31+y*17)&255;}
            public int detailQ8(int x,int y){return (x*13+y*7)%257;}
        };
        for(int mode=0;mode<5;mode++) {
            int w=mode==2?64:mode==3?61:47,h=mode==2?96:mode==3?45:79;
            int[] original=noisy(mode==1?constant(w,h,rgb(37,44,51)):scene(w,h),mode==1?12:8,9000+mode);
            if(mode==3)for(int i=0;i<original.length;i+=29)original[i]=((i%254)<<24)|(original[i]&0x00ffffff);
            SingleNoise1955.Model m=model(original,w,h);
            int origin=mode==4?13:0,rows=mode==4?52:h,begin=mode==4?7:0,end=mode==4?45:h;
            int[] input=Arrays.copyOfRange(original,origin*w,(origin+rows)*w),snapshot=input.clone();
            int[] nativeOut=new int[input.length],javaOut=new int[input.length];
            Arrays.fill(nativeOut,0x76543210);Arrays.fill(javaOut,0x76543210);
            SingleNoise1955.Protection p=mode==4?mask:UNPROTECTED;
            SingleNoise1955.processRange(input,nativeOut,w,rows,begin,end,0,rows,origin,4,(mode&1)==0,m,p);
            SingleNoise1955.processJavaRange(input,javaOut,w,rows,begin,end,0,rows,origin,4,(mode&1)==0,m,p);
            check(SingleNoise1955.nativeAvailable(),"native backend lost ABI during parity gate");
            check(Arrays.equals(nativeOut,javaOut),"native/Java mismatch, mode="+mode+" first="+firstDifference(nativeOut,javaOut));
            check(Arrays.equals(input,snapshot),"native parity source mutated");nativeParityCases++;cases++;
        }
    }
    public static void main(String[] args) {
        exactConstantsAndNoiseOff();estimatedNoiseAndFlatDenoising();textureAndText();
        meanAndLightingPreservation();correlatedNoiseAndSpatialProbe();
        alphaAndRangeOwnership();stripeEquality();cancellationAndBounds();realImageGeometryWithBoundedMemory();
        nativeJavaParityWhenAvailable();
        System.out.println("{\"suite\":\"single-noise1955\",\"cases\":"+cases+",\"assertions\":"+assertions+
            ",\"darkRmseBefore\":"+darkInput+",\"darkRmseAfter\":"+darkOutput+
            ",\"wallRmseBefore\":"+wallInput+",\"wallRmseAfter\":"+wallOutput+
            ",\"wallTextureTransfer\":"+wallTransfer+",\"meshTextureTransfer\":"+meshTransfer+
            ",\"maximumRgbMeanDrift\":"+maximumMeanDrift+",\"correlatedRmseBefore\":"+correlatedBefore+
            ",\"correlatedRmseAfter\":"+correlatedAfter+",\"fullWidthStripMillis\":"+largeStripMillis+
            ",\"nativeAvailable\":"+SingleNoise1955.nativeAvailable()+",\"nativeParityCases\":"+nativeParityCases+",\"passed\":true}");
    }
}
