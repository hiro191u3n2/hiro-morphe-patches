package app.hiro.oneback.runtime;
import android.app.*;import android.view.*;import android.graphics.Rect;import android.os.*;
/** Regression fixtures model reported states; not the proprietary apps or a physical device. */
public class Back115Repro {
 public static void main(String[] x)throws Exception{
  try(HomeBackHostTest.F f=new HomeBackHostTest.F(HomeBackHostTest.JP)){
   Activity child=new Activity(f.app,11,false);OneBackExit.install(child);child.getWindow().attach();f.root.focus(false);child.getWindow().getDecorView().focus(true);OneBackExit.onResume(child);Handler.drain();
   OneBackExit.enterBack(child);boolean blocked;try{blocked=OneBackExit.tryTerminalBack(child,0,false);}finally{OneBackExit.leaveNative();}
   System.out.println("TikTok non-root native Back incorrectly blocked: "+blocked);
   OneBackExit.onDestroy(child);child.destroyed=true;child.getWindow().getDecorView().detach();
  }
  try(HomeBackHostTest.F f=new HomeBackHostTest.F(HomeBackHostTest.IG)){
   HomeBackHostTest.marker(f,"layout_comment_thread",new Rect(0,500,1080,2200));
   System.out.println("Instagram comment thread over selected Home incorrectly allows exit: "+ExitScreens.shouldExit(f.activity));
  }
  try(HomeBackHostTest.F f=new HomeBackHostTest.F(HomeBackHostTest.JP)){
   ViewGroup button=new ViewGroup(f.activity);button.clickable=true;button.bounds=new Rect(0,20,100,100);View icon=new View(f.activity);icon.description="戻る";icon.bounds=new Rect(10,30,90,90);button.addView(icon);f.root.addView(button);
   System.out.println("TikTok Back icon with clickable parent not recognized: "+(TikTokScreen.inspect(f.activity).closeTarget==null));
  }
 }
}
