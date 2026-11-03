package app.hiro.berrybfcache.patches;

import com.android.tools.smali.dexlib2.formatter.DexFormatter;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Reference-index-independent, exact guard for this one branch-only method. */
public final class ExactBody {
    private ExactBody() {}

    public static String canonical(Method method) {
        MethodImplementation body = method.getImplementation();
        if (body == null || !body.getTryBlocks().isEmpty())
            throw new IllegalStateException("BFCache補正対象の例外処理・実装が想定と異なります");
        StringBuilder out = new StringBuilder();
        out.append(DexFormatter.INSTANCE.getMethodDescriptor(method)).append('\n');
        out.append(method.getAccessFlags()).append('/').append(body.getRegisterCount()).append('\n');
        for (Instruction i : body.getInstructions()) {
            out.append(i.getOpcode().name()).append('/').append(i.getCodeUnits());
            if (i instanceof OneRegisterInstruction) out.append(" A=").append(((OneRegisterInstruction)i).getRegisterA());
            if (i instanceof TwoRegisterInstruction) out.append(" B=").append(((TwoRegisterInstruction)i).getRegisterB());
            if (i instanceof ThreeRegisterInstruction) out.append(" C=").append(((ThreeRegisterInstruction)i).getRegisterC());
            if (i instanceof FiveRegisterInstruction) {
                FiveRegisterInstruction x = (FiveRegisterInstruction)i;
                out.append(" N=").append(x.getRegisterCount());
                out.append(" CDEFG=").append(x.getRegisterC()).append(',').append(x.getRegisterD())
                    .append(',').append(x.getRegisterE()).append(',').append(x.getRegisterF()).append(',').append(x.getRegisterG());
            }
            if (i instanceof RegisterRangeInstruction) {
                RegisterRangeInstruction x = (RegisterRangeInstruction)i;
                out.append(" RANGE=").append(x.getStartRegister()).append(',').append(x.getRegisterCount());
            }
            if (i instanceof WideLiteralInstruction) out.append(" L=").append(((WideLiteralInstruction)i).getWideLiteral());
            if (i instanceof OffsetInstruction) out.append(" O=").append(((OffsetInstruction)i).getCodeOffset());
            if (i instanceof ReferenceInstruction) out.append(" R=").append(DexFormatter.INSTANCE.getReference(((ReferenceInstruction)i).getReference()));
            if (i instanceof DualReferenceInstruction) out.append(" R2=").append(DexFormatter.INSTANCE.getReference(((DualReferenceInstruction)i).getReference2()));
            if (i instanceof PayloadInstruction)
                throw new IllegalStateException("BFCache補正対象に未対応のpayloadがあります");
            out.append('\n');
        }
        return out.toString();
    }

    public static String sha256(Method method) {
        try {
            byte[] sum = MessageDigest.getInstance("SHA-256").digest(canonical(method).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : sum) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

}
