package com.hiro.ulike;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import i.f.l.n.q.y.m;

/** Production event helper under host View/native fixtures, not a device test. */
public final class LensVisibilityHost1936 {
    private static int assertions;
    private static void check(boolean ok,String name) {
        assertions++; if(!ok)throw new AssertionError(name);
        System.out.println("PASS\t"+name);
    }
    private static final class Fixture {
        final i.f.l.j session=new i.f.l.j();
        final i.f.l.u.g camera=new i.f.l.u.g();
        final i.f.l.u.j state=new i.f.l.u.j();
        final i.f.l.u.p<Object> facing=new i.f.l.u.p<Object>();
        final i.f.l.n.q.y.c provider=new i.f.l.n.q.y.c(session,"main-camera");
        Fixture(boolean front) {
            state.facing=facing;camera.state=state;session.scenes.put(provider.b,camera);
            m.a=new m();m.b=session;m.c=provider.b;facing.value=Boolean.valueOf(front);
            Looper.worker=false;
        }
        void select(boolean front) {
            facing.value=Boolean.valueOf(front);
            LensVisibility1936.acceptedSwitch(provider,front);
        }
    }
    private static void immediateAndBack() {
        Fixture f=new Fixture(false);
        OpticalZoomUi.Bar bar=new OpticalZoomUi.Bar();
        LensVisibility1936.register(bar);
        check(bar.root.getVisibility()==View.VISIBLE,"initial rear registration preserves visibility");
        f.select(true);
        check(bar.root.getVisibility()==View.GONE,"accepted front switch immediately hides whole multiplier and macro root without preDraw");
        check(bar.root.layouts==1&&bar.root.invalidations==1,"front event schedules one layout and draw");
        check(f.facing.value==Boolean.TRUE,"front native property untouched by helper");
        check(!RearLensUi1930.rearSelected(),"existing stale click/read guard also denies front");
        f.select(false);
        check(bar.root.getVisibility()==View.GONE,"rear event does not prematurely expose an unready rear camera route");
        check(bar.root.layouts==2&&bar.root.invalidations==2,"rear event explicitly schedules existing guarded updater");
        check(RearLensUi1930.rearSelected(),"existing updater can restore only current rear selection");
        bar.root.setVisibility(View.VISIBLE); // Existing updater after route-ready.
        f.select(true);
        check(bar.root.getVisibility()==View.GONE,"second rear to front transition hides again");
        OpticalZoomUi.Bar recreated=new OpticalZoomUi.Bar();
        LensVisibility1936.register(recreated);
        check(recreated.root.getVisibility()==View.GONE,"view recreated while front starts hidden");
        f.select(false);bar.root.setVisibility(View.VISIBLE);recreated.root.setVisibility(View.VISIBLE);
        f.select(true);
        check(bar.root.getVisibility()==View.GONE&&recreated.root.getVisibility()==View.GONE,"all registered copies hide including old and recreated overlay");
        LensVisibility1936.register(new Object());LensVisibility1936.register(null);
        check(f.facing.value==Boolean.TRUE,"unrelated registration never mutates selection");
    }
    private static void queuedAndStale() {
        Fixture f=new Fixture(false);OpticalZoomUi.Bar bar=new OpticalZoomUi.Bar();LensVisibility1936.register(bar);
        Looper.worker=true;f.select(true);f.select(false);
        check(Handler.queued()==2&&bar.root.getVisibility()==View.VISIBLE,"off-main events defer View changes");
        Handler.drain();
        check(bar.root.getVisibility()==View.VISIBLE,"queued old front event rereads newer rear state instead of rehiding it");
        check(bar.root.layouts==2,"queued events request current guarded refresh");
        Looper.worker=true;f.select(false);f.select(true);Handler.drain();
        check(bar.root.getVisibility()==View.GONE,"queued rear then front resolves current front hidden");
        f.select(false);bar.root.setVisibility(View.VISIBLE);
        Looper.worker=true;f.select(true);
        Fixture replacement=new Fixture(false);
        Handler.drain();
        check(bar.root.getVisibility()==View.VISIBLE,"queued retired provider cannot hide replacement session");
        LensVisibility1936.acceptedSwitch(f.provider,true);
        check(bar.root.getVisibility()==View.VISIBLE,"retired provider event ignored synchronously");
        replacement.select(true);
        check(bar.root.getVisibility()==View.GONE,"current replacement provider hides registered overlay");
    }
    private static void unchangedAndMissing() {
        Fixture f=new Fixture(false);OpticalZoomUi.Bar bar=new OpticalZoomUi.Bar();LensVisibility1936.register(bar);
        // The native hook is after the accepted facing write. A native rejected
        // switch never enters this helper; register itself must not toggle.
        LensVisibility1936.register(bar);
        check(bar.root.getVisibility()==View.VISIBLE&&bar.root.layouts==0&&f.facing.value==Boolean.FALSE,"unchanged/rejected native selection retains rear overlay and property");
        LensVisibility1936.acceptedSwitch(null,true);
        check(bar.root.getVisibility()==View.VISIBLE,"null provider ignored");
        m.c="other-scene";LensVisibility1936.acceptedSwitch(f.provider,true);
        check(bar.root.getVisibility()==View.VISIBLE,"inactive scene cannot hide current screen");
        m.c=f.provider.b;f.facing.value=null;LensVisibility1936.acceptedSwitch(f.provider,true);
        check(bar.root.getVisibility()==View.GONE,"unknown facing value fails closed");
        f.facing.value=Boolean.FALSE;bar.root.setVisibility(View.VISIBLE);f.session.scenes.clear();
        LensVisibility1936.acceptedSwitch(f.provider,true);
        check(bar.root.getVisibility()==View.VISIBLE,"missing scene causes no exception or speculative camera switch");
        f.session.scenes.put(f.provider.b,f.camera);f.session.runtimeFailure=new IllegalStateException();
        LensVisibility1936.acceptedSwitch(f.provider,true);f.session.runtimeFailure=null;
        f.facing.value=Boolean.TRUE;LensVisibility1936.acceptedSwitch(f.provider,true);
        check(bar.root.getVisibility()==View.GONE,"bridge recovers after transient native lookup failure");
    }
    public static void main(String[]args) {
        immediateAndBack();queuedAndStale();unchangedAndMissing();
        System.out.println("HOST_LENS1936_ASSERTIONS="+assertions);
        System.out.println("PRODUCTION_HELPER_WITH_HOST_VIEW_AND_NATIVE_FIXTURES_NOT_ANDROID_DEVICE_TEST");
    }
}
