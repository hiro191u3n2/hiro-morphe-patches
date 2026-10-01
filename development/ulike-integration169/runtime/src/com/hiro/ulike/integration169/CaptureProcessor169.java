package com.hiro.ulike.integration169;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import com.hiro.ulike.binding.ShotStyleSettings;
import com.hiro.ulike.hdr.analysisinput.AnalysisInput;
import com.hiro.ulike.hdr.analysisinput.AndroidAnalysisInput;
import com.hiro.ulike.hdr.beauty.HdrAppearance;
import com.hiro.ulike.hdr.beauty.HdrBeautyProcessor;
import com.hiro.ulike.hdr.color.SdrRendition;
import com.hiro.ulike.hdr.faceprobe.AnalysisCapacity;
import com.hiro.ulike.hdr.gainmap.GainmapSave;
import com.hiro.ulike.hdr.photo.AndroidPhotoTransaction;
import com.hiro.ulike.hdr.photo.GeometryPairWriter;
import com.hiro.ulike.hdr.photo.PhotoGeometry;
import com.hiro.ulike.hdr.photo.PhotoIdentity;
import com.hiro.ulike.hdr.stillanalysis.PausedStockPreview;
import com.hiro.ulike.hdr.stillanalysis.SdkFaceSnapshot;
import com.hiro.ulike.hdr.stillanalysis.StockStillAnalysis;
import com.hiro.ulike.style.SampledMakeupPipeline;
import hiro.ulike.beauty.BeautyImageEngine;
import hiro.ulike.beauty.PinnedAssets;
import hiro.ulike.beauty.PreparedNeuralLayer;
import hiro.ulike.model.PinnedModel;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Concrete owned-P010 -> owned analysis -> inference/HDR -> transformed pair -> URI flow.
 * No instance is installed. Trusted native plan/calibration providers still have to be implemented,
 * reviewed and measured on the phone. Evidence strings are trace references, not proof by themselves.
 * The hard-false capture gate is deliberately independent of this callable coordinator.
 */
