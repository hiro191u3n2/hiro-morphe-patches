package android.os;
import java.util.ArrayList;
import java.util.List;
public class Handler {
    private static final class Job { final Runnable action; final long at; Job(Runnable r,long t){action=r;at=t;} }
    private static final List<Job> QUEUE = new ArrayList<Job>();
    public Handler(Looper looper) { }
    public boolean post(Runnable action) { return postDelayed(action,0); }
    public boolean postDelayed(Runnable action,long delay){synchronized(QUEUE){QUEUE.add(new Job(action,SystemClock.time+Math.max(0,delay)));}return true;}
    public void removeCallbacks(Runnable action){synchronized(QUEUE){QUEUE.removeIf(j->j.action==action);}}
    public static void clear(){synchronized(QUEUE){QUEUE.clear();}}
    public static void advanceBy(long millis){SystemClock.time+=millis;runDue();}
    public static void runDue(){int remaining=1000;for(;;){Job job=null;synchronized(QUEUE){for(Job j:QUEUE)if(j.at<=SystemClock.time&&(job==null||j.at<job.at))job=j;if(job!=null)QUEUE.remove(job);}if(job==null)return;if(--remaining==0)throw new AssertionError("unbounded posted work");job.action.run();}}
    public static void drain() {
        int remaining=1000;while(queuedCount()>0){if(--remaining==0)throw new AssertionError("unbounded posted work");long at=Long.MAX_VALUE;synchronized(QUEUE){for(Job j:QUEUE)at=Math.min(at,j.at);}SystemClock.time=Math.max(SystemClock.time,at);runDue();}
    }
    public static int queuedCount() { synchronized (QUEUE) { return QUEUE.size(); } }
}
