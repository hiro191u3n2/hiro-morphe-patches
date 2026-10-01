package com.hiro.ulike.hdr.beauty;

import com.hiro.ulike.hdr.color.P010SceneSource;
import com.hiro.ulike.hdr.color.SdrRendition;
import com.hiro.ulike.style.SampledMakeupPipeline;
import com.hiro.ulike.style.SampledMakeupPipeline.*;
import com.hiro.ulike.style.StyleLutPipeline;
import hiro.ulike.beauty.PreparedNeuralLayer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Actual HDR appearance processing and paired output, using a declared replacement policy.
 * No input/output gain ratio, pre-edit gainmap or captured clipping residual is reused.
 */
public final class HdrBeautyProcessor {
    private HdrBeautyProcessor(){}
    public interface PairSink {
        void begin(Object exactSource,int width,int height,String settingsSha256,String geometryId,double headroom)throws Exception;
        /** Borrowed packed full-width FP64 rows in display-linear BT2020 / 203-nit units. */
        void writeRows(int firstRow,int rows,double[] processedSdrBt2020,double[] processedHdrBt2020)throws Exception;
        /** Seal private pair staging only; publishing a final file is a later operation. */
        void commit()throws Exception;
        void abort()throws Exception;
    }
    public enum GraphPolicy { DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS }
    public static final class Snapshot {
        public final Object frameIdentity;
        public final int width,height;
        public final String captureId,geometryId,settingsSha256;
        public final Style style;
        public final HdrAppearance.Policy policy;
        private final SdrRendition rendition;
        private final PreparedNeuralLayer neural;
        private final byte[] canonical;
        private Snapshot(SdrRendition rendition,PreparedNeuralLayer layer,HdrAppearance.Policy policy,
                         String capture,String geometry,byte[] applicationSettings,GraphPolicy graphPolicy){
            require(rendition!=null && layer!=null && policy!=null && graphPolicy==GraphPolicy.DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS,"explicit rendition/layer/HDR graph policy required");
            require(layer.preparedFrom(rendition) && layer.frameIdentity==rendition.frameIdentity()
                    && layer.width==rendition.width() && layer.height==rendition.height(),"layer was not prepared from this exact captured SDR rendition");
            id(capture);id(geometry);require(applicationSettings!=null && applicationSettings.length>0 && applicationSettings.length<=1024*1024,"bounded exact application settings required");
            this.rendition=rendition;this.neural=layer;this.policy=policy;this.frameIdentity=layer.frameIdentity;
            this.width=layer.width;this.height=layer.height;this.captureId=capture;this.geometryId=geometry;
            this.style=Style.valueOf(layer.style.name());
            String text="ulike-hdr-appearance-settings-v1\npolicy="+HdrAppearance.POLICY
                +"\nstyle="+style+"\nneural-layer-sha256="+layer.processingSha256+"\nproxy="+rendition.policyName()+"\ngeometry="+geometry
                +"\ngraph="+graphPolicy+"\nsdr-white-nits=203\nhlg-reference-nits=1000\nhlg-system-gamma=1.2"
                +"\ngenerated-peak="+Double.toHexString(policy.generatedPeakNits)+"\nstorage-peak="+Double.toHexString(policy.storagePeakNits)
                +"\napplication-settings-sha256="+sha(applicationSettings.clone())+"\n";
            canonical=text.getBytes(StandardCharsets.UTF_8);settingsSha256=sha(canonical);
        }
        public static Snapshot create(SdrRendition rendition,PreparedNeuralLayer layer,HdrAppearance.Policy policy,
                String captureId,String geometryId,byte[] exactAppSettingsSnapshot,GraphPolicy graphPolicy){
            return new Snapshot(rendition,layer,policy,captureId,geometryId,exactAppSettingsSnapshot,graphPolicy);
        }
        public byte[] settingsSnapshotBytes(){return canonical.clone();}
    }
    /** Exact external HDR shader source for Purity video/u_basic bindings. Never inferred from SDR. */
    public static final class HdrBase {
        public final Object frameIdentity;
        public final FrameTile tile;
        public final String geometryId,bindingEvidence;
        public final double[] displayBt2020;
        public HdrBase(Object identity,FrameTile tile,String geometryId,String bindingEvidence,double[] hdr){
            this.frameIdentity=identity;this.tile=tile;this.geometryId=geometryId;this.bindingEvidence=bindingEvidence;this.displayBt2020=hdr;
        }
    }
    public static final class HdrPass {
        public final ResolvedPass parameters;
        public final HdrBase shaderBase;
        /** parameters.shaderBaseRgb must be null: a separate matched HDR base is mandatory where used. */
        public HdrPass(ResolvedPass parameters,HdrBase shaderBase){this.parameters=parameters;this.shaderBase=shaderBase;}
    }
    public static final class ResolvedTile {
        public final Object frameIdentity;
        public final String settingsSha256,geometryId;
        public final List<HdrPass> makeup;
        public final StyleLutPipeline.ResolvedLuts luts;
        public ResolvedTile(Object frameIdentity,String settingsSha256,String geometryId,List<HdrPass> makeup,StyleLutPipeline.ResolvedLuts luts){
            this.frameIdentity=frameIdentity;this.settingsSha256=settingsSha256;this.geometryId=geometryId;this.makeup=makeup;this.luts=luts;
        }
    }
    public interface BindingProvider {
        /** All arrays are borrowed and must remain immutable during processing of this tile.
         * Geometry and runtime settings must be resolved for this exact still, not a preview.
         */
        ResolvedTile resolve(Snapshot snapshot,FrameTile tile)throws Exception;
    }
    public static final class Budget {
        public final int tileRows,maxTilePixels;
        public final long maxImagePixels,maxModuleArrayAllocationBytesPerTile;
        public Budget(int tileRows,int maxTilePixels,long maxImagePixels,long maxArrayBytes){
            require(tileRows>=1 && tileRows<=64 && maxTilePixels>0 && maxTilePixels<=262144 && maxImagePixels>0 && maxArrayBytes>0,"bounded HDR tile policy required");
            this.tileRows=tileRows;this.maxTilePixels=maxTilePixels;this.maxImagePixels=maxImagePixels;this.maxModuleArrayAllocationBytesPerTile=maxArrayBytes;
        }
        public static Budget standard(){return new Budget(4,262144,25000000,32L*1024*1024);}
    }
    public static final class Result {
        public final Snapshot snapshot;
        public final long sourceGamutMappedPixels,sourceNonpositiveLumaPixels,generatedPeakLimitedPixels,workingGamutMappingEvents,changedStagePixels;
        public final boolean wholeCapturedHdrInformationPreserved=false,nativeHdrStyleEquivalent=false,androidDeviceVerified=false;
        private Result(Snapshot snapshot,Counts c){this.snapshot=snapshot;sourceGamutMappedPixels=c.sourceGamut;sourceNonpositiveLumaPixels=c.sourceBlack;generatedPeakLimitedPixels=c.generatedCap;workingGamutMappingEvents=c.workingGamut;changedStagePixels=c.changed;}
    }
    private static final class Counts {long sourceGamut,sourceBlack,generatedCap,workingGamut,changed;}
    private static void require(boolean b,String message){if(!b)throw new IllegalArgumentException(message);}
    private static void id(String s){require(s!=null && !s.trim().isEmpty() && s.length()<=256 && s.indexOf('\n')<0 && s.indexOf('\r')<0,"bounded explicit identity required");}
    private static String sha(byte[] b){try{byte[] d=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder();for(byte v:d)s.append(Character.forDigit((v&255)>>>4,16)).append(Character.forDigit(v&15,16));return s.toString();}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static boolean same(FrameTile a,FrameTile b){return a!=null && b!=null && a.captureId.equals(b.captureId) && a.sensorTimestampNs==b.sensorTimestampNs && a.imageWidth==b.imageWidth && a.imageHeight==b.imageHeight && a.x==b.x && a.y==b.y && a.width==b.width && a.height==b.height;}
    private static long tileAllocationBound(Style style,int pixels){return 65536L+(style==Style.NATURAL_BLUSH?256L:1024L)*pixels;}

    public static Result render(Snapshot snapshot,BindingProvider bindings,PairSink sink,Budget budget)throws Exception {
        require(snapshot!=null && bindings!=null && sink!=null && budget!=null,"snapshot, bindings, private pair sink and budget required");
        int w=snapshot.width,h=snapshot.height;require(w>=1 && h>=1 && w<=16384 && h<=16384 && (long)w*h<=budget.maxImagePixels && w<=budget.maxTilePixels,"HDR raster budget");
        require(tileAllocationBound(snapshot.style,w)<=budget.maxModuleArrayAllocationBytesPerTile,"one HDR row exceeds allocation budget");
        int rows=Math.min(h,Math.min(budget.tileRows,budget.maxTilePixels/w));
        while(tileAllocationBound(snapshot.style,w*rows)>budget.maxModuleArrayAllocationBytesPerTile)rows--;
        P010SceneSource captured=snapshot.rendition.hdrSource();require(captured.frameIdentity()==snapshot.frameIdentity,"captured source changed");
        Counts counts=new Counts();boolean active=false;
        try {
            active=true;sink.begin(snapshot.frameIdentity,w,h,snapshot.settingsSha256,snapshot.geometryId,snapshot.policy.headroom());
            double[] scene=new double[3],display=new double[3],layer=new double[4],patch=new double[3];
            for(int start=0;start<h;start+=rows){
                if(Thread.currentThread().isInterrupted())throw new java.io.IOException("HDR processing interrupted");
                int count=Math.min(rows,h-start),n=w*count;double[] current=new double[n*3];
                FrameTile tile=new FrameTile(snapshot.captureId,captured.frameIdentity().timestampNs,w,h,0,start,w,count);
                ResolvedTile resolved=bindings.resolve(snapshot,tile);validateBindings(snapshot,tile,resolved);
                for(int row=0;row<count;row++)for(int x=0;x<w;x++){
                    int off=(row*w+x)*3;captured.readPixel(x,start+row,scene);
                    int flags=HdrAppearance.sceneToDisplay(scene[0],scene[1],scene[2],snapshot.policy,display);
                    if((flags&1)!=0)counts.sourceGamut++;if((flags&2)!=0)counts.sourceBlack++;
                    snapshot.neural.sampleAt(x,start+row,layer);double weight=layer[3];
                    if(weight>0){if(HdrAppearance.generatedPatch(layer[0],layer[1],layer[2],snapshot.rendition.exposureScale,snapshot.policy,patch))counts.generatedCap++;
                        for(int c=0;c<3;c++)display[c]=weight==1?patch[c]:display[c]*(1-weight)+patch[c]*weight;}
                    HdrAppearance.checkedDisplay(display[0],display[1],display[2],snapshot.policy.headroom());
                    System.arraycopy(display,0,current,off,3);
                }
                for(HdrPass pass:resolved.makeup)current=makeup(snapshot,tile,current,pass,counts);
                current=lut(snapshot,tile,current,resolved.luts,false,counts);
                if(snapshot.style==Style.PURITY2)current=lut(snapshot,tile,current,resolved.luts,true,counts);
                double[] sdr=new double[current.length];
                for(int i=0;i<n;i++){int off=i*3;HdrAppearance.checkedDisplay(current[off],current[off+1],current[off+2],snapshot.policy.headroom());
                    HdrAppearance.deriveSdr(current[off],current[off+1],current[off+2],display);System.arraycopy(display,0,sdr,off,3);}
                sink.writeRows(start,count,sdr,current);
            }
            sink.commit();active=false;return new Result(snapshot,counts);
        }catch(Exception|Error failure){if(active)try{sink.abort();}catch(Exception|Error cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
    private static void validateBindings(Snapshot s,FrameTile tile,ResolvedTile resolved){
        require(resolved!=null && resolved.frameIdentity==s.frameIdentity && s.settingsSha256.equals(resolved.settingsSha256) && s.geometryId.equals(resolved.geometryId),"HDR style bindings are not the exact still/settings/geometry");
        require(resolved.makeup!=null && resolved.luts!=null && same(tile,resolved.luts.tile),"matched complete makeup/LUT bindings required");
        Set<Pass> found=new HashSet<>();int z=Integer.MIN_VALUE;
        for(HdrPass hp:resolved.makeup){
            require(hp!=null && hp.parameters!=null,"missing HDR pass");ResolvedPass p=hp.parameters;
            require(same(tile,p.tile) && p.pass.style==s.style && found.add(p.pass) && p.pass.zOrder>=z,"wrong/repeated/out-of-order HDR pass");z=p.pass.zOrder;
            require(p.shaderBaseRgb==null,"SDR-only external shader base is forbidden in HDR processing");
            boolean external=p.pass==Pass.PURITY_BLUSHER || p.pass==Pass.PURITY_3D;
            if(external){HdrBase base=hp.shaderBase;require(base!=null && base.frameIdentity==s.frameIdentity && same(tile,base.tile) && s.geometryId.equals(base.geometryId),"explicit matched HDR shader source required");id(base.bindingEvidence);
                require(base.displayBt2020!=null && base.displayBt2020.length==tile.pixels()*3,"HDR shader source shape");
                for(int i=0;i<base.displayBt2020.length;i+=3)HdrAppearance.checkedDisplay(base.displayBt2020[i],base.displayBt2020[i+1],base.displayBt2020[i+2],s.policy.headroom());
            }else require(hp.shaderBase==null,"unexpected external HDR shader source");
        }
        for(Pass p:Pass.values())if(p.style==s.style)require(found.contains(p),"missing required HDR style pass "+p);
        if(s.style==Style.NATURAL_BLUSH)require(resolved.luts.finalLut==null && resolved.luts.finalIntensity==0,"Natural has no separate final LUT");
        else require(resolved.luts.finalLut!=null,"Purity final LUT required");
    }
    private static final class Working {
        final double[] encoded,base,scale,explicitHdrBase;
        Working(double[] hdr,double[] external,Counts counts){
            explicitHdrBase=external;int n=hdr.length/3;encoded=new double[hdr.length];base=external==null?null:new double[hdr.length];scale=new double[n];
            double[] a=new double[3],b=new double[3];
            for(int i=0;i<n;i++){int off=i*3;if(HdrAppearance.workingSrgb(hdr[off],hdr[off+1],hdr[off+2],a))counts.workingGamut++;
                double m=Math.max(1,Math.max(a[0],Math.max(a[1],a[2])));
                if(external!=null){if(HdrAppearance.workingSrgb(external[off],external[off+1],external[off+2],b))counts.workingGamut++;m=Math.max(m,Math.max(b[0],Math.max(b[1],b[2])));}
                scale[i]=m;for(int c=0;c<3;c++){encoded[off+c]=HdrAppearance.encodeSrgb(a[c]/m);if(base!=null)base[off+c]=HdrAppearance.encodeSrgb(b[c]/m);}
            }
        }
    }
    /** Full-effect normalized fragment is expanded independently, then composed
     * with its actual outer weight in display light. Tiny weights therefore do
     * not abruptly discard the current pixel's wide gamut. No captured residual
     * is reintroduced: original below means the current already-edited stage.
     */
    private static void compose(Snapshot snapshot,double[] current,Working working,int i,
            double[] fragment,double[] output,double[] rgb,Counts counts){
        int off=i*3;double weight=fragment[3];
        require(Double.isFinite(weight) && weight>=0 && weight<=1,"invalid effect weight");
        if(weight==0)return;
        boolean unchanged=true,unchangedExternal=working.base!=null;
        for(int c=0;c<3;c++){double value=fragment[c];require(Double.isFinite(value) && value>=0 && value<=1,"stage emitted invalid normalized sRGB");
            if(Double.doubleToRawLongBits(value)!=Double.doubleToRawLongBits(working.encoded[off+c]))unchanged=false;
            if(working.base!=null && Double.doubleToRawLongBits(value)!=Double.doubleToRawLongBits(working.base[off+c]))unchangedExternal=false;}
        // An explicitly bound source has its own HDR appearance. If the shader
        // leaves that source unchanged (notably 3D intensity zero), retain its
        // actual wide gamut. Equal normalized colors cannot prove equal HDR.
        if(unchangedExternal)System.arraycopy(working.explicitHdrBase,off,rgb,0,3);
        else if(working.base==null && unchanged)return;
        else HdrAppearance.srgbToBt2020(HdrAppearance.decodeSrgb(fragment[0])*working.scale[i],
                HdrAppearance.decodeSrgb(fragment[1])*working.scale[i],HdrAppearance.decodeSrgb(fragment[2])*working.scale[i],rgb);
        HdrAppearance.checkedDisplay(rgb[0],rgb[1],rgb[2],snapshot.policy.headroom());
        for(int c=0;c<3;c++)output[off+c]=weight==1?rgb[c]:current[off+c]*(1-weight)+rgb[c]*weight;
        HdrAppearance.checkedDisplay(output[off],output[off+1],output[off+2],snapshot.policy.headroom());counts.changed++;
    }
    private static double[] makeup(Snapshot snapshot,FrameTile tile,double[] current,HdrPass pass,Counts counts){
        Working work=new Working(current,pass.shaderBase==null?null:pass.shaderBase.displayBt2020,counts);ResolvedPass p=pass.parameters;
        ResolvedPass normalized=new ResolvedPass(p.pass,p.tile,p.rgba,p.coverage,p.intensity,p.opacity,p.colorOverride,p.segmentation,p.segmentationInside,work.base);
        SampledMakeupPipeline.BorrowedLayer layer=SampledMakeupPipeline.borrowLayer(Domain.ENCODED_SDR_FULL_RANGE,tile,work.encoded,normalized);
        double[] output=current.clone(),fragment=new double[4],rgb=new double[3];
        for(int i=0;i<tile.pixels();i++){layer.sample(i,fragment);compose(snapshot,current,work,i,fragment,output,rgb,counts);}
        return output;
    }
    private static double[] lut(Snapshot snapshot,FrameTile tile,double[] current,StyleLutPipeline.ResolvedLuts luts,boolean last,Counts counts){
        Working work=new Working(current,null,counts);
        // Validate the supplied intensity using the same SDR leaf; full-strength
        // color is sampled separately so the HDR blend weight stays explicit.
        double intensity=last?luts.finalIntensity:luts.skinIntensity;
        require(Double.isFinite(intensity) && intensity>=0 && intensity<=1,"invalid LUT intensity");
        double[] effect=last?StyleLutPipeline.finalLut(Domain.ENCODED_SDR_FULL_RANGE,tile,work.encoded,luts.finalLut,1)
                :StyleLutPipeline.skin(Domain.ENCODED_SDR_FULL_RANGE,tile,work.encoded,luts.background,luts.skin,luts.skinMaskAfterShaderFlip,1);
        double[] output=current.clone(),fragment=new double[4],rgb=new double[3];fragment[3]=intensity;
        for(int i=0;i<tile.pixels();i++){System.arraycopy(effect,i*3,fragment,0,3);compose(snapshot,current,work,i,fragment,output,rgb,counts);}
        return output;
    }
}
