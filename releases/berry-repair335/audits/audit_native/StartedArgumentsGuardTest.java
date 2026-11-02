package app.hiro.berrybfcache.patches;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.immutable.*;
import com.android.tools.smali.dexlib2.immutable.instruction.*;
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference;
public final class StartedArgumentsGuardTest {
    static Method method(Instruction instruction) {
        List<ImmutableMethodParameter> params=Arrays.asList(
            new ImmutableMethodParameter("Landroid/webkit/WebView;",Collections.emptySet(),null),
            new ImmutableMethodParameter("Ljava/lang/String;",Collections.emptySet(),null),
            new ImmutableMethodParameter("Landroid/graphics/Bitmap;",Collections.emptySet(),null));
        return new ImmutableMethod("Ltest/Client;","onPageStarted",params,"V",1,Collections.emptySet(),Collections.emptySet(),
            new ImmutableMethodImplementation(8,Arrays.asList(instruction,new ImmutableInstruction10x(Opcode.RETURN_VOID)),Collections.emptyList(),Collections.emptyList()));
    }
    static void reject(String name,Instruction instruction) {
        try { BfCacheRepairPatch.requireStartedArgumentsIntact(method(instruction)); throw new AssertionError("Accepted "+name); }
        catch (IllegalStateException expected) { System.out.println("PASS rejected "+name); }
    }
    public static void main(String[] args) {
        BfCacheRepairPatch.requireStartedArgumentsIntact(method(new ImmutableInstruction11n(Opcode.CONST_4,0,0)));
        BfCacheRepairPatch.requireStartedArgumentsIntact(method(new ImmutableInstruction22c(Opcode.IPUT_OBJECT,5,0,new ImmutableFieldReference("Ltest/Client;","view","Landroid/webkit/WebView;"))));
        System.out.println("PASS local writes and argument reads");
        reject("WebView parameter write",new ImmutableInstruction11n(Opcode.CONST_4,5,0));
        reject("URL parameter write",new ImmutableInstruction11n(Opcode.CONST_4,6,0));
        reject("wide high half overlaps WebView",new ImmutableInstruction21s(Opcode.CONST_WIDE_16,4,0));
        reject("wide low half overlaps WebView",new ImmutableInstruction21s(Opcode.CONST_WIDE_16,5,0));
        reject("wide low half overlaps URL",new ImmutableInstruction21s(Opcode.CONST_WIDE_16,6,0));
        reject("move-result overwrites parameter",new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT,5));
    }
}
