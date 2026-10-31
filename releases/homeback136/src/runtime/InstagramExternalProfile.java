package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.KeyEvent;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** A URL-created profile is an entry page, not every profile/OTHER screen.
 * The verified native deep-link factory supplies its exact Activity/Fragment.
 * Native profile Back runs first; only its unconsumed result may become exit.
 * No URI, username, retained tab, or elapsed timeout can grant exit by itself.
 */
final class InstagramExternalProfile {
    private InstagramExternalProfile() {}
    private static final String PROFILE="com.instagram.profile.fragment.UserDetailFragment";
    private static final String FRAGMENT="androidx.fragment.app.Fragment",MANAGER="X.00fk";
    private static final int MAIN=0x7f0b22da;
    private static final Map<Activity,Entry> ENTRIES=new WeakHashMap<Activity,Entry>();
    private static final Map<Activity,WeakReference<Intent>> INTENTS=new WeakHashMap<Activity,WeakReference<Intent>>();
    private static final Map<Object,Origin> CALLS=new WeakHashMap<Object,Origin>();
    private static final Map<Activity,Origin> PREDICTIONS=new WeakHashMap<Activity,Origin>();
    private static final Map<Activity,KeyOrigin> KEYS=new WeakHashMap<Activity,KeyOrigin>();
    private static final Map<Activity,Object> RETURNS=new WeakHashMap<Activity,Object>();
    private static final ThreadLocal<Origin> NATIVE=new ThreadLocal<Origin>();
    private static final ThreadLocal<Activity> DISPATCHING=new ThreadLocal<Activity>();
    private static final Handler MAIN_HANDLER=new Handler(Looper.getMainLooper());
    private static final class KeyOrigin {
        final long downTime;final Origin origin;
        KeyOrigin(long downTime,Origin origin){this.downTime=downTime;this.origin=origin;}
    }
    private static final class Entry {
        final WeakReference<Object> profile;final int task;
        Entry(Activity activity,Object fragment){profile=new WeakReference<Object>(fragment);task=activity.getTaskId();}
    }
    private static final class Origin {
        final WeakReference<Activity> activity;final WeakReference<Object> target;final Entry entry;final boolean clear;
        Origin(Activity owner,Object fragment,Entry entry,boolean clear){activity=new WeakReference<Activity>(owner);target=new WeakReference<Object>(fragment);this.entry=entry;this.clear=clear;}
        boolean matches(Activity owner,Object fragment,Entry current){return clear&&activity.get()==owner&&target.get()==fragment&&entry==current;}
    }
    /** Only the verified A09 profile creation callsite calls this, before attach.
     * Native URL delegates merge an original_url envelope with the parser's
     * result. The parser omits uri for /_u/ and instagram://user routes; requiring
     * that optional field made those real entry profiles invisible to Back.
     * Parser-only in-app navigation has no delegate envelope and stays native.
     */
    static void remember(Activity activity,Object fragment,Bundle route) {
        InstagramBackTrace.registration(activity,2);
        if(Looper.myLooper()!=Looper.getMainLooper()||!InstagramBack.owns(activity)||fragment==null
                ||!PROFILE.equals(fragment.getClass().getName()))return;
        try {
            InstagramBackTrace.registration(activity,3);
            if(activity.isDestroyed()||activity.isFinishing()||route==null)return;
            WeakReference<Intent> incoming=INTENTS.get(activity);
            Intent intent=incoming==null?null:incoming.get();if(intent==null)intent=activity.getIntent();
            InstagramBackTrace.registration(activity,4);
            if(Boolean.TRUE.equals(route.get("InstagramUrlLaunchConstants.EXTRA_IS_INTERNAL_LINK"))
                    ||intent!=null&&intent.getBooleanExtra("InstagramUrlLaunchConstants.EXTRA_IS_INTERNAL_LINK",false))return;
            Object original=route.get("original_url");String module=entryModule(activity);
            InstagramBackTrace.registration(activity,5);
            if(!(original instanceof String)||!Boolean.TRUE.equals(route.get("com.instagram.url.extra.IS_ON_CREATE"))
                    ||module==null||!module.equals(route.getString("com.instagram.url.constants.ARGUMENTS_KEY_ANALYTICS_MODULE_NAME")))return;
            Object uri=route.get("uri");String username=route.getString("InstagramUrlLaunchConstants.EXTRA_USER_NAME");
            InstagramBackTrace.registration(activity,6);
            int kind=profileUri((String)original,username);if(kind==0)return;
            // Canonical HTTP profiles always carry uri in the verified parser.
            // The other two forms omit it. An existing value must still agree.
            InstagramBackTrace.registration(activity,7);
            if(kind==1&&uri==null||uri!=null&&(!(uri instanceof android.net.Uri)||!original.equals(uri.toString())))return;
            InstagramBackTrace.registration(activity,8);
            if(route.get("InstagramUrlLaunchConstants.EXTRA_AUTO_LAUNCH_REEL_PARAMS")!=null
                    ||route.getString("InstagramUrlLaunchConstants.EXTRA_STARTING_TAB")!=null
                    ||route.get("InstagramUrlLaunchConstants.EXTRA_ENTRYPOINT_OVERRIDE")!=null
                    ||Boolean.TRUE.equals(route.get("InstagramUrlLaunchConstants.EXTRA_USER_OPEN_CREATION_SHEET")))return;
            ENTRIES.put(activity,new Entry(activity,fragment));RETURNS.remove(activity);
            InstagramBackTrace.registration(activity,1);
            android.util.Log.d("HiroBack131","Instagram external profile registered: native route="+kind+", host="+module);
        }catch(RuntimeException|LinkageError invalid){ InstagramBackTrace.registration(activity,9); /* An unverified route remains native. */ }
    }
    private static String entryModule(Activity activity) {
        String type=activity.getClass().getName();
        if(type.equals("com.instagram.url.UrlHandlerActivity"))return "url_handler";
        if(type.equals("com.instagram.url.UrlHandlerLauncherActivity"))return "url_handler_launcher";
        return named(activity.getClass(),"com.instagram.mainactivity.InstagramMainActivity")!=null?"main_activity":null;
    }
    /** 1=canonical web profile, 2=web /_u/ alias, 3=native user URI. */
    private static int profileUri(String value,String username) {
        try {
            if(username==null||!username.matches("[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*"))return 0;
            URI uri=new URI(value);String scheme=uri.getScheme(),host=uri.getHost();
            if(uri.getRawUserInfo()!=null||uri.getPort()!=-1||uri.getRawFragment()!=null)return 0;
            String path=uri.getRawPath();int kind;
            if("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme)) {
                if(!("www.instagram.com".equalsIgnoreCase(host)||"instagram.com".equalsIgnoreCase(host)||"m.instagram.com".equalsIgnoreCase(host))||path==null)return 0;
                if(path.endsWith("/"))path=path.substring(0,path.length()-1);
                if(path.equalsIgnoreCase("/"+username))kind=1;
                else if(path.regionMatches(true,0,"/_u/",0,4)&&path.substring(4).equalsIgnoreCase(username))kind=2;
                else return 0;
            } else if("instagram".equalsIgnoreCase(scheme)&&"user".equalsIgnoreCase(host)
                    &&(path==null||path.isEmpty()||path.equals("/")))kind=3;
            else return 0;
            // The verified web parser retains the profile URI and username
            // without treating hl as a navigation control. Accept one bounded
            // language tag; other controls can launch Stories or a sheet.
            // The native qr value remains a special entrypoint.
            String query=uri.getRawQuery();boolean userParameter=false,languageParameter=false;
            if(query!=null)for(String item:query.split("&")) {
                String[] pair=item.split("=",2);String key=URLDecoder.decode(pair[0],"UTF-8");
                if(kind==3&&key.equals("username")) {
                    if(userParameter||pair.length!=2||!URLDecoder.decode(pair[1],"UTF-8").equalsIgnoreCase(username))return 0;
                    userParameter=true;
                } else {
                    key=key.toLowerCase(Locale.ROOT);
                    if(key.equals("hl")) {
                        if(kind==3||languageParameter||pair.length!=2)return 0;
                        String language=URLDecoder.decode(pair[1],"UTF-8");
                        if(!language.matches("[a-zA-Z]{2,3}(?:[-_][a-zA-Z]{4})?(?:[-_](?:[a-zA-Z]{2}|[0-9]{3}))?"))return 0;
                        languageParameter=true;continue;
                    }
                    if(!key.equals("igsh")&&!key.equals("igshid")&&!key.startsWith("utm_"))return 0;
                    if(key.equals("utm_source")&&pair.length==2&&URLDecoder.decode(pair[1],"UTF-8").equalsIgnoreCase("qr"))return 0;
                }
            }
            return kind==3&&!userParameter?0:kind;
        }catch(Exception invalid){return 0;}
    }
    /** The URL delegate otherwise skips all fragment Back handlers when its
     * return-to-sender flag is set. Select the existing native super route for
     * this exact entry, including when a layer/IME must consume the Back first.
     * This method cannot grant exit and does not call any Back handler itself.
     */
    static boolean dispatch(Activity activity) {
        if(!InstagramBack.owns(activity)||activity.isDestroyed()||activity.isFinishing())return false;
        String type=activity.getClass().getName();
        if(!type.equals("com.instagram.url.UrlHandlerActivity")&&!type.equals("com.instagram.url.UrlHandlerLauncherActivity"))return false;
        Entry entry=entry(activity);Object profile=entry==null?null:entry.profile.get();
        if(profile==null||owner(profile)!=activity)return false;
        try {
            Object manager=call(activity,"androidx.fragment.app.FragmentActivity","A1Z",MANAGER);
            return Boolean.TRUE.equals(call(profile,FRAGMENT,"isAdded","boolean"))
                &&call(profile,FRAGMENT,"getParentFragment",FRAGMENT)==null
                &&method(named(manager.getClass(),MANAGER),"A0O",FRAGMENT,int.class).invoke(manager,MAIN)==profile;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){return false;}
    }
    static void newIntent(Activity activity,Intent intent) {
        if(!InstagramBack.owns(activity)||intent==null)return;
        WeakReference<Intent> previous=INTENTS.get(activity);
        // Several inherited onNewIntent hooks see the same dispatch. A super
        // callback must not erase the profile created by a subclass earlier.
        if(previous!=null&&previous.get()==intent)return;
        InstagramBackTrace.clear(activity);
        INTENTS.put(activity,new WeakReference<Intent>(intent));ENTRIES.remove(activity);RETURNS.remove(activity);PREDICTIONS.remove(activity);
    }
    static void destroyed(Activity activity){InstagramBackTrace.clear(activity);ENTRIES.remove(activity);INTENTS.remove(activity);PREDICTIONS.remove(activity);KEYS.remove(activity);RETURNS.remove(activity);}
    private static Entry entry(Activity activity) {
        Entry entry=ENTRIES.get(activity);
        if(entry!=null&&(entry.profile.get()==null||activity.isDestroyed()||entry.task!=activity.getTaskId())){ENTRIES.remove(activity);return null;}
        return entry;
    }
    static boolean modalHost(Activity activity) {
        Entry entry=activity==null?null:entry(activity);Object profile=entry==null?null:entry.profile.get();
        if(profile==null||!Instagram452.supports(activity)||owner(profile)!=activity)return false;
        try {
            Object parent=call(profile,FRAGMENT,"getParentFragment",FRAGMENT);
            if(parent==null||!"X.03CG".equals(parent.getClass().getName())
                    ||call(parent,FRAGMENT,"getActivity","androidx.fragment.app.FragmentActivity")!=activity
                    ||!Boolean.TRUE.equals(call(parent,FRAGMENT,"isAdded","boolean"))
                    ||!Boolean.TRUE.equals(call(parent,FRAGMENT,"isVisible","boolean")))return false;
            Object service=field(named(activity.getClass(),"com.instagram.mainactivity.InstagramMainActivity"),"A09","X.0A4v").get(activity);
            if(service==null||!"X.0A4v".equals(service.getClass().getName()))return false;
            Object reference=field(service.getClass(),"A06",WeakReference.class.getName()).get(service);
            return reference instanceof WeakReference<?> && ((WeakReference<?>)reference).get()==parent;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){return false;}
    }
    static InstagramCurrent.Result modal(Activity activity) {
        if(!modalHost(activity))return null;
        Entry entry=entry(activity);
        InstagramCurrent.Result result=InstagramExternalProfileModal.inspect(activity,entry.profile.get());
        if(result!=null)return result;
        // A changed/uncached modal route stays authoritative UNKNOWN. Falling
        // through to Main's underlying tab getter could initialize an uncached
        // lazy value and change the route while merely checking exit eligibility.
        result=new InstagramCurrent.Result(activity);result.available=true;return result;
    }
    private static Origin origin(Activity activity) {
        Entry entry=activity==null?null:entry(activity);Object target=entry==null?null:entry.profile.get();
        return new Origin(activity,target,entry,entry!=null&&!RETURNS.containsKey(activity)&&!InstagramBack.forwarding()&&safe(activity,target));
    }
    static void beginNative(Activity activity){
        KeyOrigin key=KEYS.get(activity);NATIVE.set(key==null?origin(activity):key.origin);
    }
    static void key(final Activity activity,KeyEvent event) {
        if(Looper.myLooper()!=Looper.getMainLooper()||!InstagramBack.owns(activity)||event==null||event.getKeyCode()!=KeyEvent.KEYCODE_BACK)return;
        if(event.getAction()==KeyEvent.ACTION_DOWN) {
            KeyOrigin previous=KEYS.get(activity);
            if(event.getRepeatCount()==0&&(previous==null||previous.downTime!=event.getDownTime()))
                KEYS.put(activity,new KeyOrigin(event.getDownTime(),origin(activity)));
        }else if(event.getAction()==KeyEvent.ACTION_UP) {
            KeyOrigin value=KEYS.get(activity);
            if(event.isCanceled()){value=new KeyOrigin(event.getDownTime(),new Origin(activity,null,null,false));KEYS.put(activity,value);keepReturn(activity);}
            final KeyOrigin token=value;
            MAIN_HANDLER.post(new Runnable(){public void run(){if(KEYS.get(activity)==token)KEYS.remove(activity);}});
        }
    }
    static void nativeReturn(Activity activity) {
        if(activity==null)return;
        for(Activity owner:new ArrayList<Activity>(ENTRIES.keySet()))
            if(owner!=null&&owner.getTaskId()==activity.getTaskId())keepReturn(owner);
    }
    static void endNative(){NATIVE.remove();}
    static void predictionStart(Activity activity){if(InstagramBack.owns(activity))PREDICTIONS.put(activity,origin(activity));}
    static void predictionEnd(Activity activity){PREDICTIONS.remove(activity);}
    static void dispatcherReturn(Activity activity) {
        if(entry(activity)!=null&&!origin(activity).clear)keepReturn(activity);
    }
    private static void keepReturn(final Activity activity) {
        if(activity==null)return;
        final Object token=new Object();RETURNS.put(activity,token);
        MAIN_HANDLER.post(new Runnable(){public void run(){if(RETURNS.get(activity)==token)RETURNS.remove(activity);}});
    }
    /** Captured before the original profile Back can dismiss a popup and still
     * return false. The same dispatch may not re-enter the newly cleared page.
     */
    static void begin(Object fragment) {
        Activity activity=owner(fragment);Entry entry=activity==null?null:entry(activity);
        InstagramBackTrace.begin(activity,4);
        boolean clear=eligible(activity,fragment,entry);
        CALLS.put(fragment,new Origin(activity,fragment,entry,clear));
        if(entry!=null&&!clear)keepReturn(activity);
    }
    /** Read-only eligibility shared with begin(): retain the original native,
     * key, prediction and same-dispatch return guards. */
    private static boolean eligible(Activity activity,Object fragment,Entry entry) {
        InstagramBackTrace.entry(activity,entry!=null);
        InstagramBackTrace.guards(activity,RETURNS.containsKey(activity),InstagramBack.forwarding());
        boolean clear=entry!=null&&entry.profile.get()==fragment&&!RETURNS.containsKey(activity)&&!InstagramBack.forwarding()&&safe(activity,fragment);
        Origin nativeOrigin=NATIVE.get(),prediction=PREDICTIONS.get(activity);KeyOrigin key=KEYS.get(activity);
        if(nativeOrigin!=null&&!nativeOrigin.matches(activity,fragment,entry)){InstagramBackTrace.origin(activity,1);clear=false;}
        if(prediction!=null&&!prediction.matches(activity,fragment,entry)){InstagramBackTrace.origin(activity,2);clear=false;}
        if(key!=null&&!key.origin.matches(activity,fragment,entry)){InstagramBackTrace.origin(activity,4);clear=false;}
        return clear;
    }
    /** AndroidX can pop the profile without invoking its existing native Back
     * handler. Resolve that exact public final boolean method without running
     * it during predictive start or eligibility inspection. */
    static boolean canDispatchBack(Activity activity) {
        if(Looper.myLooper()!=Looper.getMainLooper()||!InstagramBack.owns(activity))return false;
        Entry entry=entry(activity);Object fragment=entry==null?null:entry.profile.get();
        if(DISPATCHING.get()!=null||CALLS.containsKey(fragment)||!eligible(activity,fragment,entry))return false;
        try {
            method(fragment.getClass(),"onBackPressed","boolean");return true;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){return false;}
    }
    /** Invoke the already-patched native handler once. begin()/finish() retain
     * authority over exit. Any completed native invocation consumes this Back
     * so AndroidX cannot additionally pop a page in the same dispatch. */
    static boolean dispatchBack(Activity activity) {
        if(!canDispatchBack(activity))return false;
        Object fragment=entry(activity).profile.get();
        try {
            Method back=method(fragment.getClass(),"onBackPressed","boolean");
            DISPATCHING.set(activity);
            try {back.invoke(fragment,null);}
            finally {DISPATCHING.remove();CALLS.remove(fragment);}
            return true;
        }catch(InvocationTargetException nativeFailure){
            InstagramBackTrace.finish(activity,4);
            keepReturn(activity);
            android.util.Log.d("HiroBack133","Instagram external profile dispatcher: native handler threw; no second Back");
            return true;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){return false;}
    }
    static boolean finish(Object fragment,boolean consumed) {
        Origin before=CALLS.remove(fragment);Activity activity=owner(fragment);
        InstagramBackTrace.nativeResult(activity,consumed);
        InstagramBackTrace.finish(activity,0);
        if(consumed){InstagramBackTrace.finish(activity,1);if(activity!=null)keepReturn(activity);return true;}
        Entry entry=activity==null?null:entry(activity);
        // Another UserDetailFragment may be a native child of the registered
        // entry. Preserve its false result so its caller can pop it. begin()
        // still guards any same-dispatch callback on the revealed entry.
        if(entry==null||entry.profile.get()!=fragment||before==null||before.entry!=entry||before.activity.get()!=activity||before.target.get()!=fragment
                ||!safe(activity,fragment))return false;
        if(!before.clear||RETURNS.containsKey(activity)||InstagramBack.forwarding()) {
            // Native UserDetail can close its popup and still return false.
            // Claim that completed dismissal/child-return so its caller cannot
            // also pop the initial profile or finish the Activity in this Back.
            InstagramBackTrace.finish(activity,2);
            keepReturn(activity);return true;
        }
        InstagramBackTrace.finish(activity,3);
        android.util.Log.d("HiroBack131","Instagram external profile native Back: exit entry page");
        OneBackExit.terminalStopped(activity);return true;
    }
    private static Activity owner(Object fragment) {
        if(fragment==null||!PROFILE.equals(fragment.getClass().getName()))return null;
        Object value=BackNavigation.invoke(fragment,"getActivity");
        return value instanceof Activity&&InstagramBack.owns((Activity)value)?(Activity)value:null;
    }
    private static boolean safe(Activity activity,Object fragment) {
        if(InstagramBackTrace.observeReject(activity,1,activity==null||fragment==null||owner(fragment)!=activity||!HomeScreen.inputReady(activity)))return false;
        try {
            if(InstagramBackTrace.observeReject(activity,2,!Boolean.TRUE.equals(call(fragment,FRAGMENT,"isAdded","boolean"))
                    ||!Boolean.TRUE.equals(call(fragment,FRAGMENT,"isVisible","boolean"))
                    ||!Boolean.TRUE.equals(call(fragment,FRAGMENT,"isResumed","boolean"))))return false;
            Object value=call(fragment,FRAGMENT,"getView",View.class.getName());if(InstagramBackTrace.observeReject(activity,3,!(value instanceof View)))return false;
            View view=(View)value,decor=activity.getWindow().peekDecorView();Rect area=new Rect(),screen=new Rect();
            if(InstagramBackTrace.observeReject(activity,4,view.getRootView()!=decor||!InstagramBack.shown(view,area)||!InstagramBack.shown(decor,screen)
                    ||!InstagramReels.large(area,screen)))return false;
            Object children=call(fragment,FRAGMENT,"getChildFragmentManager",MANAGER);
            if(!InstagramBackTrace.observeAllow(activity,5,InstagramExternalProfileTabs.clear(fragment,children))||!InstagramBackTrace.observeAllow(activity,6,popupClear(fragment)))return false;
            if(Instagram452.supports(activity)) {
                if(InstagramBackTrace.observeParent(activity,call(fragment,FRAGMENT,"getParentFragment",FRAGMENT))==null) {
                    // Native 01PZ can put the external entry directly in MAIN
                    // when its selected tab wrapper is unavailable. That exact
                    // root transaction is authoritative; asking the Home tab
                    // getter here can instead return null or a retained tab.
                    Object manager=call(activity,"androidx.fragment.app.FragmentActivity","A1Z",MANAGER);
                    if(InstagramBackTrace.observeReject(activity,7,manager==null||call(fragment,FRAGMENT,"getParentFragmentManager",MANAGER)!=manager
                            ||InstagramBackTrace.observeContainer(activity,field(named(fragment.getClass(),FRAGMENT),"mFragmentId","int").getInt(fragment))!=MAIN
                            ||InstagramBackTrace.observeMismatch(activity,method(named(manager.getClass(),MANAGER),"A0O",FRAGMENT,int.class).invoke(manager,MAIN)!=fragment)
                            ||!managerClear(manager,fragment,false)
                            ||InstagramActivityLayers452.inspect(activity)!=InstagramActivityLayers452.CLEAR))return false;
                    InstagramBack.Snapshot layer=InstagramBack.window(decor);
                    InstagramBackTrace.layers(activity,layer);
                    if(InstagramBackTrace.observeReject(activity,8,layer.blocked||layer.unknown||layer.otherContent))return false;
                } else {
                    InstagramBack.Snapshot scene=InstagramBack.inspect(activity);
                    InstagramBackTrace.scene(activity,fragment,scene);
                    if(InstagramBackTrace.observeReject(activity,9,scene.blocked||scene.unknown||scene.otherContent||scene.current==null
                            ||!scene.current.nativeRecognized||!scene.current.stateClear||scene.current.target!=fragment))return false;
                }
            } else {
                if(InstagramBackTrace.observeReject(activity,10,InstagramReelsExit.named(activity.getClass(),"com.instagram.base.activity.IgFragmentActivity")==null))return false;
                Object manager=call(activity,"androidx.fragment.app.FragmentActivity","A1Z",MANAGER);
                Object selected=method(named(manager.getClass(),MANAGER),"A0O",FRAGMENT,int.class).invoke(manager,MAIN);
                if(InstagramBackTrace.observeReject(activity,11,selected!=fragment||call(fragment,FRAGMENT,"getParentFragment",FRAGMENT)!=null
                        ||call(fragment,FRAGMENT,"getParentFragmentManager",MANAGER)!=manager
                        ||!managerClear(manager,fragment,false)||!activityLayerClear(activity)))return false;
                InstagramBack.Snapshot layer=InstagramBack.window(decor);
                InstagramBackTrace.layers(activity,layer);
                if(InstagramBackTrace.observeReject(activity,8,layer.blocked||layer.unknown||layer.otherContent))return false;
            }
            InstagramBackTrace.safe(activity);
            return true;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError invalid){InstagramBackTrace.rejected(activity,99);return false;}
    }
    /** Read the native popup bit before its Back handler mutates it. A0B only
     * compares 01MU.A0R to 0008.A01; its complete body is patch-time verified.
     */
    private static boolean popupClear(Object fragment)throws ReflectiveOperationException {
        Object holder=field(fragment.getClass(),"A0p","X.0DiR").get(fragment);
        if(holder==null)return true;
        if(!"X.0DiR".equals(holder.getClass().getName()))return false;
        Object popup=field(holder.getClass(),"A01","X.01MU").get(holder);
        return popup==null||("X.01MU".equals(popup.getClass().getName())
                &&Boolean.FALSE.equals(method(popup.getClass(),"A0B","boolean").invoke(popup)));
    }
    private static boolean activityLayerClear(Activity activity)throws ReflectiveOperationException {
        Class<?> ig=named(activity.getClass(),"com.instagram.base.activity.IgFragmentActivity");
        Field field=field(ig,"A00","X.00i6");Object layer=field.get(activity);
        if(layer==null)return true;
        if(!"X.00i7".equals(layer.getClass().getName()))return false;
        return !field(layer.getClass(),"A0W","boolean").getBoolean(layer)
            ||method(field.getType(),"A0N",FRAGMENT).invoke(layer)==null;
    }
    static boolean managerClear(Object manager,Object allowed,boolean emptyStack)throws ReflectiveOperationException {
        if(manager==null)return false;Class<?> type=named(manager.getClass(),MANAGER);if(type==null)return false;
        if(Boolean.TRUE.equals(method(type,"A15","boolean").invoke(manager))
                ||field(type,"mExecutingActions","boolean").getBoolean(manager))return false;
        int count=((Integer)method(type,"A0L","int").invoke(manager)).intValue();if(count<0||emptyStack&&count!=0)return false;
        Object pending=field(type,"A0W",ArrayList.class.getName()).get(manager);
        if(!(pending instanceof ArrayList<?>))return false;synchronized(pending){if(!((ArrayList<?>)pending).isEmpty())return false;}
        Object store=field(type,"A0V","X.00fz").get(manager);if(store==null||!"X.00fz".equals(store.getClass().getName()))return false;
        Object raw=field(store.getClass(),"A01",ArrayList.class.getName()).get(store);if(!(raw instanceof ArrayList<?>))return false;
        List<?> pages;synchronized(raw){pages=new ArrayList<Object>((ArrayList<?>)raw);}
        boolean found=allowed==null;
        for(Object page:pages) {
            if(page==null)continue;if(named(page.getClass(),FRAGMENT)==null)return false;
            if(page==allowed){found=true;continue;}
            Class<?> dialog=named(page.getClass(),"X.007r");
            if(dialog!=null){Object value=field(dialog,"A00",Dialog.class.getName()).get(page);if(value instanceof Dialog&&((Dialog)value).isShowing())return false;}
            if(Boolean.TRUE.equals(call(page,FRAGMENT,"isVisible","boolean"))) {
                Object value=call(page,FRAGMENT,"getView",View.class.getName());
                if(!(value instanceof View)||InstagramBack.shown((View)value,new Rect()))return false;
            }
        }
        return found;
    }
    private static Class<?> named(Class<?> type,String name){return InstagramReelsExit.named(type,name);}
    private static Field field(Class<?> owner,String name,String type)throws ReflectiveOperationException {
        if(owner==null)throw new NoSuchFieldException(name);Field field=owner.getDeclaredField(name);
        if(!Modifier.isPublic(field.getModifiers())||Modifier.isStatic(field.getModifiers())||!type.equals(field.getType().getName()))throw new NoSuchFieldException(name);
        return field;
    }
    private static Method method(Class<?> owner,String name,String result,Class<?>... parameters)throws ReflectiveOperationException {
        if(owner==null)throw new NoSuchMethodException(name);Method method=owner.getDeclaredMethod(name,parameters);
        if(!Modifier.isPublic(method.getModifiers())||(!name.equals("getView")&&!Modifier.isFinal(method.getModifiers()))||Modifier.isStatic(method.getModifiers())||!result.equals(method.getReturnType().getName()))throw new NoSuchMethodException(name);
        return method;
    }
    private static Object call(Object target,String owner,String name,String result)throws ReflectiveOperationException {
        return method(named(target.getClass(),owner),name,result).invoke(target);
    }
}
