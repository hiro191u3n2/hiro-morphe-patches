package android.graphics;
public class Canvas {
 public static class Text {public final String text;public final float x,y,size,width;public final int color;Text(String t,float x,float y,Paint p){text=t;this.x=x;this.y=y;size=p.textSize;width=p.measureText(t);color=p.color;}}
 public static class Box {public final float left,top,right,bottom;public final int color;public final Paint.Style style;Box(float l,float t,float r,float b,Paint p){left=l;top=t;right=r;bottom=b;color=p.color;style=p.style;}}
 public final java.util.List<Text> texts=new java.util.ArrayList<>();public final java.util.List<Box> boxes=new java.util.ArrayList<>();public int lines;
 public void drawText(String t,float x,float y,Paint p){texts.add(new Text(t,x,y,p));}
 public void drawRoundRect(RectF r,float rx,float ry,Paint p){boxes.add(new Box(r.left,r.top,r.right,r.bottom,p));}
 public void drawRoundRect(float l,float t,float r,float b,float rx,float ry,Paint p){boxes.add(new Box(l,t,r,b,p));}
 public void drawRect(RectF r,Paint p){boxes.add(new Box(r.left,r.top,r.right,r.bottom,p));}
 public void drawRect(float l,float t,float r,float b,Paint p){boxes.add(new Box(l,t,r,b,p));}
 public void drawLine(float a,float b,float c,float d,Paint p){lines++;}
 public void drawCircle(float x,float y,float radius,Paint p){}
 public int save(){return 1;}public void restore(){}public void restoreToCount(int s){}public boolean clipRect(float l,float t,float r,float b){return true;}public void translate(float x,float y){}
}
