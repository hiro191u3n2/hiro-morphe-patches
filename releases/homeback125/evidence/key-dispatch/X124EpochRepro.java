package app.hiro.oneback.runtime;
import android.view.KeyEvent;
public final class X124EpochRepro {
 public static void main(String[] args) throws Exception {
  try (Back124TwitterTest.Scene s = new Back124TwitterTest.Scene(Back124TwitterTest.HOME)) {
   OneBackExit.enterKey(s.activity, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
   try {
    OneBackExit.onUserInteraction(s.activity);
    OneBackExit.enterBack(s.activity);
    try {
     boolean root = s.nativeBack();
     boolean terminal = OneBackExit.tryTerminalBack(s.activity, 0, false);
     System.out.println("rootHookAccepted=" + root + " terminalHookAccepted=" + terminal + " taskRemoveCalls=" + s.task.removeCalls + " excluded=" + s.task.excluded + " homeMarker=" + Back124TwitterTest.marker(s.app));
     if (root || terminal || s.task.removeCalls != 0) throw new AssertionError("Expected exact v124 omission not reproduced");
    } finally { OneBackExit.leaveNative(); }
   } finally { OneBackExit.leaveNative(); }
  }
 }
}
