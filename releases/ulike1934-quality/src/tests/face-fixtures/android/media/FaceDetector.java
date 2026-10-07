package android.media;
import android.graphics.Bitmap;import android.graphics.PointF;
public final class FaceDetector {
 public static Face[] scripted=new Face[0]; public static int failMode,calls;
 private final int w,h;
 public FaceDetector(int w,int h,int count){if(w%2!=0)throw new AssertionError("even width");this.w=w;this.h=h;}
 public int findFaces(Bitmap b,Face[]out){calls++;if(b.getConfig()!=Bitmap.Config.RGB_565||b.getWidth()!=w||b.getHeight()!=h)throw new AssertionError("detector bitmap contract");if(failMode==1)throw new RuntimeException("scripted");if(failMode==2)throw new UnsatisfiedLinkError("scripted");if(failMode==3)throw new OutOfMemoryError("scripted");System.arraycopy(scripted,0,out,0,Math.min(scripted.length,out.length));return scripted.length;}
 public static final class Face {
  public static final int EULER_Z=2,EULER_Y=1;private final float x,y,d,roll,yaw,confidence;
  public Face(float x,float y,float d,float roll,float yaw,float confidence){this.x=x;this.y=y;this.d=d;this.roll=roll;this.yaw=yaw;this.confidence=confidence;}
  public void getMidPoint(PointF p){p.x=x;p.y=y;}public float eyesDistance(){return d;}public float pose(int index){return index==EULER_Z?roll:yaw;}public float confidence(){return confidence;}
 }
}
