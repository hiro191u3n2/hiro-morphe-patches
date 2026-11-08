package app.hiro.mapsbikes.patches;

import java.security.MessageDigest;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/**
 * Frozen placed-label contract for Maps 26.36.04.973607363, verified in:
 * v2.8.5 APK 7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063
 * v2.8.7 APK 238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71.
 *
 * Both inputs have identical bodies and schemas below. The two hashes per
 * method represent the pinned morphe.jar and morphe-2.2.jar DexPool encodings,
 * with debug items omitted. Never refresh them from an unverified target APK.
 *
 * URL contract: cift.f -> cifq(c==4).d -> cifs.b -> cifr.c -> cier(b==1).c.
 * Lbkje.a decodes that exact sequence into composed icon URLs. The three known
 * factory call sites explicitly handle null; SharedLabelerJni also removes
 * previous draw, collision and hit-test state for the suppressed label.
 * This proves the Java/shared-labeler path, not every full-native geoxp path.
 */
public final class NativeContracts {
    private NativeContracts() {}
    public static final String SHARED_CALLER =
        "Lcom/google/android/libraries/geo/mapcore/internal/label/jni/SharedLabelerJni;->nativeUpdatePlacedLabels([B)V";
    public static final String CLIENT_CALLER = "Lbjwc;->run()V";
    public static final String ICON_DECODER = "Lbkje;->a(Lcifs;)Lbkje;";

    private static final Set<String> TARGET_HASHES = hashes(
        "623133af6e89fd29152b9cda17a3bd707d036f55e90e35e37ed535ffeca7b4d4",
        "d102e011d3e6665064be2b0b766481b42f3d8385092db4a17fdcfd9f9c2d3ee8");
    private static final Set<String> SHARED_HASHES = hashes(
        "199777d7463428c4f83f396df48f88dee0c1f21889bcdbc218635dde07565693",
        "ae98a2d28310230a55f7015e7ddf2f3fc4726ed36d84fe0008b83f0d3ceab9c2");
    private static final Set<String> CLIENT_HASHES = hashes(
        "79627d0bb34bdb71238fcb9c889f39d1d35493d7516716f1afd80d9d1cc86a9e",
        "5b3e12a4d2c5ab0a8e0be86194868f604c5b2bd1b51b42a9307689dc4f23fda8");
    private static final Set<String> DECODER_HASHES = hashes(
        "82fa2a20e9befb1ef2f2dbbc606bacb481267543202be42370dc4310e2fe7a4a",
        "eaf674fd9f57dc972f2dc396fd60d60467a5d2f5410be732a1843ebd2853656c");

