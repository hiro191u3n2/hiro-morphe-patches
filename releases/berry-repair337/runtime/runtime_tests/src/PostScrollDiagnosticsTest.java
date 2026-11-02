import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import hiro.berrybfcache.PostScrollDiagnostics;
import hiro.berrybfcache.PresentationRecovery;
import hiro.berrybfcache.RenderDiagnostics;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public final class PostScrollDiagnosticsTest {
    private static int assertions, scenarios;
    private static final String URL="https://private.example/secret?input=never-export";
    private static final String VALID="[1,12.5,360,700,0,1200,3,360,700,360,6000,1,360,700,0,0,0,1200,1,3,3,1,1,1,0,0,1,7,1,0]";
    private static final class Guard implements PresentationRecovery.Proof {
        boolean valid=true;
        public boolean isCurrent(WebView view){return valid;}
    }
    private static void check(boolean ok,String reason){assertions++;if(!ok)throw new AssertionError(reason);}
    private static WebView fresh(){return new WebView(URL);}
    private static void start(WebView v,Guard g){PostScrollDiagnostics.start(v,g);v.flushPosts();}
    private static void answer(WebView v,int index){v.answerNumeric(index,VALID);v.flushPosts();}
    private static void scroll(WebView v,int dy){v.scrollY+=dy;v.observer.dispatchOnScrollChanged();v.flushPosts();}
    private static String description(WebView v){return PostScrollDiagnostics.describe(v);}
    private static String parse(String value)throws Exception{
        Method m=PostScrollDiagnostics.class.getDeclaredMethod("parse",String.class);m.setAccessible(true);return (String)m.invoke(null,value);
    }
    private static void inert(WebView v){check(v.invalidations==0&&v.layoutRequests==0&&v.visuals.isEmpty(),"numeric diagnosis never requests rendering or layout");}
    public static void main(String[] args)throws Exception{
        Field script=PostScrollDiagnostics.class.getDeclaredField("SCRIPT");script.setAccessible(true);
        if(args.length>0){Files.createDirectories(Paths.get(args[0]));Files.write(Paths.get(args[0],"post_scroll.js"),((String)script.get(null)).getBytes(StandardCharsets.UTF_8));
            Method guarded=PostScrollDiagnostics.class.getDeclaredMethod("scriptFor",String.class);guarded.setAccessible(true);
            Files.write(Paths.get(args[0],"post_scroll_guarded.js"),((String)guarded.invoke(null,"abcdefgh:7")).getBytes(StandardCharsets.UTF_8));}

        check(parse(VALID)!=null,"fixed finite numeric response accepted");
        String[] invalid={null,"null","{}","[]",VALID.replace("12.5","\"https://private.example/secret\""),
            VALID.replace("12.5","NaN"),VALID.replace("12.5","Infinity"),VALID.replace("12.5","1e999"),
            VALID.replace("12.5","true"),VALID.substring(0,VALID.length()-1)+",4]",VALID+"secret",
            "["+new String(new char[5000]).replace('\0','1')+"]"};
        for(String bad:invalid)check(parse(bad)==null,"malformed or content-bearing response rejected without exporting it");
        String canonical=parse(VALID);check(!canonical.contains("private")&&!canonical.contains("URL"),"accepted response cannot acquire page strings");scenarios++;

        WebView v=fresh();Guard g=new Guard();start(v,g);
        check(v.numericReads.size()==1,"confirmed restore requests one numeric baseline");
        check(v.reads.isEmpty(),"diagnostic probe does not enter coordinator marker channel in harness");
        check(v.observer.scrolls.size()==1,"diagnostic uses one additive observer");
        v.advance(37);v.scrollY=90;answer(v,0);
        String report=description(v);check(report.contains("SAMPLE_OK")&&report.contains("delayMs=37"),"result carries native request-to-callback delay");
        check(report.contains("moved=true"),"movement during asynchronous evaluation is explicit");
        check(!report.contains("private.example")&&!report.contains("never-export"),"sample report never includes URL");inert(v);PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);answer(v,0);
        final int[] other={0};ViewTreeObserver.OnScrollChangedListener nativeListener=new ViewTreeObserver.OnScrollChangedListener(){public void onScrollChanged(){other[0]++;}};
        v.observer.addOnScrollChangedListener(nativeListener);
        for(int i=0;i<100;i++)scroll(v,1);
        check(other[0]==100,"all existing observer callbacks continue receiving scrolls");
        check(v.numericReads.size()==2,"many scroll callbacks coalesce first-scroll probe");answer(v,1);
        v.advance(199);check(v.numericReads.size()==2,"settled sample waits for quiet interval");v.advance(1);
        check(v.numericReads.size()==3,"quiet interval requests one settled sample");answer(v,2);
        for(int i=0;i<20;i++){scroll(v,10);v.advance(200);if(v.numericReads.size()>3+i)answer(v,v.numericReads.size()-1);}
        check(v.numericReads.size()<=5,"baseline first-scroll and settled reads have finite total budget");
        check(v.observer.scrolls.contains(nativeListener),"diagnostic cleanup preserves existing observers");
        check(description(v).contains("SCROLL_LIMIT"),"automatic collection limit is visible");inert(v);PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);v.advance(800);
        check(description(v).contains("JS_TIMEOUT"),"lost callback has finite watchdog");
        final int[] done={0};PostScrollDiagnostics.inspect(v,new Runnable(){public void run(){done[0]++;}});
        v.flushPosts();check(v.numericReads.size()==1,"timeout keeps outstanding native lease rather than stacking requests");
        check(description(v).contains("JS_BUSY")&&done[0]==1,"manual inspection reports busy and finishes without waiting forever");
        for(int i=0;i<5;i++){PostScrollDiagnostics.start(v,new Guard());v.advance(1000);}
        check(v.numericReads.size()==1,"new restore generations cannot accumulate hung JS requests");
        answer(v,0);int previous=v.numericReads.size();PostScrollDiagnostics.inspect(v,null);v.flushPosts();
        check(v.numericReads.size()==previous+1,"real late callback releases native lease for later explicit request");
        answer(v,v.numericReads.size()-1);inert(v);PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);g.valid=false;answer(v,0);
        check(!description(v).contains("SAMPLE_OK"),"proof invalidation rejects callback result even at same URL");
        check(v.observer.scrolls.isEmpty(),"stale proof removes postscroll observation");inert(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);PostScrollDiagnostics.cancel(v);start(v,new Guard());
        check(v.numericReads.size()==1,"navigation cancellation retains un-cancellable lease");answer(v,0);
        check(!description(v).contains("SAMPLE_OK"),"old same-URL generation callback cannot populate new report");
        PostScrollDiagnostics.inspect(v,null);v.flushPosts();check(v.numericReads.size()==2,"new generation can sample after old callback has returned");
        answer(v,1);check(description(v).contains("SAMPLE_OK"),"new generation publishes its own result");PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);v.detach();answer(v,0);
        check(v.observer.scrolls.isEmpty()&&v.timers.isEmpty(),"detach removes scroll listener and own timers");
        check(!description(v).contains("SAMPLE_OK"),"detached callback cannot be published");inert(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);answer(v,0);v.advance(45000);
        check(v.observer.scrolls.isEmpty()&&v.timers.isEmpty(),"automatic collection expires without polling forever");
        check(description(v).contains("AUTO_DEADLINE"),"automatic deadline is visible");
        check(v.attachListeners.isEmpty(),"automatic deadline removes attachment observer too");
        int previousReads=v.numericReads.size();PostScrollDiagnostics.inspect(v,null);v.flushPosts();
        check(v.numericReads.size()==previousReads+1,"explicit user inspection still samples current proven page after auto deadline");
        answer(v,previousReads);inert(v);PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();v.throwOnEvaluate=true;g=new Guard();start(v,g);
        check(description(v).contains("JS_EXCEPTION"),"optional evaluation failure is recorded without crashing recovery");
        v.throwOnEvaluate=false;PostScrollDiagnostics.inspect(v,null);v.flushPosts();
        check(v.numericReads.size()==1,"synchronous evaluate failure does not poison native lease");answer(v,0);PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);v.answerNumeric(0,"[\"private secret\"]");v.flushPosts();
        check(description(v).contains("JS_REJECTED")&&!description(v).contains("private secret"),"malicious callback is rejected without retaining its content");
        PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);answer(v,0);
        for(int i=0;i<200;i++){v.observer.dispatchOnScrollChanged();v.flushPosts();}
        v.advance(200);check(v.numericReads.size()==1,"shared tree observer notifications without WebView movement do not create samples");
        check(description(v).contains("scrollEvents=0"),"foreign scroll callbacks do not pollute WebView movement count");
        PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);answer(v,0);
        for(int i=0;i<40;i++){PostScrollDiagnostics.inspect(v,null);answer(v,v.numericReads.size()-1);}
        int rows=0;for(String line:description(v).split("\n"))if(line.startsWith("+"))rows++;
        check(rows<=8,"repeated explicit inspections retain at most eight sample rows");
        PostScrollDiagnostics.cancel(v);scenarios++;

        v=fresh();g=new Guard();start(v,g);v.throwOnScrollRead=true;
        answer(v,0);v.observer.dispatchOnScrollChanged();
        PostScrollDiagnostics.cancel(v);check(v.timers.isEmpty(),"getter failure cannot escape callback or prevent timer cleanup");scenarios++;

        v=fresh();g=new Guard();start(v,g);v.observer.throwOnRemoveScroll=true;v.throwOnRemoveAttach=true;
        PostScrollDiagnostics.cancel(v);check(v.timers.isEmpty(),"observer cleanup exceptions do not prevent request watchdog cleanup");
        answer(v,0);check(!description(v).contains("SAMPLE_OK"),"late callbacks stay rejected after cleanup exception");scenarios++;

        v=fresh();g=new Guard();start(v,g);
        PostScrollDiagnostics.inspect(v,new Runnable(){public void run(){throw new IllegalStateException("optional UI completion failure");}});
        answer(v,0);PostScrollDiagnostics.inspect(v,new Runnable(){public void run(){throw new IllegalStateException("optional UI completion failure");}});
        answer(v,1);PostScrollDiagnostics.cancel(v);
        PostScrollDiagnostics.inspect(v,new Runnable(){public void run(){throw new IllegalStateException("optional UI completion failure");}});
        inert(v);scenarios++;

        for(Class<?> c:PostScrollDiagnostics.class.getDeclaredClasses())for(Field f:c.getDeclaredFields())
            check(f.getType()!=WebView.class&&f.getType()!=View.class&&f.getType()!=ViewTreeObserver.class,"diagnostic state cannot strongly retain View or shared observer");scenarios++;

        Activity a=new Activity();v=new WebView(a,URL);((FrameLayout)a.getWindow().getDecorView()).addView(v,new FrameLayout.LayoutParams(-1,-1,0));
        RenderDiagnostics.begin(v);start(v,new Guard());answer(v,0);
        Field badges=RenderDiagnostics.class.getDeclaredField("BADGES");badges.setAccessible(true);Object badge=((Map<?,?>)badges.get(null)).get(a);
        Field label=badge.getClass().getDeclaredField("label");label.setAccessible(true);TextView control=(TextView)((WeakReference<?>)label.get(badge)).get();
        AlertDialog.latest=null;control.performClick();v.flushPosts();
        check(v.numericReads.size()==2,"badge opening requests current numeric inspection");
        check(a.clipboard.writes==0,"inspection does not automatically copy");v.advance(15);v.scrollY=777;answer(v,1);v.flushPosts();
        AlertDialog dialog=AlertDialog.latest;check(dialog!=null&&!dialog.dismissed,"report dialog remains available after asynchronous sample");
        dialog.clickPositive();check(a.clipboard.writes==1,"only explicit copy writes clipboard");
        String copied=a.clipboard.clip.text.toString();check(copied.contains("SAMPLE_OK")&&copied.contains("delayMs=15"),"copy uses report containing completed asynchronous inspection");
        check(!copied.contains("private.example")&&!copied.contains("never-export"),"asynchronous copy preserves privacy");
        RenderDiagnostics.cancel(v);PostScrollDiagnostics.cancel(v);scenarios++;
        System.out.println("{\"result\":\"PASS\",\"scenarios\":"+scenarios+",\"assertions\":"+assertions+",\"android_device_tested\":false,\"renderer_behavior_tested\":false}");
    }
}
