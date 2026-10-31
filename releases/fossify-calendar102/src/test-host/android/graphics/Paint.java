package android.graphics;
public class Paint {
    private int color;
    public Paint() { }
    public Paint(Paint other) { color=other.color; }
    public void setColor(int value) { color=value; }
    public int getColor() { return color; }
}
