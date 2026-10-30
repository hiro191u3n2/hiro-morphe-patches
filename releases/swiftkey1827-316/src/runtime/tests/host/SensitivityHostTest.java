import hiro.swiftkey.FlickSymbols;
import android.app.AlertDialog;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import java.util.Arrays;

/** Production sensitivity behavior; Android windows and native actors are explicit host fakes. */
public final class SensitivityHostTest extends FlickSymbolsHostTest {
    static final String SKEY="hiro_up_flick_sensitivity";
    static final String[] LABELS={"とても低い","低い","標準","高い","とても高い"};
    static View configured(int level,boolean glide) {
        scenario();AlertDialog.reset();View view=new View(true,glide);
        view.prefs.intValues.put(SKEY,level);return view;
    }
    static Preference setting(View view) {
        PreferenceScreen screen=new PreferenceScreen(view.service);
        FlickSymbols.install(screen);return screen.H(SKEY);
    }
    static AlertDialog open(Preference option) {
        AlertDialog.lastShown=null;option.f.k(option);
        check(AlertDialog.lastShown!=null,"sensitivity click opens standard single-choice dialog");
        return AlertDialog.lastShown;
    }
    static void select(View view,int level) {open(setting(view)).select(level);}
    static boolean gesture(View v,float dx,float up,long elapsed) {
        long d=down(v,'a');return up(v,'a',d,dx,up,elapsed);
    }
    static void assertOne(View v,String message) throws Exception {
        oneSymbol(v,message);check(v.events.equals(Arrays.asList(0,3)),message+" native sequence remains DOWN/CANCEL");
    }

