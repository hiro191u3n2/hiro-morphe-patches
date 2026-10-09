package android.app;
public class Activity extends android.content.Context {
 public int dispatchCount;
 public void runOnUiThread(Runnable r){dispatchCount++;r.run();}
}
