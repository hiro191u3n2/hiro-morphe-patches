package com.hiro.ulike.hdr.input;

import com.hiro.ulike.hdr.color.*;
import com.hiro.ulike.hdr.beauty.*;
import com.hiro.ulike.style.SampledMakeupPipeline.*;
import com.hiro.ulike.style.StyleLutPipeline;
import hiro.ulike.beauty.*;
import hiro.ulike.model.PinnedModel;
import java.io.*;
import java.nio.*;
import java.security.MessageDigest;
import java.util.*;

/** Author integration: actual pinned models/masks, synthetic captured codes and resolved geometry.
 * This is not a real portrait, camera execution, geometry resolver or native HDR style oracle.
 */
public final class HdrBeautyIntegration {
    static long checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static HdrFrame fixture(int w,int h){
        short[][] p={new short[w*h],new short[w*h/4],new short[w*h/4]};
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[0][y*w+x]=(short)(160+(x*3+y*7)%780);
        for(int i=0;i<p[1].length;i++){p[1][i]=(short)(504+i%17);p[2][i]=(short)(504+(i*3)%17);}
        Object id=new Object();CaptureMatch.Context c=new CaptureMatch.Context(id,id,id,id,id,21,"0",null);
        CaptureMatch.Result r=new CaptureMatch.Result(c,id,1234567,123,null,"5",10000000L,200);
        return new HdrFrame(w,h,HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED,c,r,p,(long)w*h*3);
    }
    public static final class Fixture {
        public final HdrFrame frame;
        public final P010SceneSource hdr;
        public final SdrRendition rendition;
        public final PreparedNeuralLayer layer;
        public final HdrBeautyProcessor.Snapshot snapshot;
        private Fixture(HdrFrame f,P010SceneSource h,SdrRendition r,PreparedNeuralLayer l,String geometry){
            frame=f;hdr=h;rendition=r;layer=l;
            snapshot=HdrBeautyProcessor.Snapshot.create(r,l,HdrAppearance.Policy.standard(),"author-synthetic-still",geometry,
                    new byte[]{1,3,5,7},HdrBeautyProcessor.GraphPolicy.DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS);
        }
    }
    /** Executes the real model once and closes ORT; prepared layer remains owned and usable. */
    public static Fixture prepare(PinnedModel.Style style,File model,File bytenn,File effect,File zip,String geometry)throws Exception{
        return prepareFromFrame(style,model,bytenn,effect,zip,fixture(512,384),geometry);
    }
    /** Test-only caller-supplied owned frame, to distinguish smooth codec fixtures from hard rejection fixtures. */
    public static Fixture prepareFromFrame(PinnedModel.Style style,File model,File bytenn,File effect,File zip,HdrFrame f,String geometry)throws Exception{
        P010SceneSource h=new P010SceneSource(f,P010SceneSource.ChromaLocation.COSITED,P010SceneSource.ChromaLocation.COSITED,"author-synthetic-explicit-cosited");
        SdrRendition r=new SdrRendition(h,1);PinnedModel.CompiledModel compiled;
        try(InputStream m=new FileInputStream(model);InputStream b=new FileInputStream(bytenn);InputStream e=new FileInputStream(effect)){compiled=PinnedModel.compile(style,m,b,e);}
        PinnedAssets.NeuralMask mask=PinnedAssets.loadMask(zip,style,false);PreparedNeuralLayer layer;
        try(BeautyImageEngine engine=BeautyImageEngine.openHostForVerification(compiled)){
            layer=engine.prepareLayer(r,new double[]{.86,.07,-51,-.07,.86,-36,0,0,1},mask,.7,BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard());
        }
        return new Fixture(f,h,r,layer,geometry);
    }
    public static HdrBeautyProcessor.BindingProvider bindings(final boolean active){
        final StyleLutPipeline.Texture bg=new StyleLutPipeline.Texture(1,1,new double[]{.52,.48,.42});
        final StyleLutPipeline.Texture skin=new StyleLutPipeline.Texture(1,1,new double[]{.62,.52,.46});
        return (snapshot,tile)->{
            int n=tile.pixels();List<HdrBeautyProcessor.HdrPass> passes=new ArrayList<>();
            for(Pass p:Pass.values())if(p.style==snapshot.style){
                double[] rgba=new double[n*4],cov=new double[n],seg=null,base=null;boolean[] inside=null;
                for(int i=0;i<n;i++){rgba[4*i]=.36;rgba[4*i+1]=.28;rgba[4*i+2]=.24;rgba[4*i+3]=.6;cov[i]=active?.25:0;}
                boolean usesSeg=p==Pass.PURITY_LIPS||p==Pass.PURITY_EYE_MULTIPLY||p==Pass.PURITY_EYE_SCREEN||p==Pass.PURITY_EYELASH||p==Pass.PURITY_SHADOW_MULTIPLY||p==Pass.PURITY_SHADOW_SCREEN;
                if(usesSeg){seg=new double[n];Arrays.fill(seg,.8);inside=new boolean[n];Arrays.fill(inside,true);}
                HdrBeautyProcessor.HdrBase bound=null;
                if(p==Pass.PURITY_BLUSHER||p==Pass.PURITY_3D){base=new double[n*3];for(int i=0;i<n;i++){base[3*i]=.7;base[3*i+1]=.6;base[3*i+2]=.5;}
                    bound=new HdrBeautyProcessor.HdrBase(snapshot.frameIdentity,tile,snapshot.geometryId,"author-synthetic-explicit-constant-hdr-shader-source",base);}
                ResolvedPass params=new ResolvedPass(p,tile,rgba,cov,.4,.8,null,seg,inside,null);
                passes.add(new HdrBeautyProcessor.HdrPass(params,bound));
            }
            double[] mask=new double[n];Arrays.fill(mask,.6);
            StyleLutPipeline.ResolvedLuts luts=new StyleLutPipeline.ResolvedLuts(tile,bg,skin,mask,active?.2:0,snapshot.style==Style.PURITY2?bg:null,active&&snapshot.style==Style.PURITY2?.1:0);
            return new HdrBeautyProcessor.ResolvedTile(snapshot.frameIdentity,snapshot.settingsSha256,snapshot.geometryId,passes,luts);
        };
    }
    static final class Sink implements HdrBeautyProcessor.PairSink {
        final HdrBeautyProcessor.Snapshot expected;double[] sdr,hdr;int rows,begins,aborts;boolean committed;String failure="";
        Sink(HdrBeautyProcessor.Snapshot s){expected=s;}
        public void begin(Object id,int w,int h,String settings,String geometry,double headroom){begins++;check(id==expected.frameIdentity&&w==expected.width&&h==expected.height&&settings.equals(expected.settingsSha256)&&geometry.equals(expected.geometryId)&&headroom==expected.policy.headroom(),"exact pair identity");sdr=new double[w*h*3];hdr=new double[sdr.length];if(failure.equals("begin"))throw new IllegalStateException("begin");}
        public void writeRows(int first,int count,double[] s,double[] h){check(first==rows&&count>0&&s.length==expected.width*count*3&&h.length==s.length,"same-size sequential pair");System.arraycopy(s,0,sdr,first*expected.width*3,s.length);System.arraycopy(h,0,hdr,first*expected.width*3,h.length);rows+=count;if(failure.equals("write"))throw new IllegalStateException("write");}
        public void commit(){check(rows==expected.height,"complete pair before seal");if(failure.equals("commit"))throw new IllegalStateException("commit");committed=true;}
        public void abort(){aborts++;sdr=hdr=null;committed=false;}
    }
    static String hash(HdrFrame f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");for(HdrFrame.Component c:HdrFrame.Component.values()){ShortBuffer p=f.samples(c);while(p.hasRemaining()){int v=p.get();md.update((byte)v);md.update((byte)(v>>>8));}}return hex(md.digest());}
    static String hex(byte[] digest){StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format("%02x",b&255));return s.toString();}
    public static void main(String[] args)throws Exception{
        Fixture f=prepare(PinnedModel.Style.valueOf(args[0]),new File(args[1]),new File(args[2]),new File(args[3]),new File(args[4]),"identity-native-raster");String before=hash(f.frame);
        Sink plain=new Sink(f.snapshot);HdrBeautyProcessor.Result result=HdrBeautyProcessor.render(f.snapshot,bindings(false),plain,HdrBeautyProcessor.Budget.standard());
        check(plain.committed&&plain.aborts==0,"private pair sealed");check(!result.wholeCapturedHdrInformationPreserved&&!result.nativeHdrStyleEquivalent&&!result.androidDeviceVerified,"honest result scope");
        double[] scene=new double[3],expected=new double[3],patch=new double[3],layer=new double[4],sdr=new double[3];long unchanged=0,edited=0,hdrHeadroom=0;
        for(int y=0;y<f.frame.height;y++)for(int x=0;x<f.frame.width;x++){
            int off=(y*f.frame.width+x)*3;f.hdr.readPixel(x,y,scene);HdrAppearance.sceneToDisplay(scene[0],scene[1],scene[2],f.snapshot.policy,expected);f.layer.sampleAt(x,y,layer);
            if(layer[3]>0){HdrAppearance.generatedPatch(layer[0],layer[1],layer[2],f.rendition.exposureScale,f.snapshot.policy,patch);for(int c=0;c<3;c++)expected[c]=layer[3]==1?patch[c]:expected[c]*(1-layer[3])+patch[c]*layer[3];edited++;}else unchanged++;
            HdrAppearance.deriveSdr(expected[0],expected[1],expected[2],sdr);
            for(int c=0;c<3;c++){check(Double.doubleToRawLongBits(expected[c])==Double.doubleToRawLongBits(plain.hdr[off+c]),"actual neural layer enters processed HDR");check(Double.doubleToRawLongBits(sdr[c])==Double.doubleToRawLongBits(plain.sdr[off+c]),"SDR derives from processed HDR");if(plain.hdr[off+c]>1)hdrHeadroom++;}
        }
        check(edited>100&&unchanged>100&&hdrHeadroom>100,"actual effect/outside region/HDR headroom");
        Sink active=new Sink(f.snapshot);HdrBeautyProcessor.render(f.snapshot,bindings(true),active,HdrBeautyProcessor.Budget.standard());
        long changed=0;for(int i=0;i<active.hdr.length;i++)if(active.hdr[i]!=plain.hdr[i])changed++;
        check(changed>100,"remaining actual style equations change HDR");
        Sink tiled=new Sink(f.snapshot);HdrBeautyProcessor.render(f.snapshot,bindings(true),tiled,new HdrBeautyProcessor.Budget(1,512,25000000,32*1024*1024));check(Arrays.equals(active.hdr,tiled.hdr)&&Arrays.equals(active.sdr,tiled.sdr),"tile-row invariance");
        for(String failure:Arrays.asList("begin","write","commit")){Sink fail=new Sink(f.snapshot);fail.failure=failure;try{HdrBeautyProcessor.render(f.snapshot,bindings(false),fail,HdrBeautyProcessor.Budget.standard());throw new AssertionError("failure accepted");}catch(IllegalStateException expectedFailure){check(fail.aborts==1&&!fail.committed&&fail.hdr==null,"pair rollback "+failure);}}
        Sink fail=new Sink(f.snapshot);HdrBeautyProcessor.BindingProvider good=bindings(false);
        try{HdrBeautyProcessor.render(f.snapshot,(s,t)->{HdrBeautyProcessor.ResolvedTile r=good.resolve(s,t);return t.y==0?r:new HdrBeautyProcessor.ResolvedTile(r.frameIdentity,"foreign-settings",r.geometryId,r.makeup,r.luts);},fail,HdrBeautyProcessor.Budget.standard());throw new AssertionError("foreign late settings accepted");}catch(IllegalArgumentException expectedFailure){check(fail.rows>0&&fail.aborts==1&&!fail.committed,"late binding rollback");}
        check(before.equals(hash(f.frame)),"captured P010 remained bit-identical");
        byte[] exposed=f.snapshot.settingsSnapshotBytes();exposed[0]^=7;check(!Arrays.equals(exposed,f.snapshot.settingsSnapshotBytes()),"settings defensive copy");
        Sink budget=new Sink(f.snapshot);try{HdrBeautyProcessor.render(f.snapshot,bindings(false),budget,new HdrBeautyProcessor.Budget(1,512,25000000,1));throw new AssertionError("bad budget accepted");}catch(IllegalArgumentException expectedFailure){check(budget.begins==0,"budget fails before sink begin");}
        System.out.println("{\"style\":\""+f.snapshot.style+"\",\"checks\":"+checks+",\"actual_neural_edited_pixels\":"+edited+",\"unmodified_neural_region_pixels\":"+unchanged+",\"hdr_headroom_channels\":"+hdrHeadroom+",\"post_style_changed_channels\":"+changed+",\"status\":\"PASS\"}");
    }
}
