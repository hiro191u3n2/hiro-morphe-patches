/* Copyright 2026 Hiro contributors. SPDX-License-Identifier: GPL-3.0-only */
package app.hiro.tiktoklink.runtime;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inspector.WindowInspector;
import com.ss.android.ugc.aweme.detail.ui.DetailActivity;
import com.ss.android.ugc.aweme.detail.ui.saf.DetailSafRootFragment;
import com.ss.android.ugc.aweme.detail.ui.saf.WithoutShellDetailSafRootFragment;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

/** Executes the separately compiled production runtime against observable Android
 * API stubs. Native Back branching is checked independently in LinkBackAudit;
 * these tests do not substitute a Java copy of that native navigation algorithm.
 * A fresh class loader isolates production static state for every case without
 * adding a reset API or modifying production internals. Not an Android device test.
 */
public final class SharedLinkExitHostTest {
    static final String PKG="com.zhiliaoapp.musically";
    static final String DETAIL="com.ss.android.ugc.aweme.detail.ui.DetailActivity";
    static final String TOKEN="app.hiro.tiktoklink.issued_session102";
    static final String ID="7391234567890123456";
    static int checks,cases;
    static String current;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(current+": "+why);}
    interface Test {void run(Rt r)throws Exception;}
    static void test(String name,Test body)throws Exception{
        current=name;Handler.clear();WindowInspector.clear();TestEvents.clear();android.os.Process.killed.clear();SystemClock.time=1000;Build.VERSION.SDK_INT=35;
        try(Rt r=new Rt()){body.run(r);cases++;}
        finally{Handler.clear();WindowInspector.clear();}
    }
    static final class Rt implements AutoCloseable {
        final URLClassLoader loader;final Class<?> type;
        Rt()throws Exception{
            URL production=SharedLinkExit.class.getProtectionDomain().getCodeSource().getLocation();
            loader=new URLClassLoader(new URL[]{production},SharedLinkExitHostTest.class.getClassLoader()){
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                    if(name.equals("app.hiro.tiktoklink.runtime.SharedLinkExit")||name.startsWith("app.hiro.tiktoklink.runtime.SharedLinkExit$")){
                        synchronized(getClassLoadingLock(name)){Class<?> c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}
                    }return super.loadClass(name,resolve);
                }};
            type=Class.forName("app.hiro.tiktoklink.runtime.SharedLinkExit",true,loader);
            check(type!=SharedLinkExit.class,"production runtime must be isolated, not replaced by a test fixture");
        }
        Object call(String name,Class<?>[] signature,Object... args){try{return type.getMethod(name,signature).invoke(null,args);}catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof RuntimeException)throw(RuntimeException)t;if(t instanceof Error)throw(Error)t;throw new AssertionError(t);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
        void prepare(Intent i,Uri u){call("prepare",new Class<?>[]{Intent.class,Uri.class},i,u);}
        void transfer(Intent a,Intent b){call("transfer",new Class<?>[]{Intent.class,Intent.class},a,b);}
        boolean prepared(Intent i){return Boolean.TRUE.equals(call("isPrepared",new Class<?>[]{Intent.class},i));}
        void bind(Object f){call("bind",new Class<?>[]{Object.class},f);}
        void forget(Object f){call("forget",new Class<?>[]{Object.class},f);}
        boolean exit(Object f){return Boolean.TRUE.equals(call("tryExit",new Class<?>[]{Object.class},f));}
        boolean suppress(Object f){return Boolean.TRUE.equals(call("suppressNativeReturn",new Class<?>[]{Object.class},f));}
        public void close()throws Exception{loader.close();}
    }
    static Uri uri(String id){return Uri.parse("aweme://aweme/detail/"+id+"?utm_campaign=client_share&insert_feed=1");}
    static Intent actual(String id){return new Intent().setComponent(new ComponentName(PKG,DETAIL)).putExtra("id",id);}
    static Intent issue(Rt r,String id){Intent temp=new Intent(),out=actual(id);r.prepare(temp,uri(id));r.transfer(temp,out);check(r.prepared(out),"native final Intent must receive an issued session");return out;}
    static ActivityManager.AppTask task(ActivityManager manager,String pkg,int id){
        ActivityManager.RecentTaskInfo info=new ActivityManager.RecentTaskInfo();info.taskId=id;info.id=id;info.persistentId=id;info.baseActivity=pkg==null?null:new ComponentName(pkg,pkg+".Root");info.baseIntent=new Intent();if(info.baseActivity!=null)info.baseIntent.setComponent(info.baseActivity);
        ActivityManager.AppTask t=new ActivityManager.AppTask(info);manager.tasks.add(t);return t;
    }
    static final class Scene {
        final Rt r;final Application app;final Activity activity;final ActivityManager manager=new ActivityManager();final DetailSafRootFragment fragment;final View root;final ActivityManager.AppTask own;Intent intent;
        Scene(Rt runtime,boolean saf,boolean shell,String pkg,boolean ownTask)throws Exception{
            r=runtime;app=new Application(pkg);app.setService(Context.ACTIVITY_SERVICE,manager);
            activity=saf?new Activity(app,20,false):new DetailActivity(app,20,false);
            activity.getWindow().attach();root=activity.getWindow().getDecorView();root.focus(true);
            fragment=shell?new WithoutShellDetailSafRootFragment():new DetailSafRootFragment();fragment.activity=activity;fragment.view=root;
            own=ownTask?task(manager,pkg,20):null;if(own!=null)own.activities.add(activity);
        }
        Scene(Rt r)throws Exception{this(r,false,false,PKG,true);}
        void bind(){intent=issue(r,ID);fragment.route=intent;activity.setIntent(intent);r.bind(fragment);}
        void assertUntouched(){check(TestEvents.values.isEmpty(),"no cleanup before authorized Back");check(android.os.Process.killed.isEmpty()&&Handler.queuedCount()==0,"no premature process exit");}
        void denied(){check(!r.exit(fragment),"exit should be rejected");assertUntouched();}
        void exits(){check(r.exit(fragment),"admitted foreground native Back should exit");check(android.os.Process.killed.isEmpty(),"cleanup must precede process kill");Handler.advanceBy(100);check(android.os.Process.killed.equals(Collections.singletonList(android.os.Process.OWN_PID)),"exactly the owning PID is terminated");}
    }
    static void noKill(){check(android.os.Process.killed.isEmpty(),"no process was killed");}

    public static void main(String[] args)throws Exception{
        Looper.getMainLooper();
        test("dedicated Detail cleanup ordering and one shot",r->{Scene s=new Scene(r);s.bind();s.assertUntouched();check(!r.suppress(s.fragment),"ordinary native return remains enabled");s.own.onRemove=()->check(r.suppress(s.fragment),"native back_to navigation suppressed during authorized cleanup");check(r.exit(s.fragment),"first Back accepted");check(s.own.excludeCalls==1&&s.own.removeCalls==1,"owned task excluded then removed once");check(TestEvents.values.equals(Arrays.asList("exclude:20:true","task-remove:20")),"no navigation or process termination before cleanup requests");check(r.exit(s.fragment),"duplicate callback consumed without a second exit");check(s.own.removeCalls==1&&Handler.queuedCount()==1,"duplicate callback does not enqueue another kill");Handler.advanceBy(99);noKill();Handler.advanceBy(1);check(android.os.Process.killed.equals(Collections.singletonList(android.os.Process.OWN_PID)),"own process killed once after delay");check(TestEvents.values.get(TestEvents.values.size()-1).equals("kill:4242"),"kill is the final observed operation");});
        test("SAF route uses fragment Intent",r->{Scene s=new Scene(r,true,false,PKG,true);s.bind();s.activity.setIntent(new Intent().putExtra("id","unrelated-main"));s.exits();});
        test("actual SAF extras bridge loses component",r->{Scene s=new Scene(r,true,false,PKG,true);Intent issued=issue(r,ID);s.fragment.arguments=issued.getExtras();s.fragment.arguments.putInt("use_saf",1);s.fragment.route=new Intent().putExtras(s.fragment.arguments);s.activity.setIntent(new Intent().putExtra("id","unrelated-main"));check(!r.prepared(s.fragment.route),"animation admission still requires the native final component");r.bind(s.fragment);s.exits();});
        test("componentless route without SAF argument proof",r->{Scene s=new Scene(r,true,false,PKG,true);Intent issued=issue(r,ID);s.fragment.route=new Intent().putExtras(issued.getExtras());r.bind(s.fragment);s.denied();});
        test("SAF arguments must match issued route token",r->{Scene s=new Scene(r,true,false,PKG,true);Intent issued=issue(r,ID);s.fragment.route=new Intent().putExtras(issued.getExtras());s.fragment.arguments=issued.getExtras();s.fragment.arguments.putInt("use_saf",1);s.fragment.arguments.putString(TOKEN,"different");r.bind(s.fragment);s.denied();});
        test("componentless route cannot authorize dedicated Activity",r->{Scene s=new Scene(r);Intent issued=issue(r,ID);s.activity.setIntent(issued);s.fragment.route=new Intent().putExtras(issued.getExtras());r.bind(s.fragment);s.denied();});
        test("WithoutShell native subclass",r->{Scene s=new Scene(r,true,true,PKG,true);s.bind();s.exits();});
        test("no route is a no-op",r->{Scene s=new Scene(r);s.fragment.route=actual(ID);s.activity.setIntent(s.fragment.route);r.bind(s.fragment);s.denied();check(!r.suppress(s.fragment),"unselected native return not intercepted");});
        test("forged caller token cannot grant exit",r->{Scene s=new Scene(r);s.fragment.route=actual(ID).putExtra(TOKEN,"externally-forged-token");s.activity.setIntent(s.fragment.route);r.bind(s.fragment);check(!r.prepared(s.fragment.route),"no process-owned issued record");s.denied();});
        test("unprepared transfer rejected",r->{Intent out=actual(ID);r.transfer(new Intent(),out);check(!r.prepared(out),"no temporary-Intent authority");});
        test("temporary identity cannot be copied",r->{Intent temp=new Intent(),out=actual(ID);r.prepare(temp,uri(ID));r.transfer(new Intent(temp),out);check(!r.prepared(out),"copy of pending Intent not trusted");r.transfer(temp,out);check(r.prepared(out),"original pending identity still valid");});
        test("one temporary Intent cannot issue twice",r->{Intent temp=new Intent(),a=actual(ID),b=actual(ID);r.prepare(temp,uri(ID));r.transfer(temp,a);r.transfer(temp,b);check(r.prepared(a)&&!r.prepared(b),"transfer consumes authority");});
        test("final native target mismatch rejected",r->{Intent temp=new Intent(),out=actual("123");r.prepare(temp,uri(ID));r.transfer(temp,out);check(!r.prepared(out),"cannot attach exit authority to a different video");});
        test("non Detail native component rejected",r->{Intent temp=new Intent(),out=actual(ID).setComponent(new ComponentName(PKG,PKG+".MainActivity"));r.prepare(temp,uri(ID));r.transfer(temp,out);check(!r.prepared(out),"wrong native component");});
        test("foreign component rejected",r->{Intent temp=new Intent(),out=actual(ID).setComponent(new ComponentName("jp.naver.line.android",DETAIL));r.prepare(temp,uri(ID));r.transfer(temp,out);check(!r.prepared(out),"foreign package must not receive authority");});
        for(String invalid:new String[]{"aweme://aweme/detail/0","aweme://aweme/detail/01","aweme://aweme/detail/1,2","aweme://aweme/detail/123456789012345678901","mailto:video@example.com","aweme://profile/123"})test("invalid pending target "+invalid,r->{Intent temp=new Intent(),out=actual(ID);r.prepare(temp,Uri.parse(invalid));r.transfer(temp,out);check(!r.prepared(out),"invalid target cannot be issued");});
        for(String host:new String[]{"detail","item","musical"})test("legacy native shape "+host,r->{Intent temp=new Intent(),out=actual(ID);r.prepare(temp,Uri.parse("aweme://"+host+"/?id="+ID+"&utm_campaign=client_share"));r.transfer(temp,out);check(r.prepared(out),"legacy singleton route carries authority");});
        test("pending expiry",r->{Intent temp=new Intent(),out=actual(ID);r.prepare(temp,uri(ID));SystemClock.time+=300001;r.transfer(temp,out);check(!r.prepared(out),"expired pending route rejected");});
        test("issued but unbound expiry",r->{Intent out=issue(r,ID);SystemClock.time+=300001;check(!r.prepared(out),"unused issued token expires");});
        test("long viewing session remains bound",r->{Scene s=new Scene(r);s.bind();SystemClock.time+=3600000;s.exits();});
        test("issued map evicts old unbound tokens",r->{Intent first=issue(r,ID);for(int i=1;i<=70;i++)issue(r,String.valueOf(100+i));check(!r.prepared(first),"bounded issuance rejects evicted oldest token");});
        test("copied live token cannot bind a second fragment",r->{Scene s=new Scene(r);s.bind();DetailSafRootFragment second=new DetailSafRootFragment();second.activity=s.activity;second.view=s.root;second.route=new Intent(s.intent);r.bind(second);check(!r.exit(second),"issued session bound to its original fragment");s.assertUntouched();s.exits();});
        test("native Android Intent parcel copy can bind once",r->{Scene s=new Scene(r);Intent issued=issue(r,ID);s.intent=new Intent(issued);s.fragment.route=s.intent;s.activity.setIntent(s.intent);r.bind(s.fragment);s.exits();});
        test("target mutation revokes current session",r->{Scene s=new Scene(r);s.bind();s.intent.putExtra("id","123");s.denied();s.intent.putExtra("id",ID);check(!r.exit(s.fragment),"a rejected stale session cannot revive by changing id back");});
        test("new Activity Intent rejects lazy old fragment route",r->{Scene s=new Scene(r);s.bind();s.activity.setIntent(actual("456"));s.denied();s.activity.setIntent(s.intent);check(!r.exit(s.fragment),"old cached route stays revoked");});
        test("unprepared rebind revokes old session",r->{Scene s=new Scene(r);s.bind();Intent old=s.intent;s.fragment.route=actual("456");s.activity.setIntent(s.fragment.route);r.bind(s.fragment);s.fragment.route=old;s.activity.setIntent(old);r.bind(s.fragment);s.denied();});
        test("fresh accepted route replaces old bound route",r->{Scene s=new Scene(r);s.bind();Intent old=s.intent;s.intent=issue(r,"456");s.fragment.route=s.intent;s.activity.setIntent(s.intent);r.bind(s.fragment);check(!r.prepared(old),"old session revoked on rebind");s.exits();});
        test("fragment destroy forgets authority",r->{Scene s=new Scene(r);s.bind();r.forget(s.fragment);r.bind(s.fragment);s.denied();});
        test("Activity destruction invalidates bound token",r->{Scene s=new Scene(r);s.bind();s.app.destroyed(s.activity);s.denied();});
        test("background thread cannot operate exit",r->{Scene s=new Scene(r);s.bind();boolean[] accepted={true};Thread t=new Thread(()->accepted[0]=r.exit(s.fragment));t.start();t.join();check(!accepted[0],"non-main Back rejected");s.assertUntouched();s.exits();});
        test("native executor may prepare and transfer synchronously",r->{Scene s=new Scene(r);Intent temp=new Intent(),out=actual(ID);boolean[] prepared={false};Throwable[] error={null};Thread t=new Thread(()->{try{r.prepare(temp,uri(ID));r.transfer(temp,out);prepared[0]=r.prepared(out);}catch(Throwable e){error[0]=e;}});t.start();t.join();check(error[0]==null&&prepared[0],"executor route must be issued before native launch/animation decisions");s.intent=out;s.fragment.route=out;s.activity.setIntent(out);r.bind(s.fragment);s.exits();});
        test("foreign Activity package cannot bind",r->{Scene s=new Scene(r,false,false,"jp.naver.line.android",false);s.bind();s.denied();});
        test("unrecognized fragment receiver rejected",r->{Scene s=new Scene(r);s.bind();check(!r.exit(new Object())&&!r.exit(null),"only native detail receiver can request exit");s.assertUntouched();});
        test("paused fragment",r->{Scene s=new Scene(r);s.bind();s.fragment.resumed=false;s.denied();s.fragment.resumed=true;s.exits();});
        test("nonvisible fragment",r->{Scene s=new Scene(r);s.bind();s.fragment.visible=false;s.denied();s.fragment.visible=true;s.exits();});
        test("detached window",r->{Scene s=new Scene(r);s.bind();s.root.detach();s.denied();});
        test("unfocused window",r->{Scene s=new Scene(r);s.bind();s.root.focus(false);s.denied();});
        test("hidden native view",r->{Scene s=new Scene(r);s.bind();s.root.hidden=true;s.denied();});
        test("zero size native view",r->{Scene s=new Scene(r);s.bind();s.root.bounds.set(0,0,0,0);s.denied();});
        test("fragment from a different window",r->{Scene s=new Scene(r);s.bind();View wrong=new View(s.activity);wrong.attach(null);s.fragment.view=wrong;s.denied();});
        test("IME retains the Back",r->{Scene s=new Scene(r);s.bind();s.root.insets.ime=true;s.denied();s.root.insets.ime=false;s.exits();});
        test("unknown modern insets fail closed",r->{Scene s=new Scene(r);s.bind();s.root.insets=null;s.denied();});
        test("active editor retains the Back",r->{Scene s=new Scene(r);s.bind();View editor=new View(s.activity);editor.editor=true;s.root.focusView=editor;s.denied();s.root.focusView=null;s.exits();});
        test("hidden stale editor is not an active editor",r->{Scene s=new Scene(r);s.bind();View editor=new View(s.activity);editor.editor=true;editor.hidden=true;s.root.focusView=editor;s.exits();});
        test("focused same-app overlay retains Back",r->{Scene s=new Scene(r);s.bind();View overlay=new View(s.activity);overlay.attach(null);overlay.focus(true);s.denied();overlay.detach();s.exits();});
        test("LINE task is preserved",r->{Scene s=new Scene(r,false,false,PKG,false);ActivityManager.AppTask line=task(s.manager,"jp.naver.line.android",20);s.bind();s.exits();check(line.excludeCalls==0&&line.removeCalls==0,"never exclude or delete caller LINE task");check(s.activity.finishCalls==1&&s.activity.removeCalls==0,"finish only embedded TikTok Activity");});
        test("conflicting task ownership fails closed",r->{Scene s=new Scene(r);s.own.info.baseIntent.setComponent(new ComponentName("jp.naver.line.android","LINE.Root"));s.bind();s.exits();check(s.own.excludeCalls==0&&s.own.removeCalls==0,"conflicting baseActivity/baseIntent cannot remove caller task");check(s.activity.finishCalls==1,"only own Activity finished when ownership conflicts");});
        test("all owned tasks removed but another app retained",r->{Scene s=new Scene(r);s.bind();ActivityManager.AppTask second=task(s.manager,PKG,21),other=task(s.manager,"com.example.other",22);s.exits();check(second.excludeCalls==1&&second.removeCalls==1,"additional own task removed");check(other.excludeCalls==0&&other.removeCalls==0,"foreign task untouched");});
        test("one failed task does not stop remaining cleanup",r->{Scene s=new Scene(r);s.bind();s.own.failRemove=true;ActivityManager.AppTask second=task(s.manager,PKG,21);s.exits();check(second.removeCalls==1&&s.activity.finishCalls==1,"continue and close remaining tracked child");});
        test("exclude failure still attempts remove",r->{Scene s=new Scene(r);s.bind();s.own.failExclude=true;s.exits();check(s.own.removeCalls==1,"independent remove attempt");});
        test("task service failure falls back to child finish",r->{Scene s=new Scene(r);s.bind();s.manager.failList=true;s.exits();check(s.activity.finishCalls==1,"tracked Activity fallback");});
        test("no accepted cleanup request means no kill",r->{Scene s=new Scene(r,false,false,PKG,false);s.bind();s.activity.failFinish=true;check(!r.exit(s.fragment),"failed cleanup cannot claim accepted exit");Handler.drain();noKill();check(!r.suppress(s.fragment),"native return restored after all cleanup failure");});
        test("lifecycle tracking closes own later Activities",r->{Scene s=new Scene(r);s.bind();Activity later=new Activity(s.app,23,false);s.app.created(later);Application foreign=new Application("jp.naver.line.android");Activity other=new Activity(foreign,24,false);s.app.created(other);s.exits();check(later.finishCalls==1&&other.finishCalls==0,"only own tracked Activities close");check(s.app.registrations==1,"one lifecycle observer per Application");});
        test("new accepted link cancels delayed old kill",r->{Scene s=new Scene(r);s.bind();check(r.exit(s.fragment),"old exit accepted");r.prepare(new Intent(),uri("456"));Handler.advanceBy(100);noKill();check(!r.suppress(s.fragment),"old exit state canceled by new accepted entry");});
        test("new own Activity cancels delayed old kill",r->{Scene s=new Scene(r);s.bind();check(r.exit(s.fragment),"old exit accepted");Activity fresh=new Activity(s.app,99,true);s.app.created(fresh);Handler.advanceBy(100);noKill();check(!fresh.finishing,"new user entry preserved");});
        test("foreign Activity cannot cancel own exit",r->{Scene s=new Scene(r);s.bind();check(r.exit(s.fragment),"old exit accepted");Activity foreign=new Activity(new Application("jp.naver.line.android"),99,true);s.app.created(foreign);Handler.advanceBy(100);check(android.os.Process.killed.equals(Collections.singletonList(android.os.Process.OWN_PID)),"foreign callback does not alter own exit");check(!foreign.finishing,"foreign Activity untouched");});
        test("original closing Activity resume does not double exit",r->{Scene s=new Scene(r);s.bind();check(r.exit(s.fragment),"exit accepted");s.app.resumed(s.activity);Handler.advanceBy(100);check(android.os.Process.killed.size()==1,"closing Activity resume does not restart or duplicate exit");});
        System.out.println("PASS SharedLinkExitHostTest cases="+cases+" assertions="+checks+" isolated_production_runtime=true; issued Intent authority, native-host variants, stale/replayed/new routes, foreground/IME/editor guards, foreign-task preservation, cleanup-before-own-PID kill and new-entry cancellation; native Back branches audited separately; not an Android device test");
    }
}
