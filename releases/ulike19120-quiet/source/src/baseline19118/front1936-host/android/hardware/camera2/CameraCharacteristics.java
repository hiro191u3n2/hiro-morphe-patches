package android.hardware.camera2;
public class CameraCharacteristics {
    public static final class Key<T> {}
    public static final Key<Integer> LENS_FACING=new Key<>();
    public int facing;
    @SuppressWarnings("unchecked") public <T> T get(Key<T> key){return (T)Integer.valueOf(facing);}
}
