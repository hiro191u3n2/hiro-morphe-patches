package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Read-only current-page contracts for Instagram 452's FEED_SWITCHER tab.
 * The audited native 00nT factory creates 0A4c for fragment_feed_switcher.
 * No factory, adapter or current getter is modified by this contract. */
public final class InstagramFeedSwitcherContract {
    private InstagramFeedSwitcherContract() {}
    private static final String F="Landroidx/fragment/app/Fragment;";
    private static void require(boolean value,String name) {
        if(!value)throw new IllegalStateException("Instagram452 feed switcher contract changed: "+name);
    }
    private static ClassDef clazz(Map<String,ClassDef> classes,String name,int flags,String parent) {
        ClassDef value=classes.get(name);
        require(value!=null&&value.getAccessFlags()==flags&&(parent==null||parent.equals(value.getSuperclass())),name);
        return value;
    }
    private static void field(Map<String,ClassDef> classes,String owner,String name,String type,int flags) {
        ClassDef c=classes.get(owner);require(c!=null,owner);
        for(Field f:c.getFields())if(f.getName().equals(name)&&f.getType().equals(type)&&f.getAccessFlags()==flags)return;
        throw new IllegalStateException("Instagram452 feed switcher field changed: "+owner+"->"+name);
    }
    private static void method(Map<String,ClassDef> classes,String owner,String name,List<String> args,String result,int registers,String sha) {
        ClassDef c=classes.get(owner);require(c!=null,owner);
        for(Method m:c.getMethods())if(OneBackPatch.signature(m,name,args,result)) {
            require(m.getAccessFlags()==17&&m.getImplementation()!=null&&m.getImplementation().getRegisterCount()==registers
                &&sha.equals(OneBackPatch.instagramNavigationDigest(m)),owner+"->"+name);return;
        }
        throw new IllegalStateException("Instagram452 feed switcher method missing: "+owner+"->"+name);
    }
    public static void verifyOriginal(Map<String,ClassDef> classes) {
        clazz(classes,"LX/0A4c;",17,"LX/0CTn;");clazz(classes,"LX/0Clp;",17,"LX/04d0;");
        clazz(classes,"LX/0mBs;",1025,"Ljava/lang/Object;");
        clazz(classes,"Landroidx/viewpager2/widget/ViewPager2;",17,"Landroid/view/ViewGroup;");
        field(classes,"LX/08kM;","A0D","LX/08kM;",16409);
        field(classes,"LX/0A4c;","A0B","Landroidx/viewpager2/widget/ViewPager2;",1);
        field(classes,"LX/0A4c;","A0G","LX/0Clp;",1);
        field(classes,"LX/0Clp;","A00",F,1);field(classes,"LX/0Clp;","A02","Ljava/util/List;",1);
        field(classes,"Landroidx/viewpager2/widget/ViewPager2;","A00","I",1);
        field(classes,"LX/052y;","A06","LX/0mBs;",1);
        method(classes,"LX/0A4c;","A0e",Collections.<String>emptyList(),F,4,"e1d7e9e728e2c05ca5f141dca2e83a8471c608ee94d6e44e13e718e6a78ba9ca");
        method(classes,"LX/0Clp;","A0i",Collections.singletonList("I"),"LX/052y;",7,"7944b9d78357fe8f2ff1047302cfd7829b553063cf228e1b7889c8e2ac1deef1");
        method(classes,"LX/0Clp;","getItemId",Collections.singletonList("I"),"J",6,"b8b92109a56dc37f020cce26d8d8e6d6135b4c956db2fc61a01103f6b11b4bc3");
        method(classes,"LX/0mBs;","A05",Collections.<String>emptyList(),"I",2,"d32d93aeb408f15352c48e9b376b3c2ba735f0757690c56c7142d91f198fc366");
        method(classes,"LX/052y;","A07",Collections.<String>emptyList(),F,5,"3901abc7ff123a2f13089d96d92bee7c4dcc0c453913f7dcdd143b337067a8f6");
    }
}
