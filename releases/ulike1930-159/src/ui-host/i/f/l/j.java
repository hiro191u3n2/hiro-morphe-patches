package i.f.l;

import java.util.HashMap;
import java.util.Map;

/** Host-only fixture for existing scene lookup; it never creates a scene. */
public final class j {
    public final Map<String, i.f.l.u.g> scenes = new HashMap<String, i.f.l.u.g>();
    public RuntimeException runtimeFailure;
    public LinkageError linkageFailure;
    public int reads;
    public String lastScene;

    public i.f.l.u.g g(String scene) {
        reads++;
        lastScene = scene;
        if (runtimeFailure != null) throw runtimeFailure;
        if (linkageFailure != null) throw linkageFailure;
        if (scene == null) throw new IllegalArgumentException("native scene is required");
        return scenes.get(scene);
    }
}
