package androidx.preference;
public class Preference {
    public android.content.Context a;public String l;public boolean s=true;public oa.j e;public oa.k f;public String title,summary;public boolean failSummary;
    public Preference(android.content.Context c){a=c;}
    public Preference(android.content.Context c,android.util.AttributeSet attrs){a=c;}
    public void B(CharSequence v){title=v.toString();}
    public void A(CharSequence v){if(failSummary)throw new IllegalStateException("PRIVATE_SUMMARY_FAILURE");summary=v.toString();}
}

