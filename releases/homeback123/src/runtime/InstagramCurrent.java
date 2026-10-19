package app.hiro.oneback.runtime;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Read-only current-route adapter for the verified Instagram 447 native API.
 * A native container's selected fragment, rather than all retained fragments,
 * determines the content whose Back is being handled. Unknown shapes fail closed.
 */
final class InstagramCurrent {
    private InstagramCurrent() {}
    static final int UNKNOWN=0,HOME=1,REELS=2,OTHER=3;
    private static final int MAIN=0x7f0b226f,BASE=0x7f0b2275;
    private static final String FRAGMENT="androidx.fragment.app.Fragment",MANAGER="X.0dj";
    private static final String MAIN_ACTIVITY="com.instagram.mainactivity.InstagramMainActivity";

    static final class Frame {
        final Object manager,owner,chosen;
        final int containerId,backStackCount,pageCount;
        final String tag;
        final boolean stateSaved,executing,pending;
        final List<?> added;
        Frame(Object manager,Object owner,Object chosen,int containerId,String tag,int count,
                boolean saved,boolean executing,boolean pending,List<?> added,int pageCount) {
            this.manager=manager;this.owner=owner;this.chosen=chosen;this.containerId=containerId;
            this.tag=tag;this.backStackCount=count;this.stateSaved=saved;
            this.executing=executing;this.pending=pending;this.added=added;
            this.pageCount=pageCount;
        }
    }
    static final class Result {
        final Activity activity;
        boolean available,nativeRecognized,stateClear,stackClear;
        int kind=UNKNOWN;
        Object selected,target,selectedTab;
        View view;
        final List<Object> path=new ArrayList<Object>();
        final List<Frame> frames=new ArrayList<Frame>();
        final Set<View> backgroundRoots=Collections.newSetFromMap(new IdentityHashMap<View,Boolean>());
        Result(Activity activity){this.activity=activity;}
    }
    private static final class Invalid extends Exception { private static final long serialVersionUID=1L; }

