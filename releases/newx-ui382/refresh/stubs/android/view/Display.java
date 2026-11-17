package android.view;
public class Display {
    public Mode current;
    public Mode[] modes;
    public Mode getMode() { return current; }
    public Mode[] getSupportedModes() { return modes; }
    public static class Mode {
        private final int id, width, height;
        private final float rate;
        public Mode(int id, int width, int height, float rate) {
            this.id=id;this.width=width;this.height=height;this.rate=rate;
        }
        public int getModeId() { return id; }
        public int getPhysicalWidth() { return width; }
        public int getPhysicalHeight() { return height; }
        public float getRefreshRate() { return rate; }
    }
}
