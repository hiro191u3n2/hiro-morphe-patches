package android.view;
import android.content.Context;
public class View {
    private final Context context;
    public View(Context c) { context=c; }
    public Context getContext() { return context; }
}
