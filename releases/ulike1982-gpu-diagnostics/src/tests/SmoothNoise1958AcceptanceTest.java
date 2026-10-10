package com.hiro.ulike;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/** Independent known-truth image acceptance, rather than feature flag checks.
 * The fixtures contain explicit random noise, coherent geometry, faint periodic
 * detail, and an almost isoluminant colour boundary. Error is measured against
 * the clean image, not against a preferred implementation's output. */
public final class SmoothNoise1958AcceptanceTest {
    private static int assertions,nativeParityCases;
    private static final Map<String,Double> metrics=new LinkedHashMap<String,Double>();
    private static final StrongNoise1958.Protection FREE=new StrongNoise1958.Protection(){
        public int budgetQ8(int x,int y){return 256;}
        public int detailQ8(int x,int y){return 0;}
    };
    private static final StrongNoise1957.Protection FREE57=new StrongNoise1957.Protection(){
        public int budgetQ8(int x,int y){return 256;}
        public int detailQ8(int x,int y){return 0;}
    };
    private static void check(boolean ok,String why){
        assertions++;
        if(!ok)throw new AssertionError(why+"; measured="+metrics);
    }
    private static void metric(String name,double value){
        check(!Double.isNaN(value)&&!Double.isInfinite(value),"nonfinite "+name);
        metrics.put(name,value);
    }
    private static int clip(int v){return Math.max(0,Math.min(255,v));}
    private static int rgb(int r,int g,int b){return 0xff000000|clip(r)<<16|clip(g)<<8|clip(b);}
    private static int c(int p,int k){return p>>>(16-k*8)&255;}
    private static double luma(int p){return .299*c(p,0)+.587*c(p,1)+.114*c(p,2);}
    private static int[] constant(int w,int h,int p){int[] out=new int[w*h];Arrays.fill(out,p);return out;}
    private static int[] noisy(int[] truth,double sy,double sc,long seed){
        Random random=new Random(seed);int[] out=truth.clone();
        for(int i=0;i<out.length;i++){
            int y=(int)Math.round(random.nextGaussian()*sy);
            int r=(int)Math.round(random.nextGaussian()*sc),b=(int)Math.round(random.nextGaussian()*sc);
            out[i]=(truth[i]&0xff000000)|(rgb(c(truth[i],0)+y+r,
                c(truth[i],1)+y-(int)Math.round((.299*r+.114*b)/.587),c(truth[i],2)+y+b)&0xffffff);
        }
        return out;
    }
    private static StrongNoise1958.Patches patches(final int[] image,final int w,final int h){
        return new StrongNoise1958.Patches(){public void read(int[] out,int x,int y,int width,int height){
            check(x>=0&&y>=0&&x+width<=w&&y+height<=h&&out.length>=width*height,"out of range source read");
            for(int row=0;row<height;row++)System.arraycopy(image,(y+row)*w+x,out,row*width,width);
        }};
    }
    private static StrongNoise1957.Patches patches57(final int[] image,final int w,final int h){
        return new StrongNoise1957.Patches(){public void read(int[] out,int x,int y,int width,int height){
            check(x>=0&&y>=0&&x+width<=w&&y+height<=h&&out.length>=width*height,"out of range .57 source read");
            for(int row=0;row<height;row++)System.arraycopy(image,(y+row)*w+x,out,row*width,width);
        }};
    }
    private static StrongNoise1958.Model model(int[] src,int w,int h){
        int[] snapshot=src.clone();
        StrongNoise1958.Model m=StrongNoise1958.prepare(patches(src,w,h),w,h,4,true);
        check(Arrays.equals(snapshot,src),"prepare changed caller's source");return m;
    }
    private static int[] filter(int[] src,int w,int h,StrongNoise1958.Model m){
        int[] snapshot=src.clone(),out=src.clone();
        StrongNoise1958.processRange(src,out,w,h,0,h,0,h,0,4,true,m,FREE);
        check(Arrays.equals(snapshot,src),"filter changed caller's source");return out;
    }
    private static int[] filter57(int[] src,int w,int h){
        int[] snapshot=src.clone(),out=src.clone();
        StrongNoise1957.Model m=StrongNoise1957.prepareJava(patches57(src,w,h),w,h,4,true);
        StrongNoise1957.processJavaRange(src,out,w,h,0,h,0,h,0,4,true,m,FREE57);
        check(Arrays.equals(snapshot,src),".57 filter changed caller's source");return out;
    }
    private static double rmse(int[] actual,int[] truth,int w,int x0,int y0,int x1,int y1){
        double sum=0;long n=0;
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++)for(int k=0;k<3;k++){
            double d=c(actual[y*w+x],k)-c(truth[y*w+x],k);sum+=d*d;n++;
        }
        return Math.sqrt(sum/n);
    }
    private static double chromaRmse(int[] actual,int[] truth,int w,int x0,int y0,int x1,int y1){
        double sum=0;long n=0;
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
            int p=actual[y*w+x],q=truth[y*w+x];
            double r=(c(p,0)-c(p,1))-(c(q,0)-c(q,1));
            double b=(c(p,2)-c(p,1))-(c(q,2)-c(q,1));sum+=r*r+b*b;n+=2;
        }
        return Math.sqrt(sum/n);
    }
    private static double transfer(int[] actual,int[] truth,int w,int x0,int y0,int x1,int y1){
        double am=0,tm=0,n=0;
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){am+=luma(actual[y*w+x]);tm+=luma(truth[y*w+x]);n++;}
        am/=n;tm/=n;double cov=0,energy=0;
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
            double a=luma(actual[y*w+x])-am,t=luma(truth[y*w+x])-tm;cov+=a*t;energy+=t*t;
        }
        return cov/energy;
    }
    private static double meanChannel(int[] image,int w,int x0,int y0,int x1,int y1,int channel){
        double sum=0;int n=0;
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){sum+=c(image[y*w+x],channel);n++;}
        return sum/n;
    }
    private static int differences(int[] a,int[] b){int n=0;for(int i=0;i<a.length;i++)if(a[i]!=b[i])n++;return n;}

    private static void visibleFlatImprovement(){
        int w=256,h=192;int[] truth=constant(w,h,rgb(42,47,53));
        int[] src=noisy(truth,6,5,19580901L),old=filter57(src,w,h),out=filter(src,w,h,model(src,w,h));
        double before=rmse(src,truth,w,20,20,w-20,h-20),r57=rmse(old,truth,w,20,20,w-20,h-20);
        double r58=rmse(out,truth,w,20,20,w-20,h-20);
        metric("darkFlatInputRmse",before);metric("darkFlat1957Rmse",r57);metric("darkFlat1958Rmse",r58);
        check(r58<=r57*.80,"NR9/10 must reduce residual grain by >=20% versus .57");
        check(r58<=before*.55,"NR9/10 must reduce RGB error by >=45% versus unprocessed noise");
        double meanBias=Math.abs(meanChannel(out,w,20,20,w-20,h-20,1)-47);
        metric("darkFlatGreenMeanBias",meanBias);check(meanBias<.8,"dark flat brightness drift");
        // Repeat at other brightness/noise levels and independent random seeds.
        // A primary-image win must not hide a new regression elsewhere.
        double sum57=r57*r57,sum58=r58*r58;
        int[][] levels={{29,35,42,4,4},{65,71,77,8,3},{43,47,54,6,5}};
        for(int i=0;i<levels.length;i++){
            int[] z=levels[i];int[] clean=constant(192,144,rgb(z[0],z[1],z[2]));
            int[] grain=noisy(clean,z[3],z[4],19580903L+i),previous=filter57(grain,192,144);
            int[] improved=filter(grain,192,144,model(grain,192,144));
            double a=rmse(previous,clean,192,16,16,176,128),b=rmse(improved,clean,192,16,16,176,128);
            metric("flatRepeat"+i+"1957Rmse",a);metric("flatRepeat"+i+"1958Rmse",b);
            check(b<=a*.95,"flat-noise improvement must repeat across brightness and independent seeds");
            sum57+=a*a;sum58+=b*b;
        }
        double aggregateRatio=Math.sqrt(sum58/sum57);metric("aggregateFlat1958To1957RmseRatio",aggregateRatio);
        check(aggregateRatio<=.80,"aggregate flat residual improvement must be >=20%");
    }

    private static void localAdaptation(){
        int w=384,h=192;int[] truth=constant(w,h,rgb(52,55,61));
        int[] mild=noisy(truth,1.5,1.5,19581001L),heavy=noisy(truth,8,6,19581002L),src=mild.clone();
        for(int y=0;y<h;y++)System.arraycopy(heavy,y*w+w/2,src,y*w+w/2,w/2);
        StrongNoise1958.Model m=model(src,w,h);int[] out=filter(src,w,h,m);
        double mildBefore=rmse(src,truth,w,24,20,160,h-20),mildAfter=rmse(out,truth,w,24,20,160,h-20);
        double heavyBefore=rmse(src,truth,w,224,20,w-24,h-20),heavyAfter=rmse(out,truth,w,224,20,w-24,h-20);
        double mildCorrection=rmse(out,src,w,24,20,160,h-20),heavyCorrection=rmse(out,src,w,224,20,w-24,h-20);
        metric("localMildInputRmse",mildBefore);metric("localMildOutputRmse",mildAfter);
        metric("localHeavyInputRmse",heavyBefore);metric("localHeavyOutputRmse",heavyAfter);
        metric("localMildCorrectionRmse",mildCorrection);metric("localHeavyCorrectionRmse",heavyCorrection);
        check(heavyAfter<heavyBefore*.55,"local heavy grain not removed");
        check(mildAfter<mildBefore*.85,"local mild grain not removed");
        check(heavyCorrection>mildCorrection*2.5,"same-luma regions need different effective corrections");
        double seam=rmse(out,truth,w,186,20,198,h-20);
        metric("localJoinRmse",seam);check(seam<heavyBefore*.75,"regional boundary creates residual seam");
    }

    private static void periodicDetailAndGlyphs(){
        int w=256,h=192;int[] truth=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int v=64+(int)Math.round(4*Math.sin(2*Math.PI*x/17)+3*Math.sin(2*Math.PI*(x+y)/29));
            if(y>=128)v=132;
            // Three coherent glyph-like shapes, including two-pixel strokes.
            if(y>=138&&y<178){
                if(x>=25&&x<28||x>=48&&x<51||y>=157&&y<160&&x>=25&&x<51)v=28;
                if(x>=77&&x<80||x>=103&&x<106||y>=138&&y<141&&x>=77&&x<106||y>=175&&y<178&&x>=77&&x<106)v=28;
                if(x>=139&&x<141||y>=138&&y<140&&x>=139&&x<170||y>=157&&y<159&&x>=139&&x<164)v=28;
            }
            truth[y*w+x]=rgb(v,v+2,v+4);
        }
        int[] cleanOut=filter(truth,w,h,model(truth,w,h));
        double cleanTransfer=transfer(cleanOut,truth,w,16,16,w-16,110);
        double cleanRmse=rmse(cleanOut,truth,w,16,16,w-16,110);
        metric("cleanFaintTextureTransfer",cleanTransfer);metric("cleanFaintTextureRmse",cleanRmse);
        int[] src=noisy(truth,3,2,19580902L),out=filter(src,w,h,model(src,w,h));
        double noisyTransfer=transfer(out,truth,w,16,16,w-16,110);
        metric("noisyFaintTexture1957Transfer",transfer(filter57(src,w,h),truth,w,16,16,w-16,110));
        metric("noisyFaintTextureTransfer",noisyTransfer);
        double stroke=meanChannel(out,w,25,143,28,153,1),background=meanChannel(out,w,33,143,43,153,1);
        double contrast=(background-stroke)/104.0;
        metric("glyphStrokeContrastTransfer",contrast);
        double narrow=meanChannel(out,w,139,143,141,153,1);
        metric("twoPixelStrokeCenterError",Math.abs(narrow-30));
        check(cleanTransfer>=.92,"low-contrast coherent texture contrast must stay >=92%");
        check(cleanRmse<=1.0,"clean faint texture must not be smoothed as noise");
        check(noisyTransfer>=.85,"noisy faint coherent texture contrast must stay >=85%");
        check(contrast>=.94,"coherent glyph contrast lost");
        check(Math.abs(narrow-30)<=4,"two-pixel coherent stroke blurred");
    }

    private static void independentChromaProtection(){
        int w=256,h=192;int[] truth=new int[w*h];
        // Equal-luma boundary: Y=48.46 versus 48.27, but a strong real colour edge.
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int add=y>=h/2?28:0;
            truth[y*w+x]=x<w/2?rgb(70+add,37+add,51+add):rgb(36+add,55+add,46+add);
        }
        int[] src=noisy(truth,1.5,7,19581201L),old=filter57(src,w,h),out=filter(src,w,h,model(src,w,h));
        double before=chromaRmse(src,truth,w,20,20,108,76),r57=chromaRmse(old,truth,w,20,20,108,76);
        double r58=chromaRmse(out,truth,w,20,20,108,76);
        metric("chromaInputRmse",before);metric("chroma1957Rmse",r57);metric("chroma1958Rmse",r58);
        double redContrast=meanChannel(out,w,126,20,128,76,0)-meanChannel(out,w,128,20,130,76,0);
        double greenContrast=meanChannel(out,w,128,20,130,76,1)-meanChannel(out,w,126,20,128,76,1);
        metric("isoluminantRedEdgeTransfer",redContrast/34);
        metric("isoluminantGreenEdgeTransfer",greenContrast/18);
        double nearLumaEdge=chromaRmse(out,truth,w,22,93,106,99);
        double nearLumaInput=chromaRmse(src,truth,w,22,93,106,99);
        metric("chromaNearLumaEdgeInputRmse",nearLumaInput);metric("chromaNearLumaEdgeOutputRmse",nearLumaEdge);
        check(r58<=before*.50,"wide chroma grain insufficiently reduced");
        check(r58<=r57*.90,"independent chroma NR must improve >=10% versus .57");
        check(redContrast/34>=.88&&greenContrast/18>=.88,"real isoluminant colour boundary smeared");
        check(nearLumaEdge<nearLumaInput*.65,"luma edge must not prevent independent colour grain removal");
    }

    private static void nlmRealContribution(){
        int w=72,h=56;int[] truth=constant(w,h,rgb(47,51,56));
        int[] src=noisy(truth,3.5,4.0,19581101L);float[] ev=new float[16];Arrays.fill(ev,4f);
        int[] local=new int[src.length],nlm=new int[src.length];
        StrongNoise1958.prepareJavaRangeLocalOnly(src,local,w,h,0,h,4,true,ev,0);
        StrongNoise1958.prepareJavaRange(src,nlm,w,h,0,h,4,true,ev,0);
        int[] localImage=reconstruct(src,local),nlmImage=reconstruct(src,nlm);
        double localError=rmse(localImage,truth,w,10,10,w-10,h-10),nlmError=rmse(nlmImage,truth,w,10,10,w-10,h-10);
        metric("halfNlmLocalOnlyRmse",localError);metric("halfNlmEnabledRmse",nlmError);
        metric("halfNlmChangedFraction",differences(local,nlm)/(double)src.length);
        check(differences(local,nlm)>src.length/100,"half-resolution NLM must make an actual image correction");
        check(nlmError<=localError*.995,"half-resolution NLM must improve known-truth error");
        StrongNoise1958.Model m=StrongNoise1958.prepareJava(patches(src,w,h),w,h,4,true);
        int[] fullLocal=src.clone(),fullNlm=src.clone();
        StrongNoise1958.processJavaRangeLocalOnly(src,fullLocal,w,h,0,h,0,h,0,4,true,m,FREE);
        StrongNoise1958.processJavaRange(src,fullNlm,w,h,0,h,0,h,0,4,true,m,FREE);
        double fullLocalError=rmse(fullLocal,truth,w,10,10,w-10,h-10),fullNlmError=rmse(fullNlm,truth,w,10,10,w-10,h-10);
        metric("fullNlmLocalOnlyRmse",fullLocalError);metric("fullNlmEnabledRmse",fullNlmError);
        metric("fullNlmChangedFraction",differences(fullLocal,fullNlm)/(double)src.length);
        check(differences(fullLocal,fullNlm)>src.length/100,"full-resolution NLM must make an actual image correction");
        check(fullNlmError<=fullLocalError*.995,"full-resolution NLM must improve known-truth error");
    }
    private static int[] reconstruct(int[] src,int[] map){
        int[] out=new int[src.length];for(int i=0;i<src.length;i++)out[i]=rgb(
            c(src[i],0)+(byte)(map[i]>>>16),c(src[i],1)+(byte)(map[i]>>>8),c(src[i],2)+(byte)map[i]);
        return out;
    }

    private static void offOwnershipStripsAndParity(){
        int w=91,h=137;int[] src=noisy(constant(w,h,rgb(45,50,56)),4,5,19581301L);
        for(int i=0;i<src.length;i+=43)src[i]=(src[i]&0xffffff)|((i*17)&255)<<24;
        int[] snapshot=src.clone(),off=src.clone();
        StrongNoise1958.Model disabled=StrongNoise1958.prepare(patches(src,w,h),w,h,0,true);
        StrongNoise1958.processRange(src,off,w,h,0,h,0,h,0,0,true,disabled,FREE);
        check(Arrays.equals(src,off),"noise OFF must be exact identity for every RGBA pixel");
        StrongNoise1958.Model m=model(src,w,h);int[] whole=filter(src,w,h,m);
        for(int i=0;i<src.length;i++){
            check((src[i]>>>24)==(whole[i]>>>24),"alpha channel changed");
            if((src[i]>>>24)!=255)check(src[i]==whole[i],"nonopaque source colour changed");
        }
        for(int chunk:new int[]{1,7,23,64}){
            int[] out=src.clone();
            for(int y=0;y<h;y+=chunk){
                int end=Math.min(h,y+chunk),origin=Math.max(0,y-StrongNoise1958.HALO),last=Math.min(h,end+StrongNoise1958.HALO);
                int[] input=Arrays.copyOfRange(src,origin*w,last*w),output=input.clone();
                StrongNoise1958.processRange(input,output,w,last-origin,y-origin,end-origin,0,last-origin,origin,4,true,m,FREE);
                System.arraycopy(output,(y-origin)*w,out,y*w,(end-y)*w);
            }
            check(Arrays.equals(whole,out),"whole/strip image differs at chunk "+chunk);
        }
        final int[] parallel=src.clone();final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] workers=new Thread[3];
        for(int i=0;i<workers.length;i++){
            final int begin=i*h/workers.length,end=(i+1)*h/workers.length;
            workers[i]=new Thread(new Runnable(){public void run(){try{
                StrongNoise1958.processRange(src,parallel,w,h,begin,end,0,h,0,4,true,m,FREE);
            }catch(Throwable t){failure.set(t);}}});workers[i].start();
        }
        for(Thread worker:workers)try{worker.join();}catch(InterruptedException e){throw new AssertionError(e);}
        check(failure.get()==null,"concurrent processing failed");check(Arrays.equals(whole,parallel),"parallel output differs");
        check(Arrays.equals(src,snapshot),"source ownership broken after strip/concurrent runs");
        if(StrongNoise1958.nativeAvailable()){
            for(int level=1;level<=4;level++){
                StrongNoise1958.Model jm=StrongNoise1958.prepareJava(patches(src,w,h),w,h,level,true);
                StrongNoise1958.Model nm=StrongNoise1958.prepare(patches(src,w,h),w,h,level,true);
                int[] java=src.clone(),nativeResult=src.clone();
                StrongNoise1958.processJavaRange(src,java,w,h,0,h,0,h,0,level,true,jm,FREE);
                StrongNoise1958.processRange(src,nativeResult,w,h,0,h,0,h,0,level,true,nm,FREE);
                check(Arrays.equals(java,nativeResult),"fresh native/Java output differs at noise level "+level);nativeParityCases++;
            }
            int[] opaque=noisy(constant(72,56,rgb(46,52,57)),6,5,19581302L);
            StrongNoise1958.Model jm=StrongNoise1958.prepareJava(patches(opaque,72,56),72,56,4,true);
            StrongNoise1958.Model nm=StrongNoise1958.prepare(patches(opaque,72,56),72,56,4,true);
            int[] java=opaque.clone(),nativeResult=opaque.clone();
            StrongNoise1958.processJavaRange(opaque,java,72,56,0,56,0,56,0,4,true,jm,FREE);
            StrongNoise1958.processRange(opaque,nativeResult,72,56,0,56,0,56,0,4,true,nm,FREE);
            check(Arrays.equals(java,nativeResult),"fresh native/Java output differs on unrestricted grain");nativeParityCases++;
            // More than eight evidence-grid rows exercise native row-window
            // offsets at the beginning, middle, and end of a narrow image.
            int tw=17,th=513;int[] truth=constant(tw,th,rgb(44,50,58));
            int[] a=noisy(truth,2,3,19581311L),b=noisy(truth,6,2,19581312L),d=noisy(truth,3,6,19581313L);
            int[] tall=a.clone();
            for(int y=170;y<th;y++)System.arraycopy(y<340?b:d,y*tw,tall,y*tw,tw);
            StrongNoise1958.Model tallJava=StrongNoise1958.prepareJava(patches(tall,tw,th),tw,th,4,true);
            StrongNoise1958.Model tallNative=StrongNoise1958.prepare(patches(tall,tw,th),tw,th,4,true);
            int[] expected=tall.clone();
            StrongNoise1958.processJavaRange(tall,expected,tw,th,0,th,0,th,0,4,true,tallJava,FREE);
            for(int chunk:new int[]{1,37,128}){
                int[] assembled=tall.clone();
                for(int y=0;y<th;y+=chunk){
                    int end=Math.min(th,y+chunk),origin=Math.max(0,y-StrongNoise1958.HALO),last=Math.min(th,end+StrongNoise1958.HALO);
                    int[] input=Arrays.copyOfRange(tall,origin*tw,last*tw),output=input.clone();
                    StrongNoise1958.processRange(input,output,tw,last-origin,y-origin,end-origin,0,last-origin,origin,4,true,tallNative,FREE);
                    System.arraycopy(output,(y-origin)*tw,assembled,y*tw,(end-y)*tw);
                }
                check(Arrays.equals(expected,assembled),"native regional evidence row-window differs chunk="+chunk);
                nativeParityCases++;
            }
        }
        long peak=StrongNoise1958.modelMemoryBytes(4284,5712),workspace=StrongNoise1958.workspaceBytes(4284,128);
        metric("modelPeak24MP",peak);metric("workspace24MP128Rows",workspace);
        check(peak<128L*1024*1024,"24.5MP model reservation must fit 128MiB");
        check(workspace<48L*1024*1024,"24.5MP worker reservation must fit 48MiB");
    }

    public static void main(String[] args){
        long start=System.nanoTime();
        visibleFlatImprovement();localAdaptation();periodicDetailAndGlyphs();independentChromaProtection();
        nlmRealContribution();offOwnershipStripsAndParity();
        metric("hostAcceptanceMilliseconds",(System.nanoTime()-start)/1e6);
        StringBuilder json=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions)
            .append(",\"nativeAvailable\":").append(StrongNoise1958.nativeAvailable())
            .append(",\"nativeParityCases\":").append(nativeParityCases)
            .append(",\"nativeParityPixelDifferences\":0,\"knownTruthFixtures\":true,\"relative1957Comparison\":true,\"halfAndFullNlmAblation\":true,\"physicalAndroidTested\":false,\"metrics\":{");
        boolean comma=false;for(Map.Entry<String,Double> entry:metrics.entrySet()){
            if(comma)json.append(',');comma=true;json.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
        }
        System.out.println(json.append("}}"));
    }
}
