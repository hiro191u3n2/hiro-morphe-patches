package android.view;
public class View {public interface OnClickListener {void onClick(View view);} private OnClickListener listener;
 public void setOnClickListener(OnClickListener value){listener=value;} public boolean performClick(){if(listener==null)return false;listener.onClick(this);return true;}}
