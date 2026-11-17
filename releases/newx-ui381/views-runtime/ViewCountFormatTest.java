package app.hiro.newxui;

import app.morphe.extension.crimera.settings.SettingsRegistry;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class ViewCountFormatTest {
    private static int assertions;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static void formatted(long count, String expected) {
        String actual=ViewCountFormat.formatOrNull(Long.valueOf(count));
        check(expected.equals(actual),count+" expected "+expected+" got "+actual);
    }
    private static ThreadLocal<?> cache() throws Exception {
        Field field=ViewCountFormat.class.getDeclaredField("FORMAT");
        field.setAccessible(true);
        return (ThreadLocal<?>)field.get(null);
    }
    private static Object formatter(Object state) throws Exception {
        Field field=state.getClass().getDeclaredField("number");
        field.setAccessible(true);
        return field.get(state);
    }
    public static void main(String[] args) throws Exception {
        Locale saved=Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            UiOptions.register();
            ThreadLocal<?> local=cache();
            local.remove();
            check(ViewCountFormat.formatOrNull(null)==null,"null raw requests native fallback");
            check(ViewCountFormat.formatOrNull(Long.valueOf(-1))==null,"negative raw requests native fallback");
            check(ViewCountFormat.formatOrNull(Long.valueOf(Long.MIN_VALUE))==null,"minimum long requests native fallback");
            check(local.get()==null,"invalid values do not allocate a formatter");
            SettingsRegistry.values.put(UiOptions.VIEWS,false);
            check(ViewCountFormat.formatOrNull(Long.valueOf(12345))==null,"OFF requests native abbreviation");
            check(ViewCountFormat.formatOrNull(Long.valueOf(Long.MAX_VALUE))==null,"OFF preserves large native count path");
            check(local.get()==null,"OFF does not allocate a formatter");
            SettingsRegistry.values.put(UiOptions.VIEWS,true);

            formatted(0,"0");formatted(1,"1");formatted(999,"999");formatted(1000,"1,000");
            formatted(1001,"1,001");formatted(9999,"9,999");formatted(10000,"10,000");
            formatted(12345,"12,345");formatted(1000000,"1,000,000");formatted(2147483648L,"2,147,483,648");
            formatted(9007199254740993L,"9,007,199,254,740,993");
            formatted(Long.MAX_VALUE,"9,223,372,036,854,775,807");

            Object initial=local.get(),number=formatter(initial);
            for(int i=0;i<1000;i++) ViewCountFormat.formatOrNull(Long.valueOf(i));
            check(local.get()==initial && formatter(local.get())==number,"1000 new values reuse same formatter instance");
            Locale.setDefault(new Locale("en","US"));
            formatted(12345,"12,345");
            check(local.get()==initial,"equal Locale object reuses formatter");
            SettingsRegistry.values.put(UiOptions.VIEWS,false);
            Locale.setDefault(Locale.GERMANY);
            check(ViewCountFormat.formatOrNull(Long.valueOf(1))==null,"OFF after locale change requests fallback");
            check(local.get()==initial,"OFF leaves cached formatter untouched");
            SettingsRegistry.values.put(UiOptions.VIEWS,true);
            formatted(1234567,"1.234.567");
            Object german=local.get();
            check(german!=initial && formatter(german)!=number,"locale change replaces formatter");
            formatted(1234567,"1.234.567");
            check(local.get()==german,"replacement reused");
            Locale.setDefault(Locale.JAPAN);formatted(12345,"12,345");
            Locale.setDefault(Locale.FRANCE);formatted(1234567,"1\u202f234\u202f567");
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));formatted(1234567,"\u0661\u066c\u0662\u0663\u0664\u066c\u0665\u0666\u0667");
            Locale.setDefault(Locale.US);formatted(1234567,"1,234,567");
            check(local.get()!=initial,"old locale instances are not retained in a locale map");

            final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
            final CountDownLatch start=new CountDownLatch(1),done=new CountDownLatch(4);
            final Set<Object> states=Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));
            final Set<Object> formats=Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));
            for (int i=0;i<4;i++) new Thread(new Runnable() {
                public void run() {
                    try {
                        start.await();
                        ThreadLocal<?> own=cache();
                        Object stable=null;
                        for (int j=0;j<1000;j++) {
                            String value=ViewCountFormat.formatOrNull(Long.valueOf(9007199254740993L));
                            if (!"9,007,199,254,740,993".equals(value)) throw new AssertionError(value);
                            if(stable==null) stable=own.get();
                            else if(own.get()!=stable) throw new AssertionError("formatter changed without locale change");
                        }
                        states.add(stable);formats.add(formatter(stable));
                    } catch(Throwable problem) { failure.compareAndSet(null,problem); }
                    finally { done.countDown(); }
                }
            }).start();
            start.countDown();done.await();
            check(failure.get()==null,"parallel formatting keeps precision and reuses each thread's instance");
            check(states.size()==4 && formats.size()==4,"four threads use four distinct formatter states and NumberFormats");
            check(!states.contains(local.get()) && !formats.contains(formatter(local.get())),"worker formatters not shared with main thread");
            System.out.println("PASS: "+assertions+" formatter assertions, 1000 reuse calls and 4000 concurrent calls across 4 threads");
        } finally { Locale.setDefault(saved); cache().remove(); }
    }
}
