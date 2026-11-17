import app.hiro.newx.runtime.NewXAbsoluteTime;
import java.util.*;
import java.time.*;
import kotlin.time.Instant;
public class TimeTest {
    private static int n;
    private static void equal(Object a,Object b){n++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] args)throws Exception{
        TimeZone saved=TimeZone.getDefault();Locale old=Locale.getDefault();
        try{
            long epoch=java.time.Instant.parse("2026-11-17T03:07:00Z").toEpochMilli();
            Locale.setDefault(Locale.JAPAN);TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            equal(NewXAbsoluteTime.absoluteOrNull(epoch),"2026/11/17 12:07");
            equal(NewXAbsoluteTime.post(new Instant(epoch),null,null),"2026/11/17 12:07");
            equal(NewXAbsoluteTime.detail(null,new Instant(epoch),null),"2026/11/17 12:07");
            equal(NewXAbsoluteTime.accessible(new Instant(epoch),null,null,false),"2026/11/17 12:07");
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            equal(NewXAbsoluteTime.absoluteOrNull(epoch),"2026/11/16 22:07");
            equal(NewXAbsoluteTime.absoluteOrNull(java.time.Instant.parse("2026-07-17T03:07:00Z").toEpochMilli()),"2026/07/16 23:07");
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));Locale.setDefault(new Locale("ar"));
            equal(NewXAbsoluteTime.absoluteOrNull(epoch),"2026/11/17 03:07");
            equal(NewXAbsoluteTime.absoluteOrNull(0),null);equal(NewXAbsoluteTime.absoluteOrNull(-1),null);
            equal(NewXAbsoluteTime.absoluteOrNull(Long.MAX_VALUE),null);equal(NewXAbsoluteTime.fromInstant(null),null);
            equal(NewXAbsoluteTime.post(new Instant(0),null,null),"original-relative");
            equal(NewXAbsoluteTime.detail(null,null,null),"original-detail");
            equal(NewXAbsoluteTime.accessible(new Instant(-1),null,null,true),"original-accessible");
            List<Thread> threads=new ArrayList<>();List<Throwable> errors=Collections.synchronizedList(new ArrayList<>());
            for(int j=0;j<8;j++){Thread t=new Thread(()->{try{for(int i=0;i<1000;i++)if(!"2026/11/17 03:07".equals(NewXAbsoluteTime.absoluteOrNull(epoch)))throw new AssertionError();}catch(Throwable e){errors.add(e);}});threads.add(t);t.start();}
            for(Thread t:threads)t.join();equal(errors.size(),0);
            System.out.println("PASS: "+n+" timezone/fallback assertions and 8000 concurrent format operations");
        }finally{TimeZone.setDefault(saved);Locale.setDefault(old);}
    }
}
