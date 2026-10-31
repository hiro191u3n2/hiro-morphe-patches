package app.hiro.fossifycalendar.runtime;

import android.content.Context;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class MonthColorsTest {
    private static final Context CONTEXT = new Context();
    private static final int WHITE = 0xffffffff, BLUE = 0xff2196f3, RED = 0xffff0000;
    private static int assertions;

    public static final Dao DAO = new Dao();
    public static final class Dao {
        public volatile List<Object> rows = new ArrayList<Object>();
        public volatile int reads;
        public volatile boolean fail;
        public List<Object> c() {
            reads++;
            if (fail) throw new IllegalStateException("database unavailable");
            return rows;
        }
    }

    public static final class Day {
        final String code;
        final List<Object> events;
        Day(String code, Object... events) { this.code = code; this.events = Arrays.asList(events); }
        public String getCode() { return code; }
        public List<Object> getDayEvents() { return events; }
    }
    public static final class Event {
        final long calendarId;
        final String importId;
        final boolean allDay, task, canceled;
        Event(long id, String uid) { this(id, uid, true, false, false); }
        Event(long id, String uid, boolean allDay, boolean task, boolean canceled) {
            this.calendarId=id; this.importId=uid; this.allDay=allDay; this.task=task; this.canceled=canceled;
        }
        public long getCalendarId() { return calendarId; }
        public String getImportId() { return importId; }
        public boolean getIsAllDay() { return allDay; }
        public boolean isTask() { return task; }
        public boolean isEventCanceled() { return canceled; }
        public int getColor() { return RED; }
        public String getTitle() { return "祝日"; }
    }
    public static final class CalendarRow {
        final Long id;
        final int type;
        final String title, displayName;
        CalendarRow(long id, int type, String title, String displayName) {
            this.id=id; this.type=type; this.title=title; this.displayName=displayName;
        }
        public Long getId() { return id; }
        public int getType() { return type; }
        public String getTitle() { return title; }
        public String getCaldavDisplayName() { return displayName; }
    }
    private static void equal(String message, int expected, int actual) {
        assertions++;
        if (expected != actual) throw new AssertionError(message + ": expected=" + Integer.toHexString(expected) + " actual=" + Integer.toHexString(actual));
    }
    private static int color(String code, Object... events) {
        return MonthColors.dateColor(CONTEXT, new Day(code, events), WHITE);
    }
    private static void rows(Object... rows) {
        DAO.rows = new ArrayList<Object>(Arrays.asList(rows));
        MonthColors.beginRender(CONTEXT);
    }
    public static void main(String[] args) throws Exception {
        rows();
        // Screenshot month: Saturday columns and actual Saturday dates; Sunday remains native.
        for (String day : Arrays.asList("20270501","20270508","20270515","20270522","20270529","20270605")) equal("Saturday " + day, BLUE, color(day));
        for (String day : Arrays.asList("20270502","20270509","20270516","20270523","20270530")) equal("Sunday", WHITE, color(day));
        equal("ordinary weekday", WHITE, color("20270506"));
        equal("leap day Saturday", BLUE, color("20200229"));
        equal("year boundary Saturday", BLUE, color("20220101"));

        // Labels are independent of locale and support every first-day-of-week setting.
        int[] saturdayColumns = {5,4,3,2,1,0,6};
        for (int first=1; first<=7; first++) for (int column=0; column<7; column++)
            equal("label " + first + "/" + column, column==saturdayColumns[first-1] ? BLUE : WHITE, MonthColors.weekdayColor(first,column,WHITE));
        equal("invalid first day", WHITE, MonthColors.weekdayColor(0,5,WHITE));
        equal("invalid column", WHITE, MonthColors.weekdayColor(1,7,WHITE));
        equal("dim label", 0x802196f3, MonthColors.weekdayColor(7,6,0x80ffffff));
        equal("dim adjacent-month date", 0x802196f3, MonthColors.dateColor(CONTEXT,new Day("20270605"),0x80ffffff));
        equal("transparent date", 0x002196f3, MonthColors.dateColor(CONTEXT,new Day("20270605"),0x00ffffff));

        Event managed = new Event(90,"managed-holiday:JP:public:event");
        for (String day : Arrays.asList("20270429","20270503","20270504","20270505")) equal("screenshot managed holiday " + day, RED, color(day,managed));
        equal("Saturday holiday red wins", RED, color("20270501",managed));
        equal("dim holiday",0x80ff0000,MonthColors.dateColor(CONTEXT,new Day("20270429",managed),0x80ffffff));
        equal("ordinary red event/title not a holiday", WHITE, color("20270506",new Event(91,"ordinary")));
        equal("nonmanaged substring not a holiday", WHITE, color("20270506",new Event(91,"x-managed-holiday:event")));
        equal("task excluded", WHITE, color("20270506",new Event(90,"managed-holiday:x",true,true,false)));
        equal("timed event excluded", WHITE, color("20270506",new Event(90,"managed-holiday:x",false,false,false)));
        equal("canceled holiday excluded", WHITE, color("20270506",new Event(90,"managed-holiday:x",true,false,true)));

        rows(new CalendarRow(10,3,"custom renamed local holiday calendar",""));
        equal("legacy local holiday calendar",RED,color("20270503",new Event(10,"legacy")));
        equal("other calendar unaffected",WHITE,color("20270503",new Event(11,"legacy")));
        for (String name : Arrays.asList("祝日","日本の祝日","日本の祝祭日","国民の祝日","Holidays","Public Holidays","Japanese Holidays","Japan Holidays","Holidays in Japan","Japan Public Holidays","Japanese Public Holidays","  HOLIDAYS IN JAPAN  ")) {
            rows(new CalendarRow(12,0,name,""));
            equal("synced name " + name,RED,color("20270503",new Event(12,"google-uid")));
            rows(new CalendarRow(13,0,"renamed",name));
            equal("synced display name " + name,RED,color("20270503",new Event(13,"google-uid")));
        }
        for (String name : Arrays.asList("休日出勤","祝日の打合せ","Holiday trip","Birthdays","Anniversaries","Work holidays","日本","", "Holiday")) {
            rows(new CalendarRow(14,0,name,""));
            equal("ordinary calendar " + name,WHITE,color("20270503",new Event(14,"normal")));
        }
        rows(new CalendarRow(12,0,"祝日",""));
        int before=DAO.reads;
        for(int i=0;i<42;i++) color("20270503",new Event(12,"google-uid"));
        equal("no per-date database queries",before,DAO.reads);
        Looper.setMainThread(true);
        MonthColors.beginRender(CONTEXT);
        equal("UI thread does not query Room",before,DAO.reads);
        equal("UI thread can reuse last metadata snapshot",RED,color("20270503",new Event(12,"google-uid")));
        Looper.setMainThread(false);
        rows();
        equal("removed calendar does not remain cached",WHITE,color("20270503",new Event(12,"google-uid")));
        DAO.fail=true;
        MonthColors.beginRender(CONTEXT);
        equal("DB unavailable still identifies managed holiday",RED,color("20270503",managed));
        equal("DB unavailable still colors Saturday",BLUE,color("20270501"));
        DAO.fail=false;

        for(String invalid : Arrays.asList("","2027051","202705001","2027-5-1","abcdefgh","20270229","20271301","20270001","20270500","00000101"))
            equal("invalid date " + invalid,WHITE,color(invalid,managed));
        equal("null day",WHITE,MonthColors.dateColor(CONTEXT,null,WHITE));
        equal("foreign object",WHITE,MonthColors.dateColor(CONTEXT,new Object(),WHITE));
        equal("invalid event ignored",BLUE,color("20270501",new Object(),null));

        // Concurrent widget render callbacks must not overwrite each other's calendar snapshot.
        final CountDownLatch firstPrepared=new CountDownLatch(1), secondPrepared=new CountDownLatch(1);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread first=new Thread(new Runnable(){public void run(){try{
            rows(new CalendarRow(20,3,"local",""));firstPrepared.countDown();secondPrepared.await();
            if(color("20270503",new Event(20,"old"))!=RED)throw new AssertionError("concurrent snapshot lost");
        }catch(Throwable t){failure.set(t);firstPrepared.countDown();}}});
        Thread second=new Thread(new Runnable(){public void run(){try{
            firstPrepared.await();rows();secondPrepared.countDown();
            if(color("20270503",new Event(20,"old"))!=WHITE)throw new AssertionError("concurrent snapshot leaked");
        }catch(Throwable t){failure.set(t);secondPrepared.countDown();}}});
        first.start();second.start();first.join();second.join();
        if(failure.get()!=null)throw new AssertionError(failure.get());
        assertions+=2;
        System.out.println("PASS MonthColorsTest: " + assertions + " assertions");
    }
}
