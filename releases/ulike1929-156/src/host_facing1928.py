from pathlib import Path
import subprocess,re
STUBS={
'android/content/SharedPreferences.java': '''package android.content; public interface SharedPreferences { boolean contains(String key); boolean getBoolean(String key, boolean fallback); Editor edit(); interface Editor { Editor putBoolean(String key,boolean value); boolean commit(); } }''',
'android/util/Log.java': '''package android.util;public class Log { public static int w(String t,String m){return 0;} public static int w(String t,String m,Throwable e){return 0;} }'''}
HARNESS=r'''
package com.hiro.ulike;
import java.util.*;import android.content.SharedPreferences;
public class FacingHost1928 {
 static final String KEY="last_use_front_camera";static int count;
 static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
 static class Prefs implements SharedPreferences {
  Map<String,Object> mem=new HashMap<>(),disk=new HashMap<>();int commits;boolean fail,throwWrite,throwRead;
  public boolean contains(String k){if(throwRead)throw new IllegalStateException();return mem.containsKey(k);}
  public boolean getBoolean(String k,boolean v){if(throwRead)throw new IllegalStateException();return mem.containsKey(k)?(Boolean)mem.get(k):v;}
  public Editor edit(){if(throwWrite)throw new IllegalStateException();return new Editor(){String key;boolean value;public Editor putBoolean(String k,boolean v){key=k;value=v;return this;}public boolean commit(){commits++;mem.put(key,value);if(fail)return false;disk.put(key,value);return true;}};}
  Prefs restart(){Prefs p=new Prefs();p.mem.putAll(disk);p.disk.putAll(disk);return p;}
 }
 static Prefs use(Prefs p){i.f.j0.f.a.b=p;i.f.j0.d.d.b=null;return p;}
 static void state(Boolean value){i.f.l.n.q.y.m.a=new i.f.l.n.q.y.m();var s=new i.f.l.u.j();s.facing=new i.f.l.u.p<>();s.facing.value=value;i.f.l.n.q.y.m.a.state=s;}
 static void flushReset()throws Exception{var f=FacingMemory1928.class.getDeclaredField("needsFlush");f.setAccessible(true);f.setBoolean(null,false);}
 public static void main(String[]args)throws Exception{
  Prefs p=use(new Prefs());flushReset();
  check(FacingMemory1928.initial(true),"unsaved front default");check(!FacingMemory1928.initial(false),"unsaved rear default");check(p.commits==0,"read never writes");
  p.mem.put("other_setting",true);p.disk.put("other_setting",true);
  state(true);FacingMemory1928.rememberCurrent();check(p.commits==1,"front same as default still persisted");check(Boolean.TRUE.equals(p.disk.get(KEY)),"front on disk before return");check(Boolean.TRUE.equals(i.f.j0.d.d.b),"native cache front");
  p=use(p.restart());flushReset();check(FacingMemory1928.initial(false),"front survives simulated process reset despite rear default");check(Boolean.TRUE.equals(p.disk.get("other_setting")),"other preference unchanged");
  for(int n=0;n<12;n++){
   boolean front=(n%2==0);FacingMemory1928.remember(front);check(p.disk.get(KEY).equals(front),"selection committed");check(i.f.j0.d.d.b.equals(front),"native cache follows selection");
   int before=p.commits;FacingMemory1928.remember(front);check(p.commits==before,"avoid repeated disk write for same durable selection");
   p=use(p.restart());flushReset();check(FacingMemory1928.initial(!front)==front,"saved selection wins opposite startup setting");
  }
  state(true);FacingMemory1928.rememberCurrent();p=use(p.restart());flushReset();check(FacingMemory1928.initial(false),"exit model front snapshot");
  state(false);FacingMemory1928.rememberCurrent();p=use(p.restart());flushReset();check(!FacingMemory1928.initial(true),"exit model rear snapshot");
  FacingMemory1928.remember(true);int before=p.commits;i.f.l.n.q.y.m.a=null;FacingMemory1928.rememberCurrent();check(p.commits==before&&FacingMemory1928.initial(false),"missing owner must not become rear");
  i.f.l.n.q.y.m.a=new i.f.l.n.q.y.m();FacingMemory1928.rememberCurrent();check(p.commits==before&&FacingMemory1928.initial(false),"missing model must not become rear");
  i.f.l.n.q.y.m.a.state=new i.f.l.u.j();FacingMemory1928.rememberCurrent();check(p.commits==before&&FacingMemory1928.initial(false),"missing property must not become rear");
  state(null);FacingMemory1928.rememberCurrent();check(p.commits==before&&FacingMemory1928.initial(false),"null value must not become rear");
  p.mem.put(KEY,"broken");check(!FacingMemory1928.initial(false),"corrupt type falls back");FacingMemory1928.remember(false);check(Boolean.FALSE.equals(p.disk.get(KEY)),"corrupt type repaired");
  p.fail=true;FacingMemory1928.remember(true);check(!Boolean.TRUE.equals(p.disk.get(KEY)),"failed disk write not reported successful");before=p.commits;p.fail=false;FacingMemory1928.remember(true);check(p.commits==before+1&&Boolean.TRUE.equals(p.disk.get(KEY)),"failed commit retried despite matching memory value");
  p.throwWrite=true;FacingMemory1928.remember(false);check(Boolean.TRUE.equals(p.disk.get(KEY)),"write exception retains last durable selection");p.throwWrite=false;FacingMemory1928.remember(false);check(Boolean.FALSE.equals(p.disk.get(KEY)),"retry write after exception");
  p.throwRead=true;check(FacingMemory1928.initial(true),"read exception fallback true");check(!FacingMemory1928.initial(false),"read exception fallback false");p.throwRead=false;
  i.f.j0.f.a.b=null;check(FacingMemory1928.initial(true),"null preferences fallback");FacingMemory1928.remember(true);i.f.j0.f.a.b=p;FacingMemory1928.remember(true);check(Boolean.TRUE.equals(p.disk.get(KEY)),"retry after unavailable preferences");
  state(false);FacingMemory1928.rememberCurrent();state(true);FacingMemory1928.rememberCurrent();p=use(p.restart());check(FacingMemory1928.initial(false),"last completed user choice wins");
  System.out.println("PASS "+count+" facing persistence assertions; actual helper on fake preferences/model, not Android/device execution");
 }
}
'''
def test(root,work):
 folder=work/'facing-host';folder.mkdir();classes=folder/'classes';classes.mkdir()
 for n,s in STUBS.items():
  p=folder/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s+'\n')
 (folder/'FacingHost1928.java').write_text(HARNESS)
 cmd=['javac','--release','8','-encoding','UTF-8','-d',str(classes),*[str(p) for p in sorted((root/'facing-stubs').rglob('*.java'))],str(root/'FacingMemory1928.java'),*[str(folder/n) for n in STUBS],str(folder/'FacingHost1928.java')]
 # Harness uses local-variable inference only in two convenience fixture lines.
 cmd[1:3]=['--release','11']
 subprocess.run(cmd,check=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 p=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.FacingHost1928'],check=True,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (work/'host-facing1928.txt').write_text(p.stdout);return int(re.search(r'PASS (\d+)',p.stdout).group(1))
