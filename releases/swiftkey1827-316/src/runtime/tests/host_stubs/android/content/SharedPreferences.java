package android.content;
public interface SharedPreferences {
    boolean getBoolean(String k,boolean def);
    default int getInt(String k,int def){return def;}
    default Editor edit(){throw new UnsupportedOperationException();}
    interface Editor {
        Editor putBoolean(String k,boolean value);
        default Editor putInt(String k,int value){throw new UnsupportedOperationException();}
        void apply();
    }
}

