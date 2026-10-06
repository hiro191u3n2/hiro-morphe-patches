from pathlib import Path
import subprocess, shutil

def test(root, work):
    folder=work/'restart-tests';folder.mkdir();src=folder/'src';src.mkdir();classes=folder/'classes';classes.mkdir()
    sources={
    'android/content/Context.java':'package android.content; public class Context {}',
    'android/util/Log.java':'package android.util; public class Log { public static int w(String t,String s,Throwable e){return 0;} public static int i(String t,String s){return 0;} }',
    'android/view/View.java':'''package android.view; public class View { public static final int VISIBLE=0; public boolean attached=true,shown=true,focus=true;public int visibility=0,w=1080,h=1920;public View root=this;public boolean isAttachedToWindow(){return attached;}public boolean isShown(){return shown;}public boolean hasWindowFocus(){return focus;}public int getWindowVisibility(){return visibility;}public int getWidth(){return w;}public int getHeight(){return h;}public View getRootView(){return root;} }''',
    'com/bytedance/corecamera/ui/view/CameraShadeView.java':'package com.bytedance.corecamera.ui.view; public class CameraShadeView extends android.view.View {}',
    'com/hiro/ulike/PreviewLayout1922.java':'package com.hiro.ulike; import com.bytedance.corecamera.ui.view.CameraShadeView; public class PreviewLayout1922 {static CameraShadeView active;public static CameraShadeView gestureView1925(){return active;}}',
    'com/hiro/ulike/ManualLens170.java':'''package com.hiro.ulike; import java.util.*; public class ManualLens170 {static final Map<String,Object> f=new HashMap<>();static Object get(String n)throws ReflectiveOperationException{return f.get(n);}static long number(String n)throws ReflectiveOperationException{return ((Number)get(n)).longValue();}static boolean yes(String n)throws ReflectiveOperationException{return Boolean.TRUE.equals(get(n));}}''',
    'com/hiro/ulike/OpticalZoom.java':'''package com.hiro.ulike;import android.content.Context; public class OpticalZoom {static int tracks;static Object tracked;static Context context;static Object field(Object o,String n)throws ReflectiveOperationException{return o.getClass().getField(n).get(o);}public static void track(Object o,Context c){tracks++;tracked=o;context=c;} static final class Route{final long epoch;final boolean rear;final int nominal;final boolean physical;volatile boolean failed; Route(long e,boolean r,int n,boolean p){epoch=e;rear=r;nominal=n;physical=p;}boolean physical(){return physical;}} }''',
    'com/hiro/ulike/RestartTest1926.java':r'''package com.hiro.ulike;
import java.util.*;import java.lang.ref.WeakReference;import android.view.View;import android.content.Context;import com.bytedance.corecamera.ui.view.CameraShadeView;
public class RestartTest1926 {
 static Object keepCapture;static int count;static void check(boolean b,String why){count++;if(!b)throw new AssertionError(why);}
 public static class Capture{public Object d=new Context(),a=new Object(),c=new Object();}
 public static class Settings{public boolean u0=true;public int M=0;public int l=0;public int width=4080,height=3060;}
 public static class Mode{public Settings h=new Settings();}
 static void reset(){ManualLens170.f.clear();ManualLens170.f.put("LOCK",new Object());ManualLens170.f.put("foreground",true);ManualLens170.f.put("epoch",7L);ManualLens170.f.put("capture",new WeakReference<Object>(keepCapture=new Capture()));ManualLens170.f.put("modes",new HashMap<Object,OpticalZoom.Route>());CameraShadeView v=new CameraShadeView();v.root=new View();PreviewLayout1922.active=v;}
 static OpticalZoom.Route bind(Mode m,boolean rear,boolean physical,int nominal){OpticalZoom.Route r=new OpticalZoom.Route(7,rear,nominal,physical);((Map)ManualLens170.f.get("modes")).put(m,r);ManualLens170.f.put("active",r);return r;}
 public static void main(String[]args){
 reset();Capture c=new Capture();RearRestart1926.initialized(c);check(OpticalZoom.tracks==1&&OpticalZoom.tracked==c&&OpticalZoom.context==c.d,"native init completion tracked once");
 for(int i=0;i<4;i++){Capture bad=new Capture();if(i==0)bad.a=null;if(i==1)bad.c=null;if(i==2)bad.d=null;if(i==3)bad.d=new Object();RearRestart1926.initialized(bad);check(OpticalZoom.tracks==1,"partial initialization excluded "+i);}
 RearRestart1926.initialized(null);RearRestart1926.initialized(new Object());check(OpticalZoom.tracks==1,"null and missing native fields safe");
 for(boolean rear:new boolean[]{false,true})for(boolean physical:new boolean[]{false,true})for(int kind:new int[]{0,1,2})for(int result:new int[]{0,-439}){
  reset();Mode m=new Mode();m.h.M=kind;bind(m,rear,physical,1);RearRestart1926.prepared(m,result);check(m.h.u0!= (rear&&physical&&kind<=1&&result==0),"deferred policy combination");check(m.h.l==0&&m.h.width==4080&&m.h.height==3060,"facing and dimensions unchanged");
 }
 for(int kind=0;kind<7;kind++){
  reset();Mode m=new Mode();OpticalZoom.Route r=bind(m,true,true,1);
  if(kind==0)ManualLens170.f.put("epoch",8L);if(kind==1)ManualLens170.f.put("active",new OpticalZoom.Route(7,true,1,true));if(kind==2)ManualLens170.f.put("foreground",false);if(kind==3)r.failed=true;if(kind==4)((Map)ManualLens170.f.get("modes")).clear();if(kind==5)m.h=null;if(kind==6)bind(m,true,true,0);
  RearRestart1926.prepared(m,0);check(m.h==null||m.h.u0,"stale/front/background preparation excluded "+kind);
 }
 reset();Mode m=new Mode();bind(m,true,true,1);RearRestart1926.prepared(m,0);RearRestart1926.prepared(m,0);check(!m.h.u0,"idempotent concrete-surface policy");RearRestart1926.prepared(null,0);RearRestart1926.prepared(new Object(),0);check(true,"missing mode safely ignored");
 for(int mask=0;mask<64;mask++){
  reset();CameraShadeView s=PreviewLayout1922.active;View root=s.root;
  boolean foreground=(mask&1)==0,attached=(mask&2)==0,focus=(mask&4)==0,visible=(mask&8)==0,measured=(mask&16)==0,owner=(mask&32)==0;
  ManualLens170.f.put("foreground",foreground);s.attached=attached;root.focus=focus;root.visibility=visible?0:8;s.w=measured?1080:0;if(!owner)ManualLens170.f.put("capture",new WeakReference<Object>(null));
  s.shown=false;check(RearRestart1926.recoveryWindowReady()==(foreground&&attached&&focus&&visible&&measured&&owner),"live-window readiness combination "+mask);
 }
 reset();PreviewLayout1922.active=null;check(!RearRestart1926.recoveryWindowReady(),"no old shade reuse");reset();PreviewLayout1922.active.root=null;check(!RearRestart1926.recoveryWindowReady(),"null window denied");reset();PreviewLayout1922.active.root.shown=false;check(!RearRestart1926.recoveryWindowReady(),"hidden window denied");reset();PreviewLayout1922.active.visibility=8;check(!RearRestart1926.recoveryWindowReady(),"hidden window visibility denied");reset();PreviewLayout1922.active.h=0;check(!RearRestart1926.recoveryWindowReady(),"unmeasured view denied");
 System.out.println("PASS rear-start production helper checks="+count);
 }
}'''
    }
    for n,text in sources.items():p=src/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
    shutil.copy(root/'RearRestart1926.java',src/'com/hiro/ulike/RearRestart1926.java')
    cmd=['javac','--release','8','-encoding','UTF-8','-d',str(classes),*map(str,sorted(src.rglob('*.java')))];cp=subprocess.run(cmd,capture_output=True,text=True);(folder/'javac.log').write_text(cp.stdout+cp.stderr);cp.check_returncode()
    cp=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.RestartTest1926'],capture_output=True,text=True);(work/'host-restart1926.txt').write_text(cp.stdout+cp.stderr);cp.check_returncode();return int(cp.stdout.strip().split('=')[-1])
