package android.preference;
import java.util.ArrayList;
public class PreferenceGroup extends Preference {
 private final ArrayList<Preference> children=new ArrayList<Preference>();
 public void addPreference(Preference p){children.add(p);}public int getPreferenceCount(){return children.size();}public Preference getPreference(int i){return children.get(i);}
}
