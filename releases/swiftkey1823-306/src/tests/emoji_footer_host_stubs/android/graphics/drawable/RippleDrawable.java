package android.graphics.drawable; import android.content.res.ColorStateList;
public class RippleDrawable extends Drawable { public ColorStateList color;public RippleDrawable(ColorStateList color,Drawable content,Drawable mask){this.color=color;}public void setColor(ColorStateList color){this.color=color;} }
