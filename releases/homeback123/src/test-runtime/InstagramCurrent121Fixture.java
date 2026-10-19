package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exact descriptors plus native container relationships, without a device VM.
 * View counts and route transitions are explicit test inputs, not APK observations.
 */
public final class InstagramCurrent121Fixture {
    static final int MAIN=0x7f0b226f,BASE=0x7f0b2275;
    static final String FRAGMENT="androidx/fragment/app/Fragment",OWNER="androidx/fragment/app/FragmentActivity";
    static final String IG="com/instagram/base/activity/IgFragmentActivity",MAIN_CLASS="com/instagram/mainactivity/InstagramMainActivity";
    private static final String PREFIX="app/hiro/oneback/runtime/InstagramCurrent121Fixture$";
    public static class Node {
        public Activity activity;public View view;public boolean added=true,visible=true,resumed=true;
        public String module="";
        public String getModuleName(){return module;}
    }
    public static class ManagerState {public int count;public boolean saved;}
    public static class SelectedState {public int index;}
    public static class SceneActivity extends Activity {
        public Object manager,current;public boolean failCurrent;
        public SceneActivity(Application app,int id,boolean root){super(app,id,root);}
        public Object nativeManager(){return manager;}
        public Object nativeCurrent(){if(failCurrent)throw new IllegalStateException("native getter failed");return current;}
    }
    interface Body {byte[] bytes(Instagram447Fixture.Pool pool)throws Exception;}
    private static final class Member {
        final String name,type;final int flags,stack,locals;final Body body;
        Member(String n,String t,int f,int s,int l,Body b){name=n;type=t;flags=f;stack=s;locals=l;body=b;}
    }
    private static final class Type {
        final String name,parent;final int flags;final String[] interfaces;
        final List<Member> fields=new ArrayList<Member>(),methods=new ArrayList<Member>();
        Type(String n,String p,int f,String... i){name=n;parent=p;flags=f;interfaces=i;}
        Type field(String name,String type,int flags){fields.add(new Member(name,type,flags,0,0,null));return this;}
        Type method(String name,String type,int flags,int stack,int locals,Body body){methods.add(new Member(name,type,flags,stack,locals,body));return this;}
        Type constructor(final boolean activity){return method("<init>",activity?"(Landroid/app/Application;IZ)V":"()V",1,activity?4:1,activity?4:1,p->{
            int call=p.ref(10,parent,"<init>",activity?"(Landroid/app/Application;IZ)V":"()V");
            return activity?code(0x2a,0x2b,0x1c,0x1d,0xb7,hi(call),lo(call),0xb1):code(0x2a,0xb7,hi(call),lo(call),0xb1);
        });}
        byte[] bytes()throws Exception {
            Instagram447Fixture.Pool p=new Instagram447Fixture.Pool();int self=p.cls(name),sup=p.cls(parent),code=p.utf("Code");
            int[] ifaces=new int[interfaces.length];for(int i=0;i<ifaces.length;i++)ifaces[i]=p.cls(interfaces[i]);
            int[] fn=new int[fields.size()],ft=new int[fields.size()],mn=new int[methods.size()],mt=new int[methods.size()];
            byte[][] bodies=new byte[methods.size()][];
            for(int i=0;i<fields.size();i++){fn[i]=p.utf(fields.get(i).name);ft[i]=p.utf(fields.get(i).type);}
            for(int i=0;i<methods.size();i++){Member m=methods.get(i);mn[i]=p.utf(m.name);mt[i]=p.utf(m.type);bodies[i]=m.body==null?null:m.body.bytes(p);}
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(0xcafebabe);out.writeShort(0);out.writeShort(52);out.writeShort(p.values.size()+1);for(byte[] value:p.values)out.write(value);
            out.writeShort(flags);out.writeShort(self);out.writeShort(sup);out.writeShort(ifaces.length);for(int i:ifaces)out.writeShort(i);
            out.writeShort(fields.size());for(int i=0;i<fields.size();i++){out.writeShort(fields.get(i).flags);out.writeShort(fn[i]);out.writeShort(ft[i]);out.writeShort(0);}
            out.writeShort(methods.size());for(int i=0;i<methods.size();i++){
                Member m=methods.get(i);
                if(bodies[i]==null){out.writeShort(m.flags);out.writeShort(mn[i]);out.writeShort(mt[i]);out.writeShort(0);}
                else NativeActivity121Fixture.writeMethod(out,m.flags,mn[i],mt[i],code,bodies[i],m.stack,m.locals);
            }
            out.writeShort(0);return bytes.toByteArray();
        }
    }
    private static int hi(int value){return value>>>8;}private static int lo(int value){return value&255;}
    private static byte[] code(int... bytes)throws Exception{return Instagram447Fixture.code(bytes);}
    private static Body get(final String owner,final String name,final String type,final int opcode){return p->{int field=p.ref(9,owner,name,type);return code(0x2a,0xb4,hi(field),lo(field),opcode);};}
    private static Body objectCall(final String owner,final String name,final String result){return p->{int call=p.ref(10,owner,name,"()Ljava/lang/Object;"),cast=p.cls(result);return code(0x2a,0xb6,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);};}
    private static void put(Map<String,byte[]> all,Type type)throws Exception{all.put(type.name.replace('/','.'),type.bytes());}
    static Map<String,byte[]> definitions(String fault)throws Exception {
        Map<String,byte[]> all=new HashMap<String,byte[]>();
        Type fragment=new Type(FRAGMENT,PREFIX+"Node",0x21).constructor(false)
            .field("mFragmentId","I",1).field("mTag","Ljava/lang/String;",1)
            .field("mFragmentManager","LX/0dj;",1).field("mChildFragmentManager","LX/0dj;",1).field("mParentFragment","L"+FRAGMENT+";",1);
        for(String[] item:new String[][]{{"isAdded","added"},{"isVisible","visible"},{"isResumed","resumed"}})
            fragment.method(item[0],"()Z",17,1,1,get(PREFIX+"Node",item[1],"Z",0xac));
        fragment.method("getView","()Landroid/view/View;",1,1,1,get(PREFIX+"Node","view","Landroid/view/View;",0xb0));
        fragment.method("getActivity","()L"+OWNER+";",17,1,1,p->{int field=p.ref(9,PREFIX+"Node","activity","Landroid/app/Activity;"),cast=p.cls(OWNER);return code(0x2a,0xb4,hi(field),lo(field),0xc0,hi(cast),lo(cast),0xb0);});
        fragment.method("getParentFragment","()L"+FRAGMENT+";",17,1,1,get(FRAGMENT,"mParentFragment","L"+FRAGMENT+";",0xb0));
        fragment.method("getParentFragmentManager","()LX/0dj;",17,1,1,get(FRAGMENT,"mFragmentManager","LX/0dj;",0xb0));
        fragment.method("getChildFragmentManager","()LX/0dj;",17,1,1,get(FRAGMENT,"mChildFragmentManager","LX/0dj;",0xb0));put(all,fragment);
        put(all,new Type("X/0dw","java/lang/Object",0x31).constructor(false).field("A01","Ljava/util/ArrayList;",17));
        Type manager=new Type("X/0dj",PREFIX+"ManagerState",0x21).constructor(false)
            .field("A0V","LX/0dw;",17).field("A0W",fault.equals("pending_type")?"Ljava/util/List;":"Ljava/util/ArrayList;",17).field("mExecutingActions","Z",1)
            .method("A0L","()I",17,1,1,get(PREFIX+"ManagerState","count","I",0xac))
            .method("A14","()Z",17,1,1,get(PREFIX+"ManagerState","saved","Z",0xac));
        manager.method("A0O",fault.equals("find_type")?"(I)Ljava/lang/Object;":"(I)L"+FRAGMENT+";",17,2,2,p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent121Fixture","findId","(Ljava/lang/Object;I)Ljava/lang/Object;"),cast=p.cls(FRAGMENT);return code(0x2a,0x1b,0xb8,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);});
        manager.method("A0Q","(Ljava/lang/String;)L"+FRAGMENT+";",17,2,2,p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent121Fixture","findTag","(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"),cast=p.cls(FRAGMENT);return code(0x2a,0x2b,0xb8,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);});put(all,manager);
        put(all,new Type(OWNER,PREFIX+"SceneActivity",0x21).constructor(true)
            .method("A1Y","()LX/0dj;",17,1,1,objectCall(PREFIX+"SceneActivity","nativeManager","X/0dj")));
        NativeActivity121Fixture.installLayerShells(all);
        Type main=new Type(MAIN_CLASS,IG,0x21).constructor(true).field("A08","LX/3MM;",1)
            .method("A1e","()L"+FRAGMENT+";",fault.equals("current_nonfinal")?1:17,1,1,objectCall(PREFIX+"SceneActivity","nativeCurrent",FRAGMENT));
        if(!fault.equals("missing_controller"))main.field("A07",fault.equals("controller_type")?"Ljava/lang/Object;":"LX/2xZ;",1);put(all,main);
        put(all,new Type("X/2xZ","java/lang/Object",0x31).constructor(false).field("A02","LX/2yE;",1));
        put(all,new Type("X/2yE","java/lang/Object",0x31).constructor(false).field("A03","L"+OWNER+";",17).field("current","LX/2yl;",1)
            .field("selected","L"+FRAGMENT+";",1)
            .method("A06","()LX/2yl;",17,1,1,get("X/2yE","current","LX/2yl;",0xb0))
            .method("A05","()L"+FRAGMENT+";",fault.equals("tabback_current_nonfinal")?1:17,1,1,
                fault.equals("tabback_current_throws")?p->code(0x01,0xbf):get("X/2yE","selected","L"+FRAGMENT+";",0xb0)));
        Type tab=new Type("X/2yl","java/lang/Enum",0x4031).field("A03","Ljava/lang/String;",17).field("A0C","LX/2yl;",0x4019).field("A08","LX/2yl;",0x4019);
        tab.method("<init>","(Ljava/lang/String;ILjava/lang/String;)V",1,3,4,p->{int init=p.ref(10,"java/lang/Enum","<init>","(Ljava/lang/String;I)V"),field=p.ref(9,"X/2yl","A03","Ljava/lang/String;");return code(0x2a,0x2b,0x1c,0xb7,hi(init),lo(init),0x2a,0x2d,0xb5,hi(field),lo(field),0xb1);});
        tab.method("<clinit>","()V",8,5,0,p->{int type=p.cls("X/2yl"),init=p.ref(10,"X/2yl","<init>","(Ljava/lang/String;ILjava/lang/String;)V"),feed=p.ref(9,"X/2yl","A0C","LX/2yl;"),clips=p.ref(9,"X/2yl","A08","LX/2yl;"),a=p.add(8,p.utf("FEED")),b=p.add(8,p.utf("feed")),c=p.add(8,p.utf("CLIPS")),d=p.add(8,p.utf("clips"));return code(0xbb,hi(type),lo(type),0x59,0x13,hi(a),lo(a),0x03,0x13,hi(b),lo(b),0xb7,hi(init),lo(init),0xb3,hi(feed),lo(feed),0xbb,hi(type),lo(type),0x59,0x13,hi(c),lo(c),0x04,0x13,hi(d),lo(d),0xb7,hi(init),lo(init),0xb3,hi(clips),lo(clips),0xb1);});put(all,tab);
        put(all,new Type("X/Esp","java/lang/Object",0x601).method("EKu","()Z",0x401,0,0,null).method("getValue","()Ljava/lang/Object;",0x401,0,0,null));
        put(all,new Type("X/4z8","java/lang/Object",0x31,"X/Esp").constructor(false).field("A00","Ljava/lang/Object;",1)
            .method("EKu","()Z",17,1,1,p->code(0x04,0xac)).method("getValue","()Ljava/lang/Object;",17,1,1,p->code(0x01,0xbf)));
        put(all,new Type("X/0jG",FRAGMENT,0x31).constructor(false).field("A07","LX/Esp;",17).method("A03","()L"+FRAGMENT+";",17,1,1,nativeChild("baseChild")));
        put(all,new Type("X/3gY",FRAGMENT,0x31).constructor(false).method("A03","()L"+FRAGMENT+";",17,1,1,nativeChild("mainChild")));
        put(all,new Type("X/22c",FRAGMENT,0x31).constructor(false).field("A05","LX/eBE;",1).method("A07","()L"+FRAGMENT+";",17,1,1,p->code(0x01,0xbf)));
        put(all,new Type("X/9sY",FRAGMENT,0x31).constructor(false).field("A00","Landroidx/viewpager2/widget/ViewPager2;",1).field("A05","LX/A9v;",1));
        put(all,new Type("X/8QJ","java/lang/Object",0x421).constructor(false));
        put(all,new Type("X/3e5","X/8QJ",0x421).constructor(false));
        put(all,new Type("X/A9v","X/3e5",0x31).constructor(false).field("A04","Ljava/util/List;",1));
        put(all,new Type("androidx/viewpager2/widget/ViewPager2","android/view/ViewGroup",0x31).field("A00","I",1).field("adapter","LX/8QJ;",1)
            .method("getAdapter","()LX/8QJ;",1,1,1,get("androidx/viewpager2/widget/ViewPager2","adapter","LX/8QJ;",0xb0))
            .method("<init>","(Landroid/content/Context;)V",1,2,2,p->{int init=p.ref(10,"android/view/ViewGroup","<init>","(Landroid/content/Context;)V");return code(0x2a,0x2b,0xb7,hi(init),lo(init),0xb1);}));
        put(all,new Type("X/eBE",PREFIX+"SelectedState",0x421).constructor(false).method("A04","()I",17,1,1,get(PREFIX+"SelectedState","index","I",0xac)));
        put(all,new Type("X/SelectedFeed","X/eBE",0x31).constructor(false));
        put(all,new Type("X/3mI",FRAGMENT,0x31).constructor(false));
        put(all,new Type("X/21x","java/lang/Object",0x31).constructor(false).field("A05","Z",1));
        put(all,new Type("X/8Zv",FRAGMENT,0x21).constructor(false).field("A1Y","LX/21x;",1).field("A0L","LX/0wS;",1));
        put(all,new Type("X/Ze9","X/8Zv",0x31).constructor(false));
        put(all,new Type("X/NativeProfile",FRAGMENT,0x31).constructor(false));
        return all;
    }
    static final class Scene implements AutoCloseable {
        final ClassLoader loader;final SceneActivity activity;final Application app;final ViewGroup root;
        final Object manager,controller;final ActivityManager tasks=new ActivityManager();
        Scene()throws Exception{this("");}
        Scene(String fault)throws Exception {
            final Map<String,byte[]> definitions=definitions(fault);
            loader=new ClassLoader(InstagramCurrent121Fixture.class.getClassLoader()){
                @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    byte[] bytes=definitions.get(name);if(bytes==null)return super.loadClass(name,resolve);
                    Class<?> type=findLoadedClass(name);if(type==null)type=defineClass(name,bytes,0,bytes.length);if(resolve)resolveClass(type);return type;
                }
            };
            app=new Application("com.instagram.android");app.setService(Context.ACTIVITY_SERVICE,tasks);
            activity=(SceneActivity)loader.loadClass(MAIN_CLASS.replace('/','.')).getConstructor(Application.class,int.class,boolean.class).newInstance(app,11,true);
            manager=newManager();activity.manager=manager;controller=create("X.2yE");set(controller,"A03",activity);set(controller,"current",enumTab(true));
            Object nav=create("X.2xZ");set(nav,"A02",controller);if(!fault.equals("missing_controller"))set(activity,"A07",nav);
            root=(ViewGroup)activity.getWindow().getDecorView();activity.getWindow().attach();root.focus(true);
        }
        Object create(String type)throws Exception{return loader.loadClass(type).getConstructor().newInstance();}
        Object enumTab(boolean home)throws Exception{return loader.loadClass("X.2yl").getField(home?"A0C":"A08").get(null);}
        Object newManager()throws Exception {
            Object manager=create("X.0dj"),store=create("X.0dw");set(store,"A01",new ArrayList<Object>());set(manager,"A0V",store);set(manager,"A0W",new ArrayList<Object>());return manager;
        }
        Node node(String type,Node parent,int id)throws Exception{return node(type,parent,id,null);}
        Node node(String type,Node parent,int id,View view)throws Exception {
            Node node=(Node)create(type);node.activity=activity;node.view=view==null?new ViewGroup(activity):view;
            node.module=type.equals("X.8Zv")||type.equals("X.Ze9")?"clips_viewer_clips_tab":type.equals("X.3mI")?"feed_timeline":type.equals("X.NativeProfile")?"profile_view":"";
            Object manager=parent==null?this.manager:child(parent);
            set(node,"mFragmentManager",manager);set(node,"mParentFragment",parent);set(node,"mFragmentId",id);set(node,"mChildFragmentManager",newManager());
            added(manager).add(node);((ViewGroup)(parent==null?root:parent.view)).addView(node.view);
            if(type.equals("X.0jG"))cacheTab(node,"feed");
            return node;
        }
        Object child(Node node)throws Exception{return node.getClass().getField("mChildFragmentManager").get(node);}
        void select(Node root,boolean home)throws Exception{activity.current=root;set(controller,"selected",root);set(controller,"current",enumTab(home));}
        void cacheTab(Node wrapper,String key)throws Exception {Object cache=create("X.4z8");set(cache,"A00",key);set(wrapper,"A07",cache);}
        void merged(Node wrapper,Integer index)throws Exception {Object selected=index==null?null:create("X.SelectedFeed");if(selected!=null)((SelectedState)selected).index=index;set(wrapper,"A05",selected);}
        ViewGroup pager(Node wrapper,Integer index)throws Exception {
            return pager(wrapper,index,2);
        }
        ViewGroup pager(Node wrapper,Integer index,int count)throws Exception {
            ViewGroup pager=index==null?null:(ViewGroup)loader.loadClass("androidx.viewpager2.widget.ViewPager2").getConstructor(Context.class).newInstance(activity);
            Object adapter=pager==null?null:create("X.A9v");
            if(pager!=null){ArrayList<Object> pages=new ArrayList<Object>();for(int i=0;i<count;i++)pages.add(new Object());set(adapter,"A04",pages);set(pager,"adapter",adapter);set(pager,"A00",index);((ViewGroup)wrapper.view).addView(pager);}
            set(wrapper,"A05",adapter);set(wrapper,"A00",pager);return pager;
        }
        void tag(Node node,String tag)throws Exception{set(node,"mTag",tag);}
        ViewGroup group(ViewGroup parent,String name,Rect bounds){ViewGroup view=new ViewGroup(activity);view.bounds=bounds;view.id=++HomeBackHostTest.markerId;android.content.res.Resources.IDS.put(view.id,name);parent.addView(view);return view;}
        void install(){OneBackExit.install(activity);OneBackExit.onResume(activity);Handler.drain();}
        @Override public void close(){OneBackExit.onDestroy(activity);activity.destroyed=true;root.detach();Handler.drain();WindowInspector.clear();}
    }
    static void set(Object object,String name,Object value)throws Exception {Field field=object.getClass().getField(name);field.setAccessible(true);field.set(object,value);}
    private static Body nativeChild(final String name){return p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent121Fixture",name,"(Ljava/lang/Object;)Ljava/lang/Object;"),cast=p.cls(FRAGMENT);return code(0x2a,0xb8,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);};}
    public static Object baseChild(Object node)throws Exception{return findId(node.getClass().getField("mChildFragmentManager").get(node),BASE);}
    public static Object mainChild(Object node)throws Exception{return findId(node.getClass().getField("mChildFragmentManager").get(node),MAIN);}
    @SuppressWarnings("unchecked") static List<Object> added(Object manager)throws Exception {Object store=manager.getClass().getField("A0V").get(manager);return (List<Object>)store.getClass().getField("A01").get(store);}
    public static Object findId(Object manager,int id)throws Exception {List<Object> list=added(manager);for(int i=list.size()-1;i>=0;i--){Object node=list.get(i);if(node.getClass().getField("mFragmentId").getInt(node)==id)return node;}return null;}
    public static Object findTag(Object manager,String tag)throws Exception {List<Object> list=added(manager);for(int i=list.size()-1;i>=0;i--){Object node=list.get(i);if(tag.equals(node.getClass().getField("mTag").get(node)))return node;}return null;}
}
