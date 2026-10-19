package android.os;
import java.util.*;
public class Handler {
    private static long sequence;
    private static final List<Task> queue = new ArrayList<>();
    private static final class Task {
        final Handler owner; final Runnable run; final long time, order;
        Task(Handler owner, Runnable run, long delay) {
            this.owner=owner; this.run=run; time=SystemClock.now+delay; order=++sequence;
        }
    }
    public boolean accept = true;
    public boolean held;
    public int posts;
    private final Looper looper;
    public Handler() { this(new Looper()); }
    public Handler(Looper looper) { this.looper=looper; }
    public Looper getLooper() { return looper; }
    public boolean post(Runnable run) { return postDelayed(run, 0L); }
    public boolean postDelayed(Runnable run, long delay) {
        if (!accept) return false;
        posts++; queue.add(new Task(this, run, Math.max(0L, delay))); return true;
    }
    public void removeCallbacks(Runnable run) {
        for (Iterator<Task> it=queue.iterator();it.hasNext();) {
            Task task=it.next(); if(task.owner==this && task.run==run)it.remove();
        }
    }
    public void removeCallbacksAndMessages(Object token) {
        for(Iterator<Task> it=queue.iterator();it.hasNext();)if(it.next().owner==this)it.remove();
    }
    public static void reset() { queue.clear(); sequence=0; Looper.current=Looper.getMainLooper(); }
    public static int queued() { return queue.size(); }
    public static void until(long time) {
        int count=0;
        while(true) {
            Task next=null;
            for(Task task:queue)if(!task.owner.held && task.time<=time && (next==null || task.time<next.time || task.time==next.time && task.order<next.order))next=task;
            if(next==null)break;
            if(++count>10000)throw new AssertionError("unbounded handler loop");
            queue.remove(next);SystemClock.now=Math.max(SystemClock.now,next.time);
            Looper old=Looper.current;Looper.current=next.owner.looper;
            try { next.run.run(); } finally { Looper.current=old; }
        }
        SystemClock.now=time;
    }
}
