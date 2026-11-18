#!/usr/bin/env python3
"""Replay the old scheduling race with the published foreground runtime and a
deterministic Android clock/queue. This verifies the stale callback independently
of display rendering. Inputs: previous MPP, Enjarify checkout, ECJ JAR.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
for name in ('previous_mpp','enjarify','ecj'):
    parser.add_argument(name, type=lambda v: Path(v).resolve())
args = parser.parse_args()
extension = 'extensions/berry_foreground_tabs.mpe'
with zipfile.ZipFile(args.previous_mpp) as archive:
    dex = archive.read(extension)
digest = hashlib.sha256(dex).hexdigest()
if digest != '9af184ec2d3f70b7646f17b008edc02c29d83fd5525332afda6dfc65758f5a01':
    raise ValueError('The provided MPP does not contain the tested runtime.')

sources = {
    'ProvePending.java': '''import android.os.*;
import android.view.View;
import android.webkit.WebView;
import hiro.berry.TabPriority;
import hiro.berry.PreviewScheduler;
import java.util.*;
public class ProvePending {
 public static void main(String[] args) {
  WebView w=new WebView(); View container=new View();
  List<String> captured=new ArrayList<>();
  TabPriority.resumed(w);
  w.url="https://news.yahoo.co.jp/articles/example";
  TabPriority.started(w);TabPriority.finished(w,w.url);
  Runnable original=()->captured.add(w.url+"@"+SystemClock.uptimeMillis());
  if(!PreviewScheduler.defer(container,original))throw new AssertionError("not queued");
  Handler.advance(1741);
  w.url="https://news.yahoo.co.jp/categories/entertainment";
  if(args.length==0)TabPriority.started(w);
  TabPriority.finished(w,w.url);
  Handler.advance(1751);
  if(!captured.equals(List.of(w.url+"@1750")))throw new AssertionError(captured);
  System.out.println("pending article preview ran against current restored list, 9 ms after back: "+captured+"; optional TabPriority.started="+(args.length==0));
 }
}
''',
    'android/content/Context.java': '''package android.content;public class Context{}
''',
    'android/os/Bundle.java': '''package android.os;public class Bundle{}
''',
    'android/os/Handler.java': '''package android.os;
import java.util.*;
public class Handler {
 public Handler(Looper loop) {}
 static final class Task {Runnable r;long due;Task(Runnable r,long due){this.r=r;this.due=due;}}
 static final List<Task> tasks=new ArrayList<>();
 public boolean postDelayed(Runnable r,long delay){tasks.add(new Task(r,SystemClock.t+delay));return true;}
 public void removeCallbacks(Runnable r){tasks.removeIf(t->t.r==r);}
 public static void advance(long now){while(true){Task e=tasks.stream().min(Comparator.comparingLong(t->t.due)).orElse(null);if(e==null||e.due>now)break;SystemClock.t=e.due;tasks.remove(e);e.r.run();}SystemClock.t=now;}
}
''',
    'android/os/Looper.java': '''package android.os;public class Looper{static final Looper m=new Looper();public static Looper getMainLooper(){return m;}}
''',
    'android/os/SystemClock.java': '''package android.os;public class SystemClock{public static long t=1000;public static long uptimeMillis(){return t;}}
''',
    'android/view/MotionEvent.java': '''package android.view;public class MotionEvent{public int getActionMasked(){return 1;}}
''',
    'android/view/View.java': '''package android.view;
import android.content.Context;
public class View{
 public interface OnAttachStateChangeListener{void onViewAttachedToWindow(View v);void onViewDetachedFromWindow(View v);}
 public static final Context c=new Context();
 public Context getContext(){return c;}
 public boolean isAttachedToWindow(){return true;}
 public boolean isShown(){return true;}
 public boolean hasWindowFocus(){return true;}
 public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){}
 public void removeOnAttachStateChangeListener(OnAttachStateChangeListener l){}
}
''',
    'android/webkit/WebView.java': '''package android.webkit;
import android.view.View;
public class WebView extends View {
 public String url;
 public String getUrl(){return url;}
 public int getProgress(){return 100;}
 public void loadUrl(String url){this.url=url;}
}
''',
}
with tempfile.TemporaryDirectory(prefix='berry-preview-proof-') as temp:
    work = Path(temp)
    (work/'runtime.dex').write_bytes(dex)
    subprocess.run([sys.executable, '-m', 'enjarify.main', str(work/'runtime.dex'), '-o', str(work/'runtime.jar')],
                   cwd=args.enjarify, check=True, stdout=sys.stderr)
    paths = []
    for relative, content in sources.items():
        source = work/'src'/relative
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text(content)
        paths.append(str(source))
    classes = work/'classes'
    classes.mkdir()
    subprocess.run(['java','-jar',str(args.ecj),'-source','11','-target','11','-cp',str(work/'runtime.jar'),
                    '-d',str(classes),*paths],check=True)
    outcomes = []
    for optional_start in (True,False):
        cmd = ['java','-cp',str(classes)+':'+str(work/'runtime.jar'),'ProvePending']
        run = subprocess.run(cmd + ([] if optional_start else ['skip']), check=True, capture_output=True, text=True)
        outcomes.append({'start_observed':optional_start,'runtime_result':run.stdout.strip()})
    print(json.dumps({'result':'REPRODUCED','runtime_sha256':digest,'outcomes':outcomes,
                      'clock':'deterministic host JVM','android_renderer_exercised':False},indent=2))
