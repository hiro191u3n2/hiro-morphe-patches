package com.ss.android.vesdk;
import android.view.Surface;
import com.hiro.ulike.hdr.stillanalysis.RecorderAdmission;
import com.ss.android.medialib.RecordInvoker;
/** Host-only state transition fixture, no native execution. */
public final class TECameraVideoRecorder {
    public static final class Presenter { public final RecordInvoker mfbInvoker=new RecordInvoker(); }
    public final Presenter mRecordPresenter=new Presenter();
    public Surface g1=new Surface();
    public int state=2;
    public int getRecordStatus(){return state;}
    public void releaseInteralRecorder(){
        RecorderAdmission.Ticket ticket=RecorderAdmission.beforeNativeUninit(mRecordPresenter.mfbInvoker);
        mRecordPresenter.mfbInvoker.handle=0;state=0;
        RecorderAdmission.nativeUninitialized(ticket,0);
    }
}
