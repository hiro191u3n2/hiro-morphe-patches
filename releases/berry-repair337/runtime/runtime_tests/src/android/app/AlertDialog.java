package android.app;
public class AlertDialog implements android.content.DialogInterface {
 public static AlertDialog latest;
 public static int shows;
 public CharSequence title,message;
 public android.content.DialogInterface.OnClickListener positive,negative,neutral;
 public boolean dismissed;
 public void show(){latest=this;shows++;}
 public void setMessage(CharSequence message){this.message=message;}
 public void dismiss(){dismissed=true;}
 public void clickPositive(){if(positive!=null)positive.onClick(this,-1);}
 public void clickNeutral(){if(neutral!=null)neutral.onClick(this,-3);}
 public static class Builder {
  private final AlertDialog dialog=new AlertDialog();
  public Builder(android.content.Context context){}
  public Builder setTitle(CharSequence title){dialog.title=title;return this;}
  public Builder setMessage(CharSequence message){dialog.message=message;return this;}
  public Builder setPositiveButton(CharSequence text,android.content.DialogInterface.OnClickListener listener){dialog.positive=listener;return this;}
  public Builder setNegativeButton(CharSequence text,android.content.DialogInterface.OnClickListener listener){dialog.negative=listener;return this;}
  public Builder setNeutralButton(CharSequence text,android.content.DialogInterface.OnClickListener listener){dialog.neutral=listener;return this;}
  public AlertDialog create(){return dialog;}
 }
}
