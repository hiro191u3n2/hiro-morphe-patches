package android.os;
public final class Process {
    private static final ThreadLocal<Integer> TID=new ThreadLocal<Integer>();
    public static int myTid() { Integer v=TID.get();return v==null?923:v.intValue(); }
    public static void setTid(int value) { TID.set(Integer.valueOf(value)); }
}
