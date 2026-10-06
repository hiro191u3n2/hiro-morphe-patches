package jp.ddo.sugihiro.quicksearch.activity;
import android.app.Activity;
import app.hiro.quicksearch.runtime.SearchTask;
/** Name-compatible host fixture, not packaged into the Android app. */
public class MainActivity extends Activity implements SearchTask.Cleanup {
    public int cleanupCalls;
    @Override public void hiroQuickSearchCleanup(){cleanupCalls++;}
}
