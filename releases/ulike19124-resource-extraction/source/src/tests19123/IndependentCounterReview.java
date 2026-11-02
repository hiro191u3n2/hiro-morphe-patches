import java.lang.reflect.*;import java.util.*;import com.hiro.ulike.CaptureBoost19123;
class IndependentCounterReview {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static class Poison {public String toString(){throw new AssertionError("receiver inspected");}public int hashCode(){throw new AssertionError("receiver hashed");}public boolean equals(Object o){throw new AssertionError("receiver compared");}}
 public static void main(String[]a)throws Exception{
  check(CaptureBoost19123.suppressedCount()==0,"new process starts zero");CaptureBoost19123.suppress(null);CaptureBoost19123.suppress(new Poison());check(CaptureBoost19123.suppressedCount()==2,"each site counted without receiver access");
  List<Thread> ts=new ArrayList<>();for(int j=0;j<8;j++){Thread t=new Thread(()->{for(int i=0;i<1000;i++)CaptureBoost19123.suppress(null);});ts.add(t);t.start();}for(Thread t:ts)t.join();check(CaptureBoost19123.suppressedCount()==8002,"no lost updates");
  Field f=CaptureBoost19123.class.getDeclaredField("suppressed");f.setAccessible(true);f.setLong(null,Long.MAX_VALUE-1);CaptureBoost19123.suppress(null);CaptureBoost19123.suppress(null);check(CaptureBoost19123.suppressedCount()==Long.MAX_VALUE,"count saturates");
  for(Field x:CaptureBoost19123.class.getDeclaredFields())check(x.getType().isPrimitive(),"no object holding field");
  System.out.println("PASS independent JVM counter behavior checks="+checks+" input_class=actual_build_helperclass physical_device_tested=false");
 }
}
