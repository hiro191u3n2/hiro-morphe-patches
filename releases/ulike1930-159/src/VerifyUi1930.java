import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.reference.*;
import com.android.tools.smali.dexlib2.builder.*;
import com.android.tools.smali.dexlib2.builder.instruction.*;
import com.android.tools.smali.dexlib2.formatter.DexFormatter;

/** Independent audit of the serialized 1.9.30 UI-only DEX delta.
 *
 * The small VM below interprets the instructions from the supplied DEX files.
 * Its native selection result, clock, and view are controlled host fixtures;
 * it is not an Android emulator and does not claim device/camera execution.
 */
public final class VerifyUi1930 {
    static final String Z = "Lcom/hiro/ulike/OpticalZoom;";
    static final String B = "Lcom/hiro/ulike/OpticalZoomUi$Bar;";
    static final String H = "Lcom/hiro/ulike/RearLensUi1930;";
    static final String SHOW = Z + "->show()Z";
    static final String SELECT = Z + "->select(I)V";
    static final String DRAW = B + "->onPreDraw()Z";
    static final String HIDE = B + "->hide()V";
    static final String UPDATE = B + "->update()V";
    static final String REAR = H + "->rearSelected()Z";
    static final String MANUAL_SELECT = "Lcom/hiro/ulike/ManualLens170;->select(I)V";
    static final String CLOCK = "Landroid/os/SystemClock;->uptimeMillis()J";
    static final String OLD_META = "v1.9.29（v1.9.28基準）";
    static final String NEW_META = "v1.9.30（v1.9.29基準）";
    static final String PATCH = "Lapp/hiro/ulike/patches/UlikeHqMaxPatch;";
    static final Set<String> TARGETS = Set.of(SHOW, SELECT, DRAW);
    static final Map<String, Integer> PREFIX = Map.of(SHOW, 5, SELECT, 4, DRAW, 6);
    static final Set<String> NATIVE_FIELDS = Set.of(
            "Li/f/l/n/q/y/m;->a:Li/f/l/n/q/y/m;",
            "Li/f/l/n/q/y/m;->b:Li/f/l/j;",
            "Li/f/l/n/q/y/m;->c:Ljava/lang/String;");
    static final Set<String> NATIVE_METHODS = Set.of(
            "Li/f/l/j;->g(Ljava/lang/String;)Li/f/l/u/g;",
            "Li/f/l/u/g;->k()Li/f/l/u/j;",
            "Li/f/l/u/j;->x()Li/f/l/u/p;",
            "Li/f/l/u/p;->a()Ljava/lang/Object;");
    static int assertions;