    static Result inspect(Activity activity) {
        Result result=new Result(activity);
        if(!InstagramBack.owns(activity))return result;
        Class<?> main=named(activity.getClass(),MAIN_ACTIVITY);
        if(main==null||named(activity.getClass(),"androidx.fragment.app.FragmentActivity")==null
                ||named(activity.getClass(),"com.instagram.base.activity.IgFragmentActivity")==null)return result;
        // Earlier supported hosts and test doubles can expose A1e alone. The
        // native hierarchy plus controller family makes this adapter authoritative.
        result.available=true;
        Method current;
        try {current=method(main,"A1e",FRAGMENT,true);}
        catch(ReflectiveOperationException|Invalid|RuntimeException|LinkageError unsupported){return result;}
        try {
            if(activity.isDestroyed()||activity.isFinishing()||activity.getWindow()==null)throw new Invalid();
            View decor=activity.getWindow().peekDecorView();Rect screen=new Rect();
            if(decor==null||!InstagramBack.shown(decor,screen))throw new Invalid();
            Object selected=current.invoke(activity);if(selected==null)throw new Invalid();
            result.selected=selected;
            Object rootManager=call(activity,"androidx.fragment.app.FragmentActivity","A1Y",MANAGER,true);
            if(rootManager==null)throw new Invalid();
            Object tab=selectedTab(activity,main);result.selectedTab=tab;
            String tabKey=(String)field(tab.getClass(),"A03",String.class.getName(),true).get(tab);
            if(tabKey==null||tabKey.length()==0)throw new Invalid();
            Set<String> tabKeys=tabKeys(tab);
            if(!tabKeys.contains(tabKey))throw new Invalid();
            boolean homeTab=field(tab.getClass(),"A0C",tab.getClass().getName(),true,true).get(null)==tab;
            boolean tabWrapper="X.0jG".equals(selected.getClass().getName());
            if(tabWrapper) {
                if(!tabKey.equals(cachedTabKey(selected)))throw new Invalid();
            } else if(find(rootManager,MAIN)!=selected)throw new Invalid();

            Object item=selected,parent=null,manager=rootManager;String routeTag=null;int routePageCount=-1;
            Set<Object> visited=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
            for(int depth=0;depth<12;depth++) {
                if(item==null||!visited.add(item))throw new Invalid();
                node(item,activity,parent,manager);
                int id=((Integer)field(named(item.getClass(),FRAGMENT),"mFragmentId","int",false).get(item)).intValue();
                Frame containing=frame(manager,parent,item,routeTag==null?id:0,routeTag,routePageCount);
                if(!containsIdentity(containing.added,item))throw new Invalid();
                result.frames.add(containing);result.path.add(item);
                if(parent==null&&tabWrapper)inactiveTabs(result,containing,item,tabKey,tabKeys,decor);
                else if(routeTag!=null)inactiveMerged(result,containing,item,routeTag,decor);
                else if(id!=0)inactiveContainer(result,containing,item,id,decor);

                String type=item.getClass().getName();
                Object child=call(item,FRAGMENT,"getChildFragmentManager",MANAGER,true);
                if(child==null)throw new Invalid();
                Object next=null;boolean wrapper=true;int chosenId=0,chosenPageCount=-1;String chosenTag=null;
                if("X.0jG".equals(type)) {
                    method(item.getClass(),"A03",FRAGMENT,true);
                    Object overlay=find(child,MAIN);
                    if(overlay!=null){next=overlay;chosenId=MAIN;}
                    else {next=find(child,BASE);chosenId=BASE;}
                    // Native EwD and onBackPressed prefer MAIN over the swipeable
                    // base. The base must not poison the overlay's own layer scan.
                    if(overlay!=null) {
                        Frame baseFrame=frame(child,item,overlay,MAIN,null);
                        inactiveContainer(result,baseFrame,overlay,BASE,decor);
                    }
                } else if("X.3gY".equals(type)) {
                    method(item.getClass(),"A03",FRAGMENT,true);
                    next=find(child,MAIN);chosenId=MAIN;
                } else if("X.22c".equals(type)) {
                    method(item.getClass(),"A07",FRAGMENT,true);
                    Object cached=field(item.getClass(),"A05","X.eBE",false).get(item);
                    if(cached==null)throw new Invalid(); // A07's fallback initializes a lazy value.
                    int index=((Integer)call(cached,"X.eBE","A04","int",true)).intValue();
                    if(index<0||index>4)throw new Invalid();
                    chosenTag="merged_feed_tab_"+index;
                    next=findTag(child,chosenTag);
                } else if("X.9sY".equals(type)) {
                    // Native ClipsTabFragment uses ViewPager2's selected index
                    // and the child tag f{index}. A nonzero Reels page remains
                    // Reels; the app's ordinary page reset is not a modal layer.
                    Object pager=field(item.getClass(),"A00","androidx.viewpager2.widget.ViewPager2",false).get(item);
                    if(pager==null||!"androidx.viewpager2.widget.ViewPager2".equals(pager.getClass().getName())
                            ||!Modifier.isFinal(pager.getClass().getModifiers())||!(pager instanceof android.view.ViewGroup))throw new Invalid();
                    Object adapter=field(item.getClass(),"A05","X.A9v",false).get(item);
                    if(adapter==null||!"X.A9v".equals(adapter.getClass().getName())||!Modifier.isFinal(adapter.getClass().getModifiers())
                            ||method(pager.getClass(),"getAdapter","X.8QJ",false).invoke(pager)!=adapter)throw new Invalid();
                    Object pages=field(adapter.getClass(),"A04",List.class.getName(),false).get(adapter);
                    if(!(pages instanceof List<?>))throw new Invalid();chosenPageCount=((List<?>)pages).size();
                    int index=field(pager.getClass(),"A00","int",false).getInt(pager);
                    if(index<0||index>=chosenPageCount)throw new Invalid();
                    chosenTag="f"+index;next=findTag(child,chosenTag);
                } else wrapper=false;
                if(wrapper) {
                    if(next==null)throw new Invalid();
                    if(chosenId!=0&&((Integer)field(named(next.getClass(),FRAGMENT),"mFragmentId","int",false).get(next)).intValue()!=chosenId)throw new Invalid();
                    if(chosenTag!=null&&!chosenTag.equals(field(named(next.getClass(),FRAGMENT),"mTag",String.class.getName(),false).get(next)))throw new Invalid();
                    parent=item;item=next;manager=child;routeTag=chosenTag;routePageCount=chosenPageCount;
                    continue;
                }

                Frame layers=frame(child,item,null,0,null);result.frames.add(layers);
                Object value=call(item,FRAGMENT,"getView",View.class.getName(),false);
                if(!(value instanceof View))throw new Invalid();
                View view=(View)value;Rect bounds=new Rect();
                if(view.getRootView()!=decor||!InstagramBack.shown(view,bounds)
                        ||!InstagramReels.large(bounds,screen))throw new Invalid();
                result.target=item;result.view=view;
                result.kind=named(item.getClass(),"X.8Zv")!=null?REELS:"X.3mI".equals(type)&&homeTab?HOME:OTHER;
                result.nativeRecognized=true;result.stateClear=true;result.stackClear=true;
                for(Frame frame:result.frames) {
                    result.stateClear&=!frame.stateSaved&&!frame.executing&&!frame.pending;
                    result.stackClear&=frame.backStackCount==0;
                }
                // A retained root may never contain the selected content itself.
                for(View background:new ArrayList<View>(result.backgroundRoots))
                    if(ancestor(background,view))result.backgroundRoots.remove(background);
                return result;
            }
        } catch(ReflectiveOperationException|Invalid|RuntimeException|LinkageError invalid) { /* Unknown is native Back. */ }
        result.kind=UNKNOWN;result.nativeRecognized=false;result.stateClear=false;result.stackClear=false;
        result.backgroundRoots.clear();return result;
    }

