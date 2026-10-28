package app.hiro.oneback.patches;

import java.util.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;

/** Complete authored native contracts from Instagram 452.1.0.53.64. Every
 * contract is checked before the first mutable class is requested. These hooks
 * are intentionally absent on older APKs; an unknown 452 body is rejected.
 */
public final class InstagramExternalProfilePatch {
    private InstagramExternalProfilePatch() {}
    public static final String ROUTE="LX/02HW;",PROFILE="Lcom/instagram/profile/fragment/UserDetailFragment;";
    private static final String FRAGMENT="Landroidx/fragment/app/Fragment;",MANAGER="LX/00fk;",IG="Lcom/instagram/base/activity/IgFragmentActivity;";
    private static final List<String> ROUTE_ARGS=Arrays.asList("Landroid/os/Bundle;","Landroidx/fragment/app/FragmentActivity;","LX/08eI;");
    private static Method method(Map<String,ClassDef> classes,String type,String name,List<String> args,String returns) {
        ClassDef owner=classes.get(type);
        if(owner!=null)for(Method method:owner.getMethods())if(name.equals(method.getName())&&args.equals(method.getParameterTypes())&&returns.equals(method.getReturnType()))return method;
        throw new IllegalStateException("Instagram 452 external profile contract missing: "+type+"->"+name);
    }
    private static Method contract(Map<String,ClassDef> classes,String type,String name,List<String> args,String returns,int flags,int registers,String sha) {
        Method method=method(classes,type,name,args,returns);
        if(method.getAccessFlags()!=flags||method.getImplementation()==null||method.getImplementation().getRegisterCount()!=registers
                ||!sha.equals(OneBackPatch.instagramNavigationDigest(method)))
            throw new IllegalStateException("Instagram 452 external profile native body changed: "+type+"->"+name);
        return method;
    }
    private static void field(Map<String,ClassDef> classes,String owner,String name,String type,int flags) {
        ClassDef clazz=classes.get(owner);
        if(clazz!=null)for(Field field:clazz.getFields())if(name.equals(field.getName())&&type.equals(field.getType())&&field.getAccessFlags()==flags)return;
        throw new IllegalStateException("Instagram 452 external profile field changed: "+owner+"->"+name);
    }
    private static void getter(Map<String,ClassDef> classes,String owner,String name,String result,int flags) {
        if(method(classes,owner,name,Collections.<String>emptyList(),result).getAccessFlags()!=flags)
            throw new IllegalStateException("Instagram 452 external profile getter changed: "+owner+"->"+name);
    }
    public static void verifyOriginal(Map<String,ClassDef> classes) {
        for(String type:Arrays.asList("Lcom/instagram/url/UrlHandlerActivity;","Lcom/instagram/url/UrlHandlerLauncherActivity;")) {
            ClassDef clazz=classes.get(type);if(clazz!=null)for(Method method:clazz.getMethods())
                if(method.getName().startsWith(OneBackPatch.HELPER_PREFIX))
                    throw new IllegalStateException("Instagram 452 external-profile update requires the original unpatched 452.1.0.53.64 APK; do not patch an already OneBack-patched APK");
        }
        InstagramExternalProfileTabsContract.verifyOriginal(classes);
        InstagramExternalProfileModalContract.verifyOriginal(classes);
        contract(classes,"LX/0Rts;","A03",Collections.<String>emptyList(),"Ljava/lang/Integer;",17,8,"e011c904547ddca7f5fa56fe346bae36f734769e2b3e84a22e525849a19401cc");
        contract(classes,"Lcom/instagram/url/UrlHandlerActivity;","onBackPressed",Collections.<String>emptyList(),"V",17,3,"0c6c81450f403928d8eb75a96b098a64639d96114dd389a11c373ee4402b2d83");
        contract(classes,"Lcom/instagram/url/UrlHandlerLauncherActivity;","onBackPressed",Collections.<String>emptyList(),"V",17,3,"b95951eb060101ba3cefac75af4fcee70c1784a9865627e502726c3796aab2ad");
        Method route=contract(classes,ROUTE,"A09",ROUTE_ARGS,"V",25,27,"1292d9a5bdedece0aca3f773d16e7a224535e6279deedf616c6b3d89f46ba9b9");
        Method profile=contract(classes,PROFILE,"onBackPressed",Collections.<String>emptyList(),"Z",17,11,"b32adfa6b171c5c5494196820df5e62a873c3964ed3a0d04aaa932cd4f6d5d13");
        contract(classes,IG,"A1f",Collections.<String>emptyList(),FRAGMENT,1,3,"0b7bd863d2054b2947a5ad76142b94b873f13b59c9584774e9fe125f04febb04");
        contract(classes,"LX/01MU;","A0B",Collections.<String>emptyList(),"Z",17,3,"afd10f1a72f4ef02fbbc5aa2c5f769d2c5f94ed8e960f9be3035baa7d483258f");
        List<Instruction> body=list(route);Instruction shared=body.get(302);
        if(shared.getOpcode()!=Opcode.INVOKE_VIRTUAL||!(shared instanceof FiveRegisterInstruction)||!(shared instanceof ReferenceInstruction))throw new IllegalStateException("External profile argument boundary changed");
        FiveRegisterInstruction registers=(FiveRegisterInstruction)shared;Object ref=((ReferenceInstruction)shared).getReference();
        if(registers.getRegisterCount()!=2||registers.getRegisterC()!=2||registers.getRegisterD()!=0||!(ref instanceof MethodReference)
                ||!"Landroidx/fragment/app/Fragment;->setArguments(Landroid/os/Bundle;)V".equals(ref.toString()))throw new IllegalStateException("External profile argument registers changed");
        List<Instruction> back=list(profile);int returns=0;
        for(int i=0;i<back.size();i++)if(back.get(i).getOpcode()==Opcode.RETURN){returns++;if(i!=271||((OneRegisterInstruction)back.get(i)).getRegisterA()!=2)throw new IllegalStateException("External profile result boundary changed");}
        if(returns!=1)throw new IllegalStateException("External profile native result count changed");
        field(classes,PROFILE,"A0p","LX/0DiR;",1);field(classes,"LX/0DiR;","A01","LX/01MU;",1);
        field(classes,IG,"A00","LX/00i6;",1);field(classes,"LX/00i7;","A0W","Z",1);
        field(classes,MANAGER,"mExecutingActions","Z",1);field(classes,MANAGER,"A0W","Ljava/util/ArrayList;",17);
        field(classes,MANAGER,"A0V","LX/00fz;",17);field(classes,"LX/00fz;","A01","Ljava/util/ArrayList;",17);
        field(classes,"LX/007r;","A00","Landroid/app/Dialog;",1);
        getter(classes,FRAGMENT,"getView","Landroid/view/View;",1);
        getter(classes,FRAGMENT,"getActivity","Landroidx/fragment/app/FragmentActivity;",17);
        getter(classes,FRAGMENT,"isAdded","Z",17);getter(classes,FRAGMENT,"isVisible","Z",17);getter(classes,FRAGMENT,"isResumed","Z",17);
        getter(classes,FRAGMENT,"getParentFragment",FRAGMENT,17);getter(classes,FRAGMENT,"getParentFragmentManager",MANAGER,17);
        getter(classes,FRAGMENT,"getChildFragmentManager",MANAGER,17);
        getter(classes,"Landroidx/fragment/app/FragmentActivity;","A1Z",MANAGER,17);
        getter(classes,MANAGER,"A15","Z",17);getter(classes,MANAGER,"A0L","I",17);getter(classes,"LX/00i6;","A0N",FRAGMENT,17);
        if(method(classes,MANAGER,"A0O",Collections.singletonList("I"),FRAGMENT).getAccessFlags()!=17)throw new IllegalStateException("External profile current getter changed");
    }
    private static List<Instruction> list(Method method){List<Instruction> list=new ArrayList<Instruction>();for(Instruction instruction:method.getImplementation().getInstructions())list.add(instruction);return list;}
    private static MutableMethod mutable(MutableClass clazz,String name,List<String> args,String result) {
        for(Method method:clazz.getMethods())if(name.equals(method.getName())&&args.equals(method.getParameterTypes())&&result.equals(method.getReturnType()))return (MutableMethod)method;
        throw new IllegalStateException("External profile mutable boundary missing");
    }
    public static void apply(Map<String,ClassDef> original,OneBackPatch.Editor editor,Map<String,Integer> counts) {
        verifyOriginal(original);
        MutableMethod route=mutable(editor.mutable(ROUTE),"A09",ROUTE_ARGS,"V");
        route.getImplementation().addInstruction(303,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,4,2,5,0,0,
            new ImmutableMethodReference(OneBackPatch.RUNTIME,"rememberInstagramExternalProfile",Arrays.asList("Landroid/app/Activity;","Ljava/lang/Object;","Landroid/os/Bundle;"),"V")));
        MutableMethod profile=mutable(editor.mutable(PROFILE),"onBackPressed",Collections.<String>emptyList(),"Z");
        MutableMethodImplementation code=profile.getImplementation();
        // Replace, rather than insert before, the existing return: all native
        // branches targeting that return must also visit the new result gate.
        code.replaceInstruction(271,new BuilderInstruction35c(Opcode.INVOKE_STATIC,2,10,2,0,0,0,
            new ImmutableMethodReference(OneBackPatch.RUNTIME,"finishInstagramExternalProfileBack",Arrays.asList("Ljava/lang/Object;","Z"),"Z")));
        code.addInstruction(272,new BuilderInstruction11x(Opcode.MOVE_RESULT,2));
        code.addInstruction(273,new BuilderInstruction11x(Opcode.RETURN,2));
        code.addInstruction(0,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,10,0,0,0,0,
            new ImmutableMethodReference(OneBackPatch.RUNTIME,"beginInstagramExternalProfileBack",Collections.singletonList("Ljava/lang/Object;"),"V")));
        MutableMethod delegate=mutable(editor.mutable("LX/0Rts;"),"A03",Collections.<String>emptyList(),"Ljava/lang/Integer;");
        MutableMethodImplementation decision=delegate.getImplementation();
        // Both the root-count and return-to-sender edges target instruction 10.
        // Replace that shared instruction so neither edge can skip this gate.
        decision.replaceInstruction(10,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,3,0,0,0,0,
            new ImmutableMethodReference(OneBackPatch.RUNTIME,"dispatchInstagramExternalProfile",Collections.singletonList("Landroid/app/Activity;"),"Z")));
        decision.addInstruction(11,new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
        decision.addInstruction(12,new BuilderInstruction10x(Opcode.NOP));
        decision.addInstruction(13,new BuilderInstruction11n(Opcode.CONST_4,5,1));
        decision.addInstruction(14,new BuilderInstruction22c(Opcode.IGET_OBJECT,4,7,new ImmutableFieldReference("LX/0Rts;","A03","LX/08eI;")));
        decision.replaceInstruction(12,new BuilderInstruction21t(Opcode.IF_EQZ,0,decision.newLabelForIndex(14)));
        counts.put("instagram_452_external_profile_url_dispatch",1);
        counts.put("instagram_452_external_profile_capture",1);
        counts.put("instagram_452_external_profile_native_begin",1);
        counts.put("instagram_452_external_profile_native_result",1);
    }
}
