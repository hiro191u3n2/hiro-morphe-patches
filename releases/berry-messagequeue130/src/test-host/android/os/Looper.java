package android.os;
import android.util.Printer;
import java.util.ArrayList;
import java.util.List;
public class Looper {
    public static final Looper MAIN = new Looper();
    public final List<String> lines = new ArrayList<String>();
    public int dumpCalls;
    public boolean throwOnDump;
    public static Looper getMainLooper() { return MAIN; }
    public void dump(Printer printer, String prefix) {
        dumpCalls++;
        if (throwOnDump) { throw new IllegalStateException("private dump error data"); }
        for (String line : lines) { printer.println(line); }
    }
}
