package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Runtime-only upgrades of the distributed 1.1.17/452 path. Whole-method
 * fingerprints were extracted from its rebuilt APK and checked against the
 * independently audited fresh output. Branch/exception endpoints and register
 * operands remain significant; only DEX string/goto widths are normalized.
 * Validation is read-only and runs before any mutable class is requested. */
public final class Instagram452Upgrade {
    private Instagram452Upgrade() {}
    private static final List<String> NONE=Collections.emptyList();
    private static void body(Map<String,ClassDef> classes,String owner,String name,
                             List<String> args,String result,String expected) {
        ClassDef type=classes.get(owner);
        if(type!=null)outer:for(Method method:type.getMethods()) {
            if(!name.equals(method.getName())||!result.equals(method.getReturnType())
                    ||args.size()!=method.getParameterTypes().size())continue;
            for(int i=0;i<args.size();i++)if(!args.get(i).contentEquals(method.getParameterTypes().get(i)))continue outer;
            if(expected.equals(OneBackPatch.instagramNavigationDigest(method)))return;
            break;
        }
        throw new IllegalStateException("Instagram 452 published Back boundary changed: "+owner+"->"+name);
    }
    public static void verify(Map<String,ClassDef> classes) {
        if(!Instagram452Patch.supports(classes)||!Instagram452Boundaries.supports(classes))
            throw new IllegalStateException("Instagram 452 upgrade dependencies absent");
        body(classes,"LX/02Hr;","onBackPressed",NONE,"Z",
            "6c5797ca8485cfbc05c652bfc2994a839eb3409bc720d720528b726eb133eca3");
        body(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","A1u",
            Collections.singletonList("LX/05Jc;"),"V",
            "a80156e157e09894929ed150fbfbea1ce6b906692cf44a3804a640b4318c3eea");
        body(classes,"Landroidx/activity/ComponentActivity;","CxU",NONE,"LX/001p;",
            "e967ddae938bb0d2b1ba8316fddbbf3ecc408697355ec77ec5a598cfd722b1c0");
        body(classes,"LX/001p;","A04",NONE,"V",
            "5456a8cc4df912615696ce080f7f1d8967012dece8bd6b562b7abf63f65a9801");
        body(classes,"LX/001p;","A00",Collections.singletonList("LX/001p;"),"V",
            "b83f272ef353f605f9f729b4d0c0b94a1e80cfe63fe41bd8fa08d68c0e3edca3");
        body(classes,"LX/00nv;","invoke",Collections.singletonList("Ljava/lang/Object;"),"Ljava/lang/Object;",
            "453c2d72bba80c3afd2a028b2574ece9a2271dfc82ba432d98f7d309c48380e4");
        Instagram452Tab.verifyPatched(classes);
    }
}
