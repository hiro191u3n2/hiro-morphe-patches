package com.hiro.ulike.style.integration;

import hiro.ulike.beauty.BeautyImageEngine;
import com.hiro.ulike.style.*;
import static com.hiro.ulike.style.SampledMakeupPipeline.*;
import java.util.*;

public final class IndependentSink {
    static int checks;static final Object FRAME=new Object();static final int W=7,H=10;
    static void check(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
    interface Action{void run()throws Exception;}
    static Throwable fails(Action run,String why){checks++;try{run.run();}catch(Exception|AssertionError e){return e;}throw new AssertionError("expected failure: "+why);}
    static final class Storage implements BeautyImageEngine.TransactionalSink {
        int begins,aborts,commits,nextRow;float[] staging,visible;String failure="";boolean abortFails;
        final List<Integer> rowCounts=new ArrayList<>();
        public void begin(int w,int h,Object id){begins++;staging=new float[w*h*3];if(failure.equals("begin"))throw new IllegalStateException("original-begin");}
        public void writeRows(int row,int rows,float[] rgb){check(staging!=null && row==nextRow,"downstream contiguous");rowCounts.add(rows);System.arraycopy(rgb,0,staging,row*W*3,rows*W*3);nextRow+=rows;if(failure.equals("write"))throw new IllegalStateException("original-write");}
        public void commit(){commits++;if(failure.equals("commit"))throw new IllegalStateException("original-commit");visible=staging;staging=null;}
        public void abort(){aborts++;staging=null;if(abortFails)throw new IllegalStateException("cleanup-error");}
    }
    static final class Provider implements PostNeuralStyleSink.BindingProvider {
        int calls;String failure="";
        public PostNeuralStyleSink.ResolvedTile resolve(Object identity,FrameTile tile)throws Exception{
            calls++;check(identity==FRAME,"provider exact identity");check(tile.imageWidth==W && tile.imageHeight==H && tile.sensorTimestampNs==43 && tile.captureId.equals("actual") && tile.x==0,"provider full frame");
            if(failure.equals("throw"))throw new IllegalStateException("provider-error");
            FrameTile used=failure.equals("timestamp")?new FrameTile(tile.captureId,44,W,H,0,tile.y,tile.width,tile.height):tile;
            int n=used.pixels();double[] rgba=new double[n*4],cover=new double[n],mask=new double[n];Arrays.fill(cover,1);
            for(int i=0;i<n;i++){rgba[i*4]=.125;rgba[i*4+1]=.25;rgba[i*4+2]=.375;rgba[i*4+3]=.5;}
            ResolvedPass pass=new ResolvedPass(Pass.NATURAL_BLUSHER,used,rgba,cover,.6,.8,null,null,null);
            StyleLutPipeline.Texture black=new StyleLutPipeline.Texture(1,1,new double[]{0,0,0});
            StyleLutPipeline.ResolvedLuts luts=new StyleLutPipeline.ResolvedLuts(used,black,black,mask,0,null,0);
            return new PostNeuralStyleSink.ResolvedTile(failure.equals("identity")?new Object():identity,Collections.singletonList(pass),luts);
        }
    }
    static PostNeuralStyleSink adapter(Provider provider,Storage sink){return new PostNeuralStyleSink(FRAME,"actual",43,W,H,Style.NATURAL_BLUSH,Domain.ENCODED_SDR_FULL_RANGE,PostNeuralStyleSink.GraphPolicy.DECLARED_OUTER_ORDER_WITH_CALLER_RESOLVED_SOURCES_AND_SUBORDER,new PostNeuralStyleSink.Budget(8,1000,1000,3L*W*3*36),provider,sink);}
    static float value(int y,int x,int c){return (float)((y*W*3+x*3+c+.123456)/(W*H*3));}
    static float[] input(int first,int rows,int capacity){float[] out=new float[capacity*W*3];Arrays.fill(out,Float.NaN);for(int y=0;y<rows;y++)for(int x=0;x<W;x++)for(int c=0;c<3;c++)out[(y*W+x)*3+c]=value(first+y,x,c);return out;}
    public static void main(String[] args)throws Exception{
        Provider provider=new Provider();Storage storage=new Storage();PostNeuralStyleSink sink=adapter(provider,storage);
        sink.begin(W,H,FRAME);sink.writeRows(0,8,input(0,8,8));sink.writeRows(8,2,input(8,2,8));sink.commit();
        check(storage.visible!=null && storage.aborts==0 && storage.commits==1 && provider.calls==4,"successful staged transaction");
        check(storage.rowCounts.equals(Arrays.asList(3,3,2,2)),"memory subdivision exact");
        for(int y=0;y<H;y++)for(int x=0;x<W;x++)for(int c=0;c<3;c++){double source=value(y,x,c),color=(c+1)*.25;float expected=(float)(source*(1-.24)+source*color*.24);check(Float.floatToRawIntBits(storage.visible[(y*W+x)*3+c])==Float.floatToRawIntBits(expected),"analytic row conversion");}
        sink.abort();check(storage.aborts==0,"successful commit never aborted");fails(sink::commit,"double commit");fails(()->sink.writeRows(0,1,input(0,1,1)),"write after commit");
        for(String failure:Arrays.asList("identity","timestamp","throw")){
            Provider b=new Provider();b.failure=failure;Storage s=new Storage();PostNeuralStyleSink a=adapter(b,s);a.begin(W,H,FRAME);fails(()->a.writeRows(0,8,input(0,8,8)),failure);check(s.aborts==1 && s.visible==null,"provider failure rollback");a.abort();check(s.aborts==1,"idempotent outer abort");fails(a::commit,"failed transaction commit");
        }
        for(String failure:Arrays.asList("begin","write","commit")){
            Storage s=new Storage();s.failure=failure;s.abortFails=true;PostNeuralStyleSink a=adapter(new Provider(),s);
            Throwable problem=fails(()->{a.begin(W,H,FRAME);a.writeRows(0,8,input(0,8,8));a.writeRows(8,2,input(8,2,8));a.commit();},failure);
            check(problem.getMessage().equals("original-"+failure) && problem.getSuppressed().length==1 && problem.getSuppressed()[0].getMessage().equals("cleanup-error"),"first failure retained with cleanup suppressed");
            check(s.aborts==1 && s.visible==null,"downstream rollback");a.abort();check(s.aborts==1,"no repeated failing cleanup");
        }
        for(int scenario=0;scenario<7;scenario++){
            final int which=scenario;Storage s=new Storage();Provider b=new Provider();PostNeuralStyleSink a=adapter(b,s);a.begin(W,H,FRAME);
            fails(()->{if(which==0)a.commit();else if(which==1)a.writeRows(1,1,input(0,1,1));else if(which==2)a.writeRows(0,9,input(0,9,9));else if(which==3)a.writeRows(0,1,new float[2]);else if(which==4){float[] bad=input(0,1,1);bad[5]=Float.NaN;a.writeRows(0,1,bad);}else if(which==5)a.writeRows(0,1,new float[W*9*3]);else{a.writeRows(0,1,input(0,1,1));a.writeRows(0,1,input(0,1,1));}},"row preflight "+which);
            check(s.aborts==1 && s.visible==null,"invalid row rollback "+which);
            if(which!=6)check(b.calls==0,"preflight precedes provider");
        }
        Storage untouched=new Storage();PostNeuralStyleSink identity=adapter(new Provider(),untouched);fails(()->identity.begin(W,H,new Object()),"wrong object identity");check(untouched.begins==0,"identity before staging");identity.abort();check(untouched.aborts==0,"no staging cleanup needed");
        fails(()->new PostNeuralStyleSink(FRAME,"actual",43,W,H,Style.NATURAL_BLUSH,Domain.ENCODED_SDR_FULL_RANGE,null,PostNeuralStyleSink.Budget.standard(),new Provider(),new Storage()),"missing graph policy");
        fails(()->new PostNeuralStyleSink(FRAME,"actual",43,W,H,Style.NATURAL_BLUSH,Domain.ENCODED_SDR_FULL_RANGE,PostNeuralStyleSink.GraphPolicy.DECLARED_OUTER_ORDER_WITH_CALLER_RESOLVED_SOURCES_AND_SUBORDER,new PostNeuralStyleSink.Budget(8,1000,1000,W*3*36-1),new Provider(),new Storage()),"one row budget");
        check(sink.arrayBytesForPixels(1)==108,"natural allocationvolume");
        PostNeuralStyleSink purity=new PostNeuralStyleSink(FRAME,"actual",43,W,H,Style.PURITY2,Domain.ENCODED_SDR_FULL_RANGE,PostNeuralStyleSink.GraphPolicy.DECLARED_OUTER_ORDER_WITH_CALLER_RESOLVED_SOURCES_AND_SUBORDER,PostNeuralStyleSink.Budget.standard(),new Provider(),new Storage());check(purity.arrayBytesForPixels(1)==324,"purity allocationvolume");
        System.out.println("{\"status\":\"PASS\",\"independent_assertions\":"+checks+",\"frame_width\":7,\"frame_height\":10,\"rows_split\":[3,3,2,2],\"input_output_float_bits_analytically_verified\":210}");
    }
}
