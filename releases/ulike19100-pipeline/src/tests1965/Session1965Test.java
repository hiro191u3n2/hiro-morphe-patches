package com.hiro.ulike;

import android.content.Context;
import android.os.SystemClock;
import i.s.a.w.q;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Runs the actual CameraSession1965 body with controlled scalar ownership APIs. */
public final class Session1965Test {
    private static int checks;
    private static final String SECRET="private-frame-location-error-text";
    public enum Facing {
        FACING_FRONT,FACING_BACK;
        @Override public String toString(){throw new AssertionError("enum toString must not be used");}
    }
    public static final class Sensitive {
        static int inspected;
        @Override public String toString(){inspected++;throw new AssertionError(SECRET);}
        @Override public int hashCode(){inspected++;throw new AssertionError(SECRET);}
        @Override public boolean equals(Object other){inspected++;throw new AssertionError(SECRET);}
    }
    public static final class Frame implements ProviderLifecycle1929.ForbiddenFrame {
        static int inspected;
        public Object pixels(){inspected++;throw new AssertionError(SECRET);}
        public int getWidth(){inspected++;throw new AssertionError(SECRET);}
        @Override public String toString(){inspected++;throw new AssertionError(SECRET);}
        @Override public int hashCode(){inspected++;throw new AssertionError(SECRET);}
        @Override public boolean equals(Object other){inspected++;throw new AssertionError(SECRET);}
    }
    public static final class Camera {
        public Facing facing=Facing.FACING_FRONT;
        public Facing getCameraFacing(){return facing;}
    }
    public static final class Capture {public Camera a=new Camera();}
    public static final class Provider {public Object d=new Sensitive(),e=true,h=false;}
    public static final class Manager {
        public Object current;
        public boolean unavailable;
        public Object h(){if(unavailable)throw new AssertionError("manager unavailable");return current;}
    }
    private static Capture capture;
    private static Manager manager;
    private static Map<Object,Object> samples()throws Exception {
        Field f=CameraSession1965.class.getDeclaredField("samples");f.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Object,Object> value=(Map<Object,Object>)f.get(null);
        return value;
    }
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static CameraTrace1965.Entry last(String phase) {
        CameraTrace1965.Entry found=null;
        for(CameraTrace1965.Entry e:CameraTrace1965.entries)if(e.phase.equals(phase))found=e;
        if(found==null)throw new AssertionError("missing actual event: "+phase);
        return found;
    }
    private static int count(String phase) {
        int n=0;for(CameraTrace1965.Entry e:CameraTrace1965.entries)if(e.phase.equals(phase))n++;return n;
    }
    private static void reset()throws Exception {
        samples().clear();SystemClock.now=0;ManualLens170.reset();ProviderLifecycle1929.reset();CameraTrace1965.reset();
        Frame.inspected=Sensitive.inspected=0;
        capture=new Capture();ManualLens170.values.put("epoch",11L);
        ManualLens170.values.put("capture",new WeakReference<Object>(capture));
        ManualLens170.values.put("foreground",true);ManualLens170.values.put("recording",false);
        manager=new Manager();q.INSTANCE=new q();q.INSTANCE.mProviderManager=manager;
        q.INSTANCE.mCameraInstance=new Sensitive();q.INSTANCE.mCurrentCameraState=2;
        q.INSTANCE.mIsCameraSwitchState=false;q.INSTANCE.mIsCameraPendingClose=true;q.INSTANCE.mOnBackGround=false;
    }
    private static void throttleAndOwnership()throws Exception {
        reset();Provider provider=new Provider();Frame frame=new Frame();manager.current=provider;
        CameraSession1965.input(null,frame);CameraSession1965.input(provider,null);
        check(ManualLens170.reads==0&&ProviderLifecycle1929.reads==0&&ProviderLifecycle1929.calls==0,
            "null notifications exit before ownership and deep reflection");
        check(CameraTrace1965.entries.isEmpty()&&samples().isEmpty(),"null notifications retain and log nothing");
        CameraSession1965.input(provider,frame);
        check(count("provider_first_frame_notice")==1,"first provider notice is emitted once");
        check(last("provider_first_frame_notice").epoch==11L&&last("provider_first_frame_notice").fields.contains("current=true"),
            "first notice identifies the actual current provider and epoch");
        int reads=ProviderLifecycle1929.reads,calls=ProviderLifecycle1929.calls;
        for(int n=0;n<10000;n++)CameraSession1965.input(provider,frame);
        check(CameraTrace1965.entries.size()==1,"high-frequency input does not repeat diagnostic events");
        check(ProviderLifecycle1929.reads==reads&&ProviderLifecycle1929.calls==calls,
            "high-frequency input does not repeat native/provider reflection");
        SystemClock.now=4999;CameraSession1965.input(provider,frame);
        check(CameraTrace1965.entries.size()==1&&ProviderLifecycle1929.reads==reads,"4999ms stays below sampling boundary");
        SystemClock.now=5000;CameraSession1965.input(provider,frame);
        check(count("provider_frame_sample")==1&&ProviderLifecycle1929.reads>reads,"exactly 5000ms emits one real sample");
        reads=ProviderLifecycle1929.reads;SystemClock.now=9999;CameraSession1965.input(provider,frame);
        check(count("provider_frame_sample")==1&&ProviderLifecycle1929.reads==reads,"sample timestamp establishes the next five-second bound");
        SystemClock.now=10000;CameraSession1965.input(provider,frame);
        check(count("provider_frame_sample")==2,"second five-second boundary emits one sample");
        ManualLens170.values.put("epoch",12L);SystemClock.now=10001;CameraSession1965.input(provider,frame);
        check(count("provider_first_frame_notice")==2&&last("provider_first_frame_notice").epoch==12L,
            "epoch replacement emits first notice immediately instead of waiting five seconds");
        capture=new Capture();ManualLens170.values.put("capture",new WeakReference<Object>(capture));
        SystemClock.now=10002;CameraSession1965.input(provider,frame);
        check(count("provider_first_frame_notice")==3&&last("provider_first_frame_notice").fields.contains("capture="+System.identityHashCode(capture)),
            "capture replacement independently emits first notice in the same epoch");
        CameraSession1965.input(provider,frame);
        check(CameraTrace1965.entries.size()==5,"replacement notice also starts a fresh sampling interval");
        Provider other=new Provider();SystemClock.now=10003;CameraSession1965.input(other,frame);
        check(last("provider_first_frame_notice").fields.contains("current=false"),"non-current provider notice is labelled rather than accepted as current");
        check(Frame.inspected==0&&ProviderLifecycle1929.frameReads==0,"actual input helper never inspects or reflects on frame data");
        check(Sensitive.inspected==0,"identity-only camera observations never call sensitive object methods");
    }
    private static void boundedWeakOwnership()throws Exception {
        reset();Frame frame=new Frame();List<Provider> held=new ArrayList<>();
        for(int n=0;n<16;n++){Provider p=new Provider();held.add(p);CameraSession1965.input(p,frame);}
        check(samples() instanceof WeakHashMap,"actual provider table uses weak keys");
        check(samples().size()==16,"sixteen live providers fit the fixed bound");
        SystemClock.now=5000;CameraSession1965.input(held.get(0),frame);
        check(samples().size()==16&&samples().containsKey(held.get(15)),"refreshing an existing provider does not evict the other fifteen");
        Provider seventeenth=new Provider();held.add(seventeenth);CameraSession1965.input(seventeenth,frame);
        check(samples().size()==1&&samples().containsKey(seventeenth),"seventeenth new provider bounds retained observer entries");
        int before=count("provider_first_frame_notice");CameraSession1965.input(held.get(0),frame);
        check(count("provider_first_frame_notice")==before+1,"held provider evicted by cap gets a new first notice");
        boolean bounded=true;
        for(int n=0;n<1000;n++){Provider p=new Provider();held.add(p);CameraSession1965.input(p,frame);bounded&=samples().size()<=16;}
        check(bounded,"continued provider replacement cannot grow the table above sixteen");
        Object sample=samples().values().iterator().next();Field owner=sample.getClass().getDeclaredField("capture");owner.setAccessible(true);
        check(owner.get(sample) instanceof WeakReference&&((WeakReference<?>)owner.get(sample)).get()==capture,
            "sample keeps capture ownership weakly rather than retaining a live camera");
        boolean scalarOrWeak=true;
        for(Field f:sample.getClass().getDeclaredFields())if(!Modifier.isStatic(f.getModifiers()))
            scalarOrWeak&=f.getType()==long.class||f.getType()==WeakReference.class;
        check(scalarOrWeak,"sample retains no strong frame, provider or camera object");
        check(Frame.inspected==0&&ProviderLifecycle1929.frameReads==0,"bounded churn never accesses frame content");
    }
    private static void lifecycleAndPrivacy()throws Exception {
        reset();Sensitive owner=new Sensitive(),device=new Sensitive();Context context=new Context();
        CameraSession1965.track(owner,context);
        check(CameraTrace1965.inits==1&&count("capture_track_request")==1,"track initializes diagnostics and records a request");
        check(last("capture_track_request").fields.contains("facing=FACING_FRONT foreground=true recording=false"),
            "foreground phase contains enum name and scalar state only");
        check(last("native_state").fields.contains("state=2 switching=false closing=true background=false"),
            "known native state remains distinct numeric and boolean evidence");
        CameraSession1965.foreground(owner);check(count("camera_foreground_request")==1,"foreground transition is labelled as a request");
        ManualLens170.values.put("foreground",false);ManualLens170.values.put("recording",true);q.INSTANCE.mOnBackGround=true;
        capture.a.facing=Facing.FACING_BACK;CameraSession1965.background(owner);
        check(last("camera_background_request").fields.contains("facing=FACING_BACK foreground=false recording=true"),
            "background transition reads current owner state without inventing foreground success");
        check(last("native_state").fields.contains("background=true"),"background native flag is separately observed");
        CameraSession1965.opened(owner,device);
        check(count("camera_open_callback")==1&&last("camera_open_owner").fields.contains("device="+System.identityHashCode(device)),
            "open callback records only scalar owner and device identities");
        CameraSession1965.closing(owner);check(count("camera_close_request")==1,"close records request phase");
        CameraSession1965.prepared(owner,-8);check(last("camera_prepare_result").fields.endsWith("result=-8"),"prepare preserves actual error result");
        CameraSession1965.select(1);check(last("lens_select_request").fields.equals("selection=1")&&count("lens_selection_state")==1,
            "selection request and observed state remain separate");
        CameraSession1965.cameraError(owner,73,SECRET);
        check(last("camera_error").anomaly&&last("camera_error").fields.endsWith("code=73"),"camera error anomaly includes only numeric code");
        check(count("camera_error_callback")==1,"camera error also records lifecycle state");
        CameraSession1965.previewResult(owner,0);
        check(last("input_preview_start_result").fields.endsWith("result=0")&&count("input_preview_start_rejected")==0,
            "zero input-start result is recorded without a rejection anomaly");
        CameraSession1965.previewResult(owner,-5);
        check(count("input_preview_start_rejected")==1&&last("input_preview_start_rejected").anomaly,
            "nonzero input-start result produces one rejection anomaly");
        q.INSTANCE.mCurrentCameraState=SECRET;q.INSTANCE.mIsCameraSwitchState=new Sensitive();
        q.INSTANCE.mIsCameraPendingClose=Facing.FACING_BACK;q.INSTANCE.mOnBackGround=new Sensitive();
        CameraSession1965.phase("unknown_native_test",owner);
        check(last("native_state").fields.contains("state=unknown switching=unknown closing=FACING_BACK background=unknown"),
            "arbitrary native values are unknown while enum names remain scalar");
        Provider provider=new Provider();provider.e=SECRET;provider.h=new Sensitive();manager.current=provider;
        CameraSession1965.input(provider,new Frame());
        check(last("provider_first_frame_notice").fields.contains("enabled=unknown ended=unknown"),
            "provider diagnostic ignores arbitrary string and object values");
        boolean privateOnly=true,noVisibleClaim=true;
        for(CameraTrace1965.Entry e:CameraTrace1965.entries){
            privateOnly&=!e.fields.contains(SECRET)&&!e.fields.contains("@")&&!e.fields.contains("pixels");
            noVisibleClaim&=!e.phase.contains("visible")&&!e.phase.contains("presented")&&!e.phase.contains("success")
                &&!e.fields.contains("visible=true")&&!e.fields.contains("visible_confirmed=true");
        }
        check(privateOnly&&Sensitive.inspected==0&&Frame.inspected==0,"logs contain scalar identities and never error text, frame content or arbitrary object rendering");
        check(noVisibleClaim,"requests, native state, input notices and start result never claim visible preview success");
        CameraSession1965.inputTimeout(owner,7L,true);
        check(last("input_preview_timeout").anomaly&&last("input_preview_timeout").epoch==7L,
            "input timeout anomaly correlates to ticket epoch rather than current global epoch");
        check(last("input_preview_timeout").fields.equals("owner="+System.identityHashCode(owner)
            +" awaiting_frame=true current_owned=true visible_confirmed=false"),
            "first-frame timeout explicitly records current ownership while refusing visible success");
        CameraSession1965.inputTimeout(owner,8L,false);
        check(last("input_preview_timeout").epoch==8L&&last("input_preview_timeout").fields.contains("awaiting_frame=false"),
            "input-readiness timeout stays distinct from waiting for a first frame");
        check(Sensitive.inspected==0&&!last("input_preview_timeout").fields.contains(SECRET),
            "timeout retains only owner identity and scalar evidence");
    }
    private static void unavailableState()throws Exception {
        reset();ManualLens170.numberUnavailable=true;
        check(CameraSession1965.epoch()==-1L,"unavailable epoch is represented explicitly as minus one");
        ManualLens170.getUnavailable=true;ManualLens170.yesUnavailable=true;q.INSTANCE=null;
        CameraSession1965.phase("unavailable_test",new Sensitive());
        check(last("unavailable_test").epoch==-1L&&last("unavailable_test").fields.contains("capture=0 facing=unknown foreground=false recording=false"),
            "missing ownership fails open with unknown facing and zero capture identity");
        check(last("native_state").fields.equals("camera=0 state=unknown switching=unknown closing=unknown background=unknown"),
            "missing native host reports only unknown state without throwing");
        Provider p=new Provider();ProviderLifecycle1929.unavailable=true;
        CameraSession1965.input(p,new Frame());
        check(last("provider_first_frame_notice").epoch==-1L&&last("provider_first_frame_notice").fields.contains("current=false capture=0 camera=0 enabled=unknown ended=unknown"),
            "missing native fields and manager remain diagnostic unknowns");
        reset();manager.unavailable=true;p=new Provider();CameraSession1965.input(p,new Frame());
        check(last("provider_first_frame_notice").fields.contains("current=false"),"throwing native manager cannot fabricate current provider ownership");
        reset();CameraTrace1965.failInit=true;CameraSession1965.track(new Sensitive(),new Context());
        check(CameraTrace1965.inits==1&&count("capture_track_request")==1,"optional diagnostic initialization failure does not stop lifecycle observation");
        check(Frame.inspected==0&&Sensitive.inspected==0,"unavailable paths do not fall back to inspecting frame or arbitrary objects");
    }
    private static void returnsAfterActualFault(Runnable observer,String why) {
        int before=CameraTrace1965.faults;boolean returned=false;
        try{observer.run();returned=true;}catch(Throwable unexpected){}
        check(returned&&CameraTrace1965.faults>before,why);
    }
    private static void optionalSinkFaults()throws Exception {
        reset();final Sensitive owner=new Sensitive(),device=new Sensitive();final Context context=new Context();
        String[] names={"phase","track","foreground","background","opened","closing","prepared","select","cameraError","previewResult zero","previewResult rejected","input"};
        Runnable[] observers={
            ()->CameraSession1965.phase("fault_phase",owner),()->CameraSession1965.track(owner,context),
            ()->CameraSession1965.foreground(owner),()->CameraSession1965.background(owner),
            ()->CameraSession1965.opened(owner,device),()->CameraSession1965.closing(owner),
            ()->CameraSession1965.prepared(owner,-1),()->CameraSession1965.select(1),
            ()->CameraSession1965.cameraError(owner,5,SECRET),()->CameraSession1965.previewResult(owner,0),
            ()->CameraSession1965.previewResult(owner,-1),()->CameraSession1965.input(new Provider(),new Frame())};
        for(int mode=1;mode<=2;mode++){
            CameraTrace1965.eventFault=mode;
            for(int n=0;n<observers.length;n++)returnsAfterActualFault(observers[n],
                "actual "+names[n]+" returns to camera caller after diagnostic "+(mode==1?"RuntimeException":"Error"));
            CameraTrace1965.eventFault=0;CameraTrace1965.anomalyFault=mode;
            returnsAfterActualFault(()->CameraSession1965.cameraError(owner,5,SECRET),"cameraError suppresses actual anomaly sink fault mode "+mode);
            returnsAfterActualFault(()->CameraSession1965.previewResult(owner,-1),"rejected preview result suppresses actual anomaly sink fault mode "+mode);
            returnsAfterActualFault(()->CameraSession1965.inputTimeout(owner,7L,true),"input timeout suppresses actual anomaly sink fault mode "+mode);
            CameraTrace1965.anomalyFault=0;CameraTrace1965.initFault=mode;
            returnsAfterActualFault(()->CameraSession1965.init(context),"init suppresses actual storage fault mode "+mode);
            returnsAfterActualFault(()->CameraSession1965.track(owner,context),"track continues after actual storage fault mode "+mode);
            CameraTrace1965.initFault=0;
        }
        CameraTrace1965.entries.clear();Provider p=new Provider();manager.current=p;CameraSession1965.input(p,new Frame());
        check(count("provider_first_frame_notice")==1,"sink faults do not permanently disable later normal input observation");
        CameraSession1965.previewResult(owner,0);
        check(count("input_preview_start_result")==1&&count("input_preview_start_rejected")==0,
            "normal success-result diagnostic is restored without inventing a rejection after sink faults");
        check(Frame.inspected==0&&Sensitive.inspected==0&&ProviderLifecycle1929.frameReads==0,
            "diagnostic failure has no fallback frame or sensitive-object access");
    }
    public static void main(String[] args)throws Exception {
        throttleAndOwnership();boundedWeakOwnership();lifecycleAndPrivacy();unavailableState();optionalSinkFaults();
        System.out.println("CAMERA_SESSION1965_ASSERTIONS="+checks);
    }
}
