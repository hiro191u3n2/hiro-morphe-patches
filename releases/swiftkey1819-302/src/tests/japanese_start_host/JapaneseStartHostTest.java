import android.os.Looper;
import android.view.inputmethod.EditorInfo;
import com.touchtype.KeyboardService;
import hiro.swiftkey.JapaneseStart;
import java.lang.reflect.*;
import java.util.*;

/** Lifecycle and native-selection model; does not claim Android execution. */
public final class JapaneseStartHostTest {
    static int assertions, scenarios;
    static void check(boolean b, String why) { assertions++; if (!b) throw new AssertionError(why); }
    static final class Fixture {
        final KeyboardService service = new KeyboardService();
        final EditorInfo editor = new EditorInfo();
        final fb0.i0 layout = new fb0.i0();
        final fb0.f5 languages;
        Fixture(qj0.a... enabled) { languages = new fb0.f5(enabled); layout.a=service; layout.E=languages; }
        void begin(boolean restart) { JapaneseStart.begin(service, editor, restart); }
        qj0.a select() { return JapaneseStart.select(layout, languages, languages.i()); }
        // g0.b resets secondary mode and assigns the chosen layout after the hook.
        void nativeStart(boolean restart) { begin(restart); layout.m=select(); layout.secondary=false; }
    }
    static Fixture fresh(qj0.a... enabled) { scenarios++; return new Fixture(enabled); }
    static void lifecycle() {
        Fixture f=fresh(qj0.a.OTHER,qj0.a.x4); f.nativeStart(false);
        check(f.layout.m==qj0.a.x4 && f.languages.current==qj0.a.x4,"English last-used selects enabled ROMAJI");
        check(!f.layout.secondary && f.languages.choices==1,"Native reconstruction sees Japanese primary mode once");
        f.languages.current=qj0.a.OTHER; f.begin(true);
        check(f.select()==qj0.a.OTHER && f.languages.choices==1,"Active restart preserves manual English");
        check(f.select()==qj0.a.OTHER,"Unarmed repeated native callback preserves manual English");
        JapaneseStart.finish(f.service); f.nativeStart(true);
        check(f.layout.m==qj0.a.x4 && f.languages.choices==2,"Hide/reopen with restarting=true selects Japanese");
        f.languages.current=qj0.a.OTHER; JapaneseStart.finish(f.service); JapaneseStart.finish(f.service); f.nativeStart(false);
        check(f.layout.m==qj0.a.x4 && f.languages.choices==3,"FinishInputView plus WindowHidden is idempotent");
        f.languages.current=qj0.a.OTHER; f.nativeStart(false);
        check(f.layout.m==qj0.a.x4 && f.languages.choices==4,"Nonrestart starts a new input session");
        Fixture r=fresh(qj0.a.OTHER,qj0.a.x4); r.nativeStart(true);
        check(r.layout.m==qj0.a.x4,"First callback may already have restarting=true");
        Fixture kana=fresh(qj0.a.OTHER,qj0.a.y4); kana.nativeStart(false);
        check(kana.layout.m==qj0.a.y4,"Configured kana remains kana, no forced QWERTY setting");
        Fixture both=fresh(qj0.a.x4,qj0.a.y4); both.languages.current=qj0.a.y4; both.nativeStart(false);
        check(both.layout.m==qj0.a.y4 && both.languages.choices==0,"Already selected Japanese layout is preserved");
        Fixture qwerty=fresh(qj0.a.OTHER,qj0.a.x4); qwerty.languages.current=qj0.a.x4; qwerty.nativeStart(false);
        check(qwerty.layout.m==qj0.a.x4 && qwerty.languages.choices==0 && !qwerty.layout.secondary,"Secondary English mode returns to native primary without changing language");
        Fixture disabled=fresh(qj0.a.OTHER); disabled.nativeStart(false);
        check(disabled.layout.m==qj0.a.OTHER && disabled.languages.choices==0,"No installed configured Japanese leaves native alone");
        Fixture empty=fresh(qj0.a.OTHER,qj0.a.x4); empty.languages.packs.put(qj0.a.x4,Collections.emptySet());empty.nativeStart(false);
        check(empty.layout.m==qj0.a.OTHER,"Configured placeholder without enabled language pack is skipped");
        Fixture mixed=fresh(qj0.a.OTHER,qj0.a.x4,qj0.a.y4);mixed.languages.packs.put(qj0.a.x4,null);mixed.nativeStart(false);
        check(mixed.layout.m==qj0.a.y4,"Missing language packs do not override another valid configured Japanese layout");
    }
    static void editorScopes() {
        for(int type:new int[]{0,2,3,4,5,15,18,19,20}) {
            Fixture f=fresh(qj0.a.OTHER,qj0.a.x4);f.editor.inputType=type;f.nativeStart(false);
            check(f.layout.m==qj0.a.OTHER && f.languages.choices==0,"Nontext classes retain native required layout");
        }
        for(int kind=3;kind<=8;kind++) {
            Fixture f=fresh(qj0.a.OTHER,qj0.a.x4);f.layout.k.a=kind;f.nativeStart(false);
            check(f.layout.m==qj0.a.OTHER && f.languages.choices==0,"Native email/URI/password/numeric/date classification retained");
            f.layout.k.a=1;check(f.select()==qj0.a.OTHER,"Special-field callback consumes startup request before later updates");
        }
        for(int kind:new int[]{1,2})for(int type:new int[]{1,0x20001,0x1001,0x4001}) {
            Fixture f=fresh(qj0.a.OTHER,qj0.a.x4);f.editor.inputType=type;f.layout.k.a=kind;f.nativeStart(false);
            check(f.layout.m==qj0.a.x4,"Normal and message text flags allow Japanese");
        }
        Fixture ascii=fresh(qj0.a.OTHER,qj0.a.x4);ascii.editor.imeOptions=0x80000000;ascii.nativeStart(false);
        check(ascii.layout.m==qj0.a.OTHER,"App force-ASCII request preserved");
        Fixture unknown=fresh(qj0.a.OTHER,qj0.a.x4);unknown.layout.k=null;unknown.nativeStart(false);
        check(unknown.layout.m==qj0.a.OTHER,"Uninitialized native editor policy stays native");
        Fixture nullEditor=fresh(qj0.a.OTHER,qj0.a.x4);JapaneseStart.begin(nullEditor.service,null,false);
        check(nullEditor.select()==qj0.a.OTHER,"Missing editor does not arm");
    }
    static void ownershipAndReentry() {
        Fixture before=fresh(qj0.a.OTHER,qj0.a.x4);before.begin(false);before.begin(true);before.begin(true);
        check(before.select()==qj0.a.x4 && before.languages.choices==1,"Restart before initial selection retains the unconsumed startup marker");
        before.languages.current=qj0.a.OTHER;before.begin(true);
        check(before.select()==qj0.a.OTHER && before.languages.choices==1,"After consumption an active restart still preserves manual English");
        Fixture a=fresh(qj0.a.OTHER,qj0.a.x4),b=fresh(qj0.a.OTHER,qj0.a.y4);a.begin(false);
        check(b.select()==qj0.a.OTHER,"One service cannot arm another");
        b.begin(false);JapaneseStart.finish(a.service);
        check(a.select()==qj0.a.OTHER && b.select()==qj0.a.y4,"Finishing one owner leaves other independent");
        Fixture mismatch=fresh(qj0.a.OTHER,qj0.a.x4);mismatch.begin(false);
        check(JapaneseStart.select(mismatch.layout,new fb0.f5(qj0.a.x4),qj0.a.OTHER)==qj0.a.OTHER,"Foreign language controller rejected");
        check(mismatch.select()==qj0.a.OTHER,"Rejected owner request cannot leak into later callback");
        for(boolean query:new boolean[]{false,true}) {
            final Fixture f=fresh(qj0.a.OTHER,qj0.a.x4);f.begin(false);
            Runnable callback=new Runnable(){public void run(){f.begin(false);check(f.select()==qj0.a.OTHER,"Same-frame nested start cannot select twice");}};
            if(query)f.languages.onQuery=callback;else f.languages.onSelect=callback;
            check(f.select()==qj0.a.x4 && f.languages.choices==1,"Single native selection across reentry");
            f.languages.onQuery=null;f.languages.onSelect=null;f.languages.current=qj0.a.OTHER;
            check(f.select()==qj0.a.OTHER,"No pending work survives selection");
        }
        Fixture query=fresh(qj0.a.OTHER,qj0.a.x4);query.languages.queryThrows=true;query.begin(false);
        check(query.select()==qj0.a.OTHER,"Native availability exception falls back");query.languages.queryThrows=false;
        check(query.select()==qj0.a.OTHER,"Failed lookup is consumed");JapaneseStart.finish(query.service);query.begin(true);
        check(query.select()==qj0.a.x4,"New display can recover from previous failure");
        Fixture throwing=fresh(qj0.a.OTHER,qj0.a.x4);throwing.languages.selectThrows=true;throwing.begin(false);
        check(throwing.select()==qj0.a.OTHER,"Native selection exception stays nonfatal");
        Fixture rejected=fresh(qj0.a.OTHER,qj0.a.x4);rejected.languages.reject=true;rejected.begin(false);
        check(rejected.select()==qj0.a.OTHER,"Native selection rejection preserves original result");
        Fixture thread=fresh(qj0.a.OTHER,qj0.a.x4);Looper.main=false;thread.begin(false);
        check(thread.select()==qj0.a.OTHER,"Off-main callbacks do no work");Looper.main=true;
        check(thread.select()==qj0.a.OTHER,"Off-main begin left no pending selection");thread.begin(false);Looper.main=false;JapaneseStart.finish(thread.service);Looper.main=true;
        check(thread.select()==qj0.a.x4,"Off-main finish cannot corrupt live main-thread state");
        check(JapaneseStart.select(null,null,qj0.a.OTHER)==qj0.a.OTHER,"Null native owner is safe");
        JapaneseStart.begin(null,new EditorInfo(),false);JapaneseStart.finish(null);
    }
    public static void main(String[] args) throws Exception {
        lifecycle();editorScopes();ownershipAndReentry();
        Field sessions=JapaneseStart.class.getDeclaredField("SESSIONS");sessions.setAccessible(true);
        check(sessions.get(null) instanceof WeakHashMap,"Lifecycle table does not retain destroyed service owners");
        Class<?> state=Class.forName("hiro.swiftkey.JapaneseStart$Session");
        for(Field f:state.getDeclaredFields())check(f.getType()==boolean.class,"Session state retains only flags, no editor/text/owner");
        System.out.println("PASS JapaneseStart scenarios="+scenarios+" assertions="+assertions);
        System.out.println("{\"status\":\"PASSED\",\"scenarios\":"+scenarios+",\"assertions\":"+assertions+",\"device_tested\":false}");
    }
}