    private static Object selectedTab(Activity activity,Class<?> main) throws ReflectiveOperationException,Invalid {
        Object navigation=field(main,"A07","X.2xZ",false).get(activity);
        if(navigation==null||!"X.2xZ".equals(navigation.getClass().getName()))throw new Invalid();
        Object controller=field(navigation.getClass(),"A02","X.2yE",false).get(navigation);
        if(controller==null||!"X.2yE".equals(controller.getClass().getName()))throw new Invalid();
        if(field(controller.getClass(),"A03","androidx.fragment.app.FragmentActivity",true).get(controller)!=activity)throw new Invalid();
        Object tab=call(controller,"X.2yE","A06","X.2yl",true);
        if(tab==null||!"X.2yl".equals(tab.getClass().getName())||!tab.getClass().isEnum())throw new Invalid();
        return tab;
    }
    private static void node(Object value,Activity activity,Object parent,Object manager) throws ReflectiveOperationException,Invalid {
        if(!Boolean.TRUE.equals(call(value,FRAGMENT,"isAdded","boolean",true))
                ||!Boolean.TRUE.equals(call(value,FRAGMENT,"isVisible","boolean",true))
                ||!Boolean.TRUE.equals(call(value,FRAGMENT,"isResumed","boolean",true))
                ||call(value,FRAGMENT,"getActivity","androidx.fragment.app.FragmentActivity",true)!=activity
                ||call(value,FRAGMENT,"getParentFragment",FRAGMENT,true)!=parent
                ||call(value,FRAGMENT,"getParentFragmentManager",MANAGER,true)!=manager)throw new Invalid();
    }
    private static Frame frame(Object manager,Object owner,Object chosen,int id,String tag) throws ReflectiveOperationException,Invalid {
        return frame(manager,owner,chosen,id,tag,-1);
    }
    private static Frame frame(Object manager,Object owner,Object chosen,int id,String tag,int pageCount) throws ReflectiveOperationException,Invalid {
        Class<?> type=named(manager.getClass(),MANAGER);if(type==null)throw new Invalid();
        int count=((Integer)method(type,"A0L","int",true).invoke(manager)).intValue();if(count<0)throw new Invalid();
        boolean saved=Boolean.TRUE.equals(method(type,"A14","boolean",true).invoke(manager));
        boolean executing=field(type,"mExecutingActions","boolean",false).getBoolean(manager);
        Object queued=field(type,"A0W",ArrayList.class.getName(),true).get(manager);
        if(!(queued instanceof ArrayList<?>))throw new Invalid();
        boolean pending; synchronized(queued){pending=!((ArrayList<?>)queued).isEmpty();}
        Object store=field(type,"A0V","X.0dw",true).get(manager);
        if(store==null||!"X.0dw".equals(store.getClass().getName()))throw new Invalid();
        Object added=field(store.getClass(),"A01",ArrayList.class.getName(),true).get(store);
        if(!(added instanceof ArrayList<?>))throw new Invalid();
        List<?> copy;synchronized(added){copy=new ArrayList<Object>((ArrayList<?>)added);}
        return new Frame(manager,owner,chosen,id,tag,count,saved,executing,pending,copy,pageCount);
    }
    private static Object find(Object manager,int id) throws ReflectiveOperationException,Invalid {
        return method(named(manager.getClass(),MANAGER),"A0O",FRAGMENT,true,int.class).invoke(manager,id);
    }
    private static Object findTag(Object manager,String tag) throws ReflectiveOperationException,Invalid {
        return method(named(manager.getClass(),MANAGER),"A0Q",FRAGMENT,true,String.class).invoke(manager,tag);
    }
    private static void inactiveContainer(Result result,Frame frame,Object chosen,int id,View decor) throws ReflectiveOperationException,Invalid {
        for(Object fragment:frame.added)if(fragment!=null&&fragment!=chosen) {
            Class<?> type=named(fragment.getClass(),FRAGMENT);if(type==null)continue;
            if(field(type,"mFragmentId","int",false).getInt(fragment)==id)inactive(result,frame,fragment,decor);
        }
    }
    private static void inactiveTabs(Result result,Frame frame,Object chosen,String selectedKey,Set<String> tabKeys,View decor) throws ReflectiveOperationException,Invalid {
        for(Object fragment:frame.added)if(fragment!=null&&fragment!=chosen&&"X.0jG".equals(fragment.getClass().getName())) {
            String key;
            try {key=cachedTabKey(fragment);}
            catch(ReflectiveOperationException|Invalid|RuntimeException|LinkageError unmatched){continue;}
            if(key!=null&&tabKeys.contains(key)&&!key.equals(selectedKey))inactive(result,frame,fragment,decor);
        }
    }
    private static Set<String> tabKeys(Object tab) throws ReflectiveOperationException,Invalid {
        Class<?> type=tab.getClass();Set<String> keys=new HashSet<String>();
        Field key=field(type,"A03",String.class.getName(),true);
        for(Field constant:type.getDeclaredFields())if(constant.isEnumConstant()) {
            Field checked=field(type,constant.getName(),type.getName(),true,true);
            Object value=checked.get(null);if(value==null||value.getClass()!=type)throw new Invalid();
            Object text=key.get(value);if(text instanceof String&&((String)text).length()>0)keys.add((String)text);
        }
        return keys;
    }
    private static void inactiveMerged(Result result,Frame frame,Object chosen,String selectedTag,View decor) throws ReflectiveOperationException,Invalid {
        boolean pager=frame.owner!=null&&"X.9sY".equals(frame.owner.getClass().getName());
        String pattern=pager?"f[0-9]+":"merged_feed_tab_[0-4]";
        for(Object fragment:frame.added)if(fragment!=null&&fragment!=chosen) {
            Class<?> type=named(fragment.getClass(),FRAGMENT);if(type==null)continue;
            Object tag=field(type,"mTag",String.class.getName(),false).get(fragment);
            if(tag instanceof String&&!selectedTag.equals(tag)&&((String)tag).matches(pattern)) {
                if(pager) {
                    int index;try {index=Integer.parseInt(((String)tag).substring(1));}catch(NumberFormatException invalid){continue;}
                    if(index<0||index>=frame.pageCount||!("f"+index).equals(tag))continue;
                }
                inactive(result,frame,fragment,decor);
            }
        }
    }
    private static void inactive(Result result,Frame frame,Object fragment,View decor) throws ReflectiveOperationException,Invalid {
        if(!Boolean.TRUE.equals(call(fragment,FRAGMENT,"isAdded","boolean",true))
                ||!Boolean.FALSE.equals(call(fragment,FRAGMENT,"isResumed","boolean",true))
                ||call(fragment,FRAGMENT,"getActivity","androidx.fragment.app.FragmentActivity",true)!=result.activity
                ||call(fragment,FRAGMENT,"getParentFragment",FRAGMENT,true)!=frame.owner
                ||call(fragment,FRAGMENT,"getParentFragmentManager",MANAGER,true)!=frame.manager)return;
        Object value=call(fragment,FRAGMENT,"getView",View.class.getName(),false);
        if(value instanceof View&&value!=decor&&((View)value).getRootView()==decor)result.backgroundRoots.add((View)value);
    }
    private static String cachedTabKey(Object wrapper) throws ReflectiveOperationException,Invalid {
        Object cache=field(wrapper.getClass(),"A07","X.Esp",true).get(wrapper);
        if(cache==null)return null;
        // 0jG constructs this exact NONE-mode lazy (4z1 -> 4z8). Read its
        // already-cached value directly; getValue could initialize another tab.
        Class<?> type=cache.getClass();
        if(!"X.4z8".equals(type.getName())||!Modifier.isFinal(type.getModifiers()))throw new Invalid();
        Object value=field(type,"A00",Object.class.getName(),false).get(cache);
        return value instanceof String?(String)value:null;
    }
    private static boolean containsIdentity(List<?> objects,Object target){for(Object value:objects)if(value==target)return true;return false;}
    private static boolean ancestor(View parent,View child) {
        for(int depth=0;child!=null&&depth<128;depth++,child=child.getParent() instanceof View?(View)child.getParent():null)
            if(parent==child)return true;
        return false;
    }
    private static Object call(Object object,String owner,String name,String returns,boolean last) throws ReflectiveOperationException,Invalid {
        if(object==null)throw new Invalid();return method(named(object.getClass(),owner),name,returns,last).invoke(object);
    }
    private static Class<?> named(Class<?> type,String name) {
        for(Class<?> current=type;current!=null;current=current.getSuperclass())if(name.equals(current.getName()))return current;
        return null;
    }
    private static Method method(Class<?> owner,String name,String returns,boolean last,Class<?>... parameters) throws ReflectiveOperationException,Invalid {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new Invalid();
        Method method=owner.getDeclaredMethod(name,parameters);int modifiers=method.getModifiers();
        if(!Modifier.isPublic(modifiers)||Modifier.isStatic(modifiers)||(last&&!Modifier.isFinal(modifiers))
                ||!returns.equals(method.getReturnType().getName()))throw new Invalid();
        return method;
    }
    private static Field field(Class<?> owner,String name,String type,boolean last) throws ReflectiveOperationException,Invalid {
        return field(owner,name,type,last,false);
    }
    private static Field field(Class<?> owner,String name,String type,boolean last,boolean global) throws ReflectiveOperationException,Invalid {
        if(owner==null||!Modifier.isPublic(owner.getModifiers()))throw new Invalid();
        Field field=owner.getDeclaredField(name);int modifiers=field.getModifiers();
        if(!Modifier.isPublic(modifiers)||Modifier.isStatic(modifiers)!=global||(last&&!Modifier.isFinal(modifiers))
                ||!type.equals(field.getType().getName()))throw new Invalid();
        return field;
    }
}
