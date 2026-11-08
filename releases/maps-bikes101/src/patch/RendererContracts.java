package app.hiro.mapsbikes.patches;

import java.util.List;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/**
 * Additional renderer contracts, frozen from both pinned Maps inputs.
 * Each pair is the verified current / 2.2 DexPool encoding, not a newly
 * accepted target signature. Native callbacks retain their original request
 * identifiers and completion semantics. No native library or renderer switch
 * is modified. The online cache directory is versioned once because an old
 * native SQLite resource can bypass a newly added download callback.
 */
public final class RendererContracts {
    private RendererContracts() {}
    public static final String LEGACY =
        "Lbkex;->b(Lbken;Lcom/google/common/collect/ImmutableList;Lchvn;Lbkjj;Lbkeo;Lbkrf;Z)Lbkex;";
    public static final String NETWORK = "Lbkup;->b(Ljava/lang/Object;)V";
    public static final String FACTORY = "Lcom/google/android/libraries/geo/mapcore/geoxp/jni/NativeMapFactory;";
    public static final String OFFLINE = FACTORY + "->r(J[B)V";
    public static final String NETWORK_CALLBACK = FACTORY + "->nativeLoadUrlSuccess(JJ[B)V";
    public static final String OFFLINE_CALLBACK = FACTORY + "->nativeOnOfflineCacheFetchSuccess(JJ[B)V";
    public static final String CACHE_CONFIG =
        "Lcom/google/android/libraries/geo/mapcore/geoxp/jni/providers/MapCoreConfigProvider;->c(Lcmzk;)Lcdqi;";
    public static final int CACHE_INDEX = 132;
    // Keep this namespace stable in future fixes unless a new cache migration
    // is required. The old geoxp_cache is retained; geoxp_secure is untouched.
    public static final String CACHE_DIRECTORY = "geoxp_cache_hiro_bikes101";
    public static final String LEGACY_POINT_CALLER =
        "Lbkaw;->p(Lchvo;Lbkdh;ILcom/google/common/collect/ImmutableList;Landroid/content/res/Resources;Lbken;Lbkfx;Lbkez;Lbkrf;Lbkeo;ZZ)Lbkaw;";
    public static final String LEGACY_FEATURE_CALLER = "Lbkcz;->k(Lbkcx;Lbkdh;)Lbkcy;";

    private static void require(boolean value, String why) { MapsHideBicycleMarksPatch.require(value, why); }
    private static Method method(MapsHideBicycleMarksPatch.Lookup lookup, String descriptor) {
        return NativeContracts.method(lookup, descriptor);
    }
    private static void shape(MapsHideBicycleMarksPatch.Lookup lookup, String descriptor,
            int flags, int registers, int count, int units, int tries, String current, String sdk22) {
        NativeContracts.shape(method(lookup, descriptor), flags, registers, count, units, tries,
            NativeContracts.hashes(current, sdk22));
    }

