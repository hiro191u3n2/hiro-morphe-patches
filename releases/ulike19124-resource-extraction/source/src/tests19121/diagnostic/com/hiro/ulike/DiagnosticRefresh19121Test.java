package com.hiro.ulike;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/** Real timing recorder and view dispatcher; the fixture controls UI visibility. */
public final class DiagnosticRefresh19121Test {
    private static int assertions;
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private static ArrayList<?> views()throws Exception {
        Field f=ProcessingTiming1947.class.getDeclaredField("VIEWS");f.setAccessible(true);return (ArrayList<?>)f.get(null);
    }
    private static boolean pending(Object binding)throws Exception {
        Field f=binding.getClass().getDeclaredField("refreshPending19121");f.setAccessible(true);return f.getBoolean(binding);
    }
    private static Object binding(PreferenceActivity activity)throws Exception {
        for(Object b:views()){
            Field f=b.getClass().getDeclaredField("activity");f.setAccessible(true);
            if(((WeakReference<?>)f.get(b)).get()==activity)return b;
        }
        throw new AssertionError("view not installed");
    }
    private static Preference install(PreferenceActivity activity){
        Preference row=new Preference();row.key="detail_last_result";activity.screen.children.add(row);
        ProcessingTiming1947.install(activity);row.summaryAttempts=0;row.summaryWrites=0;return row;
    }
    private static void notifyView(){ProcessingTiming1947.legacyStatus("ignored old text");}
    public static void main(String[] args)throws Exception {
        PreferenceActivity activity=new PreferenceActivity();Preference row=install(activity);Object binding=binding(activity);
        check(row.click!=null,"real install registered result row and manual refresh");
        activity.focused=false;
        for(int i=0;i<100;i++)notifyView();
        check(activity.postAttempts==0 && row.summaryAttempts==0 && !pending(binding),"hidden diagnostic does not enqueue or rewrite summary");
        activity.focused=true;activity.finishing=true;notifyView();
        check(activity.postAttempts==0 && !pending(binding),"finishing activity skipped");
        activity.finishing=false;activity.destroyed=true;notifyView();
        check(activity.postAttempts==0 && !pending(binding),"destroyed activity skipped");
        activity.destroyed=false;activity.failState=true;notifyView();
        check(activity.postAttempts==0 && !pending(binding),"state API failure safely skips view");
        activity.failState=false;

        for(int i=0;i<100;i++)notifyView();
        check(activity.queued()==1 && activity.postAttempts==1 && pending(binding),"visible burst queues one callback only");
        check(row.summaryAttempts==0,"summary build and write wait for UI dispatch");
        ProcessingTiming1947.Trace first=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Trace latest=ProcessingTiming1947.begin(new Object());
        check(first!=null && latest!=null && latest.id>first.id,"real recorder advances capture trace");
        check(activity.queued()==1 && activity.postAttempts==1,"new captures share pending refresh slot");
        activity.runNext();
        check(row.summaryWrites==1 && row.summary.toString().contains("撮影 #"+latest.id+" /"),"queued callback renders latest trace at execution");
        check(!row.summary.toString().contains("撮影 #"+first.id+" /"),"queued callback does not restore stale trace");
        check(!pending(binding) && activity.queued()==0,"successful UI update releases pending slot");

        notifyView();int written=row.summaryWrites;activity.focused=false;activity.runNext();
        check(row.summaryWrites==written && !pending(binding),"lost focus before dispatch skips update and releases slot");
        activity.focused=true;notifyView();activity.finishing=true;activity.runNext();
        check(row.summaryWrites==written && !pending(binding),"finishing before dispatch is rechecked");
        activity.finishing=false;notifyView();activity.destroyed=true;activity.runNext();
        check(row.summaryWrites==written && !pending(binding),"destroyed before dispatch is rechecked");
        activity.destroyed=false;notifyView();activity.failState=true;activity.runNext();
        check(row.summaryWrites==written && !pending(binding),"state-query failure at dispatch releases slot");
        activity.failState=false;notifyView();activity.runNext();
        check(row.summaryWrites==written+1 && !pending(binding),"visible view can update after skipped callbacks");

        activity.failPost=true;notifyView();
        check(activity.queued()==0 && !pending(binding),"post exception releases pending slot");
        activity.failPost=false;notifyView();
        check(activity.queued()==1 && pending(binding),"post failure can retry");
        row.failSummary=true;written=row.summaryWrites;activity.runNext();
        check(row.summaryWrites==written && !pending(binding),"summary exception releases pending slot");
        row.failSummary=false;notifyView();activity.runNext();
        check(row.summaryWrites==written+1 && !pending(binding),"summary failure can retry successfully");

        final CountDownLatch start=new CountDownLatch(1);Thread[] callers=new Thread[12];
        for(int i=0;i<callers.length;i++){
            callers[i]=new Thread(new Runnable(){public void run(){
                try{start.await();}catch(InterruptedException e){throw new AssertionError(e);}
                for(int j=0;j<80;j++)notifyView();
            }});callers[i].start();
        }
        start.countDown();for(Thread caller:callers){caller.join(5000);check(!caller.isAlive(),"concurrent publisher completes");}
        check(activity.queued()==1 && activity.maxQueued==1 && pending(binding),"concurrent publishers never queue more than one view callback");
        written=row.summaryWrites;activity.runNext();
        check(row.summaryWrites==written+1 && !pending(binding),"coalesced concurrent publishers perform one summary update");

        PreferenceActivity second=new PreferenceActivity();Preference secondRow=install(second);Object secondBinding=binding(second);
        notifyView();check(activity.queued()==1 && second.queued()==1,"each visible view gets its own bounded callback");
        Field preference=binding.getClass().getDeclaredField("preference");preference.setAccessible(true);
        ((WeakReference<?>)preference.get(binding)).clear();activity.runNext();
        check(!pending(binding),"collected preference releases pending callback");
        second.runNext();check(secondRow.summaryWrites==1 && !pending(secondBinding),"other view update survives collected peer");
        Field activityRef=binding.getClass().getDeclaredField("activity");activityRef.setAccessible(true);
        ((WeakReference<?>)activityRef.get(binding)).clear();notifyView();
        check(activity.queued()==0 && second.queued()==1,"collected activity is skipped while live view continues");
        second.runNext();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"hidden_view_updates\":0,\"maximum_pending_per_view\":1,\"latest_trace_verified\":true,\"negative_controls\":{\"hidden\":true,\"finishing\":true,\"destroyed\":true,\"visibility_api_failure\":true,\"lost_focus_before_dispatch\":true,\"post_exception\":true,\"summary_exception\":true,\"collected_weak_references\":true}}");
    }
}
