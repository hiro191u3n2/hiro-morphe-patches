package android.graphics;
public class Canvas { public int lines;public int lastColor;public void drawLine(float a,float b,float c,float d,Paint p){lines++;lastColor=p.color;} }
