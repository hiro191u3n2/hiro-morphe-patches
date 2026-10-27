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
        public boolean f(lh0.a token,MotionEvent e){events.add(e.action);if(e.action==3){cancelCalls++;if(onCancel!=null)onCancel.run();if(cancelFail)throw new RuntimeException("native cancel");}if(e.action==1)tapUps++;return true;}
        NativeKey key(char c){return nativeKeys.get(c);}
    }
    static MotionEvent event(int action,float x,float y,long down,long delta){return new MotionEvent(action,x,y,down,down+delta);}
    static long down(View v,char c){NativeKey k=v.key(c);long d=clock;clock+=1000;check(!dispatch(v,event(0,k.x(v),k.y(v),d,0)),"DOWN passes to native");return d;}
    static boolean dispatch(View v,MotionEvent e){boolean consumed=JapaneseGlide.handle(v,TOKEN,e);if(!consumed)v.f(TOKEN,e);return consumed;}
    static boolean direct(View v,MotionEvent e){boolean consumed=FlickSymbols.handle(v,TOKEN,e,false);if(!consumed)v.f(TOKEN,e);return consumed;}
    static boolean up(View v,char c,long d,float dx,float upward,long delta){NativeKey k=v.key(c);return dispatch(v,event(1,k.x(v)+dx,k.y(v)-upward,d,delta));}
    static void flick(View v,char c){long d=down(v,c);NativeKey k=v.key(c);check(!dispatch(v,event(2,k.x(v),k.y(v)-20,d,30)),"candidate MOVE passes to native");check(up(v,c,d,0,45,90),"flick UP consumed");}
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
    static void touchSlopTests() throws Exception {
        // The default host slop is deliberately zero. Explicit configurations
        // below emulate density-scaled values; no Android device is emulated.
        for(boolean glide:new boolean[]{false,true})for(boolean history:new boolean[]{false,true}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,glide);long d=down(v,'q');NativeKey k=v.key('q');
            if(history) {
                MotionEvent release=event(1,k.x(v),k.y(v)-45,d,90);
                release.history=new float[][]{{k.x(v),k.y(v)+12},{k.x(v),k.y(v)-20}};
                check(dispatch(v,release),"historical contact jitter accepts later upward flick glide="+glide);
            } else {
                check(!dispatch(v,event(2,k.x(v),k.y(v)+12,d,20)),"contact jitter stays native until UP");
                check(FlickSymbols.pending(v)&&v.cancelCalls==0&&v.insertions.isEmpty(),"contact jitter preserves undecided ownership");
                check(up(v,'q',d,0,45,90),"contact jitter accepts later upward flick glide="+glide);
            }
            check(v.insertions.equals(Arrays.asList("1"))&&v.cancelCalls==1&&v.tapUps==0,"slop tolerance commits only one symbol on UP");
            check(pendingGlide()==0,"contact jitter does not queue glide");
        }
        for(boolean history:new boolean[]{false,true}) {
            scenario();View v=new View(true,false);long d=down(v,'q');NativeKey k=v.key('q');
            if(history) {
                MotionEvent release=event(1,k.x(v),k.y(v)-45,d,90);release.history=new float[][]{{k.x(v),k.y(v)+12}};dispatch(v,release);
            } else {dispatch(v,event(2,k.x(v),k.y(v)+12,d,20));up(v,'q',d,0,45,90);}
            noSymbol(v,"zero-slop configuration retains 189 contact-jitter limit");
            pathReason(v,"PATH_"+(history?"HISTORY":"CURRENT")+"_DOWN");
        }
        for(boolean glide:new boolean[]{false,true})for(boolean history:new boolean[]{false,true}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,glide);long d=down(v,'q');NativeKey k=v.key('q');
            if(history) {
                MotionEvent release=event(1,k.x(v),k.y(v)-21,d,90);release.history=new float[][]{{k.x(v),k.y(v)-45}};
                check(dispatch(v,release),"historical lift rebound at scaled slop accepted");
            } else {
                check(!dispatch(v,event(2,k.x(v),k.y(v)-45,d,40)),"peak never commits on MOVE");
                check(v.insertions.isEmpty()&&v.cancelCalls==0,"peak retains native MOVE ownership");
                check(up(v,'q',d,0,21,90),"lift rebound at scaled slop accepted");
            }
            check(v.insertions.size()==1&&v.cancelCalls==1&&v.tapUps==0&&pendingGlide()==0,"rebound still has one flick owner glide="+glide);
        }
        for(boolean history:new boolean[]{false,true}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,false);long d=down(v,'q');NativeKey k=v.key('q');
            if(history) {
                MotionEvent release=event(1,k.x(v),k.y(v)-20,d,90);release.history=new float[][]{{k.x(v),k.y(v)-45},{k.x(v),k.y(v)-20}};dispatch(v,release);
            } else {dispatch(v,event(2,k.x(v),k.y(v)-45,d,40));up(v,'q',d,0,20,90);}
            noSymbol(v,"rebound one pixel beyond slop remains rejected");pathReason(v,"PATH_"+(history?"HISTORY":"CURRENT")+"_REVERSE");
        }
        // Scaled configurations and key sizes advance together; the movement
        // above the old percentage limit is accepted at each modeled density.
        for(int scale:new int[]{1,2,4}) {
            scenario();ViewConfiguration.scaledTouchSlop=12*scale;View v=new View(true,false);v.width=1000*scale;v.height=400*scale;
            long d=down(v,'q');NativeKey k=v.key('q');
            check(!dispatch(v,event(2,k.x(v),k.y(v)+11*scale,d,20)),"scaled downward drift remains pending scale="+scale);
            check(FlickSymbols.pending(v),"scaled contact drift uses device slop floor");
            check(up(v,'q',d,0,45*scale,90),"scaled contact drift followed by flick accepted");
            check(v.insertions.size()==1&&v.cancelCalls==1,"scaled gesture commits once");
        }
        for(int beyond:new int[]{0,1}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,false);long d=down(v,'q');NativeKey k=v.key('q');
            dispatch(v,event(2,k.x(v),k.y(v)+24+beyond,d,20));
            check(FlickSymbols.pending(v)==(beyond==0),"downward slop boundary is inclusive");
            boolean consumed=up(v,'q',d,0,45,90);check(consumed==(beyond==0),"downward boundary determines eventual flick");
            if(beyond==0)check(v.insertions.size()==1,"boundary accepted one symbol");else {noSymbol(v,"excessive initial downward movement");pathReason(v,"PATH_CURRENT_DOWN");}
        }
        for(int beyond:new int[]{0,1}) {
            scenario();ViewConfiguration.scaledTouchSlop=1000;View v=new View(true,false);long d=down(v,'q');NativeKey k=v.key('q');
            dispatch(v,event(2,k.x(v),k.y(v)+40+beyond,d,20));
            check(FlickSymbols.pending(v)==(beyond==0),"OEM slop is capped at half shorter key side");
            check(up(v,'q',d,0,45,90)==(beyond==0),"capped slop preserves valid upward completion only");
            if(beyond==1) {String report=copyDiagnostic(v.service);check(pathMetric(report,"slop")==40f&&pathMetric(report,"down")==40f,"diagnostic shows effective capped slop");noSymbol(v,"overlarge OEM slop does not allow arbitrary motion");}
        }
        scenario();ViewConfiguration.scaledTouchSlop=-10;View negative=new View(true,false);long negD=down(negative,'q');NativeKey negK=negative.key('q');
        dispatch(negative,event(2,negK.x(negative),negK.y(negative)+12,negD,20));up(negative,'q',negD,0,45,90);noSymbol(negative,"negative platform slop retains old limits");
        check(pathMetric(copyDiagnostic(negative.service),"slop")==0f,"negative slop clamped to zero");
        for(int failure:new int[]{1,2})for(boolean getter:new boolean[]{false,true}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;if(getter)ViewConfiguration.valueFailure=failure;else ViewConfiguration.getFailure=failure;
            View v=new View(true,false);long d=down(v,'q');NativeKey k=v.key('q');dispatch(v,event(2,k.x(v),k.y(v)+12,d,20));up(v,'q',d,0,45,90);
            noSymbol(v,"unavailable platform configuration falls back without relaxing old limits");String report=copyDiagnostic(v.service);
            check(report.contains("最後の拒否: PATH_CURRENT_DOWN")&&pathMetric(report,"slop")==0f,"configuration exception fallback retains active gesture checks");privacy(report);
        }
        for(boolean glide:new boolean[]{false,true}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,glide);long d=down(v,'q');NativeKey k=v.key('q');
            MotionEvent release=event(1,k.x(v)+3,k.y(v)-4,d,90);release.history=new float[][]{{k.x(v),k.y(v)+12},{k.x(v)+2,k.y(v)-8}};
            check(!dispatch(v,release),"expanded contact slop never turns ordinary tap into flick");noSymbol(v,"ordinary tap with contact jitter");check(v.tapUps==1,"tap delivers one native UP with glide="+glide);
            scenario();ViewConfiguration.scaledTouchSlop=24;View back=new View(true,glide);long backD=down(back,'q');NativeKey backK=back.key('q');
            dispatch(back,event(2,backK.x(back),backK.y(back)-21,backD,30));check(!up(back,'q',backD,0,0,90),"return to origin remains a tap despite tolerated rebound");noSymbol(back,"up then return to origin");
            scenario();ViewConfiguration.scaledTouchSlop=24;View longUp=new View(true,glide);long longD=down(longUp,'q');
            check(!up(longUp,'q',longD,0,150,90),"long vertical stroke still yields with glide="+glide);noSymbol(longUp,"upper distance cap retained");pathReason(longUp,"PATH_CURRENT_UP_LIMIT");
        }
        // A nonzero slop must not change horizontal, upper-distance, or invalid
        // coordinate rejection. Both history and current samples expose cause.
        for(boolean history:new boolean[]{false,true})for(int kind=0;kind<5;kind++) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,false);long d=down(v,'q');NativeKey k=v.key('q');
            float x=k.x(v),y=k.y(v)-55;
            String reason;
            if(kind==0){x+=150;reason="HORIZONTAL";}
            else if(kind==1){y=k.y(v)+25;reason="DOWN";}
            else if(kind==2){y=k.y(v)-101;reason="UP_LIMIT";}
            else if(kind==3){x=Float.NaN;reason="NON_FINITE";}
            else {y=Float.POSITIVE_INFINITY;reason="NON_FINITE";}
            MotionEvent release=event(1,history?k.x(v):x,history?k.y(v)-45:y,d,90);
            if(history)release.history=new float[][]{{x,y}};
            check(!dispatch(v,release),"invalid path remains native "+reason);noSymbol(v,"granular path "+reason);
            pathReason(v,"PATH_"+(history?"HISTORY":"CURRENT")+"_"+reason);
        }
        for(int beyond:new int[]{0,1})for(boolean horizontal:new boolean[]{false,true}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,false);long d=down(v,'q');
            boolean consumed=up(v,'q',d,horizontal?86.5f+beyond:0,horizontal?50:100+beyond,90);
            check(consumed==(beyond==0),"angle and upper-distance limits distinguish the two boundary samples");
            if(beyond==0)check(v.insertions.size()==1,"unchanged boundary inserts once");else noSymbol(v,"unchanged bound rejects just outside");
        }
        scenario();ViewConfiguration.scaledTouchSlop=24;View measured=new View(true,false);measured.before="PRIVATE_BEFORE";measured.after="PRIVATE_AFTER";
        measured.model.state.composition="PRIVATE_COMPOSITION";measured.model.state.raw="PRIVATE_RAW";
        long measuredD=down(measured,'q');NativeKey measuredK=measured.key('q');dispatch(measured,event(2,measuredK.x(measured),measuredK.y(measured)-45,measuredD,30));up(measured,'q',measuredD,0,20,90);
        String measuredReport=copyDiagnostic(measured.service);String measurement=pathLine(measuredReport);
        check(pathMetric(measuredReport,"dx")==0f&&pathMetric(measuredReport,"up")==20f&&pathMetric(measuredReport,"reverse")==25f,"measurements use failed sample relative to starting point and peak");
        check(pathMetric(measuredReport,"down")==24f&&pathMetric(measuredReport,"reverseLimit")==24f&&pathMetric(measuredReport,"slop")==24f,"measurements expose effective jitter limits for same stroke");
        check(pathMetric(measuredReport,"directionDeadzone")==24f&&pathMetric(measuredReport,"liftLimit")==14.4f,"measurements expose directional and release limits for same stroke");
        check(measuredReport.contains("entryAngle=60deg exitAngle=65deg timeLimit=500ms"),"diagnostic exposes the new angular and timing policy");
        privacy(measuredReport);FlickSymbols.reset();FlickSymbols.init(measured.prefs);
        check(pathLine(copyDiagnostic(measured.service)).equals(measurement),"path evidence persists through settings reset and init");
        flick(measured,'q');String later=copyDiagnostic(measured.service);
        check(later.contains("最後の段階: SUCCESS")&&pathLine(later).equals(measurement),"later success does not mix old failure measurements with new sample");privacy(later);
        long laterTap=down(measured,'q');up(measured,'q',laterTap,0,2,90);String afterTap=copyDiagnostic(measured.service);
        check(afterTap.contains("最後の拒否: UP_TOO_SHORT")&&pathLine(afterTap).equals(measurement),"later nonpath rejection is distinct from retained last path refusal");
        down(measured,'q');FlickSymbols.reset();String afterActiveReset=copyDiagnostic(measured.service);
        check(afterActiveReset.contains("最後の拒否: RESET_ACTIVE")&&pathLine(afterActiveReset).equals(measurement),"active reset retains and labels historical path evidence separately");
        FlickSymbols.reset();check(pathLine(copyDiagnostic(measured.service)).equals(measurement),"inactive reset also retains historical path evidence");
    }


    static float horizontalAt(float degrees,float length) {return (float)(Math.sin(Math.toRadians(degrees))*length);}
    static float upwardAt(float degrees,float length) {return (float)(Math.cos(Math.toRadians(degrees))*length);}
    static void oneSymbol(View v,String reason) throws Exception {
        check(v.insertions.size()==1&&v.cancelCalls==1&&v.tapUps==0,reason+" commits one symbol and consumes native UP");
        check(pendingGlide()==0&&v.model.glide.isEmpty(),reason+" has no second glide owner");
    }
    static void moveAt(View v,char key,long down,float dx,float up,long elapsed) {
        NativeKey k=v.key(key);
        check(!dispatch(v,event(2,k.x(v)+dx,k.y(v)-up,down,elapsed)),"accepted MOVE remains native until release");
        check(v.insertions.isEmpty()&&v.cancelCalls==0,"MOVE cannot insert or cancel native input");
    }
    static void naturalFlickTests() throws Exception {
        // Human-readable angular examples exercise both the integrated glide
        // hook and production flick code. This is not Android/native emulation.
        for(boolean glide:new boolean[]{false,true})for(int side:new int[]{-1,1}) {
            scenario();View v=new View(true,glide);long d=down(v,'a');
            moveAt(v,'a',d,side*25,25,25);
            check(up(v,'a',d,side*50,50,90),"45-degree up flick past former fixed horizontal cutoff accepted");
            check(v.insertions.equals(Arrays.asList("@")),"diagonal selects the touched key, not release location");oneSymbol(v,"45-degree up flick glide="+glide);
            scenario();View quick=new View(true,glide);long qd=down(quick,'a');
            check(up(quick,'a',qd,side*14,14,55),"short 45-degree DOWN-UP flick uses total travel rather than vertical-only travel");oneSymbol(quick,"short radial diagonal");
            scenario();View tiny=new View(true,glide);long td=down(tiny,'a');
            check(!up(tiny,'a',td,side*13,13,55),"subthreshold radial diagonal stays a tap");noSymbol(tiny,"short diagonal tap");check(tiny.tapUps==1,"short diagonal tap still gets native UP");
        }
        for(int side:new int[]{-1,1})for(float degrees:new float[]{59.9f,60.1f,65f}) {
            scenario();View v=new View(true,false);long d=down(v,'a');
            boolean consumed=up(v,'a',d,side*horizontalAt(degrees,50),upwardAt(degrees,50),80);
            check(consumed==(degrees<60f),"direct "+degrees+"-degree gesture respects 60-degree entry sector");
            if(consumed)oneSymbol(v,"entry sector boundary");else noSymbol(v,"outside direct entry sector");
        }
        for(int side:new int[]{-1,1})for(float finish:new float[]{64.9f,65.1f}) {
            scenario();View v=new View(true,false);long d=down(v,'a');
            moveAt(v,'a',d,side*horizontalAt(50,40),upwardAt(50,40),30);
            NativeKey k=v.key('a');dispatch(v,event(2,k.x(v)+side*horizontalAt(finish,50),k.y(v)-upwardAt(finish,50),d,50));
            check(FlickSymbols.pending(v)==(finish<65f),"recognized upward direction has bounded 65-degree exit margin");
            boolean consumed=up(v,'a',d,side*horizontalAt(finish,50),upwardAt(finish,50),90);
            check(consumed==(finish<65f),"exit margin applies on UP only while gesture remains eligible");
            if(consumed)oneSymbol(v,"angular hysteresis");else noSymbol(v,"real sideways turn beyond exit sector");
        }
        for(boolean glide:new boolean[]{false,true}) {
            scenario();View v=new View(true,glide);long d=down(v,'a');moveAt(v,'a',d,10,0,20);
            check(FlickSymbols.pending(v),"small initial curve remains undecided inside deadzone");
            check(up(v,'a',d,35,35,85),"small initial sideways curve can finish as diagonal up");oneSymbol(v,"initial curve");
            scenario();ViewConfiguration.scaledTouchSlop=24;View scaled=new View(true,glide);long sd=down(scaled,'a');moveAt(scaled,'a',sd,23,0,20);
            check(FlickSymbols.pending(scaled),"device-scaled deadzone preserves short initial curve");
            check(up(scaled,'a',sd,35,35,85),"scaled initial curve can finish upward");oneSymbol(scaled,"scaled initial curve");
        }
        scenario();ViewConfiguration.scaledTouchSlop=24;View sideways=new View(true,false);long sidewaysD=down(sideways,'a');
        moveAt(sideways,'a',sidewaysD,25,0,20);check(!FlickSymbols.pending(sideways),"clear side motion outside deadzone yields permanently");
        check(!up(sideways,'a',sidewaysD,35,35,85),"later upward endpoint does not recover rejected side gesture");noSymbol(sideways,"sideways then upward");

        for(boolean glide:new boolean[]{false,true})for(boolean history:new boolean[]{false,true}) {
            scenario();View v=new View(true,glide);long d=down(v,'q');NativeKey k=v.key('q');
            if(history) {
                MotionEvent release=event(1,k.x(v),k.y(v)-12,d,80);release.history=new float[][]{{k.x(v),k.y(v)-21}};
                check(dispatch(v,release),"eligible historical MOVE supports small UP rebound");
            } else {moveAt(v,'q',d,0,21,40);check(up(v,'q',d,0,12,80),"small UP rebound does not erase immediately preceding eligible MOVE");}
            oneSymbol(v,"bounded release-distance correction glide="+glide);
            scenario();View angle=new View(true,glide);long ad=down(angle,'a');NativeKey ak=angle.key('a');
            float priorX=horizontalAt(59,22),priorUp=upwardAt(59,22);
            if(history) {MotionEvent release=event(1,ak.x(angle)+16,ak.y(angle)-8,ad,80);release.history=new float[][]{{ak.x(angle)+priorX,ak.y(angle)-priorUp}};check(dispatch(angle,release),"eligible historical point supports small UP angle bounce");}
            else {moveAt(angle,'a',ad,priorX,priorUp,40);check(up(angle,'a',ad,16,8,80),"small UP angle bounce preserves preceding eligible direction");}
            oneSymbol(angle,"bounded release-angle correction");
        }
        scenario();View hardSide=new View(true,false);long hardSideD=down(hardSide,'a');
        moveAt(hardSide,'a',hardSideD,horizontalAt(59,30),upwardAt(59,30),40);
        check(!up(hardSide,'a',hardSideD,31,13,80),"small UP movement cannot cross the hard 65-degree sideways boundary");noSymbol(hardSide,"release fallback preserves horizontal-glide boundary");
        for(boolean glide:new boolean[]{false,true})for(int duration:new int[]{351,450,500}) {
            scenario();View slow=new View(true,glide);long slowD=down(slow,'a');
            moveAt(slow,'a',slowD,20,20,200);
            check(up(slow,'a',slowD,40,40,duration),"natural slower flick remains eligible through "+duration+"ms");oneSymbol(slow,"500ms flick window");
        }
        scenario();View slowPopup=new View(true,false);long slowPopupD=down(slowPopup,'a');moveAt(slowPopup,'a',slowPopupD,20,20,200);slowPopup.I.count=1;
        check(!up(slowPopup,'a',slowPopupD,40,40,450),"long-press popup takes precedence within extended flick window");noSymbol(slowPopup,"slow flick never overrides native popup");
        scenario();View slowTap=new View(true,false);long slowTapD=down(slowTap,'a');
        check(!up(slowTap,'a',slowTapD,1,2,450),"long stationary press is never promoted into a flick");noSymbol(slowTap,"extended window does not create symbol from long press");check(slowTap.tapUps==1,"stationary long press receives native UP");
        scenario();View noMove=new View(true,false);long noMoveD=down(noMove,'a');
        check(!up(noMove,'a',noMoveD,16,8,80),"same endpoint cannot borrow a direction without prior eligible movement");noSymbol(noMove,"no stale release fallback");
        for(int rebound:new int[]{14,15}) {
            scenario();View v=new View(true,false);long d=down(v,'q');moveAt(v,'q',d,0,30,35);
            boolean consumed=up(v,'q',d,0,30-rebound,80);check(consumed==(rebound==14),"release fallback is bounded to less than 15px for this key");
            if(consumed)oneSymbol(v,"bounded small rebound");else noSymbol(v,"excessive release-only fallback distance");
        }
        // A late release sample may cross the peak-based reverse bound even
        // though it is only a few pixels from the immediately preceding valid
        // point. This exception is UP-only, not general peak retention.
        for(boolean glide:new boolean[]{false,true}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,glide);long d=down(v,'q');
            moveAt(v,'q',d,0,50,25);moveAt(v,'q',d,0,30,50);
            check(up(v,'q',d,0,25,80),"small final UP step may cross peak-based reverse limit");oneSymbol(v,"UP-only terminal reverse correction");
        }
        for(boolean history:new boolean[]{false,true}) {
            scenario();ViewConfiguration.scaledTouchSlop=24;View v=new View(true,false);long d=down(v,'q');NativeKey k=v.key('q');
            if(history) {MotionEvent release=event(1,k.x(v),k.y(v)-25,d,80);release.history=new float[][]{{k.x(v),k.y(v)-50},{k.x(v),k.y(v)-30},{k.x(v),k.y(v)-25}};check(!dispatch(v,release),"historical reversal cannot use terminal UP correction");}
            else {moveAt(v,'q',d,0,50,25);moveAt(v,'q',d,0,30,50);dispatch(v,event(2,k.x(v),k.y(v)-25,d,65));check(!up(v,'q',d,0,25,80),"MOVE reversal cannot use terminal UP correction");}
            noSymbol(v,"terminal reverse exception never repairs non-UP path");pathReason(v,"PATH_"+(history?"HISTORY":"CURRENT")+"_REVERSE");
        }
        scenario();View nearOrigin=new View(true,false);long nearD=down(nearOrigin,'q');moveAt(nearOrigin,'q',nearD,0,20,35);
        check(!up(nearOrigin,'q',nearD,0,8,80),"return close to origin does not become a symbol even within release slop");noSymbol(nearOrigin,"near-origin return");
        scenario();ViewConfiguration.scaledTouchSlop=24;View stalePeak=new View(true,false);long peakD=down(stalePeak,'q');
        moveAt(stalePeak,'q',peakD,0,30,30);moveAt(stalePeak,'q',peakD,0,10,50);
        check(!up(stalePeak,'q',peakD,0,12,80),"UP fallback cannot use old peak after last MOVE was no longer eligible");noSymbol(stalePeak,"old peak not latched");
        scenario();View hardLimit=new View(true,false);long limitD=down(hardLimit,'q');moveAt(hardLimit,'q',limitD,0,99,35);
        check(!up(hardLimit,'q',limitD,0,101,80),"tiny UP bounce cannot bypass long-stroke limit");noSymbol(hardLimit,"release fallback keeps upper-distance guard");
        for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY}) {
            scenario();View v=new View(true,false);long d=down(v,'q');NativeKey k=v.key('q');moveAt(v,'q',d,0,21,35);
            check(!dispatch(v,event(1,invalid,k.y(v)-12,d,80)),"release fallback never accepts nonfinite coordinates");noSymbol(v,"nonfinite release fallback");
        }
        scenario();View upThenDown=new View(true,false);long upDownD=down(upThenDown,'a');
        moveAt(upThenDown,'a',upDownD,horizontalAt(59,20),upwardAt(59,20),30);
        check(!up(upThenDown,'a',upDownD,horizontalAt(59,20),-1,80),"release correction never accepts a below-origin endpoint");noSymbol(upThenDown,"negative upward release");
        scenario();View rejected=new View(true,false);long rejectD=down(rejected,'q');NativeKey rejectK=rejected.key('q');
        moveAt(rejected,'q',rejectD,0,21,30);dispatch(rejected,event(2,rejectK.x(rejected)+80,rejectK.y(rejected)-21,rejectD,50));
        check(!up(rejected,'q',rejectD,0,21,80),"UP near former valid point cannot revive a rejected MOVE path");noSymbol(rejected,"no fallback after real turn");
        scenario();View rejectedHistory=new View(true,false);long historyD=down(rejectedHistory,'q');NativeKey hk=rejectedHistory.key('q');
        MotionEvent historyRelease=event(1,hk.x(rejectedHistory),hk.y(rejectedHistory)-21,historyD,80);
        historyRelease.history=new float[][]{{hk.x(rejectedHistory),hk.y(rejectedHistory)-21},{hk.x(rejectedHistory)+80,hk.y(rejectedHistory)-21}};
        check(!dispatch(rejectedHistory,historyRelease),"rejected history is not bypassed by eligible final point");noSymbol(rejectedHistory,"no fallback after rejected history");
        scenario();View changedEditor=new View(true,false);long changedD=down(changedEditor,'q');moveAt(changedEditor,'q',changedD,0,21,35);changedEditor.service.i=new EditorInfo();
        check(!up(changedEditor,'q',changedD,0,12,80),"release correction never bypasses changed editor");noSymbol(changedEditor,"release correction editor guard");
        scenario();View popup=new View(true,false);long popupD=down(popup,'q');moveAt(popup,'q',popupD,0,21,35);popup.I.count=1;
        check(!up(popup,'q',popupD,0,12,80),"release correction never bypasses native popup");noSymbol(popup,"release correction popup guard");
        scenario();View noToken=new View(true,false);long tokenD=down(noToken,'q');NativeKey tk=noToken.key('q');moveAt(noToken,'q',tokenD,0,21,35);
        check(!FlickSymbols.handle(noToken,null,event(1,tk.x(noToken),tk.y(noToken)-12,tokenD,80),false),"release correction requires native token");noSymbol(noToken,"release correction token guard");

        scenario();View flat=new View(true,true);long flatD=down(flat,'a');NativeKey fk=flat.key('a');
        check(dispatch(flat,event(2,fk.x(flat)+350,fk.y(flat)-30,flatD,45)),"mostly horizontal trajectory still transfers to glide");
        check(dispatch(flat,event(1,fk.x(flat)+650,fk.y(flat)-50,flatD,90)),"flat glide UP belongs to glide");waitForCandidate();
        check(flat.insertions.isEmpty()&&flat.cancelCalls==1,"flat trace has one glide owner and no symbol");JapaneseGlideCandidates.select();check(flat.model.glide.size()==1,"flat glide candidate still commits natively");
    }

    public static void main(String[] args)throws Exception{
        scenario();Prefs p=new Prefs();FlickSymbols.init(p);PreferenceScreen screen=new PreferenceScreen(new Context());FlickSymbols.install(screen);FlickSymbols.install(screen);check(screen.children.size()==2,"toggle and diagnostic setting after repeated installation");SwitchPreferenceCompat option=(SwitchPreferenceCompat)screen.H(FlickSymbols.KEY);check(!option.checked&&!option.s,"default off and explicit store");check(option.title.equals("上フリックで長押し文字を入力"),"setting title");
        Preference initialDiagnostic=screen.H(FlickSymbols.DIAGNOSTIC_KEY);
        ClipboardManager initialClipboard=(ClipboardManager)screen.a.getSystemService(Context.CLIPBOARD_SERVICE);
        check(initialDiagnostic!=null&&!initialDiagnostic.s&&initialDiagnostic.title.equals("上フリック診断をコピー"),"diagnostic setting is nonpersistent");
        check(initialDiagnostic.summary.contains("1.8.11")&&initialDiagnostic.summary.contains("EVENT_NOT_RECEIVED"),"diagnostic version and no events are visible before input");
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
        for(boolean glide:new boolean[]{false,true})for(char c:new char[]{'q','a'}){scenario();View v=new View(true,glide);flick(v,c);check(v.insertions.equals(Arrays.asList(c=='q'?"1":"@")),"dynamic native symbol "+c+" glide="+glide);check(v.key(c).actor.calls==1&&v.key(c).actor.lastToken==TOKEN,"native actor exactly once with token");check(v.events.equals(Arrays.asList(0,2,3))&&v.cancelCalls==1&&v.tapUps==0,"native DOWN/MOVE/CANCEL, no base letter UP");check(!FlickSymbols.pending(v)&&pendingGlide()==0,"flick clears both pending tracks");check(!up(v,c,clock,0,45,90),"duplicate orphan UP not captured");check(v.key(c).actor.calls==1,"duplicate UP no duplicate symbol");}

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
        scenario();View history=new View(true,false);long hd=down(history,'q');MotionEvent h=event(1,50,15,hd,90);h.history=new float[][]{{90,40}};dispatch(history,h);noSymbol(history,"historical sideways excursion");
        scenario();View downward=new View(true,false);long downd=down(downward,'q');dispatch(downward,event(2,50,85,downd,30));up(downward,'q',downd,0,45,90);noSymbol(downward,"down then up");
        scenario();View reverse=new View(true,false);long revd=down(reverse,'a');dispatch(reverse,event(2,100,80,revd,30));up(reverse,'a',revd,0,35,90);noSymbol(reverse,"reverse path");
        scenario();View longStroke=new View(true,false);long lsd=down(longStroke,'a');up(longStroke,'a',lsd,0,150,90);noSymbol(longStroke,"long stroke yields");
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
        scenario();View neighboring=new View(true,false);long nbd=down(neighboring,'a');check(!dispatch(neighboring,event(2,100,105,nbd,30)),"native MOVE into key above remains undecided");check(up(neighboring,'a',nbd,0,60,90),"UP above original key captures flick");check(neighboring.insertions.equals(Arrays.asList("@"))&&neighboring.key('q').actor.calls==0,"starting key chooses alternate, not release key");

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
        check(!dispatch(gentleRebound,event(2,grk.x(gentleRebound),grk.y(gentleRebound)-35,grd,40)),"upward movement waits for UP");
        check(up(gentleRebound,'a',grd,0,21,80),"small release rebound keeps previous size-based tolerance");
        check(gentleRebound.key('a').actor.calls==1,"release rebound inserts once");
        scenario();View excessiveRebound=new View(true,false);long erd=down(excessiveRebound,'a');NativeKey erk=excessiveRebound.key('a');
        dispatch(excessiveRebound,event(2,erk.x(excessiveRebound),erk.y(excessiveRebound)-40,erd,40));
        check(!up(excessiveRebound,'a',erd,0,22,80),"reversal beyond previous tolerance is rejected");noSymbol(excessiveRebound,"excessive rebound");
        scenario();View tiny=new View(true,false);tiny.width=100;tiny.height=40;long tid=down(tiny,'q');
        check(up(tiny,'q',tid,0,7,70),"small layout uses reduced minimum floor");check(tiny.key('q').actor.calls==1,"small layout native actor once");
        scenario();View tinyJitter=new View(true,false);tinyJitter.width=100;tinyJitter.height=40;long tjd=down(tinyJitter,'q');
        check(!up(tinyJitter,'q',tjd,0,5,70),"minimum floor still rejects tiny-layout jitter");noSymbol(tinyJitter,"small layout jitter");
        scenario();View large=new View(true,false);large.width=2000;large.height=800;long lgd=down(large,'a');
        check(up(large,'a',lgd,0,42,70),"large layout threshold scales with key size");check(large.key('a').actor.calls==1,"large layout native actor once");
        scenario();View largeJitter=new View(true,false);largeJitter.width=2000;largeJitter.height=800;long ljd=down(largeJitter,'a');
        check(!up(largeJitter,'a',ljd,0,36,70),"large-layout subthreshold movement stays native");noSymbol(largeJitter,"large layout subthreshold");

        touchSlopTests();
        naturalFlickTests();
        scenario();View horizontal=new View(true,true);long horD=down(horizontal,'a');check(dispatch(horizontal,event(2,500,140,horD,50)),"horizontal movement captured by glide");check(dispatch(horizontal,event(1,800,140,horD,90)),"glide UP consumed");waitForCandidate();check(horizontal.insertions.isEmpty()&&horizontal.cancelCalls==1,"horizontal trace belongs only to glide");JapaneseGlideCandidates.select();check(horizontal.model.glide.size()==1,"horizontal glide native insertion retained");
        scenario();View turn=new View(true,true);long turnD=down(turn,'a');check(!dispatch(turn,event(2,100,115,turnD,20)),"initial upward MOVE defers glide capture");check(FlickSymbols.pending(turn),"upward candidate pending");check(dispatch(turn,event(2,500,100,turnD,50)),"turning sideways yields full trace to glide");dispatch(turn,event(1,800,100,turnD,90));waitForCandidate();check(turn.insertions.isEmpty()&&turn.cancelCalls==1,"turn only one glide cancel");JapaneseGlideCandidates.close();
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
        check(successReport.contains("1.8.11")&&successReport.contains("設定: ON"),"copied report identifies version and enabled option");
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