    static Set<String> hashes(String... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(values)));
    }
    private static void require(boolean ok, String why) { MapsHideBicycleMarksPatch.require(ok, why); }

    /** Includes every field's name, descriptor and flags plus the class hierarchy. */
    public static String schemaFingerprint(ClassDef type) {
        try {
            require(type != null, "Missing native schema");
            List<String> interfaces = new ArrayList<String>(type.getInterfaces());
            Collections.sort(interfaces);
            List<String> fields = new ArrayList<String>();
            for (Field field : type.getFields())
                fields.add(field.getName() + ":" + field.getType() + ":" + field.getAccessFlags());
            Collections.sort(fields);
            StringBuilder value = new StringBuilder(type.getType()).append('|').append(type.getAccessFlags())
                .append('|').append(type.getSuperclass()).append('|').append(interfaces).append('\n');
            for (String field : fields) value.append(field).append('\n');
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes("UTF-8"));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format(Locale.ROOT, "%02x", item & 255));
            return result.toString();
        } catch (Exception e) { throw new IllegalStateException("Cannot fingerprint placed-label schema", e); }
    }
    static void schema(MapsHideBicycleMarksPatch.Lookup lookup, String type, String expected) {
        ClassDef found = lookup.get(type);
        require(found != null && expected.equals(schemaFingerprint(found)), "Placed-label schema changed " + type);
    }
    static Method method(MapsHideBicycleMarksPatch.Lookup lookup, String descriptor) {
        return MapsHideBicycleMarksPatch.find(lookup.get(MapsHideBicycleMarksPatch.owner(descriptor)), descriptor);
    }
    static List<Instruction> instructions(Method method) {
        require(method.getImplementation() != null, "Missing native body " + MapsHideBicycleMarksPatch.descriptor(method));
        List<Instruction> result = new ArrayList<Instruction>();
        for (Instruction instruction : method.getImplementation().getInstructions()) result.add(instruction);
        return result;
    }
    static void shape(Method method, int flags, int registers, int count, int units, int tries,
                              Set<String> fingerprints) {
        List<Instruction> body = instructions(method);
        int actualUnits = 0;
        for (Instruction instruction : body) actualUnits += instruction.getCodeUnits();
        require(method.getAccessFlags() == flags && method.getImplementation().getRegisterCount() == registers
            && body.size() == count && actualUnits == units && method.getImplementation().getTryBlocks().size() == tries,
            "Native method layout changed " + MapsHideBicycleMarksPatch.descriptor(method));
        require(fingerprints.contains(MapsHideBicycleMarksPatch.fingerprint(method)),
            "Native method fingerprint changed " + MapsHideBicycleMarksPatch.descriptor(method));
    }
    private static void safeNullCalls(Method caller, int expectedCalls) {
        List<Instruction> body = instructions(caller);
        int calls = 0;
        for (int at = 0; at < body.size(); at++) {
            Instruction instruction = body.get(at);
            if (!(instruction instanceof ReferenceInstruction)) continue;
            Reference reference = ((ReferenceInstruction)instruction).getReference();
            if (!(reference instanceof MethodReference)
                || !MapsHideBicycleMarksPatch.TARGET.equals(MapsHideBicycleMarksPatch.descriptor((MethodReference)reference))) continue;
            calls++;
            require(instruction.getOpcode() == Opcode.INVOKE_VIRTUAL_RANGE, "Factory caller invocation changed");
            require(at + 2 < body.size() && body.get(at + 1).getOpcode() == Opcode.MOVE_RESULT_OBJECT,
                "Factory caller result changed");
            int register = ((OneRegisterInstruction)body.get(at + 1)).getRegisterA();
            int guard = at + 2;
            if (body.get(guard).getOpcode() == Opcode.CHECK_CAST) {
                require(((OneRegisterInstruction)body.get(guard)).getRegisterA() == register,
                    "Factory caller result cast changed");
                guard++;
            }
            require(guard < body.size() && (body.get(guard).getOpcode() == Opcode.IF_EQZ
                || body.get(guard).getOpcode() == Opcode.IF_NEZ)
                && ((OneRegisterInstruction)body.get(guard)).getRegisterA() == register,
                "Factory caller no longer handles null");
        }
        require(calls == expectedCalls, "Factory call count changed " + MapsHideBicycleMarksPatch.descriptor(caller));
    }

    public static void validate(MapsHideBicycleMarksPatch.Lookup lookup) {
        require(lookup != null, "Missing native lookup");
        ClassDef owner = lookup.get(MapsHideBicycleMarksPatch.OWNER);
        require(owner != null && owner.getAccessFlags() == 17 && "Ljava/lang/Object;".equals(owner.getSuperclass())
            && owner.getInterfaces().isEmpty(), "Placed-label factory class changed");
        validateTarget(MapsHideBicycleMarksPatch.find(owner, MapsHideBicycleMarksPatch.TARGET));
        schema(lookup, "Lcift;", "325797f9326a89d73b0c8eb7d32e4d56b55226faab4ec4a5dfe49015df16b2e7");
        schema(lookup, "Lcifq;", "fa01a7a9ac0fb48105211b2712ea0f654205f2e9dfefe526a2eca288a3352b46");
        schema(lookup, "Lcifs;", "70bf56a8a166482ffe1d07bcf1fb9d66b8d5bc64ea634844e53639c0dba92c1a");
        schema(lookup, "Lcifr;", "7fe363b71e4b69182cea43f4eb4e4db88ee28f69385b664879e4d5b4da4754ea");
        schema(lookup, "Lcier;", "5300ec380cfd399e41e7fe841a2d94c98ef546b67dfd4190306cdd80427a89e2");
        Method shared = method(lookup, SHARED_CALLER);
        shape(shared, 2, 28, 587, 1140, 20, SHARED_HASHES);
        safeNullCalls(shared, 2);
        Method client = method(lookup, CLIENT_CALLER);
        shape(client, 17, 18, 779, 1594, 14, CLIENT_HASHES);
        safeNullCalls(client, 1);
        shape(method(lookup, ICON_DECODER), 9, 15, 151, 290, 0, DECODER_HASHES);
    }
    public static void validateTarget(Method method) {
        require(method != null && MapsHideBicycleMarksPatch.TARGET.equals(MapsHideBicycleMarksPatch.descriptor(method)),
            "Unexpected placed-label factory method");
        shape(method, 17, 35, 435, 817, 0, TARGET_HASHES);
        List<Instruction> body = instructions(method);
        for (int index = 0; index < 3; index++) {
            Instruction instruction = body.get(index);
            require(instruction.getOpcode() == Opcode.MOVE_OBJECT_FROM16
                && ((TwoRegisterInstruction)instruction).getRegisterA() == index
                && ((TwoRegisterInstruction)instruction).getRegisterB() == 27 + index,
                "Placed-label entry parameter registers changed");
        }
    }
}
