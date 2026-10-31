package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** The original 452 Home receiver has its own "Back again to exit" branch.
 * Only that verified branch, after its original child Back returned false,
 * may use this fallback. Generic content heuristics do not grant permission.
 */
final class InstagramNativeHome {
    private InstagramNativeHome() {}
    private static final String HOME="X.00nV",FRAGMENT="androidx.fragment.app.Fragment";
    private static final String MAIN_ACTIVITY="com.instagram.mainactivity.InstagramMainActivity",MANAGER="X.00fk";
    private static final int MAIN=0x7f0b22da,BASE=0x7f0b22e0;
    private static final Handler HANDLER=new Handler(Looper.getMainLooper());
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<Scope>();
    private static final Map<Activity,Key> KEYS=new WeakHashMap<Activity,Key>();
    private static final Map<Activity,Key> PREDICTIONS=new WeakHashMap<Activity,Key>();
    private static final class Scope {
        final Activity activity;final int task;final Object initial,initialView;final boolean clear;
        Object feed;View view;boolean begun,returned,consumed,used;
        Scope(Activity a,Object target,boolean ready){activity=a;task=a.getTaskId();initial=target;
            initialView=BackNavigation.invoke(target,"getView");clear=ready;}
    }
    private static final class Key {
        final long time;final boolean clear;final WeakReference<Object> initial;final WeakReference<Object> view;boolean canceled;
        Key(long time,Object target,boolean clear){this.time=time;this.clear=clear;
            initial=new WeakReference<Object>(target);view=new WeakReference<Object>(BackNavigation.invoke(target,"getView"));}
        boolean matches(Object target){return clear&&!canceled&&target!=null&&initial.get()==target
                &&view.get()!=null&&view.get()==BackNavigation.invoke(target,"getView");}
    }
    static void beginScope(Activity activity) {
        if(Looper.myLooper()!=Looper.getMainLooper())return;
        CURRENT.remove();
        if(!InstagramBack.owns(activity))return;
        boolean ready=input(activity);Object target=ready?selectedHome(activity):null;Key key=KEYS.get(activity),prediction=PREDICTIONS.get(activity);
        CURRENT.set(new Scope(activity,target,target!=null&&ready
                &&(key==null||key.matches(target))&&(prediction==null||prediction.matches(target))));
    }
    static void endScope(){if(Looper.myLooper()==Looper.getMainLooper())CURRENT.remove();}
    static void key(final Activity activity,KeyEvent event) {
        if(Looper.myLooper()!=Looper.getMainLooper()||!InstagramBack.owns(activity)||event==null
                ||event.getKeyCode()!=KeyEvent.KEYCODE_BACK)return;
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0) {
            Key previous=KEYS.get(activity);
            if(previous==null||previous.time!=event.getDownTime()) {
                boolean ready=input(activity);Object target=ready?selectedHome(activity):null;
                KEYS.put(activity,new Key(event.getDownTime(),target,target!=null&&ready));
            }
        } else if(event.getAction()==KeyEvent.ACTION_UP) {
            Key key=KEYS.get(activity);
            if(key==null||key.time!=event.getDownTime())key=new Key(event.getDownTime(),null,false);
            if(event.isCanceled())key.canceled=true;
            final Key token=key;KEYS.put(activity,key);
            if(key.canceled){Scope scope=CURRENT.get();if(scope!=null&&scope.activity==activity)scope.used=true;}
            HANDLER.post(new Runnable(){public void run(){if(KEYS.get(activity)==token)KEYS.remove(activity);}});
        }
    }
    static void destroyed(Activity activity) {
        KEYS.remove(activity);PREDICTIONS.remove(activity);Scope scope=CURRENT.get();if(scope!=null&&scope.activity==activity)CURRENT.remove();
    }
    static void predictionStart(Activity activity) {
        if(Looper.myLooper()!=Looper.getMainLooper()||!InstagramBack.owns(activity))return;
        Key previous=PREDICTIONS.get(activity);if(previous!=null&&!previous.canceled)return;
        boolean ready=input(activity);Object target=ready?selectedHome(activity):null;
        PREDICTIONS.put(activity,new Key(-1,target,target!=null&&ready));
    }
    static void predictionCancel(final Activity activity) {
        if(Looper.myLooper()!=Looper.getMainLooper()||!InstagramBack.owns(activity))return;
        Key key=PREDICTIONS.get(activity);if(key==null)key=new Key(-1,null,false);key.canceled=true;
        final Key token=key;PREDICTIONS.put(activity,key);
        Scope scope=CURRENT.get();if(scope!=null&&scope.activity==activity)scope.used=true;
        HANDLER.post(new Runnable(){public void run(){if(PREDICTIONS.get(activity)==token)PREDICTIONS.remove(activity);}});
    }
    static void predictionEnd(Activity activity) {
        Key key=PREDICTIONS.get(activity);if(key==null||!key.canceled)PREDICTIONS.remove(activity);
    }
    static void begin(Object fragment) {
        Scope scope=CURRENT.get();
        if(scope==null||scope.used)return;
        // A second Home callback in one native transaction is not a new Back.
        if(scope.begun){scope.used=true;return;}
        scope.begun=true;
        if(!scope.clear||fragment!=scope.initial||!live(scope,fragment)||scope.initialView==null
                ||BackNavigation.invoke(fragment,"getView")!=scope.initialView)return;
        scope.feed=fragment;scope.view=(View)BackNavigation.invoke(fragment,"getView");
    }
    static boolean finish(Object fragment,boolean consumed) {
        Scope scope=CURRENT.get();
        if(scope!=null&&scope.feed==fragment){scope.returned=true;scope.consumed=consumed;}
        return consumed;
    }
    static boolean prompt(Object fragment) {
        if(Looper.myLooper()!=Looper.getMainLooper())return false;
        Scope scope=CURRENT.get();
        if(scope==null||scope.used||!scope.clear||scope.feed!=fragment||!scope.returned||scope.consumed)return false;
        Key key=KEYS.get(scope.activity),prediction=PREDICTIONS.get(scope.activity);
        if(key!=null&&!key.matches(fragment)||prediction!=null&&!prediction.matches(fragment))return false;
        if(!input(scope.activity)||!live(scope,fragment)||BackNavigation.invoke(fragment,"getView")!=scope.view
                ||selectedHome(scope.activity)!=fragment)return false;
        scope.used=true;
        android.util.Log.d("HiroBack134","Instagram native Home exit prompt: remove task");
        OneBackExit.terminalStopped(scope.activity);return true;
    }
    private static boolean live(Scope scope,Object fragment) {
        if(fragment==null||!HOME.equals(fragment.getClass().getName())||scope.activity.isDestroyed()
                ||scope.activity.isFinishing()||scope.activity.isChangingConfigurations()||scope.activity.getTaskId()!=scope.task)return false;
        if(BackNavigation.invoke(fragment,"getActivity")!=scope.activity
                ||!Boolean.TRUE.equals(BackNavigation.invoke(fragment,"isAdded"))
                ||!Boolean.TRUE.equals(BackNavigation.invoke(fragment,"isVisible"))
                ||!Boolean.TRUE.equals(BackNavigation.invoke(fragment,"isResumed")))return false;
        Object view=BackNavigation.invoke(fragment,"getView");
        View decor=scope.activity.getWindow()==null?null:scope.activity.getWindow().peekDecorView();
        return view instanceof View&&decor!=null&&((View)view).getRootView()==decor&&InstagramBack.shown((View)view,new Rect());
    }
    /** Keep actual input windows and explicit sheets. Captions, feed contents,
     * retained fragment names and selected-tab caches are not exit vetoes here.
     */
    private static boolean input(Activity activity) {
        if(!HomeScreen.inputReady(activity)||InstagramActivityLayers452.inspect(activity)!=InstagramActivityLayers452.CLEAR)return false;
        try {
            View root=activity.getWindow().peekDecorView();ArrayDeque<View> queue=new ArrayDeque<View>();queue.add(root);int count=0;
            while(!queue.isEmpty()) {
                if(++count>1800)return false;
                View view=queue.removeFirst();if(!InstagramBack.shown(view,new Rect()))continue;
                String id=TikTokScreen.id(view);
                if("bottom_sheet".equals(id)||"comment_sheet".equals(id)||"comments_sheet".equals(id)||"share_sheet".equals(id)
                        ||"design_bottom_sheet".equals(id)||"comment_edit_text".equals(id))return false;
                if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;if(group.getChildCount()>1800-count)return false;
                    for(int i=0;i<group.getChildCount();i++)queue.add(group.getChildAt(i));}
            }
            return true;
        }catch(RuntimeException|LinkageError invalid){return false;}
    }
    /** Read only the native selected receiver needed for initial identity.
     * This is deliberately independent of the broader Home/Reels classifier.
     */
    private static Object selectedHome(Activity activity) {
        if(!InstagramBack.owns(activity)||activity.isDestroyed()||activity.isFinishing())return null;
        try {
            Object page=call(activity,MAIN_ACTIVITY,"A1f",FRAGMENT);
            for(int depth=0;page!=null&&depth<8;depth++) {
                if(BackNavigation.invoke(page,"getActivity")!=activity)return null;
                String type=page.getClass().getName();if(HOME.equals(type))return page;
                if("X.0A4c".equals(type)) {
                    Object pager=field(page,"X.0A4c","A0B","androidx.viewpager2.widget.ViewPager2").get(page);
                    Object adapter=field(page,"X.0A4c","A0G","X.0Clp").get(page);
                    if(pager==null||adapter==null||!"X.0Clp".equals(adapter.getClass().getName())
                            ||field(adapter,"X.0Clp","A00",FRAGMENT).get(adapter)!=page)return null;
                    int index=field(pager,"androidx.viewpager2.widget.ViewPager2","A00","int").getInt(pager);
                    Object pages=field(adapter,"X.0Clp","A02",List.class.getName()).get(adapter);
                    if(!(pages instanceof List<?>)||index<0||index>=((List<?>)pages).size())return null;
                    Object model=((List<?>)pages).get(index);int id=((Integer)call(model,"X.0mBs","A05","int")).intValue();
                    if(id<0||id>4)return null;
                    Object manager=call(page,FRAGMENT,"getChildFragmentManager",MANAGER);
                    page=method(manager,MANAGER,"A0Q",FRAGMENT,String.class).invoke(manager,"f"+id);continue;
                }
                if("X.052y".equals(type)) {
                    Object model=field(page,"X.052y","A06","X.0mBs").get(page);if(model==null)return null;
                    int index=((Integer)call(model,"X.0mBs","A05","int")).intValue();if(index<0||index>4)return null;
                    Object manager=call(page,FRAGMENT,"getChildFragmentManager",MANAGER);
                    page=method(manager,MANAGER,"A0Q",FRAGMENT,String.class).invoke(manager,"merged_feed_tab_"+index);continue;
                }
                if(!"X.00VS".equals(type)&&!"X.02Wh".equals(type))return null;
                Object manager=call(page,FRAGMENT,"getChildFragmentManager",MANAGER);
                Object next=method(manager,MANAGER,"A0O",FRAGMENT,int.class).invoke(manager,MAIN);
                if(next==null&&"X.00VS".equals(type))next=method(manager,MANAGER,"A0O",FRAGMENT,int.class).invoke(manager,BASE);
                page=next;
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){return null;}
        return null;
    }
    private static Object call(Object object,String owner,String name,String result)throws ReflectiveOperationException {
        return method(object,owner,name,result).invoke(object);
    }
    private static Field field(Object object,String owner,String name,String result)throws ReflectiveOperationException {
        Class<?> type=object==null?null:object.getClass();while(type!=null&&!owner.equals(type.getName()))type=type.getSuperclass();
        if(type==null)throw new NoSuchFieldException(owner+"."+name);
        Field field=type.getDeclaredField(name);int flags=field.getModifiers();
        if(!Modifier.isPublic(flags)||Modifier.isStatic(flags)||!result.equals(field.getType().getName()))
            throw new NoSuchFieldException("Changed native Home field "+owner+"."+name);
        return field;
    }
    private static Method method(Object object,String owner,String name,String result,Class<?>...args)throws ReflectiveOperationException {
        Class<?> type=object==null?null:object.getClass();while(type!=null&&!owner.equals(type.getName()))type=type.getSuperclass();
        if(type==null)throw new NoSuchMethodException(owner+"."+name);
        Method method=type.getDeclaredMethod(name,args);int flags=method.getModifiers();
        if(!Modifier.isPublic(flags)||!Modifier.isFinal(flags)||Modifier.isStatic(flags)||!result.equals(method.getReturnType().getName()))
            throw new NoSuchMethodException("Changed native Home receiver "+owner+"."+name);
        return method;
    }
}
