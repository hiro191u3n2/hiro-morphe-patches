import java.io.*;
import java.util.*;
import app.hiro.newxui.patches.ChatRecommendationsHook;
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

public final class ChatRecommendationsTest {
    static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        ClassDef original = null;
        for (File f : new File(args[0]).listFiles()) if (f.getName().endsWith(".dex"))
            for (ClassDef c : DexFileFactory.loadDexFile(f, Opcodes.getDefault()).getClasses())
                if (ChatRecommendationsHook.TARGET.equals(c.getType())) { require(original == null, "unique class"); original = c; }
        List<Instruction> before = ChatRecommendationsHook.code(ChatRecommendationsHook.target(original));
        String fingerprint = ChatRecommendationsHook.fingerprint(before);
        System.out.println("fingerprint=" + fingerprint);
        if (args.length > 1 && args[1].equals("fingerprint")) return;
        ChatRecommendationsHook.verify(original);
        MutableClass changed = new MutableClass(original);
        ChatRecommendationsHook.apply(changed);
        List<Instruction> after = ChatRecommendationsHook.code(ChatRecommendationsHook.target(changed));
        require(after.size() == before.size() + 3, "exactly three instructions added");
        require(ChatRecommendationsHook.fingerprint(before.subList(0,136)).equals(ChatRecommendationsHook.fingerprint(after.subList(0,136))), "all conversation rendering and pagination preserved");
        require(ChatRecommendationsHook.fingerprint(before.subList(136,before.size())).equals(ChatRecommendationsHook.fingerprint(after.subList(139,after.size()))), "original recommendations and native guard preserved for OFF");
        require(after.get(136).getOpcode() == Opcode.INVOKE_STATIC && ChatRecommendationsHook.ref(after.get(136)).equals("Lapp/hiro/newxui/UiOptions;->hideChatRecommendations()Z"), "runtime toggle call");
        require(after.get(137).getOpcode() == Opcode.MOVE_RESULT && ((OneRegisterInstruction)after.get(137)).getRegisterA() == 6, "dead scratch register only");
        require(after.get(138).getOpcode() == Opcode.IF_NEZ && ((OneRegisterInstruction)after.get(138)).getRegisterA() == 6, "ON skips block");
        int here = 0, destination = 0;
        for (int n = 0; n < after.size(); n++) { if (n == 138) here = destination; if (n == 184) break; destination += after.get(n).getCodeUnits(); }
        require(here + ((OffsetInstruction)after.get(138)).getCodeOffset() == destination, "ON jumps exactly to native Unit return");
        boolean rejected = false;
        try { ChatRecommendationsHook.apply(changed); } catch (IllegalStateException expected) { rejected = true; }
        require(rejected, "duplicate application rejected");
        int modified = 0;
        for (Method m : original.getMethods()) if (m.getImplementation() != null) {
            Method match = null;
            for (Method n : changed.getMethods()) if (m.toString().equals(n.toString())) match = n;
            require(match != null, "original method retained");
            if (!ChatRecommendationsHook.fingerprint(ChatRecommendationsHook.code(m)).equals(ChatRecommendationsHook.fingerprint(ChatRecommendationsHook.code(match)))) modified++;
        }
        require(modified == 1, "one scoped method changed");
        System.out.println("PASS: exact native boundary; existing conversations and pagination untouched; recommendations ON/OFF branch; one method; duplicate rejected");
    }
}
