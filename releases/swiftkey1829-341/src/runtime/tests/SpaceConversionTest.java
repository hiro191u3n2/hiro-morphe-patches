package hiro.swiftkey;

import java.util.*;
import java.util.concurrent.*;
import com.microsoft.fluency.ResultsFilter.PredictionSearchType;

public final class SpaceConversionTest {
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static final class State extends ec0.f {
        ec0.y marker=new ec0.y();
        State(){marker.k=2;}
        public ec0.y a0(){return marker;}
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
        Object value; boolean done=true,cancelled; int gets,timedGets; RuntimeException failure; boolean timeout,interrupted; long waitMs; Runnable onGet;
        Prediction(Object value){this.value=value;}
        public boolean isDone(){return done;} public boolean isCancelled(){return cancelled;}
        public boolean cancel(boolean interrupt){return false;}
        public Object get(){gets++; if(failure!=null)throw failure;return value;}
        public Object get(long timeout,TimeUnit unit) throws TimeoutException,InterruptedException {
            timedGets++;waitMs=unit.toMillis(timeout);
            if(this.timeout)throw new TimeoutException();
            if(interrupted)throw new InterruptedException();
            if(onGet!=null)onGet.run();done=true;return get();
        }
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
        int commits,flushes; boolean failAfterCommit; Prediction buffered; boolean failFlush; State state=new State();Factory factory=new Factory();
        Model(){
            c=new wb0.n0(){public dc0.a C(){return state;}};b=factory;
            g=new wb0.h1(){public void b(lh0.a token,boolean immediate,ib0.m reason,ec0.j snapshot){
                flushes++;if(failFlush)throw new IllegalStateException();if(buffered!=null)a.h=buffered;
            }};g.a=new ib0.p();g.b=new wb0.m();g.b.l=PredictionSearchType.JAPANESE;
            g.j=new lh0.a();g.h=new ib0.m();g.i=new ec0.j();
        }
        public void i0(yb0.a event){commits++;state.composition="今日";state.raw="きょう";state.marker.k=5;if(failAfterCommit)throw new IllegalStateException();}
        Prediction candidates(Object... values){Prediction p=new Prediction(Arrays.asList(values));g.a.h=p;return p;}
    }
    static boolean press(Model m){return SpaceConversion.handle(m,new lh0.a()," "," ");}
    static Candidate kanji(){return new Candidate("今日","きょう");}
    static Model ready(){Model m=new Model();m.candidates(new Candidate("きょう","きょう"),kanji());return m;}
    static void rejected(Model m,String name){check(!press(m),name);check(m.commits==0,name+" no write");}
    public static void main(String[] args){
        if(args.length>0 && "baseline".equals(args[0])){
            Model raw=ready();raw.state.composition="前の文きょう";
            check(!press(raw),"1828 reproduces raw/display mismatch");
            Model pending=ready();((Prediction)pending.g.a.h).done=false;
            check(!press(pending),"1828 reproduces running prediction fallback");
            Model buffered=ready();buffered.g.f=true;
            check(!press(buffered),"1828 reproduces buffered prediction fallback");
            System.out.println("{\"status\":\"PASSED\",\"assertions\":"+checks+",\"baseline_failures_reproduced\":3}");return;
        }
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
        m=ready();m.state.composition="";m.state.raw="";rejected(m,"empty raw input");
        m=ready();m.state.raw="abc きょう";rejected(m,"mixed raw not suffix matched");
        m=ready();m.state.marker.k=5;rejected(m,"committed marker with retained raw");
        m=ready();m.state.end=3;rejected(m,"selected text");
        m=ready();m.g.f=true;m.buffered=new Prediction(Arrays.asList(kanji()));
        Prediction old=(Prediction)m.g.a.h;check(press(m),"buffered prediction submitted before conversion");
        check(m.flushes==1&&!m.g.f&&old.gets==0&&m.buffered.timedGets==1,"only fresh buffered request awaited");
        m=ready();((Prediction)m.g.a.h).done=false;Prediction p=(Prediction)m.g.a.h;
        check(press(m),"unfinished future awaited");check(p.timedGets==1&&p.waitMs==750,"wait has finite 750ms bound");
        m=ready();((Prediction)m.g.a.h).cancelled=true;rejected(m,"cancelled future");
        m=ready();((Prediction)m.g.a.h).failure=new CancellationException();rejected(m,"failed future");
        m=ready();m.g.a.h=null;rejected(m,"no future");
        m=ready();((Prediction)m.g.a.h).value="wrong";rejected(m,"unexpected future type");
        m=ready();m.candidates(new Candidate("昨日","きのう"));rejected(m,"stale reading");
        m=ready();Candidate c=kanji();c.request.f=true;m.candidates(c);check(press(m),"complex prediction flag alone is not context-only");
        m=ready();c=kanji();c.request.d=PredictionSearchType.NORMAL;m.candidates(c);rejected(m,"English request");
        m=ready();m.candidates(null,"bad",new Candidate("きょう","きょう"),new Candidate("😊","きょう"),new Candidate("today","きょう"));rejected(m,"literal emoji and English skipped");
        m=ready();Candidate preferred=new Candidate("京","きょう");m.candidates(preferred,kanji());check(press(m)&&m.factory.last.candidate==preferred,"learned candidate order honored");
        m=ready();m.state.raw="キョウ";check(press(m),"katakana raw matching");
        m=ready();m.state.raw="kyou";check(press(m),"Japanese romaji matching");
        m=ready();c=kanji();final Model changed=m;c.onText=new Runnable(){public void run(){changed.state.composition="あした";}};m.candidates(c);rejected(m,"composition changed during selection");
        m=ready();c=kanji();final Model switched=m;c.onText=new Runnable(){public void run(){switched.g.b.l=PredictionSearchType.NORMAL;}};m.candidates(c);rejected(m,"language changed during selection");
        m=ready();c=kanji();final Model replaced=m;c.onText=new Runnable(){public void run(){replaced.g.a.h=new Prediction(Collections.emptyList());}};m.candidates(c);rejected(m,"future replaced during selection");
        m=ready();m.factory.fail=true;selected=(Candidate)((List<?>)((Prediction)m.g.a.h).value).get(1);rejected(m,"factory failure before write");check(" ".equals(selected.sep),"separator restored on failure");
        m=ready();m.factory.empty=true;rejected(m,"null native event");
        m=ready();m.failAfterCommit=true;check(press(m)&&m.commits==1,"dispatch failure does not duplicate space");
        m=ready();m.state.composition="前の文きょう";check(press(m),"display spans differ from raw reading");
        m=ready();m.state.composition="今日";check(press(m),"provisional display same as candidate still commits");
        for(int kind:new int[]{2,3,4,6,7}){m=ready();m.state.marker.k=kind;check(press(m),"native editing marker "+kind);}
        for(int kind:new int[]{0,1,5,8}){m=ready();m.state.marker.k=kind;rejected(m,"nonediting marker "+kind);}
        m=ready();m.state.marker=null;rejected(m,"no active marker");
        m=ready();m.g.f=true;m.g.i=null;rejected(m,"missing buffered snapshot");
        m=ready();m.g.f=true;m.failFlush=true;rejected(m,"failed buffer submission");check(m.g.f,"failed flush preserves pending flag");
        m=ready();p=(Prediction)m.g.a.h;p.timeout=true;rejected(m,"prediction timeout falls back without write");
        m=ready();p=(Prediction)m.g.a.h;p.interrupted=true;rejected(m,"prediction interrupted");check(Thread.interrupted(),"interrupt status restored");
        m=ready();p=(Prediction)m.g.a.h;final Model moving=m;p.onGet=new Runnable(){public void run(){moving.state.start++;moving.state.end++;}};rejected(m,"cursor moved while waiting");
        m=ready();c=kanji();final Model retyped=m;c.onText=new Runnable(){public void run(){retyped.state.raw="あした";}};m.candidates(c);rejected(m,"raw changed during selection");
        m=ready();c=kanji();final Model committed=m;c.onText=new Runnable(){public void run(){committed.state.marker.k=5;}};m.candidates(c);rejected(m,"marker committed during selection");
        m=ready();c=kanji();final Model sameText=m;c.onText=new Runnable(){public void run(){sameText.state.marker=new ec0.y();sameText.state.marker.k=2;}};m.candidates(c);rejected(m,"same text with replaced marker");
        // A real incomplete Future finishes on a worker, like vj0.c's fluency service result.
        m=ready();final CompletableFuture<Object> async=new CompletableFuture<Object>();
        m.g.a.h=new vj0.c(){public boolean cancel(boolean x){return async.cancel(x);}public boolean isDone(){return async.isDone();}public boolean isCancelled(){return async.isCancelled();}
            public Object get()throws InterruptedException,ExecutionException{return async.get();}
            public Object get(long t,TimeUnit u)throws InterruptedException,ExecutionException,TimeoutException{return async.get(t,u);}};
        Thread producer=new Thread(new Runnable(){public void run(){try{Thread.sleep(30);}catch(InterruptedException e){throw new RuntimeException(e);}async.complete(Arrays.asList(kanji()));}});
        producer.start();check(press(m),"real worker Future converts during outstanding calculation");
        android.os.Looper.main=false;rejected(ready(),"off main thread");android.os.Looper.main=true;
        check(SpaceConversion.hasKanji("𠮷野"),"supplementary kanji");check(!SpaceConversion.hasKanji("カナABC😊"),"nonkanji filtering");
        check(SpaceConversion.reading(" きょう")==null,"no trimming");check(SpaceConversion.reading("123")==null,"numeric unchanged");
        System.out.println("{\"status\":\"PASSED\",\"assertions\":"+checks+",\"native_dispatch_only\":true,\"prediction_wait_limit_ms\":750,\"device_tested\":false}");
    }
}
