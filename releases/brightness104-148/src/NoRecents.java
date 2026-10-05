package app.hiro.brightnessclick;

import java.util.HashSet;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Patcher-only manifest edit. No runtime hooks, noHistory or task-killing code. */
public final class NoRecents {
    private static final String NS = "http://schemas.android.com/apk/res/android";
    private static final String PKG = "jp.gr.java_conf.fimyulab.brightnessclick";
    private NoRecents() {}

    public static void apply(Element application) {
        if (application == null || !"application".equals(application.getTagName()))
            throw new IllegalArgumentException("Expected application element");
        Element manifest = (Element) application.getParentNode();
        if (!PKG.equals(manifest.getAttribute("package")))
            throw new IllegalArgumentException("Unexpected target package");
        Set<String> found = new HashSet<>();
        // Validate the existing patch's three entry points before making any edits.
        for (Node node = application.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && "activity".equals(node.getNodeName())) {
                Element activity = (Element) node;
                String name = activity.getAttribute("android:name");
                if (name.startsWith(".")) name = PKG + name;
                else if (name.indexOf('.') < 0) name = PKG + "." + name;
                found.add(name);
            }
        }
        for (String name : new String[]{"BlankActivity", "MainActivity", "PermissionActivity"})
            if (!found.contains(PKG + "." + name))
                throw new IllegalStateException("Missing expected activity: " + name);
        for (Node node = application.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && "activity".equals(node.getNodeName())) {
                Element activity = (Element) node;
                activity.removeAttribute("android:excludeFromRecents");
                activity.setAttributeNS(NS, "android:excludeFromRecents", "true");
            }
        }
    }
}
