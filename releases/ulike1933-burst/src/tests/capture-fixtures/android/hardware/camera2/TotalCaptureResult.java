package android.hardware.camera2;
import java.util.*;
public final class TotalCaptureResult extends CaptureResult {
    public final Map<String,CaptureResult> physical=new HashMap<String,CaptureResult>();
    public Map<String,CaptureResult> getPhysicalCameraResults(){return physical;}
}
