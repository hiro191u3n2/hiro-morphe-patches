package hiro.ulike.beauty;

import com.hiro.ulike.hdr.beauty.HdrAppearance;
import com.hiro.ulike.hdr.beauty.HdrBeautyProcessor;
import com.hiro.ulike.hdr.beauty.HdrBeautyProcessor.*;
import com.hiro.ulike.hdr.color.*;
import com.hiro.ulike.hdr.input.HdrBeautyIntegration;
import com.hiro.ulike.style.SampledMakeupPipeline.Style;
import hiro.ulike.model.PinnedModel;
import java.lang.reflect.Constructor;
import java.util.*;

/** Author regression of explicit caller-observed face ordering on tiny owned P010 frames.
 * Synthetic neural crops/masks and remaining bindings, not native ordering or device parity.
 */
public final class OrderedFacesTest {
    static long checks;
    static final byte[] SETTINGS={2,4,6};
    static final String CAPTURE="synthetic ordered still",GEOMETRY="synthetic 4x4";
    static final GraphPolicy GRAPH=GraphPolicy.DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    interface Run {void run()throws Exception;}
    static void rejects(Run run,String why)throws Exception{boolean rejected=false;try{run.run();}catch(IllegalArgumentException|UnsupportedOperationException expected){rejected=true;}check(rejected,"accepted "+why);}
    static SdrRendition source(){return new SdrRendition(new P010SceneSource(HdrBeautyIntegration.fixture(4,4),P010SceneSource.ChromaLocation.COSITED,P010SceneSource.ChromaLocation.COSITED,"synthetic explicit sample grid"),1);}
    static PreparedNeuralLayer layer(SdrRendition source,Style style,double red,double green,double blue,double alpha)throws Exception{
        PinnedModel.Style pinned=PinnedModel.Style.valueOf(style.name());
        Constructor<PinnedAssets.NeuralMask> constructor=PinnedAssets.NeuralMask.class.getDeclaredConstructor(PinnedModel.Style.class,boolean.class,byte[].class);
        constructor.setAccessible(true);byte[] mask=new byte[320*320];Arrays.fill(mask,(byte)255);
        PinnedAssets.NeuralMask ownedMask=constructor.newInstance(pinned,false,mask);
        double[] rgba=new double[256*256*4];for(int i=0;i<256*256;i++){rgba[i*4]=red;rgba[i*4+1]=green;rgba[i*4+2]=blue;rgba[i*4+3]=alpha;}
        double[] affine={1,0,0,0,1,0,0,0,1};
        PreparedNeuralLayer result=new PreparedNeuralLayer(pinned,source.width(),source.height(),source.frameIdentity(),source,rgba,affine,ownedMask,1);
        Arrays.fill(rgba,0);Arrays.fill(affine,0); // Public callers cannot retain the owned generated crop or affine.
        return result;
    }
    static ObservedNeuralOrder order(SdrRendition source,Style style,List<String> ids){return ObservedNeuralOrder.capture(source,style,CAPTURE,GEOMETRY,SETTINGS,ids,"synthetic explicitly observed native draw sequence");}
    static Snapshot snapshot(SdrRendition source,ObservedNeuralOrder order,List<NeuralFace> faces){return Snapshot.createOrderedFaces(source,order,faces,HdrAppearance.Policy.standard(),CAPTURE,GEOMETRY,SETTINGS,GRAPH);}
    static class Sink implements PairSink {
        int begins,writes,rows,aborts,commits;double[] hdr,sdr;final Snapshot snapshot;final String failure;
        Sink(Snapshot snapshot){this(snapshot,"");}Sink(Snapshot snapshot,String failure){this.snapshot=snapshot;this.failure=failure;}
        public void begin(Object source,int w,int h,String settings,String geometry,double headroom)throws Exception{
            begins++;check(source==snapshot.frameIdentity&&w==4&&h==4&&settings.equals(snapshot.settingsSha256)&&geometry.equals(GEOMETRY),"exact ordered pair transaction identity");
            hdr=new double[w*h*3];sdr=new double[hdr.length];if(failure.equals("begin"))throw new java.io.IOException("begin");
        }
        public void writeRows(int first,int count,double[] s,double[] h)throws Exception{
            check(first==rows&&s.length==count*12&&h.length==s.length,"contiguous ordered pair");writes++;
            System.arraycopy(s,0,sdr,first*12,s.length);System.arraycopy(h,0,hdr,first*12,h.length);rows+=count;
            if(failure.equals("write"))throw new java.io.IOException("write");
        }
        public void commit()throws Exception{check(rows==4,"complete rows before private seal");if(failure.equals("commit"))throw new java.io.IOException("commit");commits++;}
        public void abort()throws Exception{aborts++;hdr=sdr=null;if(failure.equals("abort"))throw new java.io.IOException("abort");}
    }
    static Sink render(Snapshot snapshot,boolean active,int rows)throws Exception{
        Sink sink=new Sink(snapshot);Result result=HdrBeautyProcessor.render(snapshot,HdrBeautyIntegration.bindings(active),sink,new Budget(rows,16,16,1000000));
        check(sink.commits==1&&sink.aborts==0&&!result.nativeHdrStyleEquivalent&&!result.androidDeviceVerified,"successful declared replacement only");return sink;
    }
    static void equalsBits(double[] expected,double[] actual,String why){check(expected.length==actual.length,why+" shape");for(int i=0;i<expected.length;i++)check(Double.doubleToRawLongBits(expected[i])==Double.doubleToRawLongBits(actual[i]),why+" channel "+i);}
    static void pixelOracle(SdrRendition source,List<NeuralFace> ordered,Snapshot snapshot,Sink sink){
        double[] scene=new double[3],display=new double[3],sample=new double[4],patch=new double[3],sdr=new double[3];
        for(int y=0;y<4;y++)for(int x=0;x<4;x++){
            source.hdrSource().readPixel(x,y,scene);HdrAppearance.sceneToDisplay(scene[0],scene[1],scene[2],snapshot.policy,display);
            for(NeuralFace face:ordered){face.layer.sampleAt(x,y,sample);HdrAppearance.generatedPatch(sample[0],sample[1],sample[2],source.exposureScale,snapshot.policy,patch);
                for(int c=0;c<3;c++)display[c]=sample[3]==1?patch[c]:display[c]*(1-sample[3])+patch[c]*sample[3];}
            HdrAppearance.deriveSdr(display[0],display[1],display[2],sdr);
            for(int c=0;c<3;c++){check(Double.doubleToRawLongBits(display[c])==Double.doubleToRawLongBits(sink.hdr[(y*4+x)*3+c]),"ordered linear-light source-over oracle");check(Double.doubleToRawLongBits(sdr[c])==Double.doubleToRawLongBits(sink.sdr[(y*4+x)*3+c]),"matching SDR derived after all faces");}
        }
    }
    static void facesAndEmpty()throws Exception{
        for(Style style:Style.values()){
            SdrRendition source=source();PreparedNeuralLayer a=layer(source,style,.2,.4,.6,.25),b=layer(source,style,.8,.3,.2,.5);
            List<NeuralFace> faces=Arrays.asList(new NeuralFace("face-A",a),new NeuralFace("face-B",b));
            Snapshot multi=snapshot(source,order(source,style,Arrays.asList("face-A","face-B")),faces);
            Sink composed=render(multi,false,1);pixelOracle(source,faces,multi,composed);
            equalsBits(composed.hdr,render(multi,false,3).hdr,"partial final row tile invariance");
            List<NeuralFace> reversed=Arrays.asList(faces.get(1),faces.get(0));
            Snapshot backwards=snapshot(source,order(source,style,Arrays.asList("face-B","face-A")),reversed);
            Sink back=render(backwards,false,4);pixelOracle(source,reversed,backwards,back);
            check(!Arrays.equals(composed.hdr,back.hdr),"overlapping order intentionally changes HDR");check(!multi.settingsSha256.equals(backwards.settingsSha256),"face order affects settings fingerprint");
            Snapshot single=snapshot(source,order(source,style,Collections.singletonList("face-A")),Collections.singletonList(faces.get(0)));
            Snapshot legacy=Snapshot.create(source,a,HdrAppearance.Policy.standard(),CAPTURE,GEOMETRY,SETTINGS,GRAPH);
            equalsBits(render(legacy,true,4).hdr,render(single,true,4).hdr,"legacy single-face exact HDR regression");
            equalsBits(render(legacy,true,4).sdr,render(single,true,4).sdr,"legacy single-face exact SDR regression");
            check(single.explicitObservedNeuralOrder&&!legacy.explicitObservedNeuralOrder&&legacy.neuralFaceCount==1,"explicit versus legacy observation scope");
            Snapshot empty=snapshot(source,order(source,style,Collections.emptyList()),Collections.emptyList());
            check(empty.neuralFaceCount==0&&empty.neuralFaces().isEmpty(),"zero faces explicit");
            Sink untouched=render(empty,false,2);pixelOracle(source,Collections.emptyList(),empty,untouched);
            check(!Arrays.equals(untouched.hdr,render(empty,true,2).hdr),"zero faces still executes all remaining style passes");
            Sink zeroBudget=new Sink(empty);HdrBeautyProcessor.render(empty,HdrBeautyIntegration.bindings(false),zeroBudget,new Budget(1,16,16,1000000,0,0,0));
            check(zeroBudget.commits==1,"true zero-face neural budget accepted");
            Sink missing=new Sink(empty);rejects(()->HdrBeautyProcessor.render(empty,(s,t)->{ResolvedTile valid=HdrBeautyIntegration.bindings(false).resolve(s,t);return new ResolvedTile(s.frameIdentity,s.settingsSha256,s.geometryId,Collections.emptyList(),valid.luts);},missing,Budget.standard()),"zero-face skipping required makeup");
            check(missing.aborts==1&&missing.commits==0,"zero-face incomplete style never saves");
        }
    }
    static void identityMutationAndBounds()throws Exception{
        SdrRendition source=source(),other=source();Style style=Style.NATURAL_BLUSH;
        PreparedNeuralLayer a=layer(source,style,.2,.3,.4,.5),b=layer(source,style,.6,.5,.4,.75);
        List<String> ids=new ArrayList<>(Arrays.asList("A","B"));ObservedNeuralOrder order=order(source,style,ids);ids.clear();
        ArrayList<NeuralFace> faces=new ArrayList<>(Arrays.asList(new NeuralFace("A",a),new NeuralFace("B",b)));Snapshot owned=snapshot(source,order,faces);faces.clear();
        check(order.faceIds().size()==2&&owned.neuralFaces().size()==2,"caller list mutation cannot change owned order or layers");
        rejects(()->order.faceIds().clear(),"mutation through observation getter");rejects(()->owned.neuralFaces().clear(),"mutation through snapshot getter");
        byte[] canonical=owned.settingsSnapshotBytes();canonical[0]^=255;check(canonical[0]!=owned.settingsSnapshotBytes()[0],"owned canonical settings");
        byte[] app={2,4,6};ObservedNeuralOrder appOwned=ObservedNeuralOrder.capture(source,style,CAPTURE,GEOMETRY,app,Arrays.asList("A","B"),"explicit synthetic observed order");app[0]=9;
        snapshot(source,appOwned,owned.neuralFaces());check(true,"app settings copied into order");
        rejects(()->Snapshot.createOrderedFaces(source,order,owned.neuralFaces(),owned.policy,CAPTURE,GEOMETRY,app,GRAPH),"changed app settings");
        rejects(()->Snapshot.createOrderedFaces(source,order,owned.neuralFaces(),owned.policy,"stale capture",GEOMETRY,SETTINGS,GRAPH),"stale capture ID");
        rejects(()->Snapshot.createOrderedFaces(source,order,owned.neuralFaces(),owned.policy,CAPTURE,"stale geometry",SETTINGS,GRAPH),"stale geometry ID");
        rejects(()->snapshot(other,order,owned.neuralFaces()),"stale frame observation");
        rejects(()->snapshot(new SdrRendition(source.hdrSource(),1),order,owned.neuralFaces()),"same source but foreign rendition");
        rejects(()->order(source,style,Arrays.asList("A","A")),"duplicate observed face IDs");
        rejects(()->snapshot(source,order,Collections.singletonList(new NeuralFace("A",a))),"omitted face");
        rejects(()->snapshot(source,order,Arrays.asList(new NeuralFace("B",b),new NeuralFace("A",a))),"unobserved reorder");
        rejects(()->snapshot(source,order,Arrays.asList(new NeuralFace("A",a),new NeuralFace("B",a))),"same prepared layer under multiple IDs");
        PreparedNeuralLayer foreign=layer(other,style,.5,.5,.5,1),wrongStyle=layer(source,Style.PURITY2,.5,.5,.5,1);
        rejects(()->snapshot(source,order,Arrays.asList(new NeuralFace("A",a),new NeuralFace("B",foreign))),"mixed captured sources");
        rejects(()->snapshot(source,order,Arrays.asList(new NeuralFace("A",a),new NeuralFace("B",wrongStyle))),"mixed styles");
        ArrayList<String> seventeen=new ArrayList<>();for(int i=0;i<17;i++)seventeen.add("F"+i);
        rejects(()->order(source,style,seventeen),"hard count 17");
        // A collection lying about size cannot force allocation of its oversized toArray result.
        List<String> sizeLiar=new AbstractList<String>(){public int size(){return 0;}public String get(int i){throw new AssertionError();}public Iterator<String> iterator(){return seventeen.iterator();}};
        rejects(()->order(source,style,sizeLiar),"oversize iterator despite small reported count");
        for(Budget budget:Arrays.asList(new Budget(1,16,16,1000000,1,4400000,32),new Budget(1,16,16,1000000,2,4399999,32),new Budget(1,16,16,1000000,2,4400000,31))){
            rejects(()->HdrBeautyProcessor.preflightNeuralOrder(order,budget),"pre-inference face retention/work budget");
            Sink sink=new Sink(owned);rejects(()->HdrBeautyProcessor.render(owned,HdrBeautyIntegration.bindings(false),sink,budget),"face count/retained bytes/work budget");check(sink.begins==0,"face budget fails before staging");
        }
        HdrBeautyProcessor.preflightNeuralOrder(order,new Budget(1,16,16,1000000,2,4400000,32));check(true,"exact pre-inference neural budget boundary succeeds");
        Sink exact=new Sink(owned);HdrBeautyProcessor.render(owned,HdrBeautyIntegration.bindings(false),exact,new Budget(1,16,16,1000000,2,4400000,32));check(exact.commits==1,"exact declared neural budget boundary succeeds");
        rejects(()->new Budget(1,16,16,1000000,17,4400000,32),"invalid budget hard count");
        rejects(()->new Budget(1,16,16,1000000,2,-1,32),"negative retained budget");
        rejects(()->new Budget(1,16,16,1000000,2,4400000,-1),"negative work budget");
        for(String failure:Arrays.asList("begin","write","commit")){
            Sink sink=new Sink(owned,failure);boolean failed=false;try{HdrBeautyProcessor.render(owned,HdrBeautyIntegration.bindings(false),sink,Budget.standard());}catch(java.io.IOException expected){failed=true;}
            check(failed&&sink.aborts==1&&sink.commits==0&&sink.hdr==null,"all face transaction rolls back "+failure);
        }
        Sink late=new Sink(owned);rejects(()->HdrBeautyProcessor.render(owned,(s,t)->{ResolvedTile r=HdrBeautyIntegration.bindings(false).resolve(s,t);return t.y==0?r:new ResolvedTile(r.frameIdentity,"stale settings",r.geometryId,r.makeup,r.luts);},late,new Budget(1,16,16,1000000)),"late mismatched bindings");
        check(late.writes==1&&late.aborts==1&&late.commits==0&&late.hdr==null,"late all-face failure discards partial pair");
        Sink cleanup=new Sink(owned,"abort");boolean retained=false;try{HdrBeautyProcessor.render(owned,(s,t)->{throw new java.io.IOException("primary");},cleanup,Budget.standard());}catch(java.io.IOException expected){retained=expected.getMessage().equals("primary")&&expected.getSuppressed().length==1;}
        check(retained&&cleanup.aborts==1&&cleanup.commits==0,"cleanup exception does not replace primary failure");
        Thread.currentThread().interrupt();Sink interrupted=new Sink(owned);boolean cancelled=false;try{HdrBeautyProcessor.render(owned,HdrBeautyIntegration.bindings(false),interrupted,Budget.standard());}catch(java.io.IOException expected){cancelled=true;}finally{Thread.interrupted();}
        check(cancelled&&interrupted.aborts==1&&interrupted.commits==0,"interruption cancels whole-face pair");
    }
    public static void main(String[] args)throws Exception{
        facesAndEmpty();identityMutationAndBounds();System.out.println("{\"checks\":"+checks+",\"styles\":2,\"synthetic_only\":true,\"native_observed_order_verified\":false,\"android_device_execution\":false}");
    }
}
