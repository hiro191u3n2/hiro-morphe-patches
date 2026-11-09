package android.widget;
import android.view.ViewGroup;
import android.content.Context;
public class LinearLayout extends ViewGroup {
    public LinearLayout(Context c) {super(c);}
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public float weight;
        public int gravity=-1;
        public LayoutParams(int width,int height,float weight) {super(width,height);this.weight=weight;}
        public LayoutParams(int width,int height) {super(width,height);}
    }
}
