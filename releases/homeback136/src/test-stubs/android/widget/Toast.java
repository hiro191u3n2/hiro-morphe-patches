package android.widget;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;
public final class Toast {
    public static final int LENGTH_LONG=1,LENGTH_SHORT=0;
    public static final List<String> SHOWN=new ArrayList<String>();
    public static boolean fail;
    private final String text;
    private Toast(CharSequence text){this.text=String.valueOf(text);}
    public static Toast makeText(Context context,CharSequence text,int duration){if(fail)throw new IllegalStateException("toast unavailable");return new Toast(text);}
    public void show(){SHOWN.add(text);}
}
