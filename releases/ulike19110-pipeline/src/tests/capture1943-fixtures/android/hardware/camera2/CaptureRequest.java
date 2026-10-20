package android.hardware.camera2;
import android.view.Surface;
import java.util.*;
public final class CaptureRequest extends CameraMetadata {
    public static final class Key<T> { public final String name;public Key(String n){name=n;} }
    public static final Key<Integer> CONTROL_CAPTURE_INTENT=new Key<Integer>("intent"),CONTROL_AF_TRIGGER=new Key<Integer>("af_trigger"),CONTROL_AE_PRECAPTURE_TRIGGER=new Key<Integer>("ae_trigger"),CONTROL_MODE=new Key<Integer>("control"),CONTROL_AE_MODE=new Key<Integer>("ae_mode"),CONTROL_AF_MODE=new Key<Integer>("af_mode"),SENSOR_SENSITIVITY=new Key<Integer>("iso");
    public static final Key<Boolean> CONTROL_ENABLE_ZSL=new Key<Boolean>("zsl"),CONTROL_AWB_LOCK=new Key<Boolean>("awb_lock"),CONTROL_AE_LOCK=new Key<Boolean>("ae_lock");
    public static final Key<Long> SENSOR_EXPOSURE_TIME=new Key<Long>("exposure"),SENSOR_FRAME_DURATION=new Key<Long>("duration");
    public static final Key<Float> LENS_FOCUS_DISTANCE=new Key<Float>("focus");
    public final Map<Key<?>,Object> values; public final Map<Key<?>,Object> physical;public final Surface target;public final Object tag;public boolean reprocess;
    private CaptureRequest(Builder b){values=new HashMap<Key<?>,Object>(b.values);physical=new HashMap<Key<?>,Object>(b.physical);target=b.target;tag=b.tag;}
    @SuppressWarnings("unchecked")public <T>T get(Key<T> k){return (T)values.get(k);}
    public List<Key<?>> getKeys(){return new ArrayList<Key<?>>(values.keySet());}
    public boolean isReprocess(){return reprocess;}
    public Object getTag(){return tag;}
    public static final class Builder {
        public final Map<Key<?>,Object> values=new HashMap<Key<?>,Object>(),physical=new HashMap<Key<?>,Object>();public Surface target;public Object tag;
        public <T>void set(Key<T> k,T v){values.put(k,v);}
        @SuppressWarnings("unchecked")public <T>T get(Key<T> k){return (T)values.get(k);}
        public <T>void setPhysicalCameraKey(Key<T> k,T v,String id){physical.put(k,v);}
        public void setTag(Object t){tag=t;}public void addTarget(Surface s){target=s;}
        public CaptureRequest build(){return new CaptureRequest(this);}
    }
}

