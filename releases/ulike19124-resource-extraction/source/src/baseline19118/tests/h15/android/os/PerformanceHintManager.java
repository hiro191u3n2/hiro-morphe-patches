package android.os;
import java.util.ArrayList;
import java.util.List;
public final class PerformanceHintManager {
    public final List<Session> sessions=new ArrayList<Session>();
    public boolean unsupported,createFails,createOom,reportFails,closeFails,updateFails;
    public int attempts;
    public synchronized Session createHintSession(int[] tids,long target) {
        attempts++;
        if(target<=0 || tids==null || tids.length==0)throw new IllegalArgumentException();
        if(createOom)throw new OutOfMemoryError("optional service resource failure");
        if(createFails)throw new SecurityException("tid denied");
        if(unsupported)return null;
        Session session=new Session(this,tids.clone(),target);sessions.add(session);return session;
    }
    public static final class Session {
        public final PerformanceHintManager owner;
        public final int[] tids;
        public long target,actual;
        public int updates,reports,closes;
        public boolean closed;
        Session(PerformanceHintManager owner,int[] tids,long target){this.owner=owner;this.tids=tids;this.target=target;}
        public synchronized void updateTargetWorkDuration(long target){
            if(closed)throw new AssertionError("update after close");
            if(owner.updateFails)throw new IllegalStateException("background");
            if(target<=0)throw new IllegalArgumentException();
            updates++;this.target=target;
        }
        public synchronized void reportActualWorkDuration(long actual){
            if(closed)throw new AssertionError("report after close");
            if(owner.reportFails)throw new IllegalStateException("background");
            if(actual<=0)throw new AssertionError("nonpositive report");
            reports++;this.actual=actual;
        }
        public synchronized void close(){
            if(closed)throw new AssertionError("double close");
            closes++;closed=true;
            if(owner.closeFails)throw new IllegalStateException("close failure");
        }
    }
}
