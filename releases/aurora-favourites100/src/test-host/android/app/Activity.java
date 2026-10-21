package android.app;
import android.content.Context;
import android.content.Intent;
public class Activity extends Context {
 private Intent intent=new Intent(Intent.ACTION_MAIN);
 private Object configuration;
 private boolean finishing,destroyed;
 public Intent getIntent() { return intent; }
 public void setIntent(Intent value) { intent=value; }
 public boolean isFinishing() { return finishing; }
 public boolean isDestroyed() { return destroyed; }
 public Object getLastNonConfigurationInstance() { return configuration; }
 public void setLastNonConfigurationInstance(Object value) { configuration=value; }
 public void setFinishing(boolean value) { finishing=value; }
 public void setDestroyed(boolean value) { destroyed=value; }
}
