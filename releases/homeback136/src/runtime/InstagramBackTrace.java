package app.hiro.oneback.runtime;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.util.Map;
import java.util.WeakHashMap;

/** Temporary 320 diagnostics. Codes contain no URI, account, text or class name.
 * Existing Back callbacks alone schedule a report. Observation wrappers return
 * their already-evaluated argument unchanged; they never query native state. */
final class InstagramBackTrace {
    private InstagramBackTrace() {}
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<Activity,State> STATES=new WeakHashMap<Activity,State>();
    private static final class State {
        int registration,entry=-1,first=-1,last=-1,parent=-1,selected=-1,nativeResult=-1,finish=-1,layers=-1,dispatcher,sources,origins;
        boolean pending,returnGuard,forwarding;
        String container="-",host="O";
        Context context;
        void reset(){entry=first=last=parent=selected=nativeResult=finish=layers=-1;dispatcher=sources=origins=0;container="-";returnGuard=forwarding=false;}
    }
    private static State current(Activity a){return a==null||Looper.myLooper()!=Looper.getMainLooper()?null:STATES.get(a);}
    private static State state(Activity a) {
        if(a==null||Looper.myLooper()!=Looper.getMainLooper())return null;
        State s=STATES.get(a);if(s==null){s=new State();STATES.put(a,s);}return s;
    }
    static void registration(Activity a,int code){try{State s=state(a);if(s!=null)s.registration=code;}catch(RuntimeException|LinkageError ignored){}}
    static void begin(Activity a,int source) {
        try {
            if(Looper.myLooper()!=Looper.getMainLooper()||!InstagramBack.owns(a)||a.isDestroyed())return;
            final State s=state(a);if(s==null)return;
            if(!s.pending) {
                s.reset();s.context=a.getApplicationContext();String host=a.getClass().getName();
                s.host=host.equals("com.instagram.mainactivity.InstagramMainActivity")?"M":host.equals("com.instagram.url.UrlHandlerActivity")?"U":host.equals("com.instagram.url.UrlHandlerLauncherActivity")?"L":"O";
                s.pending=true;
                // The application context and bounded codes survive an Activity
                // finishing this Back. No Activity/View/Fragment is retained.
                MAIN.post(new Runnable(){public void run(){
                    try {
                        String code="HB320 H"+s.host+" R"+s.registration+" E"+number(s.entry)+" S"+number(s.first)+"/"+number(s.last)+" P"+number(s.parent)+" C"+s.container+" Q"+number(s.selected)
                            +"\nA"+s.sources+" D"+s.dispatcher+" N"+number(s.nativeResult)+" F"+number(s.finish)+" O"+s.origins+" T"+(s.returnGuard?1:0)+" W"+(s.forwarding?1:0)+" B"+number(s.layers);
                        s.pending=false;
                        android.util.Log.d("HiroBack320",code);
                        if(s.context!=null)Toast.makeText(s.context,code,Toast.LENGTH_LONG).show();
                    }catch(RuntimeException|LinkageError ignored){s.pending=false;}
                }});
            }
            s.sources|=source;
        }catch(RuntimeException|LinkageError ignored){}
    }
    private static String number(int value){return value<0?"-":String.valueOf(value);}
    static void clear(Activity a){try{STATES.remove(a);}catch(RuntimeException|LinkageError ignored){}}
    static void entry(Activity a,boolean present){try{State s=current(a);if(s!=null&&s.pending)s.entry=present?1:0;}catch(RuntimeException|LinkageError ignored){}}
    static void guards(Activity a,boolean returned,boolean forwarding){try{State s=current(a);if(s!=null&&s.pending){s.returnGuard|=returned;s.forwarding|=forwarding;}}catch(RuntimeException|LinkageError ignored){}}
    static void origin(Activity a,int flag){try{State s=current(a);if(s!=null&&s.pending)s.origins|=flag;}catch(RuntimeException|LinkageError ignored){}}
    static void rejected(Activity a,int stage){try{State s=current(a);if(s!=null&&s.pending){if(s.first<0)s.first=stage;s.last=stage;}}catch(RuntimeException|LinkageError ignored){}}
    static boolean observeReject(Activity a,int stage,boolean rejected){if(rejected)rejected(a,stage);return rejected;}
    static boolean observeAllow(Activity a,int stage,boolean allowed){if(!allowed)rejected(a,stage);return allowed;}
    static Object observeParent(Activity a,Object parent){try{State s=current(a);if(s!=null&&s.pending){String type=parent==null?"":parent.getClass().getName();s.parent=parent==null?0:type.equals("X.00VS")?1:type.equals("X.02Wh")?2:type.equals("X.03CG")?3:9;}}catch(RuntimeException|LinkageError ignored){}return parent;}
    static int observeContainer(Activity a,int id){try{State s=current(a);if(s!=null&&s.pending)s.container=id==0x7f0b22da?"M":id==0x7f0b22e0?"B":"O";}catch(RuntimeException|LinkageError ignored){}return id;}
    static boolean observeSelected(Activity a,boolean same){try{State s=current(a);if(s!=null&&s.pending)s.selected=same?1:0;}catch(RuntimeException|LinkageError ignored){}return same;}
    static boolean observeMismatch(Activity a,boolean different){observeSelected(a,!different);return different;}
    static void layers(Activity a,InstagramBack.Snapshot scene){try{State s=current(a);if(s!=null&&s.pending&&scene!=null)s.layers=(scene.blocked?1:0)|(scene.unknown?2:0)|(scene.otherContent?4:0);}catch(RuntimeException|LinkageError ignored){}}
    static void scene(Activity a,Object target,InstagramBack.Snapshot scene){try{State s=current(a);if(s==null||!s.pending||scene==null)return;layers(a,scene);if(scene.current==null){s.layers|=32;return;}s.layers|=(scene.current.nativeRecognized?0:8)|(scene.current.stateClear?0:16);observeSelected(a,scene.current.target==target);for(InstagramCurrent.Frame frame:scene.current.frames)if(frame.chosen==target){observeContainer(a,frame.containerId);break;}}catch(RuntimeException|LinkageError ignored){}}
    static void safe(Activity a){try{State s=current(a);if(s!=null&&s.pending)s.last=0;}catch(RuntimeException|LinkageError ignored){}}
    static void nativeResult(Activity a,boolean consumed){try{State s=current(a);if(s!=null&&s.pending)s.nativeResult=consumed?1:0;}catch(RuntimeException|LinkageError ignored){}}
    static void finish(Activity a,int result){try{State s=current(a);if(s!=null&&s.pending)s.finish=result;}catch(RuntimeException|LinkageError ignored){}}
    static void dispatcher(Activity a,int result){try{State s=current(a);if(s!=null&&s.pending)s.dispatcher=result;}catch(RuntimeException|LinkageError ignored){}}
}
