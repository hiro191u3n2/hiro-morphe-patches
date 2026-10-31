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
public final class InstagramCurrent452Fixture {
    static final int MAIN=0x7f0b22da,BASE=0x7f0b22e0;
    static final String FRAGMENT="androidx/fragment/app/Fragment",OWNER="androidx/fragment/app/FragmentActivity";
    static final String IG="com/instagram/base/activity/IgFragmentActivity",MAIN_CLASS="com/instagram/mainactivity/InstagramMainActivity";
    private static final String PREFIX="app/hiro/oneback/runtime/InstagramCurrent452Fixture$";
    public static class Node {
        public Activity activity;public View view;public boolean added=true,visible=true,resumed=true;
        public android.os.Bundle arguments;
        public com.instagram.clips.intf.ClipsViewerConfig config;
        public String module="";public int moduleReads;
        public int profileBackCalls;public boolean profileBackConsumed;public Runnable profileBackAction;
        public String getModuleName(){moduleReads++;return module;}
    }
    /** Models the existing emitted begin/native-body/finish profile wrapper.
     * No assertion in this host fixture claims Android framework execution. */
    public static boolean profileBack(Node node) {
        OneBackExit.beginInstagramExternalProfileBack(node);
        node.profileBackCalls++;
        if(node.profileBackAction!=null)node.profileBackAction.run();
        return OneBackExit.finishInstagramExternalProfileBack(node,node.profileBackConsumed);
    }
    public static class ManagerState {public int count;public boolean saved;}
    public static class PopupState {public boolean open;}
    public static class SelectedState {public int index;}
    public static class SceneActivity extends Activity {
        public Object manager,current;public boolean failCurrent;public int nativeCurrentReads;
        public SceneActivity(Application app,int id,boolean root){super(app,id,root);}
        public Object nativeManager(){return manager;}
        public Object nativeCurrent(){nativeCurrentReads++;if(failCurrent)throw new IllegalStateException("native getter failed");return current;}
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
                else NativeActivity452Fixture.writeMethod(out,m.flags,mn[i],mt[i],code,bodies[i],m.stack,m.locals);
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
            .field("mFragmentManager","LX/00fk;",1).field("mChildFragmentManager","LX/00fk;",1).field("mParentFragment","L"+FRAGMENT+";",1);
        for(String[] item:new String[][]{{"isAdded","added"},{"isVisible","visible"},{"isResumed","resumed"}})
            fragment.method(item[0],"()Z",17,1,1,get(PREFIX+"Node",item[1],"Z",0xac));
        fragment.method("getView","()Landroid/view/View;",1,1,1,get(PREFIX+"Node","view","Landroid/view/View;",0xb0));
        fragment.method("getArguments","()Landroid/os/Bundle;",17,1,1,get(PREFIX+"Node","arguments","Landroid/os/Bundle;",0xb0));
        fragment.method("setArguments","(Landroid/os/Bundle;)V",1,2,2,p->{int field=p.ref(9,PREFIX+"Node","arguments","Landroid/os/Bundle;");return code(0x2a,0x2b,0xb5,hi(field),lo(field),0xb1);});
        fragment.method("getActivity","()L"+OWNER+";",17,1,1,p->{int field=p.ref(9,PREFIX+"Node","activity","Landroid/app/Activity;"),cast=p.cls(OWNER);return code(0x2a,0xb4,hi(field),lo(field),0xc0,hi(cast),lo(cast),0xb0);});
        fragment.method("getParentFragment","()L"+FRAGMENT+";",17,1,1,get(FRAGMENT,"mParentFragment","L"+FRAGMENT+";",0xb0));
        fragment.method("getParentFragmentManager","()LX/00fk;",17,1,1,get(FRAGMENT,"mFragmentManager","LX/00fk;",0xb0));
        fragment.method("getChildFragmentManager","()LX/00fk;",17,1,1,get(FRAGMENT,"mChildFragmentManager","LX/00fk;",0xb0));put(all,fragment);
        put(all,new Type("X/00fz","java/lang/Object",0x31).constructor(false).field("A01","Ljava/util/ArrayList;",17));
        Type manager=new Type("X/00fk",PREFIX+"ManagerState",0x21).constructor(false)
            .field("A0V","LX/00fz;",17).field("A0W",fault.equals("pending_type")?"Ljava/util/List;":"Ljava/util/ArrayList;",17).field("mExecutingActions","Z",1)
            .method("A0L","()I",17,1,1,get(PREFIX+"ManagerState","count","I",0xac))
            .method("A15","()Z",17,1,1,get(PREFIX+"ManagerState","saved","Z",0xac));
        manager.method("A0O",fault.equals("find_type")?"(I)Ljava/lang/Object;":"(I)L"+FRAGMENT+";",17,2,2,p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent452Fixture","findId","(Ljava/lang/Object;I)Ljava/lang/Object;"),cast=p.cls(FRAGMENT);return code(0x2a,0x1b,0xb8,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);});
        manager.method("A0Q","(Ljava/lang/String;)L"+FRAGMENT+";",17,2,2,p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent452Fixture","findTag","(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"),cast=p.cls(FRAGMENT);return code(0x2a,0x2b,0xb8,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);});put(all,manager);
        put(all,new Type(OWNER,PREFIX+"SceneActivity",0x21).constructor(true)
            .method("A1Z","()LX/00fk;",17,1,1,objectCall(PREFIX+"SceneActivity","nativeManager","X/00fk")));
        NativeActivity452Fixture.installLayerShells(all);
        // The warm Main route hosts profiles in its live 03CG modal fragment.
        put(all,new Type("X/0A4v","java/lang/Object",0x31).constructor(false)
            .field("A05","I",1).field("A06","Ljava/lang/ref/WeakReference;",1)
            .field("A0A","Z",1).field("A04","Z",1).field("A0B","Z",1).field("A0C","Z",1));
        put(all,new Type("X/0CTn",FRAGMENT,0x421).constructor(false));
        put(all,new Type("X/03CG","X/0CTn",0x31).constructor(false).field("A01","LX/00i6;",1).field("A08","Z",1).field("A07","Z",1));
        Type main=new Type(MAIN_CLASS,IG,0x31).constructor(true).field("A09","LX/0A4v;",1)
            .method("A1f","()L"+FRAGMENT+";",fault.equals("current_nonfinal")?1:17,1,1,objectCall(PREFIX+"SceneActivity","nativeCurrent",FRAGMENT));
        if(!fault.equals("missing_controller"))main.field("A08",fault.equals("controller_type")?"Ljava/lang/Object;":"LX/00M2;",1);put(all,main);
        // The real profile Reels launcher can use ModalActivity. It has the452
        // FragmentActivity manager but no MainActivity A1f/A08 tab contracts.
        put(all,new Type("com/instagram/base/activity/BaseFragmentActivity",IG,0x21).constructor(true));
        put(all,new Type("com/instagram/modal/ModalActivity","com/instagram/base/activity/BaseFragmentActivity",0x21).constructor(true));
        put(all,new Type("com/instagram/url/UrlHandlerActivity","com/instagram/base/activity/BaseFragmentActivity",0x21).constructor(true));
        put(all,new Type("com/instagram/url/UrlHandlerLauncherActivity","com/instagram/base/activity/BaseFragmentActivity",0x31).constructor(true));
        put(all,new Type("X/00M2","java/lang/Object",0x31).constructor(false).field("A02","LX/004K;",1));
        put(all,new Type("X/004K","java/lang/Object",0x31).constructor(false).field("A03","L"+OWNER+";",17).field("current","LX/08kM;",1)
            .field("selected","L"+FRAGMENT+";",1)
            .method("A06","()LX/08kM;",17,1,1,get("X/004K","current","LX/08kM;",0xb0))
            .method("A05","()L"+FRAGMENT+";",fault.equals("tabback_current_nonfinal")?1:17,1,1,
                fault.equals("tabback_current_throws")?p->code(0x01,0xbf):get("X/004K","selected","L"+FRAGMENT+";",0xb0)));
        Type tab=new Type("X/08kM","java/lang/Enum",0x4031).field("A03","Ljava/lang/String;",17).field("A0C","LX/08kM;",0x4019).field("A08","LX/08kM;",0x4019).field("A0D","LX/08kM;",0x4019);
        tab.method("<init>","(Ljava/lang/String;ILjava/lang/String;)V",1,3,4,p->{int init=p.ref(10,"java/lang/Enum","<init>","(Ljava/lang/String;I)V"),field=p.ref(9,"X/08kM","A03","Ljava/lang/String;");return code(0x2a,0x2b,0x1c,0xb7,hi(init),lo(init),0x2a,0x2d,0xb5,hi(field),lo(field),0xb1);});
        tab.method("<clinit>","()V",8,5,0,p->{int type=p.cls("X/08kM"),init=p.ref(10,"X/08kM","<init>","(Ljava/lang/String;ILjava/lang/String;)V"),feed=p.ref(9,"X/08kM","A0C","LX/08kM;"),clips=p.ref(9,"X/08kM","A08","LX/08kM;"),a=p.add(8,p.utf("FEED")),b=p.add(8,p.utf("feed")),c=p.add(8,p.utf("CLIPS")),d=p.add(8,p.utf("clips")),switcher=p.ref(9,"X/08kM","A0D","LX/08kM;"),e=p.add(8,p.utf("FEED_SWITCHER")),f=p.add(8,p.utf("feed_switcher"));return code(0xbb,hi(type),lo(type),0x59,0x13,hi(a),lo(a),0x03,0x13,hi(b),lo(b),0xb7,hi(init),lo(init),0xb3,hi(feed),lo(feed),0xbb,hi(type),lo(type),0x59,0x13,hi(c),lo(c),0x04,0x13,hi(d),lo(d),0xb7,hi(init),lo(init),0xb3,hi(clips),lo(clips),0xbb,hi(type),lo(type),0x59,0x13,hi(e),lo(e),0x05,0x13,hi(f),lo(f),0xb7,hi(init),lo(init),0xb3,hi(switcher),lo(switcher),0xb1);});put(all,tab);
        put(all,new Type("X/0JWL","java/lang/Object",0x601).method("EKu","()Z",0x401,0,0,null).method("getValue","()Ljava/lang/Object;",0x401,0,0,null));
        put(all,new Type("X/0072","java/lang/Object",0x31,"X/0JWL").constructor(false).field("A00","Ljava/lang/Object;",1)
            .method("EKu","()Z",17,1,1,p->code(0x04,0xac)).method("getValue","()Ljava/lang/Object;",17,1,1,p->code(0x01,0xbf)));
        put(all,new Type("X/00VS",FRAGMENT,0x31).constructor(false).field("A07","LX/0JWL;",17).method("A05","()L"+FRAGMENT+";",17,1,1,nativeChild("baseChild")));
        put(all,new Type("X/02Wh",FRAGMENT,0x31).constructor(false).method("A03","()L"+FRAGMENT+";",17,1,1,nativeChild("mainChild")));
        put(all,new Type("X/052y",FRAGMENT,0x31).constructor(false).field("A06","LX/0mBs;",1).method("A07","()L"+FRAGMENT+";",17,1,1,p->code(0x01,0xbf)));
        put(all,new Type("X/04Bv",FRAGMENT,0x31).constructor(false).field("A00","Landroidx/viewpager2/widget/ViewPager2;",1).field("A05","LX/0IBl;",1).method("A00","(LX/04Bv;)L"+FRAGMENT+";",25,1,1,nativeChild("pagerChild")));
        put(all,new Type("X/0A4c","X/0CTn",0x31).constructor(false)
            .field("A0B","Landroidx/viewpager2/widget/ViewPager2;",1).field("A0G","LX/0Clp;",1)
            .method("A0e","()L"+FRAGMENT+";",17,1,1,p->code(0x01,0xbf)));
        put(all,new Type("X/04d0","X/04b8",0x421).constructor(false));
        put(all,new Type("X/0Clp","X/04d0",0x31).constructor(false)
            .field("A00","L"+FRAGMENT+";",1).field("A02","Ljava/util/List;",1));
        put(all,new Type("X/04b8","java/lang/Object",0x421).constructor(false));
        put(all,new Type("X/3e5","X/04b8",0x421).constructor(false));
        put(all,new Type("X/0IBl","X/3e5",0x31).constructor(false).field("A04","Ljava/util/List;",1));
        put(all,new Type("androidx/viewpager2/widget/ViewPager2","android/view/ViewGroup",0x31).field("A00","I",1).field("adapter","LX/04b8;",1)
            .method("getAdapter","()LX/04b8;",1,1,1,get("androidx/viewpager2/widget/ViewPager2","adapter","LX/04b8;",0xb0))
            .method("<init>","(Landroid/content/Context;)V",1,2,2,p->{int init=p.ref(10,"android/view/ViewGroup","<init>","(Landroid/content/Context;)V");return code(0x2a,0x2b,0xb7,hi(init),lo(init),0xb1);}));
        put(all,new Type("X/0mBs",PREFIX+"SelectedState",0x421).constructor(false).method("A05","()I",17,1,1,get(PREFIX+"SelectedState","index","I",0xac)));
        put(all,new Type("X/SelectedFeed","X/0mBs",0x31).constructor(false));
        put(all,new Type("X/00nV",FRAGMENT,0x31).constructor(false));
        put(all,new Type("X/07IL","java/lang/Object",0x31).constructor(false).field("A05","Z",1));
        put(all,new Type("X/02Hr",FRAGMENT,0x21).constructor(false).field("A1F","LX/07IL;",1).field("A0G","LX/00i6;",1)
            .method("A0o","()Lcom/instagram/clips/intf/ClipsViewerConfig;",17,1,1,
                get(PREFIX+"Node","config","Lcom/instagram/clips/intf/ClipsViewerConfig;",0xb0)));
        put(all,new Type("X/Ze9","X/02Hr",0x31).constructor(false));
        put(all,new Type("X/NativeProfile",FRAGMENT,0x31).constructor(false));
        put(all,new Type("X/01MU",PREFIX+"PopupState",0x21).constructor(false).method("A0B","()Z",17,1,1,get(PREFIX+"PopupState","open","Z",0xac)));
        put(all,new Type("X/0DiR","java/lang/Object",0x21).constructor(false).field("A01","LX/01MU;",1));
        put(all,new Type("com/instagram/profile/fragment/UserDetailFragment","X/0CTn",0x21).constructor(false)
            .field("A0p","LX/0DiR;",1).field("A0b","Lcom/instagram/profile/fragment/UserDetailTabController;",1)
            .method("onBackPressed","()Z",17,1,1,p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent452Fixture","profileBack","(L"+PREFIX+"Node;)Z");return code(0x2a,0xb8,hi(call),lo(call),0xac);}));
        put(all,new Type("X/0NgK","java/lang/Object",0x601));
        put(all,new Type("com/instagram/profile/fragment/ProfileTabFragment",FRAGMENT,0x31,"X/0NgK").constructor(false));
        put(all,new Type("X/00ft","java/lang/Object",0x421).constructor(false));
        put(all,new Type("X/00fy","X/00ft",0x421).constructor(false).field("A06","LX/00fk;",17).field("A01","L"+FRAGMENT+";",1));
        put(all,new Type("X/0EIp","X/00fy",0x31).constructor(false).field("A01","Ljava/util/List;",1).field("A05","Landroid/util/SparseArray;",17)
            .method("A0J","(I)LX/0NgK;",17,2,2,p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent452Fixture","profileCached","(Ljava/lang/Object;I)Ljava/lang/Object;"),cast=p.cls("X/0NgK");return code(0x2a,0x1b,0xb8,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);}));
        put(all,new Type("androidx/viewpager/widget/ViewPager","android/view/ViewGroup",0x21)
            .field("A02","I",1).field("A0W","I",1).field("A0B","LX/00ft;",1)
            .method("getCurrentItem","()I",1,1,1,get("androidx/viewpager/widget/ViewPager","A02","I",0xac))
            .method("getAdapter","()LX/00ft;",1,1,1,get("androidx/viewpager/widget/ViewPager","A0B","LX/00ft;",0xb0))
            .method("<init>","(Landroid/content/Context;)V",1,2,2,p->{int init=p.ref(10,"android/view/ViewGroup","<init>","(Landroid/content/Context;)V");return code(0x2a,0x2b,0xb7,hi(init),lo(init),0xb1);}));
        put(all,new Type("com/instagram/ui/widget/nestablescrollingview/NestableViewPager","androidx/viewpager/widget/ViewPager",0x31)
            .method("<init>","(Landroid/content/Context;)V",1,2,2,p->{int init=p.ref(10,"androidx/viewpager/widget/ViewPager","<init>","(Landroid/content/Context;)V");return code(0x2a,0x2b,0xb7,hi(init),lo(init),0xb1);}));
        put(all,new Type("X/0EdW","java/lang/Object",0x31).constructor(false)
            .field("A0F","Lcom/instagram/ui/widget/nestablescrollingview/NestableViewPager;",17));
        put(all,new Type("com/instagram/profile/fragment/UserDetailTabController","java/lang/Object",0x31).constructor(false)
            .field("A0A","LX/0EIp;",1).field("viewHolder","LX/0EdW;",1)
            .method("A0D","()LX/0NgK;",17,1,1,p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent452Fixture","currentProfileTab","(Ljava/lang/Object;)Ljava/lang/Object;"),cast=p.cls("X/0NgK");return code(0x2a,0xb8,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);}));
        return all;
    }
    static final class Scene implements AutoCloseable {
        final ClassLoader loader;final SceneActivity activity;final Application app;final ViewGroup root;
        final Object manager,controller;final ActivityManager tasks=new ActivityManager();
        Scene()throws Exception{this("");}
        Scene(String fault)throws Exception {
            final Map<String,byte[]> definitions=definitions(fault);
            loader=new ClassLoader(InstagramCurrent452Fixture.class.getClassLoader()){
                @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    byte[] bytes=definitions.get(name);if(bytes==null)return super.loadClass(name,resolve);
                    Class<?> type=findLoadedClass(name);if(type==null)type=defineClass(name,bytes,0,bytes.length);if(resolve)resolveClass(type);return type;
                }
            };
            app=new Application("com.instagram.android");app.setService(Context.ACTIVITY_SERVICE,tasks);
            boolean launcher=fault.equals("launcher"),url=fault.equals("url")||fault.equals("url_nonroot")||launcher;
            String activityType=fault.equals("modal")?"com.instagram.modal.ModalActivity":launcher?"com.instagram.url.UrlHandlerLauncherActivity":url?"com.instagram.url.UrlHandlerActivity":MAIN_CLASS.replace('/','.');
            activity=(SceneActivity)loader.loadClass(activityType).getConstructor(Application.class,int.class,boolean.class).newInstance(app,11,!fault.equals("modal")&&!fault.equals("url_nonroot"));
            manager=newManager();activity.manager=manager;controller=create("X.004K");set(controller,"A03",activity);set(controller,"current",enumTab(true));
            Object nav=create("X.00M2");set(nav,"A02",controller);if(!fault.equals("missing_controller")&&!fault.equals("modal")&&!url)set(activity,"A08",nav);
            root=(ViewGroup)activity.getWindow().getDecorView();activity.getWindow().attach();root.focus(true);
        }
        Object create(String type)throws Exception{return loader.loadClass(type).getConstructor().newInstance();}
        Object enumTab(boolean home)throws Exception{return loader.loadClass("X.08kM").getField(home?"A0C":"A08").get(null);}
        Object newManager()throws Exception {
            Object manager=create("X.00fk"),store=create("X.00fz");set(store,"A01",new ArrayList<Object>());set(manager,"A0V",store);set(manager,"A0W",new ArrayList<Object>());return manager;
        }
        Node node(String type,Node parent,int id)throws Exception{return node(type,parent,id,null);}
        Node node(String type,Node parent,int id,View view)throws Exception {
            Node node=(Node)create(type);node.activity=activity;node.view=view==null?new ViewGroup(activity):view;
            node.module=type.equals("X.02Hr")||type.equals("X.Ze9")?"clips_viewer_clips_tab":type.equals("X.00nV")?"feed_timeline":type.equals("X.NativeProfile")?"profile_view":"";
            Object manager=parent==null?this.manager:child(parent);
            set(node,"mFragmentManager",manager);set(node,"mParentFragment",parent);set(node,"mFragmentId",id);set(node,"mChildFragmentManager",newManager());
            added(manager).add(node);((ViewGroup)(parent==null?root:parent.view)).addView(node.view);
            if(type.equals("X.00VS"))cacheTab(node,"feed");
            return node;
        }
        Object child(Node node)throws Exception{return node.getClass().getField("mChildFragmentManager").get(node);}
        void select(Node root,boolean home)throws Exception{activity.current=root;set(controller,"selected",root);set(controller,"current",enumTab(home));}
        void cacheTab(Node wrapper,String key)throws Exception {Object cache=create("X.0072");set(cache,"A00",key);set(wrapper,"A07",cache);}
        void merged(Node wrapper,Integer index)throws Exception {Object selected=index==null?null:create("X.SelectedFeed");if(selected!=null)((SelectedState)selected).index=index;set(wrapper,"A06",selected);}
        ViewGroup pager(Node wrapper,Integer index)throws Exception {
            return pager(wrapper,index,2);
        }
        ViewGroup pager(Node wrapper,Integer index,int count)throws Exception {
            ViewGroup pager=index==null?null:(ViewGroup)loader.loadClass("androidx.viewpager2.widget.ViewPager2").getConstructor(Context.class).newInstance(activity);
            Object adapter=pager==null?null:create("X.0IBl");
            if(pager!=null){ArrayList<Object> pages=new ArrayList<Object>();for(int i=0;i<count;i++)pages.add(new Object());set(adapter,"A04",pages);set(pager,"adapter",adapter);set(pager,"A00",index);((ViewGroup)wrapper.view).addView(pager);}
            set(wrapper,"A05",adapter);set(wrapper,"A00",pager);return pager;
        }
        ViewGroup switcher(Node wrapper,int selected,int... ids)throws Exception {
            ViewGroup pager=(ViewGroup)loader.loadClass("androidx.viewpager2.widget.ViewPager2").getConstructor(Context.class).newInstance(activity);
            Object adapter=create("X.0Clp");ArrayList<Object> pages=new ArrayList<Object>();
            for(int id:ids){Object model=create("X.SelectedFeed");((SelectedState)model).index=id;pages.add(model);}
            set(adapter,"A00",wrapper);set(adapter,"A02",pages);set(pager,"adapter",adapter);set(pager,"A00",selected);
            set(wrapper,"A0B",pager);set(wrapper,"A0G",adapter);((ViewGroup)wrapper.view).addView(pager);return pager;
        }
        Object switcherTab()throws Exception{return loader.loadClass("X.08kM").getField("A0D").get(null);}
        void tag(Node node,String tag)throws Exception{set(node,"mTag",tag);}
        /** Audited 452 profile content uses a nested FragmentPagerAdapter. */
        Node profileTabs(Node profile,boolean retainAdjacent)throws Exception {
            Object controller=create("com.instagram.profile.fragment.UserDetailTabController"),adapter=create("X.0EIp"),holder=create("X.0EdW");
            ViewGroup pager=(ViewGroup)loader.loadClass("com.instagram.ui.widget.nestablescrollingview.NestableViewPager").getConstructor(Context.class).newInstance(activity);
            pager.bounds=new Rect(0,500,1080,2170);((ViewGroup)profile.view).addView(pager);
            set(profile,"A0b",controller);set(controller,"A0A",adapter);set(controller,"viewHolder",holder);set(holder,"A0F",pager);
            set(pager,"A02",0);set(pager,"A0W",0);set(pager,"A0B",adapter);set(adapter,"A06",child(profile));
            set(adapter,"A01",new ArrayList<Object>(java.util.Arrays.asList(new Object(),new Object())));
            android.util.SparseArray<java.lang.ref.WeakReference<Object>> cache=new android.util.SparseArray<java.lang.ref.WeakReference<Object>>();set(adapter,"A05",cache);
            Node selected=node("com.instagram.profile.fragment.ProfileTabFragment",profile,1936);selected.module="profile_tab";
            ((ViewGroup)profile.view).removeView(selected.view);pager.addView(selected.view);selected.view.bounds=new Rect(0,500,1080,2170);
            loader.loadClass("X.00fy").getDeclaredField("A01").set(adapter,selected);cache.put(0,new java.lang.ref.WeakReference<Object>(selected));
            if(retainAdjacent){Node next=node("com.instagram.profile.fragment.ProfileTabFragment",profile,1936);next.module="profile_tab";next.resumed=false;
                ((ViewGroup)profile.view).removeView(next.view);pager.addView(next.view);next.view.bounds=new Rect(1080,500,2160,2170);cache.put(1,new java.lang.ref.WeakReference<Object>(next));}
            return selected;
        }
        ViewGroup group(ViewGroup parent,String name,Rect bounds){ViewGroup view=new ViewGroup(activity);view.bounds=bounds;view.id=++HomeBackHostTest.markerId;android.content.res.Resources.IDS.put(view.id,name);parent.addView(view);return view;}
        void install(){OneBackExit.install(activity);OneBackExit.onResume(activity);Handler.drain();}
        @Override public void close(){OneBackExit.onDestroy(activity);activity.destroyed=true;root.detach();Handler.drain();WindowInspector.clear();}
    }
    static void set(Object object,String name,Object value)throws Exception {Field field=object.getClass().getField(name);field.setAccessible(true);field.set(object,value);}
    private static Body nativeChild(final String name){return p->{int call=p.ref(10,"app/hiro/oneback/runtime/InstagramCurrent452Fixture",name,"(Ljava/lang/Object;)Ljava/lang/Object;"),cast=p.cls(FRAGMENT);return code(0x2a,0xb8,hi(call),lo(call),0xc0,hi(cast),lo(cast),0xb0);};}
    public static Object pagerChild(Object node)throws Exception {Object pager=node.getClass().getField("A00").get(node);if(pager==null)return null;int index=pager.getClass().getField("A00").getInt(pager);return findTag(node.getClass().getField("mChildFragmentManager").get(node),"f"+index);}
    public static Object baseChild(Object node)throws Exception{return findId(node.getClass().getField("mChildFragmentManager").get(node),BASE);}
    public static Object mainChild(Object node)throws Exception{return findId(node.getClass().getField("mChildFragmentManager").get(node),MAIN);}
    public static Object profileCached(Object adapter,int index)throws Exception {Object value=((android.util.SparseArray<?>)adapter.getClass().getField("A05").get(adapter)).get(index);return value instanceof java.lang.ref.WeakReference<?>?((java.lang.ref.WeakReference<?>)value).get():null;}
    public static Object currentProfileTab(Object controller)throws Exception {Object holder=controller.getClass().getField("viewHolder").get(controller);if(holder==null)return null;Object pager=holder.getClass().getField("A0F").get(holder),adapter=controller.getClass().getField("A0A").get(controller);if(pager==null||adapter==null)return null;return profileCached(adapter,((Integer)pager.getClass().getMethod("getCurrentItem").invoke(pager)).intValue());}
    @SuppressWarnings("unchecked") static List<Object> added(Object manager)throws Exception {Object store=manager.getClass().getField("A0V").get(manager);return (List<Object>)store.getClass().getField("A01").get(store);}
    public static Object findId(Object manager,int id)throws Exception {List<Object> list=added(manager);for(int i=list.size()-1;i>=0;i--){Object node=list.get(i);if(node.getClass().getField("mFragmentId").getInt(node)==id)return node;}return null;}
    public static Object findTag(Object manager,String tag)throws Exception {List<Object> list=added(manager);for(int i=list.size()-1;i>=0;i--){Object node=list.get(i);if(tag.equals(node.getClass().getField("mTag").get(node)))return node;}return null;}
}
