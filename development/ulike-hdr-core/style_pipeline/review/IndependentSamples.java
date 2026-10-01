package com.hiro.ulike.style;

import java.util.*;
import static com.hiro.ulike.style.SampledMakeupPipeline.*;

/** Independent literal nested-fragment oracle, not the production factored weights. */
public final class IndependentSamples {
    static int checks;static double maximumError;
    static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
    static void near(double a,double b,String message){maximumError=Math.max(maximumError,Math.abs(a-b));check(Double.isFinite(a) && Math.abs(a-b)<2e-14,message+" "+a+" != "+b);}
    static void reject(Runnable r,String message){checks++;try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(message);}
    static double mix(double a,double b,double t){return a*(1-t)+b*t;}
    static double clamp(double x){return Math.max(0,Math.min(1,x));}
    static boolean segmented(Pass p){return p==Pass.PURITY_LIPS || p==Pass.PURITY_EYELASH || p.name().contains("EYE_") || p.name().contains("SHADOW_");}
    static boolean canColor(Pass p){return p==Pass.NATURAL_BLUSHER || p==Pass.PURITY_BLUSHER || p==Pass.PURITY_LIPS || p==Pass.PURITY_EYELASH;}
    static ResolvedPass resolved(Pass p,FrameTile t,double[] rgba,double coverage,double intensity,double opacity,double[] color,double seg,boolean inside,double[] source){
        return new ResolvedPass(p,t,rgba,new double[]{coverage},intensity,opacity,color,segmented(p)?new double[]{seg}:null,segmented(p)?new boolean[]{inside}:null,(p==Pass.PURITY_BLUSHER || p==Pass.PURITY_3D)?source:null);
    }
    static double literal(Pass p,double destination,double base,double overlay,double red,double alpha,double coverage,double intensity,double opacity,double seg,boolean inside){
        if(coverage==0)return destination;
        if(p==Pass.NATURAL_BLUSHER || p==Pass.PURITY_BLUSHER){double fragmentAlpha=alpha*intensity*opacity;return mix(destination,base*overlay*fragmentAlpha+destination*(1-fragmentAlpha),coverage);}
        if(p==Pass.PURITY_3D)return mix(destination,mix(base,base*overlay,alpha*intensity),coverage);
        if(p==Pass.PURITY_FACIAL){double result=overlay<.5?2*base*overlay+base*base*(1-2*overlay):Math.sqrt(base)*(2*overlay-1)+2*base*(1-overlay);double fragmentAlpha=clamp((Math.abs(red-.5)-2.0/255)*32)*intensity*opacity;return mix(destination,result*fragmentAlpha+destination*(1-fragmentAlpha),coverage);}
        boolean lash=p==Pass.PURITY_EYELASH;
        if(!lash && alpha<.001)return destination;
        double altered=p.name().endsWith("SCREEN")?1-(1-base)*(1-overlay):base*overlay;
        double result=mix(base,altered,alpha);
        result=mix(base,result,lash?intensity:intensity*opacity);
        result=mix(base,result,inside?seg:(lash?0:1));
        return mix(destination,result,coverage);
    }
    static StyleLutPipeline.Texture gradient(int w,int h){double[] data=new double[w*h*3];for(int y=0;y<h;y++)for(int x=0;x<w;x++){data[(y*w+x)*3]=(double)x/(w-1);data[(y*w+x)*3+1]=(double)y/(h-1);data[(y*w+x)*3+2]=(double)(x+y)/(w+h-2);}return new StyleLutPipeline.Texture(w,h,data);}
    static double textureExpected(int w,int h,double u,double v,int c){double x=clamp((u*w-.5)/(w-1)),y=clamp((v*h-.5)/(h-1));return c==0?x:c==1?y:(x*(w-1)+y*(h-1))/(w+h-2);}
    static double sampleSlice(int w,int h,int s,double r,double g,int c){double u=(s%8)/8.0+1.0/1024+(1.0/8-1.0/512)*r,v=(s/8)/8.0+1.0/1024+(1.0/8-1.0/512)*g;return textureExpected(w,h,u,v,c);}
    public static void main(String[] args){
        Random random=new Random(191603);FrameTile tile=new FrameTile("owned-still",900,4096,3072,4095,3071,1,1);
        for(Pass p:Pass.values())for(int k=0;k<1200;k++){
            double[] dst={random.nextDouble(),random.nextDouble(),random.nextDouble()},base=(p==Pass.PURITY_BLUSHER || p==Pass.PURITY_3D)?new double[]{random.nextDouble(),random.nextDouble(),random.nextDouble()}:dst;
            double a=k%13==0?.0005:k%13==1?.001:k%13==2?1:random.nextDouble();
            double[] straight={random.nextDouble(),random.nextDouble(),random.nextDouble()},rgba={straight[0]*a,straight[1]*a,straight[2]*a,a};
            double coverage=k%7==0?0:random.nextDouble(),intensity=k%11==0?0:random.nextDouble(),opacity=k%9==0?0:random.nextDouble(),seg=random.nextDouble();boolean inside=k%3!=0;
            double[] color=canColor(p) && k%5==0?new double[]{.7,.03,.34}:null;
            double[] actual=apply(Domain.ENCODED_SDR_FULL_RANGE,tile,dst,resolved(p,tile,rgba,coverage,intensity,opacity,color,seg,inside,base));
            for(int c=0;c<3;c++){double s=color==null?rgba[c]/a:color[c];near(actual[c],literal(p,dst[c],base[c],s,rgba[0]/a,a,coverage,intensity,opacity,seg,inside),p+" literal fragment case "+k);}
        }
        for(Pass p:new Pass[]{Pass.PURITY_3D,Pass.PURITY_EYELASH}){
            double[] dst={.6,.2,.8},src={.1,.9,.3},rgba={.2,.4,.1,.5};
            double[] zero=apply(Domain.ENCODED_SDR_FULL_RANGE,tile,dst,resolved(p,tile,rgba,1,.8,0,null,.7,true,src));
            double[] one=apply(Domain.ENCODED_SDR_FULL_RANGE,tile,dst,resolved(p,tile,rgba,1,.8,1,null,.7,true,src));
            check(Arrays.equals(zero,one),p+" must ignore opacity");
        }
        double[] signed={-0.0,.3333333333333333,.9};
        for(Pass p:Pass.values()){
            ResolvedPass inactive=resolved(p,tile,new double[]{.1,.2,.3,.5},0,.7,.4,null,.8,false,new double[]{.9,.4,.1});
            double[] out=apply(Domain.ENCODED_SDR_FULL_RANGE,tile,signed,inactive);
            for(int i=0;i<3;i++)check(Double.doubleToRawLongBits(out[i])==Double.doubleToRawLongBits(signed[i]),"coverage zero bits "+p);
        }
        FrameTile shifted=new FrameTile("owned-still",900,4096,3072,4094,3071,1,1);
        ResolvedPass wrong=resolved(Pass.NATURAL_BLUSHER,shifted,new double[]{.1,.2,.3,.5},1,1,1,null,1,true,null);
        reject(()->apply(Domain.ENCODED_SDR_FULL_RANGE,tile,signed,wrong),"different tile accepted");
        reject(()->apply(Domain.ENCODED_SDR_FULL_RANGE,tile,signed,new ResolvedPass(Pass.PURITY_BLUSHER,tile,new double[]{.1,.2,.3,.5},new double[]{1},1,1,null,null,null)),"missing video source accepted");
        reject(()->apply(Domain.ENCODED_SDR_FULL_RANGE,tile,signed,new ResolvedPass(Pass.PURITY_3D,tile,new double[]{.1,.2,.3,.5},new double[]{1},1,1,null,null,null)),"missing 3D source accepted");
        int[][] dims={{512,512},{1024,512},{8,16}};
        for(int[] dim:dims){StyleLutPipeline.Texture lut=gradient(dim[0],dim[1]);for(int j=0;j<256;j++){
            double blue=j<64?j/63.0:(j-64+.37)/192.0;double[] photo={random.nextDouble(),random.nextDouble(),blue};double intensity=.734;
            double[] finalImage=StyleLutPipeline.finalLut(Domain.ENCODED_SDR_FULL_RANGE,tile,photo,lut,intensity);
            double[] skin=StyleLutPipeline.skin(Domain.ENCODED_SDR_FULL_RANGE,tile,photo,lut,lut,new double[]{.312},intensity);
            int lo=(int)Math.floor(blue*63),hi=(int)Math.ceil(blue*63);double t=blue*63-lo;
            for(int c=0;c<3;c++){double lower=sampleSlice(dim[0],dim[1],lo,photo[0],photo[1],c),upper=sampleSlice(dim[0],dim[1],hi,photo[0],photo[1],c);near(finalImage[c],mix(photo[c],mix(lower,upper,t),intensity),"two-slice LUT");near(skin[c],mix(photo[c],lower,intensity),"floor-slice skin LUT");}
        }}
        System.out.println("{\"status\":\"PASS\",\"independent_assertions\":"+checks+",\"maximum_absolute_error\":"+maximumError+",\"literal_shader_cases\":12000,\"lut_pixel_cases\":768}");
    }
}
