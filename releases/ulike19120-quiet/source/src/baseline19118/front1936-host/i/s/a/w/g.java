package i.s.a.w;
import android.hardware.camera2.CameraDevice;
import com.ss.android.ttvecamera.TECameraSettings;
public class g {
    public int I=2;
    public CameraDevice K=new CameraDevice();
    public i.s.a.w.h0.b M;
    public TECameraSettings b;
    public i.s.a.w.l0.c g=new i.s.a.w.l0.c();
    public boolean c;
    public int sessionCleanups,cleanupResult;
    public RuntimeException cleanupFailure;
    public Runnable duringCleanup;
    public int p0() {
        if(android.os.Looper.myLooper()!=q.INSTANCE.mHandler.getLooper())throw new AssertionError("cleanup off native handler");
        sessionCleanups++;
        if(cleanupFailure!=null)throw cleanupFailure;
        if(cleanupResult!=0)return cleanupResult;
        if(M==null)return -1;
        M.d=null;
        if(duringCleanup!=null)duringCleanup.run();
        return 0;
    }
    public i.s.a.w.l0.c U(){return g;}
}
