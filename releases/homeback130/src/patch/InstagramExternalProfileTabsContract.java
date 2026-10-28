package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Read-only profile-tab selection contracts from original Instagram452.
 * This class never edits application code or copies its implementation.
 */
public final class InstagramExternalProfileTabsContract {
    private InstagramExternalProfileTabsContract() {}
    private static final String PROFILE="Lcom/instagram/profile/fragment/UserDetailFragment;";
    private static final String CONTROLLER="Lcom/instagram/profile/fragment/UserDetailTabController;";
    private static final String PAGER="Landroidx/viewpager/widget/ViewPager;";
    private static final String NESTABLE="Lcom/instagram/ui/widget/nestablescrollingview/NestableViewPager;";
    private static final String FRAGMENT="Landroidx/fragment/app/Fragment;",MANAGER="LX/00fk;";
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Instagram452 profile tab contract changed: "+why);}
    private static ClassDef clazz(Map<String,ClassDef> classes,String name) {
        ClassDef value=classes.get(name);require(value!=null,name);return value;
    }
    private static void field(Map<String,ClassDef> classes,String owner,String name,String type,int flags) {
        for(Field f:clazz(classes,owner).getFields())if(name.equals(f.getName())&&type.equals(f.getType())
                &&flags==f.getAccessFlags())return;
        throw new IllegalStateException("Instagram452 profile tab field changed: "+owner+"->"+name);
    }
    private static void method(Map<String,ClassDef> classes,String owner,String name,List<String> args,String result,
                               int flags,int registers,String digest) {
        for(Method m:clazz(classes,owner).getMethods())if(name.equals(m.getName())&&args.equals(m.getParameterTypes())
                &&result.equals(m.getReturnType())) {
            require(m.getAccessFlags()==flags&&m.getImplementation()!=null
                &&m.getImplementation().getRegisterCount()==registers
                &&digest.equals(OneBackPatch.instagramNavigationDigest(m)),owner+"->"+name);return;
        }
        throw new IllegalStateException("Instagram452 profile tab getter missing: "+owner+"->"+name);
    }
    public static void verifyOriginal(Map<String,ClassDef> classes) {
        require(clazz(classes,CONTROLLER).getAccessFlags()==17,"final controller");
        require(clazz(classes,"LX/0EIp;").getAccessFlags()==17
                &&"LX/00fy;".equals(clazz(classes,"LX/0EIp;").getSuperclass()),"final native adapter");
        require(clazz(classes,NESTABLE).getAccessFlags()==17&&PAGER.equals(clazz(classes,NESTABLE).getSuperclass()),"final native pager");
        for(Method m:clazz(classes,NESTABLE).getMethods())
            require(!m.getName().equals("getCurrentItem")&&!m.getName().equals("getAdapter"),"pager getter override");
        field(classes,PROFILE,"A0b",CONTROLLER,1);
        field(classes,CONTROLLER,"A0A","LX/0EIp;",1);
        field(classes,CONTROLLER,"viewHolder","LX/0EdW;",1);
        field(classes,"LX/0EdW;","A0F",NESTABLE,17);
        field(classes,"LX/0EIp;","A01","Ljava/util/List;",1);
        field(classes,"LX/0EIp;","A05","Landroid/util/SparseArray;",17);
        field(classes,"LX/00fy;","A01",FRAGMENT,1);
        field(classes,"LX/00fy;","A06",MANAGER,17);
        field(classes,PAGER,"A02","I",1);field(classes,PAGER,"A0B","LX/00ft;",1);field(classes,PAGER,"A0W","I",1);
        method(classes,CONTROLLER,"A0D",Collections.<String>emptyList(),"LX/0NgK;",17,4,
            "9f25e57a46189ec384ceae2fccfdb75c34915dcf8a086d74b4889276ba8d24fd");
        method(classes,"LX/0EIp;","A0J",Collections.singletonList("I"),"LX/0NgK;",17,3,
            "59ec190128b8feebc2b4230f12d6e98d99ee91aa9c60aa38d7157dd4e4feb309");
        method(classes,PAGER,"getCurrentItem",Collections.<String>emptyList(),"I",1,2,
            "57ca434f19fbefb67af9add3bf959aebeed634c26bdc9164ef985ab7048ecf6f");
        method(classes,PAGER,"getAdapter",Collections.<String>emptyList(),"LX/00ft;",1,2,
            "2a0982cdbb9cf026cf6c0aa2f5ec5582c791a42b2c6a1d2ead2bdf64cb6d9476");
        method(classes,PAGER,"setScrollState",Collections.singletonList("I"),"V",1,5,
            "542e41f7adf71898c31356d18633b1bbddcbf9a9ff8a5bf232cf013eb9ee16cb");
        method(classes,"LX/00fy;","A0E",Arrays.asList("Landroid/view/ViewGroup;","Ljava/lang/Object;","I"),"V",17,8,
            "085e3faa457a421a869431e2a6f9b4d45b138ef1a445a74f815c3e307cdde2c3");
    }
}
