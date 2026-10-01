package com.hiro.ulike.integration169;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import com.hiro.ulike.binding.ShotStyleSettings;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

/** Exact existing photo-choice/request hooks. Disabled private candidate never changes capture. */
public final class AppHook169 {
    private AppHook169() {}
    static volatile Context app;
    private static long nextEpoch;
    private static final Map<Object,Choice> CHOICES=new IdentityHashMap<>();
    static final class Choice {
        final Object recorder,backend,identity;
        final long epoch,dateTakenMs;
        final ShotStyleSettings.Snapshot settings;
        Object request,bitmapCallback;
        SavedUriHandoff169.Token handoff;
        Runnable timeout;boolean expired,processing;
        Choice(Object recorder,Object backend,Object identity,long epoch,ShotStyleSettings.Snapshot settings) {
            this.recorder=recorder;this.backend=backend;this.identity=identity;this.epoch=epoch;this.settings=settings;dateTakenMs=System.currentTimeMillis();
        }
    }
    public static synchronized void init(Context context) {
        if(context!=null && app==null) { Context a=context.getApplicationContext();app=a==null?context:a; }
    }
    public static void install(PreferenceActivity activity) {
        if(activity==null)return;init(activity);PreferenceScreen screen=activity.getPreferenceScreen();
        if(screen==null || screen.findPreference("hiro_candidate169")!=null)return;
        Preference p=new Preference(activity);p.setKey("hiro_candidate169");p.setTitle("保存エンジン接続検証 169");p.setSummary(CandidateGate169.STATUS);p.setSelectable(false);screen.addPreference(p);
    }
    public static synchronized void choice(Object state,Object recorder) {
        if(!CandidateGate169.enabled())return;
        try {
            Object backend=publicField(recorder,"b");
            if(!"com.ss.android.vesdk.TECameraVideoRecorder".equals(backend.getClass().getName()))throw new IllegalStateException("unsupported recorder factory");
            if(!CHOICES.isEmpty())throw new IllegalStateException("another shot is in flight");
            Object id=new Object();long epoch=++nextEpoch;
            ShotStyleSettings.Snapshot settings=ShotStyleSettings.freezeAtChoice(state,recorder,id,epoch);
            Choice choice=new Choice(recorder,backend,id,epoch,settings);CHOICES.put(backend,choice);
            choice.timeout=()->{
                synchronized(AppHook169.class){if(CHOICES.get(backend)!=choice || choice.processing)return;choice.expired=true;}
                // Never acquire CameraBridge's lock while holding this monitor.
                CameraBridge169.cancelChoice(choice);
                consumed(choice);SavedUriHandoff169.fail(choice.handoff,"request-timeout",new IllegalStateException("Photo request did not reach owned capture"));
            };
            if(!new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(choice.timeout,20000)){CHOICES.remove(backend);throw new IllegalStateException("main handler unavailable");}
        } catch(Exception e) { throw new IllegalStateException("same-shot style snapshot unavailable",e); }
    }
    public static synchronized void request(Object recorder,Object request) {
        if(!CandidateGate169.enabled())return;
        Choice choice=null;boolean claimed=false;
        try {
            choice=CHOICES.get(publicField(recorder,"b"));
            if(choice==null || choice.expired || choice.recorder!=recorder || choice.request!=null || request==null)throw new IllegalStateException("photo request has no unique choice");
            claimed=true;ShotStyleSettings.requireCurrent(choice.settings);
            Object callback=request.getClass().getMethod("getBitmapCaptureCallback").invoke(request);
            if(callback==null)throw new IllegalStateException("app callback missing");
            choice.request=request;choice.bitmapCallback=callback;choice.handoff=SavedUriHandoff169.prepare(callback,choice.identity,choice.epoch);
            request.getClass().getMethod("setFrameSource",int.class).invoke(request,0);
            request.getClass().getMethod("setForceUseFramePreviewSource",boolean.class).invoke(request,false);
            request.getClass().getMethod("setEnableShotScreenAfterCaptureFailed",boolean.class).invoke(request,false);
        } catch(Exception e) { if(choice!=null && claimed){consumed(choice);if(choice.handoff!=null)SavedUriHandoff169.fail(choice.handoff,"request",e);else SavedUriHandoff169.rejectUnprepared(choice.bitmapCallback,e);}throw new IllegalStateException("request binding rejected",e); }
    }
    static synchronized Choice forCamera(Object cameraOwner)throws Exception {
        Object callback=publicField(cameraOwner,"x0");
        if("i.s.a.w.q$g$a".equals(callback.getClass().getName()))callback=publicField(publicField(callback,"a"),"c");
        if(!"com.ss.android.vesdk.TECameraVideoRecorder$60".equals(callback.getClass().getName()))throw new IllegalStateException("unreviewed photo callback");
        Object backend=publicField(callback,"i");Choice c=CHOICES.get(backend);
        if(c==null || c.expired || c.request==null || c.bitmapCallback==null || publicField(c.recorder,"b")!=backend)throw new IllegalStateException("recorder/capture identity mismatch");
        ShotStyleSettings.requireCurrent(c.settings);return c;
    }
    static synchronized void requireLive(Choice choice){if(choice==null || choice.expired || CHOICES.get(choice.backend)!=choice)throw new IllegalStateException("Expired photo choice");}
    static synchronized void processing(Choice choice){requireLive(choice);choice.processing=true;clearTimeout(choice);}
    private static void clearTimeout(Choice choice){if(choice!=null && choice.timeout!=null){new android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(choice.timeout);choice.timeout=null;}}
    static synchronized void consumed(Choice choice) { clearTimeout(choice);if(choice!=null && CHOICES.get(choice.backend)==choice)CHOICES.remove(choice.backend); }
    static Object publicField(Object o,String name)throws Exception { if(o==null)throw new IllegalStateException("missing owner "+name);return o.getClass().getField(name).get(o); }
    static Object declaredField(Object o,String name)throws Exception { Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o); }
}
