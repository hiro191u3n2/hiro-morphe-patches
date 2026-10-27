import hiro.swiftkey.*;
import android.content.*;
import android.graphics.RectF;
import android.view.MotionEvent;
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
    static void scenario(){scenarios++;FlickSymbols.reset();JapaneseGlide.reset();LineEnter.intercept=false;JapaneseGlideDecoder.ready=true;JapaneseGlideDecoder.blockDecode=false;JapaneseGlideDecoder.blockLoad=false;JapaneseGlideDecoder.calls=0;JapaneseGlideDecoder.empty=false;JapaneseGlideDecoder.invalid=false;JapaneseGlideCandidates.dismiss();Handler.drain();}
    static final class Prefs implements SharedPreferences {
        final Map<String,Boolean> values=new HashMap<String,Boolean>();int writes;boolean fail;
        public boolean getBoolean(String k,boolean def){return values.containsKey(k)?values.get(k):def;}
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

    public static void main(String[] args)throws Exception{
        scenario();Prefs p=new Prefs();FlickSymbols.init(p);PreferenceScreen screen=new PreferenceScreen(new Context());FlickSymbols.install(screen);FlickSymbols.install(screen);check(screen.children.size()==1,"one setting after repeated installation");SwitchPreferenceCompat option=(SwitchPreferenceCompat)screen.H(FlickSymbols.KEY);check(!option.checked&&!option.s,"default off and explicit store");check(option.title.equals("上フリックで長押し文字を入力"),"setting title");check(option.e.i(option,Boolean.TRUE)&&p.getBoolean(FlickSymbols.KEY,false)&&p.writes==1,"ON persisted");check(option.e.i(option,Boolean.FALSE)&&!p.getBoolean(FlickSymbols.KEY,true),"OFF persisted");check(!option.e.i(option,"yes"),"invalid option rejected");p.fail=true;check(!option.e.i(option,Boolean.TRUE),"store failure rejected");FlickSymbols.install(null);

        scenario();View off=new View(false,false);long od=down(off,'q');check(!up(off,'q',od,0,45,90),"default OFF leaves UP native");noSymbol(off,"disabled");check(off.events.equals(Arrays.asList(0,1)),"disabled native sequence");
        for(boolean glide:new boolean[]{false,true})for(char c:new char[]{'q','a'}){scenario();View v=new View(true,glide);flick(v,c);check(v.insertions.equals(Arrays.asList(c=='q'?"1":"@")),"dynamic native symbol "+c+" glide="+glide);check(v.key(c).actor.calls==1&&v.key(c).actor.lastToken==TOKEN,"native actor exactly once with token");check(v.events.equals(Arrays.asList(0,2,3))&&v.cancelCalls==1&&v.tapUps==0,"native DOWN/MOVE/CANCEL, no base letter UP");check(!FlickSymbols.pending(v)&&pendingGlide()==0,"flick clears both pending tracks");check(!up(v,c,clock,0,45,90),"duplicate orphan UP not captured");check(v.key(c).actor.calls==1,"duplicate UP no duplicate symbol");}

        scenario();View dynamic=new View(true,false);dynamic.key('q').actor.b=dynamic.key('q').actor.c="€";flick(dynamic,'q');check(dynamic.insertions.equals(Arrays.asList("€")),"native alternate replaces hardcoded layout assumption");
        scenario();View unicode=new View(true,false);unicode.key('q').actor.b=unicode.key('q').actor.c="😀";flick(unicode,'q');check(unicode.insertions.equals(Arrays.asList("😀")),"one Unicode code point supported");
        scenario();View tap=new View(true,false);long td=down(tap,'a');check(!up(tap,'a',td,1,2,60),"tap native");noSymbol(tap,"tap");check(tap.tapUps==1,"tap gets one native UP");
        scenario();View shortMove=new View(true,false);long smd=down(shortMove,'a');check(!up(shortMove,'a',smd,0,15,50),"short movement not flick");noSymbol(shortMove,"short movement");
        scenario();View popup=new View(true,false);long pd=down(popup,'q');popup.A.popup=true;check(!up(popup,'q',pd,0,45,90),"longpress popup native");noSymbol(popup,"popup");
        scenario();View popupArray=new View(true,false);long pad=down(popupArray,'q');popupArray.I.count=1;up(popupArray,'q',pad,0,45,90);noSymbol(popupArray,"secondary popup state");
        scenario();View timeout=new View(true,false);long ltd=down(timeout,'a');up(timeout,'a',ltd,0,45,351);noSymbol(timeout,"long press timeout");
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

        scenario();View horizontal=new View(true,true);long horD=down(horizontal,'a');check(dispatch(horizontal,event(2,500,140,horD,50)),"horizontal movement captured by glide");check(dispatch(horizontal,event(1,800,140,horD,90)),"glide UP consumed");waitForCandidate();check(horizontal.insertions.isEmpty()&&horizontal.cancelCalls==1,"horizontal trace belongs only to glide");JapaneseGlideCandidates.select();check(horizontal.model.glide.size()==1,"horizontal glide native insertion retained");
        scenario();View turn=new View(true,true);long turnD=down(turn,'a');check(!dispatch(turn,event(2,100,115,turnD,20)),"initial upward MOVE defers glide capture");check(FlickSymbols.pending(turn),"upward candidate pending");check(dispatch(turn,event(2,500,100,turnD,50)),"turning sideways yields full trace to glide");dispatch(turn,event(1,800,100,turnD,90));waitForCandidate();check(turn.insertions.isEmpty()&&turn.cancelCalls==1,"turn only one glide cancel");JapaneseGlideCandidates.close();
        scenario();View line=new View(true,true);LineEnter.intercept=true;long lineD=downIgnored(line);dispatch(line,event(1,50,15,lineD,90));check(line.events.isEmpty()&&line.insertions.isEmpty(),"LineEnter retains precedence");LineEnter.intercept=false;
        scenario();View reset=new View(true,false);long resetD=down(reset,'q');JapaneseGlide.reset();up(reset,'q',resetD,0,45,90);noSymbol(reset,"service reset clears flick candidate");
        check(MotionEvent.copies==MotionEvent.recycles,"all copied cancel events recycled");
        System.out.println("PASS FlickSymbols: "+scenarios+" scenarios, "+assertions+" assertions");
        System.out.println("REPORT {\"status\":\"PASSED\",\"device_tested\":false,\"android_instrumentation_tested\":false,\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
    }
    static long downIgnored(View v){long d=clock;clock+=1000;dispatch(v,event(0,50,60,d,0));return d;}
}
