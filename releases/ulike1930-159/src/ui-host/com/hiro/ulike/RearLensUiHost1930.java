package com.hiro.ulike;

import i.f.l.n.q.y.m;
import i.f.l.u.g;
import i.f.l.u.j;
import i.f.l.u.p;

/** Executes the production selection helper against host-only native fixtures.
 * This does not execute Android Views, native switching, ART, or camera hardware.
 */
public final class RearLensUiHost1930 {
    private static int assertions;

    private static void check(boolean passed, String description) {
        assertions++;
        if (!passed) throw new AssertionError(description);
        System.out.println("PASS\t" + description);
    }

    private static final class Fixture {
        final m owner = new m();
        final i.f.l.j session = new i.f.l.j();
        final String scene = "main-camera";
        final g camera = new g();
        final j state = new j();
        final p<Object> property = new p<Object>();

        Fixture(Object selected) {
            session.scenes.put(scene, camera);
            camera.state = state;
            state.facing = property;
            property.value = selected;
            m.a = owner;
            m.b = session;
            m.c = scene;
        }

        boolean intact(Object selected) {
            return m.a == owner && m.b == session && scene.equals(m.c)
                    && session.scenes.get(scene) == camera && camera.state == state
                    && state.facing == property
                    && property.value == selected;
        }
    }

    private static void choicesAndMissingState() {
        Fixture f = new Fixture(Boolean.FALSE);
        check(RearLensUi1930.rearSelected(), "explicit rear selection is allowed");
        check(f.intact(Boolean.FALSE), "rear lookup leaves the native selection intact");
        check(f.session.reads == 1 && f.camera.reads == 1
                        && f.state.reads == 1 && f.property.reads == 1
                        && f.scene.equals(f.session.lastScene),
                "one lookup takes one snapshot of each native level");

        f = new Fixture(Boolean.TRUE);
        check(!RearLensUi1930.rearSelected(), "front selection is denied");
        check(f.intact(Boolean.TRUE), "front lookup leaves the native selection intact");

        m.a = null;
        check(!RearLensUi1930.rearSelected(), "missing native owner is denied");
        f = new Fixture(Boolean.FALSE);
        m.b = null;
        check(!RearLensUi1930.rearSelected() && f.session.reads == 0,
                "missing native session is denied without an old session lookup");
        f = new Fixture(Boolean.FALSE);
        m.c = null;
        check(!RearLensUi1930.rearSelected() && f.session.reads == 0,
                "missing current scene key is denied before native lookup");
        f = new Fixture(Boolean.FALSE);
        m.c = "unregistered-scene";
        check(!RearLensUi1930.rearSelected() && f.camera.reads == 0
                        && f.session.scenes.size() == 1,
                "unregistered scene is denied without constructing or reusing camera state");
        f = new Fixture(Boolean.FALSE);
        f.camera.state = null;
        check(!RearLensUi1930.rearSelected() && f.state.reads == 0 && f.property.reads == 0,
                "missing camera state is denied without dereferencing old fixtures");
        f = new Fixture(Boolean.FALSE);
        f.state.facing = null;
        check(!RearLensUi1930.rearSelected() && f.property.reads == 0,
                "missing facing property is denied");
        new Fixture(null);
        check(!RearLensUi1930.rearSelected(), "missing selection value is denied");

        Object[] invalid = {Integer.valueOf(0), "false", "true", new Object()};
        String[] names = {"numeric zero", "false text", "true text", "unrelated object"};
        for (int i = 0; i < invalid.length; i++) {
            f = new Fixture(invalid[i]);
            check(!RearLensUi1930.rearSelected() && f.intact(invalid[i]),
                    names[i] + " is not treated as a rear-camera Boolean");
        }
    }

