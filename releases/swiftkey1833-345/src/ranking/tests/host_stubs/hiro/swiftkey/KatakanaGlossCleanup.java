package hiro.swiftkey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import m40.b;
import m40.h0;
import l40.c;

/** Model of retained cleanup control flow; the test injects only its mapping. */
final class KatakanaGlossCleanup {
    static final Map<String,String> replacements = new HashMap<String,String>();
    static List<b> last;
    static int calls;
    static Throwable failure;
    static String clean(String reading, String surface) {
        String key = reading + "\t" + surface;
        return replacements.containsKey(key) ? replacements.get(key) : surface;
    }
    static List<b> cleanCandidates(List<b> original, c request, String reading) {
        calls++;
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        if (failure instanceof LinkageError) throw (LinkageError) failure;
        boolean changed = false;
        for (b candidate : original) {
            if (candidate == null) continue;
            String surface = candidate.getUserFacingText();
            if (surface != null && !surface.equals(clean(reading,surface))) { changed=true; break; }
        }
        if (!changed) { last=original; return original; }
        List<b> output = new ArrayList<b>(original.size());
        HashSet<String> seen = new HashSet<String>();
        for (b candidate : original) {
            if (candidate == null) { output.add(null); continue; }
            String surface = candidate.getUserFacingText(), cleaned = clean(reading,surface);
            if (cleaned != null && cleaned.length() == 0) continue;
            if (!seen.add(cleaned)) continue;
            if (surface != null && !surface.equals(cleaned)) {
                h0 added = new h0(cleaned,request.k,request); added.setTrailingSeparator(""); output.add(added);
            } else output.add(candidate);
        }
        last=output;
        return output;
    }
}
