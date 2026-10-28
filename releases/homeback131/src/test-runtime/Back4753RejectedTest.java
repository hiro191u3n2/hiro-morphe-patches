package app.hiro.oneback.runtime;
import android.os.Bundle;
import com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility;
import com.bytedance.hox.Hox;
import com.ss.android.ugc.aweme.main.assems.tabs.TabAbilityAssem;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
/** Retired ABI doubles are used only for a negative runtime recognition test. */
public final class Back4753RejectedTest {
    public static void main(String[] args)throws Exception {
        check(HomeTabAbility.class.getMethod("IA0",Bundle.class,String.class).getReturnType()==Void.TYPE,"genuine retired ABI fixture");
        check(HomeTabAbility.class.getMethod("Nu2").getReturnType()==String.class,"retired current-tab ABI");
        check(HomeTabAbility.class.getMethod("EH0").getReturnType()==Hox.class,"retired tree ABI");
        check(Hox.class.getMethod("i33",String.class).getReturnType()==Boolean.TYPE,"retired active-path ABI");
        try(F f=new F(TT)) {
            TabAbilityAssem tabs=new TabAbilityAssem();tabs.selected="HOME";tabs.tree.homeActive=true;
            ((com.ss.android.ugc.aweme.main.MainActivity)f.activity).communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility=tabs;
            check(TikTokHome.route(f.activity)==null,"47.5.3 route is no longer recognized");
            check(!TikTokHome.supportsNativeBack(f.activity),"47.5.3 cannot enter new native Back flow");
        }
        System.out.println("PASS Back4753RejectedTest: retired 47.5.3 ABI is not recognized");
    }
}