    private static void failures() {
        for (int level = 0; level < 4; level++) {
            for (int kind = 0; kind < 2; kind++) {
                Fixture f = new Fixture(Boolean.FALSE);
                RuntimeException runtime = new IllegalStateException("host fixture");
                LinkageError linkage = new NoSuchMethodError("host fixture");
                if (level == 0) {
                    f.session.runtimeFailure = kind == 0 ? runtime : null;
                    f.session.linkageFailure = kind == 1 ? linkage : null;
                } else if (level == 1) {
                    f.camera.runtimeFailure = kind == 0 ? runtime : null;
                    f.camera.linkageFailure = kind == 1 ? linkage : null;
                } else if (level == 2) {
                    f.state.runtimeFailure = kind == 0 ? runtime : null;
                    f.state.linkageFailure = kind == 1 ? linkage : null;
                } else {
                    f.property.runtimeFailure = kind == 0 ? runtime : null;
                    f.property.linkageFailure = kind == 1 ? linkage : null;
                }
                check(!RearLensUi1930.rearSelected() && f.intact(Boolean.FALSE),
                        "native level " + level + (kind == 0 ? " runtime failure" : " linkage failure")
                                + " denies selection without changing state");
                f.session.runtimeFailure = null;
                f.session.linkageFailure = null;
                f.camera.runtimeFailure = null;
                f.camera.linkageFailure = null;
                f.state.runtimeFailure = null;
                f.state.linkageFailure = null;
                f.property.runtimeFailure = null;
                f.property.linkageFailure = null;
                check(RearLensUi1930.rearSelected(),
                        "native level " + level + " recovers immediately after failure kind " + kind);
            }
        }
        Fixture f = new Fixture(Boolean.FALSE);
        f.session.linkageFailure = new ExceptionInInitializerError("host fixture");
        check(!RearLensUi1930.rearSelected(), "class initialization linkage failure is denied");
    }

    private static void selectionChanges() {
        Fixture f = new Fixture(Boolean.FALSE);
        Object[] sequence = {Boolean.FALSE, Boolean.TRUE, Boolean.FALSE,
                Boolean.TRUE, null, Boolean.TRUE, Boolean.FALSE};
        for (int i = 0; i < sequence.length; i++) {
            // Simulate the native state owner completing each choice. The helper
            // is not permitted to infer a switch from a gesture or toggle state.
            f.property.value = sequence[i];
            check(RearLensUi1930.rearSelected() == Boolean.FALSE.equals(sequence[i])
                            && f.intact(sequence[i]),
                    "successive native selection " + i + " is read without a cached result");
        }
        f.property.value = Boolean.FALSE;
        check(RearLensUi1930.rearSelected(), "rear choice before a rejected-switch simulation");
        check(RearLensUi1930.rearSelected() && f.intact(Boolean.FALSE),
                "unchanged native choice is not toggled by a second lookup");

        p<Object> replacement = new p<Object>();
        replacement.value = Boolean.TRUE;
        f.state.facing = replacement;
        check(!RearLensUi1930.rearSelected(), "replaced facing property is read immediately");
        j replacementState = new j();
        replacementState.facing = new p<Object>();
        replacementState.facing.value = Boolean.FALSE;
        f.camera.state = replacementState;
        check(RearLensUi1930.rearSelected(), "replaced camera model restores rear eligibility");
        new Fixture(Boolean.TRUE);
        check(!RearLensUi1930.rearSelected(), "replaced camera owner overrides previous rear choice");
    }

    private static void sessionsAndScenes() {
        Fixture f = new Fixture(Boolean.FALSE);
        g other = new g();
        other.state = new j();
        other.state.facing = new p<Object>();
        other.state.facing.value = Boolean.TRUE;
        f.session.scenes.put("other-camera", other);
        check(RearLensUi1930.rearSelected(), "front state in another scene cannot hide active rear choice");
        m.c = "other-camera";
        check(!RearLensUi1930.rearSelected() && "other-camera".equals(f.session.lastScene),
                "current scene key immediately selects its front-camera state");
        m.c = f.scene;
        check(RearLensUi1930.rearSelected(), "returning to the rear scene restores eligibility");
        f.session.scenes.remove(f.scene);
        check(!RearLensUi1930.rearSelected(), "removed current scene cannot reuse the last rear result");
        f.session.scenes.put(f.scene, other);
        check(!RearLensUi1930.rearSelected(), "replacement scene under the same key is read live");
        f.session.scenes.put(f.scene, f.camera);
        check(RearLensUi1930.rearSelected(), "rear scene registration restores eligibility without a timer");

        i.f.l.j replacementSession = new i.f.l.j();
        replacementSession.scenes.put(f.scene, other);
        m.b = replacementSession;
        int oldReads = f.session.reads;
        check(!RearLensUi1930.rearSelected() && f.session.reads == oldReads
                        && replacementSession.reads == 1,
                "replacement session is read instead of retaining the old session");
        m.b = f.session;
        check(RearLensUi1930.rearSelected(), "restored native session immediately supplies its rear state");
        m.c = "";
        check(!RearLensUi1930.rearSelected(), "unregistered empty scene never falls back to old rear state");
    }

    public static void main(String[] args) {
        choicesAndMissingState();
        failures();
        selectionChanges();
        sessionsAndScenes();
        System.out.println("HOST_UI1930_ASSERTIONS=" + assertions);
        System.out.println("PRODUCTION_SELECTION_HELPER_WITH_MOCK_NATIVE_MODEL_NOT_ANDROID_VIEW_OR_DEVICE_TEST");
    }
}
