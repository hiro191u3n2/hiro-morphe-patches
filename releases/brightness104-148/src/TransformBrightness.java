import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

/** One existing resource-loader hook plus version metadata; runtime payloads untouched. */
public final class TransformBrightness {
    static final Opcodes OPS=Opcodes.forApi(26);
    static final String NS="Lapp/hiro/brightnessclick/";
    static final String TARGET=NS+"S26CompatibilityPatch;";
    static final String HELPER=NS+"NoRecents;";
    static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    static Map<String,ClassDef> read(String file)throws Exception{
        Map<String,ClassDef> result=new TreeMap<>();
        for(ClassDef c:DexFileFactory.loadDexFile(file,OPS).getClasses())require(result.put(c.getType(),c)==null,"Duplicate class");
        return result;
    }
    static byte[] serialize(Collection<? extends ClassDef> classes)throws Exception{
        DexPool pool=new DexPool(OPS);for(ClassDef c:classes)pool.internClass(c);
        MemoryDataStore store=new MemoryDataStore();try{pool.writeTo(store);return store.getData();}finally{store.close();}
    }
    static String hash(ClassDef c)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(serialize(List.of(c))));}
    static Method withBody(Method m,MethodImplementation body){return new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),body);}
    static String value(Instruction i){return i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s ? s.getString() : null;}
    static void changeString(MutableMethodImplementation body,int index,String value){
        Instruction old=body.getInstructions().get(index);int register=((OneRegisterInstruction)old).getRegisterA();
        body.replaceInstruction(index,old.getOpcode()==Opcode.CONST_STRING_JUMBO
            ?new BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,register,new ImmutableStringReference(value))
            :new BuilderInstruction21c(Opcode.CONST_STRING,register,new ImmutableStringReference(value)));
    }
    public static void main(String[] args)throws Exception{
        require(args.length==4,"BASE_DEX HELPER_DEX OUT_BUNDLE_DEX OUT_SINGLE_DEX");
        Map<String,ClassDef> old=read(args[0]),added=read(args[1]),next=new TreeMap<>(old);
        require(old.size()==233,"Unexpected baseline class count");
        require(added.keySet().equals(Set.of(HELPER))&&!old.containsKey(HELPER),"Unexpected helper namespace");
        ClassDef target=Objects.requireNonNull(old.get(TARGET));List<Method> methods=new ArrayList<>();
        int manifestChanges=0,descriptionChanges=0,aboutChanges=0;
        for(Method method:target.getMethods()){
            if(method.getName().equals("manifest")){
                MutableMethodImplementation body=new MutableMethodImplementation(method.getImplementation());
                require(body.getRegisterCount()==12&&body.getInstructions().size()==130,"Manifest layout drift");
                Instruction at=body.getInstructions().get(119);
                require(at instanceof FiveRegisterInstruction r&&r.getRegisterCount()==2&&r.getRegisterC()==2&&r.getRegisterD()==1,"Application append register drift");
                MethodReference ref=(MethodReference)((ReferenceInstruction)at).getReference();
                require(ref.getDefiningClass().equals("Lorg/w3c/dom/Element;")&&ref.getName().equals("appendChild"),"Expected permission activity attachment");
                require("9".equals(value(body.getInstructions().get(24))),"VersionCode baseline drift");
                changeString(body,24,"10");
                body.addInstruction(120,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,2,0,0,0,0,
                    new ImmutableMethodReference(HELPER,"apply",List.of("Lorg/w3c/dom/Element;"),"V")));
                methods.add(withBody(method,body));manifestChanges++;
            }else if(method.getName().equals("<clinit>")||method.getName().equals("about")){
                MutableMethodImplementation body=new MutableMethodImplementation(method.getImplementation());int changes=0;
                for(int i=0;i<body.getInstructions().size();i++){
                    String s=value(body.getInstructions().get(i));if(s==null)continue;
                    if(method.getName().equals("<clinit>")&&s.startsWith("古いAndroid向けのインストール制限")){
                        changeString(body,i,"v1.0.4：起動直後から明るさタッチを最近使ったアプリに表示しません。起動・明るさ変更・設定画面すべてに適用。\n"+s);descriptionChanges++;changes++;
                    }else if(method.getName().equals("about")&&s.equals("Version 1.5 S26 1.0.2")){
                        changeString(body,i,"Version 1.5 S26 1.0.4");aboutChanges++;changes++;
                    }
                }
                require(changes==1,"Expected exactly one metadata change: "+method.getName());methods.add(withBody(method,body));
            }else methods.add(method);
        }
        require(manifestChanges==1&&descriptionChanges==1&&aboutChanges==1,"Missing or repeated changes");
        ClassDef replacement=new ImmutableClassDef(target.getType(),target.getAccessFlags(),target.getSuperclass(),target.getInterfaces(),target.getSourceFile(),target.getAnnotations(),target.getFields(),methods);
        next.put(TARGET,replacement);next.putAll(added);
        Files.write(Path.of(args[2]),serialize(next.values()));Map<String,ClassDef> emitted=read(args[2]);
        require(emitted.keySet().equals(next.keySet()),"Class set changed on serialization");int preserved=0,unrelatedMethods=0;
        for(var entry:old.entrySet())if(!entry.getKey().equals(TARGET)){
            require(hash(entry.getValue()).equals(hash(emitted.get(entry.getKey()))),"Unrelated class changed: "+entry.getKey());preserved++;
            for(Method m:entry.getValue().getMethods())unrelatedMethods++;
        }
        // Serialize single-method holders to compare all untouched methods in the edited loader.
        Map<String,Method> emittedMethods=new HashMap<>();for(Method m:emitted.get(TARGET).getMethods())emittedMethods.put(m.getName(),m);
        int preservedTargetMethods=0;
        for(Method m:target.getMethods())if(!Set.of("manifest","<clinit>","about").contains(m.getName())){
            ClassDef before=new ImmutableClassDef(TARGET,1,"Ljava/lang/Object;",List.of(),null,List.of(),List.of(),List.of(m));
            ClassDef after=new ImmutableClassDef(TARGET,1,"Ljava/lang/Object;",List.of(),null,List.of(),List.of(),List.of(emittedMethods.get(m.getName())));
            require(hash(before).equals(hash(after)),"Unexpected loader method change: "+m.getName());preservedTargetMethods++;
        }
        List<ClassDef> single=new ArrayList<>();for(ClassDef c:emitted.values())if(c.getType().startsWith(NS))single.add(c);
        require(single.size()==11,"Unexpected standalone class count");Files.write(Path.of(args[3]),serialize(single));
        System.out.println("PASS "+preserved+" existing classes / "+unrelatedMethods+" methods preserved structurally");
        System.out.println("PASS "+preservedTargetMethods+" other methods of BrightnessClick loader preserved");
        System.out.println("PASS manifest hook inserted after PermissionActivity attachment; versionCode 9 -> 10; patch metadata 1.0.4");
        System.out.println("PASS only three existing patch-loader methods changed; one patcher-only helper added; no APK runtime code changed");
    }
}
