package hiro.ulike.beauty;

import com.hiro.ulike.hdr.beauty.HdrAppearance;
import com.hiro.ulike.hdr.beauty.HdrBeautyProcessor;
import com.hiro.ulike.hdr.beauty.HdrBeautyProcessor.*;
import com.hiro.ulike.hdr.color.SdrRendition;
import com.hiro.ulike.style.SampledMakeupPipeline.Style;
import com.hiro.ulike.style.StyleLutPipeline;
import hiro.ulike.model.PinnedModel;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.*;

/** Independent properties. Synthetic crops only; never native draw-order evidence. */
public final class OrderedReview {
    static int checks,cases;
    static double maxError;
    static final MathContext MC=new MathContext(55,RoundingMode.HALF_EVEN);
    static final byte[] SETTINGS={31,42,53};
    static final GraphPolicy GRAPH=GraphPolicy.DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    interface Attempt {void run()throws Exception;}
    static void rejects(Attempt a,String why)throws Exception {try{a.run();throw new AssertionError("accepted "+why);}catch(IllegalArgumentException expected){checks++;}}
    static BigDecimal exact(double d){return new BigDecimal(d,MC);}
    static PreparedNeuralLayer layer(SdrRendition r,Style style,double[] color,double alpha,int maskCode)throws Exception {
        PinnedModel.Style model= PinnedModel.Style.valueOf(style.name());
        Constructor<PinnedAssets.NeuralMask> c=PinnedAssets.NeuralMask.class.getDeclaredConstructor(PinnedModel.Style.class,boolean.class,byte[].class);
        c.setAccessible(true);byte[] mask=new byte[320*320];Arrays.fill(mask,(byte)maskCode);
        double[] crop=new double[256*256*4];for(int i=0;i<256*256;i++){for(int j=0;j<3;j++)crop[i*4+j]=color[j];crop[i*4+3]=alpha;}
        return new PreparedNeuralLayer(model,4,4,r.frameIdentity(),r,crop,new double[]{1,0,0,0,1,0,0,0,1},c.newInstance(model,false,mask),1);
    }
    static Snapshot snapshot(SdrRendition r,Style style,List<NeuralFace> faces) {
        List<String> ids=new ArrayList<>();for(NeuralFace f:faces)ids.add(f.faceId);
        ObservedNeuralOrder order=ObservedNeuralOrder.capture(r,style,"capture","geometry-4x4",SETTINGS,ids,"independent synthetic order only");
        return Snapshot.createOrderedFaces(r,order,faces,HdrAppearance.Policy.standard(),"capture","geometry-4x4",SETTINGS,GRAPH);
    }
    static ProcessorReview.Sink render(Snapshot s,int rows)throws Exception {
        ProcessorReview.Sink sink=new ProcessorReview.Sink();
        HdrBeautyProcessor.render(s,ProcessorReview::identityBindings,sink,new Budget(rows,16,16,1048576));
        check(sink.commits==1 && sink.aborts==0,"complete ordered transaction");return sink;
    }
    static void closedForm()throws Exception {
        Random random=new Random(0x65379a21);
        for(Style style:Style.values())for(int count:new int[]{0,1,2,3,8,16})for(int iteration=0;iteration<3;iteration++) {
            SdrRendition source=ProcessorReview.rendition(ProcessorReview.frame(470+iteration*170,510,516));
            List<NeuralFace> faces=new ArrayList<>();
            for(int i=0;i<count;i++){
                double[] color={.1+.75*random.nextDouble(),.1+.75*random.nextDouble(),.1+.75*random.nextDouble()};
                double alpha=iteration==0?random.nextDouble():iteration==1?(i==count-1?1:random.nextDouble()):(i%3==0?0:random.nextDouble());
                faces.add(new NeuralFace("observed-"+i,layer(source,style,color,alpha,iteration==2?173:255)));
            }
            Snapshot s=snapshot(source,style,faces);ProcessorReview.Sink actual=render(s,1),tiled=render(s,3);cases++;
            double[] scene=new double[3],display=new double[3],sample=new double[4],patch=new double[3];
            for(int y=0;y<4;y++)for(int x=0;x<4;x++) {
                source.hdrSource().readPixel(x,y,scene);HdrAppearance.sceneToDisplay(scene[0],scene[1],scene[2],s.policy,display);
                // Independent nonrecursive expansion: source * product(transparency)
                // plus each patch weighted by all later faces' transparencies.
                BigDecimal[] result={BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO};BigDecimal behind=BigDecimal.ONE;
                for(int f=faces.size()-1;f>=0;f--) {
                    faces.get(f).layer.sampleAt(x,y,sample);BigDecimal weight=exact(sample[3]);
                    if(sample[3]>0){HdrAppearance.generatedPatch(sample[0],sample[1],sample[2],source.exposureScale,s.policy,patch);
                        for(int c=0;c<3;c++)result[c]=result[c].add(exact(patch[c]).multiply(weight,MC).multiply(behind,MC),MC);}
                    behind=behind.multiply(BigDecimal.ONE.subtract(weight,MC),MC);
                }
                double[] expected=new double[3];for(int c=0;c<3;c++)expected[c]=result[c].add(exact(display[c]).multiply(behind,MC),MC).doubleValue();
                double divisor=Math.max(1,Math.max(expected[0],Math.max(expected[1],expected[2])));
                for(int c=0;c<3;c++){
                    int p=(y*4+x)*3+c;double error=Math.abs(actual.hdr[p]-expected[c]);maxError=Math.max(maxError,error);
                    check(error<8e-14*(1+Math.abs(expected[c])),"55-digit closed form source-over");
                    check(Math.abs(actual.sdr[p]-expected[c]/divisor)<8e-14,"SDR derived after complete overlap");
                    check(Double.doubleToRawLongBits(actual.hdr[p])==Double.doubleToRawLongBits(tiled.hdr[p]),"partial-last-tile invariance");
                }
            }
            if(count>0){
                Snapshot legacy=Snapshot.create(source,faces.get(0).layer,s.policy,"capture","geometry-4x4",SETTINGS,GRAPH);
                Snapshot first=snapshot(source,style,Collections.singletonList(faces.get(0)));
                check(Arrays.equals(render(legacy,3).hdr,render(first,3).hdr),"unchanged legacy one-face output");
            }
        }
    }
    static void zeroFaceAndRetention()throws Exception {
        for(Style style:Style.values()){
            SdrRendition source=ProcessorReview.rendition(ProcessorReview.frame(900,512,512));
            Snapshot zero=snapshot(source,style,Collections.emptyList());ProcessorReview.Sink plain=render(zero,2),lut=new ProcessorReview.Sink();
            HdrBeautyProcessor.render(zero,(s,t)->{ResolvedTile r=ProcessorReview.identityBindings(s,t);
                StyleLutPipeline.Texture fixed=new StyleLutPipeline.Texture(1,1,new double[]{.2,.3,.4});
                return new ResolvedTile(r.frameIdentity,r.settingsSha256,r.geometryId,r.makeup,
                    new StyleLutPipeline.ResolvedLuts(t,fixed,fixed,new double[t.pixels()],1,s.style==Style.PURITY2?fixed:null,s.style==Style.PURITY2?1:0));
            },lut,new Budget(4,16,16,1048576,0,0,0));
            check(lut.commits==1&&!Arrays.equals(lut.hdr,plain.hdr),"zero faces still executes complete global LUT graph");
            PreparedNeuralLayer one=layer(source,style,new double[]{.2,.3,.4},.7,255);
            long retained=0;for(Field f:PreparedNeuralLayer.class.getDeclaredFields()){
                f.setAccessible(true);Object value=f.get(one);if(value instanceof double[])retained+=((double[])value).length*8L;
                if(value instanceof PinnedAssets.NeuralMask)for(Field m:PinnedAssets.NeuralMask.class.getDeclaredFields()){m.setAccessible(true);Object bytes=m.get(value);if(bytes instanceof byte[])retained+=((byte[])bytes).length;}
            }
            check(retained==2199624L,"retained layer primitive arrays independently measured");
            check(retained<=HdrBeautyProcessor.RETAINED_NEURAL_ARRAY_BYTES_PER_FACE,"conservative retained-array budget");
            List<String> ids=new ArrayList<>();List<NeuralFace> faces=new ArrayList<>();for(int i=0;i<16;i++){ids.add("F"+i);faces.add(new NeuralFace("F"+i,layer(source,style,new double[]{.2,.3,.4},.7,255)));}
            ObservedNeuralOrder order=ObservedNeuralOrder.capture(source,style,"capture","geometry-4x4",SETTINGS,ids,"synthetic sixteen faces");
            HdrBeautyProcessor.preflightNeuralOrder(order,new Budget(4,16,16,1048576,16,35200000,256));
            rejects(()->HdrBeautyProcessor.preflightNeuralOrder(order,new Budget(4,16,16,1048576,16,35200000,255)),"cumulative sampling below 16*16");
            rejects(()->HdrBeautyProcessor.preflightNeuralOrder(order,new Budget(4,16,16,1048576,16,35199999,256)),"retained-array budget one byte short");
            Snapshot sixteen=Snapshot.createOrderedFaces(source,order,faces,zero.policy,"capture","geometry-4x4",SETTINGS,GRAPH);
            check(render(sixteen,4).commits==1,"hard maximum sixteen faces completes");
        }
    }
    public static void main(String[] args)throws Exception {
        closedForm();zeroFaceAndRetention();
        System.out.println("{\"checks\":"+checks+",\"closed_form_cases\":"+cases+",\"max_absolute_error\":"+maxError+",\"synthetic_only\":true,\"native_order_verified\":false}");
    }
}
