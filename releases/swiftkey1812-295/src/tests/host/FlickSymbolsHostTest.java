import hiro.swiftkey.*;
import android.content.*;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.inputmethod.*;
import android.os.Handler;
import androidx.preference.*;
import com.touchtype.KeyboardService;
import com.touchtype_fluency.service.j0;
import java.util.*;

/** Behavioral host test. Native actors, geometry and restrictions are explicit fakes. */
public final class FlickSymbolsHostTest {
    static int assertions, scenarios;
    static final lh0.a TOKEN = new lh0.a();
    static long clock=10000;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static void scenario(){scenarios++;FlickSymbols.reset();JapaneseGlide.reset();ViewConfiguration.reset();LineEnter.intercept=false;JapaneseGlideDecoder.ready=true;JapaneseGlideDecoder.blockDecode=false;JapaneseGlideDecoder.blockLoad=false;JapaneseGlideDecoder.calls=0;JapaneseGlideDecoder.empty=false;JapaneseGlideDecoder.invalid=false;JapaneseGlideCandidates.dismiss();Handler.drain();}
    static final class Prefs implements SharedPreferences {
        final Map<String,Boolean> values=new HashMap<String,Boolean>();int writes;boolean fail,readFail;
        public boolean getBoolean(String k,boolean def){if(readFail)throw new IllegalStateException("PRIVATE_NATIVE_FAILURE");return values.containsKey(k)?values.get(k):def;}
        public Editor edit(){if(fail)throw new RuntimeException("preferences");return new Editor(){String key;boolean value;public Editor putBoolean(String k,boolean v){key=k;value=v;return this;}public void apply(){values.put(key,value);writes++;}};}
        void flick(boolean on){values.put(FlickSymbols.KEY,on);}void glide(boolean on){values.put(JapaneseGlide.KEY,on);}
    }
    static final class State implements dc0.a {
        String composition="",raw="";int start,end;
        public String s(){return composition;}public String G(){return raw;}public int k(){return start;}public int S(){return end;}
    }
    static final class Model extends wb0.x {
        final State state=new State();final List<String> glide=new ArrayList<String>();
        Model(){c=new wb0.n0();c.state=state;e=new wb0.z0();e.Y=e.L=true;e.G=k40.l.b;e.c=new ah0.w0();i=new com.microsoft.tokenshare.m();vb0.b mode=new vb0.b();mode.b=vb0.c.NORMAL;i.b=mode;}
        public void P(lh0.a token,String input,String punct,ec0.m m,boolean b,Integer n){glide.add(input);state.composition+=input;state.raw+=input;state.start+=input.length();state.end=state.start;}
    }
    static final class NativeKey {
        final fc0.n2 key=new fc0.n2();final fc0.d1 actor=new fc0.d1();final hc0.o wrapper=new hc0.o();
        NativeKey(RectF rect,String symbol){key.e=new fc0.m1();key.e.a=rect;key.d=new fc0.d2(){};oc0.m behavior=new oc0.m();key.b=behavior;behavior.b=new hc0.a();behavior.b.c=new hc0.k();ic0.e e=new ic0.e();e.a=actor;wrapper.action=e;behavior.b.c.d.add(wrapper);actor.b=actor.c=symbol;}
        float x(View v){return key.e.a.centerX()*v.width;}float y(View v){return key.e.a.centerY()*v.height;}
    }
    static final class View extends xd0.f0 {
        final KeyboardService service;final Model model=new Model();final Prefs prefs=new Prefs();
        final List<Integer> events=new ArrayList<Integer>();final List<String> insertions=new ArrayList<String>();final Map<Character,NativeKey> nativeKeys=new HashMap<Character,NativeKey>();
        String before="before",after="after";int cancelCalls,tapUps;boolean cancelFail;Runnable onCancel;
        boolean modelNativeTouch,nativePressed,nativeTimer;NativeKey nativeKey;int nativeMoves,nativeRetargets,nativeLongPresses;
        void advanceNativeTimer(){if(modelNativeTouch&&nativeTimer){nativeTimer=false;nativeLongPresses++;I.count=1;}}
        NativeKey nativeHit(float x,float y){for(NativeKey k:nativeKeys.values())if(k.key.e.a.contains(x/width,y/height))return k;return null;}
        View(boolean flick,boolean glide){
            super(new KeyboardService());service=(KeyboardService)getContext();service.i=new EditorInfo();service.h=new InputConnection(){public CharSequence getTextBeforeCursor(int n,int f){return before;}public CharSequence getTextAfterCursor(int n,int f){return after;}};
            prefs.flick(flick);prefs.glide(glide);FlickSymbols.init(prefs);JapaneseGlide.init(prefs);
            a=new fb0.f0();a.c=new fb0.q0();a.c.c=qj0.a.x4;fb0.f0 layout=(fb0.f0)a;layout.keys=new wb0.x0();layout.keys.b=com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;layout.keys.a=new ArrayList();
            z=model;j=true;A=new fz.h();I=new android.util.SparseArray();
            String[] rows={"qwertyuiop","asdfghjkl","zxcvbnm"};float[] offsets={0,.05f,.15f};
            for(int row=0;row<3;row++)for(int col=0;col<rows[row].length();col++){
                char code=rows[row].charAt(col);final RectF r=new RectF(offsets[row]+col*.1f,row*.20f+.05f,offsets[row]+(col+1)*.1f,row*.20f+.25f);
                j0 key=new j0();key.a=new t40.d(){public RectF c(android.graphics.Matrix m){return r;}};key.b=new z20.c();key.b.b=Arrays.asList(String.valueOf(code));layout.keys.a.add(key);
                final String symbol=code=='q'?"1":code=='a'?"@":String.valueOf((char)('0'+col));final NativeKey n=new NativeKey(r,symbol);nativeKeys.put(code,n);layout.d.add(n.key);
                n.actor.callback=()->{insertions.add(n.actor.b);model.state.composition+=n.actor.b;model.state.raw+=n.actor.b;model.state.start+=n.actor.b.length();model.state.end=model.state.start;before+=n.actor.b;};
            }
        }
        public boolean f(lh0.a token,MotionEvent e){
            events.add(e.action);
            if(modelNativeTouch){
                if(e.action==0){nativePressed=true;nativeTimer=true;nativeKey=nativeHit(e.x,e.y);}
                if(e.action==2&&nativePressed){nativeMoves++;NativeKey next=nativeHit(e.x,e.y);if(next!=nativeKey){nativeRetargets++;nativeKey=next;}}
                if(e.action==3||e.action==1){nativePressed=false;nativeTimer=false;}
            }
            if(e.action==3){cancelCalls++;if(onCancel!=null)onCancel.run();if(cancelFail)throw new RuntimeException("native cancel");}
            if(e.action==1)tapUps++;return true;
        }
        NativeKey key(char c){return nativeKeys.get(c);}
    }
    static MotionEvent event(int action,float x,float y,long down,long delta){return new MotionEvent(action,x,y,down,down+delta);}
    static long down(View v,char c){NativeKey k=v.key(c);long d=clock;clock+=1000;check(!dispatch(v,event(0,k.x(v),k.y(v),d,0)),"DOWN passes to native");return d;}
    static boolean dispatch(View v,MotionEvent e){boolean consumed=JapaneseGlide.handle(v,TOKEN,e);if(!consumed)v.f(TOKEN,e);return consumed;}
    static boolean direct(View v,MotionEvent e){boolean consumed=FlickSymbols.handle(v,TOKEN,e,false);if(!consumed)v.f(TOKEN,e);return consumed;}
    static boolean up(View v,char c,long d,float dx,float upward,long delta){NativeKey k=v.key(c);return dispatch(v,event(1,k.x(v)+dx,k.y(v)-upward,d,delta));}
    static void flick(View v,char c){int cancels=v.cancelCalls,inserts=v.insertions.size(),nativeUps=v.tapUps;long d=down(v,c);NativeKey k=v.key(c);check(dispatch(v,event(2,k.x(v),k.y(v)-20,d,30)),"qualified MOVE captures gesture");check(v.cancelCalls==cancels+1&&v.insertions.size()==inserts&&v.tapUps==nativeUps,"first qualified MOVE cancels without early insertion");check(up(v,c,d,0,45,90),"flick UP consumed");}
    static int pendingGlide()throws Exception{java.lang.reflect.Field f=JapaneseGlide.class.getDeclaredField("pending");f.setAccessible(true);return((Collection)f.get(null)).size();}
    static void waitForCandidate()throws Exception{for(int i=0;i<2000;i++){Handler.drain();if(JapaneseGlideCandidates.listener!=null)return;Thread.sleep(1);}throw new AssertionError("glide async timeout");}
    static void noSymbol(View v,String reason){check(v.insertions.isEmpty(),reason+" no symbol");check(v.cancelCalls==0,reason+" no flick cancel");}

