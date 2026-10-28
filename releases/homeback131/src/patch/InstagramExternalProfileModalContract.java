package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Exact native selection and Back contracts for the registered external
 * profile inside Main's root 03CG modal. No application method is edited here.
 */
public final class InstagramExternalProfileModalContract {
    private InstagramExternalProfileModalContract() {}
    private static final String MAIN="Lcom/instagram/mainactivity/InstagramMainActivity;";
    private static final String FRAGMENT="Landroidx/fragment/app/Fragment;",MANAGER="LX/00fk;";
    private static final List<String> NONE=Collections.emptyList();
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Instagram452 external profile modal contract changed: "+why);
    }
    private static ClassDef clazz(Map<String,ClassDef> classes,String name) {
        ClassDef value=classes.get(name);require(value!=null,name);return value;
    }
    private static void field(Map<String,ClassDef> classes,String owner,String name,String type,int flags) {
        for(Field f:clazz(classes,owner).getFields())if(name.equals(f.getName())&&type.equals(f.getType())
                &&f.getAccessFlags()==flags)return;
        throw new IllegalStateException("Instagram452 external profile modal field changed: "+owner+"->"+name);
    }
    private static void method(Map<String,ClassDef> classes,String owner,String name,List<String> args,String result,
                               int flags,int registers,String digest) {
        for(Method m:clazz(classes,owner).getMethods())if(name.equals(m.getName())&&args.equals(m.getParameterTypes())
                &&result.equals(m.getReturnType())) {
            require(m.getAccessFlags()==flags&&m.getImplementation()!=null
                &&m.getImplementation().getRegisterCount()==registers
                &&digest.equals(OneBackPatch.instagramNavigationDigest(m)),owner+"->"+name);return;
        }
        throw new IllegalStateException("Instagram452 external profile modal method missing: "+owner+"->"+name);
    }
    public static void verifyOriginal(Map<String,ClassDef> classes) {
        for(String type:Arrays.asList(MAIN,"LX/03CG;","LX/0A4v;","LX/00M2;","LX/004K;","LX/00fz;","LX/0072;"))
            require(clazz(classes,type).getAccessFlags()==17,"final native class "+type);
        require("LX/0CTn;".equals(clazz(classes,"LX/03CG;").getSuperclass()),"modal fragment base");
        field(classes,MAIN,"A09","LX/0A4v;",1);field(classes,MAIN,"A08","LX/00M2;",1);
        field(classes,"LX/0A4v;","A05","I",1);field(classes,"LX/0A4v;","A06","Ljava/lang/ref/WeakReference;",1);
        for(String name:Arrays.asList("A0A","A04","A0B","A0C"))field(classes,"LX/0A4v;",name,"Z",1);
        field(classes,"LX/03CG;","A01","LX/00i6;",1);
        field(classes,"LX/03CG;","A08","Z",1);field(classes,"LX/03CG;","A07","Z",1);
        field(classes,"LX/00M2;","A02","LX/004K;",1);
        field(classes,"LX/004K;","A03","Landroidx/fragment/app/FragmentActivity;",17);
        field(classes,"LX/08kM;","A03","Ljava/lang/String;",17);
        field(classes,"LX/00VS;","A07","LX/0JWL;",17);field(classes,"LX/0072;","A00","Ljava/lang/Object;",1);
        field(classes,FRAGMENT,"mFragmentId","I",1);
        boolean arguments=false;
        for(Method m:clazz(classes,FRAGMENT).getMethods())if(m.getName().equals("getArguments")
                &&m.getParameterTypes().isEmpty()&&m.getReturnType().equals("Landroid/os/Bundle;")&&m.getAccessFlags()==17)arguments=true;
        require(arguments,"Fragment arguments getter");
        method(classes,MAIN,"A1f",NONE,FRAGMENT,17,2,
            "f0d4933ec962741bb24394d52e5b44884e171e3c1874a62d82441e714da7b488");
        method(classes,MAIN,"A08",Collections.singletonList(MAIN),FRAGMENT,25,1,
            "db8620b392e19445d64019e799756d421cb41a92d211c8882bf5036103ab401e");
        method(classes,MAIN,"EWo",NONE,"Z",17,4,
            "f10037e0f44b801285136dc4dc7dfa088d98671ba9ce1cad211cc2824c3fa893");
        method(classes,MAIN,"CIq",NONE,MANAGER,17,2,
            "11554fb323555ebf74a68349dcaf311725806df1e9517f48a792f63c31cc2693");
        method(classes,MAIN,"A1u",Collections.singletonList("LX/05Jc;"),"V",17,10,
            "aecd319627ceeb636a9f64b716a6b98f48bd79e15d66509f6424a54b20ec789f");
        method(classes,"LX/00M2;","A06",NONE,FRAGMENT,17,3,
            "d6f6f110f35c7cad2a2914720478861e879d462933bb9fa44fcba51940f24421");
        method(classes,"LX/004K;","A05",NONE,FRAGMENT,17,3,
            "7dac241fd44a6159e077a7d57dde260cff075d1d4b8edb3cbc8c34baa7283c88");
        method(classes,"LX/004K;","A06",NONE,"LX/08kM;",17,3,
            "6ac0e6d0c70b5641b4ed4a8981d49ef9bcf20de66e53a777fe60403b3e814764");
        method(classes,"LX/00Xt;","A00",Arrays.asList("Landroidx/fragment/app/FragmentActivity;","LX/08kM;"),"LX/00VS;",25,6,
            "01169a8742d95060ecf1edb3cce56a5f4b34655729ab61004c5e563fc88c82d9");
        method(classes,"LX/0072;","getValue",NONE,"Ljava/lang/Object;",17,3,
            "2476c5ea97ea35301bb8657f12764ca0cddff2d5b7bade62d0305f649574a003");
        method(classes,"LX/03CG;","onBackPressed",NONE,"Z",17,8,
            "94ed7f1860f65a9986e157d3a022df9987b906b8fd00a94e8161331071f137ae");
        method(classes,"LX/0A4v;","A00",NONE,MANAGER,18,4,
            "1168da46b978fbf8d48cbf4567f2df37042306691d97203dc8dd79216006ed97");
        method(classes,"LX/0A4v;","A01",Collections.singletonList("LX/0A4v;"),"LX/03CG;",25,1,
            "7742effd750991f1ab3a665d4fe7cd7a24a65c8025216aaae0a843dbe21a631e");
        method(classes,"LX/0A4v;","A06",NONE,MANAGER,17,3,
            "0cfad13461a15e9761a0cbc78e68c1ac5696cbb79d19528d32cc1f88902c65e7");
        method(classes,"LX/0A4v;","A0B",NONE,"Z",17,4,
            "1fa43847106e77d2f41f818794c03f9b7f2f481eb6beeddedae8ab5de70a1831");
        method(classes,"LX/0A4v;","A09",Arrays.asList("Landroid/os/Bundle;",FRAGMENT,
                "Landroidx/fragment/app/FragmentActivity;","LX/0Jar;","LX/0Izk;","Ljava/lang/Integer;",
                "Ljava/lang/String;","I","Z","Z","Z","Z","Z","Z","Z"),"V",17,35,
            "1b4a91362362ac1b83a1938d0939f270955d7282535dd1fb1e5c69ae19310c32");
        method(classes,"LX/01PZ;","<init>",Arrays.asList("Landroidx/fragment/app/FragmentActivity;","LX/08eI;"),"V",65537,5,
            "7ba23fd29c8e79b39935ac24a3169c05c6733a6ac79f4a78f5e63c35f6a1b3b2");
        method(classes,"LX/01PZ;","<init>",Arrays.asList("Landroidx/fragment/app/FragmentActivity;","LX/08eI;","Z"),"V",65537,10,
            "7fc09281b509f3800b8b4eca518e19515b473e3963e4a430d81e7b9f63c8221c");
    }
}
