package app.hiro.oneback.runtime;
import android.app.Activity;
/** Compatibility entry point; all non-home screens use the same navigation transaction. */
final class TikTokHome {
    private TikTokHome() {}
    static boolean go(Activity a) {
        if(!TikTokScreen.owns(a)||!HomeScreen.inputReady(a))return false;
        TikTokBack.request(a);return true;
    }
}
