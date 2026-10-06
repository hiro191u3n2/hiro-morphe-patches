package app.morphe.patcher.patch;

import app.morphe.patcher.util.Document;
import java.io.File;
import java.util.Map;

/** Only the ResourcePatchContext API used by the original Home closure. */
public final class ResourcePatchContext {
    public final File resourceRoot;
    public final Map<String, String> aliases;
    public int assetCalls;
    public int rootGets;
    public int mappedDocuments;

    public ResourcePatchContext(File root, Map<String, String> aliases) {
        this.resourceRoot = root;
        this.aliases = aliases;
    }

    public File get(String path, boolean copy) {
        if (!"res".equals(path) || copy) throw new AssertionError("Unexpected resource-root request");
        rootGets++;
        return resourceRoot;
    }

    public Document document(String path) {
        mappedDocuments++;
        String alias = aliases.containsKey(path) ? aliases.get(path) : path;
        if (!alias.startsWith("res/")) throw new AssertionError("Unexpected logical fixture path");
        return new Document(new File(resourceRoot, alias.substring(4)));
    }
}
