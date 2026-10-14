package android.os;
public final class Looper {
 private static final Looper MAIN=new Looper();
 private static final ThreadLocal<Looper> CURRENT=new ThreadLocal<Looper>();
 private static final Thread MAIN_THREAD=Thread.currentThread();
 public static Looper getMainLooper(){return MAIN;}
 public static Looper myLooper(){Looper current=CURRENT.get();return current!=null?current:(Thread.currentThread()==MAIN_THREAD?MAIN:null);}
 static Looper select(Looper looper){Looper old=CURRENT.get();CURRENT.set(looper);return old;}
 static void restore(Looper old){if(old==null)CURRENT.remove();else CURRENT.set(old);}
}
