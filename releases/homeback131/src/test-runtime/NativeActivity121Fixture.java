package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exact-name host fixtures for native Main guards. They model observations,
 * not Android rendering or the execution of the uploaded APK. */
final class NativeActivity121Fixture {
    static final String FRAGMENT="androidx/fragment/app/Fragment";
    static final String OWNER="androidx/fragment/app/FragmentActivity";
    static final String IG="com/instagram/base/activity/IgFragmentActivity";
    static final String MAIN="com/instagram/mainactivity/InstagramMainActivity";
    static final String PREFIX="app/hiro/oneback/runtime/NativeActivity121Fixture$";

    public static class FragmentState {
        public boolean added=true,failAdded;
        public final boolean nativeAdded(){if(failAdded)throw new IllegalStateException("fixture isAdded failed");return added;}
    }
    public static class LayerState {
        public Object current;public boolean failLookup;public int lookups;
        public final Object nativeCurrent(){lookups++;if(failLookup)throw new IllegalStateException("fixture A0M failed");return current;}
    }
    public static class ActivityState extends Activity {
        public Object manager;public boolean failManager;
        public ActivityState(Application app,int id,boolean root){super(app,id,root);}
        public final Object nativeManager(){if(failManager)throw new IllegalStateException("fixture A1Y failed");return manager;}
    }
    public static class ShowingDialog extends Dialog {
        public boolean showing,failShowing;public int showingReads;
        public ShowingDialog(Activity activity){super(activity);}
        @Override public boolean isShowing(){showingReads++;if(failShowing)throw new IllegalStateException("fixture dialog read failed");return showing;}
    }
    interface Body {byte[] bytes(Instagram447Fixture.Pool pool)throws Exception;}
    static final class Method {
        final String name,descriptor;final int flags,stack,locals;final Body body;
        Method(String n,String d,int f,int s,int l,Body b){name=n;descriptor=d;flags=f;stack=s;locals=l;body=b;}
    }
    static byte[] type(String name,String parent,int flags,boolean activity,String[][] fields,Method... methods)throws Exception {
        Instagram447Fixture.Pool p=new Instagram447Fixture.Pool();int self=p.cls(name),sup=p.cls(parent);
        int code=p.utf("Code"),init=p.utf("<init>");String args=activity?"(Landroid/app/Application;IZ)V":"()V";
        int initType=p.utf(args),superInit=p.ref(10,parent,"<init>",args);
        byte[] initBody=activity?Instagram447Fixture.code(0x2a,0x2b,0x1c,0x1d,0xb7,superInit>>>8,superInit&255,0xb1)
                :Instagram447Fixture.code(0x2a,0xb7,superInit>>>8,superInit&255,0xb1);
        int[] fieldNames=new int[fields.length],fieldTypes=new int[fields.length];
        for(int i=0;i<fields.length;i++){fieldNames[i]=p.utf(fields[i][0]);fieldTypes[i]=p.utf(fields[i][1]);}
        int[] methodNames=new int[methods.length],methodTypes=new int[methods.length];byte[][] bodies=new byte[methods.length][];
        for(int i=0;i<methods.length;i++){methodNames[i]=p.utf(methods[i].name);methodTypes[i]=p.utf(methods[i].descriptor);bodies[i]=methods[i].body.bytes(p);}
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0xcafebabe);out.writeShort(0);out.writeShort(52);out.writeShort(p.values.size()+1);for(byte[] value:p.values)out.write(value);
        out.writeShort(flags);out.writeShort(self);out.writeShort(sup);out.writeShort(0);out.writeShort(fields.length);
        for(int i=0;i<fields.length;i++){out.writeShort(fields[i].length>2?Integer.parseInt(fields[i][2]):1);out.writeShort(fieldNames[i]);out.writeShort(fieldTypes[i]);out.writeShort(0);}
        out.writeShort(methods.length+1);writeMethod(out,1,init,initType,code,initBody,activity?4:1,activity?4:1);
        for(int i=0;i<methods.length;i++)writeMethod(out,methods[i].flags,methodNames[i],methodTypes[i],code,bodies[i],methods[i].stack,methods[i].locals);
        out.writeShort(0);return bytes.toByteArray();
    }
    static void writeMethod(DataOutputStream out,int flags,int name,int type,int code,byte[] body,int stack,int locals)throws Exception {
        out.writeShort(flags);out.writeShort(name);out.writeShort(type);out.writeShort(1);out.writeShort(code);out.writeInt(12+body.length);
        out.writeShort(stack);out.writeShort(locals);out.writeInt(body.length);out.write(body);out.writeShort(0);out.writeShort(0);
    }
    static void put(Map<String,byte[]> definitions,String name,byte[] bytes){definitions.put(name.replace('/','.'),bytes);}
    static Method objectGetter(String name,String result,String parent,String target,boolean last,boolean brokenStatic) {
        return new Method(name,"()L"+result+";",brokenStatic?9:last?17:1,1,brokenStatic?0:1,p->{
            if(brokenStatic)return Instagram447Fixture.code(0x01,0xb0);
            int method=p.ref(10,parent,target,"()Ljava/lang/Object;"),cast=p.cls(result);
            return Instagram447Fixture.code(0x2a,0xb6,method>>>8,method&255,0xc0,cast>>>8,cast&255,0xb0);
        });
    }
    /** Merge these shells into another exact-name fixture. Its Main must extend
     * IG above and declare public A08:LX/3MM; its Fragment needs final isAdded. */
    static void installLayerShells(Map<String,byte[]> definitions)throws Exception {installLayerShells(definitions,"");}
    static void installLayerShells(Map<String,byte[]> definitions,String fault)throws Exception {
        put(definitions,IG,type(IG,OWNER,0x21,true,new String[][]{{"A00","LX/0wS;"}}));
        put(definitions,"X/3MM",type("X/3MM","java/lang/Object",0x31,false,
                new String[][]{{"A02",fault.equals("modal_reference_type")?"Ljava/lang/Object;":"Ljava/lang/ref/WeakReference;"}}));
        put(definitions,"X/9ld",type("X/9ld",FRAGMENT,0x31,false,new String[0][]));
        String currentType=fault.equals("layer_getter_type")?"java/lang/Object":FRAGMENT;
        put(definitions,"X/0wS",type("X/0wS",PREFIX+"LayerState",0x21,false,new String[0][],
                objectGetter("A0M",currentType,PREFIX+"LayerState","nativeCurrent",!fault.equals("layer_getter_not_final"),fault.equals("layer_getter_static"))));
        put(definitions,"X/16z",type("X/16z","X/0wS",fault.equals("layer_not_final")?0x21:0x31,false,
                new String[][]{{"A0U",fault.equals("layer_flag_type")?"I":"Z"},{"A15","Z"}}));
        put(definitions,"X/07d",type("X/07d",FRAGMENT,0x21,false,
                new String[][]{{"A00",fault.equals("dialog_field_type")?"Ljava/lang/Object;":"Landroid/app/Dialog;"}}));
        put(definitions,"X/7Ap",type("X/7Ap","X/07d",0x21,false,new String[0][]));
        put(definitions,"X/8g8",type("X/8g8","java/lang/Object",0x21,false,new String[0][]));
        put(definitions,"X/RxR",type("X/RxR","X/7Ap",0x21,false,new String[][]{{"A00","LX/8g8;"}}));
    }
    static final class Fixture {
        final ClassLoader loader;final ActivityState activity;final Object manager,store;
        Fixture(ClassLoader l,ActivityState a,Object m,Object s){loader=l;activity=a;manager=m;store=s;}
        Object create(String name)throws Exception{return loader.loadClass(name).getConstructor().newInstance();}
        Object fragment(boolean added)throws Exception {Object f=create(FRAGMENT.replace('/','.'));((FragmentState)f).added=added;return f;}
        Object modal(Boolean added)throws Exception {
            Object host=create("X.3MM");Object page=added==null?null:create("X.9ld");
            if(page!=null)((FragmentState)page).added=added;
            host.getClass().getField("A02").set(host,new WeakReference<Object>(page));
            activity.getClass().getDeclaredField("A08").set(activity,host);return page;
        }
        LayerState layer(boolean enabled,Object current)throws Exception {
            Object layer=create("X.16z");layer.getClass().getField("A0U").setBoolean(layer,enabled);((LayerState)layer).current=current;
            loader.loadClass(IG.replace('/','.')).getDeclaredField("A00").set(activity,layer);return (LayerState)layer;
        }
        @SuppressWarnings("unchecked") List<Object> added()throws Exception{return (List<Object>)store.getClass().getField("A01").get(store);}
        ShowingDialog dialog(boolean rxr,boolean showing)throws Exception {
            Object page=create(rxr?"X.RxR":"X.07d");ShowingDialog dialog=new ShowingDialog(activity);dialog.showing=showing;
            loader.loadClass("X.07d").getDeclaredField("A00").set(page,dialog);
            if(rxr)page.getClass().getDeclaredField("A00").set(page,create("X.8g8"));
            added().add(page);return dialog;
        }
    }
    static Fixture create()throws Exception{return create("");}
    static Fixture create(String fault)throws Exception {
        final Map<String,byte[]> definitions=new HashMap<String,byte[]>();
        Method added=new Method("isAdded",fault.equals("added_return_type")?"()Ljava/lang/Boolean;":"()Z",17,1,1,p->{
            if(fault.equals("added_return_type"))return Instagram447Fixture.code(0x01,0xb0);
            int call=p.ref(10,PREFIX+"FragmentState","nativeAdded","()Z");return Instagram447Fixture.code(0x2a,0xb6,call>>>8,call&255,0xac);
        });
        put(definitions,FRAGMENT,type(FRAGMENT,PREFIX+"FragmentState",0x21,false,new String[0][],added));
        put(definitions,"X/0dw",type("X/0dw","java/lang/Object",0x21,false,new String[][]{{"A01",fault.equals("store_list_type")?"Ljava/util/List;":"Ljava/util/ArrayList;"}}));
        put(definitions,"X/0dj",type("X/0dj","java/lang/Object",0x21,false,new String[][]{{"A0V","LX/0dw;"}}));
        put(definitions,OWNER,type(OWNER,PREFIX+"ActivityState",0x21,true,new String[0][],
                objectGetter("A1Y",fault.equals("manager_return_type")?"java/lang/Object":"X/0dj",PREFIX+"ActivityState","nativeManager",true,false)));
        installLayerShells(definitions,fault);
        put(definitions,MAIN,type(MAIN,fault.equals("missing_ig_owner")?OWNER:IG,0x21,true,
                new String[][]{{"A08",fault.equals("main_field_type")?"Ljava/lang/Object;":"LX/3MM;",fault.equals("main_field_private")?"2":"1"},{"A00","Ljava/lang/Object;"}}));
        ClassLoader loader=new ClassLoader(NativeActivity121Fixture.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                byte[] bytes=definitions.get(name);if(bytes==null)return super.loadClass(name,resolve);
                Class<?> type=findLoadedClass(name);if(type==null)type=defineClass(name,bytes,0,bytes.length);if(resolve)resolveClass(type);return type;
            }
        };
        ActivityState activity=(ActivityState)loader.loadClass(MAIN.replace('/','.')).getConstructor(Application.class,int.class,boolean.class)
                .newInstance(new Application("com.instagram.android"),11,true);
        Object manager=loader.loadClass("X.0dj").getConstructor().newInstance(),store=loader.loadClass("X.0dw").getConstructor().newInstance();
        store.getClass().getField("A01").set(store,new ArrayList<Object>());manager.getClass().getField("A0V").set(manager,store);activity.manager=manager;
        return new Fixture(loader,activity,manager,store);
    }
}
