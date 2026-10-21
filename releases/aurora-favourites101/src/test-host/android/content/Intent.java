package android.content;
import android.net.Uri;
import android.os.Bundle;
public class Intent {
 public static final String ACTION_MAIN="android.intent.action.MAIN";
 public static final String ACTION_VIEW="android.intent.action.VIEW";
 public static final String ACTION_SEND="android.intent.action.SEND";
 public static final String CATEGORY_LAUNCHER="android.intent.category.LAUNCHER";
 private String action;
 private Uri data;
 private Bundle extras;
 public Intent() { }
 public Intent(String action) { this.action=action; }
 public String getAction() { return action; }
 public Intent setAction(String value) { action=value;return this; }
 public Uri getData() { return data; }
 public Intent setData(Uri value) { data=value;return this; }
 public Bundle getExtras() { return extras; }
 public boolean hasExtra(String key) { return extras!=null&&extras.containsKey(key); }
 public Intent putExtra(String key,String value) { if(extras==null)extras=new Bundle();extras.putString(key,value);return this; }
 public Intent putExtra(String key,boolean value) { if(extras==null)extras=new Bundle();extras.putBoolean(key,value);return this; }
}
