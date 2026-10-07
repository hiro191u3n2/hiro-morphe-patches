package i.s.a.w;
import android.os.Handler;
import com.ss.android.ttvecamera.TECameraSettings;
public class q {
    public static final q INSTANCE=new q();
    public Object mCameraClient,mCameraInstance;
    public TECameraSettings mCameraSettings;
    public i.s.a.w.l0.c mProviderManager=new i.s.a.w.l0.c();
    public Handler mHandler=new Handler();
    public final Object mStateLock=new Object(),mSurfaceTextureLock=new Object();
    public int mCurrentCameraState=2;
    public boolean mIsCameraPendingClose,mIsCameraSwitchState,mHandlerDestroyed,mOnBackGround;
    public boolean mIsInitialized=true,mIsForegroundVisible=true,mStartPreviewError,mIsCameraProviderChanged;
    public boolean mbNeedReleaseSurfaceTexture;
    public int getCameraState(){return mCurrentCameraState;}
    public int getCameraState(boolean ignored){return mCurrentCameraState;}
    public boolean isCameraSwitchState(){return mIsCameraSwitchState;}
}
