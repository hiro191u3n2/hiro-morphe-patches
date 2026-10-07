package android.hardware.camera2;
import android.graphics.Rect;
import android.hardware.camera2.params.RggbChannelVector;
import java.util.*;
public class CaptureResult extends CameraMetadata {
    public static final class Key<T>{}
    public static final Key<Long> SENSOR_TIMESTAMP=new Key<Long>(),SENSOR_EXPOSURE_TIME=new Key<Long>(),SENSOR_FRAME_DURATION=new Key<Long>();
    public static final Key<Integer> SENSOR_SENSITIVITY=new Key<Integer>(),LENS_STATE=new Key<Integer>(),FLASH_MODE=new Key<Integer>(),FLASH_STATE=new Key<Integer>();
    public static final Key<Float> LENS_FOCAL_LENGTH=new Key<Float>(),LENS_FOCUS_DISTANCE=new Key<Float>(),CONTROL_ZOOM_RATIO=new Key<Float>();
    public static final Key<Rect> SCALER_CROP_REGION=new Key<Rect>();public static final Key<String> LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID=new Key<String>();
    public static final Key<RggbChannelVector> COLOR_CORRECTION_GAINS=new Key<RggbChannelVector>();
    public final Map<Key<?>,Object> values=new HashMap<Key<?>,Object>();
    public <T>void put(Key<T> k,T v){values.put(k,v);}
    @SuppressWarnings("unchecked")public <T>T get(Key<T> k){return (T)values.get(k);}
    public CaptureRequest request;public CaptureRequest getRequest(){return request;}
}
