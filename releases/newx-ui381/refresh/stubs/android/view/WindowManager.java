package android.view;
public interface WindowManager {
    Display getDefaultDisplay();
    class LayoutParams {
        public int preferredDisplayModeId;
        public float preferredRefreshRate;
        public float screenBrightness = -1;
        public int flags;
        public int copyFrom(LayoutParams p) {
            preferredDisplayModeId=p.preferredDisplayModeId;
            preferredRefreshRate=p.preferredRefreshRate;
            screenBrightness=p.screenBrightness;
            flags=p.flags;
            return 0;
        }
    }
}
