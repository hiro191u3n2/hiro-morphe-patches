package android.view;
import java.util.ArrayList;
public class ViewTreeObserver {
 public interface OnPreDrawListener {boolean onPreDraw();}
 public interface OnDrawListener {void onDraw();}
 public boolean alive=true;
 public static boolean drawing;
 public final ArrayList<OnPreDrawListener> preDraws=new ArrayList<OnPreDrawListener>();
 public final ArrayList<OnDrawListener> draws=new ArrayList<OnDrawListener>();
 public boolean isAlive(){return alive;}
 public void addOnPreDrawListener(OnPreDrawListener l){preDraws.add(l);}
 public void removeOnPreDrawListener(OnPreDrawListener l){preDraws.remove(l);}
 public void addOnDrawListener(OnDrawListener l){assertNotDrawing();draws.add(l);}
 public void removeOnDrawListener(OnDrawListener l){assertNotDrawing();draws.remove(l);}
 public boolean dispatchOnPreDraw(){boolean allow=true;for(OnPreDrawListener l:new ArrayList<OnPreDrawListener>(preDraws))allow&=l.onPreDraw();return allow;}
 public void dispatchOnDraw(){drawing=true;try{for(OnDrawListener l:new ArrayList<OnDrawListener>(draws))l.onDraw();}finally{drawing=false;}}
 public static void assertNotDrawing(){if(drawing)throw new IllegalStateException("View/observer mutation inside onDraw");}
}