    public static void validate(MapsHideBicycleMarksPatch.Lookup lookup) {
        shape(lookup, LEGACY, 9, 18, 65, 100, 0,
            "28a4ba99526c92cf4af97dda61930b1b7b09bcf482dd4bb096b545e03c1bfbc4",
            "3b65b0b811ca8e7f370773333776f2141e9de4d087c1f891d262790996066a04");
        shape(lookup, NETWORK, 4177, 6, 17, 34, 0,
            "de02526eecc0ca6da4b308030c9d5d62e437f2c541989683d3c3f785b48fcf2c",
            "59f6a80cfcb5e64551f73d676f491da0f3e4f385d9e0fa8183791d55e35c3c70");
        shape(lookup, OFFLINE, 17, 6, 8, 16, 0,
            "bbc17a088e52c0f8bd4cc54d2beced1c4bd52bbe343d6f7e48a0b25524730146",
            "e862bf9d56deca73067e9c9057d27424da6f3412d25263df352b00559352ef17");
        shape(lookup, CACHE_CONFIG, 18, 15, 276, 552, 2,
            "734b78143a04ea3459df692d1fafc3ab08285b57a57855f994daf8c52feb1c9d",
            "09cbe3f174cc21f7c9c6857942c290d62475cdc68831a3d4b8aa0f88efe183bb");
        shape(lookup, LEGACY_POINT_CALLER, 9, 40, 280, 519, 0,
            "22e3a3f62f463d8d2ca8f2293da3276f2c977a980e2a3e5461aa387068b002b4",
            "23b04410ba3be5a14c7da7d28e39b82fb3b5506bd55b5fae0c3e68fda2acfae6");
        shape(lookup, LEGACY_FEATURE_CALLER, 18, 35, 1102, 2122, 4,
            "952e2303ba84ed7410dbdd91c90a6d44d584d2383b578dca9ed890126c591c6a",
            "40970b37ce2e6a3c2777695db2fcf2bc1931f0862851afe08fb2244db459121a");
        shape(lookup, "Lbken;->j(Lcom/google/common/collect/ImmutableList;Lbkje;)Lbkje;", 25, 13, 132, 258, 0,
            "502ee3ce366397015620d118ab96807263a518f5c51c5dee8ceb78cdd4891dc7",
            "978e1b33934651281e63ae36e180d4cd8bd958c16fb2af188fd8536aae2feddb");
        shape(lookup, "Lchyy;->c(III)V", 20, 4, 91, 242, 0,
            "81ab1005aa5b22cac3e905b68f1386a677a530810c22eb8d83cc2f7e296a87f1",
            "63fbade1503dd621d0b9a7f8a2d8d9bdd31ed9c6b1d26693bcafaecf4b2637f0");
        shape(lookup, "Lchvx;->dynamicMethod(Lcmzr;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            20, 6, 121, 242, 0,
            "dbbc4485d19a11a4a0f608c3f5a519203987ff47dfbc6eb708f6f3ad7483efcc",
            "247eb7cf53a26c7470561c5f3d1fa8cf01954c81ddd67fb8b4d1472629f041e0");
        shape(lookup, "Lchuz;->dynamicMethod(Lcmzr;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            20, 6, 80, 160, 0,
            "870d59a85e855fc2b8b79a577cc6a0e53a38e698429b2617cd567920ef0dc986",
            "1b9b92983c5a2bc692daf8a41ea9749468c61ea992a776044e48b72a8e399777");
        shape(lookup, "Lchyj;->dynamicMethod(Lcmzr;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            20, 6, 106, 212, 0,
            "a1f1e1814aa5131f156790b5f65bf50f41427ab4ad34142b9f019e6328ad25ec",
            "9588bf9813164cd6d37ee007f4782a1236d64c367f0eec347bbf7c556efb0847");
        shape(lookup, "Lbkjn;->p(J)Lbkkn;", 131090, 29, 852, 1614, 13,
            "3762c7600c1f2a5053928fbcee95cf7f447ed769e974bc6ce5edc382e5064aed",
            "ce676fd7a5d3ad2650959922075458730e1ce4225104e70ef9db23e8b314a761");
        shape(lookup, "Lbkkp;->i(Lchve;Lbkko;)V", 10, 14, 415, 799, 0,
            "822857f9df4810eb870e166cf6240ec75c292aed7523571a697697237d9b2e6e",
            "c660abcd9cf79dd656d8fd3d082057e5527bea28fe0b5263774b57bcbcecb822");

        NativeContracts.schema(lookup, "Lbkjj;", "18ae14976459201a0910989c78af0c493a07460a8012aa25b661f732870bc4c6");
        NativeContracts.schema(lookup, "Lbkje;", "9e66dd5300ecd90fb41778b63b6116e382d6b980ecd87f2c5d0f75f2ba3f0970");
        NativeContracts.schema(lookup, "Lbkjc;", "92109705f5c5e81817f95e4ee5eaf9dfec7403ef9728d1c9921bd7bd5d0d172f");
        NativeContracts.schema(lookup, "Lbken;", "4d8450b62512fc76da4a6c240433b7f5e534058871409c6a48b0eb1dfd52fe6a");
        NativeContracts.schema(lookup, "Lchue;", "a47b0a008c20a5e8928d9059b5b78b3b0f1c3a27f3d39d3e39b17d3e651c30ce");
        NativeContracts.schema(lookup, "Lchyy;", "fda1d80976717a47887d2856205891ec87647015628e379935c8da69fed4258a");
        NativeContracts.schema(lookup, "Lchvx;", "f01d6333e161cbfa9bbb4088f5b2e76295ad5cda5238d3e0ebb745b2c2128f0c");
        NativeContracts.schema(lookup, "Lchuz;", "b6251c586c44a9ea25a5bc2e5663b83b00c0413faa4dd8e6baa67003ed2d4b1e");
        NativeContracts.schema(lookup, "Lchyj;", "b334702444e0196623065257f226f4fec2a9e270989b94d5ccc3104b4315a2c3");

        nullSafeLegacyCaller(method(lookup, LEGACY_POINT_CALLER));
        nullSafeLegacyCaller(method(lookup, LEGACY_FEATURE_CALLER));
        callback(method(lookup, NETWORK), NETWORK_CALLBACK, 15, new int[] {0, 1, 2, 3, 5});
        callback(method(lookup, OFFLINE), OFFLINE_CALLBACK, 6, new int[] {0, 1, 3, 4, 5});
        List<Instruction> config = NativeContracts.instructions(method(lookup, CACHE_CONFIG));
        int units = 0, cacheNames = 0, secureNames = 0;
        for (int i = 0; i < config.size(); i++) {
            Instruction code = config.get(i);
            if (code instanceof ReferenceInstruction
                    && ((ReferenceInstruction)code).getReference() instanceof StringReference) {
                String text = ((StringReference)((ReferenceInstruction)code).getReference()).getString();
                if (text.equals("geoxp_cache")) {
                    cacheNames++;
                    require(i == CACHE_INDEX && units == 0x10b && code.getOpcode() == Opcode.CONST_STRING
                        && ((OneRegisterInstruction)code).getRegisterA() == 8, "Online cache directory load changed");
                }
                if (text.equals("geoxp_secure")) secureNames++;
            }
            units += code.getCodeUnits();
        }
        require(cacheNames == 1 && secureNames == 1, "Online/secure cache separation changed");
    }

