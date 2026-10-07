package android.hardware.camera2;
import java.util.*;import android.util.Range;
public final class CameraCharacteristics extends CameraMetadata {
    public static final class Key<T>{}
    public static final Key<Boolean> CONTROL_AWB_LOCK_AVAILABLE=new Key<Boolean>(),CONTROL_AE_LOCK_AVAILABLE=new Key<Boolean>();
    public static final Key<int[]> REQUEST_AVAILABLE_CAPABILITIES=new Key<int[]>(),CONTROL_AE_AVAILABLE_MODES=new Key<int[]>(),CONTROL_AF_AVAILABLE_MODES=new Key<int[]>();
    public static final Key<Range<Long>> SENSOR_INFO_EXPOSURE_TIME_RANGE=new Key<Range<Long>>();
    public static final Key<Range<Integer>> SENSOR_INFO_SENSITIVITY_RANGE=new Key<Range<Integer>>();
    public static final Key<Long> SENSOR_INFO_MAX_FRAME_DURATION=new Key<Long>();
    public final Set<String> ids=new HashSet<String>();public final List<CaptureRequest.Key<?>> keys=new ArrayList<CaptureRequest.Key<?>>();
    public final Map<Key<?>,Object> values=new HashMap<Key<?>,Object>();
    public <T>void put(Key<T> k,T v){values.put(k,v);}
    @SuppressWarnings("unchecked")public <T>T get(Key<T> k){return (T)values.get(k);}
    public Set<String> getPhysicalCameraIds(){return ids;}public List<CaptureRequest.Key<?>> getAvailablePhysicalCameraRequestKeys(){return keys;}
}
