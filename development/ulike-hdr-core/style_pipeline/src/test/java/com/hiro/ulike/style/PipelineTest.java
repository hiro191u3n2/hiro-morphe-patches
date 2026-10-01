package com.hiro.ulike.style;

import java.util.*;
import static com.hiro.ulike.style.SampledMakeupPipeline.*;

public final class PipelineTest {
    private static int checks=0;
    private static void check(boolean b) { checks++;if(!b)throw new AssertionError("check "+checks); }
    private static void rejects(Runnable r) {checks++;try {r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("expected rejection "+checks);}
    private static FrameTile tile(String id) {return new FrameTile(id,42,3,1,0,0,3,1);}
    private static ResolvedPass pass(Pass p,FrameTile tile,double intensity,double opacity) {
        boolean seg=p==Pass.PURITY_LIPS || p==Pass.PURITY_EYELASH || p.name().contains("EYE_") || p.name().contains("SHADOW_");
        return new ResolvedPass(p,tile,new double[]{.2,.3,.4,.5,.1,.2,.3,.5,.4,.2,.1,.5},
            new double[]{0,.5,1},intensity,opacity,null,seg?new double[]{.2,.5,1}:null,
            seg?new boolean[]{true,true,false}:null,
            (p==Pass.PURITY_BLUSHER || p==Pass.PURITY_3D) ? new double[]{.7,.1,.2,.3,.4,.5,.6,.7,.8}:null);
    }
    public static void main(String[] args) {
        FrameTile t=tile("same"); double[] photo={-0.0,.123456789,.7,.1,.2,.3,.4,.5,.6};
        for(Pass p:Pass.values()) {
            double[] result=apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(p,t,.6,.3));
            check(Double.doubleToRawLongBits(result[0])==Double.doubleToRawLongBits(photo[0]));
            for(double v:result)check(Double.isFinite(v) && v>=0 && v<=1);
            double[] zero=apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(p,t,0,1));
            if(p!=Pass.PURITY_3D)for(int i=0;i<photo.length;i++)check(Double.doubleToRawLongBits(zero[i])==Double.doubleToRawLongBits(photo[i]));
            else {check(zero[6]==.6);check(Math.abs(zero[3]-(.1*.5+.3*.5))<1e-15);}
            rejects(()->apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(p,tile("other"),.5,1)));
        }
        double[] lash0=apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(Pass.PURITY_EYELASH,t,.6,0));
        double[] lash1=apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(Pass.PURITY_EYELASH,t,.6,1));
        check(Arrays.equals(lash0,lash1)); // deliberately unused opacity
        check(Arrays.equals(apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(Pass.PURITY_3D,t,.6,0)),
                            apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(Pass.PURITY_3D,t,.6,1))));
        check(lash1[6]==photo[6]); // outside seg blocks lash
        double[] eye=apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(Pass.PURITY_EYE_MULTIPLY,t,.6,1));
        check(eye[6]!=photo[6]); // outside seg does not block eye
        rejects(()->new FrameTile("huge",0,100000,100000,0,0,100000,100000));
        rejects(()->new FrameTile("overflow",0,Integer.MAX_VALUE,1,Integer.MAX_VALUE,0,1,1));
        rejects(()->apply(null,t,photo,pass(Pass.NATURAL_BLUSHER,t,.5,1)));
        double[] bad=photo.clone();bad[2]=1.0000001;
        rejects(()->apply(Domain.ENCODED_SDR_FULL_RANGE,t,bad,pass(Pass.NATURAL_BLUSHER,t,.5,1)));
        double[] nan=photo.clone();nan[2]=Double.NaN;
        rejects(()->apply(Domain.ENCODED_SDR_FULL_RANGE,t,nan,pass(Pass.NATURAL_BLUSHER,t,.5,1)));
        rejects(()->apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,pass(Pass.NATURAL_BLUSHER,t,Double.NaN,1)));
        rejects(()->apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,new ResolvedPass(Pass.NATURAL_BLUSHER,t,
            new double[]{1,1,1,0,0,0,0,0,0,0,0,0},new double[]{1,1,1},1,1,null,null,null)));
        rejects(()->apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,new ResolvedPass(Pass.PURITY_FACIAL,t,
            new double[12],new double[]{1,1,1},1,1,null,null,null)));
        rejects(()->apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,new ResolvedPass(Pass.PURITY_LIPS,t,
            new double[12],new double[]{1,1,1},1,1,null,null,null)));
        rejects(()->apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,new ResolvedPass(Pass.PURITY_BLUSHER,t,
            new double[12],new double[]{1,1,1},1,1,null,null,null)));
        List<ResolvedPass> natural=Collections.singletonList(pass(Pass.NATURAL_BLUSHER,t,.6,1));
        check(Arrays.equals(run(Style.NATURAL_BLUSH,Domain.ENCODED_SDR_FULL_RANGE,t,photo,natural),
                            apply(Domain.ENCODED_SDR_FULL_RANGE,t,photo,natural.get(0))));
        List<ResolvedPass> purity=new ArrayList<>();
        for(Pass p:Pass.values())if(p.style==Style.PURITY2)purity.add(pass(p,t,.7,1));
        double[] pipeline=run(Style.PURITY2,Domain.ENCODED_SDR_FULL_RANGE,t,photo,purity);
        check(pipeline.length==photo.length);
        rejects(()->run(Style.PURITY2,Domain.ENCODED_SDR_FULL_RANGE,t,photo,purity.subList(0,8)));
        List<ResolvedPass> reversed=new ArrayList<>(purity);Collections.reverse(reversed);
        rejects(()->run(Style.PURITY2,Domain.ENCODED_SDR_FULL_RANGE,t,photo,reversed));
        List<ResolvedPass> duplicate=new ArrayList<>(purity);duplicate.add(purity.get(8));
        rejects(()->run(Style.PURITY2,Domain.ENCODED_SDR_FULL_RANGE,t,photo,duplicate));
        rejects(()->run(Style.NATURAL_BLUSH,Domain.ENCODED_SDR_FULL_RANGE,t,photo,purity));
        List<ResolvedPass> eyeReorder=new ArrayList<>(purity);Collections.swap(eyeReorder,4,5);
        check(run(Style.PURITY2,Domain.ENCODED_SDR_FULL_RANGE,t,photo,eyeReorder).length==9);
        StyleLutPipeline.Texture black=new StyleLutPipeline.Texture(1,1,new double[]{0,0,0});
        StyleLutPipeline.Texture white=new StyleLutPipeline.Texture(1,1,new double[]{1,1,1});
        double[] skin=StyleLutPipeline.skin(Domain.ENCODED_SDR_FULL_RANGE,t,photo,black,white,new double[]{0,.5,1},.4);
        check(Math.abs(skin[3]-(photo[3]*.6+.2))<1e-15);
        check(Arrays.equals(StyleLutPipeline.finalLut(Domain.ENCODED_SDR_FULL_RANGE,t,photo,black,0),photo));
        StyleLutPipeline.ResolvedLuts luts=new StyleLutPipeline.ResolvedLuts(t,black,white,new double[]{0,.5,1},.3,black,.2);
        check(StyleLutPipeline.runPostNeural(Style.PURITY2,Domain.ENCODED_SDR_FULL_RANGE,t,photo,purity,luts).length==9);
        rejects(()->StyleLutPipeline.runPostNeural(Style.NATURAL_BLUSH,Domain.ENCODED_SDR_FULL_RANGE,t,photo,natural,luts));
        StyleLutPipeline.ResolvedLuts wrong=new StyleLutPipeline.ResolvedLuts(tile("wrong"),black,white,new double[]{0,.5,1},.3,black,.2);
        rejects(()->StyleLutPipeline.runPostNeural(Style.PURITY2,Domain.ENCODED_SDR_FULL_RANGE,t,photo,purity,wrong));
        rejects(()->new StyleLutPipeline.Texture(2048,2048,new double[3]));
        System.out.println("{\"contract_checks\":"+checks+",\"status\":\"PASS\"}");
    }
}
