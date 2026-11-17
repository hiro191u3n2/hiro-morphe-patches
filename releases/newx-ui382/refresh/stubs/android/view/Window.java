package android.view;
public class Window {
    public WindowManager.LayoutParams attributes = new WindowManager.LayoutParams();
    public View decor = new View();
    public int writes;
    public WindowManager.LayoutParams getAttributes() { return attributes; }
    public void setAttributes(WindowManager.LayoutParams p) { attributes=p;writes++; }
    public View getDecorView() { return decor; }
}
