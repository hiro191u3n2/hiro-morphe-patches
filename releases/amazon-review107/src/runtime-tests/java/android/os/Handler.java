package android.os;
import java.util.ArrayList;
import java.util.List;
public class Handler {
    private static final class Item { Runnable action; long when, order; Item(Runnable r,long t,long o){action=r;when=t;order=o;} }
    private static final List<Item> QUEUE = new ArrayList<Item>();
    private static long now, serial;
    public static boolean rejectPosts;
    public Handler(Looper ignored) {}
    public boolean post(Runnable action) { return postDelayed(action, 0); }
    public boolean postDelayed(Runnable action,long delay) { if(rejectPosts)return false; QUEUE.add(new Item(action, now+delay, serial++)); return true; }
    public void removeCallbacks(Runnable action) { QUEUE.removeIf(item -> item.action == action); }
    public static long clock() { return now; }
    public static int pendingCount() { return QUEUE.size(); }
    public static void reset() { QUEUE.clear(); now=0; serial=0; rejectPosts=false; Looper.main=true; }
    public static void runNext() { if(QUEUE.isEmpty())return; Item next=QUEUE.get(0); for(Item item:QUEUE)if(item.when<next.when||item.when==next.when&&item.order<next.order)next=item; QUEUE.remove(next); now=next.when; boolean before=Looper.main; Looper.main=true; try{next.action.run();}finally{Looper.main=before;} }
}
