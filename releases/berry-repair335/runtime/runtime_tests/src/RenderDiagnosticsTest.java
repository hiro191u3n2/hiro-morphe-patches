import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import hiro.berrybfcache.RenderDiagnostics;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public final class RenderDiagnosticsTest {
    private static int scenarios, assertions;
    private static void check(boolean ok,String message) { assertions++; if(!ok)throw new AssertionError(message); }
    private static Object field(Object o,String name) throws Exception {
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);
    }
    private static Object state(String name) throws Exception {
        Field f=RenderDiagnostics.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);
    }
    private static void reset() throws Exception {
        ((Collection<?>)state("TIMELINES")).clear();((Map<?,?>)state("BADGES")).clear();((Map<?,?>)state("SHOW_PENDING")).clear();
        Field f=RenderDiagnostics.class.getDeclaredField("hiddenUntilRestart");f.setAccessible(true);f.setBoolean(null,false);
        android.os.Looper.background=false;android.os.SystemClock.now=0;AlertDialog.latest=null;AlertDialog.shows=0;scenarios++;
    }
    private static WebView view(Activity a) {
        WebView v=new WebView(a,"https://private.example/secret?password=never-log-this");
        ((FrameLayout)a.getWindow().getDecorView()).addView(v,new FrameLayout.LayoutParams(-1,-1,0));return v;
    }
    private static Object badge(Activity a) throws Exception { return ((Map<?,?>)state("BADGES")).get(a); }
    private static TextView label(Activity a) throws Exception { return (TextView)((WeakReference<?>)field(badge(a),"label")).get(); }
    private static int events(String report) { int count=0;for(String s:report.split("\n"))if(s.startsWith("+"))count++;return count; }

    public static void main(String[] args) throws Exception {
        reset();
        Activity a=new Activity();WebView v=view(a);ViewGroup.LayoutParams original=v.getLayoutParams();
        v.scrollX=7;v.scrollY=1234;v.scale=1.25f;
        RenderDiagnostics.begin(v);RenderDiagnostics.record(v,"PRESENTATION_START");
        check(badge(a)==null,"badge must be deferred outside restore/draw callback");
        check(v.posts.size()==1,"begin and start coalesce a single badge post");v.flushPosts();
        check(label(a)!=null&&label(a).isShown(),"confirmed restore exposes native report control");
        String report=RenderDiagnostics.report(v);
        check(report.contains("Restores=1"),"begin and engine start count one restore");
        check(report.contains("scroll=7,1234")&&report.contains("scale=1.25"),"native position and scale observed");
        check(report.contains("parent=")&&report.contains("measured=")&&report.contains("bounds=")&&report.contains("padding="),"parent and measured geometry recorded");
        check(!report.contains("private.example")&&!report.contains("never-log-this"),"report omits private URL");
        check(v.getLayoutParams()==original&&v.layoutRequests==0&&v.invalidations==0,"diagnosis does not mutate web layout or request a frame");
        check(v.reads.isEmpty()&&v.visuals.isEmpty()&&v.timers.isEmpty(),"diagnosis does not run JS, visual probes or timers");
        check(a.clipboard.writes==0&&AlertDialog.shows==0,"no automatic copying or dialog");

        reset();a=new Activity();v=view(a);RenderDiagnostics.begin(v);
        for(int i=0;i<200;i++)RenderDiagnostics.record(v,"VISUAL_BUSY");
        RenderDiagnostics.record(v,"https://private.example/secret");RenderDiagnostics.record(v,"PERSONAL_SECRET");
        report=RenderDiagnostics.report(v);
        check(events(report)==64,"per-tab event ring is bounded to64");
        check(report.contains("EVENT_REDACTED")&&!report.contains("PERSONAL_SECRET")&&!report.contains("private.example"),"unknown phases cannot export page strings");
        check(report.length()<9000,"unchanged geometry is compacted in repetitive traces");
        check(v.posts.size()==1,"repetitive diagnostic records do not accumulateUI jobs");

        reset();WebView[] many=new WebView[6];
        for(int i=0;i<many.length;i++){many[i]=view(new Activity());RenderDiagnostics.begin(many[i]);}
        check(((Collection<?>)state("TIMELINES")).size()==4,"at most4 live timeline records");
        check(((Map<?,?>)state("SHOW_PENDING")).size()==4,"at most4 queued UI jobs");
        check(many[0].posts.isEmpty()&&many[1].posts.isEmpty(),"eviction removes previous queued UI work");
        check(RenderDiagnostics.report(many[0]).contains("Restores=0"),"evicted timeline does not retain data");
        for(Object t:(Collection<?>)state("TIMELINES"))check(field(t,"view") instanceof WeakReference,"timeline only holds weakWebView");

        reset();a=new Activity();v=view(a);RenderDiagnostics.begin(v);v.flushPosts();
        label(a).performClick();AlertDialog opened=AlertDialog.latest;
        check(opened!=null&&AlertDialog.shows==1,"badge tap opens report");
        check(a.clipboard.writes==0,"report display alone doesnotcopy");
        opened.clickPositive();check(a.clipboard.writes==1,"explicit positivebutton copies once");
        check(a.clipboard.clip.text.toString().contains("DRAW_TRAVERSAL")&&!a.clipboard.clip.text.toString().contains("private.example"),"copied record retains limitations and privacy");
        check(v.reads.isEmpty()&&v.visuals.isEmpty()&&v.layoutRequests==0,"inspection itself doesnotchange page or draw readiness");
        for(Field f:badge(a).getClass().getDeclaredFields())check(!View.class.isAssignableFrom(f.getType())&&!Activity.class.isAssignableFrom(f.getType()),"badge doesnotstronglyownUI object");

        reset();a=new Activity();v=view(a);RenderDiagnostics.begin(v);RenderDiagnostics.cancel(v);v.flushPosts();
        check(badge(a)==null&&((Map<?,?>)state("SHOW_PENDING")).isEmpty(),"newnavigation cancels queued badge");
        RenderDiagnostics.begin(v);v.flushPosts();label(a).performClick();opened=AlertDialog.latest;RenderDiagnostics.cancel(v);
        check(opened.dismissed&&!label(a).isShown(),"newnavigation dismisses report and hidesbadge");
        opened.clickPositive();check(a.clipboard.writes==0,"stale dialog cannotcopy aftercancellation");
        RenderDiagnostics.begin(v);v.flushPosts();opened.clickNeutral();
        check(label(a).isShown(),"stale dialogcannot hide subsequentrestoration");
        check(RenderDiagnostics.report(v).contains("NAVIGATION_CANCEL"),"cancellation keeps timelineevidence");

        reset();a=new Activity();v=view(a);RenderDiagnostics.begin(v);v.flushPosts();label(a).performClick();AlertDialog.latest.clickNeutral();
        check(!label(a).isShown(),"explicit hidehidesnativebadge");
        Activity replacement=new Activity();WebView next=view(replacement);RenderDiagnostics.begin(next);next.flushPosts();
        check(badge(replacement)==null,"hide survivesActivityrecreation in sameprocess");
        check(RenderDiagnostics.report(next).contains("Restores=1"),"hidingUI doesnot stopboundedrecords");

        reset();a=new Activity();v=view(a);v.shown=false;RenderDiagnostics.begin(v);v.flushPosts();
        check(badge(a)==null,"hiddenrestore doesnotshowunrelatedoverlay");
        v.shown=true;RenderDiagnostics.record(v,"VISUAL_TIMEOUT");v.flushPosts();
        check(label(a).isShown(),"latevisible timeoutstillprovidesreportaccess");
        label(a).performClick();opened=AlertDialog.latest;v.detach();
        check(!label(a).isShown()&&opened.dismissed,"targetdetach hidesanddismissesUI");
        check(((WeakReference<?>)field(badge(a),"target")).get()==null,"detach releasesactive target");
        check(!RenderDiagnostics.report(v).contains("private.example"),"detachedsnapshot remainsprivate");

        reset();a=new Activity();a.finishing=true;v=view(a);RenderDiagnostics.begin(v);v.flushPosts();
        check(badge(a)==null,"finishingActivity doesnotgainoverlay");
        WebView plain=new WebView(new Context(),"secret");RenderDiagnostics.begin(plain);plain.flushPosts();
        check(((Map<?,?>)state("BADGES")).isEmpty(),"nonActivity context doesnotcrash or attachUI");
        android.os.Looper.background=true;int before=((Collection<?>)state("TIMELINES")).size();
        RenderDiagnostics.record(new WebView("secret"),"PRESENTATION_START");
        check(((Collection<?>)state("TIMELINES")).size()==before,"backgroundcalls donotmutateUIstate");
        check(RenderDiagnostics.report(v).contains("UI thread unavailable"),"backgroundreportdoesnotaccessview");android.os.Looper.background=false;

        reset();a=new Activity();v=view(a);RenderDiagnostics.begin(v);v.flushPosts();
        v.width=250;v.measuredWidth=300;v.scrollY=400;RenderDiagnostics.record(v,"WAIT_LAYOUT");
        report=RenderDiagnostics.report(v);
        check(report.contains("view=400x800")&&report.contains("view=250x800")&&report.contains("measured=300x800"),"changinggeometryretains beforeafterevidence");
        check(report.contains("scroll=0,400"),"scrollchangesrecorded");
        final WebView drawView=v;
        v.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener(){public void onDraw(){RenderDiagnostics.record(drawView,"DRAW_TRAVERSAL");}});
        v.getViewTreeObserver().dispatchOnDraw();
        check(v.posts.size()==1,"OnDrawdiagnostic defersoverlaymutation");v.flushPosts();
        check(label(a).isShown()&&v.layoutRequests==0,"deferredui leaveswebgeometryuntouched");

        reset();a=new Activity();v=new WebView(new Context(),"secret-application-context");
        ((FrameLayout)a.getWindow().getDecorView()).addView(v,new FrameLayout.LayoutParams(-1,-1,0));
        RenderDiagnostics.begin(v);v.flushPosts();
        check(label(a).isShown(),"Application-context WebView resolves its Activity parent forreportaccess");
        RenderDiagnostics.cancel(v);check(!label(a).isShown(),"parent-context lookup also cancelsoldUI");
        WebView cycle=new WebView(new Context(),"secret");cycle.parent=cycle;
        RenderDiagnostics.begin(cycle);cycle.flushPosts();
        check(((Map<?,?>)state("BADGES")).size()==1,"cyclicparentcontextlookup terminateswithoutaddingUI");

        reset();Activity[] activities=new Activity[6];
        for(int i=0;i<activities.length;i++){activities[i]=new Activity();v=view(activities[i]);RenderDiagnostics.begin(v);v.flushPosts();}
        check(((Map<?,?>)state("BADGES")).size()==4,"liveActivitybadges alsoboundedto4");
        int removed=0;for(Activity current:activities)if(badge(current)==null)removed++;
        check(removed==2,"badgeevictionremovesoldnativecontrols");
        for(Object b:((Map<?,?>)state("BADGES")).values()){
            check(field(b,"activity") instanceof WeakReference&&field(b,"label") instanceof WeakReference&&field(b,"target") instanceof WeakReference,"badgeownership staysweak");
        }
        System.out.println("{\"result\":\"PASS\",\"scenarios\":"+scenarios+",\"assertions\":"+assertions+",\"bounded_events_per_tab\":64,\"bounded_tabs\":4,\"clipboard_requires_user_action\":true,\"page_content_collected\":false,\"android_device_tested\":false}");
    }
}
