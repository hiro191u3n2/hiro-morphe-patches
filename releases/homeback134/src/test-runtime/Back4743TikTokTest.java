package app.hiro.oneback.runtime;

import com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility;
import com.bytedance.hox.Hox;
import android.os.Bundle;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Only run with test-stubs4743; the legacy R8 names are deliberately absent. */
public final class Back4743TikTokTest {
    private static boolean absent(Class<?> type,String name,Class<?>... args) {
        try{type.getMethod(name,args);return false;}catch(NoSuchMethodException expected){return true;}
    }
    public static void main(String[] args)throws Exception {
        check(absent(HomeTabAbility.class,"Lx0",Bundle.class,String.class),"47.4.3 does not expose legacy selection method");
        check(absent(HomeTabAbility.class,"Mm2"),"47.4.3 does not expose legacy current-tab method");
        check(absent(HomeTabAbility.class,"qE0"),"47.4.3 does not expose legacy tree getter");
        check(absent(Hox.class,"fU2",String.class),"47.4.3 does not expose legacy active-path method");
        check(absent(HomeTabAbility.class,"IA0",Bundle.class,String.class),"47.4.3 does not expose 47.5.3 selection method");
        check(absent(HomeTabAbility.class,"Nu2"),"47.4.3 does not expose 47.5.3 current-tab method");
        check(absent(HomeTabAbility.class,"EH0"),"47.4.3 does not expose 47.5.3 tree getter");
        check(absent(Hox.class,"i33",String.class),"47.4.3 does not expose 47.5.3 active-path method");
        check(HomeTabAbility.class.getMethod("tB0",Bundle.class,String.class).getReturnType()==Void.TYPE,"47.4.3 selector descriptor");
        check(HomeTabAbility.class.getMethod("tv2").getReturnType()==String.class,"47.4.3 current root tab descriptor");
        check(HomeTabAbility.class.getMethod("jI0").getReturnType()==Hox.class,"47.4.3 Hox descriptor");
        check(Hox.class.getMethod("w33",String.class).getReturnType()==Boolean.TYPE,"47.4.3 native active-path descriptor");
        // Behavior is exercised by Back4743NativeBackTest without feed labels.
        // Back117TikTokTest remains the unchanged 46.2.3 compatibility suite.
        System.out.println("PASS Back4743TikTokTest: 47.4.3-only ABI descriptors; native behavior is covered separately");
    }
}
