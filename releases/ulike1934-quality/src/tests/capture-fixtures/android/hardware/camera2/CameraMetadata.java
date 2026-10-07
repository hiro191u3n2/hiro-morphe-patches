package android.hardware.camera2;
public class CameraMetadata {
    public static final int CONTROL_CAPTURE_INTENT_STILL_CAPTURE=2,CONTROL_CAPTURE_INTENT_PREVIEW=1;
    public static final int CONTROL_AF_TRIGGER_IDLE=0,CONTROL_AE_PRECAPTURE_TRIGGER_IDLE=0;
    public static final int CONTROL_MODE_AUTO=1,CONTROL_AE_MODE_OFF=0,CONTROL_AE_MODE_ON=1;
    public static final int CONTROL_AF_MODE_OFF=0,CONTROL_AF_MODE_AUTO=1;
    public static final int LENS_STATE_STATIONARY=0,LENS_STATE_MOVING=1;
    public static final int FLASH_MODE_OFF=0,FLASH_MODE_SINGLE=1,FLASH_MODE_TORCH=2;
    public static final int FLASH_STATE_FIRED=3,FLASH_STATE_PARTIAL=4;
    public static final int REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR=1;
}
