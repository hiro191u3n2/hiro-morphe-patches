import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Verify direct production references on the applied APK, not fixture stubs. */
public final class VerifyLensAbi1936 {
    static int checks;
    static void need(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Map<String,ClassDef> load(String path)throws Exception{
        var container=DexFileFactory.loadDexContainer(new File(path),Opcodes.forApi(26));
        Map<String,ClassDef> all=new TreeMap<>();
        for(var name:container.getDexEntryNames())for(var c:container.getEntry(name).getDexFile().getClasses()){
            need(all.put(c.getType(),c)==null,"No duplicate class "+c.getType());
        }
        return all;
    }
    static ClassDef owner(Map<String,ClassDef> all,String id,boolean publicRequired){
        ClassDef c=all.get(id);need(c!=null,"Referenced class exists "+id);
        if(publicRequired)need((c.getAccessFlags()&1)!=0,"Cross-package class is public "+id);
        return c;
    }
    static Field field(Map<String,ClassDef> all,String type,String name,String descriptor,boolean isStatic,boolean publicRequired){
        ClassDef c=owner(all,type,publicRequired);Field found=null;
        for(var f:c.getFields())if(f.getName().equals(name)&&f.getType().equals(descriptor)){need(found==null,"Unique field "+name);found=f;}
        need(found!=null,"Exact referenced field exists "+type+"->"+name+":"+descriptor);
        need(((found.getAccessFlags()&8)!=0)==isStatic,"Static/instance field contract "+found);
        if(publicRequired)need((found.getAccessFlags()&1)!=0,"Direct cross-package field is public "+found);
        else need((found.getAccessFlags()&2)==0,"Same-package field is not private "+found);
        System.out.println("FIELD\t"+found+"\tflags="+found.getAccessFlags());return found;
    }
    static Method method(Map<String,ClassDef> all,String type,String signature,boolean isStatic,boolean publicRequired){
        ClassDef c=owner(all,type,publicRequired);Method found=null;
        for(var m:c.getMethods())if(m.toString().equals(type+"->"+signature)){need(found==null,"Unique method "+signature);found=m;}
        need(found!=null,"Exact referenced method exists "+type+"->"+signature);
        need(((found.getAccessFlags()&8)!=0)==isStatic,"Static/instance method contract "+found);
        if(publicRequired)need((found.getAccessFlags()&1)!=0,"Direct cross-package method is public "+found);
        System.out.println("METHOD\t"+found+"\tflags="+found.getAccessFlags());return found;
    }
    static String ref(Instruction i){return i instanceof ReferenceInstruction?((ReferenceInstruction)i).getReference().toString():"";}
    static List<String> refs(Method m){List<String> out=new ArrayList<>();for(var i:m.getImplementation().getInstructions())out.add(ref(i));return out;}
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("APPLIED_APK");
        var all=load(args[0]);checks=0;
        String provider="Li/f/l/n/q/y/c;", manager="Li/f/l/n/q/y/m;", bar="Lcom/hiro/ulike/OpticalZoomUi$Bar;";
        Field session=field(all,provider,"r","Li/f/l/j;",false,true);
        need((session.getAccessFlags()&16)!=0,"Provider session identity stays final");
        field(all,provider,"b","Ljava/lang/String;",false,true);
        field(all,manager,"b","Li/f/l/j;",true,true);
        field(all,manager,"c","Ljava/lang/String;",true,true);
        field(all,manager,"a",manager,true,true);
        method(all,"Li/f/l/j;","g(Ljava/lang/String;)Li/f/l/u/g;",false,true);
        method(all,"Li/f/l/u/g;","k()Li/f/l/u/j;",false,true);
        method(all,"Li/f/l/u/j;","x()Li/f/l/u/p;",false,true);
        method(all,"Li/f/l/u/p;","a()Ljava/lang/Object;",false,true);
        Field root=field(all,bar,"root","Landroid/widget/LinearLayout;",false,false);
        need((root.getAccessFlags()&16)!=0,"Bar root identity stays final");
        method(all,"Lcom/hiro/ulike/RearLensUi1930;","rearSelected()Z",true,true);
        // Constructor initializes the root; install is invoked only after
        // construction. Macro attach cannot undo initial front visibility.
        Method constructor=method(all,bar,"<init>(Landroid/view/View;)V",false,false);
        List<String> constructorRefs=refs(constructor);
        need(constructorRefs.contains(bar+"->root:Landroid/widget/LinearLayout;"),"Constructor initializes root before installation");
        Method attach=method(all,"Lcom/hiro/ulike/MacroUi168;","attach(Ljava/lang/Object;)V",true,true);
        for(String r:refs(attach))need(!r.endsWith("->setVisibility(I)V"),"Macro attach never overrides root visibility");
        Method install=method(all,bar,"installCore168()V",false,false);
        need(refs(install).contains(bar+"->update()V"),"Installation enters existing guarded updater");
        Method update=method(all,bar,"updateCore168()V",false,false);
        need(refs(update).contains("Lcom/hiro/ulike/OpticalZoom;->show()Z"),"Updater checks live native front/rear eligibility before showing");
        Method nativeSwitch=method(all,provider,"g0(Z)V",false,true);
        List<String> nativeRefs=refs(nativeSwitch);
        need(nativeRefs.contains("Li/f/l/u/g;->e()Li/f/l/u/p;"),"Accepted switch writes the provider-scoped facing property");
        need(nativeRefs.contains("Li/f/l/u/p;->k(Li/f/l/u/p;Ljava/lang/Object;ZILjava/lang/Object;)V"),"Accepted switch retains native property update");
        need(nativeRefs.contains("Li/f/l/n/n;->C(Z)V"),"Accepted switch retains original camera request");
        System.out.println("PASS lens direct native/helper ABI: checks="+checks+", apk="+new File(args[0]).getName()+". No physical device execution.");
    }
}
