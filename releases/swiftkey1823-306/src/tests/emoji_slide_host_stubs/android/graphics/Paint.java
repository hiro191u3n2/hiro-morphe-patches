package android.graphics;
public class Paint {
 public static final int ANTI_ALIAS_FLAG=1;public enum Style {STROKE,FILL}public enum Cap {ROUND,BUTT,SQUARE}public enum Join {ROUND,MITER,BEVEL}public enum Align {LEFT,CENTER,RIGHT}
 public int color,alpha=255;public float strokeWidth,textSize;public Style style=Style.FILL;public Align align=Align.LEFT;
 public Paint(){}public Paint(int flags){}public void setColor(int v){color=v;}public int getColor(){return color;}public void setStrokeWidth(float v){strokeWidth=v;}public void setStyle(Style v){style=v;}public void setStrokeCap(Cap v){}public void setStrokeJoin(Join v){}public void setAlpha(int v){alpha=v;}public void setColorFilter(ColorFilter f){}public void setTextSize(float v){textSize=v;}public float getTextSize(){return textSize;}public void setTextAlign(Align v){align=v;}public Typeface setTypeface(Typeface v){return v;}public float ascent(){return -textSize*.8f;}public float descent(){return textSize*.2f;}public float measureText(String s){return s.codePointCount(0,s.length())*textSize*.5f;}
 public static class FontMetrics {public float top,ascent,descent,bottom,leading;}
 public FontMetrics getFontMetrics(){FontMetrics f=new FontMetrics();f.ascent=ascent();f.descent=descent();f.top=f.ascent;f.bottom=f.descent;return f;}
 public void setShadowLayer(float r,float dx,float dy,int c){}public void clearShadowLayer(){}
}
