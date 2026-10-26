import com.android.tools.smali.smali.Smali;
import com.android.tools.smali.smali.SmaliOptions;
public final class Assemble184 {
    public static void main(String[] args) throws Exception {
        SmaliOptions options = new SmaliOptions();
        options.apiLevel = 26; options.jobs = 1; options.verboseErrors = true; options.outputDexFile = args[1];
        if (!Smali.assemble(options, args[0])) throw new IllegalStateException("Smali assembly failed");
    }
}
