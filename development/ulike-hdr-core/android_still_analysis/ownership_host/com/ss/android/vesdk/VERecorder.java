package com.ss.android.vesdk;
import android.view.Surface;
import com.hiro.ulike.hdr.stillanalysis.RecorderAdmission;
/** Host-only lifecycle fixture exercising the production admission ledger. */
public final class VERecorder {
    public final TECameraVideoRecorder b=new TECameraVideoRecorder();
    public int stops,starts;
    public VERecorder(){
        RecorderAdmission.constructed(RecorderAdmission.beforeConstruction(),this);
        RecorderAdmission.nativeInitialized(RecorderAdmission.beforeNativeInit(b.mRecordPresenter.mfbInvoker),1,0);
    }
    public void stopPreviewAsync(VEListener.VECallListener listener){
        RecorderAdmission.beforeApplicationLifecycle(this,"stopPreviewAsync");
        stops++;b.state=1;listener.onDone(0);
    }
    public void startPreviewAsync(Surface surface,VEListener.VECallListener listener){
        RecorderAdmission.beforeApplicationLifecycle(this,"startPreviewAsync");
        starts++;
        RecorderAdmission.Ticket ticket=RecorderAdmission.beforeNativeInit(b.mRecordPresenter.mfbInvoker);
        b.mRecordPresenter.mfbInvoker.handle=2;b.state=2;
        RecorderAdmission.nativeInitialized(ticket,2,0);listener.onDone(0);
    }
}
