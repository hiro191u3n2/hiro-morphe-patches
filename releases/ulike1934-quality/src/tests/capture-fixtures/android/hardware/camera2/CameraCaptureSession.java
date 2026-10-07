package android.hardware.camera2;
import java.util.*;import android.os.Handler;import android.view.Surface;
public final class CameraCaptureSession {
    public final List<Call> calls=new ArrayList<Call>();public boolean fail;
    public static class CaptureCallback {
        public void onCaptureStarted(CameraCaptureSession s,CaptureRequest r,long t,long f){}
        public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest r,TotalCaptureResult x){}
        public void onCaptureFailed(CameraCaptureSession s,CaptureRequest r,CaptureFailure x){}
        public void onCaptureBufferLost(CameraCaptureSession s,CaptureRequest r,Surface x,long f){}
        public void onCaptureSequenceAborted(CameraCaptureSession s,int n){}
    }
    public static final class Call {public final CaptureRequest request;public final CaptureCallback callback;public final Handler handler;Call(CaptureRequest r,CaptureCallback c,Handler h){request=r;callback=c;handler=h;}}
    public int capture(CaptureRequest r,CaptureCallback c,Handler h){if(fail)throw new IllegalStateException("capture failure");calls.add(new Call(r,c,h));return calls.size();}
}
