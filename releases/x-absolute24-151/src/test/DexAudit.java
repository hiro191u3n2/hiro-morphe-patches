import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import app.hiro.twitter.patches.XAbsoluteTimePatch;

public final class DexAudit {
    static int checks=0;
    static void check(boolean value,String reason) {if(!value)throw new AssertionError(reason);checks++;}
    static Map<String,ClassDef> classes(String input)throws Exception{
        Map<String,ClassDef> result=new TreeMap<>();
        var container=DexFileFactory.loadDexContainer(new File(input),Opcodes.getDefault());
        for(String dex:container.getDexEntryNames()) for(ClassDef c:container.getEntry(dex).getDexFile().getClasses()) {
            check(result.put(c.getType(),c)==null,"duplicate class "+c.getType());
        }
        return result;
    }
    static byte[] canonical(ClassDef c)throws Exception {
        MemoryDataStore store=new MemoryDataStore();
        DexPool.writeTo(store,new ImmutableDexFile(Opcodes.getDefault(),Collections.singleton(c)));
        byte[] out=Arrays.copyOf(store.getBuffer(),store.getSize()); store.close();return out;
    }
    static MethodReference originalCall(String name) {
        return new ImmutableMethodReference("Lcom/twitter/util/datetime/d;",name,Arrays.asList("J","Landroid/content/res/Resources;"),"Ljava/lang/String;");
    }
    static void fixtures() {
        MethodReference postOwner=new ImmutableMethodReference("Lcom/twitter/tweetview/core/g$a;","a",Arrays.asList("Lcom/twitter/model/timeline/n2;","Landroid/content/res/Resources;","J"),"Ljava/lang/String;");
        var call=new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,4,5,7,0,0,originalCall("j"));
        MethodReference newRef=XAbsoluteTimePatch.replacement(postOwner,call);
        check(newRef!=null && newRef.getName().equals("post"),"post signature");
        var redirected=(FiveRegisterInstruction)XAbsoluteTimePatch.redirect(call,newRef);
        check(redirected.getRegisterCount()==3 && redirected.getRegisterC()==4 && redirected.getRegisterD()==5 && redirected.getRegisterE()==7,"wide register pair preserved");
        var range=new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,251,3,originalCall("j"));
        var rr=(RegisterRangeInstruction)XAbsoluteTimePatch.redirect(range,newRef);
        check(rr.getStartRegister()==251 && rr.getRegisterCount()==3,"range registers preserved");
        check(((Instruction)rr).getCodeUnits()==range.getCodeUnits(),"branch offsets invariant");
        for(String prefix:Arrays.asList("Lcom/twitter/dm/Inbox;","Lcom/twitter/rooms/Space;","Lcom/twitter/tweetview/Unrelated;","Lcom/other/app;")){
            MethodReference owner=new ImmutableMethodReference(prefix,"a",postOwner.getParameterTypes(),postOwner.getReturnType());
            check(XAbsoluteTimePatch.replacement(owner,call)==null,"non-target owner changed");
        }
        for(String name:Arrays.asList("g","h","i","k","l")) check(XAbsoluteTimePatch.replacement(postOwner,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,4,5,7,0,0,originalCall(name)))==null,"duration/other helper changed");
        XAbsoluteTimePatch.validatePackage("com.twitter.android","12.19.1-release.0");
        for(String version:Arrays.asList("12.19.0-release.0","12.20.0-release.0","")){
            boolean failed=false;try{XAbsoluteTimePatch.validatePackage("com.twitter.android",version);}catch(IllegalStateException expected){failed=true;}
            check(failed,"version gate");
        }
        boolean failed=false;try{XAbsoluteTimePatch.validatePackage("com.other.app","12.19.1-release.0");}catch(IllegalStateException expected){failed=true;}
        check(failed,"package gate");
    }
    public static void main(String[] args)throws Exception {
        if(args[0].equals("merge")){
            Map<String,ClassDef> old=classes(args[1]),add=classes(args[2]);
            Map<String,ClassDef> merged=new TreeMap<>(old);
            for(ClassDef c:add.values())check(merged.put(c.getType(),c)==null,"new class collision");
            DexPool.writeTo(args[3],new ImmutableDexFile(Opcodes.getDefault(),merged.values()));
            Map<String,ClassDef> read=classes(args[3]);
            check(read.size()==old.size()+add.size(),"merged class count");
            for(ClassDef c:old.values())check(Arrays.equals(canonical(c),canonical(read.get(c.getType()))),"baseline class changed "+c.getType());
            System.out.println("PASS preserve baseline DEX classes="+old.size()+"; new patch classes="+add.size());
            return;
        }
        if(args[0].equals("fixtures")){fixtures();System.out.println("PASS hook fixture assertions="+checks);return;}
        fixtures();
        Map<String,ClassDef> original=classes(args[1]);
        Map<String,ClassDef> patched=args.length>2?classes(args[2]):null;
        XAbsoluteTimePatch.validateOriginalUtilities(original.get("Lcom/twitter/util/datetime/d;"));
        int hooked=0;
        for(String type:XAbsoluteTimePatch.TARGETS){
            ClassDef c=original.get(type);check(c!=null,"missing "+type);
            XAbsoluteTimePatch.validateClass(c);
            MutableClass changed=new MutableClass(c);
            hooked+=XAbsoluteTimePatch.apply(changed);
            boolean repatchFailed=false;try{XAbsoluteTimePatch.validateClass(changed);}catch(IllegalStateException expected){repatchFailed=true;}
            check(repatchFailed,"double patch not rejected");
            if(patched!=null) check(Arrays.equals(canonical(changed),canonical(patched.get(type))),"final target dex mismatch "+type);
            System.out.println("PASS scoped hooks "+type);
        }
        check(hooked==6,"six timestamp call sites");
        if(patched!=null){
            for(String type:Arrays.asList("Lcom/twitter/util/datetime/d;","Lcom/twitter/util/datetime/d$a;","Lcom/twitter/tweetview/core/GrokShareAttachmentView;","Lcom/twitter/explore/immersive/h;","Lcom/twitter/tweetview/screenshot/core/share/ui/timestamp/a;"))
                check(Arrays.equals(canonical(original.get(type)),canonical(patched.get(type))),"non-target class changed "+type);
            for(String type:original.keySet())check(patched.containsKey(type),"input class missing "+type);
            for(String type:patched.keySet()) if(!original.containsKey(type))check(type.startsWith("Lapp/hiro/twitter/runtime/AbsoluteTime"),"unexpected added class "+type);
            // Whole-program scan: the injected entry points may only be called by the five reviewed post classes.
            int refs=0;
            for(ClassDef c:patched.values())for(Method m:c.getMethods())if(m.getImplementation()!=null)for(Instruction ins:m.getImplementation().getInstructions()) {
                if(ins instanceof ReferenceInstruction){Reference r=((ReferenceInstruction)ins).getReference();
                    if(r instanceof MethodReference && ((MethodReference)r).getDefiningClass().equals(XAbsoluteTimePatch.RUNTIME)
                            && !c.getType().startsWith("Lapp/hiro/twitter/runtime/")) {
                        check(XAbsoluteTimePatch.TARGETS.contains(c.getType()),"runtime leaked outside post classes");refs++;
                    }
                }
            }
            check(refs==6,"published hook count");
        }
        System.out.println("PASS APK static audit: hooks="+hooked+", original classes="+original.size()+", assertions="+checks+". Android device behavior NOT tested.");
    }
}
