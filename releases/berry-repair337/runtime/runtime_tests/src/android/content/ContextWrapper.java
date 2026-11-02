package android.content;
public class ContextWrapper extends Context {
 public Context base;
 public ContextWrapper(Context base){this.base=base;}
 public Context getBaseContext(){return base;}
}
