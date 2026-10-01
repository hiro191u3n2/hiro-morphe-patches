package com.hiro.ulike.integration169;

import android.hardware.camera2.CaptureRequest;
import com.hiro.ulike.OpticalZoom;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

/** Runs the actual production disabled gate against SDK36 signatures, on host.
 * No Android framework method or enabled capture path is simulated or claimed.
 */
public final class DisabledBridgeReview {
    static int checks;
    static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static Object field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    interface Action{void run()throws Exception;}
    static void sameFailure(Action action,RuntimeException expected)throws Exception{
        try{action.run();throw new AssertionError("missing delegate exception");}catch(RuntimeException actual){require(actual==expected,"exact delegate exception preserved");}
    }
    public static void main(String[] args)throws Exception {
        require(!CandidateGate169.enabled(),"candidate gate hard false");
        Object owner=new Object();
        require(!CameraBridge169.session(owner,null,null),"original session branch");
        require(CameraBridge169.reader(owner,null)==null,"original reader branch");
        require(!CameraBridge169.ready(),"original readiness branch");
        AppHook169.init(null);AppHook169.install(null);
        AppHook169.choice(new Object(),new Object());AppHook169.request(new Object(),new Object());
        CameraBridge169.configureRequest(owner,null);CameraBridge169.release(owner);
        require(!SavedUriHandoff169.dimensions(new Object(),-1,-1),"original Bitmap dimension branch before new validation");
        require(((Map<?,?>)field(AppHook169.class,"CHOICES")).isEmpty(),"no disabled choice ownership");
        require(((Map<?,?>)field(SavedUriHandoff169.class,"ACTIVE")).isEmpty(),"no disabled URI ownership");
        for(String map:new String[]{"OWNERS","SESSIONS","BUILDERS","REQUESTS"})
            require(((Map<?,?>)field(CameraBridge169.class,map)).isEmpty(),"no disabled camera ownership "+map);
        try {CameraBridge169.installProcessor(null);throw new AssertionError("installed disabled processor");}
        catch(IllegalStateException expected){checks++;}
        require(field(CameraBridge169.class,"processor")==null,"no default processor installed");
        require(CameraBridge169.build(null)==null,"build result preserved");
        require(OpticalZoom.builds==1 && OpticalZoom.last.length==1 && OpticalZoom.last[0]==null,"one exact build delegate");
        require(CameraBridge169.capture(null,null,null,null)==1729,"capture sequence result preserved");
        require(OpticalZoom.captures==1 && OpticalZoom.last.length==4,"one exact capture delegate");
        for(Object x:OpticalZoom.last)require(x==null,"capture argument preserved");
        ArrayList<CaptureRequest> requests=new ArrayList<>();requests.add(null);
        require(CameraBridge169.burst(null,requests,null,null)==1730,"burst sequence result preserved");
        require(OpticalZoom.bursts==1 && OpticalZoom.last[1]==requests,"exact burst list passed without copy/filter");
        RuntimeException marker=new IllegalStateException("delegate-marker");OpticalZoom.failure=marker;
        sameFailure(()->CameraBridge169.build(null),marker);
        sameFailure(()->CameraBridge169.capture(null,null,null,null),marker);
        sameFailure(()->CameraBridge169.burst(null,requests,null,null),marker);
        require(OpticalZoom.builds==2 && OpticalZoom.captures==2 && OpticalZoom.bursts==2,"no duplicate delegates on failure");
        // Exercise real ownership guards without activating Android capture or
        // pretending that a synthetic snapshot is a valid style binding.
        @SuppressWarnings("unchecked") Map<Object,AppHook169.Choice> choices=(Map<Object,AppHook169.Choice>)field(AppHook169.class,"CHOICES");
        Object backend=new Object(),recorder=new Object();
        AppHook169.Choice old=new AppHook169.Choice(recorder,backend,new Object(),1,null);
        AppHook169.Choice newer=new AppHook169.Choice(recorder,backend,new Object(),2,null);
        choices.put(backend,old);AppHook169.requireLive(old);checks++;
        old.expired=true;
        try{AppHook169.requireLive(old);throw new AssertionError("expired choice accepted");}catch(IllegalStateException expected){checks++;}
        choices.put(backend,newer);
        AppHook169.consumed(old);require(choices.get(backend)==newer,"late old completion preserves newer choice");
        try{AppHook169.requireLive(old);throw new AssertionError("foreign generation accepted");}catch(IllegalStateException expected){checks++;}
        AppHook169.processing(newer);require(newer.processing && choices.get(backend)==newer,"processing retains exclusive ownership");
        AppHook169.consumed(newer);require(choices.isEmpty(),"processing completion releases own choice");
        System.out.println("{\"checks\":"+checks+",\"sdk36_signature_compile\":true,\"actual_android_execution\":false,\"enabled_path_tested\":false}");
    }
}
