package android.app;import android.content.*;import android.view.View;import android.widget.*;import java.util.*;
public class AlertDialog implements DialogInterface {
 public static final int BUTTON_NEUTRAL=-3;public static volatile AlertDialog last;
 public final Map<String,DialogInterface.OnClickListener> clicks=new HashMap<String,DialogInterface.OnClickListener>();
 public final Map<String,String> labels=new HashMap<String,String>();public String title,message;public CharSequence[] items;public boolean dismissed;
 public final TextView messageView=new TextView();public final Button neutral=new Button();private DialogInterface.OnClickListener itemListener;
 public View findViewById(int id){return id==android.R.id.message?messageView:null;}public Button getButton(int which){return which==BUTTON_NEUTRAL?neutral:null;}
 public void clickItem(int which){if(itemListener==null)throw new AssertionError("missing items");itemListener.onClick(this,which);}
 public void dismiss(){dismissed=true;}public void click(String key){DialogInterface.OnClickListener l=clicks.get(key);if(l==null)throw new AssertionError("missing button "+key);l.onClick(this,0);}
 public static class Builder {private final AlertDialog dialog=new AlertDialog();public Builder(Context c){}
 public Builder setItems(CharSequence[] labels,DialogInterface.OnClickListener listener){dialog.items=labels;dialog.itemListener=listener;return this;}
 public Builder setTitle(CharSequence s){dialog.title=s.toString();return this;}public Builder setMessage(CharSequence s){dialog.message=s.toString();return this;}
 public Builder setPositiveButton(CharSequence s,DialogInterface.OnClickListener l){dialog.clicks.put("positive",l);dialog.labels.put("positive",s.toString());return this;}
 public Builder setNegativeButton(CharSequence s,DialogInterface.OnClickListener l){dialog.clicks.put("negative",l);dialog.labels.put("negative",s.toString());return this;}
 public Builder setNeutralButton(CharSequence s,DialogInterface.OnClickListener l){dialog.clicks.put("neutral",l);dialog.labels.put("neutral",s.toString());return this;}
 public AlertDialog create(){last=dialog;return dialog;}public AlertDialog show(){last=dialog;return dialog;}}}
