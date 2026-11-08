package app.hiro.mapsstrict.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;

/**
 * Frozen request/query boundary for Maps 26.36.04.973607363.
 *
 * Captured ONLY from the previously inspected APKs:
 *   v2.8.5 7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063
 *   v2.8.7 238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71
 *
 * Both APKs have the same native method bodies. The two hashes for each method
 * are the two known DexPool serializations from the pinned morphe.jar and
 * morphe-2.2.jar. Original and MutableMethod forms were checked with both SDKs.
 * Never refresh these allowlists automatically from an arbitrary target APK.
 *
 * This is the request/query contract. The root build must add frozen response
 * schema contracts for every proto field used by the production runtime.
 */
public final class NativeContracts {
    private NativeContracts() {}
    public static final String MERGE = "Lavdv;->O(Lavdv;Landroid/app/Application;ZZ)V";
    private static final String PARSE = "Lavdv;->aj(Lcqgm;Landroid/app/Application;ILawcl;Ljava/lang/String;I)V";
    private static final Set<String> PARSE_HASHES = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "61357b3d3a3feb6e0fff81da3a11156161b8d8a7f13e4a3c16464dd8c4429d5f",
            "7d145d10f4c8aec0a7b7164ddfaa57b302e684ceeed8e518afcbf5be9f8e2d79")));
    private static final Set<String> MERGE_HASHES = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "06098f15efc934e2863aba7b3c8a24ec2acc20ba1ccc289ffe98703a96c9b634",
            "9f148b322cacee946f81aaf1fb91121c482b1e09aac1fabfe2c1db947153d868")));

    private static final Set<String> TARGET_HASHES = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "83fa0214fb0bd0ecfb88c625171083568f69328c4b66985974c26b25cff316a3",
            "fee07e85b6baf8b250cd67a24a13e3337416226820972ae20ca5d78132f8ab42")));
    private static final Set<String> QUERY_GETTER_HASHES = Collections.unmodifiableSet(
        new LinkedHashSet<String>(Arrays.asList(
            "aaa4ec60e19ee506aa22f5e235ad521b77c40f4d7c92429d455acbf1eac23c33",
            "e568b0644664b75c893066607091c61bdfcd905a10b55eca8ba44a5e18bace9d")));

    private static void require(boolean ok, String why) {
        MapsStrictSearchPatch.require(ok, why);
    }
    private static ClassDef type(MapsStrictSearchPatch.Lookup lookup, String descriptor,
                                 String superclass, int flags) {
        ClassDef type = lookup.get(descriptor);
        require(type != null && descriptor.equals(type.getType()), "Missing native class " + descriptor);
        require(superclass.equals(type.getSuperclass()) && type.getAccessFlags() == flags,
            "Native class shape changed " + descriptor);
        return type;
    }
    private static void field(ClassDef owner, String name, String descriptor, int flags) {
        Field found = null;
        for (Field candidate : owner.getFields()) if (name.equals(candidate.getName())) {
            require(found == null, "Ambiguous native field " + owner.getType() + "->" + name);
            found = candidate;
        }
        require(found != null && owner.getType().equals(found.getDefiningClass())
            && descriptor.equals(found.getType()) && found.getAccessFlags() == flags,
            "Native field shape changed " + owner.getType() + "->" + name + ":" + descriptor);
    }
    private static int instructionCount(Method method) {
        require(method.getImplementation() != null, "Missing native method body");
        int count = 0;
        for (Instruction ignored : method.getImplementation().getInstructions()) count++;
        return count;
    }

    public static void validate(MapsStrictSearchPatch.Lookup lookup) {
        require(lookup != null, "Missing native class lookup");
        ClassDef request = type(lookup, "Lavds;", "Lawcc;", 1);
        require(request.getInterfaces().contains("Ljava/io/Serializable;"),
            "Search request serialization contract changed");
        field(request, "l", "Lawfr;", 2);
        field(request, "b", "Lonh;", 1);
        field(request, "g", "Lavdv;", 1);

        ClassDef parameters = type(lookup, "Lcqgi;", "Lcmzs;", 17);
        require(parameters.getInterfaces().contains("Lcnaz;"),
            "Search parameter protobuf contract changed");
        field(parameters, "a", "Lcqgi;", 25);
        field(parameters, "e", "Ljava/lang/String;", 1);

        Method getter = MapsStrictSearchPatch.find(request, "Lavds;->c()Lcqgi;");
        require(getter.getAccessFlags() == 17 && getter.getImplementation() != null
            && getter.getImplementation().getRegisterCount() == 3 && instructionCount(getter) == 10,
            "Request query decoder shape changed");
        require(QUERY_GETTER_HASHES.contains(MapsStrictSearchPatch.fingerprint(getter)),
            "Request query decoder fingerprint changed");
        validateTarget(MapsStrictSearchPatch.find(request, MapsStrictSearchPatch.TARGET));
        validateModel(type(lookup, "Lavdv;", "Lonf;", 1));
        ClassDef scopeOwner = type(lookup, "Lonf;", "Ljava/lang/Object;", 1025);
        require(scopeOwner.getInterfaces().contains("Ljava/io/Serializable;"), "Search model base contract changed");
        Method writeScope = MapsStrictSearchPatch.find(scopeOwner, "Lonf;->e()Lone;");
        require(writeScope.getAccessFlags() == 17 && writeScope.getImplementation() != null
            && writeScope.getImplementation().getRegisterCount() == 2 && instructionCount(writeScope) == 9
            && MapsStrictSearchPatch.fingerprint(writeScope).equals("034986e3efa2649553c29fb62545d5080a72543a668c7c3e2f91f26c281fffa2"),
            "Search model write-lock contract changed");
        ClassDef scope = type(lookup, "Lone;", "Ljava/lang/Object;", 1537);
        require(scope.getInterfaces().contains("Ljava/lang/AutoCloseable;"), "Search model scope is not closeable");
        Method close = MapsStrictSearchPatch.find(scope, "Lone;->close()V");
        require(close.getAccessFlags() == 1025 && close.getImplementation() == null, "Search model scope close contract changed");
        RuntimeContracts.validate(lookup);
    }

    public static void validateModel(ClassDef model) {
        require(model != null && "Lavdv;".equals(model.getType()) && "Lonf;".equals(model.getSuperclass())
            && model.getAccessFlags() == 1, "Search model shape changed");
        field(model, "S", "Lcqgm;", 130);
        field(model, "ag", "Lawfr;", 2);
        field(model, "g", "Ljava/lang/String;", 1);
        for (Field candidate : model.getFields())
            require(!candidate.getName().equals(MapsStrictSearchPatch.MARKER), "Search success marker already installed");
        Method merge = MapsStrictSearchPatch.find(model, MERGE);
        require(merge.getAccessFlags() == 17 && merge.getImplementation() != null
            && merge.getImplementation().getRegisterCount() == 14 && instructionCount(merge) == 324,
            "Search merge method shape changed");
        require(MERGE_HASHES.contains(MapsStrictSearchPatch.fingerprint(merge)), "Search merge method fingerprint changed");
        require(MapsStrictSearchPatch.mergeInsertionIndex(merge) == 296, "Accepted merge boundary changed");
        Method parse = MapsStrictSearchPatch.find(model, PARSE);
        require(parse.getAccessFlags() == 17 && parse.getImplementation() != null
            && parse.getImplementation().getRegisterCount() == 25 && instructionCount(parse) == 707
            && PARSE_HASHES.contains(MapsStrictSearchPatch.fingerprint(parse)), "Search model response ownership changed");
    }

    public static void validateTarget(Method method) {
        require(method != null && MapsStrictSearchPatch.TARGET.equals(MapsStrictSearchPatch.descriptor(method)),
            "Unexpected search response method");
        require(method.getAccessFlags() == 17 && method.getImplementation() != null
            && method.getImplementation().getRegisterCount() == 13 && instructionCount(method) == 88,
            "Search response method shape changed");
        require(TARGET_HASHES.contains(MapsStrictSearchPatch.fingerprint(method)),
            "Search response method fingerprint changed");
        require(MapsStrictSearchPatch.insertionIndex(method) == 10,
            "Search request/response hook position changed");
    }
}
