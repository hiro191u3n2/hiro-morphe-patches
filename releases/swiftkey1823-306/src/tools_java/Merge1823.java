import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;

/** Lossless old dispatch bodies plus a narrowly scoped recent-emoji gesture interceptor. */
public final class Merge1823 {
    private static final String BASE_SHA="eef461b0d8825be2c36d88ef694f62c7f8ec0d756bd8500022738206c0abcf06";
    private static final String GLIDE="Lhiro/swiftkey/JapaneseGlide;", TOOLBAR="Lhiro/swiftkey/EmojiToolbar;";
    private static final String SLIDE="Lhiro/swiftkey/EmojiRecentsSlide;";
    private static final String SUFFIX="BeforeEmojiSlide1823";
    private static final Set<String> HOOKS=new TreeSet<>(Arrays.asList("handle","init","reset"));
    private static final Set<String> TOOLBAR_ADDITIONS=new TreeSet<>(Arrays.asList("emojiHoldMillis(Lfc0/n2;)I"));
    private static final DexFormatter FORMAT=DexFormatter.INSTANCE;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException(why);}
    private static String sha(String path)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(path))));}
    private static boolean fresh(String type){return type.startsWith("Lhiro/swiftkey/EmojiRecents");}
    private static boolean toolbar(String type){return type.equals(TOOLBAR)||type.startsWith("Lhiro/swiftkey/EmojiToolbar$");}
    private static TreeMap<String,ClassDef> load(String path)throws Exception{
        TreeMap<String,ClassDef> out=new TreeMap<>();
        for(ClassDef c:DexFileFactory.loadDexFile(new File(path),Opcodes.forApi(26)).getClasses())require(out.put(c.getType(),c)==null,"Duplicate type");
        return out;
    }
    private static byte[] canonical(ClassDef c)throws Exception{
        require(c!=null,"Missing class");DexPool pool=new DexPool(Opcodes.forApi(26));pool.internClass(c);MemoryDataStore out=new MemoryDataStore();
        try{pool.writeTo(out);return Arrays.copyOf(out.getBuffer(),out.getSize());}finally{out.close();}
    }
    private static String signature(MethodReference m){StringBuilder b=new StringBuilder(m.getName()).append('(');for(CharSequence p:m.getParameterTypes())b.append(p);return b.append(')').append(m.getReturnType()).toString();}
    private static Method named(ClassDef c,String name){Method found=null;for(Method m:c.getMethods())if(m.getName().equals(name)){require(found==null,"Overloaded hook "+name);found=m;}require(found!=null,"Missing "+name);return found;}
    private static ClassDef withMethods(ClassDef c,List<Method> methods){return new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods);}
    private static Method renamed(Method m,String name){return new ImmutableMethod(m.getDefiningClass(),name,m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),m.getImplementation());}
    private static ImmutableMethodReference reference(String owner,String name,Method m){return new ImmutableMethodReference(owner,name,m.getParameterTypes(),m.getReturnType());}
    private static Method wrapper(Method old){
        String name=old.getName();require(old.getAccessFlags()==9&&old.getImplementation()!=null,"Original hook ABI changed");
        MutableMethodImplementation body;
        if(name.equals("handle")){
            require(signature(old).equals("handle(Lxd0/d1;Llh0/a;Landroid/view/MotionEvent;)Z"),"Touch signature changed");
            body=new MutableMethodImplementation(4);
            body.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,1,2,3,0,0,reference(SLIDE,name,old)));
            body.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
            body.addInstruction(new BuilderInstruction10x(Opcode.NOP));
            body.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,1,2,3,0,0,reference(GLIDE,name+SUFFIX,old)));
            body.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT,0));
            body.addInstruction(new BuilderInstruction11x(Opcode.RETURN,0));
            body.replaceInstruction(2,new BuilderInstruction21t(Opcode.IF_NEZ,0,body.newLabelForIndex(5)));
        }else{
            int count=name.equals("init")?1:0;
            require(signature(old).equals(name.equals("init")?"init(Landroid/content/SharedPreferences;)V":"reset()V"),"Lifecycle signature changed");
            body=new MutableMethodImplementation(count);
            body.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC,count,0,0,0,0,0,reference(GLIDE,name+SUFFIX,old)));
            body.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC,count,0,0,0,0,0,reference(SLIDE,name,old)));
            body.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        }
        return new ImmutableMethod(GLIDE,name,old.getParameters(),old.getReturnType(),old.getAccessFlags(),old.getAnnotations(),old.getHiddenApiRestrictions(),body);
    }
    private static ClassDef wrapGlide(ClassDef c){
        List<Method> out=new ArrayList<>();
        for(Method m:c.getMethods()){
            require(!m.getName().endsWith(SUFFIX),"Recent emoji hook already installed");
            if(HOOKS.contains(m.getName())){out.add(renamed(m,m.getName()+SUFFIX));out.add(wrapper(m));}else out.add(m);
        }
        for(String name:HOOKS)named(c,name);
        return withMethods(c,out);
    }
    private static ClassDef restoreGlide(ClassDef c){
        List<Method> out=new ArrayList<>();
        for(Method m:c.getMethods()){
            if(HOOKS.contains(m.getName()))continue;
            String name=m.getName();
            out.add(name.endsWith(SUFFIX)?renamed(m,name.substring(0,name.length()-SUFFIX.length())):m);
        }
        return withMethods(c,out);
    }
    private static ClassDef addToolbar(ClassDef baseline,ClassDef overlay){
        require(overlay!=null,"Missing compiled EmojiToolbar key identity helper");
        Set<String> old=new HashSet<>(),added=new TreeSet<>();List<Method> out=new ArrayList<>();
        for(Method m:baseline.getMethods()){old.add(signature(m));out.add(m);}
        for(Method m:overlay.getMethods())if(!old.contains(signature(m))){
            require(m.getAccessFlags()==9&&m.getImplementation()!=null,"Key identity helper must be public static");
            added.add(signature(m));out.add(m);
        }
        require(added.equals(TOOLBAR_ADDITIONS),"Unexpected EmojiToolbar additions: "+added);
        return withMethods(baseline,out);
    }
    private static ClassDef restoreToolbar(ClassDef c){List<Method> out=new ArrayList<>();for(Method m:c.getMethods())if(!TOOLBAR_ADDITIONS.contains(signature(m)))out.add(m);return withMethods(c,out);}
    private static List<Instruction> instructions(Method m){List<Instruction> out=new ArrayList<>();for(Instruction i:m.getImplementation().getInstructions())out.add(i);return out;}
    private static void verifyWrappers(ClassDef c){
        for(String name:HOOKS){
            Method m=named(c,name),prior=named(c,name+SUFFIX);List<Instruction> code=instructions(m);
            require(m.getAccessFlags()==9&&prior.getAccessFlags()==9&&m.getImplementation().getTryBlocks().isEmpty(),"Wrapper flags/try blocks changed");
            if(name.equals("handle")){
                Opcode[] ops={Opcode.INVOKE_STATIC,Opcode.MOVE_RESULT,Opcode.IF_NEZ,Opcode.INVOKE_STATIC,Opcode.MOVE_RESULT,Opcode.RETURN};
                require(code.size()==ops.length&&m.getImplementation().getRegisterCount()==4,"Touch wrapper size/registers differ");
                for(int i=0;i<ops.length;i++)require(code.get(i).getOpcode()==ops[i],"Touch wrapper opcode differs");
                require(((OffsetInstruction)code.get(2)).getCodeOffset()==6,"Consumed branch target differs");
                for(int i:new int[]{1,2,4,5})require(((OneRegisterInstruction)code.get(i)).getRegisterA()==0,"Wrapper result register differs");
                for(int i:new int[]{0,3}){
                    FiveRegisterInstruction call=(FiveRegisterInstruction)code.get(i);
                    require(call.getRegisterCount()==3&&call.getRegisterC()==1&&call.getRegisterD()==2&&call.getRegisterE()==3,"Touch arguments differ");
                }
                require(FORMAT.getReference(((ReferenceInstruction)code.get(0)).getReference()).equals(SLIDE+"->"+signature(m)),"Helper dispatch differs");
                require(FORMAT.getReference(((ReferenceInstruction)code.get(3)).getReference()).equals(GLIDE+"->"+signature(prior)),"Original dispatch differs");
            }else{
                int count=name.equals("init")?1:0;
                require(code.size()==3&&m.getImplementation().getRegisterCount()==count,"Lifecycle wrapper size/registers differ");
                require(code.get(0).getOpcode()==Opcode.INVOKE_STATIC&&code.get(1).getOpcode()==Opcode.INVOKE_STATIC&&code.get(2).getOpcode()==Opcode.RETURN_VOID,"Lifecycle opcode differs");
                for(int i=0;i<2;i++){FiveRegisterInstruction call=(FiveRegisterInstruction)code.get(i);require(call.getRegisterCount()==count&&call.getRegisterC()==0,"Lifecycle arguments differ");}
                require(FORMAT.getReference(((ReferenceInstruction)code.get(0)).getReference()).equals(GLIDE+"->"+signature(prior)),"Original lifecycle dispatch differs");
                require(FORMAT.getReference(((ReferenceInstruction)code.get(1)).getReference()).equals(SLIDE+"->"+signature(m)),"Helper lifecycle dispatch differs");
            }
        }
    }
    private static String json(Object v){
        if(v==null)return "null";if(v instanceof Boolean||v instanceof Number)return v.toString();
        if(v instanceof Map){StringJoiner s=new StringJoiner(",","{","}");for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet())s.add(json(e.getKey())+":"+json(e.getValue()));return s.toString();}
        if(v instanceof Iterable){StringJoiner s=new StringJoiner(",","[","]");for(Object e:(Iterable<?>)v)s.add(json(e));return s.toString();}
        return "\""+v.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";
    }
    public static void main(String[] args)throws Exception{
        require(args.length==4,"Merge1823 baseline.mpe overlay.dex output.mpe report.json");
        require(sha(args[0]).equals(BASE_SHA),"Published1.8.22 extension pin differs");
        TreeMap<String,ClassDef> before=load(args[0]),overlay=load(args[1]);
        require(before.size()==84&&before.containsKey("Lhiro/swiftkey/EmojiFooter;"),"Unexpected published runtime inventory");
        require(overlay.containsKey(SLIDE)&&overlay.containsKey("Lhiro/swiftkey/EmojiRecentsBridge;"),"Missing picker runtime families");
        TreeMap<String,ClassDef> merged=new TreeMap<>(before);List<String> added=new ArrayList<>();
        for(ClassDef c:overlay.values()){
            require(fresh(c.getType())||toolbar(c.getType()),"Unexpected compiled runtime class");
            if(fresh(c.getType())){require(!before.containsKey(c.getType()),"Picker runtime already exists");merged.put(c.getType(),c);added.add(c.getType());}
        }
        merged.put(GLIDE,wrapGlide(before.get(GLIDE)));
        merged.put(TOOLBAR,addToolbar(before.get(TOOLBAR),overlay.get(TOOLBAR)));
        verifyWrappers(merged.get(GLIDE));
        require(Arrays.equals(canonical(before.get(GLIDE)),canonical(restoreGlide(merged.get(GLIDE)))),"Original dispatch cannot be restored exactly");
        require(Arrays.equals(canonical(before.get(TOOLBAR)),canonical(restoreToolbar(merged.get(TOOLBAR)))),"Original toolbar cannot be restored exactly");
        DexPool pool=new DexPool(Opcodes.forApi(26));for(ClassDef c:merged.values())pool.internClass(c);
        FileDataStore out=new FileDataStore(new File(args[2]));try{pool.writeTo(out);}finally{out.close();}
        TreeMap<String,ClassDef> result=load(args[2]);require(result.keySet().equals(merged.keySet()),"Serialized runtime inventory differs");
        for(ClassDef c:before.values()){
            ClassDef actual=result.get(c.getType());
            if(c.getType().equals(GLIDE))actual=restoreGlide(actual);else if(c.getType().equals(TOOLBAR))actual=restoreToolbar(actual);
            require(Arrays.equals(canonical(c),canonical(actual)),"Existing body or class metadata changed: "+c.getType());
        }
        for(String type:added)require(Arrays.equals(canonical(overlay.get(type)),canonical(result.get(type))),"Compiled picker helper changed in merge");
        verifyWrappers(result.get(GLIDE));
        // The only added method must exactly match its separately compiled implementation.
        ClassDef actualToolbar=result.get(TOOLBAR),compiledToolbar=overlay.get(TOOLBAR);
        require(Arrays.equals(canonical(withMethods(actualToolbar,Arrays.asList(named(actualToolbar,"emojiHoldMillis")))),
                canonical(withMethods(actualToolbar,Arrays.asList(named(compiledToolbar,"emojiHoldMillis"))))),"Key helper differs from compiled source");
        Map<String,Object> report=new LinkedHashMap<>();
        report.put("status","PASSED");report.put("scope","Lossless original gesture/lifecycle bodies; additive exact emoji-key identity and slide helpers");
        report.put("baseline_extension_sha256",sha(args[0]));report.put("overlay_sha256",sha(args[1]));report.put("extension_sha256",sha(args[2]));
        report.put("baseline_classes",84);report.put("runtime_after_classes",result.size());report.put("unchanged_existing_classes",82);
        report.put("changed_existing_classes",Arrays.asList(TOOLBAR,GLIDE));report.put("added_classes",added);
        report.put("unrelated_existing_classes_byte_identical",true);report.put("all_original_method_bodies_preserved",true);
        report.put("original_glide_restored_byte_identical",true);report.put("original_toolbar_restored_byte_identical",true);
        report.put("touch_dispatch_calls_helper_once",true);report.put("original_dispatch_called_only_when_not_consumed",true);
        report.put("init_and_reset_call_original_and_helper_once",true);report.put("wrapper_arguments_and_branch_verified",true);
        report.put("compiled_new_helpers_preserved",true);report.put("emoji_key_helper_matches_compiled_source",true);
        report.put("device_tested",false);report.put("original_apk_apply_tested",false);
        Files.writeString(Path.of(args[3]),json(report)+"\n");
        System.out.println("PASS:82 untouched classes; all original methods restored exactly;3 gesture/lifecycle wrappers; picker additions="+added.size());
    }
}
