package android.hardware.camera2;
import java.util.HashMap;
import java.util.Map;
public class TotalCaptureResult extends CaptureResult {
    public final Map<String,CaptureResult> physical=new HashMap<>();
    private final CaptureRequest request;
    public TotalCaptureResult(CaptureRequest request) { this.request=request; }
    public CaptureRequest getRequest() { return request; }
    public long getFrameNumber() { return 10; }
    public Map<String,CaptureResult> getPhysicalCameraResults() { return physical; }
}
