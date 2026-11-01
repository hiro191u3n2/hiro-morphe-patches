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
    public int stops,stopResult;
    public Runnable duringStop;
    public int stop(k client,boolean sync){
        if(client!=mCameraClient)return -108;
        if(android.os.Looper.myLooper()!=mHandler.getLooper())throw new AssertionError("stop off native handler");
        if(stopResult!=0)return stopResult;
        if(mCurrentCameraState==2)return 0;
        if(mCurrentCameraState!=3)return -105;
        mCurrentCameraState=2;stops++;
        if(mCameraInstance instanceof g){g c=(g)mCameraInstance;c.c=false;if(c.M!=null)c.M.d=null;}
        if(mCameraInstance instanceof d)((d)mCameraInstance).c=false;
        if(duringStop!=null)duringStop.run();
        return 0;
    }
    public int getCameraState(){return mCurrentCameraState;}
    public int getCameraState(boolean ignored){return mCurrentCameraState;}
    public boolean isCameraSwitchState(){return mIsCameraSwitchState;}
}
