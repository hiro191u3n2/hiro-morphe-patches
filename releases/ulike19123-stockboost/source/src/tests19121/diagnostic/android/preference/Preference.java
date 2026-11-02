package android.preference;

public class Preference {
    public interface OnPreferenceClickListener {boolean onPreferenceClick(Preference value);}
    public String key;public CharSequence title,summary;
    public int summaryAttempts,summaryWrites;
    public boolean failSummary;
    public OnPreferenceClickListener click;
    public String getKey(){return key;}
    public CharSequence getTitle(){return title;}
    public void setTitle(CharSequence value){title=value;}
    public void setSummary(CharSequence value){summaryAttempts++;if(failSummary)throw new IllegalStateException("fixture summary failure");summary=value;summaryWrites++;}
    public void setOnPreferenceClickListener(OnPreferenceClickListener value){click=value;}
}
