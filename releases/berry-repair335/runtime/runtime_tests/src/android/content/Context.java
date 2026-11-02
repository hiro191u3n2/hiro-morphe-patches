package android.content;
public class Context {
 public static final String CLIPBOARD_SERVICE="clipboard";
 public final android.content.res.Resources resources=new android.content.res.Resources();
 public final ClipboardManager clipboard=new ClipboardManager();
 public android.content.res.Resources getResources(){return resources;}
 public Object getSystemService(String name){return CLIPBOARD_SERVICE.equals(name)?clipboard:null;}
}
