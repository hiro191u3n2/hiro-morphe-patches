package android.media;
import android.graphics.Bitmap;
import android.graphics.PointF;
public final class FaceDetector {
 public FaceDetector(int w,int h,int n){throw new AssertionError();}
 public int findFaces(Bitmap b,Face[] f){throw new AssertionError();}
 public static final class Face {
  public static final int EULER_Z=2,EULER_Y=1;
  public void getMidPoint(PointF p){throw new AssertionError();}
  public float eyesDistance(){throw new AssertionError();}
  public float pose(int p){throw new AssertionError();}
  public float confidence(){throw new AssertionError();}
 }
}
