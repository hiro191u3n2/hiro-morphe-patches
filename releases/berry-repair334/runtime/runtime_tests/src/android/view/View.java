package android.view;
import java.util.ArrayList;
public class View {
    public boolean attached = true;
    public boolean shown = true;
    public int invalidations;
    public long now;
    public final ArrayList<Runnable> posts = new ArrayList<Runnable>();
    public static final class Timer { public Runnable r; public long due; Timer(Runnable r,long due){this.r=r;this.due=due;} }
    public final ArrayList<Timer> timers = new ArrayList<Timer>();
    public boolean post(Runnable runnable) { posts.add(runnable); return true; }
    public boolean postDelayed(Runnable runnable,long delay) { timers.add(new Timer(runnable,now+delay));return true; }
    public void flushPosts() {
        android.os.Looper.background = false;
        while (!posts.isEmpty()) posts.remove(0).run();
    }
    public void advance(long delta) {
        long target=now+delta; int guard=1000;
        for (;;) {
            int pick=-1; long min=Long.MAX_VALUE;
            for(int i=0;i<timers.size();i++)if(timers.get(i).due<=target && timers.get(i).due<min){pick=i;min=timers.get(i).due;}
            if(pick<0)break;
            if(--guard==0)throw new AssertionError("timer spin");
            Timer t=timers.remove(pick);now=t.due;t.r.run();
        }
        now=target;
    }
    public boolean isAttachedToWindow() { return attached; }
    public boolean isShown() { return shown; }
    public void postInvalidateOnAnimation() { invalidations++; }
}
