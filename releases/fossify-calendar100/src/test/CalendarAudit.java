import java.io.File;
import app.hiro.fossifycalendar.patches.CalendarMonthColorsPatch;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;

/** Independent, exact-scope DEX audits. These checks do not imply an Android device test. */
public final class CalendarAudit {
    private static final String LOADER = "Lapp/hiro/fossifycalendar/patches/";
    private static final String PATCH = LOADER + "CalendarMonthColorsPatch;";
    private static final String RUNTIME_PREFIX = "Lapp/hiro/fossifycalendar/runtime/";
    private static final String RUNTIME = RUNTIME_PREFIX + "MonthColors;";
    private static final String OWNER = "Lvn0;";
    private static final String CALLBACK = "i(Landroid/content/Context;Ljava/lang/String;Ljava/util/ArrayList;ZLorg/joda/time/DateTime;)V";
    private static int assertions;

    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }

    private static Map<String, ClassDef> read(String path) throws Exception {
        Map<String, ClassDef> result = new TreeMap<>();
        MultiDexContainer<? extends DexFile> container =
                DexFileFactory.loadDexContainer(new File(path), Opcodes.getDefault());
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef c : container.getEntry(entry).getDexFile().getClasses()) {
                check(result.put(c.getType(), c) == null, "duplicate DEX class: " + c.getType());
            }
        }
        return result;
    }

    /** Serializing one class at a time makes changed DEX indices irrelevant to equality. */
    private static byte[] canonical(ClassDef c) throws Exception {
        check(c != null, "missing class");
        MemoryDataStore store = new MemoryDataStore();
        try {
            DexPool.writeTo(store, new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(c)));
            return Arrays.copyOf(store.getBuffer(), store.getSize());
        } finally { store.close(); }
    }

    private static byte[] canonical(Method m) throws Exception {
        return canonical(new ImmutableClassDef(m.getDefiningClass(), 1, "Ljava/lang/Object;",
                Collections.<String>emptyList(), null, Collections.<Annotation>emptySet(),
                Collections.<Field>emptyList(), Collections.singleton(m)));
    }

    private static String signature(MethodReference m) {
        StringBuilder s = new StringBuilder(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) s.append(p);
        return s.append(')').append(m.getReturnType()).toString();
    }

    private static Map<String, Method> methods(ClassDef c) {
        Map<String, Method> result = new TreeMap<>();
        for (Method m : c.getMethods()) {
            check(result.put(signature(m), m) == null, "duplicate method: " + c.getType() + signature(m));
        }
        return result;
    }

    private static Method copy(Method from, String name, int access, MethodImplementation implementation) {
        return new ImmutableMethod(from.getDefiningClass(), name, from.getParameters(), from.getReturnType(),
                access, from.getAnnotations(), from.getHiddenApiRestrictions(), implementation);
    }

    /**
     * Morphe materializes every method of an edited class as MutableMethodImplementation.
     * DEX debug entries may point into the middle of an instruction; materialization attaches
     * each such entry to that instruction's start. Specify that exact conversion here rather
     * than discarding debug information or weakening code/handler/metadata comparisons.
     */
    private static Method normalizedOriginal(Method original) {
        MethodImplementation implementation = original.getImplementation();
        return copy(original, original.getName(), original.getAccessFlags(),
                implementation == null ? null : new MutableMethodImplementation(implementation));
    }

    private static ClassDef normalizedOriginal(ClassDef original) {
        List<Method> normalized = new ArrayList<>();
        for (Method method : original.getMethods()) normalized.add(normalizedOriginal(method));
        return new ImmutableClassDef(original.getType(), original.getAccessFlags(), original.getSuperclass(),
                original.getInterfaces(), original.getSourceFile(), original.getAnnotations(),
                original.getFields(), normalized);
    }

    private static MethodReference called(Instruction instruction) {
        if (!(instruction instanceof ReferenceInstruction)) return null;
        Reference reference = ((ReferenceInstruction) instruction).getReference();
        return reference instanceof MethodReference ? (MethodReference) reference : null;
    }

    private static List<Instruction> instructions(Method m) {
        check(m.getImplementation() != null, "callback implementation missing: " + m);
        List<Instruction> result = new ArrayList<>();
        for (Instruction instruction : m.getImplementation().getInstructions()) result.add(instruction);
        return result;
    }

    private static void merge(String basePath, String additionPath, String outputPath) throws Exception {
        Map<String, ClassDef> base = read(basePath), addition = read(additionPath), merged = new TreeMap<>(base);
        check(!addition.isEmpty() && addition.containsKey(PATCH), "new Fossify patch loader missing");
        for (ClassDef c : addition.values()) {
            check(c.getType().startsWith(LOADER), "new loader outside Fossify scope: " + c.getType());
            check(merged.put(c.getType(), c) == null, "new loader collides with baseline: " + c.getType());
        }
        DexPool.writeTo(outputPath, new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String, ClassDef> written = read(outputPath);
        check(written.keySet().equals(merged.keySet()), "merged loader inventory differs");
        for (ClassDef c : base.values()) {
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                    "existing loader changed: " + c.getType());
        }
        for (ClassDef c : addition.values()) {
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                    "new Fossify loader changed: " + c.getType());
        }
        System.out.println("PASS loader merge: every existing class unchanged=" + base.size()
                + "; Fossify loader classes added=" + addition.size() + "; assertions=" + assertions);
    }


    private static boolean call(Instruction insn, String owner, String name, String ret, String...params) {
        MethodReference ref = called(insn);
        return ref != null && owner.equals(ref.getDefiningClass()) && name.equals(ref.getName())
            && ret.equals(ref.getReturnType()) && paramNames(ref).equals(Arrays.asList(params));
    }
    private static List<String> paramNames(MethodReference ref) {
        List<String> names=new ArrayList<>();for(CharSequence p:ref.getParameterTypes())names.add(p.toString());return names;
    }
    private static boolean registers35(Instruction insn, int c,int d,int e) {
        if (!(insn instanceof FiveRegisterInstruction)) return false;
        FiveRegisterInstruction r=(FiveRegisterInstruction)insn;
        return r.getRegisterCount()==3 && r.getRegisterC()==c && r.getRegisterD()==d && r.getRegisterE()==e;
    }
    /** Reconstruct only the three reviewed hook insertions, independently of the patch implementation. */
    private static Method expected(Method original) {
        MutableMethodImplementation impl = new MutableMethodImplementation(original.getImplementation());
        check(impl.getRegisterCount()==56, "target callback register layout changed");
        List<? extends Instruction> source = impl.getInstructions();
        int header=-1, day=-1;
        for(int i=0;i<source.size();i++) {
            Instruction cur=source.get(i);
            if(call(cur,"Landroid/widget/RemoteViews;","setTextColor","V","I","I") && registers35(cur,15,8,11)) {
                check(header<0,"duplicate reviewed header color call");header=i;
            }
            if(cur.getOpcode()==Opcode.NEW_INSTANCE && ((OneRegisterInstruction)cur).getRegisterA()==10
               && ((TypeReference)((ReferenceInstruction)cur).getReference()).getType().equals("Ljava/lang/StringBuilder;")
               && i+1<source.size()) {
                Instruction next=source.get(i+1);
                if(next.getOpcode()==Opcode.CONST_STRING && ((OneRegisterInstruction)next).getRegisterA()==1
                   && ((StringReference)((ReferenceInstruction)next).getReference()).getString().equals("day_")) {
                    check(day<0,"duplicate reviewed day color anchor");day=i;
                }
            }
        }
        check(header>=0&&day>header,"native widget anchors missing or reordered");
        // Replace anchors, then append native operations. Labels entering the old anchor
        // now enter the injected color calculation, including alternate-month branches.
        BuilderInstruction oldDay=impl.getInstructions().get(day);
        impl.replaceInstruction(day,new BuilderInstruction22x(Opcode.MOVE_OBJECT_FROM16,1,24));
        impl.addInstruction(day+1,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,0,1,2,0,0,
          new ImmutableMethodReference(RUNTIME,"dateColor",Arrays.asList("Landroid/content/Context;","Ljava/lang/Object;","I"),"I")));
        impl.addInstruction(day+2,new BuilderInstruction11x(Opcode.MOVE_RESULT,2));
        impl.addInstruction(day+3,oldDay);
        BuilderInstruction oldHeader=impl.getInstructions().get(header);
        impl.replaceInstruction(header,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,3,10,11,0,0,
          new ImmutableMethodReference(RUNTIME,"weekdayColor",Arrays.asList("I","I","I"),"I")));
        impl.addInstruction(header+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,11));
        impl.addInstruction(header+2,oldHeader);
        impl.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,51,1,
          new ImmutableMethodReference(RUNTIME,"beginRender",Collections.singletonList("Landroid/content/Context;"),"V")));
        return copy(original,original.getName(),original.getAccessFlags(),impl);
    }
    private static void auditApp(String originalPath,String outputPath,String runtimePath)throws Exception {
        Map<String,ClassDef> original=read(originalPath),output=read(outputPath),runtime=read(runtimePath);
        check(runtime.containsKey(RUNTIME),"compiled color runtime missing");
        Set<String> expectedInventory=new TreeSet<>(original.keySet());
        for(ClassDef c:runtime.values()) {
            check(c.getType().startsWith(RUNTIME_PREFIX),"runtime has unrelated class "+c.getType());
            check(expectedInventory.add(c.getType()),"runtime collision "+c.getType());
            check(Arrays.equals(canonical(c),canonical(output.get(c.getType()))),"runtime changed "+c.getType());
        }
        check(output.keySet().equals(expectedInventory),"unexpected APK class inventory");
        int preserved=0,target=0,calls=0;
        for(ClassDef before:original.values()) {
            ClassDef after=output.get(before.getType());
            if(!OWNER.equals(before.getType())) {
                check(Arrays.equals(canonical(before),canonical(after)),"non-target class changed: "+before.getType());preserved++;
            } else {
                Map<String,Method> oldMethods=methods(before),newMethods=methods(after);
                check(oldMethods.keySet().equals(newMethods.keySet()),"widget callback class method inventory changed");
                List<Method> expectedMethods=new ArrayList<>();
                for(Method m:before.getMethods()) {
                    if(CALLBACK.equals(signature(m))) {expectedMethods.add(expected(m));target++;}
                    else expectedMethods.add(normalizedOriginal(m));
                }
                ClassDef expectedClass=new ImmutableClassDef(before.getType(),before.getAccessFlags(),before.getSuperclass(),
                    before.getInterfaces(),before.getSourceFile(),before.getAnnotations(),before.getFields(),expectedMethods);
                check(Arrays.equals(canonical(expectedClass),canonical(after)),"target class differs beyond three reviewed color hooks");
            }
            for(Method m:after.getMethods()) {
                if(m.getImplementation()==null)continue;
                for(Instruction i:m.getImplementation().getInstructions()) {
                    MethodReference r=called(i);
                    if(r!=null && r.getDefiningClass().startsWith(RUNTIME_PREFIX)) {
                        check(OWNER.equals(after.getType())&&CALLBACK.equals(signature(m)),"color hook outside monthly widget");
                        check(RUNTIME.equals(r.getDefiningClass())&&Arrays.asList(
                            "beginRender(Landroid/content/Context;)V","weekdayColor(III)I",
                            "dateColor(Landroid/content/Context;Ljava/lang/Object;I)I").contains(signature(r)),"unknown color runtime call");calls++;
                    }
                }
            }
        }
        check(target==1&&calls==3,"exactly one callback and three color hook calls required");
        System.out.println("PASS APK audit: unchanged non-target classes="+preserved+"; exact widget hooks=3; runtime classes="+runtime.size()+"; assertions="+assertions+"; physical Android behavior not tested.");
    }
    private static void rejected(Runnable action,String message) {
        boolean failed=false;
        try{action.run();}catch(IllegalStateException expected){failed=true;}
        check(failed,message);
    }
    private static void guards(String apk)throws Exception {
        CalendarMonthColorsPatch.validatePackage("org.fossify.calendar","1.12.1","24");
        rejected(()->CalendarMonthColorsPatch.validatePackage("org.fossify.gallery","1.12.1","24"),"wrong package accepted");
        rejected(()->CalendarMonthColorsPatch.validatePackage("org.fossify.calendar","1.12.2","24"),"wrong version accepted");
        rejected(()->CalendarMonthColorsPatch.validatePackage("org.fossify.calendar","1.12.1","25"),"wrong code accepted");
        rejected(()->CalendarMonthColorsPatch.validateClass(null),"missing class accepted");
        if(apk!=null) {
            ClassDef c=read(apk).get(OWNER);
            Method original=ImmutableMethod.of(CalendarMonthColorsPatch.validateClass(c));
            MutableClass changed=new MutableClass(c);
            check(CalendarMonthColorsPatch.apply(changed,original)==3,"production hook count");
            Method actual=methods(changed).get(CALLBACK);
            check(Arrays.equals(canonical(expected(original)),canonical(actual)),"production injection differs from independent plan");
            rejected(()->CalendarMonthColorsPatch.validateClass(changed),"already-patched body accepted");
            MutableClass modified=new MutableClass(c);
            for(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod m:modified.getMethods())
                if(CALLBACK.equals(signature(m)))m.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
            rejected(()->CalendarMonthColorsPatch.validateClass(modified),"modified original body accepted");
            // Null/missing target classes must never be silently modified.
            rejected(()->CalendarMonthColorsPatch.apply(null,original),"missing mutable owner accepted");
        }
        System.out.println("PASS guards: wrong package/version/code, missing class"+(apk!=null?", reviewed native transform, modified and repatched bodies":"")+"; assertions="+assertions);
    }
    public static void main(String[]args)throws Exception {
        if(args.length==4 && "merge".equals(args[0]))merge(args[1],args[2],args[3]);
        else if(args.length==4 && "audit-app".equals(args[0]))auditApp(args[1],args[2],args[3]);
        else if(args.length>=1&&args.length<=2 && "guard-tests".equals(args[0]))guards(args.length==2?args[1]:null);
        else throw new IllegalArgumentException("merge BASE NEW OUT | audit-app ORIGINAL PATCHED RUNTIME");
    }
}
