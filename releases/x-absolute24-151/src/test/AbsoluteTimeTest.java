import app.hiro.twitter.runtime.AbsoluteTime;
import java.text.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class AbsoluteTimeTest {
    private static final AtomicInteger checks = new AtomicInteger();
    private static void same(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
        checks.incrementAndGet();
    }
    private static long time(String iso) { return Instant.parse(iso).toEpochMilli(); }
    private static void date(String zone, String iso, String text) {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        long t = time(iso);
        same(text, AbsoluteTime.absoluteOrNull(t));
        same(text, AbsoluteTime.post(t, null));
        same(text, AbsoluteTime.accessiblePost(t, null));
        DateFormat old = new SimpleDateFormat("hh:mm a MMM d yyyy", Locale.US);
        same(text, AbsoluteTime.detail(old, Long.valueOf(t)));
        same(text, AbsoluteTime.detail(old, new Date(t)));
        same(text, AbsoluteTime.accessibleDetail(old, new Date(t)));
    }
    public static void main(String[] args) throws Exception {
        TimeZone originalZone = TimeZone.getDefault(); Locale originalLocale = Locale.getDefault();
        try {
            date("Asia/Tokyo", "2026-10-06T11:05:00Z", "2026/10/06 20:05");
            date("Asia/Tokyo", "2026-10-05T15:00:00Z", "2026/10/06 00:00");
            date("Asia/Tokyo", "2026-10-06T03:00:00Z", "2026/10/06 12:00");
            date("Asia/Tokyo", "2026-10-06T14:59:59.999Z", "2026/10/06 23:59");
            date("Asia/Tokyo", "2025-12-31T15:00:00Z", "2026/01/01 00:00");
            date("Asia/Tokyo", "2024-02-29T14:59:59Z", "2024/02/29 23:59");
            date("UTC", "2026-10-06T11:05:00Z", "2026/10/06 11:05");
            date("America/New_York", "2026-03-08T06:59:00Z", "2026/03/08 01:59");
            date("America/New_York", "2026-03-08T07:00:00Z", "2026/03/08 03:00");
            date("America/New_York", "2026-11-01T05:30:00Z", "2026/11/01 01:30");
            date("America/New_York", "2026-11-01T06:30:00Z", "2026/11/01 01:30");
            date("Asia/Kathmandu", "2026-10-06T11:05:00Z", "2026/10/06 16:50");
            for (Locale locale : Arrays.asList(Locale.JAPAN, Locale.US, new Locale("th","TH"), new Locale("ar","EG"))) {
                Locale.setDefault(locale);
                date("Asia/Tokyo", "2026-10-06T11:05:00Z", "2026/10/06 20:05");
            }
            for(long invalid : new long[]{0L,-1L,Long.MIN_VALUE,Long.MAX_VALUE,253402214400000L}) {
                same(null, AbsoluteTime.absoluteOrNull(invalid));
                same("original-relative:"+invalid, AbsoluteTime.post(invalid,null));
                same("original-accessible:"+invalid, AbsoluteTime.accessiblePost(invalid,null));
            }
            DateFormat old = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US);
            same(old.format(new Date(0)), AbsoluteTime.accessibleDetail(old, new Date(0)));
            same(old.format(0L), AbsoluteTime.detail(old, 0L));
            NumberFormat number = new DecimalFormat("0.00");
            same(number.format(42L), AbsoluteTime.detail(number,42L));
            // A same-ID zone rule replacement must not reuse the old cached offset.
            TimeZone.setDefault(new SimpleTimeZone(0,"SameID"));
            same("2026/10/06 11:05", AbsoluteTime.absoluteOrNull(time("2026-10-06T11:05:00Z")));
            TimeZone.setDefault(new SimpleTimeZone(9*3600000,"SameID"));
            same("2026/10/06 20:05", AbsoluteTime.absoluteOrNull(time("2026-10-06T11:05:00Z")));
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            ExecutorService executor = Executors.newFixedThreadPool(8);
            List<Future<?>> futures = new ArrayList<>();
            for(int thread=0;thread<8;thread++) futures.add(executor.submit(new Runnable(){
                public void run() {
                    for(int i=0;i<500;i++) {
                        same("2026/10/06 20:05",AbsoluteTime.absoluteOrNull(time("2026-10-06T11:05:00Z")));
                        same("2024/02/29 23:59",AbsoluteTime.absoluteOrNull(time("2024-02-29T14:59:59Z")));
                    }
                }
            }));
            executor.shutdown(); for(Future<?> f:futures) f.get();
            if(!executor.awaitTermination(30,TimeUnit.SECONDS)) throw new AssertionError("threads stalled");
            System.out.println("PASS formatter host assertions="+checks.get()+" (not Android device tests)");
        } finally { TimeZone.setDefault(originalZone); Locale.setDefault(originalLocale); }
    }
}