    private static void nullSafeLegacyCaller(Method method) {
        List<Instruction> code = NativeContracts.instructions(method);
        int calls = 0;
        for (int i = 0; i < code.size(); i++) {
            Instruction value = code.get(i);
            if (!(value instanceof ReferenceInstruction)) continue;
            Reference reference = ((ReferenceInstruction)value).getReference();
            if (!(reference instanceof MethodReference) || !LEGACY.equals(MapsHideBicycleMarksPatch.descriptor((MethodReference)reference))) continue;
            calls++;
            require(value.getOpcode() == Opcode.INVOKE_STATIC_RANGE && i + 2 < code.size(), "Legacy caller ABI changed");
            Instruction result = code.get(i + 1), branch = code.get(i + 2);
            require(result.getOpcode() == Opcode.MOVE_RESULT_OBJECT
                    && (branch.getOpcode() == Opcode.IF_EQZ || branch.getOpcode() == Opcode.IF_NEZ)
                    && ((OneRegisterInstruction)result).getRegisterA() == ((OneRegisterInstruction)branch).getRegisterA(),
                "Legacy icon caller no longer handles null");
        }
        require(calls == 1, "Legacy icon call count changed");
    }

    private static void callback(Method method, String descriptor, int at, int[] registers) {
        List<Instruction> body = NativeContracts.instructions(method);
        Instruction call = body.get(at);
        require(call.getOpcode() == Opcode.INVOKE_STATIC && call instanceof FiveRegisterInstruction
                && call instanceof ReferenceInstruction
                && ((ReferenceInstruction)call).getReference() instanceof MethodReference
                && descriptor.equals(MapsHideBicycleMarksPatch.descriptor((MethodReference)((ReferenceInstruction)call).getReference())),
            "Native resource completion changed: " + descriptor);
        FiveRegisterInstruction args = (FiveRegisterInstruction)call;
        require(args.getRegisterCount() == 5 && args.getRegisterC() == registers[0]
                && args.getRegisterD() == registers[1] && args.getRegisterE() == registers[2]
                && args.getRegisterF() == registers[3] && args.getRegisterG() == registers[4]
                && at + 1 == body.size() - 1 && body.get(at + 1).getOpcode() == Opcode.RETURN_VOID,
            "Native resource completion registers or return changed: " + descriptor);
    }
}
