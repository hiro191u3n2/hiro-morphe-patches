package app.hiro.newxui;

import app.morphe.extension.crimera.settings.SettingsRegistry;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class ViewCountFormatTest {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static void formatted(long count, String expected) {
        String actual=ViewCountFormat.format(Long.valueOf(count),"native abbreviated text");
        check(expected.equals(actual),count+" expected "+expected+" got "+actual);
    }
    public static void main(String[] args) throws Exception {
        Locale saved=Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            UiOptions.register();
            formatted(0,"0");formatted(1,"1");formatted(999,"999");formatted(1000,"1,000");
            formatted(1001,"1,001");formatted(9999,"9,999");formatted(10000,"10,000");
            formatted(12345,"12,345");formatted(1000000,"1,000,000");formatted(2147483648L,"2,147,483,648");
            formatted(9007199254740993L,"9,007,199,254,740,993");
            formatted(Long.MAX_VALUE,"9,223,372,036,854,775,807");

            String original=new String("1.2万 native");
            check(ViewCountFormat.format(null,original)==original,"null raw returns same native text");
            check(ViewCountFormat.format(Long.valueOf(-1),original)==original,"negative raw returns same native text");
            check(ViewCountFormat.format(Long.valueOf(Long.MIN_VALUE),original)==original,"minimum long returns native");
            check(ViewCountFormat.format(null,null)==null,"null original preserved on fallback");
            SettingsRegistry.values.put(UiOptions.VIEWS,false);
            check(ViewCountFormat.format(Long.valueOf(12345),original)==original,"OFF keeps native abbreviation");
            check(ViewCountFormat.format(Long.valueOf(Long.MAX_VALUE),original)==original,"OFF keeps large native text");
            check(ViewCountFormat.format(Long.valueOf(1),null)==null,"OFF keeps null native text");
            SettingsRegistry.values.put(UiOptions.VIEWS,true);
            formatted(12345,"12,345");

            Locale.setDefault(Locale.JAPAN);formatted(12345,"12,345");
            Locale.setDefault(Locale.GERMANY);formatted(1234567,"1.234.567");
            Locale.setDefault(Locale.FRANCE);formatted(1234567,"1\u202f234\u202f567");
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));formatted(1234567,"\u0661\u066c\u0662\u0663\u0664\u066c\u0665\u0666\u0667");
            Locale.setDefault(Locale.US);formatted(1234567,"1,234,567");

            final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
            final CountDownLatch start=new CountDownLatch(1),done=new CountDownLatch(4);
            for (int i=0;i<4;i++) new Thread(new Runnable() {
                public void run() {
                    try {
                        start.await();
                        for (int j=0;j<100;j++) {
                            String value=ViewCountFormat.format(Long.valueOf(9007199254740993L),"9P");
                            if (!"9,007,199,254,740,993".equals(value)) throw new AssertionError(value);
                        }
                    } catch(Throwable problem) { failure.compareAndSet(null,problem); }
                    finally { done.countDown(); }
                }
            }).start();
            start.countDown();done.await();
            check(failure.get()==null,"parallel formatting keeps integer precision");
            System.out.println("PASS: "+assertions+" formatter assertions and 400 concurrent calls across 4 threads");
        } finally { Locale.setDefault(saved); }
    }
}
