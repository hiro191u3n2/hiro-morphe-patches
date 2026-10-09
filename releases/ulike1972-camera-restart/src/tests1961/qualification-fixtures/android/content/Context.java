package android.content;
public class Context {
 public static final int MODE_PRIVATE=0;
 public String packageName="fixture.ulike",versionName="fixture-1";public long code=1;
 public com.hiro.ulike.Qualification1961Test.MemoryPreferences prefs;
 public Context getApplicationContext(){return this;}public String getPackageName(){return packageName;}
 public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager(this);}
 public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
}
