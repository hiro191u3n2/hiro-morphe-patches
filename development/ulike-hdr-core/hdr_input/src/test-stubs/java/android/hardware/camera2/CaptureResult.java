package android.hardware.camera2;
import java.util.HashMap;
import java.util.Map;
public class CaptureResult {
    public static final class Key<T> {}
    public static final Key<Long> SENSOR_TIMESTAMP=new Key<>(), SENSOR_EXPOSURE_TIME=new Key<>();
    public static final Key<Integer> SENSOR_SENSITIVITY=new Key<>();
    public static final Key<String> LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID=new Key<>();
    private final Map<Key<?>,Object> data=new HashMap<>();
    @SuppressWarnings("unchecked") public <T> T get(Key<T> key) { return (T)data.get(key); }
    public <T> void putForTest(Key<T> key,T value) { data.put(key,value); }
}
