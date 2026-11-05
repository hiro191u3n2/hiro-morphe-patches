package android.widget;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;
public class Toast {
    public static final int LENGTH_SHORT=0;
    public static final List<String> messages=new ArrayList<String>();
    private final String text;
    private Toast(String value){text=value;}
    public static Toast makeText(Context context,CharSequence text,int length){return new Toast(text.toString());}
    public void show(){messages.add(text);}
    public static void reset(){messages.clear();}
}
