package app.hiro.berryzoom.patches;

import app.hiro.berry.patches.BerryProfiles;
import app.hiro.berry.patches.BerryProfiles.Profile;
import app.morphe.patcher.extensions.InstructionExtensions;
import app.morphe.patcher.patch.BytecodePatch;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patcher.patch.PatchKt;
import app.morphe.patcher.patch.ResourcePatch;
import app.morphe.patcher.util.Document;
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod;
import app.morphe.patcher.util.smali.ExternalLabel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import kotlin.Unit;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class OneHandZoomPatch {
    public static final String NAME = "ダブルタップから指を離さず片手ズーム";
    public static final String EXTENSION = "extensions/berry_onehand_zoom.mpe";
    private static final String HELPER = "Lhiro/berryzoom/OneHandZoom;";
    private static final String KEY = "hiro_one_finger_zoom";
    private static final InstructionExtensions I = InstructionExtensions.INSTANCE;

    private static final BytecodePatch CHECK = PatchKt.bytecodePatch(null, null, false, builder -> {
        builder.compatibleWith(BerryProfiles.TARGET);
        builder.execute(context -> {
            BerryProfiles.require(context.classDefByOrNull(HELPER) == null,
                "片手ズームは適用済みです。未改造の APK を選んでください");
            checked(context);
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });

    private static final BytecodePatch CODE = PatchKt.bytecodePatch(null, null, false, builder -> {
        builder.compatibleWith(BerryProfiles.TARGET);
        builder.dependsOn(CHECK);
        builder.setExtensionInputStream(() -> BerryProfiles.extension(OneHandZoomPatch.class, EXTENSION));
        builder.execute(context -> {
            Profile profile = checked(context);
            MutableMethod touch = profile.checked(context, BerryProfiles.TOUCH);
            MutableMethod intercept = profile.checked(context, BerryProfiles.INTERCEPT);
            I.addInstructionsWithLabels(touch, 0,
                "invoke-static {p0, p1}, Lhiro/berryzoom/OneHandZoom;->touch(Landroid/webkit/WebView;Landroid/view/MotionEvent;)Z\n" +
                "move-result v0\nif-eqz v0, :original\nconst/4 v0, 0x1\nreturn v0\n",
                new ExternalLabel("original", touch.getImplementation().getInstructions().get(0)));
            I.addInstructionsWithLabels(intercept, 0,
                "iget-object v0, p0, " + profile.ref("Lz31;->h0:Lc41;") + "\n" +
                "iget-object v0, v0, " + profile.ref("Lc41;->q:Lcx;") + "\n" +
                "invoke-static {p0, v0, p1}, Lhiro/berryzoom/OneHandZoom;->blocksParent(Landroid/view/ViewGroup;Landroid/webkit/WebView;Landroid/view/MotionEvent;)Z\n" +
                "move-result v1\nif-eqz v1, :original\nconst/4 v0, 0x0\nreturn v0\n",
                new ExternalLabel("original", intercept.getImplementation().getInstructions().get(0)));
            return Unit.INSTANCE;
        });
        return Unit.INSTANCE;
    });

    public static final ResourcePatch PATCH = PatchKt.resourcePatch(NAME,
        "ダブルタップの2回目で指を離さず、下へ動かすと拡大、上へ動かすと縮小します。設定→アクション→片手ズームでオン・オフ可能（初期オン）。通常のタップ・スクロール・2本指ピンチを維持。ページ側の拡大制限に従います。",
        true, builder -> {
            builder.compatibleWith(BerryProfiles.TARGET);
            builder.dependsOn(CODE);
            builder.execute(context -> {
                try (Document document = context.document("res/xml/pref_action.xml")) {
                    Element root = document.getDocumentElement();
                    BerryProfiles.require("PreferenceScreen".equals(root.getTagName()), "設定画面が異なります");
                    Set<String> keys = new HashSet<>();
                    NodeList children = root.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        if (children.item(i) instanceof Element)
                            keys.add(((Element) children.item(i)).getAttribute("android:key"));
                    }
                    BerryProfiles.require(keys.containsAll(Arrays.asList("flick_enabled", "multi_touch_gesture_enabled", "stroke_gesture_enabled"))
                        && !keys.contains(KEY), "設定項目が想定と異なります");
                    Element preference = document.createElement("androidx.preference.SwitchPreferenceCompat");
                    preference.setAttribute("android:key", KEY);
                    preference.setAttribute("android:title", "片手ズーム");
                    preference.setAttribute("android:summary", "ダブルタップの2回目で指を離さず、下へ動かすと拡大、上へ動かすと縮小");
                    preference.setAttribute("android:defaultValue", "true");
                    root.appendChild(preference);
                }
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });

    private OneHandZoomPatch() {}

    private static Profile checked(BytecodePatchContext context) {
        Profile profile = BerryProfiles.select(context);
        MutableMethod touch = profile.checked(context, BerryProfiles.TOUCH);
        MutableMethod intercept = profile.checked(context, BerryProfiles.INTERCEPT);
        BerryProfiles.require(touch.getImplementation().getRegisterCount() == 4
            && intercept.getImplementation().getRegisterCount() == 6, "タッチ処理のレジスタ構造が異なります");
        profile.field(context, "Lz31;->h0:Lc41;");
        profile.field(context, "Lc41;->q:Lcx;");
        return profile;
    }
}
