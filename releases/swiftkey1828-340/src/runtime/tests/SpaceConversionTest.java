package hiro.swiftkey;

import java.util.*;
import java.util.concurrent.*;
import com.microsoft.fluency.ResultsFilter.PredictionSearchType;

public final class SpaceConversionTest {
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static final class State implements dc0.a {
        int start=2,end=2; String composition="きょう",raw="kyou";
        public int k(){return start;} public int S(){return end;}
        public String s(){return composition;} public String G(){return raw;}
    }
    static final class Candidate implements m40.b {
        String output,sep=" "; l40.c request=new l40.c(); Runnable onText;
        Candidate(String output,String reading){this.output=output;request.k=reading;request.d=PredictionSearchType.JAPANESE;}
        public String getUserFacingText(){if(onText!=null)onText.run();return output;}
        public l40.c subrequest(){return request;}
        public String getTrailingSeparator(){return sep;}
        public void setTrailingSeparator(String value){sep=value;}
    }
    static final class Prediction extends vj0.c {
        Object value; boolean done=true,cancelled; int gets; RuntimeException failure;
        Prediction(Object value){this.value=value;}
        public boolean isDone(){return done;} public boolean isCancelled(){return cancelled;}
        public boolean cancel(boolean interrupt){return false;}
        public Object get(){gets++; if(failure!=null)throw failure;return value;}
        public Object get(long timeout,TimeUnit unit){return get();}
    }
    static final class Event implements yb0.a {m40.b candidate;int position;wb0.j origin;}
    static final class Factory extends c5.m {
        boolean fail,empty; Event last;
        public yb0.a g(lh0.a token,m40.b candidate,wb0.j origin,int position){
            if(fail)throw new IllegalStateException(); if(empty)return null;
            last=new Event();last.candidate=candidate;last.position=position;last.origin=origin;return last;
        }
    }
    static final class Model extends wb0.x {
        int commits; boolean failAfterCommit; State state=new State();Factory factory=new Factory();
        Model(){
            c=new wb0.n0(){public dc0.a C(){return state;}};b=factory;
            g=new wb0.h1();g.a=new ib0.p();g.b=new wb0.m();g.b.l=PredictionSearchType.JAPANESE;
        }
        public void i0(yb0.a event){commits++;state.composition="";state.raw="";if(failAfterCommit)throw new IllegalStateException();}
        Prediction candidates(Object... values){Prediction p=new Prediction(Arrays.asList(values));g.a.h=p;return p;}
    }
    static boolean press(Model m){return SpaceConversion.handle(m,new lh0.a()," "," ");}
    static Candidate kanji(){return new Candidate("今日","きょう");}
    static Model ready(){Model m=new Model();m.candidates(new Candidate("きょう","きょう"),kanji());return m;}
    static void rejected(Model m,String name){check(!press(m),name);check(m.commits==0,name+" no write");}
    public static void main(String[] args){
        Model m=ready();Candidate selected=(Candidate)((List<?>)((Prediction)m.g.a.h).value).get(1);
        check(press(m),"Japanese space consumed");check(m.commits==1,"one native commit");
        check(m.factory.last.candidate==selected,"original native candidate kept");
        check(m.factory.last.position==1,"ranked position retained");check(m.factory.last.origin==wb0.j.a,"normal selection learning origin");
        check("".equals(selected.sep),"no trailing space");check(!press(m),"second press after commit is ordinary space");
        m=ready();check(SpaceConversion.handle(m,new lh0.a(),"　","　"),"full width space conversion");
        m=ready();check(!SpaceConversion.handle(m,new lh0.a(),"x","x"),"nonspace native path");
        m=ready();check(!SpaceConversion.handle(m,null," "," "),"missing event ignored");
        check(!SpaceConversion.handle(null,new lh0.a()," "," "),"null model");
        m=ready();m.g.b.l=PredictionSearchType.NORMAL;rejected(m,"English layout");
        m=ready();m.state.composition="";rejected(m,"empty composition");
        m=ready();m.state.composition="abc きょう";rejected(m,"mixed text not suffix matched");
        m=ready();m.state.composition="今日";rejected(m,"already converted composition");
        m=ready();m.state.end=3;rejected(m,"selected text");
        m=ready();m.g.f=true;rejected(m,"new prediction buffered");
        m=ready();((Prediction)m.g.a.h).done=false;Prediction p=(Prediction)m.g.a.h;rejected(m,"unfinished future");check(p.gets==0,"never blocks UI");
        m=ready();((Prediction)m.g.a.h).cancelled=true;rejected(m,"cancelled future");
        m=ready();((Prediction)m.g.a.h).failure=new CancellationException();rejected(m,"failed future");
        m=ready();m.g.a.h=null;rejected(m,"no future");
        m=ready();((Prediction)m.g.a.h).value="wrong";rejected(m,"unexpected future type");
        m=ready();m.candidates(new Candidate("昨日","きのう"));rejected(m,"stale reading");
        m=ready();Candidate c=kanji();c.request.f=true;m.candidates(c);rejected(m,"context-only request");
        m=ready();c=kanji();c.request.d=PredictionSearchType.NORMAL;m.candidates(c);rejected(m,"English request");
        m=ready();m.candidates(null,"bad",new Candidate("きょう","きょう"),new Candidate("😊","きょう"),new Candidate("today","きょう"));rejected(m,"literal emoji and English skipped");
        m=ready();Candidate preferred=new Candidate("京","きょう");m.candidates(preferred,kanji());check(press(m)&&m.factory.last.candidate==preferred,"learned candidate order honored");
        m=ready();m.state.composition="キョウ";check(press(m),"katakana composition matching");
        m=ready();m.state.composition="kyou";check(press(m),"Japanese romaji matching");
        m=ready();c=kanji();final Model changed=m;c.onText=new Runnable(){public void run(){changed.state.composition="あした";}};m.candidates(c);rejected(m,"composition changed during selection");
        m=ready();c=kanji();final Model switched=m;c.onText=new Runnable(){public void run(){switched.g.b.l=PredictionSearchType.NORMAL;}};m.candidates(c);rejected(m,"language changed during selection");
        m=ready();c=kanji();final Model replaced=m;c.onText=new Runnable(){public void run(){replaced.g.a.h=new Prediction(Collections.emptyList());}};m.candidates(c);rejected(m,"future replaced during selection");
        m=ready();m.factory.fail=true;selected=(Candidate)((List<?>)((Prediction)m.g.a.h).value).get(1);rejected(m,"factory failure before write");check(" ".equals(selected.sep),"separator restored on failure");
        m=ready();m.factory.empty=true;rejected(m,"null native event");
        m=ready();m.failAfterCommit=true;check(press(m)&&m.commits==1,"dispatch failure does not duplicate space");
        android.os.Looper.main=false;rejected(ready(),"off main thread");android.os.Looper.main=true;
        check(SpaceConversion.hasKanji("𠮷野"),"supplementary kanji");check(!SpaceConversion.hasKanji("カナABC😊"),"nonkanji filtering");
        check(SpaceConversion.reading(" きょう")==null,"no trimming");check(SpaceConversion.reading("123")==null,"numeric unchanged");
        System.out.println("{\"status\":\"PASSED\",\"assertions\":"+checks+",\"native_dispatch_only\":true,\"never_waits_for_prediction\":true,\"device_tested\":false}");
    }
}
