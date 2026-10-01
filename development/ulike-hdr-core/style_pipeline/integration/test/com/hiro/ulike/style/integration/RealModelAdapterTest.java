package com.hiro.ulike.style.integration;

import com.hiro.ulike.style.SampledMakeupPipeline;
import com.hiro.ulike.style.StyleLutPipeline;
import hiro.ulike.beauty.BeautyImageEngine;
import hiro.ulike.beauty.PinnedAssets;
import hiro.ulike.model.PinnedModel;
import java.io.*;
import java.util.*;

/** Both real models through the actual adapter. Makeup geometry/masks here are
 * explicitly synthetic fixtures, not recovered face tracking or style parity.
 */
public final class RealModelAdapterTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class Source implements BeautyImageEngine.SourceRgbFloat {
        final Object identity=new Object();
        public int width(){return 512;}public int height(){return 384;}
        public Object frameIdentity(){return identity;}
        public void readPixel(int x,int y,float[] rgb){rgb[0]=(float)((x+.3)/513.0);rgb[1]=(float)((y+.7)/385.0);rgb[2]=(float)(((x+3*y)%197+.1)/198.0);}
    }
    static final class Sink implements BeautyImageEngine.TransactionalSink {
        float[] staged,visible;int width,next,aborts;Object token;
        public void begin(int w,int h,Object id){width=w;staged=new float[w*h*3];token=id;}
        public void writeRows(int y,int n,float[] rgb){check(y==next,"sink row alignment");System.arraycopy(rgb,0,staged,y*width*3,n*width*3);next+=n;}
        public void commit(){visible=staged;staged=null;}
        public void abort(){aborts++;staged=null;}
    }
    static PostNeuralStyleSink.ResolvedTile resolve(Object identity,SampledMakeupPipeline.Style style,
                                                   SampledMakeupPipeline.FrameTile tile) {
        List<SampledMakeupPipeline.ResolvedPass> passes=new ArrayList<>();int n=tile.pixels();
        for(SampledMakeupPipeline.Pass p:SampledMakeupPipeline.Pass.values())if(p.style==style) {
            boolean active=p==SampledMakeupPipeline.Pass.NATURAL_BLUSHER || p==SampledMakeupPipeline.Pass.PURITY_LIPS;
            boolean seg=p==SampledMakeupPipeline.Pass.PURITY_LIPS || p==SampledMakeupPipeline.Pass.PURITY_EYELASH || p.name().contains("EYE_") || p.name().contains("SHADOW_");
            double[] rgba=new double[n*4],coverage=new double[n],mask=seg?new double[n]:null;
            boolean[] inside=seg?new boolean[n]:null;
            for(int i=0;i<n;i++){rgba[4*i]=.25;rgba[4*i+1]=.375;rgba[4*i+2]=.5;rgba[4*i+3]=.5;coverage[i]=active?1:0;if(seg){mask[i]=1;inside[i]=true;}}
            double[] base=p==SampledMakeupPipeline.Pass.PURITY_BLUSHER || p==SampledMakeupPipeline.Pass.PURITY_3D?new double[n*3]:null;
            passes.add(new SampledMakeupPipeline.ResolvedPass(p,tile,rgba,coverage,.7,1,null,mask,inside,base));
        }
        StyleLutPipeline.Texture black=new StyleLutPipeline.Texture(1,1,new double[3]);
        StyleLutPipeline.ResolvedLuts luts=new StyleLutPipeline.ResolvedLuts(tile,black,black,new double[n],0,
            style==SampledMakeupPipeline.Style.PURITY2?black:null,0);
        return new PostNeuralStyleSink.ResolvedTile(identity,passes,luts);
    }
    static PostNeuralStyleSink adapter(Source source,SampledMakeupPipeline.Style style,Sink sink,boolean lateFailure) {
        return new PostNeuralStyleSink(source.identity,"owned-fixture",42,512,384,style,
            SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE,
            PostNeuralStyleSink.GraphPolicy.DECLARED_OUTER_ORDER_WITH_CALLER_RESOLVED_SOURCES_AND_SUBORDER,
            new PostNeuralStyleSink.Budget(8,512*3,25000000,32*1024*1024),
            (id,tile)->{if(lateFailure && tile.y>=3)throw new IOException("intentional binding failure");return resolve(id,style,tile);},sink);
    }
    public static void main(String[] args)throws Exception {
        PinnedModel.Style modelStyle=PinnedModel.Style.valueOf(args[0]);PinnedModel.CompiledModel model;
        try(InputStream m=new FileInputStream(args[1]);InputStream b=new FileInputStream(args[2]);InputStream e=new FileInputStream(args[3])){model=PinnedModel.compile(modelStyle,m,b,e);}
        PinnedAssets.NeuralMask mask=PinnedAssets.loadMask(new File(args[4]),modelStyle,false);
        SampledMakeupPipeline.Style style=SampledMakeupPipeline.Style.valueOf(args[0]);Source source=new Source();
        double[] matrix={.7,0,-50,0,.7,-10,0,0,1};
        try(BeautyImageEngine engine=BeautyImageEngine.openHostForVerification(model)) {
            Sink direct=new Sink();engine.processWithTransform(source,matrix,mask,.63,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),direct);
            Sink converted=new Sink();BeautyImageEngine.Result r=engine.processWithTransform(source,matrix,mask,.63,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),adapter(source,style,converted,false));
            check(r.frameIdentity==source.identity && converted.token==source.identity,"exact frame identity");
            check(!r.hdrPreserved && !r.completeStyle && !r.nativePixelParity,"no unsupported full-style/HDR claim");
            check(converted.visible.length==512*384*3 && converted.staged==null && converted.aborts==0,"complete atomic output");
            for(int i=0;i<direct.visible.length;i++) {
                double x=direct.visible[i],s=i%3==0?.5:i%3==1?.75:1;
                float expected=(float)(x*(1-.5*.7)+x*s*(.5*.7));
                check(Float.floatToRawIntBits(converted.visible[i])==Float.floatToRawIntBits(expected),"analytic edited pixel "+i);
            }
            Sink failed=new Sink();try {
                engine.processWithTransform(source,matrix,mask,.63,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard(),adapter(source,style,failed,true));
                throw new AssertionError("late binding failure accepted");
            }catch(IOException expected){check(failed.visible==null && failed.staged==null && failed.aborts==1,"outer engine + adapter rollback once");}
        }
        System.out.println("{\"style\":\""+style+"\",\"status\":\"PASS\",\"checks\":"+checks+",\"actual_model\":true,\"actual_neural_mask\":true,\"makeup_geometry\":\"synthetic_fixture\",\"hdr_verified\":false}");
    }
}
