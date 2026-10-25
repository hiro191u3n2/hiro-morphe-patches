package android.os;
import java.util.*;
public class Handler {
 private static long sequence;
 private static final List<Task> queue=new ArrayList<Task>();
 private static final class Task {
  final Handler owner;final Runnable run;final long time,order;
  Task(Handler owner,Runnable run,long delay){this.owner=owner;this.run=run;time=SystemClock.now+delay;order=++sequence;}
 }
 public volatile boolean accept=true,held;public int posts;private final Looper looper;
 public Handler(){this(new Looper());}public Handler(Looper looper){this.looper=looper;}
 public Looper getLooper(){return looper;}public boolean post(Runnable run){return postDelayed(run,0L);}
 public boolean postDelayed(Runnable run,long delay){synchronized(queue){if(!accept)return false;posts++;queue.add(new Task(this,run,Math.max(0L,delay)));return true;}}
 public void removeCallbacks(Runnable run){synchronized(queue){for(Iterator<Task> it=queue.iterator();it.hasNext();){Task task=it.next();if(task.owner==this&&task.run==run)it.remove();}}}
 public static void reset(){synchronized(queue){queue.clear();sequence=0;}}
 public static int queued(){synchronized(queue){return queue.size();}}
 public static long nextTime(){synchronized(queue){long time=Long.MAX_VALUE;for(Task task:queue)if(!task.owner.held)time=Math.min(time,task.time);return time;}}
 public static void until(long time){
  int count=0;
  while(true){Task next=null;synchronized(queue){for(Task task:queue)if(!task.owner.held&&task.time<=time&&(next==null||task.time<next.time||task.time==next.time&&task.order<next.order))next=task;if(next!=null)queue.remove(next);}
   if(next==null)break;if(++count>10000)throw new AssertionError("unbounded handler loop");SystemClock.now=Math.max(SystemClock.now,next.time);
   Looper old=Looper.select(next.owner.looper);try{next.run.run();}finally{Looper.restore(old);}
  }
  SystemClock.now=time;
 }
}
