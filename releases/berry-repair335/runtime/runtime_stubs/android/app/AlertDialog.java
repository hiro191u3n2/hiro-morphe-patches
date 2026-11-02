package android.app;
public class AlertDialog implements android.content.DialogInterface {
 public void show(){}
 public void dismiss(){}
 public static class Builder {
  public Builder(android.content.Context context){}
  public Builder setTitle(CharSequence title){return this;}
  public Builder setMessage(CharSequence message){return this;}
  public Builder setPositiveButton(CharSequence text,android.content.DialogInterface.OnClickListener listener){return this;}
  public Builder setNegativeButton(CharSequence text,android.content.DialogInterface.OnClickListener listener){return this;}
  public Builder setNeutralButton(CharSequence text,android.content.DialogInterface.OnClickListener listener){return this;}
  public AlertDialog create(){return null;}
 }
}
