package android.content;
import android.content.pm.PackageManager;
public abstract class Context {
 public static final int MODE_PRIVATE=0;
 public Context getApplicationContext(){return this;}
 public abstract String getPackageName();
 public abstract PackageManager getPackageManager();
 public abstract SharedPreferences getSharedPreferences(String name,int mode);
}
