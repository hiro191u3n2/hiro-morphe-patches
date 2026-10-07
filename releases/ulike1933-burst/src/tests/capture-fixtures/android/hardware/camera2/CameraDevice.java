package android.hardware.camera2;
import java.util.*;
public final class CameraDevice {
    public static final int TEMPLATE_STILL_CAPTURE=2;public final String id;public boolean fail;
    public CameraDevice(String i){id=i;}public String getId(){return id;}
    public CaptureRequest.Builder createCaptureRequest(int template){if(fail)throw new IllegalStateException("closed device");return new CaptureRequest.Builder();}
    public CaptureRequest.Builder createCaptureRequest(int template,Set<String> ids){return createCaptureRequest(template);}
}
