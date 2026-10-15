package app.hiro.oneback.runtime;
import android.app.*;import android.graphics.Rect;import android.view.*;import android.widget.TextView;
public class RegressionRepro {
 public static void main(String[] args)throws Exception {
  for(String marker:new String[]{"comment_panel_container","channel_page_container","user_profile_container"}) {
   try(HomeBackHostTest.F f=new HomeBackHostTest.F(HomeBackHostTest.TT)) {
    TextView feed=new TextView(f.activity);feed.text="おすすめ";feed.bounds=new Rect(300,40,600,140);f.root.addView(feed);
    HomeBackHostTest.marker(f,marker,new Rect(0,350,1080,2200));
    boolean handled=OneBackExit.tryBack(f.activity);
    System.out.println(marker+": handled="+handled+", taskRemoved="+(f.activity.removeCalls>0)+", finishing="+f.activity.finishing);
   }
  }
 }
}