    static void settingsTests() throws Exception {
        scenario();AlertDialog.reset();Prefs prefs=new Prefs();FlickSymbols.init(prefs);
        PreferenceScreen screen=new PreferenceScreen(new Context());
        FlickSymbols.install(screen);FlickSymbols.install(screen);
        check(screen.children.size()==2,"settings installation is idempotent");
        check(screen.children.keySet().equals(new java.util.LinkedHashSet<String>(Arrays.asList(FlickSymbols.KEY,SKEY))),"only toggle and sensitivity settings are installed");
        check(Arrays.asList(screen.children.keySet().toArray()).equals(Arrays.asList(FlickSymbols.KEY,SKEY)),"sensitivity follows existing on/off setting");
        Preference initial=screen.H(SKEY);
        check("上フリックの感度".equals(initial.title)&&!initial.s,"title is localized and storage is explicit");
        check(initial.summary.contains("現在：標準")&&initial.summary.contains("低くすると"),"default summary explains level direction");
        AlertDialog dialog=open(initial);
        check(dialog.checkedIndex==2&&Arrays.equals(dialog.items,LABELS),"dialog has five ordered choices and standard default");
        check("上フリックの感度".contentEquals(dialog.title)&&"キャンセル".contentEquals(dialog.negativeTitle),"dialog title and cancel button are Japanese");
        check(prefs.writes==0,"opening settings never persists a default or input content");
        dialog.cancel();check(prefs.writes==0,"cancel leaves preferences untouched");

        for(int level=0;level<5;level++) {
            View v=configured(2,false);Preference option=setting(v);AlertDialog d=open(option);d.select(level);
            check(v.prefs.getInt(SKEY,-1)==level&&v.prefs.writes==1,"choice is stored once, level="+level);
            check(option.summary.contains("現在："+LABELS[level])&&d.dismissals==1,"successful choice updates summary and closes dialog");
            check(v.prefs.getBoolean(FlickSymbols.KEY,false),"sensitivity selection does not toggle feature");
            FlickSymbols.init(v.prefs);Preference reopened=setting(v);
            check(reopened.summary.contains("現在："+LABELS[level]),"new screen after reinitialization restores selected label");
            check(open(reopened).checkedIndex==level&&v.prefs.writes==1,"reopened dialog preselects saved level without another write");
        }
        for(int invalid:new int[]{-1,5,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
            View v=configured(1,false);Preference option=setting(v);String summary=option.summary;
            open(option).select(invalid);
            check(v.prefs.getInt(SKEY,-1)==1&&v.prefs.writes==0&&summary.equals(option.summary),"out-of-range callback ignored");
        }
        for(int failure=0;failure<2;failure++) {
            View v=configured(2,false);Preference option=setting(v);String summary=option.summary;
            if(failure==0)v.prefs.fail=true;else v.prefs.applyFail=true;
            open(option).select(0);
            check(v.prefs.getInt(SKEY,-1)==2&&v.prefs.writes==0&&summary.equals(option.summary),"failed persistence does not announce a new value");
        }
        View window=configured(2,false);Preference windowSetting=setting(window);AlertDialog.failShow=true;windowSetting.f.k(windowSetting);
        check(window.prefs.writes==0&&AlertDialog.lastShown==null,"window failure is contained without changing selection");AlertDialog.reset();

        View stale=configured(2,false);AlertDialog staleDialog=open(setting(stale));Prefs replacement=new Prefs();FlickSymbols.init(replacement);staleDialog.select(0);
        check(stale.prefs.writes==0&&replacement.writes==0,"stale dialog cannot write to another preference instance");
        FlickSymbols.init(null);staleDialog.select(1);PreferenceScreen unavailable=new PreferenceScreen(new Context());FlickSymbols.install(unavailable);
        check(unavailable.children.isEmpty()&&stale.prefs.writes==0,"missing preferences leave settings unmodified");

        View off=configured(2,false);off.prefs.flick(false);select(off,0);
        check(!off.prefs.getBoolean(FlickSymbols.KEY,true),"choosing lower sensitivity leaves OFF disabled");
        check(!gesture(off,0,60,80)&&off.insertions.isEmpty(),"sensitivity never enables disabled flicks");
        off.prefs.flick(true);check(!gesture(off,0,22,80),"saved low sensitivity applies when toggle is later enabled");

        View refresh=configured(2,false);PreferenceScreen savedScreen=new PreferenceScreen(refresh.service);FlickSymbols.install(savedScreen);
        refresh.prefs.intValues.put(SKEY,0);FlickSymbols.install(savedScreen);
        check(savedScreen.children.size()==2&&savedScreen.H(SKEY).summary.contains("現在：とても低い"),"reinstall refreshes existing summary from saved preference");
        View summaryFailure=configured(2,false);Preference failing=setting(summaryFailure);failing.failSummary=true;open(failing).select(0);
        check(summaryFailure.prefs.getInt(SKEY,-1)==0,"a window summary failure cannot roll back persisted sensitivity");
        check(setting(summaryFailure).summary.contains("現在：とても低い"),"subsequent screen recovers the saved summary");
    }

    static void fallbackTests() throws Exception {
        Object[] corrupt={null,-1,5,Integer.MIN_VALUE,Integer.MAX_VALUE,"0",Boolean.FALSE,Long.valueOf(0)};
        for(Object value:corrupt) {
            View v=configured(0,false);if(value==null)v.prefs.intValues.remove(SKEY);else v.prefs.intValues.put(SKEY,value);
            Preference option=setting(v);check(option.summary.contains("現在：標準")&&open(option).checkedIndex==2,"invalid or missing setting displays standard: "+value);
            check(!gesture(v,0,18,70),"invalid setting retains standard subthreshold behavior");
            check(gesture(v,0,21,70),"invalid setting retains standard accepted movement");
            check(v.insertions.equals(Arrays.asList("@"))&&v.prefs.writes==0,"fallback does not rewrite storage or duplicate insertion");
        }
        View unreadable=configured(0,false);unreadable.prefs.readIntFail=true;
        check(open(setting(unreadable)).checkedIndex==2,"failed integer read uses standard");
        check(!gesture(unreadable,0,18,70)&&gesture(unreadable,0,21,70),"unreadable sensitivity does not disable otherwise enabled input");
    }

    static void levelBehaviorTests() throws Exception {
        // Fixed user-visible probes distinguish all five distances in this
        // 1000x400 host layout. They do not read private implementation fields.
        final float[] below={35,25,18,14,11};
        final float[] above={42,32,21,17,14};
        for(int scale:new int[]{1,2})for(int level=0;level<5;level++)for(boolean diagonal:new boolean[]{false,true}) {
            View low=configured(level,false);low.width*=scale;low.height*=scale;
            float radians=diagonal?(float)Math.PI/4:0;
            float x=(float)Math.sin(radians),y=(float)Math.cos(radians);
            check(!gesture(low,x*below[level]*scale,y*below[level]*scale,80),"below selected distance remains a tap at level="+level+" scale="+scale+" diagonal="+diagonal);
            noSymbol(low,"level-specific subthreshold");check(low.tapUps==1,"subthreshold native tap remains single");
            View enough=configured(level,false);enough.width*=scale;enough.height*=scale;
            check(gesture(enough,x*above[level]*scale,y*above[level]*scale,80),"intended distance captures at level="+level+" scale="+scale+" diagonal="+diagonal);
            assertOne(enough,"level-specific accepted flick");
        }
        // The same physical gesture can only become easier when sensitivity
        // increases; wide diagonals remain in the same angular sector.
        for(int slop:new int[]{0,24})for(float degrees:new float[]{0,-45,45,-65,65,80})for(float length:new float[]{10,14,18,22,30,42,60}) {
            boolean lowerAccepted=false;
            for(int level=0;level<5;level++) {
                View v=configured(level,false);ViewConfiguration.scaledTouchSlop=slop;
                boolean accepted=gesture(v,horizontalAt(degrees,length),upwardAt(degrees,length),80);
                check(!lowerAccepted||accepted,"higher sensitivity cannot reject a gesture accepted at a lower setting");
                check(v.insertions.size()==(accepted?1:0)&&v.cancelCalls==(accepted?1:0)&&v.tapUps==(accepted?0:1),"each level either passes one tap or inserts one alternate");
                if(degrees==80)check(!accepted,"sensitivity never broadens the angular sector");
                lowerAccepted=accepted;
            }
        }
        for(int level=0;level<5;level++)for(boolean glide:new boolean[]{false,true}) {
            View jitter=configured(level,glide);check(!gesture(jitter,3,4,70),"small tap jitter remains native at every level");noSymbol(jitter,"small jitter");
            View sure=configured(level,glide);check(gesture(sure,20,60,80),"intentional upward diagonal remains available at every level");assertOne(sure,"all-level deliberate flick");
        }
        for(int level=0;level<3;level++) {
            View slight=configured(level,false);boolean accepted=gesture(slight,0,22,70);
            check(accepted==(level==2),"lower settings suppress the same slight movement that standard accepts");
        }
        for(int level=0;level<5;level++)for(boolean popup:new boolean[]{false,true}) {
            View v=configured(level,false);PopupKey p=null;
            if(popup)p=new PopupKey(v,'a',"？",1);else v.key('a').actor.b=v.key('a').actor.c="！";
            check(gesture(v,0,60,80),"punctuation route captures at every sensitivity");
            check(v.insertions.equals(Arrays.asList(popup?"？":"！")),"native fullwidth punctuation actor is retained");
            assertOne(v,"punctuation alternate route");if(p!=null)p.notOpened();
        }
    }

    static void snapshotAndOwnershipTests() throws Exception {
        View before=configured(2,false);long d=down(before,'a');select(before,0);
        check(up(before,'a',d,0,22,80),"choice after DOWN does not change the in-flight standard distance");
        check(before.insertions.size()==1&&!gesture(before,0,22,80),"next DOWN reads the new lower sensitivity");
        check(gesture(before,0,50,80)&&before.insertions.size()==2,"next gesture remains usable with sufficient movement");

        View decrease=configured(0,false);long lowD=down(decrease,'a');select(decrease,4);
        check(!up(decrease,'a',lowD,0,22,80),"choice after DOWN cannot make an in-flight lower-sensitivity tap into a flick");
        check(gesture(decrease,0,18,80)&&decrease.insertions.size()==1,"following DOWN uses the newly raised sensitivity");

        View captured=configured(2,false);long capD=down(captured,'a');check(moveAt(captured,'a',capD,0,22,20),"capture precedes settings change");select(captured,0);
        check(up(captured,'a',capD,0,2,900),"captured gesture remains owned through setting change and release rebound");assertOne(captured,"captured settings update");
        View reentrant=configured(2,false);reentrant.onCancel=()->select(reentrant,0);
        check(gesture(reentrant,0,22,80),"choice during native CANCEL preserves original capture");assertOne(reentrant,"reentrant settings update");
        reentrant.onCancel=null;check(!gesture(reentrant,0,22,80),"reentrant update applies to the next gesture");

        for(int level=0;level<5;level++) {
            View edge=configured(level,false);check(gesture(edge,0,60,500),"500ms capture deadline stays inclusive for every level");assertOne(edge,"capture deadline");
            View late=configured(level,false);check(!gesture(late,0,60,501),"sensitivity does not extend native long-press acquisition window");noSymbol(late,"late acquisition");
            View cancel=configured(level,false);long cancelD=down(cancel,'a');check(moveAt(cancel,'a',cancelD,0,60,20),"capture before cancellation");
            NativeKey ck=cancel.key('a');check(dispatch(cancel,event(3,ck.x(cancel),ck.y(cancel)-60,cancelD,30)),"owned system cancel consumed");
            check(cancel.insertions.isEmpty()&&cancel.cancelCalls==1,"system cancellation never commits alternate");
            View pointer=configured(level,false);long pointerD=down(pointer,'a');check(moveAt(pointer,'a',pointerD,0,60,20),"capture before second pointer");
            MotionEvent multi=event(5,0,0,pointerD,30);multi.count=2;check(dispatch(pointer,multi),"owned second pointer consumed");
            check(up(pointer,'a',pointerD,0,60,80),"owned stream drains UP after second pointer");noOwnedSymbol(pointer,"multi-pointer after capture");
            View reset=configured(level,false);long resetD=down(reset,'a');check(moveAt(reset,'a',resetD,0,60,20),"capture before lifecycle reset");FlickSymbols.reset();
            check(up(reset,'a',resetD,0,60,80),"reset drains owned UP at every sensitivity");noOwnedSymbol(reset,"reset after capture");
            View press=configured(level,false);press.modelNativeTouch=true;long pressD=down(press,'a');press.advanceNativeTimer();
            check(!up(press,'a',pressD,0,60,600)&&press.nativeLongPresses==1,"existing native long press keeps priority at every sensitivity");
            check(press.insertions.isEmpty()&&press.cancelCalls==0,"sensitivity never substitutes a custom insert after native long press");
        }
        check(MotionEvent.copies==MotionEvent.recycles,"all synthetic cancel events are recycled");
    }

    public static void main(String[] args) throws Exception {
        settingsTests();fallbackTests();levelBehaviorTests();snapshotAndOwnershipTests();
        System.out.println("PASS Sensitivity: "+scenarios+" scenarios, "+assertions+" assertions");
        System.out.println("REPORT {\"status\":\"PASSED\",\"device_tested\":false,\"android_instrumentation_tested\":false,\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
    }
}