public final class CaptureProcessor169 implements CameraBridge169.Processor {
    /** Must freeze the actual complete replay journal, calibrated chroma/geometry and output
     * choices for this exact shutter. Must throw when native queue/coverage proof is absent.
     * This private checkpoint deliberately supplies NO default implementation. */
    public interface PlanProvider { Plan prepare(CameraBridge169.OwnedShot shot)throws Exception; }
    /** Must bind actual observed native geometry, shader samplers, full makeup graph and draw
     * order to this exact analysis. Detector order/preview geometry are not acceptable substitutes.
     * Before return, consume required diagnostic pixels into owned immutable mask/binding data.
     * The coordinator closes the diagnostic on return/failure; retaining it or borrowed arrays is
     * invalid. Returning a result is a trusted adapter contract, not manufactured native proof. */
    public interface NativeBindingProvider {
        NativeBindings resolve(CameraBridge169.OwnedShot shot,AndroidAnalysisInput.BoundOutcome analysis,
                               SdrRendition rendition,byte[] exactSettings,PhotoGeometry geometry)throws Exception;
    }
    public static final class FaceParameters {
        public final int sdkFaceId;
        public final double intensity;
        private final double[] sourceToCrop;
        public FaceParameters(int sdkFaceId,double[] sourceToCrop,double intensity){
            require(sourceToCrop!=null && sourceToCrop.length==9 && Double.isFinite(intensity) && intensity>=0 && intensity<=1,"Exact affine crop and neural intensity required");
            this.sdkFaceId=sdkFaceId;this.sourceToCrop=sourceToCrop.clone();this.intensity=intensity;
            for(double v:this.sourceToCrop)require(Double.isFinite(v),"Finite crop required");
        }
    }
    /** Immutable ordered neural parameters; full tile bindings retain their provider contract. */
    public static final class NativeBindings {
        final AndroidAnalysisInput.BoundOutcome analysis;
        final List<FaceParameters> neuralFaces;
        final String nativeOrderEvidence,calibrationEvidence;
        final HdrBeautyProcessor.BindingProvider fullStyle;
        final byte[] resolvedGraphSettings;
        public NativeBindings(AndroidAnalysisInput.BoundOutcome analysis,List<FaceParameters> completeNativeDrawOrder,
                String nativeOrderEvidence,String calibrationEvidence,byte[] exactResolvedGraphSettings,HdrBeautyProcessor.BindingProvider fullStyle){
            require(analysis!=null && completeNativeDrawOrder!=null && completeNativeDrawOrder.size()<=10 && fullStyle!=null,"Exact observations and complete bounded bindings required");
            require(exactResolvedGraphSettings!=null && exactResolvedGraphSettings.length>0 && exactResolvedGraphSettings.length<=512*1024,"Exact resolved uniforms/mesh/texture/sampler bindings required");
            evidence(nativeOrderEvidence);evidence(calibrationEvidence);
            Set<Integer> observed=new HashSet<>();
            for(SdkFaceSnapshot.Face face:analysis.observations.faces.faces())require(observed.add(face.faceId),"Duplicate SDK face ID");
            List<FaceParameters> copy=new ArrayList<>();Set<Integer> bound=new HashSet<>();
            for(FaceParameters face:completeNativeDrawOrder){require(copy.size()<10,"Neural face count changed/exceeded bound");require(face!=null && bound.add(face.sdkFaceId),"Duplicate or missing neural face parameters");copy.add(face);}
            require(observed.equals(bound),"Native neural order must account for every observed face exactly once");
            this.analysis=analysis;neuralFaces=Collections.unmodifiableList(copy);this.nativeOrderEvidence=nativeOrderEvidence;
            this.calibrationEvidence=calibrationEvidence;this.fullStyle=fullStyle;
            resolvedGraphSettings=exactResolvedGraphSettings.clone();
        }
    }
    /** Owned plan. Supplying it does not install capture, validate a native calibration, or
     * claim HEIF viewer/device support. Codec and native setup implementations are required. */
    public static final class Plan implements AutoCloseable {
        final CameraBridge169.OwnedShot shot;
        final SdrRendition rendition;
        final AnalysisCapacity analysisCapacity;
        final PhotoGeometry geometry;
        final PinnedModel.CompiledModel model;
        final PinnedAssets.NeuralMask mask;
        final HdrAppearance.Policy hdrPolicy;
        final StockStillAnalysis.EffectSetup setup;
        final PausedStockPreview.RestoreVerification restoration;
        final NativeBindingProvider binding;
        final Set<String> expectedFeatures;
        final byte[] settings;
        final GainmapSave.Codec codec;
        final GainmapSave.QualityLimits quality;
        final SavedUriHandoff169.Completion completion;
        final long nativeTimeoutMillis,diskBudgetBytes;
        final AutoCloseable resourceOwner;
        private boolean closed;
        public Plan(CameraBridge169.OwnedShot shot,SdrRendition rendition,AnalysisCapacity analysisCapacity,PhotoGeometry geometry,
                PinnedModel.CompiledModel model,PinnedAssets.NeuralMask mask,HdrAppearance.Policy hdrPolicy,
                byte[] verifiedCompleteReplayTranscript,Set<String> expectedFeatures,StockStillAnalysis.EffectSetup setup,
                PausedStockPreview.RestoreVerification restoration,NativeBindingProvider binding,
                GainmapSave.Codec codec,GainmapSave.QualityLimits quality,SavedUriHandoff169.Completion completion,
                long nativeTimeoutMillis,long diskBudgetBytes,AutoCloseable resourceOwner)throws Exception {
            require(shot!=null && rendition!=null && analysisCapacity!=null && geometry!=null && model!=null && mask!=null && hdrPolicy!=null
                && expectedFeatures!=null && !expectedFeatures.isEmpty() && setup!=null && restoration!=null && binding!=null
                && codec!=null && quality!=null && completion!=null && resourceOwner!=null,"Complete explicit processing plan required");
            require(nativeTimeoutMillis>=1 && nativeTimeoutMillis<=30000 && diskBudgetBytes>0 && diskBudgetBytes<=8L*1024*1024*1024,"Bounded native deadline and disk budget required");
            require(rendition.frameIdentity()==shot.pixels && geometry.sourceWidth==shot.pixels.width && geometry.sourceHeight==shot.pixels.height,"Plan uses another capture or input grid");
            require(shot.style.shotIdentity==shot.shotIdentity && shot.style.shotEpoch==shot.shotEpoch && shot.style.recorderIdentity==shot.recorder,"Style/capture ownership mismatch");
            PinnedModel.Style expected=ShotStyleSettings.NATURAL.equals(shot.style.styleId)?PinnedModel.Style.NATURAL_BLUSH:
                ShotStyleSettings.PURITY.equals(shot.style.styleId)?PinnedModel.Style.PURITY2:null;
            require(expected!=null && model.style==expected && mask.style==expected,"Pinned model/mask differ from selected style");
            this.shot=shot;this.rendition=rendition;this.analysisCapacity=analysisCapacity;this.geometry=geometry;this.model=model;this.mask=mask;this.hdrPolicy=hdrPolicy;
            this.settings=CapturedSettings169.encode(shot.style,verifiedCompleteReplayTranscript);
            LinkedHashSet<String> features=new LinkedHashSet<>();int visited=0;
            for(String feature:expectedFeatures){require(++visited<=64 && feature!=null && feature.matches("[A-Za-z0-9_.-]{1,64}") && features.add(feature),"Invalid/duplicate/unbounded native feature list");}
            require(!features.isEmpty(),"Empty native feature list");this.expectedFeatures=Collections.unmodifiableSet(features);
            this.setup=setup;this.restoration=restoration;this.binding=binding;this.codec=codec;this.quality=quality;
            this.completion=completion;this.nativeTimeoutMillis=nativeTimeoutMillis;this.diskBudgetBytes=diskBudgetBytes;this.resourceOwner=resourceOwner;
        }
        @Override public synchronized void close()throws Exception{if(!closed){closed=true;resourceOwner.close();}}
    }
    private final Context context;
    private final PlanProvider plans;
    private final ProcessingSequence169 sequence=new ProcessingSequence169();
    public CaptureProcessor169(Context context,PlanProvider plans){
        if(context==null || plans==null)throw new NullPointerException();
        Context app=context.getApplicationContext();this.context=app==null?context:app;this.plans=plans;
    }
    public boolean cancel(CameraBridge169.OwnedShot shot){return shot!=null && sequence.cancel(shot.shotIdentity);}
    @Override public void process(CameraBridge169.OwnedShot shot)throws Exception {
        if(shot==null)throw new NullPointerException("shot");
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Owned processing requires a worker");
        ProcessingSequence169.Result result=sequence.execute(shot.shotIdentity,new Work(shot));
        if(result.cleanupFailure!=null)android.util.Log.e("ULike169","Photo committed; private processing cleanup failed",result.cleanupFailure);
    }
    private final class Work implements ProcessingSequence169.Work {
        final CameraBridge169.OwnedShot shot;
        Plan plan;AnalysisInput.Streaming input;AndroidAnalysisInput.BoundOutcome analysis;
        NativeBindings nativeBindings;HdrBeautyProcessor.Snapshot hdr;
        AndroidPhotoTransaction transaction;GeometryPairWriter pair;
        Work(CameraBridge169.OwnedShot shot){this.shot=shot;}
        @Override public void validate()throws Exception {
            ShotStyleSettings.requireCurrent(shot.style);
            plan=plans.prepare(shot);require(plan!=null && plan.shot==shot && !plan.closed,"No exact owned native plan");
            ShotStyleSettings.requireCurrent(shot.style);
            // Preflight the exact native grid and explicit allocation policy before preview
            // interruption. Rows are later written to one transferred Bitmap without an
            // int[P] staging raster or downstream Bitmap copies. The 4080x3060 candidate
            // capacity is a code budget, not a measured device/GL/native-memory guarantee.
            input=AnalysisInput.prepareStreaming(plan.rendition,plan.geometry.id(),plan.settings,
                StockStillAnalysis.newNonce(),plan.analysisCapacity);
        }
        @Override public void analyse()throws Exception {
            Object backend=AppHook169.publicField(shot.recorder,"b");
            File workspace=new File(context.getCacheDir(),"ulike-owned-analysis-"+UUID.randomUUID());
            analysis=PausedStockPreview.run(context,shot.recorder,backend,plan.nativeTimeoutMillis,plan.restoration,
                lease->AndroidAnalysisInput.run(input,lease,workspace,plan.nativeTimeoutMillis,plan.expectedFeatures,true,plan.setup));
            require(analysis!=null && analysis.input==input.descriptor() && analysis.input.frameIdentity==shot.pixels,"Analysis source changed");
            // PausedStockPreview only returns after real native teardown and required
            // original preview/composer restoration verification. Failure stops here.
        }
        @Override public void bind()throws Exception {
            try{
                nativeBindings=plan.binding.resolve(shot,analysis,plan.rendition,plan.settings.clone(),plan.geometry);
            }finally{
                // SDK teardown already joined. Release the full-resolution ARGB diagnostic
                // before model layers, HDR staging and codecs retain further image data.
                if(analysis.observations.renderedDiagnostic!=null)analysis.observations.renderedDiagnostic.close();
            }
            require(nativeBindings!=null && nativeBindings.analysis==analysis,"Native bindings belong to another analysis");
            String captureId=captureId(shot);
            byte[] resolvedSettings=resolvedSettings(plan.settings,nativeBindings);
            List<String> ids=new ArrayList<>();for(FaceParameters face:nativeBindings.neuralFaces)ids.add(Integer.toString(face.sdkFaceId));
            HdrBeautyProcessor.ObservedNeuralOrder order=HdrBeautyProcessor.ObservedNeuralOrder.capture(plan.rendition,
                SampledMakeupPipeline.Style.valueOf(plan.model.style.name()),captureId,plan.geometry.id(),resolvedSettings,ids,nativeBindings.nativeOrderEvidence);
            HdrBeautyProcessor.preflightNeuralOrder(order,HdrBeautyProcessor.Budget.standard());
            List<HdrBeautyProcessor.NeuralFace> layers=new ArrayList<>();
            if(!nativeBindings.neuralFaces.isEmpty())try(BeautyImageEngine engine=BeautyImageEngine.openAndroid(plan.model)){
                for(FaceParameters face:nativeBindings.neuralFaces){
                    if(Thread.currentThread().isInterrupted())throw new IOException("Neural inference cancelled");
                    PreparedNeuralLayer layer=engine.prepareLayer(plan.rendition,face.sourceToCrop.clone(),plan.mask,face.intensity,
                        BeautyImageEngine.SDR_DOMAIN,BeautyImageEngine.Budget.standard());
                    layers.add(new HdrBeautyProcessor.NeuralFace(Integer.toString(face.sdkFaceId),layer));
                }
            }
            hdr=HdrBeautyProcessor.Snapshot.createOrderedFaces(plan.rendition,order,layers,plan.hdrPolicy,captureId,plan.geometry.id(),resolvedSettings,
                HdrBeautyProcessor.GraphPolicy.DECLARED_OUTER_ORDER_WITH_RESOLVED_SUBORDER_AND_HDR_SOURCE_BINDINGS);
        }
        @Override public void render()throws Exception {
            PhotoIdentity identity=new PhotoIdentity(shot.pixels,plan.geometry.width,plan.geometry.height,hdr.captureId,
                plan.geometry.id(),"ulike-owned-hdr-coordinator-v1",hdr.settingsSnapshotBytes(),shot.dateTakenMs);
            transaction=AndroidPhotoTransaction.begin(context,identity,plan.diskBudgetBytes);
            pair=transaction.staging.createTransformedPair(plan.geometry,plan.hdrPolicy.headroom(),16L*1024*1024);
            HdrBeautyProcessor.render(hdr,nativeBindings.fullStyle,pair.asHdrSink(),HdrBeautyProcessor.Budget.standard());
            require(pair.pair().identity==transaction.staging.identity,"Processed pair transaction changed");
        }
        @Override public String publish()throws Exception {
            Uri saved=SavedUriHandoff169.saveAndComplete(shot,transaction,pair.pair(),plan.quality,plan.codec,plan.completion);
            return saved.toString();
        }
        @Override public void close()throws Exception {
            ProcessingSequence169.closeOwned(pair,transaction,analysis==null?null:analysis.observations.renderedDiagnostic,input,plan);
        }
    }
    private static String captureId(CameraBridge169.OwnedShot shot){
        // Unique owned shutter identity and actual sensor timestamp, not the latest preview.
        return "shot-"+shot.shotEpoch+"-sensor-"+shot.pixels.timestampNs+"-frame-"+shot.pixels.frameNumber;
    }
    private static byte[] resolvedSettings(byte[] requested,NativeBindings bindings)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeUTF("ulike-resolved-native-still-settings-v1");out.writeInt(requested.length);out.write(requested);
        out.writeUTF(bindings.analysis.input.sourceSha256);out.writeUTF(bindings.analysis.input.proxySha256);
        out.writeUTF(bindings.nativeOrderEvidence);out.writeUTF(bindings.calibrationEvidence);
        out.writeInt(bindings.resolvedGraphSettings.length);out.write(bindings.resolvedGraphSettings);out.flush();
        byte[] result=bytes.toByteArray();require(result.length<=1024*1024,"Combined native settings budget");return result;
    }
    private static void evidence(String s){require(s!=null && !s.trim().isEmpty() && s.length()<=256 && s.indexOf('\n')<0 && s.indexOf('\r')<0,"Explicit bounded native evidence reference required");}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
}
