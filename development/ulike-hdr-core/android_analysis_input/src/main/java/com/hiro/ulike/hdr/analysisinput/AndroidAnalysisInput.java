package com.hiro.ulike.hdr.analysisinput;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import com.hiro.ulike.hdr.faceprobe.StockStillFaceProbe;
import com.hiro.ulike.hdr.faceprobe.OwnedBitmapSubmission;
import com.hiro.ulike.hdr.stillanalysis.StockStillAnalysis;
import java.io.File;
import java.util.Set;

/** Submits our actual captured-rendition pixels, then checks the native bridge's owned input digest.
 * Pixel provenance is established here; native geometry/sampler correctness is not.
 */
public final class AndroidAnalysisInput {
    private AndroidAnalysisInput(){}
    public static final class BoundOutcome {
        public final AnalysisInput.Descriptor input;
        public final StockStillAnalysis.Outcome observations;
        public final boolean submittedPixelsMatchCapturedRendition=true,nativeGeometryCalibrated=false;
        private BoundOutcome(AnalysisInput.Descriptor input,StockStillAnalysis.Outcome observations){this.input=input;this.observations=observations;}
    }
    public static BoundOutcome run(AnalysisInput.Owned input,StockStillFaceProbe.IdleSdkLease lease,
            File newWorkspace,long timeoutMillis,Set<String> expectedFeatures,boolean captureDiagnostic,
            StockStillAnalysis.EffectSetup setup)throws Exception {
        if(input==null || lease==null)throw new NullPointerException("owned input and actual idle SDK lease required");
        AnalysisInput.Descriptor descriptor=input.descriptor();
        StockStillAnalysis.Request request=new StockStillAnalysis.Request(descriptor.nonce,descriptor.sensorTimestampNs,
            descriptor.sourceSha256,descriptor.settingsSha256,expectedFeatures,captureDiagnostic);
        final Bitmap[] raster=new Bitmap[1];
        try {
            AnalysisInput.Submission submitted=input.transfer(new AnalysisInput.RowTarget(){
                @Override public void begin(int w,int h){raster[0]=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888,false,ColorSpace.get(ColorSpace.Named.SRGB));}
                @Override public void row(int y,int[] argb){raster[0].setPixels(argb,0,descriptor.width,0,y,descriptor.width,1);}
                @Override public void complete(){}
                @Override public void abort(){if(raster[0]!=null)raster[0].recycle();}
            });
            if(submitted.descriptor!=descriptor)throw new IllegalStateException("Analysis transfer identity changed");
            StockStillAnalysis.Outcome result=StockStillAnalysis.run(lease,raster[0],newWorkspace,timeoutMillis,request,setup);
            if(result==null || result.request!=request || !result.observationsComplete ||
                    !descriptor.proxySha256.equals(result.submittedProxySha256) || result.nativeEvidence==null ||
                    result.nativeEvidence.width!=descriptor.width || result.nativeEvidence.height!=descriptor.height ||
                    !result.nativeEvidence.callbacksObserved || result.scriptExports==null ||
                    result.scriptExports.nonce!=descriptor.nonce)
                throw new IllegalStateException("Same-capture analysis provenance or complete callbacks not established");
            if(captureDiagnostic && (result.renderedDiagnostic==null || result.renderedDiagnostic.nonce!=descriptor.nonce ||
                    result.renderedDiagnostic.width!=descriptor.width || result.renderedDiagnostic.height!=descriptor.height))
                throw new IllegalStateException("Diagnostic raster belongs to a different submission/grid");
            return new BoundOutcome(descriptor,result);
        } finally {if(raster[0]!=null && !raster[0].isRecycled())raster[0].recycle();}
    }
    /** Native-size candidate: one Bitmap, moved through both lower layers, with no source int[P]. */
    public static BoundOutcome run(AnalysisInput.Streaming input,StockStillFaceProbe.IdleSdkLease lease,
            File newWorkspace,long timeoutMillis,Set<String> expectedFeatures,boolean captureDiagnostic,
            StockStillAnalysis.EffectSetup setup)throws Exception {
        if(input==null || lease==null)throw new NullPointerException("owned input and actual idle SDK lease required");
        final Bitmap[] raster=new Bitmap[1];OwnedBitmapSubmission moved=null;
        StockStillAnalysis.Outcome result=null;boolean delivered=false;
        try {
            AnalysisInput.Submission submitted=input.transfer(new AnalysisInput.RowTarget(){
                @Override public void begin(int w,int h){raster[0]=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888,false,ColorSpace.get(ColorSpace.Named.SRGB));}
                @Override public void row(int y,int[] argb){raster[0].setPixels(argb,0,input.width,0,y,input.width,1);}
                @Override public void complete(){}
                @Override public void abort(){if(raster[0]!=null){raster[0].recycle();raster[0]=null;}}
            });
            AnalysisInput.Descriptor descriptor=submitted.descriptor;
            if(descriptor!=input.descriptor())throw new IllegalStateException("Analysis transfer identity changed");
            moved=OwnedBitmapSubmission.adopt(raster[0],input.capacity);raster[0]=null;
            StockStillAnalysis.Request request=new StockStillAnalysis.Request(descriptor.nonce,descriptor.sensorTimestampNs,
                descriptor.sourceSha256,descriptor.settingsSha256,expectedFeatures,captureDiagnostic);
            result=StockStillAnalysis.runOwned(lease,moved,newWorkspace,timeoutMillis,request,setup);
            if(result==null || result.request!=request || !result.observationsComplete ||
                    !descriptor.proxySha256.equals(result.submittedProxySha256) || result.nativeEvidence==null ||
                    result.nativeEvidence.width!=descriptor.width || result.nativeEvidence.height!=descriptor.height ||
                    !result.nativeEvidence.callbacksObserved || result.scriptExports==null || result.scriptExports.nonce!=descriptor.nonce)
                throw new IllegalStateException("Same-capture analysis provenance or complete callbacks not established");
            if(captureDiagnostic && (result.renderedDiagnostic==null || result.renderedDiagnostic.nonce!=descriptor.nonce ||
                    result.renderedDiagnostic.width!=descriptor.width || result.renderedDiagnostic.height!=descriptor.height))
                throw new IllegalStateException("Diagnostic raster belongs to a different submission/grid");
            delivered=true;return new BoundOutcome(descriptor,result);
        } finally {
            if(!delivered && result!=null && result.renderedDiagnostic!=null)result.renderedDiagnostic.close();
            // close() releases only untransferred storage; the probe alone releases or quarantines its claim.
            if(moved!=null)moved.close();
            if(raster[0]!=null && !raster[0].isRecycled())raster[0].recycle();
        }
    }

}