    static void req(boolean ok, String text) {
        assertions++;
        if (!ok) throw new IllegalStateException(text);
    }
    static String id(Method m) { return DexFormatter.INSTANCE.getMethodDescriptor(m); }
    static String ref(Instruction i) {
        return i instanceof ReferenceInstruction r ? r.getReference().toString() : "";
    }
    static List<Instruction> ins(Method m) {
        req(m != null && m.getImplementation() != null, "Missing executable method");
        var out = new ArrayList<Instruction>();
        m.getImplementation().getInstructions().forEach(out::add);
        return out;
    }
    static Method body(Method m, MethodImplementation b) {
        return new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(),
                m.getReturnType(), m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), b);
    }
    static ClassDef methods(ClassDef c, List<Method> ms) {
        return new ImmutableClassDef(c.getType(), c.getAccessFlags(), c.getSuperclass(),
                c.getInterfaces(), c.getSourceFile(), c.getAnnotations(), c.getFields(), ms);
    }
    static String canonicalClassHash(ClassDef c) throws Exception {
        // ImmutableClassDef canonicalizes annotation sets. Apply that same
        // conversion on both sides; hashing a raw DexBackedClassDef against an
        // immutable reconstruction otherwise reports harmless set iteration
        // order as a content change. Nothing is omitted from serialization.
        var all = new ArrayList<Method>();
        c.getMethods().forEach(all::add);
        return MergePayloads.classHash(methods(c, all));
    }
    static int[] addresses(List<Instruction> instructions) {
        int[] out = new int[instructions.size() + 1];
        for (int k = 0; k < instructions.size(); k++) out[k + 1] = out[k] + instructions.get(k).getCodeUnits();
        return out;
    }
    static void one(Instruction i, Opcode op, int r) {
        req(i.getOpcode() == op && i instanceof OneRegisterInstruction x && x.getRegisterA() == r,
                "Unexpected opcode/register: " + i.getOpcode() + ", wanted " + op + " v" + r);
    }
    static void literal(Instruction i, int value) {
        one(i, Opcode.CONST_4, 0);
        req(i instanceof NarrowLiteralInstruction n && n.getNarrowLiteral() == value, "Guard return constant");
    }
    static int[] registers(Instruction i) {
        if (i instanceof FiveRegisterInstruction r) {
            int[] all = {r.getRegisterC(), r.getRegisterD(), r.getRegisterE(), r.getRegisterF(), r.getRegisterG()};
            return Arrays.copyOf(all, r.getRegisterCount());
        }
        if (i instanceof RegisterRangeInstruction r) {
            int[] out = new int[r.getRegisterCount()];
            for (int k = 0; k < out.length; k++) out[k] = r.getStartRegister() + k;
            return out;
        }
        throw new IllegalStateException("Unsupported invoke encoding " + i.getOpcode());
    }
    static void invoke(Instruction i, Opcode op, String target, int... regs) {
        req(i.getOpcode() == op && ref(i).equals(target) && Arrays.equals(registers(i), regs),
                "Wrong call/argument registers for " + target);
    }

    static Method checkGuard(Method old, Method emitted) {
        String key = id(old);
        int n = PREFIX.get(key);
        List<Instruction> actual = ins(emitted), original = ins(old);
        int expectedRegisters = key.equals(SHOW) ? 1 : key.equals(SELECT) ? 2 : 7;
        req(emitted.getImplementation().getRegisterCount() == expectedRegisters, "Pinned registers changed " + key);
        req(actual.size() == original.size() + n, "Extra/missing guard instructions " + key);
        req(old.getImplementation().getTryBlocks().isEmpty() && emitted.getImplementation().getTryBlocks().isEmpty(),
                "Guard must not alter exception regions " + key);
        invoke(actual.get(0), Opcode.INVOKE_STATIC, key.equals(DRAW) ? SHOW : REAR);
        one(actual.get(1), Opcode.MOVE_RESULT, 0);
        one(actual.get(2), Opcode.IF_NEZ, 0);
        int[] at = addresses(actual);
        req(actual.get(2) instanceof OffsetInstruction branch && at[2] + branch.getCodeOffset() == at[n],
                "Positive guard must jump to first original instruction " + key);
        if (key.equals(DRAW)) {
            invoke(actual.get(3), Opcode.INVOKE_VIRTUAL, HIDE, 6);
            literal(actual.get(4), 1);
            one(actual.get(5), Opcode.RETURN, 0);
            invoke(actual.get(n), Opcode.INVOKE_STATIC, CLOCK);
        } else if (key.equals(SHOW)) {
            literal(actual.get(3), 0);
            one(actual.get(4), Opcode.RETURN, 0);
        } else {
            req(actual.get(3).getOpcode() == Opcode.RETURN_VOID, "Denied click must return immediately");
            invoke(actual.get(n), Opcode.INVOKE_STATIC_RANGE, MANUAL_SELECT, 1);
            req(actual.get(n + 1).getOpcode() == Opcode.RETURN_VOID, "Rear click delegation must end normally");
        }
        // Deleting only the audited prefix must recover every original opcode,
        // operand, branch, register count, reference, and exception contract.
        var inverse = new MutableMethodImplementation(emitted.getImplementation());
        for (int k = n - 1; k >= 0; k--) inverse.removeInstruction(k);
        Method restored = body(emitted, inverse);
        req(MergePayloads.hash(old).equals(MergePayloads.hash(restored)), "Inverse guard failed MethodContract " + key);
        req(!MergePayloads.hash(old).equals(MergePayloads.hash(emitted)), "Guard did not change target " + key);
        return restored;
    }

    static int verifyRuntime(Map<String, ClassDef> before, Map<String, ClassDef> after) throws Exception {
        Set<String> wantedClasses = new TreeSet<>(before.keySet());
        req(wantedClasses.add(H), "Baseline already contains helper");
        req(wantedClasses.equals(after.keySet()), "Runtime added/removed unexpected classes");
        var beforeMethods = MergePayloads.methods(before.values());
        var afterMethods = MergePayloads.methods(after.values());
        Set<String> expectedMethods = new TreeSet<>(beforeMethods.keySet());
        expectedMethods.add(H + "-><init>()V");
        expectedMethods.add(REAR);
        req(expectedMethods.equals(afterMethods.keySet()), "Runtime added/removed unexpected methods");
        Map<String, Method> restored = new HashMap<>();
        for (String key : TARGETS) restored.put(key, checkGuard(beforeMethods.get(key), afterMethods.get(key)));
        int keptMethods = 0;
        for (var entry : beforeMethods.entrySet()) if (!TARGETS.contains(entry.getKey())) {
            req(MergePayloads.hash(entry.getValue()).equals(MergePayloads.hash(afterMethods.get(entry.getKey()))),
                    "Unrelated runtime method changed " + entry.getKey());
            keptMethods++;
        }
        int keptClasses = 0;
        for (var entry : before.entrySet()) {
            ClassDef actual = after.get(entry.getKey());
            var recovered = new ArrayList<Method>();
            for (Method m : actual.getMethods()) recovered.add(restored.getOrDefault(id(m), m));
            req(canonicalClassHash(entry.getValue()).equals(canonicalClassHash(methods(actual, recovered))),
                    "Runtime class metadata/fields/other content changed " + entry.getKey());
            keptClasses++;
        }
        System.out.println("RUNTIME_UI1930 changed_methods=3 retained_methods=" + keptMethods
                + " inverse_reconstructed_classes=" + keptClasses + " added_helper_classes=1");
        return keptMethods;
    }

    static int verifyLoader(Path beforeFile, Path afterFile, String label) throws Exception {
        var before = MergePayloads.classes(beforeFile.toString());
        var after = MergePayloads.classes(afterFile.toString());
        req(before.keySet().equals(after.keySet()), label + " loader class set changed");
        var previousMethods = MergePayloads.methods(before.values());
        var nextMethods = MergePayloads.methods(after.values());
        req(previousMethods.keySet().equals(nextMethods.keySet()), label + " loader method set changed");
        int replacements = 0;
        for (var entry : before.entrySet()) {
            ClassDef actualClass = after.get(entry.getKey());
            var recovered = new ArrayList<Method>();
            for (Method actual : actualClass.getMethods()) {
                Method previous = previousMethods.get(id(actual));
                if (actual.getImplementation() == null) { recovered.add(actual); continue; }
                List<Instruction> oldInstructions = ins(previous), newInstructions = ins(actual);
                req(oldInstructions.size() == newInstructions.size(), label + " loader instruction count changed " + id(actual));
                var inverse = new MutableMethodImplementation(actual.getImplementation());
                boolean changed = false;
                for (int k = 0; k < oldInstructions.size(); k++) {
                    Instruction oldInstruction = oldInstructions.get(k), newInstruction = newInstructions.get(k);
                    if (oldInstruction instanceof ReferenceInstruction r && r.getReference() instanceof StringReference text
                            && text.getString().startsWith(OLD_META)) {
                        req(entry.getKey().equals(PATCH), label + " metadata escaped ULike loader");
                        req(oldInstruction.getOpcode() == Opcode.CONST_STRING && newInstruction.getOpcode() == Opcode.CONST_STRING,
                                label + " metadata encoding changed");
                        req(newInstruction instanceof ReferenceInstruction nr && nr.getReference() instanceof StringReference ns
                                && ns.getString().startsWith(NEW_META), label + " metadata version missing");
                        int register = ((OneRegisterInstruction) oldInstruction).getRegisterA();
                        req(((OneRegisterInstruction) newInstruction).getRegisterA() == register, label + " metadata register changed");
                        inverse.replaceInstruction(k, new BuilderInstruction21c(Opcode.CONST_STRING, register,
                                new ImmutableStringReference(text.getString())));
                        replacements++;
                        changed = true;
                    }
                }
                Method restored = changed ? body(actual, inverse) : actual;
                req(MergePayloads.hash(previous).equals(MergePayloads.hash(restored)), label + " loader logic changed " + id(actual));
                recovered.add(restored);
            }
            req(canonicalClassHash(entry.getValue()).equals(canonicalClassHash(methods(actualClass, recovered))),
                    label + " loader class metadata/fields changed " + entry.getKey());
        }
        req(replacements == 1, label + " expected exactly one metadata replacement, got " + replacements);
        System.out.println("LOADER_UI1930 kind=" + label + " classes=" + before.size()
                + " methods=" + previousMethods.size() + " metadata_replacements=1 all_logic_retained=true");
        return before.size();
    }

    static void verifyHelper(Map<String, ClassDef> runtime, Path stockFile) throws Exception {
        ClassDef helper = runtime.get(H);
        req(helper != null && (helper.getAccessFlags() & 0x11) == 0x11, "Helper must be public final");
        req(!helper.getFields().iterator().hasNext(), "UI selection helper must not store/cache state");
        Set<String> seenFields = new TreeSet<>(), seenMethods = new TreeSet<>(), catches = new TreeSet<>();
        for (Method m : helper.getMethods()) {
            req((m.getAccessFlags() & 0x100) == 0, "Helper may not declare native methods");
            if (id(m).equals(REAR)) req((m.getAccessFlags() & 9) == 9, "Selection entry must be public static");
            for (var block : m.getImplementation().getTryBlocks()) for (var handler : block.getExceptionHandlers()) catches.add(handler.getExceptionType());
            for (Instruction i : ins(m)) {
                req(!i.getOpcode().name().startsWith("SPUT") && !i.getOpcode().name().startsWith("IPUT")
                                && !i.getOpcode().name().startsWith("APUT"), "Helper writes native/UI/cached state");
                if (!(i instanceof ReferenceInstruction ri)) continue;
                if (ri.getReference() instanceof FieldReference field) {
                    String key = DexFormatter.INSTANCE.getFieldDescriptor(field);
                    req(NATIVE_FIELDS.contains(key) || key.equals("Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;"),
                            "Unexpected helper field access " + key);
                    req(i.getOpcode() == Opcode.SGET_OBJECT, "Helper field access must be static read " + key);
                    if (NATIVE_FIELDS.contains(key)) seenFields.add(key);
                } else if (ri.getReference() instanceof MethodReference method) {
                    String key = DexFormatter.INSTANCE.getMethodDescriptor(method);
                    boolean nativeGetter = NATIVE_METHODS.contains(key);
                    req(nativeGetter || key.equals("Ljava/lang/Boolean;->equals(Ljava/lang/Object;)Z")
                                    || key.equals("Ljava/lang/Object;-><init>()V"),
                            "Unexpected helper call (camera/image/native mutation prohibited) " + key);
                    if (nativeGetter) {
                        req(i.getOpcode() == Opcode.INVOKE_VIRTUAL || i.getOpcode() == Opcode.INVOKE_VIRTUAL_RANGE,
                                "Native getter invoke kind mismatch " + key);
                        req(registers(i).length == method.getParameterTypes().size() + 1, "Native getter argument count " + key);
                        seenMethods.add(key);
                    }
                } else req(false, "Unexpected helper reference " + ri.getReference());
            }
        }
        req(seenFields.equals(NATIVE_FIELDS) && seenMethods.equals(NATIVE_METHODS), "Helper selection read ABI differs from approved path");
        req(catches.equals(Set.of("Ljava/lang/RuntimeException;", "Ljava/lang/LinkageError;")), "Fail-closed getter exception handlers changed");
        int resolved = 0;
        if (stockFile != null) {
            var stock = MergePayloads.classes(stockFile.toString());
            var nativeMethods = MergePayloads.methods(stock.values());
            var nativeFields = new TreeMap<String, Field>();
            for (ClassDef c : stock.values()) for (Field f : c.getFields()) nativeFields.put(DexFormatter.INSTANCE.getFieldDescriptor(f), f);
            for (String key : NATIVE_FIELDS) {
                Field f = nativeFields.get(key);
                req(f != null && (f.getAccessFlags() & 9) == 9, "Missing/nonpublic/nonstatic stock native field " + key);
                req((stock.get(f.getDefiningClass()).getAccessFlags() & 1) != 0, "Native owner class is inaccessible " + key);
                resolved++;
            }
            for (String key : NATIVE_METHODS) {
                Method m = nativeMethods.get(key);
                req(m != null && (m.getAccessFlags() & 9) == 1, "Missing/nonpublic/static stock native getter " + key);
                req((stock.get(m.getDefiningClass()).getAccessFlags() & 1) != 0, "Native getter owner is inaccessible " + key);
                resolved++;
            }
        }
        System.out.println("HELPER_UI1930 native_field_reads=" + seenFields.size() + " native_getters=" + seenMethods.size()
                + " stock_abi_verified=" + resolved + " field_writes=0 native_declarations=0 cached_fields=0"
                + " note=transitive_getter_semantics_audited_separately");
    }

    static final class ViewFixture { int visibility; }
    static final class BarFixture { final ViewFixture root = new ViewFixture(); long last; }
    static final class RouteFixture { boolean rear; }
    static final class VM {
        final Map<String, Method> methods;
        final BarFixture bar = new BarFixture();
        final RouteFixture route = new RouteFixture();
        boolean rearSelected, enabled, foreground, routePresent;
        long now = 1_000L;
        int selectionReads, stateReads, clockReads, hides, updates, viewWrites;
        final List<Integer> selections = new ArrayList<>();
        VM(Map<String, Method> methods) { this.methods = methods; }
        static long number(Object o) { return o instanceof Number n ? n.longValue() : o == null ? 0L : 1L; }
        static boolean zero(Object o) { return o == null || o instanceof Number n && n.longValue() == 0; }
        Object run(String key, Object... args) {
            Method method = methods.get(key);
            List<Instruction> code = ins(method);
            int[] at = addresses(code);
            Map<Integer, Integer> index = new HashMap<>();
            for (int k = 0; k < at.length; k++) index.put(at[k], k);
            Object[] r = new Object[method.getImplementation().getRegisterCount()];
            System.arraycopy(args, 0, r, r.length - args.length, args.length);
            Object result = null;
            int pc = 0;
            for (int budget = 0; budget < 1_000; budget++) {
                req(pc >= 0 && pc < code.size(), "DEX interpreter escaped instruction boundaries " + key);
                Instruction i = code.get(pc);
                int next = pc + 1;
                int a = i instanceof OneRegisterInstruction x ? x.getRegisterA() : -1;
                int b = i instanceof TwoRegisterInstruction x ? x.getRegisterB() : -1;
                int c = i instanceof ThreeRegisterInstruction x ? x.getRegisterC() : -1;
                boolean branch = false;
                switch (i.getOpcode()) {
                    case INVOKE_STATIC, INVOKE_STATIC_RANGE, INVOKE_VIRTUAL, INVOKE_VIRTUAL_RANGE -> {
                        int[] registers = registers(i);
                        Object[] params = new Object[registers.length];
                        for (int k = 0; k < params.length; k++) params[k] = r[registers[k]];
                        String target = ref(i);
                        switch (target) {
                            case REAR -> { selectionReads++; result = rearSelected ? 1 : 0; }
                            case SHOW -> result = run(SHOW);
                            case HIDE -> { hides++; result = run(HIDE, params); }
                            case CLOCK -> { clockReads++; result = now; }
                            case UPDATE -> { req(params[0] == bar, "Update receiver register corrupted"); updates++; result = null; }
                            case MANUAL_SELECT -> { selections.add(Math.toIntExact(number(params[0]))); result = null; }
                            case "Landroid/widget/LinearLayout;->getVisibility()I" -> {
                                req(params[0] == bar.root, "Hide queried wrong View receiver"); result = bar.root.visibility;
                            }
                            case "Landroid/widget/LinearLayout;->setVisibility(I)V" -> {
                                req(params[0] == bar.root, "Hide changed wrong View receiver");
                                bar.root.visibility = Math.toIntExact(number(params[1])); viewWrites++; result = null;
                            }
                            default -> throw new IllegalStateException("Unmodeled DEX call " + target);
                        }
                    }
                    case MOVE_RESULT, MOVE_RESULT_OBJECT, MOVE_RESULT_WIDE -> r[a] = result;
                    case SGET_BOOLEAN, SGET_OBJECT -> {
                        stateReads++;
                        r[a] = switch (ref(i)) {
                            case Z + "->enabled:Z" -> enabled ? 1 : 0;
                            case Z + "->foreground:Z" -> foreground ? 1 : 0;
                            case Z + "->active:Lcom/hiro/ulike/OpticalZoom$Route;" -> routePresent ? route : null;
                            default -> throw new IllegalStateException("Unmodeled static read " + ref(i));
                        };
                    }
                    case IGET_BOOLEAN, IGET_OBJECT, IGET_WIDE -> {
                        Object receiver = r[b];
                        r[a] = switch (ref(i)) {
                            case "Lcom/hiro/ulike/OpticalZoom$Route;->rear:Z" -> {
                                req(receiver == route, "Bad route register"); yield route.rear ? 1 : 0;
                            }
                            case B + "->last:J" -> { req(receiver == bar, "Bad timer receiver"); yield bar.last; }
                            case B + "->root:Landroid/widget/LinearLayout;" -> { req(receiver == bar, "Bad bar receiver"); yield bar.root; }
                            default -> throw new IllegalStateException("Unmodeled instance read " + ref(i));
                        };
                    }
                    case IPUT_WIDE -> {
                        req(ref(i).equals(B + "->last:J") && r[b] == bar, "Unexpected VM field write");
                        bar.last = number(r[a]);
                    }
                    case CONST_4, CONST_16, CONST_WIDE_16 -> r[a] = ((WideLiteralInstruction) i).getWideLiteral();
                    case SUB_LONG -> r[a] = number(r[b]) - number(r[c]);
                    case CMP_LONG -> r[a] = Long.compare(number(r[b]), number(r[c]));
                    case IF_EQZ -> branch = zero(r[a]);
                    case IF_NEZ -> branch = !zero(r[a]);
                    case IF_LEZ -> branch = number(r[a]) <= 0;
                    case IF_EQ -> branch = number(r[a]) == number(r[b]);
                    case GOTO, GOTO_16, GOTO_32 -> branch = true;
                    case RETURN -> { return r[a]; }
                    case RETURN_VOID -> { return null; }
                    default -> throw new IllegalStateException("Unmodeled DEX opcode " + i.getOpcode() + " in " + key);
                }
                if (branch) {
                    int target = at[pc] + ((OffsetInstruction) i).getCodeOffset();
                    Integer targetIndex = index.get(target);
                    req(targetIndex != null, "DEX branch does not land on an instruction");
                    next = targetIndex;
                }
                pc = next;
            }
            throw new IllegalStateException("DEX instruction budget exceeded " + key);
        }
        void allowRear() { rearSelected = enabled = foreground = routePresent = route.rear = true; }
    }

    static void executeRegressions(Map<String, Method> baseline, Map<String, Method> emitted) {
        int cases = 0;
        // Demonstrate the reported stale-route/throttled redraw failure against
        // actual baseline instructions, then use the same state on emitted DEX.
        VM old = new VM(baseline); old.allowRear(); old.rearSelected = false; old.bar.last = old.now;
        req(VM.number(old.run(SHOW)) == 1, "Pinned baseline no longer reproduces stale route visibility");
        req(VM.number(old.run(DRAW, old.bar)) == 1 && old.bar.root.visibility == 0 && old.updates == 0,
                "Pinned baseline no longer reproduces stale visible bar within throttle");
        VM repaired = new VM(emitted); repaired.allowRear(); repaired.rearSelected = false; repaired.bar.last = repaired.now;
        req(VM.number(repaired.run(SHOW)) == 0, "Front selection ignored with stale rear route");
        req(VM.number(repaired.run(DRAW, repaired.bar)) == 1 && repaired.bar.root.visibility == 8
                        && repaired.clockReads == 0 && repaired.updates == 0 && repaired.bar.last == repaired.now,
                "Emitted guard does not hide before throttle");
        cases += 2;
        // Exhaust the original four visibility gates and the injected fresh
        // native-selection result; front/null/error behavior maps to false and
        // is separately exercised in the shipped Java helper's host fixtures.
        for (int mask = 0; mask < 32; mask++) {
            VM vm = new VM(emitted);
            vm.rearSelected = (mask & 1) != 0; vm.enabled = (mask & 2) != 0;
            vm.foreground = (mask & 4) != 0; vm.routePresent = (mask & 8) != 0;
            vm.route.rear = (mask & 16) != 0;
            req(VM.number(vm.run(SHOW)) == (mask == 31 ? 1 : 0), "Visibility matrix mask=" + mask);
            req(vm.selectionReads == 1, "show must read fresh selection once");
            if (!vm.rearSelected) req(vm.stateReads == 0, "Denied show consulted stale route/configuration");
            cases++;
        }
        int[] indices = {-1, 0, 1, 2, 3, 4, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (boolean rear : new boolean[] {false, true}) for (int selected : indices) {
            VM vm = new VM(emitted); vm.allowRear(); vm.rearSelected = rear;
            vm.run(SELECT, selected);
            req(vm.selectionReads == 1, "Click must read current selection");
            req(vm.selections.equals(rear ? List.of(selected) : List.of()), "Stale click allowed or rear selection register corrupted");
            cases++;
        }
        // Hiding must precede the clock even when a previously visible control
        // was updated less than 100 ms ago; hide() itself is interpreted here.
        for (int mask = 0; mask < 31; mask++) for (int initialVisibility : new int[] {0, 8}) {
            VM vm = new VM(emitted);
            vm.rearSelected = (mask & 1) != 0; vm.enabled = (mask & 2) != 0;
            vm.foreground = (mask & 4) != 0; vm.routePresent = (mask & 8) != 0;
            vm.route.rear = (mask & 16) != 0;
            vm.bar.last = vm.now; vm.bar.root.visibility = initialVisibility;
            req(VM.number(vm.run(DRAW, vm.bar)) == 1, "Visibility rejection must permit Android draw");
            req(vm.bar.root.visibility == 8 && vm.hides == 1 && vm.updates == 0 && vm.clockReads == 0,
                    "Denied draw failed immediate whole-bar hide mask=" + mask);
            req(vm.bar.last == vm.now && vm.viewWrites == (initialVisibility == 0 ? 1 : 0), "Hide timer/idempotence changed");
            cases++;
        }
        for (int delay : new int[] {0, 99, 100, 101, 1_000}) {
            VM vm = new VM(emitted); vm.allowRear(); vm.bar.last = vm.now - delay;
            req(VM.number(vm.run(DRAW, vm.bar)) == 1, "Rear redraw result changed");
            req(vm.clockReads == 1 && vm.hides == 0 && vm.updates == (delay > 100 ? 1 : 0), "Rear throttle changed delay=" + delay);
            req(vm.bar.last == (delay > 100 ? vm.now : vm.now - delay), "Rear throttle timestamp changed");
            cases++;
        }
        VM switches = new VM(emitted); switches.allowRear();
        for (int k = 0; k < 128; k++) {
            switches.now += 16; switches.rearSelected = (k & 1) == 0;
            switches.bar.root.visibility = 0; // A stale attached rear-control tree.
            int hidesBefore = switches.hides, readsBefore = switches.selectionReads;
            int selectedBefore = switches.selections.size(), clockBefore = switches.clockReads;
            switches.run(DRAW, switches.bar); switches.run(SELECT, k % 5);
            req(switches.selectionReads - readsBefore >= 2, "Repeated switches reused cached selection");
            if (switches.rearSelected) {
                req(switches.hides == hidesBefore && switches.selections.size() == selectedBefore + 1,
                        "Rear controls denied during repeated switching");
            } else {
                req(switches.bar.root.visibility == 8 && switches.hides == hidesBefore + 1
                                && switches.selections.size() == selectedBefore && switches.clockReads == clockBefore,
                        "Repeated front switch left visible/actionable controls");
            }
            cases++;
        }
        System.out.println("DEX_HOST_UI1930_CASES=" + cases);
        System.out.println("DEX_HOST_UI1930 actual_Dex_instructions=true baseline_failure_reproduced=true"
                + " immediate_hide_before_clock=true rear_throttle_retained=true stale_clicks_blocked=true"
                + " environment=focused_host_interpreter native_selection_result=injected"
                + " update_body=boundary_stub Android_runtime=false device_camera=false");
    }

    static Path required(Path path) { req(Files.isRegularFile(path), "Required audit input absent: " + path); return path; }
    public static void main(String[] args) throws Exception {
        req(args.length == 2 || args.length == 3, "Usage: VerifyUi1930 BASE_DIR EMITTED_DIR [STOCK_APK]");
        Path base = Path.of(args[0]), emitted = Path.of(args[1]);
        Path oldRuntime = required(base.resolve("ulike/runtime.dex")), newRuntime = required(emitted.resolve("runtime.dex"));
        for (String name : List.of("methods.dex", "methods.tsv")) {
            req(Files.mismatch(required(base.resolve("ulike").resolve(name)), required(emitted.resolve(name))) == -1L,
                    "Native method payload/contracts changed: " + name);
        }
        var before = MergePayloads.classes(oldRuntime.toString());
        var after = MergePayloads.classes(newRuntime.toString());
        verifyRuntime(before, after);
        verifyLoader(required(base.resolve("classes.dex")), required(emitted.resolve("loader.dex")), "standalone");
        verifyLoader(required(base.resolve("bundle.dex")), required(emitted.resolve("bundle-loader.dex")), "integrated");
        verifyHelper(after, args.length == 3 ? required(Path.of(args[2])) : null);
        executeRegressions(MergePayloads.methods(before.values()), MergePayloads.methods(after.values()));
        System.out.println("VERIFY_UI1930_ASSERTIONS=" + assertions);
        System.out.println("PASS1930 emitted_guards=3 original_conditions_restored_exactly=true"
                + " unrelated_runtime_and_loaders_preserved=true native_methods_and_contracts_byte_identical=true"
                + " added_camera_or_image_mutations=false Android_device_verification=not_performed");
    }
}
