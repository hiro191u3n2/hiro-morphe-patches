package android.graphics;
public final class Canvas {
 public static boolean failDraw;public static float[] lastMatrix;public static Bitmap lastTarget;
 public Canvas(Bitmap b){lastTarget=b;}
 public void drawBitmap(Bitmap input,Matrix m,Paint p){if(failDraw)throw new IllegalStateException("scripted");lastMatrix=m.values.clone();}
}
