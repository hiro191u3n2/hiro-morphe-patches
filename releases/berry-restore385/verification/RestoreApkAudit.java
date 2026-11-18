import com.android.tools.smali.dexlib2.iface.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

public class RestoreApkAudit {
    static boolean permitted(String name) {
        return name.equals("Lhiro/berrybfcache/RestoreCoordinator;")
            || name.startsWith("Lhiro/berrybfcache/RestoreCoordinator$");
    }
    static Map<String, byte[]> resources(Path path) throws IOException {
        Map<String, byte[]> result = new TreeMap<>();
        try (ZipFile file = new ZipFile(path.toFile())) {
            for (ZipEntry item : Collections.list(file.entries())) {
                String key = item.getName();
                if (!item.isDirectory() && !key.matches("classes[0-9]*\\.dex") && !key.startsWith("META-INF/")) {
                    try (InputStream in = file.getInputStream(item)) { result.put(key, in.readAllBytes()); }
                }
            }
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        Map<String,ClassDef> old = ZoomApplicationAudit.load(args[0]);
        Map<String,ClassDef> fresh = ZoomApplicationAudit.load(args[1]);
        TreeSet<String> all = new TreeSet<>(old.keySet()); all.addAll(fresh.keySet());
        List<String> changed = new ArrayList<>();
        int same = 0;
        for (String name : all) {
            ClassDef a = old.get(name), b = fresh.get(name);
            if (a!=null && b!=null && Arrays.equals(ZoomApplicationAudit.canonical(a),ZoomApplicationAudit.canonical(b))) {
                same++; continue;
            }
            if (!permitted(name)) throw new AssertionError("Unexpected changed class: "+name);
            changed.add(name);
        }
        if (changed.isEmpty()) throw new AssertionError("Missing restore-code change");
        Map<String,byte[]> oldResources = resources(Path.of(args[0]));
        Map<String,byte[]> newResources = resources(Path.of(args[1]));
        if (!newResources.keySet().equals(oldResources.keySet())) throw new AssertionError("Resource inventory changed");
        for (String name : newResources.keySet()) {
            if (!Arrays.equals(newResources.get(name),oldResources.get(name))) throw new AssertionError("Changed resource: "+name);
        }
        System.out.println("{\"result\":\"PASS\",\"identical_class_definitions\":"+same
             +",\"original_class_count\":"+old.size()+",\"new_class_count\":"+fresh.size()
             +",\"identical_resource_entries\":"+oldResources.size()+",\"changed_classes\":[\""+String.join("\",\"",changed)+"\"]}");
    }
}
