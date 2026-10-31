package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Native provenance for a URL-created profile in Main's root manager.
 * FragmentNavigator uses that manager when Main has no attached selected tab.
 * Runtime checks the exact registered fragment in that manager; these pins do
 * not authorize URI-only profiles or evaluate Main's selected-tab getter. */
public final class InstagramInitialProfileContract {
    private InstagramInitialProfileContract() {}
    private static final String MAIN="Lcom/instagram/mainactivity/InstagramMainActivity;";
    private static final String ACTIVITY="Landroidx/fragment/app/FragmentActivity;";
    private static final String FRAGMENT="Landroidx/fragment/app/Fragment;",MANAGER="LX/00fk;";
    private static final List<String> NONE=Collections.<String>emptyList();

    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Instagram452 initial profile contract changed: "+why);
    }
    private static ClassDef clazz(Map<String,ClassDef> classes,String name,int flags) {
        ClassDef type=classes.get(name);require(type!=null&&type.getAccessFlags()==flags,name);return type;
    }
    private static void field(ClassDef owner,String name,String type,int flags) {
        for(Field field:owner.getFields())if(name.equals(field.getName())&&type.equals(field.getType())&&field.getAccessFlags()==flags)return;
        throw new IllegalStateException("Instagram452 initial profile field changed: "+owner.getType()+"->"+name);
    }
    private static void method(ClassDef owner,String name,List<String> args,String result,int flags,int registers,String sha) {
        for(Method method:owner.getMethods())if(OneBackPatch.signature(method,name,args,result)) {
            require(method.getAccessFlags()==flags&&method.getImplementation()!=null
                &&method.getImplementation().getRegisterCount()==registers
                &&sha.equals(OneBackPatch.instagramNavigationDigest(method)),owner.getType()+"->"+name);return;
        }
        throw new IllegalStateException("Instagram452 initial profile method missing: "+owner.getType()+"->"+name);
    }
    public static void verifyOriginal(Map<String,ClassDef> classes) {
        ClassDef main=clazz(classes,MAIN,17),navigator=clazz(classes,"LX/01PZ;",17);
        ClassDef activity=clazz(classes,ACTIVITY,1),manager=clazz(classes,MANAGER,1025);
        ClassDef fragment=classes.get(FRAGMENT);require(fragment!=null,FRAGMENT);
        field(fragment,"mFragmentId","I",1);
        field(manager,"A0V","LX/00fz;",17);
        ClassDef store=clazz(classes,"LX/00fz;",17);
        field(store,"A01","Ljava/util/ArrayList;",17);field(store,"A02","Ljava/util/HashMap;",17);
        // BwK may return null before a selected wrapper is attached. A00 then
        // uses Activity.A1Z, preserving the actual URL-created fragment.
        method(navigator,"A00",Collections.singletonList(ACTIVITY),MANAGER,25,2,
            "6907ce802b40d51c315461e68fc2bf86bc1f74203b3501498ae90b25c3a77b20");
        method(main,"BwK",NONE,MANAGER,17,4,
            "64b719bbbd2e940845a909b7934870c869b857ae185527de653acf6e557523c5");
        // These two getters only read existing host/store state. A0O scans
        // added fragments in reverse, then active records, without committing
        // transactions, creating fragments, or reading selected-tab lazies.
        method(activity,"A1Z",NONE,MANAGER,17,2,
            "fc6937fa31c842df7902b5fa86687aa1d39de8465c6a6957e5acf4e028bf5d62");
        method(manager,"A0O",Collections.singletonList("I"),FRAGMENT,17,7,
            "7726e7e96b3b4379f05462aacf1c986161ec43a894c6bc72a1a692a894bf7a2c");
    }
}
