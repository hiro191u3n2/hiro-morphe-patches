package android.os;
/** Records requests; does not kill the host JVM. */
public final class Process {
    public static final int OWN_PID=4242;
    public static final java.util.List<Integer> killed=new java.util.ArrayList<Integer>();
    public static int myPid(){return OWN_PID;}
    public static void killProcess(int pid){killed.add(pid);android.app.TestEvents.add("kill:"+pid);}
}
