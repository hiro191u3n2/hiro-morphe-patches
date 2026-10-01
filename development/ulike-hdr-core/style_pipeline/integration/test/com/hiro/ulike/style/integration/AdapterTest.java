package com.hiro.ulike.style.integration;

import com.hiro.ulike.style.SampledMakeupPipeline;
import com.hiro.ulike.style.StyleLutPipeline;
import hiro.ulike.beauty.BeautyImageEngine;
import java.util.*;

public final class AdapterTest {
    private static int checks;
    private interface Throwing {void run()throws Exception;}
    private static void check(boolean v) {checks++;if(!v)throw new AssertionError("check "+checks);}
    private static Throwable fails(Throwing r) {checks++;try{r.run();}catch(Exception|Error failure){return failure;}throw new AssertionError("expected failure");}
    private static final Object ID=new Object();
    private static final StyleLutPipeline.Texture BLACK=new StyleLutPipeline.Texture(1,1,new double[3]);
    private static final class Sink implements BeautyImageEngine.TransactionalSink {
        int begin,write,commit,abort,next,width;float[] stage,visible={.9876543f};Object token;
        boolean failBegin,failWrite,failCommit,failAbort;List<Integer> rows=new ArrayList<>();
        public void begin(int w,int h,Object id)throws Exception {begin++;width=w;stage=new float[w*h*3];token=id;if(failBegin)throw new Exception("begin");}
        public void writeRows(int first,int count,float[] data)throws Exception {write++;rows.add(count);if(failWrite)throw new Exception("write");if(first!=next)throw new Exception("noncontiguous sink");System.arraycopy(data,0,stage,first*width*3,count*width*3);next+=count;}
        public void commit()throws Exception {commit++;if(failCommit)throw new Exception("commit");visible=stage;stage=null;}
        public void abort()throws Exception {abort++;stage=null;if(failAbort)throw new Exception("abort");}
    }
    private static PostNeuralStyleSink.ResolvedTile bound(Object identity,SampledMakeupPipeline.FrameTile tile,double intensity) {
        int n=tile.pixels();double[] rgba=new double[n*4],coverage=new double[n],mask=new double[n];
        for(int i=0;i<n;i++){rgba[i*4]=.25;rgba[i*4+1]=.375;rgba[i*4+2]=.5;rgba[i*4+3]=.5;coverage[i]=1;}
        SampledMakeupPipeline.ResolvedPass p=new SampledMakeupPipeline.ResolvedPass(
            SampledMakeupPipeline.Pass.NATURAL_BLUSHER,tile,rgba,coverage,intensity,1,null,null,null);
        StyleLutPipeline.ResolvedLuts luts=new StyleLutPipeline.ResolvedLuts(tile,BLACK,BLACK,mask,0,null,0);
        return new PostNeuralStyleSink.ResolvedTile(identity,Collections.singletonList(p),luts);
    }
    private static PostNeuralStyleSink make(int w,int h,PostNeuralStyleSink.BindingProvider binding,Sink sink) {
        return new PostNeuralStyleSink(ID,"capture-42",42,w,h,SampledMakeupPipeline.Style.NATURAL_BLUSH,
            SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE,
            PostNeuralStyleSink.GraphPolicy.DECLARED_OUTER_ORDER_WITH_CALLER_RESOLVED_SOURCES_AND_SUBORDER,
            new PostNeuralStyleSink.Budget(8,10,1000,1080),binding,sink);
    }
    private static PostNeuralStyleSink make(Sink sink) {return make(5,7,(id,tile)->bound(id,tile,0),sink);}
    private static float[] data(int pixels,int first) {
        float[] result=new float[pixels*3];for(int i=0;i<result.length;i++)result[i]=(float)((first+i+1)/200.0);
        if(first==0)result[0]=-0.0f;return result;
    }
    public static void main(String[] args)throws Exception {
        Sink sink=new Sink();PostNeuralStyleSink adapter=make(sink);adapter.begin(5,7,ID);
        float[] first=data(20,0),last=data(20,60);Arrays.fill(last,45,last.length,Float.NaN);
        adapter.writeRows(0,4,first);adapter.writeRows(4,3,last);
        check(sink.visible.length==1);adapter.commit();
        check(sink.rows.equals(Arrays.asList(2,2,2,1)));check(sink.token==ID);
        for(int i=0;i<60;i++)check(Float.floatToRawIntBits(first[i])==Float.floatToRawIntBits(sink.visible[i]));
        for(int i=0;i<45;i++)check(Float.floatToRawIntBits(last[i])==Float.floatToRawIntBits(sink.visible[60+i]));
        check(sink.abort==0 && sink.commit==1);adapter.abort();check(sink.abort==0);
        fails(()->adapter.writeRows(7,1,new float[15]));fails(()->adapter.begin(5,7,ID));

        Sink edited=new Sink();PostNeuralStyleSink actual=make(5,1,(id,tile)->bound(id,tile,.7),edited);
        actual.begin(5,1,ID);float[] samples=data(5,5);actual.writeRows(0,1,samples);actual.commit();
        for(int i=0;i<samples.length;i++) {
            double s=(i%3==0?.5:i%3==1?.75:1);
            float expected=(float)((double)samples[i]*.65+(double)samples[i]*s*.35);
            check(Float.floatToRawIntBits(expected)==Float.floatToRawIntBits(edited.visible[i]));
        }
        check(actual.arrayBytesForPixels(5)==540);

        Sink beginFailure=new Sink();beginFailure.failBegin=true;PostNeuralStyleSink a=make(beginFailure);
        check("begin".equals(fails(()->a.begin(5,7,ID)).getMessage()));check(beginFailure.abort==1);a.abort();check(beginFailure.abort==1);
        Sink wrongId=new Sink();PostNeuralStyleSink b=make(wrongId);
        fails(()->b.begin(5,7,new Object()));check(wrongId.begin==0);b.abort();check(wrongId.abort==0);
        Sink mismatch=new Sink();PostNeuralStyleSink c=make(5,7,(id,t)->bound(new Object(),t,0),mismatch);
        c.begin(5,7,ID);fails(()->c.writeRows(0,1,new float[15]));check(mismatch.abort==1 && mismatch.write==0);fails(c::commit);
        Sink mismatchTile=new Sink();PostNeuralStyleSink d=make(5,7,(id,t)->bound(id,
            new SampledMakeupPipeline.FrameTile("capture-42",42,5,7,0,1,5,t.height),0),mismatchTile);
        d.begin(5,7,ID);fails(()->d.writeRows(0,1,new float[15]));check(mismatchTile.abort==1 && mismatchTile.write==0);
        Sink providerFail=new Sink();PostNeuralStyleSink e=make(5,7,(id,t)->{throw new Exception("provider");},providerFail);
        e.begin(5,7,ID);check("provider".equals(fails(()->e.writeRows(0,1,new float[15])).getMessage()));check(providerFail.abort==1);
        Sink writeFail=new Sink();writeFail.failWrite=true;writeFail.failAbort=true;PostNeuralStyleSink f=make(writeFail);
        f.begin(5,7,ID);Throwable problem=fails(()->f.writeRows(0,1,new float[15]));check("write".equals(problem.getMessage()));check(problem.getSuppressed().length==1);f.abort();check(writeFail.abort==1);
        Sink partial=new Sink();PostNeuralStyleSink g=make(partial);g.begin(5,7,ID);g.writeRows(0,1,new float[15]);fails(g::commit);check(partial.abort==1 && partial.commit==0 && partial.visible.length==1);
        Sink commitFail=new Sink();commitFail.failCommit=true;PostNeuralStyleSink h=make(5,1,(id,t)->bound(id,t,0),commitFail);
        h.begin(5,1,ID);h.writeRows(0,1,new float[15]);fails(h::commit);check(commitFail.abort==1 && commitFail.visible.length==1);
        for(int which=0;which<6;which++) {
            Sink target=new Sink();PostNeuralStyleSink bad=make(target);bad.begin(5,7,ID);
            final int scenario=which;
            fails(()-> {
                if(scenario==0)bad.writeRows(1,1,new float[15]);
                if(scenario==1)bad.writeRows(0,0,new float[0]);
                if(scenario==2)bad.writeRows(0,1,new float[14]);
                if(scenario==3)bad.writeRows(0,1,new float[1000]);
                if(scenario==4){float[] x=new float[15];x[0]=Float.NaN;bad.writeRows(0,1,x);}
                if(scenario==5){float[] x=new float[15];x[0]=1.00001f;bad.writeRows(0,1,x);}
            });
            check(target.abort==1 && target.write==0);bad.abort();check(target.abort==1);
        }
        fails(()->new PostNeuralStyleSink.Budget(65,10,100,1000));
        fails(()->new PostNeuralStyleSink(ID,"id",1,5,1,SampledMakeupPipeline.Style.NATURAL_BLUSH,
            SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE,null,PostNeuralStyleSink.Budget.standard(),(id,t)->bound(id,t,0),new Sink()));
        fails(()->new PostNeuralStyleSink(ID,"id",1,5,1,SampledMakeupPipeline.Style.NATURAL_BLUSH,
            SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE,
            PostNeuralStyleSink.GraphPolicy.DECLARED_OUTER_ORDER_WITH_CALLER_RESOLVED_SOURCES_AND_SUBORDER,
            new PostNeuralStyleSink.Budget(8,10,100,100),(id,t)->bound(id,t,0),new Sink()));
        System.out.println("{\"status\":\"PASS\",\"adapter_checks\":"+checks+",\"integer_photo_quantization\":false}");
    }
}
