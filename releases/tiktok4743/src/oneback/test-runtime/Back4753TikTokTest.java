package app.hiro.oneback.runtime;

import com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility;
import com.bytedance.hox.Hox;
import android.os.Bundle;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Only run with test-stubs4753; the legacy R8 names are deliberately absent. */
public final class Back4753TikTokTest {
    private static boolean absent(Class<?> type,String name,Class<?>... args) {
        try{type.getMethod(name,args);return false;}catch(NoSuchMethodException expected){return true;}
    }
    public static void main(String[] args)throws Exception {
        check(absent(HomeTabAbility.class,"Lx0",Bundle.class,String.class),"47.5.3 does not expose legacy selection method");
        check(absent(HomeTabAbility.class,"Mm2"),"47.5.3 does not expose legacy current-tab method");
        check(absent(HomeTabAbility.class,"qE0"),"47.5.3 does not expose legacy tree getter");
        check(absent(Hox.class,"fU2",String.class),"47.5.3 does not expose legacy active-path method");
        check(absent(HomeTabAbility.class,"tB0",Bundle.class,String.class),"47.5.3 does not expose 47.4.3 selection method");
        check(absent(HomeTabAbility.class,"tv2"),"47.5.3 does not expose 47.4.3 current-tab method");
        check(absent(HomeTabAbility.class,"jI0"),"47.5.3 does not expose 47.4.3 tree getter");
        check(absent(Hox.class,"w33",String.class),"47.5.3 does not expose 47.4.3 active-path method");
        check(HomeTabAbility.class.getMethod("IA0",Bundle.class,String.class).getReturnType()==Void.TYPE,"47.5.3 selector descriptor");
        check(HomeTabAbility.class.getMethod("Nu2").getReturnType()==String.class,"47.5.3 current root tab descriptor");
        check(HomeTabAbility.class.getMethod("EH0").getReturnType()==Hox.class,"47.5.3 Hox descriptor");
        check(Hox.class.getMethod("i33",String.class).getReturnType()==Boolean.TYPE,"47.5.3 native active-path descriptor");
        // Reuse the behavioral assertions, with only the actual new APK ABI:
        // profile -> Home, next Back removes task; comments first; editor kept;
        // native transition/no-op/failure bounded; retained Home never grants exit.
        Back117TikTokTest.run();
        System.out.println("PASS Back4753TikTokTest: 47.5.3-only native ABI; profile/Home/back/layer regressions; host simulation, not Android device");
    }
}
