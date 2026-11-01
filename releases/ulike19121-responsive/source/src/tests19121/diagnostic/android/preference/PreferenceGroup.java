package android.preference;

public class PreferenceGroup extends Preference {
    public final java.util.ArrayList<Preference> children=new java.util.ArrayList<Preference>();
    public int getPreferenceCount(){return children.size();}
    public Preference getPreference(int index){return children.get(index);}
}
