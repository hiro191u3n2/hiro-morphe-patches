package android.content.res;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Explicit host resource map and failures, never an Android Resources object. */
public class Resources {
    public final Map<Integer,String> names = new HashMap<Integer,String>();
    public final Map<Integer,byte[]> bytes = new HashMap<Integer,byte[]>();
    public int nameReads, opens;
    public boolean failName, failOpen;
    public InputStream override;
    public String getResourceEntryName(int id) {
        nameReads++;
        if (failName || !names.containsKey(id)) throw new NotFoundException();
        return names.get(id);
    }
    public InputStream openRawResource(int id) {
        opens++;
        if (failOpen || !bytes.containsKey(id)) throw new NotFoundException();
        return override != null ? override : new ByteArrayInputStream(bytes.get(id));
    }
    public static class NotFoundException extends RuntimeException {}
}
