package android.content;
public class ClipData {
 public CharSequence label,text;
 public static ClipData newPlainText(CharSequence label,CharSequence text){ClipData c=new ClipData();c.label=label;c.text=text;return c;}
}
