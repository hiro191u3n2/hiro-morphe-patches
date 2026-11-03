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
        int start=2,end=2; String composition="きょう",raw="kyou",context="";
        public int k(){return start;} public int S(){return end;}
        public String s(){return composition;} public String G(){return raw;} public String K(){return context;}
    }
    static final class Candidate implements m40.b {
        int setterCalls; String output,sep=" "; l40.c request=new l40.c(); Runnable onText;
        Candidate(String output,String reading){this.output=output;request.k=reading;request.d=PredictionSearchType.JAPANESE;}
        public String getUserFacingText(){if(onText!=null)onText.run();return output;}
        public l40.c subrequest(){return request;}
        public String getTrailingSeparator(){return sep;}
        public void setTrailingSeparator(String value){setterCalls++;sep=value;}
    }
    // c0's final setter is the literal native throw body, bound by fixture hashes.
    static final class ImmutableCandidate extends m40.c0 {
        final String output; final String sep; final l40.c request=new l40.c();
        ImmutableCandidate(String separator){output="今日";sep=separator;request.k="きょう";request.d=PredictionSearchType.JAPANESE;}
        public String getUserFacingText(){return output;}
        public l40.c subrequest(){return request;}
        public String getTrailingSeparator(){return sep;}
    }
    // The model uses the new read hook only when present, so baseline executes
    // its original reads. Actual native instruction replacement is tested by
    // the separate two-SDK loader probe, not claimed as execution here.
    static String readSeparator(m40.b candidate){
        try {return (String)SpaceConversion.class.getMethod("separator",m40.b.class).invoke(null,candidate);}
        catch(NoSuchMethodException baseline){return candidate.getTrailingSeparator();}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    static m40.b inner(m40.b candidate){
        if(candidate instanceof m40.g0)return ((m40.g0)candidate).a;
        if(candidate instanceof m40.x)return ((m40.x)candidate).b;
        if(candidate instanceof m40.j0)return ((m40.j0)candidate).b;
        if(candidate instanceof m40.k0)return ((m40.k0)candidate).a;
        if(candidate instanceof m40.v)return ((m40.v)candidate).b;
        if(candidate instanceof m40.a0)return ((m40.a0)candidate).a;
        if(candidate instanceof m40.b0)return ((m40.b0)candidate).a;
        if(candidate instanceof m40.i0)return ((m40.i0)candidate).a;
        if(candidate instanceof m40.y)return ((m40.y)candidate).c;
        if(candidate instanceof com.swiftkey.typeface.candidates.candidates.EmailAddressCandidate)
            return ((com.swiftkey.typeface.candidates.candidates.EmailAddressCandidate)candidate).getWrapped();
        return null;
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
        int commits,flushes,nativeRejects; String committedText,visitorSeparator; Runnable duringDispatch; boolean failAfterCommit; Prediction buffered; boolean failFlush; State state=new State();Factory factory=new Factory();
        Model(){
            c=new wb0.n0(){public dc0.a C(){return state;}};b=factory;
            g=new wb0.h1(){public void b(lh0.a token,boolean immediate,ib0.m reason,ec0.j snapshot){
                flushes++;if(failFlush)throw new IllegalStateException();if(buffered!=null)a.h=buffered;
            }};g.a=new ib0.p();g.b=new wb0.m();g.b.l=PredictionSearchType.JAPANESE;
            g.j=new lh0.a();g.h=new ib0.m();g.i=new ec0.j();
        }
        public void i0(yb0.a event){
            Event e=(Event)event;
            // ma0/o relevance check and ac0/b null separator handling.
            if(!Objects.equals(state.K(),e.candidate.subrequest().i)){nativeRejects++;return;}
            if(duringDispatch!=null)duringDispatch.run();
            if(readSeparator(e.candidate)==null)e.candidate.setTrailingSeparator(" ");
            String suffix=readSeparator(e.candidate);
            // ec0/w unwraps decorators and re-reads the native inner candidate.
            m40.b visited=e.candidate;
            for(int depth=0;depth<16&&inner(visited)!=null;depth++)visited=inner(visited);
            visitorSeparator=readSeparator(visited);
            committedText=e.candidate.getUserFacingText()+suffix;
            commits++;state.composition=e.candidate.getUserFacingText();state.raw="きょう";state.marker.k=5;
            if(failAfterCommit)throw new IllegalStateException();
        }
        Prediction candidates(Object... values){Prediction p=new Prediction(Arrays.asList(values));g.a.h=p;return p;}
        ib0.a current(String reading,m40.b... values){
            g.b.g=new lb0.a();ib0.a current=new ib0.a();
            current.a=new ArrayList<m40.b>(Arrays.asList(values));
            current.c=new l40.c();current.c.k=reading;current.c.d=PredictionSearchType.JAPANESE;
            g.b.g.e=current;return current;
        }
    }
    static boolean press(Model m){return SpaceConversion.handle(m,new lh0.a()," "," ");}
    static Candidate kanji(){return new Candidate("今日","きょう");}
    static Model ready(){Model m=new Model();m.candidates(new Candidate("きょう","きょう"),kanji());return m;}
    static void rejected(Model m,String name){check(!press(m),name);check(m.commits==0,name+" no write");}
    public static void main(String[] args){
        if(args.length>0 && "baseline".equals(args[0])){
            int failures=0;
            for(String separator:new String[]{null,""," "})for(boolean wrapped:new boolean[]{false,true}){
                Model original=ready();ImmutableCandidate nativeCandidate=new ImmutableCandidate(separator);
                m40.b choice=wrapped?new m40.x(new m40.g0(nativeCandidate)):nativeCandidate;
                original.current("きょう",choice);original.g.a.h=null;
                check(!press(original),"1831 immutable native setter fails before dispatch");
                check(original.commits==0&&original.factory.last==null,"1831 falls through without native event");
                check(Objects.equals(separator,nativeCandidate.sep),"native candidate remains unchanged");failures++;
            }
            System.out.println("{\"status\":\"PASSED\",\"assertions\":"+checks+",\"baseline_failures_reproduced\":"+failures+",\"failure_scope\":\"immutable_native_prediction_separator\",\"device_trace_reproduced\":false}");return;
        }
        Model m=ready();Candidate selected=(Candidate)((List<?>)((Prediction)m.g.a.h).value).get(1);
        check(press(m),"Japanese space consumed");check(m.commits==1,"one native commit");
        check(m.factory.last.candidate==selected,"original native candidate kept");
        check(m.factory.last.position==1,"ranked position retained");check(m.factory.last.origin==wb0.j.a,"normal selection learning origin");
        check("今日".equals(m.committedText)&&" ".equals(selected.sep)&&selected.setterCalls==0,"no trailing space without candidate mutation");check(!press(m),"second press after commit is ordinary space");
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
        // Current candidate UI is authoritative even when the predictor cache differs.
        m=ready();m.current("きょう",kanji());m.g.a.h=null;
        check(press(m),"current UI converts with missing future");check(m.flushes==0,"UI path never flushes");
        m=ready();m.current("きょう",kanji());m.g.a=null;
        check(press(m),"current UI does not require predictor object");
        m=ready();preferred=new Candidate("京","きょう");m.current("きょう",new Candidate("きょう","きょう"),preferred,kanji());
        p=(Prediction)m.g.a.h;
        check(press(m),"current UI converts before fallback");
        check(m.factory.last.candidate==preferred&&m.factory.last.position==1,"visible learned order and object retained");
        check(p.timedGets==0&&m.flushes==0,"valid UI bypasses all prediction work");
        check("京".equals(m.committedText)&&" ".equals(preferred.sep)&&preferred.setterCalls==0,"visible selection adds no space without mutation");
        check(!press(m),"second UI press on committed marker is ordinary space");
        m=ready();m.current("きょう",kanji());p=(Prediction)m.g.a.h;p.done=false;p.timeout=true;
        check(press(m)&&p.timedGets==0,"ready UI does not wait for pending prediction");
        m=ready();m.current("きょう",kanji());p=(Prediction)m.g.a.h;p.cancelled=true;
        check(press(m)&&p.timedGets==0,"ready UI survives cancelled prediction");
        m=ready();m.current("きょう",kanji());p=(Prediction)m.g.a.h;p.failure=new CancellationException();
        check(press(m)&&p.timedGets==0,"ready UI survives failed prediction");
        m=ready();m.current("きょう",kanji());m.g.f=true;m.failFlush=true;p=(Prediction)m.g.a.h;
        check(press(m)&&m.flushes==0&&p.timedGets==0,"current UI does not flush buffered request");
        m=ready();m.current("きょう",kanji());m.candidates();
        check(press(m),"current UI survives empty cached result");
        m=ready();m.current("きょう",kanji());m.candidates(new Candidate("きょう","きょう"));
        check(press(m),"current UI survives raw cached result without kanji");
        m=ready();m.current("きょう",new Candidate("昨日","きのう"),kanji());m.g.a.h=null;
        check(press(m)&&m.factory.last.position==1,"UI candidates verify their own reading");
        m=ready();c=new Candidate("今日","きょう");c.request.d=PredictionSearchType.NORMAL;
        m.current("きょう",c,kanji());m.g.a.h=null;
        check(press(m)&&m.factory.last.position==1,"UI candidates verify their own language");
        m=ready();ib0.a ui=m.current("きょう",kanji());ui.c.f=true;m.g.a.h=null;
        check(press(m),"complex mode is permitted for matching UI request");
        m=ready();m.current("きのう",new Candidate("昨日","きのう"));p=(Prediction)m.g.a.h;
        check(press(m)&&p.timedGets==1&&"今日".equals(m.factory.last.candidate.getUserFacingText()),"stale UI reading uses current future");
        m=ready();ui=m.current("きょう",kanji());ui.c.d=PredictionSearchType.NORMAL;p=(Prediction)m.g.a.h;
        check(press(m)&&p.timedGets==1,"nonJapanese UI request uses current future");
        m=ready();ui=m.current("きょう",kanji());ui.c=null;p=(Prediction)m.g.a.h;
        check(press(m)&&p.timedGets==1,"missing UI request uses current future");
        m=ready();ui=m.current("きょう",kanji());ui.a=null;p=(Prediction)m.g.a.h;
        check(press(m)&&p.timedGets==1,"missing UI list uses current future");
        m=ready();m.current("きょう",new Candidate("きょう","きょう"));p=(Prediction)m.g.a.h;
        check(press(m)&&p.timedGets==1,"UI without kanji uses current future");
        m=ready();m.current("きょう",new Candidate("きょう","きょう"));m.g.a.h=null;
        rejected(m,"no kanji in either source");
        m=ready();m.current("きのう",kanji());m.g.a.h=null;rejected(m,"stale UI cannot commit without valid fallback");
        m=ready();m.current("きょう",new Candidate("昨日","きのう"));m.g.a.h=null;rejected(m,"stale UI candidate cannot commit");
        m=ready();c=kanji();final Model holderChanged=m;c.onText=new Runnable(){public void run(){holderChanged.g.b.g=new lb0.a();}};
        m.current("きょう",c);rejected(m,"candidate holder replaced during selection");
        m=ready();c=kanji();final Model containerChanged=m;c.onText=new Runnable(){public void run(){containerChanged.g.b.g.e=new ib0.a();}};
        m.current("きょう",c);rejected(m,"candidate container replaced during selection");
        m=ready();c=kanji();final Model containerReset=m;c.onText=new Runnable(){public void run(){containerReset.g.b.g.e=null;}};
        m.current("きょう",c);rejected(m,"candidate container reset during selection");
        m=ready();c=kanji();final Model listChanged=m;c.onText=new Runnable(){public void run(){listChanged.g.b.g.e.a=new ArrayList<m40.b>();}};
        m.current("きょう",c);rejected(m,"candidate list replaced during selection");
        m=ready();c=kanji();final Model requestChanged=m;c.onText=new Runnable(){public void run(){l40.c next=new l40.c();next.k="きょう";next.d=PredictionSearchType.JAPANESE;requestChanged.g.b.g.e.c=next;}};
        m.current("きょう",c);rejected(m,"same-reading UI request replaced during selection");
        m=ready();c=kanji();final Model uiReadingChanged=m;c.onText=new Runnable(){public void run(){uiReadingChanged.g.b.g.e.c.k="あした";}};
        m.current("きょう",c);rejected(m,"UI request reading changed during selection");
        m=ready();c=kanji();final Model uiLanguageChanged=m;c.onText=new Runnable(){public void run(){uiLanguageChanged.g.b.g.e.c.d=PredictionSearchType.NORMAL;}};
        m.current("きょう",c);rejected(m,"UI request language changed during selection");
        m=ready();c=kanji();final Model uiStateChanged=m;c.onText=new Runnable(){public void run(){uiStateChanged.state.raw="あした";}};
        m.current("きょう",c);rejected(m,"raw input changed while selecting visible candidate");
        m=ready();c=kanji();final Model unrelatedFutureChanged=m;c.onText=new Runnable(){public void run(){unrelatedFutureChanged.g.a.h=null;}};
        m.current("きょう",c);check(press(m),"unrelated future change does not invalidate current UI candidate");
        m=ready();m.current("きょう",kanji());m.factory.fail=true;
        selected=(Candidate)m.g.b.g.e.a.get(0);rejected(m,"UI factory failure before write");
        check(" ".equals(selected.sep),"UI separator restored on factory failure");
        m=ready();m.current("きょう",kanji());m.failAfterCommit=true;
        check(press(m)&&m.commits==1,"UI dispatch failure does not duplicate space");
        // The native keyboard appends keys to marker.l without a script restriction.
        // request.k is copied from that raw value. These are valid native-shaped
        // states, not a claim about the unreadable internal state in the video.
        for(String rawInput:new String[]{"あi","会i","ａｉ","きょu","n","k","123"}){
            m=ready();m.state.raw=rawInput;m.state.composition="あい";
            Candidate exact=new Candidate("愛",rawInput);m.current(rawInput,exact);m.g.a.h=null;
            check(press(m)&&m.factory.last.candidate==exact,"exact native UI raw accepted: "+rawInput);
            check(m.commits==1&&"愛".equals(m.committedText)&&" ".equals(exact.sep)&&exact.setterCalls==0,"exact UI one commit without trailing blank");
            check(!press(m),"exact UI committed state rejects repeated conversion");
            m=ready();m.state.raw=rawInput;exact=new Candidate("愛",rawInput);m.candidates(exact);
            check(press(m)&&m.factory.last.candidate==exact,"exact native Future raw accepted: "+rawInput);
            m=ready();m.state.raw=rawInput;m.current(rawInput,new Candidate("愛",rawInput+"x"));m.g.a.h=null;
            rejected(m,"same-prefix candidate with different raw is stale");
            m=ready();m.state.raw=rawInput;m.current(rawInput+"x",new Candidate("愛",rawInput));m.g.a.h=null;
            rejected(m,"same-prefix container with different raw is stale");
            m=ready();m.state.raw=rawInput;m.current(rawInput,new Candidate("愛",rawInput));m.g.a.h=null;m.state.marker.k=5;
            rejected(m,"exact matching cannot convert committed text");
            m=ready();m.state.raw=rawInput;m.current(rawInput,new Candidate("愛",rawInput));m.g.a.h=null;m.g.b.l=PredictionSearchType.NORMAL;
            rejected(m,"exact matching cannot convert English layout");
            m=ready();m.state.raw=rawInput;exact=new Candidate("愛",rawInput);exact.request.d=PredictionSearchType.NORMAL;
            m.current(rawInput,exact);m.g.a.h=null;rejected(m,"exact matching cannot use English request");
        }
        m=ready();m.state.raw="あi";m.current("あi",new Candidate("愛","あk"));m.g.a.h=null;
        rejected(m,"two unnormalizable values never match through null");
        m=ready();m.state.raw="ａｉ";m.current("ａｉ",new Candidate("あい","ａｉ"));m.g.a.h=null;
        rejected(m,"exact raw without kanji leaves normal space");
        android.os.Looper.main=false;rejected(ready(),"off main thread");android.os.Looper.main=true;
        check(SpaceConversion.hasKanji("𠮷野"),"supplementary kanji");check(!SpaceConversion.hasKanji("カナABC😊"),"nonkanji filtering");
        check(SpaceConversion.reading(" きょう")==null,"no trimming");check(SpaceConversion.reading("123")==null,"numeric unchanged");
        // Ordinary taps use the same event factory with no space scope.
        Model ordinary=ready();ImmutableCandidate ordinaryCandidate=new ImmutableCandidate(" ");
        ordinary.i0(ordinary.factory.g(new lh0.a(),ordinaryCandidate,wb0.j.a,0));
        check("今日 ".equals(ordinary.committedText)&&" ".equals(ordinary.visitorSeparator),"ordinary candidate tap keeps native separator");
        check(" ".equals(ordinaryCandidate.sep),"ordinary immutable candidate is not altered");
        int immutableCases=0;
        for(String originalSeparator:new String[]{null,""," ","　"}){
            for(int source=0;source<2;source++){
                ImmutableCandidate nativeCandidate=new ImmutableCandidate(originalSeparator);
                Model immutable=ready();
                if(source==0){immutable.current("きょう",nativeCandidate);immutable.g.a.h=null;}
                else immutable.candidates(nativeCandidate);
                check(press(immutable)&&immutable.commits==1,"immutable native candidate commits from source "+source);
                check(immutable.factory.last.candidate==nativeCandidate,"native candidate object retained");
                check("今日".equals(immutable.committedText)&&"".equals(immutable.visitorSeparator),"native write and visitor add no blank");
                check(Objects.equals(originalSeparator,nativeCandidate.getTrailingSeparator()),"native getter unchanged");
                check(Objects.equals(originalSeparator,readSeparator(nativeCandidate)),"scope cleared after dispatch");
                check(!press(immutable),"second space uses normal path after native commit");immutableCases++;
            }
        }
        final ImmutableCandidate nativeCandidate=new ImmutableCandidate(" ");
        m40.b[] decorators={new m40.g0(nativeCandidate),new m40.x(nativeCandidate),new m40.j0(nativeCandidate),
            new m40.k0(nativeCandidate),new m40.v(nativeCandidate),new m40.a0(nativeCandidate),new m40.b0(nativeCandidate),
            new com.swiftkey.typeface.candidates.candidates.EmailAddressCandidate(nativeCandidate),
            new m40.x(new m40.g0(new m40.k0(nativeCandidate)))};
        for(m40.b decorated:decorators){
            Model wrapped=ready();wrapped.current("きょう",decorated);wrapped.g.a.h=null;
            check(press(wrapped)&&wrapped.commits==1,"decorated native candidate commits");
            check(wrapped.factory.last.candidate==decorated,"original decorator retained for native learning");
            check("".equals(wrapped.visitorSeparator),"visitor's exact inner identity is scoped");
            check(" ".equals(nativeCandidate.sep)&&" ".equals(readSeparator(decorated)),"decorator/native separator unchanged after scope");
        }
        m40.h0 literal=new m40.h0("今日","きょう",nativeCandidate.request);literal.d=" ";
        for(m40.b decorated:new m40.b[]{new m40.i0(literal),new m40.y(literal)}){
            Model wrapped=ready();wrapped.current("きょう",decorated);wrapped.g.a.h=null;
            check(press(wrapped)&&"".equals(wrapped.visitorSeparator),"literal decorator inner identity scoped");
            check(" ".equals(literal.d),"literal native data not mutated");
        }
        final Model scoped=ready();scoped.current("きょう",nativeCandidate);scoped.g.a.h=null;
        final Candidate sibling=kanji();sibling.request=nativeCandidate.request;sibling.sep="sibling";
        scoped.duringDispatch=new Runnable(){public void run(){
            check("".equals(readSeparator(nativeCandidate)),"selected identity suppressed during dispatch");
            check("sibling".equals(readSeparator(sibling)),"same-request different candidate unchanged");
            check(" ".equals(nativeCandidate.getTrailingSeparator()),"direct getters/hash/visitor data unchanged");
            final String[] workerRead=new String[1];Thread worker=new Thread(new Runnable(){public void run(){workerRead[0]=readSeparator(nativeCandidate);}});
            worker.start();try{worker.join();}catch(InterruptedException e){throw new AssertionError(e);}
            check(" ".equals(workerRead[0]),"scope is isolated from worker learning thread");
            Model nested=ready();Candidate nestedCandidate=kanji();nestedCandidate.sep="nested";
            nested.current("きょう",nestedCandidate);nested.g.a.h=null;
            check(press(nested)&&"今日".equals(nested.committedText),"nested dispatch has independent selection");
            check("".equals(readSeparator(nativeCandidate)),"outer scope restored after nested dispatch");
            check("nested".equals(readSeparator(nestedCandidate)),"nested scope no longer active");
        }};
        check(press(scoped)&&scoped.commits==1,"scoped dispatch completes");
        check(" ".equals(readSeparator(nativeCandidate)),"scope cleared after outer dispatch");
        Model failed=ready();failed.current("きょう",nativeCandidate);failed.failAfterCommit=true;
        check(press(failed)&&failed.commits==1,"post-dispatch failure never writes extra space");
        check(" ".equals(readSeparator(nativeCandidate)),"scope cleared after dispatch exception");
        m=ready();m.state.context="new context";rejected(m,"native context mismatch rejected before dispatch");
        check(m.factory.last==null,"stale native context never creates event");
        m=ready();c=kanji();final Model changedContext=m;c.onText=new Runnable(){public void run(){changedContext.state.context="changed";}};
        m.current("きょう",c);m.g.a.h=null;rejected(m,"context changed while selecting candidate");
        System.out.println("{\"status\":\"PASSED\",\"assertions\":"+checks+",\"native_dispatch_only\":true,\"immutable_native_cases\":"+immutableCases+",\"candidate_mutation_removed\":true,\"identity_scoped_separator_reads\":true,\"prediction_wait_limit_ms\":750,\"device_tested\":false}");
    }
}
