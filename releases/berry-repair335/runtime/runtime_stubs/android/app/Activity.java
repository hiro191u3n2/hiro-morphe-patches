package android.app;
public class Activity extends android.content.ContextWrapper {
 public Activity(){super(null);}
 public boolean isFinishing(){return false;}
 public boolean isDestroyed(){return false;}
 public android.view.Window getWindow(){return null;}
}