    static Preference diagnostic(Context context) {
        PreferenceScreen screen=new PreferenceScreen(context);
        FlickSymbols.install(screen);
        Preference diagnostic=screen.H(FlickSymbols.DIAGNOSTIC_KEY);
        check(diagnostic!=null&&diagnostic.f!=null,"diagnostic click listener installed");
        return diagnostic;
    }
    static String copyDiagnostic(Context context) {
        Preference diagnostic=diagnostic(context);
        diagnostic.f.k(diagnostic);
        ClipboardManager clipboard=(ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        check(clipboard.primaryClip!=null,"explicit click copies diagnostic");
        return clipboard.primaryClip.getItemAt(0).getText().toString();
    }
    static long metric(String report,String name) {
        java.util.regex.Matcher match=java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(name)+"=(\\d+)").matcher(report);
        check(match.find(),"diagnostic includes counter "+name);
        return Long.parseLong(match.group(1));
    }
    static void privacy(String report) {
        check(!report.contains("PRIVATE_BEFORE")&&!report.contains("PRIVATE_AFTER")
            &&!report.contains("PRIVATE_COMPOSITION")&&!report.contains("PRIVATE_RAW")
            &&!report.contains("PRIVATE_NATIVE_FAILURE")&&!report.contains("PRIVATE_CLIPBOARD_FAILURE")&&!report.contains("PRIVATE_SUMMARY_FAILURE")&&!report.contains("PRIVATE_SLOP_FAILURE"),
            "diagnostic never contains editor text or exception messages");
    }
    static void pathReason(View view,String reason) {
        String report=copyDiagnostic(view.service);
        check(report.contains("最後の拒否: "+reason+"\n"),"precise path refusal "+reason);
        check(report.contains("最後の移動拒否: "+reason),"path measurements are tied to refusal "+reason);
        privacy(report);
    }
    static float pathMetric(String report,String name) {
        java.util.regex.Matcher match=java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(name)+"=(-?\\d+(?:\\.\\d+)?)px").matcher(report);
        check(match.find(),"diagnostic includes path measurement "+name);
        return Float.parseFloat(match.group(1));
    }
    static String pathLine(String report) {
        for(String line:report.split("\n"))if(line.startsWith("最後の移動拒否: "))return line;
        throw new AssertionError("missing path diagnostic line");
    }
    static float horizontalAt(float degrees,float length) {return (float)(Math.sin(Math.toRadians(degrees))*length);}
    static float upwardAt(float degrees,float length) {return (float)(Math.cos(Math.toRadians(degrees))*length);}
    static void oneSymbol(View v,String reason) throws Exception {
        check(v.insertions.size()==1&&v.cancelCalls==1&&v.tapUps==0,reason+" commits one symbol and consumes native UP");
        check(pendingGlide()==0&&v.model.glide.isEmpty(),reason+" has no second glide owner");
    }
    static boolean moveAt(View v,char key,long down,float dx,float up,long elapsed) {
        NativeKey k=v.key(key);return dispatch(v,event(2,k.x(v)+dx,k.y(v)-up,down,elapsed));
    }
    static void captured(View v,String reason) {
        check(v.cancelCalls==1&&v.insertions.isEmpty()&&v.tapUps==0,reason+" cancels native exactly once without inserting on MOVE");
    }
    static void noOwnedSymbol(View v,String reason) {
        check(v.insertions.isEmpty()&&v.cancelCalls==1&&v.tapUps==0,reason+" aborts insertion and never sends UP after native CANCEL");
    }

    static void captureFlickTests() throws Exception {
        // Capture is a state transition, not a requirement that every later
        // sensor sample remain inside a corridor. These are production-code
        // tests on explicit host fakes, not Android/native execution.
        for(boolean glide:new boolean[]{false,true})for(int side:new int[]{-1,1})for(float angle:new float[]{0,45,60,67.4f}) {
            scenario();View v=new View(true,glide);long d=down(v,'a');
            check(moveAt(v,'a',d,side*horizontalAt(angle,40),upwardAt(angle,40),30),"upper sector captures on MOVE angle="+angle);
            captured(v,"diagonal capture");
            check(up(v,'a',d,side*150,100,90),"post-capture longer diagonal remains owned");
            check(v.insertions.equals(Arrays.asList("@")),"originating key determines symbol");oneSymbol(v,"diagonal capture glide="+glide);
        }
        for(int side:new int[]{-1,1})for(float angle:new float[]{67.4f,67.6f,90,120}) {
            scenario();View v=new View(true,false);long d=down(v,'a');
            boolean consumed=up(v,'a',d,side*horizontalAt(angle,70),upwardAt(angle,70),60);
            check(consumed==(angle<67.5f),"quick DOWN-UP uses upper three of eight direction sectors "+angle);
            if(consumed)oneSymbol(v,"quick diagonal");else noSymbol(v,"clear non-up sector");
        }
        for(boolean glide:new boolean[]{false,true})for(boolean history:new boolean[]{false,true})for(float[] first:new float[][]{{25,0},{25,10},{0,-12},{-25,0}}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,glide);long d=down(v,'a');NativeKey k=v.key('a');
            if(history){MotionEvent e=event(1,k.x(v)+50,k.y(v)-50,d,90);e.history=new float[][]{{k.x(v)+first[0],k.y(v)-first[1]}};check(dispatch(v,e),"initial curve then diagonal accepted in history");}
            else {check(!moveAt(v,'a',d,first[0],first[1],20),"small initial curve remains native and undecided");check(FlickSymbols.pending(v),"pre-recognition deadzone defers glide");check(up(v,'a',d,50,50,90),"later clear upward intent may capture");}
            oneSymbol(v,"initial curve glide="+glide+" history="+history);
        }
        // The same geometric sequence used to succeed only if 25px happened
        // to be ACTION_UP, not MOVE or a historical sample before ACTION_UP.
        for(boolean glide:new boolean[]{false,true})for(int sampling:new int[]{0,1,2}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,glide);long d=down(v,'a');NativeKey k=v.key('a');
            if(sampling==2){MotionEvent e=event(1,k.x(v),k.y(v)-25,d,100);e.history=new float[][]{{k.x(v),k.y(v)-50},{k.x(v),k.y(v)-30},{k.x(v),k.y(v)-25}};check(dispatch(v,e),"batched rebound preserves recognition");}
            else {check(moveAt(v,'a',d,0,50,20),"first upward sample captures");captured(v,"rebound");check(moveAt(v,'a',d,0,30,40),"rebound MOVE remains owned");if(sampling==1)check(moveAt(v,'a',d,0,25,60),"same terminal coordinate as MOVE remains owned");check(up(v,'a',d,0,25,100),"rebound release remains owned");}
            oneSymbol(v,"event sampling invariant "+sampling);
        }
        for(boolean glide:new boolean[]{false,true})for(int height:new int[]{99,101,150,500})for(boolean quick:new boolean[]{false,true}) {
            scenario();View v=new View(true,glide);long d=down(v,'a');
            if(!quick){check(moveAt(v,'a',d,0,25,30),"long flick first crosses activation threshold");captured(v,"long flick");}
            check(up(v,'a',d,0,height,100),"upward stroke is not invalidated by key-height cap height="+height);
            oneSymbol(v,"long flick quick="+quick+" glide="+glide);
        }
        for(boolean glide:new boolean[]{false,true})for(float[] end:new float[][]{{0,0},{0,-80},{300,0},{-300,10},{20,5}}) {
            scenario();View v=new View(true,glide);long d=down(v,'a');
            check(moveAt(v,'a',d,0,25,30),"initial upward intent locks symbol gesture");
            check(moveAt(v,'a',d,end[0],end[1],60),"return or side drift cannot transfer an already cancelled native stream");
            check(up(v,'a',d,end[0],end[1],100),"release commits latched upward intent");oneSymbol(v,"locked intent includes return-to-origin");
        }
        for(boolean glide:new boolean[]{false,true})for(int elapsed:new int[]{501,1000,3000}) {
            scenario();View v=new View(true,glide);v.modelNativeTouch=true;long d=down(v,'a');
            check(v.nativePressed&&v.nativeTimer,"modeled native press and long-press timer start on DOWN");
            check(moveAt(v,'a',d,0,45,40),"qualified MOVE cancels native press before it can retarget");captured(v,"early ownership");
            check(v.nativeMoves==0&&v.nativeRetargets==0&&!v.nativePressed&&!v.nativeTimer,"native never receives qualifying MOVE and its timer is cleared");
            v.advanceNativeTimer();check(v.nativeLongPresses==0&&v.I.size()==0,"cancelled modeled timer cannot open popup while flick is held");
            check(moveAt(v,'a',d,0,150,elapsed),"captured gesture survives later event beyond acquisition timeout");
            check(up(v,'a',d,0,140,elapsed+10),"capture time limit does not apply to release");oneSymbol(v,"slow release after early capture");
        }
        scenario();View nativeControl=new View(false,false);nativeControl.modelNativeTouch=true;long ncd=down(nativeControl,'a');
        check(!moveAt(nativeControl,'a',ncd,0,70,40),"OFF lets modeled native process the same MOVE");
        check(nativeControl.nativeMoves==1&&nativeControl.nativeRetargets==1,"control confirms model would retarget without cancellation");
        nativeControl.advanceNativeTimer();check(nativeControl.nativeLongPresses==1&&nativeControl.I.size()==1,"control confirms active modeled timer can open popup");
        up(nativeControl,'a',ncd,0,70,700);noSymbol(nativeControl,"disabled modeled native control");
        for(int elapsed:new int[]{500,501}) {
            scenario();View v=new View(true,false);long d=down(v,'a');boolean consumed=up(v,'a',d,0,30,elapsed);
            check(consumed==(elapsed==500),"500ms remains an acquisition limit, inclusive");if(consumed)oneSymbol(v,"capture deadline");else noSymbol(v,"too late before capture");
        }
        for(boolean glide:new boolean[]{false,true})for(boolean history:new boolean[]{false,true})for(float[] jitter:new float[][]{{14,6},{18,8},{13,13},{0,18},{3,4}}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,glide);long d=down(v,'a');NativeKey k=v.key('a');
            MotionEvent e=event(1,k.x(v),k.y(v),d,80);
            if(history)e.history=new float[][]{{k.x(v)+jitter[0],k.y(v)-jitter[1]}};
            else check(!moveAt(v,'a',d,jitter[0],jitter[1],30),"subthreshold intermediate jitter never captures");
            check(!dispatch(v,e),"subthreshold tap remains native across event batching dx="+jitter[0]+" up="+jitter[1]+" glide="+glide+" history="+history);noSymbol(v,"tap jitter");check(v.tapUps==1,"tap gets exactly one native UP");
        }
        for(boolean glide:new boolean[]{false,true})for(float[] diagonal:new float[][]{{14,14},{40,17},{-40,17}}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,glide);long d=down(v,'a');
            check(up(v,'a',d,diagonal[0],diagonal[1],60),"upward component guard retains short45degree and longerwide diagonal");oneSymbol(v,"vertical jitter floor remains diagonal-friendly");
        }
        for(int scale:new int[]{1,2,4}) {
            scenario();ViewConfiguration.scaledTouchSlop=12*scale;View v=new View(true,false);v.width=1000*scale;v.height=400*scale;long d=down(v,'a');
            check(!moveAt(v,'a',d,20*scale,-5*scale,20),"density-scaled initial curve stays undecided");
            check(moveAt(v,'a',d,25*scale,25*scale,40),"density-scaled diagonal captures");
            check(up(v,'a',d,100*scale,150*scale,100),"scaled longer gesture commits");oneSymbol(v,"scaled gesture");
        }
        for(int failure:new int[]{1,2})for(boolean getter:new boolean[]{false,true}) {
            scenario();if(getter)ViewConfiguration.valueFailure=failure;else ViewConfiguration.getFailure=failure;
            View v=new View(true,false);long d=down(v,'a');check(!moveAt(v,'a',d,25,0,20),"configuration failure retains finite size-based undecided zone");
            check(up(v,'a',d,35,35,80),"configuration failure does not disable upward capture");oneSymbol(v,"platform configuration fallback");privacy(copyDiagnostic(v.service));
        }
        for(int direction:new int[]{-1,1}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,false);long d=down(v,'a');
            check(!moveAt(v,'a',d,direction*49,0,20),"clear side movement stays native when glide disabled");
            check(!FlickSymbols.pending(v),"clear side movement yields ownership before any capture");
            check(!up(v,'a',d,0,60,90),"later upward motion cannot steal an already yielded stream");noSymbol(v,"clear horizontal handoff");
        }
    }

    static void ownedFailureTests() throws Exception {
        // Every state failure after synthetic CANCEL must consume the terminal
        // native UP, including failures thrown on later MOVE rather than UP.
        String[] names={"disable","reset","editor","connection","text","selection","composition","layout","resize","window","restriction","actor","inputType","imeOptions","geometry","removedKey","popup","touchExploration","readFailure"};
        for(boolean glide:new boolean[]{false,true})for(int kind=0;kind<names.length;kind++)for(boolean failOnMove:new boolean[]{false,true}) {
            scenario();View v=new View(true,glide);long d=down(v,'a');check(moveAt(v,'a',d,0,25,20),"capture before context failure "+names[kind]);captured(v,"context failure setup");
            switch(kind){
                case 0:v.prefs.flick(false);break;
                case 1:JapaneseGlide.reset();break;
                case 2:v.service.i=new EditorInfo();break;
                case 3:v.service.h=new InputConnection(){};break;
                case 4:v.before="external";break;
                case 5:v.model.state.start=v.model.state.end=2;break;
                case 6:v.model.state.raw="external";break;
                case 7:v.a=new fb0.f0();break;
                case 8:v.width=900;break;
                case 9:v.window=new android.os.IBinder(){};break;
                case 10:v.key('a').wrapper.allowed=false;break;
                case 11:((ic0.e)v.key('a').wrapper.action).a=new fc0.d1();break;
                case 12:v.service.i.inputType=129;break;
                case 13:v.service.i.imeOptions=0x1000000;break;
                case 14:v.key('a').key.e.a.right+=.01f;break;
                case 15:((fb0.f0)v.a).d.remove(v.key('a').key);break;
                case 16:v.I.count=1;break;
                case 17:v.A.popup=true;break;
                case 18:v.prefs.readFail=true;break;
            }
            if(failOnMove)check(moveAt(v,'a',d,0,30,40),"owned failure consumes MOVE "+names[kind]);
            check(up(v,'a',d,0,50,80),"owned failure consumes UP "+names[kind]);
            noOwnedSymbol(v,"captured context failure "+names[kind]);check(pendingGlide()==0,"failed captured gesture cannot become glide");
            v.prefs.readFail=false;
        }
        for(boolean glide:new boolean[]{false,true})for(int failure:new int[]{0,1,2,3,4,5,6}) {
            scenario();View v=new View(true,glide);long d=down(v,'a');NativeKey k=v.key('a');check(moveAt(v,'a',d,0,25,20),"capture before malformed event");
            MotionEvent e=event(2,k.x(v),k.y(v)-30,d,40);
            if(failure==0){e.action=5;e.count=2;}else if(failure==1)e.pointer=9;else if(failure==2)e.down=d+1;
            else if(failure==3)e.x=Float.NaN;else if(failure==4)e.history=new float[][]{{Float.POSITIVE_INFINITY,k.y(v)}};
            else if(failure==5)e.action=3;else e.event=d-1;
            check(dispatch(v,e),"captured invalid event is consumed kind="+failure);
            if(failure!=5)check(up(v,'a',d,0,50,80),"invalid owned stream drains original UP kind="+failure);
            noOwnedSymbol(v,"owned invalid event "+failure);
        }
        for(int duringCancel:new int[]{0,1,2,3,4}) {
            scenario();View v=new View(true,false);long d=down(v,'a');
            if(duringCancel==0)v.cancelFail=true;
            if(duringCancel==1)v.onCancel=()->JapaneseGlide.reset();
            if(duringCancel==2)v.onCancel=()->v.prefs.flick(false);
            if(duringCancel==3)v.onCancel=()->v.service.i=new EditorInfo();
            if(duringCancel==4)v.onCancel=()->{throw new IllegalStateException("PRIVATE_NATIVE_FAILURE");};
            check(moveAt(v,'a',d,0,25,20),"cancel failure consumes qualifying MOVE");
            check(moveAt(v,'a',d,0,30,40),"cancel failure drains later MOVE");
            check(up(v,'a',d,0,50,80),"cancel failure drains UP");noOwnedSymbol(v,"failure during capture cancellation");privacy(copyDiagnostic(v.service));
        }
        scenario();View failActor=new View(true,false);long fd=down(failActor,'a');check(moveAt(failActor,'a',fd,0,25,20),"actor failure setup");failActor.key('a').actor.fail=true;
        check(up(failActor,'a',fd,0,50,80),"actor exception consumes UP");check(failActor.key('a').actor.calls==1,"actor is never retried after exception");
        check(failActor.insertions.size()==1&&failActor.cancelCalls==1&&failActor.tapUps==0,"actor throwing after its insertion still cannot produce a duplicate native UP");
        scenario();View noToken=new View(true,false);long nd=down(noToken,'a');NativeKey nk=noToken.key('a');
        check(!FlickSymbols.handle(noToken,null,event(2,nk.x(noToken),nk.y(noToken)-25,nd,20),false),"missing token cannot cancel native on capture");
        up(noToken,'a',nd,0,50,80);noSymbol(noToken,"missing token before capture");
        scenario();View recovery=new View(true,false);long rd=down(recovery,'a');check(moveAt(recovery,'a',rd,0,25,20),"reset recovery setup");FlickSymbols.reset();
        check(up(recovery,'a',rd,0,50,80),"reset drains matching UP");noOwnedSymbol(recovery,"reset before fresh gesture");
        int cancelledBefore=recovery.cancelCalls;long fresh=down(recovery,'a');check(up(recovery,'a',fresh,0,50,80),"fresh DOWN after drain can start a new gesture");
        check(recovery.insertions.size()==1&&recovery.cancelCalls==cancelledBefore+1,"reset does not leave persistent drain suppression");
        for(boolean invalidCurrent:new boolean[]{false,true}) {
            scenario();View v=new View(true,false);long d=down(v,'a');NativeKey k=v.key('a');
            MotionEvent e=event(1,invalidCurrent?Float.NaN:k.x(v),k.y(v),d,80);
            e.history=invalidCurrent?new float[][]{{k.x(v),k.y(v)-25}}:new float[][]{{k.x(v),k.y(v)-25},{Float.NaN,k.y(v)-30}};
            check(dispatch(v,e),"qualifying history cancels native before later nonfinite point in same batch");noOwnedSymbol(v,"nonfinite after historical capture");
        }
        scenario();View popupBefore=new View(true,false);popupBefore.modelNativeTouch=true;long pbd=down(popupBefore,'a');popupBefore.advanceNativeTimer();
        check(!up(popupBefore,'a',pbd,0,50,90),"already opened native popup keeps priority before flick capture");noSymbol(popupBefore,"modeled long press before capture");
    }

    static void captureDiagnosticsTests() throws Exception {
        scenario();View v=new View(true,false);v.before="PRIVATE_BEFORE";v.after="PRIVATE_AFTER";v.model.state.raw="PRIVATE_RAW";v.model.state.composition="PRIVATE_COMPOSITION";
        String before=copyDiagnostic(v.service);long d=down(v,'a');check(moveAt(v,'a',d,0,25,20),"diagnostic capture setup");String held=copyDiagnostic(v.service);
        check(held.contains("1.8.12"),"installed helper identifies capture build");
        check(metric(held,"認識")==metric(before,"認識")+1&&metric(held,"成功")==metric(before,"成功"),"recognition is counted before release without claiming insertion");
        check(held.contains("最後の認識:")&&held.contains("最後の認識結果:"),"diagnostic distinguishes capture details and result");privacy(held);
        check(up(v,'a',d,0,180,1000),"diagnostic long captured gesture succeeds");String succeeded=copyDiagnostic(v.service);
        check(metric(succeeded,"成功")==metric(before,"成功")+1,"actual UP insertion counts success");privacy(succeeded);
        long aborted=down(v,'a');check(moveAt(v,'a',aborted,0,25,20),"diagnostic abort setup");FlickSymbols.reset();check(up(v,'a',aborted,0,50,80),"diagnostic aborted gesture drains");String failed=copyDiagnostic(v.service);
        check(metric(failed,"認識後中止")==metric(succeeded,"認識後中止")+1,"captured cancellation has separate counter");
        check(failed.contains("RESET_CAPTURED"),"reset after capture has meaningful outcome");privacy(failed);
        scenario();ViewConfiguration.scaledTouchSlop=24;View side=new View(true,false);side.before="PRIVATE_BEFORE";long sd=down(side,'a');
        check(!moveAt(side,'a',sd,60,0,20),"uncaptured side movement yields");String measured=copyDiagnostic(side.service);
        check(measured.contains("PATH_CURRENT_HORIZONTAL"),"pre-capture non-up handoff remains diagnosable");
        check(pathMetric(measured,"dx")==60f&&pathMetric(measured,"up")==0f,"handoff diagnostic measures rejected point rather than a later event");
        String last=pathLine(measured);FlickSymbols.reset();FlickSymbols.init(side.prefs);check(pathLine(copyDiagnostic(side.service)).equals(last),"settings reinitialization retains historical movement evidence");
        flick(side,'a');String recovered=copyDiagnostic(side.service);check(pathLine(recovered).equals(last),"later success cannot overwrite old path measurements");privacy(recovered);
    }

    public static void main(String[] args)throws Exception{
        scenario();Prefs p=new Prefs();FlickSymbols.init(p);PreferenceScreen screen=new PreferenceScreen(new Context());FlickSymbols.install(screen);FlickSymbols.install(screen);check(screen.children.size()==2,"toggle and diagnostic setting after repeated installation");SwitchPreferenceCompat option=(SwitchPreferenceCompat)screen.H(FlickSymbols.KEY);check(!option.checked&&!option.s,"default off and explicit store");check(option.title.equals("上フリックで長押し文字を入力"),"setting title");
        Preference initialDiagnostic=screen.H(FlickSymbols.DIAGNOSTIC_KEY);
        ClipboardManager initialClipboard=(ClipboardManager)screen.a.getSystemService(Context.CLIPBOARD_SERVICE);
        check(initialDiagnostic!=null&&!initialDiagnostic.s&&initialDiagnostic.title.equals("上フリック診断をコピー"),"diagnostic setting is nonpersistent");
        check(initialDiagnostic.summary.contains("1.8.12")&&initialDiagnostic.summary.contains("EVENT_NOT_RECEIVED"),"diagnostic version and no events are visible before input");
        check(metric(initialDiagnostic.summary,"DOWN")==0&&metric(initialDiagnostic.summary,"MOVE")==0&&metric(initialDiagnostic.summary,"UP")==0,"initial event counters are zero");
        check(initialClipboard.writes==0,"installation never writes clipboard");
        initialDiagnostic.f.k(initialDiagnostic);
        check(initialClipboard.writes==1&&initialClipboard.primaryClip.text.toString().contains("設定: OFF"),"explicit diagnostic click copies current disabled status");
        check(p.writes==0,"diagnostics do not write preferences");
        check(option.e.i(option,Boolean.TRUE)&&p.getBoolean(FlickSymbols.KEY,false)&&p.writes==1,"ON persisted");check(option.e.i(option,Boolean.FALSE)&&!p.getBoolean(FlickSymbols.KEY,true),"OFF persisted");check(!option.e.i(option,"yes"),"invalid option rejected");p.fail=true;check(!option.e.i(option,Boolean.TRUE),"store failure rejected");FlickSymbols.install(null);

        scenario();Prefs existingPrefs=new Prefs();FlickSymbols.init(existingPrefs);PreferenceScreen existingScreen=new PreferenceScreen(new Context());
        SwitchPreferenceCompat existingToggle=new SwitchPreferenceCompat(existingScreen.a,null);existingToggle.l=FlickSymbols.KEY;existingScreen.G(existingToggle);
        FlickSymbols.install(existingScreen);FlickSymbols.install(existingScreen);
        check(existingScreen.children.size()==2&&existingScreen.H(FlickSymbols.KEY)==existingToggle,"diagnostic installs when toggle already exists without replacing it");

        scenario();View off=new View(false,false);long od=down(off,'q');check(!up(off,'q',od,0,45,90),"default OFF leaves UP native");noSymbol(off,"disabled");check(off.events.equals(Arrays.asList(0,1)),"disabled native sequence");
        for(boolean glide:new boolean[]{false,true})for(char c:new char[]{'q','a'}){scenario();View v=new View(true,glide);flick(v,c);check(v.insertions.equals(Arrays.asList(c=='q'?"1":"@")),"dynamic native symbol "+c+" glide="+glide);check(v.key(c).actor.calls==1&&v.key(c).actor.lastToken==TOKEN,"native actor exactly once with token");check(v.events.equals(Arrays.asList(0,3))&&v.cancelCalls==1&&v.tapUps==0,"native DOWN/CANCEL, qualified MOVE and UP owned by flick");check(!FlickSymbols.pending(v)&&pendingGlide()==0,"flick clears both pending tracks");check(!up(v,c,clock,0,45,90),"duplicate orphan UP not captured");check(v.key(c).actor.calls==1,"duplicate UP no duplicate symbol");}

        scenario();View dynamic=new View(true,false);dynamic.key('q').actor.b=dynamic.key('q').actor.c="€";flick(dynamic,'q');check(dynamic.insertions.equals(Arrays.asList("€")),"native alternate replaces hardcoded layout assumption");
        scenario();View unicode=new View(true,false);unicode.key('q').actor.b=unicode.key('q').actor.c="😀";flick(unicode,'q');check(unicode.insertions.equals(Arrays.asList("😀")),"one Unicode code point supported");
        scenario();View tap=new View(true,false);long td=down(tap,'a');check(!up(tap,'a',td,1,2,60),"tap native");noSymbol(tap,"tap");check(tap.tapUps==1,"tap gets one native UP");
        scenario();View shortMove=new View(true,false);long smd=down(shortMove,'a');check(!up(shortMove,'a',smd,0,15,50),"short movement not flick");noSymbol(shortMove,"short movement");
        scenario();View popup=new View(true,false);long pd=down(popup,'q');popup.A.popup=true;check(!up(popup,'q',pd,0,45,90),"touch exploration leaves native input");noSymbol(popup,"touch exploration");
        scenario();View popupArray=new View(true,false);long pad=down(popupArray,'q');popupArray.I.count=1;up(popupArray,'q',pad,0,45,90);noSymbol(popupArray,"secondary popup state");
        scenario();View timeout=new View(true,false);long ltd=down(timeout,'a');up(timeout,'a',ltd,0,45,501);noSymbol(timeout,"long press timeout");
        scenario();View reverseTime=new View(true,false);long rtd=down(reverseTime,'a');up(reverseTime,'a',rtd,0,45,-1);noSymbol(reverseTime,"invalid timestamp");
        scenario();View multi=new View(true,false);long md=down(multi,'q');MotionEvent second=event(5,50,40,md,30);second.count=2;dispatch(multi,second);up(multi,'q',md,0,45,60);noSymbol(multi,"multi pointer");
        scenario();View pointer=new View(true,false);long pid=down(pointer,'q');MotionEvent changed=event(1,50,15,pid,90);changed.pointer=1;dispatch(pointer,changed);noSymbol(pointer,"pointer identity");
        scenario();View cancelled=new View(true,false);long cd=down(cancelled,'a');dispatch(cancelled,event(3,100,140,cd,20));up(cancelled,'a',cd,0,45,60);check(cancelled.insertions.isEmpty()&&cancelled.cancelCalls==1,"system CANCEL no actor, original cancel only");
        scenario();View disabledMid=new View(true,false);long dd=down(disabledMid,'q');disabledMid.prefs.flick(false);up(disabledMid,'q',dd,0,45,80);noSymbol(disabledMid,"disabled during gesture");
        scenario();View editor=new View(true,false);long ed=down(editor,'q');editor.service.i=new EditorInfo();up(editor,'q',ed,0,45,80);noSymbol(editor,"editor identity changed");
        scenario();View connection=new View(true,false);long icd=down(connection,'q');connection.service.h=new InputConnection(){};up(connection,'q',icd,0,45,80);noSymbol(connection,"connection identity changed");
        scenario();View textChanged=new View(true,false);long txd=down(textChanged,'q');textChanged.before="external";up(textChanged,'q',txd,0,45,80);noSymbol(textChanged,"surrounding text changed");
        scenario();View selection=new View(true,false);long sed=down(selection,'q');selection.model.state.start=2;selection.model.state.end=2;up(selection,'q',sed,0,45,80);noSymbol(selection,"selection changed");
        scenario();View composition=new View(true,false);long cod=down(composition,'q');composition.model.state.raw="changed";up(composition,'q',cod,0,45,80);noSymbol(composition,"composition changed");
        scenario();View layout=new View(true,false);long ld=down(layout,'q');layout.a=new fb0.f0();up(layout,'q',ld,0,45,80);noSymbol(layout,"layout replaced");
        scenario();View resize=new View(true,false);long rd=down(resize,'q');resize.width=900;up(resize,'q',rd,0,45,80);noSymbol(resize,"view resize");
        scenario();View window=new View(true,false);long wd=down(window,'q');window.window=new android.os.IBinder(){};up(window,'q',wd,0,45,80);noSymbol(window,"window identity changed");
        for(int type:new int[]{129,145,225,2}){scenario();View secure=new View(true,false);secure.service.i.inputType=type;long sd=down(secure,'q');up(secure,'q',sd,0,45,80);noSymbol(secure,"excluded input "+type);}
        scenario();View language=new View(true,false);((fb0.f0)language.a).keys.b=com.microsoft.fluency.ResultsFilter.PredictionSearchType.OTHER;long jad=down(language,'q');up(language,'q',jad,0,45,80);noSymbol(language,"non-Japanese layout");
        scenario();View unsupported=new View(true,false);MotionEvent mouse=event(0,50,60,clock,0);mouse.tool=3;dispatch(unsupported,mouse);dispatch(unsupported,event(1,50,15,clock,90));noSymbol(unsupported,"unsupported tool");
        scenario();View stylus=new View(true,false);long std=clock;clock+=1000;MotionEvent se=event(0,50,60,std,0);se.tool=2;dispatch(stylus,se);check(dispatch(stylus,event(1,50,15,std,80)),"stylus flick");check(stylus.insertions.size()==1,"stylus one symbol");
        scenario();View history=new View(true,false);long hd=down(history,'q');MotionEvent h=event(1,50,15,hd,90);h.history=new float[][]{{140,55}};dispatch(history,h);noSymbol(history,"historical sideways excursion");
        scenario();View downward=new View(true,false);long downd=down(downward,'q');dispatch(downward,event(2,50,145,downd,30));up(downward,'q',downd,0,45,90);noSymbol(downward,"down then up");
        scenario();View reverse=new View(true,false);long revd=down(reverse,'a');dispatch(reverse,event(2,100,80,revd,30));up(reverse,'a',revd,0,35,90);oneSymbol(reverse,"qualified flick survives reversal");
        scenario();View longStroke=new View(true,false);long lsd=down(longStroke,'a');up(longStroke,'a',lsd,0,150,90);oneSymbol(longStroke,"long upward stroke is still a flick");
        for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY}){scenario();View bad=new View(true,false);long badD=down(bad,'q');dispatch(bad,event(1,invalid,15,badD,80));noSymbol(bad,"nonfinite coordinates");}

        scenario();View restriction=new View(true,false);long restrD=down(restriction,'q');restriction.key('q').wrapper.allowed=false;up(restriction,'q',restrD,0,45,90);noSymbol(restriction,"native restriction changed");
        scenario();View replacedActor=new View(true,false);long rad=down(replacedActor,'q');((ic0.e)replacedActor.key('q').wrapper.action).a=new fc0.d1();up(replacedActor,'q',rad,0,45,90);noSymbol(replacedActor,"native actor changed");
        for(String invalid:new String[]{"ab","\n",""}){scenario();View text=new View(true,false);text.key('q').actor.b=text.key('q').actor.c=invalid;long d=down(text,'q');up(text,'q',d,0,45,90);noSymbol(text,"non-single printable alternate");}
        scenario();View nonCharacter=new View(true,false);nonCharacter.key('q').actor.d=false;long ncd=down(nonCharacter,'q');up(nonCharacter,'q',ncd,0,45,90);noSymbol(nonCharacter,"space/action alternate");
        scenario();View ambiguous=new View(true,false);NativeKey ak=ambiguous.key('q');((oc0.m)ak.key.b).b.c.d.add(ak.wrapper);long amd=down(ambiguous,'q');up(ambiguous,'q',amd,0,45,90);noSymbol(ambiguous,"ambiguous alternate actors");
        scenario();View overlap=new View(true,false);((fb0.f0)overlap.a).d.add(overlap.key('q').key);long ovd=down(overlap,'q');up(overlap,'q',ovd,0,45,90);noSymbol(overlap,"ambiguous hit rectangles");
        scenario();View failCancel=new View(true,false);failCancel.cancelFail=true;long fcd=down(failCancel,'q');check(up(failCancel,'q',fcd,0,45,90),"failed native cancel still consumes UP");check(failCancel.key('q').actor.calls==0&&failCancel.tapUps==0,"no insert or native UP after cancel failure");
        scenario();View failActor=new View(true,false);failActor.key('q').actor.fail=true;long fad=down(failActor,'q');check(up(failActor,'q',fad,0,45,90),"failed actor consumes UP");check(failActor.key('q').actor.calls==1&&failActor.tapUps==0,"native actor never retried");
        scenario();View cancelEditor=new View(true,false);cancelEditor.onCancel=()->cancelEditor.service.i=new EditorInfo();long ced=down(cancelEditor,'q');check(up(cancelEditor,'q',ced,0,45,90),"cancel changing editor still consumes UP");check(cancelEditor.key('q').actor.calls==0,"editor checked again after native cancel");
        scenario();View cancelledRestriction=new View(true,false);cancelledRestriction.onCancel=()->cancelledRestriction.key('q').wrapper.allowed=false;long crd=down(cancelledRestriction,'q');up(cancelledRestriction,'q',crd,0,45,90);check(cancelledRestriction.key('q').actor.calls==0,"restriction checked after cancel");

        scenario();View mutateType=new View(true,false);long mtd=down(mutateType,'q');mutateType.service.i.inputType=129;up(mutateType,'q',mtd,0,45,90);noSymbol(mutateType,"same editor changed to password");
        scenario();View mutateOptions=new View(true,false);long mod=down(mutateOptions,'q');mutateOptions.service.i.imeOptions=0x1000000;up(mutateOptions,'q',mod,0,45,90);noSymbol(mutateOptions,"same editor changed options");
        scenario();View mutateGeometry=new View(true,false);long mgd=down(mutateGeometry,'q');mutateGeometry.key('q').key.e.a.right+=.01f;up(mutateGeometry,'q',mgd,0,45,90);noSymbol(mutateGeometry,"same layout changed key bounds");
        scenario();View removedKey=new View(true,false);long rkd=down(removedKey,'q');((fb0.f0)removedKey.a).d.remove(removedKey.key('q').key);up(removedKey,'q',rkd,0,45,90);noSymbol(removedKey,"same layout removed starting key");
        scenario();View cancelDisabled=new View(true,false);cancelDisabled.onCancel=()->cancelDisabled.prefs.flick(false);long cdd=down(cancelDisabled,'q');check(up(cancelDisabled,'q',cdd,0,45,90),"post-cancel disabled UP consumed");check(cancelDisabled.key('q').actor.calls==0,"post-cancel option rechecked");
        scenario();View cancelReset=new View(true,false);cancelReset.onCancel=()->JapaneseGlide.reset();long csd=down(cancelReset,'q');check(up(cancelReset,'q',csd,0,45,90),"post-cancel reset UP consumed");check(cancelReset.key('q').actor.calls==0,"generation rechecked after cancel");
        scenario();View noActor=new View(true,false);((oc0.m)noActor.key('q').key.b).b.c.d.clear();long nad=down(noActor,'q');up(noActor,'q',nad,0,45,90);noSymbol(noActor,"popup-only key has no default character actor");
        scenario();View shifted=new View(true,false);shifted.key('q').wrapper.allowed=false;fc0.d1 shiftActor=new fc0.d1();shiftActor.b=shiftActor.c="!";ic0.e shiftWrapped=new ic0.e();shiftWrapped.a=shiftActor;hc0.o shiftRestriction=new hc0.o();shiftRestriction.action=shiftWrapped;((oc0.m)shifted.key('q').key.b).b.c.d.add(shiftRestriction);long shd=down(shifted,'q');check(up(shifted,'q',shd,0,45,90),"shifted restriction selects current actor");check(shiftActor.calls==1&&shifted.key('q').actor.calls==0,"only shifted actor executes");
        scenario();View nativePressed=new View(true,false);final boolean[] pressed={true};nativePressed.onCancel=()->pressed[0]=false;flick(nativePressed,'q');check(!pressed[0]&&nativePressed.key('q').actor.calls==1,"native cancel resets pressed state without losing alternate");
        scenario();View neighboring=new View(true,false);long nbd=down(neighboring,'a');check(dispatch(neighboring,event(2,100,105,nbd,30)),"upward MOVE captures before native can retarget key");captured(neighboring,"neighbor crossing");check(up(neighboring,'a',nbd,0,60,90),"UP above original key captures flick");check(neighboring.insertions.equals(Arrays.asList("@"))&&neighboring.key('q').actor.calls==0,"starting key chooses alternate, not release key");

        // Native float accumulation can put an otherwise valid right edge just above 1.
        scenario();View roundedRight=new View(true,false);roundedRight.key('p').key.e.a.right=1.0000001192092896f;
        flick(roundedRight,'p');check(roundedRight.key('p').actor.calls==1,"native float rounding at right edge does not reject rightmost key");
        scenario();View roundedLeft=new View(true,false);roundedLeft.key('q').key.e.a.left=-.0000001f;
        flick(roundedLeft,'q');check(roundedLeft.key('q').actor.calls==1,"tiny left-edge roundoff is accepted");
        for(float invalidRight:new float[]{1.001f,Float.NaN,Float.POSITIVE_INFINITY}){
            scenario();View badRect=new View(true,false);badRect.key('q').key.e.a.right=invalidRight;long d=clock;clock+=1000;
            direct(badRect,event(0,50,60,d,0));direct(badRect,event(1,50,15,d,80));noSymbol(badRect,"invalid right geometry "+invalidRight);
        }
        for(int side=0;side<3;side++){
            scenario();View badRect=new View(true,false);RectF r=badRect.key('q').key.e.a;
            if(side==0)r.left=Float.NEGATIVE_INFINITY;else if(side==1)r.top=Float.NaN;else r.bottom=Float.POSITIVE_INFINITY;
            long d=clock;clock+=1000;direct(badRect,event(0,50,60,d,0));direct(badRect,event(1,50,15,d,80));noSymbol(badRect,"nonfinite geometry side "+side);
        }
        scenario();View wideRect=new View(true,false);wideRect.key('q').key.e.a.left=-.001f;long wrd=clock;clock+=1000;
        direct(wideRect,event(0,50,60,wrd,0));direct(wideRect,event(1,50,15,wrd,80));noSymbol(wideRect,"gross negative geometry remains excluded");
        scenario();View emptyRect=new View(true,false);emptyRect.key('q').key.e.a.right=0;long emd=clock;clock+=1000;
        direct(emptyRect,event(0,50,60,emd,0));direct(emptyRect,event(1,50,15,emd,80));noSymbol(emptyRect,"zero-width geometry remains excluded");

        // Sensitivity regression: short, upward-biased movement remains distinct from tap jitter.
        for(boolean glide:new boolean[]{false,true})for(char c:new char[]{'q','a'}){
            scenario();View sensitive=new View(true,glide);long sd=down(sensitive,c);
            check(up(sensitive,c,sd,0,21,80),"short upward flick accepted with glide="+glide);
            check(sensitive.key(c).actor.calls==1&&sensitive.cancelCalls==1&&sensitive.tapUps==0,"short flick inserts once and cancels native UP");
            check(pendingGlide()==0,"short flick does not enqueue a glide");
        }
        for(boolean glide:new boolean[]{false,true}){
            scenario();View angled=new View(true,glide);long ad=down(angled,'q');
            check(up(angled,'q',ad,14,21,80),"slightly diagonal short flick accepted");
            check(angled.key('q').actor.calls==1&&angled.cancelCalls==1,"diagonal flick has one owner");
        }
        scenario();View widerAngle=new View(true,false);long wad=down(widerAngle,'a');
        check(up(widerAngle,'a',wad,35,50,90),"upward-biased stroke tolerates lateral drift");
        check(widerAngle.key('a').actor.calls==1,"lateral tolerance still selects starting key");
        scenario();View belowThreshold=new View(true,false);long btd=down(belowThreshold,'q');
        check(!up(belowThreshold,'q',btd,0,18,90),"movement below new threshold stays native");noSymbol(belowThreshold,"below sensitivity threshold");
        for(boolean glide:new boolean[]{false,true}){
            scenario();View jitter=new View(true,glide);long jd=down(jitter,'a');NativeKey jk=jitter.key('a');
            MotionEvent je=event(1,jk.x(jitter)+3,jk.y(jitter)-4,jd,70);
            je.history=new float[][]{{jk.x(jitter)-3,jk.y(jitter)+2},{jk.x(jitter)+2,jk.y(jitter)-3}};
            check(!dispatch(jitter,je),"small tap jitter stays native with glide="+glide);noSymbol(jitter,"tap jitter");
            check(jitter.tapUps==1,"jittery tap receives exactly one native UP");
        }
        scenario();View tooDiagonal=new View(true,false);long tdd=down(tooDiagonal,'q');
        check(up(tooDiagonal,'q',tdd,20,22,90),"ordinary diagonal now enters upward sector");oneSymbol(tooDiagonal,"ordinary diagonal");
        scenario();View tooWide=new View(true,false);long twd=down(tooWide,'q');
        check(up(tooWide,'q',twd,39,55,90),"upward diagonal beyond former fixed width now accepted");oneSymbol(tooWide,"former horizontal cutoff");
        scenario();View gentleRebound=new View(true,false);long grd=down(gentleRebound,'a');NativeKey grk=gentleRebound.key('a');
        check(dispatch(gentleRebound,event(2,grk.x(gentleRebound),grk.y(gentleRebound)-35,grd,40)),"upward movement captures without insertion");captured(gentleRebound,"rebound capture");
        check(up(gentleRebound,'a',grd,0,21,80),"small release rebound keeps previous size-based tolerance");
        check(gentleRebound.key('a').actor.calls==1,"release rebound inserts once");
        scenario();View excessiveRebound=new View(true,false);long erd=down(excessiveRebound,'a');NativeKey erk=excessiveRebound.key('a');
        dispatch(excessiveRebound,event(2,erk.x(excessiveRebound),erk.y(excessiveRebound)-40,erd,40));
        check(up(excessiveRebound,'a',erd,0,22,80),"reversal after capture no longer invalidates intent");oneSymbol(excessiveRebound,"large rebound");
        scenario();View tiny=new View(true,false);tiny.width=100;tiny.height=40;long tid=down(tiny,'q');
        check(up(tiny,'q',tid,0,7,70),"small layout uses reduced minimum floor");check(tiny.key('q').actor.calls==1,"small layout native actor once");
        scenario();View tinyJitter=new View(true,false);tinyJitter.width=100;tinyJitter.height=40;long tjd=down(tinyJitter,'q');
        check(!up(tinyJitter,'q',tjd,0,5,70),"minimum floor still rejects tiny-layout jitter");noSymbol(tinyJitter,"small layout jitter");
        scenario();View large=new View(true,false);large.width=2000;large.height=800;long lgd=down(large,'a');
        check(up(large,'a',lgd,0,42,70),"large layout threshold scales with key size");check(large.key('a').actor.calls==1,"large layout native actor once");
        scenario();View largeJitter=new View(true,false);largeJitter.width=2000;largeJitter.height=800;long ljd=down(largeJitter,'a');
        check(!up(largeJitter,'a',ljd,0,36,70),"large-layout subthreshold movement stays native");noSymbol(largeJitter,"large layout subthreshold");

        captureFlickTests();
        ownedFailureTests();
        captureDiagnosticsTests();
        scenario();View horizontal=new View(true,true);long horD=down(horizontal,'a');check(dispatch(horizontal,event(2,500,140,horD,50)),"horizontal movement captured by glide");check(dispatch(horizontal,event(1,800,140,horD,90)),"glide UP consumed");waitForCandidate();check(horizontal.insertions.isEmpty()&&horizontal.cancelCalls==1,"horizontal trace belongs only to glide");JapaneseGlideCandidates.select();check(horizontal.model.glide.size()==1,"horizontal glide native insertion retained");
        scenario();View turn=new View(true,true);long turnD=down(turn,'a');check(dispatch(turn,event(2,100,115,turnD,20)),"initial upward MOVE captures flick");captured(turn,"upward intent");check(dispatch(turn,event(2,500,100,turnD,50)),"sideways movement after capture remains owned by flick");dispatch(turn,event(1,800,100,turnD,90));oneSymbol(turn,"upward-first gesture priority over later horizontal glide");
        scenario();View line=new View(true,true);LineEnter.intercept=true;long lineD=downIgnored(line);dispatch(line,event(1,50,15,lineD,90));check(line.events.isEmpty()&&line.insertions.isEmpty(),"LineEnter retains precedence");LineEnter.intercept=false;
        scenario();View reset=new View(true,false);long resetD=down(reset,'q');JapaneseGlide.reset();up(reset,'q',resetD,0,45,90);noSymbol(reset,"service reset clears flick candidate");
        // Diagnostics expose event stages/counts, never input text, and copy only on explicit click.
        scenario();View diagnosticSuccess=new View(true,false);
        diagnosticSuccess.before="PRIVATE_BEFORE";diagnosticSuccess.after="PRIVATE_AFTER";
        diagnosticSuccess.model.state.composition="PRIVATE_COMPOSITION";diagnosticSuccess.model.state.raw="PRIVATE_RAW";
        Preference successDiagnostic=diagnostic(diagnosticSuccess.service);
        ClipboardManager successClipboard=(ClipboardManager)diagnosticSuccess.service.getSystemService(Context.CLIPBOARD_SERVICE);
        String beforeSuccess=successDiagnostic.summary;
        flick(diagnosticSuccess,'q');
        check(successClipboard.writes==0,"typing never overwrites clipboard");
        successDiagnostic.f.k(successDiagnostic);
        String successReport=successClipboard.primaryClip.text.toString();
        check(successReport.contains("1.8.12")&&successReport.contains("設定: ON"),"copied report identifies version and enabled option");
        check(metric(successReport,"DOWN")==metric(beforeSuccess,"DOWN")+1&&metric(successReport,"MOVE")==metric(beforeSuccess,"MOVE")+1&&metric(successReport,"UP")==metric(beforeSuccess,"UP")+1,"all received event kinds counted once");
        check(metric(successReport,"開始")==metric(beforeSuccess,"開始")+1&&metric(successReport,"成功")==metric(beforeSuccess,"成功")+1,"successful gesture increments start and success");
        check(successDiagnostic.summary.contains(successReport)&&successDiagnostic.summary.contains("コピーしました"),"click refreshes summary with exact copied report");
        check(diagnosticSuccess.prefs.writes==0,"typing and diagnostic copy never persist diagnostic data");privacy(successReport);
        FlickSymbols.reset();FlickSymbols.init(diagnosticSuccess.prefs);
        String retained=copyDiagnostic(diagnosticSuccess.service);
        check(metric(retained,"DOWN")==metric(successReport,"DOWN")&&metric(retained,"成功")==metric(successReport,"成功"),"reset and init retain collected counters");
        check(metric(retained,"初期化")>metric(successReport,"初期化")&&metric(retained,"リセット")>metric(successReport,"リセット"),"initialization and resets have separate counters");

        scenario();View diagnosticOff=new View(false,false);String beforeDisabled=diagnostic(diagnosticOff.service).summary;
        long doff=down(diagnosticOff,'q');up(diagnosticOff,'q',doff,0,45,80);String offReport=copyDiagnostic(diagnosticOff.service);
        check(metric(offReport,"DOWN")==metric(beforeDisabled,"DOWN")+1&&metric(offReport,"UP")==metric(beforeDisabled,"UP")+1,"disabled option still records hook receipt");
        check(metric(offReport,"開始")==metric(beforeDisabled,"開始")&&metric(offReport,"成功")==metric(beforeDisabled,"成功"),"disabled option cannot count start or insertion");
        check(offReport.contains("設定: OFF")&&offReport.contains("最後の拒否: DOWN_DISABLED"),"disabled state and hook refusal identified");

        scenario();View diagnosticLanguage=new View(true,false);((fb0.f0)diagnosticLanguage.a).keys.b=com.microsoft.fluency.ResultsFilter.PredictionSearchType.OTHER;
        long dlg=down(diagnosticLanguage,'q');up(diagnosticLanguage,'q',dlg,0,45,80);String layoutReport=copyDiagnostic(diagnosticLanguage.service);
        check(layoutReport.contains("最後の拒否: BEGIN_SEARCH_NOT_JAPANESE"),"begin layout refusal survives following orphan UP");
        long rejectedBeforeReset=metric(layoutReport,"拒否");FlickSymbols.reset();FlickSymbols.init(diagnosticLanguage.prefs);
        String afterLayoutReset=copyDiagnostic(diagnosticLanguage.service);
        check(afterLayoutReset.contains("最後の拒否: BEGIN_SEARCH_NOT_JAPANESE")&&metric(afterLayoutReset,"拒否")==rejectedBeforeReset,"settings resets do not erase diagnostic evidence");
        ((fb0.f0)diagnosticLanguage.a).keys.b=com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;flick(diagnosticLanguage,'q');String recovery=copyDiagnostic(diagnosticLanguage.service);
        check(recovery.contains("最後の段階: SUCCESS")&&recovery.contains("最後の拒否: BEGIN_SEARCH_NOT_JAPANESE"),"success stage and previous refusal can be distinguished");

        scenario();View diagnosticShort=new View(true,false);long ds=down(diagnosticShort,'q');up(diagnosticShort,'q',ds,0,2,80);
        check(copyDiagnostic(diagnosticShort.service).contains("最後の拒否: UP_TOO_SHORT"),"short gesture has useful distance refusal");
        scenario();View diagnosticTouchExploration=new View(true,false);diagnosticTouchExploration.A.popup=true;long dte=down(diagnosticTouchExploration,'q');up(diagnosticTouchExploration,'q',dte,0,45,80);
        check(copyDiagnostic(diagnosticTouchExploration.service).contains("最後の拒否: BEGIN_TOUCH_EXPLORATION"),"accessibility guard has correct meaning instead of popup label");
        scenario();View diagnosticPostCancel=new View(true,false);diagnosticPostCancel.onCancel=()->diagnosticPostCancel.service.i=new EditorInfo();long dpc=down(diagnosticPostCancel,'q');up(diagnosticPostCancel,'q',dpc,0,45,80);
        check(copyDiagnostic(diagnosticPostCancel.service).contains("最後の拒否: POST_CANCEL_EDITOR_CHANGED"),"post-cancel failure is distinguished from pre-cancel failure");
        check(diagnosticPostCancel.insertions.isEmpty()&&diagnosticPostCancel.tapUps==0,"post-cancel diagnostic leaves original guarded behavior intact");
        scenario();View diagnosticDuringEvent=new View(true,false);long dde=down(diagnosticDuringEvent,'q');diagnosticDuringEvent.service.i=new EditorInfo();up(diagnosticDuringEvent,'q',dde,0,45,80);
        check(copyDiagnostic(diagnosticDuringEvent.service).contains("最後の拒否: EVENT_EDITOR_CHANGED"),"pre-cancel event failure identifies the changed editor");

        scenario();View diagnosticFailure=new View(true,false);diagnosticFailure.onCancel=()->{throw new IllegalStateException("PRIVATE_NATIVE_FAILURE");};
        String beforeFailure=diagnostic(diagnosticFailure.service).summary;long df=down(diagnosticFailure,'q');
        check(up(diagnosticFailure,'q',df,0,45,80),"native cancel exception still consumes completed flick");
        String failureReport=copyDiagnostic(diagnosticFailure.service);
        check(failureReport.contains("java.lang.IllegalStateException"),"diagnostic records exception class");
        check(metric(failureReport,"成功")==metric(beforeFailure,"成功"),"failed native operation never counts success");
        privacy(failureReport);check(diagnosticFailure.insertions.isEmpty()&&diagnosticFailure.tapUps==0,"diagnostics do not alter cancel-exception ownership");

        scenario();Context noClipboard=new Context();noClipboard.clipboardService=null;Preference unavailable=diagnostic(noClipboard);unavailable.f.k(unavailable);
        check(unavailable.summary.contains("コピーできませんでした"),"unavailable clipboard refreshes diagnostic without crash");privacy(unavailable.summary);
        scenario();Context deniedClipboard=new Context();((ClipboardManager)deniedClipboard.clipboardService).fail=true;Preference denied=diagnostic(deniedClipboard);denied.f.k(denied);
        check(denied.summary.contains("コピーできませんでした")&&((ClipboardManager)deniedClipboard.clipboardService).writes==0,"clipboard exception does not escape or report success");privacy(denied.summary);

        scenario();View unreadablePrefs=new View(true,false);Preference unreadableDiagnostic=diagnostic(unreadablePrefs.service);unreadablePrefs.prefs.readFail=true;unreadableDiagnostic.f.k(unreadableDiagnostic);
        String unreadableReport=((ClipboardManager)unreadablePrefs.service.clipboardService).primaryClip.text.toString();
        check(unreadableReport.contains("設定: UNKNOWN"),"preference read failure still permits diagnostic report");privacy(unreadableReport);unreadablePrefs.prefs.readFail=false;

        scenario();Context failingSummary=new Context();Preference fragile=diagnostic(failingSummary);fragile.failSummary=true;fragile.f.k(fragile);
        ClipboardManager copiedBeforeSummaryFailure=(ClipboardManager)failingSummary.clipboardService;
        check(copiedBeforeSummaryFailure.writes==1,"summary update exception does not escape after clipboard succeeds");privacy(copiedBeforeSummaryFailure.primaryClip.text.toString());
        fragile.f.k(null);check(copiedBeforeSummaryFailure.writes==1,"null diagnostic click ignored");

        check(MotionEvent.copies==MotionEvent.recycles,"all copied cancel events recycled");
        System.out.println("PASS FlickSymbols: "+scenarios+" scenarios, "+assertions+" assertions");
        System.out.println("REPORT {\"status\":\"PASSED\",\"device_tested\":false,\"android_instrumentation_tested\":false,\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
    }
    static long downIgnored(View v){long d=clock;clock+=1000;dispatch(v,event(0,50,60,d,0));return d;}
}
