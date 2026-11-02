package android.app;
public class Activity extends android.content.ContextWrapper {
 public boolean finishing,destroyed;
 public android.view.Window window=new android.view.Window(this);
 public Activity(){super(new android.content.Context());}
 public boolean isFinishing(){return finishing;}
 public boolean isDestroyed(){return destroyed;}
 public android.view.Window getWindow(){return window;}
}
