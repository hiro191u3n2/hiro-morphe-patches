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
    private static final String MONTH = "Lorg/fossify/calendar/views/MonthView;";
    private static final String PREPARE = "Lit1;";
    private static final String DRAW = "onDraw(Landroid/graphics/Canvas;)V";
    private static final String DAYS = "a(Z)V";
    private static final Set<String> OWNERS = new TreeSet<>(Arrays.asList(OWNER, MONTH, PREPARE));
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
        Map<String, ClassDef> base = read(basePath), addition = read(additionPath), merged = new TreeMap<>();
        check(!addition.isEmpty() && addition.containsKey(PATCH), "replacement Fossify patch loader missing");
        int replaced = 0;
        for (ClassDef c : base.values()) {
            if (c.getType().startsWith(LOADER)) replaced++;
            else merged.put(c.getType(), c);
        }
        check(replaced > 0, "reviewed Calendar baseline loader missing");
        for (ClassDef c : addition.values()) {
            check(c.getType().startsWith(LOADER), "replacement loader outside Fossify scope: " + c.getType());
            check(merged.put(c.getType(), c) == null, "replacement collides outside Calendar scope: " + c.getType());
        }
        DexPool.writeTo(outputPath, new ImmutableDexFile(Opcodes.getDefault(), merged.values()));
        Map<String, ClassDef> written = read(outputPath);
        check(written.keySet().equals(merged.keySet()), "merged loader inventory differs");
        for (ClassDef c : base.values()) {
            if (c.getType().startsWith(LOADER)) continue;
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                    "unrelated existing loader changed: " + c.getType());
        }
        for (ClassDef c : addition.values()) {
            check(Arrays.equals(canonical(c), canonical(written.get(c.getType()))),
                    "replacement Calendar loader changed: " + c.getType());
        }
        System.out.println("PASS loader merge: unrelated existing classes unchanged=" + (base.size()-replaced)
                + "; old Calendar loader classes replaced=" + replaced + "; new Calendar loader classes=" + addition.size() + "; assertions=" + assertions);
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
    /** Independently reconstruct widget weekday/date hooks and the black today-text replacement. */
    private static Method expectedWidget(Method original) {
        MutableMethodImplementation impl = new MutableMethodImplementation(original.getImplementation());
        check(impl.getRegisterCount()==56, "target callback register layout changed");
        List<? extends Instruction> source = impl.getInstructions();
        int header=-1, day=-1, today=-1;
        for(int i=0;i<source.size();i++) {
            Instruction cur=source.get(i);
            if(call(cur,"Lx11;","C","I","I") && cur instanceof FiveRegisterInstruction
                && ((FiveRegisterInstruction)cur).getRegisterCount()==1 && ((FiveRegisterInstruction)cur).getRegisterC()==2) {
                check(today<0,"duplicate widget today contrast");today=i;
                check(i+1<source.size() && source.get(i+1).getOpcode()==Opcode.MOVE_RESULT
                    && ((OneRegisterInstruction)source.get(i+1)).getRegisterA()==7,"widget today result changed");
            }
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
        check(header>=0&&day>header&&today>day,"native widget anchors missing or reordered");
        impl.replaceInstruction(today,new BuilderInstruction21ih(Opcode.CONST_HIGH16,7,0xff000000));
        impl.replaceInstruction(today+1,new BuilderInstruction10x(Opcode.NOP));
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
        return copy(original,original.getName(),original.getAccessFlags(),impl);
    }
    private static boolean registers(Instruction instruction, int... expected) {
        if(!(instruction instanceof FiveRegisterInstruction)) return false;
        FiveRegisterInstruction r=(FiveRegisterInstruction)instruction;
        int[] actual={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};
        if(r.getRegisterCount()!=expected.length)return false;
        for(int i=0;i<expected.length;i++)if(actual[i]!=expected[i])return false;
        return true;
    }
    private static boolean field(Instruction instruction, Opcode opcode, int a, int b, String owner, String name, String type) {
        if(instruction.getOpcode()!=opcode || !(instruction instanceof TwoRegisterInstruction))return false;
        TwoRegisterInstruction r=(TwoRegisterInstruction)instruction;
        if(r.getRegisterA()!=a || r.getRegisterB()!=b)return false;
        Reference ref=((ReferenceInstruction)instruction).getReference();
        if(!(ref instanceof FieldReference))return false;
        FieldReference f=(FieldReference)ref;
        return owner.equals(f.getDefiningClass())&&name.equals(f.getName())&&type.equals(f.getType());
    }
    private static Method expectedMonth(Method original) {
        MutableMethodImplementation impl=new MutableMethodImplementation(original.getImplementation());
        check(impl.getRegisterCount()==34,"app onDraw register layout changed");
        List<? extends Instruction> source=impl.getInstructions();
        int header=-1,selection=-1,circle=-1,date=-1;
        for(int i=0;i<source.size();i++) {
            Instruction cur=source.get(i);
            if(field(cur,Opcode.IGET_OBJECT,3,0,MONTH,"D","Ljava/util/ArrayList;")) {
                check(header<0,"duplicate app weekday anchor");header=i;
                check(i+1<source.size() && call(source.get(i+1),"Ljava/util/ArrayList;","get","Ljava/lang/Object;","I")
                    && registers(source.get(i+1),3,2),"weekday index anchor differs");
            }
            if(cur.getOpcode()==Opcode.IF_EQZ && ((OneRegisterInstruction)cur).getRegisterA()==6 && i>0
                && source.get(i-1).getOpcode()==Opcode.CONST_4
                && ((OneRegisterInstruction)source.get(i-1)).getRegisterA()==11
                && ((NarrowLiteralInstruction)source.get(i-1)).getNarrowLiteral()==2) {
                check(selection<0,"duplicate selected-day branch");selection=i;
            }
            if(call(cur,"Landroid/graphics/Canvas;","drawCircle","V","F","F","F","Landroid/graphics/Paint;")&&registers(cur,1,3,2,8,11)) {
                check(circle<0,"duplicate today circle draw anchor");circle=i;
                check(i>0 && call(source.get(i-1),"Landroid/graphics/Paint;","setColor","V","I")
                    && registers(source.get(i-1),11,5),"today circle color predecessor changed");
            }
            if(call(cur,"Landroid/graphics/Canvas;","drawText","V","Ljava/lang/String;","F","F","Landroid/graphics/Paint;")&&registers(cur,1,14,3,9,15)) {
                check(date<0,"duplicate final day numeral anchor");date=i;
            }
        }
        check(header>=0 && selection>header && circle>selection && date>circle,"app month anchors missing/reordered");
        BuilderInstruction old=impl.getInstructions().get(date);
        impl.replaceInstruction(date,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,0,13,15,0,0,
            new ImmutableMethodReference(RUNTIME,"appDatePaint",Arrays.asList("Landroid/view/View;","Ljava/lang/Object;","Landroid/graphics/Paint;"),"V")));
        impl.addInstruction(date+1,old);
        old=impl.getInstructions().get(circle);
        impl.replaceInstruction(circle,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,0,13,11,0,0,
            new ImmutableMethodReference(RUNTIME,"appCirclePaint",Arrays.asList("Landroid/view/View;","Ljava/lang/Object;","Landroid/graphics/Paint;"),"V")));
        impl.addInstruction(circle+1,old);
        old=impl.getInstructions().get(selection);
        impl.replaceInstruction(selection,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,0,13,6,0,0,
            new ImmutableMethodReference(RUNTIME,"drawSelection",Arrays.asList("Landroid/view/View;","Ljava/lang/Object;","Z"),"Z")));
        impl.addInstruction(selection+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,6));
        impl.addInstruction(selection+2,old);
        old=impl.getInstructions().get(header);
        impl.replaceInstruction(header,new BuilderInstruction35c(Opcode.INVOKE_STATIC,3,0,2,5,0,0,
            new ImmutableMethodReference(RUNTIME,"appWeekdayPaint",Arrays.asList("Landroid/view/View;","I","Landroid/graphics/Paint;"),"Landroid/graphics/Paint;")));
        impl.addInstruction(header+1,new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT,5));
        impl.addInstruction(header+2,old);
        impl.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,32,1,
            new ImmutableMethodReference(RUNTIME,"appBeginRender",Collections.singletonList("Landroid/view/View;"),"V")));
        return copy(original,original.getName(),original.getAccessFlags(),impl);
    }
    private static Method expectedDays(Method original) {
        MutableMethodImplementation impl=new MutableMethodImplementation(original.getImplementation());
        check(impl.getRegisterCount()==22,"monthly day preparation register layout changed");
        List<? extends Instruction> source=impl.getInstructions();int context=-1;
        for(int i=0;i<source.size();i++) {
            if(field(source.get(i),Opcode.IGET_OBJECT,5,0,PREPARE,"b","Landroid/content/Context;")) {
                check(context<0,"duplicate monthly context anchor");context=i;
                check(i+1<source.size()&&call(source.get(i+1),"Lq50;","o","I","Landroid/content/Context;","Lorg/joda/time/DateTime;")
                    &&registers(source.get(i+1),5,2),"monthly context successor changed");
            }
        }
        check(context>=0,"monthly context anchor missing");
        BuilderInstruction old=impl.getInstructions().get(context+1);
        impl.replaceInstruction(context+1,new BuilderInstruction35c(Opcode.INVOKE_STATIC,1,5,0,0,0,0,
            new ImmutableMethodReference(RUNTIME,"beginRender",Collections.singletonList("Landroid/content/Context;"),"V")));
        impl.addInstruction(context+2,old);
        return copy(original,original.getName(),original.getAccessFlags(),impl);
    }
    private static Method expected(Method original) {
        String owner=original.getDefiningClass(), sig=signature(original);
        if(OWNER.equals(owner)&&CALLBACK.equals(sig))return expectedWidget(original);
        if(MONTH.equals(owner)&&DRAW.equals(sig))return expectedMonth(original);
        if(PREPARE.equals(owner)&&DAYS.equals(sig))return expectedDays(original);
        throw new AssertionError("Unreviewed transformation: "+owner+sig);
    }
    private static String targetSignature(String owner) {
        if(OWNER.equals(owner))return CALLBACK;
        if(MONTH.equals(owner))return DRAW;
        if(PREPARE.equals(owner))return DAYS;
        return null;
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
        Map<String,Integer> counts=new TreeMap<>();
        for(ClassDef before:original.values()) {
            ClassDef after=output.get(before.getType());
            if(!OWNERS.contains(before.getType())) {
                check(Arrays.equals(canonical(before),canonical(after)),"non-target class changed: "+before.getType());preserved++;
            } else {
                Map<String,Method> oldMethods=methods(before),newMethods=methods(after);
                check(oldMethods.keySet().equals(newMethods.keySet()),"target class method inventory changed");
                List<Method> expectedMethods=new ArrayList<>();
                for(Method m:before.getMethods()) {
                    if(targetSignature(before.getType()).equals(signature(m))) {expectedMethods.add(expected(m));target++;}
                    else expectedMethods.add(normalizedOriginal(m));
                }
                ClassDef expectedClass=new ImmutableClassDef(before.getType(),before.getAccessFlags(),before.getSuperclass(),
                    before.getInterfaces(),before.getSourceFile(),before.getAnnotations(),before.getFields(),expectedMethods);
                check(Arrays.equals(canonical(expectedClass),canonical(after)),"target class differs beyond reviewed color hooks: "+before.getType());
            }
            for(Method m:after.getMethods()) {
                if(m.getImplementation()==null)continue;
                for(Instruction i:m.getImplementation().getInstructions()) {
                    MethodReference r=called(i);
                    if(r!=null && r.getDefiningClass().startsWith(RUNTIME_PREFIX)) {
                        check(OWNERS.contains(after.getType())&&targetSignature(after.getType()).equals(signature(m)),"color hook outside reviewed method");
                        check(RUNTIME.equals(r.getDefiningClass())&&Arrays.asList(
                            "beginRender(Landroid/content/Context;)V","weekdayColor(III)I",
                            "dateColor(Landroid/content/Context;Ljava/lang/Object;I)I",
                            "appBeginRender(Landroid/view/View;)V",
                            "appWeekdayPaint(Landroid/view/View;ILandroid/graphics/Paint;)Landroid/graphics/Paint;",
                            "appDatePaint(Landroid/view/View;Ljava/lang/Object;Landroid/graphics/Paint;)V",
                            "drawSelection(Landroid/view/View;Ljava/lang/Object;Z)Z",
                            "appCirclePaint(Landroid/view/View;Ljava/lang/Object;Landroid/graphics/Paint;)V").contains(signature(r)),"unknown color runtime call");
                        counts.put(after.getType(),counts.getOrDefault(after.getType(),0)+1);calls++;
                    }
                }
            }
        }
        check(target==3&&calls==8&&counts.get(OWNER)==2&&counts.get(MONTH)==5&&counts.get(PREPARE)==1,
            "exactly three target methods and eight color hooks required");
        System.out.println("PASS APK audit: unchanged non-target classes="+preserved+"; exact reviewed methods=3; runtime hooks=8; widget today black constant=1; runtime classes="+runtime.size()+"; assertions="+assertions+"; physical Android behavior not tested.");
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
            Map<String,ClassDef> classes=read(apk);
            for(String owner:OWNERS) {
                ClassDef c=classes.get(owner);
                Method original=ImmutableMethod.of(CalendarMonthColorsPatch.validateClass(c));
                MutableClass changed=new MutableClass(c);
                check(CalendarMonthColorsPatch.apply(changed,original)>0,"production edit count");
                Method actual=methods(changed).get(targetSignature(owner));
                check(Arrays.equals(canonical(expected(original)),canonical(actual)),"production injection differs from independent plan: "+owner);
                rejected(()->CalendarMonthColorsPatch.validateClass(changed),"already-patched body accepted: "+owner);
                MutableClass modified=new MutableClass(c);
                for(app.morphe.patcher.util.proxy.mutableTypes.MutableMethod m:modified.getMethods())
                    if(targetSignature(owner).equals(signature(m)))m.getImplementation().addInstruction(0,new BuilderInstruction10x(Opcode.NOP));
                rejected(()->CalendarMonthColorsPatch.validateClass(modified),"modified original body accepted: "+owner);
                rejected(()->CalendarMonthColorsPatch.apply(null,original),"missing mutable owner accepted: "+owner);
            }
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
