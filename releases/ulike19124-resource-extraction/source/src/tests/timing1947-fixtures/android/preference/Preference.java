package android.preference;
public class Preference {
 private String key;private CharSequence title,summary;private OnPreferenceClickListener listener;
 public interface OnPreferenceClickListener {boolean onPreferenceClick(Preference p);}
 public String getKey(){return key;}public void setKey(String s){key=s;}
 public CharSequence getTitle(){return title;}public void setTitle(CharSequence s){title=s;}
 public CharSequence getSummary(){return summary;}public void setSummary(CharSequence s){summary=s;}
 public void setOnPreferenceClickListener(OnPreferenceClickListener l){listener=l;}
 public boolean click(){return listener!=null && listener.onPreferenceClick(this);}
}
