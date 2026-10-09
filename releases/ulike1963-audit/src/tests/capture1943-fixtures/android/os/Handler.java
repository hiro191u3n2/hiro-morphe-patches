package android.os;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
public final class Handler {
    public boolean reject;
    private final Queue<Runnable> pending=new ConcurrentLinkedQueue<Runnable>();
    private final List<Scheduled> delayed=new ArrayList<Scheduled>();
    private static final class Scheduled {final Runnable run;final long time;Scheduled(Runnable r,long t){run=r;time=t;}}
    public boolean post(Runnable r){if(reject)return false;pending.add(r);return true;}
    public synchronized boolean postDelayed(Runnable r,long d){if(reject)return false;delayed.add(new Scheduled(r,SystemClock.now+d));return true;}
    public boolean one(){Runnable r=pending.poll();if(r==null)return false;r.run();return true;}
    public synchronized void timers(){for(int i=delayed.size()-1;i>=0;i--){Scheduled s=delayed.get(i);if(s.time<=SystemClock.now){delayed.remove(i);pending.add(s.run);}}}
    public boolean empty(){return pending.isEmpty();}
}

