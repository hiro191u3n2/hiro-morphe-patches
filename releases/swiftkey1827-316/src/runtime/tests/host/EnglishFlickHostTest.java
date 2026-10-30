import hiro.swiftkey.FlickSymbols;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.microsoft.fluency.ResultsFilter.PredictionSearchType;
import java.util.Arrays;

/** Production dispatcher with explicit native test doubles, not an Android test. */
public final class EnglishFlickHostTest extends FlickSymbolsHostTest {
    static View english(boolean romaji, boolean glide) {
        View v = new View(true, glide);
        v.a.c.c = romaji ? qj0.a.x4 : qj0.a.N1;
        ((fb0.f0) v.a).keys.b = PredictionSearchType.NORMAL;
        return v;
    }
    static void gesture(View v, char c) {
        long d = down(v,c);
        check(FlickSymbols.pending(v), "English native alternate is eligible");
        check(moveAt(v,c,d,0,100,30), "English upward move captured");
        check(up(v,c,d,0,110,90), "English release owned");
    }
    public static void main(String[] args) throws Exception {
        boolean baseline = args.length != 0 && args[0].equals("baseline");
        if (baseline) {
            for (boolean romaji : new boolean[]{false,true}) for (boolean popup : new boolean[]{false,true}) {
                scenario(); View v=english(romaji,false);
                if(popup)new PopupKey(v,'q',"1",1);
                long d=down(v,'q');
                check(!FlickSymbols.pending(v),"published baseline rejects English before movement");
                check(!up(v,'q',d,0,100,90),"published baseline leaves English native release");
                check(v.insertions.isEmpty()&&v.cancelCalls==0&&v.tapUps==1,"baseline failure reproduced");
            }
            System.out.println("REPORT {\"status\":\"PASSED\",\"baseline_english_failure_reproduced\":true,\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
            return;
        }
        for(boolean romaji:new boolean[]{false,true}) for(boolean glide:new boolean[]{false,true}) {
            for(char key : "qwertyuiop".toCharArray()) for(boolean popup:new boolean[]{false,true}) {
                scenario();View v=english(romaji,glide);String symbol=String.valueOf("1234567890".charAt("qwertyuiop".indexOf(key)));
                PopupKey p=popup?new PopupKey(v,key,symbol,1):null;
                gesture(v,key);oneSymbol(v,"English direct and popup alternate");
                check(v.insertions.equals(Arrays.asList(symbol)),"actual configured alternate used");
                if(p!=null)p.notOpened();
            }
            scenario();View repeated=english(romaji,glide);new PopupKey(repeated,'e',"3",1);
            gesture(repeated,'q');gesture(repeated,'e');gesture(repeated,'q');
            check(repeated.insertions.equals(Arrays.asList("1","3","1")),"consecutive flicks stay eligible");
            scenario();View shifted=english(romaji,glide);shifted.key('a').actor.b=shifted.key('a').actor.c="@";
            gesture(shifted,'a');check(shifted.insertions.equals(Arrays.asList("@")),"native shift-conditioned alternate preserved");
            scenario();View nativeDefault=english(romaji,glide);new PopupKey(nativeDefault,'a',"á",2);
            gesture(nativeDefault,'a');check(nativeDefault.insertions.equals(Arrays.asList("á")),"native popup default is not hardcoded punctuation");
            for(int sensitivity=0;sensitivity<5;sensitivity++){
                scenario();View v=english(romaji,glide);v.prefs.intValues.put(FlickSymbols.SENSITIVITY_KEY,sensitivity);ViewConfiguration.scaledTouchSlop=24;
                gesture(v,'q');oneSymbol(v,"all English sensitivity levels");
            }
            scenario();View tap=english(romaji,glide);long d=down(tap,'q');check(!up(tap,'q',d,0,0,50),"ordinary English tap remains native");noSymbol(tap,"tap");
            scenario();View side=english(romaji,glide);d=down(side,'q');check(!moveAt(side,'q',d,100,0,30),"sideways English Flow stays native");check(!up(side,'q',d,120,0,90),"native English Flow release remains native");noSymbol(side,"sideways Flow");
            scenario();View hold=english(romaji,glide);hold.modelNativeTouch=true;d=down(hold,'q');hold.advanceNativeTimer();check(!up(hold,'q',d,0,100,600),"native long press keeps priority");noSymbol(hold,"long press");
            for(boolean captured:new boolean[]{false,true})for(int change=0;change<4;change++){
                scenario();View v=english(romaji,glide);d=down(v,'q');if(captured)check(moveAt(v,'q',d,0,100,30),"mode switch setup capture");
                if(change==0)v.a.c.c=romaji?qj0.a.N1:qj0.a.x4;
                if(change==1){v.a.c.c=qj0.a.x4;((fb0.f0)v.a).keys.b=PredictionSearchType.JAPANESE;}
                if(change==2)v.service.i=new android.view.inputmethod.EditorInfo();
                if(change==3)v.prefs.flick(false);
                check(up(v,'q',d,0,110,90)==captured,"changed context drains only owned stream");
                check(v.insertions.isEmpty(),"mode/editor/setting change never inserts");
            }
            scenario();View cancel=english(romaji,glide);d=down(cancel,'q');check(moveAt(cancel,'q',d,0,100,30),"cancel setup");
            check(dispatch(cancel,event(MotionEvent.ACTION_CANCEL,50,0,d,50)),"captured CANCEL consumed");check(cancel.insertions.isEmpty(),"CANCEL inserts nothing");
            scenario();View multi=english(romaji,glide);d=down(multi,'q');MotionEvent second=event(MotionEvent.ACTION_POINTER_DOWN,50,0,d,30);second.count=2;
            check(!dispatch(multi,second),"second finger leaves unowned stream native");noSymbol(multi,"multitouch");
        }
        for(qj0.a kind:new qj0.a[]{qj0.a.OTHER})for(PredictionSearchType search:new PredictionSearchType[]{PredictionSearchType.NORMAL,PredictionSearchType.JAPANESE,PredictionSearchType.OTHER}){
            scenario();View v=new View(true,false);v.a.c.c=kind;((fb0.f0)v.a).keys.b=search;long d=down(v,'q');check(!FlickSymbols.pending(v),"other layout excluded");check(!up(v,'q',d,0,100,90),"other layout stays native");noSymbol(v,"other layout");
        }
        for(int type:new int[]{2,3,4,129,145,225}){
            scenario();View v=english(false,false);v.service.i.inputType=type;long d=down(v,'q');check(!FlickSymbols.pending(v),"sensitive/nontext field excluded");check(!up(v,'q',d,0,100,90),"excluded field stays native");noSymbol(v,"excluded field");
        }
        System.out.println("REPORT {\"status\":\"PASSED\",\"english_upflick_enabled\":true,\"japanese_upflick_preserved\":true,\"gesture_safety_tests_passed\":true,\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
    }
}
