package app.hiro.tripcom.patches;

import app.morphe.patcher.patch.ResourcePatchContext;
import app.morphe.patcher.util.Document;
import java.io.File;

/** Resolve the four existing Home layout names in the decoded resource package. */
public final class HomeResourceDocuments {
    private static final String HOME_ROOT =
            "com.ctrip.ibu.home.home.presentation.page.fragment.widget.HomeAppBarLayout";

    private HomeResourceDocuments() { }

    public static Document document(ResourcePatchContext context, String logicalPath) {
        final String expectedRoot;
        if ("res/layout/t0.xml".equals(logicalPath) || "res/layout/pf.xml".equals(logicalPath)) {
            expectedRoot = HOME_ROOT;
        } else if ("res/layout/s9.xml".equals(logicalPath) || "res/layout/p8.xml".equals(logicalPath)) {
            expectedRoot = "FrameLayout";
        } else {
            throw new IllegalArgumentException("Unexpected Trip Home layout: " + logicalPath);
        }
        // Arsclib's getFile maps an original ZIP filename to its decoded alias.
        // Merging split APKs can make an original filename equal a DIFFERENT
        // logical resource name (t0 -> t4). Resolve only the resource root, then
        // address the known decoded layout directly. Keep Document's normal
        // close/write lifecycle and all existing Home element checks intact.
        File resourceRoot = context.get("res", false);
        File layout = new File(resourceRoot, logicalPath.substring("res/".length()));
        if (!layout.isFile()) {
            throw new IllegalStateException("Trip Home layout is missing: " + logicalPath);
        }
        Document document = new Document(layout);
        if (!expectedRoot.equals(document.getDocumentElement().getTagName())) {
            document.close();
            throw new IllegalStateException("Unexpected Trip Home layout root: " + logicalPath);
        }
        return document;
    }
}
