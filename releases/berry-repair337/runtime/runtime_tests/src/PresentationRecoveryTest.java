import android.webkit.WebView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import hiro.berrybfcache.PresentationRecovery;
import hiro.berrybfcache.RenderDiagnostics;
import java.lang.reflect.Field;
import java.util.Map;

public final class PresentationRecoveryTest {
    private static int assertions, scenarios;
    private static final String URL="https://example.test/page";
    private static final class Guard implements PresentationRecovery.Proof {
        boolean valid=true;
        public boolean isCurrent(WebView view){return valid;}
    }
    private static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    private static void start(WebView v,Guard g){PresentationRecovery.start(v,g);v.flushPosts();}
    private static void stable(WebView v){v.getViewTreeObserver().dispatchOnPreDraw();v.getViewTreeObserver().dispatchOnPreDraw();}
    private static void finish(WebView v,int index){v.draw(index);v.getViewTreeObserver().dispatchOnDraw();v.flushPosts();}
    private static boolean active(WebView v)throws Exception{
        Field f=PresentationRecovery.class.getDeclaredField("STATES");f.setAccessible(true);return ((Map<?,?>)f.get(null)).containsKey(v);
    }
    private static boolean event(WebView v,String phase){return RenderDiagnostics.report(v).contains("ms "+phase+" ");}
    private static void clean(WebView v)throws Exception{
        check(!active(v),"recovery state removed");check(v.timers.isEmpty(),"own watchdog removed");
        check(v.observer.preDraws.isEmpty()&&v.observer.draws.isEmpty(),"observers removed");check(v.attachListeners.isEmpty(),"attach listener removed");
    }
    public static void main(String[] args)throws Exception{
        WebView v=new WebView(URL);Guard g=new Guard();start(v,g);
        check(v.visuals.isEmpty()&&v.invalidations==1,"start requests framework traversal, not premature visual proof");
        v.observer.dispatchOnPreDraw();check(v.visuals.isEmpty(),"one pre-draw is not established geometry");
        v.observer.dispatchOnPreDraw();check(v.visuals.size()==1,"second stable pre-draw requests visual barrier");
        v.draw(0);check(active(v)&&event(v,"VISUAL_READY"),"visual readiness alone does not claim draw observed");
        v.observer.dispatchOnDraw();check(active(v),"onDraw does not mutate views or listener registrations inline");
        v.flushPosts();check(event(v,"DRAW_TRAVERSAL"),"draw traversal recorded after callback");clean(v);scenarios++;

        v=new WebView(URL);v.attached=false;g=new Guard();start(v,g);
        v.advance(500);check(v.visuals.isEmpty()&&v.invalidations==0,"detached recovery waits without draw requests");
        v.attach();stable(v);finish(v,0);clean(v);scenarios++;

        v=new WebView(URL);v.shown=false;g=new Guard();start(v,g);v.advance(400);
        check(v.visuals.isEmpty(),"hidden recovery does not create visual requests");v.shown=true;v.advance(100);stable(v);finish(v,0);clean(v);scenarios++;

        v=new WebView(URL);v.width=v.height=v.measuredWidth=v.measuredHeight=0;g=new Guard();start(v,g);v.advance(200);
        check(v.layoutRequests==0&&v.visuals.isEmpty(),"unknown zero size is not replaced with invented parent dimensions");
        v.completeLayout(360,720);v.advance(100);stable(v);finish(v,0);clean(v);scenarios++;

        v=new WebView(URL);v.measuredWidth=420;g=new Guard();start(v,g);
        check(v.layoutRequests==1&&v.layoutRequested&&v.width==400,"actual/measured mismatch requests original layout without assigning dimensions");
        v.completeLayout(420,800);v.advance(100);stable(v);finish(v,0);clean(v);scenarios++;

        v=new WebView(URL);ViewGroup parent=new ViewGroup();parent.width=900;parent.height=1200;v.parent=parent;
        v.completeLayout(300,500);v.layoutParams=new ViewGroup.LayoutParams(300,500);g=new Guard();start(v,g);stable(v);finish(v,0);
        check(v.layoutRequests==0&&v.width==300&&v.height==500,"legitimate smaller child is not stretched to fill parent");clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);v.observer.dispatchOnPreDraw();v.completeLayout(410,800);v.observer.dispatchOnPreDraw();
        check(v.visuals.isEmpty(),"changing layout restarts stable-frame sampling");v.observer.dispatchOnPreDraw();finish(v,0);clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);v.advance(700);v.flushPosts();
        check(event(v,"VISUAL_TIMEOUT")&&event(v,"DRAW_FALLBACK_REQUESTED"),"lost callback records timeout and one fallback opportunity");
        check(v.visuals.size()==1,"timeout never queues duplicate uncancellable native requests");clean(v);
        int before=v.invalidations;v.draw(0);check(v.invalidations==before&&!active(v),"late expired callback cannot redraw");scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);v.advance(700);v.flushPosts();
        for(int n=0;n<6;n++){g=new Guard();start(v,g);stable(v);v.advance(3000);v.flushPosts();check(!active(v),"blocked lease recovery has finite deadline");}
        check(v.visuals.size()==1&&v.timers.isEmpty(),"repeated restores cannot accumulate lost native callbacks");
        check(event(v,"PRESENTATION_DEADLINE")&&event(v,"VISUAL_BUSY"),"blocked callback state is observable");scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);Guard next=new Guard();start(v,next);stable(v);
        check(v.visuals.size()==1,"new generation waits for prior native lease");
        int invalid=v.invalidations;v.draw(0);check(v.visuals.size()==2&&v.invalidations==invalid,"old callback releases lease without drawing new generation");
        finish(v,1);clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);g.valid=false;v.draw(0);v.flushPosts();
        check(!event(v,"VISUAL_READY"),"stale proof rejects visual callback");clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);v.draw(0);g.valid=false;v.observer.dispatchOnDraw();v.flushPosts();
        check(event(v,"PRESENTATION_STALE"),"proof becoming stale during draw cleans up outside onDraw");clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);v.draw(0);v.measuredWidth=450;v.observer.dispatchOnDraw();
        check(v.layoutRequests==0,"geometry change observed during draw does not request layout inline");v.flushPosts();
        check(v.layoutRequests==1,"post-draw geometry mismatch requests layout safely");v.completeLayout(450,800);v.advance(100);stable(v);finish(v,1);clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);v.draw(0);v.observer.dispatchOnDraw();
        int posts=v.posts.size();PresentationRecovery.cancel(v);check(v.posts.size()<posts,"cancel removes queued post-draw finalizer");v.flushPosts();
        check(!event(v,"DRAW_TRAVERSAL"),"cancelled finalizer cannot report traversal success");clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);ViewTreeObserver old=v.observer;v.detach();
        check(old.preDraws.isEmpty()&&old.draws.isEmpty(),"detach releases old live observer registrations");
        v.draw(0);check(!event(v,"VISUAL_READY"),"detached callback is not ready proof for display");v.attach();stable(v);finish(v,1);clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);v.shown=false;v.advance(100);v.draw(0);v.shown=true;v.advance(100);stable(v);finish(v,1);
        check(v.visuals.size()==2,"show after interrupted callback gets a fresh visual barrier");clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);v.draw(0);v.shown=false;v.observer.dispatchOnDraw();
        v.shown=true;v.flushPosts();check(!event(v,"DRAW_TRAVERSAL"),"hidden-at-traversal cannot become success merely by showing before post runs");
        stable(v);finish(v,1);clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);v.draw(0);v.advance(700);stable(v);v.draw(1);v.advance(700);v.flushPosts();
        check(v.visuals.size()==2&&event(v,"DRAW_TIMEOUT"),"missing draw traversal retries are bounded");clean(v);scenarios++;

        v=new WebView(URL);v.layoutRequested=true;g=new Guard();start(v,g);v.advance(5000);v.flushPosts();
        check(v.visuals.isEmpty()&&event(v,"PRESENTATION_DEADLINE"),"permanent pending layout expires");clean(v);scenarios++;

        v=new WebView(URL);v.measuredWidth=500;g=new Guard();start(v,g);v.layoutRequested=false;v.advance(100);v.layoutRequested=false;v.advance(4000);v.flushPosts();
        check(v.layoutRequests==2,"layout inconsistency correction has a fixed request limit");clean(v);scenarios++;

        v=new WebView(URL);v.throwOnVisual=true;g=new Guard();start(v,g);stable(v);v.flushPosts();
        check(event(v,"PRESENTATION_EXCEPTION"),"optional visual API failure terminates presentation only");clean(v);v.throwOnVisual=false;start(v,new Guard());stable(v);finish(v,0);clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();android.os.Looper.background=true;PresentationRecovery.start(v,g);
        check(v.visuals.isEmpty()&&v.posts.size()==1,"background start marshals to UI");v.flushPosts();stable(v);finish(v,0);clean(v);scenarios++;

        v=new WebView(URL);g=new Guard();start(v,g);stable(v);int count=v.timers.size();for(int n=0;n<100;n++)v.observer.dispatchOnPreDraw();
        check(v.timers.size()==count&&v.visuals.size()==1,"many pre-draw callbacks coalesce watchdog and visual requests");PresentationRecovery.cancel(v);clean(v);scenarios++;

        for(Class<?> c:PresentationRecovery.class.getDeclaredClasses())for(Field f:c.getDeclaredFields())
            check(f.getType()!=WebView.class&&f.getType()!=View.class&&f.getType()!=ViewTreeObserver.class,"state does not strongly retain Android view or shared observer");
        scenarios++;
        System.out.println("{\"result\":\"PASS\",\"scenarios\":"+scenarios+",\"assertions\":"+assertions+",\"device_tested\":false}");
    }
}
