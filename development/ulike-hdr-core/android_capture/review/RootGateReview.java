import com.hiro.ulike.hdr.capture.*;
import java.util.*;
public class RootGateReview {
 static int checks;
 static void ok(boolean x){if(!x)throw new AssertionError(checks);checks++;}
 static class Img implements AutoCloseable {int closed;public void close(){closed++;if(closed>1)throw new AssertionError("duplicate close");}}
 public static void main(String[] args)throws Exception {
  Random rng=new Random(20261001);
  for(int n=0;n<100;n++){
   StillJoin<Img,Object> j=new StillJoin<>();Object a=new Object(),b=new Object(),metadata=new Object();
   j.begin(a);Img old=new Img();j.image(old,17);j.cancel();ok(old.closed==1);
   j.begin(b);ok(j.result(a,metadata,17)==null);ok(j.active());
   long time=1000+n;Img current=new Img();StillJoin.Pair<Img,Object> p;
   if(rng.nextBoolean()){ok(j.result(b,metadata,time)==null);p=j.image(current,time);}
   else{ok(j.image(current,time)==null);p=j.result(b,metadata,time);}
   ok(p.request==b&&p.result==metadata&&p.timestampNs==time&&p.image()==current);ok(!j.active());
   j.close();ok(current.closed==0);p.close();ok(current.closed==1);p.close();ok(current.closed==1);
   Img late=new Img();ok(j.image(late,time)==null);ok(late.closed==1);
  }
  for(int h:new int[]{2,100,3060,4284})for(int w:new int[]{2,100,4080,5712}){
   CapturePolicy.dimensions(w,h,3L*w*h,new int[][]{{w,h}});checks++;
   boolean rejected=false;try{CapturePolicy.dimensions(w,h,3L*w*h-1,new int[][]{{w,h}});}catch(IllegalArgumentException e){rejected=true;}ok(rejected);
  }
  System.out.println("Independent ownership/payload assertions PASS: "+checks);
 }
}
