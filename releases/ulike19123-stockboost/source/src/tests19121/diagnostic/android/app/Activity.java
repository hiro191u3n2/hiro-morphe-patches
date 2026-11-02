package android.app;

import java.util.ArrayDeque;

/** Controlled UI dispatch: enqueued callbacks run only when the test drains one. */
public class Activity extends android.content.Context {
    public volatile boolean focused=true,finishing,destroyed,failPost,failState;
    public int postAttempts,maxQueued;
    private final ArrayDeque<Runnable> posted=new ArrayDeque<Runnable>();
    public boolean hasWindowFocus(){if(failState)throw new LinkageError("fixture unavailable state API");return focused;}
    public boolean isFinishing(){if(failState)throw new LinkageError("fixture unavailable state API");return finishing;}
    public boolean isDestroyed(){if(failState)throw new LinkageError("fixture unavailable state API");return destroyed;}
    public synchronized void runOnUiThread(Runnable work){
        postAttempts++;
        if(failPost)throw new IllegalStateException("fixture post rejection");
        posted.addLast(work);maxQueued=Math.max(maxQueued,posted.size());
    }
    public synchronized int queued(){return posted.size();}
    public void runNext(){Runnable next;synchronized(this){next=posted.removeFirst();}next.run();}
}
