package android.view;
public class Window {
 public View decor;
 public Window(android.content.Context context){decor=new android.widget.FrameLayout(context);}
 public View getDecorView(){return decor;}
}
