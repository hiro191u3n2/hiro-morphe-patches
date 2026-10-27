package android.view;
public class ViewOverlay {
 public final java.util.List<android.graphics.drawable.Drawable> drawables=new java.util.ArrayList<>();
 public int adds,removes;public boolean failAdd,failRemove;
 public Runnable onRemove;
 public void add(android.graphics.drawable.Drawable value){if(failAdd)throw new IllegalStateException("overlay add failure");if(!drawables.contains(value)){drawables.add(value);adds++;}}
 public void remove(android.graphics.drawable.Drawable value){if(failRemove)throw new IllegalStateException("overlay remove failure");if(drawables.remove(value))removes++;if(onRemove!=null)onRemove.run();}
 public void clear(){drawables.clear();}
}
