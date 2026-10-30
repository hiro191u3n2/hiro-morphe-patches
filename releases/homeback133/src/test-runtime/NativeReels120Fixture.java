package app.hiro.oneback.runtime;

import android.app.*;
import android.view.*;
import android.graphics.Rect;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Exact JVM descriptors for the APK's two native Reels layer states. Java source
 * identifiers cannot express X.8Zv etc.; no installed app or Android VM is used. */
final class NativeReels120Fixture {
    public static class Page {
        public final Activity activity;public final View view;public final FragmentManager child=new FragmentManager();
        public Page(Activity a,View v){activity=a;view=v;}
        public Activity getActivity(){return activity;}public View getView(){return view;}
        public boolean isVisible(){return true;}public boolean isResumed(){return true;}
        public String getModuleName(){return "clips_viewer_clips_tab";}
        public FragmentManager getChildFragmentManager(){return child;}
    }
    static byte[] type(String name,String parent,String[][] fields,boolean page,boolean last)throws Exception {
        Instagram447Fixture.Pool p=new Instagram447Fixture.Pool();int self=p.cls(name),sup=p.cls(parent);
        int[] names=new int[fields.length],types=new int[fields.length];
        for(int i=0;i<fields.length;i++){names[i]=p.utf(fields[i][0]);types[i]=p.utf(fields[i][1]);}
        String args=page?"(Landroid/app/Activity;Landroid/view/View;)V":"()V";
        int init=p.utf("<init>"),signature=p.utf(args),code=p.utf("Code"),superInit=p.ref(10,parent,"<init>",args);
        byte[] body=page?Instagram447Fixture.code(0x2a,0x2b,0x2c,0xb7,superInit>>>8,superInit&255,0xb1)
                :Instagram447Fixture.code(0x2a,0xb7,superInit>>>8,superInit&255,0xb1);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0xcafebabe);out.writeShort(0);out.writeShort(52);out.writeShort(p.values.size()+1);
        for(byte[] value:p.values)out.write(value);
        out.writeShort(last?0x31:0x21);out.writeShort(self);out.writeShort(sup);out.writeShort(0);
        out.writeShort(fields.length);
        for(int i=0;i<fields.length;i++){out.writeShort(1);out.writeShort(names[i]);out.writeShort(types[i]);out.writeShort(0);}
        out.writeShort(1);Instagram447Fixture.method(out,init,signature,code,body,page?3:1,page?3:1);
        out.writeShort(0);return bytes.toByteArray();
    }
    static Page create(Activity a,View v,boolean mismatch)throws Exception {
        final Map<String,byte[]> definitions=new HashMap<String,byte[]>();
        definitions.put("X.21x",type("X/21x","java/lang/Object",new String[][]{{"A05","Z"}},false,false));
        definitions.put("X.0wS",type("X/0wS","java/lang/Object",new String[0][],false,false));
        definitions.put("X.16z",type("X/16z","X/0wS",new String[][]{{"A15","Z"}},false,true));
        definitions.put("X.8Zv",type("X/8Zv","app/hiro/oneback/runtime/NativeReels120Fixture$Page",
                new String[][]{{"A1Y",mismatch?"Ljava/lang/Object;":"LX/21x;"},{"A0L","LX/0wS;"}},true,false));
        ClassLoader loader=new ClassLoader(NativeReels120Fixture.class.getClassLoader()) {
            protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                byte[] raw=definitions.get(name);if(raw==null)return super.loadClass(name,resolve);
                Class<?> type=findLoadedClass(name);if(type==null)type=defineClass(name,raw,0,raw.length);
                if(resolve)resolveClass(type);return type;
            }
        };
        return (Page)loader.loadClass("X.8Zv").getConstructor(Activity.class,View.class).newInstance(a,v);
    }
    static Page reels(HomeBackHostTest.F f)throws Exception{return reels(f,false);}
    static Page reels(HomeBackHostTest.F f,boolean mismatch)throws Exception {
        ViewGroup view=Back118InstagramTest.group(f,"root_clips_layout",new Rect(0,0,1080,2100),f.root);
        Back118InstagramTest.group(f,"clips_viewer_container",new Rect(0,0,1080,1990),view);
        Page page=create(f.activity,view,mismatch);f.activity.fragments.fragments.add(page);return page;
    }
    static void popup(Page page)throws Exception {
        Field field=page.getClass().getField("A1Y");Object popup=field.getType().getConstructor().newInstance();
        popup.getClass().getField("A05").setBoolean(popup,true);field.set(page,popup);
    }
    static void sheet(Page page)throws Exception {
        Class<?> type=page.getClass().getClassLoader().loadClass("X.16z");Object sheet=type.getConstructor().newInstance();
        type.getField("A15").setBoolean(sheet,true);page.getClass().getField("A0L").set(page,sheet);
    }
    static void closeLayers(Page page) {
        try {
            for(String[] names:new String[][]{{"A1Y","A05"},{"A0L","A15"}}) {
                Object layer=page.getClass().getField(names[0]).get(page);
                if(layer!=null)layer.getClass().getField(names[1]).setBoolean(layer,false);
            }
        }catch(ReflectiveOperationException failed){throw new AssertionError("Native layer fixture close failed",failed);}
    }
}
