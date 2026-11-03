package android.view;
import java.util.ArrayList;
public class ViewGroup extends View {
 public static class LayoutParams {public static final int MATCH_PARENT=-1,WRAP_CONTENT=-2;public int width,height;public LayoutParams(int w,int h){width=w;height=h;}}
 public static class MarginLayoutParams extends LayoutParams {public int leftMargin,topMargin,rightMargin,bottomMargin;public MarginLayoutParams(int w,int h){super(w,h);}}
 public final ArrayList<View> children=new ArrayList<View>();
 public void addView(View view,LayoutParams params){ViewTreeObserver.assertNotDrawing();children.add(view);view.parent=this;view.layoutParams=params;}
 public void addView(View view){addView(view,view.layoutParams);}
 public void removeView(View view){ViewTreeObserver.assertNotDrawing();children.remove(view);view.parent=null;view.detach();}
 public int getChildCount(){return children.size();} public View getChildAt(int i){return children.get(i);}
}
