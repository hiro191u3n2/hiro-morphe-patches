package hiro.ulike.beauty;

import com.hiro.ulike.hdr.beauty.*;
import com.hiro.ulike.hdr.color.*;
import com.hiro.ulike.hdr.input.*;
import com.hiro.ulike.style.SampledMakeupPipeline.*;
import com.hiro.ulike.style.StyleLutPipeline;
import hiro.ulike.model.PinnedModel;
import java.lang.reflect.Constructor;
import java.nio.*;
import java.util.*;

/** Synthetic owned P010 frames and generated layers; no vendor model/assets embedded. */
public final class ProcessorReview {
    static long checks;
    static void check(boolean b,String reason){checks++;if(!b)throw new AssertionError(reason);}
    interface Throwing {void run()throws Exception;}
    static void fails(Throwing action,String reason)throws Exception{
        boolean failed=false;try{action.run();}catch(Exception expected){failed=true;}
        check(failed,"accepted "+reason);
    }
    static void near(double a,double b,String reason){check(Double.isFinite(a)&&Math.abs(a-b)<=2e-12*(1+Math.abs(b)),reason+": "+a+" != "+b);}
    static HdrFrame frame(int grey,int cb,int cr)throws Exception{
        int w=4,h=4;Object owner=new Object(),session=new Object(),reader=new Object(),request=new Object(),callback=new Object();
        CaptureMatch.Context context=new CaptureMatch.Context(owner,session,reader,request,callback,3,"0",null);
        CaptureMatch.Source source=new CaptureMatch.Source(owner,session,reader,"0",null,CaptureMatch.TimestampConvention.SENSOR_START_OF_EXPOSURE);
        CaptureMatch.Result result=new CaptureMatch.Result(context,request,101,7,null,"5",1000L,200);
        ByteBuffer[] bytes=new ByteBuffer[3];P010FrameReader.Plane[] planes=new P010FrameReader.Plane[3];
        for(int c=0;c<3;c++){
            int pw=c==0?w:w/2,ph=c==0?h:h/2,stride=c==0?2:4;
            bytes[c]=ByteBuffer.allocate(pw*ph*stride).order(ByteOrder.LITTLE_ENDIAN);
            for(int i=0;i<pw*ph;i++)bytes[c].putShort(i*stride,(short)((c==0?grey:c==1?cb:cr)<<6));
            planes[c]=new P010FrameReader.Plane(bytes[c],pw*stride,stride);
        }
        HdrFrame.Encoding encoding=HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED;
        P010FrameReader.Input input=new P010FrameReader.Input(54,w,h,0,0,w,h,101,encoding,source,planes);
        HdrFrame resultFrame=P010FrameReader.copy(input,new P010FrameReader.Request(w,h,encoding,48),context,result,()->context);
        for(ByteBuffer b:bytes)for(int i=0;i<b.capacity();i++)b.put(i,(byte)0);
        return resultFrame;
    }
    static SdrRendition rendition(HdrFrame frame){return new SdrRendition(new P010SceneSource(frame,P010SceneSource.ChromaLocation.COSITED,P010SceneSource.ChromaLocation.COSITED,"synthetic-review"),1);}
    static PinnedAssets.NeuralMask mask(PinnedModel.Style style)throws Exception{
        Constructor<PinnedAssets.NeuralMask> constructor=PinnedAssets.NeuralMask.class.getDeclaredConstructor(PinnedModel.Style.class,boolean.class,byte[].class);
        constructor.setAccessible(true);byte[] bytes=new byte[320*320];Arrays.fill(bytes,(byte)255);
        return constructor.newInstance(style,false,bytes);
    }
    static PreparedNeuralLayer layer(SdrRendition rendition,PinnedModel.Style style,double intensity)throws Exception{
        double[] rgba=new double[256*256*4];
        for(int i=0;i<256*256;i++){
            rgba[4*i]=rgba[4*i+1]=rgba[4*i+2]=.5;
            int x=i%256;rgba[4*i+3]=x==0?0:x==2?.5:1;
        }
        double[] matrix={1,0,0,0,1,0,0,0,1};
        PreparedNeuralLayer layer=new PreparedNeuralLayer(style,4,4,rendition.frameIdentity(),rendition,rgba,matrix,mask(style),intensity);
        // Neither caller-owned tensor nor transform can mutate the prepared layer.
        Arrays.fill(rgba,0);Arrays.fill(matrix,0);
        return layer;
    }
    static HdrBeautyProcessor.Snapshot snapshot(SdrRendition source,PinnedModel.Style style,double intensity)throws Exception{
        return HdrBeautyProcessor.Snapshot.create(source,layer(source,style,intensity),HdrAppearance.Policy.standard(),"capture","geometry-4x4",new byte[]{1,2,3},
                HdrBeautyProcessor.GraphPolicy.DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS);
    }
    static class Sink implements HdrBeautyProcessor.PairSink {
        int begins,writes,commits,aborts,next;double[] sdr,hdr;final int fail;
        Sink(){this(0);}Sink(int fail){this.fail=fail;}
        public void begin(Object source,int w,int h,String settings,String geometry,double headroom)throws Exception{
            begins++;check(w==4&&h==4&&source instanceof HdrFrame,"same raster/source");
            check(settings.length()==64&&geometry.equals("geometry-4x4"),"bound pair identity");
            if(fail==1)throw new java.io.IOException("begin failure");sdr=new double[w*h*3];hdr=new double[w*h*3];
        }
        public void writeRows(int first,int rows,double[] sr,double[] hr)throws Exception{
            check(first==next&&rows>0&&sr.length==rows*12&&hr.length==rows*12,"contiguous exact pair rows");
            writes++;if(fail==2)throw new java.io.IOException("write failure");
            System.arraycopy(sr,0,sdr,first*12,sr.length);System.arraycopy(hr,0,hdr,first*12,hr.length);next+=rows;
        }
        public void commit()throws Exception{check(next==4,"complete pair before commit");if(fail==3)throw new java.io.IOException("commit failure");commits++;}
        public void abort(){aborts++;sdr=hdr=null;}
    }
    static final StyleLutPipeline.Texture LUT=new StyleLutPipeline.Texture(1,1,new double[]{.25,.5,.75});
    static HdrBeautyProcessor.ResolvedTile identityBindings(HdrBeautyProcessor.Snapshot s,FrameTile tile){
        int n=tile.pixels();double[] rgba=new double[n*4],coverage=new double[n],seg=new double[n],base=new double[n*3];boolean[] inside=new boolean[n];
        Arrays.fill(seg,1);Arrays.fill(inside,true);Arrays.fill(base,.75);
        for(int i=0;i<n;i++){rgba[i*4]=rgba[i*4+1]=rgba[i*4+2]=.5;rgba[i*4+3]=1;}
        List<HdrBeautyProcessor.HdrPass> passes=new ArrayList<>();
        for(Pass p:Pass.values())if(p.style==s.style){
            boolean segmentation=p==Pass.PURITY_LIPS||p==Pass.PURITY_EYE_MULTIPLY||p==Pass.PURITY_EYE_SCREEN||p==Pass.PURITY_EYELASH||p==Pass.PURITY_SHADOW_MULTIPLY||p==Pass.PURITY_SHADOW_SCREEN;
            boolean external=p==Pass.PURITY_BLUSHER||p==Pass.PURITY_3D;
            ResolvedPass parameters=new ResolvedPass(p,tile,rgba,coverage,0,1,null,segmentation?seg:null,segmentation?inside:null);
            HdrBeautyProcessor.HdrBase source=external?new HdrBeautyProcessor.HdrBase(s.frameIdentity,tile,s.geometryId,"synthetic explicit shader base",base):null;
            passes.add(new HdrBeautyProcessor.HdrPass(parameters,source));
        }
        StyleLutPipeline.ResolvedLuts luts=new StyleLutPipeline.ResolvedLuts(tile,LUT,LUT,new double[n],0,s.style==Style.PURITY2?LUT:null,0);
        return new HdrBeautyProcessor.ResolvedTile(s.frameIdentity,s.settingsSha256,s.geometryId,passes,luts);
    }
    static Sink render(HdrBeautyProcessor.Snapshot snapshot,int rows)throws Exception{
        Sink sink=new Sink();HdrBeautyProcessor.Result result=HdrBeautyProcessor.render(snapshot,ProcessorReview::identityBindings,sink,new HdrBeautyProcessor.Budget(rows,262144,25000000,33554432));
        check(sink.begins==1&&sink.commits==1&&sink.aborts==0,"successful private pair transaction");
        check(!result.wholeCapturedHdrInformationPreserved&&!result.nativeHdrStyleEquivalent&&!result.androidDeviceVerified,"truthful HDR claims");
        return sink;
    }
    static void layerAndRasterTests()throws Exception{
        for(PinnedModel.Style style:PinnedModel.Style.values()){
            SdrRendition bright=rendition(frame(940,512,512)),dark=rendition(frame(500,512,512));
            HdrBeautyProcessor.Snapshot a=snapshot(bright,style,1),b=snapshot(dark,style,1);
            Sink high=render(a,3),low=render(b,1);double[] scene=new double[3],original=new double[3],patch=new double[3];
            HdrAppearance.generatedPatch(.5,.5,.5,1,a.policy,patch);
            bright.hdrSource().readPixel(0,0,scene);HdrAppearance.sceneToDisplay(scene[0],scene[1],scene[2],a.policy,original);
            for(int y=0;y<4;y++)for(int x=0;x<4;x++)for(int c=0;c<3;c++){
                int off=(y*4+x)*3+c;
                if(x==0)check(Double.doubleToRawLongBits(high.hdr[off])==Double.doubleToRawLongBits(original[c]),"weight zero exact HDR identity");
                else if(x==2)near(high.hdr[off],(original[c]+patch[c])*.5,"half-alpha linear HDR blend");
                else {check(Double.doubleToRawLongBits(high.hdr[off])==Double.doubleToRawLongBits(low.hdr[off]),"opaque patch independent of original luminance");near(high.hdr[off],patch[c],"opaque generated HDR patch");}
                double max=Math.max(1,Math.max(high.hdr[(y*4+x)*3],Math.max(high.hdr[(y*4+x)*3+1],high.hdr[(y*4+x)*3+2])));
                near(high.sdr[off],high.hdr[off]/max,"SDR derived after processed HDR");
            }
            Sink tiled=render(a,4);for(int i=0;i<high.hdr.length;i++)check(Double.doubleToRawLongBits(high.hdr[i])==Double.doubleToRawLongBits(tiled.hdr[i]),"tile-height invariance");
            for(HdrFrame.Component component:HdrFrame.Component.values()){
                ShortBuffer plane=((HdrFrame)a.frameIdentity).samples(component);while(plane.hasRemaining())check((plane.get()&65535)==(component==HdrFrame.Component.Y?940:512),"captured P010 unchanged");
            }
            HdrBeautyProcessor.Snapshot zero=snapshot(bright,style,0);
            check(!zero.settingsSha256.equals(a.settingsSha256),"actual neural intensity bound into settings digest");
            byte[] canonical=a.settingsSnapshotBytes();canonical[0]^=127;check(a.settingsSnapshotBytes()[0]!=canonical[0],"settings owned copy");
            PreparedNeuralLayer layer=layer(bright,style,1);
            fails(()->HdrBeautyProcessor.Snapshot.create(new SdrRendition(bright.hdrSource(),1),layer,a.policy,"capture","geometry-4x4",new byte[]{1},HdrBeautyProcessor.GraphPolicy.DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS),"foreign rendition object");
            fails(()->HdrBeautyProcessor.Snapshot.create(dark,layer,a.policy,"capture","geometry-4x4",new byte[]{1},HdrBeautyProcessor.GraphPolicy.DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS),"foreign frame");
        }
    }
    static void transactionAndBindingTests()throws Exception{
        HdrBeautyProcessor.Snapshot s=snapshot(rendition(frame(721,512,512)),PinnedModel.Style.NATURAL_BLUSH,1);
        for(int fail=1;fail<=3;fail++){
            Sink sink=new Sink(fail);fails(()->HdrBeautyProcessor.render(s,ProcessorReview::identityBindings,sink,HdrBeautyProcessor.Budget.standard()),"sink failure");
            check(sink.begins==1&&sink.aborts==1&&sink.commits==0&&sink.hdr==null,"failed pair transaction discards staging");
        }
        for(int mode=0;mode<5;mode++){
            final int fault=mode;Sink sink=new Sink();
            fails(()->HdrBeautyProcessor.render(s,(snapshot,tile)->{
                HdrBeautyProcessor.ResolvedTile r=identityBindings(snapshot,tile);
                if(fault==0)return new HdrBeautyProcessor.ResolvedTile(new Object(),r.settingsSha256,r.geometryId,r.makeup,r.luts);
                if(fault==1)return new HdrBeautyProcessor.ResolvedTile(r.frameIdentity,"wrong",r.geometryId,r.makeup,r.luts);
                if(fault==2)return new HdrBeautyProcessor.ResolvedTile(r.frameIdentity,r.settingsSha256,"wrong",r.makeup,r.luts);
                if(fault==3)return new HdrBeautyProcessor.ResolvedTile(r.frameIdentity,r.settingsSha256,r.geometryId,Collections.emptyList(),r.luts);
                return new HdrBeautyProcessor.ResolvedTile(r.frameIdentity,r.settingsSha256,r.geometryId,Arrays.asList(r.makeup.get(0),r.makeup.get(0)),r.luts);
            },sink,HdrBeautyProcessor.Budget.standard()),"wrong complete-stage binding");
            check(sink.aborts==1&&sink.commits==0,"binding failure never commits pair");
        }
        Sink small=new Sink();fails(()->HdrBeautyProcessor.render(s,ProcessorReview::identityBindings,small,new HdrBeautyProcessor.Budget(1,1,16,1000000)),"tile cannot hold one row");
        check(small.begins==0,"invalid budget fails before transaction");
    }
    static void postNeuralHdrTests()throws Exception{
        // BT2020 green extends outside sRGB. An arbitrarily weak edit must not
        // trigger full gamut replacement of the pre-edit pixel.
        SdrRendition green=rendition(frame(509,270,203));
        HdrBeautyProcessor.Snapshot natural=snapshot(green,PinnedModel.Style.NATURAL_BLUSH,0);
        Sink untouched=render(natural,4);
        for(double intensity:new double[]{1e-12,1e-6,.25,1}){
            Sink edited=new Sink();
            HdrBeautyProcessor.render(natural,(s,tile)->{
                HdrBeautyProcessor.ResolvedTile identity=identityBindings(s,tile);
                StyleLutPipeline.ResolvedLuts changed=new StyleLutPipeline.ResolvedLuts(tile,LUT,LUT,new double[tile.pixels()],intensity,null,0);
                return new HdrBeautyProcessor.ResolvedTile(s.frameIdentity,s.settingsSha256,s.geometryId,identity.makeup,changed);
            },edited,HdrBeautyProcessor.Budget.standard());
            double[] work=new double[3],fragment=new double[3];
            HdrAppearance.workingSrgb(untouched.hdr[0],untouched.hdr[1],untouched.hdr[2],work);
            double m=Math.max(1,Math.max(work[0],Math.max(work[1],work[2])));
            HdrAppearance.srgbToBt2020(HdrAppearance.decodeSrgb(.25)*m,HdrAppearance.decodeSrgb(.5)*m,HdrAppearance.decodeSrgb(.75)*m,fragment);
            for(int i=0;i<edited.hdr.length;i++)near(edited.hdr[i],untouched.hdr[i]*(1-intensity)+fragment[i%3]*intensity,"LUT HDR alpha is continuous outside sRGB");
        }
        // Purity 3D's zero intensity still writes the explicit u_basic source,
        // including its wide gamut; it must not preserve the different current
        // pixel or needlessly substitute the gamut-mapped SDR proxy of its base.
        HdrBeautyProcessor.Snapshot purity=snapshot(rendition(frame(721,512,512)),PinnedModel.Style.PURITY2,0);
        Sink baseWrite=new Sink();
        HdrBeautyProcessor.render(purity,(s,tile)->{
            HdrBeautyProcessor.ResolvedTile identity=identityBindings(s,tile);
            List<HdrBeautyProcessor.HdrPass> changed=new ArrayList<>(identity.makeup);
            for(int i=0;i<changed.size();i++)if(changed.get(i).parameters.pass==Pass.PURITY_3D){
                HdrBeautyProcessor.HdrPass p=changed.get(i);double[] coverage=new double[tile.pixels()];Arrays.fill(coverage,1);
                ResolvedPass params=new ResolvedPass(Pass.PURITY_3D,tile,p.parameters.rgba,coverage,0,1,null,null,null);
                double[] hdrBase=new double[tile.pixels()*3];for(int j=0;j<tile.pixels();j++)hdrBase[j*3+1]=1;
                HdrBeautyProcessor.HdrBase wideBase=new HdrBeautyProcessor.HdrBase(s.frameIdentity,tile,s.geometryId,"synthetic wide-gamut u_basic",hdrBase);
                changed.set(i,new HdrBeautyProcessor.HdrPass(params,wideBase));
            }
            return new HdrBeautyProcessor.ResolvedTile(s.frameIdentity,s.settingsSha256,s.geometryId,changed,identity.luts);
        },baseWrite,HdrBeautyProcessor.Budget.standard());
        for(int i=0;i<baseWrite.hdr.length;i++)check(Double.doubleToRawLongBits(baseWrite.hdr[i])==Double.doubleToRawLongBits(i%3==1?1.0:0.0),"Purity 3D exact wide-gamut HDR base at zero intensity");
        for(int mode=0;mode<4;mode++){
            final int fault=mode;Sink sink=new Sink();
            fails(()->HdrBeautyProcessor.render(purity,(s,tile)->{
                HdrBeautyProcessor.ResolvedTile r=identityBindings(s,tile);List<HdrBeautyProcessor.HdrPass> changed=new ArrayList<>(r.makeup);
                for(int i=0;i<changed.size();i++)if(changed.get(i).parameters.pass==Pass.PURITY_BLUSHER){
                    HdrBeautyProcessor.HdrPass p=changed.get(i);HdrBeautyProcessor.HdrBase old=p.shaderBase;
                    HdrBeautyProcessor.HdrBase wrong=fault==0?null:new HdrBeautyProcessor.HdrBase(fault==1?new Object():old.frameIdentity,old.tile,fault==2?"foreign geometry":old.geometryId,old.bindingEvidence,old.displayBt2020);
                    ResolvedPass parameters=p.parameters;
                    if(fault==3)parameters=new ResolvedPass(parameters.pass,tile,parameters.rgba,parameters.coverage,0,1,null,null,null,new double[tile.pixels()*3]);
                    changed.set(i,new HdrBeautyProcessor.HdrPass(parameters,wrong));
                }
                return new HdrBeautyProcessor.ResolvedTile(r.frameIdentity,r.settingsSha256,r.geometryId,changed,r.luts);
            },sink,HdrBeautyProcessor.Budget.standard()),"unmatched or SDR-only Purity base");
            check(sink.aborts==1&&sink.commits==0,"bad HDR base aborts pair transaction");
        }
    }
    public static void main(String[] args)throws Exception{
        layerAndRasterTests();transactionAndBindingTests();postNeuralHdrTests();
        System.out.println("{\"checks\":"+checks+",\"synthetic_only\":true,\"android_device_execution\":false}");
    }
}
