package android.widget;
public class TextView extends android.view.View {
 public CharSequence text;
 public int textChanges;
 public TextView(android.content.Context context){super(context);}
 public void setText(CharSequence text){this.text=text;textChanges++;}
 public void setTextSize(float size){}
 public void setTextColor(int color){}
}
