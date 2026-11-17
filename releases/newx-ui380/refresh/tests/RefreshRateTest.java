package app.hiro.newxui;
import android.app.Activity;
import android.view.*;

public final class RefreshRateTest {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static Display.Mode mode(int id, int w, int h, float hz) {
        return new Display.Mode(id,w,h,hz);
    }
    private static Display display(Display.Mode current, Display.Mode... modes) {
        Display d=new Display();d.current=current;d.modes=modes;return d;
    }
    private static Activity activity(final Display d) {
        Activity a=new Activity();a.window=new Window();a.window.decor.display=d;
        a.manager=new WindowManager() { public Display getDefaultDisplay() {return d;} };
        return a;
    }
    public static void main(String[] args) {
        Display.Mode sixty=mode(1,1440,3120,60), oneTwenty=mode(2,1440,3120,120);
        Display.Mode lowRes=mode(3,1080,2340,120), ninety=mode(4,1440,3120,90);
        Display.Mode nearly=mode(5,1440,3120,119.88f), fast=mode(6,1440,3120,144);
        Display d=display(sixty,lowRes,ninety,nearly,fast,oneTwenty);
        check(RefreshRate.chooseMode(d)==oneTwenty,"exact rate is preferred");
        check(RefreshRate.chooseMode(display(sixty,lowRes,ninety,fast))==null,"unsupported resolution/rates untouched");
        check(RefreshRate.chooseMode(display(sixty,nearly))==nearly,"119.88 counts as 120 Hz");
        check(RefreshRate.chooseMode(display(sixty,mode(9,1440,3120,118)))==null,"118 Hz not 120");
        check(RefreshRate.chooseMode(display(sixty,mode(9,1440,3120,Float.NaN)))==null,"NaN ignored");
        check(RefreshRate.chooseMode(null)==null,"null display safe");
        Display.Mode duplicate=mode(7,1440,3120,120);
        check(RefreshRate.chooseMode(display(oneTwenty,duplicate,oneTwenty))==oneTwenty,"current mode wins tie");

        Activity a=activity(d);a.window.attributes.flags=1234;a.window.attributes.screenBrightness=.42f;
        a.window.attributes.preferredDisplayModeId=7;a.window.attributes.preferredRefreshRate=48;
        WindowManager.LayoutParams initial=a.window.attributes;
        RefreshRate.bind(a);
        check(a.window.attributes.preferredDisplayModeId==2,"mode applied");
        check(a.window.attributes.preferredRefreshRate==120,"rate applied");
        check(a.window.attributes.flags==1234 && a.window.attributes.screenBrightness==.42f,"other window attrs preserved");
        check(initial.preferredDisplayModeId==7 && initial.preferredRefreshRate==48,"input attrs not mutated");
        check(a.window.writes==1,"single attribute update");
        RefreshRate.bind(a);check(a.window.writes==1,"resume idempotent");
        UiOptions.enabled=false;RefreshRate.bind(a);
        check(a.window.attributes.preferredDisplayModeId==7 && a.window.attributes.preferredRefreshRate==48,"off restores exact baseline");
        RefreshRate.bind(a);check(a.window.writes==2,"off idempotent");

        UiOptions.enabled=true;RefreshRate.bind(a);
        a.window.attributes.preferredRefreshRate=60;
        UiOptions.enabled=false;RefreshRate.bind(a);
        check(a.window.attributes.preferredRefreshRate==60 && a.window.attributes.preferredDisplayModeId==7,"off preserves newer native field");

        UiOptions.enabled=true;RefreshRate.bind(a);
        a.window.attributes.preferredDisplayModeId=8;a.window.attributes.preferredRefreshRate=90;
        RefreshRate.bind(a);UiOptions.enabled=false;RefreshRate.bind(a);
        check(a.window.attributes.preferredDisplayModeId==8 && a.window.attributes.preferredRefreshRate==90,"resume updates native baseline");

        UiOptions.enabled=true;Activity b=activity(display(sixty,sixty,ninety));RefreshRate.bind(b);
        check(b.window.writes==0,"unsupported display not changed");
        Activity c=activity(d);c.window.decor.display=null;RefreshRate.bind(c);
        check(c.window.attributes.preferredDisplayModeId==2,"default display fallback");
        c.window.decor.display=display(sixty,sixty,ninety);RefreshRate.bind(c);
        check(c.window.attributes.preferredDisplayModeId==0 && c.window.attributes.preferredRefreshRate==0,"moving to unsupported display restores");
        RefreshRate.bind(null);Activity empty=new Activity();RefreshRate.bind(empty);
        check(empty.window==null,"no window harmless");
        System.out.println("PASS: "+assertions+" meaningful refresh-rate assertions");
    }
}
